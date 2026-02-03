# 戒指GUI切换黑白名单时右下角乱码修复

## 问题描述
在戒指过滤GUI中点击切换黑白名单按钮时，右下角会出现乱码提示。

## 问题原因
1. **坐标系统混淆**：`drawGuiContainerForegroundLayer()` 使用相对坐标系统（相对于GUI左上角），而 `drawScreen()` 使用屏幕绝对坐标系统
2. **Tooltip渲染位置错误**：原来的代码在 `drawGuiContainerForegroundLayer()` 中调用 `drawHoveringText()`，导致tooltip使用了错误的坐标系统
3. **Tooltip冲突**：`renderHoveredToolTip()` 会自动检测鼠标下的槽位并显示物品tooltip，与自定义tooltip冲突

## 修复方案

### 1. 将自定义tooltip移到 `drawScreen()` 方法
```java
@Override
public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    refreshCapability();
    this.drawDefaultBackground();
    super.drawScreen(mouseX, mouseY, partialTicks);
    
    // 只在不悬停在按钮上时才渲染物品tooltip
    int relativeX = mouseX - this.guiLeft;
    int relativeY = mouseY - this.guiTop;
    int btnX = 150;
    int btnY = PAD / 2;
    boolean isOverButton = isMouseOverButton(relativeX, relativeY, btnX, btnY);
    
    if (!isOverButton) {
        this.renderHoveredToolTip(mouseX, mouseY); // 只在不悬停按钮时显示物品tooltip
    }
    
    // 绘制自定义工具提示
    drawCustomTooltips(mouseX, mouseY);
}
```

### 2. 从 `drawGuiContainerForegroundLayer()` 移除tooltip绘制
```java
@Override
protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    
    // 绘制标题
    String titleText = title;
    int titleX = (this.xSize - this.fontRenderer.getStringWidth(titleText)) / 2;
    this.fontRenderer.drawString(titleText, titleX, 6, 0x404040);
    
    // 绘制自定义按钮
    drawCustomButtons(mouseX, mouseY);
    
    // 不再在这里绘制tooltip
}
```

### 3. 创建独立的tooltip绘制方法
```java
/**
 * 绘制自定义工具提示（在 drawScreen 中调用，使用屏幕绝对坐标）
 */
private void drawCustomTooltips(int mouseX, int mouseY) {
    if (capability == null) return;
    
    // 转换为相对坐标进行检测
    int relativeX = mouseX - this.guiLeft;
    int relativeY = mouseY - this.guiTop;
    
    // 检查过滤槽位
    for (int i = 0; i < SLOT_COUNT; i++) {
        int slotX = SLOTX_START + i * SQ;
        int slotY = SLOTY;
        
        if (relativeX >= slotX && relativeX < slotX + SQ - 2 && 
            relativeY >= slotY && relativeY < slotY + SQ - 2) {
            String itemName = capability.getFilterSlot(i);
            if (itemName == null || itemName.isEmpty()) {
                // 使用屏幕绝对坐标绘制tooltip
                this.drawHoveringText(java.util.Arrays.asList(
                    TextFormatting.GRAY + "点击添加过滤物品",
                    TextFormatting.DARK_GRAY + "仅读取，不消耗"
                ), mouseX, mouseY);
            }
            return;
        }
    }
    
    // 检查按钮悬停
    int btnX = 150;
    int btnY = PAD / 2;
    if (isMouseOverButton(relativeX, relativeY, btnX, btnY)) {
        String mode = capability.isWhitelistMode() ? "白名单模式" : "黑名单模式";
        // 使用屏幕绝对坐标绘制tooltip
        this.drawHoveringText(java.util.Arrays.asList(
            TextFormatting.YELLOW + mode,
            TextFormatting.GRAY + "点击切换过滤模式"
        ), mouseX, mouseY);
    }
}
```

## 修复效果
- ✅ 切换黑白名单时不再出现乱码
- ✅ 按钮悬停提示正确显示
- ✅ 空槽位提示正确显示
- ✅ 物品tooltip与自定义tooltip不冲突

## 技术要点

### Minecraft GUI坐标系统
1. **屏幕绝对坐标**：`drawScreen()` 方法使用，原点在屏幕左上角
2. **GUI相对坐标**：`drawGuiContainerForegroundLayer()` 方法使用，原点在GUI窗口左上角
3. **坐标转换**：`relativeX = mouseX - guiLeft`, `relativeY = mouseY - guiTop`

### Tooltip渲染规则
1. `drawHoveringText()` 在 `drawScreen()` 中调用时使用屏幕绝对坐标
2. `drawHoveringText()` 在 `drawGuiContainerForegroundLayer()` 中调用时使用相对坐标（会导致位置错误）
3. `renderHoveredToolTip()` 自动处理物品槽位的tooltip，应该在 `drawScreen()` 中调用

## 修改的文件
- `rsring/src/main/java/com/moremod/client/GuiRingFilterContainer.java`

## 编译状态
✅ 编译成功 - 2026-02-02
