# 戒指 GUI 槽位背景修复

## 问题描述
用户报告：戒指的 GUI 背景材质和背包材质物品栏材质大小不对。

## 问题原因
`GuiRingFilter` 继承自 `GuiScreen` 而不是 `GuiContainer`，因此需要手动绘制所有 GUI 元素。

原代码只绘制了：
- ✅ GUI 背景纹理 (`table.png`)
- ✅ 过滤槽位背景纹理 (`inventory_slot.png`)
- ✅ 物品渲染
- ❌ **缺少：玩家背包槽位背景纹理**

这导致玩家背包区域没有槽位背景，物品直接显示在 GUI 背景上，看起来大小和位置不对。

## 修复内容

### 修改文件
`rsring/src/main/java/com/moremod/client/GuiRingFilter.java`

### 修改的方法
`drawPlayerInventory()`

### 修复前的代码
```java
private void drawPlayerInventory() {
    int invStartX = guiLeft + PAD;
    int invStartY = guiTop + 84;
    
    // 直接绘制物品，没有槽位背景
    GlStateManager.pushMatrix();
    RenderHelper.enableGUIStandardItemLighting();
    
    // 绘制主背包物品...
    // 绘制快捷栏物品...
    
    GlStateManager.popMatrix();
}
```

### 修复后的代码
```java
private void drawPlayerInventory() {
    int invStartX = guiLeft + PAD;
    int invStartY = guiTop + 84;
    
    // 1. 先绘制槽位背景纹理
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
    
    // 2. 然后绘制物品（位置偏移 +1 以居中）
    GlStateManager.pushMatrix();
    RenderHelper.enableGUIStandardItemLighting();
    
    // 绘制主背包物品...
    // 绘制快捷栏物品...
    
    GlStateManager.popMatrix();
}
```

## 关键改进

### 1. 添加槽位背景渲染
- 使用 `inventory_slot.png` 纹理（18x18）
- 为主背包绘制 27 个槽位背景（3行 × 9列）
- 为快捷栏绘制 9 个槽位背景（1行 × 9列）

### 2. 调整物品渲染位置
- 物品渲染位置从 `(x, y)` 改为 `(x + 1, y + 1)`
- 这样物品在槽位中居中显示，与 Cyclic 的效果一致

### 3. 渲染顺序
正确的渲染顺序：
1. GUI 背景 (`table.png`)
2. 过滤槽位背景 (`inventory_slot.png`)
3. 过滤槽位物品
4. **玩家背包槽位背景** (`inventory_slot.png`) ← 新增
5. 玩家背包物品

## 布局参数

### 槽位尺寸
- `SQ = 18` - 每个槽位 18×18 像素

### 背包布局
- 起始位置：`X = 8 (PAD)`, `Y = 84`
- 主背包：3行 × 9列 = 27 个槽位
- 快捷栏：Y偏移 +58，1行 × 9列 = 9 个槽位

### 物品偏移
- 物品在槽位内偏移 +1 像素以居中

## 参考实现
参考了 Cyclic 的 `GuiBaseContainer.drawGuiContainerBackgroundLayer()` 方法，该方法使用 `GuiContainer` 自动处理槽位背景。

由于 `GuiRingFilter` 使用 `GuiScreen`，需要手动实现相同的效果。

## 编译状态
✅ **编译成功** - 无错误，无警告

## 测试建议
1. 启动游戏并打开戒指过滤 GUI
2. 检查以下内容：
   - ✅ GUI 背景纹理正确显示
   - ✅ 过滤槽位有槽位背景
   - ✅ 玩家背包槽位有槽位背景（新修复）
   - ✅ 物品在槽位中居中显示
   - ✅ 所有槽位大小一致（18×18）

## 预期效果
修复后，戒指 GUI 应该与 Cyclic 的 Item Extraction Cable GUI 外观一致：
- 所有槽位都有清晰的背景边框
- 物品在槽位中居中显示
- 背包区域看起来整洁、对齐
- 槽位大小统一为 18×18 像素

## 相关文件
- `rsring/src/main/java/com/moremod/client/GuiRingFilter.java` - 修复的 GUI 类
- `rsring/src/main/resources/assets/rsring/textures/gui/inventory_slot.png` - 槽位背景纹理
- `rsring/src/main/resources/assets/rsring/textures/gui/table.png` - GUI 背景纹理
