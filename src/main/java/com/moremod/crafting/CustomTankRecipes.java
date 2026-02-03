package com.moremod.crafting;

import com.moremod.item.ItemExperienceTank100;
import com.moremod.item.ItemExperienceTank500;
import com.moremod.item.ItemExperienceTank1000;
import com.moremod.item.ItemExperienceTank2000;
import com.moremod.rsring.RsRingMod;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * 自定义储罐合成表注册类
 * 按照铁、金、绿宝石、钻石的顺序合成不同等级的储罐
 */
public class CustomTankRecipes {

    /**
     * 注册所有自定义储罐的合成表
     */
    public static void registerRecipes() {
        // 100级储罐合成表（铁锭 + 基础储罐）
        GameRegistry.addRecipe(
            new ItemStack(RsRingMod.experienceTank100),
            "III",
            "ITI",
            "III",
            'I', new ItemStack(Items.IRON_INGOT),
            'T', new ItemStack(RsRingMod.experiencePump) // 基础经验储罐
        );

        // 500级储罐合成表（金锭 + 100级储罐）
        GameRegistry.addRecipe(
            new ItemStack(RsRingMod.experienceTank500),
            "GGG",
            "GTG",
            "GGG",
            'G', new ItemStack(Items.GOLD_INGOT),
            'T', new ItemStack(RsRingMod.experienceTank100) // 100级经验储罐
        );

        // 1000级储罐合成表（绿宝石 + 500级储罐）
        GameRegistry.addRecipe(
            new ItemStack(RsRingMod.experienceTank1000),
            "EEE",
            "ETE",
            "EEE",
            'E', new ItemStack(Items.EMERALD),
            'T', new ItemStack(RsRingMod.experienceTank500) // 500级经验储罐
        );

        // 2000级储罐合成表（钻石 + 1000级储罐）
        GameRegistry.addRecipe(
            new ItemStack(RsRingMod.experienceTank2000),
            "DDD",
            "DTD",
            "DDD",
            'D', new ItemStack(Items.DIAMOND),
            'T', new ItemStack(RsRingMod.experienceTank1000) // 1000级经验储罐
        );
    }
}
