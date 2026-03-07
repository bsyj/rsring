package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 颜色属性 - 按物品颜色过滤（1.12.2版本简化实现）
 */
public class ColorAttribute implements ItemAttribute {
    
    private int color;
    
    public ColorAttribute() {
        this.color = -1;
    }
    
    public ColorAttribute(int color) {
        this.color = color;
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 1.12.2 简化实现：检查物品名称是否包含颜色
        String name = stack.getDisplayName().toLowerCase();
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", 
                          "silver", "cyan", "purple", "blue", "brown", "green", "red", "black",
                          "白", "橙", "品红", "淡蓝", "黄", "黄绿", "粉", "灰", "淡灰", "青", "紫", "蓝", "棕", "绿", "红", "黑"};
        for (String c : colors) {
            if (name.contains(c)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        if (stack.isEmpty()) return attributes;
        
        // 1.12.2 简化实现：从物品注册名提取颜色
        String registryName = stack.getItem().getRegistryName() != null ? 
            stack.getItem().getRegistryName().toString().toLowerCase() : "";
        String[] colorNames = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", 
                              "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        
        for (int i = 0; i < colorNames.length; i++) {
            if (registryName.contains(colorNames[i])) {
                attributes.add(new ColorAttribute(i));
                break;
            }
        }
        
        return attributes;
    }
    
    @Override
    public String getTranslationKey() {
        return "color";
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setInteger("color", color);
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        return new ColorAttribute(nbt.getInteger("color"));
    }
    
    @Override
    public Object[] getTranslationParameters() {
        String[] colorNames = {"白色", "橙色", "品红色", "淡蓝色", "黄色", "黄绿色", "粉色", "灰色", 
                              "淡灰色", "青色", "紫色", "蓝色", "棕色", "绿色", "红色", "黑色"};
        if (color >= 0 && color < colorNames.length) {
            return new Object[]{colorNames[color]};
        }
        return new Object[]{"未知"};
    }
}
