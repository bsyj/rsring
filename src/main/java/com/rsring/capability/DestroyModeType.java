package com.rsring.capability;

/**
 * 销毁模式类型枚举
 * 参考精妙背包（Sophisticated Backpacks）的VoidType实现
 * 
 * ALWAYS - 总是销毁：符合过滤条件的物品直接销毁
 * SLOT_OVERFLOW - 槽位溢出销毁：当背包中已有满堆该物品时销毁新物品
 * STORAGE_OVERFLOW - 存储溢出销毁：当背包满了之后销毁新物品
 */
public enum DestroyModeType {
    ALWAYS(0, "always"),
    SLOT_OVERFLOW(1, "slot_overflow"),
    STORAGE_OVERFLOW(2, "storage_overflow");

    private final int id;
    private final String name;

    DestroyModeType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * 获取下一个模式（循环切换）
     */
    public DestroyModeType next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /**
     * 根据ID获取模式
     */
    public static DestroyModeType fromId(int id) {
        for (DestroyModeType type : VALUES) {
            if (type.id == id) {
                return type;
            }
        }
        return ALWAYS;
    }

    /**
     * 根据名称获取模式
     */
    public static DestroyModeType fromName(String name) {
        for (DestroyModeType type : VALUES) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return ALWAYS;
    }

    private static final DestroyModeType[] VALUES = values();
}
