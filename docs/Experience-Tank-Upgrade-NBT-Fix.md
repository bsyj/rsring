# 经验储罐升级XP丢失问题修复

## 问题描述

用户报告：等级2以上的储罐在工作台升级时会丢失经验，新的储罐只有1000mb。
例如：6级2542mb储罐升级到7级时，新的储罐变成7级1000mb（经验丢失）。

## 根本原因分析

### 问题根源

在 `CraftingUpgradeHandler.getCraftingResult()` 方法中：

1. **新ItemStack创建**：`ItemStack result = new ItemStack(RsRingMod.experiencePump)`
2. **Capability初始化**：当创建新ItemStack时，`initCapabilities()` 被调用，但传入的 `nbt` 参数为 `null`
3. **默认值问题**：新的capability使用默认值初始化：
   - `capacityLevels = 1`（默认1级）
   - `xpStored = 0`（默认0经验）
4. **设置顺序问题**：
   - 调用 `setCapacityLevels(newLevels)` 时，capability内部会保存当前XP（此时是0！）
   - 然后调用 `setXpStored(originalXP)` 设置正确的XP
   - 最后调用 `syncCapabilityToStack()` 同步到NBT
5. **同步失败**：由于capability系统的复杂性，数据可能没有正确同步到ItemStack的NBT中

### 为什么会丢失经验？

关键问题在于：**新创建的ItemStack的capability是全新初始化的，不包含原储罐的任何数据**。虽然我们尝试通过capability API设置数据，但在工作台合成的特殊环境下，这些数据可能没有正确持久化到ItemStack的NBT标签中。

## 解决方案

### 修复策略

**直接操作NBT，绕过capability初始化问题**

不再依赖capability API来传递数据，而是：
1. 从原储罐的NBT中读取所有数据
2. 直接构造新储罐的NBT标签
3. 将NBT标签设置到新ItemStack上

### 代码修改

**文件**：`rsring/src/main/java/com/moremod/event/CraftingUpgradeHandler.java`

**修改前**（使用capability API）：
```java
// 创建新ItemStack
ItemStack result = new ItemStack(RsRingMod.experiencePump);

// 获取capability并设置数据
IExperiencePumpCapability resultCapability = result.getCapability(...);
resultCapability.setCapacityLevels(newLevels);
resultCapability.setXpStored(originalXP);
// ... 设置其他属性
ItemExperiencePump.syncCapabilityToStack(result, resultCapability);
```

**修改后**（直接操作NBT）：
```java
// 从原储罐读取数据（优先从NBT读取）
int originalLevels = ItemExperiencePump.getCapacityLevelsFromNBT(pumpStack);
int originalXP = ItemExperiencePump.getXpStoredFromNBT(pumpStack);

// 创建新ItemStack并直接写入NBT
ItemStack result = new ItemStack(RsRingMod.experiencePump);
int newLevels = originalLevels + 1;

// 直接构造NBT数据
NBTTagCompound stackTag = new NBTTagCompound();
NBTTagCompound dataTag = new NBTTagCompound();

dataTag.setInteger("xp", originalXP);
dataTag.setInteger("capacityLevels", newLevels);
dataTag.setInteger("mode", originalMode);
dataTag.setInteger("retainLevel", originalRetainLevel);
dataTag.setBoolean("mending", originalMending);

stackTag.setTag(ItemExperiencePump.XP_TAG, dataTag);
result.setTagCompound(stackTag);
```

### 优势

1. **可靠性**：直接操作NBT，避免capability初始化和同步问题
2. **简洁性**：代码更简单，逻辑更清晰
3. **一致性**：与 `ItemExperiencePump` 中的 `getXpStoredFromNBT()` 等方法保持一致
4. **调试友好**：添加了详细的日志输出，便于追踪问题

## 测试验证

### 测试步骤

1. 准备一个6级储罐，存储2542mb经验
2. 在工作台中使用1个末影珍珠 + 1个附魔之瓶升级
3. 检查升级后的储罐：
   - 等级应为7级
   - 经验应为2542mb（不丢失）
   - 耐久条应正确显示填充比例

### 预期日志输出

```
[INFO] === Experience Tank Upgrade Start ===
[INFO] Original - Levels: 6, XP: 2542
[INFO] After NBT write - Levels: 7, XP: 2542
[INFO] NBT verification - Levels: 7, XP: 2542, MaxXP: 64000
[INFO] === Experience Tank Upgrade End ===
```

## 技术要点

### NBT数据结构

经验储罐的NBT结构：
```
ItemStack NBT:
  ExperiencePumpData:
    xp: int (存储的经验值)
    capacityLevels: int (容量等级)
    mode: int (泵送模式)
    retainLevel: int (保留等级)
    mending: boolean (是否用于修补)
```

### 容量计算公式

```java
maxXp = BASE_XP_PER_LEVEL * 2^(capacityLevels - 1)
```

示例：
- 1级：1000 * 2^0 = 1,000 mb
- 2级：1000 * 2^1 = 2,000 mb
- 3级：1000 * 2^2 = 4,000 mb
- 6级：1000 * 2^5 = 32,000 mb
- 7级：1000 * 2^6 = 64,000 mb

## 相关文件

- `rsring/src/main/java/com/moremod/event/CraftingUpgradeHandler.java` - 升级配方处理
- `rsring/src/main/java/com/moremod/item/ItemExperiencePump.java` - 经验储罐物品
- `rsring/src/main/java/com/moremod/capability/ExperiencePumpCapability.java` - 经验储罐能力

## 修复日期

2026-02-02

## 状态

✅ 已修复并编译成功，等待游戏内测试验证
