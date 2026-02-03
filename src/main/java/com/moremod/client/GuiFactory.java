package com.moremod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.moremod.rsring.RsRingMod;

import java.util.Set;

/**
 * GUI Factory for RsRing Mod configuration
 * 
 * This class provides the configuration GUI that appears when clicking
 * the "Config" button in the Mod List screen.
 * 
 * 完全参考 Baubles 的 BaublesGuiFactory 实现
 * Based on Baubles' BaublesGuiFactory implementation
 */
@SideOnly(Side.CLIENT)
public class GuiFactory extends DefaultGuiFactory {
    
    public GuiFactory() {
        super(RsRingMod.MODID, "RsRing Mod Configuration");
    }
    
    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiRsRingConfig(parentScreen);
    }
}
