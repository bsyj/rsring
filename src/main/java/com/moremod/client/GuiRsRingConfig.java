package com.moremod.client;

import com.moremod.config.ConfigRegistry;
import com.moremod.rsring.RsRingMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;

public class GuiRsRingConfig extends GuiConfig {

    public GuiRsRingConfig(GuiScreen parentScreen) {
        super(parentScreen,
              new ConfigElement(ConfigRegistry.getConfig().getCategory(RsRingMod.MODID)).getChildElements(),
              RsRingMod.MODID,
              false,
              false,
              "RsRing Mod Configuration");
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        ConfigRegistry.syncAllConfig();
    }
}