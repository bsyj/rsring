package com.rsring.filter;

import com.rsring.util.NbtHashCache;
import com.rsring.util.NbtMatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.*;

/**
 * 过滤缓存系统 - 优化物品匹配性能
 * 
 * 原理：
 * 1. 将过滤列表转换为HashSet，实现O(1)查找
 * 2. 缓存ItemStack的解析结果（registryName, modId, nbtHash）
 * 3. 避免每次匹配时重复解析ItemStack
 * 4. 过滤列表变化时自动重建缓存
 */
public class FilterCache {
    
    // 缓存类型
    public enum CacheType {
        ITEM_ID,      // 物品ID过滤
        MOD_ID,       // 模组ID过滤
        NBT_DATA      // NBT数据过滤
    }
    
    // 缓存条目 - 使用对象池减少GC压力
    public static class CacheEntry {
        public final String registryName;
        public final String modId;
        public final int nbtHash;
        public final boolean hasNbt;

        // 空条目常量，避免重复创建
        public static final CacheEntry EMPTY = new CacheEntry("", "", 0, false);

        private CacheEntry(String registryName, String modId, int nbtHash, boolean hasNbt) {
            this.registryName = registryName;
            this.modId = modId;
            this.nbtHash = nbtHash;
            this.hasNbt = hasNbt;
        }

