package com.rsring.event;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber
public class RecipeRegistryHandler {

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<net.minecraft.item.crafting.IRecipe> event) {
        // 注册经验泵升级合成配方
        CraftingUpgradeHandler.UpgradeExperiencePumpRecipe upgradeRecipe =
            CraftingUpgradeHandler.createUpgradeRecipe();

        event.getRegistry().register(upgradeRecipe.setRegistryName(new ResourceLocation("rsring", "upgrade_experience_pump_recipe")));
    }
}