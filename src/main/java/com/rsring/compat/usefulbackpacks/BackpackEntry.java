package com.rsring.compat.usefulbackpacks;

import net.minecraft.item.ItemStack;

/**
 * 背包条目，记录背包位置和优先级
 */
public class BackpackEntry {
    private final ItemStack stack;
    private final InventoryLocation location;
    private final int slot;
    private final int priority;

    public BackpackEntry(ItemStack stack, InventoryLocation location, int slot) {
        this(stack, location, slot, location.getPriority());
    }

    public BackpackEntry(ItemStack stack, InventoryLocation location, int slot, int priority) {
        this.stack = stack;
        this.location = location;
        this.slot = slot;
        this.priority = priority;
    }

    public ItemStack getStack() {
        return stack;
    }

    public InventoryLocation getLocation() {
        return location;
    }

    public int getSlot() {
        return slot;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isValid() {
        return !stack.isEmpty() && BackpackDetector.isBackpack(stack);
    }

    @Override
    public String toString() {
        return String.format("BackpackEntry[location=%s, slot=%d, priority=%d, stack=%s]",
                location, slot, priority, stack.getDisplayName());
    }

    /**
     * 背包位置枚举，定义检测优先级
     */
    public enum InventoryLocation {
        MAIN_HAND(0),
        OFF_HAND(1),
        BAUBLES(2),
        HOTBAR(3),
        INVENTORY(4);

        private final int priority;

        InventoryLocation(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }

        public String getDisplayName() {
            switch (this) {
                case MAIN_HAND: return "主手";
                case OFF_HAND: return "副手";
                case BAUBLES: return "饰品栏";
                case HOTBAR: return "快捷栏";
                case INVENTORY: return "背包";
                default: return "未知";
            }
        }
    }
}
