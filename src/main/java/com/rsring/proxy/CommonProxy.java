package com.rsring.proxy;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

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
        // 服务端默认实现，什么都不做
    }
}