package com.moremod.client;

import com.moremod.rsring.RsRingMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * RsRing Mod 配置界面
 * Configuration GUI for RsRing Mod
 */
@SideOnly(Side.CLIENT)
public class GuiRsRingConfig extends GuiConfig {
    
    public GuiRsRingConfig(GuiScreen parentScreen) {
        super(parentScreen,
              RsRingMod.MODID,
              false,
              false,
              "RsRing Mod Configuration");
    }
}
