# 经验储罐右键显示容量信息功能

## 功能描述

右键点击经验储罐时，在聊天栏显示储罐的容量信息，包括：
- 等级
- 当前存储的经验值
- 最大容量

## 实现细节

### 修改文件
- `rsring/src/main/java/com/moremod/item/ItemExperiencePump.java`

### 代码实现

```java
@Override
public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    
    // 服务器端：显示储罐容量信息到聊天栏
    if (!world.isRemote) {
        // 从NBT读取数据（确保准确性）
        int xpStored = getXpStoredFromNBT(stack);
        int capacityLevels = getCapacityLevelsFromNBT(stack);
        int maxXp = getMaxXpFromNBT(stack);
        
        // 构造消息：等级 X - Y / Z mb
        String message = TextFormatting.AQUA + "等级 " + capacityLevels + 
                       TextFormatting.GRAY + " - " +
                       TextFormatting.GREEN + xpStored + 
                       TextFormatting.GRAY + " / " + 
                       TextFormatting.YELLOW + maxXp + 
                       TextFormatting.GRAY + " mb";
        
        player.sendMessage(new TextComponentString(message));
    }
    
    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
}
```

### 显示格式

聊天栏消息格式：
```
等级 X - Y / Z mb
```

其中：
- **等级 X**：青色（AQUA）
- **Y**（当前经验）：绿色（GREEN）
- **Z**（最大容量）：黄色（YELLOW）
- **分隔符和单位**：灰色（GRAY）

### 示例输出

1. **1级空储罐**：
   ```
   等级 1 - 0 / 1000 mb
   ```

2. **6级部分填充储罐**：
   ```
   等级 6 - 2542 / 32000 mb
   ```

3. **7级满储罐**：
   ```
   等级 7 - 64000 / 64000 mb
   ```

## 技术要点

### 1. 服务器端执行
```java
if (!world.isRemote) {
    // 只在服务器端执行，避免客户端重复显示
}
```

### 2. 从NBT读取数据
使用 `getXpStoredFromNBT()` 和 `getCapacityLevelsFromNBT()` 方法直接从NBT读取数据，确保显示的是最新、最准确的信息。

### 3. 返回值
返回 `EnumActionResult.SUCCESS` 表示操作成功，这会：
- 触发手臂挥动动画
- 播放使用物品的音效
- 正确处理物品使用事件

## 使用方法

1. 手持经验储罐
2. 右键点击（空气或方块均可）
3. 查看聊天栏显示的容量信息

## 优势

1. **便捷性**：无需打开GUI或查看tooltip，快速查看容量
2. **准确性**：直接从NBT读取，确保数据准确
3. **清晰性**：彩色格式化输出，信息一目了然
4. **轻量级**：不需要额外的GUI或网络包

## 兼容性

- 与经验泵控制器完全兼容
- 不影响储罐的其他功能（吸收经验、修补等）
- 支持所有等级的储罐（1-12级）

## 修改日期

2026-02-02

## 状态

✅ 已实现并编译成功
