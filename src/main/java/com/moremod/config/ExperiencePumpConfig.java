package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = "rsring", name = "rsring/experience_pump")
public class ExperiencePumpConfig {

    @Config.Comment({"经验泵控制器配置"})
    public static final ExperiencePumpControllerConfig experiencePumpController = new ExperiencePumpControllerConfig();

    public static class ExperiencePumpControllerConfig {
        @Config.Comment({"是否启用经验泵控制器"})
        @Config.RequiresMcRestart
        public boolean enabled = true;

        @Config.Comment({"每刻最大抽取的经验值（1秒=20刻）"})
        @Config.RangeInt(min = 1, max = 1000)
        public int xpExtractionRate = 20;

        @Config.Comment({"经验泵最大可存储的经验值"})
        @Config.RangeInt(min = 100, max = 10000000)
        public int maxXpStorage = 10000;

        @Config.Comment({"默认是否开启自动修补功能"})
        public boolean mendingOn = true;

        @Config.Comment({"是否自动抽取周围的经验瓶物品并转换为经验"})
        public boolean extractXpBottles = true;

        @Config.Comment({"是否修补玩家背包中的装备（关闭则仅修补装备本身）"})
        public boolean mendPlayerItems = true;

        @Config.Comment({"经验泵抽取范围（格）"})
        @Config.RangeInt(min = 1, max = 10)
        public int xpExtractionRange = 5;
    }

    @Mod.EventBusSubscriber(modid = "rsring")
    public static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("rsring")) {
                ConfigManager.sync("rsring", Config.Type.INSTANCE);
            }
        }
    }
}