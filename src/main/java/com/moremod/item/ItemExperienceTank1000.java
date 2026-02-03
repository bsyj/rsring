package com.moremod.item;

import com.moremod.capability.ExperiencePumpCapability;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import baubles.api.IBauble;
import org.lwjgl.input.Keyboard;

import java.util.List;

/**
 * 1000级经验储罐 - 具有更大容量的经验存储设备
 */
@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemExperienceTank1000 extends ItemExperiencePump implements IBauble {

    // 1000级所需经验：4,339,720 XP
    private static final int DEFAULT_CAPACITY = 4339720;

    public ItemExperienceTank1000() {
        super("experience_tank_1000", "rsring.experience_tank_1000");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        net.minecraft.nbt.NBTTagCompound data = getDataFromNBT(stack);
        if (data == null) {
            // 即使没有NBT数据也要显示基础信息
            tooltip.add(TextFormatting.GRAY + "玩家等级: " + TextFormatting.AQUA + "1000级");
            tooltip.add(TextFormatting.GRAY + "经验: " + TextFormatting.GREEN + "0" + TextFormatting.GRAY + " / " + DEFAULT_CAPACITY + " mb");

            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                tooltip.add("");
                tooltip.add(TextFormatting.GOLD + "功能介绍:");
                tooltip.add(TextFormatting.GRAY + "  · 吸收周围经验球");
                tooltip.add(TextFormatting.GRAY + "  · 存储经验 (需配合经验泵控制器使用)");
                tooltip.add(TextFormatting.GRAY + "  · 自动修复附魔装备");
                tooltip.add(TextFormatting.GOLD + "使用方法:");
                tooltip.add(TextFormatting.GRAY + "  · 与经验泵控制器配合使用");
            } else {
                tooltip.add(TextFormatting.DARK_GRAY + "按住 " + TextFormatting.YELLOW + "Shift" + TextFormatting.DARK_GRAY + " 查看详细信息");
            }
            return;
        }

        int xp = data.getInteger("xp");
        int max = DEFAULT_CAPACITY;

        // 基础信息显示
        tooltip.add(TextFormatting.GRAY + "玩家等级: " + TextFormatting.AQUA + "1000级");
        tooltip.add(TextFormatting.GRAY + "经验: " + TextFormatting.GREEN + xp + TextFormatting.GRAY + " / " + max + " mb");

        // 详细信息（Shift显示）
        boolean showDetail = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (!showDetail) {
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " + TextFormatting.YELLOW + "Shift" + TextFormatting.DARK_GRAY + " 查看详细信息");
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "功能介绍:");
            tooltip.add(TextFormatting.GRAY + "  · 自动吸收周围经验球和经验瓶");
            tooltip.add(TextFormatting.GRAY + "  · 智能经验存储和溢出处理");
            tooltip.add(TextFormatting.GRAY + "  · 自动修复附魔装备 (经验修补)");
            tooltip.add(TextFormatting.GRAY + "  · 与经验泵控制器协同工作");
            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "配置信息:");
            tooltip.add(TextFormatting.GRAY + "  · 抽取速率: " + TextFormatting.AQUA + com.moremod.config.ExperienceTankConfig.tank.xpExtractionRate + " XP/刻");
            tooltip.add(TextFormatting.GRAY + "  · 抽取范围: " + TextFormatting.AQUA + com.moremod.config.ExperienceTankConfig.tank.xpExtractionRange + " 格");
            tooltip.add(TextFormatting.GRAY + "  · 溢出保护: " + (com.moremod.config.ExperienceTankConfig.tank.enableOverflowBottles ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭"));
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // 服务器端：显示储罐容量信息到聊天栏
        if (!world.isRemote) {
            // 从NBT读取数据（确保准确性）
            int xpStored = getXpStoredFromNBT(stack);
            int maxXp = DEFAULT_CAPACITY;

            // 构造消息：1000级储罐 - Y / Z mb
            String message = TextFormatting.AQUA + "1000级储罐 " +
                           TextFormatting.GRAY + "- " +
                           TextFormatting.GREEN + xpStored +
                           TextFormatting.GRAY + " / " +
                           TextFormatting.YELLOW + maxXp +
                           TextFormatting.GRAY + " mb";

            player.sendMessage(new TextComponentString(message));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, net.minecraft.nbt.NBTTagCompound nbt) {
        ExperiencePumpCapability.Provider provider = new ExperiencePumpCapability.Provider();
        net.minecraft.nbt.NBTTagCompound data = nbt;
        if ((data == null || data.getKeySet().isEmpty()) && stack.getTagCompound() != null && stack.getTagCompound().hasKey(XP_TAG))
            data = stack.getTagCompound().getCompoundTag(XP_TAG);

        // 初始化默认值
        if (data == null) {
            data = new net.minecraft.nbt.NBTTagCompound();
            data.setInteger("capacityLevels", 22); // 对应原系统的22级
            data.setInteger("xp", 0);
            data.setInteger("mode", 0);
            data.setInteger("retainLevel", 1);
            data.setBoolean("mending", false);
        }

        provider.initFromNBT(data);
        return provider;
    }

}
