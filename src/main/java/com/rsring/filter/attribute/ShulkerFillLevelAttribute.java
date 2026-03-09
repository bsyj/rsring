package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 潜影盒填充等级属性 - 检测潜影盒的填充状态
 * 参照机械动力的 ShulkerFillLevelAttribute 实现
 */
public class ShulkerFillLevelAttribute implements ItemAttribute {
    
    public static final ShulkerFillLevelAttribute EMPTY = new ShulkerFillLevelAttribute(null);
    
    private final ShulkerLevels level;
    
    public ShulkerFillLevelAttribute() {
        this.level = null;
    }
    
    public ShulkerFillLevelAttribute(ShulkerLevels level) {
        this.level = level;
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        return level != null && level.canApply(stack);
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        
        for (ShulkerLevels lvl : ShulkerLevels.values()) {
            if (lvl.canApply(stack)) {
                attributes.add(new ShulkerFillLevelAttribute(lvl));
            }
        }
        
        return attributes;
    }
    
    @Override
    public String getTranslationKey() {
        return "shulker_level";
    }
    
    @Override
    public Object[] getTranslationParameters() {
        String param = level != null ? level.getKey() : "";
        return new Object[]{param};
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        if (level != null) {
            nbt.setString("level", level.getKey());
        }
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("level")) {
            return new ShulkerFillLevelAttribute(ShulkerLevels.fromKey(nbt.getString("level")));
        }
        return EMPTY;
    }
    
    /**
     * 潜影盒填充等级枚举
     */
    public enum ShulkerLevels {
        EMPTY("empty", amount -> amount == 0),
        PARTIAL("partial", amount -> amount > 0 && amount < 27),
        FULL("full", amount -> amount >= 27);
        
        private final String key;
        private final Predicate<Integer> predicate;
        
        ShulkerLevels(String key, Predicate<Integer> predicate) {
            this.key = key;
            this.predicate = predicate;
        }
        
        public String getKey() {
            return key;
        }
        
        public static ShulkerLevels fromKey(String key) {
            for (ShulkerLevels level : values()) {
                if (level.key.equals(key)) {
                    return level;
                }
            }
            return null;
        }
        
        /**
         * 检查是否可以应用到物品
         */
        public boolean canApply(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            
            // 检查是否是潜影盒
            if (!(stack.getItem() instanceof net.minecraft.item.ItemBlock)) {
                return false;
            }
            
            net.minecraft.block.Block block = ((net.minecraft.item.ItemBlock) stack.getItem()).getBlock();
            if (!(block instanceof net.minecraft.block.BlockShulkerBox)) {
                return false;
            }
            
            // 获取物品数量
            int itemCount = getItemCount(stack);
            return predicate.test(itemCount);
        }
        
        /**
         * 获取潜影盒中的物品数量
         */
        private int getItemCount(ItemStack stack) {
            if (!stack.hasTagCompound()) {
                return 0;
            }
            
            NBTTagCompound tag = stack.getTagCompound();
            if (!tag.hasKey("BlockEntityTag")) {
                return 0;
            }
            
            NBTTagCompound blockEntityTag = tag.getCompoundTag("BlockEntityTag");
            
            // 如果有战利品表，无法确定内容
            if (blockEntityTag.hasKey("LootTable")) {
                return -1;
            }
            
            if (!blockEntityTag.hasKey("Items")) {
                return 0;
            }
            
            NBTTagList items = blockEntityTag.getTagList("Items", 10);
            return items.tagCount();
        }
    }
}
