package com.rsring.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemLocationTracker {

    private static final Logger LOGGER = LogManager.getLogger(ItemLocationTracker.class);

    public enum LocationType {
        MAIN_HAND,
        OFF_HAND,
        PLAYER_INVENTORY,
        BAUBLES
    }

    private final ItemStack item;
    private final LocationType locationType;
    private final int slotIndex;
    private final Object baublesHandler;

    public ItemLocationTracker(ItemStack item, LocationType locationType, int slotIndex, Object baublesHandler) {
        this.item = item;
        this.locationType = locationType;
        this.slotIndex = slotIndex;
        this.baublesHandler = baublesHandler;

        LOGGER.debug("Created ItemLocationTracker: type={}, slot={}, item={}",
            locationType, slotIndex, item.getDisplayName());
    }

    public ItemStack getItem() {
        return item;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void syncBack(EntityPlayer player) {
        if (player == null) {
            LOGGER.warn("Cannot sync back: player is null");
            return;
        }

        LOGGER.debug("Syncing item back to {}, slot {}", locationType, slotIndex);

        switch (locationType) {
            case BAUBLES:
                if (baublesHandler != null) {
                    if (BaublesHelper.setStackInSlot(baublesHandler, slotIndex, item)) {
                        LOGGER.debug("Synced to Baubles slot {}", slotIndex);
                    } else {
                        LOGGER.error("Failed to sync to Baubles slot {}", slotIndex);
                    }
                } else {
                    LOGGER.error("Cannot sync to Baubles: handler is null");
                }
                break;

            case PLAYER_INVENTORY:
                player.inventory.setInventorySlotContents(slotIndex, item);
                player.inventory.markDirty();
                LOGGER.debug("Synced to player inventory slot {}", slotIndex);
                break;

            case MAIN_HAND:
                player.setHeldItem(EnumHand.MAIN_HAND, item);
                LOGGER.debug("Synced to main hand");
                break;

            case OFF_HAND:
                player.setHeldItem(EnumHand.OFF_HAND, item);
                LOGGER.debug("Synced to off hand");
                break;
        }
    }

    public static ItemLocationTracker findItem(EntityPlayer player, Class<? extends Item> itemClass) {
        if (player == null || itemClass == null) {
            LOGGER.debug("Cannot find item: player or itemClass is null");
            return null;
        }

        LOGGER.debug("Searching for item of type: {}", itemClass.getSimpleName());

        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && itemClass.isInstance(mainHand.getItem())) {
            LOGGER.debug("Found item in main hand");
            return new ItemLocationTracker(mainHand, LocationType.MAIN_HAND, -1, null);
        }

        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && itemClass.isInstance(offHand.getItem())) {
            LOGGER.debug("Found item in off hand");
            return new ItemLocationTracker(offHand, LocationType.OFF_HAND, -1, null);
        }

        if (BaublesHelper.isBaublesLoaded()) {
            ItemLocationTracker baublesResult = findInBaubles(player, itemClass);
            if (baublesResult != null) {
                LOGGER.debug("Found item in Baubles slot {}", baublesResult.getSlotIndex());
                return baublesResult;
            }
        }

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                LOGGER.debug("Found item in player inventory slot {}", i);
                return new ItemLocationTracker(stack, LocationType.PLAYER_INVENTORY, i, null);
            }
        }

        LOGGER.debug("Item not found in any location");
        return null;
    }

    private static ItemLocationTracker findInBaubles(EntityPlayer player, Class<? extends Item> itemClass) {
        Object handler = BaublesHelper.getBaublesHandler(player);
        if (handler == null) {
            return null;
        }

        int size = BaublesHelper.getSlots(handler);
        for (int i = 0; i < size; i++) {
            ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                LOGGER.debug("Found item in Baubles slot {}", i);
                return new ItemLocationTracker(stack, LocationType.BAUBLES, i, handler);
            }
        }

        return null;
    }

    public boolean isInBaubles() {
        return locationType == LocationType.BAUBLES;
    }

    public boolean isInHand() {
        return locationType == LocationType.MAIN_HAND || locationType == LocationType.OFF_HAND;
    }

    public boolean isInInventory() {
        return locationType == LocationType.PLAYER_INVENTORY;
    }

    @Override
    public String toString() {
        return String.format("ItemLocationTracker{type=%s, slot=%d, item=%s}",
            locationType, slotIndex, item.getDisplayName());
    }
}
