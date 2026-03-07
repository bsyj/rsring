package com.rsring.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import com.rsring.item.ItemAbsorbRing;
import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import net.minecraft.item.ItemStack;

/**
 * 客户端 -> 服务端：同步模组过滤槽位
 * 用于模组过滤模式下的槽位同步
 */
public class PacketSyncModFilter implements IMessage {

    private String[] modSlots = new String[9];
    private boolean isDestroyMode;

    public PacketSyncModFilter() {}

    public PacketSyncModFilter(String[] modSlots, boolean isDestroyMode) {
        this.isDestroyMode = isDestroyMode;
        if (modSlots != null) {
            for (int i = 0; i < Math.min(9, modSlots.length); i++) {
                this.modSlots[i] = modSlots[i];
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        isDestroyMode = buf.readBoolean();
        for (int i = 0; i < 9; i++) {
            boolean has = buf.readBoolean();
            if (has) {
                modSlots[i] = ByteBufUtils.readUTF8String(buf);
            } else {
                modSlots[i] = "";
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(isDestroyMode);
        for (int i = 0; i < 9; i++) {
            String s = modSlots[i];
            if (s != null && !s.isEmpty()) {
                buf.writeBoolean(true);
                ByteBufUtils.writeUTF8String(buf, s);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncModFilter, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncModFilter msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 使用 RingDetectionService 查找戒指
                ItemStack stack = com.rsring.service.RingDetectionService.findRing(player, ItemAbsorbRing.class);
                if (stack == null || stack.isEmpty()) {
                    stack = player.getHeldItemMainhand();
                    if (stack.isEmpty() || !(stack.getItem() instanceof ItemAbsorbRing)) {
                        stack = player.getHeldItemOffhand();
                    }
                }
                if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemAbsorbRing)) return;

                IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
                if (cap == null) return;
                if (!com.rsring.config.RsRingConfig.absorbRing.allowCustomFilters) return;

                // 根据isDestroyMode设置对应模式的模组过滤槽位
                for (int i = 0; i < 9; i++) {
                    String modId = msg.modSlots[i] == null ? "" : msg.modSlots[i];
                    if (msg.isDestroyMode) {
                        cap.setDestroyModFilterSlot(i, modId);
                    } else {
                        cap.setModFilterSlot(i, modId);
                    }
                }
                RsRingCapability.syncCapabilityToStack(stack, cap);

                // 标记物品栏为脏
                player.inventory.markDirty();

                // 标记 Baubles 为脏
                try {
                    com.rsring.service.RingDetectionService.markBaublesDirtyIfNeeded(player);
                } catch (Throwable t) {
                    // 忽略
                }

                // 发送同步数据包回客户端
                com.rsring.rsring.RsRingMod.network.sendTo(
                    new com.rsring.network.PacketSyncCapabilityToClient(cap),
                    player
                );
            });
            return null;
        }
    }
}
