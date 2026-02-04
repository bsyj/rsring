package com.rsring.network;

import com.rsring.capability.IExperiencePumpCapability;
import com.rsring.capability.ExperiencePumpCapability;
import com.rsring.item.ItemExperiencePump;
import com.rsring.network.PacketSyncTankSlots;
import com.rsring.rsring.RsRingMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/** 客户端 -> 服务器：经验泵 GUI 按钮操作 */
public class PacketPumpAction implements IMessage {

    public static final int ACTION_MODE = 0;
    public static final int ACTION_RETAIN_UP = 1;
    public static final int ACTION_RETAIN_DOWN = 2;
    public static final int ACTION_TAKE_ALL = 3;
    public static final int ACTION_TAKE_ONE = 4;
    public static final int ACTION_STORE_ONE = 5;
    public static final int ACTION_STORE_ALL = 6;
    public static final int ACTION_MENDING = 7;

    private int handOrdinal;
    private int action;
    private int value; // 用于 RETAIN 或 TAKE_ONE/STORE_ONE 的级数

    public PacketPumpAction() {}

    public PacketPumpAction(EnumHand hand, int action) {
        this.handOrdinal = hand == EnumHand.MAIN_HAND ? 0 : 1;
        this.action = action;
        this.value = 0;
    }

    public PacketPumpAction(EnumHand hand, int action, int value) {
        this.handOrdinal = hand == EnumHand.MAIN_HAND ? 0 : 1;
        this.action = action;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        action = buf.readByte();
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeByte(action);
        buf.writeInt(value);
    }

    public static class Handler implements IMessageHandler<PacketPumpAction, IMessage> {
        @Override
        public IMessage onMessage(PacketPumpAction msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            EnumHand hand = msg.handOrdinal == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack controllerStack = player.getHeldItem(hand);

                if (controllerStack.isEmpty() || !(controllerStack.getItem() instanceof com.rsring.item.ItemExperiencePumpController)) {
                    return;
                }

                java.util.List<ItemStack> tankStacks = findAllExperienceTanks(player);
                // 记录储罐位置信息，用于后续同步
                java.util.Map<ItemStack, TankLocationInfo> tankLocations = new java.util.HashMap<>();
                recordTankLocations(player, tankStacks, tankLocations);
                if (tankStacks.isEmpty()) {
                    if (msg.action == ACTION_MENDING) {
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            net.minecraft.util.text.TextFormatting.RED + "未找到经验储罐！"));
                    }
                    return;
                }

                // 构建优先级列表：存入优先空罐，取出优先非空罐
                java.util.List<ItemStack> prioritized = new java.util.ArrayList<>();
                if (msg.action == ACTION_STORE_ONE || msg.action == ACTION_STORE_ALL) {
                    for (ItemStack t : tankStacks) {
                        int stored = ItemExperiencePump.getXpStoredFromNBT(t);
                        if (stored == 0) prioritized.add(t);
                    }
                    for (ItemStack t : tankStacks) {
                        int stored = ItemExperiencePump.getXpStoredFromNBT(t);
                        if (stored > 0) prioritized.add(t);
                    }
                } else if (msg.action == ACTION_TAKE_ONE || msg.action == ACTION_TAKE_ALL) {
                    for (ItemStack t : tankStacks) {
                        int stored = ItemExperiencePump.getXpStoredFromNBT(t);
                        if (stored > 0) prioritized.add(t);
                    }
                    for (ItemStack t : tankStacks) {
                        int stored = ItemExperiencePump.getXpStoredFromNBT(t);
                        if (stored == 0) prioritized.add(t);
                    }
                } else {
                    prioritized.addAll(tankStacks);
                }
                // 过滤出由控制器管理的储罐并移至优先/次序中，以使控制器管理下的储罐按控制器逻辑工作
                java.util.List<ItemStack> controllerManaged = new java.util.ArrayList<>();
                java.util.List<ItemStack> unmanaged = new java.util.ArrayList<>();
                com.rsring.experience.ExperiencePumpController pumpController = com.rsring.experience.ExperiencePumpController.getInstance();
                for (ItemStack t : prioritized) {
                    if (pumpController.isTankManagedByController(t)) controllerManaged.add(t);
                    else unmanaged.add(t);
                }
                // 将控制器管理的储罐放在前面，确保它们优先按控制器策略处理
                prioritized.clear();
                prioritized.addAll(controllerManaged);
                prioritized.addAll(unmanaged);

