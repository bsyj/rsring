package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * 创造模式标签页属性 - 按创造模式分类标签页过滤
 * 参照机械动力的 InItemGroup 实现，适配 1.12.2 的 CreativeTabs
 */
public class CreativeTabAttribute implements ItemAttribute {

    public static final CreativeTabAttribute EMPTY = new CreativeTabAttribute(null);

    private final String tabLabel;
    private final String tabName;

    public CreativeTabAttribute() {
        this.tabLabel = "";
        this.tabName = "";
    }

    public CreativeTabAttribute(CreativeTabs tab) {
        this.tabLabel = tab != null ? tab.getTabLabel() : "";
        this.tabName = tab != null ? getTabDisplayName(tab) : "";
    }

    public CreativeTabAttribute(String tabLabel, String tabName) {
        this.tabLabel = tabLabel;
        this.tabName = tabName;
    }

    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty() || tabLabel.isEmpty()) {
            return false;
        }

        // 获取物品所在的创造模式标签页
        CreativeTabs itemTab = stack.getItem().getCreativeTab();
        if (itemTab == null) {
            return false;
        }

        return tabLabel.equals(itemTab.getTabLabel());
    }

    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();

        if (stack.isEmpty()) {
            return attributes;
        }

        // 获取物品所在的创造模式标签页
        CreativeTabs itemTab = stack.getItem().getCreativeTab();
        if (itemTab != null) {
            attributes.add(new CreativeTabAttribute(itemTab));
        }

        // 也检查其他标签页（有些物品可能在多个标签页中）
        for (CreativeTabs tab : CreativeTabs.CREATIVE_TAB_ARRAY) {
            if (tab != null && isItemInTab(stack, tab)) {
                // 避免重复添加
                boolean alreadyAdded = attributes.stream()
                    .anyMatch(attr -> {
                        if (attr instanceof CreativeTabAttribute) {
                            return ((CreativeTabAttribute) attr).tabLabel.equals(tab.getTabLabel());
                        }
                        return false;
                    });

                if (!alreadyAdded) {
                    attributes.add(new CreativeTabAttribute(tab));
                }
            }
        }

        return attributes;
    }

    /**
     * 检查物品是否在指定的创造模式标签页中
     */
    private boolean isItemInTab(ItemStack stack, CreativeTabs tab) {
        try {
            // 获取标签页的所有物品
            net.minecraft.util.NonNullList<ItemStack> tabItems = net.minecraft.util.NonNullList.create();
            tab.displayAllRelevantItems(tabItems);

            // 检查物品是否在该列表中
            for (ItemStack tabItem : tabItems) {
                if (tabItem.getItem() == stack.getItem()) {
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return false;
    }

    /**
     * 获取标签页的显示名称
     */
    private String getTabDisplayName(CreativeTabs tab) {
        try {
            // 1.12.2 使用 getTabLabel() 获取翻译键
            String label = tab.getTabLabel();
            // 尝试翻译
            return net.minecraft.util.text.translation.I18n.translateToLocal(label);
        } catch (Exception e) {
            return tab.getTabLabel();
        }
    }

    @Override
    public String getTranslationKey() {
        return "in_item_group";
    }

    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{tabName.isEmpty() ? tabLabel : tabName};
    }

    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setString("tabLabel", tabLabel);
        nbt.setString("tabName", tabName);
    }

    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        String label = nbt.getString("tabLabel");
        String name = nbt.hasKey("tabName") ? nbt.getString("tabName") : "";
        return new CreativeTabAttribute(label, name);
    }

    public String getTabLabel() {
        return tabLabel;
    }

    public String getTabName() {
        return tabName;
    }
}
