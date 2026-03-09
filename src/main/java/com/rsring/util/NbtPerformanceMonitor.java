package com.rsring.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NBT性能监控器 - 用于监控NBT操作的性能
 * 
 * 功能：
 * 1. 记录各种NBT操作的耗时和调用次数
 * 2. 计算平均耗时和调用频率
 * 3. 输出性能统计报告
 * 
 * 使用方式：
 * NbtPerformanceMonitor.recordOperation("serialize", () -> { ... });
 * 或
 * long start = System.nanoTime();
 * // ... NBT操作 ...
 * NbtPerformanceMonitor.recordTime("serialize", System.nanoTime() - start);
 */
public class NbtPerformanceMonitor {
    
    private static final Logger LOGGER = LogManager.getLogger(NbtPerformanceMonitor.class);
    
    // 是否启用监控（可通过配置控制）
    private static volatile boolean enabled = false;
    
    // 操作统计映射
    private static final Map<String, OperationStats> statsMap = new ConcurrentHashMap<>();
    
    // 慢操作阈值（纳秒）- 超过此值记录警告
    private static final long SLOW_OPERATION_THRESHOLD_NS = 1_000_000; // 1ms
    
    // 慢操作阈值（毫秒）
    private static final long SLOW_OPERATION_THRESHOLD_MS = 10;
    
    /**
     * 操作统计信息
     */
    private static class OperationStats {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicLong totalTime = new AtomicLong(0);
        final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong maxTime = new AtomicLong(0);
        final AtomicInteger slowCount = new AtomicInteger(0);
        
        void record(long timeNanos) {
            count.incrementAndGet();
            totalTime.addAndGet(timeNanos);
            
            // 更新最小值
            long currentMin;
            do {
                currentMin = minTime.get();
                if (timeNanos >= currentMin) break;
            } while (!minTime.compareAndSet(currentMin, timeNanos));
            
            // 更新最大值
            long currentMax;
            do {
                currentMax = maxTime.get();
                if (timeNanos <= currentMax) break;
            } while (!maxTime.compareAndSet(currentMax, timeNanos));
            
            // 记录慢操作
            if (timeNanos > SLOW_OPERATION_THRESHOLD_NS) {
                slowCount.incrementAndGet();
            }
        }
        
        double getAverageMs() {
            int c = count.get();
            if (c == 0) return 0;
            return (totalTime.get() / (double) c) / 1_000_000.0;
        }
        
        double getMinMs() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min / 1_000_000.0;
        }
        
        double getMaxMs() {
            return maxTime.get() / 1_000_000.0;
        }
    }
    
    /**
     * 启用监控
     */
    public static void enable() {
        enabled = true;
        LOGGER.info("NBT性能监控已启用");
    }
    
    /**
     * 禁用监控
     */
    public static void disable() {
        enabled = false;
        LOGGER.info("NBT性能监控已禁用");
    }
    
    /**
     * 检查是否启用
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 记录操作耗时
     * 
     * @param operation 操作名称
     * @param timeNanos 耗时（纳秒）
     */
    public static void recordTime(String operation, long timeNanos) {
        if (!enabled) return;
        
        statsMap.computeIfAbsent(operation, k -> new OperationStats())
                .record(timeNanos);
    }
    
    /**
     * 记录操作（使用Lambda）
     * 
     * @param operation 操作名称
     * @param runnable 要执行的操作
     */
    public static void recordOperation(String operation, Runnable runnable) {
        if (!enabled) {
            runnable.run();
            return;
        }
        
        long start = System.nanoTime();
        try {
            runnable.run();
        } finally {
            recordTime(operation, System.nanoTime() - start);
        }
    }
    
    /**
     * 记录操作并返回结果（使用Lambda）
     * 
     * @param operation 操作名称
     * @param supplier 要执行的操作
     * @return 操作结果
     */
    public static <T> T recordOperation(String operation, java.util.function.Supplier<T> supplier) {
        if (!enabled) {
            return supplier.get();
        }
        
        long start = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            recordTime(operation, System.nanoTime() - start);
        }
    }
    
    /**
     * 获取操作统计
     * 
     * @param operation 操作名称
     * @return 统计信息，不存在返回null
     */
    public static OperationMetrics getMetrics(String operation) {
        OperationStats stats = statsMap.get(operation);
        if (stats == null) {
            return null;
        }
        
        return new OperationMetrics(
            operation,
            stats.count.get(),
            stats.getAverageMs(),
            stats.getMinMs(),
            stats.getMaxMs(),
            stats.slowCount.get()
        );
    }
    
    /**
     * 获取所有操作统计
     */
    public static Map<String, OperationMetrics> getAllMetrics() {
        Map<String, OperationMetrics> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, OperationStats> entry : statsMap.entrySet()) {
            OperationStats stats = entry.getValue();
            result.put(entry.getKey(), new OperationMetrics(
                entry.getKey(),
                stats.count.get(),
                stats.getAverageMs(),
                stats.getMinMs(),
                stats.getMaxMs(),
                stats.slowCount.get()
            ));
        }
        return result;
    }
    
    /**
     * 输出性能报告到日志
     */
    public static void printReport() {
        if (!enabled) {
            LOGGER.info("NBT性能监控未启用");
            return;
        }
        
        if (statsMap.isEmpty()) {
            LOGGER.info("NBT性能监控：暂无数据");
            return;
        }
        
        LOGGER.info("========== NBT性能监控报告 ==========");
        LOGGER.info(String.format("%-20s %10s %12s %12s %12s %10s", 
            "操作", "调用次数", "平均(ms)", "最小(ms)", "最大(ms)", "慢操作"));
        
        for (Map.Entry<String, OperationStats> entry : statsMap.entrySet()) {
            OperationStats stats = entry.getValue();
            LOGGER.info(String.format("%-20s %10d %12.3f %12.3f %12.3f %10d",
                entry.getKey(),
                stats.count.get(),
                stats.getAverageMs(),
                stats.getMinMs(),
                stats.getMaxMs(),
                stats.slowCount.get()
            ));
        }
        
        LOGGER.info("=====================================");
    }
    
    /**
     * 重置所有统计
     */
    public static void reset() {
        statsMap.clear();
        LOGGER.info("NBT性能监控数据已重置");
    }
    
    /**
     * 操作指标数据类
     */
    public static class OperationMetrics {
        public final String operation;
        public final int count;
        public final double averageMs;
        public final double minMs;
        public final double maxMs;
        public final int slowCount;
        
        public OperationMetrics(String operation, int count, double averageMs, 
                               double minMs, double maxMs, int slowCount) {
            this.operation = operation;
            this.count = count;
            this.averageMs = averageMs;
            this.minMs = minMs;
            this.maxMs = maxMs;
            this.slowCount = slowCount;
        }
        
        @Override
        public String toString() {
            return String.format("%s: count=%d, avg=%.3fms, min=%.3fms, max=%.3fms, slow=%d",
                operation, count, averageMs, minMs, maxMs, slowCount);
        }
    }
}
