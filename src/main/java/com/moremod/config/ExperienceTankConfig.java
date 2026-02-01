package com.moremod.config;

/**
 * 经验储罐配置类 - 参考精妙背包设计，所有工作行为的可配置项，支持运行时修改
 * 基于1.12.2的配置系统实现
 */
public class ExperienceTankConfig {
    
    // 升级总开关
    public static boolean enabled = true;
    
    // 每刻最大抽取经验值（1秒=20刻）
    public static int xpExtractionRate = 20;
    
    // 经验抽取范围（格数）
    public static double xpExtractionRange = 5.0;
    
    // 是否抽取经验瓶物品并转换为经验
    public static boolean extractXpBottles = true;
    
    // 默认修补开关状态
    public static boolean mendingOn = true;
    
    // 是否修补玩家背包中的装备（关闭则仅修补储罐本身）
    public static boolean mendPlayerItems = true;
    
    // 溢出处理：是否在储罐满时生成经验瓶掉落
    public static boolean enableOverflowBottles = true;
    
    // 自动泵送开关：是否启用储罐的自动泵送功能
    public static boolean enableAutoPumping = true;
    
    // 泵送间隔（tick）：多少tick执行一次泵送操作
    public static int pumpingInterval = 5;
    
    // 修补间隔（tick）：多少tick执行一次修补检查
    public static int mendingInterval = 20;
    
    // 抽取间隔（tick）：多少tick执行一次经验抽取
    public static int extractionInterval = 4;
    
    /**
     * 获取配置的描述信息，用于调试和日志
     */
    public static String getConfigSummary() {
        return String.format(
            "ExperienceTankConfig: enabled=%s, extractionRate=%d, range=%.1f, " +
            "extractBottles=%s, mending=%s, mendPlayers=%s, overflow=%s, autoPump=%s",
            enabled, xpExtractionRate, xpExtractionRange, 
            extractXpBottles, mendingOn, mendPlayerItems, enableOverflowBottles, enableAutoPumping
        );
    }
    
    /**
     * 校验配置值的有效性
     */
    public static boolean validateConfig() {
        if (xpExtractionRate <= 0) {
            xpExtractionRate = 20;
            return false;
        }
        if (xpExtractionRange <= 0) {
            xpExtractionRange = 5.0;
            return false;
        }
        if (pumpingInterval <= 0) {
            pumpingInterval = 5;
            return false;
        }
        if (mendingInterval <= 0) {
            mendingInterval = 20;
            return false;
        }
        if (extractionInterval <= 0) {
            extractionInterval = 4;
            return false;
        }
        return true;
    }
    
    /**
     * 重置为默认配置
     */
    public static void resetToDefaults() {
        enabled = true;
        xpExtractionRate = 20;
        xpExtractionRange = 5.0;
        extractXpBottles = true;
        mendingOn = true;
        mendPlayerItems = true;
        enableOverflowBottles = true;
        enableAutoPumping = true;
        pumpingInterval = 5;
        mendingInterval = 20;
        extractionInterval = 4;
    }
}