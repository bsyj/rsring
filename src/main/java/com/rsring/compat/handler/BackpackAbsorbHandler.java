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
    private static final Logger LOGGER = LogManager.getLogger(BackpackAbsorbHandler.class);

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

            // 检查玩家是否正在查看这个背包的GUI
            // 如果正在查看，跳过此背包（避免NBT冲突）
            if (isPlayerViewingBackpack(player, backpack)) {
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
                // 检查玩家是否正在查看这个背包的GUI
                if (isPlayerViewingBackpack(player, backpack)) {
                    continue;
                }
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
                // 检查玩家是否正在查看这个背包的GUI
                if (isPlayerViewingBackpack(player, backpack)) {
                    continue;
                }
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
                // 检查玩家是否正在查看这个背包的GUI
                if (isPlayerViewingBackpack(player, backpack)) {
                    continue;
                }
                return entry;
            }
        }

        return null;
    }

    /**
     * 检查玩家是否正在查看指定背包的GUI
     * 当Useful-Backpacks的GUI打开时，直接修改NBT会被覆盖
     *
     * @param player 玩家
     * @param backpack 背包物品
     * @return 是否正在查看该背包的GUI
     */
    private static boolean isPlayerViewingBackpack(EntityPlayer player, ItemStack backpack) {
        if (player == null || backpack.isEmpty()) {
            return false;
        }

        // 检查玩家是否打开了Useful-Backpacks的Container
        if (player.openContainer != null && player.openContainer != player.inventoryContainer) {
            String containerClassName = player.openContainer.getClass().getName();
            // Useful-Backpacks的容器类名
            if (containerClassName.equals("info.u_team.useful_backpacks.container.ContainerBackPack")) {
                // 检查是否是同一个背包（通过比较ItemStack）
                try {
                    // 通过反射获取ContainerBackPack中的背包ItemStack
                    java.lang.reflect.Field inventoryField = player.openContainer.getClass().getDeclaredField("inventory");
                    inventoryField.setAccessible(true);
                    Object inventory = inventoryField.get(player.openContainer);

                    if (inventory != null) {
                        // 检查inventory是否是InventoryBackPack类型
                        if (inventory.getClass().getName().equals("info.u_team.useful_backpacks.inventory.InventoryBackPack")) {
                            java.lang.reflect.Method getStackMethod = inventory.getClass().getMethod("getStack");
                            ItemStack openBackpackStack = (ItemStack) getStackMethod.invoke(inventory);

                            // 比较是否是同一个背包
                            return openBackpackStack == backpack;
                        }
                    }
                } catch (Exception e) {
                    // 反射失败，保守处理：如果打开了任何背包GUI，都认为是正在查看
                    LOGGER.debug("检查背包GUI状态时出错: {}", e.getMessage());
                    return true;
                }
            }
        }

        return false;
    }
}
