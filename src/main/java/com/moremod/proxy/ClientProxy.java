package com.moremod.proxy;

import com.moremod.capability.ExperiencePumpCapability;
import com.moremod.client.ContainerRingFilter;
import com.moremod.client.GuiRingFilterContainer;
import com.moremod.item.ItemAbsorbRing;
import com.moremod.item.ItemExperiencePump;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
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

    @Override
    public void preInit() {
        super.preInit();
        // 注册戒指边框渲染器
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.moremod.client.RingBoundBoxRenderer());
    }

    // 经验储罐不再有GUI，此方法已废弃
    @Override
    public void openExperiencePumpGui(ItemStack stack, EnumHand hand) {
        // 不再打开GUI
    }

    public void openExperiencePumpControllerGui(ItemStack stack, EnumHand hand) {
        Minecraft.getMinecraft().displayGuiScreen(new com.moremod.client.GuiExperiencePumpController(stack, hand));
    }

    // 打开物品吸收戒指的黑白名单GUI - 通过发送数据包到服务器来正确同步Container
    @Override
    public void openAbsorbRingGui(ItemStack stack) {
        // 发送数据包到服务器，由服务器调用 player.openGui() 来确保 Container 同步
        RsRingMod.network.sendToServer(new com.moremod.network.PacketOpenRingGui());
    }

    @Override
    public void handleToggleRsRing(EntityPlayerMP player) {
        // 在服务器线程上处理切换逻辑
        player.getServerWorld().addScheduledTask(() -> {
            // 使用ItemLocationTracker查找戒指并追踪其位置
            com.moremod.util.ItemLocationTracker tracker = 
                com.moremod.util.ItemLocationTracker.findItem(player, ItemAbsorbRing.class);
            
            if (tracker == null) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.RED + "未找到物品吸收戒指！"));
                return;
            }
            
            ItemStack ringStack = tracker.getItem();
            IRsRingCapability capability = ringStack.getCapability(
                RsRingCapability.RS_RING_CAPABILITY, null);
            
            if (capability != null) {
                // 切换状态
                capability.setEnabled(!capability.isEnabled());
                RsRingCapability.syncCapabilityToStack(ringStack, capability);
                
                // 关键：将修改后的ItemStack写回原位置（解决Baubles副本问题）
                tracker.syncBack(player);
                
                // 发送反馈消息给玩家
                String status = capability.isEnabled() ? "启用" : "禁用";
                String location = getLocationDisplayName(tracker.getLocationType());
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.GREEN + "物品吸收戒指已" + status + 
                    net.minecraft.util.text.TextFormatting.GRAY + " (位置: " + location + ")"));
            }
        });
    }
    
    /**
     * 获取位置的显示名称
     */
    private String getLocationDisplayName(com.moremod.util.ItemLocationTracker.LocationType locationType) {
        switch (locationType) {
            case MAIN_HAND: return "主手";
            case OFF_HAND: return "副手";
            case BAUBLES: return "饰品栏";
            case PLAYER_INVENTORY: return "背包";
            default: return "未知";
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        // Register model for absorb ring
        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.absorbRing, 0, new ModelResourceLocation(RsRingMod.absorbRing.getRegistryName(), "inventory"));

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

        // 经验泵控制器（使用自定义材质）
        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.experiencePumpController, 0, new ModelResourceLocation(RsRingMod.experiencePumpController.getRegistryName(), "inventory"));
        
        // 注册特殊经验储罐模型
        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.experienceTank100, 0, new ModelResourceLocation(RsRingMod.experienceTank100.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.experienceTank500, 0, new ModelResourceLocation(RsRingMod.experienceTank500.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.experienceTank1000, 0, new ModelResourceLocation(RsRingMod.experienceTank1000.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.experienceTank2000, 0, new ModelResourceLocation(RsRingMod.experienceTank2000.getRegistryName(), "inventory"));
    }
}