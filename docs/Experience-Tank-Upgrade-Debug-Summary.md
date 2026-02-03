# 经验储罐升级经验丢失问题 - 调试总结

## 问题报告
用户报告：2000MB (2级) 的储罐在升级时会丢失存储的经验。

## 已完成的工作

### 1. 代码分析 ✅
分析了经验储罐升级的完整流程：

**升级配方处理** (`CraftingUpgradeHandler.java`):
- 创建新储罐并设置容量等级 +1
- 调用 `ExperienceTankManager.preserveExperienceOnUpgrade()` 保留经验
- 同步 Capability 到 NBT

**经验保留逻辑** (`ExperienceTankManager.java`):
- 从旧储罐获取经验
- 获取新储罐容量
- 验证并限制经验（如果超过容量则截断）
- 设置经验到新储罐
- 保留其他属性（模式、保留等级、经验修补）

### 2. 添加详细日志 ✅
在 `CraftingUpgradeHandler.getCraftingResult()` 中添加了详细的调试日志：

```java
LOGGER.info("=== Experience Tank Upgrade Start ===");
LOGGER.info("Original - Levels: {}, XP: {}, Capacity: {}", ...);
LOGGER.info("After capacity set - Levels: {}, Capacity: {}", ...);
LOGGER.info("After preserve - XP: {}, Capacity: {}", ...);
LOGGER.info("Final - Levels: {}, XP: {}, Capacity: {}", ...);
LOGGER.info("=== Experience Tank Upgrade End ===");
```

这些日志将帮助追踪升级过程中每一步的数据变化。

### 3. 创建分析文档 ✅
创建了 `Experience-Tank-Upgrade-Analysis.md`，包含：
- 详细的代码流程分析
- 潜在问题点识别
- 容量计算公式说明
- 测试场景定义
- 修复方案建议

## 潜在问题点

### 问题 1：容量计算时机
在设置新容量后立即调用 `preserveExperienceOnUpgrade()`，可能存在 Capability 和 NBT 数据不同步的问题。

### 问题 2：多次同步
代码中有三次 `syncCapabilityToStack()` 调用，可能导致数据覆盖。

### 问题 3：容量验证
如果新容量计算错误，`validateCapacity()` 可能会截断经验。

## 下一步测试步骤

### 游戏内测试
1. **创建测试储罐**：
   - 使用创造模式或合成创建一个 1级储罐（1000MB）
   - 使用末影珍珠+附魔之瓶升级到 2级（2000MB）
   - 存入 1800 XP

2. **执行升级**：
   - 使用末影珍珠+附魔之瓶再次升级到 3级（4000MB）
   - 观察升级后的经验是否保留

3. **查看日志**：
   - 打开游戏日志文件（通常在 `.minecraft/logs/latest.log`）
   - 搜索 "Experience Tank Upgrade"
   - 分析每一步的数据变化

### 预期日志输出示例
```
[INFO] === Experience Tank Upgrade Start ===
[INFO] Original - Levels: 2, XP: 1800, Capacity: 2000
[INFO] After capacity set - Levels: 3, Capacity: 4000
[INFO] After preserve - XP: 1800, Capacity: 4000
[INFO] Final - Levels: 3, XP: 1800, Capacity: 4000
[INFO] === Experience Tank Upgrade End ===
```

### 如果经验丢失，日志可能显示
```
[INFO] === Experience Tank Upgrade Start ===
[INFO] Original - Levels: 2, XP: 1800, Capacity: 2000
[INFO] After capacity set - Levels: 3, Capacity: 4000
[INFO] After preserve - XP: 0, Capacity: 4000  <-- 经验丢失！
[INFO] Final - Levels: 3, XP: 0, Capacity: 4000
[INFO] === Experience Tank Upgrade End ===
```

或者：
```
[INFO] === Experience Tank Upgrade Start ===
[INFO] Original - Levels: 2, XP: 1800, Capacity: 2000
[INFO] After capacity set - Levels: 3, Capacity: 1000  <-- 容量错误！
[INFO] After preserve - XP: 1000, Capacity: 1000  <-- 经验被截断！
[INFO] Final - Levels: 3, XP: 1000, Capacity: 1000
[INFO] === Experience Tank Upgrade End ===
```

## 可能的修复方案

### 方案 1：简化同步逻辑
移除中间的同步步骤，只在最后统一同步一次：

```java
// 设置容量（不同步）
resultCapability.setCapacityLevels(originalLevels + 1);

// 保留经验（内部会同步）
result = manager.preserveExperienceOnUpgrade(pumpStack, result);

// 最后统一同步一次
resultCapability = result.getCapability(...);
ItemExperiencePump.syncCapabilityToStack(result, resultCapability);
```

### 方案 2：在 preserveExperienceOnUpgrade 中验证容量
在 `ExperienceTankManager.preserveExperienceOnUpgrade()` 开始时添加容量验证日志：

```java
public ItemStack preserveExperienceOnUpgrade(ItemStack oldTank, ItemStack newTank) {
    int newCapacity = getTankCapacity(newTank);
    LOGGER.info("preserveExperienceOnUpgrade - New tank capacity: {}", newCapacity);
    
    // 如果容量异常，记录警告
    if (newCapacity <= 0 || newCapacity < BASE_CAPACITY) {
        LOGGER.warn("Invalid new tank capacity: {}, using base capacity", newCapacity);
        newCapacity = BASE_CAPACITY;
    }
    
    // ... 其余逻辑
}
```

### 方案 3：强制刷新 Capability
在设置容量后，强制刷新 Capability：

```java
resultCapability.setCapacityLevels(originalLevels + 1);
ItemExperiencePump.syncCapabilityToStack(result, resultCapability);

// 重新获取 Capability 确保数据同步
result.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);

result = manager.preserveExperienceOnUpgrade(pumpStack, result);
```

## 编译状态
✅ **编译成功** - 所有日志代码已添加并通过编译

## 文件修改清单
1. `rsring/src/main/java/com/moremod/event/CraftingUpgradeHandler.java`
   - 添加 Logger 实例
   - 添加详细的升级过程日志
   - 记录原始数据、中间状态和最终结果

2. `rsring/docs/Experience-Tank-Upgrade-Analysis.md` (新建)
   - 详细的问题分析文档

3. `rsring/docs/Experience-Tank-Upgrade-Debug-Summary.md` (本文档)
   - 调试总结和测试指南

## 总结
已经为经验储罐升级问题添加了完整的调试日志。现在需要：

1. **启动游戏并测试升级功能**
2. **查看日志输出**，找出经验丢失的具体环节
3. **根据日志分析结果**应用相应的修复方案

日志将清楚地显示：
- 升级前的储罐状态（等级、经验、容量）
- 设置新容量后的状态
- 经验保留后的状态
- 最终同步后的状态

这将帮助我们精确定位问题所在，并实施正确的修复。
