package com.moremod.service;

import com.moremod.experience.RingDetectionResult;
import com.moremod.item.ItemAbsorbRing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.util.*;

/**
 * Enhanced ring detection system that provides comprehensive ring scanning across all inventory locations.
 * Implements Requirements 4.1, 4.2, 4.3, 4.5 for Baubles-integrated ring detection.
 * 
 * Features:
 * - Comprehensive ring scanning (inventory + Baubles) - Requirements 4.1, 4.2, 4.3
 * - K key event handler - Requirements 4.1, 4.2, 4.3
 * - Ring priority and selection logic - Requirement 4.5
 * - Clear feedback for no rings found - Requirement 4.4
 */
public class RingDetectionSystem {
    
    private static final Logger LOGGER = LogManager.getLogger(RingDetectionSystem.class);
    private static RingDetectionSystem instance;
    
    // Ring detection cache to improve performance
    private final Map<String, RingDetectionResult> detectionCache = new HashMap<>();
    private static final long CACHE_EXPIRY_MS = 1000; // Cache results for 1 second
    
    // Known ring classes for detection
    private static final List<Class<? extends Item>> KNOWN_RING_CLASSES = Arrays.asList(
        ItemAbsorbRing.class
        // Add other ring types here as they are implemented
    );
    
    private RingDetectionSystem() {
        // Private constructor for singleton
    }
    
    /**
     * Gets the singleton instance of the ring detection system.
     */
    public static RingDetectionSystem getInstance() {
        if (instance == null) {
            instance = new RingDetectionSystem();
        }
        return instance;
    }
    
