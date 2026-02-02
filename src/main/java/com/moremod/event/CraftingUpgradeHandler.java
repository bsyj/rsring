package com.moremod.event;

import com.moremod.capability.ExperiencePumpCapability;
import com.moremod.capability.IExperiencePumpCapability;
import com.moremod.experience.ExperienceTankManager;
import com.moremod.item.ItemExperiencePump;
import com.moremod.rsring.RsRingMod;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CraftingUpgradeHandler {
    
    private static final Logger LOGGER = LogManager.getLogger(CraftingUpgradeHandler.class);

    public static class UpgradeExperiencePumpRecipe extends ShapelessOreRecipe {

        public UpgradeExperiencePumpRecipe(ResourceLocation group) {
            super(group,
                  new ItemStack(RsRingMod.experiencePump),
                  new ItemStack(RsRingMod.experiencePump),
                  net.minecraft.init.Items.ENDER_PEARL,
                  net.minecraft.init.Items.EXPERIENCE_BOTTLE);
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventoryCrafting) {
            ItemStack pumpStack = ItemStack.EMPTY;
            ItemStack pearlStack = ItemStack.EMPTY;
            ItemStack bottleStack = ItemStack.EMPTY;

            // Find the components in the crafting grid
            for (int i = 0; i < inventoryCrafting.getSizeInventory(); i++) {
                ItemStack stack = inventoryCrafting.getStackInSlot(i);
                if (stack.isEmpty()) continue;

                if (stack.getItem() instanceof ItemExperiencePump && pumpStack.isEmpty()) {
                    pumpStack = stack;
                } else if (stack.getItem() == net.minecraft.init.Items.ENDER_PEARL && pearlStack.isEmpty()) {
                    pearlStack = stack;
                } else if (stack.getItem() == net.minecraft.init.Items.EXPERIENCE_BOTTLE && bottleStack.isEmpty()) {
                    bottleStack = stack;
                }
            }

            if (pumpStack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            // Create the upgraded tank result
            ItemStack result = new ItemStack(RsRingMod.experiencePump);

            // Get original tank data
            IExperiencePumpCapability originalCap = pumpStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            int originalLevels = originalCap != null ? originalCap.getCapacityLevels() : ItemExperiencePump.getCapacityLevelsFromNBT(pumpStack);
            int originalXP = originalCap != null ? originalCap.getXpStored() : ItemExperiencePump.getXpStoredFromNBT(pumpStack);
            int originalMode = originalCap != null ? originalCap.getMode() : 0;
            int originalRetainLevel = originalCap != null ? originalCap.getRetainLevel() : 10;
            boolean originalMending = originalCap != null ? originalCap.isUseForMending() : false;

            LOGGER.info("=== Experience Tank Upgrade Start ===");
            LOGGER.info("Original - Levels: {}, XP: {}", originalLevels, originalXP);

            // Set all properties in one go to avoid multiple syncs
            IExperiencePumpCapability resultCapability = result.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (resultCapability != null) {
                // 1. Set new capacity level (original + 1)
                int newLevels = originalLevels + 1;
                resultCapability.setCapacityLevels(newLevels);
                
                // 2. Get the new capacity after level increase
                int newCapacity = resultCapability.getMaxXp();
                
                // 3. Set stored XP (capped at new capacity)
                int preservedXP = Math.min(originalXP, newCapacity);
                resultCapability.setXpStored(preservedXP);
                
                // 4. Preserve other properties
                resultCapability.setMode(originalMode);
                resultCapability.setRetainLevel(originalRetainLevel);
                resultCapability.setUseForMending(originalMending);
                
                // 5. Single sync at the end
                ItemExperiencePump.syncCapabilityToStack(result, resultCapability);
                
                LOGGER.info("Final - Levels: {}, XP: {}, Capacity: {}", 
                    resultCapability.getCapacityLevels(), resultCapability.getXpStored(), resultCapability.getMaxXp());
            }
            LOGGER.info("=== Experience Tank Upgrade End ===");

            return result;
        }

        @Override
        public boolean matches(InventoryCrafting inv, World world) {
            ItemStack pumpStack = ItemStack.EMPTY;
            boolean hasPearl = false;
            boolean hasBottle = false;

            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;

                if (stack.getItem() instanceof ItemExperiencePump && pumpStack.isEmpty()) {
                    pumpStack = stack;
                } else if (stack.getItem() == net.minecraft.init.Items.ENDER_PEARL && !hasPearl) {
                    hasPearl = true;
                } else if (stack.getItem() == net.minecraft.init.Items.EXPERIENCE_BOTTLE && !hasBottle) {
                    hasBottle = true;
                } else {
                    // Found an invalid ingredient
                    return false;
                }
            }

            return !pumpStack.isEmpty() && hasPearl && hasBottle;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return new ItemStack(RsRingMod.experiencePump);
        }
    }

    // 确保该类可以被其他类访问
    public static UpgradeExperiencePumpRecipe createUpgradeRecipe() {
        return new UpgradeExperiencePumpRecipe(new ResourceLocation("rsring", "upgrade_experience_pump"));
    }
}