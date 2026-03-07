package com.rsring.compat.usefulbackpacks;

import com.rsring.capability.IRsRingCapability;
import com.rsring.compat.CompatManager;
import com.rsring.config.RsRingConfig;
import com.rsring.compat.handler.BackpackAbsorbHandler;
import com.rsring.compat.handler.BackpackDestroyHandler;
import com.rsring.compat.inventory.BackpackInventoryHandler;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Useful-Backpacks兼容主类
 * 提供统一的接口供其他模块调用
 */
public class UsefulBackpacksCompat {
    private static final Logger LOGGER = LogManager.getLogger(UsefulBackpacksCompat.class);

    private static boolean initialized = false;

    /**
     * 初始化兼容模块
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        if (!RsRingConfig.usefulBackpacksCompat.enabled) {
            LOGGER.info("Useful-Backpacks兼容模块已在配置中禁用");
            return;
        }

        if (!BackpackDetector.isModLoaded()) {
            LOGGER.info("Useful-Backpacks模组未加载，兼容模块未启用");
            return;
        }

        initialized = true;
        LOGGER.info("Useful-Backpacks兼容模块初始化完成");
    }

    /**
     * 检查兼容模块是否可用
     */
    public static boolean isAvailable() {
        boolean result = initialized &&
               RsRingConfig.usefulBackpacksCompat.enabled &&
               BackpackDetector.isModLoaded();
        if (!result) {
            LOGGER.debug("UsefulBackpacksCompat.isAvailable() = {} (initialized={}, enabled={}, modLoaded={})",
                result, initialized, RsRingConfig.usefulBackpacksCompat.enabled, BackpackDetector.isModLoaded());
        }
        return result;
    }

    /**
     * 检查是否启用了背包模式
     */
    public static boolean isBackpackModeEnabled() {
        return isAvailable();
    }

    // ==================== 吸收模式接口 ====================

    /**
     * 尝试将物品吸收到背包
     *
     * @param player 玩家
     * @param itemStack 要吸收的物品
     * @param capability 戒指能力
     * @return 实际吸收的数量
     */
    public static int absorbToBackpacks(EntityPlayer player, ItemStack itemStack, IRsRingCapability capability) {
        if (!isAvailable()) {
            return 0;
        }
        return BackpackAbsorbHandler.tryAbsorbToBackpacks(player, itemStack, capability);
    }

    /**
     * 尝试将实体物品吸收到背包
     *
     * @param player 玩家
     * @param entityItem 实体物品
     * @param capability 戒指能力
     * @return 是否成功吸收
     */
    public static boolean absorbEntityItem(EntityPlayer player, EntityItem entityItem, IRsRingCapability capability) {
        if (!isAvailable()) {
            return false;
        }
        return BackpackAbsorbHandler.tryAbsorbEntityItem(player, entityItem, capability);
    }

    /**
     * 检查是否可以吸收到背包
     */
    public static boolean canAbsorbToBackpacks(EntityPlayer player) {
        if (!isAvailable()) {
            return false;
        }
        return BackpackAbsorbHandler.canAbsorbToBackpacks(player);
    }

    /**
     * 获取可用于吸收的总空间
     */
    public static int getAvailableSpace(EntityPlayer player) {
        if (!isAvailable()) {
            return 0;
        }
        return BackpackAbsorbHandler.getTotalAvailableSpace(player);
    }

    // ==================== 销毁模式接口 ====================

    /**
     * 尝试从背包中销毁物品
     *
     * @param player 玩家
     * @param toDestroy 要销毁的物品
     * @param capability 戒指能力
     * @return 实际销毁的数量
     */
    public static int destroyFromBackpacks(EntityPlayer player, ItemStack toDestroy, IRsRingCapability capability) {
        if (!isAvailable()) {
            return 0;
        }
        return BackpackDestroyHandler.tryDestroyFromBackpacks(player, toDestroy, capability);
    }

    /**
     * 检查是否可以从背包中销毁物品
     */
    public static boolean canDestroyFromBackpacks(EntityPlayer player, ItemStack toDestroy) {
        if (!isAvailable()) {
            return false;
        }
        return BackpackDestroyHandler.canDestroyFromBackpacks(player, toDestroy);
    }

    /**
     * 从背包中销毁指定数量的物品
     */
    public static int destroyItems(EntityPlayer player, ItemStack toDestroy, int maxDestroy) {
        if (!isAvailable()) {
            return 0;
        }
        return BackpackDestroyHandler.destroyItems(player, toDestroy, maxDestroy);
    }

    // ==================== 通用接口 ====================

    /**
     * 获取玩家身上所有背包
     */
    public static List<BackpackEntry> getPlayerBackpacks(EntityPlayer player) {
        if (!isAvailable()) {
            return java.util.Collections.emptyList();
        }
        return BackpackDetector.findAllBackpacks(player);
    }

