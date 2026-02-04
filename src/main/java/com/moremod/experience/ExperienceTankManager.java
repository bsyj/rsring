package com.moremod.experience;

import com.moremod.capability.ExperiencePumpCapability;
import com.moremod.capability.IExperiencePumpCapability;
import com.moremod.item.ItemExperiencePump;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 管理经验储罐操作，包括升级、容量验证和溢出处理。
 * 与合成事件系统集成，在储罐升级期间保留经验。
 * 
 * 支持需求 1.1、1.3、1.4 中的经验储罐升级保留和容量管理。
 */
public class ExperienceTankManager {
    
    private static final Logger LOGGER = LogManager.getLogger(ExperienceTankManager.class);
    
    // Singleton instance
    private static ExperienceTankManager instance;
    
    // Constants for tank tiers and capacities
    public static final int BASE_CAPACITY = 1000; // 10 levels * 100 XP per level
    public static final int CAPACITY_PER_TIER = 1000; // Additional capacity per tier
    public static final int MAX_TIER = 10; // Maximum tank tier
    
    /**
     * 单例模式的私有构造函数。
     */
    private ExperienceTankManager() {
        // 不需要注册事件，因为合成由 CraftingUpgradeHandler 处理
    }
    
    /**
     * 获取单例实例。
     */
    public static ExperienceTankManager getInstance() {
        if (instance == null) {
            instance = new ExperienceTankManager();
        }
        return instance;
    }
    
    /**
     * 初始化经验储罐管理器。
     * 应在模组初始化期间调用。
     */
    public static void initialize() {
        getInstance(); // 确保实例被创建和注册
        LOGGER.info("经验储罐管理器初始化完成");
    }
    
    /**
     * 在储罐从旧到新升级时保留经验。
     * 这是需求 1.1 和 1.3 的核心方法。
     * 
     * @param oldTank 正在升级的原始储罐
     * @param newTank 升级后的新储罐
     * @return 保留了经验的新储罐
     */
    public ItemStack preserveExperienceOnUpgrade(ItemStack oldTank, ItemStack newTank) {
        if (oldTank.isEmpty() || newTank.isEmpty()) {
            LOGGER.warn("Cannot preserve experience: old or new tank is empty");
            return newTank;
        }
        
        if (!(oldTank.getItem() instanceof ItemExperiencePump) || 
            !(newTank.getItem() instanceof ItemExperiencePump)) {
            LOGGER.warn("Cannot preserve experience: items are not experience tanks");
            return newTank;
        }
        
        // Get stored experience from old tank
        int storedExperience = getStoredExperience(oldTank);
        if (storedExperience <= 0) {
            LOGGER.debug("No experience to preserve in old tank");
            return newTank;
        }
        
        // Get the new tank's capacity
        int newCapacity = getTankCapacity(newTank);
        
        // Validate and cap the stored experience if necessary (Requirement 1.4)
        int preservedExperience = validateCapacity(storedExperience, newCapacity);
        
        if (preservedExperience < storedExperience) {
            LOGGER.info("Experience capped during upgrade: {} -> {} (capacity: {})", 
                       storedExperience, preservedExperience, newCapacity);
        }
        
        // Set the preserved experience in the new tank
        setStoredExperience(newTank, preservedExperience);
        
        // Preserve other tank properties
        preserveTankProperties(oldTank, newTank);
        
        LOGGER.debug("Experience preserved during upgrade: {} XP", preservedExperience);
        return newTank;
    }
    
    /**
     * 验证存储的经验值不超过储罐容量。
     * 实现需求 1.4 的容量溢出处理。
     * 
     * @param storedXP 要验证的经验值数量
     * @param maxCapacity 储罐的最大容量
     * @return 验证后的经验值数量（限制在容量范围内）
     */
    public int validateCapacity(int storedXP, int maxCapacity) {
        if (storedXP < 0) {
            LOGGER.warn("Invalid stored XP amount: {}, resetting to 0", storedXP);
            return 0;
        }
        
        if (maxCapacity <= 0) {
            LOGGER.warn("Invalid tank capacity: {}, using base capacity", maxCapacity);
            maxCapacity = BASE_CAPACITY;
        }
        
        if (storedXP > maxCapacity) {
            LOGGER.debug("Stored XP {} exceeds capacity {}, capping", storedXP, maxCapacity);
            return maxCapacity;
        }
        
        return storedXP;
    }
    
    /**
     * 从储罐物品栈获取存储的经验值。
     * 
     * @param tank 储罐物品栈
     * @return 存储的经验值数量
     */
    public int getStoredExperience(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return 0;
        }
        
