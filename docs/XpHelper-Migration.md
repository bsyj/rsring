# 经验计算系统迁移文档

## 概述

本文档说明了从 SophisticatedBackpacks 的 Experience Pump Upgrade 迁移精确经验计算方法到本项目的过程。

## 迁移内容

### 1. 核心工具类：XpHelper

创建了新的 `com.moremod.util.XpHelper` 工具类，提供以下功能：

#### 1.1 经验与液体的转换
```java
// 转换比例：1 经验点 = 20 mB 液体
public static float liquidToExperience(int liquid)
public static int experienceToLiquid(float xp)
```

#### 1.2 等级与经验的精确转换
使用 Minecraft 官方公式：
- **0-15级**：`XP = level * (12 + level * 2) / 2`
- **16-30级**：`XP = (level - 15) * (69 + (level - 15) * 5) / 2 + 315`
- **31+级**：`XP = (level - 30) * (215 + (level - 30) * 9) / 2 + 1395`

```java
public static int getExperienceForLevel(int level)
public static int getLevelForExperience(int experience)
public static double getLevelsForExperience(int experience)
public static int getExperienceLimitOnLevel(int level)
```

#### 1.3 玩家经验操作
```java
public static int getPlayerTotalExperience(EntityPlayer player)
public static void setPlayerTotalExperience(EntityPlayer player, int experience)
public static int removeExperienceFromPlayer(EntityPlayer player, int amount)
public static void addExperienceToPlayer(EntityPlayer player, int amount)
```

#### 1.4 经验格式化
```java
public static String formatExperience(int xp)
// 输出格式："{xp} XP ({levels} levels)"
// 例如："315 XP (15.0 levels)"
```

### 2. 更新的类

#### 2.1 ExperiencePumpController
所有经验计算方法现在委托给 `XpHelper`：
- `convertXPToLevel(int xp)` - 经验转等级
- `convertLevelToXP(double level)` - 等级转经验
- `getPlayerTotalExperience(EntityPlayer player)` - 获取玩家总经验
- `getXPToNextLevel(int currentLevel)` - 获取升级所需经验
- `addExperienceToPlayer(...)` - 添加经验
- `removeExperienceFromPlayer(...)` - 移除经验
- `formatExperienceDisplay(int xp)` - 格式化显示

#### 2.2 ItemExperiencePump
更新了内部经验计算方法：
- `getPlayerTotalXp(EntityPlayer player)` - 使用 XpHelper
- `getTotalXpForLevel(int level)` - 使用 XpHelper
- `addPlayerXp(EntityPlayer player, int amount)` - 使用 XpHelper

### 3. 测试覆盖

创建了 `XpHelperTest` 单元测试，覆盖：
- 液体与经验的转换
- 等级到经验的转换
- 经验到等级的转换
- 精确等级计算（包含小数）
- 升级所需经验
- 往返转换准确性
- 经验格式化
- 边界情况处理

## 关键改进

### 1. 精确性
- 使用 Minecraft 官方公式，确保与游戏内经验系统完全一致
- 避免浮点数精度问题，使用整数计算
- 正确处理等级边界（0-15、16-30、31+）

### 2. 一致性
- 所有经验计算统一使用 XpHelper
- 消除了代码重复
- 确保整个项目使用相同的计算逻辑

### 3. 可维护性
- 集中管理经验计算逻辑
- 易于测试和验证
- 清晰的文档和注释

## 使用示例

### 示例 1：获取玩家总经验
```java
EntityPlayer player = ...;
int totalXP = XpHelper.getPlayerTotalExperience(player);
System.out.println("玩家总经验：" + totalXP);
```

### 示例 2：计算等级对应的经验
```java
int level = 30;
int xp = XpHelper.getExperienceForLevel(level);
System.out.println("30级需要 " + xp + " 经验"); // 输出：30级需要 1395 经验
```

### 示例 3：从玩家移除经验
```java
EntityPlayer player = ...;
int removeAmount = 100;
int actualRemoved = XpHelper.removeExperienceFromPlayer(player, removeAmount);
System.out.println("实际移除了 " + actualRemoved + " 经验");
```

### 示例 4：格式化经验显示
```java
int xp = 315;
String formatted = XpHelper.formatExperience(xp);
System.out.println(formatted); // 输出：315 XP (15.0 levels)
```

### 示例 5：经验与液体转换
```java
// 将 100 经验点转换为液体
int liquid = XpHelper.experienceToLiquid(100.0f);
System.out.println(liquid + " mB"); // 输出：2000 mB

// 将 2000 mB 液体转换为经验
float xp = XpHelper.liquidToExperience(2000);
System.out.println(xp + " XP"); // 输出：100.0 XP
```

## 验证方法

### 运行单元测试
```bash
./gradlew test --tests XpHelperTest
```

### 游戏内验证
1. 创建一个经验储罐
2. 存入已知数量的经验（例如 315 XP = 15级）
3. 检查显示是否正确
4. 取出经验，验证玩家等级是否正确

## 与 SophisticatedBackpacks 的对比

| 功能 | SophisticatedBackpacks | 本项目 |
|------|----------------------|--------|
| 经验转液体比例 | 1 XP = 20 mB | ✅ 相同 |
| 等级计算公式 | Minecraft 官方公式 | ✅ 相同 |
| 玩家总经验计算 | `currentLevelPoints + partialLevelPoints` | ✅ 相同 |
| 经验存取 | 通过 FluidHandler | 通过 Capability |
| 自动修补 | 支持 | ✅ 支持 |

## 注意事项

1. **浮点数精度**：所有计算尽可能使用整数，避免浮点数累积误差
2. **边界处理**：正确处理 0 级、负数、极大值等边界情况
3. **性能考虑**：XpHelper 的所有方法都是静态的，无需创建实例
4. **线程安全**：XpHelper 是无状态的，可以安全地在多线程环境中使用

## 未来改进

1. 考虑添加经验倍率系统
2. 支持自定义经验公式（通过配置）
3. 添加经验转换的性能优化（缓存常用等级的经验值）
4. 支持更多的经验来源（如任务、成就等）

## 参考资料

- [Minecraft Wiki - Experience](https://minecraft.fandom.com/wiki/Experience)
- [SophisticatedBackpacks GitHub](https://github.com/P3pp3rF1y/SophisticatedBackpacks)
- Minecraft 1.12.2 源代码分析
