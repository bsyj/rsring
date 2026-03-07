package com.rsring.compat.inventory;

import com.rsring.compat.usefulbackpacks.BackpackDetector;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 背包物品处理器，负责读写Useful-Backpacks背包的物品数据
 * 
 * NBT结构说明：
 * Useful-Backpacks使用 ItemStackHelper.saveAllItems/loadAllItems 存储物品
 * 存储格式：根NBT下有一个名为"Items"的NBTTagList
 * 每个非空槽位存储为一个NBTTagCompound，包含Slot(byte)和物品数据
 */
public class BackpackInventoryHandler {
    private static final Logger LOGGER = LogManager.getLogger(BackpackInventoryHandler.class);

    // NBT标签常量 - 与ItemStackHelper使用的相同
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_SLOT = "Slot";

    /**
     * 获取背包中的物品列表
     * 使用与ItemStackHelper.loadAllItems相同的逻辑
     *
     * @param backpack 背包物品
     * @return 物品数组，空槽位为ItemStack.EMPTY
     */
    public static ItemStack[] getItems(ItemStack backpack) {
        int size = BackpackDetector.getBackpackSize(backpack);
        NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);

        if (!backpack.hasTagCompound()) {
            return list.toArray(new ItemStack[0]);
        }

        NBTTagCompound tag = backpack.getTagCompound();
        if (!tag.hasKey(TAG_ITEMS, 9)) { // 9 = NBTTagList
            return list.toArray(new ItemStack[0]);
        }

        NBTTagList itemList = tag.getTagList(TAG_ITEMS, 10); // 10 = NBTTagCompound

        for (int i = 0; i < itemList.tagCount(); i++) {
            NBTTagCompound slotTag = itemList.getCompoundTagAt(i);
            int slot = slotTag.getByte(TAG_SLOT) & 255;

            if (slot >= 0 && slot < size) {
                list.set(slot, new ItemStack(slotTag));
            }
        }

