package com.rsring.experience;

import com.rsring.item.ItemExperiencePump;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Unified inventory access layer for experience tank detection
 * Supports player inventory, hotbar, and Baubles slots
 */
public class InventoryIntegrationLayer {
    private static final Logger LOGGER = LogManager.getLogger(InventoryIntegrationLayer.class);
    private static InventoryIntegrationLayer instance;
    private Boolean baublesAvailable = null;

    private InventoryIntegrationLayer() {}

    public static InventoryIntegrationLayer getInstance() {
        if (instance == null) instance = new InventoryIntegrationLayer();
        return instance;
    }

    public static void initialize() {
        getInstance();
        LOGGER.info("Inventory integration layer initialized");
    }

    /**
     * Scan player inventory (excluding hotbar) for experience tanks
     */
    public List<ItemStack> getPlayerInventoryTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        if (player == null) return tanks;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isExperienceTank(stack)) tanks.add(stack);
        }
        return tanks;
    }

    /**
     * Scan hotbar slots for experience tanks
     */
    public List<ItemStack> getHotbarTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        if (player == null) return tanks;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isExperienceTank(stack)) tanks.add(stack);
        }

        ItemStack offHand = player.getHeldItemOffhand();
        if (isExperienceTank(offHand)) tanks.add(offHand);

        return tanks;
    }

    /**
     * Scan Baubles slots for experience tanks
     */
    public List<ItemStack> getBaublesTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        if (player == null || !isBaublesAvailable()) return tanks;

        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);
            if (handler == null) return tanks;

            java.lang.reflect.Method sizeMethod = null;
            java.lang.reflect.Method stackMethod = null;

            try {
                sizeMethod = handler.getClass().getMethod("getSizeInventory");
            } catch (NoSuchMethodException e) {
                try { sizeMethod = handler.getClass().getMethod("getSlots"); } catch (NoSuchMethodException ignored) {}
            }

            try {
                stackMethod = handler.getClass().getMethod("getStackInSlot", int.class);
            } catch (NoSuchMethodException ignored) {}

            if (sizeMethod != null && stackMethod != null) {
                int size = (int) sizeMethod.invoke(handler);
                for (int i = 0; i < size; i++) {
                    ItemStack stack = (ItemStack) stackMethod.invoke(handler, i);
                    if (isExperienceTank(stack)) tanks.add(stack);
                }
            } else if (handler instanceof IInventory) {
                IInventory baublesInventory = (IInventory) handler;
                for (int i = 0; i < baublesInventory.getSizeInventory(); i++) {
                    ItemStack stack = baublesInventory.getStackInSlot(i);
                    if (isExperienceTank(stack)) tanks.add(stack);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error scanning Baubles: {}", e.getMessage());
        }

        return tanks;
    }

    /**
     * Scan all inventory types
     */
    public TankScanResult scanAllInventories(EntityPlayer player) {
        if (player == null) return TankScanResult.empty();

        TankScanResult.Builder builder = new TankScanResult.Builder();
        builder.addTanks(getPlayerInventoryTanks(player), TankScanResult.InventoryType.PLAYER_INVENTORY);
        builder.addTanks(getHotbarTanks(player), TankScanResult.InventoryType.HOTBAR);
        builder.addTanks(getBaublesTanks(player), TankScanResult.InventoryType.BAUBLES);

        return builder.build();
    }

    /**
     * Refresh inventory state
     */
    public void refreshInventoryState(EntityPlayer player) {
        if (player == null) return;

        InventoryChangeHandler.getInstance().refreshPlayerInventory(player);
        if (isBaublesAvailable()) {
            InventoryChangeHandler.getInstance().refreshBaublesInventory(player);
        }
    }

    /**
     * Get total tank count
     */
    public int getTotalTankCount(EntityPlayer player) {
        return player == null ? 0 : scanAllInventories(player).getTankCount();
    }

    /**
     * Get total capacity across all tanks
     */
    public int getTotalCapacity(EntityPlayer player) {
        return player == null ? 0 : scanAllInventories(player).getTotalCapacity();
    }

    /**
     * Get total stored experience
     */
    public int getTotalStoredExperience(EntityPlayer player) {
        return player == null ? 0 : scanAllInventories(player).getTotalStored();
    }

    /**
     * Find first tank with available capacity
     */
    public ItemStack findFirstAvailableTank(EntityPlayer player) {
        if (player == null) return ItemStack.EMPTY;

        for (ItemStack tank : getHotbarTanks(player)) {
            if (hasAvailableCapacity(tank)) return tank;
        }

        for (ItemStack tank : getPlayerInventoryTanks(player)) {
            if (hasAvailableCapacity(tank)) return tank;
        }

        for (ItemStack tank : getBaublesTanks(player)) {
            if (hasAvailableCapacity(tank)) return tank;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Find all tanks with available capacity
     */
    public List<ItemStack> findAllAvailableTanks(EntityPlayer player) {
        List<ItemStack> availableTanks = new ArrayList<>();
        if (player == null) return availableTanks;

        for (ItemStack tank : scanAllInventories(player).getAllTanks()) {
            if (hasAvailableCapacity(tank)) availableTanks.add(tank);
        }

        return availableTanks;
    }

    // Private helper methods

    private boolean isExperienceTank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump;
    }

    private boolean hasAvailableCapacity(ItemStack tank) {
        if (!isExperienceTank(tank)) return false;

        int stored = ItemExperiencePump.getXpStoredFromNBT(tank);
        int capacity = ItemExperiencePump.getMaxXpFromNBT(tank);

        return stored < capacity;
    }

    private boolean isBaublesAvailable() {
        if (baublesAvailable == null) {
            baublesAvailable = checkBaublesAvailability();
        }
        return baublesAvailable;
    }

    private boolean checkBaublesAvailability() {
        if (!Loader.isModLoaded("baubles")) return false;

        try {
            Class.forName("baubles.api.BaublesApi");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Get diagnostic information for debugging
     */
    public Map<String, Object> getDiagnostics(EntityPlayer player) {
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        if (player == null) {
            diagnostics.put("error", "Player is null");
            return diagnostics;
        }

        diagnostics.put("playerName", player.getName());
        diagnostics.put("baublesAvailable", isBaublesAvailable());

        TankScanResult result = scanAllInventories(player);
        diagnostics.put("totalTanks", result.getTankCount());
        diagnostics.put("totalCapacity", result.getTotalCapacity());
        diagnostics.put("totalStored", result.getTotalStored());

        return diagnostics;
    }
}
