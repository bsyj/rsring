package com.rsring.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 *支持 IInventory 和 IItemHandler 实现的最小 Baubles 访问助手。
 *使用反射来避免在类加载时对 Baubles 类的硬依赖。
 */
public final class BaublesHelper {

    private static final Logger LOGGER = LogManager.getLogger(BaublesHelper.class);

    private BaublesHelper() {}

    public static boolean isBaublesLoaded() {
        return Loader.isModLoaded("baubles");
    }

    public static Object getBaublesHandler(EntityPlayer player) {
        if (player == null || !isBaublesLoaded()) {
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            return apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);
        } catch (Throwable t) {
            LOGGER.debug("Failed to access Baubles handler: {}", t.toString());
            return null;
        }
    }

    public static int getSlots(Object handler) {
        if (handler == null) {
            return 0;
        }
        try {
            if (handler instanceof IInventory) {
                return ((IInventory) handler).getSizeInventory();
            }
            if (handler instanceof IItemHandler) {
                return ((IItemHandler) handler).getSlots();
            }
            Method sizeMethod = null;
            try {
                sizeMethod = handler.getClass().getMethod("getSizeInventory");
            } catch (NoSuchMethodException ignored) {
                try {
                    sizeMethod = handler.getClass().getMethod("getSlots");
                } catch (NoSuchMethodException ignored2) {
                    // ignore
                }
            }
            if (sizeMethod != null) {
                Object result = sizeMethod.invoke(handler);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to read Baubles slot count: {}", t.toString());
        }
        return 0;
    }

    public static ItemStack getStackInSlot(Object handler, int slot) {
        if (handler == null || slot < 0) {
            return ItemStack.EMPTY;
        }
        try {
            if (handler instanceof IInventory) {
                return ((IInventory) handler).getStackInSlot(slot);
            }
            if (handler instanceof IItemHandler) {
                return ((IItemHandler) handler).getStackInSlot(slot);
            }
            Method method = handler.getClass().getMethod("getStackInSlot", int.class);
            Object result = method.invoke(handler, slot);
            if (result instanceof ItemStack) {
                return (ItemStack) result;
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to read Baubles slot {}: {}", slot, t.toString());
        }
        return ItemStack.EMPTY;
    }

    public static boolean setStackInSlot(Object handler, int slot, ItemStack stack) {
        if (handler == null || slot < 0) {
            return false;
        }
        try {
            if (handler instanceof IInventory) {
                IInventory inv = (IInventory) handler;
                inv.setInventorySlotContents(slot, stack);
                inv.markDirty();
                return true;
            }
            if (handler instanceof IItemHandlerModifiable) {
                ((IItemHandlerModifiable) handler).setStackInSlot(slot, stack);
                return true;
            }
            Method method = null;
            try {
                method = handler.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
            } catch (NoSuchMethodException ignored) {
                try {
                    method = handler.getClass().getMethod("setInventorySlotContents", int.class, ItemStack.class);
                } catch (NoSuchMethodException ignored2) {
                    // ignore
                }
            }
            if (method != null) {
                method.invoke(handler, slot, stack);
                return true;
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to write Baubles slot {}: {}", slot, t.toString());
        }
        return false;
    }
}
