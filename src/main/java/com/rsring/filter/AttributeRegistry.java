package com.rsring.filter;

import com.rsring.filter.attribute.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * 属性注册表 - 注册所有可用的物品属性
 */
public class AttributeRegistry {
    
    /**
     * 初始化并注册所有属性类型
     */
    public static void init() {
        // 注册标准特性
        ItemAttribute.register(StandardTraits.DUMMY);
        ItemAttribute.register(StandardTraits.PLACEABLE);
        ItemAttribute.register(StandardTraits.CONSUMABLE);
        ItemAttribute.register(StandardTraits.ENCHANTED);
        ItemAttribute.register(StandardTraits.MAX_ENCHANTED);
        ItemAttribute.register(StandardTraits.RENAMED);
        ItemAttribute.register(StandardTraits.DAMAGED);
        ItemAttribute.register(StandardTraits.BADLY_DAMAGED);
        ItemAttribute.register(StandardTraits.NOT_STACKABLE);
        ItemAttribute.register(StandardTraits.EQUIPABLE);
        ItemAttribute.register(StandardTraits.FURNACE_FUEL);
        
        // 注册其他属性
        ItemAttribute.register(new ModAttribute());
        ItemAttribute.register(new NbtAttribute());
        ItemAttribute.register(new EnchantAttribute());
    }
    
    /**
     * 获取物品匹配的所有属性
     */
    public static java.util.List<ItemAttribute> getAttributesForItem(ItemStack stack) {
        java.util.List<ItemAttribute> attributes = new java.util.ArrayList<>();
        
        if (stack.isEmpty()) {
            return attributes;
        }
        
        // 收集所有属性
        for (ItemAttribute attribute : ItemAttribute.REGISTRY) {
            try {
                java.util.List<ItemAttribute> attrs = attribute.listAttributesOf(stack);
                if (attrs != null && !attrs.isEmpty()) {
                    attributes.addAll(attrs);
                }
            } catch (Exception e) {
                // 忽略错误
            }
        }
        
        return attributes;
    }
}
