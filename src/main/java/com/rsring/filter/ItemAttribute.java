package com.rsring.filter;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品属性接口 - 用于属性过滤模式
 * 结合机械动力的属性过滤器和精妙背包的高级词条升级
 */
public interface ItemAttribute {
    
    /**
     * 属性注册表 - 存储所有已注册的属性类型实例
     */
    List<ItemAttribute> REGISTRY = new ArrayList<>();
    
    /**
     * 注册属性类型
     */
    static ItemAttribute register(ItemAttribute attribute) {
        REGISTRY.add(attribute);
        return attribute;
    }
    
    /**
     * 从 NBT 读取属性
     */
    static ItemAttribute fromNBT(NBTTagCompound nbt) {
        if (nbt == null) return null;
        
        try {
            for (ItemAttribute attribute : REGISTRY) {
                if (attribute != null && attribute.canRead(nbt)) {
                    return attribute.readNBT(nbt.getCompoundTag(attribute.getNBTKey()));
                }
            }
        } catch (Exception e) {
            // 安全处理异常
            return null;
        }
        return null;
    }
    
    /**
     * 检查物品是否具有此属性（带世界参数）
     * 参照机械动力的实现
     */
    default boolean appliesTo(ItemStack stack, World world) {
        return appliesTo(stack);
    }
    
    /**
     * 检查物品是否具有此属性
     */
    boolean appliesTo(ItemStack stack);
    
    /**
     * 列出物品的所有此类型属性（带世界参数）
     * 参照机械动力的实现
     */
    default List<ItemAttribute> listAttributesOf(ItemStack stack, World world) {
        return listAttributesOf(stack);
    }
    
    /**
     * 列出物品的所有此类型属性
     */
    List<ItemAttribute> listAttributesOf(ItemStack stack);
    
    /**
     * 获取翻译键
     */
    String getTranslationKey();
    
    /**
     * 写入 NBT
     */
    void writeNBT(NBTTagCompound nbt);
    
    /**
     * 从 NBT 读取
     */
    ItemAttribute readNBT(NBTTagCompound nbt);
    
    /**
     * 序列化到 NBT
     */
    default void serializeNBT(NBTTagCompound nbt) {
        NBTTagCompound compound = new NBTTagCompound();
        writeNBT(compound);
        nbt.setTag(getNBTKey(), compound);
    }
    
    /**
     * 获取 NBT 键名
     */
    default String getNBTKey() {
        return getTranslationKey();
    }
    
    /**
     * 检查 NBT 是否包含此属性
     */
    default boolean canRead(NBTTagCompound nbt) {
        return nbt.hasKey(getNBTKey());
    }
    
    /**
     * 格式化为显示文本
     */
    default ITextComponent format(boolean inverted) {
        String key = "item_attributes." + getTranslationKey() + (inverted ? ".inverted" : "");
        return new TextComponentTranslation(key, getTranslationParameters());
    }
    
    /**
     * 获取翻译参数
     */
    default Object[] getTranslationParameters() {
        return new Object[0];
    }
    
    // ==================== 内置属性类型 ====================
    
    /**
     * OreDictionary标签属性 - 检查物品是否属于指定矿物词典
     * 1.12.2版本的标签系统使用OreDictionary
     * 显示格式类似精妙背包：ore:oreIron, forge:oreIron 等
     */
    class InTag implements ItemAttribute {
        private String tagName;
        private String namespace; // 命名空间，默认为 "ore"
        
        public InTag() {
            this.tagName = "";
            this.namespace = "ore";
        }
        
        public InTag(String tagName) {
            this.tagName = tagName;
            this.namespace = inferNamespace(tagName);
        }
        
        public InTag(String tagName, String namespace) {
            this.tagName = tagName;
            this.namespace = namespace != null ? namespace : "ore";
        }
        
