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
 * 参考XRay模组的渲染实现
 * 当玩家主手持有戒指时，绘制绑定箱子的RGB彩色边框
 */
@SideOnly(Side.CLIENT)
public class RingBoundBoxRenderer {

    private static final int MAX_RENDER_DISTANCE = 512; // 最大渲染距离512格
    private static final int GL_FRONT_AND_BACK = 1032;
    private static final int GL_LINE = 6913;
    private static final int GL_FILL = 6914;
    private static final int GL_LINES = 1;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        
        if (player == null) return;

        // 只有主手手持戒指时才渲染绑定方块（参考 XRay 的渲染时机）
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof ItemAbsorbRing)) {
            return;
        }
        ItemStack heldStack = mainHand;

        // 获取戒指的capability
        IRsRingCapability cap = heldStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) {
            return;
        }
        
        // 渲染吸收箱边框（绿色系跑马灯）
        if (cap.isBound()) {
            BlockPos chestPos = cap.getTerminalPos();
            int chestDim = cap.getTerminalDimension();
            
            // 检查维度是否匹配
            if (player.dimension == chestDim) {
                // 检查距离限制
                double distance = player.getDistanceSq(chestPos);
                if (distance <= MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
                    // 渲染绿色系边框
                    renderBoundingBoxGreen(chestPos, event.getPartialTicks(), player);
                }
            }
        }
        
        // 渲染垃圾箱边框（红色系跑马灯）
        if (cap.isTrashCanBound()) {
            BlockPos trashPos = cap.getTrashCanPos();
            int trashDim = cap.getTrashCanDimension();
            
            // 检查维度是否匹配
            if (player.dimension == trashDim) {
                // 检查距离限制
                double distance = player.getDistanceSq(trashPos);
                if (distance <= MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
                    // 渲染红色系边框
                    renderBoundingBoxRed(trashPos, event.getPartialTicks(), player);
                }
            }
        }
    }

    /**
     * 渲染吸收箱边框，使用绿色系跑马灯效果
     */
    private void renderBoundingBoxGreen(BlockPos pos, float partialTicks, EntityPlayer player) {
        // 获取玩家视角偏移（使用插值以实现平滑渲染）
        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 应用OpenGL状态
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.glLineWidth(3.0F);

        // 设置平移（相对于玩家位置）
        buffer.setTranslation(-playerX, -playerY, -playerZ);

        // 计算时间因子 - 绿色系跑马灯
        long time = System.currentTimeMillis();
        float timeOffset = (time % 2000) / 2000.0F;

        // 绘制多层边框，创造发光效果
        for (int layer = 0; layer < 4; layer++) {
            // 绿色系：色相在 0.20-0.45 之间变化（黄绿色到青绿色）
            // 使用更复杂的波形创造更丰富的颜色变化
            float hueBase = 0.33F; // 纯绿色
            float hueVariation1 = (float)Math.sin((timeOffset + layer * 0.15F) * Math.PI * 2) * 0.12F;
            float hueVariation2 = (float)Math.sin((timeOffset * 1.5F + layer * 0.3F) * Math.PI * 2) * 0.05F;
            float hue = hueBase + hueVariation1 + hueVariation2;
            // 限制在绿色系范围内 (0.20 - 0.45)
            hue = Math.max(0.20F, Math.min(0.45F, hue));
            
            // 饱和度也在变化，创造更丰富的视觉效果
            float satBase = 0.85F;
            float satVariation = (float)Math.sin((timeOffset * 0.8F + layer * 0.25F) * Math.PI * 2) * 0.15F;
            float saturation = satBase + satVariation;
            
            // 亮度也有轻微变化
            float brightBase = 0.95F;
            float brightVariation = (float)Math.sin((timeOffset * 1.2F + layer * 0.2F) * Math.PI * 2) * 0.08F;
            float brightness = brightBase + brightVariation;
            
            int rgb = getRGBFromHSB(hue, saturation, brightness);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            
            // 外层更透明
            int alpha = 255 - (layer * 50);

            // 开始绘制
            buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            
            // 每层稍微扩大一点
            float expansion = layer * 0.015F;
            renderBlockBoundingWithExpansion(buffer, pos, red, green, blue, alpha, expansion);
            
            tessellator.draw();
        }

        // 重置平移
        buffer.setTranslation(0, 0, 0);

        // 清理OpenGL状态
        GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
    }

    /**
     * 渲染垃圾箱边框，使用红色系跑马灯效果
     */
    private void renderBoundingBoxRed(BlockPos pos, float partialTicks, EntityPlayer player) {
        // 获取玩家视角偏移（使用插值以实现平滑渲染）
        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 应用OpenGL状态
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.glLineWidth(3.0F);

        // 设置平移（相对于玩家位置）
        buffer.setTranslation(-playerX, -playerY, -playerZ);

        // 计算时间因子 - 红色系跑马灯
        long time = System.currentTimeMillis();
        float timeOffset = (time % 2000) / 2000.0F;

        // 绘制多层边框，创造发光效果
        for (int layer = 0; layer < 4; layer++) {
            // 红色系：色相在 0.92-0.08 之间变化（深红色到橙红色）
            // 使用更复杂的波形创造更丰富的颜色变化
            float hueBase = 0.0F; // 纯红色
            float hueVariation1 = (float)Math.sin((timeOffset + layer * 0.15F) * Math.PI * 2) * 0.06F;
            float hueVariation2 = (float)Math.sin((timeOffset * 1.3F + layer * 0.25F) * Math.PI * 2) * 0.03F;
            float hue = hueBase + hueVariation1 + hueVariation2;
            // 处理跨越0/1边界的情况，限制在红色系范围内 (-0.08 到 0.08)
            if (hue > 0.5F) hue -= 1.0F;
            hue = Math.max(-0.08F, Math.min(0.08F, hue));
            if (hue < 0) hue += 1.0F;
            
            // 饱和度也在变化，创造更丰富的视觉效果
            float satBase = 0.9F;
            float satVariation = (float)Math.sin((timeOffset * 0.7F + layer * 0.3F) * Math.PI * 2) * 0.1F;
            float saturation = satBase + satVariation;
            
            // 亮度也有轻微变化
            float brightBase = 0.95F;
            float brightVariation = (float)Math.sin((timeOffset * 1.1F + layer * 0.2F) * Math.PI * 2) * 0.08F;
            float brightness = brightBase + brightVariation;
            
            int rgb = getRGBFromHSB(hue, saturation, brightness);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            
            // 外层更透明
            int alpha = 255 - (layer * 50);

            // 开始绘制
            buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            
            // 每层稍微扩大一点
            float expansion = layer * 0.015F;
            renderBlockBoundingWithExpansion(buffer, pos, red, green, blue, alpha, expansion);
            
            tessellator.draw();
        }

        // 重置平移
        buffer.setTranslation(0, 0, 0);

        // 清理OpenGL状态
        GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
    }

    /**
     * 从HSB颜色空间转换到RGB颜色空间
     * 避免使用java.awt.Color，确保在所有环境中都能正常工作
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
     * 渲染方块边界框（带扩展效果）
     * 完全参考XRay的Utils.renderBlockBounding方法，增加expansion参数用于光晕效果
     */
    private void renderBlockBoundingWithExpansion(BufferBuilder buffer, BlockPos pos, int red, int green, int blue, int opacity, float expansion) {
        final float size = 1.0f + expansion;
        final float offset = -expansion / 2.0f; // 居中扩展
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // TOP (顶面4条边)
        buffer.pos(x + offset, y + size, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + offset).color(red, green, blue, opacity).endVertex();

        // BOTTOM (底面4条边)
        buffer.pos(x + size, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + offset, z + offset).color(red, green, blue, opacity).endVertex();

        // Edge 1 (竖边1)
        buffer.pos(x + size, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();

        // Edge 2 (竖边2)
        buffer.pos(x + size, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + offset).color(red, green, blue, opacity).endVertex();

        // Edge 3 (竖边3)
        buffer.pos(x + offset, y + offset, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + size).color(red, green, blue, opacity).endVertex();

        // Edge 4 (竖边4)
        buffer.pos(x + offset, y + offset, z + offset).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + offset, y + size, z + offset).color(red, green, blue, opacity).endVertex();
    }

    /**
     * 渲染方块边界框（原始版本，保留用于兼容）
     * 完全参考XRay的Utils.renderBlockBounding方法
     */
    private void renderBlockBounding(BufferBuilder buffer, BlockPos pos, int red, int green, int blue, int opacity) {
        renderBlockBoundingWithExpansion(buffer, pos, red, green, blue, opacity, 0.0f);
    }
}
