package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 标准特性枚举 - 预定义的物品属性
 */
public enum StandardTraits implements ItemAttribute {
    
    DUMMY(stack -> false, "dummy"),
    
    /**
     * 可放置方块
     */
    PLACEABLE(stack -> stack.getItem() instanceof ItemBlock, "placeable"),
    
    /**
     * 可食用
     */
    CONSUMABLE(stack -> stack.getItem().getItemUseAction(stack) == net.minecraft.item.EnumAction.EAT, "consumable"),
    
    /**
     * 可饮用
     */
    DRINKABLE(stack -> stack.getItem().getItemUseAction(stack) == net.minecraft.item.EnumAction.DRINK, "drinkable"),
    
    /**
     * 已附魔
     */
    ENCHANTED(ItemStack::isItemEnchanted, "enchanted"),
    
    /**
     * 附魔达到最大值
     */
    MAX_ENCHANTED(StandardTraits::isMaxEnchanted, "max_enchanted"),
    
    /**
     * 已重命名
     */
    RENAMED(ItemStack::hasDisplayName, "renamed"),
    
    /**
     * 已损坏
     */
    DAMAGED(ItemStack::isItemDamaged, "damaged"),
    
    /**
     * 严重损坏（耐久度低于 25%）
     */
    BADLY_DAMAGED(stack -> stack.isItemDamaged() && (float) stack.getItemDamage() / stack.getMaxDamage() > 0.75f, "badly_damaged"),
    
    /**
     * 轻微损坏（耐久度低于 50%）
     */
    SLIGHTLY_DAMAGED(stack -> stack.isItemDamaged() && (float) stack.getItemDamage() / stack.getMaxDamage() > 0.5f, "slightly_damaged"),
    
    /**
     * 完好无损
     */
    UNDAMAGED(stack -> stack.getItem().isDamageable() && !stack.isItemDamaged(), "undamaged"),
    
    /**
     * 不可堆叠
     */
    NOT_STACKABLE(stack -> !stack.isStackable(), "not_stackable"),
    
    /**
     * 可堆叠（堆叠数大于1）
     */
    STACKABLE(stack -> stack.isStackable() && stack.getMaxStackSize() > 1, "stackable"),
    
    /**
     * 高堆叠数（堆叠数大于等于16）
     */
    HIGH_STACKABLE(stack -> stack.getMaxStackSize() >= 16, "high_stackable"),
    
    /**
     * 最大堆叠（堆叠数64）
     */
    MAX_STACKABLE(stack -> stack.getMaxStackSize() >= 64, "max_stackable"),
    
    /**
     * 可装备
     */
    EQUIPABLE(stack -> {
        net.minecraft.inventory.EntityEquipmentSlot slot = stack.getItem().getEquipmentSlot(stack);
        return slot != null && slot.getSlotType() != net.minecraft.inventory.EntityEquipmentSlot.Type.HAND;
    }, "equipable"),
    
    /**
     * 可装备在主手
     */
    MAIN_HAND_EQUIPABLE(stack -> {
        net.minecraft.inventory.EntityEquipmentSlot slot = stack.getItem().getEquipmentSlot(stack);
        return slot == net.minecraft.inventory.EntityEquipmentSlot.MAINHAND;
    }, "main_hand_equipable"),
    
    /**
     * 可装备在副手
     */
    OFF_HAND_EQUIPABLE(stack -> {
        net.minecraft.inventory.EntityEquipmentSlot slot = stack.getItem().getEquipmentSlot(stack);
        return slot == net.minecraft.inventory.EntityEquipmentSlot.OFFHAND;
    }, "off_hand_equipable"),
    
    /**
     * 燃料
     */
    FURNACE_FUEL(TileEntityFurnace::isItemFuel, "furnace_fuel"),
    
    /**
     * 高品质燃料（燃烧时间 >= 800 ticks）
     */
    HIGH_GRADE_FUEL(stack -> TileEntityFurnace.getItemBurnTime(stack) >= 800, "high_grade_fuel"),
    
