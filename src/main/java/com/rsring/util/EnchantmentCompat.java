package com.rsring.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 附魔兼容性工具类
 * 用于检测和处理第三方模组的附魔，如 So Many Enchantments 的 Advanced Mending
 */
public final class EnchantmentCompat {

    private static final Logger LOGGER = LogManager.getLogger(EnchantmentCompat.class);
    private static final String SME_MOD_ID = "somanyenchantments";
    private static final String ADVANCED_MENDING_NAME = "advancedmending";

    private static volatile boolean smeLoaded = false;
    private static volatile boolean initialized = false;
    private static volatile Enchantment advancedMendingEnch = null;

    private EnchantmentCompat() {}

    public static boolean isSMELoaded() {
        return smeLoaded;
    }

    public static void init() {
        if (initialized) return;
        synchronized (EnchantmentCompat.class) {
            if (initialized) return;
            if (!com.rsring.config.ExperienceTankConfig.tank.enableSMECompatibility) {
                initialized = true;
                return;
            }
            smeLoaded = Loader.isModLoaded(SME_MOD_ID);
            if (smeLoaded) {
                try {
                    advancedMendingEnch = Enchantment.getEnchantmentByLocation(SME_MOD_ID + ":" + ADVANCED_MENDING_NAME);
                    if (advancedMendingEnch != null) {
                        LOGGER.info("Successfully loaded So Many Enchantments - Advanced Mending compatibility");
                    }
                } catch (Throwable t) {
                    LOGGER.debug("Failed to load Advanced Mending enchantment: {}", t.toString());
                }
            }
            initialized = true;
        }
    }

    @Nullable
    public static Enchantment getAdvancedMending() {
        if (!initialized) init();
        return advancedMendingEnch;
    }

    public static int getAdvancedMendingLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Enchantment ench = getAdvancedMending();
        if (ench == null) return 0;
        return EnchantmentHelper.getEnchantmentLevel(ench, stack);
    }

    public static boolean hasAdvancedMending(ItemStack stack) {
        return getAdvancedMendingLevel(stack) > 0;
    }

    public static int getMendingLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.MENDING, stack);
    }

    public static boolean hasMending(ItemStack stack) {
        return getMendingLevel(stack) > 0;
    }

    public static boolean hasAnyMending(ItemStack stack) {
        return hasAdvancedMending(stack) || hasMending(stack);
    }

    public static MendingType getMendingType(ItemStack stack) {
        if (hasAdvancedMending(stack)) return MendingType.ADVANCED;
        if (hasMending(stack)) return MendingType.NORMAL;
        return MendingType.NONE;
    }

    public static int getMendingEfficiency(ItemStack stack) {
        MendingType type = getMendingType(stack);
        switch (type) {
            case ADVANCED:
                return 3;
            case NORMAL:
                return 2;
            default:
                return 0;
        }
    }

    public enum MendingType {
        NONE(0),
        NORMAL(2),
        ADVANCED(3);

        private final int durabilityPerXp;

        MendingType(int durabilityPerXp) {
            this.durabilityPerXp = durabilityPerXp;
        }

        public int getDurabilityPerXp() {
            return durabilityPerXp;
        }
    }
}
