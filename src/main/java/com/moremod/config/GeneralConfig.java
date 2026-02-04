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
        config.addCustomCategoryComment(RsRingMod.MODID + ".general", "General Configuration");
        
        general.enableBaublesIntegration = config.getBoolean("enableBaublesIntegration", 
            RsRingMod.MODID + ".general", 
            true, 
            "Enable/disable Baubles integration");
            
        general.enableSoundEffects = config.getBoolean("enableSoundEffects", 
            RsRingMod.MODID + ".general", 
            true, 
            "Enable/disable sound effects");
            
        general.enableParticleEffects = config.getBoolean("enableParticleEffects", 
            RsRingMod.MODID + ".general", 
            true, 
            "Enable/disable particle effects");
            
        general.debugMode = config.getBoolean("debugMode", 
            RsRingMod.MODID + ".general", 
            false, 
            "Debug mode");
            
        general.autoSaveInterval = config.getInt("autoSaveInterval", 
            RsRingMod.MODID + ".general", 
            300, 
            60, 
            3600, 
            "Auto-save interval (seconds)");
    }
}
