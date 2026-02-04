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
            ItemLocationTracker tracker = ItemLocationTracker.findItem(player, ItemAbsorbRing.class);
            if (tracker == null) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "未找到物品吸收戒指！"));
                return;
            }

            ItemStack ringStack = tracker.getItem();
            IRsRingCapability capability = ringStack.getCapability(
                RsRingCapability.RS_RING_CAPABILITY, null);

            if (capability != null) {
                capability.setEnabled(!capability.isEnabled());
                RsRingCapability.syncCapabilityToStack(ringStack, capability);
                tracker.syncBack(player);

                String status = capability.isEnabled() ? "启用" : "禁用";
                String location = getLocationDisplayName(tracker.getLocationType());
                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "物品吸收戒指已" + status +
                    TextFormatting.GRAY + " (位置: " + location + ")"));
            }
        });
    }

    private String getLocationDisplayName(ItemLocationTracker.LocationType locationType) {
        switch (locationType) {
            case MAIN_HAND: return "主手";
            case OFF_HAND: return "副手";
            case BAUBLES: return "饰品栏";
            case PLAYER_INVENTORY: return "背包";
            default: return "未知";
        }
    }
}
