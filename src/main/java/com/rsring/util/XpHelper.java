package com.rsring.util;

import net.minecraft.entity.player.EntityPlayer;

/**
 * 经验计算辅助类
 * 参考 SophisticatedBackpacks 的 XpHelper 实现，提供精确的经验计算方法
 * 
 * 核心功能：
 * 1. 经验点与液体的精确转换（1 XP = 20 mB）
 * 2. 等级与经验点的精确转换（使用 Minecraft 官方公式）
 * 3. 玩家总经验的精确计算
 * 
 * Minecraft 经验公式（1.12.2）：
 * - 0-15级：XP = level * (12 + level * 2) / 2
 * - 16-30级：XP = (level - 15) * (69 + (level - 15) * 5) / 2 + 315
 * - 31+级：XP = (level - 30) * (215 + (level - 30) * 9) / 2 + 1395
 */
public class XpHelper {
    
    private XpHelper() {}
    
    // 经验与液体的转换比例：1 经验点 = 20 mB 液体
    private static final int RATIO = 20;
    
    /**
     * 将液体量转换为经验点
     * @param liquid 液体量（mB）
     * @return 经验点数
     */
    public static float liquidToExperience(int liquid) {
        return (float) liquid / RATIO;
    }
    
    /**
     * 将经验点转换为液体量
     * @param xp 经验点数
     * @return 液体量（mB）
     */
    public static int experienceToLiquid(float xp) {
        return (int) (xp * RATIO);
    }
    
    /**
     * 计算达到指定等级所需的总经验点数
     * 使用 Minecraft 官方公式
     * 
     * @param level 目标等级
     * @return 达到该等级所需的总经验点数
     */
    public static int getExperienceForLevel(int level) {
        if (level == 0) {
            return 0;
        }
        if (level > 0 && level < 16) {
            // 0-15级：XP = level * (12 + level * 2) / 2
            return level * (12 + level * 2) / 2;
        } else if (level > 15 && level < 31) {
            // 16-30级：XP = (level - 15) * (69 + (level - 15) * 5) / 2 + 315
            return (level - 15) * (69 + (level - 15) * 5) / 2 + 315;
        } else {
            // 31+级：XP = (level - 30) * (215 + (level - 30) * 9) / 2 + 1395
            return (level - 30) * (215 + (level - 30) * 9) / 2 + 1395;
        }
    }
    
