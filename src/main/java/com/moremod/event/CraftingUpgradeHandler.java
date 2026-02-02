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

            // Use ExperienceTankManager to handle the upgrade with experience preservation
            ExperienceTankManager manager = ExperienceTankManager.getInstance();

            // First, copy original capacity level (if any) then increase by 1 so upgrades stack
            IExperiencePumpCapability originalCap = pumpStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            int originalLevels = originalCap != null ? originalCap.getCapacityLevels() : ItemExperiencePump.getCapacityLevelsFromNBT(pumpStack);
            int originalXP = originalCap != null ? originalCap.getXpStored() : ItemExperiencePump.getXpStoredFromNBT(pumpStack);
            int originalCapacity = originalCap != null ? originalCap.getMaxXp() : ItemExperiencePump.getMaxXpFromNBT(pumpStack);

            LOGGER.info("=== Experience Tank Upgrade Start ===");
            LOGGER.info("Original - Levels: {}, XP: {}, Capacity: {}", originalLevels, originalXP, originalCapacity);

            IExperiencePumpCapability resultCapability = result.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (resultCapability != null) {
                // Set to original level + 1 (ExperiencePumpCapability will clamp to allowed range)
                resultCapability.setCapacityLevels(originalLevels + 1);
                // 先同步一次，确保容量更新到NBT
                ItemExperiencePump.syncCapabilityToStack(result, resultCapability);
                
                LOGGER.info("After capacity set - Levels: {}, Capacity: {}", 
                    resultCapability.getCapacityLevels(), resultCapability.getMaxXp());
            }

            // Then preserve experience and properties from the original tank
            result = manager.preserveExperienceOnUpgrade(pumpStack, result);
            
            resultCapability = result.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (resultCapability != null) {
                LOGGER.info("After preserve - XP: {}, Capacity: {}", 
                    resultCapability.getXpStored(), resultCapability.getMaxXp());
            }
            
            // 最后再次同步，确保所有数据都正确写入NBT（包括容量和经验）
            resultCapability = result.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (resultCapability != null) {
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