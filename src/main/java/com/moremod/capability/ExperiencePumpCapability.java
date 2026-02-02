package com.moremod.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public class ExperiencePumpCapability implements IExperiencePumpCapability {

    @CapabilityInject(IExperiencePumpCapability.class)
    public static final Capability<IExperiencePumpCapability> EXPERIENCE_PUMP_CAPABILITY = null;

    // 常量定义
    private static final int DEFAULT_CAPACITY_LEVELS = 1;  // 初始容量1级（1000 mB）
    private static final int LEVELS_PER_PEARL = 1;  // 每次升级增加1级（1000 mB）
    private static final int MAX_CAPACITY_LEVELS = 1000;
    private static final int MIN_RETAIN_LEVEL = 0;
    private static final int MIN_CAPACITY_LEVELS = 1;
    private static final int MODE_MIN = 0;
    private static final int MODE_MAX = 2;

    // NBT 标签常量
    private static final String XP_NBT_KEY = "xp";
    private static final String CAPACITY_LEVELS_NBT_KEY = "capacityLevels";
    private static final String MODE_NBT_KEY = "mode";
    private static final String RETAIN_LEVEL_NBT_KEY = "retainLevel";
    private static final String MENDING_NBT_KEY = "mending";

    private int xpStored = 0;
    private int capacityLevels = DEFAULT_CAPACITY_LEVELS;
    private int mode = MODE_OFF;
    private int retainLevel = 10;
    private boolean useForMending = false;

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
        this.capacityLevels = Math.max(MIN_CAPACITY_LEVELS, Math.min(levels, MAX_CAPACITY_LEVELS));
        setXpStored(xpStored); // clamp stored to new max
    }

    @Override
    public int getMaxXp() {
        // 按照指数增长计算容量：第n级容量 = BASE_XP_PER_LEVEL * 2^(n-1)
        // 例如：1级=1000, 2级=2000, 3级=4000, 4级=8000...
        try {
            long maxCapacity = (long) BASE_XP_PER_LEVEL * (1L << (capacityLevels - 1));
            if (maxCapacity > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) maxCapacity;
        } catch (Exception e) {
            // 防止溢出或其他异常
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
        if (levels <= 0 || capacityLevels >= MAX_CAPACITY_LEVELS) return false;
        // 检查是否会超出最大容量
        if (capacityLevels > MAX_CAPACITY_LEVELS - levels) {
            setCapacityLevels(MAX_CAPACITY_LEVELS);
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
        c.mode = this.mode;
        c.retainLevel = this.retainLevel;
        c.useForMending = this.useForMending;
        return c;
    }

    public static class Storage implements Capability.IStorage<IExperiencePumpCapability> {
        @Override
        public NBTBase writeNBT(Capability<IExperiencePumpCapability> capability, IExperiencePumpCapability instance, EnumFacing side) {
            if (instance == null) return new NBTTagCompound(); // 返回空标签而不是null

            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger(XP_NBT_KEY, instance.getXpStored());
            tag.setInteger(CAPACITY_LEVELS_NBT_KEY, instance.getCapacityLevels());
            tag.setInteger(MODE_NBT_KEY, instance.getMode());
            tag.setInteger(RETAIN_LEVEL_NBT_KEY, instance.getRetainLevel());
            tag.setBoolean(MENDING_NBT_KEY, instance.isUseForMending());
            return tag;
        }

        @Override
        public void readNBT(Capability<IExperiencePumpCapability> capability, IExperiencePumpCapability instance, EnumFacing side, NBTBase nbt) {
            if (instance == null || nbt == null) return;

            NBTTagCompound tag = (NBTTagCompound) nbt;
            instance.setXpStored(tag.hasKey(XP_NBT_KEY) ? tag.getInteger(XP_NBT_KEY) : 0);
            instance.setCapacityLevels(tag.hasKey(CAPACITY_LEVELS_NBT_KEY) ? tag.getInteger(CAPACITY_LEVELS_NBT_KEY) : DEFAULT_CAPACITY_LEVELS);
            instance.setMode(tag.hasKey(MODE_NBT_KEY) ? tag.getInteger(MODE_NBT_KEY) : MODE_OFF);
            instance.setRetainLevel(tag.hasKey(RETAIN_LEVEL_NBT_KEY) ? tag.getInteger(RETAIN_LEVEL_NBT_KEY) : 10);
            instance.setUseForMending(tag.hasKey(MENDING_NBT_KEY) ? tag.getBoolean(MENDING_NBT_KEY) : false);
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
