# 自定义容量储罐实现方案

## 方案概述

本方案（方案B）通过NBT标签直接存储容量值，实现与玩家等级对应的储罐物品，包括：

- 100级玩家经验对应的储罐
- 500级玩家经验对应的储罐
- 1000级玩家经验对应的储罐
- 2000级玩家经验对应的储罐
- 无限容量储罐

## 技术实现

### 核心原理

1. **NBT标签存储**：使用`CustomCapacity`标签直接存储容量值
2. **无限模式标记**：使用`IsInfinite`标签标记无限容量储罐
3. **兼容性保障**：保留`TankLevel`标签以支持原系统

### 关键数据

| 玩家等级 | 总经验值 | 对应储罐等级 | 储罐容量 |
|---------|---------|------------|--------|
| 100级   | 873,320 | 11级       | 873,320 |
| 500级   | 167,754,120 | 19级   | 167,754,120 |
| 1000级  | 1,418,780,120 | 22级  | 1,418,780,120 |
| 2000级  | 11,670,582,120 | 25级 | 11,670,582,120 |

## 实现步骤

### 1. 修改核心储罐类

需要修改储罐的容量计算逻辑，优先使用自定义容量：

```java
// 在储罐类中修改容量获取方法
public long getCapacity() {
    // 检查是否为无限储罐
    if (CustomCapacityTanks.isInfinite(stack)) {
        return Long.MAX_VALUE;
    }
    
    // 检查是否有自定义容量
    long customCapacity = CustomCapacityTanks.getCustomCapacity(stack);
    if (customCapacity > 0) {
        return customCapacity;
    }
    
    // 使用默认容量计算（原系统）
    return calculateDefaultCapacity();
}
```

### 2. 注册物品和合成表

在Mod主类中注册新的储罐物品和合成表：

```java
// 注册储罐物品
GameRegistry.registerItem(new ItemExperienceTank(), "tank_level100");
GameRegistry.registerItem(new ItemExperienceTank(), "tank_level500");
GameRegistry.registerItem(new ItemExperienceTank(), "tank_level1000");
GameRegistry.registerItem(new ItemExperienceTank(), "tank_level2000");
GameRegistry.registerItem(new ItemExperienceTank(), "tank_infinite");

// 注册合成表
// 参考 CustomCapacityTanks.java 中的合成表示例
```

### 3. 添加材质和模型

创建对应的材质文件和模型文件：

- **材质文件**：`assets/moremod/textures/items/tank_*.png`
- **模型文件**：`assets/moremod/models/item/tank_*.json`

### 4. 配置语言文件

在语言文件中添加物品名称和描述：

```json
{
  "item.moremod:tank_level100.name": "经验储罐 - 100级",
  "item.moremod:tank_level100.desc": "存储容量：873,320 XP",
  "item.moremod:tank_level100.desc2": "可存储最多100级经验",
  
  "item.moremod:tank_level500.name": "经验储罐 - 500级",
  "item.moremod:tank_level500.desc": "存储容量：167,754,120 XP",
  "item.moremod:tank_level500.desc2": "可存储最多500级经验",
  
  "item.moremod:tank_level1000.name": "经验储罐 - 1000级",
  "item.moremod:tank_level1000.desc": "存储容量：1,418,780,120 XP",
  "item.moremod:tank_level1000.desc2": "可存储最多1000级经验",
  
  "item.moremod:tank_level2000.name": "经验储罐 - 2000级",
  "item.moremod:tank_level2000.desc": "存储容量：11,670,582,120 XP",
  "item.moremod:tank_level2000.desc2": "可存储最多2000级经验",
  
  "item.moremod:tank_infinite.name": "经验储罐 - 无限",
  "item.moremod:tank_infinite.desc": "存储容量：无限",
  "item.moremod:tank_infinite.desc2": "可存储无限经验"
}
```

## 兼容性分析

### 与原系统的兼容性

1. **正向兼容**：原有的储罐系统不受影响
2. **反向兼容**：新储罐可以在原系统中正常使用
3. **数据迁移**：无需数据迁移，新系统自动处理

### 潜在问题与解决方案

| 潜在问题 | 解决方案 |
|---------|---------|
| NBT数据丢失 | 在物品构造和交互时确保正确读写NBT |
| 合成表冲突 | 使用独特的材料组合 |
| 无限储罐逻辑漏洞 | 在所有经验操作中添加无限模式检查 |
| 性能问题 | 对大数值计算进行缓存 |

## 使用方法

### 创建储罐物品

```java
// 创建100级储罐
ItemStack tank100 = CustomCapacityTanks.createLevel100Tank();

// 创建500级储罐
ItemStack tank500 = CustomCapacityTanks.createLevel500Tank();

// 创建1000级储罐
ItemStack tank1000 = CustomCapacityTanks.createLevel1000Tank();

// 创建2000级储罐
ItemStack tank2000 = CustomCapacityTanks.createLevel2000Tank();

// 创建无限储罐
ItemStack tankInfinite = CustomCapacityTanks.createInfiniteTank();
```

### 检查储罐类型

```java
// 检查是否为无限储罐
if (CustomCapacityTanks.isInfinite(tank)) {
    // 处理无限储罐逻辑
}

// 获取储罐容量
long capacity = CustomCapacityTanks.getTankCapacity(tank);
```

## 结论

本方案通过NBT标签直接存储容量值，实现了与玩家等级精确对应的储罐物品，同时保持了与原系统的完全兼容性。这种实现方式：

1. **技术可行性**：完全可行，基于现有的NBT系统
2. **兼容性**：与原系统无缝集成
3. **可扩展性**：易于添加新的等级对应储罐
4. **用户体验**：为玩家提供了清晰的容量标识

通过合理的材质设计和合成配方，可以为玩家提供从基础到高级的完整储罐体系，满足不同阶段的经验存储需求。