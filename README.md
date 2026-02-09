# RS Rings and Tanks

<div align="center">

**版本**: 1.4.0  
**Minecraft**: 1.12.2  
**Forge**: 14.23.5.2847+  
**作者**: bsyj

[English](#english) | [中文文档](#中文文档)

</div>

---

<div id="中文文档">

## 📖 项目简介

RS Rings and Tanks 是一个功能强大的 Minecraft 1.12.2 模组，提供智能物品管理和经验存储系统。模组包含物品吸收戒指、经验储罐系统、经验泵控制器等核心功能，旨在帮助玩家更高效地管理游戏资源。

### ✨ 核心特性

- 🔄 **智能物品吸收** - 自动收集周围掉落物，支持跨维度传输
- 💾 **经验存储管理** - 存储和管理玩家经验，支持容量升级
- 🎛️ **统一控制器** - 批量管理所有经验储罐，简化操作流程
- 🌐 **RS网络集成** - 直接与Refined Storage网络对接
- 🎮 **饰品栏支持** - 完整兼容Baubles饰品系统
- ⚙️ **高度可配置** - 丰富的配置选项，自定义功能行为
- 🔒 **数据安全** - 升级不丢失经验，跨维度物品传送

---

## 🎮 功能模块

### 物品吸收戒指

**主要功能：**
- 自动吸收8格内的掉落物品
- 黑白名单过滤系统（9个过滤槽）
- 支持绑定到RS控制器和普通箱子
- 跨维度物品传送
- FE能量驱动（最大10M FE）
- K键快速开关
- 支持背包、手持、饰品栏使用

**技术规格：**
- 吸收范围：8格（可配置）
- 吸收间隔：5 tick（可配置）
- 能量消耗：1 FE/物品（可配置）
- 最大能量：10,000,000 FE
- 过滤槽数量：9个

### 经验储罐系统

**储罐类型：**

| 储罐名称 | 容量 | 可升级 |
|---------|------|--------|
| 经验泵（基础） | 1000 XP × 2^(等级-1) | ✅ |
| 100级储罐 | 30,970 XP | ❌ |
| 500级储罐 | 1,045,970 XP | ❌ |
| 1000级储罐 | 4,339,720 XP | ❌ |
| 2000级储罐 | 17,677,220 XP | ❌ |

**核心功能：**
- 三种工作模式：关闭 / 从玩家抽 / 向玩家注
- 自动吸收经验球和经验瓶
- 经验修补自动修复装备
- 保留等级设置
- 溢出保护（满时转化为经验瓶）
- 动态纹理显示（5种填充等级）
- 升级时经验完整保留

**经验计算公式（遵循Minecraft官方）：**
- 0-15级：`level × (12 + level × 2) / 2`
- 16-30级：`(level-15) × (69 + (level-15) × 5) / 2 + 315`
- 31+级：`(level-30) × (215 + (level-30) × 9) / 2 + 1395`

### 经验泵控制器

**管理功能：**
- 自动扫描玩家背包、手持、Baubles饰品栏中的所有储罐
- 统一管理最多32个储罐（可配置）
- 精确的等级操作（存入/取出N级）
- 配置存储在控制器物品NBT上
- 实时显示储罐状态和总量

**智能分配：**
- 抽取时：优先使用经验多的储罐
- 注入时：优先填充经验少的储罐（按百分比排序）

---

## 📦 安装指南

### 系统要求

**必需依赖：**
- Minecraft 1.12.2
- Forge 14.23.5.2847 或更高版本

**推荐依赖：**
- Baubles 1.5.2+（用于饰品栏支持）
- JEI 4.16.1.1013+（用于配方显示）
- Refined Storage（可选，用于RS网络集成）

### 安装步骤

1. 确保已安装 Minecraft 1.12.2 和 Forge 14.23.5.2847+
2. 下载 RS Rings and Tanks 模组 jar 文件
3. 将 jar 文件放入 `.minecraft/mods` 目录
4. （可选）下载并安装 Baubles 模组
5. 启动游戏，模组将自动加载

---

## 🎯 使用方法

### 物品吸收戒指

#### 1. 合成戒指
在游戏中通过JEI或查看配方获取合成方法。

#### 2. 绑定箱子/RS控制器
- 手持戒指
- 蹲下（Shift）+ 右键点击箱子或RS控制器
- 聊天框显示绑定成功消息和RS网络状态（如适用）

#### 3. 配置过滤器
- 右键戒指打开GUI
- 将物品放入9个过滤槽
- 点击按钮切换黑白名单模式
  - 黑名单：过滤掉列表中的物品
  - 白名单：只吸收列表中的物品

#### 4. 开启/关闭功能
- 按下 `K` 键切换戒指开关
- 戒指可在背包、手持或饰品栏中使用

#### 5. 充能
- 使用任何FE充能器为戒指充电
- 最大容量：10M FE
- 也可通过手摇充电：潜行+右键空气

### 经验储罐系统

#### 1. 使用储罐
- 将储罐放入背包、手持或饰品栏
- 储罐会自动根据配置工作

#### 2. 使用控制器
- 手持控制器，右键打开GUI
- 控制器会自动检测所有储罐并显示

#### 3. 存取经验
- **存入**：点击"存入1级"或"存入全部"按钮
- **取出**：点击"取出1级"或"取出全部"按钮
- 经验会在玩家和储罐之间转移

#### 4. 配置模式
- 点击"模式"按钮切换工作模式
- 设置保留等级（玩家最少保留的经验等级）
- 开启/关闭修补模式（自动修复装备）

#### 5. 升级储罐
- 使用储罐 + 钻石在工作台合成
- 容量翻倍（1000 → 2000 → 4000...）
- 升级时经验完整保留
- 特殊储罐（100/500/1000/2000级）不可升级

---

## ⌨️ 按键绑定

| 按键 | 功能 |
|------|------|
| K | 切换物品吸收戒指开关 |
| E | 退出GUI |
| 右键 | 打开物品GUI（手持物品时） |
| Shift + 右键 | 绑定箱子/RS控制器（手持戒指时） |

---

## ⚙️ 配置说明

配置文件位置：`.minecraft/config/rsring/`

### 戒指配置 (ring_config.cfg)

```properties
# 默认黑名单物品（使用物品注册名）
defaultBlacklistItems=[]

# 默认白名单物品
defaultWhitelistItems=[]

# 默认使用黑名单模式
useBlacklistModeByDefault=true

# 吸收范围（格）
absorptionRange=8

# 每个物品能量消耗（FE）
energyCostPerItem=1

# 最大能量容量（FE）
maxEnergyCapacity=10000000

# 允许自定义过滤
allowCustomFilters=true

# 吸收间隔（tick）
absorptionInterval=5

# 初始能量（FE）
initialEnergy=0

# 能量消耗倍率
energyCostMultiplier=1.0

# 手摇充电量
manualChargeAmount=1000
```

### 经验储罐配置 (experience_tank_config.cfg)

```properties
# 启用经验储罐系统
enabled=true

# 经验抽取速率（每次最多抽取的经验值）
xpExtractionRate=20

# 经验抽取范围（格）
xpExtractionRange=5.0

# 从经验瓶物品中抽取经验
extractXpBottles=true

# 启用修补
mendingOn=true

# 修补玩家物品
mendPlayerItems=true

# 启用溢出保护（满时转化为经验瓶）
enableOverflowBottles=true

# 启用自动泵送
enableAutoPumping=true

# 泵送间隔（tick）
pumpingInterval=5

# 修补间隔（tick）
mendingInterval=20

# 抽取间隔（tick）
extractionInterval=4

# 默认泵送模式（0=关闭 1=从玩家抽 2=向玩家注）
defaultPumpMode=0

# 默认保留等级
defaultRetainLevel=1

# 默认修补模式
defaultMendingMode=true

# 最大储罐等级限制
maxTankLevelLimit=20

# 启用特殊储罐（100/500/1000/2000级）
enableSpecialTanks=true

# 经验瓶转换效率
xpToBottleEfficiency=1.0

# 修补效率
xpMendingEfficiency=1.0
```

### 经验泵控制器配置 (pump_controller_config.cfg)

```properties
# 最大管理储罐数
maxManagedTanks=32
```

### 配置界面访问

1. 启动游戏并进入主菜单
2. 点击"Mods"按钮
3. 找到"RS Rings and Tanks"模组
4. 点击右侧的"Config"按钮
5. 在配置界面中修改设置并保存

---

## 🔬 技术架构

### 设计模式

- **单例模式** - ExperiencePumpController, RingDetectionSystem
- **建造者模式** - RingDetectionResult.Builder
- **策略模式** - 经验泵的三种工作模式
- **观察者模式** - Forge事件总线
- **能力模式** - Forge Capability系统

### 核心技术栈

| 技术点 | 实现方式 |
|--------|----------|
| 数据持久化 | NBT + Forge Capability |
| 网络通信 | SimpleNetworkWrapper |
| GUI渲染 | Minecraft GUI系统 |
| 配置管理 | Forge Configuration API |
| 集成方式 | 反射 + @Optional接口 |
| 物品过滤 | 黑名单/白名单 + 9槽GUI |
| 经验计算 | Minecraft官方公式 |

### 项目结构

```
src/main/java/com/rsring/
├── capability/           # 能力系统实现
│   ├── IRsRingCapability
│   └── IExperiencePumpCapability
├── client/              # GUI和渲染
│   ├── gui/
│   └── render/
├── config/              # 配置管理
│   ├── RsRingConfig
│   └── ExperienceTankConfig
├── crafting/            # 合成配方
├── event/               # 事件处理
│   ├── CommonEventHandler
│   ├── CraftingUpgradeHandler
│   └── InventoryChangeHandler
├── experience/          # 经验管理核心
│   ├── ExperiencePumpController
│   ├── ExperienceTankManager
│   └── TankScanResult
├── item/                # 物品实现
│   ├── ItemAbsorbRing
│   ├── ItemExperiencePump
│   └── ItemExperienceTank*
├── network/             # 网络数据包
├── proxy/               # 客户端/服务端代理
├── rsring/             # 主模组类
├── service/             # 环检测服务
│   ├── RingDetectionService
│   └── RingDetectionSystem
├── baubles/            # Baubles集成
└── util/               # 工具类
    ├── XpHelper
    └── BaublesHelper
```

---

## 🔧 开发指南

### 构建项目

```bash
# 克隆仓库
git clone https://github.com/bsyj/rsring.git

# 搭建开发环境
gradlew setupDecompWorkspace

# 生成项目（Eclipse）
gradlew eclipse

# 或生成IntelliJ项目
gradlew genIntellijRuns

# 构建项目
gradlew build

# 构建产物位置
build/libs/rsring-1.4.0.jar
```

### 代码规范

项目遵循以下开发规范：
- SOLID设计原则
- 线程安全（使用ConcurrentHashMap、CopyOnWriteArrayList等）
- 完善的异常处理
- 性能优化（避免高复杂度算法、使用缓存）
- 客户端-服务器兼容性保证

---

## 🐛 常见问题

### Q: K键没有反应？
**A**: 检查是否有其他模组占用了K键，可以在设置中重新绑定。

### Q: 控制器显示"无储罐"？
**A**: 确保储罐在背包、手持或饰品栏中，控制器会自动检测。

### Q: 经验储罐升级后经验丢失？
**A**: 这个问题已在1.1版本修复，升级时经验会被完整保留。

### Q: 戒指在饰品栏无法使用K键？
**A**: 确保安装了Baubles模组1.5.2或更高版本。

### Q: 如何配置默认黑白名单？
**A**: 编辑配置文件 `.minecraft/config/rsring/ring_config.cfg`，添加物品注册名。

### Q: 经验储罐已满时会发生什么？
**A**: 当经验储罐已满时，如果启用了溢出保护功能，多余的经验会转化为经验瓶掉落。

### Q: 物品吸收戒指的能量消耗如何计算？
**A**: 每次传输物品消耗 `energyCostPerItem × energyCostMultiplier` FE，可配置。

### Q: RS网络满时物品会怎样？
**A**: RS网络满时物品会保留在原地，不会被吸收，避免物品丢失。

---

## 📜 许可证

本项目使用 MIT 许可证。

---

## 🙏 致谢

- **Azanor** - Baubles 模组作者，提供了饰品栏API
- **Lothrazar** - Cyclic 模组作者，提供了GUI纹理和布局参考
- **SophisticatedBackpacks** - 提供了XP计算逻辑参考
- **Minecraft Forge 团队** - 提供了模组开发框架

---

## 📚 相关文档

- [更新日志](CHANGELOG-RSRING.md) - 详细的版本更新记录
- [快速测试指南](QUICK-TEST-GUIDE.md) - 测试指南
- [项目状态](PROJECT-STATUS.md) - 项目开发状态

---

## 📮 问题反馈

如果发现问题或建议，请通过以下方式反馈：

1. **GitHub Issues** - 提交问题报告
2. 提供以下信息：
   - 详细的问题描述
   - 复现步骤
   - 日志文件 (`logs/latest.log`)
   - 截图/录屏（如果可能）
   - 模组列表

---

## 🔗 链接

- **GitHub仓库**: https://github.com/bsyj/rsring
- **问题追踪**: https://github.com/bsyj/rsring/issues

---

<div align="center">

**享受游戏！** 🎮

</div>

---

<div id="english">

---

# RS Rings and Tanks (English)

<div align="center">

**Version**: 1.4.0  
**Minecraft**: 1.12.2  
**Forge**: 14.23.5.2847+  
**Author**: bsyj

</div>

## 📖 Project Overview

RS Rings and Tanks is a powerful Minecraft 1.12.2 mod that provides intelligent item management and experience storage systems. The mod includes an Item Absorb Ring, Experience Tank System, Experience Pump Controller, and more, designed to help players manage game resources more efficiently.

### ✨ Core Features

- 🔄 **Smart Item Absorption** - Automatically collect dropped items, supports cross-dimensional transport
- 💾 **Experience Storage Management** - Store and manage player experience, supports capacity upgrades
- 🎛️ **Unified Controller** - Batch manage all experience tanks, simplify operations
- 🌐 **RS Network Integration** - Direct integration with Refined Storage network
- 🎮 **Baubles Support** - Full compatibility with Baubles accessory system
- ⚙️ **Highly Configurable** - Rich configuration options, customize behavior
- 🔒 **Data Safety** - Experience preserved during upgrades, cross-dimensional item transport

---

## 🎮 Feature Modules

### Item Absorb Ring

**Main Features:**
- Automatically absorb dropped items within 8 blocks
- Black/white list filtering system (9 filter slots)
- Supports binding to RS controllers and regular chests
- Cross-dimensional item transport
- FE energy powered (max 10M FE)
- Quick toggle with K key
- Works in inventory, hand, or Baubles slots

**Technical Specs:**
- Absorption range: 8 blocks (configurable)
- Absorption interval: 5 ticks (configurable)
- Energy cost: 1 FE/item (configurable)
- Max energy: 10,000,000 FE
- Filter slots: 9

### Experience Tank System

**Tank Types:**

| Tank Name | Capacity | Upgradeable |
|-----------|----------|-------------|
| Experience Pump (Base) | 1000 XP × 2^(level-1) | ✅ |
| 100 Level Tank | 30,970 XP | ❌ |
| 500 Level Tank | 1,045,970 XP | ❌ |
| 1000 Level Tank | 4,339,720 XP | ❌ |
| 2000 Level Tank | 17,677,220 XP | ❌ |

**Core Features:**
- Three working modes: Off / Extract from Player / Inject to Player
- Automatically absorb XP orbs and XP bottles
- Mending mode to auto-repair equipment
- Retain level setting
- Overflow protection (convert to bottles when full)
- Dynamic texture display (5 fill levels)
- Experience preserved during upgrades

**Experience Formula (Minecraft Official):**
- 0-15 levels: `level × (12 + level × 2) / 2`
- 16-30 levels: `(level-15) × (69 + (level-15) × 5) / 2 + 315`
- 31+ levels: `(level-30) × (215 + (level-30) × 9) / 2 + 1395`

### Experience Pump Controller

**Management Features:**
- Automatically scan all tanks in player inventory, hand, and Baubles slots
- Manage up to 32 tanks (configurable)
- Precise level operations (store/withdraw N levels)
- Configurations stored in controller item NBT
- Real-time display of tank status and totals

**Smart Distribution:**
- Extraction: Prioritize tanks with more XP
- Injection: Prioritize tanks with less XP (sorted by percentage)

---

## 📦 Installation Guide

### System Requirements

**Required Dependencies:**
- Minecraft 1.12.2
- Forge 14.23.5.2847 or higher

**Recommended Dependencies:**
- Baubles 1.5.2+ (for accessory slot support)
- JEI 4.16.1.1013+ (for recipe display)
- Refined Storage (optional, for RS network integration)

### Installation Steps

1. Ensure Minecraft 1.12.2 and Forge 14.23.5.2847+ are installed
2. Download the RS Rings and Tanks mod jar file
3. Place the jar file in `.minecraft/mods` directory
4. (Optional) Download and install Baubles mod
5. Launch the game, the mod will load automatically

---

## 🎯 Usage Guide

### Item Absorb Ring

#### 1. Craft the Ring
Check JEI or recipes in-game for crafting method.

#### 2. Bind Chest/RS Controller
- Hold the ring
- Sneak (Shift) + Right-click on chest or RS controller
- Chat shows bind success message and RS network status (if applicable)

#### 3. Configure Filters
- Right-click the ring to open GUI
- Place items in 9 filter slots
- Click button to switch between black/white list mode
  - Blacklist: Filter out items in the list
  - Whitelist: Only absorb items in the list

#### 4. Toggle Function
- Press `K` key to toggle ring on/off
- Ring can work in inventory, hand, or Baubles slots

#### 5. Charging
- Use any FE charger to charge the ring
- Max capacity: 10M FE
- Manual charging: Sneak + Right-click air

### Experience Tank System

#### 1. Using Tanks
- Place tank in inventory, hand, or Baubles slots
- Tank automatically works based on configuration

#### 2. Using Controller
- Hold controller, right-click to open GUI
- Controller automatically detects all tanks

#### 3. Store/Withdraw Experience
- **Store**: Click "Store 1 Level" or "Store All" button
- **Withdraw**: Click "Withdraw 1 Level" or "Withdraw All" button
- Experience transfers between player and tank

#### 4. Configure Mode
- Click "Mode" button to switch working mode
- Set retain level (minimum XP level for player)
- Toggle mending mode (auto-repair equipment)

#### 5. Upgrade Tank
- Use tank + diamond in crafting table
- Capacity doubles (1000 → 2000 → 4000...)
- Experience preserved during upgrade
- Special tanks (100/500/1000/2000 level) cannot be upgraded

---

## ⌨️ Key Bindings

| Key | Function |
|-----|----------|
| K | Toggle Item Absorb Ring |
| E | Exit GUI |
| Right-click | Open item GUI (when holding item) |
| Shift + Right-click | Bind chest/RS controller (when holding ring) |

---

## ⚙️ Configuration

Config location: `.minecraft/config/rsring/`

### Ring Config (ring_config.cfg)

```properties
# Default blacklist items (use item registry names)
defaultBlacklistItems=[]

# Default whitelist items
defaultWhitelistItems=[]

# Default use blacklist mode
useBlacklistModeByDefault=true

# Absorption range (blocks)
absorptionRange=8

# Energy cost per item (FE)
energyCostPerItem=1

# Max energy capacity (FE)
maxEnergyCapacity=10000000

# Allow custom filters
allowCustomFilters=true

# Absorption interval (ticks)
absorptionInterval=5

# Initial energy (FE)
initialEnergy=0

# Energy cost multiplier
energyCostMultiplier=1.0

# Manual charge amount
manualChargeAmount=1000
```

### Experience Tank Config (experience_tank_config.cfg)

```properties
# Enable experience tank system
enabled=true

# XP extraction rate (max XP extracted per time)
xpExtractionRate=20

# XP extraction range (blocks)
xpExtractionRange=5.0

# Extract XP from bottle items
extractXpBottles=true

# Enable mending
mendingOn=true

# Mend player items
mendPlayerItems=true

# Enable overflow protection (convert to bottles when full)
enableOverflowBottles=true

# Enable auto pumping
enableAutoPumping=true

# Pumping interval (ticks)
pumpingInterval=5

# Mending interval (ticks)
mendingInterval=20

# Extraction interval (ticks)
extractionInterval=4

# Default pump mode (0=off 1=extract 2=inject)
defaultPumpMode=0

# Default retain level
defaultRetainLevel=1

# Default mending mode
defaultMendingMode=true

# Max tank level limit
maxTankLevelLimit=20

# Enable special tanks (100/500/1000/2000 level)
enableSpecialTanks=true

# XP to bottle efficiency
xpToBottleEfficiency=1.0

# Mending efficiency
xpMendingEfficiency=1.0
```

### Experience Pump Controller Config (pump_controller_config.cfg)

```properties
# Max managed tanks
maxManagedTanks=32
```

---

## 🔬 Technical Architecture

### Design Patterns

- **Singleton Pattern** - ExperiencePumpController, RingDetectionSystem
- **Builder Pattern** - RingDetectionResult.Builder
- **Strategy Pattern** - Three working modes of experience pump
- **Observer Pattern** - Forge event bus
- **Capability Pattern** - Forge Capability system

### Core Technology Stack

| Technology | Implementation |
|------------|----------------|
| Data Persistence | NBT + Forge Capability |
| Network Communication | SimpleNetworkWrapper |
| GUI Rendering | Minecraft GUI System |
| Configuration Management | Forge Configuration API |
| Integration Method | Reflection + @Optional Interface |
| Item Filtering | Black/white list + 9-slot GUI |
| Experience Calculation | Minecraft Official Formula |

---

## 🔧 Development Guide

### Build Project

```bash
# Clone repository
git clone https://github.com/bsyj/rsring.git

# Setup development environment
gradlew setupDecompWorkspace

# Generate Eclipse project
gradlew eclipse

# Or generate IntelliJ project
gradlew genIntellijRuns

# Build project
gradlew build

# Build output location
build/libs/rsring-1.4.0.jar
```

### Code Standards

Project follows these development standards:
- SOLID design principles
- Thread safety (ConcurrentHashMap, CopyOnWriteArrayList, etc.)
- Comprehensive exception handling
- Performance optimization (avoid high complexity algorithms, use caching)
- Client-server compatibility guarantee

---

## 🐛 FAQ

### Q: K key not working?
**A**: Check if another mod uses the K key, you can rebind it in settings.

### Q: Controller shows "No Tanks"?
**A**: Ensure tanks are in inventory, hand, or Baubles slots. The controller will auto-detect.

### Q: Experience lost after tank upgrade?
**A**: This issue was fixed in version 1.1. Experience is preserved during upgrades.

### Q: Ring K key not working in Baubles slot?
**A**: Ensure Baubles mod 1.5.2 or higher is installed.

### Q: How to configure default black/white list?
**A**: Edit config file `.minecraft/config/rsring/ring_config.cfg`, add item registry names.

### Q: What happens when XP tank is full?
**A**: If overflow protection is enabled, excess XP will convert to XP bottles and drop.

### Q: How is ring energy consumption calculated?
**A**: Each item transfer consumes `energyCostPerItem × energyCostMultiplier` FE, configurable.

### Q: What happens when RS network is full?
**A**: Items remain in place when RS network is full, won't be absorbed, preventing item loss.

---

## 📜 License

This project is licensed under the MIT License.

---

## 🙏 Credits

- **Azanor** - Baubles mod author, provided accessory slot API
- **Lothrazar** - Cyclic mod author, provided GUI textures and layout reference
- **SophisticatedBackpacks** - Provided XP calculation logic reference
- **Minecraft Forge Team** - Provided mod development framework

---

## 📚 Related Documentation

- [Changelog](CHANGELOG-RSRING.md) - Detailed version history
- [Quick Test Guide](QUICK-TEST-GUIDE.md) - Testing guide
- [Project Status](PROJECT-STATUS.md) - Development status

---

## 📮 Feedback

If you find issues or have suggestions, please provide feedback via:

1. **GitHub Issues** - Submit bug reports
2. Provide the following information:
   - Detailed problem description
   - Reproduction steps
   - Log file (`logs/latest.log`)
   - Screenshots/videos (if possible)
   - Mod list

---

## 🔗 Links

- **GitHub Repository**: https://github.com/bsyj/rsring
- **Issue Tracker**: https://github.com/bsyj/rsring/issues

---

<div align="center">

**Enjoy the game!** 🎮

</div>

</div>
