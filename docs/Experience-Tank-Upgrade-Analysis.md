# 经验储罐升级经验丢失问题分析

## 问题描述
用户报告：2000MB (2级) 的储罐在升级时会丢失存储的经验。

## 代码流程分析

### 升级配方处理 (`CraftingUpgradeHandler.java`)

```java
// 1. 创建新储罐并设置容量等级 +1 (第 68-73 行)
IExperiencePumpCapability resultCapability = result.getCapability(...);
resultCapability.setCapacityLevels(originalLevels + 1);
ItemExperiencePump.syncCapabilityToStack(result, resultCapability);

// 2. 保留经验 (第 77 行)
result = manager.preserveExperienceOnUpgrade(pumpStack, result);

// 3. 再次同步 (第 80-83 行)
resultCapability = result.getCapability(...);
ItemExperiencePump.syncCapabilityToStack(result, resultCapability);
```

### 经验保留逻辑 (`ExperienceTankManager.java`)

```java
public ItemStack preserveExperienceOnUpgrade(ItemStack oldTank, ItemStack newTank) {
    // 1. 获取旧储罐的经验
    int storedExperience = getStoredExperience(oldTank);
    
    // 2. 获取新储罐的容量
    int newCapacity = getTankCapacity(newTank);
    
    // 3. 验证并限制经验（如果超过容量则截断）
    int preservedExperience = validateCapacity(storedExperience, newCapacity);
    
    // 4. 设置经验到新储罐
    setStoredExperience(newTank, preservedExperience);
    
    // 5. 保留其他属性（模式、保留等级、经验修补）
    preserveTankProperties(oldTank, newTank);
    
    return newTank;
}
```

## 潜在问题点

### 问题 1：容量计算时机
在 `getCraftingResult()` 中：
1. 第 68-73 行：设置新容量等级并同步
2. 第 77 行：调用 `preserveExperienceOnUpgrade()`

**可能的问题**：
- 如果 `preserveExperienceOnUpgrade()` 在获取新容量时，Capability 还没有正确更新
- 或者 NBT 数据和 Capability 数据不同步

### 问题 2：Capability 同步
代码中有三次同步操作：
1. 第 73 行：设置容量后同步
2. 第 77 行：`preserveExperienceOnUpgrade()` 内部的 `setStoredExperience()` 会同步
3. 第 82 行：最后再次同步

**可能的问题**：
- 多次同步可能导致数据覆盖
- 最后一次同步可能覆盖了之前保存的经验数据

### 问题 3：容量验证
在 `validateCapacity()` 中：
```java
if (storedXP > maxCapacity) {
    LOGGER.debug("Stored XP {} exceeds capacity {}, capping", storedXP, maxCapacity);
    return maxCapacity;
}
```

**可能的问题**：
- 如果新容量计算错误（比如还是旧容量），经验可能被截断
- 2级储罐容量应该是 2000MB，如果计算成 1000MB，经验就会丢失

## 容量计算公式

根据 `IExperiencePumpCapability.java`：
```java
// 基础容量
int BASE_XP_PER_LEVEL = 1000;

// 容量计算公式
maxCapacity = BASE_XP_PER_LEVEL * 2^(capacityLevels-1)
```

等级对应容量：
- 1级 = 1000 MB (1000 * 2^0)
- 2级 = 2000 MB (1000 * 2^1)
- 3级 = 4000 MB (1000 * 2^2)
- 4级 = 8000 MB (1000 * 2^3)
- ...

## 测试场景

### 场景 1：1级升级到2级（1000MB → 2000MB）
- 旧储罐：1级，存储 800 XP
- 升级后：2级，容量 2000 MB
- 预期结果：保留 800 XP ✓

### 场景 2：2级升级到3级（2000MB → 4000MB）
- 旧储罐：2级，存储 1800 XP
- 升级后：3级，容量 4000 MB
- 预期结果：保留 1800 XP
- **用户报告**：经验丢失 ✗

## 调试建议

### 1. 添加日志
在 `CraftingUpgradeHandler.getCraftingResult()` 中添加详细日志：

```java
// 在第 68 行之前
LOGGER.info("=== Upgrade Start ===");
LOGGER.info("Original levels: {}", originalLevels);
LOGGER.info("Original XP: {}", ItemExperiencePump.getXpStoredFromNBT(pumpStack));
LOGGER.info("Original capacity: {}", ItemExperiencePump.getMaxXpFromNBT(pumpStack));

// 在第 73 行之后
LOGGER.info("New levels set to: {}", resultCapability.getCapacityLevels());
LOGGER.info("New capacity: {}", resultCapability.getMaxXp());

// 在第 77 行之后
LOGGER.info("After preserve - XP: {}", ItemExperiencePump.getXpStoredFromNBT(result));
LOGGER.info("After preserve - Capacity: {}", ItemExperiencePump.getMaxXpFromNBT(result));

// 在第 82 行之后
LOGGER.info("Final - XP: {}", ItemExperiencePump.getXpStoredFromNBT(result));
LOGGER.info("Final - Capacity: {}", ItemExperiencePump.getMaxXpFromNBT(result));
LOGGER.info("=== Upgrade End ===");
```

### 2. 检查 Capability 实现
查看 `ExperiencePumpCapability.java` 中的 `getMaxXp()` 实现是否正确。

### 3. 检查 NBT 同步
确认 `ItemExperiencePump.syncCapabilityToStack()` 是否正确同步所有数据。

## 修复方案（如果确认是问题）

### 方案 1：调整同步顺序
```java
// 先设置容量
resultCapability.setCapacityLevels(originalLevels + 1);

// 然后保留经验（此时容量已更新）
result = manager.preserveExperienceOnUpgrade(pumpStack, result);

// 最后统一同步一次
resultCapability = result.getCapability(...);
ItemExperiencePump.syncCapabilityToStack(result, resultCapability);
```

### 方案 2：在 preserveExperienceOnUpgrade 中处理容量
```java
public ItemStack preserveExperienceOnUpgrade(ItemStack oldTank, ItemStack newTank) {
    // 确保新储罐的容量已经正确设置
    int newCapacity = getTankCapacity(newTank);
    LOGGER.debug("New tank capacity: {}", newCapacity);
    
    // ... 其余逻辑
}
```

### 方案 3：移除多余的同步
只在最后同步一次，避免中间状态覆盖。

## 下一步行动

1. **添加日志**：在升级过程中添加详细日志，观察每一步的数据变化
2. **游戏内测试**：
   - 创建一个 2级储罐（2000MB）
   - 存入 1800 XP
   - 使用末影珍珠+附魔之瓶升级
   - 检查升级后的经验是否保留
3. **查看日志输出**：分析日志找出经验丢失的具体环节
4. **应用修复**：根据日志分析结果应用相应的修复方案
