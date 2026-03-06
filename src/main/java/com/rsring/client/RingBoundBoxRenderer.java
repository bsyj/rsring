package com.rsring.client;

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.item.ItemAbsorbRing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 戒指绑定箱子边框渲染器
 * 使用流畅的跑马灯效果
 * 吸收箱：蓝色系（青色到深蓝渐变）
 * 垃圾箱：红色系（橙红到深红渐变）
 */
@SideOnly(Side.CLIENT)
public class RingBoundBoxRenderer {

    private static final int MAX_RENDER_DISTANCE = 512;
    private static final int GL_FRONT_AND_BACK = 1032;
    private static final int GL_LINE = 6913;
    private static final int GL_FILL = 6914;
    private static final int GL_LINES = 1;
    
    // 用于平滑动画的累计时间
    private static float animationTime = 0.0F;
    private static long lastFrameTime = System.nanoTime();

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        
        if (player == null) return;

        // 更新动画时间（使用纳秒级精度，确保流畅）
        long currentTime = System.nanoTime();
        long deltaTime = currentTime - lastFrameTime;
        lastFrameTime = currentTime;
        animationTime += deltaTime / 1_000_000_000.0F * 1.5F;

        // 只有主手手持戒指时才渲染绑定方块
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof ItemAbsorbRing)) {
            return;
        }
        ItemStack heldStack = mainHand;

        IRsRingCapability cap = heldStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) return;
        
        // 渲染吸收箱边框（蓝色系跑马灯）
        if (cap.isBound()) {
            BlockPos chestPos = cap.getTerminalPos();
            int chestDim = cap.getTerminalDimension();
            
            if (player.dimension == chestDim) {
                double distance = player.getDistanceSq(chestPos);
                if (distance <= MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
                    renderBoundingBoxBlue(chestPos, event.getPartialTicks(), player);
                }
            }
        }
        
        // 渲染垃圾箱边框（红色系跑马灯）
        if (cap.isTrashCanBound()) {
            BlockPos trashPos = cap.getTrashCanPos();
            int trashDim = cap.getTrashCanDimension();
            
            if (player.dimension == trashDim) {
                double distance = player.getDistanceSq(trashPos);
                if (distance <= MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
                    renderBoundingBoxRed(trashPos, event.getPartialTicks(), player);
                }
            }
        }
    }

    /**
     * 渲染吸收箱边框 - 蓝色系跑马灯效果
     */
    private void renderBoundingBoxBlue(BlockPos pos, float partialTicks, EntityPlayer player) {
        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.glLineWidth(4.0F);

        buffer.setTranslation(-playerX, -playerY, -playerZ);

        // 绘制多层光晕效果
        for (int layer = 0; layer < 5; layer++) {
            // 蓝色系：色相范围 0.50 (青色) - 0.68 (深蓝)
            float hueBase = 0.58F;
            float hueWave1 = (float)Math.sin(animationTime * 0.8 + layer * 0.4) * 0.06F;
            float hueWave2 = (float)Math.sin(animationTime * 1.3 + layer * 0.25) * 0.04F;
            float hue = Math.max(0.50F, Math.min(0.68F, hueBase + hueWave1 + hueWave2));
            
            float saturation = 0.85F + (float)Math.sin(animationTime * 1.2 + layer * 0.3) * 0.12F;
            float brightness = 0.92F + (float)Math.sin(animationTime * 2.0 + layer * 0.5) * 0.08F;
            
            int rgb = getRGBFromHSB(hue, saturation, brightness);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            
            int alpha = 220 - layer * 40;

            buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            renderFlowingBorder(buffer, pos, red, green, blue, alpha, layer * 0.012F, layer);
            tessellator.draw();
        }

        // 绘制发光核心线
        GlStateManager.glLineWidth(2.5F);
        buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        
        int coreBright = (int)(180 + 75 * Math.sin(animationTime * 3.0));
        renderBlockBoundingWithExpansion(buffer, pos, 
            150 + (coreBright - 180) / 2, 
            200 + (coreBright - 180) / 3, 
            255, 200, 0.0F);
        tessellator.draw();

        // 绘制对角线效果
        GlStateManager.glLineWidth(1.8F);
        buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        renderDiagonalLines(buffer, pos, 100, 150, 255, 150, animationTime);
        tessellator.draw();

        buffer.setTranslation(0, 0, 0);
        GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
    }

    /**
     * 渲染垃圾箱边框 - 红色系跑马灯效果
     */
    private void renderBoundingBoxRed(BlockPos pos, float partialTicks, EntityPlayer player) {
        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.glLineWidth(4.0F);

        buffer.setTranslation(-playerX, -playerY, -playerZ);

        // 绘制多层光晕效果
        for (int layer = 0; layer < 5; layer++) {
            // 红色系：色相范围 0.95 (粉红) - 0.10 (橙红)
            float hueBase = 0.02F;
            float hueWave1 = (float)Math.sin(animationTime * 0.9 + layer * 0.35) * 0.05F;
            float hueWave2 = (float)Math.sin(animationTime * 1.4 + layer * 0.2) * 0.03F;
            float hue = hueBase + hueWave1 + hueWave2;
            if (hue < 0) hue += 1.0F;
            if (hue > 1.0F) hue -= 1.0F;
            if (hue > 0.1F && hue < 0.9F) hue = hue > 0.5F ? 0.95F : 0.05F;
            
            float saturation = 0.88F + (float)Math.sin(animationTime * 1.1 + layer * 0.25) * 0.10F;
            float brightness = 0.90F + (float)Math.sin(animationTime * 1.8 + layer * 0.45) * 0.10F;
            
            int rgb = getRGBFromHSB(hue, saturation, brightness);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            
            int alpha = 220 - layer * 40;

            buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            renderFlowingBorder(buffer, pos, red, green, blue, alpha, layer * 0.012F, layer);
            tessellator.draw();
        }

        // 绘制发光核心线
        GlStateManager.glLineWidth(2.5F);
        buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        
        int coreBright = (int)(180 + 75 * Math.sin(animationTime * 2.5));
        renderBlockBoundingWithExpansion(buffer, pos, 
            255, 
            120 + (coreBright - 180) / 3, 
            100 + (coreBright - 180) / 4, 200, 0.0F);
        tessellator.draw();

        // 绘制对角线效果
        GlStateManager.glLineWidth(1.8F);
        buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        renderDiagonalLines(buffer, pos, 255, 100, 80, 150, animationTime);
        tessellator.draw();

        buffer.setTranslation(0, 0, 0);
        GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
    }

    /**
     * 渲染流动边框 - 每条边有独立的亮度变化
     */
    private void renderFlowingBorder(BufferBuilder buffer, BlockPos pos, int red, int green, int blue, int opacity, float expansion, int layer) {
        final float size = 1.0f + expansion;
        final float offset = -expansion / 2.0f;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        
        float basePhase = animationTime * 2.0F + layer * 0.3F;

        // 顶面4条边
        float[] phases = {basePhase, basePhase + 0.25F, basePhase + 0.50F, basePhase + 0.75F};
        
        // 边1
        int[] c1 = applyFlowingColor(red, green, blue, opacity, phases[0], x, z);
        int[] c1e = applyFlowingColor(red, green, blue, opacity, phases[0] + 0.1F, x + 1, z);
        buffer.pos(x + offset, y + size, z + offset).color(c1[0], c1[1], c1[2], c1[3]).endVertex();
        buffer.pos(x + size, y + size, z + offset).color(c1e[0], c1e[1], c1e[2], c1e[3]).endVertex();
        
        // 边2
        int[] c2 = applyFlowingColor(red, green, blue, opacity, phases[1], x + 1, z);
        int[] c2e = applyFlowingColor(red, green, blue, opacity, phases[1] + 0.1F, x + 1, z + 1);
        buffer.pos(x + size, y + size, z + offset).color(c2[0], c2[1], c2[2], c2[3]).endVertex();
        buffer.pos(x + size, y + size, z + size).color(c2e[0], c2e[1], c2e[2], c2e[3]).endVertex();
        
        // 边3
        int[] c3 = applyFlowingColor(red, green, blue, opacity, phases[2], x + 1, z + 1);
        int[] c3e = applyFlowingColor(red, green, blue, opacity, phases[2] + 0.1F, x, z + 1);
        buffer.pos(x + size, y + size, z + size).color(c3[0], c3[1], c3[2], c3[3]).endVertex();
        buffer.pos(x + offset, y + size, z + size).color(c3e[0], c3e[1], c3e[2], c3e[3]).endVertex();
        
        // 边4
        int[] c4 = applyFlowingColor(red, green, blue, opacity, phases[3], x, z + 1);
        int[] c4e = applyFlowingColor(red, green, blue, opacity, phases[3] + 0.1F, x, z);
        buffer.pos(x + offset, y + size, z + size).color(c4[0], c4[1], c4[2], c4[3]).endVertex();
        buffer.pos(x + offset, y + size, z + offset).color(c4e[0], c4e[1], c4e[2], c4e[3]).endVertex();

        // 底面4条边
        float[] phasesB = {basePhase + 1.0F, basePhase + 1.25F, basePhase + 1.50F, basePhase + 1.75F};
        
        int[] c5 = applyFlowingColor(red, green, blue, opacity, phasesB[0], x + 1, z);
        int[] c5e = applyFlowingColor(red, green, blue, opacity, phasesB[0] + 0.1F, x + 1, z + 1);
        buffer.pos(x + size, y + offset, z + offset).color(c5[0], c5[1], c5[2], c5[3]).endVertex();
        buffer.pos(x + size, y + offset, z + size).color(c5e[0], c5e[1], c5e[2], c5e[3]).endVertex();
        
        int[] c6 = applyFlowingColor(red, green, blue, opacity, phasesB[1], x + 1, z + 1);
        int[] c6e = applyFlowingColor(red, green, blue, opacity, phasesB[1] + 0.1F, x, z + 1);
        buffer.pos(x + size, y + offset, z + size).color(c6[0], c6[1], c6[2], c6[3]).endVertex();
        buffer.pos(x + offset, y + offset, z + size).color(c6e[0], c6e[1], c6e[2], c6e[3]).endVertex();
        
        int[] c7 = applyFlowingColor(red, green, blue, opacity, phasesB[2], x, z + 1);
        int[] c7e = applyFlowingColor(red, green, blue, opacity, phasesB[2] + 0.1F, x, z);
        buffer.pos(x + offset, y + offset, z + size).color(c7[0], c7[1], c7[2], c7[3]).endVertex();
        buffer.pos(x + offset, y + offset, z + offset).color(c7e[0], c7e[1], c7e[2], c7e[3]).endVertex();
        
        int[] c8 = applyFlowingColor(red, green, blue, opacity, phasesB[3], x, z);
        int[] c8e = applyFlowingColor(red, green, blue, opacity, phasesB[3] + 0.1F, x + 1, z);
        buffer.pos(x + offset, y + offset, z + offset).color(c8[0], c8[1], c8[2], c8[3]).endVertex();
        buffer.pos(x + size, y + offset, z + offset).color(c8e[0], c8e[1], c8e[2], c8e[3]).endVertex();

        // 4条竖边
        float[] phasesV = {basePhase + 0.125F, basePhase + 0.375F, basePhase + 0.625F, basePhase + 0.875F};
        
        int[] cv1 = applyFlowingColor(red, green, blue, opacity, phasesV[0], x + 1, z + 1);
        buffer.pos(x + size, y + offset, z + size).color(cv1[0], cv1[1], cv1[2], cv1[3]).endVertex();
        buffer.pos(x + size, y + size, z + size).color(cv1[0], cv1[1], cv1[2], cv1[3]).endVertex();

        int[] cv2 = applyFlowingColor(red, green, blue, opacity, phasesV[1], x + 1, z);
        buffer.pos(x + size, y + offset, z + offset).color(cv2[0], cv2[1], cv2[2], cv2[3]).endVertex();
        buffer.pos(x + size, y + size, z + offset).color(cv2[0], cv2[1], cv2[2], cv2[3]).endVertex();

        int[] cv3 = applyFlowingColor(red, green, blue, opacity, phasesV[2], x, z + 1);
        buffer.pos(x + offset, y + offset, z + size).color(cv3[0], cv3[1], cv3[2], cv3[3]).endVertex();
        buffer.pos(x + offset, y + size, z + size).color(cv3[0], cv3[1], cv3[2], cv3[3]).endVertex();

        int[] cv4 = applyFlowingColor(red, green, blue, opacity, phasesV[3], x, z);
        buffer.pos(x + offset, y + offset, z + offset).color(cv4[0], cv4[1], cv4[2], cv4[3]).endVertex();
        buffer.pos(x + offset, y + size, z + offset).color(cv4[0], cv4[1], cv4[2], cv4[3]).endVertex();
    }

    /**
     * 应用流动颜色效果
     */
    private int[] applyFlowingColor(int red, int green, int blue, int alpha, float phase, int x, int z) {
        float posOffset = (x + z) * 0.15F;
        float wave = (float)Math.sin((phase + posOffset) * Math.PI * 2);
        
        float brightnessFactor = 0.8F + wave * 0.2F;
        
        int r = Math.max(0, Math.min(255, (int)(red * brightnessFactor)));
        int g = Math.max(0, Math.min(255, (int)(green * brightnessFactor)));
        int b = Math.max(0, Math.min(255, (int)(blue * brightnessFactor)));
        int a = Math.max(0, Math.min(255, (int)(alpha * (0.7F + wave * 0.3F))));
        
        return new int[]{r, g, b, a};
    }

    /**
     * HSB转RGB
     */
    private int getRGBFromHSB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - saturation * (1.0f - f));
            switch ((int) h) {
                case 0: r = (int)(brightness * 255); g = (int)(t * 255); b = (int)(p * 255); break;
                case 1: r = (int)(q * 255); g = (int)(brightness * 255); b = (int)(p * 255); break;
                case 2: r = (int)(p * 255); g = (int)(brightness * 255); b = (int)(t * 255); break;
                case 3: r = (int)(p * 255); g = (int)(q * 255); b = (int)(brightness * 255); break;
                case 4: r = (int)(t * 255); g = (int)(p * 255); b = (int)(brightness * 255); break;
                case 5: r = (int)(brightness * 255); g = (int)(p * 255); b = (int)(q * 255); break;
            }
        }
        return (r << 16) | (g << 8) | b;
    }

    /**
     * 渲染空间对角线效果（4条内部对角线）
     */
    private void renderDiagonalLines(BufferBuilder buffer, BlockPos pos, int red, int green, int blue, int alpha, float time) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        
        // 计算脉冲亮度
        float pulse = 0.5F + 0.5F * (float)Math.sin(time * 4.0);
        int r = (int)(red * pulse);
        int g = (int)(green * pulse);
        int b = (int)(blue * pulse);
        
        // 空间对角线（4条）- 使用不同的相位创造流动效果
        float phase1 = time * 2.0F;
        float phase2 = time * 2.0F + 0.5F;
        float phase3 = time * 2.0F + 1.0F;
        float phase4 = time * 2.0F + 1.5F;
        
        // 对角线1: (0,0,0) -> (1,1,1)
        int a1 = (int)(alpha * (0.4F + 0.6F * Math.sin(phase1 * Math.PI * 2)));
        buffer.pos(x, y, z).color(r, g, b, Math.max(30, a1)).endVertex();
        buffer.pos(x + 1, y + 1, z + 1).color(r, g, b, Math.max(30, a1)).endVertex();
        
        // 对角线2: (1,0,0) -> (0,1,1)
        int a2 = (int)(alpha * (0.4F + 0.6F * Math.sin(phase2 * Math.PI * 2)));
        buffer.pos(x + 1, y, z).color(r, g, b, Math.max(30, a2)).endVertex();
        buffer.pos(x, y + 1, z + 1).color(r, g, b, Math.max(30, a2)).endVertex();
        
        // 对角线3: (0,0,1) -> (1,1,0)
        int a3 = (int)(alpha * (0.4F + 0.6F * Math.sin(phase3 * Math.PI * 2)));
        buffer.pos(x, y, z + 1).color(r, g, b, Math.max(30, a3)).endVertex();
        buffer.pos(x + 1, y + 1, z).color(r, g, b, Math.max(30, a3)).endVertex();
        
        // 对角线4: (1,0,1) -> (0,1,0)
        int a4 = (int)(alpha * (0.4F + 0.6F * Math.sin(phase4 * Math.PI * 2)));
        buffer.pos(x + 1, y, z + 1).color(r, g, b, Math.max(30, a4)).endVertex();
        buffer.pos(x, y + 1, z).color(r, g, b, Math.max(30, a4)).endVertex();
    }

    /**
     * 渲染方块边界框
     */
    private void renderBlockBoundingWithExpansion(BufferBuilder buffer, BlockPos pos, int red, int green, int blue, int opacity, float expansion) {
        final float size = 1.0f + expansion;
        final float offset = -expansion / 2.0f;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // TOP
        buffer.pos(x + offset, y + size, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + offset).color(red, green, blue, opacity).endVertex();

        // BOTTOM
        buffer.pos(x + size, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + offset, z + offset).color(red, green, blue, opacity).endVertex();

        // Vertical edges
        buffer.pos(x + size, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + offset).color(red, green, blue, opacity).endVertex();
    }
}
