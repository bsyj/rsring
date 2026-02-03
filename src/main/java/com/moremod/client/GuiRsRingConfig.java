package com.moremod.client;

/*
 * Portions of this file are based on Cyclic (https://github.com/Lothrazar/Cyclic), licensed under the MIT License.
 * Source reference: E:\mod\Cyclic-trunk-1.12
 *
 * The MIT License (MIT)
 * Copyright (C) 2014-2018 Sam Bassett (aka Lothrazar)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.moremod.rsring.RsRingMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * RsRing Mod 配置界面
 * Configuration GUI for RsRing Mod
 * 
 * 完全参考 Cyclic 的 IngameConfigGui 实现
 * Based on Cyclic's IngameConfigGui implementation
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
    
    /**
     * 获取配置元素列表
     * Get configuration elements
     * 
     * 完全参考 Cyclic 的实现
     * Based on Cyclic's implementation
     */
    private static List<IConfigElement> getConfigElements() {
        // 确保配置已加载
        if (config == null) {
            config = new Configuration(new File("config/rsring/ring_config.cfg"));
            config.load();
        }
        
        // 完全参考 Cyclic: new ConfigElement(config.getCategory(MODID)).getChildElements()
        // 获取所有分类的配置元素
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
        ConfigManager.sync(RsRingMod.MODID, Config.Type.INSTANCE);
    }
}
