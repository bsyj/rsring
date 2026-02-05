package com.rsring.service;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import com.rsring.util.BaublesHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 集中的戒指检测服务，提供统一的戒指检测逻辑
 * 跨所有物品栏位置，具有强大的错误处理和全面的日志记录。
 *
 * 搜索优先级：主手 → 副手 → Baubles 槽位 → 物品栏
 */
public class RingDetectionService {
    private static final Logger LOGGER = LogManager.getLogger(RingDetectionService.class);

    /**
     * 在玩家的物品栏位置中查找任何类型的戒指。
     * 按优先顺序搜索：主手 → 副手 → Baubles → 物品栏
     *
     * @param player 要搜索的玩家
     * @return 找到的第一个戒指，如果没有找到则返回 ItemStack.EMPTY
     */
    public static ItemStack findAnyRing(EntityPlayer player) {
        if (player == null) {
            LOGGER.debug("Player is null, cannot search for rings");
            return ItemStack.EMPTY;
        }

        LOGGER.debug("Starting ring search for player: {}", player.getName());

        // Try to find any ring type by checking common ring classes
        // This approach allows for extensibility with new ring types
        ItemStack result = ItemStack.EMPTY;
        if (!result.isEmpty()) {
            return result;
        }

        result = findRing(player, com.rsring.item.ItemAbsorbRing.class);
        if (!result.isEmpty()) {
            return result;
        }

        LOGGER.debug("No rings found for player: {}", player.getName());
        return ItemStack.EMPTY;
    }

    /**
     * 在玩家的物品栏位置中查找特定类型的戒指。
     * 按优先顺序搜索：主手 → 副手 → Baubles → 物品栏
     *
     * @param player 要搜索的玩家
     * @param ringClass 要搜索的特定戒指类
     * @return 如果找到则返回戒指，如果没有找到则返回 ItemStack.EMPTY
     */
    public static ItemStack findRing(EntityPlayer player, Class<? extends Item> ringClass) {
        if (player == null) {
            LOGGER.debug("Player is null, cannot search for ring of type: {}",
                ringClass != null ? ringClass.getSimpleName() : "null");
            return ItemStack.EMPTY;
        }

        if (ringClass == null) {
            LOGGER.debug("Ring class is null, cannot search for player: {}", player.getName());
            return ItemStack.EMPTY;
        }

        LOGGER.debug("Searching for ring type {} for player: {}",
            ringClass.getSimpleName(), player.getName());

        // Search in priority order
        ItemStack result;

        result = findInHands(player, ringClass);
        if (!result.isEmpty()) {
            logSearchResult("hands", true, result);
            return result;
        }

        // 2. Check Baubles slots
        result = findInBaubles(player, ringClass);
        if (!result.isEmpty()) {
            logSearchResult("Baubles", true, result);
            return result;
        }

        // 3. Check inventory
        result = findInInventory(player, ringClass);
        if (!result.isEmpty()) {
            logSearchResult("inventory", true, result);
            return result;
        }

        LOGGER.debug("Ring type {} not found for player: {}",
            ringClass.getSimpleName(), player.getName());
        return ItemStack.EMPTY;
    }

    /**
     * 在玩家的手部（主手和副手）中搜索戒指。
     *
     * @param player 要搜索的玩家
     * @param ringClass 要搜索的戒指类
     * @return 如果在手部找到则返回戒指，否则返回 ItemStack.EMPTY
     */
    private static ItemStack findInHands(EntityPlayer player, Class<? extends Item> ringClass) {
        LOGGER.debug("Searching hands for ring type: {}", ringClass.getSimpleName());

        // Check main hand first
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && ringClass.isInstance(mainHand.getItem())) {
            LOGGER.debug("Found ring in main hand: {}", mainHand.getItem().getClass().getSimpleName());
            return mainHand;
        }