    /**
     * 检查玩家是否有背包
     */
    public static boolean hasBackpack(EntityPlayer player) {
        if (!isAvailable()) {
            return false;
        }
        return BackpackDetector.hasAnyBackpack(player);
    }

    /**
     * 获取背包数量
     */
    public static int getBackpackCount(EntityPlayer player) {
        if (!isAvailable()) {
            return 0;
        }
        return BackpackDetector.getBackpackCount(player);
    }

    /**
     * 检查物品是否是Useful-Backpacks背包
     */
    public static boolean isBackpack(ItemStack stack) {
        return BackpackDetector.isBackpack(stack);
    }

    /**
     * 获取背包类型名称
     */
    public static String getBackpackTypeName(ItemStack backpack) {
        return BackpackDetector.getBackpackTypeName(backpack);
    }

    /**
     * 获取背包大小
     */
    public static int getBackpackSize(ItemStack backpack) {
        return BackpackDetector.getBackpackSize(backpack);
    }
    
    /**
     * 检查背包中是否已有满堆的指定物品
     */
    public static boolean hasFullStack(EntityPlayer player, ItemStack itemStack) {
        if (!isAvailable() || player == null || itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        
        try {
            List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
            for (BackpackEntry entry : backpacks) {
                if (hasFullStackInBackpack(entry.getStack(), itemStack)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("检查Useful-Backpacks满堆状态时出错", e);
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
            List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
            for (BackpackEntry entry : backpacks) {
                if (hasPartialStackInBackpack(entry.getStack(), itemStack)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("检查Useful-Backpacks部分填充状态时出错", e);
        }
        
        return false;
    }
    
    /**
     * 检查单个背包中是否已有满堆的指定物品
     */
    private static boolean hasFullStackInBackpack(ItemStack backpack, ItemStack itemStack) {
        if (backpack == null || backpack.isEmpty()) {
            return false;
        }
        
        try {
            IItemHandler inventory = BackpackInventoryHandler.getInventory(backpack);
            if (inventory == null) {
                return false;
            }
            
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && ItemHandlerHelper.canItemStacksStack(stack, itemStack)) {
                    if (stack.getCount() >= stack.getMaxStackSize()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("检查背包满堆状态时出错", e);
        }
        
        return false;
    }
    
    /**
     * 检查单个背包中是否有部分填充的指定物品槽位
     */
    private static boolean hasPartialStackInBackpack(ItemStack backpack, ItemStack itemStack) {
        if (backpack == null || backpack.isEmpty()) {
            return false;
        }
        
        try {
            IItemHandler inventory = BackpackInventoryHandler.getInventory(backpack);
            if (inventory == null) {
                return false;
            }
            
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && ItemHandlerHelper.canItemStacksStack(stack, itemStack)) {
                    // 有相同物品且未满堆
                    if (stack.getCount() < stack.getMaxStackSize()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("检查背包部分填充状态时出错", e);
        }
        
        return false;
    }
    
    /**
     * 检查所有Useful-Backpacks背包是否已满
     */
    public static boolean isFull(EntityPlayer player) {
        if (!isAvailable() || player == null) {
            return false;
        }
        
        try {
            List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
            if (backpacks.isEmpty()) {
                return false;
            }
            
            for (BackpackEntry entry : backpacks) {
                if (!isBackpackFull(entry.getStack())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("检查Useful-Backpacks是否已满时出错", e);
        }
        
        return false;
    }
    
    /**
     * 检查单个背包是否已满
     */
    private static boolean isBackpackFull(ItemStack backpack) {
        if (backpack == null || backpack.isEmpty()) {
            return true;
        }
        
        try {
            IItemHandler inventory = BackpackInventoryHandler.getInventory(backpack);
            if (inventory == null) {
                return true;
            }
            
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("检查背包是否已满时出错", e);
        }
        
        return true;
    }
    
    /**
     * 清理背包内的垃圾物品
     * 用于定时清理功能
     * 
     * @param player 玩家
     * @param capability 戒指能力
     */
    public static void cleanupBackpack(EntityPlayer player, com.rsring.capability.IRsRingCapability capability) {
        if (!isAvailable() || player == null || capability == null) {
            return;
        }
        
        try {
            java.util.List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
            for (BackpackEntry entry : backpacks) {
                cleanupSingleBackpack(player, entry.getStack(), capability);
            }
        } catch (Exception e) {
            LOGGER.error("清理Useful-Backpacks背包时出错", e);
        }
    }
    
    /**
     * 清理单个背包内的垃圾
     * 遵循垃圾箱优先原则：优先送入垃圾箱，垃圾箱满了再销毁
     * 
     * @param player 玩家
     * @param backpack 背包物品
     * @param capability 戒指能力
     */
    private static void cleanupSingleBackpack(EntityPlayer player, ItemStack backpack, com.rsring.capability.IRsRingCapability capability) {
        if (backpack == null || backpack.isEmpty()) {
            return;
        }
        
        // 检查垃圾箱是否可访问（绑定且未被破坏）
        if (capability.isTrashCanBound() && !com.rsring.item.ItemAbsorbRing.isTrashCanAccessible(capability)) {
            // 垃圾箱被破坏，跳过清理（保留物品）
            // 提示由地面物品销毁流程发送，这里不重复提示
            return;
        }
        
        try {
            IItemHandler inventory = BackpackInventoryHandler.getInventory(backpack);
            if (inventory == null) {
                return;
            }
            
            com.rsring.capability.DestroyModeType destroyModeType = capability.getDestroyModeType();
            
            // SLOT_OVERFLOW模式需要特殊处理：保留一个满堆
            if (destroyModeType == com.rsring.capability.DestroyModeType.SLOT_OVERFLOW) {
                cleanupSlotOverflowMode(inventory, capability);
                return;
            }
            
            // 其他模式的常规处理
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                
                // 检查是否应该销毁
                if (!com.rsring.item.ItemAbsorbRing.shouldDestroyItem(capability, stack)) continue;
                
                // 根据销毁类型决定是否销毁
                boolean shouldDestroy = shouldDestroyByMode(player, stack, destroyModeType);
                
                if (shouldDestroy) {
                    destroyStack(inventory, i, stack, capability);
                }
            }
        } catch (Exception e) {
            LOGGER.error("清理单个背包时出错", e);
        }
    }
    
    /**
     * SLOT_OVERFLOW模式的特殊处理
     * 保留一个满堆，销毁多余的
     */
    private static void cleanupSlotOverflowMode(IItemHandler inventory, com.rsring.capability.IRsRingCapability capability) {
        // 使用Map统计每种物品的总数量（按物品ID分组）
        java.util.Map<String, Integer> itemCountMap = new java.util.HashMap<>();
        java.util.Map<String, java.util.List<Integer>> itemSlotMap = new java.util.HashMap<>();
        java.util.Map<String, Integer> itemMaxStackMap = new java.util.HashMap<>();
        
        // 第一遍：统计每种匹配销毁过滤器的物品
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
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
                ItemStack stack = inventory.getStackInSlot(slotIndex);
                if (stack.isEmpty()) continue;
                
                int destroyCount = Math.min(toDestroy, stack.getCount());
                
                // 优先尝试送入垃圾箱
                ItemStack toDestroyStack = stack.copy();
                toDestroyStack.setCount(destroyCount);
                int sentToTrash = com.rsring.item.ItemAbsorbRing.trySendToTrashCan(capability, toDestroyStack);
                
                if (sentToTrash >= destroyCount) {
                    // 全部送入垃圾箱 - 使用extractItem正确移除物品
                    inventory.extractItem(slotIndex, destroyCount, false);
                    toDestroy -= destroyCount;
                } else if (sentToTrash > 0) {
                    // 部分送入垃圾箱
                    inventory.extractItem(slotIndex, sentToTrash, false);
                    toDestroy -= sentToTrash;
                    // 剩余部分真正销毁
                    int remaining = destroyCount - sentToTrash;
                    int actualRemaining = Math.min(remaining, stack.getCount() - sentToTrash);
                    if (actualRemaining > 0) {
                        inventory.extractItem(slotIndex, actualRemaining, false);
                        toDestroy -= actualRemaining;
                    }
                } else {
                    // 没有绑定垃圾箱或垃圾箱满了，直接销毁
                    inventory.extractItem(slotIndex, destroyCount, false);
                    toDestroy -= destroyCount;
                }
            }
        }
    }
    
    /**
     * 销毁单个槽位的物品
     */
    private static void destroyStack(IItemHandler inventory, int slotIndex, ItemStack stack, com.rsring.capability.IRsRingCapability capability) {
        // 优先尝试送入垃圾箱
        int sentToTrash = com.rsring.item.ItemAbsorbRing.trySendToTrashCan(capability, stack);
        
        if (sentToTrash >= stack.getCount()) {
            // 全部送入垃圾箱，清空槽位
            inventory.extractItem(slotIndex, stack.getCount(), false);
        } else if (sentToTrash > 0) {
            // 部分送入垃圾箱，减少数量
            stack.shrink(sentToTrash);
            // 剩余部分销毁（垃圾箱满了）
            inventory.extractItem(slotIndex, stack.getCount(), false);
        } else {
            // 没有绑定垃圾箱或垃圾箱满了，直接销毁
            inventory.extractItem(slotIndex, stack.getCount(), false);
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
}
