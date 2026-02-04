package com.moremod.client;

import com.moremod.capability.IRsRingCapability;
import com.moremod.capability.RsRingCapability;
import com.moremod.item.ItemAbsorbRing;
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
        
        if (!cap.isBound()) {
            return; // 未绑定则不渲染
        }

        // 获取绑定的箱子位置
        BlockPos chestPos = cap.getTerminalPos();
        int chestDim = cap.getTerminalDimension();
        
        // 检查维度是否匹配
        if (player.dimension != chestDim) {
            return; // 不同维度不渲染
        }

        // 检查距离限制
        double distance = player.getDistanceSq(chestPos);
        if (distance > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
            return; // 超出渲染距离
        }

        // 渲染边框
        renderBoundingBox(chestPos, event.getPartialTicks(), player);
    }

    /**
     * 渲染方块边框，使用炫酷的RGB彩色循环效果
     * 参考XRay的渲染方法，增强视觉效果
     */
    private void renderBoundingBox(BlockPos pos, float partialTicks, EntityPlayer player) {
        // 获取玩家视角偏移（使用插值以实现平滑渲染）
        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 应用OpenGL状态（参考XRay的Profile.BLOCKS.apply()）
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.glLineWidth(3.0F); // 加粗线条

        // 设置平移（相对于玩家位置）
        buffer.setTranslation(-playerX, -playerY, -playerZ);

        // 计算时间因子
        long time = System.currentTimeMillis();
        float timeOffset = (time % 2000) / 2000.0F; // 2秒一个循环，更快的变化

        // 绘制多层边框，创造发光效果
        for (int layer = 0; layer < 3; layer++) {
            // 每层使用不同的颜色偏移，创造彩虹流动效果
            float hueOffset = (layer * 0.15F + timeOffset) % 1.0F;
            int rgb = getRGBFromHSB(hueOffset, 1.0F, 1.0F);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            
            // 外层更透明，创造发光扩散效果
            int alpha = 255 - (layer * 60); // 第0层=255, 第1层=195, 第2层=135

            // 开始绘制
            buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            
            // 每层稍微扩大一点，创造光晕效果
            float expansion = layer * 0.02F;
            renderBlockBoundingWithExpansion(buffer, pos, red, green, blue, alpha, expansion);
            
            tessellator.draw();
        }

        // 重置平移
        buffer.setTranslation(0, 0, 0);

        // 清理OpenGL状态（参考XRay的Profile.BLOCKS.clean()）
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
