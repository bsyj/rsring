package com.rsring.event;

import com.rsring.capability.ExperiencePumpCapability;
import com.rsring.capability.IExperiencePumpCapability;
import com.rsring.experience.ExperienceTankManager;
import com.rsring.item.ItemExperiencePump;
import com.rsring.rsring.RsRingMod;
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
                    // 检查是否为特殊储罐，如果是则禁止升级
                    if (pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank100 ||
                        pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank500 ||
                        pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank1000 ||
                        pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank2000) {
                        return ItemStack.EMPTY; // 特殊储罐不能应用合成升级
                    }
                } else if (stack.getItem() == net.minecraft.init.Items.ENDER_PEARL && pearlStack.isEmpty()) {
                    pearlStack = stack;
                } else if (stack.getItem() == net.minecraft.init.Items.EXPERIENCE_BOTTLE && bottleStack.isEmpty()) {
                    bottleStack = stack;
                }
            }

            if (pumpStack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            // 从原储罐读取：等级与经验必须从NBT读（capability反序列化顺序曾导致>1000经验被截断）
            int originalLevels = Math.max(1, ItemExperiencePump.getCapacityLevelsFromNBT(pumpStack));
            int originalXP = ItemExperiencePump.getXpStoredFromNBT(pumpStack);
            IExperiencePumpCapability originalCap = pumpStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            int originalMode = originalCap != null ? originalCap.getMode() : 0;
            int originalRetainLevel = originalCap != null ? originalCap.getRetainLevel() : 10;
            boolean originalMending = originalCap != null ? originalCap.isUseForMending() : false;

            LOGGER.info("=== Experience Tank Upgrade Start ===");
            LOGGER.info("Original - Levels: {}, XP: {}", originalLevels, originalXP);

            // 创建新的储罐并保持原始物品类型
            ItemStack result;
            // 保持原始物品类型，而不是总是创建基础经验泵
            if (pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank100) {
                result = new ItemStack(RsRingMod.experienceTank100);
            } else if (pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank500) {
                result = new ItemStack(RsRingMod.experienceTank500);
            } else if (pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank1000) {
                result = new ItemStack(RsRingMod.experienceTank1000);
            } else if (pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank2000) {
                result = new ItemStack(RsRingMod.experienceTank2000);
            } else {
                // 基础经验泵
                result = new ItemStack(RsRingMod.experiencePump);
            }
            
            // 对于特殊储罐，保持其固定容量，不增加等级
            // 对于基础经验泵，增加容量等级
            int newLevels = originalLevels;
            if (result.getItem() instanceof ItemExperiencePump && !(result.getItem() instanceof com.rsring.item.ItemExperienceTank100) && 
                !(result.getItem() instanceof com.rsring.item.ItemExperienceTank500) && 
                !(result.getItem() instanceof com.rsring.item.ItemExperienceTank1000) && 
                !(result.getItem() instanceof com.rsring.item.ItemExperienceTank2000)) {
                newLevels = originalLevels + 1;
            }
            
            // 获取新储罐的能力并直接更新其值
            IExperiencePumpCapability newCap = result.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (newCap != null) {
                // 必须先设置容量等级，再设置XP，否则 setXpStored 会被默认1级容量(1000)截断导致经验丢失
                newCap.setCapacityLevels(newLevels);
                newCap.setXpStored(originalXP);
                newCap.setMode(originalMode);
                newCap.setRetainLevel(originalRetainLevel);
                newCap.setUseForMending(originalMending);
                
                // 将能力同步到NBT（确保持久化）
                ItemExperiencePump.syncCapabilityToStack(result, newCap);
                
                LOGGER.info("After capability update - Levels: {}, XP: {}", newCap.getCapacityLevels(), newCap.getXpStored());
            } else {
                LOGGER.error("ERROR: Could not get capability for new experience tank!");
            }
            
            // 验证最终结果
            int finalLevels = ItemExperiencePump.getCapacityLevelsFromNBT(result);
            int finalXP = ItemExperiencePump.getXpStoredFromNBT(result);
            int finalMaxXP = ItemExperiencePump.getMaxXpFromNBT(result);
            LOGGER.info("Final verification - Levels: {}, XP: {}, MaxXP: {}", finalLevels, finalXP, finalMaxXP);
            
            if (finalXP != originalXP || finalLevels != newLevels) {
                LOGGER.error("ERROR: Final data mismatch! Expected Levels={}, XP={}, but got Levels={}, XP={}", 
                    newLevels, originalXP, finalLevels, finalXP);
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
                    // 检查是否为特殊储罐，如果是则禁止升级
                    if (pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank100 ||
                        pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank500 ||
                        pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank1000 ||
                        pumpStack.getItem() instanceof com.rsring.item.ItemExperienceTank2000) {
                        return false; // 特殊储罐不能应用合成升级
                    }
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