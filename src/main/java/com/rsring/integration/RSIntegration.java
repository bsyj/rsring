package com.rsring.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Refined Storage 集成类
 * 提供与RS网络的物品传输功能
 * 
 * 支持的RS版本:
 * - RS 1.6.x (1.12.2)
 * 
 * @version 1.4
 */
@Optional.Interface(iface = "com.raoulvdberge.refinedstorage.api.network.INetwork", modid = "refinedstorage")
public class RSIntegration {
    
    private static final Logger LOGGER = LogManager.getLogger(RSIntegration.class);
    
    // RS API类缓存
    private static Class<?> apiClass;
    private static Class<?> actionClass;
    private static Class<?> networkClass;
    private static Class<?> nodeManagerClass;
    private static Class<?> networkNodeClass;
    private static Class<?> capabilityClass;
    
    // 方法缓存
    private static Method apiInstanceMethod;
    private static Method getNodeManagerMethod;
    private static Method getNodeMethod;
    private static Method getNetworkFromNodeMethod;
    private static Method insertItemMethod;
    private static Method getNetworkFromTileMethod;
    
    // 状态
    private static boolean isRSLoaded = false;
    private static boolean isInitialized = false;
    
    /**
     * 初始化RS API反射
     */
    public static void initialize() {
        if (isInitialized) return;
        
        try {
            // 尝试加载RS API类
            apiClass = Class.forName("com.raoulvdberge.refinedstorage.apiimpl.API");
            actionClass = Class.forName("com.raoulvdberge.refinedstorage.api.util.Action");
            networkClass = Class.forName("com.raoulvdberge.refinedstorage.api.network.INetwork");
            nodeManagerClass = Class.forName("com.raoulvdberge.refinedstorage.api.network.INetworkNodeManager");
            networkNodeClass = Class.forName("com.raoulvdberge.refinedstorage.api.network.node.INetworkNode");
            capabilityClass = Class.forName("com.raoulvdberge.refinedstorage.capability.CapabilityNetworkNodeProxy");
            
            // 缓存方法
            apiInstanceMethod = apiClass.getMethod("instance");
            getNodeManagerMethod = apiClass.getMethod("getNetworkNodeManager", World.class);
            getNodeMethod = nodeManagerClass.getMethod("getNode", BlockPos.class);
            getNetworkFromNodeMethod = networkNodeClass.getMethod("getNetwork");
            
            // 尝试获取insertItem方法 (支持int和long参数)
            try {
                insertItemMethod = networkClass.getMethod("insertItem", ItemStack.class, int.class, actionClass);
            } catch (NoSuchMethodException e) {
                insertItemMethod = networkClass.getMethod("insertItem", ItemStack.class, long.class, actionClass);
            }
            
            isRSLoaded = true;
            isInitialized = true;
            LOGGER.info("Refined Storage integration initialized successfully");
            
        } catch (ClassNotFoundException e) {
            LOGGER.info("Refined Storage not found, RS integration disabled");
            isRSLoaded = false;
            isInitialized = true;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize RS integration", e);
            isRSLoaded = false;
            isInitialized = true;
        }
    }
    
    /**
     * 检查RS是否加载
     */
    public static boolean isRSLoaded() {
        if (!isInitialized) {
            initialize();
        }
        return isRSLoaded;
    }
    
    /**
     * 检查方块是否是RS控制器
     */
    public static boolean isRSController(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        
        net.minecraft.util.ResourceLocation regName = world.getBlockState(pos).getBlock().getRegistryName();
        if (regName == null) return false;
        
        String blockName = regName.toString().toLowerCase();
        return blockName.equals("refinedstorage:controller") ||
               blockName.equals("refinedstorage:creative_controller");
    }
    
    /**
     * 检查方块是否是RS网络节点(可以连接网络的方块)
     */
    public static boolean isRSNetworkBlock(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        
        net.minecraft.util.ResourceLocation regName = world.getBlockState(pos).getBlock().getRegistryName();
        if (regName == null) return false;
        
        String blockName = regName.toString().toLowerCase();
        return blockName.startsWith("refinedstorage:") && 
               (blockName.contains("controller") || 
                blockName.contains("grid") || 
                blockName.contains("disk_drive") ||
                blockName.contains("crafter"));
    }
    
    /**
     * 获取RS网络对象
     * @return 网络对象，如果失败返回null
     */
    @Nullable
    public static Object getNetwork(World world, BlockPos pos) {
        if (!isRSLoaded() || world == null || pos == null) return null;
        
        try {
            // 方法1: 通过NodeManager获取
            Object network = getNetworkFromNodeManager(world, pos);
            if (network != null) return network;
            
            // 方法2: 通过TileEntity获取
            network = getNetworkFromTile(world, pos);
            if (network != null) return network;
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get RS network", e);
        }
        
        return null;
    }
    