    /**
     * 低品质燃料（燃烧时间 < 800 ticks）
     */
    LOW_GRADE_FUEL(stack -> {
        int burnTime = TileEntityFurnace.getItemBurnTime(stack);
        return burnTime > 0 && burnTime < 800;
    }, "low_grade_fuel"),
    
    /**
     * 药水物品
     */
    POTION_ITEM(stack -> stack.getItem() instanceof ItemPotion || 
                        stack.getItem() == Items.SPLASH_POTION || 
                        stack.getItem() == Items.LINGERING_POTION ||
                        stack.getItem() == Items.TIPPED_ARROW, "potion_item"),
    
    /**
     * 有药水效果
     */
    HAS_POTION_EFFECT(stack -> !PotionUtils.getEffectsFromStack(stack).isEmpty(), "has_potion_effect"),
    
    /**
     * 溅射药水
     */
    SPLASH_POTION(stack -> stack.getItem() == Items.SPLASH_POTION, "splash_potion"),
    
    /**
     * 滞留药水
     */
    LINGERING_POTION(stack -> stack.getItem() == Items.LINGERING_POTION, "lingering_potion"),
    
    /**
     * 可饮用药水
     */
    DRINKABLE_POTION(stack -> stack.getItem() instanceof ItemPotion, "drinkable_potion"),
    
    /**
     * 稀有物品（基于稀有度）
     */
    RARE_ITEM(stack -> stack.getRarity() == EnumRarity.RARE, "rare_item"),
    
    /**
     * 史诗物品
     */
    EPIC_ITEM(stack -> stack.getRarity() == EnumRarity.EPIC, "epic_item"),
    
    /**
     * 常见物品
     */
    COMMON_ITEM(stack -> stack.getRarity() == EnumRarity.COMMON, "common_item"),
    
    /**
     * 不常见物品
     */
    UNCOMMON_ITEM(stack -> stack.getRarity() == EnumRarity.UNCOMMON, "uncommon_item"),
    
    /**
     * 有修复成本（在铁砧上处理过）
     */
    HAS_REPAIR_COST(stack -> stack.hasTagCompound() && stack.getTagCompound().hasKey("RepairCost"), "has_repair_cost"),
    
    /**
     * 高修复成本（修复成本 >= 10）
     */
    HIGH_REPAIR_COST(stack -> stack.hasTagCompound() && 
                             stack.getTagCompound().hasKey("RepairCost") && 
                             stack.getTagCompound().getInteger("RepairCost") >= 10, "high_repair_cost"),
    
    /**
     * 不可破坏（创造模式物品或特殊物品）
     */
    UNBREAKABLE(stack -> stack.hasTagCompound() && stack.getTagCompound().getBoolean("Unbreakable"), "unbreakable"),
    
    /**
     * 可修复物品
     */
    REPAIRABLE(stack -> stack.getItem().isRepairable(), "repairable"),
    
    /**
     * 钻石装备/工具
     */
    DIAMOND_ITEM(stack -> {
        Item item = stack.getItem();
        if (item instanceof ItemTool) {
            return ((ItemTool) item).getToolMaterialName().equals("diamond");
        }
        if (item instanceof ItemSword) {
            return ((ItemSword) item).getToolMaterialName().equals("diamond");
        }
        if (item instanceof ItemArmor) {
            return ((ItemArmor) item).getArmorMaterial() == ItemArmor.ArmorMaterial.DIAMOND;
        }
        return false;
    }, "diamond_item"),
    
    /**
     * 铁装备/工具
     */
    IRON_ITEM(stack -> {
        Item item = stack.getItem();
        if (item instanceof ItemTool) {
            return ((ItemTool) item).getToolMaterialName().equals("iron");
        }
        if (item instanceof ItemSword) {
            return ((ItemSword) item).getToolMaterialName().equals("iron");
        }
        if (item instanceof ItemArmor) {
            return ((ItemArmor) item).getArmorMaterial() == ItemArmor.ArmorMaterial.IRON;
        }
        return false;
    }, "iron_item"),
    
