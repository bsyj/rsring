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

            // 从原储罐读取所有数据（优先从NBT读取，确保数据准确）
            int originalLevels = ItemExperiencePump.getCapacityLevelsFromNBT(pumpStack);
            int originalXP = ItemExperiencePump.getXpStoredFromNBT(pumpStack);
            
            // 从capability读取其他属性（如果有的话）
            IExperiencePumpCapability originalCap = pumpStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            int originalMode = originalCap != null ? originalCap.getMode() : 0;
            int originalRetainLevel = originalCap != null ? originalCap.getRetainLevel() : 10;
            boolean originalMending = originalCap != null ? originalCap.isUseForMending() : false;

            LOGGER.info("=== Experience Tank Upgrade Start ===");
            LOGGER.info("Original - Levels: {}, XP: {}", originalLevels, originalXP);

            // 创建新的储罐并直接写入NBT数据（绕过capability初始化问题）
            ItemStack result = new ItemStack(RsRingMod.experiencePump);
            int newLevels = originalLevels + 1;
            
            // 直接构造NBT数据
            net.minecraft.nbt.NBTTagCompound stackTag = new net.minecraft.nbt.NBTTagCompound();
            net.minecraft.nbt.NBTTagCompound dataTag = new net.minecraft.nbt.NBTTagCompound();
            
            dataTag.setInteger("xp", originalXP);
            dataTag.setInteger("capacityLevels", newLevels);
            dataTag.setInteger("mode", originalMode);
            dataTag.setInteger("retainLevel", originalRetainLevel);
            dataTag.setBoolean("mending", originalMending);
            
            stackTag.setTag(ItemExperiencePump.XP_TAG, dataTag);
            result.setTagCompound(stackTag);
            
            LOGGER.info("After NBT write - Levels: {}, XP: {}", newLevels, originalXP);
            
            // 验证NBT数据
            int nbtLevels = ItemExperiencePump.getCapacityLevelsFromNBT(result);
            int nbtXP = ItemExperiencePump.getXpStoredFromNBT(result);
            int nbtMaxXP = ItemExperiencePump.getMaxXpFromNBT(result);
            LOGGER.info("NBT verification - Levels: {}, XP: {}, MaxXP: {}", nbtLevels, nbtXP, nbtMaxXP);
            
            if (nbtXP != originalXP || nbtLevels != newLevels) {
                LOGGER.error("ERROR: NBT data mismatch! Expected Levels={}, XP={}, but got Levels={}, XP={}", 
                    newLevels, originalXP, nbtLevels, nbtXP);
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