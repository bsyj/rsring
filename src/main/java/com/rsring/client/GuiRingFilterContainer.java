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

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.filter.AttributeRegistry;
import com.rsring.filter.FilterMode;
import com.rsring.filter.ItemAttribute;
import com.rsring.util.Pair;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 物品吸收戒指过滤器GUI界面，继承自GuiContainer
 * 完全参考 Cyclic 的 GuiItemPump 和 GuiBaseContainer 实现
 */
@SideOnly(Side.CLIENT)
public class GuiRingFilterContainer extends GuiContainer {

private static final int SQ = 18;

private static final int PAD = 8;
    private static final int SLOT_COUNT = 9;
    private static final int TOGGLE_BTN_WIDTH = 18;
    private static final int TOGGLE_BTN_HEIGHT = 18;
    private static final int FILTER_MODE_BTN_WIDTH = 18;
    private static final int FILTER_MODE_BTN_HEIGHT = 18;

    private static final int SLOTX_START = PAD;
    private static final int SLOTY = SQ + PAD * 4;

    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation("rsring", "textures/gui/table.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("rsring", "textures/gui/inventory_slot.png");
    private static final ResourceLocation SLOT_BACKGROUND_REF = new ResourceLocation("rsring", "textures/gui/slots_background_ref.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation("rsring", "textures/gui/buttons.png");
    private static final ResourceLocation VANILLA_BUTTON_TEXTURE = new ResourceLocation("minecraft", "textures/gui/widgets.png");
    private static final ResourceLocation GUI_CONTROLS = new ResourceLocation("rsring", "textures/gui/gui_controls.png");
    private static final ResourceLocation SOPHISTICATED_CORE_ICONS = new ResourceLocation("rsring", "textures/gui/icons_sophisticatedcore.png");

    private final ItemStack ringStack;
    private final String title;
    private IRsRingCapability capability;
    
    // 点击冷却时间（毫秒）
    private static final long CLICK_COOLDOWN = 200;
    private long lastButtonClickTime = 0;
    
    // 属性过滤模式相关
    private static final int ATTR_BTN_SIZE = 18;
    private static final int ATTR_DISPLAY_Y = 80; // 属性显示区域Y坐标
    private int selectedAttributeIndex = 0; // 当前选中的属性索引（用于添加）
    private int selectedRemoveIndex = 0; // 当前选中的移除索引
    private int attributeScrollOffset = 0; // 属性列表滚动偏移量
    private List<ItemAttribute> availableAttributes = new ArrayList<>(); // 可添加的属性列表
    private List<Pair<ItemAttribute, Boolean>> currentAttributes = new ArrayList<>(); // 当前已添加的属性列表
    
    // Tooltip滚动相关 - 参考机械动力 SelectionScrollInput 实现
    private static final int MAX_TOOLTIP_ITEMS = 8; // Tooltip最大显示行数
    private boolean isHoveringAddButton = false; // 鼠标是否悬停在加号按钮上

    public GuiRingFilterContainer(ContainerRingFilter container, ItemStack ringStack, String title) {
        super(container);
        this.ringStack = ringStack;
        this.title = title;
        this.capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        refreshAttributeLists();
    }
    
    private void refreshAttributeLists() {
        if (capability == null) return;
        
        // 根据销毁模式UI状态决定使用哪套属性列表
        boolean isDestroyModeUI = capability.isDestroyModeUI();
        
        // 刷新当前属性列表 - 直接使用引用，确保修改同步
        currentAttributes = isDestroyModeUI ? capability.getDestroyFilterAttributes() : capability.getFilterAttributes();

        // 刷新可用属性列表（基于独立属性输入槽位中的物品）
        // 列表中已有的属性（无论正向还是反向）都不再显示
        availableAttributes.clear();
        ItemStack inputStack = isDestroyModeUI ? capability.getDestroyAttributeInputStack() : capability.getAttributeInputStack();
        if (!inputStack.isEmpty()) {
            List<ItemAttribute> attrs = AttributeRegistry.getAttributesForItem(inputStack);
            for (ItemAttribute attr : attrs) {
                // 检查是否已在availableAttributes列表中存在
                boolean existsInAvailable = false;
                for (ItemAttribute existing : availableAttributes) {
                    if (isSameAttribute(existing, attr)) {
                        existsInAvailable = true;
                        break;
                    }
                }
                if (existsInAvailable) continue;
                
                // 检查是否已添加到当前属性列表（无论正向还是反向）
                boolean existsInCurrent = false;
                for (Pair<ItemAttribute, Boolean> pair : currentAttributes) {
                    if (isSameAttribute(pair.getKey(), attr)) {
                        existsInCurrent = true;
                        break;
                    }
                }
                
                // 如果已存在（无论正向还是反向），跳过此属性
                if (existsInCurrent) {
                    continue;
                }
                
                availableAttributes.add(attr);
            }
        }

        // 确保索引不越界
        if (selectedAttributeIndex >= availableAttributes.size() && !availableAttributes.isEmpty()) {
            selectedAttributeIndex = availableAttributes.size() - 1;
        }
        if (selectedRemoveIndex >= currentAttributes.size() && !currentAttributes.isEmpty()) {
            selectedRemoveIndex = currentAttributes.size() - 1;
        }
    }
    
    /**
     * 检查两个属性是否相同
     * 通过比较翻译键和参数来判断
     */
    private boolean isSameAttribute(ItemAttribute a1, ItemAttribute a2) {
        if (!a1.getTranslationKey().equals(a2.getTranslationKey())) {
            return false;
        }
        Object[] params1 = a1.getTranslationParameters();
        Object[] params2 = a2.getTranslationParameters();
        if (params1.length != params2.length) {
            return false;
        }
        for (int i = 0; i < params1.length; i++) {
            if (params1[i] == null && params2[i] == null) continue;
            if (params1[i] == null || params2[i] == null) return false;
            if (!params1[i].equals(params2[i])) return false;
        }
        return true;
    }

    private void refreshCapability() {
        // 总是重新获取 capability，确保获取最新的数据
        capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
    }

    private boolean isCustomFiltersAllowed() {
        return com.rsring.config.RsRingConfig.absorbRing.allowCustomFilters;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 在渲染前刷新 capability，确保 drawGuiContainerBackgroundLayer 使用最新数据
        refreshCapability();
        super.drawScreen(mouseX, mouseY, partialTicks);
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
        
        // 获取当前模式（考虑销毁模式UI状态）
        boolean isDestroyModeUI = capability != null && capability.isDestroyModeUI();
        FilterMode currentMode = isDestroyModeUI ? 
            (capability != null ? capability.getDestroyFilterMode() : FilterMode.ITEM) : 
            (capability != null ? capability.getFilterMode() : FilterMode.ITEM);
        
        // 只有非属性过滤模式才绘制9格槽位
        if (capability == null || currentMode != FilterMode.ATTRIBUTE) {
            drawFilterSlots();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        
        // 获取当前模式（考虑销毁模式UI状态）
        boolean isDestroyModeUI = capability != null && capability.isDestroyModeUI();
        FilterMode currentMode = isDestroyModeUI ? 
            (capability != null ? capability.getDestroyFilterMode() : FilterMode.ITEM) : 
            (capability != null ? capability.getFilterMode() : FilterMode.ITEM);
        
        // 确定标题文本
        String titleText = isDestroyModeUI ? "⚠ 销毁模式 - 请谨慎配置" : title;

        // 属性过滤模式下不显示标题
        if (capability == null || currentMode != FilterMode.ATTRIBUTE) {
            long t = System.currentTimeMillis();

            int titlePeriod = 2000;
            float titleHue = ((t % titlePeriod) / (float) titlePeriod) % 1.0f;

            float hueOffset = (float)Math.sin(t / 500.0) * 0.1f;
            
            // 销毁模式使用红色系标题，吸收模式使用彩虹色
            int titleColor;
            if (isDestroyModeUI) {
                // 红色闪烁效果
                int redIntensity = 200 + (int)(55 * Math.sin(t / 200.0));
                titleColor = (redIntensity << 16) | 0x4444; // 红色带一点蓝
            } else {
                titleColor = hsvToRgbInt((titleHue + hueOffset) % 1.0f, 1.0f, 1.0f);
            }
            
            int titleX = (this.xSize - this.fontRenderer.getStringWidth(titleText)) / 2;
            this.fontRenderer.drawStringWithShadow(titleText, titleX, 6, titleColor);

            // 销毁模式下显示当前过滤模式提示（属性过滤界面明显，不需要提示）
            if (isDestroyModeUI && capability != null) {
                FilterMode destroyMode = capability.getDestroyFilterMode();
                String modeText = null;
                if (destroyMode == FilterMode.MOD) {
                    modeText = "模组过滤";
                } else if (destroyMode == FilterMode.ITEM) {
                    modeText = "ID过滤";
                }
                if (modeText != null) {
                    // 使用红色系跑马灯颜色，与标题同步
                    int redIntensity = 180 + (int)(55 * Math.sin(t / 200.0));
                    int modeColor = (redIntensity << 16) | 0x6666; // 红色带一点蓝
                    // 计算"请"字的X坐标：标题X + "⚠ 销毁模式 - "的宽度
                    String prefix = "⚠ 销毁模式 - ";
                    int modeX = titleX + this.fontRenderer.getStringWidth(prefix);
                    // Y坐标与第二排按钮(黑白名单按钮)齐平
                    int whitelistBtnY = SQ + PAD / 2; // 第二排按钮Y坐标
                    this.fontRenderer.drawStringWithShadow(modeText, modeX, whitelistBtnY, modeColor);
                }
            }
        }

        drawCustomButtons(mouseX, mouseY);
        
        // 属性过滤模式下绘制额外控件
        if (capability != null && currentMode == FilterMode.ATTRIBUTE) {
            drawAttributeFilterControls(mouseX, mouseY);
        }
    }
    
    private void drawAttributeFilterControls(int mouseX, int mouseY) {
        refreshAttributeLists();

        // 黑框位置和大小 - 从竖直按钮右侧开始，右边顶到GUI边缘
        int boxX = SLOTX_START + SQ + 4; // 竖直按钮右侧 + 间隔
        int boxWidth = this.xSize - boxX - 8;
        int boxY = PAD / 2; // 与"过"按钮顶部齐平 (y=4)
        int boxHeight = 47; // 属性列表区域高度，调整为5行

        // 三个按钮和属性过滤槽位放在一起
        int controlY = boxY + boxHeight + 6;
        int removeBtnX = SLOTX_START + 5 * SQ;
        int addBtnX = SLOTX_START + 6 * SQ;
        int matchBtnX = SLOTX_START + 7 * SQ;
        int itemSlotX = SLOTX_START + 8 * SQ;
        int itemSlotY = controlY;

        // 绘制纯黑背景框
        GlStateManager.disableTexture2D();
        drawRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // 绘制边框 - 使用深色描边
        // 上边框
        drawRect(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFF333333);
        // 下边框
        drawRect(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFF333333);
        // 左边框
        drawRect(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF333333);
        // 右边框
        drawRect(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF333333);
        
        // 跑马灯流星效果 - 双流星沿边框循环移动
        drawMeteorBorder(boxX, boxY, boxWidth, boxHeight);
        
        GlStateManager.enableTexture2D();

        // 绘制已添加的属性列表 - 固定5行显示
        int lineHeight = 9;  // 行高
        int maxDisplayCount = 5;  // 固定显示5行
        int startY = boxY + (boxHeight - maxDisplayCount * lineHeight) / 2 + 1;  // 垂直居中

        if (attributeScrollOffset < 0) attributeScrollOffset = 0;
        if (attributeScrollOffset > currentAttributes.size() - maxDisplayCount) {
            attributeScrollOffset = Math.max(0, currentAttributes.size() - maxDisplayCount);
        }

        for (int i = 0; i < maxDisplayCount && (i + attributeScrollOffset) < currentAttributes.size(); i++) {
            int attrIndex = i + attributeScrollOffset;
            Pair<ItemAttribute, Boolean> attrPair = currentAttributes.get(attrIndex);
            ItemAttribute attr = attrPair.getKey();
            boolean inverted = attrPair.getValue();

            String text = attr.format(inverted).getUnformattedText();
            // 截断过长的文本
            int maxTextWidth = boxWidth - 6;
            if (this.fontRenderer.getStringWidth(text) > maxTextWidth) {
                text = this.fontRenderer.trimStringToWidth(text, maxTextWidth - 2) + "...";
            }
            
            // 反转属性使用紫色，普通属性使用白色
            int textColor = inverted ? 0xDDA0DD : 0xE0E0E0;
            this.fontRenderer.drawString(text, boxX + 2, startY + i * lineHeight, textColor);
        }

        // 绘制滚动指示器
        if (currentAttributes.size() > maxDisplayCount) {
            int scrollBarHeight = Math.max(6, (maxDisplayCount * lineHeight) * maxDisplayCount / currentAttributes.size());
            int scrollBarY = startY + (attributeScrollOffset * (maxDisplayCount * lineHeight) / currentAttributes.size());
            // 滚动条背景
            GlStateManager.disableTexture2D();
            GlStateManager.color(0.3F, 0.3F, 0.3F, 0.5F);
            drawRect(boxX + boxWidth - 4, startY, boxX + boxWidth - 1, startY + maxDisplayCount * lineHeight, 0x55333333);
            // 滚动条滑块
            GlStateManager.color(0.7F, 0.7F, 0.7F, 1.0F);
            drawRect(boxX + boxWidth - 4, scrollBarY, boxX + boxWidth - 1, scrollBarY + scrollBarHeight, 0xFFAAAAAA);
            GlStateManager.enableTexture2D();
        }

// 美化的空状态提示
        if (currentAttributes.isEmpty()) {
            int centerX = boxX + boxWidth / 2;
            
            // 获取销毁模式UI状态
            boolean isDestroyModeUI = capability != null && capability.isDestroyModeUI();
            
            // 绘制虚线边框提示区域
            GlStateManager.disableTexture2D();
            
            long t = System.currentTimeMillis();
            
            if (isDestroyModeUI) {
                // 销毁模式：红色闪烁边框
                int redIntensity = 150 + (int)(50 * Math.sin(t / 150.0));
                int borderColor = (redIntensity << 16) | 0x2222; // 红色
                drawDottedRect(boxX + 4, boxY + 8, boxWidth - 8, 28, borderColor);
            } else {
                // 吸收模式：灰色边框
                GlStateManager.color(0.4F, 0.4F, 0.4F, 0.6F);
                drawDottedRect(boxX + 4, boxY + 8, boxWidth - 8, 28, 0xFF555555);
            }
            GlStateManager.enableTexture2D();
            
            // 居中绘制提示文字
            int hintY = boxY + 8;
            
            if (isDestroyModeUI) {
                // 销毁模式：红色跑马灯效果
                int period = 1500;
                float hue = ((t % period) / (float) period) % 1.0f;
                
                // 跑马灯颜色
                int colorIndex = (int)(hue * 8) % 8;
                int[] colors = {0xFFFF4444, 0xFFFF6644, 0xFFFF8844, 0xFFFFAA44, 
                                0xFFFFAA44, 0xFFFF8844, 0xFFFF6644, 0xFFFF4444};
                int line1Color = colors[colorIndex];
                
                String line1 = "⚠ 暂无销毁属性";
                String line2 = "放入物品后点击 + 添加";
                int w1 = this.fontRenderer.getStringWidth(line1);
                int w2 = this.fontRenderer.getStringWidth(line2);
                this.fontRenderer.drawStringWithShadow(line1, centerX - w1 / 2, hintY + 6, line1Color);
                this.fontRenderer.drawString(line2, centerX - w2 / 2, hintY + 17, 0xAAAAAA);
            } else {
                // 吸收模式：普通灰色提示
                String line1 = "暂无属性";
                String line2 = "放入物品后点击 + 添加";
                int w1 = this.fontRenderer.getStringWidth(line1);
                int w2 = this.fontRenderer.getStringWidth(line2);
                this.fontRenderer.drawString(line1, centerX - w1 / 2, hintY + 6, 0x888888);
                this.fontRenderer.drawString(line2, centerX - w2 / 2, hintY + 17, 0x666666);
            }
        }

        // 绘制底部四个控件（三个按钮+物品槽位）并排对齐
        // 注意：绘制在ForegroundLayer中使用相对坐标，但hover检测需要绝对坐标
        int btnDrawY = controlY - 1;
        // 计算绝对坐标用于hover检测
        int absRemoveBtnX = this.guiLeft + removeBtnX - 1;
        int absAddBtnX = this.guiLeft + addBtnX - 1;
        int absMatchBtnX = this.guiLeft + matchBtnX - 1;
        int absBtnDrawY = this.guiTop + btnDrawY;
        
        drawAttributeButton(removeBtnX - 1, btnDrawY, 0, isMouseOverAttributeButton(mouseX, mouseY, absRemoveBtnX, absBtnDrawY));
        drawAttributeButton(addBtnX - 1, btnDrawY, 1, isMouseOverAttributeButton(mouseX, mouseY, absAddBtnX, absBtnDrawY));
        boolean isDestroyModeUI = capability != null && capability.isDestroyModeUI();
        boolean matchAll = capability != null && (isDestroyModeUI ? capability.isDestroyMatchAllMode() : capability.isMatchAllMode());
        drawAttributeButton(matchBtnX - 1, btnDrawY, matchAll ? 2 : 3, isMouseOverAttributeButton(mouseX, mouseY, absMatchBtnX, absBtnDrawY));

        // 绘制物品槽位 - 与按钮统一Y坐标
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(SLOT_BACKGROUND_REF);
        this.drawTexturedModalRect(itemSlotX - 1, btnDrawY, 0, 0, SQ, SQ);

        // 绘制槽位中的物品
        // 槽位背景绘制在 (itemSlotX-1, btnDrawY)，尺寸 18x18
        // 物品应该在槽位内部，偏移1像素边框
        ItemStack inputStack = isDestroyModeUI ? capability.getDestroyAttributeInputStack() : capability.getAttributeInputStack();
        if (!inputStack.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            // ForegroundLayer中的renderItemAndEffectIntoGUI使用相对坐标
            // 槽位背景在 (itemSlotX-1, btnDrawY)，物品从 (itemSlotX, btnDrawY+1) 开始
            int itemRenderX = itemSlotX;
            int itemRenderY = btnDrawY + 1;
            this.mc.getRenderItem().renderItemAndEffectIntoGUI(inputStack, itemRenderX, itemRenderY);
            this.mc.getRenderItem().renderItemOverlayIntoGUI(this.fontRenderer, inputStack, itemRenderX, itemRenderY, null);
            RenderHelper.disableStandardItemLighting();
        }
    }

    /**
     * 绘制边框流星跑马灯效果
     * 参考经验泵控制器的双流星效果
     */
    private void drawMeteorBorder(int boxX, int boxY, int boxWidth, int boxHeight) {
        drawMeteorBorder(boxX, boxY, boxWidth, boxHeight, false, 0);
    }
    
    /**
     * 绘制销毁开关按钮的流星边框
     * 流星边框在按钮外围绘制，形成环绕效果
     * @param btnX 按钮X坐标（相对）
     * @param btnY 按钮Y坐标（相对）
     * @param enabled 是否开启（true=绿色，false=红色）
     */
    private void drawDestroyToggleMeteorBorder(int btnX, int btnY, boolean enabled) {
        int size = TOGGLE_BTN_WIDTH;
        long t = System.currentTimeMillis();
        // 外围边框尺寸比按钮大2像素
        int outerSize = size + 2;
        int offsetX = -1; // 向外偏移
        int offsetY = -1;
        int perimeter = 4 * outerSize; // 外围正方形边框周长
        int meteorLength = 10; // 流星长度
        int period = 2000; // 2秒周期
        
        // 根据开启/关闭状态选择颜色
        int baseColor = enabled ? 0xFF00FF00 : 0xFFFF4444; // 绿色或红色
        int bgColor = enabled ? 0xFF004400 : 0xFF440000; // 深绿或深红背景
        
        // 计算流星的基础相位
        float basePhase = (t % period) / (float) period;
        
        // 绘制两个流星（相位差半周）
        for (int meteor = 0; meteor < 2; meteor++) {
            float meteorPhase = (basePhase + meteor * 0.5f) % 1.0f;
            int meteorPos = (int) (meteorPhase * perimeter);
            
            // 绘制单个流星
            for (int i = 0; i < meteorLength; i++) {
                int pos = (meteorPos - i + perimeter) % perimeter;
                int mx, my;
                
                // 计算流星当前位置在外围边框的哪一边
                if (pos < outerSize) {
                    // 上边（从左到右）
                    mx = btnX + offsetX + pos;
                    my = btnY + offsetY;
                } else if (pos < 2 * outerSize) {
                    // 右边（从上到下）
                    mx = btnX + offsetX + outerSize - 1;
                    my = btnY + offsetY + (pos - outerSize);
                } else if (pos < 3 * outerSize) {
                    // 下边（从右到左）
                    mx = btnX + offsetX + outerSize - (pos - 2 * outerSize) - 1;
                    my = btnY + offsetY + outerSize - 1;
                } else {
                    // 左边（从下到上）
                    mx = btnX + offsetX;
                    my = btnY + offsetY + outerSize - (pos - 3 * outerSize) - 1;
                }
                
                // 头大尾小效果
                float meteorProgress = (float) i / meteorLength;
                float alpha = 1.0f - meteorProgress * meteorProgress * meteorProgress;
                
                // 脉冲效果 - 颜色亮度波动
                float pulse = 0.7f + 0.3f * (float)Math.sin(t / 200.0 + meteorPhase * Math.PI * 2);
                
                // 颜色混合：尾部逐渐融入背景色
                int r = (int) ((((baseColor >> 16) & 0xFF) * alpha * pulse) + (((bgColor >> 16) & 0xFF) * (1.0f - alpha)));
                int g = (int) ((((baseColor >> 8) & 0xFF) * alpha * pulse) + (((bgColor >> 8) & 0xFF) * (1.0f - alpha)));
                int b = (int) (((baseColor & 0xFF) * alpha * pulse) + ((bgColor & 0xFF) * (1.0f - alpha)));
                int blendedColor = 0xFF000000 | (r << 16) | (g << 8) | b;
                
                // 绘制单像素流星
                drawRect(mx, my, mx + 1, my + 1, blendedColor);
            }
        }
    }
    
    /**
     * 绘制边框流星跑马灯效果（可指定是否使用固定颜色）
     */
    private void drawMeteorBorder(int boxX, int boxY, int boxWidth, int boxHeight, boolean useFixedColor, int fixedColor) {
        long t = System.currentTimeMillis();
        int perimeter = 2 * (boxWidth + boxHeight); // 边框周长
        int meteorLength = 20; // 流星长度
        int period = 3000; // 3秒周期
        float hueOffset = (float)Math.sin(t / 500.0) * 0.1f; // 双重彩虹波动
        
        // 计算流星的基础相位
        float basePhase = (t % period) / (float) period;

        // 绘制两个流星（相位差半周）
        for (int meteor = 0; meteor < 2; meteor++) {
            float meteorPhase = (basePhase + meteor * 0.5f) % 1.0f;
            int meteorPos = (int) (meteorPhase * perimeter);

            // 绘制单个流星
            for (int i = 0; i < meteorLength; i++) {
                int pos = (meteorPos - i + perimeter) % perimeter;
                int mx, my;

                // 计算流星当前位置在边框的哪一边
                if (pos < boxWidth) {
                    // 上边（从左到右）
                    mx = boxX + pos;
                    my = boxY;
                } else if (pos < boxWidth + boxHeight) {
                    // 右边（从上到下）
                    mx = boxX + boxWidth - 1;
                    my = boxY + (pos - boxWidth);
                } else if (pos < 2 * boxWidth + boxHeight) {
                    // 下边（从右到左）
                    mx = boxX + boxWidth - (pos - boxWidth - boxHeight) - 1;
                    my = boxY + boxHeight - 1;
                } else {
                    // 左边（从下到上）
                    mx = boxX;
                    my = boxY + boxHeight - (pos - 2 * boxWidth - boxHeight) - 1;
                }

                // 头大尾小效果
                float meteorProgress = (float) i / meteorLength;
                float alpha = 1.0f - meteorProgress * meteorProgress * meteorProgress;

                int meteorColor;
                if (useFixedColor) {
                    meteorColor = fixedColor;
                } else {
                    // RGB 色相与双重彩虹波动同步
                    float meteorHue = (meteorPhase + hueOffset) % 1.0f;
                    meteorColor = hsvToRgbInt(meteorHue, 1.0f, 1.0f);
                }

                // 边框背景色
                int bgColor = 0xFF333333;

                // 颜色混合：尾部逐渐融入背景色
                int r = (int) ((((meteorColor >> 16) & 0xFF) * alpha) + (((bgColor >> 16) & 0xFF) * (1.0f - alpha)));
                int g = (int) ((((meteorColor >> 8) & 0xFF) * alpha) + (((bgColor >> 8) & 0xFF) * (1.0f - alpha)));
                int b = (int) (((meteorColor & 0xFF) * alpha) + ((bgColor & 0xFF) * (1.0f - alpha)));
                int blendedColor = 0xFF000000 | (r << 16) | (g << 8) | b;

                // 绘制单像素流星
                drawRect(mx, my, mx + 1, my + 1, blendedColor);
            }
        }
    }

    /**
     * 绘制虚线边框矩形
     */
    private void drawDottedRect(int x, int y, int width, int height, int color) {
        int dashLen = 2;
        int gapLen = 2;
        
        // 上边
        for (int i = 0; i < width; i += dashLen + gapLen) {
            int len = Math.min(dashLen, width - i);
            drawRect(x + i, y, x + i + len, y + 1, color);
        }
        // 下边
        for (int i = 0; i < width; i += dashLen + gapLen) {
            int len = Math.min(dashLen, width - i);
            drawRect(x + i, y + height - 1, x + i + len, y + height, color);
        }
        // 左边
        for (int i = 0; i < height; i += dashLen + gapLen) {
            int len = Math.min(dashLen, height - i);
            drawRect(x, y + i, x + 1, y + i + len, color);
        }
        // 右边
        for (int i = 0; i < height; i += dashLen + gapLen) {
            int len = Math.min(dashLen, height - i);
            drawRect(x + width - 1, y + i, x + width, y + i + len, color);
        }
    }

    private void drawAttributeButton(int x, int y, int iconIndex, boolean hovered) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        
        // 使用本地gui_controls按钮背景
        this.mc.getTextureManager().bindTexture(GUI_CONTROLS);
        int u = hovered ? 47 : 29;
        this.drawTexturedModalRect(x, y, u, 0, ATTR_BTN_SIZE, ATTR_BTN_SIZE);
        
        // 绘制图标 - 使用精妙背包的icons.png
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(SOPHISTICATED_CORE_ICONS);
        // 减号: UV(112, 32), 加号: UV(96, 32), 匹配全部(AND): UV(16, 80), 匹配任意(OR): UV(0, 80)
        int[] iconU = {112, 96, 16, 0};
        int[] iconV = {32, 32, 80, 80};
        // iconIndex: 0=减号, 1=加号, 2=匹配全部(AND), 3=匹配任意(OR)
        this.drawTexturedModalRect(x + 1, y + 1, iconU[iconIndex], iconV[iconIndex], 16, 16);
    }
    
    /**
     * 检测鼠标是否在属性按钮上
     * @param mouseX 绝对鼠标X坐标
     * @param mouseY 绝对鼠标Y坐标
     * @param btnX 按钮绝对X坐标（已包含guiLeft）
     * @param btnY 按钮绝对Y坐标（已包含guiTop）
     */
    private boolean isMouseOverAttributeButton(int mouseX, int mouseY, int btnX, int btnY) {
        // btnX和btnY已经是绝对坐标，直接与mouseX/mouseY比较
        return mouseX >= btnX && mouseX < btnX + ATTR_BTN_SIZE &&
               mouseY >= btnY && mouseY < btnY + ATTR_BTN_SIZE;
    }

    /**
     * 将HSV (h: 0..1, s:0..1, v:0..1) 转换为RGB整数
     * @param hue 色相(0.0-1.0)
     * @param saturation 饱和度(0.0-1.0)
     * @param value 亮度(0.0-1.0)
     * @return RGB 整数值 (0xRRGGBB)
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
        
        // 根据销毁模式UI状态决定使用哪套过滤槽
        boolean isDestroyModeUI = capability.isDestroyModeUI();
        // 获取当前过滤模式
        FilterMode currentMode = isDestroyModeUI ? capability.getDestroyFilterMode() : capability.getFilterMode();
        
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
        
        // 根据过滤模式绘制不同的槽位内容
        if (currentMode == FilterMode.MOD) {
            // 模组过滤模式：显示模组图标（使用模组过滤槽位）
            for (int i = 0; i < SLOT_COUNT; i++) {
                String modId = isDestroyModeUI ? capability.getDestroyModFilterSlot(i) : capability.getModFilterSlot(i);
                if (modId == null || modId.isEmpty()) continue;
                drawSlotItem(i, modId, isDestroyModeUI, true);
            }
        } else {
            // 物品ID过滤模式：显示物品图标（带NBT和耐久条）
            for (int i = 0; i < SLOT_COUNT; i++) {
                String itemName = isDestroyModeUI ? capability.getDestroyFilterSlot(i) : capability.getFilterSlot(i);
                if (itemName == null || itemName.isEmpty()) continue;
                drawSlotItem(i, itemName, isDestroyModeUI, false);
            }
        }
        GlStateManager.popMatrix();
    }
    
    /**
     * 绘制槽位物品（包括耐久条）
     */
    private void drawSlotItem(int slotIndex, String itemId, boolean isDestroyModeUI, boolean isModFilter) {
        int slotX = this.guiLeft + SLOTX_START + slotIndex * SQ;
        int slotY = this.guiTop + SLOTY;
        
        try {
            net.minecraft.item.Item item = net.minecraft.item.Item.REGISTRY.getObject(new ResourceLocation(itemId));
            if (item == null) return;
            
            ItemStack display = new ItemStack(item);
            // 应用存储的NBT数据
            net.minecraft.nbt.NBTTagCompound storedNbt;
            if (isModFilter) {
                storedNbt = isDestroyModeUI ? 
                    capability.getDestroyModFilterSlotNBT(slotIndex) : capability.getModFilterSlotNBT(slotIndex);
            } else {
                storedNbt = isDestroyModeUI ? 
                    capability.getDestroyFilterSlotNBT(slotIndex) : capability.getFilterSlotNBT(slotIndex);
            }
            if (storedNbt != null) {
                // 复制NBT并移除我们添加的耐久度标记
                net.minecraft.nbt.NBTTagCompound displayNbt = storedNbt.copy();
                // 读取并应用耐久度
                if (displayNbt.hasKey("rsring_filter_damage")) {
                    int damage = displayNbt.getInteger("rsring_filter_damage");
                    display.setItemDamage(damage);
                    displayNbt.removeTag("rsring_filter_damage");
                }
                if (displayNbt.getSize() > 0) {
                    display.setTagCompound(displayNbt);
                }
            }
            
            // 使用RenderItem的标准渲染方法（与GuiContainer一致）
            net.minecraft.client.renderer.RenderItem renderItem = this.mc.getRenderItem();
            
            // 渲染物品模型
            renderItem.renderItemAndEffectIntoGUI(display, slotX, slotY);
            // 渲染耐久条（原版方法，只有损坏的物品才显示）
            renderItem.renderItemOverlayIntoGUI(this.mc.fontRenderer, display, slotX, slotY, "");
        } catch (Exception ignored) {}
    }
    
    /**
     * 绘制模组过滤槽位
     * 显示用户放入的物品图标（带NBT和耐久条）
     */
    private void drawModIcon(int slotIndex, String modId, boolean isDestroyModeUI) {
        // 使用统一的绘制方法
        drawSlotItem(slotIndex, modId, isDestroyModeUI, true);
    }
    
    /**
     * 获取模组的代表性物品（用于模组过滤模式显示）
     */
    private ItemStack getRepresentativeItemForMod(String modId) {
        // 遍历物品注册表，找到该模组的第一个物品
        for (net.minecraft.item.Item item : net.minecraft.item.Item.REGISTRY) {
            if (item != null && item.getRegistryName() != null) {
                if (item.getRegistryName().getNamespace().equals(modId)) {
                    try {
                        return new ItemStack(item);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * 根据模组ID生成颜色
     */
    private int getModColor(String modId) {
        int hash = modId.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        // 确保颜色足够亮
        r = Math.max(80, Math.min(255, r));
        g = Math.max(80, Math.min(255, g));
        b = Math.max(80, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }

    private void drawCustomButtons(int mouseX, int mouseY) {
        if (capability == null) return;
        
        // 获取销毁模式UI状态
        boolean isDestroyModeUI = capability.isDestroyModeUI();
        boolean isDestroyEnabled = capability.isDestroyEnabled();
        
        // 根据销毁模式UI状态决定使用哪套过滤设置
        // 注意：销毁模式和吸收模式是完全独立的两个系统
        FilterMode currentMode = isDestroyModeUI ? capability.getDestroyFilterMode() : capability.getFilterMode();
        boolean isWhitelist = isDestroyModeUI ? capability.isDestroyWhitelistMode() : capability.isWhitelistMode();
        boolean matchNbt = isDestroyModeUI ? capability.shouldDestroyMatchNbt() : capability.shouldMatchNbt();
        boolean matchDurability = isDestroyModeUI ? capability.shouldDestroyMatchDurability() : capability.shouldMatchDurability();
        
        // 按钮移到左边竖排，与槽位左对齐再左移1px
        int leftBtnX = SLOTX_START - 1; // 向左移动1像素
        // 按钮上移，与黑框顶部对齐
        int filterModeBtnY = PAD / 2; // 与黑框顶部齐平
        int whitelistBtnY = filterModeBtnY + SQ; // 下方间隔一个槽位高度
        
        // 绘制过滤模式切换按钮（上）- 使用精妙背包样式
        switch (currentMode) {
            case ITEM:
                // 物品ID过滤使用钻石物品图标
                drawItemIconButton(leftBtnX, filterModeBtnY, mouseX, mouseY, new net.minecraft.item.ItemStack(net.minecraft.init.Items.DIAMOND));
                break;
            case MOD:
                drawIconToggleButton(leftBtnX, filterModeBtnY, mouseX, mouseY, true, 32, 16);
                break;
            case ATTRIBUTE:
                // 属性过滤使用NBT匹配开启图标
                drawIconToggleButton(leftBtnX, filterModeBtnY, mouseX, mouseY, true, 32, 0);
                break;
            default:
                drawIconToggleButton(leftBtnX, filterModeBtnY, mouseX, mouseY, true, 0, 0);
        }
        
        // 绘制黑白名单切换按钮（下）- 使用原来的贴图样式
        // 白名单图标索引12，黑名单图标索引11
        int whitelistIconIndex = isWhitelist ? 12 : 11;
        drawOriginalButton(leftBtnX, whitelistBtnY, mouseX, mouseY, whitelistIconIndex);

        // === 销毁模式按钮（位置根据当前过滤模式决定）===
        
        // 物品ID/模组过滤模式：销毁按钮在黑白名单右侧
        if (currentMode == FilterMode.ITEM || currentMode == FilterMode.MOD) {
            int destroyBtnX = leftBtnX + SQ; // 黑白名单右侧
            int destroyBtnY = whitelistBtnY;
            // 绘制销毁模式按钮 - 使用岩浆桶图标（销毁模式UI下显示为水桶表示退出）
            drawItemIconButton(destroyBtnX, destroyBtnY, mouseX, mouseY, 
                isDestroyModeUI ? new net.minecraft.item.ItemStack(net.minecraft.init.Items.WATER_BUCKET) 
                               : new net.minecraft.item.ItemStack(net.minecraft.init.Items.LAVA_BUCKET));
            
// 销毁模式UI下显示开关按钮和模式切换按钮
            if (isDestroyModeUI) {
                int toggleBtnX = destroyBtnX + SQ + 1; // 销毁按钮右侧
                int toggleBtnY = destroyBtnY;
                // 绘制销毁开关按钮
                drawSophisticatedToggleButton(toggleBtnX, toggleBtnY, mouseX, mouseY, isDestroyEnabled);
                // 绘制流星边框
                GlStateManager.disableTexture2D();
                drawDestroyToggleMeteorBorder(toggleBtnX, toggleBtnY, isDestroyEnabled);
                GlStateManager.enableTexture2D();
                
                // 销毁模式类型切换按钮（开关按钮右侧）
                // 只有玩家有背包时才显示三种销毁类型切换按钮
                boolean showDestroyModeType = com.rsring.compat.CompatManager.hasAnyBackpack(this.mc.player);
                if (showDestroyModeType) {
                    int modeTypeBtnX = toggleBtnX + SQ + 1;
                    int modeTypeBtnY = toggleBtnY;
                    drawDestroyModeTypeButton(modeTypeBtnX, modeTypeBtnY, mouseX, mouseY);
                }
            }
        }

        // 属性过滤模式：销毁按钮在黑白名单下方
        if (currentMode == FilterMode.ATTRIBUTE) {
            int destroyBtnX = leftBtnX;
            int destroyBtnY = whitelistBtnY + SQ; // 黑白名单下方
            // 绘制销毁模式按钮 - 使用岩浆桶图标（销毁模式UI下显示为水桶表示退出）
            drawItemIconButton(destroyBtnX, destroyBtnY, mouseX, mouseY,
                isDestroyModeUI ? new net.minecraft.item.ItemStack(net.minecraft.init.Items.WATER_BUCKET)
                               : new net.minecraft.item.ItemStack(net.minecraft.init.Items.LAVA_BUCKET));

            // 销毁模式UI下显示开关按钮和模式切换按钮
            if (isDestroyModeUI) {
                int toggleBtnX = destroyBtnX;
                int toggleBtnY = destroyBtnY + SQ + 1; // 销毁按钮下方
                // 绘制销毁开关按钮
                drawSophisticatedToggleButton(toggleBtnX, toggleBtnY, mouseX, mouseY, isDestroyEnabled);
                // 绘制流星边框
                GlStateManager.disableTexture2D();
                drawDestroyToggleMeteorBorder(toggleBtnX, toggleBtnY, isDestroyEnabled);
                GlStateManager.enableTexture2D();

                // 销毁模式类型切换按钮（开关按钮右侧）
                // 只有玩家有背包时，才显示三种销毁类型切换按钮
                boolean showDestroyModeType = com.rsring.compat.CompatManager.hasAnyBackpack(this.mc.player);
                if (showDestroyModeType) {
                    int modeTypeBtnX = toggleBtnX + SQ + 1; // 开关按钮右边
                    int modeTypeBtnY = toggleBtnY;
                    drawDestroyModeTypeButton(modeTypeBtnX, modeTypeBtnY, mouseX, mouseY);
                }
            }
        }

        // 为物品过滤和模组过滤模式绘制NBT和耐久匹配按钮（右边竖排，与左边按钮对称）
        if (currentMode == FilterMode.ITEM || currentMode == FilterMode.MOD) {
            // 右边按钮X坐标：GUI宽度 - 边距 - 按钮宽度 + 1px（向右移动）
            int rightBtnX = this.xSize - PAD - TOGGLE_BTN_WIDTH + 1;
            int nbtBtnY = filterModeBtnY;
            int durabilityBtnY = whitelistBtnY;
            
            // NBT匹配按钮 - 使用精妙背包的图标
            drawIconToggleButton(rightBtnX, nbtBtnY, mouseX, mouseY, matchNbt, 
                matchNbt ? 32 : 48, 0); // UV(32,0)=开启, UV(48,0)=关闭
            
            // 耐久匹配按钮 - 使用精妙背包的图标
            drawIconToggleButton(rightBtnX, durabilityBtnY, mouseX, mouseY, matchDurability, 
                matchDurability ? 0 : 16, 16); // UV(0,16)=开启, UV(16,16)=关闭
        }
    }

    /**
     * 绘制带物品图标的按钮
     * @param x 按钮X坐标（相对）
     * @param y 按钮Y坐标（相对）
     * @param mouseX 鼠标X坐标（绝对）
     * @param mouseY 鼠标Y坐标（绝对）
     * @param iconStack 物品图标
     */
    private void drawItemIconButton(int x, int y, int mouseX, int mouseY, net.minecraft.item.ItemStack iconStack) {
        int hoverState = isMouseOverButton(mouseX - this.guiLeft, mouseY - this.guiTop, x, y) ? 2 : 1;
        
        // 绘制按钮背景
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.mc.getTextureManager().bindTexture(GUI_CONTROLS);
        int bgU = hoverState == 2 ? 47 : 29;
        this.drawTexturedModalRect(x, y, bgU, 0, TOGGLE_BTN_WIDTH, TOGGLE_BTN_HEIGHT);
        
        // 绘制物品图标
        if (!iconStack.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            this.mc.getRenderItem().renderItemAndEffectIntoGUI(iconStack, x + 1, y + 1);
            RenderHelper.disableStandardItemLighting();
        }
    }

    /**
     * 绘制带图标的开关按钮（参考精妙背包的Toggle按钮样式）
     * @param x 按钮X坐标（相对）
     * @param y 按钮Y坐标（相对）
     * @param mouseX 鼠标X坐标（绝对）
     * @param mouseY 鼠标Y坐标（绝对）
     * @param isOn 是否开启
     * @param iconU 图标U坐标
     * @param iconV 图标V坐标
     */
    private void drawIconToggleButton(int x, int y, int mouseX, int mouseY, boolean isOn, int iconU, int iconV) {
        int hoverState = isMouseOverButton(mouseX - this.guiLeft, mouseY - this.guiTop, x, y) ? 2 : 1;
        
        // 绘制按钮背景
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.mc.getTextureManager().bindTexture(GUI_CONTROLS);
        // 使用gui_controls的按钮背景：未悬停(29,0), 悬停(47,0)
        int bgU = hoverState == 2 ? 47 : 29;
        this.drawTexturedModalRect(x, y, bgU, 0, TOGGLE_BTN_WIDTH, TOGGLE_BTN_HEIGHT);
        
        // 绘制图标 - 使用精妙背包的icons纹理
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(SOPHISTICATED_CORE_ICONS);
        this.drawTexturedModalRect(x + 1, y + 1, iconU, iconV, 16, 16);
    }

    /**
     * 绘制使用原贴图图标的按钮（黑白名单按钮）
     * 背景使用精妙背包样式，图标使用原来的buttons.png
     * @param x 按钮X坐标（相对）
     * @param y 按钮Y坐标（相对）
     * @param mouseX 鼠标X坐标（绝对）
     * @param mouseY 鼠标Y坐标（绝对）
     * @param iconIndex 图标索引（从buttons.png）
     */
    private void drawOriginalButton(int x, int y, int mouseX, int mouseY, int iconIndex) {
        int hoverState = isMouseOverButton(mouseX - this.guiLeft, mouseY - this.guiTop, x, y) ? 2 : 1;
        
        // 绘制按钮背景 - 使用精妙背包样式
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.mc.getTextureManager().bindTexture(GUI_CONTROLS);
        // 使用gui_controls的按钮背景：未悬停(29,0), 悬停(47,0)
        int bgU = hoverState == 2 ? 47 : 29;
        this.drawTexturedModalRect(x, y, bgU, 0, TOGGLE_BTN_WIDTH, TOGGLE_BTN_HEIGHT);
        
        // 绘制图标 - 使用原来的buttons.png纹理
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BUTTON_TEXTURE);
        int iconSize = 16;
        int texX = iconIndex * iconSize;
        int texY = 0;
        if (texX > 240) {
            texY = (texX / 256) * iconSize;
            texX = texX % 256;
        }
        this.drawTexturedModalRect(x + 1, y + 1, texX, texY, iconSize, iconSize);
    }

    /**
     * 绘制使用icons_sophisticatedcore.png的开关按钮
     * 开用序号1，关用序号2
     * @param x 按钮X坐标（相对）
     * @param y 按钮Y坐标（相对）
     * @param mouseX 鼠标X坐标（绝对）
     * @param mouseY 鼠标Y坐标（绝对）
     * @param isOn 是否开启
     */
    private void drawSophisticatedToggleButton(int x, int y, int mouseX, int mouseY, boolean isOn) {
        int hoverState = isMouseOverButton(mouseX - this.guiLeft, mouseY - this.guiTop, x, y) ? 2 : 1;
        
        // 绘制按钮背景 - 使用精妙背包样式
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.mc.getTextureManager().bindTexture(GUI_CONTROLS);
        // 使用gui_controls的按钮背景：未悬停(29,0), 悬停(47,0)
        int bgU = hoverState == 2 ? 47 : 29;
        this.drawTexturedModalRect(x, y, bgU, 0, TOGGLE_BTN_WIDTH, TOGGLE_BTN_HEIGHT);
        
        // 绘制图标 - 使用icons_sophisticatedcore.png
        // 贴图是256x256，图标按16x16排列
        // 序号1对应UV(0,0)，序号2对应UV(16,0)
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(SOPHISTICATED_CORE_ICONS);
        int iconIndex = isOn ? 1 : 2; // 开用序号1，关用序号2
        int iconU = ((iconIndex - 1) % 16) * 16; // 每行16个图标
        int iconV = ((iconIndex - 1) / 16) * 16;
        this.drawTexturedModalRect(x + 1, y + 1, iconU, iconV, 16, 16);
    }

    /**
     * 绘制销毁模式类型切换按钮
     * 显示当前销毁模式类型：总是销毁/槽位溢出/存储溢出
     * 使用 buttons.png 贴图：
     * 总是销毁 - 序号3
     * 槽位溢出 - 序号15
     * 存储溢出 - 序号16
     */
    private void drawDestroyModeTypeButton(int x, int y, int mouseX, int mouseY) {
        int hoverState = isMouseOverButton(mouseX - this.guiLeft, mouseY - this.guiTop, x, y) ? 2 : 1;

        // 绘制按钮背景
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.mc.getTextureManager().bindTexture(GUI_CONTROLS);
        int bgU = hoverState == 2 ? 47 : 29;
        this.drawTexturedModalRect(x, y, bgU, 0, TOGGLE_BTN_WIDTH, TOGGLE_BTN_HEIGHT);

        // 根据销毁模式类型绘制不同图标
        // 使用 buttons.png 贴图：
        // 总是销毁 - 序号3 (UV: 32,0)
        // 槽位溢出 - 序号15 (UV: 224,0)
        // 存储溢出 - 序号16 (UV: 240,0)
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BUTTON_TEXTURE);

        com.rsring.capability.DestroyModeType modeType = capability.getDestroyModeType();
        int iconIndex;
        switch (modeType) {
            case SLOT_OVERFLOW:
                iconIndex = 15; // 槽位溢出
                break;
            case STORAGE_OVERFLOW:
                iconIndex = 16; // 存储溢出
                break;
            case ALWAYS:
            default:
                iconIndex = 3; // 总是销毁
                break;
        }
        int iconU = ((iconIndex - 1) % 16) * 16;
        int iconV = ((iconIndex - 1) / 16) * 16;
        this.drawTexturedModalRect(x + 1, y + 1, iconU, iconV, 16, 16);
    }

    /**
     * 绘制开关按钮（文字版本，备用）
     */
    private void drawToggleButton(int x, int y, int mouseX, int mouseY, boolean isOn, String label) {
        int hoverState = isMouseOverButton(mouseX - this.guiLeft, mouseY - this.guiTop, x, y) ? 2 : 1;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
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
        
        // 绘制按钮文字
        int textColor = isOn ? 0x55FF55 : 0xFF5555;
        String displayText = isOn ? "§a" + label : "§c" + label;
        int textWidth = this.fontRenderer.getStringWidth(displayText);
        this.fontRenderer.drawString(displayText, x + (TOGGLE_BTN_WIDTH - textWidth) / 2, y + 5, textColor);
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int btnX, int btnY) {
        return mouseX >= btnX && mouseX < btnX + TOGGLE_BTN_WIDTH &&
               mouseY >= btnY && mouseY < btnY + TOGGLE_BTN_HEIGHT;
    }

    private boolean isMouseOverFilterModeButton(int mouseX, int mouseY, int btnX, int btnY) {
        return mouseX >= btnX && mouseX < btnX + FILTER_MODE_BTN_WIDTH &&
               mouseY >= btnY && mouseY < btnY + FILTER_MODE_BTN_HEIGHT;
    }

    private int getFilterModeIconIndex(FilterMode mode) {
        switch (mode) {
            case ITEM:
                return 0;
            case MOD:
                return 1;
            case ATTRIBUTE:
                return 2;
            default:
                return 0;
        }
    }

    /**
     * 绘制带滚轮选择的可滚动属性tooltip
     * 参考机械动力 SelectionScrollInput.updateTooltip() 实现
     * 
     * 显示格式：
     * > ... (上方有更多)
     * > xxx
     * -> xxx (当前选中，跑马灯颜色)
     * > xxx
     * > ... (下方有更多)
     * 
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param attributes 属性列表
     * @param selectedIndex 当前选中索引
     * @param title tooltip标题
     */
    private void drawScrollableAttributeTooltip(int mouseX, int mouseY, 
            List<ItemAttribute> attributes, int selectedIndex, String title) {
        List<String> tooltip = new ArrayList<>();
        
        // 跑马灯颜色计算
        long t = System.currentTimeMillis();
        int period = 2000;
        float hue = ((t % period) / (float) period) % 1.0f;
        TextFormatting[] colors = {
            TextFormatting.RED,
            TextFormatting.GOLD,
            TextFormatting.YELLOW,
            TextFormatting.GREEN,
            TextFormatting.AQUA,
            TextFormatting.BLUE,
            TextFormatting.LIGHT_PURPLE,
            TextFormatting.DARK_PURPLE
        };
        int titleColorIndex = (int)(hue * colors.length) % colors.length;
        int hintColorIndex = (int)((hue + 0.5) * colors.length) % colors.length;
        
        // 标题 - 使用跑马灯颜色
        tooltip.add(colors[titleColorIndex] + title);
        
        // 计算显示范围（类似机械动力的 min/max 逻辑）
        // 确保选中项在中间位置
        int min = Math.max(0, Math.min(attributes.size() - MAX_TOOLTIP_ITEMS, 
                selectedIndex - MAX_TOOLTIP_ITEMS / 2));
        int max = Math.min(attributes.size(), min + MAX_TOOLTIP_ITEMS);
        
        // 调整min确保显示满MAX_TOOLTIP_ITEMS行（如果数据足够）
        if (max - min < MAX_TOOLTIP_ITEMS && attributes.size() >= MAX_TOOLTIP_ITEMS) {
            min = Math.max(0, max - MAX_TOOLTIP_ITEMS);
        }
        
        // 上方省略号（如果上方有更多项）
        if (min > 0) {
            tooltip.add(TextFormatting.GRAY + "> ...");
        }
        
        // 显示属性列表
        for (int i = min; i < max; i++) {
            ItemAttribute attr = attributes.get(i);
            String text = attr.format(false).getUnformattedText();
            
            if (i == selectedIndex) {
                // 当前选中项：-> 前缀 + 跑马灯颜色高亮
                int attrColorIndex = (int)((hue + i * 0.1) * colors.length) % colors.length;
                tooltip.add(colors[attrColorIndex] + "-> " + text);
            } else {
                // 其他项：> 前缀 + 灰色
                tooltip.add(TextFormatting.GRAY + "> " + text);
            }
        }
        
        // 下方省略号（如果下方有更多项）
        if (max < attributes.size()) {
            tooltip.add(TextFormatting.GRAY + "> ...");
        }
        
        // 操作提示 - 使用跑马灯颜色
        tooltip.add("");
        tooltip.add(colors[hintColorIndex] + "" + TextFormatting.ITALIC + "滚轮选择，左键添加");
        
        this.drawHoveringText(tooltip, mouseX, mouseY);
    }

    private void drawCustomTooltips(int mouseX, int mouseY) {
        if (capability == null) return;
        boolean customAllowed = isCustomFiltersAllowed();
        int relativeX = mouseX - this.guiLeft;
        int relativeY = mouseY - this.guiTop;
        
        // 获取销毁模式UI状态
        boolean isDestroyModeUI = capability.isDestroyModeUI();
        FilterMode currentFilterMode = isDestroyModeUI ? capability.getDestroyFilterMode() : capability.getFilterMode();
        
        // 属性过滤模式下刷新属性列表，确保数据最新
        if (currentFilterMode == FilterMode.ATTRIBUTE) {
            refreshAttributeLists();
        }
        
        // 属性过滤模式下不显示9格槽位的tooltip
        if (currentFilterMode != FilterMode.ATTRIBUTE) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                int slotX = SLOTX_START + i * SQ;
                int slotY = SLOTY;
                if (isPointInRegion(slotX, slotY, SQ - 2, SQ - 2, mouseX, mouseY)) {
                    // 根据过滤模式获取不同的槽位内容
                    if (currentFilterMode == FilterMode.MOD) {
                        // 模组过滤模式：槽位存储完整物品ID，显示物品tooltip（带NBT）
                        String itemId = isDestroyModeUI ? capability.getDestroyModFilterSlot(i) : capability.getModFilterSlot(i);
                        if (itemId == null || itemId.isEmpty()) {
                            this.drawHoveringText(java.util.Arrays.asList(
                                TextFormatting.GRAY + "点击添加过滤模组",
                                TextFormatting.DARK_GRAY + "放入任意物品自动提取模组ID"
                            ), mouseX, mouseY);
                        } else {
                            try {
                                net.minecraft.item.Item item = net.minecraft.item.Item.REGISTRY.getObject(new ResourceLocation(itemId));
                                if (item != null) {
                                    ItemStack display = new ItemStack(item);
                                    // 应用存储的NBT数据
                                    net.minecraft.nbt.NBTTagCompound storedNbt = isDestroyModeUI ? 
                                        capability.getDestroyModFilterSlotNBT(i) : capability.getModFilterSlotNBT(i);
                                    if (storedNbt != null) {
                                        display.setTagCompound(storedNbt.copy());
                                    }
                                    java.util.List<String> tooltip = display.getTooltip(this.mc.player, this.mc.gameSettings.advancedItemTooltips ? net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED : net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL);
                                    // 添加模组名称到tooltip（原版风格：灰色斜体）
                                    String modName = getModName(itemId);
                                    if (!modName.isEmpty()) {
                                        tooltip.add(TextFormatting.GRAY + TextFormatting.ITALIC.toString() + modName);
                                    }
                                    this.drawHoveringText(tooltip, mouseX, mouseY);
                                }
                            } catch (Exception e) {
                                this.drawHoveringText(java.util.Arrays.asList(itemId), mouseX, mouseY);
                            }
                        }
                    } else {
                        // 物品ID过滤模式：显示物品信息（带NBT）
                        String itemName = isDestroyModeUI ? capability.getDestroyFilterSlot(i) : capability.getFilterSlot(i);
                        if (itemName == null || itemName.isEmpty()) {
                            this.drawHoveringText(java.util.Arrays.asList(
                                TextFormatting.GRAY + "点击添加过滤物品",
                                TextFormatting.DARK_GRAY + "锁定时只读"
                            ), mouseX, mouseY);
                        } else {
                            try {
                                net.minecraft.item.Item item = net.minecraft.item.Item.REGISTRY.getObject(new ResourceLocation(itemName));
                                if (item != null) {
                                    ItemStack display = new ItemStack(item);
                                    // 应用存储的NBT数据和耐久度
                                    net.minecraft.nbt.NBTTagCompound storedNbt = isDestroyModeUI ? 
                                        capability.getDestroyFilterSlotNBT(i) : capability.getFilterSlotNBT(i);
                                    if (storedNbt != null) {
                                        net.minecraft.nbt.NBTTagCompound displayNbt = storedNbt.copy();
                                        // 读取并应用耐久度
                                        if (displayNbt.hasKey("rsring_filter_damage")) {
                                            display.setItemDamage(displayNbt.getInteger("rsring_filter_damage"));
                                            displayNbt.removeTag("rsring_filter_damage");
                                        }
                                        if (displayNbt.getSize() > 0) {
                                            display.setTagCompound(displayNbt);
                                        }
                                    }
                                    java.util.List<String> tooltip = display.getTooltip(this.mc.player, this.mc.gameSettings.advancedItemTooltips ? net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED : net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL);
                                    // 添加模组名称到tooltip（原版风格：灰色斜体）
                                    String modName = getModName(itemName);
                                    if (!modName.isEmpty()) {
                                        tooltip.add(TextFormatting.GRAY + TextFormatting.ITALIC.toString() + modName);
                                    }
                                    this.drawHoveringText(tooltip, mouseX, mouseY);
                                }
                            } catch (Exception e) {
                                this.drawHoveringText(java.util.Arrays.asList(itemName), mouseX, mouseY);
                            }
                        }
                    }
                    return;
                }
            }
        }
        
        // 过滤模式按钮 tooltip（左边竖排，与槽位左对齐再左移1px）
        int leftBtnX = SLOTX_START - 1;
        int filterModeBtnY = PAD / 2;
        if (isMouseOverFilterModeButton(relativeX, relativeY, leftBtnX, filterModeBtnY)) {
            if (!customAllowed) {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                tooltip.add(TextFormatting.RED + "Locked by config");
                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }

            long t = System.currentTimeMillis();
            int period = 2000;
            float hue = ((t % period) / (float) period) % 1.0f;

            java.util.List<String> tooltip = new java.util.ArrayList<>();
            String modeText = getFilterModeDisplayName(currentFilterMode);

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

            tooltip.add(modeColor + (isDestroyModeUI ? "[销毁] " : "") + modeText);
            tooltip.add(hintColor + "点击切换过滤方式");

            this.drawHoveringText(tooltip, mouseX, mouseY);
            return;
        }
        
        // 黑白名单按钮 tooltip（左边竖排，在过滤模式按钮下方）
        int whitelistBtnY = filterModeBtnY + SQ; // 与绘制位置一致
        if (isMouseOverButton(relativeX, relativeY, leftBtnX, whitelistBtnY)) {
            if (!customAllowed) {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                tooltip.add(TextFormatting.RED + "Locked by config");
                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }

            long t = System.currentTimeMillis();
            int period = 2000;
            float hue = ((t % period) / (float) period) % 1.0f;

            java.util.List<String> tooltip = new java.util.ArrayList<>();
            boolean isWhitelist = isDestroyModeUI ? capability.isDestroyWhitelistMode() : capability.isWhitelistMode();
            String mode = isWhitelist ? "白名单模式" : "黑名单模式";

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

            tooltip.add(modeColor + (isDestroyModeUI ? "[销毁] " : "") + mode);
            
            // 销毁模式下添加额外提示
            if (isDestroyModeUI) {
                // 检查是否强制白名单模式
                boolean whitelistOnly = com.rsring.config.RsRingConfig.destroyMode.whitelistOnly;
                if (whitelistOnly && !isWhitelist) {
                    tooltip.add(TextFormatting.RED + "⚠ 配置限制：仅允许白名单模式");
                    tooltip.add(TextFormatting.YELLOW + "黑名单模式可能误销毁贵重物品");
                } else if (!isWhitelist) {
                    tooltip.add(TextFormatting.YELLOW + "⚠ 警告：黑名单模式可能误销毁物品");
                }
            }
            
            tooltip.add(hintColor + "点击切换模式");

            this.drawHoveringText(tooltip, mouseX, mouseY);
            return;
        }
        
        // 销毁模式按钮 tooltip（位置与绘制一致）
        int destroyBtnX, destroyBtnY;
        if (currentFilterMode == FilterMode.ITEM || currentFilterMode == FilterMode.MOD) {
            destroyBtnX = leftBtnX + SQ; // 黑白名单右侧
            destroyBtnY = whitelistBtnY;
        } else {
            destroyBtnX = leftBtnX;
            destroyBtnY = whitelistBtnY + SQ; // 黑白名单下方
        }
        
        if (isMouseOverButton(relativeX, relativeY, destroyBtnX, destroyBtnY)) {
            if (!customAllowed) {
                this.drawHoveringText(java.util.Arrays.asList(TextFormatting.RED + "Locked by config"), mouseX, mouseY);
                return;
            }
            
            long t = System.currentTimeMillis();
            int period = 2000;
            float hue = ((t % period) / (float) period) % 1.0f;
            
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
            int hintColorIndex = (int)((hue + 0.5) * colors.length) % colors.length;
            
            java.util.List<String> tooltip = new java.util.ArrayList<>();
            if (isDestroyModeUI) {
                int secondColorIndex = (int)((hue + 0.25) * colors.length) % colors.length;
                tooltip.add(colors[colorIndex] + "退出销毁模式");
                tooltip.add(colors[secondColorIndex] + "返回吸收模式配置");
            } else {
                // 警示跑马灯颜色（红色系）
                int warningPeriod = 1000; // 更快的闪烁频率
                float warningHue = ((t % warningPeriod) / (float) warningPeriod) % 1.0f;
                TextFormatting[] warningColors = {
                    TextFormatting.DARK_RED,
                    TextFormatting.RED,
                    TextFormatting.GOLD,
                    TextFormatting.YELLOW
                };
                int warningColorIndex = (int)(warningHue * warningColors.length) % warningColors.length;
                int secondWarningIndex = (int)((warningHue + 0.25) * warningColors.length) % warningColors.length;
                
                tooltip.add(warningColors[warningColorIndex] + "⚠ 销毁模式");
                tooltip.add(warningColors[secondWarningIndex] + "警告：匹配的物品将被永久销毁！");
                int thirdWarningIndex = (int)((warningHue + 0.5) * warningColors.length) % warningColors.length;
                tooltip.add(warningColors[thirdWarningIndex] + "" + TextFormatting.ITALIC + "建议使用白名单模式防止误销毁");
            }
            tooltip.add("");
            tooltip.add(colors[hintColorIndex] + "" + TextFormatting.ITALIC + "点击" + (isDestroyModeUI ? "退出" : "进入"));
            this.drawHoveringText(tooltip, mouseX, mouseY);
            return;
        }
        
        // 销毁开关按钮 tooltip（仅在销毁模式UI下显示）
        if (isDestroyModeUI) {
            int toggleBtnX, toggleBtnY;
            if (currentFilterMode == FilterMode.ITEM || currentFilterMode == FilterMode.MOD) {
                toggleBtnX = destroyBtnX + SQ + 1; // 销毁按钮右侧，与绘制一致
                toggleBtnY = destroyBtnY;
            } else {
                toggleBtnX = destroyBtnX;
                toggleBtnY = destroyBtnY + SQ + 1; // 销毁按钮下方，与绘制一致
            }
            
            if (isMouseOverButton(relativeX, relativeY, toggleBtnX, toggleBtnY)) {
                if (!customAllowed) {
                    this.drawHoveringText(java.util.Arrays.asList(TextFormatting.RED + "Locked by config"), mouseX, mouseY);
                    return;
                }
                
                long t = System.currentTimeMillis();
                int period = 2000;
                float hue = ((t % period) / (float) period) % 1.0f;
                
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
                
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                boolean enabled = capability.isDestroyEnabled();
                int descColorIndex1 = (int)((hue + 0.25) * colors.length) % colors.length;
                int descColorIndex2 = (int)((hue + 0.4) * colors.length) % colors.length;
                tooltip.add(colors[colorIndex] + "销毁功能");
                tooltip.add(colors[descColorIndex1] + "开启后：匹配的物品将被销毁");
                tooltip.add(colors[descColorIndex2] + "关闭后：物品不会被销毁");
                tooltip.add("");
                tooltip.add(enabled ? colors[colorIndex] + "当前：开启" : TextFormatting.RED + "当前：关闭");
                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
            
            // 销毁模式类型按钮 tooltip
            // 只有玩家有背包时，才显示销毁模式类型按钮tooltip
            boolean showDestroyModeType = com.rsring.compat.CompatManager.hasAnyBackpack(this.mc.player);
            if (showDestroyModeType) {
                // 所有过滤模式下，销毁类型按钮都在开关按钮右侧
                int modeTypeBtnX = toggleBtnX + SQ + 1;
                int modeTypeBtnY = toggleBtnY;
                
                if (isMouseOverButton(relativeX, relativeY, modeTypeBtnX, modeTypeBtnY)) {
                    if (!customAllowed) {
                        this.drawHoveringText(java.util.Arrays.asList(TextFormatting.RED + "Locked by config"), mouseX, mouseY);
                        return;
                    }
                    
                    // 跑马灯颜色
                    long t = System.currentTimeMillis();
                    int period = 2000;
                    float hue = ((t % period) / (float) period) % 1.0f;
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
                    int titleColorIndex = (int)(hue * colors.length) % colors.length;
                    int descColorIndex1 = (int)((hue + 0.25) * colors.length) % colors.length;
                    int descColorIndex2 = (int)((hue + 0.5) * colors.length) % colors.length;
                    
                    java.util.List<String> tooltip = new java.util.ArrayList<>();
                    com.rsring.capability.DestroyModeType modeType = capability.getDestroyModeType();
                    
                    // 只显示当前模式
                    String modeName;
                    String modeDesc;
                    switch (modeType) {
                        case SLOT_OVERFLOW:
                            modeName = "槽位溢出";
                            modeDesc = "背包有整组时销毁过量的";
                            break;
                        case STORAGE_OVERFLOW:
                            modeName = "存储溢出";
                            modeDesc = "背包满时销毁新物品";
                            break;
                        case ALWAYS:
                        default:
                            modeName = "总是销毁";
                            modeDesc = "直接销毁符合条件的物品";
                            break;
                    }
                    
                    int hintColorIndex = (int)((hue + 0.75) * colors.length) % colors.length;
                    
                    tooltip.add(colors[titleColorIndex] + "销毁模式类型->进入垃圾箱");
                    tooltip.add(colors[descColorIndex1] + modeName);
                    tooltip.add(colors[descColorIndex2] + " " + modeDesc);
                    tooltip.add("");
                    tooltip.add(colors[hintColorIndex] + "[点击切换模式]");
                    
                    this.drawHoveringText(tooltip, mouseX, mouseY);
                    return;
                }
            }
        }
        
        // NBT和耐久匹配按钮tooltip（物品过滤和模组过滤模式，右边竖排）
        if (currentFilterMode == FilterMode.ITEM || currentFilterMode == FilterMode.MOD) {
            int rightBtnX = this.xSize - PAD - TOGGLE_BTN_WIDTH + 1; // 向右移动1px
            int nbtBtnY = filterModeBtnY;
            int durabilityBtnY = whitelistBtnY;
            
            // 跑马灯颜色
            long t = System.currentTimeMillis();
            int period = 2000;
            float hue = ((t % period) / (float) period) % 1.0f;
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
            int titleColorIndex = (int)(hue * colors.length) % colors.length;
            int hintColorIndex = (int)((hue + 0.5) * colors.length) % colors.length;
            
            boolean currentMatchNbt = isDestroyModeUI ? capability.shouldDestroyMatchNbt() : capability.shouldMatchNbt();
            boolean currentMatchDurability = isDestroyModeUI ? capability.shouldDestroyMatchDurability() : capability.shouldMatchDurability();
            
            // NBT匹配按钮tooltip - 跑马灯效果
            if (isMouseOverButton(relativeX, relativeY, rightBtnX, nbtBtnY)) {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                int descColorIndex1 = (int)((hue + 0.25) * colors.length) % colors.length;
                int descColorIndex2 = (int)((hue + 0.4) * colors.length) % colors.length;
                tooltip.add(colors[titleColorIndex] + (isDestroyModeUI ? "[销毁] " : "") + "NBT匹配");
                tooltip.add(colors[descColorIndex1] + "开启后：过滤时匹配物品NBT数据");
                String modeHint = currentFilterMode == FilterMode.MOD ? "模组ID" : "物品ID";
                tooltip.add(colors[descColorIndex2] + "关闭后：仅匹配" + modeHint);
                tooltip.add("");
                tooltip.add(currentMatchNbt ? colors[hintColorIndex] + "当前：开启" : TextFormatting.RED + "当前：关闭");
                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
            
            // 耐久匹配按钮tooltip - 跑马灯效果
            if (isMouseOverButton(relativeX, relativeY, rightBtnX, durabilityBtnY)) {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                int descColorIndex1 = (int)((hue + 0.25) * colors.length) % colors.length;
                int descColorIndex2 = (int)((hue + 0.4) * colors.length) % colors.length;
                tooltip.add(colors[titleColorIndex] + (isDestroyModeUI ? "[销毁] " : "") + "耐久匹配");
                tooltip.add(colors[descColorIndex1] + "开启后：过滤时匹配物品耐久度");
                tooltip.add(colors[descColorIndex2] + "关闭后：忽略耐久度差异");
                tooltip.add("");
                tooltip.add(currentMatchDurability ? colors[hintColorIndex] + "当前：开启" : TextFormatting.RED + "当前：关闭");
                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
        }
        
        // 属性过滤模式下，显示加号、减号、匹配模式按钮的tooltip
        if (currentFilterMode == FilterMode.ATTRIBUTE) {
            int boxHeight = 47;
            int boxY = PAD / 2;
            int controlY = boxY + boxHeight + 6;
            int btnDrawY = controlY - 1;
            
            // 减号按钮（移除属性）tooltip - 跑马灯效果
            int removeBtnX = SLOTX_START + 5 * SQ - 1;
            if (isMouseOverButton(relativeX, relativeY, removeBtnX, btnDrawY)) {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                
                // 跑马灯颜色
                long t = System.currentTimeMillis();
                int period = 2000;
                float hue = ((t % period) / (float) period) % 1.0f;
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
                int titleColorIndex = (int)(hue * colors.length) % colors.length;
                int hintColorIndex = (int)((hue + 0.5) * colors.length) % colors.length;
                
                tooltip.add(colors[titleColorIndex] + "移除属性");
                if (currentAttributes.isEmpty()) {
                    tooltip.add(colors[hintColorIndex] + "" + TextFormatting.ITALIC + "没有可移除的属性");
                } else {
                    // 显示已添加的属性列表，当前选中项用跑马灯颜色标记
                    for (int i = 0; i < currentAttributes.size(); i++) {
                        Pair<ItemAttribute, Boolean> attrPair = currentAttributes.get(i);
                        String attrText = attrPair.getKey().format(attrPair.getValue()).getUnformattedText();
                        int attrColorIndex = (int)((hue + i * 0.1) * colors.length) % colors.length;
                        if (i == selectedRemoveIndex) {
                            tooltip.add(colors[attrColorIndex] + "-> " + attrText);
                        } else {
                            tooltip.add(TextFormatting.GRAY + "> " + attrText);
                        }
                    }
                    tooltip.add(colors[hintColorIndex] + "" + TextFormatting.ITALIC + "滚轮选择，左键移除，右键反转");
                }
                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
            
            // 加号按钮（添加属性）tooltip - 参考机械动力 SelectionScrollInput 实现
            int addBtnX = SLOTX_START + 6 * SQ - 1;
            if (isMouseOverButton(relativeX, relativeY, addBtnX, btnDrawY)) {
                isHoveringAddButton = true;
                
                boolean isDestroyModeUIForTooltip = capability != null && capability.isDestroyModeUI();
                ItemStack inputStackForTooltip = isDestroyModeUIForTooltip ? capability.getDestroyAttributeInputStack() : capability.getAttributeInputStack();
                
                if (inputStackForTooltip.isEmpty()) {
                    // 槽位为空时的提示 - 带跑马灯效果
                    java.util.List<String> tooltip = new java.util.ArrayList<>();
                    
                    // 跑马灯颜色
                    long t = System.currentTimeMillis();
                    int period = 2000;
                    float hue = ((t % period) / (float) period) % 1.0f;
                    TextFormatting[] colors = {
                        TextFormatting.RED,
                        TextFormatting.GOLD,
                        TextFormatting.YELLOW,
                        TextFormatting.GREEN,
                        TextFormatting.AQUA,
                        TextFormatting.BLUE,
                        TextFormatting.LIGHT_PURPLE,
                        TextFormatting.DARK_PURPLE
                    };
                    int titleColorIndex = (int)(hue * colors.length) % colors.length;
                    int hintColorIndex = (int)((hue + 0.5) * colors.length) % colors.length;
                    
                    tooltip.add(colors[titleColorIndex] + "添加属性");
                    tooltip.add(colors[hintColorIndex] + "" + TextFormatting.ITALIC + "在槽位放入物品");
                    this.drawHoveringText(tooltip, mouseX, mouseY);
                } else if (availableAttributes.isEmpty()) {
                    // 没有可添加属性时的提示 - 带跑马灯效果
                    java.util.List<String> tooltip = new java.util.ArrayList<>();
                    
                    // 跑马灯颜色
                    long t = System.currentTimeMillis();
                    int period = 2000;
                    float hue = ((t % period) / (float) period) % 1.0f;
                    TextFormatting[] colors = {
                        TextFormatting.RED,
                        TextFormatting.GOLD,
                        TextFormatting.YELLOW,
                        TextFormatting.GREEN,
                        TextFormatting.AQUA,
                        TextFormatting.BLUE,
                        TextFormatting.LIGHT_PURPLE,
                        TextFormatting.DARK_PURPLE
                    };
                    int titleColorIndex = (int)(hue * colors.length) % colors.length;
                    int hintColorIndex = (int)((hue + 0.5) * colors.length) % colors.length;
                    
                    tooltip.add(colors[titleColorIndex] + "添加属性");
                    tooltip.add(colors[hintColorIndex] + "" + TextFormatting.ITALIC + "没有更多可添加的属性");
                    this.drawHoveringText(tooltip, mouseX, mouseY);
                } else if (availableAttributes.size() > MAX_TOOLTIP_ITEMS) {
                    // 属性过多时使用可滚动tooltip（参考机械动力）
                    drawScrollableAttributeTooltip(mouseX, mouseY, availableAttributes, 
                            selectedAttributeIndex, "添加属性");
                } else {
                    // 属性较少时使用原样式（带跑马灯效果）
                    java.util.List<String> tooltip = new java.util.ArrayList<>();
                    
                    // 跑马灯颜色
                    long t = System.currentTimeMillis();
                    int period = 2000;
                    float hue = ((t % period) / (float) period) % 1.0f;
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
                    int titleColorIndex = (int)(hue * colors.length) % colors.length;
                    int hintColorIndex = (int)((hue + 0.5) * colors.length) % colors.length;
                    
                    tooltip.add(colors[titleColorIndex] + "添加属性");
                    
                    // 显示可添加的属性列表，当前选中项用跑马灯颜色标记
                    for (int i = 0; i < availableAttributes.size(); i++) {
                        ItemAttribute attr = availableAttributes.get(i);
                        String attrText = attr.format(false).getUnformattedText();
                        int attrColorIndex = (int)((hue + i * 0.1) * colors.length) % colors.length;
                        if (i == selectedAttributeIndex) {
                            tooltip.add(colors[attrColorIndex] + "-> " + attrText);
                        } else {
                            tooltip.add(TextFormatting.GRAY + "> " + attrText);
                        }
                    }
                    tooltip.add(colors[hintColorIndex] + "" + TextFormatting.ITALIC + "滚轮选择，左键添加");
                    this.drawHoveringText(tooltip, mouseX, mouseY);
                }
                return;
            } else {
                isHoveringAddButton = false;
            }
            
            // 匹配模式按钮tooltip - 跑马灯效果
            int matchBtnX = SLOTX_START + 7 * SQ - 1;
            if (isMouseOverButton(relativeX, relativeY, matchBtnX, btnDrawY)) {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                
                // 跑马灯颜色
                long t = System.currentTimeMillis();
                int period = 2000;
                float hue = ((t % period) / (float) period) % 1.0f;
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
                int titleColorIndex = (int)(hue * colors.length) % colors.length;
                int hintColorIndex = (int)((hue + 0.5) * colors.length) % colors.length;

                boolean isDestroyModeUIForMatch = capability.isDestroyModeUI();
                boolean matchAll = isDestroyModeUIForMatch ? capability.isDestroyMatchAllMode() : capability.isMatchAllMode();
                tooltip.add(colors[titleColorIndex] + (matchAll ? "匹配全部 (AND)" : "匹配任意 (OR)"));
                int descColorIndex = (int)((hue + 0.25) * colors.length) % colors.length;
                tooltip.add(colors[descColorIndex] + (matchAll ? "物品必须满足所有属性" : "物品满足任一属性即可"));
                tooltip.add("");
                tooltip.add(colors[hintColorIndex] + "" + TextFormatting.ITALIC + "点击切换");
                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    private String getFilterModeDisplayName(FilterMode mode) {
        switch (mode) {
            case ITEM:
                return "物品 ID 过滤";
            case MOD:
                return "模组过滤";
            case ATTRIBUTE:
                return "属性过滤";
            default:
                return "物品 ID 过滤";
        }
    }

    /**
     * 从物品注册名获取模组名称
     */
    private String getModName(String itemRegistryName) {
        if (itemRegistryName == null || itemRegistryName.isEmpty()) {
            return "";
        }
        try {
            int colonIndex = itemRegistryName.indexOf(':');
            if (colonIndex > 0) {
                String modId = itemRegistryName.substring(0, colonIndex);
                // 尝试获取模组的友好名称
                net.minecraftforge.fml.common.ModContainer mod = net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(modId);
                if (mod != null) {
                    return mod.getName();
                }
                return modId;
            }
        } catch (Exception ignored) {}
        return "";
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        refreshCapability();
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (capability == null) return;
        
        int relativeX = mouseX - this.guiLeft;
        int relativeY = mouseY - this.guiTop;
        
        // 获取销毁模式UI状态
        boolean isDestroyModeUI = capability.isDestroyModeUI();
        FilterMode currentFilterMode = isDestroyModeUI ? capability.getDestroyFilterMode() : capability.getFilterMode();
        
        // 处理过滤模式按钮点击（左边竖排，与槽位左对齐再左移1px）
        int leftBtnX = SLOTX_START - 1;
        int filterModeBtnY = PAD / 2;
        if (isMouseOverFilterModeButton(relativeX, relativeY, leftBtnX, filterModeBtnY)) {
            if (!isCustomFiltersAllowed()) {
                return;
            }
            // 检查点击冷却时间
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastButtonClickTime < CLICK_COOLDOWN) {
                return; // 冷却中，忽略点击
            }
            lastButtonClickTime = currentTime; // 记录点击时间
            
            // 根据销毁模式UI状态切换对应的过滤模式
            FilterMode newMode = currentFilterMode.next();
            if (isDestroyModeUI) {
                capability.setDestroyFilterMode(newMode);
            } else {
                capability.setFilterMode(newMode);
            }
            String newModeText = getFilterModeDisplayName(newMode);

            this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                net.minecraft.util.text.TextFormatting.GOLD + (isDestroyModeUI ? "[销毁] " : "") + "已切换过滤方式：" +
                net.minecraft.util.text.TextFormatting.AQUA + newModeText
            ));
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
            
            // 构建完整的数据包 - 使用正确的过滤槽
            String[] itemSlots = new String[9];
            String[] modSlots = new String[9];
            for (int i = 0; i < 9; i++) {
                itemSlots[i] = isDestroyModeUI ? capability.getDestroyFilterSlot(i) : capability.getFilterSlot(i);
            }
            List<String> mods = isDestroyModeUI ? capability.getDestroyFilterMods() : capability.getFilterMods();
            for (int i = 0; i < Math.min(9, mods.size()); i++) {
                modSlots[i] = mods.get(i);
            }
            List<Pair<ItemAttribute, Boolean>> attrs = isDestroyModeUI ? 
                capability.getDestroyFilterAttributes() : capability.getFilterAttributes();
            
            com.rsring.rsring.RsRingMod.network.sendToServer(
                new com.rsring.network.PacketSyncAdvancedFilter(
                    isDestroyModeUI ? capability.getDestroyFilterMode() : capability.getFilterMode(),
                    isDestroyModeUI ? capability.isDestroyWhitelistMode() : capability.isWhitelistMode(),
                    isDestroyModeUI ? capability.isDestroyMatchAllMode() : capability.isMatchAllMode(),
                    itemSlots,
                    modSlots,
                    attrs,
                    isDestroyModeUI ? capability.shouldDestroyMatchNbt() : capability.shouldMatchNbt(),
                    isDestroyModeUI ? capability.shouldDestroyMatchDurability() : capability.shouldMatchDurability(),
                    isDestroyModeUI
                ));
            return;
        }
        
        // 处理黑白名单按钮点击（左边竖排，在过滤模式按钮下方）
        int whitelistBtnY = filterModeBtnY + SQ; // 与绘制时一致
        if (isMouseOverButton(relativeX, relativeY, leftBtnX, whitelistBtnY)) {
            if (!isCustomFiltersAllowed()) {
                return;
            }
            // 检查点击冷却时间
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastButtonClickTime < CLICK_COOLDOWN) {
                return; // 冷却中，忽略点击
            }
            lastButtonClickTime = currentTime; // 记录点击时间
            
            // 根据销毁模式UI状态切换对应的黑白名单模式
            if (isDestroyModeUI) {
                boolean oldMode = capability.isDestroyWhitelistMode();
                boolean newMode = !oldMode;
                
                // 检查是否强制白名单模式
                boolean whitelistOnly = com.rsring.config.RsRingConfig.destroyMode.whitelistOnly;
                if (whitelistOnly && !newMode) {
                    // 尝试切换到黑名单模式，但被配置阻止
                    this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        TextFormatting.RED + "[销毁] 配置限制：仅允许使用白名单模式"));
                    return;
                }
                
                capability.setDestroyWhitelistMode(newMode);
                String newModeText = newMode ? "白名单" : "黑名单";
                this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.GOLD + "[销毁] 已切换过滤模式: " +
                    net.minecraft.util.text.TextFormatting.AQUA + newModeText
                ));
            } else {
                boolean oldMode = capability.isWhitelistMode();
                capability.setWhitelistMode(!oldMode);
                String newModeText = capability.isWhitelistMode() ? "白名单" : "黑名单";
                this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.GOLD + "已切换过滤模式: " +
                    net.minecraft.util.text.TextFormatting.AQUA + newModeText
                ));
            }
RsRingCapability.syncCapabilityToStack(ringStack, capability);
            String[] slots = new String[SLOT_COUNT];
            for (int i = 0; i < SLOT_COUNT; i++) {
                slots[i] = isDestroyModeUI ? capability.getDestroyFilterSlot(i) : capability.getFilterSlot(i);
            }
            com.rsring.rsring.RsRingMod.network.sendToServer(
                new com.rsring.network.PacketSyncRingFilter(
                    isDestroyModeUI ? capability.isDestroyWhitelistMode() : capability.isWhitelistMode(),
                    slots,
                    isDestroyModeUI));
            return;
        }
        
        // 处理销毁模式按钮点击
        int destroyBtnX, destroyBtnY;
        if (currentFilterMode == FilterMode.ITEM || currentFilterMode == FilterMode.MOD) {
            destroyBtnX = leftBtnX + SQ; // 黑白名单右侧
            destroyBtnY = whitelistBtnY;
        } else { // ATTRIBUTE
            destroyBtnX = leftBtnX;
            destroyBtnY = whitelistBtnY + SQ; // 黑白名单下方
        }
        
        if (isMouseOverButton(relativeX, relativeY, destroyBtnX, destroyBtnY)) {
            if (!isCustomFiltersAllowed()) return;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastButtonClickTime < CLICK_COOLDOWN) return;
            lastButtonClickTime = currentTime;
            
            // 切换销毁模式UI状态
            capability.setDestroyModeUI(!isDestroyModeUI);
            String msg = capability.isDestroyModeUI() ? 
                "进入销毁模式配置" : "退出销毁模式配置";
            this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.GOLD + msg));
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
            // 刷新属性列表引用，确保切换后使用正确的数据
            refreshAttributeLists();
            return;
        }
        
        // 处理销毁开关按钮点击（仅在销毁模式UI下显示）
        if (isDestroyModeUI) {
            int toggleBtnX, toggleBtnY;
            if (currentFilterMode == FilterMode.ITEM || currentFilterMode == FilterMode.MOD) {
                toggleBtnX = destroyBtnX + SQ + 1; // 销毁按钮右侧，与绘制位置一致
                toggleBtnY = destroyBtnY;
            } else { // ATTRIBUTE
                toggleBtnX = destroyBtnX;
                toggleBtnY = destroyBtnY + SQ + 1; // 销毁按钮下方，与绘制位置一致
            }
            
            if (isMouseOverButton(relativeX, relativeY, toggleBtnX, toggleBtnY)) {
                if (!isCustomFiltersAllowed()) return;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastButtonClickTime < CLICK_COOLDOWN) return;
                lastButtonClickTime = currentTime;
                
                boolean newEnabled = !capability.isDestroyEnabled();
                capability.setDestroyEnabled(newEnabled);
                RsRingCapability.syncCapabilityToStack(ringStack, capability);
                // 发送数据包到服务器同步销毁开关状态
                com.rsring.rsring.RsRingMod.network.sendToServer(
                    new com.rsring.network.PacketSyncDestroyToggle(newEnabled));
                this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    TextFormatting.GOLD + "销毁功能: " + (newEnabled ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭")));
                return;
            }
            
            // 处理销毁模式类型切换按钮点击
            // 只有玩家有背包时，才处理销毁模式类型按钮点击
            boolean showDestroyModeType = com.rsring.compat.CompatManager.hasAnyBackpack(this.mc.player);
            if (showDestroyModeType) {
                // 所有过滤模式下，销毁类型按钮都在开关按钮右侧
                int modeTypeBtnX = toggleBtnX + SQ + 1;
                int modeTypeBtnY = toggleBtnY;
                
                if (isMouseOverButton(relativeX, relativeY, modeTypeBtnX, modeTypeBtnY)) {
                    if (!isCustomFiltersAllowed()) return;
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastButtonClickTime < CLICK_COOLDOWN) return;
                    lastButtonClickTime = currentTime;
                    
                    com.rsring.capability.DestroyModeType currentType = capability.getDestroyModeType();
                    com.rsring.capability.DestroyModeType newType = currentType.next();
                    capability.setDestroyModeType(newType);
                    RsRingCapability.syncCapabilityToStack(ringStack, capability);
                    // 发送数据包到服务器同步销毁模式类型
                    com.rsring.rsring.RsRingMod.network.sendToServer(
                        new com.rsring.network.PacketSyncDestroyModeType(newType));
                    
                    String typeName;
                    switch (newType) {
                        case SLOT_OVERFLOW:
                            typeName = "槽位溢出销毁";
                            break;
                        case STORAGE_OVERFLOW:
                            typeName = "存储溢出销毁";
                            break;
                        case ALWAYS:
                        default:
                            typeName = "总是销毁";
                            break;
                    }
                    this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        TextFormatting.GOLD + "销毁模式: " + TextFormatting.AQUA + typeName));
                    return;
                }
            }
        }
        
        // 处理NBT和耐久匹配按钮点击（物品过滤和模组过滤模式，右边竖排）
        if (currentFilterMode == FilterMode.ITEM || currentFilterMode == FilterMode.MOD) {
            int rightBtnX = this.xSize - PAD - TOGGLE_BTN_WIDTH + 1; // 向右移动1px
            int nbtBtnY = filterModeBtnY;
            int durabilityBtnY = whitelistBtnY;
            
            // NBT匹配按钮
            if (isMouseOverButton(relativeX, relativeY, rightBtnX, nbtBtnY)) {
                if (!isCustomFiltersAllowed()) return;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastButtonClickTime < CLICK_COOLDOWN) return;
                lastButtonClickTime = currentTime;
                
                boolean newNbt;
                if (isDestroyModeUI) {
                    newNbt = !capability.shouldDestroyMatchNbt();
                    capability.setDestroyMatchNbt(newNbt);
                } else {
                    newNbt = !capability.shouldMatchNbt();
                    capability.setMatchNbt(newNbt);
                }
                RsRingCapability.syncCapabilityToStack(ringStack, capability);
                syncAttributesToServer();
                this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    TextFormatting.GOLD + (isDestroyModeUI ? "[销毁] " : "") + "NBT匹配: " + (newNbt ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭")));
                return;
            }
            
            // 耐久匹配按钮
            if (isMouseOverButton(relativeX, relativeY, rightBtnX, durabilityBtnY)) {
                if (!isCustomFiltersAllowed()) return;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastButtonClickTime < CLICK_COOLDOWN) return;
                lastButtonClickTime = currentTime;
                
                boolean newDurability;
                if (isDestroyModeUI) {
                    newDurability = !capability.shouldDestroyMatchDurability();
                    capability.setDestroyMatchDurability(newDurability);
                } else {
                    newDurability = !capability.shouldMatchDurability();
                    capability.setMatchDurability(newDurability);
                }
                RsRingCapability.syncCapabilityToStack(ringStack, capability);
                syncAttributesToServer();
                this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    TextFormatting.GOLD + (isDestroyModeUI ? "[销毁] " : "") + "耐久匹配: " + (newDurability ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭")));
                return;
            }
        }
        
        // 处理属性过滤模式的按钮点击
        if (currentFilterMode == FilterMode.ATTRIBUTE) {
            if (handleAttributeButtonClick(mouseX, mouseY, mouseButton)) {
                return;
            }
            // 处理属性输入槽位点击
            // 槽位相对坐标（与绘制时一致）
            int slotRelativeX = SLOTX_START + 8 * SQ - 1; // 与绘制时一致有-1偏移
            int slotRelativeY = PAD / 2 + 45 + 6 - 1; // boxY(=PAD/2) + boxHeight(=45) + 6 - 1
            if (isPointInRegion(slotRelativeX, slotRelativeY, SQ, SQ, mouseX, mouseY)) {
                handleAttributeSlotClick();
                return;
            }
        } else {
            // 非属性过滤模式，处理9个槽位
            for (int i = 0; i < SLOT_COUNT; i++) {
                int slotX = SLOTX_START + i * SQ;
                int slotY = SLOTY;
                if (isPointInRegion(slotX, slotY, SQ - 2, SQ - 2, mouseX, mouseY)) {
                    mouseClickedWrapper(i);
                    return;
                }
            }
        }
    }
    
    /**
     * 处理属性过滤模式的按钮点击
     * @param mouseX 绝对鼠标X坐标
     * @param mouseY 绝对鼠标Y坐标
     * @param mouseButton 鼠标按钮（0=左键，1=右键）
     */
    private boolean handleAttributeButtonClick(int mouseX, int mouseY, int mouseButton) {
        // 按钮位置 - 与绘制时一致，四个控件并排对齐
        int boxHeight = 47; // 与绘制时一致
        int boxY = this.guiTop + PAD / 2;
        int controlY = boxY + boxHeight + 6;
        int btnDrawY = controlY - 1; // 与绘制时一致
        
        // 获取销毁模式UI状态
        boolean isDestroyModeUI = capability != null && capability.isDestroyModeUI();

        // 检查点击冷却
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastButtonClickTime < CLICK_COOLDOWN) {
            return false;
        }

        refreshAttributeLists();

        // 减号按钮（移除属性）- 绘制位置有-1偏移
        // 左键移除属性，右键反转属性状态
        int removeBtnX = this.guiLeft + SLOTX_START + 5 * SQ - 1;
        if (isMouseOverAttributeButton(mouseX, mouseY, removeBtnX, btnDrawY)) {
            lastButtonClickTime = currentTime;
            if (!currentAttributes.isEmpty() && selectedRemoveIndex >= 0 && selectedRemoveIndex < currentAttributes.size()) {
                if (mouseButton == 1) {
                    // 右键：反转属性状态（在原位置更新）
                    Pair<ItemAttribute, Boolean> current = currentAttributes.get(selectedRemoveIndex);
                    boolean newInverted = !current.getValue();
                    if (isDestroyModeUI) {
                        capability.setDestroyFilterAttributeInverted(selectedRemoveIndex, newInverted);
                    } else {
                        capability.setFilterAttributeInverted(selectedRemoveIndex, newInverted);
                    }
                    RsRingCapability.syncCapabilityToStack(ringStack, capability);
                    refreshAttributeLists();
                    syncAttributesToServer();
                    this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        TextFormatting.YELLOW + "~ " + current.getKey().format(newInverted).getUnformattedText()));
                } else {
                    // 左键：移除属性
                    Pair<ItemAttribute, Boolean> removed = currentAttributes.get(selectedRemoveIndex);
                    if (isDestroyModeUI) {
                        capability.removeDestroyFilterAttribute(selectedRemoveIndex);
                    } else {
                        capability.removeFilterAttribute(selectedRemoveIndex);
                    }
                    RsRingCapability.syncCapabilityToStack(ringStack, capability);
                    refreshAttributeLists();
                    syncAttributesToServer();
                    this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        TextFormatting.RED + "- " + removed.getKey().format(removed.getValue()).getUnformattedText()));
                    // 调整选中索引
                    if (selectedRemoveIndex >= currentAttributes.size() && selectedRemoveIndex > 0) {
                        selectedRemoveIndex--;
                    }
                }
            }
            return true;
        }
        
        // 加号按钮（添加属性）- 绘制位置有-1偏移
        // 左键添加属性（已存在的属性不会出现在可选列表中）
        int addBtnX = this.guiLeft + SLOTX_START + 6 * SQ - 1;
        if (isMouseOverAttributeButton(mouseX, mouseY, addBtnX, btnDrawY) && mouseButton == 0) {
            lastButtonClickTime = currentTime;
            if (!availableAttributes.isEmpty() && selectedAttributeIndex >= 0 && selectedAttributeIndex < availableAttributes.size()) {
                ItemAttribute attr = availableAttributes.get(selectedAttributeIndex);
                if (isDestroyModeUI) {
                    capability.addDestroyFilterAttribute(attr, false);
                } else {
                    capability.addFilterAttribute(attr, false);
                }
                RsRingCapability.syncCapabilityToStack(ringStack, capability);
                refreshAttributeLists();
                syncAttributesToServer();
                this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    TextFormatting.GREEN + "+ " + attr.format(false).getUnformattedText()));
            }
            return true;
        }

        // 匹配全部/匹配任意按钮 - 绘制位置有-1偏移
        int matchBtnX = this.guiLeft + SLOTX_START + 7 * SQ - 1;
        if (isMouseOverAttributeButton(mouseX, mouseY, matchBtnX, btnDrawY)) {
            lastButtonClickTime = currentTime;
            boolean newMatchAll;
            if (isDestroyModeUI) {
                newMatchAll = !capability.isDestroyMatchAllMode();
                capability.setDestroyMatchAllMode(newMatchAll);
            } else {
                newMatchAll = !capability.isMatchAllMode();
                capability.setMatchAllMode(newMatchAll);
            }
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
            syncAttributesToServer();
            this.mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.GOLD + (isDestroyModeUI ? "[销毁] " : "") + "匹配模式: " + (newMatchAll ? "全部(AND)" : "任意(OR)")));
            return true;
        }
        
        return false;
    }
    
    private void syncAttributesToServer() {
        boolean isDestroyModeUI = capability.isDestroyModeUI();
        
        String[] itemSlots = new String[9];
        String[] modSlots = new String[9];
        for (int i = 0; i < 9; i++) {
            itemSlots[i] = isDestroyModeUI ? capability.getDestroyFilterSlot(i) : capability.getFilterSlot(i);
        }
        List<String> mods = isDestroyModeUI ? capability.getDestroyFilterMods() : capability.getFilterMods();
        for (int i = 0; i < Math.min(9, mods.size()); i++) {
            modSlots[i] = mods.get(i);
        }
        List<Pair<ItemAttribute, Boolean>> attrs = isDestroyModeUI ? 
            capability.getDestroyFilterAttributes() : capability.getFilterAttributes();
        
        com.rsring.rsring.RsRingMod.network.sendToServer(
            new com.rsring.network.PacketSyncAdvancedFilter(
                isDestroyModeUI ? capability.getDestroyFilterMode() : capability.getFilterMode(),
                isDestroyModeUI ? capability.isDestroyWhitelistMode() : capability.isWhitelistMode(),
                isDestroyModeUI ? capability.isDestroyMatchAllMode() : capability.isMatchAllMode(),
                itemSlots,
                modSlots,
                attrs,
                isDestroyModeUI ? capability.shouldDestroyMatchNbt() : capability.shouldMatchNbt(),
                isDestroyModeUI ? capability.shouldDestroyMatchDurability() : capability.shouldMatchDurability(),
                isDestroyModeUI
            ));
    }

    private void mouseClickedWrapper(int slotIndex) {
        if (capability == null) return;
        if (!isCustomFiltersAllowed()) return;
        ItemStack stackInMouse = this.mc.player.inventory.getItemStack();
        
        // 根据销毁模式UI状态使用对应的槽位
        boolean isDestroyModeUI = capability.isDestroyModeUI();
        FilterMode currentMode = isDestroyModeUI ? capability.getDestroyFilterMode() : capability.getFilterMode();
        
        if (currentMode == FilterMode.MOD) {
            // 模组过滤模式：使用模组过滤槽位
            // 存储完整物品ID（modId:itemName格式），以便显示物品图标
            String currentMod = isDestroyModeUI ? capability.getDestroyModFilterSlot(slotIndex) : capability.getModFilterSlot(slotIndex);
            if (stackInMouse.isEmpty() && (currentMod == null || currentMod.isEmpty())) {
                return;
            }
            
            if (stackInMouse.isEmpty()) {
                // 清除槽位和NBT数据
                if (isDestroyModeUI) {
                    capability.setDestroyModFilterSlot(slotIndex, "");
                    capability.setDestroyModFilterSlotNBT(slotIndex, null);
                } else {
                    capability.setModFilterSlot(slotIndex, "");
                    capability.setModFilterSlotNBT(slotIndex, null);
                }
            } else {
                // 存储完整物品ID（格式：modId:itemName），方便显示物品图标
                // 过滤时提取模组ID使用
                String itemId = stackInMouse.getItem().getRegistryName() != null ?
                    stackInMouse.getItem().getRegistryName().toString() : "minecraft:stone";
                // 保存物品的NBT数据（用于NBT匹配）
                net.minecraft.nbt.NBTTagCompound itemNbt = stackInMouse.getTagCompound();
                if (isDestroyModeUI) {
                    capability.setDestroyModFilterSlot(slotIndex, itemId);
                    capability.setDestroyModFilterSlotNBT(slotIndex, itemNbt);
                } else {
                    capability.setModFilterSlot(slotIndex, itemId);
                    capability.setModFilterSlotNBT(slotIndex, itemNbt);
                }
            }
            
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
            // 同步模组过滤槽位
            String[] modSlots = new String[SLOT_COUNT];
            for (int j = 0; j < SLOT_COUNT; j++) {
                modSlots[j] = isDestroyModeUI ? capability.getDestroyModFilterSlot(j) : capability.getModFilterSlot(j);
            }
            com.rsring.rsring.RsRingMod.network.sendToServer(
                new com.rsring.network.PacketSyncModFilter(
                    modSlots,
                    isDestroyModeUI));
            // 同步NBT数据
            int slotType = isDestroyModeUI ? 3 : 1; // 3=销毁模组过滤, 1=模组过滤
            com.rsring.rsring.RsRingMod.network.sendToServer(
                new com.rsring.network.PacketSyncFilterNBT(
                    slotIndex,
                    stackInMouse.isEmpty() ? null : stackInMouse.getTagCompound(),
                    slotType));
        } else {
            // 物品ID过滤模式：使用物品过滤槽位
            String currentFilter = isDestroyModeUI ? capability.getDestroyFilterSlot(slotIndex) : capability.getFilterSlot(slotIndex);
            if (stackInMouse.isEmpty() && (currentFilter == null || currentFilter.isEmpty())) {
                return;
            }
            
            if (stackInMouse.isEmpty()) {
                // 清除槽位和NBT数据
                if (isDestroyModeUI) {
                    capability.setDestroyFilterSlot(slotIndex, "");
                    capability.setDestroyFilterSlotNBT(slotIndex, null);
                } else {
                    capability.setFilterSlot(slotIndex, "");
                    capability.setFilterSlotNBT(slotIndex, null);
                }
            } else {
                String name = stackInMouse.getItem().getRegistryName() != null ?
                    stackInMouse.getItem().getRegistryName().toString() : "";
                if (!name.isEmpty()) {
                    // 保存物品的NBT数据（用于NBT匹配）和耐久度
                    net.minecraft.nbt.NBTTagCompound itemNbt = stackInMouse.getTagCompound();
                    // 创建一个新的NBT来存储所有数据（包括耐久度）
                    net.minecraft.nbt.NBTTagCompound storageNbt = itemNbt != null ? itemNbt.copy() : new net.minecraft.nbt.NBTTagCompound();
                    // 存储耐久度（Damage）
                    int damage = stackInMouse.getItemDamage();
                    if (damage > 0) {
                        storageNbt.setInteger("rsring_filter_damage", damage);
                    }
                    if (isDestroyModeUI) {
                        capability.setDestroyFilterSlot(slotIndex, name);
                        capability.setDestroyFilterSlotNBT(slotIndex, storageNbt);
                    } else {
                        capability.setFilterSlot(slotIndex, name);
                        capability.setFilterSlotNBT(slotIndex, storageNbt);
                    }
                }
            }
            
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
            String[] slots = new String[SLOT_COUNT];
            for (int j = 0; j < SLOT_COUNT; j++) {
                slots[j] = isDestroyModeUI ? capability.getDestroyFilterSlot(j) : capability.getFilterSlot(j);
            }
            com.rsring.rsring.RsRingMod.network.sendToServer(
                new com.rsring.network.PacketSyncRingFilter(
                    isDestroyModeUI ? capability.isDestroyWhitelistMode() : capability.isWhitelistMode(),
                    slots,
                    isDestroyModeUI));
            // 同步NBT数据（包含耐久度）
            int slotType = isDestroyModeUI ? 2 : 0; // 2=销毁物品ID过滤, 0=物品ID过滤
            net.minecraft.nbt.NBTTagCompound syncNbt = null;
            if (!stackInMouse.isEmpty()) {
                // 创建包含耐久度的同步NBT
                syncNbt = stackInMouse.getTagCompound() != null ? stackInMouse.getTagCompound().copy() : new net.minecraft.nbt.NBTTagCompound();
                int damage = stackInMouse.getItemDamage();
                if (damage > 0) {
                    syncNbt.setInteger("rsring_filter_damage", damage);
                }
            }
            com.rsring.rsring.RsRingMod.network.sendToServer(
                new com.rsring.network.PacketSyncFilterNBT(
                    slotIndex,
                    syncNbt,
                    slotType));
        }
    }

    /**
     * 处理属性输入槽位的点击（独立槽位，不占用9格过滤槽）
     * 参考精妙背包的TagSelectionSlot实现：存储完整的ItemStack
     */
    private void handleAttributeSlotClick() {
        if (capability == null) return;
        if (!isCustomFiltersAllowed()) return;
        ItemStack stackInMouse = this.mc.player.inventory.getItemStack();
        
        // 根据销毁模式UI状态使用对应的槽位
        boolean isDestroyModeUI = capability.isDestroyModeUI();

        ItemStack currentStack = isDestroyModeUI ? capability.getDestroyAttributeInputStack() : capability.getAttributeInputStack();
        if (stackInMouse.isEmpty() && currentStack.isEmpty()) {
            return;
        }

        if (stackInMouse.isEmpty()) {
            // 空手点击，清空槽位
            if (isDestroyModeUI) {
                capability.clearDestroyAttributeInputSlot();
            } else {
                capability.clearAttributeInputSlot();
            }
        } else {
            // 有物品，存储完整的ItemStack（包括NBT）
            if (isDestroyModeUI) {
                capability.setDestroyAttributeInputStack(stackInMouse);
            } else {
                capability.setAttributeInputStack(stackInMouse);
            }
        }

        RsRingCapability.syncCapabilityToStack(ringStack, capability);
        // 同步到服务器
        syncAttributesToServer();
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
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        
        // 获取当前模式（考虑销毁模式UI状态）
        boolean isDestroyModeUI = capability != null && capability.isDestroyModeUI();
        FilterMode currentMode = isDestroyModeUI ? 
            (capability != null ? capability.getDestroyFilterMode() : FilterMode.ITEM) : 
            (capability != null ? capability.getFilterMode() : FilterMode.ITEM);
        
        // 处理属性过滤模式的鼠标滚轮
        if (capability != null && currentMode == FilterMode.ATTRIBUTE) {
            int scroll = org.lwjgl.input.Mouse.getEventDWheel();
            if (scroll != 0) {
                handleAttributeScroll(scroll);
            }
        }
    }
    
    private void handleAttributeScroll(int scroll) {
        // 黑框区域参数 - 与绘制时一致
        int boxHeight = 47;
        int boxY = this.guiTop + PAD / 2;
        int boxX = this.guiLeft + SLOTX_START + SQ + 4;
        int boxWidth = this.xSize - (SLOTX_START + SQ + 4) - 8;

        int mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        // 检查鼠标是否在黑框区域内 - 滚动属性列表
        if (mouseX >= boxX && mouseX < boxX + boxWidth && mouseY >= boxY && mouseY < boxY + boxHeight) {
            int maxDisplayCount = 5;  // 固定显示5行
            if (currentAttributes.size() > maxDisplayCount) {
                if (scroll > 0) {
                    attributeScrollOffset--;
                } else {
                    attributeScrollOffset++;
                }
                // 边界检查
                if (attributeScrollOffset < 0) attributeScrollOffset = 0;
                if (attributeScrollOffset > currentAttributes.size() - maxDisplayCount) {
                    attributeScrollOffset = currentAttributes.size() - maxDisplayCount;
                }
            }
            return;
        }

        // 按钮与属性过滤槽位同一行 - 与绘制时一致
        int controlY = boxY + boxHeight + 6;
        int btnDrawY = controlY - 1;

        // 检查鼠标是否在加号按钮上 - 绘制位置有-1偏移
        int addBtnX = this.guiLeft + SLOTX_START + 6 * SQ - 1;
        if (isMouseOverAttributeButton(mouseX, mouseY, addBtnX, btnDrawY)) {
            // 滚动选择可添加的属性
            if (!availableAttributes.isEmpty()) {
                if (scroll > 0) {
                    selectedAttributeIndex--;
                    if (selectedAttributeIndex < 0) {
                        selectedAttributeIndex = availableAttributes.size() - 1;
                    }
                } else {
                    selectedAttributeIndex++;
                    if (selectedAttributeIndex >= availableAttributes.size()) {
                        selectedAttributeIndex = 0;
                    }
                }
            }
        }

        // 检查鼠标是否在减号按钮上 - 绘制位置有-1偏移
        int removeBtnX = this.guiLeft + SLOTX_START + 5 * SQ - 1;
        if (isMouseOverAttributeButton(mouseX, mouseY, removeBtnX, btnDrawY)) {
            // 滚动选择要移除的属性
            if (!currentAttributes.isEmpty()) {
                if (scroll > 0) {
                    selectedRemoveIndex--;
                    if (selectedRemoveIndex < 0) {
                        selectedRemoveIndex = currentAttributes.size() - 1;
                    }
                } else {
                    selectedRemoveIndex++;
                    if (selectedRemoveIndex >= currentAttributes.size()) {
                        selectedRemoveIndex = 0;
                    }
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        // 关闭 GUI 时同步 capability 数据到物品 NBT
        if (capability != null && ringStack != null) {
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
