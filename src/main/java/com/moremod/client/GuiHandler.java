package com.moremod.client;

import com.moremod.item.ItemAbsorbRing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * Forge GUI 处理器
 * 用于正确同步客户端和服务端的 Container
 */
public class GuiHandler implements IGuiHandler {

    public static final int GUI_RING_FILTER = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GUI_RING_FILTER) {
            // 查找玩家手持或饰品栏中的戒指
            ItemStack ringStack = findRingStack(player);
            if (!ringStack.isEmpty()) {
                return new ContainerRingFilter(player.inventory, ringStack);
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GUI_RING_FILTER) {
            // 查找玩家手持或饰品栏中的戒指
            ItemStack ringStack = findRingStack(player);
            if (!ringStack.isEmpty()) {
                ContainerRingFilter container = new ContainerRingFilter(player.inventory, ringStack);
                return new GuiRingFilterContainer(container, ringStack, "物品吸收戒指 - 黑白名单");
            }
        }
        return null;
    }

    /**
     * 查找戒指：主手 > 副手 > Baubles > 背包
     */
    private ItemStack findRingStack(EntityPlayer player) {
        // 主手
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof ItemAbsorbRing) {
            return mainHand;
        }
        // 副手
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && offHand.getItem() instanceof ItemAbsorbRing) {
            return offHand;
        }
        // Baubles
        if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
            try {
                Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);
                if (handler instanceof net.minecraft.inventory.IInventory) {
                    net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                    for (int i = 0; i < baubles.getSizeInventory(); i++) {
                        ItemStack stack = baubles.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof ItemAbsorbRing) {
                            return stack;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        // 背包
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemAbsorbRing) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
