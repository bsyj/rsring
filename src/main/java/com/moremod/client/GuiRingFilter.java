package com.moremod.client;

import com.moremod.capability.IRsRingCapability;
import com.moremod.capability.RsRingCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

/**
 * 箱子戒指黑白名单过滤GUI
 * 参考Cyclic管道GUI设计：顶部8格过滤槽（仅匹配、不消耗），底部显示玩家背包
 */
@SideOnly(Side.CLIENT)
public class GuiRingFilter extends GuiScreen {

    // 参考 Cyclic 的常量定义
    private static final int SQ = 18; // Const.SQ - 槽位大小
    private static final int PAD = 8; // Const.PAD - 边距
    private static final int SLOT_COUNT = 9;
    private static final int TOGGLE_BTN_WIDTH = 20;
    private static final int TOGGLE_BTN_HEIGHT = 20;

    private static final int GUI_WIDTH = 176; // 标准GUI宽度 (Const.ScreenSize.STANDARD)
    private static final int GUI_HEIGHT = 166; // 标准GUI高度

    // 参考 Cyclic 的槽位布局
    private static final int SLOTX_START = PAD; // 过滤槽起始X (参考 ContainerItemPump.SLOTX_START)
    private static final int SLOTY = SQ + PAD * 4; // 过滤槽Y位置 (参考 ContainerItemPump.SLOTY = 18 + 32 = 50)
    
