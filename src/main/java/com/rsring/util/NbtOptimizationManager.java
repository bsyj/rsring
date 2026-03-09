package com.rsring.util;

import com.rsring.filter.attribute.NbtAttribute;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NBT优化管理器 - 统一管理和监控所有NBT优化组件
 *
 * 核心职责：
 * 1. 协调各个NBT优化组件
 * 2. 监控内存使用情况
 * 3. 在内存压力下触发清理
 * 4. 收集性能指标
 * 5. 提供统一的配置接口
 *
 * 优化策略：
 * - 内存使用率>80%：减少缓存大小
 * - 内存使用率>90%：清空所有缓存
 * - 定期清理过期数据
 * - 动态调整池大小
 */
public class NbtOptimizationManager {

    private static final Logger LOGGER = LogManager.getLogger(NbtOptimizationManager.class);

    // 单例实例
    private static NbtOptimizationManager INSTANCE;

    // 性能指标
    private final AtomicLong totalNbtOperations = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong pooledObjectsCreated = new AtomicLong(0);
    private final AtomicLong pooledObjectsReused = new AtomicLong(0);

    // 内存监控
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private volatile long lastMemoryCheck = 0;
    private volatile int lastMemoryUsagePercent = 0;

    // 定时任务执行器
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NbtOptimization-Scheduler");
        t.setDaemon(true);
        return t;
    });

    // 配置参数
    private volatile boolean autoCleanupEnabled = true;
    private volatile int memoryThresholdHigh = 80; // 80%
    private volatile int memoryThresholdCritical = 90; // 90%
    private volatile long cleanupIntervalMs = 30000; // 30秒

    private NbtOptimizationManager() {
        // 启动定时任务
        startScheduledTasks();
    }

    /**
     * 获取管理器实例
     */
    public static synchronized NbtOptimizationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NbtOptimizationManager();
        }
        return INSTANCE;
    }

    /**
     * 启动定时任务
     */
    private void startScheduledTasks() {
        // 内存监控任务
        scheduler.scheduleAtFixedRate(this::checkMemoryPressure, 10, 10, TimeUnit.SECONDS);

        // 统计报告任务（每5分钟）
        scheduler.scheduleAtFixedRate(this::printStatistics, 5, 5, TimeUnit.MINUTES);

        LOGGER.info("NBT优化管理器已启动");
    }

    /**
     * 检查内存压力
     */
    private void checkMemoryPressure() {
        if (!autoCleanupEnabled) return;

        long now = System.currentTimeMillis();
        if (now - lastMemoryCheck < 5000) return; // 5秒内不重复检查
        lastMemoryCheck = now;

        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long maxMemory = heapUsage.getMax();
        long usedMemory = heapUsage.getUsed();

        if (maxMemory <= 0) {
            maxMemory = heapUsage.getCommitted();
        }

        int usagePercent = (int) ((usedMemory * 100) / maxMemory);
        lastMemoryUsagePercent = usagePercent;

        if (usagePercent > memoryThresholdCritical) {
            LOGGER.warn("内存使用率严重过高: {}%，执行紧急清理", usagePercent);
            performEmergencyCleanup();
        } else if (usagePercent > memoryThresholdHigh) {
            LOGGER.debug("内存使用率较高: {}%，执行常规清理", usagePercent);
            performRegularCleanup();
        }
    }

    /**
     * 执行常规清理
     */
    private void performRegularCleanup() {
        // 减少缓存大小（调用无参方法）
        SmartNbtCache.getInstance().reduceCacheSize();

        // 清理NBT匹配器缓存
        if (NbtMatcher.getCacheSize() > 500) {
            NbtMatcher.clearCache();
        }

        // 建议GC（但不强制）
        System.gc();

        LOGGER.debug("常规清理完成");
    }

    /**
     * 执行紧急清理
     */
    private void performEmergencyCleanup() {
        // 清空所有缓存
        SmartNbtCache.getInstance().clear();
        NbtMatcher.clearCache();

        // 强制GC
        System.gc();

        LOGGER.warn("紧急清理完成");
    }

    /**
     * 打印统计信息
     */
    private void printStatistics() {
        if (!LOGGER.isDebugEnabled()) return;

        long totalOps = totalNbtOperations.get();
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (hits * 100.0 / total) : 0;

        LOGGER.debug("=== NBT优化统计 ===");
        LOGGER.debug("总操作数: {}", totalOps);
        LOGGER.debug("缓存命中率: {:.2f}% ({}/{})", hitRate, hits, total);
        LOGGER.debug("对象池创建: {}, 复用: {}",
            pooledObjectsCreated.get(), pooledObjectsReused.get());
        LOGGER.debug("内存使用率: {}%", lastMemoryUsagePercent);
        LOGGER.debug("SmartNbtCache大小: {}", SmartNbtCache.getInstance().size());
        LOGGER.debug("NbtMatcher缓存: {}", NbtMatcher.getCacheSize());
        LOGGER.debug("NbtAttribute路径缓存: {}", NbtAttribute.getPathCacheSize());
    }

    // ==================== 公共API ====================

    /**
     * 记录NBT操作
     */
    public void recordNbtOperation() {
        totalNbtOperations.incrementAndGet();
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * 记录对象池创建
     */
    public void recordPooledObjectCreated() {
        pooledObjectsCreated.incrementAndGet();
    }

    /**
     * 记录对象池复用
     */
    public void recordPooledObjectReused() {
        pooledObjectsReused.incrementAndGet();
    }

    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (hits * 100.0 / total) : 0;
    }

    /**
     * 获取对象池复用率
     */
    public double getPoolReuseRate() {
        long created = pooledObjectsCreated.get();
        long reused = pooledObjectsReused.get();
        long total = created + reused;
        return total > 0 ? (reused * 100.0 / total) : 0;
    }

    /**
     * 手动触发清理
     */
    public void triggerCleanup() {
        performRegularCleanup();
    }

    /**
     * 手动触发紧急清理
     */
    public void triggerEmergencyCleanup() {
        performEmergencyCleanup();
    }

    /**
     * 设置自动清理开关
     */
    public void setAutoCleanupEnabled(boolean enabled) {
        this.autoCleanupEnabled = enabled;
    }

    /**
     * 设置内存阈值
     */
    public void setMemoryThresholds(int high, int critical) {
        this.memoryThresholdHigh = high;
        this.memoryThresholdCritical = critical;
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        LOGGER.info("NBT优化管理器已关闭");
    }

    /**
     * 获取当前内存使用率
     */
    public int getCurrentMemoryUsage() {
        return lastMemoryUsagePercent;
    }

    /**
     * 获取完整统计信息
     */
    public NbtOptimizationStats getStats() {
        return new NbtOptimizationStats(
            totalNbtOperations.get(),
            cacheHits.get(),
            cacheMisses.get(),
            pooledObjectsCreated.get(),
            pooledObjectsReused.get(),
            lastMemoryUsagePercent,
            SmartNbtCache.getInstance().size(),
            NbtMatcher.getCacheSize(),
            NbtAttribute.getPathCacheSize()
        );
    }

    /**
     * 统计信息数据类
     */
    public static class NbtOptimizationStats {
        public final long totalOperations;
        public final long cacheHits;
        public final long cacheMisses;
        public final long pooledCreated;
        public final long pooledReused;
        public final int memoryUsagePercent;
        public final int smartCacheSize;
        public final int matcherCacheSize;
        public final int pathCacheSize;

        public NbtOptimizationStats(long totalOperations, long cacheHits, long cacheMisses,
                                     long pooledCreated, long pooledReused, int memoryUsagePercent,
                                     int smartCacheSize, int matcherCacheSize, int pathCacheSize) {
            this.totalOperations = totalOperations;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.pooledCreated = pooledCreated;
            this.pooledReused = pooledReused;
            this.memoryUsagePercent = memoryUsagePercent;
            this.smartCacheSize = smartCacheSize;
            this.matcherCacheSize = matcherCacheSize;
            this.pathCacheSize = pathCacheSize;
        }

        public double getCacheHitRate() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (cacheHits * 100.0 / total) : 0;
        }

        public double getPoolReuseRate() {
            long total = pooledCreated + pooledReused;
            return total > 0 ? (pooledReused * 100.0 / total) : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "NbtOptimizationStats[ops=%d, cacheHit=%.2f%%, poolReuse=%.2f%%, mem=%d%%, " +
                "smartCache=%d, matcherCache=%d, pathCache=%d]",
                totalOperations, getCacheHitRate(), getPoolReuseRate(),
                memoryUsagePercent, smartCacheSize, matcherCacheSize, pathCacheSize
            );
        }
    }
}
