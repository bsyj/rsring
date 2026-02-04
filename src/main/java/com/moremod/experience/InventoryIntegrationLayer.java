package com.moremod.experience;

import com.moremod.item.ItemExperiencePump;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * 为经验储罐检测提供不同物品栏系统的统一访问。
 * 支持玩家物品栏、快捷栏和 Baubles 饰品槽位，具有全面的
 * 错误处理和当模组不可用时的优雅降级。
 * 
 * 实现需求 3.1、3.2、3.3、7.1 的全面物品栏集成。
 */
public class InventoryIntegrationLayer {
    
    private static final Logger LOGGER = LogManager.getLogger(InventoryIntegrationLayer.class);
    
    // Singleton instance
    private static InventoryIntegrationLayer instance;
    
    // Cache for Baubles availability check
    private Boolean baublesAvailable = null;
    
    /**
     * 单例模式的私有构造函数。
     */
    private InventoryIntegrationLayer() {
        LOGGER.debug("InventoryIntegrationLayer 初始化完成");
    }
    
    /**
     * 获取单例实例。
     */
    public static InventoryIntegrationLayer getInstance() {
        if (instance == null) {
            instance = new InventoryIntegrationLayer();
        }
        return instance;
    }
    
    /**
     * 初始化物品栏集成层。
     * 应在模组初始化期间调用。
     */
    public static void initialize() {
        getInstance(); // 确保实例被创建
        LOGGER.info("物品栏集成层初始化完成");
    }
    
    /**
     * 扫描玩家物品栏槽位以查找经验储罐。
     * 排除快捷栏槽位以避免重复计数。
     * 
     * 实现需求 3.1：玩家物品栏扫描
     * 
     * @param player 要扫描的玩家
     * @return 在玩家物品栏中找到的经验储罐列表（不包括快捷栏）
     */
    public List<ItemStack> getPlayerInventoryTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        
        if (player == null) {
            LOGGER.debug("Player is null, cannot scan player inventory");
            return tanks;
        }
        
        LOGGER.debug("Scanning player inventory for experience tanks: {}", player.getName());
        
