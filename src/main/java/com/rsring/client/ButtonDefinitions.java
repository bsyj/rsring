package com.rsring.client;

import com.rsring.filter.FilterMode;
import net.minecraft.util.text.TextFormatting;

/**
 * GUI按钮定义类
 * 移植SophisticatedBackpacks的按钮系统设计
 */
public class ButtonDefinitions {

    /**
     * 过滤模式按钮状态 - 参考SophisticatedBackpacks的ContentsFilterType
     */
    public enum FilterButtonState {
        ALLOW(0, 0, TextFormatting.GREEN + "允许", "只吸收列表中的物品"),
        BLOCK(16, 0, TextFormatting.RED + "禁止", "吸收除列表外的所有物品"),
        MATCH_STORAGE(80, 16, TextFormatting.AQUA + "匹配背包", "根据物品所在容器进行过滤");

        private final int textureU;
        private final int textureV;
        private final String displayName;
        private final String description;

        FilterButtonState(int textureU, int textureV, String displayName, String description) {
            this.textureU = textureU;
            this.textureV = textureV;
            this.displayName = displayName;
            this.description = description;
        }

        public int getTextureU() {
            return textureU;
        }

        public int getTextureV() {
            return textureV;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 获取下一个过滤模式 - 参考SophisticatedBackpacks的next()方法
         */
        public FilterButtonState next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        private static final FilterButtonState[] VALUES = values();
    }

    /**
     * 匹配模式按钮状态(AND/OR)
     */
    public enum MatchButtonState {
        ALL(0, 32, TextFormatting.AQUA + "全部(AND)", "所有属性都满足才吸收"),
        ANY(16, 32, TextFormatting.YELLOW + "任意(OR)", "任一属性满足就吸收");

        private final int textureU;
        private final int textureV;
        private final String displayName;
        private final String description;

        MatchButtonState(int textureU, int textureV, String displayName, String description) {
            this.textureU = textureU;
            this.textureV = textureV;
            this.displayName = displayName;
            this.description = description;
        }

        public int getTextureU() {
            return textureU;
        }

        public int getTextureV() {
            return textureV;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 切换匹配模式
         */
        public MatchButtonState toggle() {
            return this == ALL ? ANY : ALL;
        }
    }

    /**
     * 从Capability获取对应的按钮状态
     * 参考SophisticatedBackpacks的过滤类型系统
     */
    public static FilterButtonState getFilterButtonState(boolean whitelistMode, FilterMode filterMode) {
        // 在RSRING中,暂时简化为黑白名单两种状态
        // 如果后续需要"匹配背包"功能,可以扩展
        return whitelistMode ? FilterButtonState.ALLOW : FilterButtonState.BLOCK;
    }

    /**
     * 将按钮状态转换为Capability设置
     * 参考SophisticatedBackpacks的过滤逻辑
     */
    public static boolean isWhitelistMode(FilterButtonState state) {
        switch (state) {
            case ALLOW:
            case MATCH_STORAGE:
                return true;
            case BLOCK:
            default:
                return false;
        }
    }

    /**
     * 获取过滤类型短名称 - 用于按钮标签
     */
    public static String getFilterTypeShortName(FilterMode mode) {
        switch (mode) {
            case ITEM: return "物品";
            case MOD: return "模组";
            case ATTRIBUTE: return "属性";
            default: return "";
        }
    }

    private ButtonDefinitions() {}
}

