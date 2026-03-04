package com.rsring.client;

import com.rsring.capability.IExperiencePumpCapability;
import com.rsring.experience.ExperiencePumpController;
import com.rsring.experience.TankScanResult;
import com.rsring.item.ItemExperiencePump;
import com.rsring.item.ItemExperiencePumpController;
import com.rsring.network.PacketPumpAction;
import com.rsring.network.PacketPumpData;
import com.rsring.rsring.RsRingMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * 经验泵控制器GUI，用于控制经验储罐。
 * 实现需求3.6、3.7、6.3的GUI控制器功能。
 *
 * 功能：
 * - 经验显示格式化（经验值+等级）- 需求6.3
 * - 鼠标滚轮事件处理 - 需求3.6、3.7
 * - 提取/注入的微调控制 - 需求3.6、3.7
 * - 跨所有库存类型的全面储罐检测
 */
@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
public class GuiExperiencePumpController extends GuiScreen {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int GOLD = 0xFFD700;
    private static final int BG_COLOR = 0xFFC6C6C6; // #C6C6C6
    // Use custom texture placed under resources: assets/rsring/textures/gui/kzq.png
    private static final ResourceLocation GUI_TEXTURES = new ResourceLocation("rsring", "textures/gui/kzq.png");

    // 操作按钮统一尺寸与间距
    private static final int OP_BUTTON_WIDTH = 60;
    private static final int OP_BUTTON_HEIGHT = 20;
    private static final int OP_BUTTON_SPACING = 5;

    private final ItemStack controllerStack;
    private final EnumHand hand;

    // Experience pump controller for comprehensive storage management
    private final ExperiencePumpController pumpController;

    private int guiLeft;
    private int guiTop;

    // Storage data - updated from comprehensive scanning
    private int xpStored;
    private int capacityLevels;
    private int mode;
    private int retainLevel;
    private boolean useForMending;
    private int maxXp;

    // Comprehensive storage information
    private int totalTanks;
    private int totalCapacity;
    private int totalStored;
    // animated color for tank count
    private int animatedTankCountColor = 0xE0E0E0;

    // 简化的等级控制
    private int extractLevels = 1;  // 取出等级数，默认1级
    private int storeLevels = 1;    // 存入等级数，默认1级

    // 已存等级悬停提示所需的数据
    private double storedLevelsBasedOnPlayer;
    private int storedLevelsBasedOnPlayerY;
    private int storedLevelsBasedOnPlayerWidth;
    private double storedLevelsFromZero;
    private int storedLevelsFromZeroY;
    private int storedLevelsFromZeroWidth;

    public GuiExperiencePumpController(ItemStack controllerStack, EnumHand hand) {
        this.controllerStack = controllerStack;
        this.hand = hand;
        this.pumpController = ExperiencePumpController.getInstance();

        // 从控制器加载配置
        loadControllerConfiguration();

        // 从控制器加载存取等级
        loadExtractStoreLevels();

        // Initialize with comprehensive storage scanning
        refreshTankData();

        // 将控制器配置同步到所有储罐
        syncControllerToTanks();
    }

    /**
     * 从控制器加载存取等级
     */
    private void loadExtractStoreLevels() {
        extractLevels = ItemExperiencePumpController.getExtractLevels(controllerStack);
        storeLevels = ItemExperiencePumpController.getStoreLevels(controllerStack);
    }

    /**
     * 保存存取等级到控制器
     */
    private void saveExtractStoreLevels() {
        ItemExperiencePumpController.setExtractStoreLevels(controllerStack, extractLevels, storeLevels);
    }

