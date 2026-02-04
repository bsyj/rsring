package com.rsring.proxy;

import com.rsring.item.ItemExperiencePump;
import com.rsring.rsring.RsRingMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = RsRingMod.MODID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        super.preInit();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.rsring.client.RingBoundBoxRenderer());
    }

    @Override
    public void openExperiencePumpGui(ItemStack stack, EnumHand hand) {
        // GUI no longer used
    }

    @Override
    public void openExperiencePumpControllerGui(ItemStack stack, EnumHand hand) {
        Minecraft.getMinecraft().displayGuiScreen(new com.rsring.client.GuiExperiencePumpController(stack, hand));
    }

    @Override
    public void openAbsorbRingGui(ItemStack stack) {
        RsRingMod.network.sendToServer(new com.rsring.network.PacketOpenRingGui());
    }

    @Override
    public void handleToggleRsRing(EntityPlayerMP player) {
        super.handleToggleRsRing(player);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.absorbRing, 0, new ModelResourceLocation(RsRingMod.absorbRing.getRegistryName(), "inventory"));

        ModelLoader.setCustomMeshDefinition(RsRingMod.experiencePump, stack -> {
            int fillLevel = ItemExperiencePump.getXpFillLevel(stack);
            String variant;
            switch (fillLevel) {
                case 0: variant = "experience_pump_0"; break;
                case 1: variant = "experience_pump_25"; break;
                case 2: variant = "experience_pump_50"; break;
                case 3: variant = "experience_pump_75"; break;
                case 4: variant = "experience_pump_100"; break;
                default: variant = "experience_pump_0"; break;
            }
            return new ModelResourceLocation(new ResourceLocation("rsring", variant), "inventory");
        });

        ModelLoader.registerItemVariants(RsRingMod.experiencePump,
            new ResourceLocation("rsring", "experience_pump_0"),
            new ResourceLocation("rsring", "experience_pump_25"),
            new ResourceLocation("rsring", "experience_pump_50"),
            new ResourceLocation("rsring", "experience_pump_75"),
            new ResourceLocation("rsring", "experience_pump_100"));

        ModelLoader.setCustomModelResourceLocation(
            RsRingMod.experiencePumpController, 0, new ModelResourceLocation(RsRingMod.experiencePumpController.getRegistryName(), "inventory"));

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
