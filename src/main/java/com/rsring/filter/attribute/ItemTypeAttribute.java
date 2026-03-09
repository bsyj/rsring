package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品类型属性 - 按物品默认名称/类型过滤
 * 显示物品本身的名称（如"钻石剑"、"草方块"等）
 */
public class ItemTypeAttribute implements ItemAttribute {

    private String itemTypeName;

    public ItemTypeAttribute() {
        this.itemTypeName = "";
    }

    public ItemTypeAttribute(String itemTypeName) {
        this.itemTypeName = itemTypeName != null ? itemTypeName : "";
    }

    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String typeName = extractTypeName(stack);
        return typeName.equals(itemTypeName);
    }

    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        if (stack.isEmpty()) return attributes;

        String typeName = extractTypeName(stack);
        if (!typeName.isEmpty()) {
            attributes.add(new ItemTypeAttribute(typeName));
        }
        return attributes;
    }

    /**
     * 提取物品的默认类型名称
     */
    private String extractTypeName(ItemStack stack) {
        // 获取物品的未本地化名称（显示名称）
        String displayName = stack.getItem().getItemStackDisplayName(stack);
        if (displayName == null || displayName.isEmpty()) {
            // 如果显示名称为空，尝试获取注册名
            if (stack.getItem().getRegistryName() != null) {
                return stack.getItem().getRegistryName().toString();
            }
            return "";
        }
        return displayName;
    }

    @Override
    public String getTranslationKey() {
        return "item_type";
    }

    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setString("itemType", itemTypeName);
    }

    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        return new ItemTypeAttribute(nbt.getString("itemType"));
    }

    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{itemTypeName};
    }

    public String getItemTypeName() {
        return itemTypeName;
    }
}
