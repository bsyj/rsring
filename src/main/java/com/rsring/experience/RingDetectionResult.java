package com.rsring.experience;

import net.minecraft.item.ItemStack;
import java.util.*;

/**
 * 跨不同物品栏位置的戒指检测结果。
 * 为戒指检测系统提供关于检测到的戒指的综合信息。
 *
 * 支持需求 4.1、4.2、4.3、4.5 的综合戒指检测和选择。
 */
public class RingDetectionResult {

    /**
     * 可以找到戒指的物品栏位置枚举。
     */
    public enum InventoryLocation {
        MAIN_HAND("主手", 1),
        OFF_HAND("副手", 2),
        BAUBLES_RING("饰品栏戒指槽", 3),
        BAUBLES_OTHER("饰品栏其他槽", 4),
        HOTBAR("快捷栏", 5),
        PLAYER_INVENTORY("玩家物品栏", 6);

        private final String displayName;
        private final int priority; // 数字越小 = 优先级越高

        InventoryLocation(String displayName, int priority) {
            this.displayName = displayName;
            this.priority = priority;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getPriority() {
            return priority;
        }
    }

    private final List<ItemStack> foundRings;                    // All detected rings
    private final Map<String, ItemStack> ringsByType;           // Rings categorized by type
    private final ItemStack primaryRing;                        // Selected primary ring
    private final InventoryLocation primaryLocation;            // Where primary ring was found
    private final Map<InventoryLocation, List<ItemStack>> ringsByLocation; // 按位置分类的戒指
    private final long detectionTimestamp;                      // 检测执行的时间

