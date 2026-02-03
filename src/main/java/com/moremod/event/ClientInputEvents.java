package com.moremod.event;

import com.moremod.item.ItemAbsorbRing;
import com.moremod.rsring.RsRingMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 客户端输入事件处理器
 * 处理在GUI界面中右键点击戒指打开黑白名单界面的逻辑
 */
@Mod.EventBusSubscriber(modid = RsRingMod.MODID, value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class ClientInputEvents {
    
    private static final Logger LOGGER = LogManager.getLogger(ClientInputEvents.class);
    
    /**
     * 处理GUI界面中的鼠标点击事件
     * 当玩家在背包界面右键点击物品吸收戒指时，打开黑白名单GUI
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMouseEvent(GuiScreenEvent.MouseInputEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        
        // 检查是否在容器GUI中
        if (!(mc.currentScreen instanceof GuiContainer)) {
            return;
        }
        
        GuiContainer gui = (GuiContainer) mc.currentScreen;
        
        // 检查是否是右键点击
        boolean rightClick = org.lwjgl.input.Mouse.getEventButton() == 1 && org.lwjgl.input.Mouse.getEventButtonState();
        
        if (!rightClick) {
            return;
        }
        
        try {
            // 获取鼠标下的槽位
            Slot slotUnderMouse = gui.getSlotUnderMouse();
            if (slotUnderMouse == null) {
                return;
            }
            
            ItemStack stackInSlot = slotUnderMouse.getStack();
            
            // 检查是否是物品吸收戒指
            if (!stackInSlot.isEmpty() && stackInSlot.getItem() instanceof ItemAbsorbRing) {
                // 直接在客户端打开GUI，不需要发送数据包
                com.moremod.rsring.RsRingMod.proxy.openAbsorbRingGui(stackInSlot);
                event.setCanceled(true);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling ring GUI click", e);
        }
    }
}