    /**
     * 计算在指定等级下，升到下一级所需的经验点数
     * 
     * @param level 当前等级
     * @return 升到下一级所需的经验点数
     */
    public static int getExperienceLimitOnLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else {
            if (level >= 15) {
                return 37 + (level - 15) * 5;
            }
            return 7 + level * 2;
        }
    }
    
    /**
     * 根据总经验点数计算对应的等级（向下取整）
     * 
     * @param experience 总经验点数
     * @return 对应的等级
     */
    public static int getLevelForExperience(int experience) {
        int i = 0;
        while (getExperienceForLevel(i) <= experience) {
            i++;
        }
        return i - 1;
    }
    
    /**
     * 根据总经验点数计算精确的等级（包含小数部分）
     * 
     * @param experience 总经验点数
     * @return 精确的等级（包含小数）
     */
    public static double getLevelsForExperience(int experience) {
        int baseLevel = getLevelForExperience(experience);
        int remainingExperience = experience - getExperienceForLevel(baseLevel);
        return baseLevel + ((double) remainingExperience / getExperienceLimitOnLevel(baseLevel));
    }
    
    /**
     * 获取玩家的总经验点数
     * 包括当前等级的经验 + 当前等级进度的经验
     * 
     * @param player 玩家实例
     * @return 玩家的总经验点数
     */
    public static int getPlayerTotalExperience(EntityPlayer player) {
        // 当前等级对应的总经验
        int currentLevelPoints = getExperienceForLevel(player.experienceLevel);
        // 当前等级进度对应的经验
        int partialLevelPoints = (int) (player.experience * player.xpBarCap());
        return currentLevelPoints + partialLevelPoints;
    }
    
    /**
     * 设置玩家的总经验点数
     * 参考 XP-Tome 的实现，直接计算并设置对应的等级和进度
     * 
     * @param player 玩家实例
     * @param experience 要设置的总经验点数
     */
    public static void setPlayerTotalExperience(EntityPlayer player, int experience) {
        experience = Math.max(0, experience);
        player.experienceTotal = experience;
        player.experienceLevel = getLevelForExperience(experience);
        int expForLevel = getExperienceForLevel(player.experienceLevel);
        player.experience = (float)(experience - expForLevel) / (float)player.xpBarCap();
    }
    
    /**
     * 从玩家身上移除指定数量的经验点
     * 
     * @param player 玩家实例
     * @param amount 要移除的经验点数
     * @return 实际移除的经验点数
     */
    public static int removeExperienceFromPlayer(EntityPlayer player, int amount) {
        if (amount <= 0) {
            return 0;
        }
        
        int currentXP = getPlayerTotalExperience(player);
        int actualRemove = Math.min(amount, currentXP);
        
        if (actualRemove > 0) {
            int newXP = currentXP - actualRemove;
            setPlayerTotalExperience(player, newXP);
        }
        
        return actualRemove;
    }
    
    /**
     * 向玩家添加指定数量的经验点
     * 参考 XP-Tome 的实现，直接计算并设置玩家的总经验
     * 
     * @param player 玩家实例
     * @param amount 要添加的经验点数
     */
    public static void addExperienceToPlayer(EntityPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        
        int experience = getPlayerTotalExperience(player) + amount;
        player.experienceTotal = experience;
        player.experienceLevel = getLevelForExperience(experience);
        int expForLevel = getExperienceForLevel(player.experienceLevel);
        player.experience = (float)(experience - expForLevel) / (float)player.xpBarCap();
    }
    
    /**
     * 格式化经验显示
     * 显示格式：XP点数 (等级)
     * 与Minecraft客户端保持一致，只显示整数等级
     * 
     * @param xp 经验点数
     * @return 格式化的字符串
     */
    public static String formatExperience(int xp) {
        if (xp <= 0) {
            return "0 XP (0 levels)";
        }
        
        int levels = getLevelForExperience(xp);
        return String.format("%d XP (%d levels)", xp, levels);
    }
    
    /**
     * 计算从当前等级到目标等级所需的经验点数
     * 
     * @param currentLevel 当前等级
     * @param targetLevel 目标等级
     * @return 所需的经验点数
     */
    public static int getExperienceBetweenLevels(int currentLevel, int targetLevel) {
        if (currentLevel == targetLevel) {
            return 0;
        }
        
        int currentTotal = getExperienceForLevel(currentLevel);
        int targetTotal = getExperienceForLevel(targetLevel);
        return Math.abs(targetTotal - currentTotal);
    }
    
    /**
     * 从玩家身上提取指定等级数的经验
     * 
     * @param player 玩家实例
     * @param levelsToExtract 要提取的等级数
     * @return 实际提取的经验点数
     */
    public static int extractExperienceLevels(EntityPlayer player, int levelsToExtract) {
        if (levelsToExtract <= 0) {
            return 0;
        }
        
        int currentLevel = player.experienceLevel;
        int targetLevel = Math.max(0, currentLevel - levelsToExtract);
        
        int currentTotal = getPlayerTotalExperience(player);
        int targetTotal = getExperienceForLevel(targetLevel);
        int toExtract = Math.max(0, currentTotal - targetTotal);
        
        if (toExtract > 0) {
            int actualRemoved = removeExperienceFromPlayer(player, toExtract);
            return actualRemoved;
        }
        
        return 0;
    }
    
    /**
     * 向玩家添加指定等级数的经验
     * 
     * @param player 玩家实例
     * @param levelsToAdd 要添加的等级数
     * @return 实际添加的经验点数
     */
    public static int addExperienceLevels(EntityPlayer player, int levelsToAdd) {
        if (levelsToAdd <= 0) {
            return 0;
        }
        
        int currentLevel = player.experienceLevel;
        int targetLevel = currentLevel + levelsToAdd;
        
        int currentTotal = getPlayerTotalExperience(player);
        int targetTotal = getExperienceForLevel(targetLevel);
        int toAdd = Math.max(0, targetTotal - currentTotal);
        
        if (toAdd > 0) {
            addExperienceToPlayer(player, toAdd);
            return toAdd;
        }
        
        return 0;
    }
}
