package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.moremod.rsring.RsRingMod;

@Config(modid = RsRingMod.MODID, name = "rsring/config")
public class RsRingConfig {

    @Config.Comment("吸收戒指黑名单配置")
    public static BlacklistConfig blacklist = new BlacklistConfig();

    public static class BlacklistConfig {
        @Config.Comment("黑名单物品的资源位置列表，例如：diamond, iron_ingot")
        @Config.RequiresMcRestart
        @Config.Name("item_blacklist")
        public String[] itemBlacklist = new String[]{"xp_orb"};

        @Config.Comment("是否启用黑名单模式（true）还是白名单模式（false）")
        @Config.RequiresMcRestart
        @Config.Name("use_blacklist_mode")
        public boolean useBlacklistMode = true;
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