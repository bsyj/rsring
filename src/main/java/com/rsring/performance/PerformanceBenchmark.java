package com.rsring.performance;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 性能基准测试框架
 * 用于测量优化前后的性能指标
 */
public class PerformanceBenchmark {
    private static final Logger LOGGER = LogManager.getLogger(PerformanceBenchmark.class);

    // 测试配置
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int BENCHMARK_ITERATIONS = 10000;
    private static final int BATCH_SIZE = 100;

    // 测试结果存储
    private final Map<String, BenchmarkResult> results = new LinkedHashMap<>();

    /**
     * 基准测试结果
     */
    public static class BenchmarkResult {
        public final String name;
        public final long totalTimeNs;
        public final long avgTimeNs;
        public final long minTimeNs;
        public final long maxTimeNs;
        public final double throughputOpsPerMs;
        public final long memoryUsedBytes;
        public final int iterations;

        public BenchmarkResult(String name, long totalTimeNs, long minTimeNs, long maxTimeMs,
                               long memoryUsedBytes, int iterations) {
            this.name = name;
            this.totalTimeNs = totalTimeNs;
            this.avgTimeNs = totalTimeNs / iterations;
            this.minTimeNs = minTimeNs;
            this.maxTimeNs = maxTimeMs;
            this.throughputOpsPerMs = iterations / (totalTimeNs / 1_000_000.0);
            this.memoryUsedBytes = memoryUsedBytes;
            this.iterations = iterations;
        }

        @Override
        public String toString() {
            return String.format(
                "Benchmark[%s]: avg=%.3fμs, min=%.3fμs, max=%.3fμs, throughput=%.2f ops/ms, memory=%d bytes",
                name, avgTimeNs / 1000.0, minTimeNs / 1000.0, maxTimeNs / 1000.0,
                throughputOpsPerMs, memoryUsedBytes
            );
        }
    }

    /**
     * 运行基准测试
     */
    public BenchmarkResult benchmark(String name, Runnable task, int iterations) {
        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            task.run();
        }

        // 强制GC
        System.gc();
        System.gc();

        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;

        // 执行测试
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            task.run();
            long elapsed = System.nanoTime() - start;

            totalTime += elapsed;
            minTime = Math.min(minTime, elapsed);
            maxTime = Math.max(maxTime, elapsed);
        }

        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = Math.max(0, memoryAfter - memoryBefore);

        BenchmarkResult result = new BenchmarkResult(name, totalTime, minTime, maxTime, memoryUsed, iterations);
        results.put(name, result);

        LOGGER.info(result.toString());
        return result;
    }

    /**
     * 批量测试
     */
    public BenchmarkResult benchmarkBatch(String name, Consumer<List<ItemStack>> batchTask,
                                          Supplier<List<ItemStack>> dataSupplier, int batches) {
        // 预热
        for (int i = 0; i < 100; i++) {
            batchTask.accept(dataSupplier.get());
        }

        System.gc();

        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.nanoTime();

        for (int i = 0; i < batches; i++) {
            batchTask.accept(dataSupplier.get());
        }

        long totalTime = System.nanoTime() - start;
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = Math.max(0, memoryAfter - memoryBefore);

        int totalOps = batches * BATCH_SIZE;
        BenchmarkResult result = new BenchmarkResult(name, totalTime, 0, 0, memoryUsed, totalOps);
        results.put(name, result);

        LOGGER.info(result.toString());
        return result;
    }

    /**
     * 生成对比报告
     */
    public void generateComparisonReport(String baselineName, String optimizedName) {
        BenchmarkResult baseline = results.get(baselineName);
        BenchmarkResult optimized = results.get(optimizedName);

        if (baseline == null || optimized == null) {
            LOGGER.warn("无法生成对比报告：缺少测试结果");
            return;
        }

        double speedup = (double) baseline.avgTimeNs / optimized.avgTimeNs;
        double memoryReduction = (double) baseline.memoryUsedBytes / Math.max(1, optimized.memoryUsedBytes);

        LOGGER.info("========================================");
        LOGGER.info("性能对比报告: {} vs {}", baselineName, optimizedName);
        LOGGER.info("========================================");
        LOGGER.info("平均执行时间: {} → {} ({}x {})",
            formatTime(baseline.avgTimeNs),
            formatTime(optimized.avgTimeNs),
            String.format("%.2f", speedup),
            speedup > 1 ? "加速" : "减速"
        );
        LOGGER.info("内存使用: {} → {} ({}x {})",
            formatBytes(baseline.memoryUsedBytes),
            formatBytes(optimized.memoryUsedBytes),
            String.format("%.2f", memoryReduction),
            memoryReduction > 1 ? "减少" : "增加"
        );
        LOGGER.info("吞吐量: {} → {} ops/ms",
            String.format("%.2f", baseline.throughputOpsPerMs),
            String.format("%.2f", optimized.throughputOpsPerMs)
        );
        LOGGER.info("========================================");
    }

    private String formatTime(long nanos) {
        if (nanos < 1000) return nanos + "ns";
        if (nanos < 1_000_000) return String.format("%.2fμs", nanos / 1000.0);
        return String.format("%.2fms", nanos / 1_000_000.0);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.2fKB", bytes / 1024.0);
        return String.format("%.2fMB", bytes / (1024.0 * 1024.0));
    }

    /**
     * 获取所有结果
     */
    public Map<String, BenchmarkResult> getResults() {
        return Collections.unmodifiableMap(results);
    }

    /**
     * 清空结果
     */
    public void clear() {
        results.clear();
    }
}
