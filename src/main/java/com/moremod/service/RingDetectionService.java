package com.moremod.service;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Centralized ring detection service that provides unified ring detection logic
 * across all inventory locations with robust error handling and comprehensive logging.
 * 
 * Search priority: main hand → off hand → Baubles slots → inventory
 */
public class RingDetectionService {
    private static final Logger LOGGER = LogManager.getLogger(RingDetectionService.class);
    
    /**
     * Finds any ring type in the player's inventory locations.
     * Searches in priority order: main hand → off hand → Baubles → inventory
     * 
     * @param player The player to search
     * @return The first ring found, or ItemStack.EMPTY if none found
     */
    public static ItemStack findAnyRing(EntityPlayer player) {
        if (player == null) {
            LOGGER.debug("Player is null, cannot search for rings");
            return ItemStack.EMPTY;
        }
        
        LOGGER.debug("Starting ring search for player: {}", player.getName());
        
        // Try to find any ring type by checking common ring classes
        // This approach allows for extensibility with new ring types
        // 移除对ItemRsRing的引用，因为RS戒指已被删除
        ItemStack result = ItemStack.EMPTY;
        if (!result.isEmpty()) {
            return result;
        }
        
        result = findRing(player, com.moremod.item.ItemChestRing.class);
        if (!result.isEmpty()) {
            return result;
        }
        
        LOGGER.debug("No rings found for player: {}", player.getName());
        return ItemStack.EMPTY;
    }
    
    /**
     * Finds a specific ring type in the player's inventory locations.
     * Searches in priority order: main hand → off hand → Baubles → inventory
     * 
     * @param player The player to search
     * @param ringClass The specific ring class to search for
     * @return The ring if found, or ItemStack.EMPTY if not found
     */
    public static ItemStack findRing(EntityPlayer player, Class<? extends Item> ringClass) {
        if (player == null) {
            LOGGER.debug("Player is null, cannot search for ring of type: {}", 
                ringClass != null ? ringClass.getSimpleName() : "null");
            return ItemStack.EMPTY;
        }
        
        if (ringClass == null) {
            LOGGER.debug("Ring class is null, cannot search for player: {}", player.getName());
            return ItemStack.EMPTY;
        }
        
        LOGGER.debug("Searching for ring type {} for player: {}", 
            ringClass.getSimpleName(), player.getName());
        
        // Search in priority order
        ItemStack result;
        
        // 1. Check hands (main hand → off hand)
        result = findInHands(player, ringClass);
        if (!result.isEmpty()) {
            logSearchResult("hands", true, result);
            return result;
        }
        
        // 2. Check Baubles slots
        result = findInBaubles(player, ringClass);
        if (!result.isEmpty()) {
            logSearchResult("Baubles", true, result);
            return result;
        }
        
        // 3. Check inventory
        result = findInInventory(player, ringClass);
        if (!result.isEmpty()) {
            logSearchResult("inventory", true, result);
            return result;
        }
        
        LOGGER.debug("Ring type {} not found for player: {}", 
            ringClass.getSimpleName(), player.getName());
        return ItemStack.EMPTY;
    }
    
    /**
     * Searches for rings in the player's hands (main hand and off hand).
     * 
     * @param player The player to search
     * @param ringClass The ring class to search for
     * @return The ring if found in hands, or ItemStack.EMPTY
     */
    private static ItemStack findInHands(EntityPlayer player, Class<? extends Item> ringClass) {
        LOGGER.debug("Searching hands for ring type: {}", ringClass.getSimpleName());
        
        // Check main hand first
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && ringClass.isInstance(mainHand.getItem())) {
            LOGGER.debug("Found ring in main hand: {}", mainHand.getItem().getClass().getSimpleName());
            return mainHand;
        }
        
