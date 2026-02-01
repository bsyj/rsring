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

                // 验证手中确实是经验泵控制器
                if (controllerStack.isEmpty() || !(controllerStack.getItem() instanceof com.moremod.item.ItemExperiencePumpController)) {
                    return; // 手中不是控制器，无法操作
                }

                // 查找所有经验储罐
                java.util.List<ItemStack> tankStacks = findAllExperienceTanks(player);
                if (tankStacks.isEmpty()) {
                    // 如果是设置经验修补，即使没有储罐也可以发送反馈信息
                    if (msg.action == ACTION_MENDING) {
                        // 发送反馈信息告知玩家没有找到经验储罐
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            net.minecraft.util.text.TextFormatting.RED + "未找到经验储罐！"));
                    }
                    return; // 没有经验储罐，无法操作
                }

                // 对所有储罐执行操作
                for (ItemStack tankStack : tankStacks) {
                    IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                    if (cap == null) continue;

                    switch (msg.action) {
                        case ACTION_MODE:
                            cap.setMode((cap.getMode() + 1) % 3);
                            break;
                        case ACTION_RETAIN_UP:
                            cap.setRetainLevel(cap.getRetainLevel() + (msg.value > 0 ? msg.value : 1));
                            break;
                        case ACTION_RETAIN_DOWN:
                            cap.setRetainLevel(Math.max(0, cap.getRetainLevel() - (msg.value > 0 ? msg.value : 1)));
                            break;
                        case ACTION_TAKE_ALL:
                            int takeAll = cap.takeXp(cap.getXpStored());
                            if (takeAll > 0) player.addExperience(takeAll);
                            break;
                        case ACTION_TAKE_ONE:
                            // 取出N级经验：value参数表示要取出的等级数
                            int levelsToTake = msg.value > 0 ? msg.value : 1;
                            for (int i = 0; i < levelsToTake; i++) {
                                int playerLevel = player.experienceLevel;
                                // 计算1级的经验值（使用Minecraft的经验公式）
                                int oneLevelXP = xpForOneLevel(playerLevel);
                                
                                // 从储罐取出1级的经验
                                int xpToTake = Math.min(oneLevelXP, cap.getXpStored());
                                if (xpToTake > 0) {
                                    int taken = cap.takeXp(xpToTake);
                                    if (taken > 0) {
                                        player.addExperience(taken);
                                    }
                                } else {
                                    break; // 储罐没有足够的经验，停止
                                }
                            }
                            break;
                        case ACTION_STORE_ONE:
                            // 存入N级经验：value参数表示要存入的等级数
                            int levelsToStore = msg.value > 0 ? msg.value : 1;
                            for (int i = 0; i < levelsToStore; i++) {
                                int currentLevel = player.experienceLevel;
                                if (currentLevel > 0) {
                                    // 计算当前等级的1级经验值
                                    int oneLevelXPStore = xpForOneLevel(currentLevel - 1);
                                    
                                    // 确保不超过储罐剩余容量
                                    int availableSpace = cap.getMaxXp() - cap.getXpStored();
                                    int xpToStore = Math.min(oneLevelXPStore, availableSpace);
                                    
                                    // 确保玩家有足够的经验
                                    int playerTotalXp = getPlayerTotalXp(player);
                                    xpToStore = Math.min(xpToStore, playerTotalXp);
                                    
                                    if (xpToStore > 0) {
                                        addPlayerXp(player, -xpToStore);
                                        cap.addXp(xpToStore);
                                    } else {
                                        break; // 玩家没有足够的经验或储罐已满，停止
                                    }
                                } else {
                                    break; // 玩家等级为0，停止
                                }
                            }
                            break;
                        case ACTION_STORE_ALL:
                            int all = getPlayerTotalXp(player);
                            int stored = cap.addXp(Math.min(all, cap.getMaxXp() - cap.getXpStored()));
                            if (stored > 0) addPlayerXp(player, -stored);
                            break;
                        case ACTION_MENDING:
                            cap.setUseForMending(!cap.isUseForMending());
                            break;
                    }

                    // 如果当前模式是泵入或泵出，则执行相应的操作
                    if (cap.getMode() != IExperiencePumpCapability.MODE_OFF) {
                        pumpExperienceBetweenPlayerAndTank(player, cap);
                    }

                    // 同步能力到物品堆栈
                    ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                }

                // 发送数据包更新客户端（使用第一个储罐的数据作为代表）
                if (!tankStacks.isEmpty()) {
                    RsRingMod.network.sendTo(new PacketPumpData(tankStacks.get(0)), player);
                }
            });
            return null;
        }

        /** 查找玩家身上的经验储罐 */
        private ItemStack findExperienceTank(net.minecraft.entity.player.EntityPlayer player) {
            if (player == null) return net.minecraft.item.ItemStack.EMPTY;

            // 检查主手和副手
            for (EnumHand h : EnumHand.values()) {
                ItemStack heldStack = player.getHeldItem(h);
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    return heldStack;
                }
            }

            // 检查背包
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    return stack;
                }
            }

            // 检查饰品栏
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

            return net.minecraft.item.ItemStack.EMPTY;
        }

        private static int xpForOneLevel(int level) {
            if (level <= 0) return IExperiencePumpCapability.XP_PER_LEVEL;
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
            int targetXp = levelToTotalXp(retain);

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
                // 向玩家泵出：维持玩家在保留等级
                if (playerTotal < targetXp && cap.getXpStored() > 0) {
                    int need = targetXp - playerTotal;
                    int give = cap.takeXp(Math.min(need, 100));
                    if (give > 0) addPlayerXp(player, give);
                }
            }
        }

        /** 查找玩家身上的所有经验储罐 */
        private java.util.List<ItemStack> findAllExperienceTanks(net.minecraft.entity.player.EntityPlayer player) {
            java.util.List<ItemStack> tanks = new java.util.ArrayList<>();
            if (player == null) return tanks;

            // 检查主手和副手
            for (EnumHand h : EnumHand.values()) {
                ItemStack heldStack = player.getHeldItem(h);
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    tanks.add(heldStack);
                }
            }

            // 检查背包
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePump) {
                    tanks.add(stack);
                }
            }

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
                                tanks.add(stack);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            return tanks;
        }
    }
}
