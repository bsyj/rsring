package com.rsring.client;

import com.rsring.config.ConfigRegistry;
import com.rsring.rsring.RsRingMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;

public class GuiRsRingConfig extends GuiConfig {

    public GuiRsRingConfig(GuiScreen parentScreen) {
        super(parentScreen,
              new ConfigElement(ConfigRegistry.getConfig().getCategory(RsRingMod.MODID)).getChildElements(),
              RsRingMod.MODID,
              false,
              false,
              I18n.format("config.rsring.gui.title"));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        ConfigRegistry.syncAllConfig();
    }
}