package com.moremod.experience;

import com.moremod.item.ItemExperiencePump;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Central controller for experience extraction, injection, and tank management.
 * 参考精妙背包设计：统一的经验管理控制器，提供配置驱动的经验操作
 * 
 * 核心功能：
 * 1. 统一的储罐扫描和管理
 * 2. 经验抽取/注入操作
 * 3. 与储罐的协调工作（避免冲突）
 * 4. 配置驱动的行为控制
 * 
 * Implements Requirements 3.1, 3.2, 3.3, 3.4 for comprehensive experience pump controller functionality.
 */
public class ExperiencePumpController {
    
    private static final Logger LOGGER = LogManager.getLogger(ExperiencePumpController.class);
    
    // Singleton instance
    private static ExperiencePumpController instance;
    
    // Integration with inventory layer
    private final InventoryIntegrationLayer inventoryLayer;
    
    // 经验计算现在委托给 XpHelper 工具类
    // XpHelper 使用 Minecraft 官方公式，确保精确计算
    
    /**
     * Private constructor for singleton pattern.
     */
    private ExperiencePumpController() {
        this.inventoryLayer = InventoryIntegrationLayer.getInstance();
        LOGGER.debug("ExperiencePumpController initialized");
    }
    
    /**
     * Gets the singleton instance.
     */
    public static ExperiencePumpController getInstance() {
        if (instance == null) {
            instance = new ExperiencePumpController();
        }
        return instance;
    }
    
    /**
     * Initializes the experience pump controller.
     * Should be called during mod initialization.
     */
    public static void initialize() {
        getInstance(); // Ensure instance is created
        LOGGER.info("Experience pump controller initialized");
    }
    
    /**
     * Scans all inventory types for experience tanks.
     * Implements Requirements 3.1, 3.2, 3.3 for comprehensive tank detection.
     * 
     * @param player The player to scan
     * @return TankScanResult containing all detected tanks with location information
     */
    public TankScanResult scanAllInventories(EntityPlayer player) {
        if (player == null) {
            LOGGER.debug("Player is null, returning empty scan result");
            return TankScanResult.empty();
        }
        
        LOGGER.debug("Scanning all inventories for experience tanks: {}", player.getName());
        
        // Delegate to inventory integration layer for comprehensive scanning
        TankScanResult result = inventoryLayer.scanAllInventories(player);
        
        LOGGER.debug("Tank scan complete for player {}: {} tanks found with total capacity {}",
                    player.getName(), result.getTankCount(), result.getTotalCapacity());
        
        return result;
    }
    
    /**
     * Calculates the total capacity of all detected tanks.
     * Implements Requirement 3.4 for total capacity calculation and display.
     * 
     * @param player The player to calculate total capacity for
     * @return The total capacity of all tanks in XP points
     */
    public int calculateTotalCapacity(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        
        TankScanResult scanResult = scanAllInventories(player);
        int totalCapacity = scanResult.getTotalCapacity();
        
        LOGGER.debug("Total capacity calculated for player {}: {} XP", player.getName(), totalCapacity);
        
        return totalCapacity;
    }
    
    /**
     * Calculates the total stored experience across all tanks.
     * 
     * @param player The player to calculate total stored XP for
     * @return The total stored XP across all tanks
     */
    public int calculateTotalStored(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        
        TankScanResult scanResult = scanAllInventories(player);
        int totalStored = scanResult.getTotalStored();
        
        LOGGER.debug("Total stored XP calculated for player {}: {} XP", player.getName(), totalStored);
        
        return totalStored;
    }
    
    /**
     * Gets the total remaining capacity across all tanks.
     * 
     * @param player The player to calculate remaining capacity for
     * @return The total remaining capacity in XP points
     */
    public int calculateTotalRemainingCapacity(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        
        int totalCapacity = calculateTotalCapacity(player);
        int totalStored = calculateTotalStored(player);
        
        return Math.max(0, totalCapacity - totalStored);
    }
    
