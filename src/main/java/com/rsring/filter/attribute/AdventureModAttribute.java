package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 冒险模组属性 - 检测物品是否来自冒险类模组
 * 支持怪物、地牢、维度、武器、任务、饰品、探索辅助等分类
 */
public class AdventureModAttribute implements ItemAttribute {

    private String modId;
    private String category; // 分类：monsters, dungeons, dimensions, weapons, quests, trinkets, exploration

    // 怪物/Boss模组
    private static final List<String> MONSTER_MODS = Arrays.asList(
        "iceandfire",      // Ice and Fire 冰与火
        "lycanitesmobs",   // Lycanites Mobs
        "fishsundeadrising", // Fish's Undead Rising
        "srparasites",     // SRParasites 寄生虫
        "swparasites",     // SWParasites
        "InfernalMobs",    // Infernal Mobs 精英怪
        "champions",       // Champions 精英BOSS
        "familiarfauna",   // Familiar Fauna
        "trumpetskeleton"  // Trumpet Skeleton
    );

    // 地牢/遗迹模组
    private static final List<String> DUNGEON_MODS = Arrays.asList(
        "AS_BattleTowers",        // Battle Towers 战斗塔
        "dldungeonsjbg",          // Doomlike Dungeons
        "roguelikedungeons",      // Roguelike Dungeons
        "AS_Ruins",               // Ruins 随机遗迹
        "reccomplex",             // Recurrent Complex
        "lostcities",             // Lost Cities 失落城市
        "antiquecities",          // Antique Cities 古代城市
        "bettermineshaftsforge",  // Better Mineshafts Forge
        "bettercaves",            // Better Caves
        "betternether"            // Better Nether
    );

    // 维度/世界模组
    private static final List<String> DIMENSION_MODS = Arrays.asList(
        "deeperdepths",      // Deeper Depths
        "huntingdim",        // Hunting Dimension
        "netherized",        // Netherized
        "frozenocean",       // Frozen Ocean
        "biomesoplenty",     // Biomes O' Plenty
        "traverse",          // Traverse
        "rtg"                // Realistic Terrain Generation
    );

    // 武器/战斗模组
    private static final List<String> WEAPON_MODS = Arrays.asList(
        "spartanweaponry",   // Spartan Weaponry
        "spartanshields",    // Spartan Shields
        "spartandragonsteel",// Spartan Dragonsteel
        "spartandefiled",    // Spartan Defiled
        "spartanfire",       // Spartan Fire
        "spartanfire_rlcraft", // Spartan and Fire: RLCraft Edition
        "rlcombat",          // RLCombat
        "elenaidodge",       // Elenai Dodge
        "scalinghealth",     // Scaling Health
        "simpledifficulty",  // Simple Difficulty
        "epicsiegemod"       // Epic Siege Mod
    );

    // 任务/奖励模组
    private static final List<String> QUEST_MODS = Arrays.asList(
        "ftbquests",         // FTB Quests
        "bountiful",         // Bountiful
        "bountifulbaubles",  // Bountiful Baubles
        "scrolls"            // Scrolls
    );

    // 饰品/装备模组
    private static final List<String> TRINKET_MODS = Arrays.asList(
        "baubles",           // Baubles (基础，已硬依赖)
        "baublevault",       // Bauble Vault
        "trinketsandbaubles",// Trinkets and Baubles
        "rlartifacts",       // RL Artifacts
        "rldragonsteel"      // RL Dragonsteel
    );

    // 探索辅助模组
    private static final List<String> EXPLORATION_MODS = Arrays.asList(
        "waystones",         // Waystones
        "naturescompass",    // Nature's Compass
        "xaerominimap",      // Xaero's Minimap
        "xaeroworldmap",     // Xaero's World Map
        "sereneseasons"      // Serene Seasons
    );

    public AdventureModAttribute() {
        this.modId = "";
        this.category = "";
    }

    public AdventureModAttribute(String modId) {
        this.modId = modId != null ? modId : "";
        this.category = detectCategory(modId);
    }

