package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * NBT 标签属性 - 按物品 NBT 数据过滤
 */
public class NbtAttribute implements ItemAttribute {
    
    private String nbtPath;
    private String expectedValue;
    private MatchType matchType;
    
    public enum MatchType {
        EXACT,      // 精确匹配
        CONTAINS,   // 包含
        EXISTS      // 存在即可
    }
    
    public NbtAttribute() {
        this.nbtPath = "";
        this.expectedValue = "";
        this.matchType = MatchType.EXISTS;
    }
    
    public NbtAttribute(String nbtPath, String expectedValue, MatchType matchType) {
        this.nbtPath = nbtPath;
        this.expectedValue = expectedValue;
        this.matchType = matchType;
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }
        
        NBTTagCompound nbt = stack.getTagCompound();
        return checkNBT(nbt, nbtPath, expectedValue, matchType);
    }
    
    /**
     * 递归检查 NBT 路径
     */
    private boolean checkNBT(NBTTagCompound nbt, String path, String expectedValue, MatchType matchType) {
        if (path.isEmpty()) {
            return true;
        }
        
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return false;
        }
        
        String key = parts[0];
        
        // 如果是最后一级
        if (parts.length == 1) {
            if (matchType == MatchType.EXISTS) {
                return nbt.hasKey(key);
            }
            
            if (nbt.hasKey(key)) {
                String actualValue = nbt.getString(key);
                if (matchType == MatchType.EXACT) {
                    return expectedValue.equals(actualValue);
                } else if (matchType == MatchType.CONTAINS) {
                    return actualValue.contains(expectedValue);
                }
            }
            return false;
        }
        
        // 递归检查下一级
        if (nbt.hasKey(key) && nbt.getTag(key) instanceof NBTTagCompound) {
            String subPath = String.join(".", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            return checkNBT(nbt.getCompoundTag(key), subPath, expectedValue, matchType);
        }
        
        return false;
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return attributes;
        }
        
        // 提取主要的 NBT 路径作为属性
        extractNBTPaths(stack.getTagCompound(), "", attributes);
        
        return attributes;
    }
    
    /**
     * 提取 NBT 路径
     */
    private void extractNBTPaths(NBTTagCompound nbt, String prefix, List<ItemAttribute> attributes) {
        for (String key : nbt.getKeySet()) {
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            
            // 只提取顶层和次级路径
            if (nbt.getTag(key) instanceof NBTTagCompound) {
                attributes.add(new NbtAttribute(fullPath, "", MatchType.EXISTS));
                
                // 递归提取（最多两层）
                if (prefix.isEmpty()) {
                    extractNBTPaths(nbt.getCompoundTag(key), fullPath, attributes);
                }
            } else {
                attributes.add(new NbtAttribute(fullPath, nbt.getString(key), MatchType.EXACT));
            }
        }
    }
    
    @Override
    public String getTranslationKey() {
        return "nbt_tag";
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setString("nbtPath", nbtPath);
        nbt.setString("expectedValue", expectedValue);
        nbt.setString("matchType", matchType.name());
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        String path = nbt.getString("nbtPath");
        String value = nbt.getString("expectedValue");
        MatchType type = MatchType.valueOf(nbt.getString("matchType"));
        return new NbtAttribute(path, value, type);
    }
    
    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{nbtPath, matchType.name(), expectedValue};
    }
}