    /**
     * Processes scroll wheel input for fine-tuning controls.
     * Implements Requirements 3.6, 3.7 for scroll wheel fine-tuned adjustment controls.
     * 
     * @param scrollDelta The scroll wheel delta (positive for up, negative for down)
     * @param isExtraction True if this is for extraction operations, false for injection
     * @param baseAmount The base amount to adjust
     * @return The adjusted amount based on scroll input
     */
    public int processScrollInput(int scrollDelta, boolean isExtraction, int baseAmount) {
        if (scrollDelta == 0) {
            return baseAmount;
        }
        
        // Fine-tuning: scroll wheel adjusts by 10% of base amount, minimum 1 XP
        int adjustment = Math.max(1, baseAmount / 10);
        
        if (scrollDelta > 0) {
            // Scroll up increases the amount
            int newAmount = baseAmount + adjustment;
            LOGGER.debug("Scroll up: {} -> {} (adjustment: {})", baseAmount, newAmount, adjustment);
            return newAmount;
        } else {
            // Scroll down decreases the amount
            int newAmount = Math.max(1, baseAmount - adjustment);
            LOGGER.debug("Scroll down: {} -> {} (adjustment: {})", baseAmount, newAmount, adjustment);
            return newAmount;
        }
    }
    
    /**
     * Performs an experience operation (extraction or injection).
     * 
     * @param player The player to perform the operation on
     * @param amount The amount of XP to transfer
     * @param isExtraction True for extraction (tank to player), false for injection (player to tank)
     * @return The actual amount transferred
     */
    public int performExperienceOperation(EntityPlayer player, int amount, boolean isExtraction) {
        if (player == null || amount <= 0) {
            return 0;
        }
        
        LOGGER.debug("Performing {} operation for player {}: {} XP", 
                    isExtraction ? "extraction" : "injection", player.getName(), amount);
        
        if (isExtraction) {
            return extractExperienceFromTanks(player, amount);
        } else {
            return injectExperienceToTanks(player, amount);
        }
    }
    