        public static CacheEntry create(ItemStack stack) {
            if (stack.isEmpty()) {
                return EMPTY;
            }

            // 使用局部变量减少字段访问
            String registryName = stack.getItem().getRegistryName().toString();
            int colonIndex = registryName.indexOf(':');
            String modId = colonIndex > 0 ? registryName.substring(0, colonIndex) : "minecraft";

            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null && !nbt.isEmpty()) {
                return new CacheEntry(registryName, modId, nbt.hashCode(), true);
            } else {
                return new CacheEntry(registryName, modId, 0, false);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheEntry)) return false;
            CacheEntry that = (CacheEntry) o;
            return registryName.equals(that.registryName);
        }

        @Override
        public int hashCode() {
            return registryName.hashCode();
        }
    }
    
    // 物品ID缓存 - 使用HashSet实现O(1)查找
    private Set<String> itemIdCache = Collections.emptySet();
    
    // 模组ID缓存
    private Set<String> modIdCache = Collections.emptySet();
    
    // NBT模板缓存 - 存储过滤槽位的NBT数据
    private Map<Integer, NBTTagCompound> nbtTemplateCache = Collections.emptyMap();
    
    // 预编译的NBT匹配器缓存 - 优化NBT匹配性能
    private Map<Integer, NbtMatcher> nbtMatcherCache = Collections.emptyMap();
    
    // 缓存是否有效
    private boolean valid = false;
    
    // 缓存版本号（用于检测变化）
    private int version = 0;
    
    // 上次访问时间（用于LRU清理）
    private long lastAccessTime = 0;
    
    /**
     * 从过滤列表构建缓存
     * 
     * @param itemIds 物品ID列表
     * @param modIds 模组ID列表
     * @param nbtTemplates NBT模板
     */
    public void buildCache(List<String> itemIds, List<String> modIds, Map<Integer, NBTTagCompound> nbtTemplates) {
        // 构建物品ID缓存
        if (itemIds != null && !itemIds.isEmpty()) {
            Set<String> newItemCache = new HashSet<>(itemIds.size());
            for (String id : itemIds) {
                if (id != null && !id.isEmpty()) {
                    newItemCache.add(id);
                }
            }
            this.itemIdCache = newItemCache;
        } else {
            this.itemIdCache = Collections.emptySet();
        }
        
        // 构建模组ID缓存
        if (modIds != null && !modIds.isEmpty()) {
            Set<String> newModCache = new HashSet<>(modIds.size());
            for (String id : modIds) {
                if (id != null && !id.isEmpty()) {
                    newModCache.add(id);
                }
            }
            this.modIdCache = newModCache;
        } else {
            this.modIdCache = Collections.emptySet();
        }
        
        // 构建NBT模板缓存
        if (nbtTemplates != null && !nbtTemplates.isEmpty()) {
            this.nbtTemplateCache = new HashMap<>(nbtTemplates);
            // 预编译NBT匹配器
            Map<Integer, NbtMatcher> matchers = new HashMap<>();
            for (Map.Entry<Integer, NBTTagCompound> entry : nbtTemplates.entrySet()) {
                NBTTagCompound template = entry.getValue();
                if (template != null && !template.isEmpty()) {
                    NbtMatcher matcher = NbtMatcher.getMatcher(template);
                    if (matcher != null) {
                        matchers.put(entry.getKey(), matcher);
                    }
                }
            }
            this.nbtMatcherCache = matchers;
        } else {
            this.nbtTemplateCache = Collections.emptyMap();
            this.nbtMatcherCache = Collections.emptyMap();
        }
        
        this.valid = true;
        this.version++;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * 快速检查物品是否在过滤列表中
     * 
     * @param stack 要检查的物品
     * @param cacheEntry 缓存的物品条目（可为null，会重新创建）
     * @return 是否在列表中
     */
    public boolean containsItem(ItemStack stack, CacheEntry cacheEntry) {
        if (!valid || stack.isEmpty()) return false;
        
        CacheEntry entry = cacheEntry != null ? cacheEntry : new CacheEntry(stack);
        lastAccessTime = System.currentTimeMillis();
        
        return itemIdCache.contains(entry.registryName);
    }
    
    /**
     * 快速检查模组是否在过滤列表中
     * 
     * @param stack 要检查的物品
     * @param cacheEntry 缓存的物品条目（可为null，会重新创建）
     * @return 是否在列表中
     */
    public boolean containsMod(ItemStack stack, CacheEntry cacheEntry) {
        if (!valid || stack.isEmpty()) return false;
        
        CacheEntry entry = cacheEntry != null ? cacheEntry : new CacheEntry(stack);
        lastAccessTime = System.currentTimeMillis();
        
        return modIdCache.contains(entry.modId);
    }
    
    /**
     * 检查物品的NBT是否匹配过滤模板
     * 
     * @param stack 要检查的物品
     * @param matchPartial 是否允许部分匹配（模板中的NBT是物品的子集）
     * @return 是否匹配
     */
    public boolean matchesNbt(ItemStack stack, boolean matchPartial) {
        if (!valid || stack.isEmpty() || nbtTemplateCache.isEmpty()) return false;
        
        NBTTagCompound itemNbt = stack.getTagCompound();
        if (itemNbt == null) itemNbt = new NBTTagCompound();
        
        lastAccessTime = System.currentTimeMillis();
        
        // 优先使用预编译的NBT匹配器（性能优化）
        if (!nbtMatcherCache.isEmpty()) {
            for (NbtMatcher matcher : nbtMatcherCache.values()) {
                if (matcher == null) continue;
                
                if (matchPartial) {
                    // 使用预编译匹配器进行部分匹配
                    if (matcher.matchesPartial(itemNbt)) {
                        return true;
                    }
                } else {
                    // 使用预编译匹配器进行完全匹配
                    if (matcher.matchesExact(itemNbt)) {
                        return true;
                    }
                }
            }
        } else {
            // 回退到传统匹配方式
            for (NBTTagCompound template : nbtTemplateCache.values()) {
                if (template == null || template.isEmpty()) continue;
                
                if (matchPartial) {
                    // 部分匹配：模板中的每个键值对都必须在物品NBT中存在且相等
                    if (isPartialMatch(template, itemNbt)) {
                        return true;
                    }
                } else {
                    // 完全匹配：使用NbtHashCache加速
                    if (NbtHashCache.fastEquals(template, itemNbt)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查模板是否是目标NBT的子集（传统递归方式）
     * 保留此方法作为回退方案
     */
    private boolean isPartialMatch(NBTTagCompound template, NBTTagCompound target) {
        for (String key : template.getKeySet()) {
            if (!target.hasKey(key)) return false;
            
            net.minecraft.nbt.NBTBase templateValue = template.getTag(key);
            net.minecraft.nbt.NBTBase targetValue = target.getTag(key);
            
            if (templateValue instanceof NBTTagCompound && targetValue instanceof NBTTagCompound) {
                // 递归检查嵌套NBT
                if (!isPartialMatch((NBTTagCompound) templateValue, (NBTTagCompound) targetValue)) {
                    return false;
                }
            } else if (!templateValue.equals(targetValue)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 使缓存失效
     */
    public void invalidate() {
        this.valid = false;
        this.itemIdCache = Collections.emptySet();
        this.modIdCache = Collections.emptySet();
        this.nbtTemplateCache = Collections.emptyMap();
        this.nbtMatcherCache = Collections.emptyMap();
    }
    
    /**
     * 检查缓存是否有效
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * 获取缓存版本号
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * 获取上次访问时间
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    /**
     * 检查缓存是否过期（用于定期清理）
     * 
     * @param maxAgeMs 最大存活时间（毫秒）
     * @return 是否过期
     */
    public boolean isExpired(long maxAgeMs) {
        return System.currentTimeMillis() - lastAccessTime > maxAgeMs;
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return new CacheStats(itemIdCache.size(), modIdCache.size(), nbtTemplateCache.size(), valid, version);
    }
    
    /**
     * 缓存统计
     */
    public static class CacheStats {
        public final int itemIdCount;
        public final int modIdCount;
        public final int nbtTemplateCount;
        public final boolean valid;
        public final int version;
        
        public CacheStats(int itemIdCount, int modIdCount, int nbtTemplateCount, boolean valid, int version) {
            this.itemIdCount = itemIdCount;
            this.modIdCount = modIdCount;
            this.nbtTemplateCount = nbtTemplateCount;
            this.valid = valid;
            this.version = version;
        }
        
        @Override
        public String toString() {
            return String.format("FilterCache[items=%d, mods=%d, nbts=%d, valid=%s, v=%d]",
                itemIdCount, modIdCount, nbtTemplateCount, valid, version);
        }
    }
}
