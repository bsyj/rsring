# Baubles Integration Fixes - Design

## 1. Architecture Overview

修复Baubles集成的核心问题是：**Baubles返回的ItemStack是副本，修改后必须写回原inventory**。

### 1.1 Problem Analysis

**问题1: 戒指K键切换**
- `PacketToggleRsRing.Handler` 调用 `findAnyRingForToggle()` 找到戒指
- 修改capability后调用 `RsRingCapability.syncCapabilityToStack()`
- 但如果戒指在Baubles中，修改的是副本，没有写回Baubles inventory

**问题2: 控制器操作储罐**
- `PacketPumpAction.Handler` 已有 `syncTankBack()` 方法
- 但 `recordTankLocations()` 和 `syncTankBack()` 的实现可能不完整
- 需要确保Baubles inventory引用正确保存和使用

### 1.2 Solution Strategy

1. **统一的位置追踪系统**: 创建 `ItemLocationTracker` 类来追踪物品位置
2. **统一的同步机制**: 所有修改后都通过tracker写回原位置
3. **修复K键切换**: 在 `ClientProxy.handleToggleRsRing()` 中添加Baubles同步
4. **验证控制器同步**: 确保 `PacketPumpAction` 的同步逻辑完整

## 2. Component Design

### 2.1 ItemLocationTracker (新增)

```java
package com.moremod.util;

/**
 * 追踪物品在玩家inventory中的位置，支持Baubles
 */
public class ItemLocationTracker {
    public enum LocationType {
        MAIN_HAND,
        OFF_HAND,
        PLAYER_INVENTORY,
        BAUBLES
    }
    
    private final ItemStack item;
    private final LocationType locationType;
    private final int slotIndex;
    private final IInventory baublesInventory; // 仅Baubles使用
    
    /**
     * 将修改后的ItemStack写回原位置
     */
    public void syncBack(EntityPlayer player) {
        switch (locationType) {
            case BAUBLES:
                if (baublesInventory != null) {
                    baublesInventory.setInventorySlotContents(slotIndex, item);
                    baublesInventory.markDirty();
                }
                break;
            case PLAYER_INVENTORY:
                player.inventory.setInventorySlotContents(slotIndex, item);
                player.inventory.markDirty();
                break;
            case MAIN_HAND:
                player.setHeldItem(EnumHand.MAIN_HAND, item);
                break;
            case OFF_HAND:
                player.setHeldItem(EnumHand.OFF_HAND, item);
                break;
        }
    }
    
    /**
     * 查找物品并返回带位置信息的tracker
     */
    public static ItemLocationTracker findItem(EntityPlayer player, Class<? extends Item> itemClass) {
        // 1. 检查主手/副手
        // 2. 检查Baubles
        // 3. 检查背包
        // 返回第一个匹配的物品及其位置
    }
}
```

### 2.2 ClientProxy.handleToggleRsRing() 修复

```java
@Override
public void handleToggleRsRing(EntityPlayerMP player) {
    player.getServerWorld().addScheduledTask(() -> {
        // 使用ItemLocationTracker查找戒指
        ItemLocationTracker tracker = ItemLocationTracker.findItem(player, ItemAbsorbRing.class);
        
        if (tracker == null) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "未找到箱子戒指！"));
            return;
        }
        
        ItemStack ringStack = tracker.getItem();
        IRsRingCapability cap = ringStack.getCapability(
            RsRingCapability.RS_RING_CAPABILITY, null);
        
        if (cap != null) {
            // 切换状态
            cap.setEnabled(!cap.isEnabled());
            RsRingCapability.syncCapabilityToStack(ringStack, cap);
            
            // 写回原位置（关键！）
            tracker.syncBack(player);
            
            // 发送反馈
            String status = cap.isEnabled() ? "已开启" : "已关闭";
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "箱子戒指" + status));
        }
    });
}
```

### 2.3 PacketPumpAction 验证和修复

检查现有的 `recordTankLocations()` 和 `syncTankBack()`:

