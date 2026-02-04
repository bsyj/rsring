package com.rsring.experience;

import net.minecraft.nbt.NBTTagCompound;
import java.util.UUID;

/**
 * 经验储罐管理的核心数据结构。
 * 处理经验储罐数据的存储、容量和序列化。
 * 
 * 此类为经验储罐升级保留和跨物品栏储罐管理提供基础，
 * 如需求 1.1、1.3、7.1 中所指定。
 */
public class ExperienceTankData {
    
    private int storedExperience;     // 当前存储的经验值
    private int maxCapacity;          // 最大经验容量  
    private int tankTier;             // 储罐升级等级
    private UUID tankId;              // 唯一标识符
    
    // 序列化用的 NBT 键
    private static final String NBT_STORED_XP = "storedExperience";
    private static final String NBT_MAX_CAPACITY = "maxCapacity";
    private static final String NBT_TANK_TIER = "tankTier";
    private static final String NBT_TANK_ID = "tankId";
    
    /**
     * 创建具有默认值的新经验储罐数据。
     */
    public ExperienceTankData() {
        this.storedExperience = 0;
        this.maxCapacity = 1000; // 默认 10 级 * 每级 100 经验值
        this.tankTier = 1;
        this.tankId = UUID.randomUUID();
    }
    
    /**
     * 创建具有指定值的新经验储罐数据。
     */
    public ExperienceTankData(int storedExperience, int maxCapacity, int tankTier) {
        this.storedExperience = Math.max(0, Math.min(storedExperience, maxCapacity));
        this.maxCapacity = Math.max(1, maxCapacity);
        this.tankTier = Math.max(1, tankTier);
        this.tankId = UUID.randomUUID();
    }
    
    /**
     * 从现有数据创建储罐数据的复制构造函数。
     */
    public ExperienceTankData(ExperienceTankData other) {
        this.storedExperience = other.storedExperience;
        this.maxCapacity = other.maxCapacity;
        this.tankTier = other.tankTier;
        this.tankId = other.tankId;
    }
    
    // Getters and setters
    
    public int getStoredExperience() {
        return storedExperience;
    }
    
    public void setStoredExperience(int storedExperience) {
        this.storedExperience = Math.max(0, Math.min(storedExperience, maxCapacity));
    }
    
    public int getMaxCapacity() {
        return maxCapacity;
    }
    
    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = Math.max(1, maxCapacity);
        // Ensure stored XP doesn't exceed new capacity
        this.storedExperience = Math.min(this.storedExperience, this.maxCapacity);
    }
    
    public int getTankTier() {
        return tankTier;
    }
    
    public void setTankTier(int tankTier) {
        this.tankTier = Math.max(1, tankTier);
    }
    
    public UUID getTankId() {
        return tankId;
    }
    
    public void setTankId(UUID tankId) {
        this.tankId = tankId != null ? tankId : UUID.randomUUID();
    }
    
    // Utility methods
    
    /**
     * 检查储罐是否已满。
     */
    public boolean isFull() {
        return storedExperience >= maxCapacity;
    }
    
    /**
     * 检查储罐是否为空。
     */
    public boolean isEmpty() {
        return storedExperience <= 0;
    }
    
    /**
     * 获取剩余容量。
     */
    public int getRemainingCapacity() {
        return maxCapacity - storedExperience;
    }
    
    /**
     * 获取填充百分比（0.0 到 1.0）。
     */
    public double getFillPercentage() {
        return maxCapacity > 0 ? (double) storedExperience / maxCapacity : 0.0;
    }
    
    /**
     * 向储罐添加经验，返回实际添加的数量。
     */
    public int addExperience(int amount) {
        if (amount <= 0) return 0;
        
        int canAdd = Math.min(amount, getRemainingCapacity());
        storedExperience += canAdd;
        return canAdd;
    }
    
    /**
     * 从储罐移除经验，返回实际移除的数量。
     */
    public int removeExperience(int amount) {
        if (amount <= 0) return 0;
        
        int canRemove = Math.min(amount, storedExperience);
        storedExperience -= canRemove;
        return canRemove;
    }
    
    // NBT Serialization methods
    
    /**
     * 将储罐数据写入 NBT 格式。
     * 支持需求 1.1 和 1.3 中的升级保留。
     */
    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        
        nbt.setInteger(NBT_STORED_XP, storedExperience);
        nbt.setInteger(NBT_MAX_CAPACITY, maxCapacity);
        nbt.setInteger(NBT_TANK_TIER, tankTier);
        
        if (tankId != null) {
            nbt.setString(NBT_TANK_ID, tankId.toString());
        }
        
        return nbt;
    }
    
    /**
     * 从 NBT 格式读取储罐数据。
     * 提供向后兼容性和验证。
     */
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt == null) return;
        
        // 读取存储的经验值并验证
        if (nbt.hasKey(NBT_STORED_XP)) {
            this.storedExperience = Math.max(0, nbt.getInteger(NBT_STORED_XP));
        }
        
        // 读取最大容量并验证
        if (nbt.hasKey(NBT_MAX_CAPACITY)) {
            this.maxCapacity = Math.max(1, nbt.getInteger(NBT_MAX_CAPACITY));
        }
        
        // 读取储罐等级并验证
        if (nbt.hasKey(NBT_TANK_TIER)) {
            this.tankTier = Math.max(1, nbt.getInteger(NBT_TANK_TIER));
        }
        
        // 读取储罐 ID 并验证
        if (nbt.hasKey(NBT_TANK_ID)) {
            try {
                this.tankId = UUID.fromString(nbt.getString(NBT_TANK_ID));
            } catch (IllegalArgumentException e) {
                // 无效的 UUID，生成新的
                this.tankId = UUID.randomUUID();
            }
        } else {
            this.tankId = UUID.randomUUID();
        }
        
        // 确保读取后存储的经验值不超过容量
        this.storedExperience = Math.min(this.storedExperience, this.maxCapacity);
    }
    
    /**
     * 从 NBT 创建新的 ExperienceTankData。
     */
    public static ExperienceTankData fromNBT(NBTTagCompound nbt) {
        ExperienceTankData data = new ExperienceTankData();
        data.readFromNBT(nbt);
        return data;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ExperienceTankData that = (ExperienceTankData) obj;
        return storedExperience == that.storedExperience &&
               maxCapacity == that.maxCapacity &&
               tankTier == that.tankTier &&
               (tankId != null ? tankId.equals(that.tankId) : that.tankId == null);
    }
    
    @Override
    public int hashCode() {
        int result = storedExperience;
        result = 31 * result + maxCapacity;
        result = 31 * result + tankTier;
        result = 31 * result + (tankId != null ? tankId.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("ExperienceTankData{stored=%d, capacity=%d, tier=%d, id=%s}", 
                           storedExperience, maxCapacity, tankTier, tankId);
    }
}