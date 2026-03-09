package com.rsring.item;

import com.rsring.capability.ExperiencePumpCapability;
import com.rsring.capability.IExperiencePumpCapability;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
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
import baubles.api.BaubleType;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.UUID;

/**
 * 10000级经验储罐 - 具有超大容量的经验存储设备
 */
@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemExperienceTank10000 extends ItemExperiencePump implements IBauble {

    // 10000级所需经验：448,377,220 XP
    private static final int DEFAULT_CAPACITY = 448377220;

    // 彩蛋储罐幸运属性基础UUID（用于生成槽位相关UUID）
    private static final UUID LUCK_ATTRIBUTE_BASE_UUID = UUID.fromString("f3b4a5c7-9d0e-5f1b-2c3d-4e5f6a7b8c9d");
    private static final String LUCK_ATTRIBUTE_NAME = "rsring.easter_egg_tank.luck";

    public ItemExperienceTank10000() {
        super("experience_tank_10000", "rsring.experience_tank_10000");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        // 彩蛋储罐显示发光附魔效果
        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        return cap != null && cap.isEasterEgg();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap != null && cap.isEasterEgg()) {
            // 彩蛋储罐始终显示彩蛋名称，忽略重命名
            return TextFormatting.BLUE + "苍穹狂傲魔龙弑神战帝";
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);

        // 彩蛋储罐特殊标识
        if (cap != null && cap.isEasterEgg()) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + "[ 彩蛋觉醒 ]");
            tooltip.add(TextFormatting.YELLOW + "  +5 幸运值 (装备时)");
        }

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

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.TRINKET;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer)) return;

        EntityPlayer entityPlayer = (EntityPlayer) player;
        IExperiencePumpCapability capability = itemstack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);

        // 装备彩蛋储罐时添加幸运属性
        if (capability != null && capability.isEasterEgg()) {
            int slot = findBaubleSlot(entityPlayer, itemstack);
            applyLuckAttribute(entityPlayer, slot);
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer)) return;

        EntityPlayer entityPlayer = (EntityPlayer) player;
        IExperiencePumpCapability capability = itemstack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);

        // 卸下彩蛋储罐时移除幸运属性
        if (capability != null && capability.isEasterEgg()) {
            removeLuckAttribute(entityPlayer);
        }
    }

    /**
     * 查找物品在饰品栏中的槽位索引
     */
    private int findBaubleSlot(EntityPlayer player, ItemStack targetStack) {
        if (!com.rsring.util.BaublesHelper.isBaublesLoaded()) return -1;

        Object handler = com.rsring.util.BaublesHelper.getBaublesHandler(player);
        if (handler == null) return -1;

        int size = com.rsring.util.BaublesHelper.getSlots(handler);
        for (int i = 0; i < size; i++) {
            ItemStack stack = com.rsring.util.BaublesHelper.getStackInSlot(handler, i);
            if (stack == targetStack) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 生成槽位相关的UUID
     */
    private UUID getSlotUUID(int slot) {
        // 基于基础UUID和槽位索引生成唯一UUID
        long most = LUCK_ATTRIBUTE_BASE_UUID.getMostSignificantBits();
        long least = LUCK_ATTRIBUTE_BASE_UUID.getLeastSignificantBits() + slot;
        return new UUID(most, least);
    }

    /**
     * 应用幸运属性修饰符
     * @param slot 饰品槽位索引，-1表示使用基础UUID
     */
    private void applyLuckAttribute(EntityPlayer player, int slot) {
        net.minecraft.entity.ai.attributes.IAttributeInstance luckAttribute = player.getEntityAttribute(
            net.minecraft.entity.SharedMonsterAttributes.LUCK);
        if (luckAttribute == null) return;

        UUID uuid = getSlotUUID(slot >= 0 ? slot : 0);

        // 检查是否已存在修饰符
        if (luckAttribute.getModifier(uuid) != null) return;

        // 添加幸运+5修饰符 (0 = ADDITION)
        net.minecraft.entity.ai.attributes.AttributeModifier modifier = new net.minecraft.entity.ai.attributes.AttributeModifier(
            uuid, LUCK_ATTRIBUTE_NAME + "_" + slot, 5.0, 0);
        luckAttribute.applyModifier(modifier);
    }

    /**
     * 移除所有彩蛋储罐的幸运属性修饰符
     */
    private void removeLuckAttribute(EntityPlayer player) {
        net.minecraft.entity.ai.attributes.IAttributeInstance luckAttribute = player.getEntityAttribute(
            net.minecraft.entity.SharedMonsterAttributes.LUCK);
        if (luckAttribute == null) return;

        // 移除所有可能的槽位修饰符（最多50个槽位）
        for (int i = 0; i < 50; i++) {
            UUID uuid = getSlotUUID(i);
            net.minecraft.entity.ai.attributes.AttributeModifier modifier = luckAttribute.getModifier(uuid);
            if (modifier != null) {
                luckAttribute.removeModifier(modifier);
            }
        }
    }

    /**
     * 重新应用所有彩蛋储罐的幸运属性（用于玩家登录时）
     */
    public static void reapplyAllEasterEggLuck(EntityPlayer player) {
        if (player.world.isRemote) return;
        if (!com.rsring.util.BaublesHelper.isBaublesLoaded()) return;

        Object handler = com.rsring.util.BaublesHelper.getBaublesHandler(player);
        if (handler == null) return;

        int size = com.rsring.util.BaublesHelper.getSlots(handler);
        for (int i = 0; i < size; i++) {
            ItemStack stack = com.rsring.util.BaublesHelper.getStackInSlot(handler, i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemExperienceTank10000)) continue;

            IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (cap != null && cap.isEasterEgg()) {
                ItemExperienceTank10000 tank = (ItemExperienceTank10000) stack.getItem();
                tank.applyLuckAttribute(player, i);
            }
        }
    }

}
