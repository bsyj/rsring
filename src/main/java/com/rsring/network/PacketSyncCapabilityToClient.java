package com.rsring.network;

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.filter.FilterMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 服务端 -> 客户端：同步戒指 capability 数据
 * 用于服务器更新后通知客户端刷新数据
 * 支持同步吸收模式和销毁模式的完整数据
 */
public class PacketSyncCapabilityToClient implements IMessage {

    // 吸收模式数据
    private FilterMode filterMode;
    private boolean whitelistMode;
    private boolean matchAllMode;
    private String[] itemSlots = new String[9];

    // 销毁模式数据
    private FilterMode destroyFilterMode;
    private boolean destroyWhitelistMode;
    private boolean destroyMatchAllMode;
    private String[] destroyItemSlots = new String[9];
    private boolean destroyEnabled;
    private boolean destroyModeUI;
    
    // 垃圾箱绑定数据
    private boolean trashCanBound;
    private int trashCanX, trashCanY, trashCanZ;
    private int trashCanDimension;

    public PacketSyncCapabilityToClient() {
        this.filterMode = FilterMode.ITEM;
        this.destroyFilterMode = FilterMode.ITEM;
    }

    public PacketSyncCapabilityToClient(IRsRingCapability cap) {
        // 吸收模式数据
        this.filterMode = cap.getFilterMode();
        this.whitelistMode = cap.isWhitelistMode();
        this.matchAllMode = cap.isMatchAllMode();
        for (int i = 0; i < 9; i++) {
            this.itemSlots[i] = cap.getFilterSlot(i);
        }

        // 销毁模式数据
        this.destroyFilterMode = cap.getDestroyFilterMode();
        this.destroyWhitelistMode = cap.isDestroyWhitelistMode();
        this.destroyMatchAllMode = cap.isDestroyMatchAllMode();
        for (int i = 0; i < 9; i++) {
            this.destroyItemSlots[i] = cap.getDestroyFilterSlot(i);
        }
        this.destroyEnabled = cap.isDestroyEnabled();
        this.destroyModeUI = cap.isDestroyModeUI();
        
        // 垃圾箱绑定数据
        this.trashCanBound = cap.isTrashCanBound();
        if (this.trashCanBound && cap.getTrashCanPos() != null) {
            this.trashCanX = cap.getTrashCanPos().getX();
            this.trashCanY = cap.getTrashCanPos().getY();
            this.trashCanZ = cap.getTrashCanPos().getZ();
            this.trashCanDimension = cap.getTrashCanDimension();
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 吸收模式数据
        filterMode = FilterMode.fromName(ByteBufUtils.readUTF8String(buf));
        whitelistMode = buf.readBoolean();
        matchAllMode = buf.readBoolean();
        for (int i = 0; i < 9; i++) {
            boolean has = buf.readBoolean();
            if (has) {
                itemSlots[i] = ByteBufUtils.readUTF8String(buf);
            } else {
                itemSlots[i] = "";
            }
        }

        // 销毁模式数据
        destroyFilterMode = FilterMode.fromName(ByteBufUtils.readUTF8String(buf));
        destroyWhitelistMode = buf.readBoolean();
        destroyMatchAllMode = buf.readBoolean();
        for (int i = 0; i < 9; i++) {
            boolean has = buf.readBoolean();
            if (has) {
                destroyItemSlots[i] = ByteBufUtils.readUTF8String(buf);
            } else {
                destroyItemSlots[i] = "";
            }
        }
        destroyEnabled = buf.readBoolean();
        destroyModeUI = buf.readBoolean();
        
        // 垃圾箱绑定数据
        trashCanBound = buf.readBoolean();
        if (trashCanBound) {
            trashCanX = buf.readInt();
            trashCanY = buf.readInt();
            trashCanZ = buf.readInt();
            trashCanDimension = buf.readInt();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 吸收模式数据
        ByteBufUtils.writeUTF8String(buf, filterMode.getName());
        buf.writeBoolean(whitelistMode);
        buf.writeBoolean(matchAllMode);
        for (int i = 0; i < 9; i++) {
            String s = itemSlots[i];
            if (s != null && !s.isEmpty()) {
                buf.writeBoolean(true);
                ByteBufUtils.writeUTF8String(buf, s);
            } else {
                buf.writeBoolean(false);
            }
        }

        // 销毁模式数据
        ByteBufUtils.writeUTF8String(buf, destroyFilterMode.getName());
        buf.writeBoolean(destroyWhitelistMode);
        buf.writeBoolean(destroyMatchAllMode);
        for (int i = 0; i < 9; i++) {
            String s = destroyItemSlots[i];
            if (s != null && !s.isEmpty()) {
                buf.writeBoolean(true);
                ByteBufUtils.writeUTF8String(buf, s);
            } else {
                buf.writeBoolean(false);
            }
        }
        buf.writeBoolean(destroyEnabled);
        buf.writeBoolean(destroyModeUI);
        
        // 垃圾箱绑定数据
        buf.writeBoolean(trashCanBound);
        if (trashCanBound) {
            buf.writeInt(trashCanX);
            buf.writeInt(trashCanY);
            buf.writeInt(trashCanZ);
            buf.writeInt(trashCanDimension);
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncCapabilityToClient, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncCapabilityToClient msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                // 在客户端找到戒指并更新 capability
                net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) return;

                // 尝试在主手、副手、背包中找到戒指
                ItemStack ringStack = findRingStack(player);
                if (ringStack.isEmpty()) return;

                IRsRingCapability cap = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
                if (cap == null) return;

                // 更新吸收模式数据
                cap.setFilterMode(msg.filterMode);
                cap.setWhitelistMode(msg.whitelistMode);
                cap.setMatchAllMode(msg.matchAllMode);
                for (int i = 0; i < 9; i++) {
                    cap.setFilterSlot(i, msg.itemSlots[i] == null ? "" : msg.itemSlots[i]);
                }

                // 更新销毁模式数据
                cap.setDestroyFilterMode(msg.destroyFilterMode);
                cap.setDestroyWhitelistMode(msg.destroyWhitelistMode);
                cap.setDestroyMatchAllMode(msg.destroyMatchAllMode);
                for (int i = 0; i < 9; i++) {
                    cap.setDestroyFilterSlot(i, msg.destroyItemSlots[i] == null ? "" : msg.destroyItemSlots[i]);
                }
                cap.setDestroyEnabled(msg.destroyEnabled);
                cap.setDestroyModeUI(msg.destroyModeUI);
                
                // 更新垃圾箱绑定数据
                if (msg.trashCanBound) {
                    net.minecraft.util.math.BlockPos trashPos = new net.minecraft.util.math.BlockPos(msg.trashCanX, msg.trashCanY, msg.trashCanZ);
                    cap.bindTrashCan(net.minecraftforge.common.DimensionManager.getWorld(msg.trashCanDimension), trashPos);
                } else {
                    cap.unbindTrashCan();
                }

                // 同步到物品 NBT
                RsRingCapability.syncCapabilityToStack(ringStack, cap);
            });
            return null;
        }

        @SideOnly(Side.CLIENT)
        private ItemStack findRingStack(net.minecraft.entity.player.EntityPlayer player) {
            // 主手
            ItemStack mainHand = player.getHeldItemMainhand();
            if (!mainHand.isEmpty() && mainHand.getItem() instanceof com.rsring.item.ItemAbsorbRing) {
                return mainHand;
            }
            // 副手
            ItemStack offHand = player.getHeldItemOffhand();
            if (!offHand.isEmpty() && offHand.getItem() instanceof com.rsring.item.ItemAbsorbRing) {
                return offHand;
            }
            // 背包
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
