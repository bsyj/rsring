/**
 * 模组兼容包 - 提供与其他模组的兼容支持
 * 
 * <p>此包包含所有模组兼容相关的类和接口，通过统一的管理器模式
 * 简化多模组兼容的实现和维护。</p>
 * 
 * <p>支持的模组：</p>
 * <ul>
 *   <li><b>Useful-Backpacks</b> - 物品栏背包系统兼容</li>
 *   <li><b>WearableBackpacks</b> - 可穿戴背包系统兼容</li>
 * </ul>
 * 
 * <p>架构设计：</p>
 * <ul>
 *   <li>{@link com.rsring.compat.CompatManager} - 统一兼容管理器，提供简化API</li>
 *   <li>{@link com.rsring.compat.usefulbackpacks.UsefulBackpacksCompat} - Useful-Backpacks兼容实现</li>
 *   <li>{@link com.rsring.compat.wearablebackpacks.WearableBackpacksCompat} - WearableBackpacks兼容实现</li>
 * </ul>
 * 
 * <p>使用方式：</p>
 * <pre>
 * // 初始化所有兼容模块
 * CompatManager.initialize();
 * 
 * // 检查是否有背包模组可用
 * if (CompatManager.isAnyBackpackModAvailable()) {
 *     // 存入物品到背包
 *     int inserted = CompatManager.absorbToAnyBackpack(player, itemStack, capability, preferBackpacks);
 *     
 *     // 从背包销毁物品
 *     int destroyed = CompatManager.destroyFromAnyBackpack(player, itemStack, capability);
 * }
 * </pre>
 * 
 * @author RsRing Team
 * @since 1.3.3
 */
package com.rsring.compat;
