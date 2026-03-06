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
import com.rsring.util.Pair;
import com.rsring.config.RsRingConfig;
import java.util.List;
import java.util.ArrayList;

public class RsRingCapability implements IRsRingCapability {

    @CapabilityInject(IRsRingCapability.class)
    public static final Capability<IRsRingCapability> RS_RING_CAPABILITY = null;

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

    // NBT和耐久匹配选项
    private boolean matchNbt = false;
    private boolean matchDurability = false;
    
    // ==================== 销毁模式字段 ====================
    private transient boolean destroyModeUI = false; // 销毁模式UI状态（不持久化）
    private boolean destroyEnabled = false; // 销毁功能开关
    private FilterMode destroyFilterMode = FilterMode.ITEM; // 销毁过滤模式
    private boolean destroyWhitelistMode = true; // 销毁黑白名单模式（默认为白名单）
    private List<String> destroyFilterSlots = new ArrayList<>(); // 销毁物品过滤槽
    private List<Pair<ItemAttribute, Boolean>> destroyFilterAttributes = new ArrayList<>(); // 销毁属性过滤列表
    private List<String> destroyFilterMods = new ArrayList<>(); // 销毁模组过滤列表
    private NBTTagCompound destroyAttributeInputSlotNBT = new NBTTagCompound(); // 销毁属性输入槽NBT
    private boolean destroyMatchAllMode = false; // 销毁AND/OR模式
    private boolean destroyMatchNbt = false; // 销毁NBT匹配
    private boolean destroyMatchDurability = false; // 销毁耐久匹配
    
    // ==================== 垃圾箱绑定字段 ====================
    private BlockPos trashCanPos; // 垃圾箱位置
    private int trashCanDimension; // 垃圾箱所在维度

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

    @Override
    public void bindTerminal(World world, BlockPos pos) {
        this.terminalPos = pos;
        this.terminalDimension = world.provider.getDimension();
    }

