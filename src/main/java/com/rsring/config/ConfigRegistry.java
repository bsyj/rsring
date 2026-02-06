package com.rsring.config;

import java.util.ArrayList;
import net.minecraftforge.common.config.Configuration;

import com.rsring.config.ExperienceTankConfig;
import com.rsring.config.RsRingConfig;

public class ConfigRegistry {

    public static ArrayList<IHasConfig> configHandlers;
    private static Configuration config;

    public static Configuration getConfig() {
        return config;
    }

    public static void init(Configuration c) {
        config = c;
        config.load();
        configHandlers = new ArrayList<IHasConfig>();
    }

    public static void register(IHasConfig c) {
        configHandlers.add(c);
    }

    public static void syncAllConfig() {
        for (IHasConfig conf : ConfigRegistry.configHandlers) {
            conf.syncConfig(config);
        }
        boolean changed = false;
        if (RsRingConfig.validateConfig()) {
            changed = true;
        }
        if (ExperienceTankConfig.validateConfig()) {
            changed = true;
        }

        // 移除不需要的配置类别 (general 和 network)
        if (config.hasCategory("rsring.general")) {
            config.removeCategory(config.getCategory("rsring.general"));
            changed = true;
        }
        if (config.hasCategory("rsring.network")) {
            config.removeCategory(config.getCategory("rsring.network"));
            changed = true;
        }

        if (changed) {
            config.save();
            return;
        }
        config.save();
    }
}
