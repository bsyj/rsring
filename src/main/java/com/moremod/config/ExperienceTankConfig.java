package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.moremod.rsring.RsRingMod;

/**
 * 经验储罐配置类 - 参考精妙背包设计，所有工作行为的可配置项，支持运行时修改
 * 基于1.12.2的配置系统实现
 */
@Config(modid = RsRingMod.MODID, name = "rsring/experience_tank_config")
public class ExperienceTankConfig {
    
    @Config.Comment({
        "经验储罐配置",
        "Experience Tank Configuration"
    })
    @Config.Name("Experience Tank Settings")
    public static TankConfig tank = new TankConfig();
    
    @Config.Comment({
        "经验泵控制器配置",
        "Experience Pump Controller Configuration"
    })
    @Config.Name("Pump Controller Settings")
    public static ControllerConfig controller = new ControllerConfig();
    
    public static class TankConfig {
        
        @Config.Comment({
            "升级总开关",
            "Enable/disable experience tank functionality"
        })
        @Config.Name("Enabled")
        public boolean enabled = true;
        
        @Config.Comment({
            "每刻最大抽取经验值（1秒=20刻）",
            "Maximum XP extraction per tick"
        })
        @Config.Name("XP Extraction Rate")
        @Config.RangeInt(min = 1, max = 1000)
        public int xpExtractionRate = 20;
        
        @Config.Comment({
            "经验抽取范围（格数）",
            "XP extraction range (blocks)"
        })
        @Config.Name("XP Extraction Range")
        @Config.RangeDouble(min = 1.0, max = 10.0)
        public double xpExtractionRange = 5.0;
        
        @Config.Comment({
            "是否抽取经验瓶物品并转换为经验",
            "Extract XP bottles and convert to experience"
        })
        @Config.Name("Extract XP Bottles")
        public boolean extractXpBottles = true;
        
        @Config.Comment({
            "默认修补开关状态",
            "Default mending enabled"
        })
        @Config.Name("Mending Enabled")
        public boolean mendingOn = true;
        
        @Config.Comment({
            "是否修补玩家背包中的装备（关闭则仅修补储罐本身）",
            "Mend player inventory items"
        })
        @Config.Name("Mend Player Items")
        public boolean mendPlayerItems = true;
        
        @Config.Comment({
            "溢出处理：是否在储罐满时生成经验瓶掉落",
            "Generate XP bottles when tank is full"
        })
        @Config.Name("Enable Overflow Bottles")
        public boolean enableOverflowBottles = true;
        
        @Config.Comment({
            "自动泵送开关：是否启用储罐的自动泵送功能",
            "Enable auto pumping functionality"
        })
        @Config.Name("Enable Auto Pumping")
        public boolean enableAutoPumping = true;
        
        @Config.Comment({
            "泵送间隔（tick）：多少tick执行一次泵送操作",
            "Pumping interval (ticks)"
        })
        @Config.Name("Pumping Interval")
        @Config.RangeInt(min = 1, max = 100)
        public int pumpingInterval = 5;
        
        @Config.Comment({
            "修补间隔（tick）：多少tick执行一次修补检查",
            "Mending interval (ticks)"
        })
        @Config.Name("Mending Interval")
        @Config.RangeInt(min = 1, max = 100)
        public int mendingInterval = 20;
        
        @Config.Comment({
            "抽取间隔（tick）：多少tick执行一次经验抽取",
            "Extraction interval (ticks)"
        })
        @Config.Name("Extraction Interval")
        @Config.RangeInt(min = 1, max = 100)
        public int extractionInterval = 4;
        
        @Config.Comment({
            "经验泵默认模式",
            "Default pump mode (0=off, 1=pump from player, 2=pump to player)"
        })
        @Config.Name("Default Pump Mode")
        @Config.RangeInt(min = 0, max = 2)
        public int defaultPumpMode = 0;
        
        @Config.Comment({
            "经验泵默认保留等级",
            "Default retain level"
        })
        @Config.Name("Default Retain Level")
        @Config.RangeInt(min = 0, max = 100)
        public int defaultRetainLevel = 1;
        
        @Config.Comment({
            "经验泵默认修补模式",
            "Default mending mode"
        })
        @Config.Name("Default Mending Mode")
        public boolean defaultMendingMode = true;
        
        @Config.Comment({
            "经验储罐最大等级限制",
            "Maximum tank level limit"
        })
        @Config.Name("Max Tank Level Limit")
        @Config.RangeInt(min = 5, max = 100)
        public int maxTankLevelLimit = 20;
        