    /**
     * 金装备/工具
     */
    GOLDEN_ITEM(stack -> {
        Item item = stack.getItem();
        if (item instanceof ItemTool) {
            return ((ItemTool) item).getToolMaterialName().equals("gold");
        }
        if (item instanceof ItemSword) {
            return ((ItemSword) item).getToolMaterialName().equals("gold");
        }
        if (item instanceof ItemArmor) {
            return ((ItemArmor) item).getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD;
        }
        return false;
    }, "golden_item"),
    
    /**
     * 石制工具
     */
    STONE_TOOL(stack -> {
        Item item = stack.getItem();
        if (item instanceof ItemTool) {
            return ((ItemTool) item).getToolMaterialName().equals("stone");
        }
        if (item instanceof ItemSword) {
            return ((ItemSword) item).getToolMaterialName().equals("stone");
        }
        return false;
    }, "stone_tool"),
    
    /**
     * 木制工具
     */
    WOODEN_TOOL(stack -> {
        Item item = stack.getItem();
        if (item instanceof ItemTool) {
            return ((ItemTool) item).getToolMaterialName().equals("wood");
        }
        if (item instanceof ItemSword) {
            return ((ItemSword) item).getToolMaterialName().equals("wood");
        }
        return false;
    }, "wooden_tool"),
    
    /**
     * 皮革装备
     */
    LEATHER_ARMOR(stack -> {
        if (stack.getItem() instanceof ItemArmor) {
            return ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER;
        }
        return false;
    }, "leather_armor"),
    
    /**
     * 锁链装备
     */
    CHAINMAIL_ARMOR(stack -> {
        if (stack.getItem() instanceof ItemArmor) {
            return ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.CHAIN;
        }
        return false;
    }, "chainmail_armor"),
    
    /**
     * 有冷却时间的物品
     */
    HAS_COOLDOWN(stack -> stack.getItem().hasEffect(stack), "has_cooldown"),
    
    /**
     * 发光物品（有附魔光效）
     */
    GLINT_EFFECT(stack -> stack.hasEffect(), "glint_effect"),
    
    /**
     * 可染色物品
     */
    DYEABLE(stack -> stack.getItem() instanceof net.minecraft.item.IItemPropertyGetter && 
                    stack.getItem() == Items.LEATHER_HELMET ||
                    stack.getItem() == Items.LEATHER_CHESTPLATE ||
                    stack.getItem() == Items.LEATHER_LEGGINGS ||
                    stack.getItem() == Items.LEATHER_BOOTS, "dyeable"),
    
    /**
     * 已染色物品
     */
    DYED(stack -> stack.hasTagCompound() && stack.getTagCompound().hasKey("display") &&
                 stack.getTagCompound().getCompoundTag("display").hasKey("color"), "dyed"),
    
    /**
     * 有方块实体标签
     */
    HAS_BLOCK_ENTITY_TAG(stack -> stack.hasTagCompound() && stack.getTagCompound().hasKey("BlockEntityTag"), "has_block_entity_tag"),
    
    /**
     * 创造模式专用物品
     */
    CREATIVE_ONLY(stack -> stack.getItem().getCreativeTab() == null, "creative_only"),
    
    /**
     * 可修复
     */
    CAN_REPAIR(stack -> stack.getItem().getIsRepairable(stack, ItemStack.EMPTY), "can_repair"),
    
    /**
     * 旗帜
     */
    BANNER(stack -> stack.getItem() == Items.BANNER, "banner"),
    
    /**
     * 盾牌
     */
    SHIELD(stack -> stack.getItem() == Items.SHIELD, "shield"),
    
    /**
     * 有图案的盾牌
     */
    PATTERNED_SHIELD(stack -> {
        if (stack.getItem() != Items.SHIELD) return false;
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("BlockEntityTag");
    }, "patterned_shield"),
    
    /**
     * 烟花火箭
     */
    FIREWORK_ROCKET(stack -> stack.getItem() == Items.FIREWORKS, "firework_rocket"),
    
    /**
     * 烟花之星
     */
    FIREWORK_STAR(stack -> stack.getItem() == Items.FIREWORK_CHARGE, "firework_star"),
    
    /**
     * 信标相关物品
     */
    BEACON_PAYMENT(stack -> {
        Item item = stack.getItem();
        return item == Items.DIAMOND || item == Items.EMERALD || 
               item == Items.GOLD_INGOT || item == Items.IRON_INGOT;
    }, "beacon_payment"),
    
