package com.rsring.proxy;

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.item.ItemAbsorbRing;
import com.rsring.util.ItemLocationTracker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CommonProxy {
    public void preInit() {}
    public void init() {}
    public void postInit() {}
    /** 仅客户端：打开经验泵 GUI */
    public void openExperiencePumpGui(ItemStack stack, EnumHand hand) {}
    /** 仅客户端：打开经验泵控制器 GUI */
    public void openExperiencePumpControllerGui(ItemStack stack, EnumHand hand) {}
    /** 仅客户端：打开物品吸收戒指 GUI */
    public void openAbsorbRingGui(ItemStack stack) {
        // 服务端默认实现，什么都不做
    }

    public void handleToggleRsRing(EntityPlayerMP player) {
        if (player == null) {
            return;
        }

        player.getServerWorld().addScheduledTask(() -> {
            // 获取所有戒指
            java.util.List<ItemStack> allRings = com.rsring.event.CommonEventHandler.findAllRings(player, com.rsring.item.ItemAbsorbRing.class);
            if (allRings.isEmpty()) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "未找到物品吸收戒指！"));
                return;
            }

            // 同时切换所有戒指的状态（每个戒指各自取反），并记录状态变化
            java.util.List<String> statusMessages = new java.util.ArrayList<>();
            
            for (int i = 0; i < allRings.size(); i++) {
                ItemStack ringStack = allRings.get(i);
                IRsRingCapability capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
                if (capability != null) {
                    boolean newState = !capability.isEnabled();
                    capability.setEnabled(newState);
                    RsRingCapability.syncCapabilityToStack(ringStack, capability);
                    
                    // 获取戒指名称和位置
                    String ringName = ringStack.getDisplayName();
                    String location = getRingLocationName(player, ringStack);
                    String status = newState ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭";
                    
                    statusMessages.add(ringName + " " + status + TextFormatting.GRAY + " (" + location + ")");
                }
            }

            // 发送每个戒指的状态消息
            for (String msg : statusMessages) {
                player.sendMessage(new TextComponentString(msg));
            }
        });
    }

    /**
     * 获取戒指所在位置的名称
     */
    private String getRingLocationName(EntityPlayerMP player, ItemStack ringStack) {
        // 主手
        if (ringStack == player.getHeldItemMainhand()) {
            return "主手";
        }
        // 副手
        if (ringStack == player.getHeldItemOffhand()) {
            return "副手";
        }
        // 饰品栏
        if (com.rsring.util.BaublesHelper.isBaublesLoaded()) {
            Object handler = com.rsring.util.BaublesHelper.getBaublesHandler(player);
            int size = com.rsring.util.BaublesHelper.getSlots(handler);
            for (int i = 0; i < size; i++) {
                if (com.rsring.util.BaublesHelper.getStackInSlot(handler, i) == ringStack) {
                    return "饰品栏";
                }
            }
        }
        // 背包
        return "背包";
    }
}
