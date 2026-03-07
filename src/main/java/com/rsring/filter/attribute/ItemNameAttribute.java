package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品名称属性 - 按自定义名称过滤
 */
public class ItemNameAttribute implements ItemAttribute {
    
    private String itemName;
    
    public ItemNameAttribute() {
        this.itemName = "";
    }
    
    public ItemNameAttribute(String itemName) {
        this.itemName = itemName != null ? itemName : "";
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = extractCustomName(stack);
        return name.equals(itemName);
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        if (stack.isEmpty()) return attributes;
        
        String name = extractCustomName(stack);
        if (!name.isEmpty()) {
            attributes.add(new ItemNameAttribute(name));
        }
        return attributes;
    }
    
    private String extractCustomName(ItemStack stack) {
        if (stack.hasDisplayName()) {
            return stack.getDisplayName();
        }
        return "";
    }
    
    @Override
    public String getTranslationKey() {
        return "has_name";
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setString("name", itemName);
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        return new ItemNameAttribute(nbt.getString("name"));
    }
    
    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{itemName};
    }
}
