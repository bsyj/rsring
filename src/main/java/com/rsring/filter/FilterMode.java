package com.rsring.filter;

/**
 * 过滤模式枚举
 */
public enum FilterMode {
    ITEM("item", "物品过滤"),
    MOD("mod", "模组过滤"),
    ATTRIBUTE("attribute", "属性过滤");

    private final String name;
    private final String displayName;

    FilterMode(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 从名称获取过滤模式
     */
    public static FilterMode fromName(String name) {
        for (FilterMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return ITEM; // 默认返回物品过滤
    }

    /**
     * 获取下一个过滤模式
     */
    public FilterMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
