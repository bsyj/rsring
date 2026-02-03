# 戒指GUI背包对齐修复 / Ring GUI Inventory Alignment Fix

## 问题 / Problem

戒指的GUI背包和物品栏位置不正确，与玩家背包没有对齐。
The ring's GUI inventory and item slots were not positioned correctly and not aligned with the player inventory.

## 解决方案 / Solution

完全参考 Cyclic 的实现，使用精确的常量和布局计算。
Completely reference Cyclic's implementation, using exact constants and layout calculations.

## 修复内容 / Changes Made

### 1. ContainerRingFilter.java

**完全参考 Cyclic 的常量定义**:
```java
// 完全参考 Cyclic 的 Const 常量
private static final int SQ = 18; // Const.SQ - 槽位大小
private static final int PAD = 8; // Const.PAD - 边距

// 玩家背包位置（完全参考 Cyclic 的 ScreenSize.STANDARD）
// playerOffsetX() = PAD = 8
// playerOffsetY() = 84
private static final int PLAYER_OFFSET_X = PAD;
private static final int PLAYER_OFFSET_Y = 84;
```

**完全参考 Cyclic 的槽位绑定**:
```java
// 主背包（3行9列）- 完全参考 Cyclic 的实现
for (int i = 0; i < 3; i++) {
    for (int j = 0; j < 9; j++) {
        addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9,
            PLAYER_OFFSET_X + j * SQ, // X
            PLAYER_OFFSET_Y + i * SQ  // Y
        ));
    }
}

// 快捷栏 - 完全参考 Cyclic 的实现
for (int i = 0; i < 9; i++) {
    addSlotToContainer(new Slot(inventoryPlayer, i,
        PLAYER_OFFSET_X + i * SQ,
        PLAYER_OFFSET_Y + PAD / 2 + 3 * SQ // PAD/2 = 4
    ));
}
```

### 2. GuiRingFilterContainer.java

**完全参考 Cyclic 的过滤槽位布局**:
```java
// 完全参考 Cyclic 的 ContainerItemPump 槽位布局
// SLOTX_START = Const.PAD = 8
// SLOTY = Const.SQ + Const.PAD * 4 = 18 + 32 = 50
private static final int SLOTX_START = PAD;
private static final int SLOTY = SQ + PAD * 4;
```

**完全参考 Cyclic 的 GUI 尺寸**:
```java
// 完全参考 Cyclic 的 ScreenSize.STANDARD
// width() = 176
// height() = 166
this.xSize = 176;
this.ySize = 166;
```

**完全参考 Cyclic 的槽位背景绘制**:
```java
// 绘制槽位背景（完全参考 Cyclic 的 GuiItemPump.drawGuiContainerBackgroundLayer）
this.mc.getTextureManager().bindTexture(SLOT_TEXTURE);
for (int i = 0; i < SLOT_COUNT; i++) {
    // 完全参考 Cyclic: x = guiLeft + SLOTX_START + i * SQ - 1
    // 完全参考 Cyclic: y = guiTop + SLOTY - 1
    int slotX = this.guiLeft + SLOTX_START + i * SQ - 1;
    int slotY = this.guiTop + SLOTY - 1;
    // 使用 Cyclic 的绘制方法
    net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(
        slotX, slotY, 0, 0, SQ, SQ, SQ, SQ);
}
```

**完全参考 Cyclic 的物品渲染位置**:
```java
// 物品渲染位置（完全参考 Cyclic，槽位背景是 -1，所以物品不需要 +1）
int slotX = this.guiLeft + SLOTX_START + i * SQ;
int slotY = this.guiTop + SLOTY;
this.mc.getRenderItem().renderItemAndEffectIntoGUI(display, slotX + 1, slotY + 1);
```

## Cyclic 参考源码 / Cyclic Reference Sources

### 常量定义 / Constants
- **文件 / File**: `Cyclic-trunk-1.12/src/main/java/com/lothrazar/cyclicmagic/util/Const.java`
- **常量 / Constants**:
  - `SQ = 18` (槽位大小)
  - `PAD = 8` (边距)
  - `ScreenSize.STANDARD.width() = 176`
  - `ScreenSize.STANDARD.height() = 166`
  - `ScreenSize.STANDARD.playerOffsetX() = 8`
  - `ScreenSize.STANDARD.playerOffsetY() = 84`

### Container 实现 / Container Implementation
- **文件 / File**: `Cyclic-trunk-1.12/src/main/java/com/lothrazar/cyclicmagic/gui/container/ContainerBase.java`
- **方法 / Methods**:
  - `bindPlayerInventory(InventoryPlayer)` - 绑定玩家主背包
  - `bindPlayerHotbar(InventoryPlayer)` - 绑定玩家快捷栏

### GUI 实现 / GUI Implementation
- **文件 / File**: `Cyclic-trunk-1.12/src/main/java/com/lothrazar/cyclicmagic/block/cablepump/item/GuiItemPump.java`
- **文件 / File**: `Cyclic-trunk-1.12/src/main/java/com/lothrazar/cyclicmagic/block/cablepump/item/ContainerItemPump.java`
- **布局 / Layout**:
  - `SLOTX_START = Const.PAD = 8`
  - `SLOTY = Const.SQ + Const.PAD * 4 = 50`

## 关键修复点 / Key Fixes

1. **玩家背包Y偏移**: 从错误值改为 `84` (Cyclic 的 STANDARD 屏幕尺寸)
2. **玩家背包X偏移**: 确认为 `8` (PAD)
3. **快捷栏Y偏移**: `84 + 4 + 54 = 142` (playerOffsetY + PAD/2 + 3*SQ)
4. **过滤槽位Y位置**: `50` (SQ + PAD * 4 = 18 + 32)
5. **槽位背景偏移**: `-1` (Cyclic 的标准做法)
6. **物品渲染偏移**: `+1` (相对于槽位背景)

## 布局计算 / Layout Calculations

### 玩家背包 / Player Inventory
```
主背包起始Y = 84
主背包行高 = 18 (SQ)
主背包3行 = 84, 102, 120

快捷栏Y = 84 + 4 + 54 = 142
(playerOffsetY + PAD/2 + 3*SQ)
```

### 过滤槽位 / Filter Slots
```
过滤槽Y = 18 + 32 = 50
(SQ + PAD * 4)

槽位背景 = guiLeft + 8 + i*18 - 1
物品渲染 = guiLeft + 8 + i*18 + 1
```

## 编译状态 / Build Status

✅ **BUILD SUCCESSFUL**
- 所有修改编译通过
- 完全符合 Cyclic 的实现标准

## 测试建议 / Testing Recommendations

1. 验证玩家背包与GUI背景完美对齐
2. 验证快捷栏位置正确
3. 验证过滤槽位与背包不重叠
4. 验证物品可以正常拖放到背包槽位
5. 验证过滤槽位点击功能正常

## 参考文档 / Reference Documents

- [Ring GUI GuiContainer Migration](Ring-GUI-GuiContainer-Migration.md)
- [Ring Filter GUI Migration Complete](Ring-Filter-GUI-Migration-Complete.md)
- [GUI Texture Migration](GUI-Texture-Migration.md)
