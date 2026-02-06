# RsRing Mod - 更新日志

## Version 1.4 - 2026-02-06

### 🎉 新增功能

#### RS网络集成（1.4核心功能）
- ✨ **RS控制器绑定** - 物品吸收戒指现在可以绑定到Refined Storage控制器
- ✨ **直接物品传输** - 吸收的物品可以直接存入RS网络存储
- ✨ **网络状态显示** - 绑定后显示RS网络在线状态和存储空间
- ✨ **RS网络存储** - 物品直接存入RS网络，网络满时物品保留在原地
- ✨ **仅支持控制器** - 当前版本仅支持绑定到RS控制器（普通/创造）

#### RS集成技术实现
- ✨ 创建 `RSIntegration` 集成类，封装所有RS API调用
- ✨ 支持RS 1.6.x版本（Minecraft 1.12.2）
- ✨ 反射加载RS API，无RS时优雅降级
- ✨ 网络状态验证（在线/离线/存储空间）
- ✨ 详细的错误处理和日志记录

### 🐛 修复问题

- 修复了一些小问题
- 优化了模组性能和稳定性
- 更新了文档和说明

### 💄 改进优化

- 更新版本号到1.4
- 优化代码结构
- 改进用户反馈信息（显示绑定目标类型）
- 添加RS网络状态提示

---

## Version 1.3.3 - 2026-02-04

### 🐛 修复问题

- 修复了一些小问题
- 优化了模组性能和稳定性
- 更新了文档和说明

### 💄 改进优化

- 更新版本号到1.3.3
- 优化代码结构

---

## Version 1.0 - 2026-02-02

### 🎉 新增功能

#### Baubles饰品栏完整支持
- ✨ 戒指在饰品栏时可以使用K键切换开关
- ✨ 控制器可以完全操作饰品栏中的储罐（存取经验、切换模式、修改保留等级）
- ✨ 新增ItemLocationTracker工具类统一处理物品位置追踪和同步
- ✨ K键切换时显示物品位置信息（主手/副手/背包/饰品栏）

#### 经验系统改进
- ✨ 迁移SophisticatedBackpacks的精确XP计算逻辑
- ✨ 新增XpHelper工具类（使用Minecraft官方经验公式）
- ✨ 经验泵控制器支持背包检测（不需要手持即可生效）
- ✨ 简化经验储罐tooltip显示（只显示等级和经验）
- ✨ 统一经验计算逻辑，避免不一致

#### 戒指GUI迁移到Cyclic风格
- ✨ 从GuiScreen迁移到GuiContainer架构
- ✨ 使用Cyclic原版纹理和布局（原汁原味）
- ✨ 创建ContainerRingFilter容器类
- ✨ 创建GuiRingFilterContainer GUI类
- ✨ 支持E键退出GUI
- ✨ 精确的槽位点击检测（16像素区域）

#### 戒指配置系统
- ✨ 新增配置文件支持 (`.minecraft/config/rsring/ring_config.cfg`)
- ✨ 可配置默认黑白名单物品列表
- ✨ 可配置吸收范围（1-32格，默认8格）
- ✨ 可配置能量消耗（每个物品消耗的FE）
- ✨ 可配置最大能量容量
- ✨ 支持配置热重载（无需重启游戏）

### 🐛 修复问题

#### 经验系统修复
- 🐛 修复经验储罐升级时丢失经验的问题
  - 原因：setCapacityLevels()调用setXpStored()时使用旧容量限制
  - 解决：先保存当前XP，更新容量，再应用新容量限制
- 🐛 修复经验计算不精确的问题（使用Minecraft官方公式）

#### Baubles集成修复
- 🐛 修复戒指在饰品栏时K键无法切换的问题
  - 原因：修改的是Baubles返回的ItemStack副本
  - 解决：使用ItemLocationTracker追踪位置并写回
- 🐛 修复控制器无法操作饰品栏储罐的问题
  - 原因：缺少inventory.markDirty()调用
  - 解决：添加markDirty()确保同步

#### GUI修复
- 🐛 修复戒指GUI tooltip显示乱码的问题
  - 原因：tooltip在错误的坐标系统中渲染
  - 解决：移动到drawScreen()方法中渲染
- 🐛 修复戒指GUI槽位点击区域不准确的问题
  - 原因：使用了错误的点击检测方法
  - 解决：使用isPointInRegion()精确检测16像素区域
