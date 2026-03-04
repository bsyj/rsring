package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 模组来源属性 - 按物品来源模组过滤
 */
public class ModAttribute implements ItemAttribute {
    
    private String modId;
    
    public ModAttribute() {
        this.modId = "";
    }
    
    public ModAttribute(String modId) {
        this.modId = modId;
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String stackModId = stack.getItem().getCreatorModId(stack);
        return modId.equals(stackModId);
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return Collections.emptyList();
        }
        String modId = stack.getItem().getCreatorModId(stack);
        return modId == null ? Collections.emptyList() : Arrays.asList(new ModAttribute(modId));
    }
    
    @Override
    public String getTranslationKey() {
        return "added_by";
    }
    
    @Override
    public void writeNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        nbt.setString("modId", modId);
    }
    
    @Override
    public ItemAttribute readNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        return new ModAttribute(nbt.getString("modId"));
    }
    
    @Override
    public Object[] getTranslationParameters() {
        // 获取模组显示名称
        net.minecraftforge.fml.common.ModContainer container = 
            net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(modId);
        if (container != null) {
            return new Object[]{container.getName()};
        }
        return new Object[]{modId};
    }
}
