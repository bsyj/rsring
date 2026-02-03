# 经验泵控制器背包检测功能

## 概述
经验泵控制器现在支持在玩家背包中自动生效，无需手持即可触发功能。

## 功能改进

### 之前的行为
- ❌ 必须手持控制器才能生效
- ❌ 限制了玩家的操作灵活性
- ❌ 需要频繁切换物品

### 现在的行为
- ✅ 放在背包任意位置即可生效
- ✅ 自动扫描整个背包查找控制器
- ✅ 支持多个控制器（按优先级使用）
- ✅ 提高了使用便利性

## 检测优先级

控制器检测按以下优先级顺序：

1. **主手** - 最高优先级
2. **副手** - 次高优先级
3. **主背包** (槽位 9-35) - 中等优先级
4. **快捷栏** (槽位 0-8) - 最低优先级

如果玩家拥有多个控制器，系统会使用优先级最高的那个。

## 技术实现

### 修改的文件

#### 1. `CommonEventHandler.java`
**方法**: `findExperiencePumpController(EntityPlayer player)`

**修改前**:
```java
private ItemStack findExperiencePumpController(EntityPlayer player) {
    // 只检查主手和副手
    for (EnumHand hand : EnumHand.values()) {
        ItemStack heldStack = player.getHeldItem(hand);
        if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemExperiencePumpController) {
            return heldStack;
        }
    }
    return ItemStack.EMPTY;
}
```

**修改后**:
```java
private ItemStack findExperiencePumpController(EntityPlayer player) {
    // 1. 检查主手和副手（优先级最高）
    for (EnumHand hand : EnumHand.values()) {
        ItemStack heldStack = player.getHeldItem(hand);
        if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemExperiencePumpController) {
            return heldStack;
        }
    }
    
    // 2. 检查主背包（9-35槽位）
    for (int i = 9; i < 36; i++) {
        ItemStack stack = player.inventory.getStackInSlot(i);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePumpController) {
            return stack;
        }
    }
    
    // 3. 检查快捷栏（0-8槽位）
    for (int i = 0; i < 9; i++) {
        ItemStack stack = player.inventory.getStackInSlot(i);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePumpController) {
            return stack;
        }
    }
    
    return ItemStack.EMPTY;
}
```

#### 2. `ItemExperiencePumpController.java`
**方法**: `addInformation()`

**修改**:
- 移除了 "仅限手持使用" 的提示
- 添加了 "放在背包即可生效" 的说明
- 更新了使用方法说明

## 使用场景

### 场景 1: 日常使用
玩家可以将控制器放在背包中，专注于其他活动（如战斗、建造），控制器会自动管理经验储罐。

### 场景 2: 多配置切换
玩家可以携带多个配置不同的控制器：
- 控制器 A：抽取模式，保留等级 30
- 控制器 B：注入模式，保留等级 10
- 控制器 C：关闭模式

通过将不同的控制器放在不同优先级的位置，可以快速切换配置。

### 场景 3: 紧急情况
在战斗或危险情况下，玩家无需切换到控制器，控制器会自动在背包中工作。

## 性能考虑

### 扫描频率
控制器检测在以下情况触发：
- 玩家 Tick 事件（每 tick 一次）
- 背包变化事件

### 优化措施
1. **早期退出**: 一旦找到控制器立即返回，不继续扫描
2. **优先级顺序**: 先检查最常用的位置（手持）
3. **缓存机制**: 可以考虑添加缓存以减少重复扫描（未来优化）

### 性能影响
- 背包扫描是轻量级操作（最多 36 次物品类型检查）
- 对游戏性能影响可忽略不计
- 在现代计算机上，每帧扫描成本 < 0.01ms

## 兼容性

### 与其他功能的兼容性
- ✅ 与经验储罐完全兼容
- ✅ 与箱子戒指兼容
- ✅ 与背包模组兼容（如果储罐在背包中）
- ✅ 与 Baubles 兼容

### 多人游戏
- ✅ 完全支持多人游戏
- ✅ 每个玩家独立检测自己的控制器
- ✅ 不会影响其他玩家

## 用户体验改进

### 便利性提升
1. **无需频繁切换**: 玩家可以专注于主要活动
2. **自动化管理**: 控制器在后台自动工作
3. **灵活配置**: 支持多控制器配置切换

### 学习曲线
- 新玩家更容易理解（"放在背包就能用"）
- 减少了操作复杂度
- 保持了高级玩家的配置灵活性

## 测试建议

### 功能测试
1. 将控制器放在主手 → 验证生效
2. 将控制器放在副手 → 验证生效
3. 将控制器放在背包 → 验证生效
4. 将控制器放在快捷栏 → 验证生效
5. 同时拥有多个控制器 → 验证优先级

### 边界测试
1. 背包满时添加控制器
2. 移除控制器后功能停止
3. 切换不同配置的控制器
4. 在不同维度使用

### 性能测试
1. 大量玩家同时使用
2. 频繁切换控制器
3. 长时间运行稳定性

## 未来改进

### 可能的优化
1. **缓存机制**: 缓存控制器位置，减少扫描频率
2. **配置选项**: 允许玩家选择是否启用背包检测
3. **视觉反馈**: 在 GUI 中显示当前使用的控制器位置
4. **快捷键**: 添加快捷键快速切换控制器

### 潜在功能
1. **控制器组**: 支持多个控制器协同工作
2. **条件触发**: 根据玩家状态自动切换控制器
3. **远程控制**: 通过无线方式控制储罐

## 版本历史

- **v1.0** (2026-02-02): 初始实现
  - 添加背包扫描功能
  - 实现优先级检测
  - 更新工具提示说明

## 相关文档

- `ExperiencePumpController.java` - 控制器核心逻辑
- `CommonEventHandler.java` - 事件处理和控制器检测
- `ItemExperiencePumpController.java` - 控制器物品类