        // Check off hand
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && ringClass.isInstance(offHand.getItem())) {
            LOGGER.debug("Found ring in off hand: {}", offHand.getItem().getClass().getSimpleName());
            return offHand;
        }
        
        LOGGER.debug("No ring found in hands");
        return ItemStack.EMPTY;
    }
    
    /**
     * Searches for rings in Baubles slots using the BaublesIntegration helper.
     * 
     * @param player The player to search
     * @param ringClass The ring class to search for
     * @return The ring if found in Baubles, or ItemStack.EMPTY
     */
    private static ItemStack findInBaubles(EntityPlayer player, Class<? extends Item> ringClass) {
        return BaublesIntegration.findRingInBaubles(player, ringClass);
    }
    
    /**
     * Searches for rings in the player's main inventory.
     * 
     * @param player The player to search
     * @param ringClass The ring class to search for
     * @return The ring if found in inventory, or ItemStack.EMPTY
     */
    private static ItemStack findInInventory(EntityPlayer player, Class<? extends Item> ringClass) {
        LOGGER.debug("Searching inventory for ring type: {}", ringClass.getSimpleName());
        
        return searchInventoryForRing(player.inventory, ringClass);
    }
    
    /**
     * Helper method to search through an inventory for a specific ring type.
     * 
     * @param inventory The inventory to search
     * @param ringClass The ring class to search for
     * @return The ring if found, or ItemStack.EMPTY
     */
    private static ItemStack searchInventoryForRing(IInventory inventory, Class<? extends Item> ringClass) {
        if (inventory == null) {
            LOGGER.debug("Inventory is null, cannot search");
            return ItemStack.EMPTY;
        }
        
        int slots = inventory.getSizeInventory();
        LOGGER.debug("Searching inventory with {} slots", slots);
        
        for (int i = 0; i < slots; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) {
                LOGGER.debug("Found ring in slot {}: {}", i, stack.getItem().getClass().getSimpleName());
                return stack;
            }
        }
        
        LOGGER.debug("No ring found in inventory");
        return ItemStack.EMPTY;
    }
    
    /**
     * Logs the result of a ring search operation.
     * 
     * @param location The location where the search was performed
     * @param found Whether a ring was found
     * @param result The ring that was found (if any)
     */
    private static void logSearchResult(String location, boolean found, ItemStack result) {
        if (found && !result.isEmpty()) {
            LOGGER.debug("Ring search successful - Location: {}, Ring: {}", 
                location, result.getItem().getClass().getSimpleName());
        } else {
            LOGGER.debug("Ring search failed - Location: {}", location);
        }
    }
    
    /**
     * Marks the Baubles inventory as dirty to ensure proper synchronization.
     * This is a public method that delegates to the BaublesIntegration helper.
     * 
     * @param player The player whose Baubles inventory should be marked dirty
     */
    public static void markBaublesDirtyIfNeeded(EntityPlayer player) {
        BaublesIntegration.markBaublesDirtyIfNeeded(player);
    }
    
    /**
     * Checks if the Baubles mod is available and the API is accessible.
     * This is a public method that delegates to the BaublesIntegration helper.
     * 
     * @return true if Baubles is available and API is accessible, false otherwise
     */
    public static boolean isBaublesAvailable() {
        return BaublesIntegration.isBaublesAvailable();
    }
    
    /**
     * Helper class for robust Baubles API integration with enhanced error handling.
     * Encapsulates all Baubles-specific logic and provides graceful fallback when
     * Baubles mod is not present or API calls fail.
     */
    private static class BaublesIntegration {
        private static final Logger LOGGER = LogManager.getLogger(BaublesIntegration.class);
        
        /**
         * Finds a ring in Baubles slots with comprehensive error handling.
         * 
         * @param player The player to search
         * @param ringClass The ring class to search for
         * @return The ring if found in Baubles, or ItemStack.EMPTY
         */
        public static ItemStack findRingInBaubles(EntityPlayer player, Class<? extends Item> ringClass) {
            if (player == null) {
                LOGGER.debug("Player is null, cannot search Baubles");
                return ItemStack.EMPTY;
            }
            
            if (ringClass == null) {
                LOGGER.debug("Ring class is null, cannot search Baubles");
                return ItemStack.EMPTY;
            }
            
            LOGGER.debug("Searching Baubles for ring type: {}", ringClass.getSimpleName());
            
            if (!Loader.isModLoaded("baubles")) {
                LOGGER.debug("Baubles mod not loaded, skipping Baubles search");
                return ItemStack.EMPTY;
            }
            
            try {
                // Use reflection to access Baubles API
                Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                                       .invoke(null, player);
                
                if (handler instanceof IInventory) {
                    IInventory baubles = (IInventory) handler;
                    LOGGER.debug("Successfully accessed Baubles inventory with {} slots", 
                        baubles.getSizeInventory());
                    
                    return searchInventoryForRing(baubles, ringClass);
                } else {
                    LOGGER.debug("Baubles handler is not an IInventory: {}", 
                        handler != null ? handler.getClass().getName() : "null");
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error("Baubles API class not found - mod may be outdated", e);
            } catch (NoSuchMethodException e) {
                LOGGER.error("Baubles API method not found - incompatible version", e);
            } catch (IllegalAccessException e) {
                LOGGER.error("Failed to access Baubles API - security restriction", e);
            } catch (java.lang.reflect.InvocationTargetException e) {
                LOGGER.error("Baubles API method invocation failed", e);
                if (e.getCause() != null) {
                    LOGGER.error("Root cause: {}", e.getCause().getMessage());
                }
            } catch (Exception e) {
                LOGGER.error("Unexpected error accessing Baubles inventory", e);
            }
            
            return ItemStack.EMPTY;
        }
        
        /**
         * Marks the Baubles inventory as dirty to ensure proper synchronization.
         * This method handles errors gracefully and logs failures without interrupting operation.
         * 
         * @param player The player whose Baubles inventory should be marked dirty
         */
        public static void markBaublesDirtyIfNeeded(EntityPlayer player) {
            if (player == null) {
                LOGGER.debug("Player is null, cannot mark Baubles dirty");
                return;
            }
            
            if (!Loader.isModLoaded("baubles")) {
                LOGGER.debug("Baubles mod not loaded, skipping dirty marking");
                return;
            }
            
            try {
                // Use reflection to access Baubles API
                Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                                       .invoke(null, player);
                
                if (handler instanceof IInventory) {
                    IInventory baubles = (IInventory) handler;
                    baubles.markDirty();
                    LOGGER.debug("Successfully marked Baubles inventory as dirty for player: {}", 
                        player.getName());
                } else {
                    LOGGER.debug("Baubles handler is not an IInventory, cannot mark dirty: {}", 
                        handler != null ? handler.getClass().getName() : "null");
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error("Baubles API class not found - cannot mark dirty", e);
            } catch (NoSuchMethodException e) {
                LOGGER.error("Baubles API method not found - cannot mark dirty", e);
            } catch (IllegalAccessException e) {
                LOGGER.error("Failed to access Baubles API - cannot mark dirty", e);
            } catch (java.lang.reflect.InvocationTargetException e) {
                LOGGER.error("Baubles API method invocation failed - cannot mark dirty", e);
                if (e.getCause() != null) {
                    LOGGER.error("Root cause: {}", e.getCause().getMessage());
                }
            } catch (Exception e) {
                LOGGER.error("Unexpected error marking Baubles inventory dirty", e);
            }
        }
        
        /**
         * Checks if the Baubles mod is available and the API is accessible.
         * 
         * @return true if Baubles is available and API is accessible, false otherwise
         */
        public static boolean isBaublesAvailable() {
            if (!Loader.isModLoaded("baubles")) {
                return false;
            }
            
            try {
                Class.forName("baubles.api.BaublesApi");
                return true;
            } catch (ClassNotFoundException e) {
                LOGGER.debug("Baubles API class not found despite mod being loaded");
                return false;
            }
        }
    }
}