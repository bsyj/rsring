package com.rsring.network;

import com.rsring.capability.IExperiencePumpCapability;
import com.rsring.capability.ExperiencePumpCapability;
import com.rsring.item.ItemExperiencePump;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 服务器 -> 客户端：同步经验储罐数据（用于 GUI 刷新） */
public class PacketPumpData implements IMessage {

    private int handOrdinal;
    private int xpStored;
    private int capacityLevels;
    private int mode;
    private int retainLevel;
    private boolean useForMending;

    public PacketPumpData() {}

    public PacketPumpData(ItemStack stack) {
        handOrdinal = -1;
        xpStored = capacityLevels = mode = retainLevel = 0;
        useForMending = false;
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemExperiencePump)) return;
        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap == null) return;
        this.xpStored = cap.getXpStored();
        this.capacityLevels = cap.getCapacityLevels();
        this.mode = cap.getMode();
        this.retainLevel = cap.getRetainLevel();
        this.useForMending = cap.isUseForMending();
    }

    public PacketPumpData(EnumHand hand, ItemStack stack) {
        this(stack);
        this.handOrdinal = hand == EnumHand.MAIN_HAND ? 0 : 1;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        xpStored = buf.readInt();
        capacityLevels = buf.readInt();
        mode = buf.readByte();
        retainLevel = buf.readInt();
        useForMending = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeInt(xpStored);
        buf.writeInt(capacityLevels);
        buf.writeByte(mode);
        buf.writeInt(retainLevel);
        buf.writeBoolean(useForMending);
    }

    public int getHandOrdinal() { return handOrdinal; }
    public int getXpStored() { return xpStored; }
    public int getCapacityLevels() { return capacityLevels; }
    public int getMode() { return mode; }
    public int getRetainLevel() { return retainLevel; }
    public boolean isUseForMending() { return useForMending; }
    public int getMaxXp() {
        // 使用指数增长公式：BASE_XP_PER_LEVEL * 2^(capacityLevels-1)
        if (capacityLevels <= 0) return com.rsring.capability.IExperiencePumpCapability.BASE_XP_PER_LEVEL;
        try {
            long maxCapacity = (long) com.rsring.capability.IExperiencePumpCapability.BASE_XP_PER_LEVEL * (1L << (capacityLevels - 1));
            if (maxCapacity > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) maxCapacity;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    public static class Handler implements IMessageHandler<PacketPumpData, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketPumpData msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().currentScreen instanceof com.rsring.client.GuiExperiencePumpController) {
                    ((com.rsring.client.GuiExperiencePumpController) Minecraft.getMinecraft().currentScreen).updateFromPacket(msg);
                }
            });
            return null;
        }
    }
}
