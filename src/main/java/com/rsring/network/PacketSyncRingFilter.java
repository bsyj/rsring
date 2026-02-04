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

/** 客户端 -> 服务端：同步指定戒指的过滤槽位和黑白名单模式 */
public class PacketSyncRingFilter implements IMessage {

    private boolean whitelistMode;
    private String[] slots = new String[9];

    public PacketSyncRingFilter() {}

    public PacketSyncRingFilter(boolean whitelistMode, String[] slots) {
        this.whitelistMode = whitelistMode;
        if (slots != null) {
            for (int i = 0; i < Math.min(9, slots.length); i++) this.slots[i] = slots[i];
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        whitelistMode = buf.readBoolean();
        for (int i = 0; i < 9; i++) {
            boolean has = buf.readBoolean();
            if (has) slots[i] = ByteBufUtils.readUTF8String(buf);
            else slots[i] = "";
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(whitelistMode);
        for (int i = 0; i < 9; i++) {
            String s = slots[i];
            if (s != null && !s.isEmpty()) {
                buf.writeBoolean(true);
                ByteBufUtils.writeUTF8String(buf, s);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncRingFilter, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncRingFilter msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 使用 RingDetectionService 全面扫描玩家的所有物品栏（包括戒指，支持 Baubles 饰品栏）
                ItemStack stack = com.rsring.service.RingDetectionService.findRing(player, com.rsring.item.ItemAbsorbRing.class);
                // 未找到时检查主手/副手
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

                cap.setWhitelistMode(msg.whitelistMode);
                for (int i = 0; i < 9; i++) {
                    cap.setFilterSlot(i, msg.slots[i] == null ? "" : msg.slots[i]);
                }
                RsRingCapability.syncCapabilityToStack(stack, cap);
                // 如果指定戒指在 Baubles 饰品栏，需要标记 Baubles 为脏（修改）以便同步
                try {
                    com.rsring.service.RingDetectionService.markBaublesDirtyIfNeeded(player);
                } catch (Throwable t) {
                    // 忽略任何错误，不影响主流程
                }
                // 不同步时不发聊天提示，避免放一个物品就刷屏
            });
            return null;
        }
    }
}
