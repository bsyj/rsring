package com.rsring.config;

import com.rsring.rsring.RsRingMod;
import net.minecraftforge.common.config.Configuration;

public class RsRingConfig implements IHasConfig {

    /**
     * 配置类，用于存储吸能环的各种参数设置
     * 包含黑名单/白名单物品、吸收范围、能量消耗等配置项
     */
    public static class AbsorbRingConfig {
        // 默认黑名单物品列表，初始为空数组
        public String[] defaultBlacklistItems = new String[]{};
        // 默认白名单物品列表，初始为空数组
        public String[] defaultWhitelistItems = new String[]{};
        // 默认是否使用黑名单模式，true表示使用黑名单模式
        public boolean useBlacklistModeByDefault = true;
        // 吸能环的吸收范围，单位为方块，默认为8
        public int absorptionRange = 8;
        // 每个物品吸收所需的能量点数，默认为1
        public int energyCostPerItem = 1;
        public int maxEnergyCapacity = 100000;
        public boolean allowCustomFilters = true;
        public int absorptionInterval = 5;
        public int initialEnergy = 0;
        public double energyCostMultiplier = 1.0;
        public int manualChargeAmount = 1000;
        public boolean blockExternalCharging = true;
        // 严格模式：只有戴在饰品栏才能使用，默认为false
        public boolean strictMode = false;
        // 低电量提醒阈值（百分比，0-100）
        public int lowEnergyWarningThreshold = 5;
        // 低电量提醒冷却时间（秒）
        public int lowEnergyWarningCooldown = 180;
        // 是否启用低电量提醒
        public boolean enableLowEnergyWarning = true;
    }
    
    /**
     * 销毁模式配置类
     * 包含销毁模式的默认黑白名单配置
     */
    public static class DestroyModeConfig {
        // 默认销毁黑名单物品列表
        public String[] defaultBlacklistItems = new String[]{};
        // 默认销毁白名单物品列表
        public String[] defaultWhitelistItems = new String[]{};
        // 默认是否使用黑名单模式
        public boolean useBlacklistModeByDefault = true;
        // 强制销毁模式只能使用白名单（安全选项）
        public boolean whitelistOnly = true;
    }

    public static AbsorbRingConfig absorbRing = new AbsorbRingConfig();
    public static DestroyModeConfig destroyMode = new DestroyModeConfig();

    /**
     * Useful-Backpacks兼容配置
     */
    public static class UsefulBackpacksCompatConfig {
        // 是否启用Useful-Backpacks兼容
        public boolean enabled = true;
        // 背包满时是否继续查找其他背包
        public boolean cascadeToNextBackpack = true;
        // 销毁模式是否从背包中删除物品
        public boolean destroyFromBackpacks = true;
        // 是否优先使用背包而非绑定箱子
        public boolean preferBackpacks = false;
    }

    public static UsefulBackpacksCompatConfig usefulBackpacksCompat = new UsefulBackpacksCompatConfig();

    /**
     * WearableBackpacks兼容配置
     */
    public static class WearableBackpacksCompatConfig {
        // 是否启用WearableBackpacks兼容
        public boolean enabled = true;
        // 销毁模式是否从背包中删除物品
        public boolean destroyFromBackpacks = true;
        // 是否优先使用背包而非绑定箱子
        public boolean preferBackpacks = false;
    }

    public static WearableBackpacksCompatConfig wearableBackpacksCompat = new WearableBackpacksCompatConfig();

