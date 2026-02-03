# 戒指过滤 GUI 迁移总结

## 迁移目标
将 Cyclic 的 Item Extraction Cable (物品提取管道) GUI 设计迁移到 rsring 的箱子戒指过滤 GUI。

## 已完成的更改

### 1. 布局调整
- **过滤槽位置**: 
  - 旧: 居中布局 `(GUI_WIDTH - SLOT_COUNT * 18) / 2`
  - 新: 左对齐 `SLOTX_START = 8 (PAD)`, `SLOTY = 50 (SQ + PAD * 4)`
  - 参考: `ContainerItemPump.SLOTX_START` 和 `SLOTY`

- **按钮位置**:
  - 旧: 左上角 `(10, 10)`
  - 新: 右上角 `(150, PAD/2)` - 匹配 Cyclic 的 `GuiItemPump` 布局
  - 尺寸: 20x20 (简化版，不显示文本)

- **玩家背包位置**:
  - 保持标准布局: `X=8 (PAD)`, `Y=84`
  - 参考: `ScreenSize.STANDARD.playerOffsetX/Y()`

### 2. 常量定义
采用 Cyclic 的命名约定：
```java
private static final int SQ = 18;        // Const.SQ - 槽位大小
private static final int PAD = 8;        // Const.PAD - 边距
private static final int SLOTX_START = PAD;
private static final int SLOTY = SQ + PAD * 4;  // 50
```

### 3. 渲染优化
- **槽位渲染**: 使用 `GlStateManager.pushMatrix/popMatrix` 包裹物品渲染
- **Ghost Items**: 过滤槽中的物品不显示数量叠加层，保持"幽灵物品"外观
- **纹理绑定**: 参考 Cyclic 的纹理绑定模式

### 4. 交互改进
- **鼠标点击检测**: 使用 `isPointInRegion()` 方法替代手动坐标计算
- **按钮悬停**: 添加 `isMouseOverButton()` 辅助方法
- **工具提示**: 增强按钮悬停提示，显示当前模式和操作说明

### 5. 代码结构
参考 Cyclic 的方法命名和组织：
- `drawFilterSlots()` - 对应 `renderStackWrappers()`
- `drawPlayerInventory()` - 对应玩家背包渲染逻辑
- `drawCustomButtons()` - 简化的按钮渲染
- `drawTooltips()` - 对应 `drawButtonTooltips()` 和 `drawStackWrappers()` 悬停检测

## 关键差异

### Cyclic 使用的特性（未完全迁移）
1. **StackWrapper 系统**: Cyclic 使用专门的 `StackWrapper` 类管理过滤槽
   - rsring 使用简化的字符串数组存储物品注册名
   
2. **Container 系统**: Cyclic 使用 `ContainerItemPump` 管理槽位
   - rsring 的过滤 GUI 不使用 Container（纯客户端 GUI）

3. **网络同步**: Cyclic 使用 `PacketTileStackWrapped` 同步
   - rsring 使用 `PacketSyncRingFilter` 同步

### rsring 的简化设计
- 不需要 TileEntity（戒指是物品，不是方块）
- 过滤槽仅存储物品类型（注册名），不存储 ItemStack
- 直接在 Capability 中管理过滤数据

## 纹理要求

需要从 Cyclic 复制或创建以下纹理：

1. **inventory_slot.png** (18x18) - 槽位背景
   - 源: `Cyclic-trunk-1.12/src/main/resources/assets/cyclicmagic/textures/gui/inventory_slot.png`
   - 目标: `rsring/src/main/resources/assets/rsring/textures/gui/inventory_slot.png`

2. **icon_filter.png** (16x16) - 过滤器图标（可选）
3. **icon_extract.png** (16x16) - 提取图标（可选）

详见: `GUI-Texture-Migration.md`

## 测试清单

- [ ] 过滤槽位置正确（左对齐，顶部）
- [ ] 按钮位置正确（右上角）
- [ ] 玩家背包显示正确
- [ ] 点击过滤槽可以设置/清除过滤物品
- [ ] 点击背包物品可以添加到过滤槽
- [ ] 按钮点击可以切换黑白名单模式
- [ ] 鼠标悬停显示正确的工具提示
- [ ] 槽位纹理正确加载和显示
- [ ] Ghost items 正确渲染（无数量显示）

## 参考文件

### Cyclic 源文件
- `GuiItemPump.java` - GUI 主类
- `ContainerItemPump.java` - Container 和槽位布局
- `GuiBaseContainer.java` - 基础 GUI 类
- `Const.java` - 常量定义

### rsring 更新文件
- `GuiRingFilter.java` - 已更新以匹配 Cyclic 布局

## 下一步

1. **复制纹理文件**: 按照 `GUI-Texture-Migration.md` 的说明复制纹理
2. **游戏内测试**: 启动游戏并测试 GUI 的所有功能
3. **微调**: 根据测试结果调整布局和交互
4. **文档更新**: 更新用户文档说明新的 GUI 布局

## 版本历史

- **v1.0** (2026-02-02): 初始迁移完成
  - 布局匹配 Cyclic 的 Item Extraction Cable GUI
  - 采用 Cyclic 的常量命名约定
  - 优化渲染和交互逻辑
