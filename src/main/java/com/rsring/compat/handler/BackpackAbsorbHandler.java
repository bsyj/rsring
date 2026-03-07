package com.rsring.compat.handler;

import com.rsring.capability.IRsRingCapability;
import com.rsring.config.RsRingConfig;
import com.rsring.compat.usefulbackpacks.BackpackDetector;
import com.rsring.compat.usefulbackpacks.BackpackEntry;
import com.rsring.compat.inventory.BackpackInventoryHandler;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * 背包吸收处理器，当没有绑定箱子时将物品存入Useful-Backpacks背包
 */
public class BackpackAbsorbHandler {

    /**
     * 尝试将物品吸收到玩家的背包中
     */
    public static int tryAbsorbToBackpacks(EntityPlayer player, ItemStack itemStack, IRsRingCapability capability) {
        if (player == null || itemStack == null || itemStack.isEmpty()) {
            return 0;
        }

        if (!RsRingConfig.usefulBackpacksCompat.enabled) {
            return 0;
        }

        if (!BackpackDetector.isModLoaded()) {
            return 0;
        }

        List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
        if (backpacks.isEmpty()) {
            return 0;
        }

        int totalInserted = 0;
        ItemStack remaining = itemStack.copy();

        for (BackpackEntry entry : backpacks) {
            if (remaining.isEmpty()) {
                break;
            }

            ItemStack backpack = entry.getStack();
            if (backpack.isEmpty()) {
                continue;
            }

            int inserted = BackpackInventoryHandler.insertItem(backpack, remaining);

            if (inserted > 0) {
                totalInserted += inserted;
                remaining.shrink(inserted);

                BackpackDetector.markBackpackDirty(player, entry);

                if (!RsRingConfig.usefulBackpacksCompat.cascadeToNextBackpack) {
                    break;
                }
            }
        }

        return totalInserted;
    }

    /**
     * 尝试将实体物品吸收到背包
     *
     * @param player 玩家
     * @param entityItem 实体物品
     * @param capability 戒指能力
     * @return 是否成功吸收（全部或部分）
     */
    public static boolean tryAbsorbEntityItem(EntityPlayer player, EntityItem entityItem, IRsRingCapability capability) {
        if (player == null || entityItem == null || entityItem.isDead) {
            return false;
        }

        ItemStack itemStack = entityItem.getItem();
        if (itemStack.isEmpty()) {
            return false;
        }

        int inserted = tryAbsorbToBackpacks(player, itemStack, capability);
        if (inserted <= 0) {
            return false;
        }

        // 更新实体物品
        int remaining = itemStack.getCount() - inserted;
        if (remaining <= 0) {
            entityItem.setDead();
        } else {
            itemStack.setCount(remaining);
            entityItem.setItem(itemStack);
        }

        return true;
    }

    /**
     * 检查是否可以吸收到背包
     *
     * @param player 玩家
     * @return 是否有可用的背包空间
     */
    public static boolean canAbsorbToBackpacks(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        if (!RsRingConfig.usefulBackpacksCompat.enabled) {
            return false;
        }

        if (!BackpackDetector.isModLoaded()) {
            return false;
        }

        List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
        if (backpacks.isEmpty()) {
            return false;
        }

        // 检查是否有背包有空闲槽位
        for (BackpackEntry entry : backpacks) {
            ItemStack backpack = entry.getStack();
            if (!backpack.isEmpty() && !BackpackInventoryHandler.isFull(backpack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取可用于吸收的总空间
     *
     * @param player 玩家
     * @return 总空闲槽位数
     */
    public static int getTotalAvailableSpace(EntityPlayer player) {
        if (player == null || !RsRingConfig.usefulBackpacksCompat.enabled) {
            return 0;
        }

        if (!BackpackDetector.isModLoaded()) {
            return 0;
        }

        List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);
        int totalSpace = 0;

        for (BackpackEntry entry : backpacks) {
            ItemStack backpack = entry.getStack();
            if (!backpack.isEmpty()) {
                totalSpace += BackpackInventoryHandler.getEmptySlotsCount(backpack);
            }
        }

        return totalSpace;
    }

    /**
     * 获取第一个有空间的背包
     *
     * @param player 玩家
     * @return 背包条目，如果没有则返回null
     */
    public static BackpackEntry getFirstAvailableBackpack(EntityPlayer player) {
        if (player == null || !RsRingConfig.usefulBackpacksCompat.enabled) {
            return null;
        }

        if (!BackpackDetector.isModLoaded()) {
            return null;
        }

        List<BackpackEntry> backpacks = BackpackDetector.findAllBackpacks(player);

        for (BackpackEntry entry : backpacks) {
            ItemStack backpack = entry.getStack();
            if (!backpack.isEmpty() && !BackpackInventoryHandler.isFull(backpack)) {
                return entry;
            }
        }

        return null;
    }
}
