package com.rsring.network;

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.filter.FilterMode;
import com.rsring.filter.ItemAttribute;
import com.rsring.util.Pair;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 -> 服务端：同步高级过滤系统数据
 * 支持三种过滤模式和属性过滤列表
 * 支持销毁模式和吸收模式
 */
public class PacketSyncAdvancedFilter implements IMessage {

    private FilterMode filterMode;
    private boolean whitelistMode;
    private boolean matchAllMode;
    
    // 物品过滤
    private String[] itemSlots = new String[9];
    
    // 模组过滤
    private String[] modSlots = new String[9];
    
    // 属性过滤
    private List<NBTTagCompound> attributeTags = new ArrayList<>();
    private boolean[] attributeInverted = new boolean[9];
    
    // NBT和耐久匹配选项
    private boolean matchNbt;
    private boolean matchDurability;
    
    // 是否为销毁模式
    private boolean isDestroyMode;

    public PacketSyncAdvancedFilter() {
        this.filterMode = FilterMode.ITEM;
        this.isDestroyMode = false;
    }

    public PacketSyncAdvancedFilter(FilterMode filterMode, boolean whitelistMode, boolean matchAllMode,
                                    String[] itemSlots, String[] modSlots, 
                                    List<Pair<ItemAttribute, Boolean>> attributes,
                                    boolean matchNbt, boolean matchDurability) {
        this(filterMode, whitelistMode, matchAllMode, itemSlots, modSlots, attributes, matchNbt, matchDurability, false);
    }

