package com.moremod.experience;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles inventory change detection and event firing for experience tanks and rings.
 * Monitors player inventory, hotbar, and Baubles slots for changes.
 * 
 * Supports Requirements 7.1, 7.2, 7.3 for inventory change detection and cross-inventory integration.
 */
public class InventoryChangeHandler {
    
    private static final Logger LOGGER = LogManager.getLogger(InventoryChangeHandler.class);
    
    // Singleton instance
    private static InventoryChangeHandler instance;
    
    // Player inventory snapshots for change detection
    private final Map<UUID, InventorySnapshot> playerSnapshots = new ConcurrentHashMap<>();
    
    // Tick counter for periodic checks
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10; // Check every 10 ticks (0.5 seconds)
    
    /**
     * Private constructor for singleton pattern.
     */
    private InventoryChangeHandler() {
        // Register this handler with the event bus
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * Gets the singleton instance.
     */
    public static InventoryChangeHandler getInstance() {
        if (instance == null) {
            instance = new InventoryChangeHandler();
        }
        return instance;
    }
    
    /**
     * Initializes the inventory change handler.
     * Should be called during mod initialization.
     */
    public static void initialize() {
        getInstance(); // Ensure instance is created and registered
        LOGGER.info("Inventory change handler initialized");
    }
    
    /**
     * Handles player tick events to monitor inventory changes.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        
        // Only check periodically to avoid performance issues
        if (++tickCounter % CHECK_INTERVAL != 0) {
            return;
        }
        
        checkPlayerInventoryChanges(event.player);
    }
    
    /**
     * Handles item pickup events to trigger inventory refresh.
     * Requirements 7.1 - Inventory change detection for item pickups.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onItemPickup(net.minecraftforge.event.entity.item.ItemTossEvent event) {
        if (event.getPlayer() == null || event.getPlayer().world.isRemote) return;
        
        ItemStack tossedItem = event.getEntityItem().getItem();
        if (isRelevantItem(tossedItem)) {
            LOGGER.debug("Player tossed relevant item: {}", tossedItem.getDisplayName());
            
            // Schedule immediate inventory check on next tick
            scheduleInventoryRefresh(event.getPlayer());
        }
    }
    
    /**
     * Handles container close events to check for inventory changes.
     * Requirements 7.1 - Inventory change detection when using containers/GUIs.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onContainerClosed(net.minecraftforge.event.entity.player.PlayerContainerEvent.Close event) {
        if (event.getEntityPlayer().world.isRemote) return;
        
        EntityPlayer player = event.getEntityPlayer();
        LOGGER.debug("Container closed for player: {}", player.getName());
        
        // Schedule inventory refresh after container interaction
        scheduleInventoryRefresh(player);
    }
    
    /**
     * Handles crafting events to detect tank upgrades and other relevant changes.
     * Requirements 7.1 - Inventory change detection during crafting.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.player.world.isRemote) return;
        
        ItemStack craftedItem = event.crafting;
        if (isRelevantItem(craftedItem)) {
            LOGGER.debug("Player crafted relevant item: {}", craftedItem.getDisplayName());
            
            // Force immediate inventory refresh for crafting events
            forceFullInventoryRefresh(event.player);
        }
    }
    
    /**
     * Manually triggers an inventory refresh for a player.
     * Useful when external code knows an inventory change has occurred.
     */
    public void refreshPlayerInventory(EntityPlayer player) {
        if (player == null) return;
        
        LOGGER.debug("Manually refreshing inventory for player: {}", player.getName());
        checkPlayerInventoryChanges(player);
        
        // Fire a general refresh event
        InventoryChangeEvent refreshEvent = InventoryChangeEvent.inventoryRefreshed(
            player, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY);
        MinecraftForge.EVENT_BUS.post(refreshEvent);
    }
    
    /**
     * Manually triggers a Baubles inventory refresh for a player.
     */
    public void refreshBaublesInventory(EntityPlayer player) {
        if (player == null) return;
        
        LOGGER.debug("Manually refreshing Baubles inventory for player: {}", player.getName());
        
        // Fire a Baubles refresh event
        InventoryChangeEvent refreshEvent = InventoryChangeEvent.inventoryRefreshed(
            player, InventoryChangeEvent.InventoryLocation.BAUBLES);
        MinecraftForge.EVENT_BUS.post(refreshEvent);
        
        // Force a full inventory check
        checkPlayerInventoryChanges(player);
    }
    
    /**
     * Schedules an inventory refresh for the next tick.
     * Requirements 7.3 - Immediate state refresh mechanisms.
     */
    public void scheduleInventoryRefresh(EntityPlayer player) {
        if (player == null) return;
        
        // Reset tick counter to force immediate check on next tick
        tickCounter = CHECK_INTERVAL - 1;
        
        LOGGER.debug("Scheduled immediate inventory refresh for player: {}", player.getName());
    }
    
    /**
     * Forces an immediate comprehensive inventory refresh.
     * Requirements 7.1, 7.2, 7.3 - Complete state refresh across all inventory types.
     */
    public void forceFullInventoryRefresh(EntityPlayer player) {
        if (player == null) return;
        
        LOGGER.debug("Forcing full inventory refresh for player: {}", player.getName());
        
        // Clear existing snapshot to force complete re-scan
        UUID playerId = player.getUniqueID();
        playerSnapshots.remove(playerId);
        
        // Perform immediate inventory check
        checkPlayerInventoryChanges(player);
        
        // Fire comprehensive refresh events
        InventoryChangeEvent playerRefresh = InventoryChangeEvent.inventoryRefreshed(
            player, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY);
        MinecraftForge.EVENT_BUS.post(playerRefresh);
        
        InventoryChangeEvent hotbarRefresh = InventoryChangeEvent.inventoryRefreshed(
            player, InventoryChangeEvent.InventoryLocation.HOTBAR);
        MinecraftForge.EVENT_BUS.post(hotbarRefresh);
        
        InventoryChangeEvent baublesRefresh = InventoryChangeEvent.inventoryRefreshed(
            player, InventoryChangeEvent.InventoryLocation.BAUBLES);
        MinecraftForge.EVENT_BUS.post(baublesRefresh);
    }
    
    /**
     * Refreshes inventory state for all online players.
     * Useful for mod initialization or configuration changes.
     */
    public void refreshAllPlayersInventory() {
        LOGGER.debug("Refreshing inventory for all online players");
        
        // This would need to be called from a context where we have access to all players
        // For now, we'll just clear all snapshots to force refresh on next tick
        playerSnapshots.clear();
    }
    
    /**
     * Checks for inventory changes for a specific player.
     */
    private void checkPlayerInventoryChanges(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        InventorySnapshot currentSnapshot = createInventorySnapshot(player);
        InventorySnapshot previousSnapshot = playerSnapshots.get(playerId);
        
        if (previousSnapshot == null) {
            // First time seeing this player, just store the snapshot
            playerSnapshots.put(playerId, currentSnapshot);
            return;
        }
        
        // Enhanced Baubles monitoring before general comparison
        monitorBaublesChanges(player);
        
        // Compare snapshots and fire events for changes
        compareAndFireEvents(player, previousSnapshot, currentSnapshot);
        
        // Update stored snapshot
        playerSnapshots.put(playerId, currentSnapshot);
    }
    
    /**
     * Creates a snapshot of a player's inventory state.
     */
    private InventorySnapshot createInventorySnapshot(EntityPlayer player) {
        InventorySnapshot snapshot = new InventorySnapshot();
        
        // Capture player inventory
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isRelevantItem(stack)) {
                snapshot.addItem(InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY, i, stack);
            }
        }
        
