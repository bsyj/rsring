package com.rsring.client;

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.filter.FilterMode;
import com.rsring.filter.ItemAttribute;
import com.rsring.util.Pair;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 高级过滤系统 Container
 * 支持三种过滤模式切换和属性过滤管理
 */
public class ContainerAdvancedFilter extends Container {
    
    private static final int SQ = 18;
    private static final int PAD = 8;
    private static final int PLAYER_OFFSET_X = PAD;
    private static final int PLAYER_OFFSET_Y = 84;
    
    private final ItemStack ringStack;
    private final IRsRingCapability capability;
    
    // 虚拟槽位 ID 范围
    public static final int FILTER_SLOT_START = 0;
    public static final int FILTER_SLOT_END = 8;
    public static final int MOD_SLOT_START = 9;
    public static final int MOD_SLOT_END = 17;
    
    public ContainerAdvancedFilter(InventoryPlayer playerInventory, ItemStack ringStack) {
        this.ringStack = ringStack;
        this.capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        
        // 添加玩家背包槽位（过滤槽是虚拟的，不实际存在）
        bindPlayerInventory(playerInventory);
    }
    
    protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {
        // 主背包
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9,
                    PLAYER_OFFSET_X + j * SQ,
                    PLAYER_OFFSET_Y + i * SQ));
            }
        }
        
        // 快捷栏
        bindPlayerHotbar(inventoryPlayer);
    }
    
    protected void bindPlayerHotbar(InventoryPlayer inventoryPlayer) {
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(inventoryPlayer, i,
                PLAYER_OFFSET_X + i * SQ,
                PLAYER_OFFSET_Y + PAD / 2 + 3 * SQ));
        }
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }
    
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        return ItemStack.EMPTY;
    }
    
    public IRsRingCapability getCapability() {
        return capability;
    }
    
    public ItemStack getRingStack() {
        return ringStack;
    }
    
    /**
     * 获取过滤模式
     */
    @SideOnly(Side.CLIENT)
    public FilterMode getFilterMode() {
        return capability != null ? capability.getFilterMode() : FilterMode.ITEM;
    }
    
    /**
     * 检查是否是属性过滤模式
     */
    @SideOnly(Side.CLIENT)
    public boolean isAttributeMode() {
        return getFilterMode() == FilterMode.ATTRIBUTE;
    }
    
    /**
     * 获取属性列表
     */
    @SideOnly(Side.CLIENT)
    public List<Pair<ItemAttribute, Boolean>> getFilterAttributes() {
        return capability != null ? capability.getFilterAttributes() : new ArrayList<>();
    }
}
