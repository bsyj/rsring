package com.rsring.filter;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品属性接口 - 用于属性过滤模式
 * 参照机械动力的属性过滤器实现
 */
public interface ItemAttribute {

    /**
     * 属性注册表 - 存储所有已注册的属性类型实例
     */
    List<ItemAttribute> REGISTRY = new ArrayList<>();

    /**
     * 注册属性类型
     */
    static ItemAttribute register(ItemAttribute attribute) {
        REGISTRY.add(attribute);
        return attribute;
    }

    /**
     * 从 NBT 读取属性
     */
    static ItemAttribute fromNBT(NBTTagCompound nbt) {
        if (nbt == null) return null;

        try {
            for (ItemAttribute attribute : REGISTRY) {
                if (attribute != null && attribute.canRead(nbt)) {
                    return attribute.readNBT(nbt.getCompoundTag(attribute.getNBTKey()));
                }
            }
        } catch (Exception e) {
            // 安全处理异常
            return null;
        }
        return null;
    }

    /**
     * 检查物品是否具有此属性（带世界参数）
     * 参照机械动力的实现
     */
    default boolean appliesTo(ItemStack stack, World world) {
        return appliesTo(stack);
    }

    /**
     * 检查物品是否具有此属性
     */
    boolean appliesTo(ItemStack stack);

    /**
     * 列出物品的所有此类型属性（带世界参数）
     * 参照机械动力的实现
     */
    default List<ItemAttribute> listAttributesOf(ItemStack stack, World world) {
        return listAttributesOf(stack);
    }

    /**
     * 列出物品的所有此类型属性
     */
    List<ItemAttribute> listAttributesOf(ItemStack stack);

    /**
     * 获取翻译键
     */
    String getTranslationKey();

    /**
     * 写入 NBT
     */
    void writeNBT(NBTTagCompound nbt);

    /**
     * 从 NBT 读取
     */
    ItemAttribute readNBT(NBTTagCompound nbt);

    /**
     * 序列化到 NBT
     */
    default void serializeNBT(NBTTagCompound nbt) {
        NBTTagCompound compound = new NBTTagCompound();
        writeNBT(compound);
        nbt.setTag(getNBTKey(), compound);
    }

    /**
     * 获取 NBT 键名
     */
    default String getNBTKey() {
        return getTranslationKey();
    }

    /**
     * 检查 NBT 是否包含此属性
     */
    default boolean canRead(NBTTagCompound nbt) {
        return nbt.hasKey(getNBTKey());
    }

    /**
     * 格式化为显示文本
     */
    default ITextComponent format(boolean inverted) {
        String key = "item_attributes." + getTranslationKey() + (inverted ? ".inverted" : "");
        return new TextComponentTranslation(key, getTranslationParameters());
    }

    /**
     * 获取翻译参数
     */
    default Object[] getTranslationParameters() {
        return new Object[0];
    }
}
