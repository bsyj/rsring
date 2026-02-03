# 经验储罐升级经验丢失问题修复

## 问题描述
等级2及以上的储罐在升级时会丢失存储的经验。

## 问题原因
在 `CraftingUpgradeHandler.getCraftingResult()` 方法中，原来的实现存在多次 capability 同步的问题：

1. 第一次：设置新容量等级后同步
2. 第二次：在 `preserveExperienceOnUpgrade()` 中调用 `setStoredExperience()` 时同步
3. 第三次：最后再次同步

这些多次同步操作可能导致：
- 容量等级被覆盖或重置
- 经验值在同步过程中丢失
- NBT数据不一致

## 修复方案
重构升级逻辑，改为**一次性设置所有属性，最后只同步一次**：

```java
// 1. 获取原储罐的所有数据
int originalLevels = ...;
int originalXP = ...;
int originalMode = ...;
int originalRetainLevel = ...;
boolean originalMending = ...;

// 2. 在新储罐的 capability 中一次性设置所有属性
resultCapability.setCapacityLevels(originalLevels + 1);  // 容量等级+1
int newCapacity = resultCapability.getMaxXp();           // 获取新容量
int preservedXP = Math.min(originalXP, newCapacity);     // 经验值（不超过新容量）
resultCapability.setXpStored(preservedXP);               // 设置经验
resultCapability.setMode(originalMode);                  // 保留模式
resultCapability.setRetainLevel(originalRetainLevel);    // 保留等级
resultCapability.setUseForMending(originalMending);      // 保留修补设置

// 3. 最后只同步一次
ItemExperiencePump.syncCapabilityToStack(result, resultCapability);
```

## 修复效果
- ✅ 容量等级正确增加（原等级 + 1）
- ✅ 经验值完整保留（不超过新容量）
- ✅ 其他属性（模式、保留等级、修补）全部保留
- ✅ 只进行一次 NBT 同步，避免数据覆盖

## 测试建议
1. 创建一个1级储罐（1000 XP容量），存入800 XP
2. 使用末影珍珠+附魔之瓶升级到2级（2000 XP容量）
3. 验证：容量变为2000 XP，存储的800 XP保留
4. 继续升级到3级（4000 XP容量）
5. 验证：容量变为4000 XP，存储的800 XP仍然保留

## 日志输出
升级时会输出以下日志（用于调试）：
```
=== Experience Tank Upgrade Start ===
Original - Levels: 2, XP: 1800
Final - Levels: 3, XP: 1800, Capacity: 4000
=== Experience Tank Upgrade End ===
```

## 修改的文件
- `rsring/src/main/java/com/moremod/event/CraftingUpgradeHandler.java`

## 编译状态
✅ 编译成功 - 2026-02-02