    /**
     * 检查网络是否有效(在线且有存储)
     */
    public static boolean isNetworkValid(Object network) {
        if (network == null) return false;
        
        try {
            // 检查网络是否有能量
            Method getEnergyUsage = network.getClass().getMethod("getEnergyUsage");
            Object energyUsage = getEnergyUsage.invoke(network);
            
            // 检查网络状态
            Method canRun = network.getClass().getMethod("canRun");
            return (boolean) canRun.invoke(network);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to check network validity", e);
            return false;
        }
    }
    
    /**
     * 获取网络存储信息
     * @return [已使用空间, 总空间]，如果失败返回null
     */
    @Nullable
    public static long[] getNetworkStorageInfo(Object network) {
        if (network == null) return null;
        
        try {
            // 尝试获取存储缓存
            Method getItemStorageCache = network.getClass().getMethod("getItemStorageCache");
            Object storageCache = getItemStorageCache.invoke(network);
            
            if (storageCache != null) {
                Method getStored = storageCache.getClass().getMethod("getStored");
                Method getCapacity = storageCache.getClass().getMethod("getCapacity");
                
                long stored = (long) getStored.invoke(storageCache);
                long capacity = (long) getCapacity.invoke(storageCache);
                
                return new long[]{stored, capacity};
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get storage info", e);
        }
        
        return null;
    }
    
    /**
     * 插入物品到RS网络
     * @param world 世界
     * @param pos 网络位置
     * @param stack 要插入的物品
     * @return 成功插入的数量
     */
    public static int insertItem(World world, BlockPos pos, ItemStack stack) {
        if (!isRSLoaded() || world == null || pos == null || stack.isEmpty()) return 0;
        
        try {
            Object network = getNetwork(world, pos);
            if (network == null) {
                LOGGER.debug("No RS network found at {}", pos);
                return 0;
            }
            
            // 检查网络是否在线
            if (!isNetworkValid(network)) {
                LOGGER.debug("RS network is not valid (offline or no power)");
                return 0;
            }
            
            // 执行插入
            Object perform = java.lang.Enum.valueOf((Class<? extends java.lang.Enum>) actionClass, "PERFORM");
            Object remainderObj = insertItemMethod.invoke(network, stack.copy(), stack.getCount(), perform);
            
            if (remainderObj == null) {
                return stack.getCount();
            }
            
            if (remainderObj instanceof ItemStack) {
                ItemStack remainder = (ItemStack) remainderObj;
                return Math.max(0, stack.getCount() - remainder.getCount());
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to insert item into RS network", e);
        }
        
        return 0;
    }
    
    /**
     * 获取网络状态信息(用于显示)
     */
    public static String getNetworkStatus(World world, BlockPos pos) {
        if (!isRSLoaded()) return "RS未安装";
        
        Object network = getNetwork(world, pos);
        if (network == null) return "无网络连接";
        
        if (!isNetworkValid(network)) return "网络离线";
        
        long[] storage = getNetworkStorageInfo(network);
        if (storage != null) {
            return String.format("在线 | 存储: %d/%d", storage[0], storage[1]);
        }
        
        return "在线";
    }
    
    // ========== 私有辅助方法 ==========
    
    private static Object getNetworkFromNodeManager(World world, BlockPos pos) throws Exception {
        Object api = apiInstanceMethod.invoke(null);
        Object nodeManager = getNodeManagerMethod.invoke(api, world);
        if (nodeManager == null) return null;
        
        Object node = getNodeMethod.invoke(nodeManager, pos);
        if (node == null) return null;
        
        return getNetworkFromNodeMethod.invoke(node);
    }
    
    private static Object getNetworkFromTile(World world, BlockPos pos) {
        try {
            TileEntity te = world.getTileEntity(pos);
            if (te == null) return null;
            
            // 尝试通过Capability获取
            try {
                java.lang.reflect.Field capField = capabilityClass.getField("NETWORK_NODE_PROXY_CAPABILITY");
                Object cap = capField.get(null);
                if (cap != null) {
                    java.lang.reflect.Method getCap = te.getClass().getMethod("getCapability",
                        net.minecraftforge.common.capabilities.Capability.class, net.minecraft.util.EnumFacing.class);
                    Object proxy = getCap.invoke(te, cap, null);
                    if (proxy != null) {
                        java.lang.reflect.Method getNode = proxy.getClass().getMethod("getNode");
                        Object node = getNode.invoke(proxy);
                        if (node != null) {
                            return getNetworkFromNodeMethod.invoke(node);
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略，尝试下一个方法
            }
            
            // 直接调用getNetwork()
            for (java.lang.reflect.Method m : te.getClass().getMethods()) {
                if ("getNetwork".equals(m.getName()) && m.getParameterTypes().length == 0) {
                    Object net = m.invoke(te);
                    if (net != null) return net;
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get network from tile", e);
        }
        
        return null;
    }
}
