package com.moremod.rsring;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.moremod.capability.IExperiencePumpCapability;
import com.moremod.capability.ExperiencePumpCapability;
import com.moremod.capability.IRsRingCapability;
import com.moremod.capability.RsRingCapability;
import com.moremod.event.CommonEventHandler;
import com.moremod.event.CraftingExperiencePumpController;
import com.moremod.item.ItemAbsorbRing;
import com.moremod.item.ItemExperiencePump;
import com.moremod.item.ItemExperienceTank100;
import com.moremod.item.ItemExperienceTank500;
import com.moremod.item.ItemExperienceTank1000;
import com.moremod.item.ItemExperienceTank2000;
import com.moremod.crafting.CustomTankRecipes;
import com.moremod.network.PacketPumpAction;
import com.moremod.network.PacketPumpData;
import com.moremod.proxy.CommonProxy;
import com.moremod.config.RsRingConfig;
import com.moremod.config.ExperienceTankConfig;
import com.moremod.config.GeneralConfig;
import com.moremod.config.NetworkConfig;
import net.minecraftforge.fml.common.SidedProxy;
import com.moremod.experience.InventoryChangeHandler;
import com.moremod.experience.ExperienceTankManager;

@Mod(modid = RsRingMod.MODID, name = RsRingMod.NAME, version = RsRingMod.VERSION, guiFactory = "com.moremod.client.GuiFactory")
public class RsRingMod
{
    public static final String MODID = "rsring";
    public static final String NAME = "RS Rings and Tanks";
    public static final String VERSION = "1.3";

    @Mod.Instance(MODID)
    public static RsRingMod instance;

    private static Logger logger;

    @SidedProxy(serverSide = "com.moremod.proxy.CommonProxy", clientSide = "com.moremod.proxy.ClientProxy")
    public static CommonProxy proxy;

    public static ItemAbsorbRing absorbRing;
    public static ItemExperiencePump experiencePump;
    public static com.moremod.item.ItemExperiencePumpController experiencePumpController;
    public static ItemExperienceTank100 experienceTank100;
    public static ItemExperienceTank500 experienceTank500;
    public static ItemExperienceTank1000 experienceTank1000;
    public static ItemExperienceTank2000 experienceTank2000;
    public static SimpleNetworkWrapper network;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        // 配置已经在 RsRingConfig 类中通过 @Config 注解设置，不需要手动创建 Configuration 对象

        // Register absorb ring (replaces old chest ring)
        absorbRing = new ItemAbsorbRing();
        ForgeRegistries.ITEMS.register(absorbRing);
        experiencePump = new ItemExperiencePump();
        ForgeRegistries.ITEMS.register(experiencePump);
        experiencePumpController = new com.moremod.item.ItemExperiencePumpController();
        ForgeRegistries.ITEMS.register(experiencePumpController);
        
        // Register custom experience tanks
        experienceTank100 = new ItemExperienceTank100();
        ForgeRegistries.ITEMS.register(experienceTank100);
        experienceTank500 = new ItemExperienceTank500();
        ForgeRegistries.ITEMS.register(experienceTank500);
        experienceTank1000 = new ItemExperienceTank1000();
        ForgeRegistries.ITEMS.register(experienceTank1000);
        experienceTank2000 = new ItemExperienceTank2000();
        ForgeRegistries.ITEMS.register(experienceTank2000);

        CapabilityManager.INSTANCE.register(IExperiencePumpCapability.class, new ExperiencePumpCapability.Storage(), ExperiencePumpCapability::new);
        CapabilityManager.INSTANCE.register(IRsRingCapability.class, new RsRingCapability.RsRingStorage(), RsRingCapability::new);

        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(PacketPumpAction.Handler.class, PacketPumpAction.class, 1, Side.SERVER);
        network.registerMessage(PacketPumpData.Handler.class, PacketPumpData.class, 2, Side.CLIENT);
        network.registerMessage(com.moremod.network.PacketToggleRsRing.Handler.class, com.moremod.network.PacketToggleRsRing.class, 3, Side.SERVER);
        // Register packet for syncing ring filter from client -> server
        network.registerMessage(com.moremod.network.PacketSyncRingFilter.Handler.class, com.moremod.network.PacketSyncRingFilter.class, 4, Side.SERVER);
        // Register packet for syncing modified tanks to client (Baubles/inventory)
        network.registerMessage(com.moremod.network.PacketSyncTankSlots.Handler.class, com.moremod.network.PacketSyncTankSlots.class, 5, Side.CLIENT);
        // Register packet for opening ring filter GUI
        network.registerMessage(com.moremod.network.PacketOpenRingGui.Handler.class, com.moremod.network.PacketOpenRingGui.class, 6, Side.SERVER);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(new CommonEventHandler());

        // 注册 GUI 处理器
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new com.moremod.client.GuiHandler());

        // 初始化经验系统基础设施
        InventoryChangeHandler.initialize();
        ExperienceTankManager.initialize();

        // 注册经验泵控制器的合成配方
        new CraftingExperiencePumpController().registerRecipes();
        
        // 注册自定义储罐的合成配方
        CustomTankRecipes.registerRecipes();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // 经验泵加入矿辞，供升级配方（经验泵+附魔之瓶+末影珍珠）匹配
        OreDictionary.registerOre("experience_pump", new ItemStack(experiencePump));
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        // 服务器启动事件
    }
}
