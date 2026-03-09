package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 冒险物品标签属性 - 检测物品是否具有冒险相关特性
 * 如Boss掉落、地牢战利品、任务奖励等
 */
public class AdventureItemTags implements ItemAttribute {

    private String tagId;

    // 所有冒险标签定义
    public static final String TAG_BOSS_DROP = "boss_drop";           // Boss掉落
    public static final String TAG_DUNGEON_LOOT = "dungeon_loot";     // 地牢战利品
    public static final String TAG_QUEST_REWARD = "quest_reward";     // 任务奖励
    public static final String TAG_DIMENSIONAL = "dimensional";       // 维度物品
    public static final String TAG_EPIC_WEAPON = "epic_weapon";       // 史诗武器
    public static final String TAG_ARTIFACT = "artifact";             // 神器
    public static final String TAG_DRAGON_ITEM = "dragon_item";       // 龙相关物品
    public static final String TAG_PARASITE_ITEM = "parasite_item";   // 寄生虫物品
    public static final String TAG_ELITE_ITEM = "elite_item";         // 精英怪物品
    public static final String TAG_EXPLORE_ITEM = "explore_item";     // 探索物品

    public AdventureItemTags() {
        this.tagId = "";
    }

    public AdventureItemTags(String tagId) {
        this.tagId = tagId != null ? tagId : "";
    }

    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty() || tagId.isEmpty()) {
            return false;
        }

        // 根据标签ID检测物品
        switch (tagId) {
            case TAG_BOSS_DROP:
                return isBossDrop(stack);
            case TAG_DUNGEON_LOOT:
                return isDungeonLoot(stack);
            case TAG_QUEST_REWARD:
                return isQuestReward(stack);
            case TAG_DIMENSIONAL:
                return isDimensionalItem(stack);
            case TAG_EPIC_WEAPON:
                return isEpicWeapon(stack);
            case TAG_ARTIFACT:
                return isArtifact(stack);
            case TAG_DRAGON_ITEM:
                return isDragonItem(stack);
            case TAG_PARASITE_ITEM:
                return isParasiteItem(stack);
            case TAG_ELITE_ITEM:
                return isEliteItem(stack);
            case TAG_EXPLORE_ITEM:
                return isExploreItem(stack);
            default:
                return false;
        }
    }

    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        if (stack.isEmpty()) return attributes;

        // 检查所有冒险标签
        if (isBossDrop(stack)) attributes.add(new AdventureItemTags(TAG_BOSS_DROP));
        if (isDungeonLoot(stack)) attributes.add(new AdventureItemTags(TAG_DUNGEON_LOOT));
        if (isQuestReward(stack)) attributes.add(new AdventureItemTags(TAG_QUEST_REWARD));
        if (isDimensionalItem(stack)) attributes.add(new AdventureItemTags(TAG_DIMENSIONAL));
        if (isEpicWeapon(stack)) attributes.add(new AdventureItemTags(TAG_EPIC_WEAPON));
        if (isArtifact(stack)) attributes.add(new AdventureItemTags(TAG_ARTIFACT));
        if (isDragonItem(stack)) attributes.add(new AdventureItemTags(TAG_DRAGON_ITEM));
        if (isParasiteItem(stack)) attributes.add(new AdventureItemTags(TAG_PARASITE_ITEM));
        if (isEliteItem(stack)) attributes.add(new AdventureItemTags(TAG_ELITE_ITEM));
        if (isExploreItem(stack)) attributes.add(new AdventureItemTags(TAG_EXPLORE_ITEM));

        return attributes;
    }

    /**
     * 检测是否为Boss掉落物品
     */
    private boolean isBossDrop(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        // Ice and Fire 龙相关
        if (modId.equals("iceandfire")) {
            String itemName = stack.getItem().getRegistryName().toString();
            return itemName.contains("dragon") || itemName.contains("dragonsteel") ||
                   itemName.contains("myrmex") || itemName.contains("sea_serpent") ||
                   itemName.contains("tide_trident") || itemName.contains("hippogryph");
        }

        // Lycanites Mobs
        if (modId.equals("lycanitesmobs")) {
            String itemName = stack.getItem().getRegistryName().toString();
            return itemName.contains("soulstone") || itemName.contains("summoning");
        }

        // Champions 精英BOSS
        if (modId.equals("champions")) {
            return stack.hasTagCompound() && stack.getTagCompound().hasKey("champions");
        }

        return false;
    }

    /**
     * 检测是否为地牢战利品
     */
    private boolean isDungeonLoot(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        // 地牢模组相关物品
        List<String> dungeonMods = java.util.Arrays.asList(
            "AS_BattleTowers", "dldungeonsjbg", "roguelikedungeons",
            "AS_Ruins", "reccomplex", "lostcities", "antiquecities",
            "bettermineshaftsforge", "bettercaves", "betternether"
        );

        if (dungeonMods.contains(modId)) {
            return true;
        }

        // 检查NBT标签
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            // 检查LootTable标签
            if (nbt.hasKey("LootTable")) {
                String lootTable = nbt.getString("LootTable");
                return lootTable.contains("chest") || lootTable.contains("dungeon");
            }
        }

        return false;
    }

    /**
     * 检测是否为任务奖励
     */
    private boolean isQuestReward(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        // FTB Quests
        if (modId.equals("ftbquests")) {
            return true;
        }

        // Bountiful
        if (modId.equals("bountiful")) {
            return true;
        }

        // Scrolls 卷轴
        if (modId.equals("scrolls")) {
            return true;
        }

        // 检查NBT
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            return nbt.hasKey("quest_reward") || nbt.hasKey("bounty_reward");
        }

        return false;
    }

    /**
     * 检测是否为维度物品
     */
    private boolean isDimensionalItem(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        List<String> dimMods = java.util.Arrays.asList(
            "deeperdepths", "huntingdim", "netherized",
            "frozenocean", "biomesoplenty", "betternether"
        );

        return dimMods.contains(modId);
    }

    /**
     * 检测是否为史诗武器
     */
    private boolean isEpicWeapon(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        // Spartan Weaponry 系列
        if (modId.startsWith("spartan")) {
            return true;
        }

        // RLCombat
        if (modId.equals("rlcombat")) {
            return true;
        }

        // Ice and Fire 武器
        if (modId.equals("iceandfire")) {
            String itemName = stack.getItem().getRegistryName().toString();
            return itemName.contains("sword") || itemName.contains("axe") ||
                   itemName.contains("bow") || itemName.contains("trident");
        }

        return false;
    }

    /**
     * 检测是否为神器
     */
    private boolean isArtifact(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        // RL Artifacts
        if (modId.equals("rlartifacts")) {
            return true;
        }

        // Bountiful Baubles
        if (modId.equals("bountifulbaubles")) {
            return true;
        }

        // 检查稀有度
        if (stack.getRarity() == net.minecraft.item.EnumRarity.EPIC) {
            return true;
        }

        // 检查NBT
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            return nbt.hasKey("artifact") || nbt.hasKey("unique");
        }

        return false;
    }

    /**
     * 检测是否为龙相关物品
     */
    private boolean isDragonItem(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        // Ice and Fire
        if (modId.equals("iceandfire")) {
            String itemName = stack.getItem().getRegistryName().toString();
            return itemName.contains("dragon");
        }

        // Spartan Dragonsteel
        if (modId.equals("spartandragonsteel") || modId.equals("spartanfire_rlcraft")) {
            return true;
        }

        // RL Dragonsteel
        if (modId.equals("rldragonsteel")) {
            return true;
        }

        return false;
    }

    /**
     * 检测是否为寄生虫物品
     */
    private boolean isParasiteItem(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        return modId.equals("srparasites") || modId.equals("swparasites");
    }

    /**
     * 检测是否为精英怪物品
     */
    private boolean isEliteItem(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        // Infernal Mobs
        if (modId.equals("InfernalMobs")) {
            return true;
        }

        // Champions
        if (modId.equals("champions")) {
            return true;
        }

        // 检查NBT
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            return nbt.hasKey("infernal") || nbt.hasKey("champion");
        }

        return false;
    }

    /**
     * 检测是否为探索物品
     */
    private boolean isExploreItem(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) return false;

        List<String> exploreMods = java.util.Arrays.asList(
            "waystones", "naturescompass", "xaerominimap",
            "xaeroworldmap", "sereneseasons", "familiarfauna"
        );

        return exploreMods.contains(modId);
    }

    @Override
    public String getTranslationKey() {
        return "adventure_tag_" + tagId;
    }

    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{tagId};
    }

    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setString("tagId", tagId);
    }

    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        return new AdventureItemTags(nbt.getString("tagId"));
    }

    public String getTagId() {
        return tagId;
    }
}