    /**
     * Extracts experience from tanks to player.
     * Priority: Consolidate XP from multiple tanks, prioritizing tanks with more XP first
     *
     * @param player The player to give experience to
     * @param amount The amount of XP to extract
     * @return The actual amount extracted
     */
    private int extractExperienceFromTanks(EntityPlayer player, int amount) {
        TankScanResult scanResult = scanAllInventories(player);
        List<ItemStack> tanks = scanResult.getAllTanks();

        // Sort tanks by stored XP in descending order to consolidate extraction from fullest tanks first
        tanks.sort((tank1, tank2) -> {
            int stored1 = ItemExperiencePump.getXpStoredFromNBT(tank1);
            int stored2 = ItemExperiencePump.getXpStoredFromNBT(tank2);
            return Integer.compare(stored2, stored1); // Descending order
        });

        int totalExtracted = 0;
        int remainingToExtract = amount;

        // Extract from tanks in priority order until we have enough or run out of stored XP
        for (ItemStack tank : tanks) {
            if (remainingToExtract <= 0) {
                break;
            }

            int storedInTank = ItemExperiencePump.getXpStoredFromNBT(tank);
            if (storedInTank <= 0) {
                continue;
            }

            int extractFromThisTank = Math.min(remainingToExtract, storedInTank);

            // Remove XP from tank using capability
            com.moremod.capability.IExperiencePumpCapability tankCap = tank.getCapability(
                com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (tankCap != null) {
                tankCap.takeXp(extractFromThisTank);
                ItemExperiencePump.syncCapabilityToStack(tank, tankCap);
            }

            // Add XP to player
            addExperienceToPlayer(player, extractFromThisTank);

            totalExtracted += extractFromThisTank;
            remainingToExtract -= extractFromThisTank;

            LOGGER.debug("Extracted {} XP from tank, total extracted: {}", extractFromThisTank, totalExtracted);
        }

        LOGGER.debug("Extraction complete for player {}: {} XP extracted", player.getName(), totalExtracted);
        return totalExtracted;
    }
    
    /**
     * Injects experience from player to tanks.
     * Improved algorithm: Prioritizes filling tanks to maximize efficiency
     *
     * @param player The player to take experience from
     * @param amount The amount of XP to inject
     * @return The actual amount injected
     */
    private int injectExperienceToTanks(EntityPlayer player, int amount) {
        // Check if player has enough XP
        int playerXP = getPlayerTotalExperience(player);
        if (playerXP < amount) {
            amount = playerXP;
        }

        if (amount <= 0) {
            return 0;
        }

        TankScanResult scanResult = scanAllInventories(player);
        List<ItemStack> tanks = scanResult.getAllTanks();

        // Sort tanks by available space in ascending order to fill nearly-full tanks first
        // This maximizes the efficiency of storage utilization
        tanks.sort((tank1, tank2) -> {
            int available1 = ItemExperiencePump.getMaxXpFromNBT(tank1) - ItemExperiencePump.getXpStoredFromNBT(tank1);
            int available2 = ItemExperiencePump.getMaxXpFromNBT(tank2) - ItemExperiencePump.getXpStoredFromNBT(tank2);
            return Integer.compare(available1, available2); // Ascending order (smaller available space first)
        });

        int totalInjected = 0;
        int remainingToInject = amount;

        // Inject into tanks in priority order until we fill them or run out of XP
        for (ItemStack tank : tanks) {
            if (remainingToInject <= 0) {
                break;
            }

            int storedInTank = ItemExperiencePump.getXpStoredFromNBT(tank);
            int tankCapacity = ItemExperiencePump.getMaxXpFromNBT(tank);
            int availableSpace = tankCapacity - storedInTank;

            if (availableSpace <= 0) {
                continue;
            }

            int injectIntoThisTank = Math.min(remainingToInject, availableSpace);

            // Add XP to tank using capability
            com.moremod.capability.IExperiencePumpCapability tankCap = tank.getCapability(
                com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (tankCap != null) {
                tankCap.addXp(injectIntoThisTank);
                ItemExperiencePump.syncCapabilityToStack(tank, tankCap);
            }

            totalInjected += injectIntoThisTank;
            remainingToInject -= injectIntoThisTank;

            LOGGER.debug("Injected {} XP into tank, total injected: {}", injectIntoThisTank, totalInjected);
        }

        // Remove XP from player
        if (totalInjected > 0) {
            removeExperienceFromPlayer(player, totalInjected);
        }

        LOGGER.debug("Injection complete for player {}: {} XP injected", player.getName(), totalInjected);
        return totalInjected;
    }
    
    /**
     * Converts XP points to equivalent level using Minecraft's official formulas.
     * Implements Requirements 6.1, 6.2 for accurate XP calculation.
     * 
     * 现在委托给 XpHelper 工具类，使用精确的 Minecraft 官方公式
     *
     * @param xp The XP amount to convert
     * @return The equivalent level (can be fractional)
     */
    public double convertXPToLevel(int xp) {
        return com.moremod.util.XpHelper.getLevelsForExperience(xp);
    }
    
    /**
     * Converts level to XP points using Minecraft's official formulas.
     * Implements Requirements 6.1, 6.2 for accurate XP calculation.
     * 
     * 现在委托给 XpHelper 工具类，使用精确的 Minecraft 官方公式
     *
     * @param level The level to convert
     * @return The equivalent XP amount
     */
    public int convertLevelToXP(double level) {
        if (level <= 0) {
            return 0;
        }

        // Overflow protection: cap at reasonable maximum
        if (level > 21863) {
            LOGGER.warn("Level {} is extremely high, capping to prevent overflow", level);
            level = 21863; // This level corresponds to near Integer.MAX_VALUE XP
        }

        // 使用 XpHelper 的精确计算
        return com.moremod.util.XpHelper.getExperienceForLevel((int) Math.floor(level));
    }
    
    /**
     * Gets the player's total experience points.
     * 现在委托给 XpHelper 工具类，使用精确的计算方法
     * 
     * @param player The player
     * @return The player's total XP
     */
    public int getPlayerTotalExperience(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        
        return com.moremod.util.XpHelper.getPlayerTotalExperience(player);
    }
    
    /**
     * Gets the XP required to reach the next level from the current level.
     * 现在委托给 XpHelper 工具类
     *
     * @param currentLevel The current level
     * @return XP required for next level
     */
    public int getXPToNextLevel(int currentLevel) {
        return com.moremod.util.XpHelper.getExperienceLimitOnLevel(currentLevel);
    }
    
    /**
     * Adds experience to a player using Minecraft's experience system.
     * 现在委托给 XpHelper 工具类
     * 
     * @param player The player to add experience to
     * @param amount The amount of XP to add
     */
    private void addExperienceToPlayer(EntityPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        
        com.moremod.util.XpHelper.addExperienceToPlayer(player, amount);
        LOGGER.debug("Added {} XP to player {}", amount, player.getName());
    }
    
    /**
     * Removes experience from a player.
     * 现在委托给 XpHelper 工具类
     * 
     * @param player The player to remove experience from
     * @param amount The amount of XP to remove
     */
    private void removeExperienceFromPlayer(EntityPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        
        int actualRemoved = com.moremod.util.XpHelper.removeExperienceFromPlayer(player, amount);
        LOGGER.debug("Removed {} XP from player {}", actualRemoved, player.getName());
    }
    
    /**
     * Finds the first available tank with remaining capacity.
     * 
     * @param player The player to search
     * @return The first tank with available capacity, or ItemStack.EMPTY if none found
     */
    public ItemStack findFirstAvailableTank(EntityPlayer player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        
        return inventoryLayer.findFirstAvailableTank(player);
    }
    
    /**
     * Finds all tanks with available capacity.
     * 
     * @param player The player to search
     * @return List of tanks with available capacity
     */
    public List<ItemStack> findAllAvailableTanks(EntityPlayer player) {
        if (player == null) {
            return new ArrayList<>();
        }
        
        return inventoryLayer.findAllAvailableTanks(player);
    }
    
    /**
     * 检查储罐是否正在被控制器管理
     * 用于避免储罐自动泵送与控制器操作冲突
     * 
     * @param tank 要检查的储罐
     * @return true如果储罐正在被控制器管理
     */
    public boolean isTankManagedByController(ItemStack tank) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return false;
        }
        
        com.moremod.capability.IExperiencePumpCapability cap = tank.getCapability(
            com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        
        if (cap == null) {
            return false;
        }
        
        // 如果储罐模式不是关闭，且控制器配置启用，则认为正在被管理
        return cap.getMode() != com.moremod.capability.IExperiencePumpCapability.MODE_OFF && 
               com.moremod.config.ExperienceTankConfig.enabled;
    }
    
    /**
     * 设置储罐的控制器管理状态
     * 当控制器开始/停止管理储罐时调用
     * 
     * @param tank 储罐物品
     * @param managed true表示开始管理，false表示停止管理
     */
    public void setTankManagedState(ItemStack tank, boolean managed) {
        if (tank.isEmpty() || !(tank.getItem() instanceof ItemExperiencePump)) {
            return;
        }
        
        com.moremod.capability.IExperiencePumpCapability cap = tank.getCapability(
            com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        
        if (cap != null) {
            // 通过设置模式来控制储罐的自动行为
            if (managed) {
                // 控制器管理时，储罐停止自动泵送（但保持其他功能如抽取经验）
                if (cap.getMode() == com.moremod.capability.IExperiencePumpCapability.MODE_OFF) {
                    cap.setMode(com.moremod.capability.IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER);
                }
            }
            // 注意：不在这里设置为OFF，因为储罐可能有其他用途
            
            ItemExperiencePump.syncCapabilityToStack(tank, cap);
        }
    }
    
    /**
     * 批量管理多个储罐
     * 当控制器需要同时管理多个储罐时使用
     * 
     * @param tanks 储罐列表
     * @param managed 管理状态
     */
    public void setMultipleTanksManagedState(List<ItemStack> tanks, boolean managed) {
        if (tanks == null) {
            return;
        }
        
        for (ItemStack tank : tanks) {
            setTankManagedState(tank, managed);
        }
        
        LOGGER.debug("Set managed state for {} tanks: {}", tanks.size(), managed);
    }
    
    /**
     * Refreshes the inventory state for a player.
     * Should be called when inventory changes are detected.
     * 
     * @param player The player whose inventory state should be refreshed
     */
    public void refreshInventoryState(EntityPlayer player) {
        if (player == null) {
            return;
        }
        
        LOGGER.debug("Refreshing inventory state for player: {}", player.getName());
        inventoryLayer.refreshInventoryState(player);
    }
    
    /**
     * Gets diagnostic information about the controller state.
     * 
     * @param player The player to get diagnostics for
     * @return Diagnostic information map
     */
    public java.util.Map<String, Object> getDiagnostics(EntityPlayer player) {
        java.util.Map<String, Object> diagnostics = new java.util.LinkedHashMap<>();
        
        if (player == null) {
            diagnostics.put("error", "Player is null");
            return diagnostics;
        }
        
        diagnostics.put("playerName", player.getName());
        diagnostics.put("playerXP", getPlayerTotalExperience(player));
        diagnostics.put("playerLevel", player.experienceLevel);
        
        TankScanResult scanResult = scanAllInventories(player);
        diagnostics.put("totalTanks", scanResult.getTankCount());
        diagnostics.put("totalCapacity", scanResult.getTotalCapacity());
        diagnostics.put("totalStored", scanResult.getTotalStored());
        diagnostics.put("totalRemaining", scanResult.getTotalRemainingCapacity());
        diagnostics.put("overallFillPercentage", scanResult.getOverallFillPercentage());
        
        // Add inventory layer diagnostics
        diagnostics.putAll(inventoryLayer.getDiagnostics(player));
        
        return diagnostics;
    }
    
    /**
     * Formats experience amount for display showing both XP points and equivalent levels.
     * Implements Requirement 6.3 for experience display format.
     * 现在委托给 XpHelper 工具类
     * 
     * @param xp The XP amount to format
     * @return Formatted string showing XP and levels
     */
    public String formatExperienceDisplay(int xp) {
        return com.moremod.util.XpHelper.formatExperience(xp);
    }
    
    /**
     * Calculates XP amount for level-based extraction operations.
     * Implements Requirement 6.4 for level-based extraction calculation.
     *
     * @param player The player performing the extraction
     * @param targetLevel The level to extract down to
     * @return The amount of XP that can be extracted
     */
    public int calculateLevelBasedExtraction(EntityPlayer player, int targetLevel) {
        if (player == null || targetLevel < 0) {
            return 0;
        }

        int currentXP = getPlayerTotalExperience(player);
        int targetXP = convertLevelToXP(targetLevel);

        // Ensure targetXP doesn't exceed currentXP to avoid negative extraction
        if (currentXP <= targetXP) {
            return 0; // Player doesn't have enough XP to extract
        }

        int extractableXP = currentXP - targetXP;

        LOGGER.debug("Level-based extraction calculation for player {}: current {} XP, target level {}, extractable {} XP",
                    player.getName(), currentXP, targetLevel, extractableXP);

        return extractableXP;
    }
}