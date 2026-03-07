package com.rsring.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import net.minecraft.item.ItemStack;
import com.rsring.item.ItemAbsorbRing;

/**
 * 客户端 -> 服务端：同步过滤槽位的NBT数据
 * 用于NBT匹配功能
 */
public class PacketSyncFilterNBT implements IMessage {
    
    private int slotIndex;
    private NBTTagCompound nbt;
    private int slotType; // 0=物品ID过滤, 1=模组过滤, 2=销毁物品ID过滤, 3=销毁模组过滤
    
    public PacketSyncFilterNBT() {}
    
    public PacketSyncFilterNBT(int slotIndex, NBTTagCompound nbt, int slotType) {
        this.slotIndex = slotIndex;
        this.nbt = nbt;
        this.slotType = slotType;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        slotIndex = buf.readInt();
        slotType = buf.readInt();
        boolean hasNbt = buf.readBoolean();
        if (hasNbt) {
            nbt = ByteBufUtils.readTag(buf);
        } else {
            nbt = null;
        }
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slotIndex);
        buf.writeInt(slotType);
        if (nbt != null) {
            buf.writeBoolean(true);
            ByteBufUtils.writeTag(buf, nbt);
        } else {
            buf.writeBoolean(false);
        }
    }
    
    public static class Handler implements IMessageHandler<PacketSyncFilterNBT, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncFilterNBT message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 使用 RingDetectionService 全面扫描玩家的所有物品栏（包括戒指，支持 Baubles 饰品栏）
                ItemStack heldItem = com.rsring.service.RingDetectionService.findRing(player, ItemAbsorbRing.class);
                // 未找到时检查主手/副手
                if (heldItem == null || heldItem.isEmpty()) {
                    heldItem = player.getHeldItemMainhand();
                    if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemAbsorbRing)) {
                        heldItem = player.getHeldItemOffhand();
                    }
                }
                if (heldItem == null || heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemAbsorbRing)) {
                    return;
                }

                IRsRingCapability capability = heldItem.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
                if (capability == null) return;

                // 根据槽位类型设置NBT
                switch (message.slotType) {
                    case 0: // 物品ID过滤
                        capability.setFilterSlotNBT(message.slotIndex, message.nbt);
                        break;
                    case 1: // 模组过滤
                        capability.setModFilterSlotNBT(message.slotIndex, message.nbt);
                        break;
                    case 2: // 销毁物品ID过滤
                        capability.setDestroyFilterSlotNBT(message.slotIndex, message.nbt);
                        break;
                    case 3: // 销毁模组过滤
                        capability.setDestroyModFilterSlotNBT(message.slotIndex, message.nbt);
                        break;
                }

                // 同步到物品NBT
                RsRingCapability.syncCapabilityToStack(heldItem, capability);

                // 标记物品栏为脏，触发同步到客户端
                player.inventory.markDirty();

                // 如果指定戒指在 Baubles 饰品栏，需要标记 Baubles 为脏（修改）以便同步
                try {
                    com.rsring.service.RingDetectionService.markBaublesDirtyIfNeeded(player);
                } catch (Throwable t) {
                    // 忽略任何错误，不影响主流程
                }
            });
            return null;
        }
    }
}
