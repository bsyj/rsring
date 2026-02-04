package com.rsring.crafting;

import com.rsring.config.ExperienceTankConfig;
import com.rsring.rsring.RsRingMod;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * 特殊经验储罐配方注册。
 */
public class CustomTankRecipes {

    /**
     * 注册特殊储罐的合成配方。
     */
    public static void registerRecipes() {
        if (!ExperienceTankConfig.tank.enableSpecialTanks) {
            return;
        }
        if (RsRingMod.experienceTank100 == null || RsRingMod.experienceTank500 == null ||
            RsRingMod.experienceTank1000 == null || RsRingMod.experienceTank2000 == null) {
            return;
        }

        // 100级经验储罐：铁锭 + 经验泵
        GameRegistry.addShapedRecipe(
            new net.minecraft.util.ResourceLocation("rsring", "experience_tank_100"),
            null,
            new ItemStack(RsRingMod.experienceTank100),
            "III",
            "ITI",
            "III",
            'I', Items.IRON_INGOT,
            'T', RsRingMod.experiencePump
        );

        // 500级经验储罐：金锭 + 100级储罐
        GameRegistry.addShapedRecipe(
            new net.minecraft.util.ResourceLocation("rsring", "experience_tank_500"),
            null,
            new ItemStack(RsRingMod.experienceTank500),
            "GGG",
            "GTG",
            "GGG",
            'G', Items.GOLD_INGOT,
            'T', RsRingMod.experienceTank100
        );

        // 1000级经验储罐：绿宝石 + 500级储罐
        GameRegistry.addShapedRecipe(
            new net.minecraft.util.ResourceLocation("rsring", "experience_tank_1000"),
            null,
            new ItemStack(RsRingMod.experienceTank1000),
            "EEE",
            "ETE",
            "EEE",
            'E', Items.EMERALD,
            'T', RsRingMod.experienceTank500
        );

        // 2000级经验储罐：钻石 + 1000级储罐
        GameRegistry.addShapedRecipe(
            new net.minecraft.util.ResourceLocation("rsring", "experience_tank_2000"),
            null,
            new ItemStack(RsRingMod.experienceTank2000),
            "DDD",
            "DTD",
            "DDD",
            'D', Items.DIAMOND,
            'T', RsRingMod.experienceTank1000
        );
    }
}
