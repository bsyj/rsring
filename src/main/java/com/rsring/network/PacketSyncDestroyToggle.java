package com.rsring.network;

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端 -> 服务端：同步销毁开关状态
 */
public class PacketSyncDestroyToggle implements IMessage {

    private boolean destroyEnabled;

    public PacketSyncDestroyToggle() {}

    public PacketSyncDestroyToggle(boolean destroyEnabled) {
        this.destroyEnabled = destroyEnabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        destroyEnabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(destroyEnabled);
    }

    public static class Handler implements IMessageHandler<PacketSyncDestroyToggle, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncDestroyToggle msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 找到玩家的戒指
                ItemStack stack = com.rsring.service.RingDetectionService.findRing(
                    player, 
                    com.rsring.item.ItemAbsorbRing.class
                );
                
                if (stack == null || stack.isEmpty()) {
                    stack = player.getHeldItemMainhand();
                    if (stack.isEmpty() || !(stack.getItem() instanceof com.rsring.item.ItemAbsorbRing)) {
                        stack = player.getHeldItemOffhand();
                    }
                }
                
                if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof com.rsring.item.ItemAbsorbRing)) {
                    return;
                }

                IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
                if (cap == null) {
                    return;
                }
                
                if (!com.rsring.config.RsRingConfig.absorbRing.allowCustomFilters) {
                    return;
                }

                // 设置销毁开关状态
                cap.setDestroyEnabled(msg.destroyEnabled);
                RsRingCapability.syncCapabilityToStack(stack, cap);
                
                // 标记物品栏为脏，触发同步到客户端
                player.inventory.markDirty();
                
                // 标记 Baubles 为脏
                try {
                    com.rsring.service.RingDetectionService.markBaublesDirtyIfNeeded(player);
                } catch (Throwable t) {
                    // 忽略错误
                }
            });
            return null;
        }
    }
}