- 🐛 修复戒指过滤模式切换时聊天消息乱码
  - 原因：源文件编码问题
  - 解决：修复所有中文字符编码

#### 其他修复
- 🐛 修复聊天消息重复显示的问题（移除重复的绑定消息）
- 🐛 修复控制器检测不到背包中的储罐

### 💄 改进优化

#### UI/UX改进
- 💄 优化tooltip显示
  - 经验泵控制器：详细信息移到Shift提示
  - 箱子戒指：重新组织信息顺序（能量→状态→绑定→过滤模式）
- 💄 控制器GUI添加"无储罐"警告提示（红色文字）
- 💄 K键切换时显示物品位置信息
- 💄 经验储罐根据填充百分比显示不同纹理（0%, 25%, 50%, 75%, 100%）

#### 性能优化
- 🔧 优化Baubles反射访问，保持可选依赖
- 🔧 改进capability同步机制
- 🔧 统一物品位置管理，减少重复代码

#### 代码质量
- 📝 完善JavaDoc文档和代码注释
- 📝 创建完整的技术文档和测试指南
- 🧪 添加XpHelper单元测试
- 🔧 重构代码结构，提高可维护性

### 📚 文档

#### 新增文档
- 📖 PROJECT-STATUS.md - 项目完整状态总结
- 📖 QUICK-TEST-GUIDE.md - 快速测试指南
- 📖 Baubles-Integration-Fix-Complete.md - Baubles集成修复详细文档
- 📖 Baubles-Fix-Summary.md - Baubles修复简要总结
- 📖 Ring-Filter-GUI-Migration-Complete.md - 戒指GUI迁移完成报告
- 📖 Experience-Tank-Upgrade-Fix.md - 经验储罐升级修复文档

#### 规范文档
- 📋 experience-system-improvements/ - 经验系统改进规范
- 📋 ring-gui-cyclic-migration/ - 戒指GUI迁移规范
- 📋 baubles-integration-fixes/ - Baubles集成修复规范

### 🔧 技术改进

#### 新增工具类
- `ItemLocationTracker` - 统一物品位置追踪和同步
  - 支持主手、副手、背包、Baubles
  - 解决Baubles ItemStack副本问题
  - 提供findItem()和syncBack()方法

- `XpHelper` - 统一经验计算
  - 使用Minecraft官方公式
  - 支持经验↔等级转换
  - 支持经验↔液体转换（1 XP = 20 mB）

#### 架构改进
- 🏗️ 戒指GUI从GuiScreen迁移到GuiContainer
- 🏗️ 创建ContainerRingFilter容器类绑定槽位
- 🏗️ 改进capability同步机制
- 🏗️ 统一经验计算逻辑

### ⚙️ 配置

#### 新增配置项
```
# 戒指配置 (.minecraft/config/rsring/ring_config.cfg)
defaultBlacklistItems=[]        # 默认黑名单物品
defaultWhitelistItems=[]        # 默认白名单物品
useBlacklistModeByDefault=true  # 默认使用黑名单模式
absorptionRange=8               # 吸收范围（1-32格）
energyCostPerItem=1             # 每个物品能量消耗（FE）
maxEnergyCapacity=10000000      # 最大能量容量（FE）
allowCustomFilters=true         # 允许自定义过滤
```

### 🎯 兼容性

#### 必需
- Minecraft 1.12.2
- Forge 14.23.5.2838+ (推荐最新版)

#### 可选
- Baubles (用于饰品栏功能)

#### 已测试
- ✅ 单人游戏
- ✅ 多人服务器
- ✅ 与Baubles集成
- ✅ 配置热重载

### 📊 统计

- **新增类**: 5个
- **修改类**: 15+
- **测试类**: 1个
- **文档**: 10+篇
- **代码行数**: 3000+行

### 🙏 致谢

- SophisticatedBackpacks - XP计算逻辑参考
- Cyclic - GUI纹理和布局参考
- Baubles - 饰品栏API

---

## 已知问题

目前无已知问题。

如果发现问题，请提供：
1. 问题描述
2. 复现步骤
3. 日志文件 (`logs/latest.log`)
4. 截图或录屏

---

## 下一步计划

- [ ] 添加更多配置选项
- [ ] 优化性能
- [ ] 添加更多语言支持
- [ ] 考虑1.16+版本移植

---

**感谢使用RsRing Mod！** 🎮
