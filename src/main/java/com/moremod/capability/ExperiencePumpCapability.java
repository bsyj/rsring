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

    private static final int DEFAULT_CAPACITY_LEVELS = 1;  // 初始容量1级（1000 mB）
    private static final int LEVELS_PER_PEARL = 1;  // 每次升级增加1级（1000 mB）
    private static final int MAX_CAPACITY_LEVELS = 1000;

    private int xpStored = 0;
    private int capacityLevels = DEFAULT_CAPACITY_LEVELS;
    private int mode = MODE_OFF;
    private int retainLevel = 10;
    private boolean useForMending = false;

    @Override
    public int getXpStored() { return xpStored; }
    @Override
    public void setXpStored(int xp) { this.xpStored = Math.max(0, Math.min(xp, getMaxXp())); }

    @Override
    public int getCapacityLevels() { return capacityLevels; }
    @Override
    public void setCapacityLevels(int levels) {
        this.capacityLevels = Math.max(1, Math.min(levels, MAX_CAPACITY_LEVELS));
        setXpStored(xpStored); // clamp stored to new max
    }

    @Override
    public int getMaxXp() { return capacityLevels * XP_PER_LEVEL; }

    @Override
    public int getMode() { return mode; }
    @Override
    public void setMode(int mode) { this.mode = mode >= 0 && mode <= 2 ? mode : MODE_OFF; }

    @Override
    public int getRetainLevel() { return retainLevel; }
    @Override
    public void setRetainLevel(int level) { this.retainLevel = Math.max(0, level); }

    @Override
    public boolean isUseForMending() { return useForMending; }
    @Override
    public void setUseForMending(boolean use) { this.useForMending = use; }

    @Override
    public boolean addCapacityLevels(int levels) {
        if (levels <= 0 || capacityLevels >= MAX_CAPACITY_LEVELS) return false;
        setCapacityLevels(capacityLevels + levels);
        return true;
    }

    @Override
    public int addXp(int amount) {
        int maxAdd = Math.min(amount, getMaxXp() - xpStored);
        if (maxAdd <= 0) return 0;
        xpStored += maxAdd;
        return maxAdd;
    }

    @Override
    public int takeXp(int amount) {
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
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("xp", instance.getXpStored());
            tag.setInteger("capacityLevels", instance.getCapacityLevels());
            tag.setInteger("mode", instance.getMode());
            tag.setInteger("retainLevel", instance.getRetainLevel());
            tag.setBoolean("mending", instance.isUseForMending());
            return tag;
        }

        @Override
        public void readNBT(Capability<IExperiencePumpCapability> capability, IExperiencePumpCapability instance, EnumFacing side, NBTBase nbt) {
            NBTTagCompound tag = (NBTTagCompound) nbt;
            instance.setXpStored(tag.getInteger("xp"));
            instance.setCapacityLevels(tag.hasKey("capacityLevels") ? tag.getInteger("capacityLevels") : DEFAULT_CAPACITY_LEVELS);
            instance.setMode(tag.getInteger("mode"));
            instance.setRetainLevel(tag.getInteger("retainLevel"));
            instance.setUseForMending(tag.getBoolean("mending"));
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
