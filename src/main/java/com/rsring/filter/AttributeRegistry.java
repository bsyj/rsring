package com.rsring.filter;

import com.rsring.filter.attribute.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * 属性注册表 - 注册所有可用的物品属性
 * 参照机械动力的属性过滤器实现
 */
public class AttributeRegistry {
    
    /**
     * 初始化并注册所有属性类型
     */
    public static void init() {
        // 注册标准特性 - 只需注册一个代理，因为StandardTraits使用统一的NBT键
        // readNBT会根据写入的枚举名称返回正确的trait
        ItemAttribute.register(StandardTraits.DUMMY);

        // 注册行为标签属性（模拟 1.20.x 的 minecraft:piglin_loved 等）
        ItemAttribute.register(new BehaviorTagAttribute());

        // 注册其他属性（参考机械动力）
        ItemAttribute.register(new ModAttribute());        // 模组来源
        ItemAttribute.register(new CreativeTabAttribute()); // 创造模式标签页
        ItemAttribute.register(new OreDictAttribute());    // 矿物词典标签
        ItemAttribute.register(new NbtAttribute());        // NBT数据
        ItemAttribute.register(new EnchantAttribute());    // 附魔
        ItemAttribute.register(new ColorAttribute());      // 颜色
        ItemAttribute.register(new ItemNameAttribute());   // 物品自定义名称
        ItemAttribute.register(new ItemTypeAttribute());   // 物品类型名称
        ItemAttribute.register(new BookAuthorAttribute()); // 书籍作者
        ItemAttribute.register(new BookCopyAttribute());   // 书籍复制状态
        ItemAttribute.register(new FluidContentsAttribute()); // 流体内容
        ItemAttribute.register(new ShulkerFillLevelAttribute()); // 潜影盒填充等级

        // 注册冒险模组相关属性
        ItemAttribute.register(new AdventureModAttribute()); // 冒险模组分类
        ItemAttribute.register(new AdventureItemTags());     // 冒险物品标签
    }
    
    /**
     * 获取物品匹配的所有属性
     * 用于在GUI中显示可选择添加的属性列表
     */
    public static java.util.List<ItemAttribute> getAttributesForItem(ItemStack stack) {
        java.util.List<ItemAttribute> attributes = new java.util.ArrayList<>();
        
        // 空值检查
        if (stack == null || stack.isEmpty()) {
            return attributes;
        }
        
        // 收集所有属性
        for (ItemAttribute attribute : ItemAttribute.REGISTRY) {
            if (attribute == null) continue;
            
            try {
                java.util.List<ItemAttribute> attrs = attribute.listAttributesOf(stack);
                if (attrs != null && !attrs.isEmpty()) {
                    // 过滤掉空属性
                    for (ItemAttribute attr : attrs) {
                        if (attr != null) {
                            attributes.add(attr);
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略错误，继续收集其他属性
            }
        }
        
        return attributes;
    }
}
