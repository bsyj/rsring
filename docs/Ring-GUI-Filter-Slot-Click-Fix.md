# 戒指GUI过滤槽位点击修复

## 问题描述
在戒指的GUI中无法添加物品到过滤槽位（黑白名单）。

## 解决方案
完全参考 Cyclic 的 `GuiItemPump` 和 `GuiBaseContainer` 实现，使用 `mouseClickedWrapper` 模式。

## 关键实现细节

### 1. 事件处理顺序
```java
@Override
protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    // 1. 先检查按钮点击
    if (isMouseOverButton(...)) {
        // 处理按钮点击
        return; // 拦截，不传递给Container
    }
    
    // 2. 再检查过滤槽位点击
    for (int i = 0; i < SLOT_COUNT; i++) {
        if (isPointInRegion(...)) {
            handleFilterSlotClick(i);
            return; // 拦截，不传递给Container
        }
    }
    
    // 3. 最后才调用 super.mouseClicked（处理玩家背包槽位）
    super.mouseClicked(mouseX, mouseY, mouseButton);
}
```

### 2. Ghost Item 实现
```java
private void handleFilterSlotClick(int slot) {
    // 获取鼠标上的物品（完全参考 Cyclic）
    ItemStack stackInMouse = this.mc.player.inventory.getItemStack();
    
    if (stackInMouse.isEmpty()) {
        // 清空过滤槽位
        capability.setFilterSlot(slot, "");
    } else {
        // 仅读取物品类型，不消耗物品（Ghost Item）
        String name = stackInMouse.getItem().getRegistryName().toString();
        capability.setFilterSlot(slot, name);
    }
    
    // 同步到服务器
    RsRingCapability.syncCapabilityToStack(ringStack, capability);
    // 发送网络包...
}
```

### 3. 精确的点击检测
```java
// 使用 isPointInRegion 进行精确检测（SQ - 2 = 16 像素可点击区域）
int slotX = SLOTX_START + i * SQ;
int slotY = SLOTY;
if (isPointInRegion(slotX, slotY, SQ - 2, SQ - 2, mouseX, mouseY)) {
    handleFilterSlotClick(i);
    return;
}
```

## 参考文件
- `Cyclic-trunk-1.12/src/main/java/com/lothrazar/cyclicmagic/gui/GuiBaseContainer.java`
  - `mouseClickedWrapper()` 方法
  - Ghost Item 实现模式
- `Cyclic-trunk-1.12/src/main/java/com/lothrazar/cyclicmagic/block/cablepump/item/GuiItemPump.java`
  - 过滤槽位布局和点击处理

## 测试步骤
1. 编译代码：`.\gradlew.bat build -x test --console=plain`
2. 启动游戏并打开戒指GUI
3. 测试以下场景：
   - 从背包点击物品，再点击过滤槽位 → 物品应该作为Ghost Item出现在槽位中
   - 背包中的物品不应该被消耗
   - 点击已填充的槽位（鼠标为空）→ 应该清空槽位
   - 点击空槽位（鼠标为空）→ 不应该有任何操作
   - 切换黑白名单模式按钮应该正常工作
   - 点击背包槽位应该正常移动物品

## 编译状态
✅ 编译成功（2026-02-02）

## 下一步
需要在游戏中测试所有功能是否正常工作。
