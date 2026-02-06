package com.rsring.experience;

import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * Result of ring detection operation
 */
public class RingDetectionResult {
    private final boolean found;
    private final ItemStack ringStack;
    private final InventoryLocation location;
    private final int slot;
    private final long detectionTimestamp;
    private final Map<InventoryLocation, List<ItemStack>> ringsByLocation;
    private final List<ItemStack> allRings;

    private RingDetectionResult(boolean found, ItemStack ringStack, InventoryLocation location, int slot,
                                Map<InventoryLocation, List<ItemStack>> ringsByLocation, List<ItemStack> allRings) {
        this.found = found;
        this.ringStack = ringStack != null ? ringStack : ItemStack.EMPTY;
        this.location = location != null ? location : InventoryLocation.UNKNOWN;
        this.slot = slot;
        this.detectionTimestamp = System.currentTimeMillis();
        this.ringsByLocation = ringsByLocation != null ? new EnumMap<>(ringsByLocation) : new EnumMap<>(InventoryLocation.class);
        this.allRings = allRings != null ? new ArrayList<>(allRings) : new ArrayList<>();
    }

    public static RingDetectionResult found(ItemStack ring, InventoryLocation location, int slot) {
        Map<InventoryLocation, List<ItemStack>> map = new EnumMap<>(InventoryLocation.class);
        List<ItemStack> list = new ArrayList<>();
        if (!ring.isEmpty()) {
            map.computeIfAbsent(location, k -> new ArrayList<>()).add(ring);
            list.add(ring);
        }
        return new RingDetectionResult(true, ring, location, slot, map, list);
    }

    public static RingDetectionResult notFound() {
        return new RingDetectionResult(false, ItemStack.EMPTY, InventoryLocation.UNKNOWN, -1, 
            new EnumMap<>(InventoryLocation.class), new ArrayList<>());
    }

    public static RingDetectionResult empty() {
        return notFound();
    }

    public boolean isFound() {
        return found;
    }

    public boolean hasRings() {
        return !allRings.isEmpty();
    }

    public int getRingCount() {
        return allRings.size();
    }

    public ItemStack getRingStack() {
        return ringStack;
    }

    public ItemStack getPrimaryRing() {
        return allRings.isEmpty() ? ItemStack.EMPTY : allRings.get(0);
    }

    public InventoryLocation getLocation() {
        return location;
    }

    public InventoryLocation getPrimaryLocation() {
        return location;
    }

    public int getSlot() {
        return slot;
    }

    public long getDetectionTimestamp() {
        return detectionTimestamp;
    }

    public Map<InventoryLocation, List<ItemStack>> getRingsByLocation() {
        return new EnumMap<>(ringsByLocation);
    }

    public List<ItemStack> getAllRings() {
        return new ArrayList<>(allRings);
    }

    public List<ItemStack> getRingsFromLocation(InventoryLocation location) {
        return ringsByLocation.getOrDefault(location, new ArrayList<>());
    }

    public List<ItemStack> getFoundRings() {
        return new ArrayList<>(allRings);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for RingDetectionResult
     */
    public static class Builder {
        private final Map<InventoryLocation, List<ItemStack>> ringsByLocation = new EnumMap<>(InventoryLocation.class);
        private final List<ItemStack> allRings = new ArrayList<>();

        public Builder addRing(ItemStack ring, InventoryLocation location) {
            if (!ring.isEmpty()) {
                ringsByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(ring);
                allRings.add(ring);
            }
            return this;
        }

        public RingDetectionResult build() {
            ItemStack primary = allRings.isEmpty() ? ItemStack.EMPTY : allRings.get(0);
            InventoryLocation primaryLoc = ringsByLocation.isEmpty() ? InventoryLocation.UNKNOWN : 
                ringsByLocation.keySet().iterator().next();
            return new RingDetectionResult(!allRings.isEmpty(), primary, primaryLoc, -1, 
                ringsByLocation, allRings);
        }
    }

    /**
     * Inventory location types
     */
    public enum InventoryLocation {
        MAIN_HAND("主手"),
        OFF_HAND("副手"),
        HOTBAR("快捷栏"),
        PLAYER_INVENTORY("背包"),
        BAUBLES_RING("饰品栏"),
        UNKNOWN("未知");

        private final String displayName;

        InventoryLocation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
