package com.rsring.compat.wearablebackpacks;

import com.rsring.capability.IRsRingCapability;
import com.rsring.compat.CompatManager;
import com.rsring.config.RsRingConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * WearableBackpacks兼容主类
 */
public class WearableBackpacksCompat {
    private static final Logger LOGGER = LogManager.getLogger(WearableBackpacksCompat.class);
    private static final String MODID = "wearablebackpacks";

    private static boolean initialized = false;
    private static Boolean modLoaded = null;

    /**
     * 检查模组是否加载
     */
    public static boolean isModLoaded() {
        if (modLoaded == null) {
            modLoaded = Loader.isModLoaded(MODID);
            if (modLoaded) {
                LOGGER.info("WearableBackpacks兼容模块已启用");
            }
        }
        return modLoaded;
    }

    /**
     * 初始化兼容模块
     */
    public static void initialize() {
        if (initialized) return;

        if (!RsRingConfig.wearableBackpacksCompat.enabled) {
            LOGGER.info("WearableBackpacks兼容模块已在配置中禁用");
            return;
        }

        if (!isModLoaded()) {
            LOGGER.info("WearableBackpacks模组未加载，兼容模块未启用");
            return;
        }

        initialized = true;
        LOGGER.info("WearableBackpacks兼容模块初始化完成");
    }

    /**
     * 检查兼容模块是否可用
     */
    public static boolean isAvailable() {
        return initialized &&
               RsRingConfig.wearableBackpacksCompat.enabled &&
               isModLoaded();
    }