    @Override
    public void unbindTerminal() {
        this.terminalPos = null;
        this.terminalDimension = 0;
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
        this.enabled = enabled;
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
            }
        }
    }

    @Override
    public void removeFromBlacklist(ItemStack item) {
        if (!allowCustomFilters()) return;
        if (!item.isEmpty()) {
            String itemName = item.getItem().getRegistryName().toString();
            blacklistItems.remove(itemName);
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
        blacklistItems.set(slot, itemRegistryName == null || itemRegistryName.isEmpty() ? "" : itemRegistryName);
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
    public boolean isSealed() {
        return sealed;
    }

    @Override
    public void setSealed(boolean sealed) {
        this.sealed = sealed;
    }

    @Override
    public FilterMode getFilterMode() {
        return this.filterMode;
    }

    @Override
    public void setFilterMode(FilterMode mode) {
        this.filterMode = mode;
    }

    @Override
    public boolean isMatchAllMode() {
        return this.matchAllMode;
    }

    @Override
    public void setMatchAllMode(boolean matchAll) {
        this.matchAllMode = matchAll;
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
        }
    }

    @Override
    public void removeFilterMod(String modId) {
        if (!allowCustomFilters()) return;
        this.filterMods.remove(modId);
    }

    // 属性过滤模式的输入槽位（存储完整ItemStack NBT）
    private NBTTagCompound attributeInputSlotNBT = new NBTTagCompound();

    @Override
    public void setAttributeInputSlotNBT(NBTTagCompound nbt) {
        if (!allowCustomFilters()) return;
        this.attributeInputSlotNBT = nbt != null ? nbt : new NBTTagCompound();
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
            clearAttributeInputSlot();
        } else {
            // 保存物品的完整NBT数据（数量设为1）
            ItemStack copy = stack.copy();
            copy.setCount(1);
            this.attributeInputSlotNBT = copy.writeToNBT(new NBTTagCompound());
        }
    }

    // NBT和耐久匹配方法实现
    @Override
    public boolean shouldMatchNbt() {
        return this.matchNbt;
    }

    @Override
    public void setMatchNbt(boolean matchNbt) {
        this.matchNbt = matchNbt;
    }

    @Override
    public boolean shouldMatchDurability() {
        return this.matchDurability;
    }

    @Override
    public void setMatchDurability(boolean matchDurability) {
        this.matchDurability = matchDurability;
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
        this.destroyEnabled = enabled;
    }
    
    @Override
    public FilterMode getDestroyFilterMode() {
        return this.destroyFilterMode;
    }
    
    @Override
    public void setDestroyFilterMode(FilterMode mode) {
        this.destroyFilterMode = mode;
    }
    
    @Override
    public boolean isDestroyWhitelistMode() {
        return this.destroyWhitelistMode;
    }
    
    @Override
    public void setDestroyWhitelistMode(boolean whitelist) {
        this.destroyWhitelistMode = whitelist;
    }
    
    @Override
    public void setDestroyFilterSlot(int slot, String itemRegistryName) {
        if (!allowCustomFilters()) return;
        if (slot < 0 || slot > 8) return;
        while (destroyFilterSlots.size() <= slot) destroyFilterSlots.add("");
        destroyFilterSlots.set(slot, itemRegistryName == null || itemRegistryName.isEmpty() ? "" : itemRegistryName);
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
        }
    }
    
    @Override
    public void removeDestroyFilterAttribute(int index) {
        if (!allowCustomFilters()) return;
        if (index >= 0 && index < this.destroyFilterAttributes.size()) {
            this.destroyFilterAttributes.remove(index);
        }
    }
    
    @Override
    public void setDestroyFilterAttributeInverted(int index, boolean inverted) {
        if (!allowCustomFilters()) return;
        if (index >= 0 && index < this.destroyFilterAttributes.size()) {
            Pair<ItemAttribute, Boolean> current = this.destroyFilterAttributes.get(index);
            this.destroyFilterAttributes.set(index, Pair.of(current.getKey(), inverted));
        }
    }
    
    @Override
    public void clearDestroyFilterAttributes() {
        this.destroyFilterAttributes.clear();
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
        }
    }
    
    @Override
    public void removeDestroyFilterMod(String modId) {
        if (!allowCustomFilters()) return;
        this.destroyFilterMods.remove(modId);
    }
    
    @Override
    public void setDestroyAttributeInputSlotNBT(NBTTagCompound nbt) {
        if (!allowCustomFilters()) return;
        this.destroyAttributeInputSlotNBT = nbt != null ? nbt : new NBTTagCompound();
    }
    
    @Override
    public NBTTagCompound getDestroyAttributeInputSlotNBT() {
        if (!allowCustomFilters()) return new NBTTagCompound();
        return this.destroyAttributeInputSlotNBT;
    }
    
    @Override
    public void clearDestroyAttributeInputSlot() {
        this.destroyAttributeInputSlotNBT = new NBTTagCompound();
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
            clearDestroyAttributeInputSlot();
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            this.destroyAttributeInputSlotNBT = copy.writeToNBT(new NBTTagCompound());
        }
    }
    
    @Override
    public boolean isDestroyMatchAllMode() {
        return this.destroyMatchAllMode;
    }
    
    @Override
    public void setDestroyMatchAllMode(boolean matchAll) {
        this.destroyMatchAllMode = matchAll;
    }
    
    @Override
    public boolean shouldDestroyMatchNbt() {
        return this.destroyMatchNbt;
    }
    
    @Override
    public void setDestroyMatchNbt(boolean matchNbt) {
        this.destroyMatchNbt = matchNbt;
    }
    
    @Override
    public boolean shouldDestroyMatchDurability() {
        return this.destroyMatchDurability;
    }
    
    @Override
    public void setDestroyMatchDurability(boolean matchDurability) {
        this.destroyMatchDurability = matchDurability;
    }
    
    // ==================== 垃圾箱绑定方法实现 ====================
    @Override
    public void bindTrashCan(World world, BlockPos pos) {
        this.trashCanPos = pos;
        this.trashCanDimension = world.provider.getDimension();
    }
    
    @Override
    public void unbindTrashCan() {
        this.trashCanPos = null;
        this.trashCanDimension = 0;
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

            tag.setBoolean("enabled", cap.enabled);
            tag.setInteger("energy", cap.energyStorage.getEnergyStored());

            tag.setBoolean("whitelistMode", cap.whitelistMode);
            tag.setBoolean("sealed", cap.sealed);
            
            // 保存高级过滤数据
            tag.setString("filterMode", cap.filterMode.getName());
            tag.setBoolean("matchAllMode", cap.matchAllMode);
            
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

            // 保存NBT和耐久匹配选项
            tag.setBoolean("matchNbt", cap.matchNbt);
            tag.setBoolean("matchDurability", cap.matchDurability);
            
            // ==================== 保存销毁模式数据 ====================
            tag.setBoolean("destroyEnabled", cap.destroyEnabled);
            tag.setString("destroyFilterMode", cap.destroyFilterMode.getName());
            tag.setBoolean("destroyWhitelistMode", cap.destroyWhitelistMode);
            tag.setBoolean("destroyMatchAllMode", cap.destroyMatchAllMode);
            tag.setBoolean("destroyMatchNbt", cap.destroyMatchNbt);
            tag.setBoolean("destroyMatchDurability", cap.destroyMatchDurability);
            
            // 保存销毁物品过滤槽
            net.minecraft.nbt.NBTTagList destroySlotList = new net.minecraft.nbt.NBTTagList();
            for (String item : cap.destroyFilterSlots) {
                destroySlotList.appendTag(new net.minecraft.nbt.NBTTagString(item));
            }
            tag.setTag("destroyFilterSlots", destroySlotList);
            
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
            
            // ==================== 保存垃圾箱绑定数据 ====================
            if (cap.trashCanPos != null) {
                tag.setInteger("trashCanX", cap.trashCanPos.getX());
                tag.setInteger("trashCanY", cap.trashCanPos.getY());
                tag.setInteger("trashCanZ", cap.trashCanPos.getZ());
                tag.setInteger("trashCanDimension", cap.trashCanDimension);
            }

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

            cap.enabled = tag.getBoolean("enabled");
            int maxEnergy = getConfiguredMaxEnergy();
            int energy = Math.min(tag.getInteger("energy"), maxEnergy);
            cap.energyStorage = new EnergyStorage(maxEnergy, MAX_IO, MAX_IO, energy);

            if (tag.hasKey("whitelistMode")) {
                cap.whitelistMode = tag.getBoolean("whitelistMode");
            } else {
                cap.whitelistMode = getConfiguredWhitelistMode();
            }
            cap.sealed = tag.getBoolean("sealed");
            
            // 读取高级过滤数据
            if (tag.hasKey("filterMode")) {
                cap.filterMode = FilterMode.fromName(tag.getString("filterMode"));
            }
            cap.matchAllMode = tag.getBoolean("matchAllMode");
            
            // 读取模组过滤列表
            cap.filterMods.clear();
            if (tag.hasKey("filterMods")) {
                net.minecraft.nbt.NBTTagList modList = tag.getTagList("filterMods", 8);
                for (int i = 0; i < modList.tagCount(); i++) {
                    cap.filterMods.add(modList.getStringTagAt(i));
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

            // 读取NBT和耐久匹配选项
            cap.matchNbt = tag.getBoolean("matchNbt");
            cap.matchDurability = tag.getBoolean("matchDurability");
            
            // ==================== 读取销毁模式数据 ====================
            cap.destroyEnabled = tag.getBoolean("destroyEnabled");
            if (tag.hasKey("destroyFilterMode")) {
                cap.destroyFilterMode = FilterMode.fromName(tag.getString("destroyFilterMode"));
            }
            cap.destroyWhitelistMode = tag.getBoolean("destroyWhitelistMode");
            cap.destroyMatchAllMode = tag.getBoolean("destroyMatchAllMode");
            cap.destroyMatchNbt = tag.getBoolean("destroyMatchNbt");
            cap.destroyMatchDurability = tag.getBoolean("destroyMatchDurability");
            
            // 读取销毁物品过滤槽
            cap.destroyFilterSlots.clear();
            if (tag.hasKey("destroyFilterSlots")) {
                net.minecraft.nbt.NBTTagList slotList = tag.getTagList("destroyFilterSlots", 8);
                for (int i = 0; i < slotList.tagCount(); i++) {
                    cap.destroyFilterSlots.add(slotList.getStringTagAt(i));
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
            
            // ==================== 读取垃圾箱绑定数据 ====================
            if (tag.hasKey("trashCanX") && tag.hasKey("trashCanY") && tag.hasKey("trashCanZ")) {
                int x = tag.getInteger("trashCanX");
                int y = tag.getInteger("trashCanY");
                int z = tag.getInteger("trashCanZ");
                cap.trashCanPos = new BlockPos(x, y, z);
                cap.trashCanDimension = tag.getInteger("trashCanDimension");
            }
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






