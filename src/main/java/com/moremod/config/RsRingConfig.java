package com.moremod.config;

import com.moremod.rsring.RsRingMod;
import net.minecraftforge.common.config.Configuration;

public class RsRingConfig implements IHasConfig {

    public static class AbsorbRingConfig {
        public String[] defaultBlacklistItems = new String[]{};
        public String[] defaultWhitelistItems = new String[]{};
        public boolean useBlacklistModeByDefault = true;
        public int absorptionRange = 8;
        public int energyCostPerItem = 1;
        public int maxEnergyCapacity = 10000000;
        public boolean allowCustomFilters = true;
        public int absorptionInterval = 5;
        public int initialEnergy = 0;
        public double energyCostMultiplier = 1.0;
    }

    public static AbsorbRingConfig absorbRing = new AbsorbRingConfig();

    @Override
    public void syncConfig(Configuration config) {
        config.addCustomCategoryComment(RsRingMod.MODID + ".ring", "config.rsring.ring");

        absorbRing.defaultBlacklistItems = config.getStringList("defaultBlacklistItems",
            RsRingMod.MODID + ".ring",
            new String[]{},
            "config.rsring.ring.defaultBlacklistItems");

        absorbRing.defaultWhitelistItems = config.getStringList("defaultWhitelistItems",
            RsRingMod.MODID + ".ring",
            new String[]{},
            "config.rsring.ring.defaultWhitelistItems");

        absorbRing.useBlacklistModeByDefault = config.getBoolean("useBlacklistModeByDefault",
            RsRingMod.MODID + ".ring",
            true,
            "config.rsring.ring.useBlacklistModeByDefault");

        absorbRing.absorptionRange = config.getInt("absorptionRange",
            RsRingMod.MODID + ".ring",
            8,
            1,
            32,
            "config.rsring.ring.absorptionRange");

        absorbRing.energyCostPerItem = config.getInt("energyCostPerItem",
            RsRingMod.MODID + ".ring",
            1,
            0,
            1000,
            "config.rsring.ring.energyCostPerItem");

        absorbRing.maxEnergyCapacity = config.getInt("maxEnergyCapacity",
            RsRingMod.MODID + ".ring",
            10000000,
            1000,
            100000000,
            "config.rsring.ring.maxEnergyCapacity");

        absorbRing.allowCustomFilters = config.getBoolean("allowCustomFilters",
            RsRingMod.MODID + ".ring",
            true,
            "config.rsring.ring.allowCustomFilters");

        absorbRing.absorptionInterval = config.getInt("absorptionInterval",
            RsRingMod.MODID + ".ring",
            5,
            1,
            20,
            "config.rsring.ring.absorptionInterval");

        absorbRing.initialEnergy = config.getInt("initialEnergy",
            RsRingMod.MODID + ".ring",
            0,
            0,
            10000000,
            "config.rsring.ring.initialEnergy");

        absorbRing.energyCostMultiplier = config.get(RsRingMod.MODID + ".ring", "energyCostMultiplier", 1.0D, "config.rsring.ring.energyCostMultiplier").getDouble();
    }
}