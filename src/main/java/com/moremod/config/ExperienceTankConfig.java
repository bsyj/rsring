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
        config.addCustomCategoryComment(RsRingMod.MODID + ".tank", "config.rsring.tank");
        config.setCategoryLanguageKey(RsRingMod.MODID + ".tank", "config.rsring.tank");
        config.addCustomCategoryComment(RsRingMod.MODID + ".controller", "config.rsring.controller");
        config.setCategoryLanguageKey(RsRingMod.MODID + ".controller", "config.rsring.controller");

        tank.enabled = config.getBoolean("enabled",
            RsRingMod.MODID + ".tank",
            true,
            "config.rsring.tank.enabled",
            "config.rsring.tank.enabled");

        tank.xpExtractionRate = config.getInt("xpExtractionRate",
            RsRingMod.MODID + ".tank",
            20,
            1,
            1000,
            "config.rsring.tank.xpExtractionRate",
            "config.rsring.tank.xpExtractionRate");

        net.minecraftforge.common.config.Property xpRangeProp = config.get(RsRingMod.MODID + ".tank", "xpExtractionRange", 5.0D, "config.rsring.tank.xpExtractionRange");
        xpRangeProp.setLanguageKey("config.rsring.tank.xpExtractionRange");
        tank.xpExtractionRange = xpRangeProp.getDouble();

        tank.extractXpBottles = config.getBoolean("extractXpBottles",
            RsRingMod.MODID + ".tank",
            true,
            "config.rsring.tank.extractXpBottles",
            "config.rsring.tank.extractXpBottles");

        tank.mendingOn = config.getBoolean("mendingOn",
            RsRingMod.MODID + ".tank",
            true,
            "config.rsring.tank.mendingOn",
            "config.rsring.tank.mendingOn");

        tank.mendPlayerItems = config.getBoolean("mendPlayerItems",
            RsRingMod.MODID + ".tank",
            true,
            "config.rsring.tank.mendPlayerItems",
            "config.rsring.tank.mendPlayerItems");

        tank.enableOverflowBottles = config.getBoolean("enableOverflowBottles",
            RsRingMod.MODID + ".tank",
            true,
            "config.rsring.tank.enableOverflowBottles",
            "config.rsring.tank.enableOverflowBottles");

        tank.enableAutoPumping = config.getBoolean("enableAutoPumping",
            RsRingMod.MODID + ".tank",
            true,
            "config.rsring.tank.enableAutoPumping",
            "config.rsring.tank.enableAutoPumping");

        tank.pumpingInterval = config.getInt("pumpingInterval",
            RsRingMod.MODID + ".tank",
            5,
            1,
            100,
            "config.rsring.tank.pumpingInterval",
            "config.rsring.tank.pumpingInterval");

        tank.mendingInterval = config.getInt("mendingInterval",
            RsRingMod.MODID + ".tank",
            20,
            1,
            100,
            "config.rsring.tank.mendingInterval",
            "config.rsring.tank.mendingInterval");

        tank.extractionInterval = config.getInt("extractionInterval",
            RsRingMod.MODID + ".tank",
            4,
            1,
            100,
            "config.rsring.tank.extractionInterval",
            "config.rsring.tank.extractionInterval");

        tank.defaultPumpMode = config.getInt("defaultPumpMode",
            RsRingMod.MODID + ".tank",
            0,
            0,
            2,
            "config.rsring.tank.defaultPumpMode",
            "config.rsring.tank.defaultPumpMode");

        tank.defaultRetainLevel = config.getInt("defaultRetainLevel",
            RsRingMod.MODID + ".tank",
            1,
            0,
            100,
            "config.rsring.tank.defaultRetainLevel",
            "config.rsring.tank.defaultRetainLevel");

        tank.defaultMendingMode = config.getBoolean("defaultMendingMode",
            RsRingMod.MODID + ".tank",
            true,
            "config.rsring.tank.defaultMendingMode",
            "config.rsring.tank.defaultMendingMode");

        tank.maxTankLevelLimit = config.getInt("maxTankLevelLimit",
            RsRingMod.MODID + ".tank",
            20,
            5,
            100,
            "config.rsring.tank.maxTankLevelLimit",
            "config.rsring.tank.maxTankLevelLimit");

        tank.enableSpecialTanks = config.getBoolean("enableSpecialTanks",
            RsRingMod.MODID + ".tank",
            true,
            "config.rsring.tank.enableSpecialTanks",
            "config.rsring.tank.enableSpecialTanks");

        net.minecraftforge.common.config.Property bottleEffProp = config.get(RsRingMod.MODID + ".tank", "xpToBottleEfficiency", 1.0D, "config.rsring.tank.xpToBottleEfficiency");
        bottleEffProp.setLanguageKey("config.rsring.tank.xpToBottleEfficiency");
        tank.xpToBottleEfficiency = bottleEffProp.getDouble();

        net.minecraftforge.common.config.Property mendingEffProp = config.get(RsRingMod.MODID + ".tank", "xpMendingEfficiency", 1.0D, "config.rsring.tank.xpMendingEfficiency");
        mendingEffProp.setLanguageKey("config.rsring.tank.xpMendingEfficiency");
        tank.xpMendingEfficiency = mendingEffProp.getDouble();

        controller.maxManagedTanks = config.getInt("maxManagedTanks",
            RsRingMod.MODID + ".controller",
            32,
            1,
            32,
            "config.rsring.controller.maxManagedTanks",
            "config.rsring.controller.maxManagedTanks");
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