    // 纹理资源（参考 Cyclic 的 Const.Res）- 使用 Cyclic 的材质
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation("rsring", "textures/gui/table.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("rsring", "textures/gui/inventory_slot.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation("rsring", "textures/gui/buttons.png");
    private static final ResourceLocation VANILLA_BUTTON_TEXTURE = new ResourceLocation("minecraft", "textures/gui/widgets.png");

    private final ItemStack ringStack;
    private final String title;
    private IRsRingCapability capability;

    private int guiLeft;
    private int guiTop;

    public GuiRingFilter(ItemStack ringStack, String title) {
        this.ringStack = ringStack;
        this.title = title;
        this.capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
    }

    @Override
    public void initGui() {
        super.initGui();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;
        buttonList.clear();
    }

    private void refreshCapability() {
        if (capability == null) capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
    }

    private String getToggleButtonText() {
        if (capability == null) return "黑名单";
        return capability.isWhitelistMode() ? "白名单" : "黑名单";
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        refreshCapability();
        
        // 1. 绘制背景（参考Cyclic的renderBackground）
        drawDefaultBackground();
        
        // 2. 绘制GUI背景纹理（参考Cyclic的renderBg）
        drawGuiBackground();
        
        // 3. 绘制过滤槽位
        drawFilterSlots();
        
        // 4. 绘制玩家背包
        drawPlayerInventory();
        
        // 5. 绘制按钮和其他组件（参考Cyclic的render）
        // 绘制自定义按钮
        drawCustomButtons(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        // 6. 绘制标题和标签（参考Cyclic的renderLabels）
        updateButtonState();
        String titleText = title;
        int titleX = guiLeft + (GUI_WIDTH - fontRenderer.getStringWidth(titleText)) / 2;
        fontRenderer.drawString(titleText, titleX, guiTop + 6, 0x404040);
        
        // 7. 绘制提示信息（参考Cyclic的renderTooltip）
        drawTooltips(mouseX, mouseY);
    }
    
    /**
     * 更新按钮状态（参考Cyclic的onValueUpdate）
     */
    private void updateButtonState() {
        // no-op: custom button rendered directly
    }

    /**
     * 绘制自定义按钮（参考 Cyclic 的 ButtonTileEntityField 和 GuiButtonTexture）
     * 使用 buttons.png 纹理，索引 11=黑名单，12=白名单
     */
    private void drawCustomButtons(int mouseX, int mouseY) {
        if (capability == null) return;
        // 参考 GuiItemPump: x = guiLeft + 150, y = guiTop + PAD / 2
        int x = guiLeft + 150;
        int y = guiTop + PAD / 2;

        // 绘制按钮背景（参考 GuiButton 的标准渲染）
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
        
        // 绘制按钮状态（悬停高亮）
        int hoverState = isMouseOverButton(mouseX, mouseY, x, y) ? 2 : 1;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, 
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, 
            GlStateManager.SourceFactor.ONE, 
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        
        // 绘制按钮背景（使用原版按钮纹理）
        mc.getTextureManager().bindTexture(VANILLA_BUTTON_TEXTURE);
        drawTexturedModalRect(x, y, 0, 46 + hoverState * 20, TOGGLE_BTN_WIDTH / 2, TOGGLE_BTN_HEIGHT);
        drawTexturedModalRect(x + TOGGLE_BTN_WIDTH / 2, y, 200 - TOGGLE_BTN_WIDTH / 2, 46 + hoverState * 20, TOGGLE_BTN_WIDTH / 2, TOGGLE_BTN_HEIGHT);
        
        // 绘制按钮图标（参考 GuiButtonTexture.drawButton）
        mc.getTextureManager().bindTexture(BUTTON_TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 计算纹理索引：11=黑名单，12=白名单（参考 GuiItemPump）
        int textureIndex = capability.isWhitelistMode() ? 12 : 11;
        int sizeBtnTexture = 16; // Const.SQ - 2
        int texX = textureIndex * sizeBtnTexture;
        int texY = 0;
        if (textureIndex > 15) {
            texY = (textureIndex / 15) * sizeBtnTexture;
        }
        
        // 绘制图标（参考 GuiButtonTexture: x+1 偏移，因为按钮宽18，纹理宽16）
        drawTexturedModalRect(x + 2, y + 2, texX, texY, sizeBtnTexture, sizeBtnTexture);
    }
    
    private boolean isMouseOverButton(int mouseX, int mouseY, int btnX, int btnY) {
        return mouseX >= btnX && mouseX < btnX + TOGGLE_BTN_WIDTH && 
               mouseY >= btnY && mouseY < btnY + TOGGLE_BTN_HEIGHT;
    }
    
    /**
     * 检查鼠标是否在指定区域内（参考 GuiContainer.isPointInRegion）
     */
    private boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        int x = guiLeft + rectX;
        int y = guiTop + rectY;
        return pointX >= x && pointX < x + rectWidth && pointY >= y && pointY < y + rectHeight;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        // 检测自定义按钮点击（切换黑白名单）- 参考 Cyclic 按钮位置
        if (capability == null) return;
        int x = guiLeft + 150;
        int y = guiTop + PAD / 2;
        if (isMouseOverButton(mouseX, mouseY, x, y)) {
            // 切换并发送到服务器
            capability.setWhitelistMode(!capability.isWhitelistMode());
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
            // 发送同步包到服务器
            String[] slots = new String[SLOT_COUNT];
            for (int i = 0; i < SLOT_COUNT; i++) slots[i] = capability.getFilterSlot(i);
            com.moremod.rsring.RsRingMod.network.sendToServer(new com.moremod.network.PacketSyncRingFilter(capability.isWhitelistMode(), slots));
        }
    }
    
    /**
     * 绘制GUI背景（参考Cyclic的drawGuiContainerBackgroundLayer方法）
     */
    private void drawGuiBackground() {
        // 使用 Cyclic 的 GUI 背景纹理
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GUI_BACKGROUND);
        
        // 绘制背景纹理 (176x166)
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
    }
    
    /**
     * 绘制过滤槽位（参考 Cyclic 的 renderStackWrappers 方法）
     * 槽位布局：SLOTX_START + (j-1) * SQ, SLOTY
     */
    private void drawFilterSlots() {
        if (capability == null) return;
        
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 绘制槽位背景（参考 Cyclic 的 Const.Res.SLOT 纹理）
        mc.getTextureManager().bindTexture(SLOT_TEXTURE);
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = guiLeft + SLOTX_START + i * SQ;
            int slotY = guiTop + SLOTY;
            // 绘制 18x18 槽位纹理
            drawTexturedModalRect(slotX, slotY, 0, 0, SQ, SQ);
        }
        
        // 绘制过滤槽位中的物品（参考 Cyclic 的 StackWrapper 渲染）
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        
        for (int i = 0; i < SLOT_COUNT; i++) {
            String itemName = capability.getFilterSlot(i);
            if (itemName == null || itemName.isEmpty()) continue;

            try {
                ItemStack display = new ItemStack(net.minecraft.item.Item.REGISTRY.getObject(new ResourceLocation(itemName)));
                if (!display.isEmpty()) {
                    int slotX = guiLeft + SLOTX_START + i * SQ + 1;
                    int slotY = guiTop + SLOTY + 1;
                    mc.getRenderItem().renderItemAndEffectIntoGUI(display, slotX, slotY);
                    // 不渲染数量叠加层，保持 ghost item 外观
                }
            } catch (Exception ignored) {}
        }
        
        GlStateManager.popMatrix();
    }
    
