package com.moremod.client;

import com.moremod.rsring.RsRingMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * RsRing Mod 配置界面
 * Configuration GUI for RsRing Mod
 */
@SideOnly(Side.CLIENT)
public class GuiRsRingConfig extends GuiConfig {

    private static Configuration config;

    public GuiRsRingConfig(GuiScreen parentScreen) {
        super(parentScreen,
              getConfigElements(),
              RsRingMod.MODID,
              false,
              false,
              "RsRing Mod Configuration");
    }

    private static List<IConfigElement> getConfigElements() {
        // 直接加载配置文件（完全参考 Cyclic）
        if (config == null) {
            config = new Configuration(new File("config/rsring/ring_config.cfg"));
            config.load();
        }

        // 完全参考 Cyclic: new ConfigElement(config.getCategory(MODID)).getChildElements()
        List<IConfigElement> list = new ArrayList<>();
        for (String categoryName : config.getCategoryNames()) {
            list.addAll(new ConfigElement(config.getCategory(categoryName)).getChildElements());
        }

        return list;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        // 保存配置更改（完全参考 Cyclic）
        if (config != null && config.hasChanged()) {
            config.save();
        }
        // 同步 @Config 注解系统
        net.minecraftforge.common.config.ConfigManager.sync(RsRingMod.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
    }
}