                // Use centralized ExperiencePumpController for XP/level calculations to avoid inconsistencies
                java.util.function.Function<Integer, Integer> levelToTotalXp = (lvl) -> pumpController.convertLevelToXP(lvl);
                java.util.function.Supplier<Integer> getPlayerTotalXp = () -> pumpController.getPlayerTotalExperience(player);

                switch (msg.action) {
                    case ACTION_MODE:
                    case ACTION_RETAIN_UP:
                    case ACTION_RETAIN_DOWN:
                    case ACTION_MENDING:
                        // Update controller item NBT on server so changes persist
                        if (!controllerStack.isEmpty() && controllerStack.getItem() instanceof com.rsring.item.ItemExperiencePumpController) {
                            int ctrlMode = com.rsring.item.ItemExperiencePumpController.getMode(controllerStack);
                            int ctrlRetain = com.rsring.item.ItemExperiencePumpController.getRetainLevel(controllerStack);
                            boolean ctrlMend = com.rsring.item.ItemExperiencePumpController.isUseForMending(controllerStack);

                            switch (msg.action) {
                                case ACTION_MODE:
                                    ctrlMode = (ctrlMode + 1) % 3;
                                    break;
                                case ACTION_RETAIN_UP:
                                    ctrlRetain = ctrlRetain + (msg.value > 0 ? msg.value : 1);
                                    break;
                                case ACTION_RETAIN_DOWN:
                                    ctrlRetain = Math.max(0, ctrlRetain - (msg.value > 0 ? msg.value : 1));
                                    break;
                                case ACTION_MENDING:
                                    ctrlMend = !ctrlMend;
                                    break;
                            }

                            // Persist updated controller data to the held controller item
                            com.rsring.item.ItemExperiencePumpController.setControllerData(controllerStack, ctrlMode, ctrlRetain, ctrlMend);
                        }

                        for (ItemStack tankStack : prioritized) {
                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;
                            switch (msg.action) {
                                case ACTION_MODE:
                                    cap.setMode((cap.getMode() + 1) % 3);
                                    // 模式切换后立即执行一次泵送操作
                                    pumpExperienceBetweenPlayerAndTank(player, cap);
                                    break;
                                case ACTION_RETAIN_UP:
                                    cap.setRetainLevel(cap.getRetainLevel() + (msg.value > 0 ? msg.value : 1));
                                    break;
                                case ACTION_RETAIN_DOWN:
                                    cap.setRetainLevel(Math.max(0, cap.getRetainLevel() - (msg.value > 0 ? msg.value : 1)));
                                    break;
                                case ACTION_MENDING:
                                    cap.setUseForMending(!cap.isUseForMending());
                                    break;
                            }
                            ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                            syncTankBack(tankStack, tankLocations.get(tankStack), player);
                        }
                        break;

                    case ACTION_TAKE_ALL: {
                        // 提取所有经验：从储罐中提取所有经验
                        int totalExtracted = 0;
                        
                        for (ItemStack tankStack : prioritized) {
                            int extracted = ItemExperiencePump.extractAllExperience(tankStack, player);
                            if (extracted > 0) {
                                totalExtracted += extracted;
                                syncTankBack(tankStack, tankLocations.get(tankStack), player);
                            }
                        }
                        break;
                    }

                    case ACTION_TAKE_ONE: {
                        // 取出N级：从储罐中提取指定等级数的经验
                        int levelsToTake = msg.value > 0 ? msg.value : 1;
                        if (levelsToTake <= 0) break;
                        
                        // 计算需要的总经验数（基于玩家当前等级）
                        int currentLevel = player.experienceLevel;
                        int targetLevel = currentLevel + levelsToTake;
                        int totalXPNeeded = com.rsring.util.XpHelper.getExperienceBetweenLevels(currentLevel, targetLevel);
                        if (totalXPNeeded <= 0) break;
                        
                        int totalExtracted = 0;
                        
                        for (ItemStack tankStack : prioritized) {
                            if (totalXPNeeded <= 0) break;
                            
                            // 从当前储罐中提取经验
                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;
                            
                            int available = cap.getXpStored();
                            if (available <= 0) continue;
                            
                            int take = Math.min(available, totalXPNeeded);
                            int extracted = cap.takeXp(take);
                            if (extracted > 0) {
                                totalExtracted += extracted;
                                totalXPNeeded -= extracted;
                                com.rsring.util.XpHelper.addExperienceToPlayer(player, extracted);
                                ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                                syncTankBack(tankStack, tankLocations.get(tankStack), player);
                            }
                        }
                        break;
                    }

                    case ACTION_STORE_ALL: {
                        // 存储所有经验：将玩家的所有经验存储到储罐中
                        int totalStored = 0;
                        
                        for (ItemStack tankStack : prioritized) {
                            int stored = ItemExperiencePump.storeAllExperience(tankStack, player);
                            if (stored > 0) {
                                totalStored += stored;
                                syncTankBack(tankStack, tankLocations.get(tankStack), player);
                            }
                        }
                        break;
                    }

                    case ACTION_STORE_ONE: {
                        // 存入N级：将指定等级数的经验存储到储罐中
                        int levelsToStore = msg.value > 0 ? msg.value : 1;
                        if (levelsToStore <= 0) break;
                        
                        // 计算需要存储的总经验数（基于玩家当前等级）
                        int currentLevel = player.experienceLevel;
                        int targetLevel = Math.max(0, currentLevel - levelsToStore);
                        int totalXPToStore = com.rsring.util.XpHelper.getExperienceBetweenLevels(targetLevel, currentLevel);
                        if (totalXPToStore <= 0) break;
                        
                        // 确保不超过玩家实际拥有的经验
                        int playerTotalXP = com.rsring.util.XpHelper.getPlayerTotalExperience(player);
                        int targetTotalXP = com.rsring.util.XpHelper.getExperienceForLevel(targetLevel);
                        if (playerTotalXP <= targetTotalXP) break;
                        totalXPToStore = Math.min(totalXPToStore, playerTotalXP - targetTotalXP);
                        if (totalXPToStore <= 0) break;
                        
                        int totalStored = 0;
                        
                        for (ItemStack tankStack : prioritized) {
                            if (totalXPToStore <= 0) break;
                            
                            // 向当前储罐中存储经验
                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;
                            
                            int availableSpace = cap.getMaxXp() - cap.getXpStored();
                            if (availableSpace <= 0) continue;
                            
                            int store = Math.min(availableSpace, totalXPToStore);
                            int stored = cap.addXp(store);
                            if (stored > 0) {
                                totalStored += stored;
                                totalXPToStore -= stored;
                                com.rsring.util.XpHelper.removeExperienceFromPlayer(player, stored);
                                ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                                syncTankBack(tankStack, tankLocations.get(tankStack), player);
                            }
                        }
                        break;
                    }

                    default:
                        break;
                }

