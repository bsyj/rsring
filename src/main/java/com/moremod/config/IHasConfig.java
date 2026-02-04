package com.moremod.config;

import net.minecraftforge.common.config.Configuration;

public interface IHasConfig {
    void syncConfig(Configuration config);
}