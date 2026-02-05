package com.rsring.capability;

/**
 * 经验泵饰品能力：存储经验、泵入/泵出、保留等级、经验修补。
 */
public interface IExperiencePumpCapability {

    /** 模式：从玩家泵入经验 */
    int MODE_PUMP_FROM_PLAYER = 0;
    /** 模式：向玩家泵出经验 */
    int MODE_PUMP_TO_PLAYER = 1;
    /** 模式：取消泵送 */
    int MODE_OFF = 2;

    /** 基础容量对应的经验点数（1级 = 1000 mB液体经验，后续每级翻倍） */
    int BASE_XP_PER_LEVEL = 1000;

    /**
     * 获取当前存储的经验点数
     * @return 当前存储的经验点数
     */
    int getXpStored();

    /**
     * 设置当前存储的经验点数
     * @param xp 要设置的经验点数
     */
    void setXpStored(int xp);

    /**
     * 获取容量等级数（末影珍珠升级增加）
     * @return 容量等级数
     */
    int getCapacityLevels();

    /**
     * 设置容量等级数
     * @param levels 要设置的容量等级数
     */
    void setCapacityLevels(int levels);

    /**
     * 获取最大可存储经验点数
     * @return 最大可存储经验点数 = BASE_XP_PER_LEVEL * 2^(capacityLevels-1)，即1级=1000, 2级=2000, 3级=4000...
     */
    int getMaxXp();

    /**
     * 获取当前模式
     * @return 当前模式：0=从玩家泵入，1=向玩家泵出，2=关闭
     */
    int getMode();

    /**
     * 设置当前模式
     * @param mode 要设置的模式
     */
    void setMode(int mode);

    /**
     * 获取保留经验等级（泵入时不低于此级，泵出时维持此级）
     * @return 保留经验等级
     */
    int getRetainLevel();

    /**
     * 设置保留经验等级
     * @param level 要设置的保留等级
     */
    void setRetainLevel(int level);

    /**
     * 检查是否用存储的经验修复经验修补物品
     * @return 如果使用存储经验修复则返回true，否则返回false
     */
    boolean isUseForMending();

    /**
     * 设置是否用存储的经验修复经验修补物品
     * @param use 是否使用存储经验修复
     */
    void setUseForMending(boolean use);

    /**
     * 增加容量等级（末影珍珠右键）
     * @param levels 要增加的等级数
     * @return 如果成功增加则返回true，否则返回false
     */
    boolean addCapacityLevels(int levels);

    /**
     * 存入经验
     * @param amount 要存入的经验量
     * @return 实际存入量
     */
    int addXp(int amount);

    /**
     * 取出经验
     * @param amount 要取出的经验量
     * @return 实际取出量
     */
    int takeXp(int amount);

    /**
     * 创建当前实例的副本
     * @return 当前实例的副本
     */
    IExperiencePumpCapability copy();
}