        /**
         * 根据标签名推断命名空间
         */
        private String inferNamespace(String name) {
            if (name == null || name.isEmpty()) return "ore";
            // 常见的 Forge 统一标签前缀
            if (name.startsWith("ingot") || name.startsWith("ore") || name.startsWith("nugget") ||
                name.startsWith("block") || name.startsWith("dust") || name.startsWith("gem") ||
                name.startsWith("plate") || name.startsWith("gear") || name.startsWith("rod") ||
                name.startsWith("storage") || name.startsWith("dye")) {
                return "forge";
            }
            // 原版相关标签
            if (name.startsWith("log") || name.startsWith("plank") || name.startsWith("leaves") ||
                name.startsWith("sapling") || name.startsWith("slab") || name.startsWith("stairs") ||
                name.startsWith("fence") || name.startsWith("door") || name.startsWith("wool") ||
                name.startsWith("carpet") || name.startsWith("glass") || name.startsWith("sand") ||
                name.startsWith("stone") || name.startsWith("cobblestone")) {
                return "minecraft";
            }
            // 默认使用 ore
            return "ore";
        }
        
        @Override
        public boolean appliesTo(ItemStack stack) {
            if (tagName == null || tagName.isEmpty() || stack.isEmpty()) return false;
            
            // 1. 使用OreDictionary检查
            int[] ids = net.minecraftforge.oredict.OreDictionary.getOreIDs(stack);
            for (int id : ids) {
                String name = net.minecraftforge.oredict.OreDictionary.getOreName(id);
                if (tagName.equals(name)) {
                    return true;
                }
            }
            
            // 2. 检查推断的虚拟标签
            List<ItemAttribute> inferredAttrs = new ArrayList<>();
            addInferredTags(stack, inferredAttrs);
            for (ItemAttribute attr : inferredAttrs) {
                if (attr instanceof InTag) {
                    InTag inferredTag = (InTag) attr;
                    if (tagName.equals(inferredTag.tagName)) {
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        @Override
        public List<ItemAttribute> listAttributesOf(ItemStack stack) {
            List<ItemAttribute> attributes = new ArrayList<>();
            if (stack.isEmpty()) return attributes;
            
            // 获取物品的所有矿物词典标签
            int[] ids = net.minecraftforge.oredict.OreDictionary.getOreIDs(stack);
            for (int id : ids) {
                String name = net.minecraftforge.oredict.OreDictionary.getOreName(id);
                attributes.add(new InTag(name));
            }
            
            // 始终添加推断的虚拟标签（不仅仅是在OreDictionary为空时）
            // 这样即使物品有OreDictionary标签，也能获得更细分的标签
            addInferredTags(stack, attributes);
            
            return attributes;
        }
        
        /**
         * 基于物品类型推断虚拟标签（模拟 1.20.x 的 ItemTags）
         */
        private void addInferredTags(ItemStack stack, List<ItemAttribute> attributes) {
            net.minecraft.item.Item item = stack.getItem();
            
            // ===== 矿物相关 =====
            // 钻石
            if (item == net.minecraft.init.Items.DIAMOND) {
                attributes.add(new InTag("gems/diamond", "forge"));
                attributes.add(new InTag("gems", "forge"));
            }
            // 铁锭
            if (item == net.minecraft.init.Items.IRON_INGOT) {
                attributes.add(new InTag("ingots/iron", "forge"));
                attributes.add(new InTag("ingots", "forge"));
            }
            // 金锭
            if (item == net.minecraft.init.Items.GOLD_INGOT) {
                attributes.add(new InTag("ingots/gold", "forge"));
                attributes.add(new InTag("ingots", "forge"));
            }
            // 铁粒
            if (item == net.minecraft.init.Items.IRON_NUGGET) {
                attributes.add(new InTag("nuggets/iron", "forge"));
                attributes.add(new InTag("nuggets", "forge"));
            }
            // 金粒
            if (item == net.minecraft.init.Items.GOLD_NUGGET) {
                attributes.add(new InTag("nuggets/gold", "forge"));
                attributes.add(new InTag("nuggets", "forge"));
            }
            // 煤炭
            if (item == net.minecraft.init.Items.COAL) {
                attributes.add(new InTag("gems/coal", "forge"));
                attributes.add(new InTag("gems", "forge"));
                attributes.add(new InTag("coal", "forge"));
            }
            // 木炭
            if (item == net.minecraft.init.Items.COAL && stack.getMetadata() == 1) {
                attributes.add(new InTag("gems/charcoal", "forge"));
                attributes.add(new InTag("charcoal", "forge"));
            }
            // 青金石
            if (item == net.minecraft.init.Items.DYE && stack.getMetadata() == 4) {
                attributes.add(new InTag("gems/lapis", "forge"));
                attributes.add(new InTag("gems", "forge"));
            }
            // 红石
            if (item == net.minecraft.init.Items.REDSTONE) {
                attributes.add(new InTag("dusts/redstone", "forge"));
                attributes.add(new InTag("dusts", "forge"));
            }
            // 荧石粉
            if (item == net.minecraft.init.Items.GLOWSTONE_DUST) {
                attributes.add(new InTag("dusts/glowstone", "forge"));
                attributes.add(new InTag("dusts", "forge"));
            }
            // 火药
            if (item == net.minecraft.init.Items.GUNPOWDER) {
                attributes.add(new InTag("dusts/gunpowder", "forge"));
                attributes.add(new InTag("dusts", "forge"));
            }
            // 骨粉
            if (item == net.minecraft.init.Items.DYE && stack.getMetadata() == 15) {
                attributes.add(new InTag("dusts/bone", "forge"));
                attributes.add(new InTag("dusts", "forge"));
            }
            // 末影珍珠
            if (item == net.minecraft.init.Items.ENDER_PEARL) {
                attributes.add(new InTag("ender_pearls", "forge"));
                attributes.add(new InTag("pearls", "forge"));
            }
            // 末影之眼
            if (item == net.minecraft.init.Items.ENDER_EYE) {
                attributes.add(new InTag("ender_eyes", "forge"));
                attributes.add(new InTag("eyes", "forge"));
            }
            // 烈焰棒
            if (item == net.minecraft.init.Items.BLAZE_ROD) {
                attributes.add(new InTag("rods/blaze", "forge"));
                attributes.add(new InTag("rods", "forge"));
            }
            // 烈焰粉
            if (item == net.minecraft.init.Items.BLAZE_POWDER) {
                attributes.add(new InTag("dusts/blaze", "forge"));
                attributes.add(new InTag("dusts", "forge"));
            }
            // 恶魂之泪
            if (item == net.minecraft.init.Items.GHAST_TEAR) {
                attributes.add(new InTag("gems/ghast_tear", "forge"));
                attributes.add(new InTag("tears", "forge"));
            }
            // 粘液球
            if (item == net.minecraft.init.Items.SLIME_BALL) {
                attributes.add(new InTag("slimeballs", "forge"));
                attributes.add(new InTag("balls", "forge"));
            }
            // 岩浆膏
            if (item == net.minecraft.init.Items.MAGMA_CREAM) {
                attributes.add(new InTag("magma_creams", "forge"));
                attributes.add(new InTag("creams", "forge"));
            }
            // 糖
            if (item == net.minecraft.init.Items.SUGAR) {
                attributes.add(new InTag("sugar", "forge"));
            }
            // 线
            if (item == net.minecraft.init.Items.STRING) {
                attributes.add(new InTag("string", "forge"));
            }
            // 羽毛
            if (item == net.minecraft.init.Items.FEATHER) {
                attributes.add(new InTag("feathers", "forge"));
            }
            // 皮革
            if (item == net.minecraft.init.Items.LEATHER) {
                attributes.add(new InTag("leather", "forge"));
            }
            // 兔子皮
            if (item == net.minecraft.init.Items.RABBIT_HIDE) {
                attributes.add(new InTag("hides/rabbit", "forge"));
                attributes.add(new InTag("hides", "forge"));
            }
            // 墨囊
            if (item == net.minecraft.init.Items.DYE && stack.getMetadata() == 0) {
                attributes.add(new InTag("dyes/black", "forge"));
                attributes.add(new InTag("dyes", "forge"));
            }
            // 下界石英
            if (item == net.minecraft.init.Items.QUARTZ) {
                attributes.add(new InTag("gems/quartz", "forge"));
                attributes.add(new InTag("gems", "forge"));
            }
            // 绿宝石
            if (item == net.minecraft.init.Items.EMERALD) {
                attributes.add(new InTag("gems/emerald", "forge"));
                attributes.add(new InTag("gems", "forge"));
            }
            // 紫颂果
            if (item == net.minecraft.init.Items.CHORUS_FRUIT) {
                attributes.add(new InTag("fruits/chorus", "forge"));
                attributes.add(new InTag("fruits", "forge"));
            }
            // 紫颂花
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.CHORUS_FLOWER)) {
                attributes.add(new InTag("flowers/chorus", "forge"));
            }
            // 地图
            if (item == net.minecraft.init.Items.MAP) {
                attributes.add(new InTag("maps", "forge"));
            }
            // 探险家地图
            if (item == net.minecraft.init.Items.FILLED_MAP) {
                attributes.add(new InTag("maps/filled", "forge"));
                attributes.add(new InTag("maps", "forge"));
            }
            // 龙息
            if (item == net.minecraft.init.Items.DRAGON_BREATH) {
                attributes.add(new InTag("breaths/dragon", "forge"));
            }
            // 经验瓶
            if (item == net.minecraft.init.Items.EXPERIENCE_BOTTLE) {
                attributes.add(new InTag("bottles/experience", "forge"));
                attributes.add(new InTag("bottles", "forge"));
            }
            // 发酵蛛眼
            if (item == net.minecraft.init.Items.FERMENTED_SPIDER_EYE) {
                attributes.add(new InTag("eyes/spider", "forge"));
            }
            // 恶魂之泪
            if (item == net.minecraft.init.Items.SPECKLED_MELON) {
                attributes.add(new InTag("melons/speckled", "forge"));
            }
            // 营火材料
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.TORCH)) {
                attributes.add(new InTag("lights/torch", "forge"));
                attributes.add(new InTag("lights", "forge"));
            }
            // 火焰弹
            if (item == net.minecraft.init.Items.FIRE_CHARGE) {
                attributes.add(new InTag("fireballs", "forge"));
                attributes.add(new InTag("charges", "forge"));
            }
            // 雪
            if (item == net.minecraft.init.Items.SNOWBALL) {
                attributes.add(new InTag("snowballs", "forge"));
                attributes.add(new InTag("balls", "forge"));
            }
            
            // ===== 方块类推断标签 =====
            // 矿石方块
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.DIAMOND_ORE)) {
                attributes.add(new InTag("ores/diamond", "forge"));
                attributes.add(new InTag("ores", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.IRON_ORE)) {
                attributes.add(new InTag("ores/iron", "forge"));
                attributes.add(new InTag("ores", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.GOLD_ORE)) {
                attributes.add(new InTag("ores/gold", "forge"));
                attributes.add(new InTag("ores", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.COAL_ORE)) {
                attributes.add(new InTag("ores/coal", "forge"));
                attributes.add(new InTag("ores", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.LAPIS_ORE)) {
                attributes.add(new InTag("ores/lapis", "forge"));
                attributes.add(new InTag("ores", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.REDSTONE_ORE) ||
                item == Item.getItemFromBlock(net.minecraft.init.Blocks.LIT_REDSTONE_ORE)) {
                attributes.add(new InTag("ores/redstone", "forge"));
                attributes.add(new InTag("ores", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.EMERALD_ORE)) {
                attributes.add(new InTag("ores/emerald", "forge"));
                attributes.add(new InTag("ores", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.QUARTZ_ORE)) {
                attributes.add(new InTag("ores/quartz", "forge"));
                attributes.add(new InTag("ores", "forge"));
            }
            
            // 存储方块
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.DIAMOND_BLOCK)) {
                attributes.add(new InTag("storage_blocks/diamond", "forge"));
                attributes.add(new InTag("storage_blocks", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.IRON_BLOCK)) {
                attributes.add(new InTag("storage_blocks/iron", "forge"));
                attributes.add(new InTag("storage_blocks", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.GOLD_BLOCK)) {
                attributes.add(new InTag("storage_blocks/gold", "forge"));
                attributes.add(new InTag("storage_blocks", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.COAL_BLOCK)) {
                attributes.add(new InTag("storage_blocks/coal", "forge"));
                attributes.add(new InTag("storage_blocks", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.LAPIS_BLOCK)) {
                attributes.add(new InTag("storage_blocks/lapis", "forge"));
                attributes.add(new InTag("storage_blocks", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.REDSTONE_BLOCK)) {
                attributes.add(new InTag("storage_blocks/redstone", "forge"));
                attributes.add(new InTag("storage_blocks", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.EMERALD_BLOCK)) {
                attributes.add(new InTag("storage_blocks/emerald", "forge"));
                attributes.add(new InTag("storage_blocks", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.QUARTZ_BLOCK)) {
                attributes.add(new InTag("storage_blocks/quartz", "forge"));
                attributes.add(new InTag("storage_blocks", "forge"));
            }
            if (item == Item.getItemFromBlock(net.minecraft.init.Blocks.GLOWSTONE)) {
                attributes.add(new InTag("storage_blocks/glowstone", "forge"));
                attributes.add(new InTag("storage_blocks", "forge"));
            }
            
            // ===== 食物类标签 =====
            if (item instanceof net.minecraft.item.ItemFood) {
                net.minecraft.item.ItemFood food = (net.minecraft.item.ItemFood) item;
                
                // 检查是否为肉类
                if (food == net.minecraft.init.Items.COOKED_BEEF || food == net.minecraft.init.Items.BEEF) {
                    attributes.add(new InTag("cooked_beef", "forge"));
                    attributes.add(new InTag("meats", "forge"));
                    attributes.add(new InTag("foods/cooked_meat", "forge"));
                } else if (food == net.minecraft.init.Items.COOKED_PORKCHOP || food == net.minecraft.init.Items.PORKCHOP) {
                    attributes.add(new InTag("cooked_porkchop", "forge"));
                    attributes.add(new InTag("meats", "forge"));
                    attributes.add(new InTag("foods/cooked_meat", "forge"));
                } else if (food == net.minecraft.init.Items.COOKED_CHICKEN || food == net.minecraft.init.Items.CHICKEN) {
                    attributes.add(new InTag("cooked_chicken", "forge"));
                    attributes.add(new InTag("meats", "forge"));
                } else if (food == net.minecraft.init.Items.COOKED_MUTTON || food == net.minecraft.init.Items.MUTTON) {
                    attributes.add(new InTag("cooked_mutton", "forge"));
                    attributes.add(new InTag("meats", "forge"));
                } else if (food == net.minecraft.init.Items.COOKED_RABBIT || food == net.minecraft.init.Items.RABBIT) {
                    attributes.add(new InTag("cooked_rabbit", "forge"));
                    attributes.add(new InTag("meats", "forge"));
                } else if (food == net.minecraft.init.Items.COOKED_FISH || food == net.minecraft.init.Items.FISH) {
                    attributes.add(new InTag("cooked_fish", "forge"));
                    attributes.add(new InTag("fishes", "forge"));
                } else if (food == net.minecraft.init.Items.BREAD) {
                    attributes.add(new InTag("bread", "forge"));
                    attributes.add(new InTag("foods", "forge"));
                } else if (food == net.minecraft.init.Items.APPLE) {
                    attributes.add(new InTag("fruits/apple", "forge"));
                    attributes.add(new InTag("fruits", "forge"));
                } else if (food == net.minecraft.init.Items.CARROT) {
                    attributes.add(new InTag("vegetables/carrot", "forge"));
                    attributes.add(new InTag("vegetables", "forge"));
                } else if (food == net.minecraft.init.Items.POTATO || food == net.minecraft.init.Items.BAKED_POTATO) {
                    attributes.add(new InTag("vegetables/potato", "forge"));
                    attributes.add(new InTag("vegetables", "forge"));
                } else if (food == net.minecraft.init.Items.GOLDEN_APPLE || food == net.minecraft.init.Items.GOLDEN_CARROT) {
                    attributes.add(new InTag("foods/golden", "forge"));
                } else if (food == net.minecraft.init.Items.CAKE) {
                    attributes.add(new InTag("foods/cake", "forge"));
                } else if (food == net.minecraft.init.Items.COOKIE) {
                    attributes.add(new InTag("foods/cookie", "forge"));
                } else if (food == net.minecraft.init.Items.PUMPKIN_PIE) {
                    attributes.add(new InTag("foods/pie", "forge"));
                } else if (food == net.minecraft.init.Items.MELON) {
                    attributes.add(new InTag("fruits/melon", "forge"));
                    attributes.add(new InTag("fruits", "forge"));
                } else if (food == net.minecraft.init.Items.EGG) {
                    attributes.add(new InTag("eggs", "forge"));
                } else if (food == net.minecraft.init.Items.SPIDER_EYE) {
                    attributes.add(new InTag("foods/spider_eye", "forge"));
                } else {
                    // 通用食物标签
                    attributes.add(new InTag("foods", "forge"));
                }
            }
            
            // ===== 工具类标签 =====
            if (item instanceof net.minecraft.item.ItemTool || item instanceof net.minecraft.item.ItemSword || 
                item instanceof net.minecraft.item.ItemHoe || item instanceof net.minecraft.item.ItemSpade ||
                item instanceof net.minecraft.item.ItemAxe || item instanceof net.minecraft.item.ItemPickaxe) {
                attributes.add(new InTag("tools", "forge"));
            }
            // 镐子
            if (item instanceof net.minecraft.item.ItemPickaxe) {
                attributes.add(new InTag("tools/pickaxes", "forge"));
            }
            // 斧头
            if (item instanceof net.minecraft.item.ItemAxe) {
                attributes.add(new InTag("tools/axes", "forge"));
            }
            // 铲子
            if (item instanceof net.minecraft.item.ItemSpade) {
                attributes.add(new InTag("tools/shovels", "forge"));
            }
            // 锄头
            if (item instanceof net.minecraft.item.ItemHoe) {
                attributes.add(new InTag("tools/hoes", "forge"));
            }
            // 剪刀
            if (item == net.minecraft.init.Items.SHEARS) {
                attributes.add(new InTag("tools/shears", "forge"));
            }
            // 钓鱼竿
            if (item == net.minecraft.init.Items.FISHING_ROD) {
                attributes.add(new InTag("tools/fishing_rods", "forge"));
            }
            // 打火石
            if (item == net.minecraft.init.Items.FLINT_AND_STEEL) {
                attributes.add(new InTag("tools/flint_and_steel", "forge"));
            }
            // 指南针
            if (item == net.minecraft.init.Items.COMPASS) {
                attributes.add(new InTag("tools/compasses", "forge"));
            }
            // 时钟
            if (item == net.minecraft.init.Items.CLOCK) {
                attributes.add(new InTag("tools/clocks", "forge"));
            }
            
            // ===== 武器类标签 =====
            if (item instanceof net.minecraft.item.ItemSword || item instanceof net.minecraft.item.ItemBow) {
                attributes.add(new InTag("weapons", "forge"));
            }
            // 剑
            if (item instanceof net.minecraft.item.ItemSword) {
                attributes.add(new InTag("weapons/swords", "forge"));
            }
            // 弓
            if (item instanceof net.minecraft.item.ItemBow) {
                attributes.add(new InTag("weapons/bows", "forge"));
            }
            // 盾牌
            if (item == net.minecraft.init.Items.SHIELD) {
                attributes.add(new InTag("tools/shields", "forge"));
            }
            
            // ===== 盔甲类标签 - 细分为头盔、胸甲、护腿、靴子 =====
            if (item instanceof net.minecraft.item.ItemArmor) {
                net.minecraft.item.ItemArmor armor = (net.minecraft.item.ItemArmor) item;
                attributes.add(new InTag("armors", "forge"));
                
                // 根据装备槽位细分（使用 armorType 字段更可靠）
                net.minecraft.inventory.EntityEquipmentSlot slot = armor.armorType;
                if (slot != null) {
                    switch (slot) {
                        case HEAD:
                            attributes.add(new InTag("armors/helmets", "forge"));
                            break;
                        case CHEST:
                            attributes.add(new InTag("armors/chestplates", "forge"));
                            break;
                        case LEGS:
                            attributes.add(new InTag("armors/leggings", "forge"));
                            break;
                        case FEET:
                            attributes.add(new InTag("armors/boots", "forge"));
                            break;
                        default:
                            break;
                    }
                }
            }
            // 马铠
            if (item == net.minecraft.init.Items.IRON_HORSE_ARMOR || 
                item == net.minecraft.init.Items.GOLDEN_HORSE_ARMOR ||
                item == net.minecraft.init.Items.DIAMOND_HORSE_ARMOR) {
                attributes.add(new InTag("armors/horse_armors", "forge"));
            }
            
            // ===== 箭矢类标签 =====
            if (item instanceof net.minecraft.item.ItemArrow) {
                attributes.add(new InTag("arrows", "forge"));
            }
            
            // ===== 特殊物品标签 =====
            // 附魔书
            if (item == net.minecraft.init.Items.ENCHANTED_BOOK) {
                attributes.add(new InTag("books/enchanted", "forge"));
            }
            // 书
            if (item == net.minecraft.init.Items.BOOK) {
                attributes.add(new InTag("books", "forge"));
            }
            // 成书
            if (item == net.minecraft.init.Items.WRITTEN_BOOK) {
                attributes.add(new InTag("books/written", "forge"));
            }
            // 书与笔
            if (item == net.minecraft.init.Items.WRITABLE_BOOK) {
                attributes.add(new InTag("books/writable", "forge"));
            }
            // 画
            if (item == net.minecraft.init.Items.PAINTING) {
                attributes.add(new InTag("decorations/paintings", "forge"));
            }
            // 物品展示框
            if (item == net.minecraft.init.Items.ITEM_FRAME) {
                attributes.add(new InTag("decorations/item_frames", "forge"));
            }
            // 床
            if (item == net.minecraft.init.Items.BED) {
                attributes.add(new InTag("beds", "forge"));
            }
            // 花盆
            if (item == net.minecraft.init.Items.FLOWER_POT) {
                attributes.add(new InTag("decorations/flower_pots", "forge"));
            }
            // 鞍
            if (item == net.minecraft.init.Items.SADDLE) {
                attributes.add(new InTag("saddles", "forge"));
            }
            // 火焰弹
            if (item == net.minecraft.init.Items.FIRE_CHARGE) {
                attributes.add(new InTag("fireballs", "forge"));
            }
            // 雪
            if (item == net.minecraft.init.Items.SNOWBALL) {
                attributes.add(new InTag("snowballs", "forge"));
            }
            // 铁桶
            if (item == net.minecraft.init.Items.BUCKET) {
                attributes.add(new InTag("buckets/empty", "forge"));
            }
            // 水桶
            if (item == net.minecraft.init.Items.WATER_BUCKET) {
                attributes.add(new InTag("buckets/water", "forge"));
            }
            // 岩浆桶
            if (item == net.minecraft.init.Items.LAVA_BUCKET) {
                attributes.add(new InTag("buckets/lava", "forge"));
            }
            // 奶桶
            if (item == net.minecraft.init.Items.MILK_BUCKET) {
                attributes.add(new InTag("buckets/milk", "forge"));
            }
        }
        
        @Override
        public String getTranslationKey() {
            return "in_tag";
        }
        
        @Override
        public void writeNBT(NBTTagCompound nbt) {
            nbt.setString("tagName", tagName != null ? tagName : "");
            nbt.setString("namespace", namespace != null ? namespace : "ore");
        }
        
        @Override
        public ItemAttribute readNBT(NBTTagCompound nbt) {
            String name = nbt.getString("tagName");
            String ns = nbt.hasKey("namespace") ? nbt.getString("namespace") : "ore";
            return new InTag(name, ns);
        }
        
        @Override
        public Object[] getTranslationParameters() {
            if (tagName == null || tagName.isEmpty()) {
                return new Object[]{"<none>"};
            }
            // 显示格式：namespace:tagName
            return new Object[]{namespace + ":" + tagName};
        }
    }
    
    /**
     * 模组来源属性 - 按物品来源模组过滤
     */
    class AddedBy implements ItemAttribute {
        private String modId;
        
        public AddedBy() {
            this.modId = "";
        }
        
        public AddedBy(String modId) {
            this.modId = modId;
        }
        
        @Override
        public boolean appliesTo(ItemStack stack) {
            if (stack.isEmpty() || modId.isEmpty()) return false;
            String creatorModId = stack.getItem().getCreatorModId(stack);
            return modId.equals(creatorModId);
        }
        
        @Override
        public List<ItemAttribute> listAttributesOf(ItemStack stack) {
            List<ItemAttribute> attributes = new ArrayList<>();
            if (!stack.isEmpty()) {
                String creatorModId = stack.getItem().getCreatorModId(stack);
                if (creatorModId != null && !creatorModId.isEmpty()) {
                    attributes.add(new AddedBy(creatorModId));
                }
            }
            return attributes;
        }
        
        @Override
        public String getTranslationKey() {
            return "added_by";
        }
        
        @Override
        public void writeNBT(NBTTagCompound nbt) {
            nbt.setString("modId", modId != null ? modId : "");
        }
        
        @Override
        public ItemAttribute readNBT(NBTTagCompound nbt) {
            return new AddedBy(nbt.getString("modId"));
        }
        
        @Override
        public Object[] getTranslationParameters() {
            // 获取模组显示名称
            net.minecraftforge.fml.common.ModContainer container = 
                net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(modId);
            String name = container != null ? container.getName() : modId;
            return new Object[]{name};
        }
    }
}
