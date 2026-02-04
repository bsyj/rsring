package com.moremod.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.moremod.rsring.RsRingMod;

/**
 * GUI Factory for RsRing Mod configuration
 * 
 * This class provides the configuration GUI that appears when clicking
 * the "Config" button in the Mod List screen.
 */
@SideOnly(Side.CLIENT)
public class GuiFactory extends DefaultGuiFactory {
    
    public GuiFactory() {
        super(RsRingMod.MODID, "RsRing Mod Configuration");
    }
    
    @Override
    public boolean hasConfigGui() {
        return true;
    }
    
    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiRsRingConfig(parentScreen);
    }
}
