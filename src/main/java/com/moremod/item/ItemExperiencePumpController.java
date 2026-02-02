package com.moremod.item;

import com.moremod.capability.IExperiencePumpCapability;
import com.moremod.rsring.RsRingMod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class ItemExperiencePumpController extends Item {

    public static final String CONTROLLER_TAG = "ControllerData";

    public ItemExperiencePumpController() {
        super();
        setTranslationKey("rsring.experience_pump_controller");
        setRegistryName(new ResourceLocation("rsring", "experience_pump_controller"));
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    /**
     * 获取控制器的配置数据
     */
    public static NBTTagCompound getControllerData(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return null;
        }
        if (!stack.getTagCompound().hasKey(CONTROLLER_TAG)) {
            return null;
        }
        return stack.getTagCompound().getCompoundTag(CONTROLLER_TAG);
    }

    /**
     * 设置控制器的配置数据
     */
    public static void setControllerData(ItemStack stack, int mode, int retainLevel, boolean useForMending) {
        if (stack.isEmpty()) return;
        
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("mode", mode);
        data.setInteger("retainLevel", retainLevel);
        data.setBoolean("mending", useForMending);
        
        stack.getTagCompound().setTag(CONTROLLER_TAG, data);
    }

    /**
     * 获取控制器的模式
     */
    public static int getMode(ItemStack stack) {
        NBTTagCompound data = getControllerData(stack);
        return data != null ? data.getInteger("mode") : IExperiencePumpCapability.MODE_OFF;
    }

    /**
     * 获取控制器的保留等级
     */
    public static int getRetainLevel(ItemStack stack) {
        NBTTagCompound data = getControllerData(stack);
        return data != null && data.hasKey("retainLevel") ? data.getInteger("retainLevel") : 10;
    }

    /**
     * 获取控制器的修补开关
     */
    public static boolean isUseForMending(ItemStack stack) {
        NBTTagCompound data = getControllerData(stack);
        return data != null && data.getBoolean("mending");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "经验泵控制器");
        tooltip.add(TextFormatting.GRAY + "配合经验储罐使用");
        tooltip.add(TextFormatting.AQUA + "放在背包即可生效");
        
        // 显示控制器当前配置
        NBTTagCompound data = getControllerData(stack);
        if (data != null) {
            int mode = data.getInteger("mode");
            int retainLevel = data.hasKey("retainLevel") ? data.getInteger("retainLevel") : 10;
            boolean mending = data.getBoolean("mending");
            
            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "当前配置:");
            tooltip.add(TextFormatting.GRAY + "  · 模式: " + getModeText(mode));
            tooltip.add(TextFormatting.GRAY + "  · 保留等级: " + TextFormatting.AQUA + retainLevel);
            tooltip.add(TextFormatting.GRAY + "  · 修补: " + (mending ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭"));
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "功能介绍:");
            tooltip.add(TextFormatting.GRAY + "  · 控制经验储罐泵入/泵出");
            tooltip.add(TextFormatting.GRAY + "  · 设置保留等级");
            tooltip.add(TextFormatting.GRAY + "  · 经验修补开关");
            tooltip.add(TextFormatting.GRAY + "  · 配置存储在控制器上");
            tooltip.add(TextFormatting.GOLD + "使用方法:");
            tooltip.add(TextFormatting.GRAY + "  · 右键打开控制界面");
            tooltip.add(TextFormatting.GRAY + "  · 放在背包中自动生效");
            tooltip.add(TextFormatting.GRAY + "  · 切换控制器会应用不同配置");
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " + TextFormatting.YELLOW + "Shift" + TextFormatting.DARK_GRAY + " 查看详细信息");
        }
    }

    private String getModeText(int mode) {
        switch (mode) {
            case IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER: return TextFormatting.AQUA + "抽→罐";
            case IExperiencePumpCapability.MODE_PUMP_TO_PLAYER: return TextFormatting.GOLD + "罐→人";
            default: return TextFormatting.GRAY + "关闭";
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (world.isRemote) {
            RsRingMod.proxy.openExperiencePumpControllerGui(stack, hand);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}