        // Scan player inventory slots 9-35 (excluding hotbar slots 0-8)
        // Player inventory structure: 0-8 hotbar, 9-35 main inventory, 36-39 armor, 40 offhand
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isExperienceTank(stack)) {
                tanks.add(stack);
                LOGGER.debug("Found experience tank in player inventory slot {}: {}", i, stack.getDisplayName());
            }
        }
        
        LOGGER.debug("Found {} experience tanks in player inventory", tanks.size());
        return tanks;
    }
    
    /**
     * 扫描快捷栏槽位以查找经验储罐。
     * 
     * 实现需求 3.2：快捷栏扫描
     * 
     * @param player 要扫描的玩家
     * @return 在快捷栏槽位中找到的经验储罐列表
     */
    public List<ItemStack> getHotbarTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        
        if (player == null) {
            LOGGER.debug("Player is null, cannot scan hotbar");
            return tanks;
        }
        
        LOGGER.debug("Scanning hotbar for experience tanks: {}", player.getName());
        
        // Scan hotbar slots 0-8
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isExperienceTank(stack)) {
                tanks.add(stack);
                LOGGER.debug("Found experience tank in hotbar slot {}: {}", i, stack.getDisplayName());
            }
        }
        
        // Also check off-hand slot
        ItemStack offHand = player.getHeldItemOffhand();
        if (isExperienceTank(offHand)) {
            tanks.add(offHand);
            LOGGER.debug("Found experience tank in off-hand: {}", offHand.getDisplayName());
        }
        
        LOGGER.debug("Found {} experience tanks in hotbar", tanks.size());
        return tanks;
    }
    
    /**
     * 扫描 Baubles 饰品槽位以查找经验储罐。
     * 优雅处理 Baubles 模组不可用的情况。
     * 
     * 实现需求 3.3：Baubles API 集成饰品槽位
     * 
     * @param player 要扫描的玩家
     * @return 在 Baubles 槽位中找到的经验储罐列表
     */
    public List<ItemStack> getBaublesTanks(EntityPlayer player) {
        List<ItemStack> tanks = new ArrayList<>();
        
        if (player == null) {
            LOGGER.debug("Player is null, cannot scan Baubles");
            return tanks;
        }
        
        if (!isBaublesAvailable()) {
            LOGGER.debug("Baubles mod not available, skipping Baubles scan");
            return tanks;
        }
        
        LOGGER.debug("Scanning Baubles slots for experience tanks: {}", player.getName());
        
        try {
            // Use reflection to access baubles handler without assuming IInventory
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);
            if (handler != null) {
                // Try common methods: getSizeInventory / getStackInSlot
                java.lang.reflect.Method sizeMethod = null;
                java.lang.reflect.Method stackMethod = null;
                try {
                    sizeMethod = handler.getClass().getMethod("getSizeInventory");
                } catch (NoSuchMethodException ignored) {
                    try { sizeMethod = handler.getClass().getMethod("getSlots"); } catch (NoSuchMethodException ignored2) {}
                }
                try {
                    stackMethod = handler.getClass().getMethod("getStackInSlot", int.class);
                } catch (NoSuchMethodException ignored) {}

                if (sizeMethod != null && stackMethod != null) {
                    int size = (int) sizeMethod.invoke(handler);
                    for (int i = 0; i < size; i++) {
                        ItemStack stack = (ItemStack) stackMethod.invoke(handler, i);
                        if (isExperienceTank(stack)) {
                            tanks.add(stack);
                            LOGGER.debug("Found experience tank in Baubles slot {}: {}", i, stack.getDisplayName());
                        }
                    }
                } else if (handler instanceof IInventory) {
                    IInventory baublesInventory = (IInventory) handler;
                    for (int i = 0; i < baublesInventory.getSizeInventory(); i++) {
                        ItemStack stack = baublesInventory.getStackInSlot(i);
                        if (isExperienceTank(stack)) {
                            tanks.add(stack);
                            LOGGER.debug("Found experience tank in Baubles slot {}: {}", i, stack.getDisplayName());
                        }
                    }
                } else {
                    LOGGER.debug("Baubles handler does not expose slot methods and is not IInventory: {}", handler.getClass().getName());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error scanning Baubles inventory for player {}: {}", player.getName(), e.getMessage());
            LOGGER.debug("Baubles scan error details", e);
        }
        
        LOGGER.debug("Found {} experience tanks in Baubles slots", tanks.size());
        return tanks;
    }
    
    /**
     * 对所有物品栏类型执行全面扫描。
     * 返回按位置分类的详细储罐结果。
     * 
     * 实现需求 3.1、3.2、3.3 的全面储罐检测
     * 
     * @param player 要扫描的玩家
     * @return 包含所有检测到的储罐及其位置信息的 TankScanResult
     */
    public TankScanResult scanAllInventories(EntityPlayer player) {
        if (player == null) {
            LOGGER.debug("Player is null, returning empty scan result");
            return TankScanResult.empty();
        }
        
        LOGGER.debug("Performing comprehensive inventory scan for player: {}", player.getName());
        
        TankScanResult.Builder builder = new TankScanResult.Builder();
        
        // Scan player inventory (excluding hotbar)
        List<ItemStack> playerTanks = getPlayerInventoryTanks(player);
        builder.addTanks(playerTanks, TankScanResult.InventoryType.PLAYER_INVENTORY);
        
        // Scan hotbar
        List<ItemStack> hotbarTanks = getHotbarTanks(player);
        builder.addTanks(hotbarTanks, TankScanResult.InventoryType.HOTBAR);
        
        // Scan Baubles
        List<ItemStack> baublesTanks = getBaublesTanks(player);
        builder.addTanks(baublesTanks, TankScanResult.InventoryType.BAUBLES);
        
        TankScanResult result = builder.build();
        
        LOGGER.debug("Comprehensive scan complete - Total tanks: {}, Player: {}, Hotbar: {}, Baubles: {}", 
                    result.getTankCount(), playerTanks.size(), hotbarTanks.size(), baublesTanks.size());
        
        return result;
    }
    
    /**
     * 刷新玩家的缓存物品栏状态。
     * 当外部代码知道物品栏发生变化时很有用。
     * 
     * 实现需求 7.1：跨物品栏集成
     * 
     * @param player 要刷新物品栏状态的玩家
     */
    public void refreshInventoryState(EntityPlayer player) {
        if (player == null) {
            LOGGER.debug("Player is null, cannot refresh inventory state");
            return;
        }
        
        LOGGER.debug("Refreshing inventory state for player: {}", player.getName());
        
        // Clear any cached state (currently none, but future-proofing)
        // This method can be extended to clear caches if we add them later
        
        // Trigger inventory change handler refresh
        InventoryChangeHandler.getInstance().refreshPlayerInventory(player);
        
        // If Baubles is available, also refresh Baubles state
        if (isBaublesAvailable()) {
            InventoryChangeHandler.getInstance().refreshBaublesInventory(player);
        }
        
        LOGGER.debug("Inventory state refresh complete for player: {}", player.getName());
    }
    
    /**
     * 获取所有物品栏类型中的经验储罐总数。
     * 
     * @param player 要计数储罐的玩家
     * @return 找到的经验储罐总数
     */
    public int getTotalTankCount(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        
        TankScanResult result = scanAllInventories(player);
        return result.getTankCount();
    }
    
    /**
     * 获取所有物品栏类型中所有经验储罐的总容量。
     * 
     * @param player 要计算总容量的玩家
     * @return 总容量（经验值）
     */
    public int getTotalCapacity(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        
        TankScanResult result = scanAllInventories(player);
        return result.getTotalCapacity();
    }
    
    /**
     * 获取所有物品栏类型中所有储罐存储的总经验值。
     * 
     * @param player 要计算总存储经验值的玩家
     * @return 总存储经验值
     */
    public int getTotalStoredExperience(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        
        TankScanResult result = scanAllInventories(player);
        return result.getTotalStored();
    }
    
    /**
     * 查找所有物品栏中第一个有剩余容量的储罐。
     * 按优先级顺序搜索：快捷栏 -> 玩家物品栏 -> Baubles
     * 
     * @param player 要搜索的玩家
     * @return 第一个有可用容量的储罐，如果未找到则返回 ItemStack.EMPTY
     */
    public ItemStack findFirstAvailableTank(EntityPlayer player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        
        LOGGER.debug("Searching for first available tank for player: {}", player.getName());
        
        // Search hotbar first (most accessible)
        List<ItemStack> hotbarTanks = getHotbarTanks(player);
        for (ItemStack tank : hotbarTanks) {
            if (hasAvailableCapacity(tank)) {
                LOGGER.debug("Found available tank in hotbar: {}", tank.getDisplayName());
                return tank;
            }
        }
        
        // Search player inventory
        List<ItemStack> playerTanks = getPlayerInventoryTanks(player);
        for (ItemStack tank : playerTanks) {
            if (hasAvailableCapacity(tank)) {
                LOGGER.debug("Found available tank in player inventory: {}", tank.getDisplayName());
                return tank;
            }
        }
        
        // Search Baubles slots
        List<ItemStack> baublesTanks = getBaublesTanks(player);
        for (ItemStack tank : baublesTanks) {
            if (hasAvailableCapacity(tank)) {
                LOGGER.debug("Found available tank in Baubles: {}", tank.getDisplayName());
                return tank;
            }
        }
        
        LOGGER.debug("No available tanks found for player: {}", player.getName());
        return ItemStack.EMPTY;
    }
    
    /**
     * 查找所有物品栏中有可用容量的所有储罐。
     * 
     * @param player 要搜索的玩家
     * @return 有可用容量的储罐列表
     */
    public List<ItemStack> findAllAvailableTanks(EntityPlayer player) {
        List<ItemStack> availableTanks = new ArrayList<>();
        
        if (player == null) {
            return availableTanks;
        }
        
        TankScanResult result = scanAllInventories(player);
        
        for (ItemStack tank : result.getAllTanks()) {
            if (hasAvailableCapacity(tank)) {
                availableTanks.add(tank);
            }
        }
        
        LOGGER.debug("Found {} tanks with available capacity for player: {}", 
                    availableTanks.size(), player.getName());
        
        return availableTanks;
    }
    
    // Private helper methods
    
    /**
     * 检查物品栈是否是经验储罐。
     * 
     * @param stack 要检查的物品栈
     * @return 如果栈是经验储罐则返回 true
     */
    private boolean isExperienceTank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump;
    }
    
    /**
     * 检查储罐是否有存储更多经验的可用容量。
     * 
     * @param tank 要检查的储罐
     * @return 如果储罐有可用容量则返回 true
     */
    private boolean hasAvailableCapacity(ItemStack tank) {
        if (!isExperienceTank(tank)) {
            return false;
        }
        
        int stored = ItemExperiencePump.getXpStoredFromNBT(tank);
        int capacity = ItemExperiencePump.getMaxXpFromNBT(tank);
        
        return stored < capacity;
    }
    
    /**
     * 检查 Baubles 模组是否可用且可访问。
     * 使用缓存避免重复反射调用。
     * 
     * @return 如果 Baubles 可用则返回 true
     */
    private boolean isBaublesAvailable() {
        if (baublesAvailable == null) {
            baublesAvailable = checkBaublesAvailability();
        }
        return baublesAvailable;
    }
    
    /**
     * 使用反射执行实际的 Baubles 可用性检查。
     * 
     * @return 如果 Baubles 可用且可访问则返回 true
     */
    private boolean checkBaublesAvailability() {
        if (!Loader.isModLoaded("baubles")) {
            LOGGER.debug("Baubles mod not loaded");
            return false;
        }
        
        try {
            Class.forName("baubles.api.BaublesApi");
            LOGGER.debug("Baubles API available");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Baubles API class not found despite mod being loaded");
            return false;
        }
    }
    
    /**
     * 使用反射获取玩家的 Baubles 物品栏。
     * 
     * @param player 要获取 Baubles 物品栏的玩家
     * @return Baubles 物品栏，如果不可用则返回 null
     * @throws Exception 如果反射失败
     */
    private IInventory getBaublesInventory(EntityPlayer player) throws Exception {
        Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
        Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                               .invoke(null, player);
        
        if (handler instanceof IInventory) {
            IInventory baubles = (IInventory) handler;
            LOGGER.debug("Successfully accessed Baubles inventory with {} slots", 
                        baubles.getSizeInventory());
            return baubles;
        } else {
            LOGGER.debug("Baubles handler is not an IInventory: {}", 
                        handler != null ? handler.getClass().getName() : "null");
            return null;
        }
    }
    
    /**
     * 重置 Baubles 可用性缓存。
     * 对测试或当模组状态变化时有用。
     */
    public void resetBaublesCache() {
        baublesAvailable = null;
        LOGGER.debug("Baubles availability cache reset");
    }
    
    /**
     * 获取物品栏集成层的诊断信息。
     * 对调试和监控有用。
     * 
     * @param player 要获取诊断信息的玩家
     * @return 包含诊断信息的映射
     */
    public Map<String, Object> getDiagnostics(EntityPlayer player) {
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        
        if (player == null) {
            diagnostics.put("error", "Player is null");
            return diagnostics;
        }
        
        diagnostics.put("playerName", player.getName());
        diagnostics.put("baublesAvailable", isBaublesAvailable());
        
        TankScanResult result = scanAllInventories(player);
        diagnostics.put("totalTanks", result.getTankCount());
        diagnostics.put("totalCapacity", result.getTotalCapacity());
        diagnostics.put("totalStored", result.getTotalStored());
        diagnostics.put("tanksByLocation", result.getLocationSummary());
        
        return diagnostics;
    }
}