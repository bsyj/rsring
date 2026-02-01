# RS Ring Mod

## 功能介绍

RS Ring是一个Minecraft饰品模组，允许玩家制作多个特殊的戒指和设备，具有以下功能：

### 主要特性
1. **箱子戒指（Chest Ring）**：蹲下右键点击箱子/容器进行绑定，自动收集玩家周围8格内的物品并传输到绑定的箱子
2. **快捷键控制**：按K键开启或关闭自动收集功能
3. **FE能量驱动**：使用FE能量运行，最大容量10M FE
4. **跨维度物品收集**：自动收集玩家周围的物品并传输到绑定的箱子，支持跨维度传输
5. **Baubles兼容性**：完全支持在Baubles饰品槽位中使用戒指
6. **经验泵（Experience Pump）**：自动收集经验并存储
7. **经验储罐（Experience Tank）**：存储经验的容器
8. **经验泵控制器（Experience Pump Controller）**：控制经验泵的合成配方

### 使用方法
1. 制作箱子戒指（具体合成配方需在游戏中查看）
2. 将戒指装备到饰品栏（需要Baubles模组支持）或手持
3. 蹲下并右键点击箱子/容器进行绑定
4. 按K键开启/关闭自动收集功能
5. 当功能开启时，戒指会消耗FE能量自动收集玩家周围8格内的物品并传输到绑定的箱子

### 技术细节
- 每tick消耗1FE维持运行
- 每次传输物品消耗1FE
- 支持跨维度传输（可在不同维度间传输物品）
- 使用Forge能力系统存储绑定信息和能量数据
- 改进了Baubles集成，确保戒指在饰品槽位中正常工作
- 实现了统一的环检测服务（RingDetectionService），优化了戒指查找算法
- 支持黑白名单功能，可控制哪些物品会被收集

### 依赖
- Minecraft 1.12.2
- Forge 14.23.5.2847 或更高版本
- Baubles 1.5.2 或更高版本（可选，用于饰品栏支持）
- JEI 4.16.1.1013 或更高版本（可选，用于配方显示）

## 编译说明
要编译此模组，您需要：
1. 安装JDK 8（不是JRE）
2. 配置JAVA_HOME环境变量指向JDK安装目录
3. 运行 `gradlew build` 命令
4. 编译后的文件将在 `build/libs` 目录中生成

## 文件结构
```
src/
├── main/
│   ├── java/
│   │   └── com/moremod/
│   │       ├── capability/
│   │       │   ├── IExperiencePumpCapability.java  # 经验泵能力接口
│   │       │   ├── IRsRingCapability.java          # RS戒指能力接口
│   │       │   ├── ExperiencePumpCapability.java   # 经验泵能力实现
│   │       │   └── RsRingCapability.java           # RS戒指能力实现
│   │       ├── client/
│   │       ├── config/
│   │       ├── event/
│   │       │   └── CommonEventHandler.java         # 事件处理器
│   │       ├── experience/
│   │       ├── integration/
│   │       ├── item/
│   │       │   ├── ItemChestRing.java              # 箱子戒指实现
│   │       │   ├── ItemExperiencePump.java         # 经验泵实现
│   │       │   └── ItemExperiencePumpController.java # 经验泵控制器实现
│   │       ├── network/
│   │       ├── proxy/
│   │       ├── rsring/
│   │       │   └── RsRingMod.java                  # 主模组类
│   │       └── service/
│   │           └── RingDetectionService.java       # 统一的戒指检测服务
│   └── resources/
│       └── assets/
│           └── rsring/
│               ├── lang/
│               │   ├── en_us.lang                  # 英文语言文件
│               │   └── zh_cn.lang                  # 中文语言文件
│               ├── models/
│               ├── recipes/
│               └── textures/
```

## 开发说明
### Baubles环检测修复
最近的更新解决了戒指在Baubles饰品槽位中无法正常工作的关键问题。主要改进包括：

1. **统一的环检测服务**：创建了集中式的`RingDetectionService`，消除了代码重复
2. **增强的Baubles API集成**：改进了反射调用，增加了错误处理机制
3. **优先级搜索顺序**：确保戒指检测遵循手部→Baubles饰品槽→背包的优先级顺序
4. **错误处理**：增强了异常处理，防止Baubles API调用失败时出现静默错误

设计文档详见 `.kiro/specs/baubles-ring-detection-fix/design.md`

### 测试
项目包含全面的测试套件，包括单元测试和集成测试，确保功能的稳定性和可靠性。

## 注意事项
- 确保绑定的箱子所在的维度在服务器运行期间保持加载
- 当能量不足时，戒指会停止工作直到重新充能
- 绑定的箱子必须可访问（未被破坏且维度存在）
- 如需饰品栏支持，需要安装Baubles模组
- 编译需要JDK 8环境
- 支持黑白名单功能，可通过Shift+右键戒指查看详细信息