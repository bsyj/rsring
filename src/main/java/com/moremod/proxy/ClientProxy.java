package com.moremod.proxy;

import com.moremod.capability.ExperiencePumpCapability;
import com.moremod.item.ItemChestRing;
import com.moremod.item.ItemExperiencePump;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import com.moremod.rsring.RsRingMod;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import com.moremod.capability.IRsRingCapability;
import com.moremod.capability.RsRingCapability;
import com.moremod.event.CommonEventHandler;

@Mod.EventBusSubscriber(modid = RsRingMod.MODID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy {

    // 经验储罐不再有GUI，此方法已废弃
    @Override
    public void openExperiencePumpGui(ItemStack stack, EnumHand hand) {
        // 不再打开GUI
    }

    public void openExperiencePumpControllerGui(ItemStack stack, EnumHand hand) {
        Minecraft.getMinecraft().displayGuiScreen(new com.moremod.client.GuiExperiencePumpController(stack, hand));
    }

    // 打开箱子戒指的黑白名单GUI
    @Override
    public void openChestRingGui(ItemStack stack) {
        Minecraft.getMinecraft().displayGuiScreen(new com.moremod.client.GuiRingFilter(stack, "箱子戒指 - 黑白名单"));
    }

    @Override
    public void handleToggleRsRing(EntityPlayerMP player) {
        // 在服务器上处理切换逻辑
        ItemStack ringStack = CommonEventHandler.findAnyRingForToggle(player);
        if (!ringStack.isEmpty() && ringStack.getItem() instanceof ItemChestRing) {
            IRsRingCapability capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
            if (capability != null) {
                capability.setEnabled(!capability.isEnabled());
                RsRingCapability.syncCapabilityToStack(ringStack, capability);

                // 发送反馈消息给玩家
                String status = capability.isEnabled() ? "启用" : "禁用";
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.GREEN + "箱子戒指已" + status));
            }
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.chestRing, 0, new ModelResourceLocation(RsRingMod.chestRing.getRegistryName(), "inventory"));

        // 经验储罐：根据经验百分比显示不同纹理（0%, 25%, 50%, 75%, 100%）
        ModelLoader.setCustomMeshDefinition(RsRingMod.experiencePump, stack -> {
            int fillLevel = ItemExperiencePump.getXpFillLevel(stack);
            String variant;
            switch (fillLevel) {
                case 0: variant = "experience_pump_0"; break;    // 0% 空
                case 1: variant = "experience_pump_25"; break;   // 1-25%
                case 2: variant = "experience_pump_50"; break;   // 26-50%
                case 3: variant = "experience_pump_75"; break;   // 51-75%
                case 4: variant = "experience_pump_100"; break;  // 76-100% 满
                default: variant = "experience_pump_0"; break;
            }
            return new ModelResourceLocation(new ResourceLocation("rsring", variant), "inventory");
        });
        
        // 注册所有经验储罐变体
        ModelLoader.registerItemVariants(RsRingMod.experiencePump,
            new ResourceLocation("rsring", "experience_pump_0"),
            new ResourceLocation("rsring", "experience_pump_25"),
            new ResourceLocation("rsring", "experience_pump_50"),
            new ResourceLocation("rsring", "experience_pump_75"),
            new ResourceLocation("rsring", "experience_pump_100"));

        // 经验泵控制器（使用红石材质）
        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.experiencePumpController, 0, new ModelResourceLocation(new ResourceLocation("minecraft", "redstone"), "inventory"));
    }
}