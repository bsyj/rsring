package com.rsring.client;

import com.rsring.config.ConfigRegistry;
import com.rsring.rsring.RsRingMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

public class GuiRsRingConfig extends GuiConfig {

    public GuiRsRingConfig(GuiScreen parentScreen) {
        super(parentScreen,
              getConfigElementsFiltered(),
              RsRingMod.MODID,
              false,
              false,
              I18n.format("config.rsring.gui.title"));
    }

    private static List<IConfigElement> getConfigElementsFiltered() {
        Configuration config = ConfigRegistry.getConfig();
        List<IConfigElement> allElements = new ConfigElement(config.getCategory(RsRingMod.MODID)).getChildElements();

        List<IConfigElement> filteredElements = new ArrayList<>();
        for (IConfigElement element : allElements) {
            String name = element.getName();
            // 排除 general 和 network 配置类别
            if (!name.equals(RsRingMod.MODID + ".general") && !name.equals(RsRingMod.MODID + ".network")) {
                filteredElements.add(element);
            }
        }

        return filteredElements;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        ConfigRegistry.syncAllConfig();
    }
}
