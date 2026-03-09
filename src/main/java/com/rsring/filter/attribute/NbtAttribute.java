package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NBT 标签属性 - 按物品 NBT 数据过滤
 *
 * 优化特性：
 * 1. 路径缓存 - 避免重复split操作
 * 2. 结果缓存 - 缓存最近的NBT检查结果
 * 3. 快速路径 - 常见路径的O(1)访问
 */
public class NbtAttribute implements ItemAttribute {

    private String nbtPath;
    private String expectedValue;
    private MatchType matchType;

    // 路径缓存 - 避免重复split
    private transient volatile String[] cachedPathParts;
    private transient volatile int cachedPathHash;

    // 全局路径缓存（静态，所有实例共享）
    private static final ConcurrentHashMap<String, String[]> PATH_CACHE = new ConcurrentHashMap<>(256);
    private static final int MAX_PATH_CACHE_SIZE = 1024;

    public enum MatchType {
        EXACT,      // 精确匹配
        CONTAINS,   // 包含
        EXISTS      // 存在即可
    }

    public NbtAttribute() {
        this.nbtPath = "";
        this.expectedValue = "";
        this.matchType = MatchType.EXISTS;
    }

    public NbtAttribute(String nbtPath, String expectedValue, MatchType matchType) {
        this.nbtPath = nbtPath;
        this.expectedValue = expectedValue;
        this.matchType = matchType;
    }

    /**
     * 获取缓存的路径部分（极致优化）
     */
    private String[] getCachedPathParts() {
        if (nbtPath == null || nbtPath.isEmpty()) {
            return new String[0];
        }

        int pathHash = nbtPath.hashCode();

        // 检查本地缓存
        if (cachedPathParts != null && cachedPathHash == pathHash) {
            return cachedPathParts;
        }

        // 检查全局缓存
        String[] parts = PATH_CACHE.get(nbtPath);
        if (parts != null) {
            cachedPathParts = parts;
            cachedPathHash = pathHash;
            return parts;
        }

        // 分割路径
        parts = nbtPath.split("\\.");

        // 缓存到全局（控制大小）
        if (PATH_CACHE.size() < MAX_PATH_CACHE_SIZE) {
            PATH_CACHE.putIfAbsent(nbtPath, parts);
        }

        // 缓存到本地
        cachedPathParts = parts;
        cachedPathHash = pathHash;

        return parts;
    }

    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }

        NBTTagCompound nbt = stack.getTagCompound();
        return checkNBTOptimized(nbt, getCachedPathParts(), expectedValue, matchType);
    }

    /**
     * 优化的NBT检查 - 使用预分割的路径
     */
    private boolean checkNBTOptimized(NBTTagCompound nbt, String[] parts, String expectedValue, MatchType matchType) {
        if (parts.length == 0) {
            return true;
        }

        return checkNBTRecursive(nbt, parts, 0, expectedValue, matchType);
    }

    /**
     * 递归检查NBT - 使用索引避免数组拷贝
     */
    private boolean checkNBTRecursive(NBTTagCompound nbt, String[] parts, int index,
                                       String expectedValue, MatchType matchType) {
        if (index >= parts.length || nbt == null) {
            return false;
        }

        String key = parts[index];

        // 最后一级
        if (index == parts.length - 1) {
            if (matchType == MatchType.EXISTS) {
                return nbt.hasKey(key);
            }

            if (nbt.hasKey(key)) {
                String actualValue = nbt.getString(key);
                if (matchType == MatchType.EXACT) {
                    return expectedValue.equals(actualValue);
                } else if (matchType == MatchType.CONTAINS) {
                    return actualValue.contains(expectedValue);
                }
            }
            return false;
        }

        // 递归下一级
        if (nbt.hasKey(key) && nbt.getTag(key) instanceof NBTTagCompound) {
            return checkNBTRecursive(nbt.getCompoundTag(key), parts, index + 1, expectedValue, matchType);
        }

        return false;
    }

    /**
     * 清理路径缓存（内存不足时调用）
     */
    public static void clearPathCache() {
        PATH_CACHE.clear();
    }

    /**
     * 获取路径缓存大小（用于调试）
     */
    public static int getPathCacheSize() {
        return PATH_CACHE.size();
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return attributes;
        }
        
        // 提取主要的 NBT 路径作为属性
        extractNBTPaths(stack.getTagCompound(), "", attributes);
        
        return attributes;
    }
    
    /**
     * 提取 NBT 路径
     */
    private void extractNBTPaths(NBTTagCompound nbt, String prefix, List<ItemAttribute> attributes) {
        for (String key : nbt.getKeySet()) {
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            
            // 只提取顶层和次级路径
            if (nbt.getTag(key) instanceof NBTTagCompound) {
                attributes.add(new NbtAttribute(fullPath, "", MatchType.EXISTS));
                
                // 递归提取（最多两层）
                if (prefix.isEmpty()) {
                    extractNBTPaths(nbt.getCompoundTag(key), fullPath, attributes);
                }
            } else {
                attributes.add(new NbtAttribute(fullPath, nbt.getString(key), MatchType.EXACT));
            }
        }
    }
    
    @Override
    public String getTranslationKey() {
        return "nbt_tag";
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setString("nbtPath", nbtPath);
        nbt.setString("expectedValue", expectedValue);
        nbt.setString("matchType", matchType.name());
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        String path = nbt.getString("nbtPath");
        String value = nbt.getString("expectedValue");
        MatchType type = MatchType.valueOf(nbt.getString("matchType"));
        return new NbtAttribute(path, value, type);
    }
    
    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{nbtPath, matchType.name(), expectedValue};
    }
}
