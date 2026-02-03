# 戒指绑定箱子边框渲染功能

## 功能描述

当玩家主手持有箱子戒指时，自动在绑定的箱子位置绘制彩色边框，方便玩家快速定位绑定的箱子。

## 功能特性

### 1. 自动渲染
- 主手持有戒指时自动显示边框
- 切换到其他物品时自动隐藏
- 无需额外操作

### 2. RGB彩色效果
- 边框颜色循环变化（RGB彩虹效果）
- 2秒完成一个颜色循环（更快更动感）
- 使用HSB色彩空间实现平滑过渡
- 多层光晕效果，每层颜色略有偏移

### 3. 距离限制
- 最大渲染距离：512格
- 超出距离自动隐藏边框
- 优化性能，避免远距离渲染

### 4. 维度检测
- 只在同一维度显示边框
- 跨维度自动隐藏
- 避免错误的位置显示

### 5. 绑定状态检测
- 只有已绑定的戒指才显示边框
- 未绑定戒指不显示任何内容

## 实现细节

### 文件结构

**新增文件**：
- `rsring/src/main/java/com/moremod/client/RingBoundBoxRenderer.java` - 边框渲染器

**修改文件**：
- `rsring/src/main/java/com/moremod/proxy/ClientProxy.java` - 注册渲染器

### 核心代码

#### 1. 渲染器类 (RingBoundBoxRenderer.java)

```java
@SideOnly(Side.CLIENT)
public class RingBoundBoxRenderer {
    private static final int MAX_RENDER_DISTANCE = 512; // 最大渲染距离
    private static final float LINE_WIDTH = 2.0F; // 边框线宽度

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        // 1. 检查主手是否持有戒指
        // 2. 获取戒指capability
        // 3. 检查绑定状态
        // 4. 检查维度和距离
        // 5. 渲染边框
    }
}
```

#### 2. RGB颜色计算

```java
// 基于时间计算HSB颜色，2秒一个循环
long time = System.currentTimeMillis();
float timeOffset = (time % 2000) / 2000.0F;

// 多层渲染，每层颜色偏移
for (int layer = 0; layer < 3; layer++) {
    float hueOffset = (layer * 0.15F + timeOffset) % 1.0F;
    int rgb = java.awt.Color.HSBtoRGB(hueOffset, 1.0F, 1.0F);
    int red = (rgb >> 16) & 0xFF;
    int green = (rgb >> 8) & 0xFF;
    int blue = rgb & 0xFF;
    int alpha = 255 - (layer * 60); // 渐变透明度
}
```

#### 3. 边框绘制

使用OpenGL绘制12条线段（立方体的12条边），并添加多层光晕效果：
- 底面4条边
- 顶面4条边
- 4条竖边

**多层渲染**：
```java
for (int layer = 0; layer < 3; layer++) {
    // 计算颜色和透明度
    float expansion = layer * 0.02F; // 每层向外扩展
    renderBlockBoundingWithExpansion(buffer, pos, red, green, blue, alpha, expansion);
}
```

**单层绘制**：
```java
private void renderBlockBoundingWithExpansion(BufferBuilder buffer, BlockPos pos, 
    int red, int green, int blue, int opacity, float expansion) {
    final float size = 1.0f + expansion;
    final float offset = -expansion / 2.0f; // 居中扩展
    
    // 绘制12条边...
}
```

### OpenGL状态管理

```java
// 启用混合和透明度
GlStateManager.enableBlend();
GlStateManager.tryBlendFuncSeparate(...);

// 设置线宽
GlStateManager.glLineWidth(LINE_WIDTH);

// 禁用纹理和深度测试（确保边框始终可见）
GlStateManager.disableTexture2D();
GlStateManager.disableDepth();

// 渲染后恢复状态
GlStateManager.enableDepth();
GlStateManager.enableTexture2D();
GlStateManager.disableBlend();
```

## 技术要点

### 1. 事件监听
使用 `RenderWorldLastEvent` 在世界渲染的最后阶段绘制边框，确保边框显示在所有方块之上。

### 2. 坐标转换
```java
// 获取玩家视角偏移
double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

// 平移到方块位置（相对于玩家）
GlStateManager.translate(-playerX, -playerY, -playerZ);
```

### 3. 边界框扩展
```java
// 稍微扩大边界框以避免Z-fighting
AxisAlignedBB box = new AxisAlignedBB(
    pos.getX() - 0.002, pos.getY() - 0.002, pos.getZ() - 0.002,
    pos.getX() + 1.002, pos.getY() + 1.002, pos.getZ() + 1.002
);
```

### 4. 性能优化
- 距离检测：超过512格不渲染
- 维度检测：不同维度不渲染
- 状态检测：未绑定不渲染
- 主手检测：只有主手持有才渲染

## 使用方法

1. **绑定箱子**：
   - 手持戒指右键点击箱子进行绑定

2. **查看边框**：
   - 将戒指放在主手
   - 自动显示绑定箱子的彩色边框

3. **隐藏边框**：
   - 切换到其他物品
   - 或者走出512格范围

## 视觉效果

### 边框样式
- **颜色**：RGB彩虹循环（红→黄→绿→青→蓝→紫→红）
- **多层光晕**：3层边框叠加，创造发光扩散效果
  - 内层：最亮，完全不透明（alpha=255）
  - 中层：稍透明（alpha=195）
  - 外层：更透明（alpha=135）
- **线宽**：3.0像素（加粗）
- **循环周期**：2秒（更快的颜色变化）
- **光晕扩展**：每层向外扩展0.02格，创造立体感

### 炫酷效果
1. **彩虹流动**：每层使用不同的颜色偏移，创造彩虹流动效果
2. **发光光晕**：多层半透明边框叠加，模拟发光效果
3. **动态变化**：2秒完成一个颜色循环，视觉更加动感
4. **立体感**：外层边框稍大，创造3D光晕效果

### 显示条件
| 条件 | 是否显示 |
|------|---------|
| 主手持有戒指 | ✅ |
| 副手持有戒指 | ❌ |
| 戒指在背包 | ❌ |
| 戒指在饰品栏 | ❌ |
| 未绑定箱子 | ❌ |
| 不同维度 | ❌ |
| 距离>512格 | ❌ |

## 兼容性

- 完全客户端渲染，不影响服务器性能
- 支持所有维度（主世界、下界、末地）
- 与其他模组的渲染兼容
- 不影响游戏性能

## 配置选项

当前版本暂无配置选项，未来可能添加：
- 边框颜色自定义
- 渲染距离调整
- 线宽调整
- 开关选项

## 已知限制

1. 只支持主手持有时显示
2. 最大渲染距离512格
3. 不支持跨维度显示
4. 边框始终显示在方块上方（禁用深度测试）

## 修改日期

2026-02-02

## 状态

✅ 已实现并编译成功