    /**
     * 创建一个新的戒指检测结果。
     */
    public RingDetectionResult(List<ItemStack> foundRings,
                              Map<InventoryLocation, List<ItemStack>> ringsByLocation) {
        this.foundRings = new ArrayList<>(foundRings != null ? foundRings : Collections.emptyList());
        this.ringsByLocation = new EnumMap<>(InventoryLocation.class);

        // Copy the rings by location map
        if (ringsByLocation != null) {
            for (Map.Entry<InventoryLocation, List<ItemStack>> entry : ringsByLocation.entrySet()) {
                this.ringsByLocation.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }

        // Build rings by type map
        this.ringsByType = buildRingsByType();

        // Determine primary ring based on priority
        this.primaryRing = determinePrimaryRing();
        this.primaryLocation = determinePrimaryLocation();

        this.detectionTimestamp = System.currentTimeMillis();
    }

    /**
     * 创建一个空的检测结果。
     */
    public static RingDetectionResult empty() {
        return new RingDetectionResult(Collections.emptyList(), Collections.emptyMap());
    }

    // Getters

    public List<ItemStack> getFoundRings() {
        return new ArrayList<>(foundRings);
    }

    public Map<String, ItemStack> getRingsByType() {
        return new HashMap<>(ringsByType);
    }

    public ItemStack getPrimaryRing() {
        return primaryRing;
    }

    public InventoryLocation getPrimaryLocation() {
        return primaryLocation;
    }

    public Map<InventoryLocation, List<ItemStack>> getRingsByLocation() {
        Map<InventoryLocation, List<ItemStack>> result = new EnumMap<>(InventoryLocation.class);
        for (Map.Entry<InventoryLocation, List<ItemStack>> entry : ringsByLocation.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    public long getDetectionTimestamp() {
        return detectionTimestamp;
    }

    // Utility methods

    /**
     * 检查是否找到了任何戒指。
     */
    public boolean hasRings() {
        return !foundRings.isEmpty();
    }

    /**
     * 获取找到的戒指数量。
     */
    public int getRingCount() {
        return foundRings.size();
    }

    /**
     * 获取特定位置的戒指数量。
     */
    public int getRingCount(InventoryLocation location) {
        List<ItemStack> rings = ringsByLocation.get(location);
        return rings != null ? rings.size() : 0;
    }

    /**
     * 检查是否在特定位置找到了戒指。
     */
    public boolean hasRingsInLocation(InventoryLocation location) {
        return getRingCount(location) > 0;
    }

    /**
     * 获取来自特定位置的戒指。
     */
    public List<ItemStack> getRingsFromLocation(InventoryLocation location) {
        List<ItemStack> rings = ringsByLocation.get(location);
        return rings != null ? new ArrayList<>(rings) : Collections.emptyList();
    }

    /**
     * 检查是否找到了特定类型的戒指。
     */
    public boolean hasRingType(String ringType) {
        return ringsByType.containsKey(ringType);
    }

    /**
     * 获取特定类型的戒指。
     */
    public ItemStack getRingOfType(String ringType) {
        return ringsByType.get(ringType);
    }

    /**
     * 获取找到的所有唯一戒指类型。
     */
    public Set<String> getRingTypes() {
        return new HashSet<>(ringsByType.keySet());
    }

    /**
     * 获取按位置分类的戒指摘要，用于显示目的。
     */
    public Map<String, Integer> getLocationSummary() {
        Map<String, Integer> summary = new LinkedHashMap<>();

        // 按优先级排序位置
        List<InventoryLocation> sortedLocations = new ArrayList<>(ringsByLocation.keySet());
        sortedLocations.sort(Comparator.comparingInt(InventoryLocation::getPriority));

        for (InventoryLocation location : sortedLocations) {
            int count = getRingCount(location);
            if (count > 0) {
                summary.put(location.getDisplayName(), count);
            }
        }

        return summary;
    }

    /**
     * 获取按类型分类的戒指摘要，用于显示目的。
     */
    public Map<String, Integer> getTypeSummary() {
        Map<String, Integer> summary = new HashMap<>();

        for (String type : ringsByType.keySet()) {
            // 计算我们有多少个这种类型的戒指
            int count = 0;
            for (ItemStack ring : foundRings) {
                if (getRingType(ring).equals(type)) {
                    count++;
                }
            }
            summary.put(type, count);
        }

        return summary;
    }

    // Private helper methods

    /**
     * 构建按类型分类的戒指映射。
     */
    private Map<String, ItemStack> buildRingsByType() {
        Map<String, ItemStack> byType = new HashMap<>();

        for (ItemStack ring : foundRings) {
            String type = getRingType(ring);
            if (!byType.containsKey(type)) {
                byType.put(type, ring);
            }
        }

        return byType;
    }

    /**
     * 根据位置优先级确定主要戒指。
     */
    private ItemStack determinePrimaryRing() {
        if (foundRings.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 找到具有最高优先级位置的戒指
        ItemStack primary = ItemStack.EMPTY;
        int highestPriority = Integer.MAX_VALUE;

        for (Map.Entry<InventoryLocation, List<ItemStack>> entry : ringsByLocation.entrySet()) {
            InventoryLocation location = entry.getKey();
            List<ItemStack> rings = entry.getValue();

            if (!rings.isEmpty() && location.getPriority() < highestPriority) {
                highestPriority = location.getPriority();
                primary = rings.get(0); // 从最高优先级位置获取第一个戒指
            }
        }

        return primary;
    }

    /**
     * 确定主要戒指的位置。
     */
    private InventoryLocation determinePrimaryLocation() {
        if (primaryRing.isEmpty()) {
            return null;
        }

        // 找到包含主要戒指的位置
        for (Map.Entry<InventoryLocation, List<ItemStack>> entry : ringsByLocation.entrySet()) {
            if (entry.getValue().contains(primaryRing)) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * 获取戒指的类型名称。
     */
    private String getRingType(ItemStack ring) {
        if (ring.isEmpty()) {
            return "Unknown";
        }

        String className = ring.getItem().getClass().getSimpleName();

        // 将类名转换为可读的类型名称
        if (className.startsWith("Item")) {
            className = className.substring(4); // 移除 "Item" 前缀
        }

        return className;
    }

    @Override
    public String toString() {
        return String.format("RingDetectionResult{rings=%d, types=%s, primary=%s, location=%s, timestamp=%d}",
                           foundRings.size(), ringsByType.keySet(),
                           primaryRing.isEmpty() ? "none" : getRingType(primaryRing),
                           primaryLocation != null ? primaryLocation.getDisplayName() : "none",
                           detectionTimestamp);
    }

    /**
     * 用于创建 RingDetectionResult 实例的构建器类。
     */
    public static class Builder {
        private final List<ItemStack> foundRings = new ArrayList<>();
        private final Map<InventoryLocation, List<ItemStack>> ringsByLocation = new EnumMap<>(InventoryLocation.class);

        public Builder addRing(ItemStack ring, InventoryLocation location) {
            if (ring != null && !ring.isEmpty()) {
                foundRings.add(ring);
                ringsByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(ring);
            }
            return this;
        }

        public Builder addRings(List<ItemStack> rings, InventoryLocation location) {
            if (rings != null) {
                for (ItemStack ring : rings) {
                    addRing(ring, location);
                }
            }
            return this;
        }

        public RingDetectionResult build() {
            return new RingDetectionResult(foundRings, ringsByLocation);
        }
    }
}
