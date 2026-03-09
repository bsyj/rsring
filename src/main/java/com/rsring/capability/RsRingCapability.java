package com.rsring.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.DimensionManager;
import com.rsring.filter.FilterMode;
import com.rsring.filter.ItemAttribute;
import com.rsring.filter.FilterCache;
import com.rsring.util.Pair;
import com.rsring.config.RsRingConfig;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class RsRingCapability implements IRsRingCapability {

    @CapabilityInject(IRsRingCapability.class)
    public static final Capability<IRsRingCapability> RS_RING_CAPABILITY = null;

    // ==================== 脏标记机制 - NBT优化 ====================
    // 使用位标记来追踪哪些数据发生了变化，避免不必要的序列化
    public static final int DIRTY_TERMINAL = 1 << 0;      // 终端绑定
    public static final int DIRTY_ENERGY = 1 << 1;        // 能量存储
    public static final int DIRTY_ENABLED = 1 << 2;       // 启用状态
    public static final int DIRTY_FILTER_MODE = 1 << 3;   // 过滤模式
    public static final int DIRTY_FILTERS = 1 << 4;       // 过滤列表
    public static final int DIRTY_DESTROY = 1 << 5;       // 销毁模式
    public static final int DIRTY_TRASH = 1 << 6;         // 垃圾箱绑定
    public static final int DIRTY_MISC = 1 << 7;          // 其他杂项
    
    private transient int dirtyFlags = 0xFFFFFFFF; // 初始标记所有字段为脏（首次序列化需要）
    private transient boolean dirty = true;
    
    private BlockPos terminalPos;
    private int terminalDimension;
    private boolean enabled = false;

    private List<String> blacklistItems = new ArrayList<>();
    private boolean whitelistMode = !com.rsring.config.RsRingConfig.absorbRing.useBlacklistModeByDefault;
    private boolean sealed = com.rsring.config.RsRingConfig.absorbRing.blockExternalCharging;

    private FilterMode filterMode = FilterMode.ITEM;
    private boolean matchAllMode = false;
    private List<Pair<ItemAttribute, Boolean>> filterAttributes = new ArrayList<>();
    private List<String> filterMods = new ArrayList<>();
    private List<String> modFilterSlots = new ArrayList<>(); // 模组过滤槽位（独立于物品ID过滤槽）
    
    // NBT存储：用于NBT匹配
    private Map<Integer, NBTTagCompound> filterSlotNBTs = new HashMap<>(); // 物品ID过滤槽NBT
    private Map<Integer, NBTTagCompound> modFilterSlotNBTs = new HashMap<>(); // 模组过滤槽NBT

    // NBT和耐久匹配选项
    private boolean matchNbt = false;
    private boolean matchDurability = false;
    
    // ==================== 销毁模式字段 ====================
    private transient boolean destroyModeUI = false; // 销毁模式UI状态（不持久化）
    private boolean destroyEnabled = false; // 销毁功能开关
    private DestroyModeType destroyModeType = DestroyModeType.ALWAYS; // 销毁模式类型（默认总是销毁）
    private FilterMode destroyFilterMode = FilterMode.ITEM; // 销毁过滤模式
    private boolean destroyWhitelistMode = true; // 销毁黑白名单模式（默认为白名单）
    private List<String> destroyFilterSlots = new ArrayList<>(); // 销毁物品过滤槽
    private List<String> destroyModFilterSlots = new ArrayList<>(); // 销毁模组过滤槽
    private List<Pair<ItemAttribute, Boolean>> destroyFilterAttributes = new ArrayList<>(); // 销毁属性过滤列表
    private List<String> destroyFilterMods = new ArrayList<>(); // 销毁模组过滤列表
    private NBTTagCompound destroyAttributeInputSlotNBT = new NBTTagCompound(); // 销毁属性输入槽NBT
    private boolean destroyMatchAllMode = false; // 销毁AND/OR模式
    private boolean destroyMatchNbt = false; // 销毁NBT匹配
    private boolean destroyMatchDurability = false; // 销毁耐久匹配
    private boolean shouldWorkInGUI = true; // GUI内工作（默认启用）
    
    // 销毁模式NBT存储
    private Map<Integer, NBTTagCompound> destroyFilterSlotNBTs = new HashMap<>(); // 销毁物品ID过滤槽NBT
    private Map<Integer, NBTTagCompound> destroyModFilterSlotNBTs = new HashMap<>(); // 销毁模组过滤槽NBT
    
    // ==================== 垃圾箱绑定字段 ====================
    private BlockPos trashCanPos; // 垃圾箱位置
    private int trashCanDimension; // 垃圾箱所在维度
    
    // ==================== 彩蛋模式字段 ====================
    private boolean easterEgg = false; // 彩蛋戒指标记

    // ==================== 过滤缓存 - 性能优化 ====================
    private transient FilterCache absorbFilterCache = new FilterCache();
    private transient FilterCache destroyFilterCache = new FilterCache();
    private transient int absorbCacheVersion = 0;  // 缓存版本，用于检测是否需要重建
    private transient int destroyCacheVersion = 0;

    // Constructor
    public RsRingCapability() {
        loadDefaultFilterList();
        this.energyStorage = createEnergyStorage(getConfiguredInitialEnergy());
    }

    /**
     * 加载默认过滤列表
     */
    private void loadDefaultFilterList() {
        blacklistItems.clear();

        if (allowCustomFilters()) {
            return;
        }

        String[] items = com.rsring.config.RsRingConfig.absorbRing.useBlacklistModeByDefault
            ? com.rsring.config.RsRingConfig.absorbRing.defaultBlacklistItems
            : com.rsring.config.RsRingConfig.absorbRing.defaultWhitelistItems;

        for (String item : items) {
            if (item != null && !item.trim().isEmpty()) {
                String formattedItem = item.trim();
                if (!formattedItem.contains(":")) {
                    formattedItem = "minecraft:" + formattedItem;
                }
                blacklistItems.add(formattedItem);
            }
        }
    }

    private static final int DEFAULT_MAX_ENERGY = 10_000_000;
    private static final int MAX_IO = 10000;
    private EnergyStorage energyStorage;

    private static boolean allowCustomFilters() {
        return com.rsring.config.RsRingConfig.absorbRing.allowCustomFilters;
    }

    private static boolean getConfiguredWhitelistMode() {
        return !com.rsring.config.RsRingConfig.absorbRing.useBlacklistModeByDefault;
    }

    private static String[] getConfiguredFilterList(boolean whitelistMode) {
        return whitelistMode
            ? com.rsring.config.RsRingConfig.absorbRing.defaultWhitelistItems
            : com.rsring.config.RsRingConfig.absorbRing.defaultBlacklistItems;
    }

    private static String normalizeItemName(String item) {
        if (item == null) return "";
        String formatted = item.trim();
        if (formatted.isEmpty()) return "";
        if (!formatted.contains(":")) {
            formatted = "minecraft:" + formatted;
        }
        return formatted;
    }

    private static int getConfiguredMaxEnergy() {
        int configured = com.rsring.config.RsRingConfig.absorbRing.maxEnergyCapacity;
        if (configured <= 0) {
            configured = DEFAULT_MAX_ENERGY;
        }
        return configured;
    }

    private static int getConfiguredInitialEnergy() {
        int max = getConfiguredMaxEnergy();
        int configured = com.rsring.config.RsRingConfig.absorbRing.initialEnergy;
        if (configured < 0) {
            configured = 0;
        }
        if (configured > max) {
            configured = max;
        }
        return configured;
    }

    private static EnergyStorage createEnergyStorage(int stored) {
        int max = getConfiguredMaxEnergy();
        int clamped = Math.max(0, Math.min(stored, max));
        return new EnergyStorage(max, MAX_IO, MAX_IO, clamped);
    }

    private void refreshEnergyStorage() {
        int configuredMax = getConfiguredMaxEnergy();
        if (energyStorage == null) {
            energyStorage = createEnergyStorage(getConfiguredInitialEnergy());
            return;
        }
        int currentMax = energyStorage.getMaxEnergyStored();
        if (currentMax != configuredMax) {
            int stored = Math.min(energyStorage.getEnergyStored(), configuredMax);
            energyStorage = new EnergyStorage(configuredMax, MAX_IO, MAX_IO, stored);
        }
    }

    public static void refreshEnergyStorage(IRsRingCapability cap) {
        if (cap instanceof RsRingCapability) {
            ((RsRingCapability) cap).refreshEnergyStorage();
        }
    }

    // ==================== 脏标记辅助方法 ====================
    private void markDirty(int flag) {
        this.dirty = true;
        this.dirtyFlags |= flag;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void clearDirty() {
        this.dirty = false;
        this.dirtyFlags = 0;
    }
    
    public int getDirtyFlags() {
        return dirtyFlags;
    }

    // ==================== 过滤缓存管理 ====================

    /**
     * 获取吸收模式过滤缓存（自动重建）
     */
    public FilterCache getAbsorbFilterCache() {
        if (!absorbFilterCache.isValid() || absorbCacheVersion != absorbFilterCache.getVersion()) {
            rebuildAbsorbFilterCache();
        }
        return absorbFilterCache;
    }

    /**
     * 获取销毁模式过滤缓存（自动重建）
     */
    public FilterCache getDestroyFilterCache() {
        if (!destroyFilterCache.isValid() || destroyCacheVersion != destroyFilterCache.getVersion()) {
            rebuildDestroyFilterCache();
        }
        return destroyFilterCache;
    }

    /**
     * 重建吸收模式过滤缓存
     */
    private void rebuildAbsorbFilterCache() {
        List<String> itemIds = new ArrayList<>();
        List<String> modIds = new ArrayList<>();

        // 根据过滤模式收集数据
        if (filterMode == FilterMode.ITEM) {
            // 物品ID模式：使用blacklistItems（过滤槽位）
            for (int i = 0; i < 9; i++) {
                String slot = getFilterSlot(i);
                if (slot != null && !slot.isEmpty()) {
                    itemIds.add(slot);
                }
            }
        } else if (filterMode == FilterMode.MOD) {
            // 模组模式：使用modFilterSlots
            for (int i = 0; i < 9; i++) {
                String slot = getModFilterSlot(i);
                if (slot != null && !slot.isEmpty()) {
                    modIds.add(slot);
                }
            }
            // 同时添加filterMods列表
            modIds.addAll(filterMods);
        }

        absorbFilterCache.buildCache(itemIds, modIds, filterSlotNBTs);
        absorbCacheVersion = absorbFilterCache.getVersion();
    }

    /**
     * 重建销毁模式过滤缓存
     */
    private void rebuildDestroyFilterCache() {
        List<String> itemIds = new ArrayList<>();
        List<String> modIds = new ArrayList<>();

        // 根据过滤模式收集数据
        if (destroyFilterMode == FilterMode.ITEM) {
            // 物品ID模式
            for (int i = 0; i < 9; i++) {
                String slot = getDestroyFilterSlot(i);
                if (slot != null && !slot.isEmpty()) {
                    itemIds.add(slot);
                }
            }
        } else if (destroyFilterMode == FilterMode.MOD) {
            // 模组模式
            for (int i = 0; i < 9; i++) {
                String slot = getDestroyModFilterSlot(i);
                if (slot != null && !slot.isEmpty()) {
                    modIds.add(slot);
                }
            }
            modIds.addAll(destroyFilterMods);
        }

        destroyFilterCache.buildCache(itemIds, modIds, destroyFilterSlotNBTs);
        destroyCacheVersion = destroyFilterCache.getVersion();
    }

    /**
     * 使吸收模式缓存失效（过滤列表变化时调用）
     */
    public void invalidateAbsorbCache() {
        absorbFilterCache.invalidate();
        absorbCacheVersion = 0;
    }

    /**
     * 使销毁模式缓存失效
     */
    public void invalidateDestroyCache() {
        destroyFilterCache.invalidate();
        destroyCacheVersion = 0;
    }

    /**
     * 使所有缓存失效
     */
    public void invalidateAllCaches() {
        invalidateAbsorbCache();
        invalidateDestroyCache();
    }

    @Override
    public void bindTerminal(World world, BlockPos pos) {
        this.terminalPos = pos;
        this.terminalDimension = world.provider.getDimension();
        markDirty(DIRTY_TERMINAL);
    }

    @Override
    public void unbindTerminal() {
        this.terminalPos = null;
        this.terminalDimension = 0;
        markDirty(DIRTY_TERMINAL);
    }

    @Override
    public BlockPos getTerminalPos() {
        return this.terminalPos;
    }

    @Override
    public int getTerminalDimension() {
        return this.terminalDimension;
    }

    @Override
    public World getTerminalWorld() {
        return net.minecraftforge.common.DimensionManager.getWorld(this.terminalDimension);
    }

    @Override
    public boolean isBound() {
        return this.terminalPos != null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            markDirty(DIRTY_ENABLED);
        }
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    @Override
    public IRsRingCapability copy() {
        RsRingCapability copy = new RsRingCapability();
        copy.terminalPos = this.terminalPos;
        copy.terminalDimension = this.terminalDimension;
        copy.enabled = this.enabled;
        copy.blacklistItems = new ArrayList<>(this.blacklistItems);
        copy.whitelistMode = this.whitelistMode;
        copy.sealed = this.sealed;
        copy.energyStorage = createEnergyStorage(this.energyStorage.getEnergyStored());
        return copy;
    }

    @Override
    public void addToBlacklist(ItemStack item) {
        if (!allowCustomFilters()) return;
        if (!item.isEmpty()) {
            String itemName = item.getItem().getRegistryName().toString();
            if (!blacklistItems.contains(itemName)) {
                blacklistItems.add(itemName);
                invalidateAbsorbCache(); // 使缓存失效
            }
        }
    }

    @Override
    public void removeFromBlacklist(ItemStack item) {
        if (!allowCustomFilters()) return;
        if (!item.isEmpty()) {
            String itemName = item.getItem().getRegistryName().toString();
            if (blacklistItems.remove(itemName)) {
                invalidateAbsorbCache(); // 使缓存失效
            }
        }
    }

    @Override
    public boolean isInBlacklist(ItemStack item) {
        if (item.isEmpty()) return false;
        String itemName = item.getItem().getRegistryName().toString();
        for (String s : blacklistItems) {
            if (s != null && !s.isEmpty() && s.equals(itemName)) return true;
        }
        return false;
    }

    @Override
    public boolean isWhitelistMode() {
        if (!allowCustomFilters()) {
            return getConfiguredWhitelistMode();
        }
        return whitelistMode;
    }

    @Override
    public void setWhitelistMode(boolean whitelistMode) {
        if (!allowCustomFilters()) return;
        this.whitelistMode = whitelistMode;
    }

    @Override
    public List<String> getBlacklistItems() {
        if (!allowCustomFilters()) {
            boolean whitelistMode = getConfiguredWhitelistMode();
            String[] configItems = getConfiguredFilterList(whitelistMode);
            List<String> result = new ArrayList<>();
            if (configItems != null) {
                for (String item : configItems) {
                    String normalized = normalizeItemName(item);
                    if (!normalized.isEmpty()) {
                        result.add(normalized);
                    }
                }
            }
            return result;
        }
        return new ArrayList<>(blacklistItems);
    }

    @Override
    public void setFilterSlot(int slot, String itemRegistryName) {
        if (!allowCustomFilters()) return;
        if (slot < 0 || slot > 8) return;
        while (blacklistItems.size() <= slot) blacklistItems.add("");
        String newValue = itemRegistryName == null || itemRegistryName.isEmpty() ? "" : itemRegistryName;
        if (!blacklistItems.get(slot).equals(newValue)) {
            blacklistItems.set(slot, newValue);
            markDirty(DIRTY_FILTERS);
            invalidateAbsorbCache(); // 使吸收模式缓存失效
        }
    }

    @Override
    public String getFilterSlot(int slot) {
        if (!allowCustomFilters()) {
            if (slot < 0 || slot > 8) return "";
            boolean whitelistMode = getConfiguredWhitelistMode();
            String[] configItems = getConfiguredFilterList(whitelistMode);
            if (configItems == null || slot >= configItems.length) return "";
            return normalizeItemName(configItems[slot]);
        }
        if (slot < 0 || slot > 8) return "";
        if (slot >= blacklistItems.size()) return "";
        String s = blacklistItems.get(slot);
        return s == null ? "" : s;
    }

    @Override
    public void setModFilterSlot(int slot, String modId) {
        if (!allowCustomFilters()) return;
        if (slot < 0 || slot > 8) return;
        while (modFilterSlots.size() <= slot) modFilterSlots.add("");
        String newValue = modId == null || modId.isEmpty() ? "" : modId;
        if (!modFilterSlots.get(slot).equals(newValue)) {
            modFilterSlots.set(slot, newValue);
            markDirty(DIRTY_FILTERS);
            invalidateAbsorbCache(); // 使吸收模式缓存失效
        }
    }

    @Override
    public String getModFilterSlot(int slot) {
        if (!allowCustomFilters()) return "";
        if (slot < 0 || slot > 8) return "";
        if (slot >= modFilterSlots.size()) return "";
        String s = modFilterSlots.get(slot);
        return s == null ? "" : s;
    }

    @Override
    public void setFilterSlotNBT(int slot, NBTTagCompound nbt) {
        if (slot < 0 || slot > 8) return;
        if (nbt == null) {
            if (filterSlotNBTs.remove(slot) != null) {
                markDirty(DIRTY_FILTERS);
                invalidateAbsorbCache(); // 使吸收模式缓存失效
            }
        } else {
            NBTTagCompound old = filterSlotNBTs.put(slot, nbt);
            if (old == null || !old.equals(nbt)) {
                markDirty(DIRTY_FILTERS);
                invalidateAbsorbCache(); // 使吸收模式缓存失效
            }
        }
    }

    @Override
    public NBTTagCompound getFilterSlotNBT(int slot) {
        if (slot < 0 || slot > 8) return null;
        return filterSlotNBTs.get(slot);
    }

    @Override
    public void setModFilterSlotNBT(int slot, NBTTagCompound nbt) {
        if (slot < 0 || slot > 8) return;
        if (nbt == null) {
            if (modFilterSlotNBTs.remove(slot) != null) {
                markDirty(DIRTY_FILTERS);
            }
        } else {
            NBTTagCompound old = modFilterSlotNBTs.put(slot, nbt);
            if (old == null || !old.equals(nbt)) {
                markDirty(DIRTY_FILTERS);
            }
        }
    }

    @Override
    public NBTTagCompound getModFilterSlotNBT(int slot) {
        if (slot < 0 || slot > 8) return null;
        return modFilterSlotNBTs.get(slot);
    }

    @Override
    public boolean isSealed() {
        return sealed;
    }

    @Override
    public void setSealed(boolean sealed) {
        if (this.sealed != sealed) {
            this.sealed = sealed;
            markDirty(DIRTY_MISC);
        }
    }

    @Override
    public FilterMode getFilterMode() {
        return this.filterMode;
    }

    @Override
    public void setFilterMode(FilterMode mode) {
        if (this.filterMode != mode) {
            this.filterMode = mode;
            markDirty(DIRTY_FILTER_MODE);
        }
    }

    @Override
    public boolean isMatchAllMode() {
        return this.matchAllMode;
    }

    @Override
    public void setMatchAllMode(boolean matchAll) {
        if (this.matchAllMode != matchAll) {
            this.matchAllMode = matchAll;
            markDirty(DIRTY_FILTER_MODE);
        }
    }

    @Override
    public List<Pair<ItemAttribute, Boolean>> getFilterAttributes() {
        return this.filterAttributes;
    }

    @Override
    public void addFilterAttribute(ItemAttribute attr, boolean inverted) {
        if (!allowCustomFilters()) return;
        if (attr != null) {
            // 检查是否已存在完全相同的属性（类型和参数都相同）
            for (int i = 0; i < this.filterAttributes.size(); i++) {
                Pair<ItemAttribute, Boolean> existing = this.filterAttributes.get(i);
                if (isSameAttribute(existing.getKey(), attr) && existing.getValue() == inverted) {
                    // 已存在完全相同的属性，不重复添加
                    return;
                }
            }
            // 添加新属性
            this.filterAttributes.add(Pair.of(attr, inverted));
        }
    }
    
    /**
     * 检查两个属性是否相同（类型和参数都相同）
     */
    private boolean isSameAttribute(ItemAttribute a1, ItemAttribute a2) {
        if (!a1.getTranslationKey().equals(a2.getTranslationKey())) {
            return false;
        }
        Object[] params1 = a1.getTranslationParameters();
        Object[] params2 = a2.getTranslationParameters();
        if (params1.length != params2.length) {
            return false;
        }
        for (int i = 0; i < params1.length; i++) {
            if (params1[i] == null && params2[i] == null) continue;
            if (params1[i] == null || params2[i] == null) return false;
            if (!params1[i].equals(params2[i])) return false;
        }
        return true;
    }
    
    @Override
    public void removeFilterAttribute(int index) {
        if (!allowCustomFilters()) return;
        if (index >= 0 && index < this.filterAttributes.size()) {
            this.filterAttributes.remove(index);
        }
    }
    
    @Override
    public void setFilterAttributeInverted(int index, boolean inverted) {
        if (!allowCustomFilters()) return;
        if (index >= 0 && index < this.filterAttributes.size()) {
            Pair<ItemAttribute, Boolean> current = this.filterAttributes.get(index);
            // 在原位置更新反转状态
            this.filterAttributes.set(index, Pair.of(current.getKey(), inverted));
        }
    }
    
    @Override
    public void clearFilterAttributes() {
        this.filterAttributes.clear();
    }

    @Override
    public List<String> getFilterMods() {
        return new ArrayList<>(this.filterMods);
    }

    @Override
    public void addFilterMod(String modId) {
        if (!allowCustomFilters()) return;
        if (modId != null && !this.filterMods.contains(modId)) {
            this.filterMods.add(modId);
            markDirty(DIRTY_FILTERS);
        }
    }

    @Override
    public void removeFilterMod(String modId) {
        if (!allowCustomFilters()) return;
        if (this.filterMods.remove(modId)) {
            markDirty(DIRTY_FILTERS);
        }
    }

    // 属性过滤模式的输入槽位（存储完整ItemStack NBT）
    private NBTTagCompound attributeInputSlotNBT = new NBTTagCompound();

    @Override
    public void setAttributeInputSlotNBT(NBTTagCompound nbt) {
        if (!allowCustomFilters()) return;
        NBTTagCompound newNbt = nbt != null ? nbt : new NBTTagCompound();
        if (!newNbt.equals(this.attributeInputSlotNBT)) {
            this.attributeInputSlotNBT = newNbt;
            markDirty(DIRTY_FILTERS);
        }
    }

    @Override
    public NBTTagCompound getAttributeInputSlotNBT() {
        if (!allowCustomFilters()) return new NBTTagCompound();
        return this.attributeInputSlotNBT;
    }

    @Override
    public void clearAttributeInputSlot() {
        this.attributeInputSlotNBT = new NBTTagCompound();
    }

    @Override
    public ItemStack getAttributeInputStack() {
        if (attributeInputSlotNBT == null || attributeInputSlotNBT.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(attributeInputSlotNBT);
    }

    @Override
    public void setAttributeInputStack(ItemStack stack) {
        if (!allowCustomFilters()) return;
        if (stack == null || stack.isEmpty()) {
            if (!this.attributeInputSlotNBT.isEmpty()) {
                clearAttributeInputSlot();
                markDirty(DIRTY_FILTERS);
            }
        } else {
            // 保存物品的完整NBT数据（数量设为1）
            ItemStack copy = stack.copy();
            copy.setCount(1);
            NBTTagCompound newNbt = copy.writeToNBT(new NBTTagCompound());
            if (!newNbt.equals(this.attributeInputSlotNBT)) {
                this.attributeInputSlotNBT = newNbt;
                markDirty(DIRTY_FILTERS);
            }
        }
    }

    // NBT和耐久匹配方法实现
    @Override
    public boolean shouldMatchNbt() {
        return this.matchNbt;
    }

    @Override
    public void setMatchNbt(boolean matchNbt) {
        if (this.matchNbt != matchNbt) {
            this.matchNbt = matchNbt;
            markDirty(DIRTY_FILTER_MODE);
        }
    }

    @Override
    public boolean shouldMatchDurability() {
        return this.matchDurability;
    }

    @Override
    public void setMatchDurability(boolean matchDurability) {
        if (this.matchDurability != matchDurability) {
            this.matchDurability = matchDurability;
            markDirty(DIRTY_FILTER_MODE);
        }
    }
    
    // ==================== 销毁模式方法实现 ====================
    
    @Override
    public boolean isDestroyModeUI() {
        return this.destroyModeUI;
    }
    
    @Override
    public void setDestroyModeUI(boolean destroyMode) {
        this.destroyModeUI = destroyMode;
    }
    
    @Override
    public boolean isDestroyEnabled() {
        return this.destroyEnabled;
    }
    
    @Override
    public void setDestroyEnabled(boolean enabled) {
        if (this.destroyEnabled != enabled) {
            this.destroyEnabled = enabled;
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public DestroyModeType getDestroyModeType() {
        return this.destroyModeType;
    }

    @Override
    public void setDestroyModeType(DestroyModeType type) {
        if (this.destroyModeType != type) {
            this.destroyModeType = type;
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public FilterMode getDestroyFilterMode() {
        return this.destroyFilterMode;
    }

    @Override
    public void setDestroyFilterMode(FilterMode mode) {
        if (this.destroyFilterMode != mode) {
            this.destroyFilterMode = mode;
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public boolean isDestroyWhitelistMode() {
        return this.destroyWhitelistMode;
    }

    @Override
    public void setDestroyWhitelistMode(boolean whitelist) {
        if (this.destroyWhitelistMode != whitelist) {
            this.destroyWhitelistMode = whitelist;
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public void setDestroyFilterSlot(int slot, String itemRegistryName) {
        if (!allowCustomFilters()) return;
        if (slot < 0 || slot > 8) return;
        while (destroyFilterSlots.size() <= slot) destroyFilterSlots.add("");
        String newValue = itemRegistryName == null || itemRegistryName.isEmpty() ? "" : itemRegistryName;
        if (!destroyFilterSlots.get(slot).equals(newValue)) {
            destroyFilterSlots.set(slot, newValue);
            markDirty(DIRTY_DESTROY);
            invalidateDestroyCache(); // 使销毁模式缓存失效
        }
    }

    @Override
    public String getDestroyFilterSlot(int slot) {
        if (!allowCustomFilters()) return "";
        if (slot < 0 || slot > 8) return "";
        if (slot >= destroyFilterSlots.size()) return "";
        String s = destroyFilterSlots.get(slot);
        return s == null ? "" : s;
    }

    @Override
    public void setDestroyModFilterSlot(int slot, String modId) {
        if (!allowCustomFilters()) return;
        if (slot < 0 || slot > 8) return;
        while (destroyModFilterSlots.size() <= slot) destroyModFilterSlots.add("");
        String newValue = modId == null || modId.isEmpty() ? "" : modId;
        if (!destroyModFilterSlots.get(slot).equals(newValue)) {
            destroyModFilterSlots.set(slot, newValue);
            markDirty(DIRTY_DESTROY);
            invalidateDestroyCache(); // 使销毁模式缓存失效
        }
    }

    @Override
    public String getDestroyModFilterSlot(int slot) {
        if (!allowCustomFilters()) return "";
        if (slot < 0 || slot > 8) return "";
        if (slot >= destroyModFilterSlots.size()) return "";
        String s = destroyModFilterSlots.get(slot);
        return s == null ? "" : s;
    }

    @Override
    public void setDestroyFilterSlotNBT(int slot, NBTTagCompound nbt) {
        if (slot < 0 || slot > 8) return;
        if (nbt == null) {
            if (destroyFilterSlotNBTs.remove(slot) != null) {
                markDirty(DIRTY_DESTROY);
                invalidateDestroyCache(); // 使销毁模式缓存失效
            }
        } else {
            NBTTagCompound old = destroyFilterSlotNBTs.put(slot, nbt);
            if (old == null || !old.equals(nbt)) {
                markDirty(DIRTY_DESTROY);
                invalidateDestroyCache(); // 使销毁模式缓存失效
            }
        }
    }

    @Override
    public NBTTagCompound getDestroyFilterSlotNBT(int slot) {
        if (slot < 0 || slot > 8) return null;
        return destroyFilterSlotNBTs.get(slot);
    }

    @Override
    public void setDestroyModFilterSlotNBT(int slot, NBTTagCompound nbt) {
        if (slot < 0 || slot > 8) return;
        if (nbt == null) {
            if (destroyModFilterSlotNBTs.remove(slot) != null) {
                markDirty(DIRTY_DESTROY);
                invalidateDestroyCache(); // 使销毁模式缓存失效
            }
        } else {
            NBTTagCompound old = destroyModFilterSlotNBTs.put(slot, nbt);
            if (old == null || !old.equals(nbt)) {
                markDirty(DIRTY_DESTROY);
                invalidateDestroyCache(); // 使销毁模式缓存失效
                markDirty(DIRTY_DESTROY);
            }
        }
    }

    @Override
    public NBTTagCompound getDestroyModFilterSlotNBT(int slot) {
        if (slot < 0 || slot > 8) return null;
        return destroyModFilterSlotNBTs.get(slot);
    }
    
    @Override
    public List<Pair<ItemAttribute, Boolean>> getDestroyFilterAttributes() {
        return this.destroyFilterAttributes;
    }
    
    @Override
    public void addDestroyFilterAttribute(ItemAttribute attr, boolean inverted) {
        if (!allowCustomFilters()) return;
        if (attr != null) {
            for (int i = 0; i < this.destroyFilterAttributes.size(); i++) {
                Pair<ItemAttribute, Boolean> existing = this.destroyFilterAttributes.get(i);
                if (isSameAttribute(existing.getKey(), attr) && existing.getValue() == inverted) {
                    return;
                }
            }
            this.destroyFilterAttributes.add(Pair.of(attr, inverted));
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public void removeDestroyFilterAttribute(int index) {
        if (!allowCustomFilters()) return;
        if (index >= 0 && index < this.destroyFilterAttributes.size()) {
            this.destroyFilterAttributes.remove(index);
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public void setDestroyFilterAttributeInverted(int index, boolean inverted) {
        if (!allowCustomFilters()) return;
        if (index >= 0 && index < this.destroyFilterAttributes.size()) {
            Pair<ItemAttribute, Boolean> current = this.destroyFilterAttributes.get(index);
            if (current.getValue() != inverted) {
                this.destroyFilterAttributes.set(index, Pair.of(current.getKey(), inverted));
                markDirty(DIRTY_DESTROY);
            }
        }
    }

    @Override
    public void clearDestroyFilterAttributes() {
        if (!this.destroyFilterAttributes.isEmpty()) {
            this.destroyFilterAttributes.clear();
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public List<String> getDestroyFilterMods() {
        return new ArrayList<>(this.destroyFilterMods);
    }

    @Override
    public void addDestroyFilterMod(String modId) {
        if (!allowCustomFilters()) return;
        if (modId != null && !this.destroyFilterMods.contains(modId)) {
            this.destroyFilterMods.add(modId);
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public void removeDestroyFilterMod(String modId) {
        if (!allowCustomFilters()) return;
        if (this.destroyFilterMods.remove(modId)) {
            markDirty(DIRTY_DESTROY);
        }
    }
    
    @Override
    public void setDestroyAttributeInputSlotNBT(NBTTagCompound nbt) {
        if (!allowCustomFilters()) return;
        NBTTagCompound newNbt = nbt != null ? nbt : new NBTTagCompound();
        if (!newNbt.equals(this.destroyAttributeInputSlotNBT)) {
            this.destroyAttributeInputSlotNBT = newNbt;
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public NBTTagCompound getDestroyAttributeInputSlotNBT() {
        if (!allowCustomFilters()) return new NBTTagCompound();
        return this.destroyAttributeInputSlotNBT;
    }

    @Override
    public void clearDestroyAttributeInputSlot() {
        if (!this.destroyAttributeInputSlotNBT.isEmpty()) {
            this.destroyAttributeInputSlotNBT = new NBTTagCompound();
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public ItemStack getDestroyAttributeInputStack() {
        if (destroyAttributeInputSlotNBT == null || destroyAttributeInputSlotNBT.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(destroyAttributeInputSlotNBT);
    }

    @Override
    public void setDestroyAttributeInputStack(ItemStack stack) {
        if (!allowCustomFilters()) return;
        if (stack == null || stack.isEmpty()) {
            if (!this.destroyAttributeInputSlotNBT.isEmpty()) {
                clearDestroyAttributeInputSlot();
            }
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            NBTTagCompound newNbt = copy.writeToNBT(new NBTTagCompound());
            if (!newNbt.equals(this.destroyAttributeInputSlotNBT)) {
                this.destroyAttributeInputSlotNBT = newNbt;
                markDirty(DIRTY_DESTROY);
            }
        }
    }

    @Override
    public boolean isDestroyMatchAllMode() {
        return this.destroyMatchAllMode;
    }

    @Override
    public void setDestroyMatchAllMode(boolean matchAll) {
        if (this.destroyMatchAllMode != matchAll) {
            this.destroyMatchAllMode = matchAll;
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public boolean shouldDestroyMatchNbt() {
        return this.destroyMatchNbt;
    }

    @Override
    public void setDestroyMatchNbt(boolean matchNbt) {
        if (this.destroyMatchNbt != matchNbt) {
            this.destroyMatchNbt = matchNbt;
            markDirty(DIRTY_DESTROY);
        }
    }
    
    @Override
    public boolean shouldDestroyMatchDurability() {
        return this.destroyMatchDurability;
    }
    
    @Override
    public void setDestroyMatchDurability(boolean matchDurability) {
        if (this.destroyMatchDurability != matchDurability) {
            this.destroyMatchDurability = matchDurability;
            markDirty(DIRTY_DESTROY);
        }
    }

    @Override
    public boolean shouldWorkInGUI() {
        return this.shouldWorkInGUI;
    }

    @Override
    public void setShouldWorkInGUI(boolean shouldWork) {
        if (this.shouldWorkInGUI != shouldWork) {
            this.shouldWorkInGUI = shouldWork;
            markDirty(DIRTY_MISC);
        }
    }

    // ==================== 垃圾箱绑定方法实现 ====================
    @Override
    public void bindTrashCan(World world, BlockPos pos) {
        this.trashCanPos = pos;
        this.trashCanDimension = world.provider.getDimension();
        markDirty(DIRTY_TRASH);
    }

    @Override
    public void unbindTrashCan() {
        this.trashCanPos = null;
        this.trashCanDimension = 0;
        markDirty(DIRTY_TRASH);
    }
    
    @Override
    public BlockPos getTrashCanPos() {
        return this.trashCanPos;
    }
    
    @Override
    public int getTrashCanDimension() {
        return this.trashCanDimension;
    }
    
    @Override
    public World getTrashCanWorld() {
        return net.minecraftforge.common.DimensionManager.getWorld(this.trashCanDimension);
    }
    
    @Override
    public boolean isTrashCanBound() {
        return this.trashCanPos != null;
    }
    
    // ==================== 彩蛋模式实现 ====================
    @Override
    public boolean isEasterEgg() {
        return this.easterEgg;
    }
    
    @Override
    public void setEasterEgg(boolean easterEgg) {
        if (this.easterEgg != easterEgg) {
            this.easterEgg = easterEgg;
            markDirty(DIRTY_MISC);
        }
    }

    // ==================== 布尔值打包方法 - NBT优化 ====================
    // 位标记定义
    private static final int FLAG_ENABLED = 1 << 0;
    private static final int FLAG_WHITELIST_MODE = 1 << 1;
    private static final int FLAG_SEALED = 1 << 2;
    private static final int FLAG_MATCH_ALL = 1 << 3;
    private static final int FLAG_MATCH_NBT = 1 << 4;
    private static final int FLAG_MATCH_DURABILITY = 1 << 5;
    private static final int FLAG_EASTER_EGG = 1 << 6;
    
    // 销毁模式位标记
    private static final int FLAG_DESTROY_ENABLED = 1 << 0;
    private static final int FLAG_DESTROY_WHITELIST = 1 << 1;
    private static final int FLAG_DESTROY_MATCH_ALL = 1 << 2;
    private static final int FLAG_DESTROY_MATCH_NBT = 1 << 3;
    private static final int FLAG_DESTROY_MATCH_DURABILITY = 1 << 4;
    private static final int FLAG_SHOULD_WORK_IN_GUI = 1 << 5;
    
    /**
     * 打包主要布尔值到单个字节
     */
    private static byte packBooleanFlags(RsRingCapability cap) {
        byte flags = 0;
        if (cap.enabled) flags |= FLAG_ENABLED;
        if (cap.whitelistMode) flags |= FLAG_WHITELIST_MODE;
        if (cap.sealed) flags |= FLAG_SEALED;
        if (cap.matchAllMode) flags |= FLAG_MATCH_ALL;
        if (cap.matchNbt) flags |= FLAG_MATCH_NBT;
        if (cap.matchDurability) flags |= FLAG_MATCH_DURABILITY;
        if (cap.easterEgg) flags |= FLAG_EASTER_EGG;
        return flags;
    }
    
    /**
     * 从字节解包主要布尔值
     */
    private static void unpackBooleanFlags(RsRingCapability cap, byte flags) {
        cap.enabled = (flags & FLAG_ENABLED) != 0;
        cap.whitelistMode = (flags & FLAG_WHITELIST_MODE) != 0;
        cap.sealed = (flags & FLAG_SEALED) != 0;
        cap.matchAllMode = (flags & FLAG_MATCH_ALL) != 0;
        cap.matchNbt = (flags & FLAG_MATCH_NBT) != 0;
        cap.matchDurability = (flags & FLAG_MATCH_DURABILITY) != 0;
        cap.easterEgg = (flags & FLAG_EASTER_EGG) != 0;
    }
    
    /**
     * 打包匹配相关布尔值
     */
    private static byte packMatchFlags(RsRingCapability cap) {
        byte flags = 0;
        if (cap.matchNbt) flags |= FLAG_MATCH_NBT;
        if (cap.matchDurability) flags |= FLAG_MATCH_DURABILITY;
        return flags;
    }
    
    /**
     * 解包匹配相关布尔值
     */
    private static void unpackMatchFlags(RsRingCapability cap, byte flags) {
        cap.matchNbt = (flags & FLAG_MATCH_NBT) != 0;
        cap.matchDurability = (flags & FLAG_MATCH_DURABILITY) != 0;
    }
    
    /**
     * 打包销毁模式布尔值
     */
    private static byte packDestroyFlags(RsRingCapability cap) {
        byte flags = 0;
        if (cap.destroyEnabled) flags |= FLAG_DESTROY_ENABLED;
        if (cap.destroyWhitelistMode) flags |= FLAG_DESTROY_WHITELIST;
        if (cap.destroyMatchAllMode) flags |= FLAG_DESTROY_MATCH_ALL;
        if (cap.destroyMatchNbt) flags |= FLAG_DESTROY_MATCH_NBT;
        if (cap.destroyMatchDurability) flags |= FLAG_DESTROY_MATCH_DURABILITY;
        if (cap.shouldWorkInGUI) flags |= FLAG_SHOULD_WORK_IN_GUI;
        return flags;
    }
    
    /**
     * 解包销毁模式布尔值
     */
    private static void unpackDestroyFlags(RsRingCapability cap, byte flags) {
        cap.destroyEnabled = (flags & FLAG_DESTROY_ENABLED) != 0;
        cap.destroyWhitelistMode = (flags & FLAG_DESTROY_WHITELIST) != 0;
        cap.destroyMatchAllMode = (flags & FLAG_DESTROY_MATCH_ALL) != 0;
        cap.destroyMatchNbt = (flags & FLAG_DESTROY_MATCH_NBT) != 0;
        cap.destroyMatchDurability = (flags & FLAG_DESTROY_MATCH_DURABILITY) != 0;
        cap.shouldWorkInGUI = (flags & FLAG_SHOULD_WORK_IN_GUI) != 0;
    }

    public static class RsRingStorage implements Capability.IStorage<IRsRingCapability> {
        @Override
        public NBTBase writeNBT(Capability<IRsRingCapability> capability, IRsRingCapability instance, EnumFacing side) {
            RsRingCapability cap = (RsRingCapability) instance;
            NBTTagCompound tag = new NBTTagCompound();

            if (cap.terminalPos != null) {
                tag.setInteger("x", cap.terminalPos.getX());
                tag.setInteger("y", cap.terminalPos.getY());
                tag.setInteger("z", cap.terminalPos.getZ());
                tag.setInteger("dimension", cap.terminalDimension);
            }

            // 使用字节打包多个布尔值，减少NBT大小
            byte boolFlags = packBooleanFlags(cap);
            tag.setByte("boolFlags", boolFlags);
            
            tag.setInteger("energy", cap.energyStorage.getEnergyStored());
            
            // 保存高级过滤数据
            tag.setString("filterMode", cap.filterMode.getName());
            
            net.minecraft.nbt.NBTTagList blacklistList = new net.minecraft.nbt.NBTTagList();
            for (String item : cap.blacklistItems) {
                blacklistList.appendTag(new net.minecraft.nbt.NBTTagString(item));
            }
            tag.setTag("blacklistItems", blacklistList);
            
            // 保存模组过滤列表
            net.minecraft.nbt.NBTTagList modList = new net.minecraft.nbt.NBTTagList();
            for (String modId : cap.filterMods) {
                modList.appendTag(new net.minecraft.nbt.NBTTagString(modId));
            }
            tag.setTag("filterMods", modList);
            
            // 保存模组过滤槽位
            net.minecraft.nbt.NBTTagList modSlotList = new net.minecraft.nbt.NBTTagList();
            for (String modId : cap.modFilterSlots) {
                modSlotList.appendTag(new net.minecraft.nbt.NBTTagString(modId));
            }
            tag.setTag("modFilterSlots", modSlotList);
            
            // 保存物品ID过滤槽的NBT数据
            NBTTagCompound filterSlotNBTsTag = new NBTTagCompound();
            for (Map.Entry<Integer, NBTTagCompound> entry : cap.filterSlotNBTs.entrySet()) {
                filterSlotNBTsTag.setTag(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.setTag("filterSlotNBTs", filterSlotNBTsTag);
            
            // 保存模组过滤槽的NBT数据
            NBTTagCompound modFilterSlotNBTsTag = new NBTTagCompound();
            for (Map.Entry<Integer, NBTTagCompound> entry : cap.modFilterSlotNBTs.entrySet()) {
                modFilterSlotNBTsTag.setTag(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.setTag("modFilterSlotNBTs", modFilterSlotNBTsTag);
            
            // 保存属性过滤列表 - 使用serializeNBT保存完整属性信息
            net.minecraft.nbt.NBTTagList attrList = new net.minecraft.nbt.NBTTagList();
            for (Pair<ItemAttribute, Boolean> pair : cap.filterAttributes) {
                NBTTagCompound attrTag = new NBTTagCompound();
                // 使用serializeNBT保存属性类型和参数
                pair.getKey().serializeNBT(attrTag);
                attrTag.setBoolean("inverted", pair.getValue());
                attrList.appendTag(attrTag);
            }
            tag.setTag("filterAttributes", attrList);

            // 保存属性输入槽位的完整NBT
            if (cap.attributeInputSlotNBT != null && !cap.attributeInputSlotNBT.isEmpty()) {
                tag.setTag("attributeInputSlotNBT", cap.attributeInputSlotNBT);
            }

            // 保存NBT和耐久匹配选项 - 使用字节打包
            byte matchFlags = packMatchFlags(cap);
            tag.setByte("matchFlags", matchFlags);
            
            // ==================== 保存销毁模式数据 ====================
            tag.setString("destroyModeType", cap.destroyModeType.getName());
            tag.setString("destroyFilterMode", cap.destroyFilterMode.getName());
            
            // 销毁模式布尔值打包
            byte destroyFlags = packDestroyFlags(cap);
            tag.setByte("destroyFlags", destroyFlags);
            
            // 保存销毁物品过滤槽
            net.minecraft.nbt.NBTTagList destroySlotList = new net.minecraft.nbt.NBTTagList();
            for (String item : cap.destroyFilterSlots) {
                destroySlotList.appendTag(new net.minecraft.nbt.NBTTagString(item));
            }
            tag.setTag("destroyFilterSlots", destroySlotList);
            
            // 保存销毁模组过滤槽位
            net.minecraft.nbt.NBTTagList destroyModSlotList = new net.minecraft.nbt.NBTTagList();
            for (String modId : cap.destroyModFilterSlots) {
                destroyModSlotList.appendTag(new net.minecraft.nbt.NBTTagString(modId));
            }
            tag.setTag("destroyModFilterSlots", destroyModSlotList);
            
            // 保存销毁模组过滤列表
            net.minecraft.nbt.NBTTagList destroyModList = new net.minecraft.nbt.NBTTagList();
            for (String modId : cap.destroyFilterMods) {
                destroyModList.appendTag(new net.minecraft.nbt.NBTTagString(modId));
            }
            tag.setTag("destroyFilterMods", destroyModList);
            
            // 保存销毁属性过滤列表
            net.minecraft.nbt.NBTTagList destroyAttrList = new net.minecraft.nbt.NBTTagList();
            for (Pair<ItemAttribute, Boolean> pair : cap.destroyFilterAttributes) {
                NBTTagCompound attrTag = new NBTTagCompound();
                pair.getKey().serializeNBT(attrTag);
                attrTag.setBoolean("inverted", pair.getValue());
                destroyAttrList.appendTag(attrTag);
            }
            tag.setTag("destroyFilterAttributes", destroyAttrList);
            
            // 保存销毁属性输入槽
            if (cap.destroyAttributeInputSlotNBT != null && !cap.destroyAttributeInputSlotNBT.isEmpty()) {
                tag.setTag("destroyAttributeInputSlotNBT", cap.destroyAttributeInputSlotNBT);
            }
            
            // 保存销毁物品ID过滤槽的NBT数据
            NBTTagCompound destroyFilterSlotNBTsTag = new NBTTagCompound();
            for (Map.Entry<Integer, NBTTagCompound> entry : cap.destroyFilterSlotNBTs.entrySet()) {
                destroyFilterSlotNBTsTag.setTag(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.setTag("destroyFilterSlotNBTs", destroyFilterSlotNBTsTag);
            
            // 保存销毁模组过滤槽的NBT数据
            NBTTagCompound destroyModFilterSlotNBTsTag = new NBTTagCompound();
            for (Map.Entry<Integer, NBTTagCompound> entry : cap.destroyModFilterSlotNBTs.entrySet()) {
                destroyModFilterSlotNBTsTag.setTag(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.setTag("destroyModFilterSlotNBTs", destroyModFilterSlotNBTsTag);
            
            // ==================== 保存垃圾箱绑定数据 ====================
            if (cap.trashCanPos != null) {
                tag.setInteger("trashCanX", cap.trashCanPos.getX());
                tag.setInteger("trashCanY", cap.trashCanPos.getY());
                tag.setInteger("trashCanZ", cap.trashCanPos.getZ());
                tag.setInteger("trashCanDimension", cap.trashCanDimension);
            }
            
            // ==================== 保存彩蛋模式 ====================
            // 彩蛋模式已包含在boolFlags中

            return tag;
        }

        @Override
        public void readNBT(Capability<IRsRingCapability> capability, IRsRingCapability instance, EnumFacing side, NBTBase nbt) {
            RsRingCapability cap = (RsRingCapability) instance;
            NBTTagCompound tag = (NBTTagCompound) nbt;

            if (tag.hasKey("x") && tag.hasKey("y") && tag.hasKey("z")) {
                cap.terminalPos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
                cap.terminalDimension = tag.getInteger("dimension");
            }

            // 读取打包的布尔值（新版本）或单独字段（向后兼容）
            if (tag.hasKey("boolFlags")) {
                unpackBooleanFlags(cap, tag.getByte("boolFlags"));
            } else {
                // 向后兼容：读取旧格式的单独字段
                cap.enabled = tag.getBoolean("enabled");
                if (tag.hasKey("whitelistMode")) {
                    cap.whitelistMode = tag.getBoolean("whitelistMode");
                } else {
                    cap.whitelistMode = getConfiguredWhitelistMode();
                }
                cap.sealed = tag.getBoolean("sealed");
                cap.matchAllMode = tag.getBoolean("matchAllMode");
                cap.easterEgg = tag.getBoolean("easterEgg");
            }
            
            int maxEnergy = getConfiguredMaxEnergy();
            int energy = Math.min(tag.getInteger("energy"), maxEnergy);
            cap.energyStorage = new EnergyStorage(maxEnergy, MAX_IO, MAX_IO, energy);
            
            // 读取高级过滤数据
            if (tag.hasKey("filterMode")) {
                cap.filterMode = FilterMode.fromName(tag.getString("filterMode"));
            }
            
            // 读取模组过滤列表
            cap.filterMods.clear();
            if (tag.hasKey("filterMods")) {
                net.minecraft.nbt.NBTTagList modList = tag.getTagList("filterMods", 8);
                for (int i = 0; i < modList.tagCount(); i++) {
                    cap.filterMods.add(modList.getStringTagAt(i));
                }
            }
            
            // 读取模组过滤槽位
            cap.modFilterSlots.clear();
            if (tag.hasKey("modFilterSlots")) {
                net.minecraft.nbt.NBTTagList modSlotList = tag.getTagList("modFilterSlots", 8);
                for (int i = 0; i < modSlotList.tagCount(); i++) {
                    cap.modFilterSlots.add(modSlotList.getStringTagAt(i));
                }
            }
            
            // 读取物品ID过滤槽的NBT数据
            cap.filterSlotNBTs.clear();
            if (tag.hasKey("filterSlotNBTs")) {
                NBTTagCompound filterSlotNBTsTag = tag.getCompoundTag("filterSlotNBTs");
                for (String key : filterSlotNBTsTag.getKeySet()) {
                    try {
                        int slot = Integer.parseInt(key);
                        cap.filterSlotNBTs.put(slot, filterSlotNBTsTag.getCompoundTag(key));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // 读取模组过滤槽的NBT数据
            cap.modFilterSlotNBTs.clear();
            if (tag.hasKey("modFilterSlotNBTs")) {
                NBTTagCompound modFilterSlotNBTsTag = tag.getCompoundTag("modFilterSlotNBTs");
                for (String key : modFilterSlotNBTsTag.getKeySet()) {
                    try {
                        int slot = Integer.parseInt(key);
                        cap.modFilterSlotNBTs.put(slot, modFilterSlotNBTsTag.getCompoundTag(key));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // 读取属性过滤列表
            cap.filterAttributes.clear();
            if (tag.hasKey("filterAttributes")) {
                net.minecraft.nbt.NBTTagList attrList = tag.getTagList("filterAttributes", 10);
                for (int i = 0; i < attrList.tagCount(); i++) {
                    NBTTagCompound attrTag = attrList.getCompoundTagAt(i);
                    ItemAttribute attr = ItemAttribute.fromNBT(attrTag);
                    boolean inverted = attrTag.getBoolean("inverted");
                    if (attr != null) {
                        cap.filterAttributes.add(Pair.of(attr, inverted));
                    }
                }
            }
            
            if (tag.hasKey("blacklistItems")) {
                net.minecraft.nbt.NBTTagList blacklistList =
tag.getTagList("blacklistItems", 8); // 8 = String tag
                cap.blacklistItems.clear();
                for (int i = 0; i < blacklistList.tagCount(); i++) {
                    cap.blacklistItems.add(blacklistList.getStringTagAt(i));
                }
            } else {
                cap.loadDefaultFilterList();
            }

            // 读取属性输入槽位的完整NBT
            if (tag.hasKey("attributeInputSlotNBT")) {
                cap.attributeInputSlotNBT = tag.getCompoundTag("attributeInputSlotNBT");
            } else {
                cap.attributeInputSlotNBT = new NBTTagCompound();
            }

            // 读取NBT和耐久匹配选项 - 优先使用打包格式
            if (tag.hasKey("matchFlags")) {
                unpackMatchFlags(cap, tag.getByte("matchFlags"));
            } else {
                // 向后兼容
                cap.matchNbt = tag.getBoolean("matchNbt");
                cap.matchDurability = tag.getBoolean("matchDurability");
            }
            
            // ==================== 读取销毁模式数据 ====================
            if (tag.hasKey("destroyFilterMode")) {
                cap.destroyFilterMode = FilterMode.fromName(tag.getString("destroyFilterMode"));
            }
            
            // 读取销毁模式布尔值 - 优先使用打包格式
            if (tag.hasKey("destroyFlags")) {
                unpackDestroyFlags(cap, tag.getByte("destroyFlags"));
            } else {
                // 向后兼容
                cap.destroyEnabled = tag.getBoolean("destroyEnabled");
                cap.destroyWhitelistMode = tag.getBoolean("destroyWhitelistMode");
                cap.destroyMatchAllMode = tag.getBoolean("destroyMatchAllMode");
                cap.destroyMatchNbt = tag.getBoolean("destroyMatchNbt");
                cap.destroyMatchDurability = tag.getBoolean("destroyMatchDurability");
                if (tag.hasKey("shouldWorkInGUI")) {
                    cap.shouldWorkInGUI = tag.getBoolean("shouldWorkInGUI");
                }
            }
            
            if (tag.hasKey("destroyModeType")) {
                cap.destroyModeType = DestroyModeType.fromName(tag.getString("destroyModeType"));
            }
            
            // 读取销毁物品过滤槽
            cap.destroyFilterSlots.clear();
            if (tag.hasKey("destroyFilterSlots")) {
                net.minecraft.nbt.NBTTagList slotList = tag.getTagList("destroyFilterSlots", 8);
                for (int i = 0; i < slotList.tagCount(); i++) {
                    cap.destroyFilterSlots.add(slotList.getStringTagAt(i));
                }
            }
            
            // 读取销毁模组过滤槽位
            cap.destroyModFilterSlots.clear();
            if (tag.hasKey("destroyModFilterSlots")) {
                net.minecraft.nbt.NBTTagList modSlotList = tag.getTagList("destroyModFilterSlots", 8);
                for (int i = 0; i < modSlotList.tagCount(); i++) {
                    cap.destroyModFilterSlots.add(modSlotList.getStringTagAt(i));
                }
            }
            
            // 读取销毁模组过滤列表
            cap.destroyFilterMods.clear();
            if (tag.hasKey("destroyFilterMods")) {
                net.minecraft.nbt.NBTTagList modList = tag.getTagList("destroyFilterMods", 8);
                for (int i = 0; i < modList.tagCount(); i++) {
                    cap.destroyFilterMods.add(modList.getStringTagAt(i));
                }
            }
            
            // 读取销毁属性过滤列表
            cap.destroyFilterAttributes.clear();
            if (tag.hasKey("destroyFilterAttributes")) {
                net.minecraft.nbt.NBTTagList attrList = tag.getTagList("destroyFilterAttributes", 10);
                for (int i = 0; i < attrList.tagCount(); i++) {
                    NBTTagCompound attrTag = attrList.getCompoundTagAt(i);
                    ItemAttribute attr = ItemAttribute.fromNBT(attrTag);
                    boolean inverted = attrTag.getBoolean("inverted");
                    if (attr != null) {
                        cap.destroyFilterAttributes.add(Pair.of(attr, inverted));
                    }
                }
            }
            
            // 读取销毁属性输入槽
            if (tag.hasKey("destroyAttributeInputSlotNBT")) {
                cap.destroyAttributeInputSlotNBT = tag.getCompoundTag("destroyAttributeInputSlotNBT");
            } else {
                cap.destroyAttributeInputSlotNBT = new NBTTagCompound();
            }
            
            // 读取销毁物品ID过滤槽的NBT数据
            cap.destroyFilterSlotNBTs.clear();
            if (tag.hasKey("destroyFilterSlotNBTs")) {
                NBTTagCompound destroyFilterSlotNBTsTag = tag.getCompoundTag("destroyFilterSlotNBTs");
                for (String key : destroyFilterSlotNBTsTag.getKeySet()) {
                    try {
                        int slot = Integer.parseInt(key);
                        cap.destroyFilterSlotNBTs.put(slot, destroyFilterSlotNBTsTag.getCompoundTag(key));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // 读取销毁模组过滤槽的NBT数据
            cap.destroyModFilterSlotNBTs.clear();
            if (tag.hasKey("destroyModFilterSlotNBTs")) {
                NBTTagCompound destroyModFilterSlotNBTsTag = tag.getCompoundTag("destroyModFilterSlotNBTs");
                for (String key : destroyModFilterSlotNBTsTag.getKeySet()) {
                    try {
                        int slot = Integer.parseInt(key);
                        cap.destroyModFilterSlotNBTs.put(slot, destroyModFilterSlotNBTsTag.getCompoundTag(key));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // ==================== 读取垃圾箱绑定数据 ====================
            if (tag.hasKey("trashCanX") && tag.hasKey("trashCanY") && tag.hasKey("trashCanZ")) {
                int x = tag.getInteger("trashCanX");
                int y = tag.getInteger("trashCanY");
                int z = tag.getInteger("trashCanZ");
                cap.trashCanPos = new BlockPos(x, y, z);
                cap.trashCanDimension = tag.getInteger("trashCanDimension");
            }
            
            // ==================== 读取彩蛋模式 ====================
            cap.easterEgg = tag.getBoolean("easterEgg");
        }
    }

    public static class ReadOnlyEnergyWrapper implements IEnergyStorage {
        private final IEnergyStorage inner;

        public ReadOnlyEnergyWrapper(IEnergyStorage inner) {
            this.inner = inner;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return inner.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return inner.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return inner.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return inner.canExtract();
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    public static class RsRingCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {
        private IRsRingCapability capability = new RsRingCapability();
        private ReadOnlyEnergyWrapper readOnlyWrapper;

        public RsRingCapabilityProvider() {}

        /** 从NBT初始化能力数据 */
        public void initFromNBT(NBTTagCompound nbt) {
            if (nbt != null && !nbt.getKeySet().isEmpty() && RS_RING_CAPABILITY != null) {
                RS_RING_CAPABILITY.getStorage().readNBT(RS_RING_CAPABILITY, capability, null, nbt);
            }
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == RS_RING_CAPABILITY || capability == CapabilityEnergy.ENERGY;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            if (capability == RS_RING_CAPABILITY) {
                return (T) this.capability;
            }
            if (capability == CapabilityEnergy.ENERGY) {
                if (this.capability.isSealed()) {
                    if (readOnlyWrapper == null || readOnlyWrapper.inner != this.capability.getEnergyStorage()) {
                        readOnlyWrapper = new ReadOnlyEnergyWrapper(this.capability.getEnergyStorage());
                    }
                    return (T) readOnlyWrapper;
                }
                return (T) this.capability.getEnergyStorage();
            }
            return null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return (NBTTagCompound) RS_RING_CAPABILITY.getStorage().writeNBT(RS_RING_CAPABILITY, capability, null);
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            RS_RING_CAPABILITY.getStorage().readNBT(RS_RING_CAPABILITY, capability, null, nbt);
        }
    }

    public static void syncCapabilityToStack(ItemStack stack, IRsRingCapability cap) {
        if (cap == null || RS_RING_CAPABILITY == null) return;
        NBTBase nbt = RS_RING_CAPABILITY.getStorage().writeNBT(RS_RING_CAPABILITY, cap, null);
        if (nbt instanceof NBTTagCompound) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setTag("RsRingData", (NBTTagCompound) nbt);
        }
    }
}






