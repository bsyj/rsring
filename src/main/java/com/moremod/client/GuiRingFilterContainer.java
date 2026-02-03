package com.moremod.client;

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

import com.moremod.capability.IRsRingCapability;
import com.moremod.capability.RsRingCapability;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

/**
 * 物品吸收戒指黑白名单过滤GUI（使用 GuiContainer）
 * 完全参考 Cyclic 的 GuiItemPump 和 GuiBaseContainer 实现
 */
@SideOnly(Side.CLIENT)
public class GuiRingFilterContainer extends GuiContainer {

    // 参考 Cyclic 的常量定义
    private static final int SQ = 18; // Const.SQ - 槽位大小
    private static final int PAD = 8; // Const.PAD - 边距
    private static final int SLOT_COUNT = 9;
    private static final int TOGGLE_BTN_WIDTH = 20;
    private static final int TOGGLE_BTN_HEIGHT = 20;

    // 完全参考 Cyclic 的 ContainerItemPump 槽位布局
    private static final int SLOTX_START = PAD;
    private static final int SLOTY = SQ + PAD * 4;
    
    // 纹理资源（使用 Cyclic 的材质）
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation("rsring", "textures/gui/table.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("rsring", "textures/gui/inventory_slot.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation("rsring", "textures/gui/buttons.png");
    private static final ResourceLocation VANILLA_BUTTON_TEXTURE = new ResourceLocation("minecraft", "textures/gui/widgets.png");

    private final ItemStack ringStack;
    private final String title;
    private IRsRingCapability capability;

    public GuiRingFilterContainer(ContainerRingFilter container, ItemStack ringStack, String title) {
        super(container);
        this.ringStack = ringStack;
        this.title = title;
        this.capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        this.xSize = 176;
        this.ySize = 166;  // 保持原有高度不变
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    private void refreshCapability() {
        if (capability == null) capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        refreshCapability();
        int relativeX = mouseX - this.guiLeft;
        int relativeY = mouseY - this.guiTop;
        int btnX = 150;
        int btnY = PAD / 2;
        boolean isOverButton = isMouseOverButton(relativeX, relativeY, btnX, btnY);
        if (!isOverButton) {
            this.renderHoveredToolTip(mouseX, mouseY);
        }
        drawCustomTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(GUI_BACKGROUND);
        int thisX = (this.width - this.xSize) / 2;
        int thisY = (this.height - this.ySize) / 2;
        int u = 0, v = 0;
        net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(
            thisX, thisY, u, v, this.xSize, this.ySize, this.xSize, this.ySize);
        drawFilterSlots();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        
        // 绘制标题 - RGB 跑马灯效果
        String titleText = title;
        long t = System.currentTimeMillis();
        int titlePeriod = 2000; // 2秒一个周期
        float titleHue = ((t % titlePeriod) / (float) titlePeriod) % 1.0f;
        // 添加额外的色相偏移，产生更炫酷的双重彩虹效果
        float hueOffset = (float)Math.sin(t / 500.0) * 0.1f; // 额外的波动
        int titleColor = hsvToRgbInt((titleHue + hueOffset) % 1.0f, 1.0f, 1.0f); // 饱和度和明度都是最大，色彩鲜艳
        int titleX = (this.xSize - this.fontRenderer.getStringWidth(titleText)) / 2;
        this.fontRenderer.drawStringWithShadow(titleText, titleX, 6, titleColor);
        
        drawCustomButtons(mouseX, mouseY);
    }
    
    /**
     * 将 HSV 颜色转换为 RGB 整数值
     * @param hue 色相 (0.0-1.0)
     * @param saturation 饱和度 (0.0-1.0)
     * @param value 明度 (0.0-1.0)
     * @return RGB 颜色的整数值 (0xRRGGBB)
     */
    private int hsvToRgbInt(float hue, float saturation, float value) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = Math.round(value * 255);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)Math.floor(h);
            float p = value * (1.0f - saturation);
            float q = value * (1.0f - saturation * f);
            float t = value * (1.0f - (saturation * (1.0f - f)));
            
            switch ((int)h) {
                case 0:
                    r = Math.round(value * 255);
                    g = Math.round(t * 255);
                    b = Math.round(p * 255);
                    break;
                case 1:
                    r = Math.round(q * 255);
                    g = Math.round(value * 255);
                    b = Math.round(p * 255);
                    break;
                case 2:
                    r = Math.round(p * 255);
                    g = Math.round(value * 255);
                    b = Math.round(t * 255);
                    break;
                case 3:
                    r = Math.round(p * 255);
                    g = Math.round(q * 255);
                    b = Math.round(value * 255);
                    break;
                case 4:
                    r = Math.round(t * 255);
                    g = Math.round(p * 255);
                    b = Math.round(value * 255);
                    break;
                case 5:
                    r = Math.round(value * 255);
                    g = Math.round(p * 255);
                    b = Math.round(q * 255);
                    break;
            }
        }
        return (r << 16) | (g << 8) | b;
    }

    private void drawFilterSlots() {
        if (capability == null) return;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(SLOT_TEXTURE);
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = this.guiLeft + SLOTX_START + i * SQ - 1;
            int slotY = this.guiTop + SLOTY - 1;
            net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(
                slotX, slotY, 0, 0, SQ, SQ, SQ, SQ);
        }
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < SLOT_COUNT; i++) {
            String itemName = capability.getFilterSlot(i);
            if (itemName == null || itemName.isEmpty()) continue;
            try {
                ItemStack display = new ItemStack(net.minecraft.item.Item.REGISTRY.getObject(new ResourceLocation(itemName)));
                if (!display.isEmpty()) {
                    int slotX = this.guiLeft + SLOTX_START + i * SQ;
                    int slotY = this.guiTop + SLOTY;
                    this.mc.getRenderItem().renderItemAndEffectIntoGUI(display, slotX, slotY);
                }
            } catch (Exception ignored) {}
        }
        GlStateManager.popMatrix();
    }

    private void drawCustomButtons(int mouseX, int mouseY) {
        if (capability == null) return;
        int x = 150;
        int y = PAD / 2;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int hoverState = isMouseOverButton(mouseX, mouseY, x, y) ? 2 : 1;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, 
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, 
            GlStateManager.SourceFactor.ONE, 
            GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.mc.getTextureManager().bindTexture(VANILLA_BUTTON_TEXTURE);
        this.drawTexturedModalRect(x, y, 0, 46 + hoverState * 20, TOGGLE_BTN_WIDTH / 2, TOGGLE_BTN_HEIGHT);
        this.drawTexturedModalRect(x + TOGGLE_BTN_WIDTH / 2, y, 200 - TOGGLE_BTN_WIDTH / 2, 46 + hoverState * 20, TOGGLE_BTN_WIDTH / 2, TOGGLE_BTN_HEIGHT);
        this.mc.getTextureManager().bindTexture(BUTTON_TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int textureIndex = capability.isWhitelistMode() ? 12 : 11;
        int sizeBtnTexture = 16;
        int texX = textureIndex * sizeBtnTexture;
        int texY = 0;
        if (textureIndex > 15) {
            texY = (textureIndex / 15) * sizeBtnTexture;
        }
        this.drawTexturedModalRect(x + 2, y + 2, texX, texY, sizeBtnTexture, sizeBtnTexture);
    }
    
    private boolean isMouseOverButton(int mouseX, int mouseY, int btnX, int btnY) {
        return mouseX >= btnX && mouseX < btnX + TOGGLE_BTN_WIDTH && 
               mouseY >= btnY && mouseY < btnY + TOGGLE_BTN_HEIGHT;
    }

    private void drawCustomTooltips(int mouseX, int mouseY) {
        if (capability == null) return;
        int relativeX = mouseX - this.guiLeft;
        int relativeY = mouseY - this.guiTop;
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = SLOTX_START + i * SQ;
            int slotY = SLOTY;
            if (isPointInRegion(slotX, slotY, SQ - 2, SQ - 2, mouseX, mouseY)) {
                String itemName = capability.getFilterSlot(i);
                if (itemName == null || itemName.isEmpty()) {
                    this.drawHoveringText(java.util.Arrays.asList(
                        TextFormatting.GRAY + "点击添加过滤物品",
                        TextFormatting.DARK_GRAY + "仅读取，不消耗"
                    ), mouseX, mouseY);
                } else {
                    // 显示物品名称
                    try {
                        ItemStack display = new ItemStack(net.minecraft.item.Item.REGISTRY.getObject(new ResourceLocation(itemName)));
                        if (!display.isEmpty()) {
                            java.util.List<String> tooltip = display.getTooltip(this.mc.player, this.mc.gameSettings.advancedItemTooltips ? net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED : net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL);
                            this.drawHoveringText(tooltip, mouseX, mouseY);
                        }
                    } catch (Exception e) {
                        // 如果无法获取物品，显示原始名称
                        this.drawHoveringText(java.util.Arrays.asList(itemName), mouseX, mouseY);
                    }
                }
                return;
            }
        }
        int btnX = 150;
        int btnY = PAD / 2;
        if (isMouseOverButton(relativeX, relativeY, btnX, btnY)) {
            // 为按钮悬停提示添加 RGB 跑马灯效果
            long t = System.currentTimeMillis();
            int period = 2000; // 2秒一个周期
            float hue = ((t % period) / (float) period) % 1.0f;
            
            // 创建带RGB颜色的文本
            java.util.List<String> tooltip = new java.util.ArrayList<>();
            String mode = capability.isWhitelistMode() ? "白名单模式" : "黑名单模式";
            
            // 使用TextFormatting的颜色代码，通过HUE值循环切换颜色
            net.minecraft.util.text.TextFormatting[] colors = {
                net.minecraft.util.text.TextFormatting.RED,
                net.minecraft.util.text.TextFormatting.GOLD,
                net.minecraft.util.text.TextFormatting.YELLOW,
                net.minecraft.util.text.TextFormatting.GREEN,
                net.minecraft.util.text.TextFormatting.AQUA,
                net.minecraft.util.text.TextFormatting.BLUE,
                net.minecraft.util.text.TextFormatting.LIGHT_PURPLE,
                net.minecraft.util.text.TextFormatting.DARK_PURPLE
            };
            
            int colorIndex = (int)(hue * colors.length) % colors.length;
            net.minecraft.util.text.TextFormatting modeColor = colors[colorIndex];
            
            int hintColorIndex = (int)((hue + 0.5) * colors.length) % colors.length;
            net.minecraft.util.text.TextFormatting hintColor = colors[hintColorIndex];
            
            tooltip.add(modeColor + mode);
            tooltip.add(hintColor + "点击切换过滤模式");
            
            this.drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        refreshCapability();
        // 完全照抄 Cyclic GuiBaseContainer：先 super（背包槽位/拖放），再处理过滤槽与按钮
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (capability == null) return;
        int relativeX = mouseX - this.guiLeft;
        int relativeY = mouseY - this.guiTop;
        int btnX = 150;
        int btnY = PAD / 2;
        if (isMouseOverButton(relativeX, relativeY, btnX, btnY)) {
            boolean oldMode = capability.isWhitelistMode();
            capability.setWhitelistMode(!oldMode);
            String newModeText = capability.isWhitelistMode() ? "白名单模式" : "黑名单模式";

            // 发送切换模式的消息给玩家
            this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                net.minecraft.util.text.TextFormatting.GOLD + "模式切换为: " +
                net.minecraft.util.text.TextFormatting.AQUA + newModeText
            ));

            RsRingCapability.syncCapabilityToStack(ringStack, capability);
            String[] slots = new String[SLOT_COUNT];
            for (int i = 0; i < SLOT_COUNT; i++) slots[i] = capability.getFilterSlot(i);
            com.moremod.rsring.RsRingMod.network.sendToServer(
                new com.moremod.network.PacketSyncRingFilter(capability.isWhitelistMode(), slots));
            return;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = SLOTX_START + i * SQ;
            int slotY = SLOTY;
            if (isPointInRegion(slotX, slotY, SQ - 2, SQ - 2, mouseX, mouseY)) {
                mouseClickedWrapper(i);
                return;
            }
        }
    }
    
    private void mouseClickedWrapper(int slotIndex) {
        if (capability == null) return;
        ItemStack stackInMouse = this.mc.player.inventory.getItemStack();
        
        String currentFilter = capability.getFilterSlot(slotIndex);
        if (stackInMouse.isEmpty() && (currentFilter == null || currentFilter.isEmpty())) {
            return;
        }
        
        if (stackInMouse.isEmpty()) {
            capability.setFilterSlot(slotIndex, "");
        } else {
            String name = stackInMouse.getItem().getRegistryName() != null ? 
                stackInMouse.getItem().getRegistryName().toString() : "";
            if (!name.isEmpty()) {
                capability.setFilterSlot(slotIndex, name);
                // 过滤器仅读取不消耗：不清空鼠标，保留物品在玩家手中
            }
        }
        
        RsRingCapability.syncCapabilityToStack(ringStack, capability);
        String[] slots = new String[SLOT_COUNT];
        for (int j = 0; j < SLOT_COUNT; j++) {
            slots[j] = capability.getFilterSlot(j);
        }
        com.moremod.rsring.RsRingMod.network.sendToServer(
            new com.moremod.network.PacketSyncRingFilter(capability.isWhitelistMode(), slots));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            this.mc.player.closeScreen();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
