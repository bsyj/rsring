package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 行为标签属性 - 模拟 1.20.x 的 minecraft 行为标签
 * 如 piglin_loved、trimmable_armor 等
 * 
 * 这些标签在 1.12.2 中不存在，需要通过硬编码实现
 */
public class BehaviorTagAttribute implements ItemAttribute {
    
    private String tagId;
    
    // 所有已注册的行为标签
    private static final List<BehaviorTag> BEHAVIOR_TAGS = new ArrayList<>();
    
    // 注册行为标签
    static {
        // ===== 猪灵相关 =====
        // piglin_loved - 猪灵喜爱的物品（金制品）
        register("piglin_loved", stack -> {
            Item item = stack.getItem();
            // 金制装备
            if (item instanceof ItemArmor) {
                ItemArmor.ArmorMaterial material = ((ItemArmor) item).getArmorMaterial();
                if (material == ItemArmor.ArmorMaterial.GOLD) {
                    return true;
                }
            }
            // 金锭、金粒、金块相关
            return item == Items.GOLD_INGOT || item == Items.GOLD_NUGGET || 
                   item == Items.GOLDEN_APPLE || item == Items.GOLDEN_CARROT ||
                   item == Items.GOLDEN_HORSE_ARMOR || item == Item.getItemFromBlock(Blocks.GOLD_BLOCK) ||
                   item == Item.getItemFromBlock(Blocks.GOLD_ORE);
        });
        
        // piglin_food - 猪灵食物
        register("piglin_food", stack -> {
            return stack.getItem() == Items.PORKCHOP || 
                   stack.getItem() == Items.COOKED_PORKCHOP;
        });
        
        // ===== 盔甲相关 =====
        // trimmable_armor - 可装饰纹样的盔甲（所有可锻造的盔甲）
        register("trimmable_armor", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                ItemArmor.ArmorMaterial material = ((ItemArmor) stack.getItem()).getArmorMaterial();
                // 所有原版盔甲材质都可以装饰纹样
                return material == ItemArmor.ArmorMaterial.LEATHER ||
                       material == ItemArmor.ArmorMaterial.CHAIN ||
                       material == ItemArmor.ArmorMaterial.IRON ||
                       material == ItemArmor.ArmorMaterial.GOLD ||
                       material == ItemArmor.ArmorMaterial.DIAMOND;
            }
            return false;
        });
        
        // 盔甲细分 - 头盔
        register("helmets", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                return armor.armorType == net.minecraft.inventory.EntityEquipmentSlot.HEAD;
            }
            return false;
        });
        
        // 盔甲细分 - 胸甲
        register("chestplates", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                return armor.armorType == net.minecraft.inventory.EntityEquipmentSlot.CHEST;
            }
            return false;
        });
        
        // 盔甲细分 - 护腿
        register("leggings", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                return armor.armorType == net.minecraft.inventory.EntityEquipmentSlot.LEGS;
            }
            return false;
        });
        
        // 盔甲细分 - 靴子
        register("boots", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                return armor.armorType == net.minecraft.inventory.EntityEquipmentSlot.FEET;
            }
            return false;
        });
        
        // 材质细分 - 皮革盔甲
        register("leather_armors", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                return ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER;
            }
            return false;
        });
        
        // 材质细分 - 铁盔甲
        register("iron_armors", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                return ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.IRON;
            }
            return false;
        });
        
        // 材质细分 - 金盔甲
        register("golden_armors", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                return ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD;
            }
            return false;
        });
        
        // 材质细分 - 钻石盔甲
        register("diamond_armors", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                return ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.DIAMOND;
            }
            return false;
        });
        
        // 材质细分 - 锁链盔甲
        register("chainmail_armors", stack -> {
            if (stack.getItem() instanceof ItemArmor) {
                return ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.CHAIN;
            }
            return false;
        });
        
        // ===== 动物食物 =====
        // cat_food - 猫/豹猫食物
        register("cat_food", stack -> {
            return stack.getItem() == Items.FISH || stack.getItem() == Items.COOKED_FISH;
        });
        
        // wolf_food - 狼食物（肉类）
        register("wolf_food", stack -> {
            Item item = stack.getItem();
            if (item instanceof net.minecraft.item.ItemFood) {
                // 排除有毒食物
                return item != Items.SPIDER_EYE && item != Items.POISONOUS_POTATO;
            }
            return false;
        });
        
        // horse_food - 马食物
        register("horse_food", stack -> {
            Item item = stack.getItem();
            return item == Items.WHEAT || item == Items.SUGAR || item == Items.APPLE ||
                   item == Items.GOLDEN_APPLE || item == Items.GOLDEN_CARROT ||
                   item == Item.getItemFromBlock(Blocks.HAY_BLOCK);
        });
        
        // llama_food - 羊驼食物
        register("llama_food", stack -> {
            return stack.getItem() == Item.getItemFromBlock(Blocks.HAY_BLOCK);
        });
        
        // parrot_food - 鹦鹉食物
        register("parrot_food", stack -> {
            Item item = stack.getItem();
            return item == Items.WHEAT_SEEDS || item == Items.PUMPKIN_SEEDS ||
                   item == Items.MELON_SEEDS || item == Items.BEETROOT_SEEDS;
        });
        
        // parrot_poisonous_food - 鹦鹉有毒食物
        register("parrot_poisonous_food", stack -> {
            return stack.getItem() == Items.COOKIE;
        });
        
        // rabbit_food - 兔子食物
        register("rabbit_food", stack -> {
            return stack.getItem() == Items.CARROT || stack.getItem() == Items.GOLDEN_CARROT;
        });
        
        // chicken_food - 鸡食物
        register("chicken_food", stack -> {
            Item item = stack.getItem();
            return item == Items.WHEAT_SEEDS || item == Items.PUMPKIN_SEEDS ||
                   item == Items.MELON_SEEDS || item == Items.BEETROOT_SEEDS;
        });
        
        // pig_food - 猪食物
        register("pig_food", stack -> {
            Item item = stack.getItem();
            return item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT;
        });
        
        // sheep_food - 羊食物
        register("sheep_food", stack -> {
            return stack.getItem() == Items.WHEAT || 
                   stack.getItem() == Item.getItemFromBlock(Blocks.HAY_BLOCK);
        });
        
        // cow_food - 牛食物
        register("cow_food", stack -> {
            return stack.getItem() == Items.WHEAT || 
                   stack.getItem() == Item.getItemFromBlock(Blocks.HAY_BLOCK);
        });
        
        // mooshroom_food - 哞菇食物（用于蘑菇汤）
        register("mooshroom_food", stack -> {
            Item item = stack.getItem();
            return item == Items.BOWL || item == Items.REDSTONE || item == Items.GLOWSTONE_DUST;
        });
        
        // ocelot_food - 豹猫食物
        register("ocelot_food", stack -> {
            return stack.getItem() == Items.FISH || stack.getItem() == Items.COOKED_FISH;
        });
        
        // villager_food - 村民食物
        register("villager_food", stack -> {
            Item item = stack.getItem();
            return item == Items.BREAD || item == Items.POTATO || 
                   item == Items.CARROT || item == Items.BEETROOT || item == Items.WHEAT;
        });
        
        // zombie_villager_food - 僵尸村民治疗物品
        register("zombie_villager_food", stack -> {
            return stack.getItem() == Items.GOLDEN_APPLE;
        });
        
        // ===== 方块类型 =====
        // flowers - 花朵
        register("flowers", stack -> {
            Item item = stack.getItem();
            // 1.12.2 中花朵使用 RED_FLOWER 和 YELLOW_FLOWER
            return item == Item.getItemFromBlock(Blocks.RED_FLOWER) ||
                   item == Item.getItemFromBlock(Blocks.YELLOW_FLOWER);
        });
        
        // small_flowers - 小型花朵
        register("small_flowers", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.RED_FLOWER) ||
                   item == Item.getItemFromBlock(Blocks.YELLOW_FLOWER);
        });
        
        // tall_flowers - 大型花朵
        register("tall_flowers", stack -> {
            Item item = stack.getItem();
            // 1.12.2 中大型花朵使用 DOUBLE_PLANT
            return item == Item.getItemFromBlock(Blocks.DOUBLE_PLANT);
        });
        
        // rail_blocks - 铁轨方块
        register("rail_blocks", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.RAIL) ||
                   item == Item.getItemFromBlock(Blocks.GOLDEN_RAIL) ||
                   item == Item.getItemFromBlock(Blocks.DETECTOR_RAIL) ||
                   item == Item.getItemFromBlock(Blocks.ACTIVATOR_RAIL);
        });
        
        // boats - 船
        register("boats", stack -> {
            return stack.getItem() instanceof net.minecraft.item.ItemBoat;
        });
        
        // music_discs - 音乐唱片
        register("music_discs", stack -> {
            return stack.getItem() instanceof net.minecraft.item.ItemRecord;
        });
        
        // beds - 床
        register("beds", stack -> {
            return stack.getItem() == Items.BED;
        });
        
        // ===== 矿石 =====
        register("coal_ores", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.COAL_ORE));
        register("iron_ores", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.IRON_ORE));
        register("gold_ores", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.GOLD_ORE));
        register("diamond_ores", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.DIAMOND_ORE));
        register("redstone_ores", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.REDSTONE_ORE) ||
                   item == Item.getItemFromBlock(Blocks.LIT_REDSTONE_ORE);
        });
        register("lapis_ores", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.LAPIS_ORE));
        register("emerald_ores", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.EMERALD_ORE));
        register("quartz_ores", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.QUARTZ_ORE));
        
        // ===== 木材相关 =====
        register("logs", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.LOG) ||
                   item == Item.getItemFromBlock(Blocks.LOG2);
        });
        
        register("planks", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.PLANKS));
        register("saplings", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.SAPLING));
        register("leaves", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.LEAVES) ||
                   item == Item.getItemFromBlock(Blocks.LEAVES2);
        });
        
        // ===== 建筑方块 =====
        register("sand", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.SAND));
        register("sandstone", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.SANDSTONE) ||
                   item == Item.getItemFromBlock(Blocks.RED_SANDSTONE);
        });
        register("wool", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.WOOL));
        register("carpets", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.CARPET));
        register("glass", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.GLASS) ||
                   item == Item.getItemFromBlock(Blocks.STAINED_GLASS);
        });
        register("glass_panes", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.GLASS_PANE) ||
                   item == Item.getItemFromBlock(Blocks.STAINED_GLASS_PANE);
        });
        register("terracotta", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.HARDENED_CLAY) ||
                   item == Item.getItemFromBlock(Blocks.STAINED_HARDENED_CLAY);
        });
        register("concrete", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.CONCRETE));
        register("concrete_powder", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.CONCRETE_POWDER));
        
        // ===== 机关方块 =====
        register("buttons", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.STONE_BUTTON) ||
                   item == Item.getItemFromBlock(Blocks.WOODEN_BUTTON);
        });
        register("pressure_plates", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.STONE_PRESSURE_PLATE) ||
                   item == Item.getItemFromBlock(Blocks.WOODEN_PRESSURE_PLATE) ||
                   item == Item.getItemFromBlock(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE) ||
                   item == Item.getItemFromBlock(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
        });
        register("doors", stack -> stack.getItem() instanceof net.minecraft.item.ItemDoor);
        register("trapdoors", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.TRAPDOOR) ||
                   item == Item.getItemFromBlock(Blocks.IRON_TRAPDOOR);
        });
        register("fences", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.OAK_FENCE) ||
                   item == Item.getItemFromBlock(Blocks.SPRUCE_FENCE) ||
                   item == Item.getItemFromBlock(Blocks.BIRCH_FENCE) ||
                   item == Item.getItemFromBlock(Blocks.JUNGLE_FENCE) ||
                   item == Item.getItemFromBlock(Blocks.DARK_OAK_FENCE) ||
                   item == Item.getItemFromBlock(Blocks.ACACIA_FENCE) ||
                   item == Item.getItemFromBlock(Blocks.NETHER_BRICK_FENCE);
        });
        register("fence_gates", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.OAK_FENCE_GATE) ||
                   item == Item.getItemFromBlock(Blocks.SPRUCE_FENCE_GATE) ||
                   item == Item.getItemFromBlock(Blocks.BIRCH_FENCE_GATE) ||
                   item == Item.getItemFromBlock(Blocks.JUNGLE_FENCE_GATE) ||
                   item == Item.getItemFromBlock(Blocks.DARK_OAK_FENCE_GATE) ||
                   item == Item.getItemFromBlock(Blocks.ACACIA_FENCE_GATE);
        });
        register("slabs", stack -> stack.getItem() instanceof net.minecraft.item.ItemSlab);
        register("stairs", stack -> {
            // 1.12.2 中楼梯没有统一的类，通过物品名判断
            String name = stack.getItem().getRegistryName() != null ? 
                stack.getItem().getRegistryName().toString() : "";
            return name.contains("_stairs") || name.contains("Stairs");
        });
        register("walls", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.COBBLESTONE_WALL));
        
        // ===== 武器相关 =====
        // 剑类武器
        register("swords", stack -> stack.getItem() instanceof net.minecraft.item.ItemSword);
        
        // 弓类武器
        register("bows", stack -> stack.getItem() instanceof net.minecraft.item.ItemBow);
        
        // 所有武器（剑+弓）
        register("weapons", stack -> {
            Item item = stack.getItem();
            return item instanceof net.minecraft.item.ItemSword || 
                   item instanceof net.minecraft.item.ItemBow;
        });
        
        // 材质细分 - 木剑
        register("wooden_swords", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSword) {
                return ((net.minecraft.item.ItemSword) stack.getItem()).getToolMaterialName().equals("wood");
            }
            return false;
        });
        
        // 材质细分 - 石剑
        register("stone_swords", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSword) {
                return ((net.minecraft.item.ItemSword) stack.getItem()).getToolMaterialName().equals("stone");
            }
            return false;
        });
        
        // 材质细分 - 铁剑
        register("iron_swords", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSword) {
                return ((net.minecraft.item.ItemSword) stack.getItem()).getToolMaterialName().equals("iron");
            }
            return false;
        });
        
        // 材质细分 - 金剑
        register("golden_swords", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSword) {
                return ((net.minecraft.item.ItemSword) stack.getItem()).getToolMaterialName().equals("gold");
            }
            return false;
        });
        
        // 材质细分 - 钻石剑
        register("diamond_swords", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSword) {
                return ((net.minecraft.item.ItemSword) stack.getItem()).getToolMaterialName().equals("diamond");
            }
            return false;
        });
        
        // 箭矢
        register("arrows", stack -> stack.getItem() instanceof net.minecraft.item.ItemArrow);
        
        // 盾牌
        register("shields", stack -> stack.getItem() == Items.SHIELD);
        
        // ===== 工具材质细分 =====
        // 镐子
        register("pickaxes", stack -> stack.getItem() instanceof net.minecraft.item.ItemPickaxe);
        register("wooden_pickaxes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemPickaxe) {
                return ((net.minecraft.item.ItemPickaxe) stack.getItem()).getToolMaterialName().equals("wood");
            }
            return false;
        });
        register("stone_pickaxes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemPickaxe) {
                return ((net.minecraft.item.ItemPickaxe) stack.getItem()).getToolMaterialName().equals("stone");
            }
            return false;
        });
        register("iron_pickaxes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemPickaxe) {
                return ((net.minecraft.item.ItemPickaxe) stack.getItem()).getToolMaterialName().equals("iron");
            }
            return false;
        });
        register("golden_pickaxes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemPickaxe) {
                return ((net.minecraft.item.ItemPickaxe) stack.getItem()).getToolMaterialName().equals("gold");
            }
            return false;
        });
        register("diamond_pickaxes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemPickaxe) {
                return ((net.minecraft.item.ItemPickaxe) stack.getItem()).getToolMaterialName().equals("diamond");
            }
            return false;
        });
        
        // 斧头
        register("axes", stack -> stack.getItem() instanceof net.minecraft.item.ItemAxe);
        register("wooden_axes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemAxe) {
                return ((net.minecraft.item.ItemAxe) stack.getItem()).getToolMaterialName().equals("wood");
            }
            return false;
        });
        register("stone_axes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemAxe) {
                return ((net.minecraft.item.ItemAxe) stack.getItem()).getToolMaterialName().equals("stone");
            }
            return false;
        });
        register("iron_axes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemAxe) {
                return ((net.minecraft.item.ItemAxe) stack.getItem()).getToolMaterialName().equals("iron");
            }
            return false;
        });
        register("golden_axes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemAxe) {
                return ((net.minecraft.item.ItemAxe) stack.getItem()).getToolMaterialName().equals("gold");
            }
            return false;
        });
        register("diamond_axes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemAxe) {
                return ((net.minecraft.item.ItemAxe) stack.getItem()).getToolMaterialName().equals("diamond");
            }
            return false;
        });
        
        // 铲子
        register("shovels", stack -> stack.getItem() instanceof net.minecraft.item.ItemSpade);
        register("wooden_shovels", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSpade) {
                return ((net.minecraft.item.ItemSpade) stack.getItem()).getToolMaterialName().equals("wood");
            }
            return false;
        });
        register("stone_shovels", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSpade) {
                return ((net.minecraft.item.ItemSpade) stack.getItem()).getToolMaterialName().equals("stone");
            }
            return false;
        });
        register("iron_shovels", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSpade) {
                return ((net.minecraft.item.ItemSpade) stack.getItem()).getToolMaterialName().equals("iron");
            }
            return false;
        });
        register("golden_shovels", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSpade) {
                return ((net.minecraft.item.ItemSpade) stack.getItem()).getToolMaterialName().equals("gold");
            }
            return false;
        });
        register("diamond_shovels", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemSpade) {
                return ((net.minecraft.item.ItemSpade) stack.getItem()).getToolMaterialName().equals("diamond");
            }
            return false;
        });
        
        // 锄头
        register("hoes", stack -> stack.getItem() instanceof net.minecraft.item.ItemHoe);
        register("wooden_hoes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemHoe) {
                return ((net.minecraft.item.ItemHoe) stack.getItem()).getMaterialName().equals("wood");
            }
            return false;
        });
        register("stone_hoes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemHoe) {
                return ((net.minecraft.item.ItemHoe) stack.getItem()).getMaterialName().equals("stone");
            }
            return false;
        });
        register("iron_hoes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemHoe) {
                return ((net.minecraft.item.ItemHoe) stack.getItem()).getMaterialName().equals("iron");
            }
            return false;
        });
        register("golden_hoes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemHoe) {
                return ((net.minecraft.item.ItemHoe) stack.getItem()).getMaterialName().equals("gold");
            }
            return false;
        });
        register("diamond_hoes", stack -> {
            if (stack.getItem() instanceof net.minecraft.item.ItemHoe) {
                return ((net.minecraft.item.ItemHoe) stack.getItem()).getMaterialName().equals("diamond");
            }
            return false;
        });
        
        // ===== 特殊工具 =====
        register("shears", stack -> stack.getItem() == Items.SHEARS);
        register("fishing_rods", stack -> stack.getItem() == Items.FISHING_ROD);
        register("flint_and_steel", stack -> stack.getItem() == Items.FLINT_AND_STEEL);
        register("compasses", stack -> stack.getItem() == Items.COMPASS);
        register("clocks", stack -> stack.getItem() == Items.CLOCK);
        register("lead", stack -> stack.getItem() == Items.LEAD);
        register("name_tags", stack -> stack.getItem() == Items.NAME_TAG);
        register("saddles", stack -> stack.getItem() == Items.SADDLE);
        
        // ===== 食物细分 =====
        register("meats", stack -> {
            Item item = stack.getItem();
            return item == Items.BEEF || item == Items.COOKED_BEEF ||
                   item == Items.PORKCHOP || item == Items.COOKED_PORKCHOP ||
                   item == Items.CHICKEN || item == Items.COOKED_CHICKEN ||
                   item == Items.MUTTON || item == Items.COOKED_MUTTON ||
                   item == Items.RABBIT || item == Items.COOKED_RABBIT;
        });
        register("raw_meats", stack -> {
            Item item = stack.getItem();
            return item == Items.BEEF || item == Items.PORKCHOP ||
                   item == Items.CHICKEN || item == Items.MUTTON ||
                   item == Items.RABBIT;
        });
        register("cooked_meats", stack -> {
            Item item = stack.getItem();
            return item == Items.COOKED_BEEF || item == Items.COOKED_PORKCHOP ||
                   item == Items.COOKED_CHICKEN || item == Items.COOKED_MUTTON ||
                   item == Items.COOKED_RABBIT;
        });
        register("fishes", stack -> {
            Item item = stack.getItem();
            return item == Items.FISH || item == Items.COOKED_FISH;
        });
        register("raw_fishes", stack -> stack.getItem() == Items.FISH);
        register("cooked_fishes", stack -> stack.getItem() == Items.COOKED_FISH);
        register("fruits", stack -> {
            Item item = stack.getItem();
            return item == Items.APPLE || item == Items.MELON ||
                   item == Items.CHORUS_FRUIT;
        });
        register("vegetables", stack -> {
            Item item = stack.getItem();
            return item == Items.CARROT || item == Items.POTATO ||
                   item == Items.BEETROOT || item == Items.BAKED_POTATO;
        });
        register("seeds", stack -> {
            Item item = stack.getItem();
            return item == Items.WHEAT_SEEDS || item == Items.PUMPKIN_SEEDS ||
                   item == Items.MELON_SEEDS || item == Items.BEETROOT_SEEDS;
        });
        register("golden_foods", stack -> {
            Item item = stack.getItem();
            return item == Items.GOLDEN_APPLE || item == Items.GOLDEN_CARROT;
        });
        register("soups", stack -> {
            Item item = stack.getItem();
            return item == Items.MUSHROOM_STEW || item == Items.RABBIT_STEW ||
                   item == Items.BEETROOT_SOUP;
        });
        register("sweets", stack -> {
            Item item = stack.getItem();
            return item == Items.COOKIE || item == Items.CAKE ||
                   item == Items.PUMPKIN_PIE;
        });
        
        // ===== 药水相关 =====
        register("potions", stack -> {
            Item item = stack.getItem();
            return item instanceof net.minecraft.item.ItemPotion ||
                   item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
        });
        register("splash_potions", stack -> stack.getItem() == Items.SPLASH_POTION);
        register("lingering_potions", stack -> stack.getItem() == Items.LINGERING_POTION);
        register("tipped_arrows", stack -> stack.getItem() == Items.TIPPED_ARROW);
        
        // ===== 矿物相关 =====
        register("gems", stack -> {
            Item item = stack.getItem();
            return item == Items.DIAMOND || item == Items.EMERALD ||
                   item == Items.QUARTZ || item == Items.PRISMARINE_SHARD ||
                   item == Items.PRISMARINE_CRYSTALS;
        });
        register("ingots", stack -> {
            Item item = stack.getItem();
            return item == Items.IRON_INGOT || item == Items.GOLD_INGOT;
        });
        register("nuggets", stack -> {
            Item item = stack.getItem();
            return item == Items.IRON_NUGGET || item == Items.GOLD_NUGGET;
        });
        register("dusts", stack -> {
            Item item = stack.getItem();
            return item == Items.REDSTONE || item == Items.GLOWSTONE_DUST ||
                   item == Items.GUNPOWDER || item == Items.BLAZE_POWDER;
        });
        register("ores", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.COAL_ORE) ||
                   item == Item.getItemFromBlock(Blocks.IRON_ORE) ||
                   item == Item.getItemFromBlock(Blocks.GOLD_ORE) ||
                   item == Item.getItemFromBlock(Blocks.DIAMOND_ORE) ||
                   item == Item.getItemFromBlock(Blocks.EMERALD_ORE) ||
                   item == Item.getItemFromBlock(Blocks.LAPIS_ORE) ||
                   item == Item.getItemFromBlock(Blocks.REDSTONE_ORE) ||
                   item == Item.getItemFromBlock(Blocks.LIT_REDSTONE_ORE) ||
                   item == Item.getItemFromBlock(Blocks.QUARTZ_ORE);
        });
        
        // ===== 特殊物品 =====
        register("ender_pearls", stack -> stack.getItem() == Items.ENDER_PEARL);
        register("ender_eyes", stack -> stack.getItem() == Items.ENDER_EYE);
        register("blaze_rods", stack -> stack.getItem() == Items.BLAZE_ROD);
        register("ghast_tears", stack -> stack.getItem() == Items.GHAST_TEAR);
        register("slimeballs", stack -> stack.getItem() == Items.SLIME_BALL);
        register("magma_creams", stack -> stack.getItem() == Items.MAGMA_CREAM);
        register("nether_stars", stack -> stack.getItem() == Items.NETHER_STAR);
        register("dragon_breath", stack -> stack.getItem() == Items.DRAGON_BREATH);
        register("chorus_fruits", stack -> stack.getItem() == Items.CHORUS_FRUIT);
        register("popped_chorus_fruits", stack -> stack.getItem() == Items.CHORUS_FRUIT_POPPED);
        register("experience_bottles", stack -> stack.getItem() == Items.EXPERIENCE_BOTTLE);
        
        // ===== 染料相关 =====
        register("dyes", stack -> {
            Item item = stack.getItem();
            return item == Items.DYE;
        });
        register("black_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 0);
        register("red_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 1);
        register("green_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 2);
        register("brown_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 3);
        register("blue_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 4);
        register("purple_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 5);
        register("cyan_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 6);
        register("light_gray_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 7);
        register("gray_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 8);
        register("pink_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 9);
        register("lime_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 10);
        register("yellow_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 11);
        register("light_blue_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 12);
        register("magenta_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 13);
        register("orange_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 14);
        register("white_dyes", stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 15);
        
        // ===== 桶类 =====
        register("buckets", stack -> {
            Item item = stack.getItem();
            return item == Items.BUCKET || item == Items.WATER_BUCKET ||
                   item == Items.LAVA_BUCKET || item == Items.MILK_BUCKET;
        });
        register("water_buckets", stack -> stack.getItem() == Items.WATER_BUCKET);
        register("lava_buckets", stack -> stack.getItem() == Items.LAVA_BUCKET);
        register("milk_buckets", stack -> stack.getItem() == Items.MILK_BUCKET);
        register("empty_buckets", stack -> stack.getItem() == Items.BUCKET);
        
        // ===== 书本类 =====
        register("books", stack -> {
            Item item = stack.getItem();
            return item == Items.BOOK || item == Items.ENCHANTED_BOOK ||
                   item == Items.WRITTEN_BOOK || item == Items.WRITABLE_BOOK;
        });
        register("enchanted_books", stack -> stack.getItem() == Items.ENCHANTED_BOOK);
        register("written_books", stack -> stack.getItem() == Items.WRITTEN_BOOK);
        register("writable_books", stack -> stack.getItem() == Items.WRITABLE_BOOK);
        register("paper", stack -> stack.getItem() == Items.PAPER);
        
        // ===== 附魔书细分 =====
        // 有附魔的书
        register("enchanted_books.any", stack -> {
            if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
            return enchantments != null && enchantments.tagCount() > 0;
        });
        
        // 多附魔书（包含多个附魔）
        register("enchanted_books.multiple", stack -> {
            if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
            return enchantments != null && enchantments.tagCount() > 1;
        });
        
        // 单附魔书（只有一个附魔）
        register("enchanted_books.single", stack -> {
            if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
            return enchantments != null && enchantments.tagCount() == 1;
        });
        
        // 满级附魔书
        register("enchanted_books.max_level", stack -> {
            if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
            if (enchantments == null) return false;
            for (int i = 0; i < enchantments.tagCount(); i++) {
                NBTTagCompound tag = enchantments.getCompoundTagAt(i);
                int id = tag.getShort("id");
                int level = tag.getShort("lvl");
                Enchantment ench = Enchantment.getEnchantmentByID(id);
                if (ench != null && level >= ench.getMaxLevel()) {
                    return true;
                }
            }
            return false;
        });
        
        // ===== 附魔书 - 武器附魔 =====
        register("enchanted_books.weapon", stack -> hasEnchantmentType(stack, "weapon"));
        register("enchanted_books.sharpness", stack -> hasSpecificEnchantment(stack, Enchantments.SHARPNESS));
        register("enchanted_books.smite", stack -> hasSpecificEnchantment(stack, Enchantments.SMITE));
        register("enchanted_books.bane_of_arthropods", stack -> hasSpecificEnchantment(stack, Enchantments.BANE_OF_ARTHROPODS));
        register("enchanted_books.fire_aspect", stack -> hasSpecificEnchantment(stack, Enchantments.FIRE_ASPECT));
        register("enchanted_books.looting", stack -> hasSpecificEnchantment(stack, Enchantments.LOOTING));
        register("enchanted_books.knockback", stack -> hasSpecificEnchantment(stack, Enchantments.KNOCKBACK));
        register("enchanted_books.sweeping", stack -> hasSpecificEnchantment(stack, Enchantments.SWEEPING));
        register("enchanted_books.power", stack -> hasSpecificEnchantment(stack, Enchantments.POWER));
        register("enchanted_books.punch", stack -> hasSpecificEnchantment(stack, Enchantments.PUNCH));
        register("enchanted_books.flame", stack -> hasSpecificEnchantment(stack, Enchantments.FLAME));
        register("enchanted_books.infinity", stack -> hasSpecificEnchantment(stack, Enchantments.INFINITY));
        
        // ===== 附魔书 - 工具附魔 =====
        register("enchanted_books.tool", stack -> hasEnchantmentType(stack, "tool"));
        register("enchanted_books.efficiency", stack -> hasSpecificEnchantment(stack, Enchantments.EFFICIENCY));
        register("enchanted_books.silk_touch", stack -> hasSpecificEnchantment(stack, Enchantments.SILK_TOUCH));
        register("enchanted_books.fortune", stack -> hasSpecificEnchantment(stack, Enchantments.FORTUNE));
        
        // ===== 附魔书 - 盔甲附魔 =====
        register("enchanted_books.armor", stack -> hasEnchantmentType(stack, "armor"));
        register("enchanted_books.protection", stack -> hasSpecificEnchantment(stack, Enchantments.PROTECTION));
        register("enchanted_books.fire_protection", stack -> hasSpecificEnchantment(stack, Enchantments.FIRE_PROTECTION));
        register("enchanted_books.feather_falling", stack -> hasSpecificEnchantment(stack, Enchantments.FEATHER_FALLING));
        register("enchanted_books.blast_protection", stack -> hasSpecificEnchantment(stack, Enchantments.BLAST_PROTECTION));
        register("enchanted_books.projectile_protection", stack -> hasSpecificEnchantment(stack, Enchantments.PROJECTILE_PROTECTION));
        register("enchanted_books.respiration", stack -> hasSpecificEnchantment(stack, Enchantments.RESPIRATION));
        register("enchanted_books.aqua_affinity", stack -> hasSpecificEnchantment(stack, Enchantments.AQUA_AFFINITY));
        register("enchanted_books.thorns", stack -> hasSpecificEnchantment(stack, Enchantments.THORNS));
        register("enchanted_books.depth_strider", stack -> hasSpecificEnchantment(stack, Enchantments.DEPTH_STRIDER));
        register("enchanted_books.frost_walker", stack -> hasSpecificEnchantment(stack, Enchantments.FROST_WALKER));
        
        // ===== 附魔书 - 通用附魔 =====
        register("enchanted_books.utility", stack -> hasEnchantmentType(stack, "utility"));
        register("enchanted_books.unbreaking", stack -> hasSpecificEnchantment(stack, Enchantments.UNBREAKING));
        register("enchanted_books.mending", stack -> hasSpecificEnchantment(stack, Enchantments.MENDING));
        register("enchanted_books.binding_curse", stack -> hasSpecificEnchantment(stack, Enchantments.BINDING_CURSE));
        register("enchanted_books.vanishing_curse", stack -> hasSpecificEnchantment(stack, Enchantments.VANISHING_CURSE));
        
        // ===== 附魔书 - 特殊附魔 =====
        register("enchanted_books.fishing", stack -> hasSpecificEnchantment(stack, Enchantments.LUCK_OF_THE_SEA) || 
                                                        hasSpecificEnchantment(stack, Enchantments.LURE));
        register("enchanted_books.luck_of_the_sea", stack -> hasSpecificEnchantment(stack, Enchantments.LUCK_OF_THE_SEA));
        register("enchanted_books.lure", stack -> hasSpecificEnchantment(stack, Enchantments.LURE));
        
        // ===== 附魔书 - 诅咒附魔 =====
        register("enchanted_books.cursed", stack -> hasSpecificEnchantment(stack, Enchantments.BINDING_CURSE) || 
                                                     hasSpecificEnchantment(stack, Enchantments.VANISHING_CURSE));
        
        // ===== 附魔书 - 基于类型的通用检测（支持模组附魔） =====
        // 这些标签会自动识别模组添加的附魔，只要它们的类型正确
        register("enchanted_books.weapon_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.WEAPON));
        register("enchanted_books.bow_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.BOW));
        register("enchanted_books.digger_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.DIGGER));
        register("enchanted_books.armor_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.ARMOR));
        register("enchanted_books.armor_head_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.ARMOR_HEAD));
        register("enchanted_books.armor_chest_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.ARMOR_CHEST));
        register("enchanted_books.armor_legs_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.ARMOR_LEGS));
        register("enchanted_books.armor_feet_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.ARMOR_FEET));
        register("enchanted_books.fishing_rod_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.FISHING_ROD));
        register("enchanted_books.breakable_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.BREAKABLE));
        register("enchanted_books.all_type", stack -> hasEnchantmentType(stack, EnumEnchantmentType.ALL));
        
        // 附魔书 - 保护类（包含所有保护类型附魔，支持模组）
        register("enchanted_books.protection_type", stack -> {
            if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
            if (enchantments == null) return false;
            for (int i = 0; i < enchantments.tagCount(); i++) {
                NBTTagCompound tag = enchantments.getCompoundTagAt(i);
                int id = tag.getShort("id");
                Enchantment ench = Enchantment.getEnchantmentByID(id);
                if (ench != null) {
                    // 检查是否是保护类附魔（通过名称或类型判断）
                    String name = ench.getName().toLowerCase();
                    if (name.contains("protection") || name.contains("protection")) {
                        return true;
                    }
                    // 或者通过类型判断
                    if (ench.type == EnumEnchantmentType.ARMOR ||
                        ench.type == EnumEnchantmentType.ARMOR_FEET ||
                        ench.type == EnumEnchantmentType.ARMOR_HEAD ||
                        ench.type == EnumEnchantmentType.ARMOR_CHEST ||
                        ench.type == EnumEnchantmentType.ARMOR_LEGS) {
                        return true;
                    }
                }
            }
            return false;
        });
        
        // 附魔书 - 伤害类（包含所有伤害增加附魔，支持模组）
        register("enchanted_books.damage_type", stack -> {
            if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
            if (enchantments == null) return false;
            for (int i = 0; i < enchantments.tagCount(); i++) {
                NBTTagCompound tag = enchantments.getCompoundTagAt(i);
                int id = tag.getShort("id");
                Enchantment ench = Enchantment.getEnchantmentByID(id);
                if (ench != null) {
                    String name = ench.getName().toLowerCase();
                    if (name.contains("damage") || name.contains("sharpness") || 
                        name.contains("smite") || name.contains("bane") ||
                        name.contains("power")) {
                        return true;
                    }
                }
            }
            return false;
        });
        
        // 附魔书 - 诅咒类（通用检测，支持模组添加的诅咒）
        register("enchanted_books.curse_type", stack -> {
            if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
            if (enchantments == null) return false;
            for (int i = 0; i < enchantments.tagCount(); i++) {
                NBTTagCompound tag = enchantments.getCompoundTagAt(i);
                int id = tag.getShort("id");
                Enchantment ench = Enchantment.getEnchantmentByID(id);
                if (ench != null && ench.isCurse()) {
                    return true;
                }
            }
            return false;
        });
        
        // 附魔书 - 宝藏附魔（如修补、冰霜行者等，支持模组）
        register("enchanted_books.treasure_type", stack -> {
            if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
            if (enchantments == null) return false;
            for (int i = 0; i < enchantments.tagCount(); i++) {
                NBTTagCompound tag = enchantments.getCompoundTagAt(i);
                int id = tag.getShort("id");
                Enchantment ench = Enchantment.getEnchantmentByID(id);
                if (ench != null && ench.isTreasureEnchantment()) {
                    return true;
                }
            }
            return false;
        });
        
        // ===== 生物掉落物 =====
        register("mob_drops", stack -> {
            Item item = stack.getItem();
            return item == Items.ROTTEN_FLESH || item == Items.BONE ||
                   item == Items.ARROW || item == Items.SPIDER_EYE ||
                   item == Items.STRING || item == Items.FEATHER ||
                   item == Items.LEATHER || item == Items.SLIME_BALL ||
                   item == Items.BLAZE_ROD || item == Items.BLAZE_POWDER ||
                   item == Items.GHAST_TEAR || item == Items.ENDER_PEARL;
        });
        register("bones", stack -> stack.getItem() == Items.BONE);
        register("rotten_flesh", stack -> stack.getItem() == Items.ROTTEN_FLESH);
        register("strings", stack -> stack.getItem() == Items.STRING);
        register("feathers", stack -> stack.getItem() == Items.FEATHER);
        register("leathers", stack -> stack.getItem() == Items.LEATHER);
        register("rabbit_hides", stack -> stack.getItem() == Items.RABBIT_HIDE);
        register("rabbit_feet", stack -> stack.getItem() == Items.RABBIT_FOOT);
        
        // ===== 红石机械 =====
        register("redstone_components", stack -> {
            Item item = stack.getItem();
            return item == Items.REDSTONE || item == Items.REPEATER ||
                   item == Items.COMPARATOR || item == Item.getItemFromBlock(Blocks.REDSTONE_TORCH) ||
                   item == Item.getItemFromBlock(Blocks.REDSTONE_LAMP) ||
                   item == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) ||
                   item == Item.getItemFromBlock(Blocks.PISTON) ||
                   item == Item.getItemFromBlock(Blocks.STICKY_PISTON) ||
                   item == Item.getItemFromBlock(Blocks.HOPPER) ||
                   item == Item.getItemFromBlock(Blocks.DROPPER) ||
                   item == Item.getItemFromBlock(Blocks.DISPENSER) ||
                   item == Item.getItemFromBlock(Blocks.OBSERVER);
        });
        register("hoppers", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.HOPPER));
        register("dispensers", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.DISPENSER));
        register("droppers", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.DROPPER));
        register("observers", stack -> stack.getItem() == Item.getItemFromBlock(Blocks.OBSERVER));
        register("pistons", stack -> {
            Item item = stack.getItem();
            return item == Item.getItemFromBlock(Blocks.PISTON) ||
                   item == Item.getItemFromBlock(Blocks.STICKY_PISTON);
        });
        
        // ===== 装饰类 =====
        register("decorations", stack -> {
            Item item = stack.getItem();
            return item == Items.PAINTING || item == Items.ITEM_FRAME ||
                   item == Items.FLOWER_POT || item == Items.ARMOR_STAND ||
                   item == Items.BANNER || item == Items.END_CRYSTAL;
        });
        register("paintings", stack -> stack.getItem() == Items.PAINTING);
        register("item_frames", stack -> stack.getItem() == Items.ITEM_FRAME);
        register("flower_pots", stack -> stack.getItem() == Items.FLOWER_POT);
        register("armor_stands", stack -> stack.getItem() == Items.ARMOR_STAND);
        register("end_crystals", stack -> stack.getItem() == Items.END_CRYSTAL);
        
        // ===== 杂项 =====
        register("fireworks", stack -> {
            Item item = stack.getItem();
            return item == Items.FIREWORKS || item == Items.FIREWORK_CHARGE;
        });
        register("music_discs", stack -> stack.getItem() instanceof net.minecraft.item.ItemRecord);
        register("maps", stack -> {
            Item item = stack.getItem();
            return item == Items.MAP || item == Items.FILLED_MAP;
        });
        register("eggs", stack -> stack.getItem() == Items.EGG);
        register("snowballs", stack -> stack.getItem() == Items.SNOWBALL);
        register("fire_charges", stack -> stack.getItem() == Items.FIRE_CHARGE);

        // ===== 1.12.2 重要矿物词典兼容标签 =====
        // 这些标签用于兼容常用模组的矿物词典命名约定

        // 金属锭
        register("ingot_iron", stack -> isOreDict(stack, "ingotIron"));
        register("ingot_gold", stack -> isOreDict(stack, "ingotGold"));
        register("ingot_copper", stack -> isOreDict(stack, "ingotCopper"));
        register("ingot_tin", stack -> isOreDict(stack, "ingotTin"));
        register("ingot_silver", stack -> isOreDict(stack, "ingotSilver"));
        register("ingot_lead", stack -> isOreDict(stack, "ingotLead"));
        register("ingot_aluminum", stack -> isOreDict(stack, "ingotAluminum") || isOreDict(stack, "ingotAluminium"));
        register("ingot_nickel", stack -> isOreDict(stack, "ingotNickel"));
        register("ingot_platinum", stack -> isOreDict(stack, "ingotPlatinum"));
        register("ingot_steel", stack -> isOreDict(stack, "ingotSteel"));
        register("ingot_bronze", stack -> isOreDict(stack, "ingotBronze"));
        register("ingot_brass", stack -> isOreDict(stack, "ingotBrass"));
        register("ingot_electrum", stack -> isOreDict(stack, "ingotElectrum"));
        register("ingot_invar", stack -> isOreDict(stack, "ingotInvar"));
        register("ingot_constantan", stack -> isOreDict(stack, "ingotConstantan"));
        register("ingot_uranium", stack -> isOreDict(stack, "ingotUranium") || isOreDict(stack, "ingotYellorium"));
        register("ingot_osmium", stack -> isOreDict(stack, "ingotOsmium"));

        // 金属粒
        register("nugget_iron", stack -> isOreDict(stack, "nuggetIron"));
        register("nugget_gold", stack -> isOreDict(stack, "nuggetGold"));
        register("nugget_copper", stack -> isOreDict(stack, "nuggetCopper"));
        register("nugget_silver", stack -> isOreDict(stack, "nuggetSilver"));
        register("nugget_lead", stack -> isOreDict(stack, "nuggetLead"));

        // 金属块
        register("block_iron", stack -> isOreDict(stack, "blockIron"));
        register("block_gold", stack -> isOreDict(stack, "blockGold"));
        register("block_copper", stack -> isOreDict(stack, "blockCopper"));
        register("block_coal", stack -> isOreDict(stack, "blockCoal"));
        register("block_diamond", stack -> isOreDict(stack, "blockDiamond"));
        register("block_emerald", stack -> isOreDict(stack, "blockEmerald"));
        register("block_lapis", stack -> isOreDict(stack, "blockLapis"));
        register("block_redstone", stack -> isOreDict(stack, "blockRedstone"));

        // 矿石
        register("ore_iron", stack -> isOreDict(stack, "oreIron"));
        register("ore_gold", stack -> isOreDict(stack, "oreGold"));
        register("ore_copper", stack -> isOreDict(stack, "oreCopper"));
        register("ore_tin", stack -> isOreDict(stack, "oreTin"));
        register("ore_silver", stack -> isOreDict(stack, "oreSilver"));
        register("ore_lead", stack -> isOreDict(stack, "oreLead"));
        register("ore_aluminum", stack -> isOreDict(stack, "oreAluminum") || isOreDict(stack, "oreAluminium"));
        register("ore_nickel", stack -> isOreDict(stack, "oreNickel"));
        register("ore_platinum", stack -> isOreDict(stack, "orePlatinum"));
        register("ore_uranium", stack -> isOreDict(stack, "oreUranium") || isOreDict(stack, "oreYellorium"));
        register("ore_osmium", stack -> isOreDict(stack, "oreOsmium"));

        // 粉末/尘
        register("dust_iron", stack -> isOreDict(stack, "dustIron"));
        register("dust_gold", stack -> isOreDict(stack, "dustGold"));
        register("dust_copper", stack -> isOreDict(stack, "dustCopper"));
        register("dust_tin", stack -> isOreDict(stack, "dustTin"));
        register("dust_silver", stack -> isOreDict(stack, "dustSilver"));
        register("dust_lead", stack -> isOreDict(stack, "dustLead"));
        register("dust_coal", stack -> isOreDict(stack, "dustCoal"));
        register("dust_charcoal", stack -> isOreDict(stack, "dustCharcoal"));
        register("dust_obsidian", stack -> isOreDict(stack, "dustObsidian"));
        register("dust_wood", stack -> isOreDict(stack, "dustWood") || isOreDict(stack, "pulpWood"));

        // 宝石
        register("gem_diamond", stack -> isOreDict(stack, "gemDiamond"));
        register("gem_emerald", stack -> isOreDict(stack, "gemEmerald"));
        register("gem_quartz", stack -> isOreDict(stack, "gemQuartz"));
        register("gem_lapis", stack -> isOreDict(stack, "gemLapis"));
        register("gem_ruby", stack -> isOreDict(stack, "gemRuby"));
        register("gem_sapphire", stack -> isOreDict(stack, "gemSapphire"));
        register("gem_peridot", stack -> isOreDict(stack, "gemPeridot"));

        // 通用材料
        register("material_wood", stack -> isOreDict(stack, "plankWood"));
        register("material_stone", stack -> isOreDict(stack, "stone"));
        register("material_cobblestone", stack -> isOreDict(stack, "cobblestone"));
        register("material_gravel", stack -> isOreDict(stack, "gravel"));
        register("material_sand", stack -> isOreDict(stack, "sand"));
        register("material_dirt", stack -> isOreDict(stack, "dirt"));
        register("material_clay", stack -> isOreDict(stack, "clay"));
        register("material_glass", stack -> isOreDict(stack, "blockGlass"));
        register("material_wool", stack -> isOreDict(stack, "wool"));

        // 作物/农业
        register("crop_wheat", stack -> isOreDict(stack, "cropWheat"));
        register("crop_carrot", stack -> isOreDict(stack, "cropCarrot"));
        register("crop_potato", stack -> isOreDict(stack, "cropPotato"));
        register("crop_beetroot", stack -> isOreDict(stack, "cropBeetroot"));
        register("crop_nether_wart", stack -> isOreDict(stack, "cropNetherWart"));
        register("crop_cactus", stack -> isOreDict(stack, "cropCactus"));
        register("crop_sugarcane", stack -> isOreDict(stack, "cropSugarcane") || isOreDict(stack, "sugarcane"));
        register("crop_melon", stack -> isOreDict(stack, "cropMelon"));
        register("crop_pumpkin", stack -> isOreDict(stack, "cropPumpkin"));

        // 种子
        register("seed_wheat", stack -> isOreDict(stack, "seedWheat"));
        register("seed_carrot", stack -> isOreDict(stack, "seedCarrot"));
        register("seed_potato", stack -> isOreDict(stack, "seedPotato"));
        register("seed_beetroot", stack -> isOreDict(stack, "seedBeetroot"));
        register("seed_pumpkin", stack -> isOreDict(stack, "seedPumpkin"));
        register("seed_melon", stack -> isOreDict(stack, "seedMelon"));

        // 食物分类
        register("food_meat_raw", stack -> isOreDict(stack, "listAllmeatraw"));
        register("food_meat_cooked", stack -> isOreDict(stack, "listAllmeatcooked"));
        register("food_fish_raw", stack -> isOreDict(stack, "listAllfishraw"));
        register("food_fish_cooked", stack -> isOreDict(stack, "listAllfishcooked"));
        register("food_vegetable", stack -> isOreDict(stack, "listAllveggie"));
        register("food_fruit", stack -> isOreDict(stack, "listAllfruit"));
        register("food_berry", stack -> isOreDict(stack, "listAllberry"));
        register("food_mushroom", stack -> isOreDict(stack, "listAllmushroom"));
        register("food_nut", stack -> isOreDict(stack, "listAllnut"));
        register("food_spice", stack -> isOreDict(stack, "listAllspice"));
        register("food_egg", stack -> isOreDict(stack, "listAllegg"));
        register("food_sugar", stack -> isOreDict(stack, "listAllsugar"));

        // 工具/装备通用
        register("tool_pickaxe", stack -> isOreDict(stack, "toolPickaxe"));
        register("tool_axe", stack -> isOreDict(stack, "toolAxe"));
        register("tool_shovel", stack -> isOreDict(stack, "toolShovel"));
        register("tool_hoe", stack -> isOreDict(stack, "toolHoe"));
        register("tool_sword", stack -> isOreDict(stack, "toolSword"));
        register("tool_shears", stack -> isOreDict(stack, "toolShears"));
        register("tool_bow", stack -> isOreDict(stack, "toolBow"));
        register("tool_fishing_rod", stack -> isOreDict(stack, "toolFishingRod"));

        // 装备部位
        register("armor_helmet", stack -> isOreDict(stack, "armorHelmet"));
        register("armor_chestplate", stack -> isOreDict(stack, "armorChestplate"));
        register("armor_leggings", stack -> isOreDict(stack, "armorLeggings"));
        register("armor_boots", stack -> isOreDict(stack, "armorBoots"));

        // 电路/科技模组兼容
        register("circuit_basic", stack -> isOreDict(stack, "circuitBasic") || isOreDict(stack, "basicCircuit"));
        register("circuit_advanced", stack -> isOreDict(stack, "circuitAdvanced") || isOreDict(stack, "advancedCircuit"));
        register("circuit_elite", stack -> isOreDict(stack, "circuitElite") || isOreDict(stack, "eliteCircuit"));
        register("circuit_ultimate", stack -> isOreDict(stack, "circuitUltimate") || isOreDict(stack, "ultimateCircuit"));

        // 线缆/管道
        register("cable_copper", stack -> isOreDict(stack, "cableCopper"));
        register("cable_gold", stack -> isOreDict(stack, "cableGold"));
        register("cable_iron", stack -> isOreDict(stack, "cableIron"));
        register("pipe_fluid", stack -> isOreDict(stack, "pipeFluid"));
        register("pipe_item", stack -> isOreDict(stack, "pipeItem"));
        register("pipe_energy", stack -> isOreDict(stack, "pipeEnergy"));

        // 存储
        register("battery", stack -> isOreDict(stack, "battery"));
        register("capacitor", stack -> isOreDict(stack, "capacitor"));
        register("cell_empty", stack -> isOreDict(stack, "cellEmpty") || isOreDict(stack, "emptyCell"));
        register("cell_fluid", stack -> isOreDict(stack, "cellFluid") || isOreDict(stack, "fluidCell"));

        // 杂项材料
        register("rubber", stack -> isOreDict(stack, "itemRubber") || isOreDict(stack, "rubber"));
        register("plastic", stack -> isOreDict(stack, "itemPlastic") || isOreDict(stack, "plastic"));
        register("silicon", stack -> isOreDict(stack, "itemSilicon") || isOreDict(stack, "silicon"));
        register("sulfur", stack -> isOreDict(stack, "dustSulfur") || isOreDict(stack, "sulphur"));
        register("saltpeter", stack -> isOreDict(stack, "dustSaltpeter") || isOreDict(stack, "dustNiter"));
        register("salt", stack -> isOreDict(stack, "dustSalt") || isOreDict(stack, "itemSalt"));

        // 容器
        register("container_empty", stack -> isOreDict(stack, "containerEmpty") || isOreDict(stack, "emptyContainer"));
        register("bucket_empty", stack -> isOreDict(stack, "bucketEmpty"));
        register("bucket_water", stack -> isOreDict(stack, "bucketWater"));
        register("bucket_lava", stack -> isOreDict(stack, "bucketLava"));
        register("bucket_milk", stack -> isOreDict(stack, "bucketMilk"));

        // ===== 冰与火之歌(Ice and Fire)模组兼容 =====
        // 龙相关材料
        register("iceandfire_dragon_scales", stack -> isOreDict(stack, "dragonScales") || 
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonscales"));
        register("iceandfire_dragon_bones", stack -> isOreDict(stack, "dragonBones") || 
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonbone"));
        register("iceandfire_wither_bones", stack -> isOreDict(stack, "witherBones") || 
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:witherbone"));
        register("iceandfire_fire_dragon_heart", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:fire_dragon_heart"));
        register("iceandfire_ice_dragon_heart", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:ice_dragon_heart"));
        register("iceandfire_lightning_dragon_heart", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:lightning_dragon_heart"));
        register("iceandfire_dragon_heart", stack -> {
            String name = stack.getItem().getRegistryName().toString();
            return name.equals("iceandfire:fire_dragon_heart") || 
                   name.equals("iceandfire:ice_dragon_heart") ||
                   name.equals("iceandfire:lightning_dragon_heart");
        });
        
        // 龙血
        register("iceandfire_fire_dragon_blood", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:fire_dragon_blood"));
        register("iceandfire_ice_dragon_blood", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:ice_dragon_blood"));
        register("iceandfire_lightning_dragon_blood", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:lightning_dragon_blood"));
        register("iceandfire_dragon_blood", stack -> {
            String name = stack.getItem().getRegistryName().toString();
            return name.contains("dragon_blood");
        });
        
        // 龙钢材料
        register("iceandfire_fire_dragonsteel", stack -> 
            stack.getItem().getRegistryName().toString().contains("dragonsteel_fire"));
        register("iceandfire_ice_dragonsteel", stack -> 
            stack.getItem().getRegistryName().toString().contains("dragonsteel_ice"));
        register("iceandfire_lightning_dragonsteel", stack -> 
            stack.getItem().getRegistryName().toString().contains("dragonsteel_lightning"));
        register("iceandfire_dragonsteel", stack -> 
            stack.getItem().getRegistryName().toString().contains("dragonsteel"));
        
        // 龙蛋
        register("iceandfire_dragon_egg", stack -> 
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonegg"));
        register("iceandfire_fire_dragon_egg", stack -> 
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonegg_red") ||
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonegg_green") ||
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonegg_bronze") ||
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonegg_gray"));
        register("iceandfire_ice_dragon_egg", stack -> 
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonegg_blue") ||
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonegg_white") ||
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonegg_sapphire") ||
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:dragonegg_silver"));
        
        // 神话生物材料
        register("iceandfire_pixie_dust", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:pixie_dust"));
        register("iceandfire_pixie_wings", stack -> 
            stack.getItem().getRegistryName().toString().contains("pixie_wings"));
        register("iceandfire_siren_tear", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:siren_tear"));
        register("iceandfire_shiny_scales", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:shiny_scales"));
        register("iceandfire_hippogryph_talon", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:hippogryph_talon"));
        register("iceandfire_hippogryph_feather", stack -> 
            stack.getItem().getRegistryName().toString().contains("hippogryph_feather"));
        register("iceandfire_cyclops_eye", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:cyclops_eye"));
        register("iceandfire_troll_leather", stack -> 
            stack.getItem().getRegistryName().toString().contains("troll_leather"));
        register("iceandfire_amphithere_feather", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:amphithere_feather"));
        register("iceandfire_amphithere_macaw", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:amphithere_macaw"));
        register("iceandfire_deathworm_chitin", stack -> 
            stack.getItem().getRegistryName().toString().contains("deathworm_chitin"));
        register("iceandfire_myremex_jaw", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:myrmex_jaw"));
        register("iceandfire_myremex_resin", stack -> 
            stack.getItem().getRegistryName().toString().contains("myrmex_resin"));
        register("iceandfire_stymphalian_bird_feather", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:stymphalian_bird_feather"));
        register("iceandfire_stymphalian_arrow", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:stymphalian_arrow"));
        register("iceandfire_ectoplasm", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:ectoplasm"));
        register("iceandfire_dread_shard", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:dread_shard"));
        register("iceandfire_dread_key", stack -> 
            stack.getItem().getRegistryName().toString().contains("dread_key"));
        
        // 冰与火装备分类
        register("iceandfire_dragon_armor", stack -> 
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:armor_") &&
            stack.getItem().getRegistryName().toString().contains("dragon"));
        register("iceandfire_dragon_weapon", stack -> {
            String name = stack.getItem().getRegistryName().toString();
            return name.startsWith("iceandfire:") && (name.contains("dragonbone_") || name.contains("dragonsteel_"));
        });
        register("iceandfire_silver_weapon", stack -> 
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:silver_"));
        register("iceandfire_silver_armor", stack -> 
            stack.getItem().getRegistryName().toString().startsWith("iceandfire:armor_silver"));
        register("iceandfire_sapphire_item", stack -> 
            stack.getItem().getRegistryName().toString().contains("sapphire"));
        
        // 冰与火工具/武器
        register("iceandfire_bestiary", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:bestiary"));
        register("iceandfire_lectern", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:lectern"));
        register("iceandfire_dragon_skull", stack -> 
            stack.getItem().getRegistryName().toString().contains("dragon_skull"));
        register("iceandfire_dragon_horn", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:dragon_horn"));
        register("iceandfire_summoning_crystal", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:summoning_crystal"));
        register("iceandfire_dragon_meal", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:dragon_meal"));
        register("iceandfire_fire_stew", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:fire_stew"));
        register("iceandfire_frost_stew", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:frost_stew"));
        register("iceandfire_lightning_stew", stack -> 
            stack.getItem().getRegistryName().toString().equals("iceandfire:lightning_stew"));

        // ===== 斯巴达武器(SpartanWeaponry)模组兼容 =====
        // 武器类型
        register("spartan_dagger", stack -> 
            stack.getItem().getRegistryName().toString().contains("dagger") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_longsword", stack -> 
            stack.getItem().getRegistryName().toString().contains("longsword") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_katana", stack -> 
            stack.getItem().getRegistryName().toString().contains("katana") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_saber", stack -> 
            stack.getItem().getRegistryName().toString().contains("saber") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_rapier", stack -> 
            stack.getItem().getRegistryName().toString().contains("rapier") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_greatsword", stack -> 
            stack.getItem().getRegistryName().toString().contains("greatsword") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_hammer", stack -> 
            stack.getItem().getRegistryName().toString().contains("hammer") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_warhammer", stack -> 
            stack.getItem().getRegistryName().toString().contains("warhammer") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_spear", stack -> 
            stack.getItem().getRegistryName().toString().contains("spear") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_halberd", stack -> 
            stack.getItem().getRegistryName().toString().contains("halberd") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_pike", stack -> 
            stack.getItem().getRegistryName().toString().contains("pike") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_lance", stack -> 
            stack.getItem().getRegistryName().toString().contains("lance") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_longbow", stack -> 
            stack.getItem().getRegistryName().toString().contains("longbow") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_crossbow", stack -> 
            stack.getItem().getRegistryName().toString().contains("crossbow") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_throwing_knife", stack -> 
            stack.getItem().getRegistryName().toString().contains("throwing_knife") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_throwing_axe", stack -> 
            stack.getItem().getRegistryName().toString().contains("throwing_axe") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_javelin", stack -> 
            stack.getItem().getRegistryName().toString().contains("javelin") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_boomerang", stack -> 
            stack.getItem().getRegistryName().toString().contains("boomerang") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_battleaxe", stack -> 
            stack.getItem().getRegistryName().toString().contains("battleaxe") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_mace", stack -> 
            stack.getItem().getRegistryName().toString().contains("mace") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_glaive", stack -> 
            stack.getItem().getRegistryName().toString().contains("glaive") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_staff", stack -> 
            stack.getItem().getRegistryName().toString().contains("staff") && 
            isFromMod(stack, "spartanweaponry"));
        
        // 斯巴达武器材质分类
        register("spartan_wooden_weapon", stack -> 
            stack.getItem().getRegistryName().toString().contains("_wooden") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_stone_weapon", stack -> 
            stack.getItem().getRegistryName().toString().contains("_stone") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_iron_weapon", stack -> 
            stack.getItem().getRegistryName().toString().contains("_iron") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_golden_weapon", stack -> 
            stack.getItem().getRegistryName().toString().contains("_golden") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_diamond_weapon", stack -> 
            stack.getItem().getRegistryName().toString().contains("_diamond") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_bronze_weapon", stack -> 
            stack.getItem().getRegistryName().toString().contains("_bronze") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_steel_weapon", stack -> 
            stack.getItem().getRegistryName().toString().contains("_steel") && 
            isFromMod(stack, "spartanweaponry"));
        register("spartan_silver_weapon", stack -> 
            stack.getItem().getRegistryName().toString().contains("_silver") && 
            isFromMod(stack, "spartanweaponry"));
        
        // 斯巴达盾牌
        register("spartan_shield", stack -> 
            isFromMod(stack, "spartanshields") || 
            (stack.getItem().getRegistryName().toString().contains("shield") && 
             isFromMod(stack, "spartanweaponry")));
        register("spartan_basic_shield", stack -> 
            stack.getItem().getRegistryName().toString().contains("shield_basic"));
        register("spartan_tower_shield", stack -> 
            stack.getItem().getRegistryName().toString().contains("shield_tower"));
        register("spartan_heat_shield", stack -> 
            stack.getItem().getRegistryName().toString().contains("shield_heat"));
        register("spartan_riot_shield", stack -> 
            stack.getItem().getRegistryName().toString().contains("shield_riot"));

        // ===== 饰品栏(Baubles)模组兼容 =====
        // 饰品类型检测
        register("bauble_amulet", stack -> 
            stack.getItem().getRegistryName().toString().contains("amulet") ||
            isBaubleType(stack, "AMULET"));
        register("bauble_ring", stack -> 
            stack.getItem().getRegistryName().toString().contains("ring") ||
            isBaubleType(stack, "RING"));
        register("bauble_belt", stack -> 
            stack.getItem().getRegistryName().toString().contains("belt") ||
            isBaubleType(stack, "BELT"));
        register("bauble_head", stack -> 
            stack.getItem().getRegistryName().toString().contains("head") ||
            isBaubleType(stack, "HEAD"));
        register("bauble_body", stack -> 
            stack.getItem().getRegistryName().toString().contains("body") ||
            isBaubleType(stack, "BODY"));
        register("bauble_charm", stack -> 
            stack.getItem().getRegistryName().toString().contains("charm") ||
            isBaubleType(stack, "CHARM"));
        register("bauble_trinket", stack -> 
            stack.getItem().getRegistryName().toString().contains("trinket") ||
            isBaubleType(stack, "TRINKET"));
        register("bauble_any", stack -> isBaubleType(stack, null));
        
        // 饰品模组物品
        register("bountiful_bauble", stack -> 
            isFromMod(stack, "bountifulbaubles"));
        register("trinkets_bauble", stack -> 
            isFromMod(stack, "trinketsandbaubles"));
        register("bauble_vault_item", stack -> 
            isFromMod(stack, "baublevault"));
        
        // 功能性饰品分类
        register("bauble_speed", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("speed") || name.contains("swiftness") || name.contains("agility");
        });
        register("bauble_strength", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("strength") || name.contains("power") || name.contains("might");
        });
        register("bauble_resistance", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("resistance") || name.contains("protection") || name.contains("shield");
        });
        register("bauble_regeneration", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("regeneration") || name.contains("healing") || name.contains("recovery");
        });
        register("bauble_jump", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("jump") || name.contains("leaping") || name.contains("step");
        });
        register("bauble_night_vision", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("night") || name.contains("vision") || name.contains("glowing");
        });
        register("bauble_water_breathing", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("water") || name.contains("breathing") || name.contains("aqua");
        });
        register("bauble_fire_resistance", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("fire") || name.contains("flame") || name.contains("lava");
        });
        register("bauble_flight", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("flight") || name.contains("flying") || name.contains("wing");
        });
        register("bauble_invisibility", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("invisibility") || name.contains("hidden") || name.contains("cloak");
        });
        register("bauble_luck", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("luck") || name.contains("fortune") || name.contains("lucky");
        });
        register("bauble_mining", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("mining") || name.contains("haste") || name.contains("digging");
        });

        // ===== 恐怖生物(Lycanites Mobs)模组兼容 =====
        register("lycanites_part", stack -> 
            isFromMod(stack, "lycanitesmobs") && 
            stack.getItem().getRegistryName().toString().contains("_part"));
        register("lycanites_soulstone", stack -> 
            isFromMod(stack, "lycanitesmobs") && 
            stack.getItem().getRegistryName().toString().contains("soulstone"));
        register("lycanites_soulgazer", stack -> 
            stack.getItem().getRegistryName().toString().equals("lycanitesmobs:soulgazer"));
        register("lycanites_creature_soul", stack -> 
            isFromMod(stack, "lycanitesmobs") && 
            stack.getItem().getRegistryName().toString().contains("soul_"));
        register("lycanites_summoning_staff", stack -> 
            stack.getItem().getRegistryName().toString().equals("lycanitesmobs:summoningstaff"));
        register("lycanites_scepter", stack -> 
            isFromMod(stack, "lycanitesmobs") && 
            stack.getItem().getRegistryName().toString().contains("scepter"));
        register("lycanites_equipment", stack -> 
            isFromMod(stack, "lycanitesmobs") && 
            (stack.getItem().getRegistryName().toString().contains("equipment") ||
             stack.getItem().getRegistryName().toString().contains("_charge")));
        register("lycanites_mob_drop", stack -> 
            isFromMod(stack, "lycanitesmobs") && 
            !stack.getItem().getRegistryName().toString().contains("equipment") &&
            !stack.getItem().getRegistryName().toString().contains("soul"));

        // ===== 战斗塔(Battle Towers)模组兼容 =====
        register("battletowers_key", stack -> 
            stack.getItem().getRegistryName().toString().contains("battletower") &&
            stack.getItem().getRegistryName().toString().contains("key"));
        register("battletowers_loot", stack -> 
            isFromMod(stack, "battletowers") ||
            stack.getItem().getRegistryName().toString().contains("battletower"));

        // ===== 失落城市(Lost Cities)模组兼容 =====
        register("lostcities_item", stack -> isFromMod(stack, "lostcities"));

        // ===== 更好的矿井(Better Mineshafts)模组兼容 =====
        register("bettermineshafts_item", stack -> isFromMod(stack, "bettermineshafts"));

        // ===== 悬赏(Bountiful)模组兼容 =====
        register("bountiful_bounty", stack -> 
            stack.getItem().getRegistryName().toString().equals("bountiful:bounty"));
        register("bountiful_decree", stack -> 
            stack.getItem().getRegistryName().toString().equals("bountiful:decree"));
        register("bountiful_item", stack -> isFromMod(stack, "bountiful"));

        // ===== 任务(FTB Quests)模组兼容 =====
        register("ftbquests_book", stack -> 
            stack.getItem().getRegistryName().toString().equals("ftbquests:book"));
        register("ftbquests_lootcrate", stack -> 
            stack.getItem().getRegistryName().toString().contains("lootcrate"));
        register("ftbquests_item", stack -> isFromMod(stack, "ftbquests"));

        // ===== 传送石碑(Waystones)模组兼容 =====
        register("waystones_waystone", stack -> 
            stack.getItem().getRegistryName().toString().contains("waystone"));
        register("waystones_scroll", stack -> 
            stack.getItem().getRegistryName().toString().contains("scroll") &&
            isFromMod(stack, "waystones"));
        register("waystones_warp_stone", stack -> 
            stack.getItem().getRegistryName().toString().equals("waystones:warp_stone"));
        register("waystones_item", stack -> isFromMod(stack, "waystones"));

        // ===== 自然指南针(Nature's Compass)模组兼容 =====
        register("naturescompass_compass", stack -> 
            stack.getItem().getRegistryName().toString().equals("naturescompass:naturescompass"));

        // ===== 四季(Serene Seasons)模组兼容 =====
        register("sereneseasons_calendar", stack -> 
            stack.getItem().getRegistryName().toString().equals("sereneseasons:calendar"));
        register("sereneseasons_greenhouse_glass", stack -> 
            stack.getItem().getRegistryName().toString().equals("sereneseasons:greenhouse_glass"));
        register("sereneseasons_season_sensor", stack -> 
            stack.getItem().getRegistryName().toString().contains("season_sensor"));

        // ===== 缩放生命(Scaling Health)模组兼容 =====
        register("scalinghealth_cursed_heart", stack -> 
            stack.getItem().getRegistryName().toString().equals("scalinghealth:cursed_heart"));
        register("scalinghealth_enchanted_heart", stack -> 
            stack.getItem().getRegistryName().toString().equals("scalinghealth:enchanted_heart"));
        register("scalinghealth_champion_heart", stack -> 
            stack.getItem().getRegistryName().toString().equals("scalinghealth:champion_heart"));
        register("scalinghealth_difficulty_changer", stack -> 
            stack.getItem().getRegistryName().toString().contains("difficulty_changer"));
        register("scalinghealth_bandages", stack -> 
            stack.getItem().getRegistryName().toString().equals("scalinghealth:bandages"));
        register("scalinghealth_medkit", stack -> 
            stack.getItem().getRegistryName().toString().equals("scalinghealth:medkit"));
        register("scalinghealth_heart_container", stack -> 
            stack.getItem().getRegistryName().toString().contains("heart_container"));

        // ===== 精英怪(Infernal Mobs/Champions)模组兼容 =====
        register("champions_rank_item", stack -> isFromMod(stack, "champions"));
        register("infernalmobs_loot", stack -> isFromMod(stack, "infernalmobs"));

        // ===== 简单难度(Simple Difficulty)模组兼容 =====
        register("simpledifficulty_canteen", stack -> 
            stack.getItem().getRegistryName().toString().contains("canteen"));
        register("simpledifficulty_juice", stack -> 
            stack.getItem().getRegistryName().toString().contains("juice"));
        register("simpledifficulty_purified_water", stack -> 
            stack.getItem().getRegistryName().toString().contains("purified_water"));
        register("simpledifficulty_water_filter", stack -> 
            stack.getItem().getRegistryName().toString().equals("simpledifficulty:water_filter"));
        register("simpledifficulty_heater", stack -> 
            stack.getItem().getRegistryName().toString().equals("simpledifficulty:heater"));
        register("simpledifficulty_chiller", stack -> 
            stack.getItem().getRegistryName().toString().equals("simpledifficulty:chiller"));
        register("simpledifficulty_thermometer", stack -> 
            stack.getItem().getRegistryName().toString().equals("simpledifficulty:thermometer"));
        register("simpledifficulty_item", stack -> isFromMod(stack, "simpledifficulty"));

        // ===== 闪避系统(Elenai Dodge)模组兼容 =====
        register("elenaidodge_ring", stack -> 
            isFromMod(stack, "elenaidodge") && 
            stack.getItem().getRegistryName().toString().contains("ring"));
        register("elenaidodge_item", stack -> isFromMod(stack, "elenaidodge"));

        // ===== 战斗革新(RLCombat)模组兼容 =====
        register("rlcompat_item", stack -> isFromMod(stack, "rlcombat"));

        // ===== 史诗攻城(Epic Siege Mod)模组兼容 =====
        register("epicsiege_item", stack -> isFromMod(stack, "epicsiege"));

        // ===== 更多生物群系(Biomes O' Plenty)模组兼容 =====
        register("biomesoplenty_gem", stack -> 
            isFromMod(stack, "biomesoplenty") && 
            stack.getItem().getRegistryName().toString().contains("gem"));
        register("biomesoplenty_wood", stack -> 
            isFromMod(stack, "biomesoplenty") && 
            (stack.getItem().getRegistryName().toString().contains("log") ||
             stack.getItem().getRegistryName().toString().contains("plank") ||
             stack.getItem().getRegistryName().toString().contains("wood")));
        register("biomesoplenty_plant", stack -> 
            isFromMod(stack, "biomesoplenty") && 
            (stack.getItem().getRegistryName().toString().contains("plant") ||
             stack.getItem().getRegistryName().toString().contains("flower") ||
             stack.getItem().getRegistryName().toString().contains("mushroom")));
        register("biomesoplenty_food", stack -> 
            isFromMod(stack, "biomesoplenty") && 
            stack.getItem().getRegistryName().toString().contains("food"));
        register("biomesoplenty_item", stack -> isFromMod(stack, "biomesoplenty"));

        // ===== 遍历(Traverse)模组兼容 =====
        register("traverse_item", stack -> isFromMod(stack, "traverse"));

        // ===== 真实地形(RTG)模组兼容 =====
        register("rtg_item", stack -> isFromMod(stack, "rtg"));

        // ===== 更好的洞穴(Better Caves)模组兼容 =====
        register("bettercaves_item", stack -> isFromMod(stack, "bettercaves"));

        // ===== 更好的下界(Better Nether)模组兼容 =====
        register("betternether_item", stack -> isFromMod(stack, "betternether"));

        // ===== 地牢(DLDungeons)模组兼容 =====
        register("dldungeons_item", stack -> isFromMod(stack, "dldungeonsjbg"));

        // ===== 肉鸽地牢(Roguelike Dungeons)模组兼容 =====
        register("roguelike_dungeon_loot", stack -> isFromMod(stack, "roguelike"));

        // ===== 遗迹(Ruins)模组兼容 =====
        register("ruins_item", stack -> isFromMod(stack, "ruins"));

        // ===== 复杂结构(Recurrent Complex)模组兼容 =====
        register("reccomplex_item", stack -> isFromMod(stack, "reccomplex"));

        // ===== 古代城市(Antique Cities)模组兼容 =====
        register("antiquecities_item", stack -> isFromMod(stack, "antiquecities"));

        // ===== 深层维度(Deeper Depths)模组兼容 =====
        register("deeperdepths_item", stack -> isFromMod(stack, "deeperdepths"));

        // ===== 狩猎维度(Hunting Dimension)模组兼容 =====
        register("huntingdim_frame", stack -> 
            stack.getItem().getRegistryName().toString().equals("huntingdim:frame"));
        register("huntingdim_item", stack -> isFromMod(stack, "huntingdim"));

        // ===== 重制下界(Netherized)模组兼容 =====
        register("netherized_item", stack -> isFromMod(stack, "netherized"));

        // ===== 冰冻海洋(Frozen Ocean)模组兼容 =====
        register("frozenocean_item", stack -> isFromMod(stack, "frozenocean"));

        // ===== 卷轴(Scrolls)模组兼容 =====
        register("scrolls_scroll", stack -> 
            isFromMod(stack, "scrolls") && 
            stack.getItem().getRegistryName().toString().contains("scroll"));
        register("scrolls_item", stack -> isFromMod(stack, "scrolls"));

        // ===== 神器(RLArtifacts)模组兼容 =====
        register("rlartifacts_artifact", stack -> isFromMod(stack, "rlartifacts"));

        // ===== 龙钢(RLDragonsteel)模组兼容 =====
        register("rldragonsteel_dragonsteel", stack -> 
            isFromMod(stack, "rldragonsteel") && 
            stack.getItem().getRegistryName().toString().contains("dragonsteel"));
        register("rldragonsteel_item", stack -> isFromMod(stack, "rldragonsteel"));

        // ===== 寄生虫(SRParasites)模组兼容 =====
        register("srparasites_lure", stack -> 
            stack.getItem().getRegistryName().toString().equals("srparasites:lure"));
        register("srparasites_evolution", stack -> 
            stack.getItem().getRegistryName().toString().contains("evolution"));
        register("srparasites_living", stack -> 
            stack.getItem().getRegistryName().toString().contains("living"));
        register("srparasites_dropped_item", stack -> 
            isFromMod(stack, "srparasites") && 
            stack.getItem().getRegistryName().toString().contains("dropped"));
        register("srparasites_item", stack -> isFromMod(stack, "srparasites"));

        // ===== 亡灵崛起(Fish's Undead Rising)模组兼容 =====
        register("undeadrising_item", stack -> isFromMod(stack, "undeadrising"));

        // ===== 喇叭骷髅(Trumpet Skeleton)模组兼容 =====
        register("trumpetskeleton_trumpet", stack -> 
            stack.getItem().getRegistryName().toString().contains("trumpet"));
        register("trumpetskeleton_item", stack -> isFromMod(stack, "trumpetskeleton"));

        // ===== 动物伙伴(Familiar Fauna)模组兼容 =====
        register("familiarfauna_item", stack -> isFromMod(stack, "familiarfauna"));

        // ===== 通用冒险分类标签 =====
        // 这些标签可以跨模组识别冒险相关物品
        register("adventure_weapon", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("dagger") || name.contains("longsword") || name.contains("katana") ||
                   name.contains("saber") || name.contains("rapier") || name.contains("greatsword") ||
                   name.contains("hammer") || name.contains("warhammer") || name.contains("halberd") ||
                   name.contains("pike") || name.contains("lance") || name.contains("glaive") ||
                   name.contains("mace") || name.contains("battleaxe") || name.contains("spear") ||
                   name.contains("javelin") || name.contains("boomerang") || name.contains("throwing");
        });
        register("adventure_armor", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return (name.contains("armor") || name.contains("helmet") || name.contains("chestplate") ||
                    name.contains("leggings") || name.contains("boots")) &&
                   (name.contains("dragon") || name.contains("silver") || name.contains("steel") ||
                    name.contains("bronze") || name.contains("mythic"));
        });
        register("adventure_boss_loot", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("heart") || name.contains("scale") || name.contains("bone") ||
                   name.contains("skull") || name.contains("trophy") || name.contains("soul");
        });
        register("adventure_consumable", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("stew") || name.contains("meal") || name.contains("feast") ||
                   name.contains("elixir") || name.contains("potion") || name.contains("drink");
        });
        register("adventure_tool", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("horn") || name.contains("flute") || name.contains("staff") ||
                   name.contains("scepter") || name.contains("wand") || name.contains("rod");
        });
        register("adventure_treasure", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("treasure") || name.contains("artifact") || name.contains("relic") ||
                   name.contains("ancient") || name.contains("legendary") || name.contains("mythic");
        });
        register("adventure_key", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("key") || name.contains("lock") || name.contains("pass");
        });
        register("adventure_map_item", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("map") || name.contains("compass") || name.contains("locator") ||
                   name.contains("navigator");
        });
        register("adventure_teleport", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("teleport") || name.contains("warp") || name.contains("portal") ||
                   name.contains("waystone") || name.contains("scroll");
        });
        register("adventure_quest_item", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("quest") || name.contains("bounty") || name.contains("contract") ||
                   name.contains("mission") || name.contains("task");
        });
        register("adventure_bauble", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("ring") || name.contains("amulet") || name.contains("belt") ||
                   name.contains("charm") || name.contains("trinket") || name.contains("bauble") ||
                   name.contains("talisman") || name.contains("medallion");
        });
        register("adventure_material_rare", stack -> {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("dragon") || name.contains("mythic") || name.contains("ancient") ||
                   name.contains("celestial") || name.contains("infernal") || name.contains("void");
        });
    }

    /**
     * 检查物品是否来自指定模组
     */
    private static boolean isFromMod(ItemStack stack, String modId) {
        if (stack.isEmpty() || modId == null || modId.isEmpty()) {
            return false;
        }
        net.minecraft.util.ResourceLocation registryName = stack.getItem().getRegistryName();
        return registryName != null && registryName.getNamespace().equals(modId);
    }

    /**
     * 检查物品是否是Baubles饰品类型
     * 使用反射避免硬依赖
     */
    private static boolean isBaubleType(ItemStack stack, String type) {
        if (stack.isEmpty()) {
            return false;
        }
        try {
            // 检查Baubles API是否存在
            Class<?> iBaubleClass = Class.forName("baubles.api.IBauble");
            Class<?> baubleTypeClass = Class.forName("baubles.api.BaubleType");
            
            // 检查物品是否实现了IBauble接口
            if (iBaubleClass.isInstance(stack.getItem())) {
                if (type == null) {
                    return true;
                }
                Object bauble = iBaubleClass.cast(stack.getItem());
                java.lang.reflect.Method getBaubleType = iBaubleClass.getMethod("getBaubleType", ItemStack.class);
                Object baubleType = getBaubleType.invoke(bauble, stack);
                if (baubleType != null) {
                    return baubleType.toString().equals(type);
                }
            }
        } catch (ClassNotFoundException e) {
            // Baubles未安装，忽略
        } catch (Exception e) {
            // 其他异常，安全处理
        }
        return false;
    }
    
    /**
     * 行为标签定义
     */
    private static class BehaviorTag {
        final String id;
        final Predicate<ItemStack> predicate;
        
        BehaviorTag(String id, Predicate<ItemStack> predicate) {
            this.id = id;
            this.predicate = predicate;
        }
    }
    
    /**
     * 注册行为标签
     */
    private static void register(String id, Predicate<ItemStack> predicate) {
        BEHAVIOR_TAGS.add(new BehaviorTag(id, predicate));
    }

    /**
     * 检查物品是否匹配指定的矿物词典名称
     */
    private static boolean isOreDict(ItemStack stack, String oreName) {
        if (stack.isEmpty() || oreName == null || oreName.isEmpty()) {
            return false;
        }
        int[] oreIDs = net.minecraftforge.oredict.OreDictionary.getOreIDs(stack);
        int targetID = net.minecraftforge.oredict.OreDictionary.getOreID(oreName);
        for (int id : oreIDs) {
            if (id == targetID) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查附魔书是否包含特定附魔
     */
    private static boolean hasSpecificEnchantment(ItemStack stack, Enchantment enchantment) {
        if (stack.getItem() != Items.ENCHANTED_BOOK || enchantment == null) return false;
        NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
        if (enchantments == null) return false;
        
        int targetId = Enchantment.getEnchantmentID(enchantment);
        for (int i = 0; i < enchantments.tagCount(); i++) {
            NBTTagCompound tag = enchantments.getCompoundTagAt(i);
            if (tag.getShort("id") == targetId) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查附魔书是否包含特定类型的附魔
     * @param type 类型：weapon, tool, armor, utility
     */
    private static boolean hasEnchantmentType(ItemStack stack, String type) {
        if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
        NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
        if (enchantments == null) return false;
        
        for (int i = 0; i < enchantments.tagCount(); i++) {
            NBTTagCompound tag = enchantments.getCompoundTagAt(i);
            int id = tag.getShort("id");
            Enchantment ench = Enchantment.getEnchantmentByID(id);
            if (ench == null) continue;
            
            // 根据附魔类型判断
            switch (type) {
                case "weapon":
                    if (ench.type == EnumEnchantmentType.WEAPON ||
                        ench.type == EnumEnchantmentType.BOW) {
                        return true;
                    }
                    break;
                case "tool":
                    if (ench.type == EnumEnchantmentType.DIGGER ||
                        ench.type == EnumEnchantmentType.ALL) {
                        return true;
                    }
                    break;
                case "armor":
                    if (ench.type == EnumEnchantmentType.ARMOR ||
                        ench.type == EnumEnchantmentType.ARMOR_FEET ||
                        ench.type == EnumEnchantmentType.ARMOR_HEAD ||
                        ench.type == EnumEnchantmentType.ARMOR_CHEST ||
                        ench.type == EnumEnchantmentType.ARMOR_LEGS) {
                        return true;
                    }
                    break;
                case "utility":
                    if (ench.type == EnumEnchantmentType.ALL ||
                        ench.type == EnumEnchantmentType.FISHING_ROD ||
                        ench.type == EnumEnchantmentType.BREAKABLE) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }
    
    /**
     * 检查附魔书是否包含特定附魔类型（支持模组附魔）
     * @param type 附魔类型枚举
     */
    private static boolean hasEnchantmentType(ItemStack stack, EnumEnchantmentType type) {
        if (stack.getItem() != Items.ENCHANTED_BOOK || type == null) return false;
        NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
        if (enchantments == null) return false;
        
        for (int i = 0; i < enchantments.tagCount(); i++) {
            NBTTagCompound tag = enchantments.getCompoundTagAt(i);
            int id = tag.getShort("id");
            Enchantment ench = Enchantment.getEnchantmentByID(id);
            if (ench != null && ench.type == type) {
                return true;
            }
        }
        return false;
    }
    
    public BehaviorTagAttribute() {
        this.tagId = "";
    }
    
    public BehaviorTagAttribute(String tagId) {
        this.tagId = tagId != null ? tagId : "";
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack == null || stack.isEmpty() || tagId == null || tagId.isEmpty()) return false;
        
        try {
            for (BehaviorTag tag : BEHAVIOR_TAGS) {
                if (tagId.equals(tag.id)) {
                    return tag.predicate.test(stack);
                }
            }
        } catch (Exception e) {
            // 安全处理异常
            return false;
        }
        return false;
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        if (stack == null || stack.isEmpty()) return attributes;
        
        // 检查所有行为标签
        for (BehaviorTag tag : BEHAVIOR_TAGS) {
            try {
                if (tag.predicate.test(stack)) {
                    attributes.add(new BehaviorTagAttribute(tag.id));
                }
            } catch (Exception e) {
                // 安全处理异常，继续检查下一个标签
            }
        }
        return attributes;
    }
    
    @Override
    public String getTranslationKey() {
        return "behavior_tag";
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        if (nbt != null) {
            nbt.setString("tagId", tagId != null ? tagId : "");
        }
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        if (nbt != null) {
            return new BehaviorTagAttribute(nbt.getString("tagId"));
        }
        return new BehaviorTagAttribute("");
    }
    
    @Override
    public String getNBTKey() {
        return "behavior_tag";
    }
    
    @Override
    public boolean canRead(NBTTagCompound nbt) {
        return nbt != null && nbt.hasKey("behavior_tag");
    }
    
    @Override
    public Object[] getTranslationParameters() {
        // 使用翻译键显示标签名称，如果翻译不存在则显示原始ID
        String translationKey = "behavior_tag." + tagId;
        String translated = new TextComponentTranslation(translationKey).getUnformattedText();
        // 如果翻译不存在，翻译键会原样返回
        if (translated.equals(translationKey)) {
            return new Object[]{tagId};
        }
        return new Object[]{translated};
    }
}
