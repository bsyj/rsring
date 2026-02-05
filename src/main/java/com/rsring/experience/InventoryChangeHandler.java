package com.rsring.experience;

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
 * 处理经验储罐和戒指的物品栏变化检测和事件触发。
 * 监控玩家物品栏、快捷栏和饰品栏槽位的变化。
 *
 *
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
     * 单例模式的私有构造函数。
     */
    private InventoryChangeHandler() {
        // 将册此处理器到事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 获取单例实例。
     */
    public static InventoryChangeHandler getInstance() {
        if (instance == null) {
            instance = new InventoryChangeHandler();
        }
        return instance;
    }

    /**
     * 初始化物品栏变化处理器。
     * 应在模组初始化期间调用。
     */
    public static void initialize() {
        getInstance(); // 确保实例被创建和注册
        LOGGER.info("Inventory change handler initialized");
    }

    /**
     * 处理玩家tick事件以监控物品栏变化。
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
     * 处理物品丢弃事件以触发物品栏刷新。
     * 
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
     * 处理容器关闭事件以检查物品栏变化。
     * 
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
     * 处理合成事件以检测储罐升级和其他相关变化。
     * 
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.player.world.isRemote) return;

        ItemStack craftedItem = event.crafting;
        if (isRelevantItem(craftedItem)) {
            LOGGER.debug("Player crafted relevant item: {}", craftedItem.getDisplayName());

            //强制立即刷新制作事件的库存
            forceFullInventoryRefresh(event.player);
        }
    }

    /**
     * 手动触发玩家的物品栏刷新。
     * 当外部代码知道物品栏变化已发生时很有用。
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
     * 手动触发玩家的饰品栏物品栏刷新。
     */
    public void refreshBaublesInventory(EntityPlayer player) {
        if (player == null) return;

        LOGGER.debug("Manually refreshing Baubles inventory for player: {}", player.getName());

        //触发 Baubles 刷新事件
        InventoryChangeEvent refreshEvent = InventoryChangeEvent.inventoryRefreshed(
            player, InventoryChangeEvent.InventoryLocation.BAUBLES);
        MinecraftForge.EVENT_BUS.post(refreshEvent);

        // Force a full inventory check
        checkPlayerInventoryChanges(player);
    }

    /**
     * 安排下次tick时进行物品栏刷新。
     * 
     */
    public void scheduleInventoryRefresh(EntityPlayer player) {
        if (player == null) return;

        // Reset tick counter to force immediate check on next tick
        tickCounter = CHECK_INTERVAL - 1;

        LOGGER.debug("Scheduled immediate inventory refresh for player: {}", player.getName());
    }

    /**
     * 强制立即进行全面的物品栏刷新。
     * 
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
     * 刷新所有在线玩家的物品栏状态。
     * 对于模组初始化或配置更改很有用。
     */
    public void refreshAllPlayersInventory() {
        LOGGER.debug("Refreshing inventory for all online players");

        //这需要从我们可以访问所有玩家的上下文中调用
        //现在，我们只需清除所有快照以在下一个tick时强制刷新
        playerSnapshots.clear();
    }

    /**
     * 检查特定玩家的物品栏变化。
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
     * 创建玩家物品栏状态的快照。
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
     * 如果模组可用则捕获饰品栏物品。
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
     * 使用直接API集成增强饰品栏物品栏监控。
     * 需求 7.2 - 饰品栏槽位修改监听器。
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
     * 比较两个物品栏快照并触发适当的事件。
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
     * 触发物品添加事件。
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
     * 触发物品移除事件。
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
     * 触发物品修改事件。
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

    /**
     * 表示物品栏快照的内部类。
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
