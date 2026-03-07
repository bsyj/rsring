package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * 矿物词典属性 - 按矿物词典标签过滤
 * 1.12.2中矿物词典是模组间兼容的核心机制
 */
public class OreDictAttribute implements ItemAttribute {
    
    private String oreDictName;
    
    public OreDictAttribute() {
        this.oreDictName = "";
    }
    
    public OreDictAttribute(String oreDictName) {
        this.oreDictName = oreDictName;
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty() || oreDictName.isEmpty()) {
            return false;
        }
        int[] oreIDs = OreDictionary.getOreIDs(stack);
        int targetID = OreDictionary.getOreID(oreDictName);
        for (int id : oreIDs) {
            if (id == targetID) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        if (stack.isEmpty()) {
            return attributes;
        }
        
        int[] oreIDs = OreDictionary.getOreIDs(stack);
        for (int id : oreIDs) {
            String name = OreDictionary.getOreName(id);
            if (!name.equals("Unknown")) {
                attributes.add(new OreDictAttribute(name));
            }
        }
        return attributes;
    }
    
    @Override
    public String getTranslationKey() {
        return "ore_dict";
    }
    
    @Override
    public void writeNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        nbt.setString("oreDict", oreDictName);
    }
    
    @Override
    public ItemAttribute readNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        return new OreDictAttribute(nbt.getString("oreDict"));
    }
    
    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{oreDictName};
    }
    
    public String getOreDictName() {
        return oreDictName;
    }
    
    @Override
    public String getNBTKey() {
        return "ore_dict";
    }
    
    @Override
    public boolean canRead(net.minecraft.nbt.NBTTagCompound nbt) {
        return nbt.hasKey("ore_dict");
    }
}
