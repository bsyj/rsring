package com.rsring.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.player.EntityPlayerMP;
import com.rsring.rsring.RsRingMod;
import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.event.CommonEventHandler;
import net.minecraft.item.ItemStack;

public class PacketToggleRsRing implements IMessage {

    public PacketToggleRsRing() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 该数据包不需要任何参数
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 该数据包不需要任何参数
    }

    public static class Handler implements IMessageHandler<PacketToggleRsRing, IMessage> {

        @Override
        public IMessage onMessage(PacketToggleRsRing message, MessageContext ctx) {
            // 在服务器线程上处理消息
            EntityPlayerMP player = ctx.getServerHandler().player;
            RsRingMod.proxy.handleToggleRsRing(player);
            return null;
        }
    }
}