package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流体内容属性 - 检测容器中的流体类型
 * 参照机械动力的 FluidContentsAttribute 实现
 */
public class FluidContentsAttribute implements ItemAttribute {
    
    public static final FluidContentsAttribute EMPTY = new FluidContentsAttribute(null);
    
    private final String fluidName;
    
    public FluidContentsAttribute() {
        this.fluidName = "";
    }
    
    public FluidContentsAttribute(@Nullable String fluidName) {
        this.fluidName = fluidName != null ? fluidName : "";
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        List<String> fluids = extractFluids(stack);
        return fluids.contains(fluidName);
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        
        if (stack.isEmpty()) {
            return attributes;
        }
        
        List<String> fluids = extractFluids(stack);
        for (String fluid : fluids) {
            if (!fluid.isEmpty()) {
                attributes.add(new FluidContentsAttribute(fluid));
            }
        }
        
        return attributes;
    }
    
    /**
     * 提取物品中的所有流体名称
     */
    private List<String> extractFluids(ItemStack stack) {
        List<String> fluids = new ArrayList<>();
        
        if (!stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
            return fluids;
        }
        
        IFluidHandlerItem handler = stack.getCapability(
            CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
        
        if (handler == null) {
            return fluids;
        }
        
        // 遍历所有储罐
        for (int i = 0; i < handler.getTankProperties().length; i++) {
            FluidStack fluidStack = handler.getTankProperties()[i].getContents();
            if (fluidStack != null && fluidStack.getFluid() != null) {
                String fluidName = fluidStack.getFluid().getName();
                if (!fluids.contains(fluidName)) {
                    fluids.add(fluidName);
                }
            }
        }
        
        return fluids;
    }
    
    @Override
    public String getTranslationKey() {
        return "has_fluid";
    }
    
    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{fluidName};
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setString("fluid", fluidName);
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        return new FluidContentsAttribute(nbt.getString("fluid"));
    }
    
    public String getFluidName() {
        return fluidName;
    }
}