```java
// 确保recordTankLocations正确保存Baubles inventory引用
private void recordTankLocations(EntityPlayer player, List<ItemStack> tanks, 
                                 Map<ItemStack, TankLocationInfo> locations) {
    // Baubles部分
    if (Loader.isModLoaded("baubles")) {
        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                                    .invoke(null, player);
            if (handler instanceof IInventory) {
                IInventory baubles = (IInventory) handler;
                for (int i = 0; i < baubles.getSizeInventory(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                        for (ItemStack tank : tanks) {
                            if (tank == stack) { // 引用相等
                                locations.put(tank, 
                                    new TankLocationInfo("baubles", i, baubles));
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
    // ... 背包部分
}

// 确保syncTankBack正确写回
private void syncTankBack(ItemStack tank, TankLocationInfo location) {
    if (location == null) return;
    
    if ("baubles".equals(location.locationType) && location.inventory != null) {
        location.inventory.setInventorySlotContents(location.slotIndex, tank);
        location.inventory.markDirty(); // 添加markDirty
    } else if ("inventory".equals(location.locationType) && location.inventory != null) {
        location.inventory.setInventorySlotContents(location.slotIndex, tank);
        location.inventory.markDirty();
    }
}
```

## 3. Implementation Plan

### Phase 1: 创建ItemLocationTracker工具类
- 创建 `ItemLocationTracker.java`
- 实现位置追踪和同步逻辑
- 支持所有inventory类型（手持、背包、Baubles）

### Phase 2: 修复K键切换
- 修改 `ClientProxy.handleToggleRsRing()`
- 使用 `ItemLocationTracker` 查找和同步戒指
- 测试饰品栏戒指切换

### Phase 3: 验证控制器同步
- 检查 `PacketPumpAction` 的 `recordTankLocations()`
- 检查 `syncTankBack()` 实现
- 添加 `markDirty()` 调用
- 测试控制器操作饰品栏储罐

### Phase 4: 测试和验证
- 测试戒指在饰品栏时K键切换
- 测试控制器对饰品栏储罐的所有操作
- 验证状态同步正确性

## 4. Testing Strategy

### 4.1 Unit Tests
- `ItemLocationTracker.findItem()` 各种位置测试
- `ItemLocationTracker.syncBack()` 同步测试

### 4.2 Integration Tests
- 戒指在饰品栏，按K键切换，检查状态
- 储罐在饰品栏，控制器存取经验，检查容量
- 储罐在饰品栏，控制器修改模式，检查设置

### 4.3 Manual Tests
1. 将戒指放入饰品栏，按K键，观察tooltip状态变化
2. 将储罐放入饰品栏，用控制器存入经验，检查储罐容量
3. 将储罐放入饰品栏，用控制器取出经验，检查玩家经验
4. 将储罐放入饰品栏，用控制器切换模式，检查tooltip

## 5. Correctness Properties

### Property 1: 位置追踪正确性
**Property**: 对于任何在玩家inventory中的物品，`ItemLocationTracker.findItem()` 能够找到并返回正确的位置信息

**Test Strategy**: 
- 在各个位置放置物品，验证findItem返回正确的LocationType和slotIndex

### Property 2: 同步完整性
**Property**: 对于任何通过tracker修改的物品，调用 `syncBack()` 后，原位置的物品状态与修改后的状态一致

**Test Strategy**:
- 修改物品capability，调用syncBack，重新获取原位置物品，验证状态一致

### Property 3: Baubles同步正确性
**Property**: 对于Baubles中的物品，修改后调用syncBack，Baubles inventory中的ItemStack正确更新

**Test Strategy**:
- 在Baubles中放置物品，修改capability，syncBack，从Baubles重新获取，验证更新

## 6. Edge Cases

1. **Baubles未安装**: ItemLocationTracker应该跳过Baubles检查
2. **物品在多个位置**: 返回第一个找到的位置（优先级：手持 > Baubles > 背包）
3. **物品被移动**: 如果物品在修改期间被移动，syncBack可能失败（可接受）
4. **并发修改**: 服务器端单线程处理，不会有并发问题

## 7. Performance Considerations

- `ItemLocationTracker.findItem()` 需要扫描inventory，但只在需要时调用
- Baubles反射调用已经被缓存（在现有代码中）
- 同步操作是O(1)，性能影响可忽略

## 8. Compatibility

- 保持与Baubles API的兼容性
- 使用反射访问Baubles，处理ClassNotFoundException
- 不破坏现有的手持/背包功能
