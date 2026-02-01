package com.moremod.experience;

import net.minecraft.item.ItemStack;
import java.util.*;

/**
 * Result of ring detection across different inventory locations.
 * Provides comprehensive information about detected rings for the Ring Detection System.
 * 
 * Supports Requirements 4.1, 4.2, 4.3, 4.5 for comprehensive ring detection and selection.
 */
public class RingDetectionResult {
    
    /**
     * Enumeration of inventory locations where rings can be found.
     */
    public enum InventoryLocation {
        MAIN_HAND("Main Hand", 1),
        OFF_HAND("Off Hand", 2),
        BAUBLES_RING("Baubles Ring Slot", 3),
        BAUBLES_OTHER("Baubles Other Slot", 4),
        HOTBAR("Hotbar", 5),
        PLAYER_INVENTORY("Player Inventory", 6);
        
        private final String displayName;
        private final int priority; // Lower number = higher priority
        
        InventoryLocation(String displayName, int priority) {
            this.displayName = displayName;
            this.priority = priority;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getPriority() {
            return priority;
        }
    }
    
    private final List<ItemStack> foundRings;                    // All detected rings
    private final Map<String, ItemStack> ringsByType;           // Rings categorized by type
    private final ItemStack primaryRing;                        // Selected primary ring
    private final InventoryLocation primaryLocation;            // Where primary ring was found
    private final Map<InventoryLocation, List<ItemStack>> ringsByLocation; // Rings by location
    private final long detectionTimestamp;                      // When detection was performed
    
