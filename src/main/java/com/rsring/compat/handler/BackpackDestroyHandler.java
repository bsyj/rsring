package com.rsring.compat.handler;

import com.rsring.capability.IRsRingCapability;
import com.rsring.config.RsRingConfig;
import com.rsring.compat.usefulbackpacks.BackpackDetector;
import com.rsring.compat.usefulbackpacks.BackpackEntry;
import com.rsring.compat.inventory.BackpackInventoryHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * 背包销毁处理器，当没有绑定垃圾箱时从Useful-Backpacks背包中销毁物品
 */
public class BackpackDestroyHandler {
    private static final Logger LOGGER = LogManager.getLogger(BackpackDestroyHandler.class);

    /**
     * 尝试从背包中销毁物品
     *
     * @param player 玩家
     * @param toDestroy 要销毁的物品
     * @param capability 戒指能力
     * @return 实际销毁的数量
     */
    public static int tryDestroyFromBackpacks(EntityPlayer player, ItemStack toDestroy, IRsRingCapability capability) {
        if (player == null || toDestroy == null || toDestroy.isEmpty()) {
            return 0;
        }

        // 检查兼容模块是否启用
        if (!RsRingConfig.usefulBackpacksCompat.enabled) {
            return 0;
        }

        // 检查是否启用了从背包销毁
        if (!RsRingConfig.usefulBackpacksCompat.destroyFromBackpacks) {
            return 0;
        }

        // 检查Useful-Backpacks模组是否加载
        if (!BackpackDetector.isModLoaded()) {
            return 0;
        }

        // 获取玩家身上所有背包
        List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
        if (backpacks.isEmpty()) {
            return 0;
        }

        int totalDestroyed = 0;
        int maxDestroy = toDestroy.getCount();

        // 尝试从各个背包中销毁物品
        for (BackpackEntry entry : backpacks) {
            if (totalDestroyed >= maxDestroy) {
                break;
            }

            ItemStack backpack = entry.getStack();
            if (backpack.isEmpty()) {
                continue;
            }

            // 计算还需要销毁的数量
            int remainingToDestroy = maxDestroy - totalDestroyed;

            // 尝试销毁物品
            int destroyed = BackpackInventoryHandler.destroyItem(backpack, toDestroy, remainingToDestroy);
            if (destroyed > 0) {
                totalDestroyed += destroyed;

                // 标记背包数据已更改
                BackpackDetector.markBackpackDirty(player, entry);

                LOGGER.debug("从 {} 中销毁了 {} 个 {}",
                        BackpackDetector.getBackpackTypeName(backpack),
                        destroyed,
                        toDestroy.getDisplayName());
            }
        }

        if (totalDestroyed > 0) {
            LOGGER.debug("总共从背包中销毁了 {} 个 {}", totalDestroyed, toDestroy.getDisplayName());
        }

        return totalDestroyed;
    }

    /**
     * 检查是否可以从背包中销毁物品
     *
     * @param player 玩家
     * @param toDestroy 要销毁的物品
     * @return 是否可以销毁
     */
    public static boolean canDestroyFromBackpacks(EntityPlayer player, ItemStack toDestroy) {
        if (player == null || toDestroy == null || toDestroy.isEmpty()) {
            return false;
        }

        if (!RsRingConfig.usefulBackpacksCompat.enabled) {
            return false;
        }

        if (!RsRingConfig.usefulBackpacksCompat.destroyFromBackpacks) {
            return false;
        }

        if (!BackpackDetector.isModLoaded()) {
            return false;
        }

        List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
        if (backpacks.isEmpty()) {
            return false;
        }

        // 检查是否有背包包含该物品
        for (BackpackEntry entry : backpacks) {
            ItemStack backpack = entry.getStack();
            if (!backpack.isEmpty()) {
                int count = BackpackInventoryHandler.getItemCount(backpack, toDestroy);
                if (count > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取背包中指定物品的总数量
     *
     * @param player 玩家
     * @param toCount 要统计的物品
     * @return 总数量
     */
    public static int getTotalItemCount(EntityPlayer player, ItemStack toCount) {
        if (player == null || toCount == null || toCount.isEmpty()) {
            return 0;
        }

        if (!RsRingConfig.usefulBackpacksCompat.enabled) {
            return 0;
        }

        if (!BackpackDetector.isModLoaded()) {
            return 0;
        }

        List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
        int totalCount = 0;

        for (BackpackEntry entry : backpacks) {
            ItemStack backpack = entry.getStack();
            if (!backpack.isEmpty()) {
                totalCount += BackpackInventoryHandler.getItemCount(backpack, toCount);
            }
        }

        return totalCount;
    }

    /**
     * 尝试从背包中销毁指定数量的物品
     *
     * @param player 玩家
     * @param toDestroy 要销毁的物品
     * @param maxDestroy 最大销毁数量
     * @return 实际销毁的数量
     */
    public static int destroyItems(EntityPlayer player, ItemStack toDestroy, int maxDestroy) {
        if (player == null || toDestroy == null || toDestroy.isEmpty() || maxDestroy <= 0) {
            return 0;
        }

        if (!RsRingConfig.usefulBackpacksCompat.enabled) {
            return 0;
        }

        if (!RsRingConfig.usefulBackpacksCompat.destroyFromBackpacks) {
            return 0;
        }

        if (!BackpackDetector.isModLoaded()) {
            return 0;
        }

        List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
        if (backpacks.isEmpty()) {
            return 0;
        }

        int totalDestroyed = 0;

        for (BackpackEntry entry : backpacks) {
            if (totalDestroyed >= maxDestroy) {
                break;
            }

            ItemStack backpack = entry.getStack();
            if (backpack.isEmpty()) {
                continue;
            }

            int remainingToDestroy = maxDestroy - totalDestroyed;
            int destroyed = BackpackInventoryHandler.destroyItem(backpack, toDestroy, remainingToDestroy);

            if (destroyed > 0) {
                totalDestroyed += destroyed;
                BackpackDetector.markBackpackDirty(player, entry);
            }
        }

        return totalDestroyed;
    }
}
