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
        public int maxEnergyCapacity = 10000000;
        public boolean allowCustomFilters = true;
        public int absorptionInterval = 5;
        public int initialEnergy = 0;
        public double energyCostMultiplier = 1.0;
    }

    public static AbsorbRingConfig absorbRing = new AbsorbRingConfig();

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
            10000000,
            1000,
            100000000,
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
            10000000,
            "config.rsring.ring.initialEnergy",
            "config.rsring.ring.initialEnergy");

        net.minecraftforge.common.config.Property energyMultProp = config.get(RsRingMod.MODID + ".ring", "energyCostMultiplier", 1.0D, "config.rsring.ring.energyCostMultiplier");
        energyMultProp.setLanguageKey("config.rsring.ring.energyCostMultiplier");
        absorbRing.energyCostMultiplier = energyMultProp.getDouble();
    }
}