package com.moremod.client;

import com.moremod.capability.IExperiencePumpCapability;
import com.moremod.experience.ExperiencePumpController;
import com.moremod.experience.TankScanResult;
import com.moremod.item.ItemExperiencePump;
import com.moremod.item.ItemExperiencePumpController;
import com.moremod.network.PacketPumpAction;
import com.moremod.network.PacketPumpData;
import com.moremod.rsring.RsRingMod;
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
 * Experience pump controller GUI for controlling experience tanks.
 * Implements Requirements 3.6, 3.7, 6.3 for GUI controller functionality.
 * 
 * Features:
 * - Experience display formatting (XP + levels) - Requirement 6.3
 * - Mouse scroll wheel event handling - Requirements 3.6, 3.7
 * - Fine-tuning controls for extraction/injection - Requirements 3.6, 3.7
 * - Comprehensive tank detection across all inventory types
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
            com.moremod.capability.IExperiencePumpCapability cap = tank.getCapability(
                com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
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
                mode = data.getInteger("mode");
                retainLevel = data.hasKey("retainLevel") ? data.getInteger("retainLevel") : 10;
                useForMending = data.getBoolean("mending");
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

        // 绘制经验存储进度条
        drawXpProgressBar();

        // 绘制经验信息文本 - Enhanced with proper formatting (Requirement 6.3)
        drawExperienceInformation();

        // 绘制综合坦克信息
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
        // Total capacity used / total (显示为 XP 格式：已用 / 总容量)
        String totalCapInfo = String.format("%d / %d XP", totalStored, totalCapacity);
        // RGB 跑马灯颜色，根据时间变化
        long t = System.currentTimeMillis();
        int period = 5000; // 周期 ms，比标题慢一点

        // 三行分别有相位偏移，形成跑马灯效果
        int colorLine1 = hsvToRgbInt(((t % period) / (float) period + 0.0f) % 1.0f, 0.9f, 1.0f);
        int colorLine2 = hsvToRgbInt(((t % period) / (float) period + 0.33f) % 1.0f, 0.9f, 1.0f);
        int colorLine3 = hsvToRgbInt(((t % period) / (float) period + 0.66f) % 1.0f, 0.9f, 1.0f);

        fontRenderer.drawStringWithShadow("总容: " + totalCapInfo, guiLeft + 8, guiTop + 115, colorLine1);

        // Player current XP with level formatting
        int playerXP = pumpController.getPlayerTotalExperience(player);
        String playerXpInfo = pumpController.formatExperienceDisplay(playerXP);
        fontRenderer.drawStringWithShadow("玩家: " + playerXpInfo, guiLeft + 8, guiTop + 125, colorLine2);

        // 储罐数在 drawComprehensiveTankInfo 中绘制，使用 colorLine3
        // 把颜色保存到字段，供 drawComprehensiveTankInfo 使用
        animatedTankCountColor = colorLine3;
    }
    
    /**
     * Draws comprehensive tank information.
     */
    private void drawComprehensiveTankInfo() {
        // Tank count information (total capacity display removed)
        String tankCountInfo = "储罐数: " + totalTanks;
        // 使用动画颜色显示储罐数，形成跑马灯效果
        fontRenderer.drawStringWithShadow(tankCountInfo, guiLeft + 8, guiTop + 135, animatedTankCountColor);
        
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
     * Draws hover tips for buttons
     */
    private void drawHoverTips(int mouseX, int mouseY) {
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

    private void drawXpProgressBar() {
        int barX = guiLeft + 8;
        int barY = guiTop + 95;
        int barWidth = 160;
        int barHeight = 15;

        // 绘制进度条背景
        drawRect(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);

        // 计算进度 - Use total capacity for comprehensive view
        float progress = totalCapacity > 0 ? (float) totalStored / totalCapacity : 0;
        int fillWidth = (int) (barWidth * progress);

        // 绘制进度条填充
        int fillColor = 0xFF7EFF05; // 绿色 #7EFF05
        drawRect(barX, barY, barX + fillWidth, barY + barHeight, fillColor);

        // 绘制边框
        drawRect(barX, barY, barX + 1, barY + barHeight, 0xFFFFFFFF);
        drawRect(barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, 0xFFFFFFFF);
        drawRect(barX, barY, barX + barWidth, barY + 1, 0xFFFFFFFF);
        drawRect(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, 0xFFFFFFFF);
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
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_MENDING, tankStack));
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
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_MODE, tankStack));
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
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_RETAIN_UP, tankStack, 1));
                }
                break;
            case 3: // 全部取出（需要储罐）
                if (!tankStack.isEmpty()) {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_TAKE_ALL, tankStack));
                }
                break;
            case 4: // 取出N级（需要储罐）
                if (!tankStack.isEmpty()) {
                    // 发送取出N级的请求，value参数传递要取出的等级数
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_TAKE_ONE, tankStack, extractLevels));
                }
                break;
            case 5: // 存入N级（需要储罐）
                if (!tankStack.isEmpty()) {
                    // 发送存入N级的请求，value参数传递要存入的等级数
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_STORE_ONE, tankStack, storeLevels));
                }
                break;
            case 6: // 全部存入（需要储罐）
                if (!tankStack.isEmpty()) {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_STORE_ALL, tankStack));
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
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_RETAIN_UP, tankStack, 1));
                } else {
                    RsRingMod.network.sendToServer(new PacketPumpAction(hand, PacketPumpAction.ACTION_RETAIN_DOWN, tankStack, 1));
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