        // Capture hotbar (already included in player inventory, but track separately for events)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isRelevantItem(stack)) {
                snapshot.addItem(InventoryChangeEvent.InventoryLocation.HOTBAR, i, stack);
            }
        }
        
        // Capture off-hand
        ItemStack offHand = player.getHeldItemOffhand();
        if (isRelevantItem(offHand)) {
            snapshot.addItem(InventoryChangeEvent.InventoryLocation.OFFHAND, 0, offHand);
        }
        
        // Capture Baubles inventory if available
        captureBaublesInventory(player, snapshot);
        
        return snapshot;
    }
    
    /**
     * Captures Baubles inventory items if the mod is available.
     */
    private void captureBaublesInventory(EntityPlayer player, InventorySnapshot snapshot) {
        if (!Loader.isModLoaded("baubles")) {
            return;
        }
        
        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                                   .invoke(null, player);
            
            if (handler instanceof IInventory) {
                IInventory baubles = (IInventory) handler;
                for (int i = 0; i < baubles.getSizeInventory(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (isRelevantItem(stack)) {
                        snapshot.addItem(InventoryChangeEvent.InventoryLocation.BAUBLES, i, stack);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to capture Baubles inventory: {}", e.getMessage());
        }
    }
    
    /**
     * Enhanced Baubles inventory monitoring with direct API integration.
     * Requirements 7.2 - Baubles slot modification listeners.
     */
    private void monitorBaublesChanges(EntityPlayer player) {
        if (!Loader.isModLoaded("baubles")) {
            return;
        }
        
        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                                   .invoke(null, player);
            
            if (handler instanceof IInventory) {
                IInventory baubles = (IInventory) handler;
                
                // Check if Baubles inventory has changed since last check
                UUID playerId = player.getUniqueID();
                InventorySnapshot currentSnapshot = playerSnapshots.get(playerId);
                
                if (currentSnapshot != null) {
                    boolean baublesChanged = false;
                    
                    for (int i = 0; i < baubles.getSizeInventory(); i++) {
                        ItemStack currentStack = baubles.getStackInSlot(i);
                        String key = InventoryChangeEvent.InventoryLocation.BAUBLES.name() + ":" + i;
                        ItemStack previousStack = currentSnapshot.getItem(key);
                        
                        if (!ItemStack.areItemStacksEqual(currentStack, previousStack)) {
                            baublesChanged = true;
                            
                            // Fire specific Baubles change event
                            if (isRelevantItem(currentStack) || isRelevantItem(previousStack)) {
                                InventoryChangeEvent baublesEvent = InventoryChangeEvent.baublesChanged(
                                    player, currentStack.isEmpty() ? previousStack : currentStack, i);
                                MinecraftForge.EVENT_BUS.post(baublesEvent);
                                
                                LOGGER.debug("Baubles slot {} changed for player {}: {} -> {}", 
                                           i, player.getName(), 
                                           previousStack.isEmpty() ? "empty" : previousStack.getDisplayName(),
                                           currentStack.isEmpty() ? "empty" : currentStack.getDisplayName());
                            }
                        }
                    }
                    
                    if (baublesChanged) {
                        // Fire general Baubles refresh event
                        InventoryChangeEvent refreshEvent = InventoryChangeEvent.inventoryRefreshed(
                            player, InventoryChangeEvent.InventoryLocation.BAUBLES);
                        MinecraftForge.EVENT_BUS.post(refreshEvent);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to monitor Baubles changes: {}", e.getMessage());
        }
    }
    
    /**
     * Compares two inventory snapshots and fires appropriate events.
     */
    private void compareAndFireEvents(EntityPlayer player, InventorySnapshot previous, InventorySnapshot current) {
        // Check for added items
        for (Map.Entry<String, ItemStack> entry : current.getAllItems().entrySet()) {
            String key = entry.getKey();
            ItemStack currentItem = entry.getValue();
            ItemStack previousItem = previous.getItem(key);
            
            if (previousItem.isEmpty()) {
                // Item was added
                InventoryChangeEvent.InventoryLocation location = parseLocationFromKey(key);
                int slot = parseSlotFromKey(key);
                fireItemAddedEvent(player, location, currentItem, slot);
            } else if (!ItemStack.areItemStacksEqual(previousItem, currentItem)) {
                // Item was modified
                InventoryChangeEvent.InventoryLocation location = parseLocationFromKey(key);
                int slot = parseSlotFromKey(key);
                fireItemModifiedEvent(player, location, currentItem, slot);
            }
        }
        
        // Check for removed items
        for (Map.Entry<String, ItemStack> entry : previous.getAllItems().entrySet()) {
            String key = entry.getKey();
            ItemStack previousItem = entry.getValue();
            ItemStack currentItem = current.getItem(key);
            
            if (currentItem.isEmpty()) {
                // Item was removed
                InventoryChangeEvent.InventoryLocation location = parseLocationFromKey(key);
                int slot = parseSlotFromKey(key);
                fireItemRemovedEvent(player, location, previousItem, slot);
            }
        }
    }
    
    /**
     * Fires an item added event.
     */
    private void fireItemAddedEvent(EntityPlayer player, InventoryChangeEvent.InventoryLocation location, ItemStack item, int slot) {
        InventoryChangeEvent event;
        
        if (isExperienceTank(item)) {
            event = InventoryChangeEvent.tankAdded(player, location, item, slot);
        } else if (isRing(item)) {
            event = InventoryChangeEvent.ringAdded(player, location, item, slot);
        } else {
            return; // Not a relevant item
        }
        
        LOGGER.debug("Firing item added event: {}", event.getDescription());
        MinecraftForge.EVENT_BUS.post(event);
    }
    
    /**
     * Fires an item removed event.
     */
    private void fireItemRemovedEvent(EntityPlayer player, InventoryChangeEvent.InventoryLocation location, ItemStack item, int slot) {
        InventoryChangeEvent event;
        
        if (isExperienceTank(item)) {
            event = InventoryChangeEvent.tankRemoved(player, location, item, slot);
        } else if (isRing(item)) {
            event = InventoryChangeEvent.ringRemoved(player, location, item, slot);
        } else {
            return; // Not a relevant item
        }
        
        LOGGER.debug("Firing item removed event: {}", event.getDescription());
        MinecraftForge.EVENT_BUS.post(event);
    }
    
    /**
     * Fires an item modified event.
     */
    private void fireItemModifiedEvent(EntityPlayer player, InventoryChangeEvent.InventoryLocation location, ItemStack item, int slot) {
        InventoryChangeEvent event;
        
        if (isExperienceTank(item)) {
            event = InventoryChangeEvent.tankModified(player, location, item, slot);
        } else if (isRing(item)) {
            event = InventoryChangeEvent.ringModified(player, location, item, slot);
        } else {
            return; // Not a relevant item
        }
        
        LOGGER.debug("Firing item modified event: {}", event.getDescription());
        MinecraftForge.EVENT_BUS.post(event);
    }
    
    // Helper methods
    
    private boolean isRelevantItem(ItemStack item) {
        return isExperienceTank(item) || isRing(item);
    }
    
    private boolean isExperienceTank(ItemStack item) {
        return !item.isEmpty() && item.getItem() instanceof com.moremod.item.ItemExperiencePump;
    }
    
    private boolean isRing(ItemStack item) {
        return !item.isEmpty() && item.getItem() instanceof com.moremod.item.ItemChestRing;
    }
    
    private InventoryChangeEvent.InventoryLocation parseLocationFromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length >= 1) {
            try {
                return InventoryChangeEvent.InventoryLocation.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                return InventoryChangeEvent.InventoryLocation.UNKNOWN;
            }
        }
        return InventoryChangeEvent.InventoryLocation.UNKNOWN;
    }
    
    private int parseSlotFromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
    
    /**
     * Internal class to represent an inventory snapshot.
     */
    private static class InventorySnapshot {
        private final Map<String, ItemStack> items = new HashMap<>();
        
        public void addItem(InventoryChangeEvent.InventoryLocation location, int slot, ItemStack item) {
            if (!item.isEmpty()) {
                String key = location.name() + ":" + slot;
                items.put(key, item.copy());
            }
        }
        
        public ItemStack getItem(String key) {
            return items.getOrDefault(key, ItemStack.EMPTY);
        }
        
        public Map<String, ItemStack> getAllItems() {
            return new HashMap<>(items);
        }
    }
}