    /**
     * 同步配置方法，用于从配置文件中读取并设置吸收戒指的各项参数
     * @param config 配置对象，用于获取和设置配置值
     */
    @Override
    public void syncConfig(Configuration config) {
        // 添加自定义配置类别注释和语言键
        config.addCustomCategoryComment(RsRingMod.MODID + ".ring", "config.rsring.ring");
        config.setCategoryLanguageKey(RsRingMod.MODID + ".ring", "config.rsring.ring");

        absorbRing.defaultBlacklistItems = config.getStringList("defaultBlacklistItems",
            RsRingMod.MODID + ".ring",
            new String[]{},
            "config.rsring.ring.defaultBlacklistItems",
            null,
            "config.rsring.ring.defaultBlacklistItems");

        absorbRing.defaultWhitelistItems = config.getStringList("defaultWhitelistItems",
            RsRingMod.MODID + ".ring",
            new String[]{},
            "config.rsring.ring.defaultWhitelistItems",
            null,
            "config.rsring.ring.defaultWhitelistItems");

        absorbRing.useBlacklistModeByDefault = config.getBoolean("useBlacklistModeByDefault",
            RsRingMod.MODID + ".ring",
            true,
            "config.rsring.ring.useBlacklistModeByDefault",
            "config.rsring.ring.useBlacklistModeByDefault");

        absorbRing.absorptionRange = config.getInt("absorptionRange",
            RsRingMod.MODID + ".ring",
            8,
            1,
            32,
            "config.rsring.ring.absorptionRange",
            "config.rsring.ring.absorptionRange");

        absorbRing.energyCostPerItem = config.getInt("energyCostPerItem",
            RsRingMod.MODID + ".ring",
            1,
            0,
            1000,
            "config.rsring.ring.energyCostPerItem",
            "config.rsring.ring.energyCostPerItem");

        absorbRing.maxEnergyCapacity = config.getInt("maxEnergyCapacity",
            RsRingMod.MODID + ".ring",
            100000,
            1000,
            10000000,
            "config.rsring.ring.maxEnergyCapacity",
            "config.rsring.ring.maxEnergyCapacity");

        absorbRing.allowCustomFilters = config.getBoolean("allowCustomFilters",
            RsRingMod.MODID + ".ring",
            true,
            "config.rsring.ring.allowCustomFilters",
            "config.rsring.ring.allowCustomFilters");

        absorbRing.absorptionInterval = config.getInt("absorptionInterval",
            RsRingMod.MODID + ".ring",
            5,
            1,
            20,
            "config.rsring.ring.absorptionInterval",
            "config.rsring.ring.absorptionInterval");

        absorbRing.initialEnergy = config.getInt("initialEnergy",
            RsRingMod.MODID + ".ring",
            0,
            0,
            100000,
            "config.rsring.ring.initialEnergy",
            "config.rsring.ring.initialEnergy");

        net.minecraftforge.common.config.Property energyMultProp = config.get(RsRingMod.MODID + ".ring", "energyCostMultiplier", 1.0D, "config.rsring.ring.energyCostMultiplier");
        energyMultProp.setLanguageKey("config.rsring.ring.energyCostMultiplier");
        absorbRing.energyCostMultiplier = energyMultProp.getDouble();

        absorbRing.manualChargeAmount = config.getInt("manualChargeAmount",
            RsRingMod.MODID + ".ring",
            1000,
            0,
            1000000,
            "config.rsring.ring.manualChargeAmount",
            "config.rsring.ring.manualChargeAmount");

        net.minecraftforge.common.config.Property blockExternalChargingProp = config.get(RsRingMod.MODID + ".ring", "blockExternalCharging", true);
        blockExternalChargingProp.setLanguageKey("config.rsring.ring.blockExternalCharging");
        blockExternalChargingProp.setComment("新合成的戒指默认是否密封");
        absorbRing.blockExternalCharging = blockExternalChargingProp.getBoolean();
        
        net.minecraftforge.common.config.Property strictModeProp = config.get(RsRingMod.MODID + ".ring", "strictMode", false);
        strictModeProp.setLanguageKey("config.rsring.ring.strictMode");
        strictModeProp.setComment("只有戴在饰品栏时戒指才能使用");
        absorbRing.strictMode = strictModeProp.getBoolean();
        
        // 低电量提醒配置
        absorbRing.enableLowEnergyWarning = config.getBoolean("enableLowEnergyWarning",
            RsRingMod.MODID + ".ring",
            true,
            "config.rsring.ring.enableLowEnergyWarning",
            "config.rsring.ring.enableLowEnergyWarning");
        
        absorbRing.lowEnergyWarningThreshold = config.getInt("lowEnergyWarningThreshold",
            RsRingMod.MODID + ".ring",
            5,
            1,
            50,
            "config.rsring.ring.lowEnergyWarningThreshold",
            "config.rsring.ring.lowEnergyWarningThreshold");
        
        absorbRing.lowEnergyWarningCooldown = config.getInt("lowEnergyWarningCooldown",
            RsRingMod.MODID + ".ring",
            180,
            10,
            300,
            "config.rsring.ring.lowEnergyWarningCooldown",
            "config.rsring.ring.lowEnergyWarningCooldown");
        
        // ==================== 销毁模式配置（放在吸收戒指设置下）====================
        destroyMode.defaultBlacklistItems = config.getStringList("destroyDefaultBlacklistItems",
            RsRingMod.MODID + ".ring",
            new String[]{},
            "config.rsring.ring.destroyDefaultBlacklistItems",
            null,
            "config.rsring.ring.destroyDefaultBlacklistItems");
        
        destroyMode.defaultWhitelistItems = config.getStringList("destroyDefaultWhitelistItems",
            RsRingMod.MODID + ".ring",
            new String[]{},
            "config.rsring.ring.destroyDefaultWhitelistItems",
            null,
            "config.rsring.ring.destroyDefaultWhitelistItems");
        
        destroyMode.useBlacklistModeByDefault = config.getBoolean("destroyUseBlacklistModeByDefault",
            RsRingMod.MODID + ".ring",
            true,
            "config.rsring.ring.destroyUseBlacklistModeByDefault",
            "config.rsring.ring.destroyUseBlacklistModeByDefault");
        
        destroyMode.whitelistOnly = config.getBoolean("destroyWhitelistOnly",
            RsRingMod.MODID + ".ring",
            true,
            "config.rsring.ring.destroyWhitelistOnly",
            "config.rsring.ring.destroyWhitelistOnly");

        // ==================== 模组兼容配置父类别 ====================
        config.addCustomCategoryComment(RsRingMod.MODID + ".compat", "config.rsring.compat");
        config.setCategoryLanguageKey(RsRingMod.MODID + ".compat", "config.rsring.compat");

        // ==================== Useful-Backpacks兼容配置 ====================
        config.addCustomCategoryComment(RsRingMod.MODID + ".compat.usefulbackpacks", "config.rsring.compat.usefulbackpacks");
        config.setCategoryLanguageKey(RsRingMod.MODID + ".compat.usefulbackpacks", "config.rsring.compat.usefulbackpacks");

        usefulBackpacksCompat.enabled = config.getBoolean("usefulBackpacksEnabled",
            RsRingMod.MODID + ".compat.usefulbackpacks",
            true,
            "config.rsring.compat.usefulbackpacks.enabled",
            "config.rsring.compat.usefulbackpacks.enabled");

        usefulBackpacksCompat.cascadeToNextBackpack = config.getBoolean("cascadeToNextBackpack",
            RsRingMod.MODID + ".compat.usefulbackpacks",
            true,
            "config.rsring.compat.usefulbackpacks.cascadeToNextBackpack",
            "config.rsring.compat.usefulbackpacks.cascadeToNextBackpack");

        usefulBackpacksCompat.destroyFromBackpacks = config.getBoolean("destroyFromBackpacks",
            RsRingMod.MODID + ".compat.usefulbackpacks",
            true,
            "config.rsring.compat.usefulbackpacks.destroyFromBackpacks",
            "config.rsring.compat.usefulbackpacks.destroyFromBackpacks");

        usefulBackpacksCompat.preferBackpacks = config.getBoolean("preferBackpacks",
            RsRingMod.MODID + ".compat.usefulbackpacks",
            false,
            "config.rsring.compat.usefulbackpacks.preferBackpacks",
            "config.rsring.compat.usefulbackpacks.preferBackpacks");

        // ==================== WearableBackpacks兼容配置 ====================
        config.addCustomCategoryComment(RsRingMod.MODID + ".compat.wearablebackpacks", "config.rsring.compat.wearablebackpacks");
        config.setCategoryLanguageKey(RsRingMod.MODID + ".compat.wearablebackpacks", "config.rsring.compat.wearablebackpacks");

        wearableBackpacksCompat.enabled = config.getBoolean("wearableBackpacksEnabled",
            RsRingMod.MODID + ".compat.wearablebackpacks",
            true,
            "config.rsring.compat.wearablebackpacks.enabled",
            "config.rsring.compat.wearablebackpacks.enabled");

        wearableBackpacksCompat.destroyFromBackpacks = config.getBoolean("destroyFromBackpacks",
            RsRingMod.MODID + ".compat.wearablebackpacks",
            true,
            "config.rsring.compat.wearablebackpacks.destroyFromBackpacks",
            "config.rsring.compat.wearablebackpacks.destroyFromBackpacks");

        wearableBackpacksCompat.preferBackpacks = config.getBoolean("preferBackpacks",
            RsRingMod.MODID + ".compat.wearablebackpacks",
            false,
            "config.rsring.compat.wearablebackpacks.preferBackpacks",
            "config.rsring.compat.wearablebackpacks.preferBackpacks");
    }

