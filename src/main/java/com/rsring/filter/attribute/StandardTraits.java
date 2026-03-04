package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 标准特性枚举 - 预定义的物品属性
 */
public enum StandardTraits implements ItemAttribute {
    
    DUMMY(stack -> false, "dummy"),
    
    /**
     * 可放置方块
     */
    PLACEABLE(stack -> stack.getItem() instanceof net.minecraft.item.ItemBlock, "placeable"),
    
    /**
     * 可食用
     */
    CONSUMABLE(stack -> stack.getItem().getItemUseAction(stack) == net.minecraft.item.EnumAction.EAT, "consumable"),
    
    /**
     * 已附魔
     */
    ENCHANTED(ItemStack::isItemEnchanted, "enchanted"),
    
    /**
     * 附魔达到最大值
     */
    MAX_ENCHANTED(StandardTraits::isMaxEnchanted, "max_enchanted"),
    
    /**
     * 已重命名
     */
    RENAMED(ItemStack::hasDisplayName, "renamed"),
    
    /**
     * 已损坏
     */
    DAMAGED(ItemStack::isItemDamaged, "damaged"),
    
    /**
     * 严重损坏（耐久度低于 25%）
     */
    BADLY_DAMAGED(stack -> stack.isItemDamaged() && (float) stack.getItemDamage() / stack.getMaxDamage() > 0.75f, "badly_damaged"),
    
    /**
     * 不可堆叠
     */
    NOT_STACKABLE(stack -> !stack.isStackable(), "not_stackable"),
    
    /**
     * 可装备
     */
    EQUIPABLE(stack -> {
        net.minecraft.inventory.EntityEquipmentSlot slot = stack.getItem().getEquipmentSlot(stack);
        return slot != null && slot.getSlotType() != net.minecraft.inventory.EntityEquipmentSlot.Type.HAND;
    }, "equipable"),
    
    /**
     * 燃料
     */
    FURNACE_FUEL(TileEntityFurnace::isItemFuel, "furnace_fuel");
    
    private final Predicate<ItemStack> test;
    private final String translationKey;
    
    StandardTraits(Predicate<ItemStack> test, String translationKey) {
        this.test = test;
        this.translationKey = translationKey;
    }
    
    /**
     * 检查附魔是否达到最大值
     */
    private static boolean isMaxEnchanted(ItemStack stack) {
        if (!stack.isItemEnchanted()) {
            return false;
        }
        
        net.minecraft.nbt.NBTTagList enchantments = stack.getEnchantmentTagList();
        if (enchantments == null) {
            return false;
        }
        
        for (int i = 0; i < enchantments.tagCount(); i++) {
            net.minecraft.nbt.NBTTagCompound enchantment = enchantments.getCompoundTagAt(i);
            int enchantId = enchantment.getShort("id");
            int level = enchantment.getShort("lvl");
            
            net.minecraft.enchantment.Enchantment enchant = net.minecraft.enchantment.Enchantment.getEnchantmentByID(enchantId);
            if (enchant != null && level >= enchant.getMaxLevel()) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        return test.test(stack);
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        for (StandardTraits trait : values()) {
            if (trait != DUMMY && trait.appliesTo(stack)) {
                attributes.add(trait);
            }
        }
        return attributes;
    }
    
    @Override
    public String getTranslationKey() {
        return translationKey;
    }
    
    @Override
    public void writeNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        nbt.setBoolean(name(), true);
    }
    
    @Override
    public ItemAttribute readNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        for (StandardTraits trait : values()) {
            if (nbt.hasKey(trait.name())) {
                return trait;
            }
        }
        return null;
    }
    
    @Override
    public String getNBTKey() {
        return "standard_trait";
    }
}
