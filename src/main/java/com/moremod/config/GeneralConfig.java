package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.moremod.rsring.RsRingMod;

@Config(modid = RsRingMod.MODID, name = "rsring/general_config")
public class GeneralConfig {

    @Config.Comment({
        "通用配置",
        "General Configuration"
    })
    @Config.Name("General Settings")
    public static GeneralSettings general = new GeneralSettings();

    public static class GeneralSettings {
        
        @Config.Comment({
            "启用/禁用Baubles集成",
            "Enable/disable Baubles integration"
        })
        @Config.Name("Enable Baubles Integration")
        public boolean enableBaublesIntegration = true;

        @Config.Comment({
            "启用/禁用声音效果",
            "Enable/disable sound effects"
        })
        @Config.Name("Enable Sound Effects")
        public boolean enableSoundEffects = true;

        @Config.Comment({
            "启用/禁用粒子效果",
            "Enable/disable particle effects"
        })
        @Config.Name("Enable Particle Effects")
        public boolean enableParticleEffects = true;

        @Config.Comment({
            "调试模式",
            "Debug mode"
        })
        @Config.Name("Debug Mode")
        public boolean debugMode = false;

        @Config.Comment({
            "自动保存间隔（秒）",
            "Auto-save interval (seconds)"
        })
        @Config.Name("Auto-save Interval")
        @Config.RangeInt(min = 60, max = 3600)
        public int autoSaveInterval = 300;
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
}
