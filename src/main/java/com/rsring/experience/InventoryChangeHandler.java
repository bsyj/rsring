package com.rsring.experience;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles inventory change detection for experience tanks and rings
 */
public class InventoryChangeHandler {
    private static final Logger LOGGER = LogManager.getLogger(InventoryChangeHandler.class);
    private static InventoryChangeHandler instance;
    private final Map<UUID, InventorySnapshot> playerSnapshots = new ConcurrentHashMap<>();
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;

    private InventoryChangeHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static InventoryChangeHandler getInstance() {
        if (instance == null) instance = new InventoryChangeHandler();
        return instance;
    }

    public static void initialize() {
        getInstance();
        LOGGER.info("Inventory change handler initialized");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        if (++tickCounter % CHECK_INTERVAL != 0) return;
        checkPlayerInventoryChanges(event.player);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onItemToss(net.minecraftforge.event.entity.item.ItemTossEvent event) {
        if (event.getPlayer() == null || event.getPlayer().world.isRemote) return;
        if (isRelevantItem(event.getEntityItem().getItem())) {
            scheduleInventoryRefresh(event.getPlayer());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onContainerClosed(net.minecraftforge.event.entity.player.PlayerContainerEvent.Close event) {
        if (event.getEntityPlayer().world.isRemote) return;
        scheduleInventoryRefresh(event.getEntityPlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.player.world.isRemote) return;
        if (isRelevantItem(event.crafting)) {
            forceFullInventoryRefresh(event.player);
        }
    }

    public void refreshPlayerInventory(EntityPlayer player) {
        if (player == null) return;
        checkPlayerInventoryChanges(player);
        MinecraftForge.EVENT_BUS.post(InventoryChangeEvent.inventoryRefreshed(
            player, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY));
    }

    public void refreshBaublesInventory(EntityPlayer player) {
        if (player == null) return;
        MinecraftForge.EVENT_BUS.post(InventoryChangeEvent.inventoryRefreshed(
            player, InventoryChangeEvent.InventoryLocation.BAUBLES));
        checkPlayerInventoryChanges(player);
    }

    public void scheduleInventoryRefresh(EntityPlayer player) {
        if (player == null) return;
        tickCounter = CHECK_INTERVAL - 1;
    }

    public void forceFullInventoryRefresh(EntityPlayer player) {
        if (player == null) return;
        playerSnapshots.remove(player.getUniqueID());
        checkPlayerInventoryChanges(player);
    }

    private void checkPlayerInventoryChanges(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        InventorySnapshot currentSnapshot = createInventorySnapshot(player);
        InventorySnapshot previousSnapshot = playerSnapshots.get(playerId);

        if (previousSnapshot == null) {
            playerSnapshots.put(playerId, currentSnapshot);
            return;
        }

        monitorBaublesChanges(player);
        compareAndFireEvents(player, previousSnapshot, currentSnapshot);
        playerSnapshots.put(playerId, currentSnapshot);
    }

    private InventorySnapshot createInventorySnapshot(EntityPlayer player) {
        InventorySnapshot snapshot = new InventorySnapshot();

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isRelevantItem(stack)) {
                snapshot.addItem(InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY, i, stack);
            }
        }

        ItemStack offHand = player.getHeldItemOffhand();
        if (isRelevantItem(offHand)) {
            snapshot.addItem(InventoryChangeEvent.InventoryLocation.OFFHAND, 0, offHand);
        }

        captureBaublesInventory(player, snapshot);
        return snapshot;
    }

    private void captureBaublesInventory(EntityPlayer player, InventorySnapshot snapshot) {
        if (!Loader.isModLoaded("baubles")) return;

        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);

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

    private void monitorBaublesChanges(EntityPlayer player) {
        if (!Loader.isModLoaded("baubles")) return;

        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);

            if (handler instanceof IInventory) {
                IInventory baubles = (IInventory) handler;
                UUID playerId = player.getUniqueID();
                InventorySnapshot currentSnapshot = playerSnapshots.get(playerId);

                if (currentSnapshot != null) {
                    for (int i = 0; i < baubles.getSizeInventory(); i++) {
                        ItemStack currentStack = baubles.getStackInSlot(i);
                        String key = InventoryChangeEvent.InventoryLocation.BAUBLES.name() + ":" + i;
                        ItemStack previousStack = currentSnapshot.getItem(key);

                        if (!ItemStack.areItemStacksEqual(currentStack, previousStack)) {
                            if (isRelevantItem(currentStack) || isRelevantItem(previousStack)) {
                                MinecraftForge.EVENT_BUS.post(InventoryChangeEvent.baublesChanged(
                                    player, currentStack.isEmpty() ? previousStack : currentStack, i));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to monitor Baubles changes: {}", e.getMessage());
        }
    }

    private void compareAndFireEvents(EntityPlayer player, InventorySnapshot previous, InventorySnapshot current) {
        for (Map.Entry<String, ItemStack> entry : current.getAllItems().entrySet()) {
            String key = entry.getKey();
            ItemStack currentItem = entry.getValue();
            ItemStack previousItem = previous.getItem(key);

            if (previousItem.isEmpty()) {
                fireItemAddedEvent(player, parseLocationFromKey(key), currentItem, parseSlotFromKey(key));
            } else if (!ItemStack.areItemStacksEqual(previousItem, currentItem)) {
                fireItemModifiedEvent(player, parseLocationFromKey(key), currentItem, parseSlotFromKey(key));
            }
        }

        for (Map.Entry<String, ItemStack> entry : previous.getAllItems().entrySet()) {
            String key = entry.getKey();
            ItemStack previousItem = entry.getValue();
            ItemStack currentItem = current.getItem(key);

            if (currentItem.isEmpty()) {
                fireItemRemovedEvent(player, parseLocationFromKey(key), previousItem, parseSlotFromKey(key));
            }
        }
    }

    private void fireItemAddedEvent(EntityPlayer player, InventoryChangeEvent.InventoryLocation location, ItemStack item, int slot) {
        InventoryChangeEvent event = isExperienceTank(item) ? InventoryChangeEvent.tankAdded(player, location, item, slot) :
                                    isRing(item) ? InventoryChangeEvent.ringAdded(player, location, item, slot) : null;
        if (event != null) MinecraftForge.EVENT_BUS.post(event);
    }

    private void fireItemRemovedEvent(EntityPlayer player, InventoryChangeEvent.InventoryLocation location, ItemStack item, int slot) {
        InventoryChangeEvent event = isExperienceTank(item) ? InventoryChangeEvent.tankRemoved(player, location, item, slot) :
                                    isRing(item) ? InventoryChangeEvent.ringRemoved(player, location, item, slot) : null;
        if (event != null) MinecraftForge.EVENT_BUS.post(event);
    }

    private void fireItemModifiedEvent(EntityPlayer player, InventoryChangeEvent.InventoryLocation location, ItemStack item, int slot) {
        InventoryChangeEvent event = isExperienceTank(item) ? InventoryChangeEvent.tankModified(player, location, item, slot) :
                                    isRing(item) ? InventoryChangeEvent.ringModified(player, location, item, slot) : null;
        if (event != null) MinecraftForge.EVENT_BUS.post(event);
    }

    private boolean isRelevantItem(ItemStack item) {
        return isExperienceTank(item) || isRing(item);
    }

    private boolean isExperienceTank(ItemStack item) {
        return !item.isEmpty() && item.getItem() instanceof com.rsring.item.ItemExperiencePump;
    }

    private boolean isRing(ItemStack item) {
        return !item.isEmpty() && item.getItem() instanceof com.rsring.item.ItemAbsorbRing;
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

    private static class InventorySnapshot {
        private final Map<String, ItemStack> items = new HashMap<>();

        public void addItem(InventoryChangeEvent.InventoryLocation location, int slot, ItemStack item) {
            if (!item.isEmpty()) {
                items.put(location.name() + ":" + slot, item.copy());
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
