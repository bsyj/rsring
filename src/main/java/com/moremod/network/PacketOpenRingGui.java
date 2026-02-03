package com.moremod.network;

import com.moremod.client.GuiHandler;
import com.moremod.rsring.RsRingMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端 -> 服务器：请求打开戒指过滤器 GUI
 */
public class PacketOpenRingGui implements IMessage {

    public PacketOpenRingGui() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        // 无需参数
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 无需参数
    }

    public static class Handler implements IMessageHandler<PacketOpenRingGui, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenRingGui message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 通过 Forge 的 GUI 系统打开 GUI，确保服务端和客户端同步
                player.openGui(RsRingMod.instance, GuiHandler.GUI_RING_FILTER, player.world, 
                              (int) player.posX, (int) player.posY, (int) player.posZ);
            });
            return null;
        }
    }
}