    /**
     * Convert HSV (h: 0..1, s:0..1, v:0..1) to packed ARGB int (opaque)
     */
    private static int hsvToRgbInt(float h, float s, float v) {
        if (s <= 0.0f) {
            int c = (int) (v * 255.0f);
            return (0xFF << 24) | (c << 16) | (c << 8) | c;
        }
        float hf = (h - (float)Math.floor(h)) * 6.0f;
        int i = (int) Math.floor(hf);
        float f = hf - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - s * f);
        float t = v * (1.0f - s * (1.0f - f));
        float r, g, b;
        switch (i) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5:
            default: r = v; g = p; b = q; break;
        }
        int ri = (int) (r * 255.0f);
        int gi = (int) (g * 255.0f);
        int bi = (int) (b * 255.0f);
        return (0xFF << 24) | (ri << 16) | (gi << 8) | bi;
    }

    public void updateFromPacket(PacketPumpData msg) {
        xpStored = msg.getXpStored();
        capacityLevels = msg.getCapacityLevels();
        mode = msg.getMode();
        retainLevel = msg.getRetainLevel();
        useForMending = msg.isUseForMending();
        maxXp = msg.getMaxXp();

        // Refresh comprehensive storage data
        refreshTankData();
    }

    /**
     * 从控制器加载配置
     */
    private void loadControllerConfiguration() {
        mode = ItemExperiencePumpController.getMode(controllerStack);
        retainLevel = ItemExperiencePumpController.getRetainLevel(controllerStack);
        useForMending = ItemExperiencePumpController.isUseForMending(controllerStack);
    }

    /**
     * 将控制器配置同步到所有储罐
     */
    private void syncControllerToTanks() {
        net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        // 扫描所有储罐
        TankScanResult scanResult = pumpController.scanAllInventories(player);
        List<ItemStack> tanks = scanResult.getAllTanks();

        // 将控制器配置应用到每个储罐
        for (ItemStack tank : tanks) {
            com.rsring.capability.IExperiencePumpCapability cap = tank.getCapability(
                com.rsring.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (cap != null) {
                cap.setMode(mode);
                cap.setRetainLevel(retainLevel);
                cap.setUseForMending(useForMending);
                ItemExperiencePump.syncCapabilityToStack(tank, cap);
            }
        }
    }

    /**
     * Refreshes storage data using comprehensive scanning from ExperiencePumpController.
     * Implements comprehensive storage detection across all inventory types.
     */
    private void refreshTankData() {
        net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }

        // Use comprehensive storage scanning
        TankScanResult scanResult = pumpController.scanAllInventories(player);
        totalTanks = scanResult.getTankCount();
        totalCapacity = scanResult.getTotalCapacity();
        totalStored = scanResult.getTotalStored();

        // Update individual storage data from first available storage for compatibility
        List<ItemStack> tanks = scanResult.getAllTanks();
        if (!tanks.isEmpty()) {
            ItemStack firstTank = tanks.get(0);
            net.minecraft.nbt.NBTTagCompound data = ItemExperiencePump.getDataFromNBT(firstTank);
            if (data != null) {
                xpStored = data.getInteger("xp");
                capacityLevels = data.hasKey("capacityLevels") ? data.getInteger("capacityLevels") : 1;  // 默认1级
                mode = data.hasKey("mode") ? data.getInteger("mode") : com.rsring.config.ExperienceTankConfig.tank.defaultPumpMode;
                retainLevel = data.hasKey("retainLevel") ? data.getInteger("retainLevel") : com.rsring.config.ExperienceTankConfig.tank.defaultRetainLevel;
                useForMending = data.hasKey("mending") ? data.getBoolean("mending") : com.rsring.config.ExperienceTankConfig.tank.defaultMendingMode;
                maxXp = ItemExperiencePump.getMaxXpFromNBT(firstTank);
            }
        } else {
            // No storages found - reset to defaults
            totalTanks = 0;
            totalCapacity = 0;
            totalStored = 0;
            maxXp = 0;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;
        buttonList.clear();

        // 第一行：模式切换、保留等级、经验修补开关 - 三个按钮平均并列排列
        int topRowY = guiTop + 20;
        int topRowButtonSpacing = 5;
        int topRowButtonWidth = (GUI_WIDTH - 8 * 2 - 2 * topRowButtonSpacing) / 3; // 三个按钮平均分配宽度

        // 模式切换按钮
        buttonList.add(new GuiButton(1, guiLeft + 8, topRowY, topRowButtonWidth, 20, getModeButtonText()));

        // 保留等级按钮（默认0级，鼠标悬停滚轮调整）
        buttonList.add(new GuiButton(2, guiLeft + 8 + topRowButtonWidth + topRowButtonSpacing, topRowY, topRowButtonWidth, 20, "保留: " + retainLevel));

        // 经验修补开关按钮
        buttonList.add(new GuiButton(0, guiLeft + 8 + (topRowButtonWidth + topRowButtonSpacing) * 2, topRowY, topRowButtonWidth, 20, getMendingButtonText()));

        // 操作按钮区域 - 四个按钮平均分布
        int opButtonStartX = guiLeft + 8;  // 起始X坐标
        int opButtonWidth = (GUI_WIDTH - 8 * 2 - 3 * OP_BUTTON_SPACING) / 4; // 平均分配宽度，考虑间距
        int opButtonY = guiTop + 45; // 操作按钮Y坐标

        // 全部取出按钮
        buttonList.add(new GuiButton(3, opButtonStartX, opButtonY, opButtonWidth, 20, "全取"));
        // 取出N级按钮（默认取1级，鼠标悬停滚轮调整）
        buttonList.add(new GuiButton(4, opButtonStartX + opButtonWidth + OP_BUTTON_SPACING, opButtonY, opButtonWidth, 20, "取 " + extractLevels + " 级"));
        // 存入N级按钮（默认存1级，鼠标悬停滚轮调整）
        buttonList.add(new GuiButton(5, opButtonStartX + (opButtonWidth + OP_BUTTON_SPACING) * 2, opButtonY, opButtonWidth, 20, "存 " + storeLevels + " 级"));
        // 全部存入按钮
        buttonList.add(new GuiButton(6, opButtonStartX + (opButtonWidth + OP_BUTTON_SPACING) * 3, opButtonY, opButtonWidth, 20, "全存"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        refreshTankData();

        // 绘制背景：使用纯色背景（由控制器项的材质已在物品模型中定义）
        drawDefaultBackground();
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, BG_COLOR);

        // 绘制标题 - RGB 跑马灯效果（更加炫酷：更快速度 + 双重色彩叠加效果）
        String title = "经验泵控制器";
        long t = System.currentTimeMillis();
        int titlePeriod = 2000; // 2秒一个周期，比下面的更快
        float titleHue = ((t % titlePeriod) / (float) titlePeriod) % 1.0f;
        // 添加额外的色相偏移，产生更炫酷的双重彩虹效果
        float hueOffset = (float)Math.sin(t / 500.0) * 0.1f; // 额外的波动
        int titleColor = hsvToRgbInt((titleHue + hueOffset) % 1.0f, 1.0f, 1.0f); // 饱和度和明度都是最大，色彩鲜艳
        fontRenderer.drawStringWithShadow(title, guiLeft + (GUI_WIDTH - fontRenderer.getStringWidth(title)) / 2, guiTop + 6, titleColor);

        // 更新按钮文本
        updateButtonTexts();

        super.drawScreen(mouseX, mouseY, partialTicks);

        // 绘制经验存储进度条（在文字上方）
        drawXpProgressBar();

        // 绘制经验信息文本 - Enhanced with proper formatting (Requirement 6.3)
        drawExperienceInformation();

        // 绘制综合坦克信息（警告）
        drawComprehensiveTankInfo();

        // 绘制鼠标悬停提示
        drawHoverTips(mouseX, mouseY);
    }

    /**
     * Draws experience information with proper formatting showing both XP and levels.
     * Implements Requirement 6.3 for experience display format.
     */
    private void drawExperienceInformation() {
        net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        // Total capacity used / total (显示为 XP 格式：已用 / 总容量）
        String totalCapInfo = String.format("%d / %d XP", totalStored, totalCapacity);
        // RGB 跑马灯颜色，神光同步（与标题的炫酷双重彩虹一致）
        long t = System.currentTimeMillis();
        int period = 2000; // 与标题同步：2秒周期
        float hueOffset = (float)Math.sin(t / 500.0) * 0.1f; // 与标题同步：双重彩虹波动

        // 各行分别有相位偏移，形成跑马灯效果
        int colorLine1 = hsvToRgbInt(((t % period) / (float) period + 0.0f + hueOffset) % 1.0f, 1.0f, 1.0f);
        int colorLine2 = hsvToRgbInt(((t % period) / (float) period + 0.1f + hueOffset) % 1.0f, 1.0f, 1.0f);
        int colorLine3 = hsvToRgbInt(((t % period) / (float) period + 0.2f + hueOffset) % 1.0f, 1.0f, 1.0f);
        int colorLine4 = hsvToRgbInt(((t % period) / (float) period + 0.3f + hueOffset) % 1.0f, 1.0f, 1.0f);
        int colorLine5 = hsvToRgbInt(((t % period) / (float) period + 0.4f + hueOffset) % 1.0f, 1.0f, 1.0f);

        int startY = guiTop + 92;  // 进度条下方开始（增加间距）
        int lineSpacing = 12;

        // 第一行：总容量
        fontRenderer.drawStringWithShadow("总容: " + totalCapInfo, guiLeft + 8, startY, colorLine1);

        // 第二行：已存等级（基于玩家当前等级）
        int playerXP = pumpController.getPlayerTotalExperience(player);
        storedLevelsBasedOnPlayer = com.rsring.util.XpHelper.getStoredLevelsRelativeToPlayer(playerXP, totalStored);
        String line2Text = "已存等级: " + String.format("%.1f", storedLevelsBasedOnPlayer);
        fontRenderer.drawStringWithShadow(line2Text, guiLeft + 8, startY + lineSpacing, colorLine2);
        storedLevelsBasedOnPlayerY = startY + lineSpacing;
        storedLevelsBasedOnPlayerWidth = fontRenderer.getStringWidth(line2Text);

        // 第三行：已存等级（从0级开始计算）
        storedLevelsFromZero = com.rsring.util.XpHelper.getLevelsForExperience(totalStored);
        String line3Text = "已存等级: " + String.format("%.1f", storedLevelsFromZero);
        fontRenderer.drawStringWithShadow(line3Text, guiLeft + 8, startY + lineSpacing * 2, colorLine3);
        storedLevelsFromZeroY = startY + lineSpacing * 2;
        storedLevelsFromZeroWidth = fontRenderer.getStringWidth(line3Text);

        // 第四行：玩家当前经验
        String playerXpInfo = pumpController.formatExperienceDisplay(playerXP);
        fontRenderer.drawStringWithShadow("玩家: " + playerXpInfo, guiLeft + 8, startY + lineSpacing * 3, colorLine4);

        // 第五行：储罐数（使用 colorLine5）
        String tankCountInfo = "储罐数: " + totalTanks;
        fontRenderer.drawStringWithShadow(tankCountInfo, guiLeft + 8, startY + lineSpacing * 4, colorLine5);

        // 把颜色保存到字段，供 drawComprehensiveTankInfo 使用（用于警告显示）
        animatedTankCountColor = colorLine5;
    }

    /**
     * 绘制经验存储进度条
     */
    private void drawXpProgressBar() {
        int barX = guiLeft + 8;
        int barY = guiTop + 68;  // 操作按钮下方，文字上方
        int barWidth = 160;      // 进度条宽度
        int barHeight = 16;      // 进度条高度（加高）

        // 绘制进度条背景
        drawRect(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);

        // 计算进度 - 使用总容量
        float progress = totalCapacity > 0 ? (float) totalStored / totalCapacity : 0;
        int fillWidth = Math.max(1, (int) (barWidth * progress)); // 至少1像素

        // 绘制进度条填充（带动态效果）
        if (totalStored > 0) {
            long t = System.currentTimeMillis();
            
            // 基础绿色
            int baseColor = 0xFF7EFF05;
            
            // 动态效果1：光泽流动（从左到右的光带）
            int shinePos = (int) ((t / 10) % (fillWidth + 40)) - 20;  // 光带位置
            int shineWidth = 20;  // 光带宽度
            
            // 动态效果2：颜色脉冲（亮度周期性变化）
            float pulse = (float) Math.sin(t / 300.0) * 0.15f + 0.85f;  // 0.7~1.0
            
            // 逐像素绘制，实现光泽流动效果
            for (int x = 0; x < fillWidth; x++) {
                // 计算当前像素的亮度
                float brightness = pulse;
                
                // 光泽效果：光带经过的地方更亮
                int distToShine = Math.abs(x - shinePos);
                if (distToShine < shineWidth) {
                    float shineIntensity = 1.0f - (float) distToShine / shineWidth;
                    shineIntensity = shineIntensity * shineIntensity;  // 平滑曲线
                    brightness += shineIntensity * 0.5f;  // 最多增加50%亮度
                }
                
                // 计算最终颜色
                int r = (int) Math.min(255, (baseColor >> 16 & 0xFF) * brightness);
                int g = (int) Math.min(255, (baseColor >> 8 & 0xFF) * brightness);
                int b = (int) Math.min(255, (baseColor & 0xFF) * brightness);
                int pixelColor = 0xFF000000 | (r << 16) | (g << 8) | b;
                
                drawRect(barX + x, barY, barX + x + 1, barY + barHeight, pixelColor);
            }
        }

        // 绘制边框（灰色边框）
        int borderColor = 0xFF888888;
        drawRect(barX, barY, barX + 1, barY + barHeight, borderColor); // 左边框
        drawRect(barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, borderColor); // 右边框
        drawRect(barX, barY, barX + barWidth, barY + 1, borderColor); // 上边框
        drawRect(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, borderColor); // 下边框

        // 跑马灯流星效果 - 双流星沿边框循环移动
        long t = System.currentTimeMillis();
        int perimeter = 2 * (barWidth + barHeight); // 边框周长
        int meteorLength = 25; // 流星长度
        int period = 2000; // 与标题同步：2 秒周期
        float hueOffset = (float)Math.sin(t / 500.0) * 0.1f; // 与标题同步：双重彩虹波动
        
        // 计算流星的基础相位（与字体颜色使用相同的时间基准）
        float basePhase = (t % period) / (float) period;

        // 绘制两个流星（相位差半周）
        for (int meteor = 0; meteor < 2; meteor++) {
            float meteorPhase = (basePhase + meteor * 0.5f) % 1.0f; // 第二个流星相位差半周
            int meteorPos = (int) (meteorPhase * perimeter); // 流星位置（与字体颜色同步）

            // 绘制单个流星
            for (int i = 0; i < meteorLength; i++) {
                int pos = (meteorPos - i + perimeter) % perimeter;
                int mx, my;

                // 计算流星当前位置在边框的哪一边
                if (pos < barWidth) {
                    // 上边（从左到右）
                    mx = barX + pos;
                    my = barY;
                } else if (pos < barWidth + barHeight) {
                    // 右边（从上到下）
                    mx = barX + barWidth - 1;
                    my = barY + (pos - barWidth);
                } else if (pos < 2 * barWidth + barHeight) {
                    // 下边（从右到左）
                    mx = barX + barWidth - (pos - barWidth - barHeight) - 1;
                    my = barY + barHeight - 1;
                } else {
                    // 左边（从下到上）
                    mx = barX;
                    my = barY + barHeight - (pos - 2 * barWidth - barHeight) - 1;
                }

                // 头大尾小效果：头部亮，尾部融入边框背景
                float meteorProgress = (float) i / meteorLength;
                // 使用更陡的曲线，让尾巴快速消失
                float alpha = 1.0f - meteorProgress * meteorProgress * meteorProgress; // 立方曲线

                // RGB 色相与标题同步，双重彩虹波动，两个流星色相错开
                float meteorHue = (meteorPhase + hueOffset) % 1.0f;
                int meteorColor = hsvToRgbInt(meteorHue, 1.0f, 1.0f);

                // 边框背景色（灰色）
                int bgColor = 0xFF888888;

                // 颜色混合：尾部逐渐融入背景色
                int r = (int) ((((meteorColor >> 16) & 0xFF) * alpha) + (((bgColor >> 16) & 0xFF) * (1.0f - alpha)));
                int g = (int) ((((meteorColor >> 8) & 0xFF) * alpha) + (((bgColor >> 8) & 0xFF) * (1.0f - alpha)));
                int b = (int) (((meteorColor & 0xFF) * alpha) + ((bgColor & 0xFF) * (1.0f - alpha)));
                int blendedColor = 0xFF000000 | (r << 16) | (g << 8) | b;

                // 绘制单像素流星
                drawRect(mx, my, mx + 1, my + 1, blendedColor);
            }
        }
    }

    /**
     * Draws comprehensive tank information (warning only).
     */
    private void drawComprehensiveTankInfo() {
        // 无储罐警告 - 显示在右下角，使用红色基础的RGB跑马灯效果
        if (totalTanks == 0) {
            String warningText = "无储罐";
            int warningX = guiLeft + GUI_WIDTH - fontRenderer.getStringWidth(warningText) - 8;
            int warningY = guiTop + GUI_HEIGHT - 16;

            // 红色基础的RGB跑马灯：色相在红色范围内变化（0.0-0.1），高饱和度，明度变化
            long t = System.currentTimeMillis();
            int warningPeriod = 1000; // 1秒周期，快速闪烁警示
            float phase = ((t % warningPeriod) / (float) warningPeriod);
            // 色相在红色范围 (0.0-0.1)，饱和度固定为1.0，明度在0.6-1.0之间变化
            float hue = phase * 0.1f; // 红色到橙红色
            float brightness = 0.6f + 0.4f * (float)Math.sin(phase * Math.PI * 2); // 明度波动
            int warningColor = hsvToRgbInt(hue, 1.0f, brightness);

            fontRenderer.drawStringWithShadow(warningText, warningX, warningY, warningColor);
        }
    }

    /**
     * Draws hover tips for buttons and stored level lines
     */
    private void drawHoverTips(int mouseX, int mouseY) {
        // 检查是否悬停在"已存等级（基于玩家）"文字上
        if (isMouseOverText(mouseX, mouseY, storedLevelsBasedOnPlayerY, storedLevelsBasedOnPlayerWidth)) {
            drawHoveringText(java.util.Arrays.asList("基于玩家: 玩家使用储罐经验后能升的等级"), mouseX, mouseY, fontRenderer);
            return;
        }

        // 检查是否悬停在"已存等级（从0开始）"文字上
        if (isMouseOverText(mouseX, mouseY, storedLevelsFromZeroY, storedLevelsFromZeroWidth)) {
            drawHoveringText(java.util.Arrays.asList("从0开始: 储罐经验相当于从0级升到的等级"), mouseX, mouseY, fontRenderer);
            return;
        }

        // 检查按钮悬停
        for (GuiButton button : buttonList) {
            if (button.isMouseOver()) {
                String tip = "";
                switch (button.id) {
                    case 0:
                        tip = "经验修补开关：开启后储罐可自动修复附魔装备";
                        break;
                    case 1:
                        tip = "模式切换：关闭/抽→罐/罐→人";
                        break;
                    case 2:
                        tip = "保留等级：鼠标滚轮可快速调整";
                        break;
                    case 3:
                        tip = "全部取出：将所有储罐中的经验取出到玩家";
                        break;
                    case 4:
                        tip = "取N级：从储罐取出指定等级的经验到玩家，鼠标滚轮可调整等级数";
                        break;
                    case 5:
                        tip = "存N级：从玩家存入指定等级的经验到储罐，鼠标滚轮可调整等级数";
                        break;
                    case 6:
                        tip = "全部存入：将玩家多余经验全部存入储罐";
                        break;
                }

                if (!tip.isEmpty()) {
                    drawHoveringText(java.util.Arrays.asList(tip.split("\n")), mouseX, mouseY, fontRenderer);
                }
            }
        }
    }

    private void updateButtonTexts() {
        GuiButton mendingBtn = getButton(0);
        if (mendingBtn != null) mendingBtn.displayString = getMendingButtonText();

        GuiButton modeBtn = getButton(1);
        if (modeBtn != null) modeBtn.displayString = getModeButtonText();

        GuiButton retainBtn = getButton(2);
        if (retainBtn != null) retainBtn.displayString = "保留: " + retainLevel;

        GuiButton takeAllBtn = getButton(3);
        if (takeAllBtn != null) takeAllBtn.displayString = "全取";

        GuiButton extractBtn = getButton(4);
        if (extractBtn != null) extractBtn.displayString = "取 " + extractLevels + " 级";

        GuiButton storeBtn = getButton(5);
        if (storeBtn != null) storeBtn.displayString = "存 " + storeLevels + " 级";

        GuiButton storeAllBtn = getButton(6);
        if (storeAllBtn != null) storeAllBtn.displayString = "全存";
    }

    /**
     * 检查鼠标是否悬停在指定行上
     */
    private boolean isMouseOverLine(int mouseX, int mouseY, int lineY) {
        if (lineY <= 0) return false;
        int lineLeft = guiLeft + 8;
        int lineRight = guiLeft + GUI_WIDTH - 8;
        int lineTop = lineY;
        int lineBottom = lineY + 10;  // 行高约10像素
        return mouseX >= lineLeft && mouseX <= lineRight && mouseY >= lineTop && mouseY <= lineBottom;
    }

    /**
     * 检查鼠标是否悬停在指定文字区域上
     */
    private boolean isMouseOverText(int mouseX, int mouseY, int textY, int textWidth) {
        if (textY <= 0 || textWidth <= 0) return false;
        int textLeft = guiLeft + 8;
        int textRight = guiLeft + 8 + textWidth;
        int textTop = textY;
        int textBottom = textY + 10;
        return mouseX >= textLeft && mouseX <= textRight && mouseY >= textTop && mouseY <= textBottom;
    }

    private String getMendingButtonText() {
        return useForMending ? "修ON" : "修OFF";
    }

    private String getModeButtonText() {
        switch (mode) {
            case IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER: return "抽→罐";
            case IExperiencePumpCapability.MODE_PUMP_TO_PLAYER: return "罐→人";
            default: return "关闭";
        }
    }

    private GuiButton getButton(int id) {
        for (GuiButton b : buttonList) if (b.id == id) return b;
        return null;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (hand == null) return;

        net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        // 扫描储罐（可能没有）
        TankScanResult scanResult = pumpController.scanAllInventories(player);
        ItemStack tankStack = scanResult.getTankCount() > 0 ? scanResult.getAllTanks().get(0) : ItemStack.EMPTY;

        switch (button.id) {
            case 0: // 经验修补开关
                // 更新控制器配置
                useForMending = !useForMending;
                ItemExperiencePumpController.setControllerData(controllerStack, mode, retainLevel, useForMending);
                // 同步到所有储罐（如果有）
                syncControllerToTanks();
                // 发送网络包（如果有储罐）
                if (!tankStack.isEmpty()) {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_MENDING));
                }
                break;
            case 1: // 模式切换
                // 更新控制器配置
                mode = (mode + 1) % 3;
                ItemExperiencePumpController.setControllerData(controllerStack, mode, retainLevel, useForMending);
                // 同步到所有储罐（如果有）
                syncControllerToTanks();
                // 发送网络包（如果有储罐）
                if (!tankStack.isEmpty()) {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_MODE));
                }
                break;
            case 2: // 保留等级（点击+1）
                // 更新控制器配置
                retainLevel = (retainLevel + 1) % 101; // 0-100循环
                ItemExperiencePumpController.setControllerData(controllerStack, mode, retainLevel, useForMending);
                // 同步到所有储罐（如果有）
                syncControllerToTanks();
                // 发送网络包（如果有储罐）
                if (!tankStack.isEmpty()) {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_RETAIN_UP, 1));
                }
                break;
            case 3: // 全部取出（需要储罐）
                if (!tankStack.isEmpty()) {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_TAKE_ALL));
                }
                break;
            case 4: // 取出N级（需要储罐）
                if (!tankStack.isEmpty()) {
                    // 发送取出N级的请求，value参数传递要取出的等级数
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_TAKE_ONE, extractLevels));
                }
                break;
            case 5: // 存入N级（需要储罐）
                if (!tankStack.isEmpty()) {
                    // 发送存入N级的请求，value参数传递要存入的等级数
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_STORE_ONE, storeLevels));
                }
                break;
            case 6: // 全部存入（需要储罐）
                if (!tankStack.isEmpty()) {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_STORE_ALL));
                }
                break;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dw = Mouse.getEventDWheel();
        if (dw == 0) return;

        net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        TankScanResult scanResult = pumpController.scanAllInventories(player);
        ItemStack tankStack = scanResult.getTankCount() > 0 ? scanResult.getAllTanks().get(0) : ItemStack.EMPTY;

        // 保留等级按钮 - 鼠标悬停时滚轮调整（向上滚轮增加，向下滚轮减少）
        GuiButton retainBtn = getButton(2);
        if (retainBtn != null && retainBtn.isMouseOver()) {
            if (dw > 0) {
                // 向上滚轮：增加保留等级
                retainLevel = Math.min(100, retainLevel + 1);
            } else {
                // 向下滚轮：减少保留等级
                retainLevel = Math.max(0, retainLevel - 1);
            }
            ItemExperiencePumpController.setControllerData(controllerStack, mode, retainLevel, useForMending);
            syncControllerToTanks();
            if (!tankStack.isEmpty()) {
                if (dw > 0) {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_RETAIN_UP, 1));
                } else {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_RETAIN_DOWN, 1));
                }
            }
            return;
        }

        // 取出等级按钮 - 鼠标悬停时滚轮调整（向上滚轮增加，向下滚轮减少）
        GuiButton extractBtn = getButton(4);
        if (extractBtn != null && extractBtn.isMouseOver()) {
            if (dw > 0) {
                // 向上滚轮：增加取出等级，无上限
                extractLevels = extractLevels + 1;
            } else {
                // 向下滚轮：减少取出等级，最低1级
                extractLevels = Math.max(1, extractLevels - 1);
            }
            // 保存取出等级到控制器
            saveExtractStoreLevels();
            return;
        }

        // 存入等级按钮 - 鼠标悬停时滚轮调整（向上滚轮增加，向下滚轮减少）
        GuiButton storeBtn = getButton(5);
        if (storeBtn != null && storeBtn.isMouseOver()) {
            if (dw > 0) {
                // 向上滚轮：增加存入等级，无上限
                storeLevels = storeLevels + 1;
            } else {
                // 向下滚轮：减少存入等级，最低1级
                storeLevels = Math.max(1, storeLevels - 1);
            }
            // 保存存入等级到控制器
            saveExtractStoreLevels();
            return;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // 检查是否按下了背包键（通常是E键）
        if (keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            this.mc.player.closeScreen();
            return;
        }
        // 调用父类处理其他按键（包括ESC）
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
