package com.moremod.capability;

/**
 * 经验泵饰品能力：存储经验、泵入/泵出、保留等级、经验修补。
 * 容量以“经验等级”为单位，默认10级，每级按100经验点计算；1经验球=1 mb（1点）。
 */
public interface IExperiencePumpCapability {

    /** 模式：从玩家泵入经验 */
    int MODE_PUMP_FROM_PLAYER = 0;
    /** 模式：向玩家泵出经验 */
    int MODE_PUMP_TO_PLAYER = 1;
    /** 模式：取消泵送 */
    int MODE_OFF = 2;

    /** 每级容量对应的经验点数（1级 = 1000 mB液体经验） */
    int XP_PER_LEVEL = 1000;

    /** 当前存储的经验点数 */
    int getXpStored();
    void setXpStored(int xp);
    /** 容量等级数（末影珍珠升级增加） */
    int getCapacityLevels();
    void setCapacityLevels(int levels);
    /** 最大可存储经验点数 = capacityLevels * XP_PER_LEVEL */
    int getMaxXp();

    /** 当前模式：0=从玩家泵入，1=向玩家泵出，2=关闭 */
    int getMode();
    void setMode(int mode);

    /** 保留经验等级（泵入时不低于此级，泵出时维持此级） */
    int getRetainLevel();
    void setRetainLevel(int level);

    /** 是否用存储的经验修复经验修补物品 */
    boolean isUseForMending();
    void setUseForMending(boolean use);

    /** 增加容量等级（末影珍珠右键），每珍珠+10级 */
    boolean addCapacityLevels(int levels);

    /** 存入经验，返回实际存入量 */
    int addXp(int amount);
    /** 取出经验，返回实际取出量 */
    int takeXp(int amount);

    IExperiencePumpCapability copy();
}
