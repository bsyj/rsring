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
            net.minecraft.nbt.NBTTagList blacklistList = new net.minecraft.nbt.NBTTagList();
            for (String item : cap.blacklistItems) {
                blacklistList.appendTag(new net.minecraft.nbt.NBTTagString(item));
            }
            tag.setTag("blacklistItems", blacklistList);

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
        }
    }

    public static class RsRingCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {
        private IRsRingCapability capability = new RsRingCapability();

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






