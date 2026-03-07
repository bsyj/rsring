package com.rsring.event;

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.item.ItemAbsorbRing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 铁砧彩蛋事件处理器
 * 
 * 功能：将戒指与樱花维度物品合成，获得彩蛋戒指
 * 彩蛋戒指在饰品栏时增加5点幸运值
 */
public class AnvilEasterEggHandler {
    
    // 樱花模组的维度物品ID
    private static final String SAKURA_DIMENSION_ITEM = "sakura:sakuradimension";
    
    /**
     * 监听铁砧更新事件，检测合成配方
     */
    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();  // 左侧物品
        ItemStack right = event.getRight(); // 右侧物品
        
        if (left.isEmpty()) return;
        
        // 检查左侧是否为吸收戒指
        if (!(left.getItem() instanceof ItemAbsorbRing)) return;
        
        // 获取戒指能力
        IRsRingCapability cap = left.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) return;
        
        // 如果已经是彩蛋戒指，不再处理合成
        if (cap.isEasterEgg()) return;
        
        // 右侧必须是非空物品才能继续合成
        if (right.isEmpty()) return;
        
        // 检查右侧是否为樱花维度物品
        ResourceLocation rightRegName = right.getItem().getRegistryName();
        if (rightRegName == null) return;
        if (!SAKURA_DIMENSION_ITEM.equals(rightRegName.toString())) return;
        
        // 创建输出物品（复制原戒指）
        ItemStack output = left.copy();

        // 清除自定义名称，让彩蛋物品显示默认名称
        // 需要移除display.Name标签
        if (output.hasTagCompound() && output.getTagCompound().hasKey("display")) {
            net.minecraft.nbt.NBTTagCompound display = output.getTagCompound().getCompoundTag("display");
            if (display.hasKey("Name")) {
                display.removeTag("Name");
                // 如果display标签没有内容了，则移除整个display标签
                if (display.getKeySet().isEmpty()) {
                    output.getTagCompound().removeTag("display");
                }
            }
        }

        IRsRingCapability outputCap = output.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);

        if (outputCap != null) {
            // 设置为彩蛋戒指
            outputCap.setEasterEgg(true);
            RsRingCapability.syncCapabilityToStack(output, outputCap);
        }

        // 添加标记，表示这是刚刚觉醒的彩蛋（用于区分改名操作）
        if (output.getTagCompound() == null) {
            output.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }
        output.getTagCompound().setBoolean("RsRingEasterEggNew", true);

        // 设置输出
        event.setOutput(output);
        // 设置经验消耗
        event.setCost(10);
        // 设置材料消耗数量
        event.setMaterialCost(1);
    }
    
    /**
     * 监听铁砧修复完成事件，发送提示消息
     */
    @SubscribeEvent
    public void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack output = event.getItemResult();

        if (output.isEmpty()) return;
        if (!(output.getItem() instanceof ItemAbsorbRing)) return;

        IRsRingCapability cap = output.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) return;

        // 检查是否是刚刚觉醒的彩蛋（通过NBT标记判断）
        boolean isNewEasterEgg = output.hasTagCompound() &&
            output.getTagCompound().hasKey("RsRingEasterEggNew") &&
            output.getTagCompound().getBoolean("RsRingEasterEggNew");

        // 如果是彩蛋戒指且是刚刚觉醒的，发送提示
        if (cap.isEasterEgg() && isNewEasterEgg) {
            EntityPlayer player = event.getEntityPlayer();
            if (player != null && !player.world.isRemote) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "=== 彩蛋发现! ==="
                ));
                player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "至尊狂傲暴龙灭杀战神 " +
                    TextFormatting.GRAY + "已觉醒!"
                ));
                player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "装备时获得 +5 幸运值"
                ));

                // 播放升级音效
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5F, 1.0F);

                // 清除标记，防止重复触发
                output.getTagCompound().removeTag("RsRingEasterEggNew");
            }
        }
    }
    
}
