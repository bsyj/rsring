package com.rsring.experience;

import net.minecraft.item.ItemStack;
import java.util.*;

/**
 * 跨不同物品栏位置扫描经验储罐的结果。
 * 为经验泵控制器提供关于检测到的储罐的综合信息。
 * 
 * 支持需求 3.1、3.2、3.3、3.4 的全面储罐检测和容量计算。
 */
public class TankScanResult {
    
    /**
     * 可以找到储罐的物品栏类型枚举。
     */
    public enum InventoryType {
        PLAYER_INVENTORY("玩家物品栏"),
        HOTBAR("快捷栏"),
        BAUBLES("Baubles 槽位"),
        OFFHAND("副手");
        
        private final String displayName;
        
        InventoryType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final List<ItemStack> allTanks;                    // All detected tanks
    private final int totalCapacity;                           // Combined capacity
    private final int totalStored;                             // Combined stored XP
    private final Map<InventoryType, List<ItemStack>> tanksByLocation; // Tanks categorized by location
    private final long scanTimestamp;                          // When the scan was performed
    
    /**
     * 创建新的储罐扫描结果。
     */
    public TankScanResult(List<ItemStack> allTanks, 
                         Map<InventoryType, List<ItemStack>> tanksByLocation) {
        this.allTanks = new ArrayList<>(allTanks != null ? allTanks : Collections.emptyList());
        this.tanksByLocation = new EnumMap<>(InventoryType.class);
        
        // Copy the tanks by location map
        if (tanksByLocation != null) {
            for (Map.Entry<InventoryType, List<ItemStack>> entry : tanksByLocation.entrySet()) {
                this.tanksByLocation.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        
        // Calculate totals
        this.totalCapacity = calculateTotalCapacity();
        this.totalStored = calculateTotalStored();
        this.scanTimestamp = System.currentTimeMillis();
    }
    
    /**
     * 创建空的扫描结果。
     */
    public static TankScanResult empty() {
        return new TankScanResult(Collections.emptyList(), Collections.emptyMap());
    }
    
    // Getters
    
    public List<ItemStack> getAllTanks() {
        return new ArrayList<>(allTanks);
    }
    
    public int getTotalCapacity() {
        return totalCapacity;
    }
    
    public int getTotalStored() {
        return totalStored;
    }
    
    public Map<InventoryType, List<ItemStack>> getTanksByLocation() {
        Map<InventoryType, List<ItemStack>> result = new EnumMap<>(InventoryType.class);
        for (Map.Entry<InventoryType, List<ItemStack>> entry : tanksByLocation.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
    
    public long getScanTimestamp() {
        return scanTimestamp;
    }
    
    // Utility methods
    
    /**
     * 获取找到的储罐数量。
     */
    public int getTankCount() {
        return allTanks.size();
    }
    
    /**
     * 获取特定位置的储罐数量。
     */
    public int getTankCount(InventoryType location) {
        List<ItemStack> tanks = tanksByLocation.get(location);
        return tanks != null ? tanks.size() : 0;
    }
    
    /**
     * 从特定位置获取储罐。
     */
    public List<ItemStack> getTanksFromLocation(InventoryType location) {
        List<ItemStack> tanks = tanksByLocation.get(location);
        return tanks != null ? new ArrayList<>(tanks) : Collections.emptyList();
    }
    
    /**
     * 检查是否找到任何储罐。
     */
    public boolean hasTanks() {
        return !allTanks.isEmpty();
    }
    
    /**
     * 检查是否在特定位置找到储罐。
     */
    public boolean hasTanksInLocation(InventoryType location) {
        return getTankCount(location) > 0;
    }
    
    /**
     * 获取所有储罐的剩余总容量。
     */
    public int getTotalRemainingCapacity() {
        return totalCapacity - totalStored;
    }
    
    /**
     * 获取总体填充百分比（0.0 到 1.0）。
     */
    public double getOverallFillPercentage() {
        return totalCapacity > 0 ? (double) totalStored / totalCapacity : 0.0;
    }
    
    /**
     * 获取按位置分类的储罐摘要，用于显示目的。
     */
    public Map<String, Integer> getLocationSummary() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        
        for (InventoryType type : InventoryType.values()) {
            int count = getTankCount(type);
            if (count > 0) {
                summary.put(type.getDisplayName(), count);
            }
        }
        
        return summary;
    }
    
    // Private helper methods
    
    /**
     * 计算所有储罐的总容量。
     */
    private int calculateTotalCapacity() {
        int total = 0;
        
        for (ItemStack tank : allTanks) {
            if (tank.isEmpty()) continue;
            
            // Get capacity from the tank's capability or NBT data
            int capacity = getTankCapacity(tank);
            total += capacity;
        }
        
        return total;
    }
    
    /**
     * 计算所有储罐中存储的总经验值。
     */
    private int calculateTotalStored() {
        int total = 0;
        
        for (ItemStack tank : allTanks) {
            if (tank.isEmpty()) continue;
            
            // Get stored XP from the tank's capability or NBT data
            int stored = getTankStoredXP(tank);
            total += stored;
        }
        
        return total;
    }
    
    /**
     * 获取单个储罐的容量。
     * 使用现有的 ItemExperiencePump 方法以保持兼容性。
     */
    private int getTankCapacity(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof com.rsring.item.ItemExperiencePump)) {
            return 0;
        }
        
        return com.rsring.item.ItemExperiencePump.getMaxXpFromNBT(tank);
    }
    
    /**
     * 获取单个储罐存储的经验值。
     * 使用现有的 ItemExperiencePump 方法以保持兼容性。
     */
    private int getTankStoredXP(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof com.rsring.item.ItemExperiencePump)) {
            return 0;
        }
        
        return com.rsring.item.ItemExperiencePump.getXpStoredFromNBT(tank);
    }
    
    @Override
    public String toString() {
        return String.format("TankScanResult{tanks=%d, totalCapacity=%d, totalStored=%d, locations=%s, timestamp=%d}",
                           allTanks.size(), totalCapacity, totalStored, 
                           tanksByLocation.keySet(), scanTimestamp);
    }
    
    /**
     * 用于创建 TankScanResult 实例的构建器类。
     */
    public static class Builder {
        private final List<ItemStack> allTanks = new ArrayList<>();
        private final Map<InventoryType, List<ItemStack>> tanksByLocation = new EnumMap<>(InventoryType.class);
        
        public Builder addTank(ItemStack tank, InventoryType location) {
            if (tank != null && !tank.isEmpty()) {
                allTanks.add(tank);
                tanksByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(tank);
            }
            return this;
        }
        
        public Builder addTanks(List<ItemStack> tanks, InventoryType location) {
            if (tanks != null) {
                for (ItemStack tank : tanks) {
                    addTank(tank, location);
                }
            }
            return this;
        }
        
        public TankScanResult build() {
            return new TankScanResult(allTanks, tanksByLocation);
        }
    }
}