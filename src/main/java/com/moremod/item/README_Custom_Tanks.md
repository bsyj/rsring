# 自定义等级经验储罐实现

## 概述

本实现添加了四种新的经验储罐物品，对应不同玩家等级的经验容量：

- 100级经验储罐：容量 873,320 XP
- 500级经验储罐：容量 167,754,120 XP
- 1000级经验储罐：容量 1,418,780,120 XP
- 2000级经验储罐：容量 11,670,582,120 XP

## 实现文件

### 物品类

1. **ItemExperienceTank100.java** - 100级经验储罐
2. **ItemExperienceTank500.java** - 500级经验储罐
3. **ItemExperienceTank1000.java** - 1000级经验储罐
4. **ItemExperienceTank2000.java** - 2000级经验储罐

### 合成表

**CustomTankRecipes.java** - 注册所有自定义储罐的合成表

## 注册步骤

### 1. 在Mod主类中注册物品

```java
// 在RsRingMod.java中添加
public static Item experienceTank100;   // 100级经验储罐
public static Item experienceTank500;   // 500级经验储罐
public static Item experienceTank1000;  // 1000级经验储罐
public static Item experienceTank2000;  // 2000级经验储罐

// 在preInit方法中初始化
@Override
public void preInit(FMLPreInitializationEvent event) {
    // 现有代码...
    
    // 注册自定义经验储罐
    experienceTank100 = new ItemExperienceTank100();
    GameRegistry.register(experienceTank100);
    
    experienceTank500 = new ItemExperienceTank500();
    GameRegistry.register(experienceTank500);
    
    experienceTank1000 = new ItemExperienceTank1000();
    GameRegistry.register(experienceTank1000);
    
    experienceTank2000 = new ItemExperienceTank2000();
    GameRegistry.register(experienceTank2000);
    
    // 注册合成表
    CustomTankRecipes.registerRecipes();
    
    // 现有代码...
}
```

### 2. 添加材质文件

在 `assets/moremod/textures/items/` 目录下创建以下纹理文件：

- `experience_tank_100.png` - 100级储罐纹理（建议使用绿色调）
- `experience_tank_500.png` - 500级储罐纹理（建议使用蓝色调）
- `experience_tank_1000.png` - 1000级储罐纹理（建议使用紫色调）
- `experience_tank_2000.png` - 2000级储罐纹理（建议使用金色调）

### 3. 添加模型文件

在 `assets/moremod/models/item/` 目录下创建以下模型文件：

**experience_tank_100.json**:
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "moremod:items/experience_tank_100"
  }
}
```

**experience_tank_500.json**:
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "moremod:items/experience_tank_500"
  }
}
```

**experience_tank_1000.json**:
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "moremod:items/experience_tank_1000"
  }
}
```

**experience_tank_2000.json**:
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "moremod:items/experience_tank_2000"
  }
}
```

### 4. 添加语言文件条目

在语言文件中添加以下条目：

```json
{
  "item.rsring:experience_tank_100.name": "经验储罐 - 100级",
  "item.rsring:experience_tank_500.name": "经验储罐 - 500级",
  "item.rsring:experience_tank_1000.name": "经验储罐 - 1000级",
  "item.rsring:experience_tank_2000.name": "经验储罐 - 2000级"
}
```

## 合成表

| 储罐等级 | 合成材料 | 配方形状 |
|---------|---------|----------|
| 100级   | 铁锭 + 基础储罐 | 铁锭围绕基础储罐 |
| 500级   | 金锭 + 100级储罐 | 金锭围绕100级储罐 |
| 1000级  | 绿宝石 + 500级储罐 | 绿宝石围绕500级储罐 |
| 2000级  | 钻石 + 1000级储罐 | 钻石围绕1000级储罐 |

## 功能特性

1. **完全兼容**：与原经验储罐系统完全兼容
2. **相同功能**：支持经验吸收、存储、提取、自动修补等所有功能
3. **智能显示**：右键点击显示当前存储量和总容量
4. **详细tooltip**：按住Shift查看详细信息
5. **耐久条**：显示经验填充状态

## 使用方法

1. **合成储罐**：使用对应材料在工作台合成
2. **存储经验**：使用经验泵控制器将经验存入储罐
3. **提取经验**：使用经验泵控制器从储罐中提取经验
4. **自动吸收**：佩戴或手持时自动吸收周围经验
5. **自动修补**：开启修补模式后自动修复装备

## 注意事项

1. **容量限制**：每个储罐的容量固定对应相应玩家等级的经验值
2. **升级系统**：这些储罐不支持原有的升级系统，因为它们已经是特定等级的最终形态
3. **控制器兼容**：所有储罐都可以被经验泵控制器管理
4. **Baubles兼容**：支持在饰品栏中使用

## 技术细节

- **继承结构**：所有自定义储罐都继承自 `ItemExperiencePump`
- **NBT存储**：使用相同的NBT结构，确保与原系统兼容
- **Capability系统**：使用相同的Capability接口，确保功能一致
- **经验计算**：使用与原系统相同的经验计算逻辑

## 故障排除

### 常见问题

1. **物品不显示**：检查材质文件是否正确放置
2. **合成表不存在**：确保调用了 `CustomTankRecipes.registerRecipes()`
3. **功能不正常**：检查物品注册是否正确
4. **控制器不识别**：确保储罐使用与原系统相同的NBT结构

### 调试建议

- 在游戏中使用 `/give` 命令获取储罐进行测试
- 检查控制台输出是否有错误信息
- 验证合成表是否正确显示在工作台中
- 测试所有功能：经验吸收、存储、提取、修补

## 结论

这些自定义等级经验储罐为玩家提供了更直观、更符合需求的经验存储选项，每个储罐都对应一个特定的玩家等级，容量精确匹配该等级所需的经验值。通过合理的合成配方和美观的材质，它们将成为玩家经验管理的重要工具。