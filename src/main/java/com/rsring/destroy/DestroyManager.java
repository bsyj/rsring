package com.rsring.destroy;

import com.rsring.capability.DestroyModeType;
import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.compat.CompatManager;
import com.rsring.item.ItemAbsorbRing;
import com.rsring.service.RingDetectionService;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * 销毁管理器
 * 
 * 功能：定时清理背包模组背包内的垃圾物品
 * 
 * 注意事项：
 * 1. 销毁功能在所有模式下都生效（绑定吸收箱和背包模式）
 * 2. 销毁类型只在有背包模组背包时生效（背包有容量限制，需要数量判断）
 * 3. 无背包时走地面销毁逻辑（直接销毁，不需要数量判断）
 * 4. 只清理背包模组的背包内容，不涉及玩家物品栏
 * 5. 遵循垃圾箱优先原则：优先送入垃圾箱，垃圾箱满了再销毁
 * 6. STORAGE_OVERFLOW模式不参与定时清理，只在新物品吸收时处理
 */
public class DestroyManager {
    
    /**
     * 定时清理背包模组背包内的垃圾物品
     * 每秒检查一次
     * 
     * @param player 玩家
     */
    public static void onTickCleanup(EntityPlayer player) {
        if (player.world.isRemote || player.world.getTotalWorldTime() % 20 != 0) {
            return; // 每秒检查一次
        }
        
        ItemStack ringStack = RingDetectionService.findRing(player, ItemAbsorbRing.class);
        if (ringStack == null || ringStack.isEmpty()) {
            return;
        }
        
        IRsRingCapability capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (capability == null || !capability.isDestroyEnabled()) {
            return;
        }
        
        // 检查是否应该工作在GUI内
        if (!capability.shouldWorkInGUI() && player.openContainer != player.inventoryContainer) {
            return;
        }
        
        // STORAGE_OVERFLOW模式不参与定时清理，只在新物品吸收时处理
        DestroyModeType destroyModeType = capability.getDestroyModeType();
        if (destroyModeType == DestroyModeType.STORAGE_OVERFLOW) {
            return;
        }
        
        // 检查是否有背包模组背包
        boolean hasBackpack = CompatManager.isAnyBackpackModAvailable() && CompatManager.hasAnyBackpack(player);
        
        if (hasBackpack) {
            // 有背包：清理背包内容，销毁类型生效
            CompatManager.cleanupBackpacks(player, capability);
        }
        // 无背包时不处理（地面物品销毁在 ItemAbsorbRing.onUpdate 中处理）
    }
}
