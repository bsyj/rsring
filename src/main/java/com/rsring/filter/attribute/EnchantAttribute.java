package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 附魔属性 - 按物品附魔过滤
 */
public class EnchantAttribute implements ItemAttribute {
    
    private ResourceLocation enchantmentId;
    private int minLevel;
    private int maxLevel;
    
    public EnchantAttribute() {
        this.enchantmentId = null;
        this.minLevel = 1;
        this.maxLevel = Integer.MAX_VALUE;
    }
    
    public EnchantAttribute(ResourceLocation enchantmentId, int minLevel, int maxLevel) {
        this.enchantmentId = enchantmentId;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty() || !stack.isItemEnchanted()) {
            return false;
        }
        
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchant);
            
            // 如果指定了附魔 ID，检查是否匹配
            if (enchantmentId != null) {
                if (!enchantmentId.equals(id)) {
                    continue;
                }
            }
            
            // 检查等级范围
            if (level >= minLevel && level <= maxLevel) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.isItemEnchanted()) {
            return Collections.emptyList();
        }
        
        List<ItemAttribute> attributes = new ArrayList<>();
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchant);
            if (id != null) {
                attributes.add(new EnchantAttribute(id, level, level));
            }
        }
        
        return attributes;
    }
    
    @Override
    public String getTranslationKey() {
        return "enchantment";
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        if (enchantmentId != null) {
            nbt.setString("enchantmentId", enchantmentId.toString());
        }
        nbt.setInteger("minLevel", minLevel);
        nbt.setInteger("maxLevel", maxLevel);
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        ResourceLocation id = null;
        if (nbt.hasKey("enchantmentId")) {
            id = new ResourceLocation(nbt.getString("enchantmentId"));
        }
        int min = nbt.getInteger("minLevel");
        int max = nbt.getInteger("maxLevel");
        return new EnchantAttribute(id, min, max);
    }
    
    @Override
    public Object[] getTranslationParameters() {
        if (enchantmentId != null) {
            Enchantment enchant = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
            if (enchant != null) {
                return new Object[]{enchant.getTranslatedName(minLevel), minLevel};
            }
            return new Object[]{enchantmentId.toString(), minLevel};
        }
        return new Object[]{"Any", minLevel, maxLevel};
    }
}
