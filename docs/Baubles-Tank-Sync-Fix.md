# 饰品栏储罐同步修复

## 问题描述
用户报告：控制器能找到饰品栏（Baubles）的储罐，但是不能进行操作，也没有功能。

## 问题原因
从 Baubles 饰品栏获取的 `ItemStack` 是一个副本，对它的修改不会自动同步回饰品栏的实际物品。这是 Minecraft/Forge 的一个常见问题：某些背包（特别是 Baubles 饰品栏）返回的是 ItemStack 副本，修改后需要显式调用 `setInventorySlotContents()` 才能同步回去。

## 解决方案

### 修改文件
- `rsring/src/main/java/com/moremod/network/PacketPumpAction.java`

### 实现细节

#### 1. 添加位置记录机制
在 `onMessage` 方法开始时，记录所有储罐的位置信息：

```java
java.util.List<ItemStack> tankStacks = findAllExperienceTanks(player);
// 记录储罐位置信息，用于后续同步
java.util.Map<ItemStack, TankLocationInfo> tankLocations = new java.util.HashMap<>();
recordTankLocations(player, tankStacks, tankLocations);
```

#### 2. 添加辅助类和方法

**TankLocationInfo 类**：
```java
private static class TankLocationInfo {
    final String locationType; // "baubles", "inventory", "hand"
    final int slotIndex;
    final net.minecraft.inventory.IInventory inventory;
    
    TankLocationInfo(String locationType, int slotIndex, net.minecraft.inventory.IInventory inventory) {
        this.locationType = locationType;
        this.slotIndex = slotIndex;
        this.inventory = inventory;
    }
}
```

**recordTankLocations 方法**：
- 扫描饰品栏、背包、手持位置
- 记录每个储罐的位置信息（类型、槽位索引、IInventory引用）
- 使用引用相等（`==`）来匹配储罐

**syncTankBack 方法**：
```java
private void syncTankBack(ItemStack tank, TankLocationInfo location) {
    if (location == null) return;
    
    if ("baubles".equals(location.locationType) && location.inventory != null) {
        location.inventory.setInventorySlotContents(location.slotIndex, tank);
    } else if ("inventory".equals(location.locationType) && location.inventory != null) {
        location.inventory.setInventorySlotContents(location.slotIndex, tank);
    }
    // 手持物品会自动同步，无需特殊处理
}
```

#### 3. 在每次修改储罐后同步
在所有修改储罐的地方，添加同步调用：

```java
ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
syncTankBack(tankStack, tankLocations.get(tankStack)); // 新增：同步回原位置
```

修改位置包括：
- ACTION_MODE / ACTION_RETAIN_UP / ACTION_RETAIN_DOWN / ACTION_MENDING
- ACTION_TAKE_ALL
- ACTION_TAKE_ONE
- ACTION_STORE_ALL
- ACTION_STORE_ONE

## 工作原理

1. **记录阶段**：在操作开始时，扫描所有储罐并记录它们的位置信息（饰品栏/背包/手持，以及具体槽位）

2. **操作阶段**：正常执行储罐操作（修改 Capability 数据）

3. **同步阶段**：每次修改储罐后，立即调用 `syncTankBack()` 将修改后的 ItemStack 写回原位置

4. **关键点**：
   - 对于饰品栏：调用 `inventory.setInventorySlotContents(slotIndex, tank)`
   - 对于背包：调用 `player.inventory.setInventorySlotContents(slotIndex, tank)`
   - 对于手持：无需特殊处理（自动同步）

## 测试建议

1. **将储罐放入饰品栏**
2. **打开控制器GUI**
3. **测试所有操作**：
   - 模式切换（关闭/抽→罐/罐→人）
   - 保留等级调整
   - 经验修补开关
   - 全部取出
   - 取出N级
   - 存入N级
   - 全部存入
4. **确认储罐数据正确更新**（查看储罐tooltip）
5. **确认玩家经验正确变化**

## 编译状态
✅ **编译成功** - 修复已实现并通过编译

## 相关问题
- Baubles API 的 `getStackInSlot()` 返回副本而非引用
- 需要显式调用 `setInventorySlotContents()` 来同步修改
- 这是 Minecraft 1.12.2 + Baubles 的已知行为

## 参考
- Baubles API 文档
- Forge IInventory 接口
- SophisticatedBackpacks 中的类似实现（使用 `setStackInSlot` 同步）