        // Check off hand
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && ringClass.isInstance(offHand.getItem())) {
            LOGGER.debug("Found ring in off hand: {}", offHand.getItem().getClass().getSimpleName());
            return offHand;
        }

        LOGGER.debug("No ring found in hands");
        return ItemStack.EMPTY;
    }

    /**
     * 使用 BaublesIntegration 辅助类在 Baubles 槽位中搜索戒指。
     *
     * @param player 要搜索的玩家
     * @param ringClass 要搜索的戒指类
     * @return 如果在 Baubles 中找到则返回戒指，否则返回 ItemStack.EMPTY
     */
    private static ItemStack findInBaubles(EntityPlayer player, Class<? extends Item> ringClass) {
        return BaublesIntegration.findRingInBaubles(player, ringClass);
    }

    /**
     * 在玩家的主要物品栏中搜索戒指。
     *
     * @param player 要搜索的玩家
     * @param ringClass 要搜索的戒指类
     * @return 如果在物品栏中找到则返回戒指，否则返回 ItemStack.EMPTY
     */
    private static ItemStack findInInventory(EntityPlayer player, Class<? extends Item> ringClass) {
        LOGGER.debug("Searching inventory for ring type: {}", ringClass.getSimpleName());

        return searchInventoryForRing(player.inventory, ringClass);
    }

    /**
     * 遍历物品栏搜索特定类型戒指的辅助方法。
     *
     * @param inventory 要搜索的物品栏
     * @param ringClass 要搜索的戒指类
     * @return 如果找到则返回戒指，否则返回 ItemStack.EMPTY
     */
    private static ItemStack searchInventoryForRing(IInventory inventory, Class<? extends Item> ringClass) {
        if (inventory == null) {
            LOGGER.debug("Inventory is null, cannot search");
            return ItemStack.EMPTY;
        }

        int slots = inventory.getSizeInventory();
        LOGGER.debug("Searching inventory with {} slots", slots);

        for (int i = 0; i < slots; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) {
                LOGGER.debug("Found ring in slot {}: {}", i, stack.getItem().getClass().getSimpleName());
                return stack;
            }
        }

        LOGGER.debug("No ring found in inventory");
        return ItemStack.EMPTY;
    }

    /**
     * 记录戒指搜索操作的结果。
     *
     * @param location 执行搜索的位置
     * @param found 是否找到了戒指
     * @param result 找到的戒指（如果有）
     */
    private static void logSearchResult(String location, boolean found, ItemStack result) {
        if (found && !result.isEmpty()) {
            LOGGER.debug("Ring search successful - Location: {}, Ring: {}",
                location, result.getItem().getClass().getSimpleName());
        } else {
            LOGGER.debug("Ring search failed - Location: {}", location);
        }
    }

    /**
     * 将 Baubles 物品栏标记为脏以确保正确的同步。
     * 这是一个公共方法，委托给 BaublesIntegration 辅助类。
     *
     * @param player 其 Baubles 物品栏应被标记为脏的玩家
     */
    public static void markBaublesDirtyIfNeeded(EntityPlayer player) {
            if (player == null) {
                LOGGER.debug("Player is null, cannot mark Baubles dirty");
                return;
            }
            if (!BaublesHelper.isBaublesLoaded()) {
                LOGGER.debug("Baubles mod not loaded, skipping dirty marking");
                return;
            }
            Object handler = BaublesHelper.getBaublesHandler(player);
            if (handler instanceof IInventory) {
                ((IInventory) handler).markDirty();
                LOGGER.debug("Marked Baubles inventory dirty for player: {}", player.getName());
            } else {
                LOGGER.debug("Baubles handler is not IInventory, cannot mark dirty");
            }
        }




    /**
     * 检查 Baubles 模组是否可用以及 API 是否可访问。
     * 这是一个公共方法，委托给 BaublesIntegration 辅助类。
     *
     * @return 如果 Baubles 可用且 API 可访问则返回 true，否则返回 false
     */
    public static boolean isBaublesAvailable() {
            return BaublesHelper.isBaublesLoaded();
        }



    /**
     * 用于强大 Baubles API 集成的辅助类，具有增强的错误处理。
     * 封装所有 Baubles 特定逻辑，并在 Baubles 模组不存在或 API 调用失败时提供优雅的回退。
     */
    private static class BaublesIntegration {
        private static final Logger LOGGER = LogManager.getLogger(BaublesIntegration.class);

        /**
         * 在 Baubles 槽位中查找戒指，具有全面的错误处理。
         *
         * @param player 要搜索的玩家
         * @param ringClass 要搜索的戒指类
         * @return 如果在 Baubles 中找到则返回戒指，否则返回 ItemStack.EMPTY
         */
        public static ItemStack findRingInBaubles(EntityPlayer player, Class<? extends Item> ringClass) {
            if (player == null) {
                LOGGER.debug("Player is null, cannot search Baubles");
                return ItemStack.EMPTY;
            }
            if (ringClass == null) {
                LOGGER.debug("Ring class is null, cannot search Baubles");
                return ItemStack.EMPTY;
            }
            if (!BaublesHelper.isBaublesLoaded()) {
                LOGGER.debug("Baubles mod not loaded, skipping Baubles search");
                return ItemStack.EMPTY;
            }

            Object handler = BaublesHelper.getBaublesHandler(player);
            if (handler == null) {
                LOGGER.debug("Baubles handler is null");
                return ItemStack.EMPTY;
            }

            int size = BaublesHelper.getSlots(handler);
            for (int i = 0; i < size; i++) {
                ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) {
                    LOGGER.debug("Found ring in Baubles slot {}", i);
                    return stack;
                }
            }

            return ItemStack.EMPTY;
        }


        /**
         * 将 Baubles 物品栏标记为脏以确保正确的同步。
         * 此方法优雅地处理错误并在不中断操作的情况下记录失败。
         *
         * @param player 其 Baubles 物品栏应被标记为脏的玩家
         */
        public static void markBaublesDirtyIfNeeded(EntityPlayer player) {
            if (player == null) {
                LOGGER.debug("Player is null, cannot mark Baubles dirty");
                return;
            }
            if (!BaublesHelper.isBaublesLoaded()) {
                LOGGER.debug("Baubles mod not loaded, skipping dirty marking");
                return;
            }
            Object handler = BaublesHelper.getBaublesHandler(player);
            if (handler instanceof IInventory) {
                ((IInventory) handler).markDirty();
                LOGGER.debug("Marked Baubles inventory dirty for player: {}", player.getName());
            } else {
                LOGGER.debug("Baubles handler is not IInventory, cannot mark dirty");
            }
        }


        /**
         * 检查 Baubles 模组是否可用以及 API 是否可访问。
         *
         * @return 如果 Baubles 可用且 API 可访问则返回 true，否则返回 false
         */
        public static boolean isBaublesAvailable() {
            return BaublesHelper.isBaublesLoaded();
        }

    }
}