    /**
     * 红石信号相关
     */
    REDSTONE_RELATED(stack -> {
        Item item = stack.getItem();
        return item == Items.REDSTONE || item == Items.REPEATER ||
               item == Items.COMPARATOR ||
               item == Item.getItemFromBlock(net.minecraft.init.Blocks.REDSTONE_TORCH);
    }, "redstone_related"),

    /**
     * 可熔炼（可在熔炉中烧炼）
     */
    SMELTABLE(stack -> {
        // 检查是否有熔炉烧炼配方
        return getSmeltingResult(stack) != null;
    }, "smeltable"),

    /**
     * 可堆肥（可放入堆肥桶）
     */
    COMPOSTABLE(stack -> {
        // 1.12.2 使用 ComposterBlock 的 compostables 列表
        // 由于1.12.2没有堆肥桶，这里模拟一些常见的可堆肥物品
        Item item = stack.getItem();
        // 种子类
        if (item == Items.WHEAT_SEEDS || item == Items.PUMPKIN_SEEDS ||
            item == Items.MELON_SEEDS || item == Items.BEETROOT_SEEDS) return true;
        // 植物类
        if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.LEAVES) ||
            item == Item.getItemFromBlock(net.minecraft.init.Blocks.LEAVES2)) return true;
        if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.SAPLING)) return true;
        if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.TALLGRASS)) return true;
        // 食物残渣
        if (item == Items.APPLE || item == Items.POTATO || item == Items.CARROT ||
            item == Items.BEETROOT || item == Items.MELON || item == Items.WHEAT ||
            item == Items.BREAD || item == Items.COOKIE || item == Items.CAKE ||
            item == Items.PUMPKIN_PIE) return true;
        // 其他有机物
        if (item == Items.EGG) return true;
        if (item == Items.FISH || item == Items.COOKED_FISH) return true;
        if (item == Items.STRING) return true;
        return false;
    }, "compostable");

    private final Predicate<ItemStack> test;
    private final String translationKey;
    
    StandardTraits(Predicate<ItemStack> test, String translationKey) {
        this.test = test;
        this.translationKey = translationKey;
    }
    
    /**
     * 检查附魔是否达到最大值
     */
    private static boolean isMaxEnchanted(ItemStack stack) {
        if (!stack.isItemEnchanted()) {
            return false;
        }

        net.minecraft.nbt.NBTTagList enchantments = stack.getEnchantmentTagList();
        if (enchantments == null) {
            return false;
        }

        for (int i = 0; i < enchantments.tagCount(); i++) {
            net.minecraft.nbt.NBTTagCompound enchantment = enchantments.getCompoundTagAt(i);
            int enchantId = enchantment.getShort("id");
            int level = enchantment.getShort("lvl");

            net.minecraft.enchantment.Enchantment enchant = net.minecraft.enchantment.Enchantment.getEnchantmentByID(enchantId);
            if (enchant != null && level >= enchant.getMaxLevel()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取熔炉烧炼结果
     * 用于检测物品是否可熔炼
     */
    private static ItemStack getSmeltingResult(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        try {
            // 使用 FurnaceRecipes 获取烧炼结果
            return net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingResult(stack);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        return test.test(stack);
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        for (StandardTraits trait : values()) {
            if (trait != DUMMY && trait.appliesTo(stack)) {
                attributes.add(trait);
            }
        }
        return attributes;
    }
    
    @Override
    public String getTranslationKey() {
        return translationKey;
    }
    
    @Override
    public void writeNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        nbt.setBoolean(name(), true);
    }
    
    @Override
    public ItemAttribute readNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        for (StandardTraits trait : values()) {
            if (nbt.hasKey(trait.name())) {
                return trait;
            }
        }
        return null;
    }
    
    @Override
    public String getNBTKey() {
        return "standard_trait";
    }
    
    @Override
    public boolean canRead(net.minecraft.nbt.NBTTagCompound nbt) {
        return nbt.hasKey("standard_trait");
    }
}
