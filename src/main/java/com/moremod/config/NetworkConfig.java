package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.moremod.rsring.RsRingMod;

@Config(modid = RsRingMod.MODID, name = "rsring/network_config")
public class NetworkConfig {

    @Config.Comment({
        "网络配置",
        "Network Configuration"
    })
    @Config.Name("Network Settings")
    public static NetworkSettings network = new NetworkSettings();

    public static class NetworkSettings {
        
        @Config.Comment({
            "数据包发送频率（tick）",
            "Packet send frequency (ticks)"
        })
        @Config.Name("Packet Send Frequency")
        @Config.RangeInt(min = 1, max = 30)
        public int packetSendFrequency = 10;

        @Config.Comment({
            "数据包大小限制（字节）",
            "Packet size limit (bytes)"
        })
        @Config.Name("Packet Size Limit")
        @Config.RangeInt(min = 1024, max = 16384)
        public int packetSizeLimit = 4096;

        @Config.Comment({
            "启用/禁用网络压缩",
            "Enable/disable network compression"
        })
        @Config.Name("Enable Network Compression")
        public boolean enableNetworkCompression = true;
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