        // Try to get from capability first
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            return capability.getXpStored();
        }
        
        // Fallback to NBT data
        if (tank.hasTagCompound() && tank.getTagCompound().hasKey(ItemExperiencePump.XP_TAG)) {
            NBTTagCompound data = tank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
            return data.getInteger("xp");
        }
        
        return 0;
    }
    
    /**
     * 在储罐物品栈中设置存储的经验值。
     * 
     * @param tank 储罐物品栈
     * @param experience 要存储的经验值数量
     */
    public void setStoredExperience(ItemStack tank, int experience) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            LOGGER.warn("Cannot set experience: invalid tank");
            return;
        }
        
        // Validate the experience amount
        int capacity = getTankCapacity(tank);
        int validatedExperience = validateCapacity(experience, capacity);
        
        // Set via capability if available
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            capability.setXpStored(validatedExperience);
            ItemExperiencePump.syncCapabilityToStack(tank, capability);
            return;
        }
        
        // Fallback to NBT data
        if (!tank.hasTagCompound()) {
            tank.setTagCompound(new NBTTagCompound());
        }
        
        NBTTagCompound data = tank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
        if (data.isEmpty()) {
            data = new NBTTagCompound();
            tank.getTagCompound().setTag(ItemExperiencePump.XP_TAG, data);
        }
        
        data.setInteger("xp", validatedExperience);
    }
    
    /**
     * 根据储罐等级获取其容量。
     * 
     * @param tank 储罐物品栈
     * @return 储罐的容量
     */
    public int getTankCapacity(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return BASE_CAPACITY;
        }
        
        // Try to get from capability first
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            return capability.getMaxXp();
        }
        
        // Fallback to calculating from tier
        int tier = getTankTier(tank);
        return calculateCapacityForTier(tier);
    }
    
    /**
     * 获取储罐的等级。
     * 
     * @param tank 储罐物品栈
     * @return 储罐的等级
     */
    public int getTankTier(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return 1;
        }
        
        // Try to get from capability first
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            // Calculate tier from capacity
            int capacity = capability.getMaxXp();
            return calculateTierFromCapacity(capacity);
        }
        
        // Fallback to NBT data
        if (tank.hasTagCompound() && tank.getTagCompound().hasKey(ItemExperiencePump.XP_TAG)) {
            NBTTagCompound data = tank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
            if (data.hasKey("tier")) {
                return Math.max(1, Math.min(data.getInteger("tier"), MAX_TIER));
            }
        }
        
        return 1; // Default tier
    }
    
    /**
     * 计算给定等级的容量。
     * 
     * @param tier 储罐等级
     * @return 该等级的容量
     */
    public int calculateCapacityForTier(int tier) {
        tier = Math.max(1, Math.min(tier, MAX_TIER));
        return BASE_CAPACITY + (tier - 1) * CAPACITY_PER_TIER;
    }
    
    /**
     * 从给定容量计算等级。
     * 
     * @param capacity 储罐容量
     * @return 该容量对应的等级
     */
    public int calculateTierFromCapacity(int capacity) {
        if (capacity <= BASE_CAPACITY) {
            return 1;
        }
        
        int tier = 1 + (capacity - BASE_CAPACITY) / CAPACITY_PER_TIER;
        return Math.max(1, Math.min(tier, MAX_TIER));
    }
    
    /**
     * 在升级期间保留储罐属性（模式、保留等级、经验修补）。
     * 
     * @param oldTank 原始储罐
     * @param newTank 新储罐
     */
    private void preserveTankProperties(ItemStack oldTank, ItemStack newTank) {
        IExperiencePumpCapability oldCap = oldTank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        IExperiencePumpCapability newCap = newTank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        
        if (oldCap != null && newCap != null) {
            // Preserve capability properties
            newCap.setMode(oldCap.getMode());
            newCap.setRetainLevel(oldCap.getRetainLevel());
            newCap.setUseForMending(oldCap.isUseForMending());
            
            // Sync to stack
            ItemExperiencePump.syncCapabilityToStack(newTank, newCap);
        } else {
            // Fallback to NBT preservation
            preserveNBTProperties(oldTank, newTank);
        }
    }
    
    /**
     * 当能力不可用时保留 NBT 属性。
     * 
     * @param oldTank 原始储罐
     * @param newTank 新储罐
     */
    private void preserveNBTProperties(ItemStack oldTank, ItemStack newTank) {
        if (!oldTank.hasTagCompound() || !oldTank.getTagCompound().hasKey(ItemExperiencePump.XP_TAG)) {
            return;
        }
        
        NBTTagCompound oldData = oldTank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
        
        if (!newTank.hasTagCompound()) {
            newTank.setTagCompound(new NBTTagCompound());
        }
        
        NBTTagCompound newData = newTank.getTagCompound().getCompoundTag(ItemExperiencePump.XP_TAG);
        if (newData.isEmpty()) {
            newData = new NBTTagCompound();
            newTank.getTagCompound().setTag(ItemExperiencePump.XP_TAG, newData);
        }
        
        // Preserve properties
        if (oldData.hasKey("mode")) {
            newData.setInteger("mode", oldData.getInteger("mode"));
        }
        if (oldData.hasKey("retainLevel")) {
            newData.setInteger("retainLevel", oldData.getInteger("retainLevel"));
        }
        if (oldData.hasKey("mending")) {
            newData.setBoolean("mending", oldData.getBoolean("mending"));
        }
    }
    
    /**
     * 处理向储罐添加经验时的溢出。
     * 返回无法添加的数量。
     * 
     * @param tank 要添加经验的储罐
     * @param amount 要添加的经验值数量
     * @return 无法添加的数量（溢出）
     */
    public int handleExperienceOverflow(ItemStack tank, int amount) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump) || amount <= 0) {
            return amount;
        }
        
        int currentStored = getStoredExperience(tank);
        int capacity = getTankCapacity(tank);
        int availableSpace = capacity - currentStored;
        
        if (availableSpace <= 0) {
            return amount; // Tank is full, all is overflow
        }
        
        if (amount <= availableSpace) {
            // All can be added
            setStoredExperience(tank, currentStored + amount);
            return 0;
        } else {
            // Partial addition, return overflow
            setStoredExperience(tank, capacity);
            return amount - availableSpace;
        }
    }
    
    /**
     * 查找玩家物品栏中的所有经验储罐。
     * 用于跨所有物品栏类型的综合储罐管理。
     * 
     * @param player 要扫描的玩家
     * @return 找到的所有经验储罐列表
     */
    public List<ItemStack> findAllTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        
        if (player == null) {
            return tanks;
        }
        
        // Scan player inventory
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                tanks.add(stack);
            }
        }
        
        // Scan off-hand
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && offHand.getItem() instanceof ItemExperiencePump) {
            tanks.add(offHand);
        }
        
        // Scan Baubles if available
        tanks.addAll(findBaublesTanks(player));
        
        return tanks;
    }
    
    /**
     * 在 Baubles 槽位中查找经验储罐。
     * 
     * @param player 要扫描的玩家
     * @return 在 Baubles 槽位中找到的储罐列表
     */
    private List<ItemStack> findBaublesTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        
        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                                   .invoke(null, player);
            
            if (handler instanceof net.minecraft.inventory.IInventory) {
                net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                for (int i = 0; i < baubles.getSizeInventory(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                        tanks.add(stack);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Baubles not available or error accessing: {}", e.getMessage());
        }
        
        return tanks;
    }
    
    /**
     * 计算玩家所有储罐的总容量。
     * 
     * @param player 要计算的玩家
     * @return 所有储罐的总容量
     */
    public int calculateTotalCapacity(EntityPlayer player) {
        List<ItemStack> tanks = findAllTanks(player);
        return tanks.stream()
                   .mapToInt(this::getTankCapacity)
                   .sum();
    }
    
    /**
     * 计算玩家所有储罐中存储的总经验值。
     * 
     * @param player 要计算的玩家
     * @return 存储的总经验值
     */
    public int calculateTotalStored(EntityPlayer player) {
        List<ItemStack> tanks = findAllTanks(player);
        return tanks.stream()
                   .mapToInt(this::getStoredExperience)
                   .sum();
    }
    
    
    /**
     * 从物品栈创建新的 ExperienceTankData。
     * 
     * @param tank 储罐物品栈
     * @return 表示储罐的 ExperienceTankData
     */
    public ExperienceTankData createTankData(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return new ExperienceTankData();
        }
        
        int stored = getStoredExperience(tank);
        int capacity = getTankCapacity(tank);
        int tier = getTankTier(tank);
        
        return new ExperienceTankData(stored, capacity, tier);
    }
    
    /**
     * 将 ExperienceTankData 应用到物品栈。
     * 
     * @param tank 要修改的储罐物品栈
     * @param data 要应用的数据
     */
    public void applyTankData(ItemStack tank, ExperienceTankData data) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump) || data == null) {
            return;
        }
        
        setStoredExperience(tank, data.getStoredExperience());
        
        // Update capacity if needed via capability
        IExperiencePumpCapability capability = tank.getCapability(
            ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (capability != null) {
            // Calculate the difference in capacity levels
            int currentCapacity = capability.getMaxXp();
            int targetCapacity = data.getMaxCapacity();
            
            if (targetCapacity > currentCapacity) {
                int levelsToAdd = (targetCapacity - currentCapacity) / CAPACITY_PER_TIER;
                capability.addCapacityLevels(levelsToAdd);
            }
            
            ItemExperiencePump.syncCapabilityToStack(tank, capability);
        }
    }
}