        @Config.Comment({
            "特殊储罐启用/禁用",
            "Enable special tanks"
        })
        @Config.Name("Enable Special Tanks")
        public boolean enableSpecialTanks = true;
        
        @Config.Comment({
            "经验瓶转换效率",
            "XP to bottle conversion efficiency"
        })
        @Config.Name("XP to Bottle Efficiency")
        @Config.RangeDouble(min = 0.1, max = 2.0)
        public double xpToBottleEfficiency = 1.0;
        
        @Config.Comment({
            "经验修补效率",
            "XP mending efficiency"
        })
        @Config.Name("XP Mending Efficiency")
        @Config.RangeDouble(min = 0.1, max = 2.0)
        public double xpMendingEfficiency = 1.0;
    }
    
    public static class ControllerConfig {
        
        @Config.Comment({
            "控制器管理的最大储罐数量",
            "Maximum number of tanks the controller can manage"
        })
        @Config.Name("Max Managed Tanks")
        @Config.RangeInt(min = 1, max = 32)
        public int maxManagedTanks = 32;
    }
    
    @Mod.EventBusSubscriber(modid = RsRingMod.MODID)
    public static class EventHandler {

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(RsRingMod.MODID)) {
                ConfigManager.sync(RsRingMod.MODID, Config.Type.INSTANCE);
            }
        }
    }
    
    /**
     * 获取配置的描述信息，用于调试和日志
     */
    public static String getConfigSummary() {
        return String.format(
            "ExperienceTankConfig: enabled=%s, extractionRate=%d, range=%.1f, " +
            "extractBottles=%s, mending=%s, mendPlayers=%s, overflow=%s, autoPump=%s, " +
            "defaultMode=%d, defaultRetain=%d, defaultMending=%s, maxLevel=%d, " +
            "specialTanks=%s, bottleEfficiency=%.1f, mendingEfficiency=%.1f",
            tank.enabled, tank.xpExtractionRate, tank.xpExtractionRange, 
            tank.extractXpBottles, tank.mendingOn, tank.mendPlayerItems, tank.enableOverflowBottles, tank.enableAutoPumping,
            tank.defaultPumpMode, tank.defaultRetainLevel, tank.defaultMendingMode, tank.maxTankLevelLimit,
            tank.enableSpecialTanks, tank.xpToBottleEfficiency, tank.xpMendingEfficiency
        );
    }
    
    /**
     * 校验配置值的有效性
     */
    public static boolean validateConfig() {
        boolean changed = false;
        
        if (tank.xpExtractionRate <= 0) {
            tank.xpExtractionRate = 20;
            changed = true;
        }
        if (tank.xpExtractionRange <= 0) {
            tank.xpExtractionRange = 5.0;
            changed = true;
        }
        if (tank.pumpingInterval <= 0) {
            tank.pumpingInterval = 5;
            changed = true;
        }
        if (tank.mendingInterval <= 0) {
            tank.mendingInterval = 20;
            changed = true;
        }
        if (tank.extractionInterval <= 0) {
            tank.extractionInterval = 4;
            changed = true;
        }
        if (tank.defaultPumpMode < 0 || tank.defaultPumpMode > 2) {
            tank.defaultPumpMode = 0;
            changed = true;
        }
        if (tank.defaultRetainLevel < 0) {
            tank.defaultRetainLevel = 1;
            changed = true;
        }
        if (tank.maxTankLevelLimit < 5) {
            tank.maxTankLevelLimit = 20;
            changed = true;
        }
        if (tank.xpToBottleEfficiency < 0.1) {
            tank.xpToBottleEfficiency = 1.0;
            changed = true;
        }
        if (tank.xpMendingEfficiency < 0.1) {
            tank.xpMendingEfficiency = 1.0;
            changed = true;
        }
        
        return changed;
    }
    
    /**
     * 重置为默认配置
     */
    public static void resetToDefaults() {
        tank.enabled = true;
        tank.xpExtractionRate = 20;
        tank.xpExtractionRange = 5.0;
        tank.extractXpBottles = true;
        tank.mendingOn = true;
        tank.mendPlayerItems = true;
        tank.enableOverflowBottles = true;
        tank.enableAutoPumping = true;
        tank.pumpingInterval = 5;
        tank.mendingInterval = 20;
        tank.extractionInterval = 4;
        tank.defaultPumpMode = 0;
        tank.defaultRetainLevel = 1;
        tank.defaultMendingMode = true;
        tank.maxTankLevelLimit = 20;
        tank.enableSpecialTanks = true;
        tank.xpToBottleEfficiency = 1.0;
        tank.xpMendingEfficiency = 1.0;
    }
}