package com.rsring.service;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import com.rsring.util.BaublesHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ring detection service for finding rings across inventory slots
 */
public class RingDetectionService {
    private static final Logger LOGGER = LogManager.getLogger(RingDetectionService.class);
    private static RingDetectionService instance;

    private RingDetectionService() {}

    public static RingDetectionService getInstance() {
        if (instance == null) {
            instance = new RingDetectionService();
        }
        return instance;
    }

    /**
     * Find any ring in player's inventory
     * Priority: main hand -> off hand -> Baubles -> inventory
     */
    public static ItemStack findAnyRing(EntityPlayer player) {
        if (player == null) return ItemStack.EMPTY;
        return findRing(player, com.rsring.item.ItemAbsorbRing.class);
    }

    /**
     * Find specific ring type in player's inventory
     */
    public static ItemStack findRing(EntityPlayer player, Class<? extends Item> ringClass) {
        if (player == null || ringClass == null) return ItemStack.EMPTY;

        ItemStack result = findInHands(player, ringClass);
        if (!result.isEmpty()) return result;

        result = findInBaubles(player, ringClass);
        if (!result.isEmpty()) return result;

        return findInInventory(player, ringClass);
    }

    private static ItemStack findInHands(EntityPlayer player, Class<? extends Item> ringClass) {
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && ringClass.isInstance(mainHand.getItem())) return mainHand;

        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && ringClass.isInstance(offHand.getItem())) return offHand;

        return ItemStack.EMPTY;
    }

    private static ItemStack findInBaubles(EntityPlayer player, Class<? extends Item> ringClass) {
        if (!BaublesHelper.isBaublesLoaded()) return ItemStack.EMPTY;

        Object handler = BaublesHelper.getBaublesHandler(player);
        if (handler == null) return ItemStack.EMPTY;

        int size = BaublesHelper.getSlots(handler);
        for (int i = 0; i < size; i++) {
            ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
            if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findInInventory(EntityPlayer player, Class<? extends Item> ringClass) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static void markBaublesDirtyIfNeeded(EntityPlayer player) {
        if (player == null || !BaublesHelper.isBaublesLoaded()) return;
        Object handler = BaublesHelper.getBaublesHandler(player);
        if (handler instanceof IInventory) {
            ((IInventory) handler).markDirty();
        }
    }

    public static boolean isBaublesAvailable() {
        return BaublesHelper.isBaublesLoaded();
    }
}
