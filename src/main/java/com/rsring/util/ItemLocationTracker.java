package com.rsring.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 追踪物品在玩家inventory中的位置，支持Baubles饰品栏
 * 
 * 核心功能：
 * 1. 查找物品并记录其位置信息
 * 2. 修改物品后将其同步回原位置
 * 3. 特别处理Baubles返回的ItemStack副本问题
 */
public class ItemLocationTracker {
    
    private static final Logger LOGGER = LogManager.getLogger(ItemLocationTracker.class);
    
    /**
     * 物品位置类型
     */
    public enum LocationType {
        MAIN_HAND,      // 主手
        OFF_HAND,       // 副手
        PLAYER_INVENTORY, // 玩家背包
        BAUBLES         // Baubles饰品栏
    }
    
    private final ItemStack item;
    private final LocationType locationType;
    private final int slotIndex;
    private final IInventory baublesInventory; // 仅Baubles使用
    
    /**
     * 构造函数
     * 
     * @param item 物品ItemStack
     * @param locationType 位置类型
     * @param slotIndex 槽位索引（手持时为-1）
     * @param baublesInventory Baubles inventory引用（仅Baubles使用）
     */
    public ItemLocationTracker(ItemStack item, LocationType locationType, int slotIndex, IInventory baublesInventory) {
        this.item = item;
        this.locationType = locationType;
        this.slotIndex = slotIndex;
        this.baublesInventory = baublesInventory;
        
        LOGGER.debug("Created ItemLocationTracker: type={}, slot={}, item={}", 
                    locationType, slotIndex, item.getDisplayName());
    }
    
    /**
     * 获取追踪的物品
     */
    public ItemStack getItem() {
        return item;
    }
    
    /**
     * 获取位置类型
     */
    public LocationType getLocationType() {
        return locationType;
    }
    
    /**
     * 获取槽位索引
     */
    public int getSlotIndex() {
        return slotIndex;
    }
    
    /**
     * 将修改后的ItemStack写回原位置
     * 这是解决Baubles副本问题的关键方法
     * 
     * @param player 玩家实体
     */
    public void syncBack(EntityPlayer player) {
        if (player == null) {
            LOGGER.warn("Cannot sync back: player is null");
            return;
        }
        
        LOGGER.debug("Syncing item back to {}, slot {}", locationType, slotIndex);
        
        switch (locationType) {
            case BAUBLES:
                if (baublesInventory != null) {
                    baublesInventory.setInventorySlotContents(slotIndex, item);
                    baublesInventory.markDirty();
                    LOGGER.debug("Synced to Baubles slot {}", slotIndex);
                } else {
                    LOGGER.error("Cannot sync to Baubles: inventory is null");
                }
                break;
                
            case PLAYER_INVENTORY:
                player.inventory.setInventorySlotContents(slotIndex, item);
                player.inventory.markDirty();
                LOGGER.debug("Synced to player inventory slot {}", slotIndex);
                break;
                
            case MAIN_HAND:
                player.setHeldItem(EnumHand.MAIN_HAND, item);
                LOGGER.debug("Synced to main hand");
                break;
                
            case OFF_HAND:
                player.setHeldItem(EnumHand.OFF_HAND, item);
                LOGGER.debug("Synced to off hand");
                break;
        }
    }
    
    /**
     * 查找指定类型的物品并返回带位置信息的tracker
     * 
     * 查找优先级：
     * 1. 主手
     * 2. 副手
     * 3. Baubles饰品栏
     * 4. 玩家背包
     * 
     * @param player 玩家实体
     * @param itemClass 物品类型
     * @return ItemLocationTracker，如果未找到则返回null
     */
    public static ItemLocationTracker findItem(EntityPlayer player, Class<? extends Item> itemClass) {
        if (player == null || itemClass == null) {
            LOGGER.debug("Cannot find item: player or itemClass is null");
            return null;
        }
        
        LOGGER.debug("Searching for item of type: {}", itemClass.getSimpleName());
        
        // 1. 检查主手
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && itemClass.isInstance(mainHand.getItem())) {
            LOGGER.debug("Found item in main hand");
            return new ItemLocationTracker(mainHand, LocationType.MAIN_HAND, -1, null);
        }
        
        // 2. 检查副手
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && itemClass.isInstance(offHand.getItem())) {
            LOGGER.debug("Found item in off hand");
            return new ItemLocationTracker(offHand, LocationType.OFF_HAND, -1, null);
        }
        
        // 3. 检查Baubles饰品栏
        if (Loader.isModLoaded("baubles")) {
            ItemLocationTracker baublesResult = findInBaubles(player, itemClass);
            if (baublesResult != null) {
                LOGGER.debug("Found item in Baubles slot {}", baublesResult.getSlotIndex());
                return baublesResult;
            }
        }
        
        // 4. 检查玩家背包
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                LOGGER.debug("Found item in player inventory slot {}", i);
                return new ItemLocationTracker(stack, LocationType.PLAYER_INVENTORY, i, null);
            }
        }
        
        LOGGER.debug("Item not found in any location");
        return null;
    }
    
    /**
     * 在Baubles饰品栏中查找物品
     * 使用反射访问Baubles API
     * 
     * @param player 玩家实体
     * @param itemClass 物品类型
     * @return ItemLocationTracker，如果未找到则返回null
     */
    private static ItemLocationTracker findInBaubles(EntityPlayer player, Class<? extends Item> itemClass) {
        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                                    .invoke(null, player);
            
            if (handler instanceof IInventory) {
                IInventory baubles = (IInventory) handler;
                for (int i = 0; i < baubles.getSizeInventory(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                        LOGGER.debug("Found item in Baubles inventory slot {}", i);
                        return new ItemLocationTracker(stack, LocationType.BAUBLES, i, baubles);
                    }
                }
            } else {
                LOGGER.debug("Baubles handler is not IInventory: {}", 
                           handler != null ? handler.getClass().getName() : "null");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Baubles API not found (mod not loaded)");
        } catch (Exception e) {
            LOGGER.error("Error accessing Baubles inventory", e);
        }
        
        return null;
    }
    
    /**
     * 检查物品是否在Baubles中
     */
    public boolean isInBaubles() {
        return locationType == LocationType.BAUBLES;
    }
    
    /**
     * 检查物品是否在手持位置
     */
    public boolean isInHand() {
        return locationType == LocationType.MAIN_HAND || locationType == LocationType.OFF_HAND;
    }
    
    /**
     * 检查物品是否在背包中
     */
    public boolean isInInventory() {
        return locationType == LocationType.PLAYER_INVENTORY;
    }
    
    @Override
    public String toString() {
        return String.format("ItemLocationTracker{type=%s, slot=%d, item=%s}", 
                           locationType, slotIndex, item.getDisplayName());
    }
}
