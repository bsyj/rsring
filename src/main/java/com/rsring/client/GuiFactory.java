package com.rsring.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.rsring.rsring.RsRingMod;

/**
 *用于 RsRing Mod 配置的 GUI 工厂
 *
 *该类提供点击时出现的配置GUI
 *Mod列表屏幕中的“配置”按钮。
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