    /**
     * Creates a new ring detection result.
     */
    public RingDetectionResult(List<ItemStack> foundRings, 
                              Map<InventoryLocation, List<ItemStack>> ringsByLocation) {
        this.foundRings = new ArrayList<>(foundRings != null ? foundRings : Collections.emptyList());
        this.ringsByLocation = new EnumMap<>(InventoryLocation.class);
        
        // Copy the rings by location map
        if (ringsByLocation != null) {
            for (Map.Entry<InventoryLocation, List<ItemStack>> entry : ringsByLocation.entrySet()) {
                this.ringsByLocation.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        
        // Build rings by type map
        this.ringsByType = buildRingsByType();
        
        // Determine primary ring based on priority
        this.primaryRing = determinePrimaryRing();
        this.primaryLocation = determinePrimaryLocation();
        
        this.detectionTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates an empty detection result.
     */
    public static RingDetectionResult empty() {
        return new RingDetectionResult(Collections.emptyList(), Collections.emptyMap());
    }
    
    // Getters
    
    public List<ItemStack> getFoundRings() {
        return new ArrayList<>(foundRings);
    }
    
    public Map<String, ItemStack> getRingsByType() {
        return new HashMap<>(ringsByType);
    }
    
    public ItemStack getPrimaryRing() {
        return primaryRing;
    }
    
    public InventoryLocation getPrimaryLocation() {
        return primaryLocation;
    }
    
    public Map<InventoryLocation, List<ItemStack>> getRingsByLocation() {
        Map<InventoryLocation, List<ItemStack>> result = new EnumMap<>(InventoryLocation.class);
        for (Map.Entry<InventoryLocation, List<ItemStack>> entry : ringsByLocation.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
    
    public long getDetectionTimestamp() {
        return detectionTimestamp;
    }
    
    // Utility methods
    
    /**
     * Checks if any rings were found.
     */
    public boolean hasRings() {
        return !foundRings.isEmpty();
    }
    
    /**
     * Gets the number of rings found.
     */
    public int getRingCount() {
        return foundRings.size();
    }
    
    /**
     * Gets the number of rings in a specific location.
     */
    public int getRingCount(InventoryLocation location) {
        List<ItemStack> rings = ringsByLocation.get(location);
        return rings != null ? rings.size() : 0;
    }
    
    /**
     * Checks if rings were found in a specific location.
     */
    public boolean hasRingsInLocation(InventoryLocation location) {
        return getRingCount(location) > 0;
    }
    
    /**
     * Gets rings from a specific location.
     */
    public List<ItemStack> getRingsFromLocation(InventoryLocation location) {
        List<ItemStack> rings = ringsByLocation.get(location);
        return rings != null ? new ArrayList<>(rings) : Collections.emptyList();
    }
    
    /**
     * Checks if a specific ring type was found.
     */
    public boolean hasRingType(String ringType) {
        return ringsByType.containsKey(ringType);
    }
    
    /**
     * Gets a ring of a specific type.
     */
    public ItemStack getRingOfType(String ringType) {
        return ringsByType.get(ringType);
    }
    
    /**
     * Gets all unique ring types found.
     */
    public Set<String> getRingTypes() {
        return new HashSet<>(ringsByType.keySet());
    }
    
    /**
     * Gets a summary of rings by location for display purposes.
     */
    public Map<String, Integer> getLocationSummary() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        
        // Sort locations by priority
        List<InventoryLocation> sortedLocations = new ArrayList<>(ringsByLocation.keySet());
        sortedLocations.sort(Comparator.comparingInt(InventoryLocation::getPriority));
        
        for (InventoryLocation location : sortedLocations) {
            int count = getRingCount(location);
            if (count > 0) {
                summary.put(location.getDisplayName(), count);
            }
        }
        
        return summary;
    }
    
    /**
     * Gets a summary of rings by type for display purposes.
     */
    public Map<String, Integer> getTypeSummary() {
        Map<String, Integer> summary = new HashMap<>();
        
        for (String type : ringsByType.keySet()) {
            // Count how many rings of this type we have
            int count = 0;
            for (ItemStack ring : foundRings) {
                if (getRingType(ring).equals(type)) {
                    count++;
                }
            }
            summary.put(type, count);
        }
        
        return summary;
    }
    
    // Private helper methods
    
    /**
     * Builds the rings by type map.
     */
    private Map<String, ItemStack> buildRingsByType() {
        Map<String, ItemStack> byType = new HashMap<>();
        
        for (ItemStack ring : foundRings) {
            String type = getRingType(ring);
            if (!byType.containsKey(type)) {
                byType.put(type, ring);
            }
        }
        
        return byType;
    }
    
    /**
     * Determines the primary ring based on location priority.
     */
    private ItemStack determinePrimaryRing() {
        if (foundRings.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        // Find the ring with the highest priority location
        ItemStack primary = ItemStack.EMPTY;
        int highestPriority = Integer.MAX_VALUE;
        
        for (Map.Entry<InventoryLocation, List<ItemStack>> entry : ringsByLocation.entrySet()) {
            InventoryLocation location = entry.getKey();
            List<ItemStack> rings = entry.getValue();
            
            if (!rings.isEmpty() && location.getPriority() < highestPriority) {
                highestPriority = location.getPriority();
                primary = rings.get(0); // Take the first ring from the highest priority location
            }
        }
        
        return primary;
    }
    
    /**
     * Determines the location of the primary ring.
     */
    private InventoryLocation determinePrimaryLocation() {
        if (primaryRing.isEmpty()) {
            return null;
        }
        
        // Find which location contains the primary ring
        for (Map.Entry<InventoryLocation, List<ItemStack>> entry : ringsByLocation.entrySet()) {
            if (entry.getValue().contains(primaryRing)) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Gets the type name of a ring.
     */
    private String getRingType(ItemStack ring) {
        if (ring.isEmpty()) {
            return "Unknown";
        }
        
        String className = ring.getItem().getClass().getSimpleName();
        
        // Convert class name to readable type name
        if (className.startsWith("Item")) {
            className = className.substring(4); // Remove "Item" prefix
        }
        
        return className;
    }
    
    @Override
    public String toString() {
        return String.format("RingDetectionResult{rings=%d, types=%s, primary=%s, location=%s, timestamp=%d}",
                           foundRings.size(), ringsByType.keySet(), 
                           primaryRing.isEmpty() ? "none" : getRingType(primaryRing),
                           primaryLocation != null ? primaryLocation.getDisplayName() : "none",
                           detectionTimestamp);
    }
    
    /**
     * Builder class for creating RingDetectionResult instances.
     */
    public static class Builder {
        private final List<ItemStack> foundRings = new ArrayList<>();
        private final Map<InventoryLocation, List<ItemStack>> ringsByLocation = new EnumMap<>(InventoryLocation.class);
        
        public Builder addRing(ItemStack ring, InventoryLocation location) {
            if (ring != null && !ring.isEmpty()) {
                foundRings.add(ring);
                ringsByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(ring);
            }
            return this;
        }
        
        public Builder addRings(List<ItemStack> rings, InventoryLocation location) {
            if (rings != null) {
                for (ItemStack ring : rings) {
                    addRing(ring, location);
                }
            }
            return this;
        }
        
        public RingDetectionResult build() {
            return new RingDetectionResult(foundRings, ringsByLocation);
        }
    }
}