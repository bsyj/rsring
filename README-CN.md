# RS Rings and Tanks

<div align="center">

![Mod Banner](src/main/resources/assets/rsring/textures/items/mod_banner.png)

**版本**: 1.4.0  
**Minecraft**: 1.12.2  
**Forge**: 14.23.5.2847+  
**作者**: bsyj

[![GitHub Release](https://img.shields.io/github/v/release/bsyj/rsring?style=flat-square&color=orange)](https://github.com/bsyj/rsring/releases)
[![GitHub Downloads](https://img.shields.io/github/downloads/bsyj/rsring/total?style=flat-square&color=brightgreen)](https://github.com/bsyj/rsring/releases)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.12.2-blue?style=flat-square)](https://www.minecraft.net/)
[![Forge Version](https://img.shields.io/badge/Forge-14.23.5.2847+-blue?style=flat-square)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/github/license/bsyj/rsring?style=flat-square&color=yellow)](LICENSE.txt)
[![Visitors](https://visitor-badge.laobi.icu/badge?page_id=bsyj.rsring)](https://github.com/bsyj/rsring)

</div>

---

## 📖 项目简介

RS Rings and Tanks 是一个功能强大的 Minecraft 1.12.2 模组，提供智能物品管理和经验存储系统。

### ✨ 核心特性

- 🔄 **智能物品吸收** - 自动收集周围掉落物，支持跨维度传输
- 💾 **经验存储管理** - 存储和管理玩家经验，支持容量升级
- 🎛️ **统一控制器** - 批量管理所有经验储罐，简化操作流程
- 🌐 **RS网络集成** - 直接与Refined Storage网络对接
- 🎮 **饰品栏支持** - 完整兼容Baubles饰品系统
- ⚙️ **高度可配置** - 丰富的配置选项，自定义功能行为

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
- 最大能量：10,000,000 FE（可配置）
- 过滤槽数量：9个（有默认黑白名单）

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

### 经验泵控制器

**管理功能：**
- 自动扫描背包、手持、Baubles饰品栏中的储罐
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
- Baubles 1.5.2+（可选，用于饰品栏支持）
- JEI 4.16.1.1013+（可选，用于配方显示）
- Refined Storage（可选，用于RS网络集成）

### 安装步骤

1. 确保已安装 Minecraft 1.12.2 和 Forge 14.23.5.2847+
2. 下载 RS Rings and Tanks 模组 jar 文件
3. 将 jar 文件放入 `.minecraft/mods` 目录
4. （可选）下载并安装 Baubles 模组
5. （可选）下载并安装 Refined Storage 模组
6. 启动游戏，模组将自动加载

---

## 🎯 使用方法

### 物品吸收戒指

#### 1. 合成戒指
在游戏中通过JEI查看合成配方。

#### 2. 绑定箱子/RS控制器
- 手持戒指
- 蹲下（Shift）+ 右键点击箱子或RS控制器
- 聊天框显示绑定成功消息

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
- **存入**：点击"存入1级"或"存入全部"
- **取出**：点击"取出1级"或"取出全部"
- 经验会在玩家和储罐之间转移

#### 4. 配置模式
- 点击"模式"按钮切换工作模式
- 设置保留等级（玩家最少保留的经验等级）
- 开启/关闭修补模式（自动修复装备）

#### 5. 升级储罐
- 使用储罐 + 钻石在工作台合成
- 容量翻倍，经验不丢失
- 特殊储罐不可升级

---

## ⌨️ 按键绑定

| 按键 | 功能 |
|------|------|
| K | 切换物品吸收戒指开关 |
| E | 退出GUI |
| 右键 | 打开物品GUI |
| Shift + 右键 | 绑定箱子/RS控制器 |

---

## ⚙️ 配置说明

配置文件位置：`.minecraft/config/rsring/`

### 戒指配置 (ring_config.cfg)

```properties
# 默认黑名单物品
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

# 吸收间隔（tick）
absorptionInterval=5

# 能量消耗倍率
energyCostMultiplier=1.0
```

### 经验储罐配置 (experience_tank_config.cfg)

```properties
# 启用经验储罐系统
enabled=true

# 经验抽取速率
xpExtractionRate=20

# 经验抽取范围（格）
xpExtractionRange=5.0

# 从经验瓶物品中抽取经验
extractXpBottles=true

# 启用修补
mendingOn=true

# 启用溢出保护
enableOverflowBottles=true

# 启用自动泵送
enableAutoPumping=true

# 泵送间隔（tick）
pumpingInterval=5

# 修补间隔（tick）
mendingInterval=20

# 默认保留等级
defaultRetainLevel=1

# 最大储罐等级限制
maxTankLevelLimit=20
```

### 配置界面访问

1. 启动游戏进入主菜单
2. 点击"Mods"按钮
3. 找到"RS Rings and Tanks"模组
4. 点击"Config"按钮
5. 在配置界面中修改设置

---

## 🔧 开发指南

### 构建项目

```bash
# 克隆仓库
git clone https://github.com/bsyj/rsring.git

# 搭建开发环境
gradlew setupDecompWorkspace

# 生成Eclipse项目
gradlew eclipse

# 生成IntelliJ项目
gradlew genIntellijRuns

# 构建项目
gradlew build

# 构建产物
build/libs/rsring-1.4.0.jar
```

---

## 🐛 常见问题

### Q: K键没有反应？
**A**: 检查是否有其他模组占用了K键，可以在设置中重新绑定。

### Q: 控制器显示"无储罐"？
**A**: 确保储罐在背包、手持或饰品栏中。

### Q: 经验储罐升级后经验丢失？
**A**: 这个问题已在1.1版本修复，升级时经验会被完整保留。

### Q: 戒指在饰品栏无法使用K键？
**A**: 确保安装了Baubles模组1.5.2或更高版本。

### Q: 如何配置默认黑白名单？
**A**: 编辑配置文件 `.minecraft/config/rsring/ring_config.cfg`。

### Q: 经验储罐已满时会发生什么？
**A**: 启用溢出保护后，多余经验会转化为经验瓶掉落。

### Q: RS网络满时物品会怎样？
**A**: 物品会保留在原地，不会被吸收。

---

## 📜 许可证

本项目使用 MIT 许可证。

---

## 🙏 致谢

- **Azanor** - Baubles 模组作者
- **Lothrazar** - Cyclic 模组作者
- **SophisticatedBackpacks** - XP计算逻辑参考
- **Minecraft Forge 团队** - 模组开发框架

---

## 📚 相关文档

- [更新日志](CHANGELOG-RSRING.md)
- [快速测试指南](QUICK-TEST-GUIDE.md)
- [项目状态](PROJECT-STATUS.md)

---

## 📮 问题反馈

发现问题请通过以下方式反馈：

1. **GitHub Issues** - 提交问题报告
2. 提供以下信息：
   - 问题描述
   - 复现步骤
   - 日志文件 (`logs/latest.log`)
   - 截图/录屏（如果可能）
   - 模组列表

---

## 🔗 链接

- **GitHub仓库**: https://github.com/bsyj/rsring
- **问题追踪**: https://github.com/bsyj/rsring/issues

---

## 📊 活动统计

[![Ashutosh's github activity graph](https://github-readme-activity-graph.vercel.app/graph?username=bsyj&theme=github-compact&area=true)](https://github.com/bsyj/rsring)

---

<div align="center">

**享受游戏！** 🎮

</div>
