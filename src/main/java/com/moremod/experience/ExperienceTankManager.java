package com.moremod.experience;

import com.moremod.capability.ExperiencePumpCapability;
import com.moremod.capability.IExperiencePumpCapability;
import com.moremod.item.ItemExperiencePump;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages experience tank operations including upgrades, capacity validation, and overflow handling.
 * Integrates with the crafting event system to preserve experience during tank upgrades.
 * 
 * Supports Requirements 1.1, 1.3, 1.4 for experience tank upgrade preservation and capacity management.
 */
public class ExperienceTankManager {
    
    private static final Logger LOGGER = LogManager.getLogger(ExperienceTankManager.class);
    
    // Singleton instance
    private static ExperienceTankManager instance;
    
    // Constants for tank tiers and capacities
    public static final int BASE_CAPACITY = 1000; // 10 levels * 100 XP per level
    public static final int CAPACITY_PER_TIER = 1000; // Additional capacity per tier
    public static final int MAX_TIER = 10; // Maximum tank tier
    
    /**
     * Private constructor for singleton pattern.
     */
    private ExperienceTankManager() {
        // Register this manager with the event bus for crafting events
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * Gets the singleton instance.
     */
    public static ExperienceTankManager getInstance() {
        if (instance == null) {
            instance = new ExperienceTankManager();
        }
        return instance;
    }
    
    /**
     * Initializes the experience tank manager.
     * Should be called during mod initialization.
     */
    public static void initialize() {
        getInstance(); // Ensure instance is created and registered
        LOGGER.info("Experience tank manager initialized");
    }
    
    /**
     * Preserves experience when upgrading a tank from old to new.
     * This is the core method for Requirements 1.1 and 1.3.
     * 
     * @param oldTank The original tank being upgraded
     * @param newTank The new tank after upgrade
     * @return The new tank with preserved experience
     */
    public ItemStack preserveExperienceOnUpgrade(ItemStack oldTank, ItemStack newTank) {
        if (oldTank.isEmpty() || newTank.isEmpty()) {
            LOGGER.warn("Cannot preserve experience: old or new tank is empty");
            return newTank;
        }
        
        if (!(oldTank.getItem() instanceof ItemExperiencePump) || 
            !(newTank.getItem() instanceof ItemExperiencePump)) {
            LOGGER.warn("Cannot preserve experience: items are not experience tanks");
            return newTank;
        }
        
        // Get stored experience from old tank
        int storedExperience = getStoredExperience(oldTank);
        if (storedExperience <= 0) {
            LOGGER.debug("No experience to preserve in old tank");
            return newTank;
        }
        
        // Get the new tank's capacity
        int newCapacity = getTankCapacity(newTank);
        
        // Validate and cap the stored experience if necessary (Requirement 1.4)
        int preservedExperience = validateCapacity(storedExperience, newCapacity);
        
        if (preservedExperience < storedExperience) {
            LOGGER.info("Experience capped during upgrade: {} -> {} (capacity: {})", 
                       storedExperience, preservedExperience, newCapacity);
        }
        
        // Set the preserved experience in the new tank
        setStoredExperience(newTank, preservedExperience);
        
        // Preserve other tank properties
        preserveTankProperties(oldTank, newTank);
        
        LOGGER.debug("Experience preserved during upgrade: {} XP", preservedExperience);
        return newTank;
    }
    
    /**
     * Validates that stored XP doesn't exceed tank capacity.
     * Implements Requirement 1.4 for capacity overflow handling.
     * 
     * @param storedXP The amount of XP to validate
     * @param maxCapacity The maximum capacity of the tank
     * @return The validated XP amount (capped at capacity)
     */
    public int validateCapacity(int storedXP, int maxCapacity) {
        if (storedXP < 0) {
            LOGGER.warn("Invalid stored XP amount: {}, resetting to 0", storedXP);
            return 0;
        }
        
        if (maxCapacity <= 0) {
            LOGGER.warn("Invalid tank capacity: {}, using base capacity", maxCapacity);
            maxCapacity = BASE_CAPACITY;
        }
        
        if (storedXP > maxCapacity) {
            LOGGER.debug("Stored XP {} exceeds capacity {}, capping", storedXP, maxCapacity);
            return maxCapacity;
        }
        
        return storedXP;
    }
    
    /**
     * Gets the stored experience from a tank ItemStack.
     * 
     * @param tank The tank ItemStack
     * @return The amount of stored experience
     */
    public int getStoredExperience(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return 0;
        }
        
        // Try to get from capability first
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            return capability.getXpStored();
        }
        
