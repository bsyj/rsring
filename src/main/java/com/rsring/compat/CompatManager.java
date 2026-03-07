package com.rsring.compat;

import com.rsring.compat.usefulbackpacks.UsefulBackpacksCompat;
import com.rsring.compat.wearablebackpacks.WearableBackpacksCompat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import com.rsring.capability.IRsRingCapability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 兼容管理器 - 统一管理所有模组兼容模块
 * 
 * 职责：
 * 1. 初始化所有兼容模块
 * 2. 提供统一的背包兼容接口
 * 3. 管理多个背包模组的优先级
 */
public class CompatManager {
    private static final Logger LOGGER = LogManager.getLogger(CompatManager.class);
    private static boolean initialized = false;

    /**
     * 初始化所有兼容模块
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        LOGGER.info("开始初始化兼容模块...");

        // 初始化Useful-Backpacks兼容
        UsefulBackpacksCompat.initialize();

        // 初始化WearableBackpacks兼容
        WearableBackpacksCompat.initialize();

        initialized = true;
        LOGGER.info("兼容模块初始化完成");
    }

    /**
     * 检查是否有任何背包兼容模块可用
     */
    public static boolean isAnyBackpackModAvailable() {
        return UsefulBackpacksCompat.isAvailable() || WearableBackpacksCompat.isAvailable();
    }

    /**
     * 检查玩家是否有任何背包模组背包
     * 用于判断销毁类型是否生效
     * 
     * @param player 玩家
     * @return 是否有背包
     */
    public static boolean hasAnyBackpack(EntityPlayer player) {
        if (UsefulBackpacksCompat.isAvailable() && UsefulBackpacksCompat.hasBackpack(player)) {
            return true;
        }
        if (WearableBackpacksCompat.isAvailable() && WearableBackpacksCompat.hasBackpack(player)) {
            return true;
        }
        return false;
    }

    /**
     * 尝试将物品存入任何可用的背包
     * 
     * @param player 玩家
     * @param itemStack 要存入的物品
     * @param capability 戒指能力
     * @param preferBackpacks 是否优先使用背包
     * @return 成功存入的数量
     */
    public static int absorbToAnyBackpack(EntityPlayer player, ItemStack itemStack, 
                                          IRsRingCapability capability, boolean preferBackpacks) {
        if (player == null || itemStack == null || itemStack.isEmpty()) {
            return 0;
        }

        int inserted = 0;
        ItemStack remainingStack = itemStack.copy();

        if (preferBackpacks) {
            // 优先使用背包模式
            if (UsefulBackpacksCompat.isAvailable()) {
                inserted = UsefulBackpacksCompat.absorbToBackpacks(player, remainingStack, capability);
                if (inserted >= remainingStack.getCount()) {
                    return inserted;
                }
                remainingStack.shrink(inserted);
            }

            if (WearableBackpacksCompat.isAvailable() && !remainingStack.isEmpty()) {
                int wearableInserted = WearableBackpacksCompat.absorbToBackpack(player, remainingStack, capability);
                inserted += wearableInserted;
            }
        } else {
            // 非优先模式
            if (UsefulBackpacksCompat.isAvailable()) {
                inserted = UsefulBackpacksCompat.absorbToBackpacks(player, remainingStack, capability);
                if (inserted >= remainingStack.getCount()) {
                    return inserted;
                }
                remainingStack.shrink(inserted);
            }

            if (WearableBackpacksCompat.isAvailable() && !remainingStack.isEmpty()) {
                int wearableInserted = WearableBackpacksCompat.absorbToBackpack(player, remainingStack, capability);
                inserted += wearableInserted;
            }
        }

        return inserted;
    }

    /**
     * 尝试从任何可用的背包中销毁物品
     * 
     * @param player 玩家
     * @param toDestroy 要销毁的物品
     * @param capability 戒指能力
     * @return 成功销毁的数量
     */
    public static int destroyFromAnyBackpack(EntityPlayer player, ItemStack toDestroy, 
                                             IRsRingCapability capability) {
        if (player == null || toDestroy == null || toDestroy.isEmpty()) {
            return 0;
        }

        int destroyed = 0;
        int maxDestroy = toDestroy.getCount();

        // 1. 先尝试Useful-Backpacks
        if (UsefulBackpacksCompat.isAvailable()) {
            destroyed = UsefulBackpacksCompat.destroyFromBackpacks(player, toDestroy, capability);
            if (destroyed >= maxDestroy) {
                return destroyed;
            }
        }

        // 2. 再尝试WearableBackpacks
        if (WearableBackpacksCompat.isAvailable() && destroyed < maxDestroy) {
            ItemStack remaining = toDestroy.copy();
            remaining.shrink(destroyed);
            int wearableDestroyed = WearableBackpacksCompat.destroyFromBackpack(player, remaining, capability);
            destroyed += wearableDestroyed;
        }

        return destroyed;
    }

