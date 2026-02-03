package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.moremod.rsring.RsRingMod;

@Config(modid = RsRingMod.MODID, name = "rsring/ring_config")
public class RsRingConfig {

    @Config.Comment({
        "物品吸收戒指配置",
        "Item Absorb Ring Configuration"
    })
    @Config.Name("Absorb Ring Settings")
    public static AbsorbRingConfig absorbRing = new AbsorbRingConfig();

    public static class AbsorbRingConfig {
        
        @Config.Comment({
            "默认黑名单物品列表（使用物品注册名）",
            "格式：modid:item_name",
            "例如：minecraft:dirt, minecraft:cobblestone",
            "Default blacklist items (use registry names)",
            "Format: modid:item_name",
            "Example: minecraft:dirt, minecraft:cobblestone"
        })
        @Config.Name("Default Blacklist Items")
        public String[] defaultBlacklistItems = new String[]{};

        @Config.Comment({
            "默认白名单物品列表（使用物品注册名）",
            "格式：modid:item_name",
            "例如：minecraft:diamond, minecraft:gold_ingot",
            "Default whitelist items (use registry names)",
            "Format: modid:item_name",
            "Example: minecraft:diamond, minecraft:gold_ingot"
        })
        @Config.Name("Default Whitelist Items")
        public String[] defaultWhitelistItems = new String[]{};

        @Config.Comment({
            "是否启用默认黑名单（true=黑名单模式，false=白名单模式）",
            "Enable default blacklist mode (true=blacklist, false=whitelist)"
        })
        @Config.Name("Use Blacklist Mode By Default")
        public boolean useBlacklistModeByDefault = true;

        @Config.Comment({
            "吸收范围（格）",
            "Absorption range in blocks"
        })
        @Config.Name("Absorption Range")
        @Config.RangeInt(min = 1, max = 32)
        public int absorptionRange = 8;

        @Config.Comment({
            "每个物品消耗的能量（FE）",
            "Energy cost per item (FE)"
        })
        @Config.Name("Energy Cost Per Item")
        @Config.RangeInt(min = 0, max = 1000)
        public int energyCostPerItem = 1;

        @Config.Comment({
            "最大能量容量（FE）",
            "Maximum energy capacity (FE)"
        })
        @Config.Name("Max Energy Capacity")
        @Config.RangeInt(min = 1000, max = 100000000)
        public int maxEnergyCapacity = 10000000;

        @Config.Comment({
            "是否允许玩家自定义过滤列表",
            "Allow players to customize filter lists"
        })
        @Config.Name("Allow Custom Filters")
        public boolean allowCustomFilters = true;

        @Config.Comment({
            "吸收间隔（tick）",
            "How often to absorb items (ticks)"
        })
        @Config.Name("Absorption Interval")
        @Config.RangeInt(min = 1, max = 20)
        public int absorptionInterval = 5;

        @Config.Comment({
            "初始能量（FE）",
            "Initial energy for new rings (FE)"
        })
        @Config.Name("Initial Energy")
        @Config.RangeInt(min = 0, max = 10000000)
        public int initialEnergy = 0;

        @Config.Comment({
            "能量消耗倍率",
            "Energy cost multiplier"
        })
        @Config.Name("Energy Cost Multiplier")
        @Config.RangeDouble(min = 0.1, max = 10.0)
        public double energyCostMultiplier = 1.0;
    }

    @Mod.EventBusSubscriber(modid = RsRingMod.MODID)
    public static class EventHandler {

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(RsRingMod.MODID)) {
                ConfigManager.sync(RsRingMod.MODID, Config.Type.INSTANCE);
            }
        }
    }
}