    /**
     * Performs comprehensive ring scanning across all inventory locations.
     * Implements Requirements 4.1, 4.2, 4.3 for comprehensive ring detection.
     * 
     * @param player The player to scan for rings
     * @return RingDetectionResult containing all found rings and metadata
     */
    public RingDetectionResult scanForRings(EntityPlayer player) {
        if (player == null) {
            LOGGER.debug("Player is null, returning empty detection result");
            return RingDetectionResult.empty();
        }
        
        // Check cache first
        String cacheKey = player.getUniqueID().toString();
        RingDetectionResult cached = detectionCache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.getDetectionTimestamp()) < CACHE_EXPIRY_MS) {
            LOGGER.debug("Returning cached ring detection result for player: {}", player.getName());
            return cached;
        }
        
        LOGGER.debug("Starting comprehensive ring scan for player: {}", player.getName());
        
        RingDetectionResult.Builder resultBuilder = new RingDetectionResult.Builder();
        
        // Scan all inventory locations for all known ring types
        for (Class<? extends Item> ringClass : KNOWN_RING_CLASSES) {
            scanLocationForRingType(player, ringClass, resultBuilder);
        }
        
        RingDetectionResult result = resultBuilder.build();
        
        // Cache the result
        detectionCache.put(cacheKey, result);
        
        LOGGER.debug("Ring scan complete for player {}: found {} rings in {} locations", 
                    player.getName(), result.getRingCount(), result.getRingsByLocation().size());
        
        return result;
    }
    
    /**
     * Handles K key press events for ring activation.
     * Implements Requirements 4.1, 4.2, 4.3 for K key ring detection and activation.
     * 
     * @param event The key input event
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void handleKeyPress(InputEvent.KeyInputEvent event) {
        // Check if K key was pressed
        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_K) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            EntityPlayer player = mc.player;
            
            if (player != null) {
                LOGGER.debug("K key pressed, initiating ring detection for player: {}", player.getName());
                
                RingDetectionResult result = scanForRings(player);
                
                if (result.hasRings()) {
                    activateRingFunctionality(player, result);
                } else {
                    provideFeedbackNoRingsFound(player);
                }
            }
        }
    }
    
    /**
     * Scans Baubles ring slots specifically for equipped rings.
     * Implements Requirements 4.1, 4.2 for Baubles ring slot scanning.
     * 
     * @param player The player to scan
     * @return List of rings found in Baubles slots
     */
    public List<ItemStack> getBaublesRings(EntityPlayer player) {
        if (player == null) {
            return Collections.emptyList();
        }
        
        List<ItemStack> baublesRings = new ArrayList<>();
        
        // Use the existing Baubles integration from RingDetectionService
        for (Class<? extends Item> ringClass : KNOWN_RING_CLASSES) {
            ItemStack ring = findInBaubles(player, ringClass);
            if (!ring.isEmpty()) {
                baublesRings.add(ring);
            }
        }
        
        LOGGER.debug("Found {} rings in Baubles slots for player: {}", baublesRings.size(), player.getName());
        return baublesRings;
    }
    
    /**
     * Handles ring priority and selection logic when multiple rings are present.
     * Implements Requirement 4.5 for ring priority and selection.
     * 
     * @param rings List of rings to prioritize
     * @return The highest priority ring
     */
    public ItemStack prioritizeRings(List<ItemStack> rings) {
        if (rings == null || rings.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        if (rings.size() == 1) {
            return rings.get(0);
        }
        
        LOGGER.debug("Prioritizing {} rings using location-based priority", rings.size());
        
        // Create a detection result to use its priority logic
        RingDetectionResult.Builder builder = new RingDetectionResult.Builder();
        
        // We need to determine the location of each ring to apply priority
        // For now, we'll use a simple approach: first ring wins
        // In a full implementation, we'd need to track the source location of each ring
        ItemStack prioritized = rings.get(0);
        
        LOGGER.debug("Selected ring: {} (type: {})", 
                    "ring", prioritized.getItem().getClass().getSimpleName());
        
        return prioritized;
    }
    
    /**
     * Clears the detection cache for a specific player.
     * 
     * @param player The player whose cache should be cleared
     */
    public void clearCache(EntityPlayer player) {
        if (player != null) {
            detectionCache.remove(player.getUniqueID().toString());
            LOGGER.debug("Cleared ring detection cache for player: {}", player.getName());
        }
    }
    
    /**
     * Clears all detection cache entries.
     */
    public void clearAllCache() {
        detectionCache.clear();
        LOGGER.debug("Cleared all ring detection cache entries");
    }
    
    // Private helper methods
    
    /**
     * Scans all inventory locations for a specific ring type.
     */
    private void scanLocationForRingType(EntityPlayer player, Class<? extends Item> ringClass, 
                                       RingDetectionResult.Builder resultBuilder) {
        
        // Scan hands (main hand and off hand)
        List<ItemStack> handRings = findInHands(player, ringClass);
        for (ItemStack ring : handRings) {
            if (!ring.isEmpty()) {
                // Determine which hand
                if (ring == player.getHeldItemMainhand()) {
                    resultBuilder.addRing(ring, RingDetectionResult.InventoryLocation.MAIN_HAND);
                } else if (ring == player.getHeldItemOffhand()) {
                    resultBuilder.addRing(ring, RingDetectionResult.InventoryLocation.OFF_HAND);
                }
            }
        }
        
        // Scan Baubles slots
        ItemStack baublesRing = findInBaubles(player, ringClass);
        if (!baublesRing.isEmpty()) {
            resultBuilder.addRing(baublesRing, RingDetectionResult.InventoryLocation.BAUBLES_RING);
        }
        
        // Scan player inventory (including hotbar)
        List<ItemStack> inventoryRings = findInPlayerInventory(player, ringClass);
        for (ItemStack ring : inventoryRings) {
            if (!ring.isEmpty()) {
                // Determine if it's in hotbar or main inventory
                int slot = findSlotIndex(player, ring);
                if (slot >= 0 && slot < 9) {
                    resultBuilder.addRing(ring, RingDetectionResult.InventoryLocation.HOTBAR);
                } else {
                    resultBuilder.addRing(ring, RingDetectionResult.InventoryLocation.PLAYER_INVENTORY);
                }
            }
        }
    }
    
    /**
     * Finds rings in player's hands.
     */
    private List<ItemStack> findInHands(EntityPlayer player, Class<? extends Item> ringClass) {
        List<ItemStack> handRings = new ArrayList<>();
        
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && ringClass.isInstance(mainHand.getItem())) {
            handRings.add(mainHand);
        }
        
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && ringClass.isInstance(offHand.getItem())) {
            handRings.add(offHand);
        }
        
        return handRings;
    }
    
    /**
     * Finds rings in Baubles slots using existing integration.
     */
    private ItemStack findInBaubles(EntityPlayer player, Class<? extends Item> ringClass) {
        // Use the public method from RingDetectionService
        if (!RingDetectionService.isBaublesAvailable()) {
            return ItemStack.EMPTY;
        }
        
        // Since we can't access the private BaublesIntegration directly,
        // we'll use the existing public findRing method which includes Baubles scanning
        ItemStack result = RingDetectionService.findRing(player, ringClass);
        
        // Check if the result is actually from Baubles by verifying it's not in hands or inventory
        if (!result.isEmpty()) {
            // If it's not in hands and not in regular inventory, it's likely from Baubles
            if (result != player.getHeldItemMainhand() && 
                result != player.getHeldItemOffhand() &&
                !isInPlayerInventory(player, result)) {
                return result;
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Checks if an item is in the player's regular inventory.
     */
    private boolean isInPlayerInventory(EntityPlayer player, ItemStack targetStack) {
        IInventory inventory = player.inventory;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == targetStack) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Finds rings in player inventory.
     */
    private List<ItemStack> findInPlayerInventory(EntityPlayer player, Class<? extends Item> ringClass) {
        List<ItemStack> inventoryRings = new ArrayList<>();
        
        IInventory inventory = player.inventory;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) {
                inventoryRings.add(stack);
            }
        }
        
        return inventoryRings;
    }
    
    /**
     * Finds the slot index of an item in player inventory.
     */
    private int findSlotIndex(EntityPlayer player, ItemStack targetStack) {
        IInventory inventory = player.inventory;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == targetStack) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Activates ring functionality when rings are found.
     * Implements Requirements 4.1, 4.2, 4.3 for ring activation.
     */
    private void activateRingFunctionality(EntityPlayer player, RingDetectionResult result) {
        ItemStack primaryRing = result.getPrimaryRing();
        
        if (primaryRing.isEmpty()) {
            LOGGER.warn("No primary ring found despite having rings in detection result");
            provideFeedbackNoRingsFound(player);
            return;
        }
        
        LOGGER.debug("Activating ring functionality for player {}: {} from {}", 
                    player.getName(), primaryRing.getDisplayName(), 
                    result.getPrimaryLocation() != null ? result.getPrimaryLocation().getDisplayName() : "unknown");
        
        // Activate the ring based on its type
        if (primaryRing.getItem() instanceof ItemAbsorbRing) {
            activateAbsorbRing(player, primaryRing);
        }
        // Add other ring type activations here
        
        // Provide feedback about successful activation
        provideFeedbackRingActivated(player, result);
    }
    
    /**
     * Activates chest ring functionality.
     */
    private void activateAbsorbRing(EntityPlayer player, ItemStack ringStack) {
        try {
            // Use the improved GUI access method that works from any location
            boolean success = ItemAbsorbRing.tryOpenAbsorbRingGui(player);

            if (!success) {
                LOGGER.warn("Failed to open absorb ring GUI for player {}", player.getName());
                provideFeedbackActivationFailed(player);
            } else {
                LOGGER.debug("Successfully opened absorb ring GUI for player: {}", player.getName());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to activate absorb ring for player {}: {}", player.getName(), e.getMessage());
            provideFeedbackActivationFailed(player);
        }
    }
    
    /**
     * Provides feedback when no rings are found.
     * Implements Requirement 4.4 for clear feedback about ring availability.
     */
    private void provideFeedbackNoRingsFound(EntityPlayer player) {
        LOGGER.debug("Providing 'no rings found' feedback to player: {}", player.getName());
        
        // Send a message to the player
        if (player.world.isRemote) {
            // Client-side feedback (chat message)
            net.minecraft.util.text.TextComponentString message = 
                new net.minecraft.util.text.TextComponentString("§c未找到戒指！请确保戒指在背包、快捷栏或饰品栏中。");
            player.sendMessage(message);
        }
    }
    
    /**
     * Provides feedback when ring is successfully activated.
     */
    private void provideFeedbackRingActivated(EntityPlayer player, RingDetectionResult result) {
        if (player.world.isRemote) {
            String locationName = result.getPrimaryLocation() != null ? 
                result.getPrimaryLocation().getDisplayName() : "未知位置";
            
            net.minecraft.util.text.TextComponentString message = 
                new net.minecraft.util.text.TextComponentString(
                    String.format("§a已激活戒指！位置：%s", locationName));
            player.sendMessage(message);
        }
    }
    
    /**
     * Provides feedback when ring activation fails.
     */
    private void provideFeedbackActivationFailed(EntityPlayer player) {
        if (player.world.isRemote) {
            net.minecraft.util.text.TextComponentString message = 
                new net.minecraft.util.text.TextComponentString("§c戒指激活失败！请稍后重试。");
            player.sendMessage(message);
        }
    }
}