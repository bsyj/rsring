package com.rsring.test;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 自定义容量储罐示例代�? * 方案B：通过NBT标签直接存储容量，实现与玩家等级对应的储�? */
public class CustomCapacityTanks {
    
    // 玩家等级对应的总经验值（基于Minecraft官方公式�?    public static final long XP_FOR_LEVEL_100 = 873320;
    public static final long XP_FOR_LEVEL_500 = 167754120;
    public static final long XP_FOR_LEVEL_1000 = 1418780120;
    public static final long XP_FOR_LEVEL_2000 = 11670582120;
    
    /**
     * 创建对应100级玩家经验的储罐
     */
    public static ItemStack createLevel100Tank() {
        ItemStack tank = new ItemStack(/* 储罐物品 */);
        setCustomCapacity(tank, XP_FOR_LEVEL_100);
        setTankLevel(tank, 11); // 对应原系统的储罐等级
        setTankName(tank, "经验储罐 - 100�?);
        return tank;
    }
    
    /**
     * 创建对应500级玩家经验的储罐
     */
    public static ItemStack createLevel500Tank() {
        ItemStack tank = new ItemStack(/* 储罐物品 */);
        setCustomCapacity(tank, XP_FOR_LEVEL_500);
        setTankLevel(tank, 19); // 对应原系统的储罐等级
        setTankName(tank, "经验储罐 - 500�?);
        return tank;
    }
    
    /**
     * 创建对应1000级玩家经验的储罐
     */
    public static ItemStack createLevel1000Tank() {
        ItemStack tank = new ItemStack(/* 储罐物品 */);
        setCustomCapacity(tank, XP_FOR_LEVEL_1000);
        setTankLevel(tank, 22); // 对应原系统的储罐等级
        setTankName(tank, "经验储罐 - 1000�?);
        return tank;
    }
    
    /**
     * 创建对应2000级玩家经验的储罐
     */
    public static ItemStack createLevel2000Tank() {
        ItemStack tank = new ItemStack(/* 储罐物品 */);
        setCustomCapacity(tank, XP_FOR_LEVEL_2000);
        setTankLevel(tank, 25); // 对应原系统的储罐等级
        setTankName(tank, "经验储罐 - 2000�?);
        return tank;
    }
    
    /**
     * 创建无限容量储罐
     */
    public static ItemStack createInfiniteTank() {
        ItemStack tank = new ItemStack(/* 储罐物品 */);
        setInfinite(tank, true);
        setTankName(tank, "经验储罐 - 无限");
        return tank;
    }
    
    // ==================== NBT操作方法 ====================
    
    /**
     * 设置储罐的自定义容量
     */
    public static void setCustomCapacity(ItemStack stack, long capacity) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setLong("CustomCapacity", capacity);
    }
    
    /**
     * 获取储罐的自定义容量
     */
    public static long getCustomCapacity(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("CustomCapacity")) {
            return stack.getTagCompound().getLong("CustomCapacity");
        }
        return -1; // 表示使用默认容量计算
    }
    
    /**
     * 设置储罐是否为无限容�?     */
    public static void setInfinite(ItemStack stack, boolean isInfinite) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setBoolean("IsInfinite", isInfinite);
    }
    
    /**
     * 检查储罐是否为无限容量
     */
    public static boolean isInfinite(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean("IsInfinite");
    }
    
    /**
     * 设置储罐等级（用于兼容性）
     */
    public static void setTankLevel(ItemStack stack, int level) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger("TankLevel", level);
    }
    
    /**
     * 设置储罐名称
     */
    public static void setTankName(ItemStack stack, String name) {
        stack.setStackDisplayName(name);
    }
    
    // ==================== 核心容量计算方法 ====================
    
    /**
     * 获取储罐的实际容�?     * 优先使用自定义容量，否则使用默认计算
     */
    public static long getTankCapacity(ItemStack stack) {
        // 检查是否为无限储罐
        if (isInfinite(stack)) {
            return Long.MAX_VALUE;
        }
        
        // 检查是否有自定义容�?        long customCapacity = getCustomCapacity(stack);
        if (customCapacity > 0) {
            return customCapacity;
        }
        
        // 使用默认容量计算（原系统�?        return calculateDefaultCapacity(stack);
    }
    
