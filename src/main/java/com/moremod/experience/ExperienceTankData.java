package com.moremod.experience;

import net.minecraft.nbt.NBTTagCompound;
import java.util.UUID;

/**
 * Core data structure for experience tank management.
 * Handles storage, capacity, and serialization of experience tank data.
 * 
 * This class provides the foundation for experience tank upgrade preservation
 * and cross-inventory tank management as specified in Requirements 1.1, 1.3, 7.1.
 */
public class ExperienceTankData {
    
    private int storedExperience;     // Current XP stored
    private int maxCapacity;          // Maximum XP capacity  
    private int tankTier;             // Tank upgrade tier
    private UUID tankId;              // Unique identifier
    
    // NBT keys for serialization
    private static final String NBT_STORED_XP = "storedExperience";
    private static final String NBT_MAX_CAPACITY = "maxCapacity";
    private static final String NBT_TANK_TIER = "tankTier";
    private static final String NBT_TANK_ID = "tankId";
    
    /**
     * Creates a new experience tank data with default values.
     */
    public ExperienceTankData() {
        this.storedExperience = 0;
        this.maxCapacity = 1000; // Default 10 levels * 100 XP per level
        this.tankTier = 1;
        this.tankId = UUID.randomUUID();
    }
    
    /**
     * Creates a new experience tank data with specified values.
     */
    public ExperienceTankData(int storedExperience, int maxCapacity, int tankTier) {
        this.storedExperience = Math.max(0, Math.min(storedExperience, maxCapacity));
        this.maxCapacity = Math.max(1, maxCapacity);
        this.tankTier = Math.max(1, tankTier);
        this.tankId = UUID.randomUUID();
    }
    
    /**
     * Copy constructor for creating tank data from existing data.
     */
    public ExperienceTankData(ExperienceTankData other) {
        this.storedExperience = other.storedExperience;
        this.maxCapacity = other.maxCapacity;
        this.tankTier = other.tankTier;
        this.tankId = other.tankId;
    }
    
    // Getters and setters
    
    public int getStoredExperience() {
        return storedExperience;
    }
    
    public void setStoredExperience(int storedExperience) {
        this.storedExperience = Math.max(0, Math.min(storedExperience, maxCapacity));
    }
    
    public int getMaxCapacity() {
        return maxCapacity;
    }
    
    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = Math.max(1, maxCapacity);
        // Ensure stored XP doesn't exceed new capacity
        this.storedExperience = Math.min(this.storedExperience, this.maxCapacity);
    }
    
    public int getTankTier() {
        return tankTier;
    }
    
    public void setTankTier(int tankTier) {
        this.tankTier = Math.max(1, tankTier);
    }
    
    public UUID getTankId() {
        return tankId;
    }
    
    public void setTankId(UUID tankId) {
        this.tankId = tankId != null ? tankId : UUID.randomUUID();
    }
    
    // Utility methods
    
    /**
     * Checks if the tank is full.
     */
    public boolean isFull() {
        return storedExperience >= maxCapacity;
    }
    
    /**
     * Checks if the tank is empty.
     */
    public boolean isEmpty() {
        return storedExperience <= 0;
    }
    
    /**
     * Gets the remaining capacity.
     */
    public int getRemainingCapacity() {
        return maxCapacity - storedExperience;
    }
    
    /**
     * Gets the fill percentage (0.0 to 1.0).
     */
    public double getFillPercentage() {
        return maxCapacity > 0 ? (double) storedExperience / maxCapacity : 0.0;
    }
    
    /**
     * Adds experience to the tank, returning the amount actually added.
     */
    public int addExperience(int amount) {
        if (amount <= 0) return 0;
        
        int canAdd = Math.min(amount, getRemainingCapacity());
        storedExperience += canAdd;
        return canAdd;
    }
    
    /**
     * Removes experience from the tank, returning the amount actually removed.
     */
    public int removeExperience(int amount) {
        if (amount <= 0) return 0;
        
        int canRemove = Math.min(amount, storedExperience);
        storedExperience -= canRemove;
        return canRemove;
    }
    
    // NBT Serialization methods
    
    /**
     * Writes the tank data to NBT format.
     * Supports Requirements 1.1 and 1.3 for upgrade preservation.
     */
    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        
        nbt.setInteger(NBT_STORED_XP, storedExperience);
        nbt.setInteger(NBT_MAX_CAPACITY, maxCapacity);
        nbt.setInteger(NBT_TANK_TIER, tankTier);
        
        if (tankId != null) {
            nbt.setString(NBT_TANK_ID, tankId.toString());
        }
        
        return nbt;
    }
    
    /**
     * Reads the tank data from NBT format.
     * Provides backward compatibility and validation.
     */
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt == null) return;
        
        // Read stored experience with validation
        if (nbt.hasKey(NBT_STORED_XP)) {
            this.storedExperience = Math.max(0, nbt.getInteger(NBT_STORED_XP));
        }
        
        // Read max capacity with validation
        if (nbt.hasKey(NBT_MAX_CAPACITY)) {
            this.maxCapacity = Math.max(1, nbt.getInteger(NBT_MAX_CAPACITY));
        }
        
        // Read tank tier with validation
        if (nbt.hasKey(NBT_TANK_TIER)) {
            this.tankTier = Math.max(1, nbt.getInteger(NBT_TANK_TIER));
        }
        
        // Read tank ID with validation
        if (nbt.hasKey(NBT_TANK_ID)) {
            try {
                this.tankId = UUID.fromString(nbt.getString(NBT_TANK_ID));
            } catch (IllegalArgumentException e) {
                // Invalid UUID, generate new one
                this.tankId = UUID.randomUUID();
            }
        } else {
            this.tankId = UUID.randomUUID();
        }
        
        // Ensure stored XP doesn't exceed capacity after reading
        this.storedExperience = Math.min(this.storedExperience, this.maxCapacity);
    }
    
    /**
     * Creates a new ExperienceTankData from NBT.
     */
    public static ExperienceTankData fromNBT(NBTTagCompound nbt) {
        ExperienceTankData data = new ExperienceTankData();
        data.readFromNBT(nbt);
        return data;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ExperienceTankData that = (ExperienceTankData) obj;
        return storedExperience == that.storedExperience &&
               maxCapacity == that.maxCapacity &&
               tankTier == that.tankTier &&
               (tankId != null ? tankId.equals(that.tankId) : that.tankId == null);
    }
    
    @Override
    public int hashCode() {
        int result = storedExperience;
        result = 31 * result + maxCapacity;
        result = 31 * result + tankTier;
        result = 31 * result + (tankId != null ? tankId.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("ExperienceTankData{stored=%d, capacity=%d, tier=%d, id=%s}", 
                           storedExperience, maxCapacity, tankTier, tankId);
    }
}