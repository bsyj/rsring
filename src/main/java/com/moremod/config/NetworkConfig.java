package com.moremod.config;

import com.moremod.rsring.RsRingMod;
import net.minecraftforge.common.config.Configuration;

public class NetworkConfig implements IHasConfig {

    public static class NetworkSettings {
        public int packetSendFrequency = 10;
        public int packetSizeLimit = 4096;
        public boolean enableNetworkCompression = true;
    }

    public static NetworkSettings network = new NetworkSettings();

    @Override
    public void syncConfig(Configuration config) {
        config.addCustomCategoryComment(RsRingMod.MODID + ".network", "config.rsring.network");

        network.packetSendFrequency = config.getInt("packetSendFrequency",
            RsRingMod.MODID + ".network",
            10,
            1,
            30,
            "config.rsring.network.packetSendFrequency");

        network.packetSizeLimit = config.getInt("packetSizeLimit",
            RsRingMod.MODID + ".network",
            4096,
            1024,
            16384,
            "config.rsring.network.packetSizeLimit");

        network.enableNetworkCompression = config.getBoolean("enableNetworkCompression",
            RsRingMod.MODID + ".network",
            true,
            "config.rsring.network.enableNetworkCompression");
    }
}