    /**
     * 计算默认容量（原系统�?     */
    private static long calculateDefaultCapacity(ItemStack stack) {
        int level = 1;
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("TankLevel")) {
            level = stack.getTagCompound().getInteger("TankLevel");
        }
        
        long capacity = 1000; // 初始容量
        for (int i = 1; i < level; i++) {
            capacity *= 2;
        }
        return capacity;
    }
    
    // ==================== 合成表示�?====================
    /*
     * 合成表配置示例（可在Mod类中注册）：
     * 
     * // 100级储罐合成表
     * GameRegistry.addRecipe(
     *     createLevel100Tank(),
     *     "GGG",
     *     "GTG",
     *     "GGG",
     *     'G', new ItemStack(Items.EMERALD),
     *     'T', new ItemStack(/* 基础储罐 */)
     * );
     * 
     * // 500级储罐合成表
     * GameRegistry.addRecipe(
     *     createLevel500Tank(),
     *     "DDD",
     *     "DTD",
     *     "DDD",
     *     'D', new ItemStack(Items.DIAMOND),
     *     'T', createLevel100Tank()
     * );
     * 
     * // 1000级储罐合成表
     * GameRegistry.addRecipe(
     *     createLevel1000Tank(),
     *     "NNN",
     *     "NTN",
     *     "NNN",
     *     'N', new ItemStack(Items.NETHER_STAR),
     *     'T', createLevel500Tank()
     * );
     * 
     * // 2000级储罐合成表
     * GameRegistry.addRecipe(
     *     createLevel2000Tank(),
     *     "EEE",
     *     "ETE",
     *     "EEE",
     *     'E', new ItemStack(Items.DRAGON_EGG),
     *     'T', createLevel1000Tank()
     * );
     * 
     * // 无限储罐合成表（示例�?     * GameRegistry.addRecipe(
     *     createInfiniteTank(),
     *     "OOO",
     *     "OTO",
     *     "OOO",
     *     'O', new ItemStack(/* 模组特有物品 */),
     *     'T', createLevel2000Tank()
     * );
     */
    
    // ==================== 材质配置示例 ====================
    /*
     * 材质文件结构示例�?     * 
     * assets/rsring/textures/items/
     * ├── tank_basic.png          # 基础储罐
     * ├── tank_level100.png       # 100级储罐（绿色装饰�?     * ├── tank_level500.png       # 500级储罐（蓝色装饰�?     * ├── tank_level1000.png      # 1000级储罐（紫色装饰�?     * ├── tank_level2000.png      # 2000级储罐（金色装饰�?     * └── tank_infinite.png       # 无限储罐（彩虹装饰）
     * 
     * 模型文件配置示例�?     * assets/rsring/models/item/
     * ├── tank_basic.json
     * ├── tank_level100.json
     * ├── tank_level500.json
     * ├── tank_level1000.json
     * ├── tank_level2000.json
     * └── tank_infinite.json
     */
    
    // ==================== 物品介绍配置 ====================
    /*
     * 物品介绍示例（可在语言文件中配置）�?     * 
     * item.rsring:tank_level100.name=经验储罐 - 100�?     * item.rsring:tank_level100.desc=存储容量�?73,320 XP
     * item.rsring:tank_level100.desc2=可存储最�?00级经�?     * 
     * item.rsring:tank_level500.name=经验储罐 - 500�?     * item.rsring:tank_level500.desc=存储容量�?67,754,120 XP
     * item.rsring:tank_level500.desc2=可存储最�?00级经�?     * 
     * item.rsring:tank_level1000.name=经验储罐 - 1000�?     * item.rsring:tank_level1000.desc=存储容量�?,418,780,120 XP
     * item.rsring:tank_level1000.desc2=可存储最�?000级经�?     * 
     * item.rsring:tank_level2000.name=经验储罐 - 2000�?     * item.rsring:tank_level2000.desc=存储容量�?1,670,582,120 XP
     * item.rsring:tank_level2000.desc2=可存储最�?000级经�?     * 
     * item.rsring:tank_infinite.name=经验储罐 - 无限
     * item.rsring:tank_infinite.desc=存储容量：无�?     * item.rsring:tank_infinite.desc2=可存储无限经�?     */
}


