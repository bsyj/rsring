package com.moremod.network;

import com.moremod.capability.IExperiencePumpCapability;
import com.moremod.capability.ExperiencePumpCapability;
import com.moremod.item.ItemExperiencePump;
import com.moremod.rsring.RsRingMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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
    private boolean isController; // 是否来自控制器

    public PacketPumpAction() {}

    public PacketPumpAction(EnumHand hand, int action) {
        this(hand, action, null, 0, false);
    }

    public PacketPumpAction(EnumHand hand, int action, int value) {
        this(hand, action, null, value, false);
    }

    public PacketPumpAction(EnumHand hand, int action, net.minecraft.item.ItemStack tankStack) {
        this(hand, action, tankStack, 0, true);
    }

    public PacketPumpAction(EnumHand hand, int action, net.minecraft.item.ItemStack tankStack, int value) {
        this(hand, action, tankStack, value, true);
    }

    public PacketPumpAction(EnumHand hand, int action, net.minecraft.item.ItemStack tankStack, int value, boolean isController) {
        this.handOrdinal = hand == EnumHand.MAIN_HAND ? 0 : 1;
        this.action = action;
        this.value = value;
        this.isController = isController;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        action = buf.readByte();
        value = buf.readInt();
        isController = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeByte(action);
        buf.writeInt(value);
        buf.writeBoolean(isController);
    }

    public static class Handler implements IMessageHandler<PacketPumpAction, IMessage> {
        @Override
        public IMessage onMessage(PacketPumpAction msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            EnumHand hand = msg.handOrdinal == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack controllerStack = player.getHeldItem(hand);

                if (controllerStack.isEmpty() || !(controllerStack.getItem() instanceof com.moremod.item.ItemExperiencePumpController)) {
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
                com.moremod.experience.ExperiencePumpController pumpController = com.moremod.experience.ExperiencePumpController.getInstance();
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
                        if (!controllerStack.isEmpty() && controllerStack.getItem() instanceof com.moremod.item.ItemExperiencePumpController) {
                            int ctrlMode = com.moremod.item.ItemExperiencePumpController.getMode(controllerStack);
                            int ctrlRetain = com.moremod.item.ItemExperiencePumpController.getRetainLevel(controllerStack);
                            boolean ctrlMend = com.moremod.item.ItemExperiencePumpController.isUseForMending(controllerStack);

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
                            com.moremod.item.ItemExperiencePumpController.setControllerData(controllerStack, ctrlMode, ctrlRetain, ctrlMend);
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
                            syncTankBack(tankStack, tankLocations.get(tankStack));
                        }
                        break;

                    case ACTION_TAKE_ALL: {
                        int totalTaken = 0;
                        for (ItemStack tankStack : prioritized) {
                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;
                            int take = cap.takeXp(cap.getXpStored());
                            if (take > 0) {
                                totalTaken += take;
                                ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                                syncTankBack(tankStack, tankLocations.get(tankStack));
                            }
                        }
                        if (totalTaken > 0) player.addExperience(totalTaken);
                        break;
                    }

                    case ACTION_TAKE_ONE: {
                        int levelsToTake = msg.value > 0 ? msg.value : 1;
                        int currentTotal = getPlayerTotalXp.get();
                        // Preserve player's fractional progress when calculating target total
                        float frac = player.experience; // 0.0 .. 1.0
                        int targetLevelInt = player.experienceLevel + levelsToTake;
                        int baseTarget = levelToTotalXp.apply(targetLevelInt);
                        int xpToNextTarget = getXPToNextLevel(targetLevelInt);
                        int targetTotal = baseTarget + Math.round(frac * xpToNextTarget);
                        int need = Math.max(0, targetTotal - currentTotal);
                        if (need <= 0) break;

                        int remaining = need;
                        int totalGiven = 0;
                        for (ItemStack tankStack : prioritized) {
                            if (remaining <= 0) break;
                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;
                            int available = cap.getXpStored();
                            if (available <= 0) continue;
                            int take = Math.min(available, remaining);
                            int taken = cap.takeXp(take);
                            if (taken > 0) {
                                remaining -= taken;
                                totalGiven += taken;
                                ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                                syncTankBack(tankStack, tankLocations.get(tankStack));
                            }
                        }
                        if (totalGiven > 0) player.addExperience(totalGiven);
                        break;
                    }

                    case ACTION_STORE_ALL: {
                        int playerTotal = getPlayerTotalXp.get();
                        if (playerTotal <= 0) break;
                        int remainingToStore = playerTotal;
                        int totalStored = 0;
                        for (ItemStack tankStack : prioritized) {
                            if (remainingToStore <= 0) break;
                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;
                            int space = cap.getMaxXp() - cap.getXpStored();
                            if (space <= 0) continue;
                            int store = Math.min(space, remainingToStore);
                            int stored = cap.addXp(store);
                            if (stored > 0) {
                                remainingToStore -= stored;
                                totalStored += stored;
                                ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                                syncTankBack(tankStack, tankLocations.get(tankStack));
                            }
                        }
                        if (totalStored > 0) addPlayerXp(player, -totalStored);
                        break;
                    }

                    case ACTION_STORE_ONE: {
                        int levelsToStore = msg.value > 0 ? msg.value : 1;
                        if (levelsToStore <= 0) break;
                        int currentTotal = getPlayerTotalXp.get();
                        // Preserve player's fractional progress when calculating target total
                        float frac = player.experience; // 0.0 .. 1.0
                        int targetLevel = Math.max(0, player.experienceLevel - levelsToStore);
                        int baseTarget = levelToTotalXp.apply(targetLevel);
                        int xpToNextTarget = getXPToNextLevel(targetLevel);
                        int targetTotal = baseTarget + Math.round(frac * xpToNextTarget);
                        int toStore = Math.max(0, currentTotal - targetTotal);
                        if (toStore <= 0) break;

                        int remainingToStore = toStore;
                        int totalStored = 0;
                        for (ItemStack tankStack : prioritized) {
                            if (remainingToStore <= 0) break;
                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;
                            int space = cap.getMaxXp() - cap.getXpStored();
                            if (space <= 0) continue;
                            int store = Math.min(space, remainingToStore);
                            int stored = cap.addXp(store);
                            if (stored > 0) {
                                remainingToStore -= stored;
                                totalStored += stored;
                                ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                                syncTankBack(tankStack, tankLocations.get(tankStack));
                            }
                        }
                        if (totalStored > 0) addPlayerXp(player, -totalStored);
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
        private ItemStack findExperienceTank(net.minecraft.entity.player.EntityPlayer player) {
            if (player == null) return net.minecraft.item.ItemStack.EMPTY;
            // 优先检查饰品栏（Baubles），再检查主手/副手，最后检查背包
            if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
                try {
                    Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                    Object handler = apiClass.getMethod("getBaublesHandler", net.minecraft.entity.player.EntityPlayer.class).invoke(null, player);
                    if (handler instanceof net.minecraft.inventory.IInventory) {
                        net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                        for (int i = 0; i < baubles.getSizeInventory(); i++) {
                            ItemStack stack = baubles.getStackInSlot(i);
                            if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) return stack;
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 检查主手和副手（其次）
            for (EnumHand h : EnumHand.values()) {
                ItemStack heldStack = player.getHeldItem(h);
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    return heldStack;
                }
            }

            // 最后检查背包
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    return stack;
                }
            }

            return net.minecraft.item.ItemStack.EMPTY;
        }

        private static int xpForOneLevel(int level) {
            if (level <= 0) return IExperiencePumpCapability.BASE_XP_PER_LEVEL;
            return levelToTotalXp(level) - levelToTotalXp(level - 1);
        }

        private static int getPlayerTotalXp(net.minecraft.entity.player.EntityPlayer player) {
            return (int) (player.experience * (float) player.xpBarCap()) + levelToTotalXp(player.experienceLevel);
        }

        private static int levelToTotalXp(int level) {
            if (level <= 0) return 0;
            if (level < 16) return level * (6 + level);
            if (level < 31) return (int) (level * (2.5 * level - 40.5) + 360);
            return (int) (level * (4.5 * level - 162.5) + 2220);
        }

        private static int getTotalXpForLevel(int level) {
            if (level <= 0) return 0;
            if (level < 16) return level * (6 + level);
            if (level < 31) return (int) (level * (2.5 * level - 40.5) + 360);
            return (int) (level * (4.5 * level - 162.5) + 2220);
        }

        private static int getXPToNextLevel(int level) {
            if (level < 0) return IExperiencePumpCapability.BASE_XP_PER_LEVEL;
            return levelToTotalXp(level + 1) - levelToTotalXp(level);
        }

        private static int getXPToPreviousLevel(int level) {
            if (level <= 0) return 0;
            return levelToTotalXp(level) - levelToTotalXp(level - 1);
        }

        private static void addPlayerXp(net.minecraft.entity.player.EntityPlayer player, int amount) {
            if (amount >= 0) {
                player.addExperience(amount);
                return;
            }
            int take = -amount;
            int current = getPlayerTotalXp(player);
            take = Math.min(take, current);
            if (take <= 0) return;
            int remain = current - take;
            player.experienceLevel = 0;
            player.experienceTotal = 0;
            player.experience = 0;
            if (remain > 0) player.addExperience(remain);
        }

        /** 在玩家和经验储罐之间泵送经验 */
        private void pumpExperienceBetweenPlayerAndTank(net.minecraft.entity.player.EntityPlayer player, IExperiencePumpCapability cap) {
            int retain = cap.getRetainLevel();
            int playerTotal = getPlayerTotalXp(player);
            int targetXp = getTotalXpForLevel(retain); // 使用精确的等级到经验转换

            if (cap.getMode() == IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER) {
                // 从玩家泵入：玩家高于保留等级时抽取
                if (playerTotal > targetXp) {
                    int take = Math.min(playerTotal - targetXp, cap.getMaxXp() - cap.getXpStored());
                    if (take > 0) {
                        addPlayerXp(player, -take);
                        cap.addXp(take);
                    }
                }
            } else if (cap.getMode() == IExperiencePumpCapability.MODE_PUMP_TO_PLAYER) {
                // 向玩家泵出：维持玩家达到保留等级，如果不足则从储罐泵送经验
                if (playerTotal < targetXp && cap.getXpStored() > 0) {
                    int need = targetXp - playerTotal;
                    int give = cap.takeXp(Math.min(need, 100));
                    if (give > 0) addPlayerXp(player, give);
                }
            }
        }

        /** 
         * 查找玩家身上的所有经验储罐
         * 返回 TankLocation 对象，包含储罐和位置信息，以便修改后能正确同步回去
         */
        private java.util.List<TankLocation> findAllExperienceTanksWithLocation(net.minecraft.entity.player.EntityPlayer player) {
            java.util.List<TankLocation> tanks = new java.util.ArrayList<>();
            if (player == null) return tanks;
            
            // 优先检查饰品栏（Baubles）
            if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
                try {
                    Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                    Object handler = apiClass.getMethod("getBaublesHandler", net.minecraft.entity.player.EntityPlayer.class).invoke(null, player);
                    if (handler instanceof net.minecraft.inventory.IInventory) {
                        net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                        for (int i = 0; i < baubles.getSizeInventory(); i++) {
                            ItemStack stack = baubles.getStackInSlot(i);
                            if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                                tanks.add(new TankLocation(stack, TankLocation.LocationType.BAUBLES, i, baubles));
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 然后检查背包，从前到后（index 0 开始）以保证优先使用背包前面的槽位
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    tanks.add(new TankLocation(stack, TankLocation.LocationType.PLAYER_INVENTORY, i, player.inventory));
                }
            }

            // 最后检查主手和副手
            for (EnumHand h : EnumHand.values()) {
                ItemStack heldStack = player.getHeldItem(h);
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    tanks.add(new TankLocation(heldStack, TankLocation.LocationType.HAND, h.ordinal(), null));
                }
            }

            return tanks;
        }
        
        /** 查找玩家身上的所有经验储罐（简化版，返回ItemStack列表） */
        private java.util.List<ItemStack> findAllExperienceTanks(net.minecraft.entity.player.EntityPlayer player) {
            java.util.List<ItemStack> tanks = new java.util.ArrayList<>();
            if (player == null) return tanks;
            // 优先检查饰品栏（Baubles）
            if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
                try {
                    Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                    Object handler = apiClass.getMethod("getBaublesHandler", net.minecraft.entity.player.EntityPlayer.class).invoke(null, player);
                    if (handler instanceof net.minecraft.inventory.IInventory) {
                        net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                        for (int i = 0; i < baubles.getSizeInventory(); i++) {
                            ItemStack stack = baubles.getStackInSlot(i);
                            if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                                tanks.add(stack);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 然后检查背包，从前到后（index 0 开始）以保证优先使用背包前面的槽位
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    tanks.add(stack);
                }
            }

            // 最后检查主手和副手
            for (EnumHand h : EnumHand.values()) {
                ItemStack heldStack = player.getHeldItem(h);
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
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
        private void recordTankLocations(net.minecraft.entity.player.EntityPlayer player, java.util.List<ItemStack> tanks, java.util.Map<ItemStack, TankLocationInfo> locations) {
            // 检查饰品栏
            if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
                try {
                    Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                    Object handler = apiClass.getMethod("getBaublesHandler", net.minecraft.entity.player.EntityPlayer.class).invoke(null, player);
                    if (handler instanceof net.minecraft.inventory.IInventory) {
                        net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                        for (int i = 0; i < baubles.getSizeInventory(); i++) {
                            ItemStack stack = baubles.getStackInSlot(i);
                            if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                                for (ItemStack tank : tanks) {
                                    if (tank == stack) { // 引用相等
                                        locations.put(tank, new TankLocationInfo("baubles", i, baubles));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
            
            // 检查背包
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    for (ItemStack tank : tanks) {
                        if (tank == stack) {
                            locations.put(tank, new TankLocationInfo("inventory", i, player.inventory));
                            break;
                        }
                    }
                }
            }
        }
        
        /** 同步储罐回原位置 */
        private void syncTankBack(ItemStack tank, TankLocationInfo location) {
            if (location == null) return;
            
            if ("baubles".equals(location.locationType) && location.inventory != null) {
                location.inventory.setInventorySlotContents(location.slotIndex, tank);
            } else if ("inventory".equals(location.locationType) && location.inventory != null) {
                location.inventory.setInventorySlotContents(location.slotIndex, tank);
            }
            // 手持物品会自动同步，无需特殊处理
        }
        
        /** 储罐位置信息，用于修改后同步回原位置 */
        private static class TankLocation {
            enum LocationType {
                BAUBLES,
                PLAYER_INVENTORY,
                HAND
            }
            
            final ItemStack tank;
            final LocationType locationType;
            final int slotIndex;
            final net.minecraft.inventory.IInventory inventory; // 用于 Baubles
            
            TankLocation(ItemStack tank, LocationType locationType, int slotIndex, net.minecraft.inventory.IInventory inventory) {
                this.tank = tank;
                this.locationType = locationType;
                this.slotIndex = slotIndex;
                this.inventory = inventory;
            }
            
            /** 将修改后的储罐同步回原位置 */
            void syncBack(net.minecraft.entity.player.EntityPlayer player) {
                switch (locationType) {
                    case BAUBLES:
                        if (inventory != null) {
                            inventory.setInventorySlotContents(slotIndex, tank);
                        }
                        break;
                    case PLAYER_INVENTORY:
                        player.inventory.setInventorySlotContents(slotIndex, tank);
                        break;
                    case HAND:
                        // 手持物品会自动同步，无需特殊处理
                        break;
                }
            }
        }
    }
}
