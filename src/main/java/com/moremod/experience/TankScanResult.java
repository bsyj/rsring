package com.moremod.experience;

import net.minecraft.item.ItemStack;
import java.util.*;

/**
 * Result of scanning for experience tanks across different inventory locations.
 * Provides comprehensive information about detected tanks for the Experience Pump Controller.
 * 
 * Supports Requirements 3.1, 3.2, 3.3, 3.4 for comprehensive tank detection and capacity calculation.
 */
public class TankScanResult {
    
    /**
     * Enumeration of inventory types where tanks can be found.
     */
    public enum InventoryType {
        PLAYER_INVENTORY("Player Inventory"),
        HOTBAR("Hotbar"),
        BAUBLES("Baubles Slots"),
        OFFHAND("Off Hand");
        
        private final String displayName;
        
        InventoryType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final List<ItemStack> allTanks;                    // All detected tanks
    private final int totalCapacity;                           // Combined capacity
    private final int totalStored;                             // Combined stored XP
    private final Map<InventoryType, List<ItemStack>> tanksByLocation; // Tanks categorized by location
    private final long scanTimestamp;                          // When the scan was performed
    
    /**
     * Creates a new tank scan result.
     */
    public TankScanResult(List<ItemStack> allTanks, 
                         Map<InventoryType, List<ItemStack>> tanksByLocation) {
        this.allTanks = new ArrayList<>(allTanks != null ? allTanks : Collections.emptyList());
        this.tanksByLocation = new EnumMap<>(InventoryType.class);
        
        // Copy the tanks by location map
        if (tanksByLocation != null) {
            for (Map.Entry<InventoryType, List<ItemStack>> entry : tanksByLocation.entrySet()) {
                this.tanksByLocation.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        
        // Calculate totals
        this.totalCapacity = calculateTotalCapacity();
        this.totalStored = calculateTotalStored();
        this.scanTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates an empty scan result.
     */
    public static TankScanResult empty() {
        return new TankScanResult(Collections.emptyList(), Collections.emptyMap());
    }
    
    // Getters
    
    public List<ItemStack> getAllTanks() {
        return new ArrayList<>(allTanks);
    }
    
    public int getTotalCapacity() {
        return totalCapacity;
    }
    
    public int getTotalStored() {
        return totalStored;
    }
    
    public Map<InventoryType, List<ItemStack>> getTanksByLocation() {
        Map<InventoryType, List<ItemStack>> result = new EnumMap<>(InventoryType.class);
        for (Map.Entry<InventoryType, List<ItemStack>> entry : tanksByLocation.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
    
    public long getScanTimestamp() {
        return scanTimestamp;
    }
    
    // Utility methods
    
    /**
     * Gets the number of tanks found.
     */
    public int getTankCount() {
        return allTanks.size();
    }
    
    /**
     * Gets the number of tanks in a specific location.
     */
    public int getTankCount(InventoryType location) {
        List<ItemStack> tanks = tanksByLocation.get(location);
        return tanks != null ? tanks.size() : 0;
    }
    
    /**
     * Gets tanks from a specific location.
     */
    public List<ItemStack> getTanksFromLocation(InventoryType location) {
        List<ItemStack> tanks = tanksByLocation.get(location);
        return tanks != null ? new ArrayList<>(tanks) : Collections.emptyList();
    }
    
    /**
     * Checks if any tanks were found.
     */
    public boolean hasTanks() {
        return !allTanks.isEmpty();
    }
    
    /**
     * Checks if tanks were found in a specific location.
     */
    public boolean hasTanksInLocation(InventoryType location) {
        return getTankCount(location) > 0;
    }
    
    /**
     * Gets the remaining total capacity across all tanks.
     */
    public int getTotalRemainingCapacity() {
        return totalCapacity - totalStored;
    }
    
    /**
     * Gets the overall fill percentage (0.0 to 1.0).
     */
    public double getOverallFillPercentage() {
        return totalCapacity > 0 ? (double) totalStored / totalCapacity : 0.0;
    }
    
    /**
     * Gets a summary of tanks by location for display purposes.
     */
    public Map<String, Integer> getLocationSummary() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        
        for (InventoryType type : InventoryType.values()) {
            int count = getTankCount(type);
            if (count > 0) {
                summary.put(type.getDisplayName(), count);
            }
        }
        
        return summary;
    }
    
    // Private helper methods
    
    /**
     * Calculates the total capacity of all tanks.
     */
    private int calculateTotalCapacity() {
        int total = 0;
        
        for (ItemStack tank : allTanks) {
            if (tank.isEmpty()) continue;
            
            // Get capacity from the tank's capability or NBT data
            int capacity = getTankCapacity(tank);
            total += capacity;
        }
        
        return total;
    }
    
    /**
     * Calculates the total stored experience across all tanks.
     */
    private int calculateTotalStored() {
        int total = 0;
        
        for (ItemStack tank : allTanks) {
            if (tank.isEmpty()) continue;
            
            // Get stored XP from the tank's capability or NBT data
            int stored = getTankStoredXP(tank);
            total += stored;
        }
        
        return total;
    }
    
    /**
     * Gets the capacity of a single tank.
     * Uses the existing ItemExperiencePump methods for compatibility.
     */
    private int getTankCapacity(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof com.moremod.item.ItemExperiencePump)) {
            return 0;
        }
        
        return com.moremod.item.ItemExperiencePump.getMaxXpFromNBT(tank);
    }
    
    /**
     * Gets the stored XP of a single tank.
     * Uses the existing ItemExperiencePump methods for compatibility.
     */
    private int getTankStoredXP(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof com.moremod.item.ItemExperiencePump)) {
            return 0;
        }
        
        return com.moremod.item.ItemExperiencePump.getXpStoredFromNBT(tank);
    }
    
    @Override
    public String toString() {
        return String.format("TankScanResult{tanks=%d, totalCapacity=%d, totalStored=%d, locations=%s, timestamp=%d}",
                           allTanks.size(), totalCapacity, totalStored, 
                           tanksByLocation.keySet(), scanTimestamp);
    }
    
    /**
     * Builder class for creating TankScanResult instances.
     */
    public static class Builder {
        private final List<ItemStack> allTanks = new ArrayList<>();
        private final Map<InventoryType, List<ItemStack>> tanksByLocation = new EnumMap<>(InventoryType.class);
        
        public Builder addTank(ItemStack tank, InventoryType location) {
            if (tank != null && !tank.isEmpty()) {
                allTanks.add(tank);
                tanksByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(tank);
            }
            return this;
        }
        
        public Builder addTanks(List<ItemStack> tanks, InventoryType location) {
            if (tanks != null) {
                for (ItemStack tank : tanks) {
                    addTank(tank, location);
                }
            }
            return this;
        }
        
        public TankScanResult build() {
            return new TankScanResult(allTanks, tanksByLocation);
        }
    }
}