    /**
     * 检查是否可以存入任何背包
     */
    public static boolean canAbsorbToAnyBackpack(EntityPlayer player) {
        if (UsefulBackpacksCompat.isAvailable() && UsefulBackpacksCompat.canAbsorbToBackpacks(player)) {
            return true;
        }
        if (WearableBackpacksCompat.isAvailable() && WearableBackpacksCompat.canAbsorbToBackpack(player)) {
            return true;
        }
        return false;
    }

    /**
     * 检查是否可以从任何背包销毁
     */
    public static boolean canDestroyFromAnyBackpack(EntityPlayer player, ItemStack toDestroy) {
        if (UsefulBackpacksCompat.isAvailable() && UsefulBackpacksCompat.canDestroyFromBackpacks(player, toDestroy)) {
            return true;
        }
        if (WearableBackpacksCompat.isAvailable() && WearableBackpacksCompat.canDestroyFromBackpack(player, toDestroy)) {
            return true;
        }
        return false;
    }

    /**
     * 检查任何背包中是否已有满堆的指定物品
     * 
     * @param player 玩家
     * @param itemStack 要检查的物品
     * @return 是否有满堆
     */
    public static boolean hasFullStackInAnyBackpack(EntityPlayer player, ItemStack itemStack) {
        // 检查Useful-Backpacks
        if (UsefulBackpacksCompat.isAvailable()) {
            if (UsefulBackpacksCompat.hasFullStack(player, itemStack)) {
                return true;
            }
        }
        
        // 检查WearableBackpacks
        if (WearableBackpacksCompat.isAvailable()) {
            if (WearableBackpacksCompat.hasFullStack(player, itemStack)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查任何背包中是否有部分填充的指定物品槽位
     * 用于SLOT_OVERFLOW模式的第3层处理
     * 
     * @param player 玩家
     * @param itemStack 要检查的物品
     * @return 是否有部分填充的槽位
     */
    public static boolean hasPartialStackInAnyBackpack(EntityPlayer player, ItemStack itemStack) {
        // 检查Useful-Backpacks
        if (UsefulBackpacksCompat.isAvailable()) {
            if (UsefulBackpacksCompat.hasPartialStack(player, itemStack)) {
                return true;
            }
        }
        
        // 检查WearableBackpacks
        if (WearableBackpacksCompat.isAvailable()) {
            if (WearableBackpacksCompat.hasPartialStack(player, itemStack)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查所有背包是否已满
     * 
     * @param player 玩家
     * @return 是否已满
     */
    public static boolean areAllBackpacksFull(EntityPlayer player) {
        boolean anyBackpackChecked = false;
        
        // 检查Useful-Backpacks
        if (UsefulBackpacksCompat.isAvailable()) {
            anyBackpackChecked = true;
            if (!UsefulBackpacksCompat.isFull(player)) {
                return false;
            }
        }
        
        // 检查WearableBackpacks
        if (WearableBackpacksCompat.isAvailable()) {
            anyBackpackChecked = true;
            if (!WearableBackpacksCompat.isFull(player)) {
                return false;
            }
        }
        
        // 如果没有检查任何背包，返回false（表示不满）
        return anyBackpackChecked;
    }

    /**
     * 获取兼容模块状态信息（用于调试）
     */
    public static String getCompatStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("兼容模块状态:\n");
        sb.append("  Useful-Backpacks: ").append(UsefulBackpacksCompat.isAvailable() ? "已启用" : "未启用").append("\n");
        sb.append("  WearableBackpacks: ").append(WearableBackpacksCompat.isAvailable() ? "已启用" : "未启用").append("\n");
        return sb.toString();
    }
    
    /**
     * 检查是否优先使用背包
     * 根据配置决定是否优先将物品存入背包
     */
    public static boolean shouldPreferBackpacks() {
        // 检查配置
        if (UsefulBackpacksCompat.isAvailable() && 
            com.rsring.config.RsRingConfig.usefulBackpacksCompat.preferBackpacks) {
            return true;
        }
        if (WearableBackpacksCompat.isAvailable() && 
            com.rsring.config.RsRingConfig.wearableBackpacksCompat.preferBackpacks) {
            return true;
        }
        return false;
    }
    
    /**
     * 清理背包内的垃圾物品
     * 用于定时清理功能
     * 销毁类型在所有模式下都生效
     * 
     * @param player 玩家
     * @param capability 戒指能力
     */
    public static void cleanupBackpacks(EntityPlayer player, com.rsring.capability.IRsRingCapability capability) {
        if (UsefulBackpacksCompat.isAvailable()) {
            UsefulBackpacksCompat.cleanupBackpack(player, capability);
        }
        if (WearableBackpacksCompat.isAvailable()) {
            WearableBackpacksCompat.cleanupBackpack(player, capability);
        }
    }
}
