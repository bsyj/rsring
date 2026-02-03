# GUI 纹理迁移指南

## 概述
本文档说明如何从 Cyclic 项目复制必要的 GUI 纹理文件到 rsring 项目。

## 需要复制的纹理文件

### 1. 槽位纹理
**源文件**: `Cyclic-trunk-1.12/src/main/resources/assets/cyclicmagic/textures/gui/inventory_slot.png`
**目标位置**: `rsring/src/main/resources/assets/rsring/textures/gui/inventory_slot.png`
**用途**: 过滤槽位背景纹理（18x18 像素）

### 2. 可选纹理（如需要）
- `slot_large_plain.png` - 大型槽位纹理（26x26 像素）
- `buttons.png` - 按钮纹理集
- `table.png` - GUI 背景纹理

## 手动复制步骤

1. 创建目标目录：
   ```
   rsring/src/main/resources/assets/rsring/textures/gui/
   ```

2. 从 Cyclic 项目复制以下文件：
   - `inventory_slot.png` (必需)

3. 如果需要自定义图标，创建以下文件：
   - `icon_filter.png` (16x16) - 过滤器图标
   - `icon_extract.png` (16x16) - 提取图标

## 纹理规格

### inventory_slot.png
- 尺寸: 18x18 像素
- 格式: PNG (支持透明度)
- 用途: 过滤槽位背景
- 参考: Cyclic 的标准槽位纹理

### 图标纹理
- 尺寸: 16x16 像素
- 格式: PNG (支持透明度)
- 用途: 按钮图标显示

## 代码中的纹理引用

```java
// GuiRingFilter.java 中的纹理常量
private static final ResourceLocation SLOT_TEXTURE = 
    new ResourceLocation("rsring", "textures/gui/inventory_slot.png");
private static final ResourceLocation ICON_FILTER = 
    new ResourceLocation("rsring", "textures/gui/icon_filter.png");
private static final ResourceLocation ICON_EXTRACT = 
    new ResourceLocation("rsring", "textures/gui/icon_extract.png");
```

## 注意事项

1. **纹理路径**: Minecraft 会自动在 `assets/<modid>/` 下查找资源，因此 ResourceLocation 中的路径应该从 `textures/` 开始。

2. **纹理尺寸**: 确保纹理尺寸与代码中使用的常量匹配（SQ = 18）。

3. **透明度**: 槽位纹理应该有适当的透明度以便与背景融合。

4. **备用方案**: 如果纹理文件缺失，GUI 仍然可以工作，但槽位将显示为纯色矩形。

## 完成后验证

1. 启动游戏并打开箱子戒指过滤 GUI
2. 检查过滤槽位是否正确显示纹理
3. 检查按钮图标是否正确显示
4. 验证鼠标悬停和点击交互是否正常

## 参考
- Cyclic GUI 系统: `GuiBaseContainer.java`
- Cyclic 槽位渲染: `renderStackWrappers()` 方法
- Cyclic 常量定义: `Const.java`
