package com.rsring.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public class ExperiencePumpCapability implements IExperiencePumpCapability {

    @CapabilityInject(IExperiencePumpCapability.class)
    public static final Capability<IExperiencePumpCapability> EXPERIENCE_PUMP_CAPABILITY = null;
    // Experience pump capacity constants
    private static final int DEFAULT_CAPACITY_LEVELS = 1;
    private static final int LEVELS_PER_PEARL = 1;
    private static final int MAX_CAPACITY_LEVELS = 25;
    private static final int MIN_RETAIN_LEVEL = 0;
    private static final int MIN_CAPACITY_LEVELS = 1;
    private static final int MODE_MIN = 0;
    private static final int MODE_MAX = 2;
    // NBT keys
    private static final String XP_NBT_KEY = "xp";
    private static final String CAPACITY_LEVELS_NBT_KEY = "capacityLevels";
    private static final String FIXED_MAX_XP_NBT_KEY = "fixedMaxXp";
    private static final String MODE_NBT_KEY = "mode";
    private static final String RETAIN_LEVEL_NBT_KEY = "retainLevel";
    private static final String MENDING_NBT_KEY = "mending";

    private static int getConfiguredDefaultMode() {
        int mode = com.rsring.config.ExperienceTankConfig.tank.defaultPumpMode;
        if (mode < MODE_MIN || mode > MODE_MAX) {
            mode = MODE_OFF;
        }
        return mode;
    }

    private static int getConfiguredDefaultRetainLevel() {
        int level = com.rsring.config.ExperienceTankConfig.tank.defaultRetainLevel;
        if (level < MIN_RETAIN_LEVEL) {
            level = MIN_RETAIN_LEVEL;
        }
        return level;
    }

    private static boolean getConfiguredDefaultMending() {
        return com.rsring.config.ExperienceTankConfig.tank.defaultMendingMode;
    }

    private static int getConfiguredMaxCapacityLevels() {
        int max = com.rsring.config.ExperienceTankConfig.tank.maxTankLevelLimit;
        if (max < MIN_CAPACITY_LEVELS) {
            max = MIN_CAPACITY_LEVELS;
        }
        if (max > MAX_CAPACITY_LEVELS) {
            max = MAX_CAPACITY_LEVELS;
        }
        return max;
    }

    private int xpStored = 0;
    private int capacityLevels = DEFAULT_CAPACITY_LEVELS;
    /** Fixed capacity for special tanks. */
    private int fixedMaxXp = 0;
    private int mode = getConfiguredDefaultMode();
    private int retainLevel = getConfiguredDefaultRetainLevel();
    private boolean useForMending = getConfiguredDefaultMending();

    @Override
    public int getXpStored() { return xpStored; }

    @Override
    public void setXpStored(int xp) {
        this.xpStored = Math.max(0, Math.min(xp, getMaxXp()));
    }

    @Override
    public int getCapacityLevels() { return capacityLevels; }

    @Override
    public void setCapacityLevels(int levels) {
        int currentXp = this.xpStored;
        int maxLevels = getConfiguredMaxCapacityLevels();
        this.capacityLevels = Math.max(MIN_CAPACITY_LEVELS, Math.min(levels, maxLevels));
        this.xpStored = Math.max(0, Math.min(currentXp, getMaxXp()));
    }

    @Override
    public int getMaxXp() {
        // Fixed capacity for special tanks
        if (fixedMaxXp > 0) {
            return fixedMaxXp;
        }
        // Standard tanks: BASE_XP_PER_LEVEL * 2^(n-1)
        // Example: level 1=1000, 2=2000, 3=4000, 4=8000...
        try {
            long maxCapacity = (long) BASE_XP_PER_LEVEL * (1L << (capacityLevels - 1));
            if (maxCapacity > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) maxCapacity;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getMode() { return mode; }

    @Override
    public void setMode(int mode) {
        this.mode = (mode >= MODE_MIN && mode <= MODE_MAX) ? mode : MODE_OFF;
    }

    @Override
    public int getRetainLevel() { return retainLevel; }

    @Override
    public void setRetainLevel(int level) {
        this.retainLevel = Math.max(MIN_RETAIN_LEVEL, level);
    }

    @Override
    public boolean isUseForMending() { return useForMending; }

    @Override
    public void setUseForMending(boolean use) {
        this.useForMending = use;
    }

    @Override
    public boolean addCapacityLevels(int levels) {
        int maxLevels = getConfiguredMaxCapacityLevels();
        if (levels <= 0 || capacityLevels >= maxLevels) return false;
        if (capacityLevels > maxLevels - levels) {
            setCapacityLevels(maxLevels);
        } else {
            setCapacityLevels(capacityLevels + levels);
        }
        return true;
    }

    @Override
    public int addXp(int amount) {
        if (amount <= 0) return 0;
        int maxAdd = Math.min(amount, getMaxXp() - xpStored);
        if (maxAdd <= 0) return 0;
        xpStored += maxAdd;
        return maxAdd;
    }

    @Override
    public int takeXp(int amount) {
        if (amount <= 0) return 0;
        int take = Math.min(amount, xpStored);
        if (take <= 0) return 0;
        xpStored -= take;
        return take;
    }

    @Override
    public IExperiencePumpCapability copy() {
        ExperiencePumpCapability c = new ExperiencePumpCapability();
        c.xpStored = this.xpStored;
        c.capacityLevels = this.capacityLevels;
        c.fixedMaxXp = this.fixedMaxXp;
        c.mode = this.mode;
        c.retainLevel = this.retainLevel;
        c.useForMending = this.useForMending;
        return c;
    }

    public static class Storage implements Capability.IStorage<IExperiencePumpCapability> {
        @Override
        public NBTBase writeNBT(Capability<IExperiencePumpCapability> capability, IExperiencePumpCapability instance, EnumFacing side) {
            if (instance == null) return new NBTTagCompound();

            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger(XP_NBT_KEY, instance.getXpStored());
            tag.setInteger(CAPACITY_LEVELS_NBT_KEY, instance.getCapacityLevels());
            if (instance instanceof ExperiencePumpCapability) {
                tag.setInteger(FIXED_MAX_XP_NBT_KEY, ((ExperiencePumpCapability) instance).fixedMaxXp);
            }
            tag.setInteger(MODE_NBT_KEY, instance.getMode());
            tag.setInteger(RETAIN_LEVEL_NBT_KEY, instance.getRetainLevel());
            tag.setBoolean(MENDING_NBT_KEY, instance.isUseForMending());
            return tag;
        }

        @Override
        public void readNBT(Capability<IExperiencePumpCapability> capability, IExperiencePumpCapability instance, EnumFacing side, NBTBase nbt) {
            if (instance == null || nbt == null) return;

            NBTTagCompound tag = (NBTTagCompound) nbt;
            // Set capacity before XP to avoid truncation
            instance.setCapacityLevels(tag.hasKey(CAPACITY_LEVELS_NBT_KEY) ? tag.getInteger(CAPACITY_LEVELS_NBT_KEY) : DEFAULT_CAPACITY_LEVELS);
            if (instance instanceof ExperiencePumpCapability && tag.hasKey(FIXED_MAX_XP_NBT_KEY)) {
                ((ExperiencePumpCapability) instance).fixedMaxXp = tag.getInteger(FIXED_MAX_XP_NBT_KEY);
            }
            instance.setXpStored(tag.hasKey(XP_NBT_KEY) ? tag.getInteger(XP_NBT_KEY) : 0);
            instance.setMode(tag.hasKey(MODE_NBT_KEY) ? tag.getInteger(MODE_NBT_KEY) : getConfiguredDefaultMode());
            instance.setRetainLevel(tag.hasKey(RETAIN_LEVEL_NBT_KEY) ? tag.getInteger(RETAIN_LEVEL_NBT_KEY) : getConfiguredDefaultRetainLevel());
            instance.setUseForMending(tag.hasKey(MENDING_NBT_KEY) ? tag.getBoolean(MENDING_NBT_KEY) : getConfiguredDefaultMending());
        }
    }

    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final IExperiencePumpCapability capability = new ExperiencePumpCapability();

        @Override
        public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
            return cap == EXPERIENCE_PUMP_CAPABILITY;
        }

        @Override
        public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
            return cap == EXPERIENCE_PUMP_CAPABILITY ? (T) capability : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return (NBTTagCompound) EXPERIENCE_PUMP_CAPABILITY.getStorage().writeNBT(EXPERIENCE_PUMP_CAPABILITY, capability, null);
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            EXPERIENCE_PUMP_CAPABILITY.getStorage().readNBT(EXPERIENCE_PUMP_CAPABILITY, capability, null, nbt);
        }

        public void initFromNBT(NBTTagCompound nbt) {
            if (nbt != null && !nbt.getKeySet().isEmpty())
                EXPERIENCE_PUMP_CAPABILITY.getStorage().readNBT(EXPERIENCE_PUMP_CAPABILITY, capability, null, nbt);
        }
    }
}

