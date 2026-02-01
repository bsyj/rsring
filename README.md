# RS Ring Mod

## 功能介绍

RS Ring是一个Minecraft饰品模组，允许玩家制作一个特殊的戒指，具有以下功能：

### 主要特性
1. **绑定RS终端**：蹲下右键点击Refined Storage终端进行绑定
2. **快捷键控制**：按K键开启或关闭自动收集功能
3. **FE能量驱动**：使用FE能量运行，最大容量10M FE
4. **跨维度物品收集**：自动收集玩家周围的物品并传输到绑定的RS终端，支持跨维度传输
5. **Baubles兼容性**：完全支持在Baubles饰品槽位中使用戒指

### 使用方法
1. 制作RS Ring（具体合成配方需在游戏中查看）
2. 将戒指装备到饰品栏（需要Baubles模组支持）或手持
3. 蹲下并右键点击RS终端进行绑定
4. 按K键开启/关闭自动收集功能
5. 当功能开启时，戒指会消耗FE能量自动收集玩家周围8格内的物品并传输到绑定的RS终端

### 技术细节
- 每tick消耗1FE维持运行
- 每次传输物品消耗100FE
- 支持跨维度传输（可在不同维度间传输物品）
- 使用Forge能力系统存储绑定信息和能量数据
- 改进了Baubles集成，确保戒指在饰品槽位中正常工作

### 依赖
- Minecraft 1.12.2
- Forge 14.23.5.2847 或更高版本
- Baubles 1.5.2 或更高版本（可选，用于饰品栏支持）
- Refined Storage 1.6.16 或更高版本

## 编译说明
要编译此模组，您需要：
1. 安装JDK 8（不是JRE）
2. 配置JAVA_HOME环境变量指向JDK安装目录
3. 运行 `gradlew build` 命令

## 文件结构
```
src/
├── main/
│   ├── java/
│   │   └── com/moremod/
│   │       ├── rsring/
│   │       │   └── RsRingMod.java          # 主模组类
│   │       ├── item/
│   │       │   └── ItemRsRing.java         # 饰品实现
│   │       ├── capability/
│   │       │   ├── IRsRingCapability.java  # 能力接口
│   │       │   └── RsRingCapability.java   # 能力实现
│   │       └── event/
│   │           └── CommonEventHandler.java # 事件处理器
│   └── resources/
│       └── assets/
│           └── rsring/
│               └── lang/
│                   ├── en_us.json          # 英文语言文件
│                   └── zh_cn.json          # 中文语言文件
```

## 开发说明
### Baubles环检测修复
最近的更新解决了戒指在Baubles饰品槽位中无法正常工作的关键问题。主要改进包括：

1. **统一的环检测服务**：创建了集中式的`RingDetectionService`，消除了代码重复
2. **增强的Baubles API集成**：改进了反射调用，增加了错误处理机制
3. **优先级搜索顺序**：确保戒指检测遵循手部→Baubles饰品槽→背包的优先级顺序
4. **错误处理**：增强了异常处理，防止Baubles API调用失败时出现静默错误

设计文档详见 `.kiro/specs/baubles-ring-detection-fix/design.md`

## 注意事项
- 确保RS终端所在的维度在服务器运行期间保持加载
- 当能量不足时，戒指会停止工作直到重新充能
- 绑定的终端必须可访问（未被破坏且维度存在）
- 如需饰品栏支持，需要安装Baubles模组
- 编译需要JDK 8环境