    /**
     * 绘制玩家背包（参考 Cyclic 的 ScreenSize.STANDARD.playerOffsetX/Y）
     * 标准布局：X=8 (PAD), Y=84
     */
    private void drawPlayerInventory() {
        // 背包起始位置（参考 Cyclic 的 ScreenSize.STANDARD: playerOffsetX=8, playerOffsetY=84）
        int invStartX = guiLeft + PAD;
        int invStartY = guiTop + 84;
        
        // 先绘制槽位背景纹理
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(SLOT_TEXTURE);
        
        // 绘制主背包槽位背景（3行9列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invStartX + col * SQ;
                int y = invStartY + row * SQ;
                drawTexturedModalRect(x, y, 0, 0, SQ, SQ);
            }
        }
        
        // 绘制快捷栏槽位背景（1行9列）
        int hotbarY = invStartY + 58;
        for (int i = 0; i < 9; i++) {
            int x = invStartX + i * SQ;
            drawTexturedModalRect(x, hotbarY, 0, 0, SQ, SQ);
        }
        
        // 然后绘制物品
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        
        // 绘制主背包物品（3行9列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = mc.player.inventory.getStackInSlot(slotIndex);
                if (!stack.isEmpty()) {
                    int x = invStartX + col * SQ + 1;
                    int y = invStartY + row * SQ + 1;
                    mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
                    mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer, stack, x, y, "");
                }
            }
        }
        
        // 绘制快捷栏物品（参考 Cyclic：主背包下方 +58 偏移）
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int x = invStartX + i * SQ + 1;
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, hotbarY + 1);
                mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer, stack, x, hotbarY + 1, "");
            }
        }
        
        GlStateManager.popMatrix();
    }
    
    /**
     * 绘制工具提示（参考 Cyclic 的 drawStackWrappers 悬停检测）
     */
    private void drawTooltips(int mouseX, int mouseY) {
        // 检查鼠标是否悬停在过滤槽位上
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = guiLeft + SLOTX_START + i * SQ;
            int slotY = guiTop + SLOTY;
            
            if (isPointInRegion(SLOTX_START + i * SQ, SLOTY, SQ - 2, SQ - 2, mouseX, mouseY)) {
                String itemName = capability != null ? capability.getFilterSlot(i) : "";
                if (itemName != null && !itemName.isEmpty()) {
                    try {
                        ItemStack display = new ItemStack(net.minecraft.item.Item.REGISTRY.getObject(new ResourceLocation(itemName)));
                        if (!display.isEmpty()) {
                            renderToolTip(display, mouseX, mouseY);
                        }
                    } catch (Exception ignored) {}
                } else {
                    // 显示提示：点击添加过滤物品
                    drawHoveringText(java.util.Arrays.asList(
                        TextFormatting.GRAY + "点击添加过滤物品",
                        TextFormatting.DARK_GRAY + "仅读取，不消耗"
                    ), mouseX, mouseY);
                }
                break;
            }
        }
        
        // 检查按钮悬停提示
        int btnX = guiLeft + 150;
        int btnY = guiTop + PAD / 2;
        if (isMouseOverButton(mouseX, mouseY, btnX, btnY)) {
            String mode = capability != null && capability.isWhitelistMode() ? "白名单模式" : "黑名单模式";
            drawHoveringText(java.util.Arrays.asList(
                TextFormatting.YELLOW + mode,
                TextFormatting.GRAY + "点击切换过滤模式"
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        if (mouseButton != 0) return; // 只处理左键点击
        
        refreshCapability();
        if (capability == null) return;
        
        // 检查是否点击过滤槽位（参考 Cyclic 的 mouseClickedWrapper）
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (isPointInRegion(SLOTX_START + i * SQ, SLOTY, SQ - 2, SQ - 2, mouseX, mouseY)) {
                handleFilterSlotClick(i);
                return;
            }
        }
        
        // 检查是否点击背包槽位
        int invStartX = PAD;
        int invStartY = 84;
        
        // 检查主背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col;
                if (isPointInRegion(invStartX + col * SQ, invStartY + row * SQ, SQ - 2, SQ - 2, mouseX, mouseY)) {
                    handleInventorySlotClick(slotIndex);
                    return;
                }
            }
        }
        
        // 检查快捷栏
        int hotbarY = invStartY + 58;
        for (int i = 0; i < 9; i++) {
            if (isPointInRegion(invStartX + i * SQ, hotbarY, SQ - 2, SQ - 2, mouseX, mouseY)) {
                handleInventorySlotClick(i);
                return;
            }
        }
    }
    
    /**
     * 处理过滤槽位点击（参考Cyclic：仅读取，不消耗，mayPlace允许任意物品）
     */
    private void handleFilterSlotClick(int slot) {
        if (capability == null) return;
        
        ItemStack held = mc.player.inventory.getItemStack();
        if (held.isEmpty()) {
            // 清空过滤槽位
            capability.setFilterSlot(slot, "");
        } else {
            // 仅读取物品类型，不消耗物品（参考Cyclic的过滤模板机制）
            String name = held.getItem().getRegistryName() != null ? held.getItem().getRegistryName().toString() : "";
            if (!name.isEmpty()) {
                capability.setFilterSlot(slot, name);
            }
        }
        RsRingCapability.syncCapabilityToStack(ringStack, capability);
    }
    
    /**
     * 处理背包槽位点击（参考Cyclic：复制到过滤槽，不消耗）
     */
    private void handleInventorySlotClick(int slotIndex) {
        if (capability == null) return;
        
        ItemStack stackInSlot = mc.player.inventory.getStackInSlot(slotIndex);
        if (!stackInSlot.isEmpty()) {
            String name = stackInSlot.getItem().getRegistryName() != null ? stackInSlot.getItem().getRegistryName().toString() : "";
            if (!name.isEmpty()) {
                // 寻找一个空的过滤槽位
                for (int i = 0; i < SLOT_COUNT; i++) {
                    if (capability.getFilterSlot(i) == null || capability.getFilterSlot(i).isEmpty()) {
                        capability.setFilterSlot(i, name);
                        RsRingCapability.syncCapabilityToStack(ringStack, capability);
                        break;
                    }
                }
            }
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        refreshCapability();
        if (capability == null) return;

        if (button.id == 0) {
            // 切换黑白名单模式（参考Cyclic的FILTER_TYPE字段切换）
            capability.setWhitelistMode(!capability.isWhitelistMode());
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
