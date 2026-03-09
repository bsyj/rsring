package com.rsring.network;

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 增量同步数据包 - 只同步发生变化的数据
 * 大幅减少网络传输开销
 */
public class PacketSyncCapabilityDelta implements IMessage {

    // 同步标志位
    public static final int SYNC_ENABLED = 1 << 0;
    public static final int SYNC_FILTER_MODE = 1 << 1;
    public static final int SYNC_DESTROY_MODE = 1 << 2;
    public static final int SYNC_TRASH_CAN = 1 << 3;
    public static final int SYNC_ENERGY = 1 << 4;
    public static final int SYNC_FILTER_SLOTS = 1 << 5;
    public static final int SYNC_DESTROY_SLOTS = 1 << 6;
    public static final int SYNC_FLAGS = 1 << 7; // 布尔值打包

    private int syncFlags = 0;
    
    // 紧凑数据存储
    private byte boolFlags = 0;      // 打包的布尔值
    private byte destroyFlags = 0;   // 打包的销毁模式布尔值
    private int energy = 0;
    private String filterModeName = "";
    private String destroyFilterModeName = "";
    private String destroyModeTypeName = "";
    
    // 垃圾箱数据
    private boolean trashCanBound = false;
    private int trashCanX, trashCanY, trashCanZ;
    private int trashCanDimension;
    
    // 过滤槽位（只同步非空的）
    private int filterSlotMask = 0;  // 哪些槽位有数据
    private String[] filterSlots = new String[9];
    private int destroySlotMask = 0;
    private String[] destroySlots = new String[9];

    public PacketSyncCapabilityDelta() {}

    /**
     * 从 capability 创建增量同步包
     * @param cap 能力实例
     * @param dirtyFlags 脏标记，指示哪些数据需要同步
     */
    public PacketSyncCapabilityDelta(IRsRingCapability cap, int dirtyFlags) {
        // 根据脏标记决定同步哪些数据
        if ((dirtyFlags & RsRingCapability.DIRTY_ENABLED) != 0) {
            syncFlags |= SYNC_ENABLED;
        }
        if ((dirtyFlags & RsRingCapability.DIRTY_FILTER_MODE) != 0) {
            syncFlags |= SYNC_FILTER_MODE;
            this.filterModeName = cap.getFilterMode().getName();
        }
        if ((dirtyFlags & RsRingCapability.DIRTY_DESTROY) != 0) {
            syncFlags |= SYNC_DESTROY_MODE;
            this.destroyFilterModeName = cap.getDestroyFilterMode().getName();
            this.destroyModeTypeName = cap.getDestroyModeType().getName();
        }
        if ((dirtyFlags & RsRingCapability.DIRTY_TRASH) != 0) {
            syncFlags |= SYNC_TRASH_CAN;
            this.trashCanBound = cap.isTrashCanBound();
            if (this.trashCanBound && cap.getTrashCanPos() != null) {
                BlockPos pos = cap.getTrashCanPos();
                this.trashCanX = pos.getX();
                this.trashCanY = pos.getY();
                this.trashCanZ = pos.getZ();
                this.trashCanDimension = cap.getTrashCanDimension();
            }
        }
        if ((dirtyFlags & RsRingCapability.DIRTY_ENERGY) != 0) {
            syncFlags |= SYNC_ENERGY;
            this.energy = cap.getEnergyStorage().getEnergyStored();
        }
        if ((dirtyFlags & RsRingCapability.DIRTY_FILTERS) != 0) {
            syncFlags |= SYNC_FILTER_SLOTS;
            for (int i = 0; i < 9; i++) {
                String slot = cap.getFilterSlot(i);
                if (slot != null && !slot.isEmpty()) {
                    filterSlotMask |= (1 << i);
                    filterSlots[i] = slot;
                }
            }
        }
        if ((dirtyFlags & RsRingCapability.DIRTY_DESTROY) != 0) {
            syncFlags |= SYNC_DESTROY_SLOTS;
            for (int i = 0; i < 9; i++) {
                String slot = cap.getDestroyFilterSlot(i);
                if (slot != null && !slot.isEmpty()) {
                    destroySlotMask |= (1 << i);
                    destroySlots[i] = slot;
                }
            }
        }
        if ((dirtyFlags & (RsRingCapability.DIRTY_MISC | RsRingCapability.DIRTY_FILTER_MODE | RsRingCapability.DIRTY_DESTROY)) != 0) {
            syncFlags |= SYNC_FLAGS;
            // 打包布尔值
            this.boolFlags = packBooleans(cap);
            this.destroyFlags = packDestroyBooleans(cap);
        }
    }

