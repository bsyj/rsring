package com.rsring.performance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能分析器 - 用于运行时性能监控
 * 轻量级实现，对生产环境影响最小
 */
public class PerformanceProfiler {
    private static final Logger LOGGER = LogManager.getLogger(PerformanceProfiler.class);
    private static final boolean ENABLED = Boolean.getBoolean("rsring.profiler.enabled");

    // 方法调用统计
    private final Map<String, MethodStats> methodStats = new ConcurrentHashMap<>();

    // 当前正在执行的调用栈
    private final ThreadLocal<Deque<TimedCall>> callStack = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * 方法统计信息
     */
    public static class MethodStats {
        public final String methodName;
        public final AtomicLong callCount = new AtomicLong(0);
        public final AtomicLong totalTimeNs = new AtomicLong(0);
        public final AtomicLong minTimeNs = new AtomicLong(Long.MAX_VALUE);
        public final AtomicLong maxTimeNs = new AtomicLong(0);
        public volatile long lastResetTime = System.currentTimeMillis();

        public MethodStats(String methodName) {
            this.methodName = methodName;
        }

        public void recordCall(long elapsedNs) {
            callCount.incrementAndGet();
            totalTimeNs.addAndGet(elapsedNs);

            // 使用 CAS 更新 min/max
            long currentMin;
            do {
                currentMin = minTimeNs.get();
                if (elapsedNs >= currentMin) break;
            } while (!minTimeNs.compareAndSet(currentMin, elapsedNs));

            long currentMax;
            do {
                currentMax = maxTimeNs.get();
                if (elapsedNs <= currentMax) break;
            } while (!maxTimeNs.compareAndSet(currentMax, elapsedNs));
        }

        public double getAvgTimeMs() {
            long count = callCount.get();
            return count > 0 ? (totalTimeNs.get() / count) / 1_000_000.0 : 0;
        }

        public long getCallCount() {
            return callCount.get();
        }

        public void reset() {
            callCount.set(0);
            totalTimeNs.set(0);
            minTimeNs.set(Long.MAX_VALUE);
            maxTimeNs.set(0);
            lastResetTime = System.currentTimeMillis();
        }
    }

    /**
     * 带计时的调用
     */
    private static class TimedCall {
        final String methodName;
        final long startTime;

        TimedCall(String methodName) {
            this.methodName = methodName;
            this.startTime = System.nanoTime();
        }
    }

    private static final PerformanceProfiler INSTANCE = new PerformanceProfiler();

    public static PerformanceProfiler getInstance() {
        return INSTANCE;
    }

    private PerformanceProfiler() {}

    /**
     * 开始方法计时
     */
    public void start(String methodName) {
        if (!ENABLED) return;
        callStack.get().push(new TimedCall(methodName));
    }

    /**
     * 结束方法计时
     */
    public void end() {
        if (!ENABLED) return;

        Deque<TimedCall> stack = callStack.get();
        TimedCall call = stack.poll();
        if (call == null) return;

        long elapsed = System.nanoTime() - call.startTime;
        methodStats.computeIfAbsent(call.methodName, MethodStats::new)
                   .recordCall(elapsed);
    }

    /**
     * 执行并监控方法
     */
    public <T> T profile(String methodName, java.util.function.Supplier<T> supplier) {
        start(methodName);
        try {
            return supplier.get();
        } finally {
            end();
        }
    }

    /**
     * 执行并监控无返回值方法
     */
    public void profile(String methodName, Runnable runnable) {
        start(methodName);
        try {
            runnable.run();
        } finally {
            end();
        }
    }

    /**
     * 获取方法统计
     */
    public MethodStats getStats(String methodName) {
        return methodStats.get(methodName);
    }

    /**
     * 获取所有统计
     */
    public Map<String, MethodStats> getAllStats() {
        return new HashMap<>(methodStats);
    }

    /**
     * 打印性能报告
     */
    public void printReport() {
        if (methodStats.isEmpty()) {
            LOGGER.info("没有性能数据");
            return;
        }

        LOGGER.info("========== 性能分析报告 ==========");
        LOGGER.info(String.format("%-50s %10s %12s %12s %12s", 
            "方法名", "调用次数", "平均(ms)", "最小(ms)", "最大(ms)"));
        LOGGER.info("--------------------------------------------------------------------------------");

        // 按总时间排序
        List<MethodStats> sorted = new ArrayList<>(methodStats.values());
        sorted.sort((a, b) -> Long.compare(b.totalTimeNs.get(), a.totalTimeNs.get()));

        for (MethodStats stats : sorted) {
            LOGGER.info(String.format("%-50s %10d %12.3f %12.3f %12.3f",
                stats.methodName,
                stats.getCallCount(),
                stats.getAvgTimeMs(),
                stats.minTimeNs.get() == Long.MAX_VALUE ? 0 : stats.minTimeNs.get() / 1_000_000.0,
                stats.maxTimeNs.get() / 1_000_000.0
            ));
        }
        LOGGER.info("==================================");
    }

    /**
     * 打印热点方法（最耗时的方法）
     */
    public void printHotspots(int topN) {
        if (methodStats.isEmpty()) return;

        List<MethodStats> sorted = new ArrayList<>(methodStats.values());
        sorted.sort((a, b) -> Long.compare(b.totalTimeNs.get(), a.totalTimeNs.get()));

        LOGGER.info("========== 性能热点 (Top {}) ==========", topN);
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            MethodStats stats = sorted.get(i);
            LOGGER.info("{}. {} - {}次调用, 平均{:.3f}ms",
                i + 1, stats.methodName, stats.getCallCount(), stats.getAvgTimeMs());
        }
        LOGGER.info("======================================");
    }

    /**
     * 重置统计
     */
    public void reset() {
        methodStats.clear();
        LOGGER.info("性能统计已重置");
    }

    /**
     * 重置特定方法的统计
     */
    public void reset(String methodName) {
        MethodStats stats = methodStats.get(methodName);
        if (stats != null) {
            stats.reset();
        }
    }

    /**
     * 检查是否启用
     */
    public static boolean isEnabled() {
        return ENABLED;
    }
}
