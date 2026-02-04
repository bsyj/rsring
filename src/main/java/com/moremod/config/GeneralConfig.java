package com.moremod.config;

import com.moremod.rsring.RsRingMod;
import net.minecraftforge.common.config.Configuration;

public class GeneralConfig implements IHasConfig {

    public static class GeneralSettings {
        public boolean enableBaublesIntegration = true;
        public boolean enableSoundEffects = true;
        public boolean enableParticleEffects = true;
        public boolean debugMode = false;
        public int autoSaveInterval = 300;
    }

    public static GeneralSettings general = new GeneralSettings();

    @Override
    public void syncConfig(Configuration config) {
        config.addCustomCategoryComment(RsRingMod.MODID + ".general", "config.rsring.general");
        config.setCategoryLanguageKey(RsRingMod.MODID + ".general", "config.rsring.general");

        general.enableBaublesIntegration = config.getBoolean("enableBaublesIntegration",
            RsRingMod.MODID + ".general",
            true,
            "config.rsring.general.enableBaublesIntegration",
            "config.rsring.general.enableBaublesIntegration");
        config.getCategory(RsRingMod.MODID + ".general").get("enableBaublesIntegration").setRequiresMcRestart(true);

        general.enableSoundEffects = config.getBoolean("enableSoundEffects",
            RsRingMod.MODID + ".general",
            true,
            "config.rsring.general.enableSoundEffects",
            "config.rsring.general.enableSoundEffects");

        general.enableParticleEffects = config.getBoolean("enableParticleEffects",
            RsRingMod.MODID + ".general",
            true,
            "config.rsring.general.enableParticleEffects",
            "config.rsring.general.enableParticleEffects");

        general.debugMode = config.getBoolean("debugMode",
            RsRingMod.MODID + ".general",
            false,
            "config.rsring.general.debugMode",
            "config.rsring.general.debugMode");

        general.autoSaveInterval = config.getInt("autoSaveInterval",
            RsRingMod.MODID + ".general",
            300,
            60,
            3600,
            "config.rsring.general.autoSaveInterval",
            "config.rsring.general.autoSaveInterval");
    }
}
