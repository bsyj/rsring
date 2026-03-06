package com.rsring.capability;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.item.ItemStack;
import com.rsring.filter.FilterMode;
import com.rsring.filter.ItemAttribute;
import com.rsring.util.Pair;
import java.util.List;

public interface IRsRingCapability {
    // 绑定RS终端位置
    void bindTerminal(World world, BlockPos pos);
    void unbindTerminal();
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

    // 密封状态：密封后无法接受外部充电
    boolean isSealed();
    void setSealed(boolean sealed);

    // 高级过滤模式
    FilterMode getFilterMode();
    void setFilterMode(FilterMode mode);

    boolean isMatchAllMode();
    void setMatchAllMode(boolean matchAll);

    List<Pair<ItemAttribute, Boolean>> getFilterAttributes();
    void addFilterAttribute(ItemAttribute attr, boolean inverted);
    void removeFilterAttribute(int index);
    void setFilterAttributeInverted(int index, boolean inverted);
    void clearFilterAttributes();

    List<String> getFilterMods();
    void addFilterMod(String modId);
    void removeFilterMod(String modId);

    // 属性过滤模式的输入槽位（独立槽位，不占用9格过滤槽）
    // 存储完整的ItemStack NBT以保留物品的所有数据（包括NBT、颜色等）
    void setAttributeInputSlotNBT(net.minecraft.nbt.NBTTagCompound nbt);
    net.minecraft.nbt.NBTTagCompound getAttributeInputSlotNBT();
    void clearAttributeInputSlot();
    
    // 获取属性输入槽位的ItemStack（便捷方法）
    ItemStack getAttributeInputStack();
    void setAttributeInputStack(ItemStack stack);

    // NBT和耐久匹配选项（用于物品过滤和模组过滤模式）
    boolean shouldMatchNbt();
    void setMatchNbt(boolean matchNbt);
    boolean shouldMatchDurability();
    void setMatchDurability(boolean matchDurability);
    
    // ==================== 销毁模式相关方法 ====================
    // 销毁模式与吸收模式完全独立，各自有自己的过滤设置
    
    // 销毁模式UI状态（临时状态，不持久化）
    boolean isDestroyModeUI();
    void setDestroyModeUI(boolean destroyMode);
    
    // 销毁功能开关
    boolean isDestroyEnabled();
    void setDestroyEnabled(boolean enabled);
    
    // 销毁过滤模式（独立于吸收过滤模式）
    FilterMode getDestroyFilterMode();
    void setDestroyFilterMode(FilterMode mode);
    
    // 销毁黑白名单模式
    boolean isDestroyWhitelistMode();
    void setDestroyWhitelistMode(boolean whitelist);
    
    // 销毁物品ID过滤槽
    void setDestroyFilterSlot(int slot, String itemRegistryName);
    String getDestroyFilterSlot(int slot);
    
    // 销毁属性过滤
    List<Pair<ItemAttribute, Boolean>> getDestroyFilterAttributes();
    void addDestroyFilterAttribute(ItemAttribute attr, boolean inverted);
    void removeDestroyFilterAttribute(int index);
    void setDestroyFilterAttributeInverted(int index, boolean inverted);
    void clearDestroyFilterAttributes();
    
    // 销毁模组过滤
    List<String> getDestroyFilterMods();
    void addDestroyFilterMod(String modId);
    void removeDestroyFilterMod(String modId);
    
    // 销毁属性输入槽
    void setDestroyAttributeInputSlotNBT(net.minecraft.nbt.NBTTagCompound nbt);
    net.minecraft.nbt.NBTTagCompound getDestroyAttributeInputSlotNBT();
    void clearDestroyAttributeInputSlot();
    ItemStack getDestroyAttributeInputStack();
    void setDestroyAttributeInputStack(ItemStack stack);
    
    // 销毁AND/OR匹配模式
    boolean isDestroyMatchAllMode();
    void setDestroyMatchAllMode(boolean matchAll);
    
    // 销毁NBT和耐久匹配
    boolean shouldDestroyMatchNbt();
    void setDestroyMatchNbt(boolean matchNbt);
    boolean shouldDestroyMatchDurability();
    void setDestroyMatchDurability(boolean matchDurability);
    
    // ==================== 垃圾箱绑定方法 ====================
    // 垃圾箱绑定位置（用于存放销毁的物品）
    void bindTrashCan(World world, BlockPos pos);
    void unbindTrashCan();
    BlockPos getTrashCanPos();
    int getTrashCanDimension();
    World getTrashCanWorld();
    boolean isTrashCanBound();
}