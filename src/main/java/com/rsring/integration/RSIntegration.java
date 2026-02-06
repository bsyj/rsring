package com.rsring.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Refined Storage Integration
 * Provides item transfer functionality with RS networks
 * 
 * @version 1.4
 */
public class RSIntegration {
    private static final Logger LOGGER = LogManager.getLogger(RSIntegration.class);
    
    private static Class<?> apiClass;
    private static Class<?> actionClass;
    private static Class<?> networkClass;
    private static Method insertItemMethod;
    private static boolean isRSLoaded = false;
    private static boolean isInitialized = false;
    
    /**
     * Initialize RS API via reflection
     */
    public static void initialize() {
        if (isInitialized) return;
        
        try {
            apiClass = Class.forName("com.raoulvdberge.refinedstorage.apiimpl.API");
            actionClass = Class.forName("com.raoulvdberge.refinedstorage.api.util.Action");
            networkClass = Class.forName("com.raoulvdberge.refinedstorage.api.network.INetwork");
            
            // Try int parameter first, then long
            try {
                insertItemMethod = networkClass.getMethod("insertItem", ItemStack.class, int.class, actionClass);
            } catch (NoSuchMethodException e) {
                insertItemMethod = networkClass.getMethod("insertItem", ItemStack.class, long.class, actionClass);
            }
            
            isRSLoaded = true;
            LOGGER.info("RS integration initialized");
        } catch (ClassNotFoundException e) {
            LOGGER.info("RS not found, integration disabled");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize RS integration", e);
        }
        
        isInitialized = true;
    }
    
    /**
     * Check if RS is loaded
     */
    public static boolean isRSLoaded() {
        if (!isInitialized) initialize();
        return isRSLoaded;
    }
    
    /**
     * Check if block is RS controller
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
     * Get RS network from controller position
     */
    @Nullable
    public static Object getNetwork(World world, BlockPos pos) {
        if (!isRSLoaded() || world == null || pos == null) return null;
        
        try {
            TileEntity te = world.getTileEntity(pos);
            if (te == null) return null;
            
            // Try to get network directly from tile entity
            for (Method m : te.getClass().getMethods()) {
                if ("getNetwork".equals(m.getName()) && m.getParameterTypes().length == 0) {
                    Object network = m.invoke(te);
                    if (network != null) return network;
                }
            }
            
            // Try via API
            Object api = apiClass.getMethod("instance").invoke(null);
            Object nodeManager = api.getClass().getMethod("getNetworkNodeManager", World.class).invoke(api, world);
            if (nodeManager != null) {
                Object node = nodeManager.getClass().getMethod("getNode", BlockPos.class).invoke(nodeManager, pos);
                if (node != null) {
                    Object network = node.getClass().getMethod("getNetwork").invoke(node);
                    if (network != null) return network;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get RS network: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Check if network can accept items
     * Simply check if network object exists and has energy
     */
    public static boolean canInsertItems(Object network) {
        if (network == null) return false;
        
        try {
            // Check if network has energy - try different method names
            try {
                Method getEnergy = network.getClass().getMethod("getEnergy");
                int energy = (int) getEnergy.invoke(network);
                if (energy <= 0) return false;
            } catch (NoSuchMethodException e) {
                // Try alternative method
                try {
                    Method getStored = network.getClass().getMethod("getStored");
                    long energy = (long) getStored.invoke(network);
                    if (energy <= 0) return false;
                } catch (NoSuchMethodException e2) {
                    // No energy check available, assume it works
                }
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.debug("Failed to check network energy: {}", e.getMessage());
            return true; // Assume it works if we can't check
        }
    }
    
    /**
     * Insert items into RS network
     * @return number of items successfully inserted
     */
    public static int insertItem(World world, BlockPos pos, ItemStack stack) {
        if (!isRSLoaded() || world == null || pos == null || stack.isEmpty()) return 0;
        
        try {
            Object network = getNetwork(world, pos);
            if (network == null) {
                LOGGER.debug("No RS network at {}", pos);
                return 0;
            }
            
            // Execute insert
            Object perform = java.lang.Enum.valueOf((Class<? extends java.lang.Enum>) actionClass, "PERFORM");
            Object result = insertItemMethod.invoke(network, stack.copy(), stack.getCount(), perform);
            
            if (result == null) {
                // All items inserted
                return stack.getCount();
            }
            
            if (result instanceof ItemStack) {
                ItemStack remainder = (ItemStack) result;
                return Math.max(0, stack.getCount() - remainder.getCount());
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to insert into RS: {}", e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Get network status for display
     */
    public static String getNetworkStatus(World world, BlockPos pos) {
        if (!isRSLoaded()) return "RS未安装";
        
        Object network = getNetwork(world, pos);
        if (network == null) return "未连接";
        
        return "已连接";
    }
}