    private byte packBooleans(IRsRingCapability cap) {
        byte flags = 0;
        if (cap.isEnabled()) flags |= 1;
        if (cap.isWhitelistMode()) flags |= 2;
        if (cap.isSealed()) flags |= 4;
        if (cap.isMatchAllMode()) flags |= 8;
        if (cap.shouldMatchNbt()) flags |= 16;
        if (cap.shouldMatchDurability()) flags |= 32;
        if (cap.isEasterEgg()) flags |= 64;
        return flags;
    }

    private byte packDestroyBooleans(IRsRingCapability cap) {
        byte flags = 0;
        if (cap.isDestroyEnabled()) flags |= 1;
        if (cap.isDestroyWhitelistMode()) flags |= 2;
        if (cap.isDestroyMatchAllMode()) flags |= 4;
        if (cap.shouldDestroyMatchNbt()) flags |= 8;
        if (cap.shouldDestroyMatchDurability()) flags |= 16;
        if (cap.shouldWorkInGUI()) flags |= 32;
        return flags;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        syncFlags = buf.readInt();
        
        if ((syncFlags & SYNC_FLAGS) != 0) {
            boolFlags = buf.readByte();
            destroyFlags = buf.readByte();
        }
        if ((syncFlags & SYNC_ENERGY) != 0) {
            energy = buf.readInt();
        }
        if ((syncFlags & SYNC_FILTER_MODE) != 0) {
            filterModeName = ByteBufUtils.readUTF8String(buf);
        }
        if ((syncFlags & SYNC_DESTROY_MODE) != 0) {
            destroyFilterModeName = ByteBufUtils.readUTF8String(buf);
            destroyModeTypeName = ByteBufUtils.readUTF8String(buf);
        }
        if ((syncFlags & SYNC_TRASH_CAN) != 0) {
            trashCanBound = buf.readBoolean();
            if (trashCanBound) {
                trashCanX = buf.readInt();
                trashCanY = buf.readInt();
                trashCanZ = buf.readInt();
                trashCanDimension = buf.readInt();
            }
        }
        if ((syncFlags & SYNC_FILTER_SLOTS) != 0) {
            filterSlotMask = buf.readInt();
            for (int i = 0; i < 9; i++) {
                if ((filterSlotMask & (1 << i)) != 0) {
                    filterSlots[i] = ByteBufUtils.readUTF8String(buf);
                }
            }
        }
        if ((syncFlags & SYNC_DESTROY_SLOTS) != 0) {
            destroySlotMask = buf.readInt();
            for (int i = 0; i < 9; i++) {
                if ((destroySlotMask & (1 << i)) != 0) {
                    destroySlots[i] = ByteBufUtils.readUTF8String(buf);
                }
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(syncFlags);
        
        if ((syncFlags & SYNC_FLAGS) != 0) {
            buf.writeByte(boolFlags);
            buf.writeByte(destroyFlags);
        }
        if ((syncFlags & SYNC_ENERGY) != 0) {
            buf.writeInt(energy);
        }
        if ((syncFlags & SYNC_FILTER_MODE) != 0) {
            ByteBufUtils.writeUTF8String(buf, filterModeName);
        }
        if ((syncFlags & SYNC_DESTROY_MODE) != 0) {
            ByteBufUtils.writeUTF8String(buf, destroyFilterModeName);
            ByteBufUtils.writeUTF8String(buf, destroyModeTypeName);
        }
        if ((syncFlags & SYNC_TRASH_CAN) != 0) {
            buf.writeBoolean(trashCanBound);
            if (trashCanBound) {
                buf.writeInt(trashCanX);
                buf.writeInt(trashCanY);
                buf.writeInt(trashCanZ);
                buf.writeInt(trashCanDimension);
            }
        }
        if ((syncFlags & SYNC_FILTER_SLOTS) != 0) {
            buf.writeInt(filterSlotMask);
            for (int i = 0; i < 9; i++) {
                if ((filterSlotMask & (1 << i)) != 0 && filterSlots[i] != null) {
                    ByteBufUtils.writeUTF8String(buf, filterSlots[i]);
                }
            }
        }
        if ((syncFlags & SYNC_DESTROY_SLOTS) != 0) {
            buf.writeInt(destroySlotMask);
            for (int i = 0; i < 9; i++) {
                if ((destroySlotMask & (1 << i)) != 0 && destroySlots[i] != null) {
                    ByteBufUtils.writeUTF8String(buf, destroySlots[i]);
                }
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncCapabilityDelta, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncCapabilityDelta msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) return;

                ItemStack ringStack = findRingStack(player);
                if (ringStack.isEmpty()) return;

                IRsRingCapability cap = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
                if (cap == null) return;

                // 应用增量更新
                if ((msg.syncFlags & SYNC_ENABLED) != 0) {
                    cap.setEnabled((msg.boolFlags & 1) != 0);
                }
                if ((msg.syncFlags & SYNC_FLAGS) != 0) {
                    // 解包布尔值
                    cap.setWhitelistMode((msg.boolFlags & 2) != 0);
                    cap.setSealed((msg.boolFlags & 4) != 0);
                    cap.setMatchAllMode((msg.boolFlags & 8) != 0);
                    cap.setMatchNbt((msg.boolFlags & 16) != 0);
                    cap.setMatchDurability((msg.boolFlags & 32) != 0);
                    // 彩蛋模式通常不变，但也可以更新
                }
                if ((msg.syncFlags & SYNC_ENERGY) != 0) {
                    // 能量需要特殊处理，通过存储接口更新
                    cap.getEnergyStorage().receiveEnergy(msg.energy - cap.getEnergyStorage().getEnergyStored(), false);
                }
                if ((msg.syncFlags & SYNC_FILTER_MODE) != 0 && !msg.filterModeName.isEmpty()) {
                    cap.setFilterMode(com.rsring.filter.FilterMode.fromName(msg.filterModeName));
                }
                if ((msg.syncFlags & SYNC_DESTROY_MODE) != 0) {
                    if (!msg.destroyFilterModeName.isEmpty()) {
                        cap.setDestroyFilterMode(com.rsring.filter.FilterMode.fromName(msg.destroyFilterModeName));
                    }
                    if (!msg.destroyModeTypeName.isEmpty()) {
                        cap.setDestroyModeType(com.rsring.capability.DestroyModeType.fromName(msg.destroyModeTypeName));
                    }
                    // 解包销毁模式布尔值
                    cap.setDestroyEnabled((msg.destroyFlags & 1) != 0);
                    cap.setDestroyWhitelistMode((msg.destroyFlags & 2) != 0);
                    cap.setDestroyMatchAllMode((msg.destroyFlags & 4) != 0);
                    cap.setDestroyMatchNbt((msg.destroyFlags & 8) != 0);
                    cap.setDestroyMatchDurability((msg.destroyFlags & 16) != 0);
                    cap.setShouldWorkInGUI((msg.destroyFlags & 32) != 0);
                }
                if ((msg.syncFlags & SYNC_TRASH_CAN) != 0) {
                    if (msg.trashCanBound) {
                        BlockPos pos = new BlockPos(msg.trashCanX, msg.trashCanY, msg.trashCanZ);
                        cap.bindTrashCan(net.minecraftforge.common.DimensionManager.getWorld(msg.trashCanDimension), pos);
                    } else {
                        cap.unbindTrashCan();
                    }
                }
                if ((msg.syncFlags & SYNC_FILTER_SLOTS) != 0) {
                    for (int i = 0; i < 9; i++) {
                        if ((msg.filterSlotMask & (1 << i)) != 0) {
                            cap.setFilterSlot(i, msg.filterSlots[i]);
                        } else {
                            cap.setFilterSlot(i, "");
                        }
                    }
                }
                if ((msg.syncFlags & SYNC_DESTROY_SLOTS) != 0) {
                    for (int i = 0; i < 9; i++) {
                        if ((msg.destroySlotMask & (1 << i)) != 0) {
                            cap.setDestroyFilterSlot(i, msg.destroySlots[i]);
                        } else {
                            cap.setDestroyFilterSlot(i, "");
                        }
                    }
                }

                // 同步到物品 NBT
                RsRingCapability.syncCapabilityToStack(ringStack, cap);
            });
            return null;
        }

        @SideOnly(Side.CLIENT)
        private ItemStack findRingStack(net.minecraft.entity.player.EntityPlayer player) {
            ItemStack mainHand = player.getHeldItemMainhand();
            if (!mainHand.isEmpty() && mainHand.getItem() instanceof com.rsring.item.ItemAbsorbRing) {
                return mainHand;
            }
            ItemStack offHand = player.getHeldItemOffhand();
            if (!offHand.isEmpty() && offHand.getItem() instanceof com.rsring.item.ItemAbsorbRing) {
                return offHand;
            }
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.rsring.item.ItemAbsorbRing) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
        }
    }
}
