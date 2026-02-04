package com.rsring.network;

import com.rsring.item.ItemExperiencePump;
import com.rsring.util.BaublesHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 服务器 -> 客户端：同步修改后的储罐到客户端（解决饰品栏储罐存取后客户端不同步问题）
 */
public class PacketSyncTankSlots implements IMessage {

    private static final int TYPE_BAUBLES = 1;
    private static final int TYPE_INVENTORY = 2;

    private int locationType;
    private int slotIndex;
    private ItemStack tankStack;

    public PacketSyncTankSlots() {}

    public PacketSyncTankSlots(String locationType, int slotIndex, ItemStack tankStack) {
        this.locationType = "baubles".equals(locationType) ? TYPE_BAUBLES : TYPE_INVENTORY;
        this.slotIndex = slotIndex;
        this.tankStack = tankStack == null ? ItemStack.EMPTY : tankStack;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        locationType = buf.readByte();
        slotIndex = buf.readInt();
        tankStack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(locationType);
        buf.writeInt(slotIndex);
        ByteBufUtils.writeItemStack(buf, tankStack);
    }

    public static class Handler implements IMessageHandler<PacketSyncTankSlots, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncTankSlots msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null || msg.tankStack.isEmpty() || !(msg.tankStack.getItem() instanceof ItemExperiencePump)) {
                    return;
                }
                if (msg.locationType == TYPE_BAUBLES && BaublesHelper.isBaublesLoaded()) {
                    Object handler = BaublesHelper.getBaublesHandler(player);
                    if (handler != null) {
                        BaublesHelper.setStackInSlot(handler, msg.slotIndex, msg.tankStack);
                    }
                } else if (msg.locationType == TYPE_INVENTORY && msg.slotIndex >= 0 && msg.slotIndex < player.inventory.getSizeInventory()) {
                    player.inventory.setInventorySlotContents(msg.slotIndex, msg.tankStack);
                }
            });
            return null;
        }
    }
}