    public AdventureModAttribute(String modId, String category) {
        this.modId = modId != null ? modId : "";
        this.category = category != null ? category : "";
    }

    /**
     * 检测模组属于哪个分类
     */
    private String detectCategory(String modId) {
        if (modId == null || modId.isEmpty()) return "";
        String lowerModId = modId.toLowerCase();

        if (MONSTER_MODS.contains(lowerModId)) return "monsters";
        if (DUNGEON_MODS.contains(lowerModId)) return "dungeons";
        if (DIMENSION_MODS.contains(lowerModId)) return "dimensions";
        if (WEAPON_MODS.contains(lowerModId)) return "weapons";
        if (QUEST_MODS.contains(lowerModId)) return "quests";
        if (TRINKET_MODS.contains(lowerModId)) return "trinkets";
        if (EXPLORATION_MODS.contains(lowerModId)) return "exploration";

        return "other";
    }

    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty() || modId.isEmpty()) {
            return false;
        }
        String stackModId = stack.getItem().getCreatorModId(stack);
        if (stackModId == null) return false;

        // 如果指定了分类，检查物品是否属于该分类的模组
        if (!category.isEmpty()) {
            String itemCategory = detectCategory(stackModId);
            return category.equals(itemCategory);
        }

        return modId.equalsIgnoreCase(stackModId);
    }

    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        if (stack.isEmpty()) return attributes;

        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return attributes;

        String category = detectCategory(modId);

        // 只列出属于冒险模组分类的物品
        if (!category.isEmpty() && !category.equals("other")) {
            attributes.add(new AdventureModAttribute(modId, category));
        }

        return attributes;
    }

    @Override
    public String getTranslationKey() {
        if (!category.isEmpty()) {
            return "adventure_mod_" + category;
        }
        return "adventure_mod";
    }

    @Override
    public Object[] getTranslationParameters() {
        // 获取模组显示名称
        net.minecraftforge.fml.common.ModContainer container =
            net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(modId);
        String modName = (container != null) ? container.getName() : modId;

        if (!category.isEmpty()) {
            return new Object[]{modName, category};
        }
        return new Object[]{modName};
    }

    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setString("modId", modId);
        if (!category.isEmpty()) {
            nbt.setString("category", category);
        }
    }

    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        String modId = nbt.getString("modId");
        String category = nbt.hasKey("category") ? nbt.getString("category") : "";
        return new AdventureModAttribute(modId, category);
    }

    public String getModId() {
        return modId;
    }

    public String getCategory() {
        return category;
    }

    /**
     * 检查指定分类的模组是否已加载
     */
    public static boolean isCategoryLoaded(String category) {
        List<String> modsToCheck;
        switch (category.toLowerCase()) {
            case "monsters": modsToCheck = MONSTER_MODS; break;
            case "dungeons": modsToCheck = DUNGEON_MODS; break;
            case "dimensions": modsToCheck = DIMENSION_MODS; break;
            case "weapons": modsToCheck = WEAPON_MODS; break;
            case "quests": modsToCheck = QUEST_MODS; break;
            case "trinkets": modsToCheck = TRINKET_MODS; break;
            case "exploration": modsToCheck = EXPLORATION_MODS; break;
            default: return false;
        }

        for (String modId : modsToCheck) {
            if (Loader.isModLoaded(modId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有已加载的冒险模组
     */
    public static List<String> getLoadedAdventureMods() {
        List<String> allMods = new ArrayList<>();
        allMods.addAll(MONSTER_MODS);
        allMods.addAll(DUNGEON_MODS);
        allMods.addAll(DIMENSION_MODS);
        allMods.addAll(WEAPON_MODS);
        allMods.addAll(QUEST_MODS);
        allMods.addAll(TRINKET_MODS);
        allMods.addAll(EXPLORATION_MODS);

        List<String> loaded = new ArrayList<>();
        for (String modId : allMods) {
            if (Loader.isModLoaded(modId)) {
                loaded.add(modId);
            }
        }
        return loaded;
    }
}