    /**
     * 获取玩家装备的WearableBackpacks背包
     */
    public static Object getEquippedBackpack(EntityPlayer player) {
        if (!isAvailable() || player == null) return null;

        try {
            Class<?> backpackHelperClass = Class.forName("net.mcft.copy.backpacks.api.BackpackHelper");
            Object backpack = backpackHelperClass.getMethod("getBackpack", Entity.class).invoke(null, player);
            return backpack;
        } catch (Exception e) {
            LOGGER.debug("获取玩家背包失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查玩家是否装备了WearableBackpacks背包
     */
    public static boolean hasEquippedBackpack(EntityPlayer player) {
        return getEquippedBackpack(player) != null;
    }

    /**
     * 检查玩家是否有背包（统一接口）
     */
    public static boolean hasBackpack(EntityPlayer player) {
        return hasEquippedBackpack(player);
    }

    /**
     * 获取背包的物品栏处理器
     */
    private static ItemStackHandler getBackpackItems(Object backpack) {
        if (backpack == null) return null;

        try {
            // 获取IBackpackData
            Object data = backpack.getClass().getMethod("getData").invoke(backpack);
            if (data == null) return null;

            // 检查是否是BackpackDataItems类型
            if (!data.getClass().getName().equals("net.mcft.copy.backpacks.misc.BackpackDataItems")) {
                return null;
            }

            // 获取items
            return (ItemStackHandler) data.getClass().getMethod("getItems").invoke(data);
        } catch (Exception e) {
            LOGGER.debug("获取背包物品栏失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 尝试将物品存入WearableBackpacks背包
     */
    public static int absorbToBackpack(EntityPlayer player, ItemStack itemStack, IRsRingCapability capability) {
        if (!isAvailable() || player == null || itemStack == null || itemStack.isEmpty()) {
            return 0;
        }

        Object backpack = getEquippedBackpack(player);
        if (backpack == null) return 0;

        // 检查是否有其他玩家正在使用这个背包
        if (isBackpackInUse(backpack)) {
            LOGGER.debug("背包正被其他玩家使用，跳过存入");
            return 0;
        }

        ItemStackHandler items = getBackpackItems(backpack);
        if (items == null) return 0;

        int originalCount = itemStack.getCount();
        int inserted = 0;

        // 先尝试合并到现有槽位
        for (int i = 0; i < items.getSlots() && !itemStack.isEmpty(); i++) {
            ItemStack existing = items.getStackInSlot(i);
            if (!existing.isEmpty() && canMerge(existing, itemStack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, itemStack.getCount());
                if (toAdd > 0) {
                    existing.grow(toAdd);
                    itemStack.shrink(toAdd);
                    inserted += toAdd;
                    items.setStackInSlot(i, existing);
                }
            }
        }

        // 再尝试放入空槽位
        for (int i = 0; i < items.getSlots() && !itemStack.isEmpty(); i++) {
            ItemStack existing = items.getStackInSlot(i);
            if (existing.isEmpty()) {
                int count = Math.min(itemStack.getCount(), itemStack.getMaxStackSize());
                ItemStack toPlace = itemStack.copy();
                toPlace.setCount(count);
                items.setStackInSlot(i, toPlace);
                itemStack.shrink(count);
                inserted += count;
            }
        }

        if (inserted > 0) {
            LOGGER.debug("成功将 {} 个物品存入WearableBackpacks背包", inserted);
        }

        return inserted;
    }

    /**
     * 尝试从WearableBackpacks背包中销毁物品
     */
    public static int destroyFromBackpack(EntityPlayer player, ItemStack toDestroy, IRsRingCapability capability) {
        if (!isAvailable() || player == null || toDestroy == null || toDestroy.isEmpty()) {
            return 0;
        }

        if (!RsRingConfig.wearableBackpacksCompat.destroyFromBackpacks) {
            return 0;
        }

        Object backpack = getEquippedBackpack(player);
        if (backpack == null) return 0;

        // 检查是否有其他玩家正在使用这个背包
        if (isBackpackInUse(backpack)) {
            LOGGER.debug("背包正被其他玩家使用，跳过销毁");
            return 0;
        }

        ItemStackHandler items = getBackpackItems(backpack);
        if (items == null) return 0;

        int maxDestroy = toDestroy.getCount();
        int destroyed = 0;

        for (int i = items.getSlots() - 1; i >= 0 && destroyed < maxDestroy; i--) {
            ItemStack existing = items.getStackInSlot(i);
            if (areItemsEqual(existing, toDestroy)) {
                int toRemove = Math.min(existing.getCount(), maxDestroy - destroyed);
                existing.shrink(toRemove);
                destroyed += toRemove;
                if (existing.isEmpty()) {
                    items.setStackInSlot(i, ItemStack.EMPTY);
                } else {
                    items.setStackInSlot(i, existing);
                }
            }
        }

        if (destroyed > 0) {
            LOGGER.debug("从WearableBackpacks背包中销毁了 {} 个物品", destroyed);
        }

        return destroyed;
    }

    /**
     * 检查是否可以存入背包
     */
    public static boolean canAbsorbToBackpack(EntityPlayer player) {
        if (!isAvailable() || player == null) return false;

        Object backpack = getEquippedBackpack(player);
        if (backpack == null) return false;

        // 检查是否有其他玩家正在使用这个背包
        if (isBackpackInUse(backpack)) {
            return false;
        }

        ItemStackHandler items = getBackpackItems(backpack);
        if (items == null) return false;

        // 检查是否有空槽位
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查是否可以从背包销毁
     */
    public static boolean canDestroyFromBackpack(EntityPlayer player, ItemStack toDestroy) {
        if (!isAvailable() || player == null || toDestroy == null) return false;

        if (!RsRingConfig.wearableBackpacksCompat.destroyFromBackpacks) return false;

        Object backpack = getEquippedBackpack(player);
        if (backpack == null) return false;

        ItemStackHandler items = getBackpackItems(backpack);
        if (items == null) return false;

        for (int i = 0; i < items.getSlots(); i++) {
            if (areItemsEqual(items.getStackInSlot(i), toDestroy)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 尝试吸收实体物品到背包
     */
    public static boolean absorbEntityItem(EntityPlayer player, EntityItem entityItem, IRsRingCapability capability) {
        if (player == null || entityItem == null || entityItem.isDead) return false;

        ItemStack itemStack = entityItem.getItem();
        if (itemStack.isEmpty()) return false;

        int inserted = absorbToBackpack(player, itemStack, capability);
        if (inserted <= 0) return false;

        int remaining = itemStack.getCount() - inserted;
        if (remaining <= 0) {
            entityItem.setDead();
        } else {
            itemStack.setCount(remaining);
            entityItem.setItem(itemStack);
        }

        return true;
    }

    // 辅助方法

    /**
     * 检查背包是否正在被其他玩家使用
     * 当WearableBackpacks的GUI打开时，直接操作可能导致冲突
     *
     * @param backpack 背包对象
     * @return 是否正在被使用
     */
    private static boolean isBackpackInUse(Object backpack) {
        if (backpack == null) return false;

        try {
            // 通过反射调用IBackpack.getPlayersUsing()
            java.lang.reflect.Method getPlayersUsing = backpack.getClass().getMethod("getPlayersUsing");
            int playersUsing = (int) getPlayersUsing.invoke(backpack);
            return playersUsing > 0;
        } catch (Exception e) {
            LOGGER.debug("检查背包使用状态时出错: {}", e.getMessage());
            return false;
        }
    }

    private static boolean canMerge(ItemStack existing, ItemStack toMerge) {
        if (existing.isEmpty() || toMerge.isEmpty()) return false;
        if (existing.getCount() >= existing.getMaxStackSize()) return false;
        return areItemsEqual(existing, toMerge);
    }

    private static boolean areItemsEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) return false;
        if (stack1.getItem() != stack2.getItem()) return false;
        if (stack1.getMetadata() != stack2.getMetadata()) return false;

        // 检查NBT
        net.minecraft.nbt.NBTTagCompound nbt1 = stack1.getTagCompound();
        net.minecraft.nbt.NBTTagCompound nbt2 = stack2.getTagCompound();

        if (nbt1 == null && nbt2 == null) return true;
        if (nbt1 == null || nbt2 == null) return false;

        return nbt1.equals(nbt2);
    }
    
    /**
     * 检查背包中是否已有满堆的指定物品
     */
    public static boolean hasFullStack(EntityPlayer player, ItemStack itemStack) {
        if (!isAvailable() || player == null || itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        
        try {
            Object backpack = getEquippedBackpack(player);
            if (backpack == null) return false;
            
            ItemStackHandler items = getBackpackItems(backpack);
            if (items == null) return false;
            
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (!stack.isEmpty() && areItemsEqual(stack, itemStack)) {
                    if (stack.getCount() >= stack.getMaxStackSize()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("检查WearableBackpacks满堆状态时出错", e);
        }
        
        return false;
    }
    
    /**
     * 检查背包中是否有部分填充的指定物品槽位
     * 用于SLOT_OVERFLOW模式的第3层处理
     */
    public static boolean hasPartialStack(EntityPlayer player, ItemStack itemStack) {
        if (!isAvailable() || player == null || itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        
        try {
            Object backpack = getEquippedBackpack(player);
            if (backpack == null) return false;
            
            ItemStackHandler items = getBackpackItems(backpack);
            if (items == null) return false;
            
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (!stack.isEmpty() && areItemsEqual(stack, itemStack)) {
                    // 有相同物品且未满堆
                    if (stack.getCount() < stack.getMaxStackSize()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("检查WearableBackpacks部分填充状态时出错", e);
        }
        
        return false;
    }
    
    /**
     * 检查WearableBackpacks背包是否已满
     */
    public static boolean isFull(EntityPlayer player) {
        if (!isAvailable() || player == null) {
            return false;
        }
        
        try {
            Object backpack = getEquippedBackpack(player);
            if (backpack == null) return false;
            
            ItemStackHandler items = getBackpackItems(backpack);
            if (items == null) return true;
            
            for (int i = 0; i < items.getSlots(); i++) {
                if (items.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("检查WearableBackpacks是否已满时出错", e);
        }
        
        return false;
    }
    
    /**
     * 清理背包内的垃圾物品
     * 用于定时清理功能
     * 遵循垃圾箱优先原则：优先送入垃圾箱，垃圾箱满了再销毁
     * 
     * @param player 玩家
     * @param capability 戒指能力
     */
    public static void cleanupBackpack(EntityPlayer player, com.rsring.capability.IRsRingCapability capability) {
        if (!isAvailable() || player == null || capability == null) {
            return;
        }
        
        // 检查垃圾箱是否可访问（绑定且未被破坏）
        if (capability.isTrashCanBound() && !com.rsring.item.ItemAbsorbRing.isTrashCanAccessible(capability)) {
            // 垃圾箱被破坏，跳过清理（保留物品）
            // 提示由地面物品销毁流程发送，这里不重复提示
            return;
        }
        
        try {
            Object backpack = getEquippedBackpack(player);
            if (backpack == null) return;
            
            ItemStackHandler items = getBackpackItems(backpack);
            if (items == null) return;
            
            com.rsring.capability.DestroyModeType destroyModeType = capability.getDestroyModeType();
            
            // SLOT_OVERFLOW模式需要特殊处理：保留一个满堆
            if (destroyModeType == com.rsring.capability.DestroyModeType.SLOT_OVERFLOW) {
                cleanupSlotOverflowMode(items, capability);
                return;
            }
            
            // 其他模式的常规处理
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                
                // 检查是否应该销毁
                if (!com.rsring.item.ItemAbsorbRing.shouldDestroyItem(capability, stack)) continue;
                
                // 根据销毁类型决定是否销毁
                boolean shouldDestroy = shouldDestroyByMode(player, stack, destroyModeType);
                
                if (shouldDestroy) {
                    destroyStack(items, i, stack, capability);
                }
            }
        } catch (Exception e) {
            LOGGER.error("清理WearableBackpacks背包时出错", e);
        }
    }
    
    /**
     * SLOT_OVERFLOW模式的特殊处理
     * 保留一个满堆，销毁多余的
     */
    private static void cleanupSlotOverflowMode(ItemStackHandler items, com.rsring.capability.IRsRingCapability capability) {
        // 使用Map统计每种物品的总数量（按物品ID分组）
        java.util.Map<String, Integer> itemCountMap = new java.util.HashMap<>();
        java.util.Map<String, java.util.List<Integer>> itemSlotMap = new java.util.HashMap<>();
        java.util.Map<String, Integer> itemMaxStackMap = new java.util.HashMap<>();
        
        // 第一遍：统计每种匹配销毁过滤器的物品
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            
            // 检查是否应该销毁
            if (!com.rsring.item.ItemAbsorbRing.shouldDestroyItem(capability, stack)) continue;
            
            String itemKey = stack.getItem().getRegistryName().toString();
            
            // 累加数量
            itemCountMap.merge(itemKey, stack.getCount(), Integer::sum);
            
            // 记录槽位
            itemSlotMap.computeIfAbsent(itemKey, k -> new java.util.ArrayList<>()).add(i);
            
            // 记录最大堆叠数
            itemMaxStackMap.putIfAbsent(itemKey, stack.getMaxStackSize());
        }
        
        // 第二遍：对每种物品，保留一个满堆，销毁多余的
        for (java.util.Map.Entry<String, Integer> entry : itemCountMap.entrySet()) {
            String itemKey = entry.getKey();
            int totalCount = entry.getValue();
            int maxStackSize = itemMaxStackMap.get(itemKey);
            java.util.List<Integer> slots = itemSlotMap.get(itemKey);
            
            // 如果总数不超过一个满堆，不销毁
            if (totalCount <= maxStackSize) {
                continue;
            }
            
            // 计算需要销毁的数量
            int toDestroy = totalCount - maxStackSize;
            
            // 从后往前销毁（保留前面的槽位）
            for (int i = slots.size() - 1; i >= 0 && toDestroy > 0; i--) {
                int slotIndex = slots.get(i);
                ItemStack stack = items.getStackInSlot(slotIndex);
                if (stack.isEmpty()) continue;
                
                int destroyCount = Math.min(toDestroy, stack.getCount());
                
                // 优先尝试送入垃圾箱
                ItemStack toDestroyStack = stack.copy();
                toDestroyStack.setCount(destroyCount);
                int sentToTrash = com.rsring.item.ItemAbsorbRing.trySendToTrashCan(capability, toDestroyStack);
                
                if (sentToTrash >= destroyCount) {
                    // 全部送入垃圾箱 - 正确更新槽位
                    int newCount = stack.getCount() - destroyCount;
                    if (newCount <= 0) {
                        items.setStackInSlot(slotIndex, net.minecraft.item.ItemStack.EMPTY);
                    } else {
                        ItemStack newStack = stack.copy();
                        newStack.setCount(newCount);
                        items.setStackInSlot(slotIndex, newStack);
                    }
                    toDestroy -= destroyCount;
                } else if (sentToTrash > 0) {
                    // 部分送入垃圾箱
                    int newCount = stack.getCount() - sentToTrash;
                    ItemStack newStack = stack.copy();
                    newStack.setCount(newCount);
                    items.setStackInSlot(slotIndex, newStack);
                    toDestroy -= sentToTrash;
                    
                    // 剩余部分真正销毁
                    int remaining = destroyCount - sentToTrash;
                    int actualRemaining = Math.min(remaining, newCount);
                    if (actualRemaining >= newCount) {
                        items.setStackInSlot(slotIndex, net.minecraft.item.ItemStack.EMPTY);
                        toDestroy -= newCount;
                    } else {
                        ItemStack finalStack = newStack.copy();
                        finalStack.setCount(newCount - actualRemaining);
                        items.setStackInSlot(slotIndex, finalStack);
                        toDestroy -= actualRemaining;
                    }
                } else {
                    // 没有绑定垃圾箱或垃圾箱满了，直接销毁
                    int newCount = stack.getCount() - destroyCount;
                    if (newCount <= 0) {
                        items.setStackInSlot(slotIndex, net.minecraft.item.ItemStack.EMPTY);
                    } else {
                        ItemStack newStack = stack.copy();
                        newStack.setCount(newCount);
                        items.setStackInSlot(slotIndex, newStack);
                    }
                    toDestroy -= destroyCount;
                }
            }
        }
    }
    
    /**
     * 销毁单个槽位的物品
     */
    private static void destroyStack(ItemStackHandler items, int slotIndex, ItemStack stack, com.rsring.capability.IRsRingCapability capability) {
        // 优先尝试送入垃圾箱
        int sentToTrash = com.rsring.item.ItemAbsorbRing.trySendToTrashCan(capability, stack);
        
        if (sentToTrash >= stack.getCount()) {
            // 全部送入垃圾箱，清空槽位
            items.setStackInSlot(slotIndex, net.minecraft.item.ItemStack.EMPTY);
        } else if (sentToTrash > 0) {
            // 部分送入垃圾箱，减少数量
            stack.shrink(sentToTrash);
            // 剩余部分销毁（垃圾箱满了）
            items.setStackInSlot(slotIndex, net.minecraft.item.ItemStack.EMPTY);
        } else {
            // 没有绑定垃圾箱或垃圾箱满了，直接销毁
            items.setStackInSlot(slotIndex, net.minecraft.item.ItemStack.EMPTY);
        }
    }
    
    /**
     * 根据销毁类型判断是否应该销毁
     * 销毁类型在所有模式下都生效
     * 
     * @param player 玩家
     * @param stack 要检查的物品
     * @param destroyModeType 销毁类型
     * @return 是否应该销毁
     */
    private static boolean shouldDestroyByMode(EntityPlayer player, 
                                                ItemStack stack,
                                                com.rsring.capability.DestroyModeType destroyModeType) {
        switch (destroyModeType) {
            case ALWAYS:
                // 总是销毁
                return true;
                
            case SLOT_OVERFLOW:
                // 只有已有满堆时才销毁（实际处理在 cleanupSlotOverflowMode 中）
                return true;
                
            case STORAGE_OVERFLOW:
                // 只有背包已满时才销毁
                return CompatManager.areAllBackpacksFull(player);

            default:
                return true;
        }
    }

    /**
     * 检查指定位置是否是放置的WearableBackpacks背包
     * @param world 世界
     * @param pos 位置
     * @return 是否是WearableBackpacks放置的背包
     */
    public static boolean isPlacedBackpack(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos) {
        if (world == null || pos == null) return false;

        net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
        if (te == null) return false;

        // 检查是否是TileEntityBackpack（使用类名匹配，不需要模组完全初始化）
        return te.getClass().getName().equals("net.mcft.copy.backpacks.block.entity.TileEntityBackpack");
    }

    /**
     * 获取放置的WearableBackpacks背包的物品栏处理器
     * 优先使用Capability方式，这样会正确处理enableMachineInteraction配置
     * @param world 世界
     * @param pos 位置
     * @return 物品栏处理器，如果不是背包则返回null
     */
    public static net.minecraftforge.items.IItemHandler getPlacedBackpackItems(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos) {
        if (!isPlacedBackpack(world, pos)) return null;

        try {
            net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
            if (te == null) return null;

            // 优先尝试通过Capability获取（会正确处理enableMachineInteraction配置）
            net.minecraftforge.items.IItemHandler handler = te.getCapability(
                net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler != null) return handler;

            // 回退方案：通过反射获取IBackpackData
            Object data = te.getClass().getMethod("getData").invoke(te);
            if (data == null) return null;

            // 检查是否是BackpackDataItems类型
            if (!data.getClass().getName().equals("net.mcft.copy.backpacks.misc.BackpackDataItems")) {
                return null;
            }

            // 获取items
            return (net.minecraftforge.items.ItemStackHandler) data.getClass().getMethod("getItems").invoke(data);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 标记放置的背包为脏（触发保存）
     * @param world 世界
     * @param pos 位置
     */
    public static void markPlacedBackpackDirty(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos) {
        if (world == null || pos == null) return;
        
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
        if (te != null) {
            te.markDirty();
        }
    }
}
