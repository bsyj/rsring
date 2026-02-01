package com.moremod.experience;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Custom event fired when inventory changes occur that affect experience tanks or rings.
 * Supports Requirements 7.1, 7.2, 7.3 for cross-inventory integration and change detection.
 */
public class InventoryChangeEvent extends Event {
    
    /**
     * Type of inventory change that occurred.
     */
    public enum ChangeType {
        TANK_ADDED("Tank Added"),
        TANK_REMOVED("Tank Removed"),
        TANK_MODIFIED("Tank Modified"),
        RING_ADDED("Ring Added"),
        RING_REMOVED("Ring Removed"),
        RING_MODIFIED("Ring Modified"),
        BAUBLES_CHANGED("Baubles Changed"),
        INVENTORY_REFRESHED("Inventory Refreshed");
        
        private final String displayName;
        
        ChangeType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Location where the change occurred.
     */
    public enum InventoryLocation {
        PLAYER_INVENTORY("Player Inventory"),
        HOTBAR("Hotbar"),
        BAUBLES("Baubles Slots"),
        OFFHAND("Off Hand"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        InventoryLocation(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final EntityPlayer player;
    private final ChangeType changeType;
    private final InventoryLocation location;
    private final ItemStack affectedItem;
    private final int slotIndex;
    private final long timestamp;
    
    /**
     * Default constructor required by Minecraft Forge event system.
     * Creates an empty event with default values.
     */
    public InventoryChangeEvent() {
        this.player = null;
        this.changeType = ChangeType.INVENTORY_REFRESHED;
        this.location = InventoryLocation.UNKNOWN;
        this.affectedItem = ItemStack.EMPTY;
        this.slotIndex = -1;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates a new inventory change event.
     */
    public InventoryChangeEvent(EntityPlayer player, ChangeType changeType, 
                               InventoryLocation location, ItemStack affectedItem, int slotIndex) {
        this.player = player;
        this.changeType = changeType;
        this.location = location;
        this.affectedItem = affectedItem != null ? affectedItem.copy() : ItemStack.EMPTY;
        this.slotIndex = slotIndex;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates a new inventory change event without slot information.
     */
    public InventoryChangeEvent(EntityPlayer player, ChangeType changeType, 
                               InventoryLocation location, ItemStack affectedItem) {
        this(player, changeType, location, affectedItem, -1);
    }
    
    /**
     * Creates a new inventory change event for general changes.
     */
    public InventoryChangeEvent(EntityPlayer player, ChangeType changeType, InventoryLocation location) {
        this(player, changeType, location, ItemStack.EMPTY, -1);
    }
    
    // Getters
    
    public EntityPlayer getPlayer() {
        return player;
    }
    
    public ChangeType getChangeType() {
        return changeType;
    }
    
    public InventoryLocation getLocation() {
        return location;
    }
    
    public ItemStack getAffectedItem() {
        return affectedItem.copy();
    }
    
    public int getSlotIndex() {
        return slotIndex;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    // Utility methods
    
    /**
     * Checks if this event affects experience tanks.
     */
    public boolean affectsTanks() {
        return changeType == ChangeType.TANK_ADDED ||
               changeType == ChangeType.TANK_REMOVED ||
               changeType == ChangeType.TANK_MODIFIED ||
               (changeType == ChangeType.INVENTORY_REFRESHED && isExperienceTank(affectedItem)) ||
               (changeType == ChangeType.BAUBLES_CHANGED && isExperienceTank(affectedItem));
    }
    
    /**
     * Checks if this event affects rings.
     */
    public boolean affectsRings() {
        return changeType == ChangeType.RING_ADDED ||
               changeType == ChangeType.RING_REMOVED ||
               changeType == ChangeType.RING_MODIFIED ||
               (changeType == ChangeType.INVENTORY_REFRESHED && isRing(affectedItem)) ||
               (changeType == ChangeType.BAUBLES_CHANGED && isRing(affectedItem));
    }
    
    /**
     * Checks if this event affects Baubles slots.
     */
    public boolean affectsBaubles() {
        return location == InventoryLocation.BAUBLES ||
               changeType == ChangeType.BAUBLES_CHANGED;
    }
    
    /**
     * Checks if the affected item is an experience tank.
     */
    public boolean isAffectedItemTank() {
        return isExperienceTank(affectedItem);
    }
    
    /**
     * Checks if the affected item is a ring.
     */
    public boolean isAffectedItemRing() {
        return isRing(affectedItem);
    }
    
    /**
     * Gets a human-readable description of this event.
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(changeType.getDisplayName());
        
        if (player != null) {
            desc.append(" for player ").append(player.getName());
        }
        
        desc.append(" in ").append(location.getDisplayName());
        
        if (!affectedItem.isEmpty()) {
            desc.append(" (").append(affectedItem.getDisplayName()).append(")");
        }
        
        if (slotIndex >= 0) {
            desc.append(" at slot ").append(slotIndex);
        }
        
        return desc.toString();
    }
    
    // Private helper methods
    
    /**
     * Checks if an item is an experience tank.
     */
    private boolean isExperienceTank(ItemStack item) {
        return !item.isEmpty() && item.getItem() instanceof com.moremod.item.ItemExperiencePump;
    }
    
    /**
     * Checks if an item is a ring.
     */
    private boolean isRing(ItemStack item) {
        if (item.isEmpty()) return false;
        
        // Check for known ring types
        return item.getItem() instanceof com.moremod.item.ItemChestRing;
        // Add other ring types as needed
    }
    
    @Override
    public String toString() {
        return String.format("InventoryChangeEvent{player=%s, type=%s, location=%s, item=%s, slot=%d, timestamp=%d}",
                           player != null ? player.getName() : "null",
                           changeType.name(),
                           location.name(),
                           affectedItem.isEmpty() ? "none" : affectedItem.getDisplayName(),
                           slotIndex,
                           timestamp);
    }
    
    // Static factory methods for common events
    
    /**
     * Creates a tank added event.
     */
    public static InventoryChangeEvent tankAdded(EntityPlayer player, InventoryLocation location, 
                                               ItemStack tank, int slot) {
        return new InventoryChangeEvent(player, ChangeType.TANK_ADDED, location, tank, slot);
    }
    
    /**
     * Creates a tank removed event.
     */
    public static InventoryChangeEvent tankRemoved(EntityPlayer player, InventoryLocation location, 
                                                 ItemStack tank, int slot) {
        return new InventoryChangeEvent(player, ChangeType.TANK_REMOVED, location, tank, slot);
    }
    
    /**
     * Creates a tank modified event.
     */
    public static InventoryChangeEvent tankModified(EntityPlayer player, InventoryLocation location, 
                                                  ItemStack tank, int slot) {
        return new InventoryChangeEvent(player, ChangeType.TANK_MODIFIED, location, tank, slot);
    }
    
    /**
     * Creates a ring added event.
     */
    public static InventoryChangeEvent ringAdded(EntityPlayer player, InventoryLocation location, 
                                               ItemStack ring, int slot) {
        return new InventoryChangeEvent(player, ChangeType.RING_ADDED, location, ring, slot);
    }
    
    /**
     * Creates a ring removed event.
     */
    public static InventoryChangeEvent ringRemoved(EntityPlayer player, InventoryLocation location, 
                                                 ItemStack ring, int slot) {
        return new InventoryChangeEvent(player, ChangeType.RING_REMOVED, location, ring, slot);
    }
    
    /**
     * Creates a ring modified event.
     */
    public static InventoryChangeEvent ringModified(EntityPlayer player, InventoryLocation location, 
                                                  ItemStack ring, int slot) {
        return new InventoryChangeEvent(player, ChangeType.RING_MODIFIED, location, ring, slot);
    }
    
    /**
     * Creates a Baubles changed event.
     */
    public static InventoryChangeEvent baublesChanged(EntityPlayer player, ItemStack item, int slot) {
        return new InventoryChangeEvent(player, ChangeType.BAUBLES_CHANGED, InventoryLocation.BAUBLES, item, slot);
    }
    
    /**
     * Creates an inventory refreshed event.
     */
    public static InventoryChangeEvent inventoryRefreshed(EntityPlayer player, InventoryLocation location) {
        return new InventoryChangeEvent(player, ChangeType.INVENTORY_REFRESHED, location);
    }
}