        return list.toArray(new ItemStack[0]);
    }

    /**
     * 获取指定槽位的物品
     */
    public static ItemStack getStackInSlot(ItemStack backpack, int slot) {
        if (!BackpackDetector.isBackpack(backpack)) return ItemStack.EMPTY;

        int size = BackpackDetector.getBackpackSize(backpack);
        if (slot < 0 || slot >= size) return ItemStack.EMPTY;

        ItemStack[] items = getItems(backpack);
        return items[slot];
    }

    /**
     * 设置指定槽位的物品
     */
    public static void setStackInSlot(ItemStack backpack, int slot, ItemStack stack) {
        if (!BackpackDetector.isBackpack(backpack)) return;

        int size = BackpackDetector.getBackpackSize(backpack);
        if (slot < 0 || slot >= size) return;

        // 读取现有物品列表
        ItemStack[] items = getItems(backpack);
        
        // 更新指定槽位
        items[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        
        // 保存回NBT
        saveItems(backpack, items);
    }

    /**
     * 保存物品列表到背包NBT
     * 使用与ItemStackHelper.saveAllItems相同的逻辑
     */
    private static void saveItems(ItemStack backpack, ItemStack[] items) {
        NBTTagCompound tag = backpack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            backpack.setTagCompound(tag);
        }

        // 检查是否所有槽位都为空
        boolean hasItems = false;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                hasItems = true;
                break;
            }
        }

        if (!hasItems) {
            // 所有槽位都为空，移除Items标签
            tag.removeTag(TAG_ITEMS);
            if (tag.isEmpty()) {
                backpack.setTagCompound(null);
            }
            return;
        }

        NBTTagList itemList = new NBTTagList();
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            if (!stack.isEmpty()) {
                NBTTagCompound slotTag = new NBTTagCompound();
                slotTag.setByte(TAG_SLOT, (byte) i);
                stack.writeToNBT(slotTag);
                itemList.appendTag(slotTag);
            }
        }

        tag.setTag(TAG_ITEMS, itemList);
    }

    /**
     * 获取背包空闲槽位数量
     */
    public static int getEmptySlotsCount(ItemStack backpack) {
        if (!BackpackDetector.isBackpack(backpack)) return 0;

        ItemStack[] items = getItems(backpack);
        int emptyCount = 0;

        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                emptyCount++;
            }
        }

        return emptyCount;
    }

    /**
     * 获取第一个空闲槽位索引，如果没有则返回-1
     */
    public static int getFirstEmptySlot(ItemStack backpack) {
        if (!BackpackDetector.isBackpack(backpack)) return -1;

        ItemStack[] items = getItems(backpack);

        for (int i = 0; i < items.length; i++) {
            if (items[i].isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 尝试将物品插入背包
     * 优先合并到现有槽位，然后放入空槽位
     *
     * @param backpack 目标背包
     * @param toInsert 要插入的物品
     * @return 实际插入的数量
     */
    public static int insertItem(ItemStack backpack, ItemStack toInsert) {
        if (!BackpackDetector.isBackpack(backpack)) {
            return 0;
        }
        if (toInsert.isEmpty()) {
            return 0;
        }

        int size = BackpackDetector.getBackpackSize(backpack);
        ItemStack[] items = getItems(backpack);
        ItemStack remaining = toInsert.copy();
        int totalInserted = 0;

        // 第一步：尝试合并到现有槽位
        for (int i = 0; i < size && !remaining.isEmpty(); i++) {
            ItemStack existing = items[i];
            if (!existing.isEmpty() && canMerge(existing, remaining)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, remaining.getCount());

                if (toAdd > 0) {
                    existing.grow(toAdd);
                    remaining.shrink(toAdd);
                    totalInserted += toAdd;
                }
            }
        }

        // 第二步：如果有剩余，放入空槽位
        for (int i = 0; i < size && !remaining.isEmpty(); i++) {
            if (items[i].isEmpty()) {
                int count = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                items[i] = remaining.copy();
                items[i].setCount(count);
                remaining.shrink(count);
                totalInserted += count;
            }
        }

        // 只有当有物品被插入时才保存
        if (totalInserted > 0) {
            saveItems(backpack, items);
        }

        return totalInserted;
    }

    /**
     * 从背包中提取物品
     *
     * @param backpack 背包
     * @param toExtract 要提取的物品（用于匹配）
     * @param maxExtract 最大提取数量
     * @return 实际提取的数量
     */
    public static int extractItem(ItemStack backpack, ItemStack toExtract, int maxExtract) {
        if (!BackpackDetector.isBackpack(backpack) || toExtract.isEmpty() || maxExtract <= 0) {
            return 0;
        }

        int size = BackpackDetector.getBackpackSize(backpack);
        ItemStack[] items = getItems(backpack);
        int extracted = 0;

        for (int i = size - 1; i >= 0 && extracted < maxExtract; i--) {
            ItemStack existing = items[i];
            if (!existing.isEmpty() && areItemsEqual(existing, toExtract)) {
                int toRemove = Math.min(existing.getCount(), maxExtract - extracted);
                existing.shrink(toRemove);
                extracted += toRemove;

                if (existing.isEmpty()) {
                    items[i] = ItemStack.EMPTY;
                }
            }
        }

        if (extracted > 0) {
            saveItems(backpack, items);
        }

        return extracted;
    }

    /**
     * 从背包中删除指定数量的物品（用于销毁模式）
     *
     * @param backpack 背包
     * @param toDestroy 要销毁的物品（用于匹配）
     * @param maxDestroy 最大销毁数量
     * @return 实际销毁的数量
     */
    public static int destroyItem(ItemStack backpack, ItemStack toDestroy, int maxDestroy) {
        return extractItem(backpack, toDestroy, maxDestroy);
    }

    /**
     * 检查两个物品是否可以合并
     */
    private static boolean canMerge(ItemStack existing, ItemStack toMerge) {
        if (existing.isEmpty() || toMerge.isEmpty()) return false;
        if (existing.getCount() >= existing.getMaxStackSize()) return false;
        return ItemHandlerHelper.canItemStacksStack(existing, toMerge);
    }

    /**
     * 检查两个物品是否相等（忽略数量）
     */
    private static boolean areItemsEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) return false;
        return ItemHandlerHelper.canItemStacksStack(stack1, stack2);
    }

    /**
     * 获取背包中指定物品的总数量
     */
    public static int getItemCount(ItemStack backpack, ItemStack toCount) {
        if (!BackpackDetector.isBackpack(backpack) || toCount.isEmpty()) {
            return 0;
        }

        int count = 0;
        ItemStack[] items = getItems(backpack);

        for (ItemStack stack : items) {
            if (!stack.isEmpty() && areItemsEqual(stack, toCount)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * 检查背包是否已满
     */
    public static boolean isFull(ItemStack backpack) {
        if (!BackpackDetector.isBackpack(backpack)) return true;
        return getEmptySlotsCount(backpack) == 0;
    }

    /**
     * 检查背包是否为空
     */
    public static boolean isEmpty(ItemStack backpack) {
        if (!BackpackDetector.isBackpack(backpack)) return true;
        if (!backpack.hasTagCompound()) return true;

        NBTTagCompound tag = backpack.getTagCompound();
        if (!tag.hasKey(TAG_ITEMS, 9)) return true;

        NBTTagList itemList = tag.getTagList(TAG_ITEMS, 10);
        return itemList.tagCount() == 0;
    }

    /**
     * 清空背包
     */
    public static void clearBackpack(ItemStack backpack) {
        if (!BackpackDetector.isBackpack(backpack)) return;

        if (backpack.hasTagCompound()) {
            NBTTagCompound tag = backpack.getTagCompound();
            tag.removeTag(TAG_ITEMS);

            if (tag.isEmpty()) {
                backpack.setTagCompound(null);
            }
        }
    }

    /**
     * 获取背包的 IItemHandler 接口
     *
     * @param backpack 背包物品
     * @return IItemHandler 实例，如果背包无效则返回 null
     */
    public static IItemHandler getInventory(ItemStack backpack) {
        if (!BackpackDetector.isBackpack(backpack)) {
            return null;
        }

        int size = BackpackDetector.getBackpackSize(backpack);
        ItemStackHandler handler = new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                syncToBackpack(backpack, this);
            }
        };

        // 从背包NBT加载物品
        ItemStack[] items = getItems(backpack);
        for (int i = 0; i < items.length && i < size; i++) {
            handler.setStackInSlot(i, items[i]);
        }

        return handler;
    }

    /**
     * 将 ItemStackHandler 的内容同步回背包NBT
     */
    private static void syncToBackpack(ItemStack backpack, ItemStackHandler handler) {
        int size = handler.getSlots();
        ItemStack[] items = new ItemStack[size];
        
        boolean hasItems = false;
        for (int i = 0; i < size; i++) {
            items[i] = handler.getStackInSlot(i);
            if (!items[i].isEmpty()) {
                hasItems = true;
            }
        }
        
        if (!hasItems) {
            if (backpack.hasTagCompound()) {
                NBTTagCompound tag = backpack.getTagCompound();
                tag.removeTag(TAG_ITEMS);
                if (tag.isEmpty()) {
                    backpack.setTagCompound(null);
                }
            }
            return;
        }
        
        if (!backpack.hasTagCompound()) {
            backpack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = backpack.getTagCompound();
        NBTTagList itemList = new NBTTagList();

        for (int i = 0; i < size; i++) {
            ItemStack stack = items[i];
            if (!stack.isEmpty()) {
                NBTTagCompound slotTag = new NBTTagCompound();
                slotTag.setByte(TAG_SLOT, (byte) i);
                stack.writeToNBT(slotTag);
                itemList.appendTag(slotTag);
            }
        }

        tag.setTag(TAG_ITEMS, itemList);
    }
}