                if (!tankStacks.isEmpty()) {
                    RsRingMod.network.sendTo(new PacketPumpData(tankStacks.get(0)), player);
                }
            });
            return null;
        }

        /** 查找玩家身上的经验储罐 */
        private ItemStack findExperienceTank(EntityPlayer player) {
            if (player == null) return net.minecraft.item.ItemStack.EMPTY;
            // 优先检查饰品栏（Baubles），再检查主手/副手，最后检查背包
            if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
                try {
                    Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                    Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);
                    if (handler instanceof net.minecraft.inventory.IInventory) {
                        net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                        for (int i = 0; i < baubles.getSizeInventory(); i++) {
                            ItemStack stack = baubles.getStackInSlot(i);
                            if (!stack.isEmpty() && stack.getItem() instanceof com.rsring.item.ItemExperiencePump) return stack;
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 检查主手和副手（其次）
            for (EnumHand h : EnumHand.values()) {
                ItemStack heldStack = player.getHeldItem(h);
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                    return heldStack;
                }
            }

            // 最后检查背包
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                    return stack;
                }
            }

            return net.minecraft.item.ItemStack.EMPTY;
        }



        /** 在玩家和经验储罐之间泵送经验 */
        private void pumpExperienceBetweenPlayerAndTank(EntityPlayer player, IExperiencePumpCapability cap) {
            int retain = cap.getRetainLevel();
            int playerTotal = com.rsring.util.XpHelper.getPlayerTotalExperience(player);
            int targetXp = com.rsring.util.XpHelper.getExperienceForLevel(retain); // 使用精确的等级到经验转换

            if (cap.getMode() == IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER) {
                // 从玩家泵入：玩家高于保留等级时抽取
                if (playerTotal > targetXp) {
                    int take = Math.min(playerTotal - targetXp, cap.getMaxXp() - cap.getXpStored());
                    if (take > 0) {
                        com.rsring.util.XpHelper.removeExperienceFromPlayer(player, take);
                        cap.addXp(take);
                    }
                }
            } else if (cap.getMode() == IExperiencePumpCapability.MODE_PUMP_TO_PLAYER) {
                // 向玩家泵出：维持玩家达到保留等级，如果不足则从储罐泵送经验
                if (playerTotal < targetXp && cap.getXpStored() > 0) {
                    int need = targetXp - playerTotal;
                    int give = cap.takeXp(Math.min(need, 100));
                    if (give > 0) com.rsring.util.XpHelper.addExperienceToPlayer(player, give);
                }
            }
        }


        
        /** 查找玩家身上的所有经验储罐（简化版，返回ItemStack列表） */
        private List<ItemStack> findAllExperienceTanks(EntityPlayer player) {
            List<ItemStack> tanks = new ArrayList<>();
            if (player == null) return tanks;
            // 优先检查饰品栏（Baubles）
            if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
                try {
                    Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                    Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);
                    if (handler instanceof net.minecraft.inventory.IInventory) {
                        net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                        for (int i = 0; i < baubles.getSizeInventory(); i++) {
                            ItemStack stack = baubles.getStackInSlot(i);
                            if (!stack.isEmpty() && stack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                                tanks.add(stack);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 然后检查背包，从前到后（index 0 开始）以保证优先使用背包前面的槽位
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                    tanks.add(stack);
                }
            }

            // 最后检查主手和副手
            for (EnumHand h : EnumHand.values()) {
                ItemStack heldStack = player.getHeldItem(h);
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                    tanks.add(heldStack);
                }
            }

            return tanks;
        }

        /** 储罐位置信息 */
        private static class TankLocationInfo {
            final String locationType; // "baubles", "inventory", "hand"
            final int slotIndex;
            final net.minecraft.inventory.IInventory inventory;
            
            TankLocationInfo(String locationType, int slotIndex, net.minecraft.inventory.IInventory inventory) {
                this.locationType = locationType;
                this.slotIndex = slotIndex;
                this.inventory = inventory;
            }
        }
        
        /** 记录储罐位置信息 */
        private void recordTankLocations(EntityPlayer player, List<ItemStack> tanks, Map<ItemStack, TankLocationInfo> locations) {
            java.util.Set<ItemStack> mapped = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

            // 检查饰品栏
            if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
                try {
                    Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                    Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);
                    if (handler instanceof net.minecraft.inventory.IInventory) {
                        net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                        for (int i = 0; i < baubles.getSizeInventory(); i++) {
                            ItemStack stack = baubles.getStackInSlot(i);
                            if (stack.isEmpty() || !(stack.getItem() instanceof com.rsring.item.ItemExperiencePump)) {
                                continue;
                            }
                            for (ItemStack tank : tanks) {
                                if (mapped.contains(tank)) continue;
                                if (tank == stack || ItemStack.areItemStacksEqual(tank, stack)) {
                                    locations.put(tank, new TankLocationInfo("baubles", i, baubles));
                                    mapped.add(tank);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 检查背包
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof com.rsring.item.ItemExperiencePump)) {
                    continue;
                }
                for (ItemStack tank : tanks) {
                    if (mapped.contains(tank)) continue;
                    if (tank == stack || ItemStack.areItemStacksEqual(tank, stack)) {
                        locations.put(tank, new TankLocationInfo("inventory", i, player.inventory));
                        mapped.add(tank);
                        break;
                    }
                }
            }
        }
        
        /** 同步储罐回原位置，并发送客户端同步包（解决饰品栏/背包储罐存取后客户端不同步） */
        private void syncTankBack(ItemStack tank, TankLocationInfo location, EntityPlayerMP player) {
            if (location == null) {
                return;
            }
            
            if ("baubles".equals(location.locationType) && location.inventory != null) {
                location.inventory.setInventorySlotContents(location.slotIndex, tank);
                location.inventory.markDirty(); // 关键：标记为脏，确保Baubles同步
                RsRingMod.network.sendTo(new PacketSyncTankSlots("baubles", location.slotIndex, tank), player);
            } else if ("inventory".equals(location.locationType) && location.inventory != null) {
                location.inventory.setInventorySlotContents(location.slotIndex, tank);
                location.inventory.markDirty(); // 标记为脏，确保背包同步
                RsRingMod.network.sendTo(new PacketSyncTankSlots("inventory", location.slotIndex, tank), player);
            }
            // 手持物品会自动同步，无需特殊处理
        }
        

    }
}
