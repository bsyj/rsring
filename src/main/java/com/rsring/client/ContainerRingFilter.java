package com.rsring.client;

/*
 * Portions of this file are based on Cyclic (https://github.com/Lothrazar/Cyclic), licensed under the MIT License.
 * Source reference: E:\mod\Cyclic-trunk-1.12
 *
 * The MIT License (MIT)
 * Copyright (C) 2014-2018 Sam Bassett (aka Lothrazar)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 物品吸收戒指过滤器 Container
 * 完全参考 Cyclic 的 ContainerItemPump 和 ContainerBase 实现
 * 
 * 注意：这个 Container 不包含任何实际的槽位（过滤槽是虚拟的，不消耗物品）
 * 只绑定玩家背包槽位以便正确渲染
 */
public class ContainerRingFilter extends Container {
    
    // 完全参考 Cyclic 的 Const 常量
    private static final int SQ = 18; // Const.SQ - 槽位大小
    private static final int PAD = 8; // Const.PAD - 边距
    
    // 玩家背包位置（完全参考 Cyclic 的 ScreenSize.STANDARD）
    // playerOffsetX() = PAD = 8
    // playerOffsetY() = 84
    private static final int PLAYER_OFFSET_X = PAD;
    private static final int PLAYER_OFFSET_Y = 84;
    
    private final ItemStack ringStack;
    
    public ContainerRingFilter(InventoryPlayer playerInventory, ItemStack ringStack) {
        this.ringStack = ringStack;
        bindPlayerInventory(playerInventory);
    }
    
    /**
     * 绑定玩家背包槽位（完全参考 Cyclic 的 ContainerBase.bindPlayerInventory）
     */
    protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {
        // 主背包（3行9列）
        // 完全参考 Cyclic 的实现
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9,
                    PLAYER_OFFSET_X + j * SQ, // X
                    PLAYER_OFFSET_Y + i * SQ  // Y
                ));
            }
        }
        
        // 快捷栏（1行9列）
        bindPlayerHotbar(inventoryPlayer);
    }
    
    /**
     * 绑定玩家快捷栏（完全参考 Cyclic 的 ContainerBase.bindPlayerHotbar）
     */
    protected void bindPlayerHotbar(InventoryPlayer inventoryPlayer) {
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(inventoryPlayer, i,
                PLAYER_OFFSET_X + i * SQ,
                PLAYER_OFFSET_Y + PAD / 2 + 3 * SQ // 快捷栏在主背包下方，PAD/2 = 4
            ));
        }
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        // 完全参考 Cyclic 的 ContainerBase.canInteractWith
        return true;
    }
    
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        // 不支持 Shift+点击转移物品（过滤槽是虚拟的）
        return ItemStack.EMPTY;
    }

    /**
     * 获取虚拟过滤槽位的ID范围（用于GUI处理）
     * 过滤槽位是虚拟的，不在容器中实际存在，但需要在GUI中处理
     */
    public static int getVirtualFilterSlotStart() {
        return 9; // 玩家背包槽位后开始
    }

    public static int getVirtualFilterSlotEnd() {
        return 18; // 9个过滤槽位
    }
}
