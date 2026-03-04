package com.rsring.crafting;

import com.rsring.capability.RsRingCapability;
import com.rsring.capability.IRsRingCapability;
import com.rsring.item.ItemAbsorbRing;
import com.rsring.rsring.RsRingMod;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class RingSealRecipe extends ShapelessOreRecipe {

    public RingSealRecipe(ResourceLocation name) {
        super(name, new ItemStack(RsRingMod.absorbRing), RsRingMod.absorbRing);
        // 必须设置 registry name，否则注册时会抛出 NullPointerException
        setRegistryName(name);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack ringStack = ItemStack.EMPTY;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemAbsorbRing) {
                ringStack = stack.copy();
                break;
            }
        }

        if (ringStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        IRsRingCapability cap = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) {
            return ringStack;
        }

        cap.setSealed(!cap.isSealed());
        RsRingCapability.syncCapabilityToStack(ringStack, cap);

        return ringStack;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        int ringCount = 0;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ItemAbsorbRing) {
                ringCount++;
            } else {
                return false;
            }
        }

        return ringCount == 1;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(RsRingMod.absorbRing);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
