package com.rsring.item;

import com.rsring.capability.ExperiencePumpCapability;
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
 * 10000级经验储罐 - 具有超大容量的经验存储设备
 */
@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemExperienceTank10000 extends ItemExperiencePump implements IBauble {

    // 10000级所需经验：448,377,220 XP
    private static final int DEFAULT_CAPACITY = 448377220;

    public ItemExperienceTank10000() {
        super("experience_tank_10000", "rsring.experience_tank_10000");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 优先从 Capability 读取数据，确保获取最新的经验值
        int xp = getXpFromCapabilityOrNBT(stack);
        int max = DEFAULT_CAPACITY;
        double storedLevels = com.rsring.util.XpHelper.getLevelsForExperience(xp);

        tooltip.add(TextFormatting.GRAY + "玩家等级: " + TextFormatting.AQUA + "10000级");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "已存等级: " + TextFormatting.YELLOW + String.format("%.1f", storedLevels));
        tooltip.add(TextFormatting.GRAY + "经验: " + TextFormatting.GREEN + xp + TextFormatting.GRAY + " / " + max + " XP");

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
            tooltip.add(TextFormatting.GRAY + "  · 抽取速率: " + TextFormatting.AQUA + com.rsring.config.ExperienceTankConfig.tank.xpExtractionRate + " XP/刻");
            tooltip.add(TextFormatting.GRAY + "  · 抽取范围: " + TextFormatting.AQUA + com.rsring.config.ExperienceTankConfig.tank.xpExtractionRange + " 格");
            tooltip.add(TextFormatting.GRAY + "  · 溢出保护: " + (com.rsring.config.ExperienceTankConfig.tank.enableOverflowBottles ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭"));
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            // 服务端从 Capability 或 NBT 读取
            int xpStored = getXpFromCapabilityOrNBT(stack);
            int maxXp = DEFAULT_CAPACITY;
            double storedLevels = com.rsring.util.XpHelper.getLevelsForExperience(xpStored);

            String message = TextFormatting.LIGHT_PURPLE + "已存等级: " + TextFormatting.YELLOW + String.format("%.1f", storedLevels) +
                           TextFormatting.GRAY + " - " +
                           TextFormatting.GREEN + xpStored +
                           TextFormatting.GRAY + " / " +
                           TextFormatting.YELLOW + maxXp +
                           TextFormatting.GRAY + " XP";

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

        if (data == null) {
            data = new net.minecraft.nbt.NBTTagCompound();
            data.setInteger("capacityLevels", 28);
            data.setInteger("fixedMaxXp", DEFAULT_CAPACITY);
            data.setInteger("xp", 0);
            data.setInteger("mode", com.rsring.config.ExperienceTankConfig.tank.defaultPumpMode);
            data.setInteger("retainLevel", com.rsring.config.ExperienceTankConfig.tank.defaultRetainLevel);
            data.setBoolean("mending", com.rsring.config.ExperienceTankConfig.tank.defaultMendingMode);
        } else if (!data.hasKey("fixedMaxXp")) {
            data.setInteger("fixedMaxXp", DEFAULT_CAPACITY);
        }

        provider.initFromNBT(data);
        return provider;
    }

}
