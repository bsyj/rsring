package com.rsring.capability;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.item.ItemStack;
import java.util.List;

public interface IRsRingCapability {
    // 绑定RS终端位置
    void bindTerminal(World world, BlockPos pos);
    BlockPos getTerminalPos();
    int getTerminalDimension();
    World getTerminalWorld();
    boolean isBound();

    // 开启/关闭功能
    void setEnabled(boolean enabled);
    boolean isEnabled();

    // 能量管理
    IEnergyStorage getEnergyStorage();

    // 获取饰品实例的副本
    IRsRingCapability copy();

    // 黑白名单相关方法
    void addToBlacklist(ItemStack item);
    void removeFromBlacklist(ItemStack item);
    boolean isInBlacklist(ItemStack item);
    boolean isWhitelistMode();
    void setWhitelistMode(boolean whitelistMode);
    java.util.List<String> getBlacklistItems();

    /** 9 格过滤槽：仅读取匹配，不消耗物品。slot 0~8 */
    void setFilterSlot(int slot, String itemRegistryName);
    String getFilterSlot(int slot);
}