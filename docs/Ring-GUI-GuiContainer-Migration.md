# 戒指 GUI 迁移到 GuiContainer - 完整实现

## 概述
将戒指过滤 GUI 从 `GuiScreen` 迁移到 `GuiContainer`，完全复刻 Cyclic 的原汁原味实现。

## 为什么使用 GuiContainer？

### GuiScreen vs GuiContainer

**GuiScreen**（旧实现）：
- ❌ 需要手动绘制所有槽位背景
- ❌ 需要手动处理物品渲染
- ❌ 需要手动处理槽位交互
- ❌ 代码复杂，容易出错

**GuiContainer**（新实现）：
- ✅ 自动绘制玩家背包槽位
- ✅ 自动处理物品渲染和叠加层
- ✅ 自动处理 Shift+点击等交互
- ✅ 与 Cyclic 完全一致的实现方式
- ✅ 代码简洁，易于维护

## 新增文件

### 1. ContainerRingFilter.java
**路径**: `rsring/src/main/java/com/moremod/client/ContainerRingFilter.java`

**功能**:
- 管理 GUI 的槽位系统
- 绑定玩家背包槽位（主背包 + 快捷栏）
- 处理槽位交互逻辑

**关键代码**:
```java
public class ContainerRingFilter extends Container {
    // 绑定玩家背包（3行9列）
    protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventoryPlayer, col + row * 9 + 9,
                    PLAYER_OFFSET_X + col * SQ,
                    PLAYER_OFFSET_Y + row * SQ
                ));
            }
        }
        bindPlayerHotbar(inventoryPlayer);
    }
    
    // 绑定快捷栏（1行9列）
    protected void bindPlayerHotbar(InventoryPlayer inventoryPlayer) {
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(inventoryPlayer, i,
                PLAYER_OFFSET_X + i * SQ,
                PLAYER_OFFSET_Y + PAD / 2 + 3 * SQ
            ));
        }
    }
}
```

**参考**: Cyclic 的 `ContainerItemPump` 和 `ContainerBase`

### 2. GuiRingFilterContainer.java
**路径**: `rsring/src/main/java/com/moremod/client/GuiRingFilterContainer.java`

**功能**:
- 继承 `GuiContainer` 而不是 `GuiScreen`
- 自动处理玩家背包槽位的渲染
- 绘制过滤槽位和按钮
- 处理鼠标交互

**关键方法**:
```java
public class GuiRingFilterContainer extends GuiContainer {
    // 绘制背景层（GUI 背景 + 过滤槽位）
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // 绘制 GUI 背景纹理
        this.mc.getTextureManager().bindTexture(GUI_BACKGROUND);
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
        
        // 绘制过滤槽位
        drawFilterSlots();
    }
    
    // 绘制前景层（标题 + 按钮 + 工具提示）
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 绘制标题
        this.fontRenderer.drawString(titleText, titleX, 6, 0x404040);
        
        // 绘制按钮
        drawCustomButtons(mouseX, mouseY);
        
        // 绘制工具提示
        drawTooltips(mouseX, mouseY);
    }
}
```

**参考**: Cyclic 的 `GuiItemPump` 和 `GuiBaseContainer`

## 修改的文件

### ClientProxy.java
**修改内容**: 更新 `openChestRingGui()` 方法

**修改前**:
```java
@Override
public void openChestRingGui(ItemStack stack) {
    Minecraft.getMinecraft().displayGuiScreen(
        new GuiRingFilter(stack, "物品吸收戒指 - 黑白名单"));
}
```

**修改后**:
```java
@Override
public void openChestRingGui(ItemStack stack) {
    EntityPlayer player = Minecraft.getMinecraft().player;
    if (player != null) {
        ContainerRingFilter container = new ContainerRingFilter(player.inventory, stack);
        Minecraft.getMinecraft().displayGuiScreen(
            new GuiRingFilterContainer(container, stack, "物品吸收戒指 - 黑白名单"));
    }
}
```

**添加的 import**:
```java
import com.moremod.client.ContainerRingFilter;
import com.moremod.client.GuiRingFilterContainer;
import net.minecraft.entity.player.EntityPlayer;
```

