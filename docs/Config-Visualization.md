# RS Ring Mod 配置可视化文档

## 配置文件位置

配置文件会在游戏运行后自动生成在：
- `config/rsring/experience_pump.cfg` - 经验泵配置
- `config/rsring/ring_config.cfg` - 戒指配置
- `config/rsring/pump_controller_config.cfg` - 泵控制器配置
- `config/rsring/general_config.cfg` - 通用配置
- `config/rsring/network_config.cfg` - 网络配置
- `ExperienceTankConfig.java` - 经验储罐配置（代码内配置）

---

## 1. 经验泵控制器配置 (ExperiencePumpConfig)

### 配置文件：`config/rsring/experience_pump.cfg`

```
┌─────────────────────────────────────────────────────────────────┐
│                    经验泵控制器配置                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ✓ enabled (是否启用)                                           │
│    默认值: true                                                 │
│    说明: 是否启用经验泵控制器功能                                │
│    需要重启: 是                                                 │
│                                                                 │
│  ⚡ xpExtractionRate (经验抽取速率)                             │
│    默认值: 20 XP/tick                                           │
│    范围: 1-1000                                                 │
│    说明: 每刻最大抽取的经验值（1秒=20刻）                        │
│    ├─ 20 XP/tick = 400 XP/秒 (默认)                            │
│    ├─ 100 XP/tick = 2000 XP/秒 (快速)                          │
│    └─ 1000 XP/tick = 20000 XP/秒 (极速)                        │
│                                                                 │
│  📦 maxXpStorage (最大存储容量)                                 │
│    默认值: 10,000 XP                                            │
│    范围: 100 - 10,000,000                                       │
│    说明: 经验泵最大可存储的经验值                                │
│    ├─ 10,000 XP ≈ 30级 (默认)                                  │
│    ├─ 100,000 XP ≈ 100级                                       │
│    └─ 1,000,000 XP ≈ 300级                                     │
│                                                                 │
│  🔧 mendingOn (自动修补)                                        │
│    默认值: true                                                 │
│    说明: 默认是否开启自动修补功能                                │
│                                                                 │
│  🍾 extractXpBottles (抽取经验瓶)                               │
│    默认值: true                                                 │
│    说明: 是否自动抽取周围的经验瓶物品并转换为经验                 │
│                                                                 │
│  🎒 mendPlayerItems (修补背包物品)                              │
│    默认值: true                                                 │
│    说明: 是否修补玩家背包中的装备（关闭则仅修补装备本身）         │
│                                                                 │
│  📏 xpExtractionRange (抽取范围)                                │
│    默认值: 5 格                                                 │
│    范围: 1-10                                                   │
│    说明: 经验泵抽取经验球的范围（格）                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 配置示例场景

#### 场景1：快速升级模式
```cfg
xpExtractionRate = 500      # 10000 XP/秒
maxXpStorage = 1000000      # 存储100万经验
mendingOn = false           # 关闭自动修补
xpExtractionRange = 10      # 最大范围
```

#### 场景2：自动修补模式
```cfg
xpExtractionRate = 20       # 默认速度
maxXpStorage = 50000        # 中等容量
mendingOn = true            # 开启自动修补
mendPlayerItems = true      # 修补背包物品
xpExtractionRange = 5       # 默认范围
```

#### 场景3：经验收集模式
```cfg
xpExtractionRate = 100      # 快速收集
maxXpStorage = 10000000     # 最大容量
extractXpBottles = true     # 收集经验瓶
xpExtractionRange = 8       # 较大范围
```

---

## 2. 物品吸收戒指配置 (RsRingConfig)

### 配置文件：`config/rsring/ring_config.cfg`

```
┌─────────────────────────────────────────────────────────────────┐
│                    物品吸收戒指配置                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  📋 defaultBlacklistItems (默认黑名单)                          │
│    默认值: [] (空数组)                                          │
│    格式: ["modid:item_name", "modid:item_name"]                │
│    说明: 默认不吸收的物品列表                                    │
│    示例:                                                        │
│      ["minecraft:dirt",                                        │
│       "minecraft:cobblestone",                                 │
│       "minecraft:gravel"]                                      │
│                                                                 │
│  ✅ defaultWhitelistItems (默认白名单)                          │
│    默认值: [] (空数组)                                          │
│    格式: ["modid:item_name", "modid:item_name"]                │
│    说明: 默认只吸收的物品列表                                    │
│    示例:                                                        │
│      ["minecraft:diamond",                                     │
│       "minecraft:gold_ingot",                                  │
│       "minecraft:emerald"]                                     │
│                                                                 │
│  🎯 useBlacklistModeByDefault (默认模式)                        │
│    默认值: true                                                 │
│    说明: true=黑名单模式, false=白名单模式                       │
│    ├─ 黑名单模式: 吸收除黑名单外的所有物品                       │
│    └─ 白名单模式: 只吸收白名单中的物品                          │
│                                                                 │
│  📏 absorptionRange (吸收范围)                                  │
│    默认值: 8 格                                                 │
│    范围: 1-32                                                   │
│    说明: 戒指吸收物品的范围（格）                                │
│    ├─ 8格 = 16x16x16 区域 (默认)                               │
│    ├─ 16格 = 32x32x32 区域                                     │
│    └─ 32格 = 64x64x64 区域 (最大)                              │
│                                                                 │
│  ⚡ energyCostPerItem (单个物品能量消耗)                        │
│    默认值: 1 FE/物品                                            │
│    范围: 0-1000                                                 │
│    说明: 吸收每个物品消耗的能量                                  │
│                                                                 │
│  🔋 maxEnergyCapacity (最大能量容量)                            │
│    默认值: 10,000,000 FE                                        │
│    范围: 1,000 - 100,000,000                                    │
│    说明: 戒指最大能量容量                                        │
│    ├─ 10,000,000 FE = 可吸收1000万个物品 (默认)                │
│    └─ 100,000,000 FE = 可吸收1亿个物品 (最大)                  │
│                                                                 │
│  🎛️ allowCustomFilters (允许自定义过滤)                         │
│    默认值: true                                                 │
│    说明: 是否允许玩家在GUI中自定义过滤列表                       │
│                                                                 │
│  ⏱️ absorptionInterval (吸收间隔)                               │
│    默认值: 5 tick                                               │
│    范围: 1-20                                                   │
│    说明: 多久检测一次物品（tick）                                │
│    ├─ 1 tick = 每tick检测 (最快，可能卡顿)                     │
│    ├─ 5 tick = 每0.25秒检测 (默认，平衡)                       │
│    └─ 20 tick = 每1秒检测 (最慢，性能最好)                     │
│                                                                 │
│  🔌 initialEnergy (初始能量)                                    │
│    默认值: 0 FE                                                 │
│    范围: 0 - 10,000,000                                         │
│    说明: 新戒指的初始能量                                        │
│                                                                 │
│  📊 energyCostMultiplier (能量消耗倍率)                         │
│    默认值: 1.0                                                  │
│    范围: 0.1 - 10.0                                             │
│    说明: 能量消耗的倍率调整                                      │
│    ├─ 0.5 = 消耗减半                                           │
│    ├─ 1.0 = 正常消耗 (默认)                                    │
│    └─ 2.0 = 消耗加倍                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 配置示例场景

