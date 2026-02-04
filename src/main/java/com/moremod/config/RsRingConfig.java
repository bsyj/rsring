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
        config.addCustomCategoryComment(RsRingMod.MODID + ".ring", "Item Absorb Ring Configuration");
        
        absorbRing.defaultBlacklistItems = config.getStringList("defaultBlacklistItems", 
            RsRingMod.MODID + ".ring", 
            new String[]{}, 
            "Default blacklist items (use registry names), format: modid:item_name");
            
        absorbRing.defaultWhitelistItems = config.getStringList("defaultWhitelistItems", 
            RsRingMod.MODID + ".ring", 
            new String[]{}, 
            "Default whitelist items (use registry names), format: modid:item_name");
            
        absorbRing.useBlacklistModeByDefault = config.getBoolean("useBlacklistModeByDefault", 
            RsRingMod.MODID + ".ring", 
            true, 
            "Enable default blacklist mode (true=blacklist, false=whitelist)");
            
        absorbRing.absorptionRange = config.getInt("absorptionRange", 
            RsRingMod.MODID + ".ring", 
            8, 
            1, 
            32, 
            "Absorption range in blocks");
            
        absorbRing.energyCostPerItem = config.getInt("energyCostPerItem", 
            RsRingMod.MODID + ".ring", 
            1, 
            0, 
            1000, 
            "Energy cost per item (FE)");
            
        absorbRing.maxEnergyCapacity = config.getInt("maxEnergyCapacity", 
            RsRingMod.MODID + ".ring", 
            10000000, 
            1000, 
            100000000, 
            "Maximum energy capacity (FE)");
            
        absorbRing.allowCustomFilters = config.getBoolean("allowCustomFilters", 
            RsRingMod.MODID + ".ring", 
            true, 
            "Allow players to customize filter lists");
            
        absorbRing.absorptionInterval = config.getInt("absorptionInterval", 
            RsRingMod.MODID + ".ring", 
            5, 
            1, 
            20, 
            "How often to absorb items (ticks)");
            
        absorbRing.initialEnergy = config.getInt("initialEnergy", 
            RsRingMod.MODID + ".ring", 
            0, 
            0, 
            10000000, 
            "Initial energy for new rings (FE)");
            
        absorbRing.energyCostMultiplier = config.get(RsRingMod.MODID + ".ring", "energyCostMultiplier", 1.0D, "Energy cost multiplier").getDouble();
    }
}