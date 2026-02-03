# Baubles Integration Fixes - Requirements

## 1. Overview
修复饰品栏（Baubles）中的戒指和储罐功能问题，确保它们能够正常工作。

## 2. User Stories

### 2.1 戒指K键切换
**作为** 玩家  
**我想要** 在饰品栏装备戒指时能够使用K键切换开关  
**以便** 不需要将戒指拿到手上就能控制其功能

**验收标准**:
- 戒指在饰品栏时，按K键能够切换开关状态
- 切换后状态正确同步到服务器
- 切换后饰品栏中的戒指ItemStack正确更新
- 玩家收到切换成功的反馈消息

### 2.2 控制器操作饰品栏储罐
**作为** 玩家  
**我想要** 控制器能够对饰品栏中的储罐进行存取经验操作  
**以便** 充分利用饰品栏的储罐容量

**验收标准**:
- 控制器能够检测到饰品栏中的储罐（已实现）
- 控制器能够从饰品栏储罐中取出经验
- 控制器能够向饰品栏储罐中存入经验
- 控制器能够修改饰品栏储罐的模式、保留等级等设置
- 所有操作后储罐状态正确同步回饰品栏

## 3. Technical Context

### 3.1 Current Implementation
- `CommonEventHandler.findAnyRingForToggle()` 已经扫描饰品栏
- `PacketPumpAction.Handler` 有 `syncTankBack()` 方法用于同步
- `InventoryIntegrationLayer.getBaublesTanks()` 能够检测饰品栏储罐

### 3.2 Known Issues
1. **戒指切换问题**: K键切换后，饰品栏中的戒指ItemStack可能没有正确更新
2. **储罐操作问题**: 控制器修改饰品栏储罐后，修改可能没有正确写回饰品栏

### 3.3 Root Cause Analysis
Baubles返回的ItemStack是副本，直接修改不会影响原始物品。需要：
1. 修改capability后，调用 `inventory.setInventorySlotContents()` 写回
2. 确保使用正确的Baubles inventory引用

## 4. Constraints
- 必须保持与Baubles API的兼容性
- 必须使用反射访问Baubles（因为是可选依赖）
- 必须处理Baubles不存在的情况
- 不能破坏现有的手持/背包功能

## 5. Success Criteria
- 戒指在饰品栏时K键切换正常工作
- 控制器能够完全操作饰品栏中的储罐
- 所有操作后状态正确同步
- 编译成功，无错误