#### 场景1：垃圾过滤模式（黑名单）
```cfg
useBlacklistModeByDefault = true
defaultBlacklistItems = [
    "minecraft:dirt",
    "minecraft:cobblestone",
    "minecraft:gravel",
    "minecraft:sand",
    "minecraft:netherrack"
]
absorptionRange = 8
energyCostPerItem = 1
```

#### 场景2：贵重物品收集（白名单）
```cfg
useBlacklistModeByDefault = false
defaultWhitelistItems = [
    "minecraft:diamond",
    "minecraft:emerald",
    "minecraft:gold_ingot",
    "minecraft:iron_ingot",
    "minecraft:ender_pearl"
]
absorptionRange = 16
energyCostPerItem = 0
```

#### 场景3：高性能模式
```cfg
absorptionRange = 4
absorptionInterval = 10
energyCostPerItem = 0
energyCostMultiplier = 0.1
maxEnergyCapacity = 100000000
```

---

## 配置文件访问方式

### 游戏内访问
1. 主菜单 → Mods → RS Ring → Config
2. 或按 `Mod Options` 键（如果已配置）

### 文件直接编辑
1. 关闭游戏
2. 编辑 `config/rsring/*.cfg` 文件
3. 保存并重新启动游戏

---

## 配置优先级

```
游戏内GUI修改 > 配置文件 > 默认值
```

1. **默认值**: 代码中定义的初始值
2. **配置文件**: 游戏启动时读取
3. **游戏内修改**: 实时生效并保存到配置文件

---

## 性能优化建议

### 低配置电脑
```cfg
# 戒指配置
absorptionRange = 4
absorptionInterval = 10

# 经验泵配置
xpExtractionRate = 20
xpExtractionRange = 3
```

### 高配置电脑
```cfg
# 戒指配置
absorptionRange = 16
absorptionInterval = 1

# 经验泵配置
xpExtractionRate = 500
xpExtractionRange = 10
```

---

## 常见问题

### Q: 戒指不吸收物品？
A: 检查以下配置：
- `allowCustomFilters = true`
- 确认过滤模式（黑名单/白名单）
- 检查能量是否充足

### Q: 经验泵不工作？
A: 检查以下配置：
- `enabled = true`
- `xpExtractionRate > 0`
- `maxXpStorage` 未满

### Q: 游戏卡顿？
A: 调整以下配置：
- 降低 `absorptionRange`
- 增加 `absorptionInterval`
- 降低 `xpExtractionRate`

---

## 配置重置

如果配置出现问题，可以删除配置文件让游戏重新生成：

```bash
# Windows
del config\rsring\*.cfg

# Linux/Mac
rm config/rsring/*.cfg
```

重新启动游戏后会使用默认配置。
