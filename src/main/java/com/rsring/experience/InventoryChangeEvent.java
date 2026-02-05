package com.rsring.experience;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 当影响经验储罐或戒指的物品栏变化发生时触发的自定义事件。
 * 支持需求 7.1、7.2、7.3 的跨物品栏集成和变化检测。
 */
public class InventoryChangeEvent extends Event {

    /**
     * 发生的物品栏变化类型。
     */
    public enum ChangeType {
        TANK_ADDED("储罐添加"),
        TANK_REMOVED("储罐移除"),
        TANK_MODIFIED("储罐修改"),
        RING_ADDED("戒指添加"),
        RING_REMOVED("戒指移除"),
        RING_MODIFIED("戒指修改"),
        BAUBLES_CHANGED("饰品栏变化"),
        INVENTORY_REFRESHED("物品栏刷新");

        private final String displayName;

        ChangeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 变化发生的地点。
     */
    public enum InventoryLocation {
        PLAYER_INVENTORY("玩家物品栏"),
        HOTBAR("快捷栏"),
        BAUBLES("饰品栏槽位"),
        OFFHAND("副手"),
        UNKNOWN("未知");

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
     * Minecraft Forge 事件系统所需的默认构造函数。
     * 使用默认值创建一个空事件。
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
     * 创建一个新的物品栏变化事件。
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
     * 创建一个不包含槽位信息的新物品栏变化事件。
     */
    public InventoryChangeEvent(EntityPlayer player, ChangeType changeType,
                               InventoryLocation location, ItemStack affectedItem) {
        this(player, changeType, location, affectedItem, -1);
    }

    /**
     * 为一般变化创建一个新的物品栏变化事件。
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
     * 检查此事件是否影响经验储罐。
     */
    public boolean affectsTanks() {
        return changeType == ChangeType.TANK_ADDED ||
               changeType == ChangeType.TANK_REMOVED ||
               changeType == ChangeType.TANK_MODIFIED ||
               (changeType == ChangeType.INVENTORY_REFRESHED && isExperienceTank(affectedItem)) ||
               (changeType == ChangeType.BAUBLES_CHANGED && isExperienceTank(affectedItem));
    }

    /**
     * 检查此事件是否影响戒指。
     */
    public boolean affectsRings() {
        return changeType == ChangeType.RING_ADDED ||
               changeType == ChangeType.RING_REMOVED ||
               changeType == ChangeType.RING_MODIFIED ||
               (changeType == ChangeType.INVENTORY_REFRESHED && isRing(affectedItem)) ||
               (changeType == ChangeType.BAUBLES_CHANGED && isRing(affectedItem));
    }

    /**
     * 检查此事件是否影响饰品栏槽位。
     */
    public boolean affectsBaubles() {
        return location == InventoryLocation.BAUBLES ||
               changeType == ChangeType.BAUBLES_CHANGED;
    }

    /**
     * 检查受影响的物品是否是经验储罐。
     */
    public boolean isAffectedItemTank() {
        return isExperienceTank(affectedItem);
    }

    /**
     * 检查受影响的物品是否是戒指。
     */
    public boolean isAffectedItemRing() {
        return isRing(affectedItem);
    }

    /**
     * 获取此事件的人类可读描述。
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
     * 检查物品是否是经验储罐。
     */
    private boolean isExperienceTank(ItemStack item) {
        return !item.isEmpty() && item.getItem() instanceof com.rsring.item.ItemExperiencePump;
    }

    /**
     * 检查物品是否是戒指。
     */
    private boolean isRing(ItemStack item) {
        if (item.isEmpty()) return false;

        // Check for known ring types
        return item.getItem() instanceof com.rsring.item.ItemAbsorbRing;
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
     * 创建一个储罐添加事件。
     */
    public static InventoryChangeEvent tankAdded(EntityPlayer player, InventoryLocation location,
                                               ItemStack tank, int slot) {
        return new InventoryChangeEvent(player, ChangeType.TANK_ADDED, location, tank, slot);
    }

    /**
     * 创建一个储罐移除事件。
     */
    public static InventoryChangeEvent tankRemoved(EntityPlayer player, InventoryLocation location,
                                                 ItemStack tank, int slot) {
        return new InventoryChangeEvent(player, ChangeType.TANK_REMOVED, location, tank, slot);
    }

    /**
     * 创建一个储罐修改事件。
     */
    public static InventoryChangeEvent tankModified(EntityPlayer player, InventoryLocation location,
                                                  ItemStack tank, int slot) {
        return new InventoryChangeEvent(player, ChangeType.TANK_MODIFIED, location, tank, slot);
    }

    /**
     * 创建一个戒指添加事件。
     */
    public static InventoryChangeEvent ringAdded(EntityPlayer player, InventoryLocation location,
                                               ItemStack ring, int slot) {
        return new InventoryChangeEvent(player, ChangeType.RING_ADDED, location, ring, slot);
    }

    /**
     * 创建一个戒指移除事件。
     */
    public static InventoryChangeEvent ringRemoved(EntityPlayer player, InventoryLocation location,
                                                 ItemStack ring, int slot) {
        return new InventoryChangeEvent(player, ChangeType.RING_REMOVED, location, ring, slot);
    }

    /**
     * 创建一个戒指修改事件。
     */
    public static InventoryChangeEvent ringModified(EntityPlayer player, InventoryLocation location,
                                                  ItemStack ring, int slot) {
        return new InventoryChangeEvent(player, ChangeType.RING_MODIFIED, location, ring, slot);
    }

    /**
     * 创建一个饰品栏变化事件。
     */
    public static InventoryChangeEvent baublesChanged(EntityPlayer player, ItemStack item, int slot) {
        return new InventoryChangeEvent(player, ChangeType.BAUBLES_CHANGED, InventoryLocation.BAUBLES, item, slot);
    }

    /**
     * 创建一个物品栏刷新事件。
     */
    public static InventoryChangeEvent inventoryRefreshed(EntityPlayer player, InventoryLocation location) {
        return new InventoryChangeEvent(player, ChangeType.INVENTORY_REFRESHED, location);
    }
}