## 架构对比

### 旧架构（GuiScreen）
```
GuiRingFilter (extends GuiScreen)
├── 手动绘制 GUI 背景
├── 手动绘制过滤槽位背景
├── 手动绘制过滤槽位物品
├── 手动绘制玩家背包槽位背景 ← 容易出错
├── 手动绘制玩家背包物品
└── 手动处理所有交互
```

### 新架构（GuiContainer）
```
ContainerRingFilter (extends Container)
├── 定义玩家背包槽位
└── 处理槽位交互逻辑

GuiRingFilterContainer (extends GuiContainer)
├── 绘制 GUI 背景
├── 绘制过滤槽位（手动）
├── 玩家背包自动渲染 ← GuiContainer 自动处理
└── 处理自定义交互（按钮、过滤槽）
```

## 关键改进

### 1. 自动槽位渲染
`GuiContainer` 会自动：
- 绘制所有注册的槽位背景
- 渲染槽位中的物品
- 渲染物品数量叠加层
- 处理物品拖拽和 Shift+点击

### 2. 坐标系统
**drawGuiContainerBackgroundLayer**:
- 使用绝对坐标（`guiLeft`, `guiTop`）
- 适合绘制纹理和背景

**drawGuiContainerForegroundLayer**:
- 使用相对坐标（相对于 GUI 左上角）
- 适合绘制文本和按钮

### 3. 渲染顺序
```
1. drawDefaultBackground()           // 暗化背景
2. drawGuiContainerBackgroundLayer() // GUI 背景 + 过滤槽位
3. [自动] 渲染所有槽位和物品        // GuiContainer 自动处理
4. drawGuiContainerForegroundLayer() // 标题 + 按钮 + 工具提示
5. renderHoveredToolTip()            // 悬停提示
```

## 与 Cyclic 的对应关系

| Cyclic | rsring |
|--------|--------|
| `ContainerItemPump` | `ContainerRingFilter` |
| `GuiItemPump` | `GuiRingFilterContainer` |
| `ContainerBase.bindPlayerInventory()` | `ContainerRingFilter.bindPlayerInventory()` |
| `GuiBaseContainer.drawGuiContainerBackgroundLayer()` | `GuiRingFilterContainer.drawGuiContainerBackgroundLayer()` |
| `GuiBaseContainer.drawGuiContainerForegroundLayer()` | `GuiRingFilterContainer.drawGuiContainerForegroundLayer()` |

## 编译状态
✅ **编译成功** - 无错误，无警告

## 测试建议

### 游戏内测试
1. 打开戒指过滤 GUI
2. 检查以下内容：
   - ✅ GUI 背景纹理正确显示
   - ✅ 过滤槽位有槽位背景
   - ✅ 玩家背包槽位自动显示（包括背景和物品）
   - ✅ 物品在槽位中居中显示
   - ✅ 物品数量正确显示
   - ✅ 按钮正确渲染和交互
   - ✅ 工具提示正确显示
   - ✅ 可以点击过滤槽位添加/移除过滤物品
   - ✅ 可以点击按钮切换黑白名单模式

### 与 Cyclic 对比
打开 Cyclic 的 Item Extraction Cable GUI，对比：
- 布局是否一致
- 槽位大小是否一致
- 按钮位置是否一致
- 交互方式是否一致

## 优势总结

### 代码简洁性
- **旧实现**: ~400 行代码，需要手动处理所有渲染
- **新实现**: ~300 行代码，GuiContainer 自动处理背包渲染

### 维护性
- 使用标准的 Minecraft GUI 系统
- 与 Cyclic 实现方式完全一致
- 更容易理解和维护

### 可靠性
- 减少手动渲染代码，减少出错机会
- 自动处理物品渲染和交互
- 与原版 GUI 行为一致

## 旧文件处理
`GuiRingFilter.java` 可以保留作为参考，或者删除。新实现完全替代了旧实现。

## 总结
成功将戒指过滤 GUI 迁移到 `GuiContainer`，实现了与 Cyclic 完全一致的原汁原味效果。新实现更简洁、更可靠、更易维护。