    public PacketSyncAdvancedFilter(FilterMode filterMode, boolean whitelistMode, boolean matchAllMode,
                                    String[] itemSlots, String[] modSlots, 
                                    List<Pair<ItemAttribute, Boolean>> attributes,
                                    boolean matchNbt, boolean matchDurability, boolean isDestroyMode) {
        this.filterMode = filterMode;
        this.whitelistMode = whitelistMode;
        this.matchAllMode = matchAllMode;
        this.matchNbt = matchNbt;
        this.matchDurability = matchDurability;
        this.isDestroyMode = isDestroyMode;
        
        if (itemSlots != null) {
            System.arraycopy(itemSlots, 0, this.itemSlots, 0, Math.min(9, itemSlots.length));
        }
        
        if (modSlots != null) {
            System.arraycopy(modSlots, 0, this.modSlots, 0, Math.min(9, modSlots.length));
        }
        
        if (attributes != null) {
            for (int i = 0; i < Math.min(9, attributes.size()); i++) {
                Pair<ItemAttribute, Boolean> pair = attributes.get(i);
                NBTTagCompound tag = new NBTTagCompound();
                pair.getKey().serializeNBT(tag); // 使用serializeNBT包含完整的属性信息
                attributeTags.add(tag);
                attributeInverted[i] = pair.getValue();
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        filterMode = FilterMode.fromName(ByteBufUtils.readUTF8String(buf));
        whitelistMode = buf.readBoolean();
        matchAllMode = buf.readBoolean();
        
        // 读取物品槽
        for (int i = 0; i < 9; i++) {
            boolean has = buf.readBoolean();
            if (has) {
                itemSlots[i] = ByteBufUtils.readUTF8String(buf);
            } else {
                itemSlots[i] = "";
            }
        }
        
        // 读取模组槽
        for (int i = 0; i < 9; i++) {
            boolean has = buf.readBoolean();
            if (has) {
                modSlots[i] = ByteBufUtils.readUTF8String(buf);
            } else {
                modSlots[i] = "";
            }
        }
        
        // 读取属性列表
        int attrCount = buf.readInt();
        attributeTags.clear();
        for (int i = 0; i < Math.min(9, attrCount); i++) {
            NBTTagCompound tag = ByteBufUtils.readTag(buf);
            attributeTags.add(tag);
            attributeInverted[i] = buf.readBoolean();
        }
        
        // 读取NBT和耐久匹配选项
        matchNbt = buf.readBoolean();
        matchDurability = buf.readBoolean();
        
        // 兼容旧版本数据包
        if (buf.isReadable()) {
            isDestroyMode = buf.readBoolean();
        } else {
            isDestroyMode = false;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, filterMode.getName());
        buf.writeBoolean(whitelistMode);
        buf.writeBoolean(matchAllMode);
        
        // 写入物品槽
        for (int i = 0; i < 9; i++) {
            String s = itemSlots[i];
            if (s != null && !s.isEmpty()) {
                buf.writeBoolean(true);
                ByteBufUtils.writeUTF8String(buf, s);
            } else {
                buf.writeBoolean(false);
            }
        }
        
        // 写入模组槽
        for (int i = 0; i < 9; i++) {
            String s = modSlots[i];
            if (s != null && !s.isEmpty()) {
                buf.writeBoolean(true);
                ByteBufUtils.writeUTF8String(buf, s);
            } else {
                buf.writeBoolean(false);
            }
        }
        
        // 写入属性列表
        buf.writeInt(Math.min(9, attributeTags.size()));
        for (int i = 0; i < Math.min(9, attributeTags.size()); i++) {
            ByteBufUtils.writeTag(buf, attributeTags.get(i));
            buf.writeBoolean(attributeInverted[i]);
        }
        
        // 写入NBT和耐久匹配选项
        buf.writeBoolean(matchNbt);
        buf.writeBoolean(matchDurability);
        
        // 写入是否为销毁模式
        buf.writeBoolean(isDestroyMode);
    }

    public static class Handler implements IMessageHandler<PacketSyncAdvancedFilter, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncAdvancedFilter msg, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                // 找到玩家的戒指
                ItemStack stack = com.rsring.service.RingDetectionService.findRing(
                    ctx.getServerHandler().player, 
                    com.rsring.item.ItemAbsorbRing.class
                );
                
                if (stack == null || stack.isEmpty()) {
                    stack = ctx.getServerHandler().player.getHeldItemMainhand();
                    if (stack.isEmpty() || !(stack.getItem() instanceof com.rsring.item.ItemAbsorbRing)) {
                        stack = ctx.getServerHandler().player.getHeldItemOffhand();
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

                if (msg.isDestroyMode) {
                    // 销毁模式数据同步
                    cap.setDestroyFilterMode(msg.filterMode);
                    cap.setDestroyWhitelistMode(msg.whitelistMode);
                    cap.setDestroyMatchAllMode(msg.matchAllMode);
                    
                    // 同步销毁物品槽
                    for (int i = 0; i < 9; i++) {
                        cap.setDestroyFilterSlot(i, msg.itemSlots[i] == null ? "" : msg.itemSlots[i]);
                    }
                    
                    // 同步销毁模组槽
                    cap.getDestroyFilterMods().clear();
                    for (int i = 0; i < 9; i++) {
                        if (msg.modSlots[i] != null && !msg.modSlots[i].isEmpty()) {
                            cap.getDestroyFilterMods().add(msg.modSlots[i]);
                        }
                    }
                    
                    // 同步销毁属性列表
                    cap.getDestroyFilterAttributes().clear();
                    for (int i = 0; i < msg.attributeTags.size(); i++) {
                        NBTTagCompound tag = msg.attributeTags.get(i);
                        ItemAttribute attr = ItemAttribute.fromNBT(tag);
                        if (attr != null) {
                            cap.addDestroyFilterAttribute(attr, msg.attributeInverted[i]);
                        }
                    }
                    
                    // 同步销毁NBT和耐久匹配选项
                    cap.setDestroyMatchNbt(msg.matchNbt);
                    cap.setDestroyMatchDurability(msg.matchDurability);
                } else {
                    // 吸收模式数据同步
                    cap.setFilterMode(msg.filterMode);
                    cap.setWhitelistMode(msg.whitelistMode);
                    cap.setMatchAllMode(msg.matchAllMode);
                    
                    // 同步物品槽
                    for (int i = 0; i < 9; i++) {
                        cap.setFilterSlot(i, msg.itemSlots[i] == null ? "" : msg.itemSlots[i]);
                    }
                    
                    // 同步模组槽
                    cap.getFilterMods().clear();
                    for (int i = 0; i < 9; i++) {
                        if (msg.modSlots[i] != null && !msg.modSlots[i].isEmpty()) {
                            cap.getFilterMods().add(msg.modSlots[i]);
                        }
                    }
                    
                    // 同步属性列表
                    cap.getFilterAttributes().clear();
                    for (int i = 0; i < msg.attributeTags.size(); i++) {
                        NBTTagCompound tag = msg.attributeTags.get(i);
                        ItemAttribute attr = ItemAttribute.fromNBT(tag);
                        if (attr != null) {
                            cap.addFilterAttribute(attr, msg.attributeInverted[i]);
                        }
                    }
                    
                    // 同步NBT和耐久匹配选项
                    cap.setMatchNbt(msg.matchNbt);
                    cap.setMatchDurability(msg.matchDurability);
                }
                
                RsRingCapability.syncCapabilityToStack(stack, cap);
                
                // 标记物品栏为脏，触发同步到客户端
                EntityPlayerMP player = ctx.getServerHandler().player;
                player.inventory.markDirty();
                
                // 标记 Baubles 为脏
                try {
                    com.rsring.service.RingDetectionService.markBaublesDirtyIfNeeded(player);
                } catch (Throwable t) {
                    // 忽略错误
                }
                
                // 发送同步数据包回客户端
                com.rsring.rsring.RsRingMod.network.sendTo(
                    new PacketSyncCapabilityToClient(cap),
                    player
                );
            });
            return null;
        }
    }
}