    public static boolean validateConfig() {
        boolean changed = false;

        if (absorbRing.absorptionRange < 1) {
            absorbRing.absorptionRange = 1;
            changed = true;
        } else if (absorbRing.absorptionRange > 32) {
            absorbRing.absorptionRange = 32;
            changed = true;
        }

        if (absorbRing.energyCostPerItem < 1) {
            absorbRing.energyCostPerItem = 1; // 最小为1，防止免费吸收/销毁
            changed = true;
        } else if (absorbRing.energyCostPerItem > 1000) {
            absorbRing.energyCostPerItem = 1000;
            changed = true;
        }

        if (absorbRing.maxEnergyCapacity < 1000) {
            absorbRing.maxEnergyCapacity = 1000;
            changed = true;
        } else if (absorbRing.maxEnergyCapacity > 10000000) {
            absorbRing.maxEnergyCapacity = 10000000;
            changed = true;
        }

        if (absorbRing.absorptionInterval < 1) {
            absorbRing.absorptionInterval = 1;
            changed = true;
        } else if (absorbRing.absorptionInterval > 20) {
            absorbRing.absorptionInterval = 20;
            changed = true;
        }

        if (absorbRing.initialEnergy < 0) {
            absorbRing.initialEnergy = 0;
            changed = true;
        } else if (absorbRing.initialEnergy > absorbRing.maxEnergyCapacity) {
            absorbRing.initialEnergy = absorbRing.maxEnergyCapacity;
            changed = true;
        }

        if (absorbRing.energyCostMultiplier < 0.1) {
            absorbRing.energyCostMultiplier = 0.1; // 最小0.1，防止免费吸收/销毁
            changed = true;
        }

        if (absorbRing.manualChargeAmount < 0) {
            absorbRing.manualChargeAmount = 0;
            changed = true;
        } else if (absorbRing.manualChargeAmount > 1000000) {
            absorbRing.manualChargeAmount = 1000000;
            changed = true;
        }

        return changed;
    }
}
