package com.rsring.compat.usefulbackpacks;

import com.rsring.util.BaublesHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 背包检测器，用于在玩家物品栏中查找Useful-Backpacks背包
 */
public class BackpackDetector {
    private static final Logger LOGGER = LogManager.getLogger(BackpackDetector.class);

    // Useful-Backpacks模组ID和物品名称
    public static final String BACKPACK_MODID = "usefulbackpacks";
    public static final String BACKPACK_ITEM_NAME = "backpack";

    // 背包物品名称列表（Useful-Backpacks只有一个注册名"backpack"，通过metadata区分大小）
    private static final String[] BACKPACK_ITEM_NAMES = {
        "backpack"  // 小/中/大背包都是同一个物品ID，通过metadata区分
    };

    // 模组是否加载的缓存
    private static Boolean modLoaded = null;

    /**
     * 检查Useful-Backpacks模组是否已加载
     */
    public static boolean isModLoaded() {
        if (modLoaded == null) {
            modLoaded = Loader.isModLoaded(BACKPACK_MODID);
            LOGGER.info("检查Useful-Backpacks模组加载状态: modid={}, loaded={}", BACKPACK_MODID, modLoaded);
            if (modLoaded) {
                LOGGER.info("Useful-Backpacks兼容模块已启用");
            } else {
                LOGGER.warn("Useful-Backpacks模组未加载，背包兼容功能将不可用");
            }
        }
        return modLoaded;
    }

    /**
     * 检查物品是否是Useful-Backpacks背包
     */
    public static boolean isBackpack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null || !BACKPACK_MODID.equals(registryName.getNamespace())) {
            return false;
        }
        // 检查是否匹配任一背包物品名称
        String itemPath = registryName.getPath();
        for (String name : BACKPACK_ITEM_NAMES) {
            if (name.equals(itemPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取背包大小（根据metadata判断）
     * 0=小背包(15格=5×3), 1=中背包(54格=9×6), 2=大背包(117格=13×9)
     */
    public static int getBackpackSize(ItemStack backpack) {
        if (!isBackpack(backpack)) return 0;
        int meta = backpack.getMetadata();
        switch (meta) {
            case 0: return 15;   // 小背包 5×3
            case 1: return 54;   // 中背包 9×6
            case 2: return 117;  // 大背包 13×9
            default: return 15;
        }
    }

    /**
     * 获取背包显示名称
     */
    public static String getBackpackTypeName(ItemStack backpack) {
        if (!isBackpack(backpack)) return "未知";
        int meta = backpack.getMetadata();
        switch (meta) {
            case 0: return "小背包(15格)";
            case 1: return "中背包(54格)";
            case 2: return "大背包(117格)";
            default: return "背包";
        }
    }

    /**
     * 查找玩家身上所有背包，按优先级排序
     * 优先级：主手 > 副手 > Baubles饰品栏 > 快捷栏 > 背包
     */
    public static List<BackpackEntry> findAllBackpacks(EntityPlayer player) {
        List<BackpackEntry> backpacks = new ArrayList<>();
        if (player == null || !isModLoaded()) return backpacks;

        // 1. 检查主手
        ItemStack mainHand = player.getHeldItemMainhand();
        if (isBackpack(mainHand)) {
            backpacks.add(new BackpackEntry(mainHand, BackpackEntry.InventoryLocation.MAIN_HAND, 0));
        }

        // 2. 检查副手
        ItemStack offHand = player.getHeldItemOffhand();
        if (isBackpack(offHand)) {
            backpacks.add(new BackpackEntry(offHand, BackpackEntry.InventoryLocation.OFF_HAND, 0));
        }

        // 3. 检查Baubles饰品栏
        if (BaublesHelper.isBaublesLoaded()) {
            findBackpacksInBaubles(player, backpacks);
        }

        // 4. 检查快捷栏(0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isBackpack(stack)) {
                backpacks.add(new BackpackEntry(stack, BackpackEntry.InventoryLocation.HOTBAR, i));
            }
        }

        // 5. 检查背包栏(9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isBackpack(stack)) {
                backpacks.add(new BackpackEntry(stack, BackpackEntry.InventoryLocation.INVENTORY, i));
            }
        }

        // 按优先级排序
        Collections.sort(backpacks, Comparator.comparingInt(BackpackEntry::getPriority));

        return backpacks;
    }

    /**
     * 查找第一个可用的背包
     */
    public static BackpackEntry findFirstBackpack(EntityPlayer player) {
        List<BackpackEntry> backpacks = findAllBackpacks(player);
        return backpacks.isEmpty() ? null : backpacks.get(0);
    }

    /**
     * 检查玩家是否有任何背包
     */
    public static boolean hasAnyBackpack(EntityPlayer player) {
        return !findAllBackpacks(player).isEmpty();
    }

    /**
     * 获取背包总数
     */
    public static int getBackpackCount(EntityPlayer player) {
        return findAllBackpacks(player).size();
    }

    /**
     * 在Baubles饰品栏中查找背包
     */
    private static void findBackpacksInBaubles(EntityPlayer player, List<BackpackEntry> backpacks) {
        try {
            Object handler = BaublesHelper.getBaublesHandler(player);
            if (handler == null) return;

            int size = BaublesHelper.getSlots(handler);
            for (int i = 0; i < size; i++) {
                ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                if (isBackpack(stack)) {
                    backpacks.add(new BackpackEntry(stack, BackpackEntry.InventoryLocation.BAUBLES, i));
                }
            }
        } catch (Exception e) {
            LOGGER.debug("扫描Baubles饰品栏时出错: {}", e.getMessage());
        }
    }

    /**
     * 刷新背包数据（标记为脏以触发保存）
     */
    public static void markBackpackDirty(EntityPlayer player, BackpackEntry entry) {
        if (player == null || entry == null) return;

        switch (entry.getLocation()) {
            case MAIN_HAND:
            case OFF_HAND:
            case HOTBAR:
            case INVENTORY:
                player.inventory.markDirty();
                break;
            case BAUBLES:
                if (BaublesHelper.isBaublesLoaded()) {
                    Object handler = BaublesHelper.getBaublesHandler(player);
                    if (handler instanceof net.minecraft.inventory.IInventory) {
                        ((net.minecraft.inventory.IInventory) handler).markDirty();
                    }
                }
                break;
        }
    }
}
