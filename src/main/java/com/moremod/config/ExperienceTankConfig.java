package com.moremod.config;

import com.moremod.rsring.RsRingMod;
import net.minecraftforge.common.config.Configuration;

public class ExperienceTankConfig implements IHasConfig {

    public static class TankConfig {
        public boolean enabled = true;
        public int xpExtractionRate = 20;
        public double xpExtractionRange = 5.0;
        public boolean extractXpBottles = true;
        public boolean mendingOn = true;
        public boolean mendPlayerItems = true;
        public boolean enableOverflowBottles = true;
        public boolean enableAutoPumping = true;
        public int pumpingInterval = 5;
        public int mendingInterval = 20;
        public int extractionInterval = 4;
        public int defaultPumpMode = 0;
        public int defaultRetainLevel = 1;
        public boolean defaultMendingMode = true;
        public int maxTankLevelLimit = 20;
        public boolean enableSpecialTanks = true;
        public double xpToBottleEfficiency = 1.0;
        public double xpMendingEfficiency = 1.0;
    }

    public static class ControllerConfig {
        public int maxManagedTanks = 32;
    }

    public static TankConfig tank = new TankConfig();
    public static ControllerConfig controller = new ControllerConfig();

    @Override
    public void syncConfig(Configuration config) {
        config.addCustomCategoryComment(RsRingMod.MODID + ".tank", "Experience Tank Configuration");
        config.addCustomCategoryComment(RsRingMod.MODID + ".controller", "Experience Pump Controller Configuration");
        
        tank.enabled = config.getBoolean("enabled", 
            RsRingMod.MODID + ".tank", 
            true, 
            "Enable/disable experience tank functionality");
            
        tank.xpExtractionRate = config.getInt("xpExtractionRate", 
            RsRingMod.MODID + ".tank", 
            20, 
            1, 
            1000, 
            "Maximum XP extraction per tick");
            
        tank.xpExtractionRange = config.get(RsRingMod.MODID + ".tank", "xpExtractionRange", 5.0D, "XP extraction range (blocks)").getDouble();
            
        tank.extractXpBottles = config.getBoolean("extractXpBottles", 
            RsRingMod.MODID + ".tank", 
            true, 
            "Extract XP bottles and convert to experience");
            
        tank.mendingOn = config.getBoolean("mendingOn", 
            RsRingMod.MODID + ".tank", 
            true, 
            "Default mending enabled");
            
        tank.mendPlayerItems = config.getBoolean("mendPlayerItems", 
            RsRingMod.MODID + ".tank", 
            true, 
            "Mend player inventory items");
            
        tank.enableOverflowBottles = config.getBoolean("enableOverflowBottles", 
            RsRingMod.MODID + ".tank", 
            true, 
            "Generate XP bottles when tank is full");
            
        tank.enableAutoPumping = config.getBoolean("enableAutoPumping", 
            RsRingMod.MODID + ".tank", 
            true, 
            "Enable auto pumping functionality");
            
        tank.pumpingInterval = config.getInt("pumpingInterval", 
            RsRingMod.MODID + ".tank", 
            5, 
            1, 
            100, 
            "Pumping interval (ticks)");
            
        tank.mendingInterval = config.getInt("mendingInterval", 
            RsRingMod.MODID + ".tank", 
            20, 
            1, 
            100, 
            "Mending interval (ticks)");
            
        tank.extractionInterval = config.getInt("extractionInterval", 
            RsRingMod.MODID + ".tank", 
            4, 
            1, 
            100, 
            "Extraction interval (ticks)");
            
        tank.defaultPumpMode = config.getInt("defaultPumpMode", 
            RsRingMod.MODID + ".tank", 
            0, 
            0, 
            2, 
            "Default pump mode (0=off, 1=pump from player, 2=pump to player)");
            
        tank.defaultRetainLevel = config.getInt("defaultRetainLevel", 
            RsRingMod.MODID + ".tank", 
            1, 
            0, 
            100, 
            "Default retain level");
            
        tank.defaultMendingMode = config.getBoolean("defaultMendingMode", 
            RsRingMod.MODID + ".tank", 
            true, 
            "Default mending mode");
            
        tank.maxTankLevelLimit = config.getInt("maxTankLevelLimit", 
            RsRingMod.MODID + ".tank", 
            20, 
            5, 
            100, 
            "Maximum tank level limit");
            
        tank.enableSpecialTanks = config.getBoolean("enableSpecialTanks", 
            RsRingMod.MODID + ".tank", 
            true, 
            "Enable special tanks");
            
        tank.xpToBottleEfficiency = config.get(RsRingMod.MODID + ".tank", "xpToBottleEfficiency", 1.0D, "XP to bottle conversion efficiency").getDouble();
            
        tank.xpMendingEfficiency = config.get(RsRingMod.MODID + ".tank", "xpMendingEfficiency", 1.0D, "XP mending efficiency").getDouble();
            
        controller.maxManagedTanks = config.getInt("maxManagedTanks", 
            RsRingMod.MODID + ".controller", 
            32, 
            1, 
            32, 
            "Maximum number of tanks the controller can manage");
    }

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