        // Fallback to NBT data
        if (tank.hasTagCompound() && tank.getTagCompound().hasKey(ItemExperiencePump.XP_TAG)) {
            NBTTagCompound data = tank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
            return data.getInteger("xp");
        }
        
        return 0;
    }
    
    /**
     * Sets the stored experience in a tank ItemStack.
     * 
     * @param tank The tank ItemStack
     * @param experience The amount of experience to store
     */
    public void setStoredExperience(ItemStack tank, int experience) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            LOGGER.warn("Cannot set experience: invalid tank");
            return;
        }
        
        // Validate the experience amount
        int capacity = getTankCapacity(tank);
        int validatedExperience = validateCapacity(experience, capacity);
        
        // Set via capability if available
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            capability.setXpStored(validatedExperience);
            ItemExperiencePump.syncCapabilityToStack(tank, capability);
            return;
        }
        
        // Fallback to NBT data
        if (!tank.hasTagCompound()) {
            tank.setTagCompound(new NBTTagCompound());
        }
        
        NBTTagCompound data = tank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
        if (data.isEmpty()) {
            data = new NBTTagCompound();
            tank.getTagCompound().setTag(ItemExperiencePump.XP_TAG, data);
        }
        
        data.setInteger("xp", validatedExperience);
    }
    
    /**
     * Gets the capacity of a tank based on its tier.
     * 
     * @param tank The tank ItemStack
     * @return The tank's capacity
     */
    public int getTankCapacity(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return BASE_CAPACITY;
        }
        
        // Try to get from capability first
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            return capability.getMaxXp();
        }
        
        // Fallback to calculating from tier
        int tier = getTankTier(tank);
        return calculateCapacityForTier(tier);
    }
    
    /**
     * Gets the tier of a tank.
     * 
     * @param tank The tank ItemStack
     * @return The tank's tier
     */
    public int getTankTier(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return 1;
        }
        
        // Try to get from capability first
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            // Calculate tier from capacity
            int capacity = capability.getMaxXp();
            return calculateTierFromCapacity(capacity);
        }
        
        // Fallback to NBT data
        if (tank.hasTagCompound() && tank.getTagCompound().hasKey(ItemExperiencePump.XP_TAG)) {
            NBTTagCompound data = tank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
            if (data.hasKey("tier")) {
                return Math.max(1, Math.min(data.getInteger("tier"), MAX_TIER));
            }
        }
        
        return 1; // Default tier
    }
    
    /**
     * Calculates the capacity for a given tier.
     * 
     * @param tier The tank tier
     * @return The capacity for that tier
     */
    public int calculateCapacityForTier(int tier) {
        tier = Math.max(1, Math.min(tier, MAX_TIER));
        return BASE_CAPACITY + (tier - 1) * CAPACITY_PER_TIER;
    }
    
    /**
     * Calculates the tier from a given capacity.
     * 
     * @param capacity The tank capacity
     * @return The tier for that capacity
     */
    public int calculateTierFromCapacity(int capacity) {
        if (capacity <= BASE_CAPACITY) {
            return 1;
        }
        
        int tier = 1 + (capacity - BASE_CAPACITY) / CAPACITY_PER_TIER;
        return Math.max(1, Math.min(tier, MAX_TIER));
    }
    
    /**
     * Preserves tank properties (mode, retain level, mending) during upgrade.
     * 
     * @param oldTank The original tank
     * @param newTank The new tank
     */
    private void preserveTankProperties(ItemStack oldTank, ItemStack newTank) {
        IExperiencePumpCapability oldCap = oldTank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        IExperiencePumpCapability newCap = newTank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        
        if (oldCap != null && newCap != null) {
            // Preserve capability properties
            newCap.setMode(oldCap.getMode());
            newCap.setRetainLevel(oldCap.getRetainLevel());
            newCap.setUseForMending(oldCap.isUseForMending());
            
            // Sync to stack
            ItemExperiencePump.syncCapabilityToStack(newTank, newCap);
        } else {
            // Fallback to NBT preservation
            preserveNBTProperties(oldTank, newTank);
        }
    }
    
    /**
     * Preserves NBT properties when capabilities are not available.
     * 
     * @param oldTank The original tank
     * @param newTank The new tank
     */
    private void preserveNBTProperties(ItemStack oldTank, ItemStack newTank) {
        if (!oldTank.hasTagCompound() || !oldTank.getTagCompound().hasKey(ItemExperiencePump.XP_TAG)) {
            return;
        }
        
        NBTTagCompound oldData = oldTank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
        
        if (!newTank.hasTagCompound()) {
            newTank.setTagCompound(new NBTTagCompound());
        }
        
        NBTTagCompound newData = newTank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
        if (newData.isEmpty()) {
            newData = new NBTTagCompound();
            newTank.getTagCompound().setTag(ItemExperiencePump.XP_TAG, newData);
        }
        
        // Preserve properties
        if (oldData.hasKey("mode")) {
            newData.setInteger("mode", oldData.getInteger("mode"));
        }
        if (oldData.hasKey("retainLevel")) {
            newData.setInteger("retainLevel", oldData.getInteger("retainLevel"));
        }
        if (oldData.hasKey("mending")) {
            newData.setBoolean("mending", oldData.getBoolean("mending"));
        }
    }
    
    /**
     * Handles overflow when adding experience to a tank.
     * Returns the amount that couldn't be added.
     * 
     * @param tank The tank to add experience to
     * @param amount The amount of experience to add
     * @return The amount that couldn't be added (overflow)
     */
    public int handleExperienceOverflow(ItemStack tank, int amount) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump) || amount <= 0) {
            return amount;
        }
        
        int currentStored = getStoredExperience(tank);
        int capacity = getTankCapacity(tank);
        int availableSpace = capacity - currentStored;
        
        if (availableSpace <= 0) {
            return amount; // Tank is full, all is overflow
        }
        
        if (amount <= availableSpace) {
            // All can be added
            setStoredExperience(tank, currentStored + amount);
            return 0;
        } else {
            // Partial addition, return overflow
            setStoredExperience(tank, capacity);
            return amount - availableSpace;
        }
    }
    
    /**
     * Finds all experience tanks in a player's inventory.
     * Used for comprehensive tank management across all inventory types.
     * 
     * @param player The player to scan
     * @return List of all experience tanks found
     */
    public List<ItemStack> findAllTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        
        if (player == null) {
            return tanks;
        }
        
        // Scan player inventory
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                tanks.add(stack);
            }
        }
        
        // Scan off-hand
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && offHand.getItem() instanceof ItemExperiencePump) {
            tanks.add(offHand);
        }
        
        // Scan Baubles if available
        tanks.addAll(findBaublesTanks(player));
        
        return tanks;
    }
    
    /**
     * Finds experience tanks in Baubles slots.
     * 
     * @param player The player to scan
     * @return List of tanks found in Baubles slots
     */
    private List<ItemStack> findBaublesTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        
        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                                   .invoke(null, player);
            
            if (handler instanceof net.minecraft.inventory.IInventory) {
                net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                for (int i = 0; i < baubles.getSizeInventory(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                        tanks.add(stack);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Baubles not available or error accessing: {}", e.getMessage());
        }
        
        return tanks;
    }
    
    /**
     * Calculates the total capacity of all tanks for a player.
     * 
     * @param player The player to calculate for
     * @return The total capacity of all tanks
     */
    public int calculateTotalCapacity(EntityPlayer player) {
        List<ItemStack> tanks = findAllTanks(player);
        return tanks.stream()
                   .mapToInt(this::getTankCapacity)
                   .sum();
    }
    
    /**
     * Calculates the total stored experience across all tanks for a player.
     * 
     * @param player The player to calculate for
     * @return The total stored experience
     */
    public int calculateTotalStored(EntityPlayer player) {
        List<ItemStack> tanks = findAllTanks(player);
        return tanks.stream()
                   .mapToInt(this::getStoredExperience)
                   .sum();
    }
    
    /**
     * Event handler for player crafting events.
     * Integrates with the crafting system to handle tank upgrades.
     */
    @SubscribeEvent
    public void onPlayerCrafting(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.crafting;
        net.minecraft.inventory.IInventory craftMatrix = event.craftMatrix;
        
        // Check if this is an experience tank upgrade
        if (result.getItem() instanceof ItemExperiencePump && isUpgradeCrafting(craftMatrix)) {
            handleTankUpgradeCrafting(result, craftMatrix, event.player);
        }
    }
    
    /**
     * Checks if the crafting matrix represents a tank upgrade.
     * 
     * @param craftMatrix The crafting matrix
     * @return True if this is a tank upgrade
     */
    private boolean isUpgradeCrafting(net.minecraft.inventory.IInventory craftMatrix) {
        int tankCount = 0;
        boolean hasUpgradeItems = false;
        
        for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
            ItemStack stack = craftMatrix.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            
            if (stack.getItem() instanceof ItemExperiencePump) {
                tankCount++;
            } else if (stack.getItem() == net.minecraft.init.Items.ENDER_PEARL ||
                      stack.getItem() == net.minecraft.init.Items.EXPERIENCE_BOTTLE) {
                hasUpgradeItems = true;
            }
        }
        
        // Tank upgrade requires exactly one tank and upgrade items
        return tankCount == 1 && hasUpgradeItems;
    }
    
    /**
     * Handles tank upgrade crafting by preserving experience.
     * 
     * @param result The crafted result
     * @param craftMatrix The crafting matrix
     * @param player The player crafting
     */
    private void handleTankUpgradeCrafting(ItemStack result, net.minecraft.inventory.IInventory craftMatrix, EntityPlayer player) {
        // Find the original tank in the crafting matrix
        ItemStack originalTank = ItemStack.EMPTY;
        
        for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
            ItemStack stack = craftMatrix.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                originalTank = stack;
                break;
            }
        }
        
        if (!originalTank.isEmpty()) {
            // Preserve experience from original to result
            preserveExperienceOnUpgrade(originalTank, result);
            
            LOGGER.debug("Tank upgrade crafting handled for player: {}", player.getName());
            
            // Fire an inventory change event
            InventoryChangeEvent event = InventoryChangeEvent.tankModified(
                player, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY, result, -1);
            MinecraftForge.EVENT_BUS.post(event);
        }
    }
    
    /**
     * Creates a new ExperienceTankData from an ItemStack.
     * 
     * @param tank The tank ItemStack
     * @return ExperienceTankData representing the tank
     */
    public ExperienceTankData createTankData(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return new ExperienceTankData();
        }
        
        int stored = getStoredExperience(tank);
        int capacity = getTankCapacity(tank);
        int tier = getTankTier(tank);
        
        return new ExperienceTankData(stored, capacity, tier);
    }
    
    /**
     * Applies ExperienceTankData to an ItemStack.
     * 
     * @param tank The tank ItemStack to modify
     * @param data The data to apply
     */
    public void applyTankData(ItemStack tank, ExperienceTankData data) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump) || data == null) {
            return;
        }
        
        setStoredExperience(tank, data.getStoredExperience());
        
        // Update capacity if needed via capability
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            // Calculate the difference in capacity levels
            int currentCapacity = capability.getMaxXp();
            int targetCapacity = data.getMaxCapacity();
            
            if (targetCapacity > currentCapacity) {
                int levelsToAdd = (targetCapacity - currentCapacity) / CAPACITY_PER_TIER;
                capability.addCapacityLevels(levelsToAdd);
            }
            
            ItemExperiencePump.syncCapabilityToStack(tank, capability);
        }
    }
}