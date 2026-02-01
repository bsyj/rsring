package com.moremod.event;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import com.moremod.rsring.RsRingMod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class CraftingExperiencePumpController {

    public void registerRecipes() {
        // 经验泵控制器：金锭+红石
        GameRegistry.addShapedRecipe(
            new net.minecraft.util.ResourceLocation("rsring", "experience_pump_controller"),
            null,
            new ItemStack(RsRingMod.experiencePumpController),
            " R ",
            "G G",
            "GGG",
            'G', Items.GOLD_INGOT,
            'R', Items.REDSTONE
        );
    }
}