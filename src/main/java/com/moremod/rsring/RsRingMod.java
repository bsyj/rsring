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
import com.moremod.network.PacketPumpAction;
import com.moremod.network.PacketPumpData;
import com.moremod.proxy.CommonProxy;
import com.moremod.config.RsRingConfig;
import net.minecraftforge.fml.common.SidedProxy;
import com.moremod.config.ExperiencePumpConfig;
import com.moremod.experience.InventoryChangeHandler;
import com.moremod.experience.ExperienceTankManager;

@Mod(modid = RsRingMod.MODID, name = RsRingMod.NAME, version = RsRingMod.VERSION)
public class RsRingMod
{
    public static final String MODID = "rsring";
    public static final String NAME = "RS Rings and Tanks";
    public static final String VERSION = "1.0";

    private static Logger logger;

    @SidedProxy(serverSide = "com.moremod.proxy.CommonProxy", clientSide = "com.moremod.proxy.ClientProxy")
    public static CommonProxy proxy;

    public static ItemAbsorbRing absorbRing;
    public static ItemExperiencePump experiencePump;
    public static com.moremod.item.ItemExperiencePumpController experiencePumpController;
    public static SimpleNetworkWrapper network;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        // 加载配置
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();

        // 添加默认黑名单项（经验球）如果配置为空
        if (RsRingConfig.blacklist.itemBlacklist.length == 0) {
            // 这里不需要手动添加，因为已经在配置类中设置了默认值
        }

        if (config.hasChanged()) config.save();

        // Register absorb ring (replaces old chest ring)
        absorbRing = new ItemAbsorbRing();
        ForgeRegistries.ITEMS.register(absorbRing);
        experiencePump = new ItemExperiencePump();
        ForgeRegistries.ITEMS.register(experiencePump);
        experiencePumpController = new com.moremod.item.ItemExperiencePumpController();
        ForgeRegistries.ITEMS.register(experiencePumpController);

        CapabilityManager.INSTANCE.register(IExperiencePumpCapability.class, new ExperiencePumpCapability.Storage(), ExperiencePumpCapability::new);
        CapabilityManager.INSTANCE.register(IRsRingCapability.class, new RsRingCapability.RsRingStorage(), RsRingCapability::new);

        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(PacketPumpAction.Handler.class, PacketPumpAction.class, 1, Side.SERVER);
        network.registerMessage(PacketPumpData.Handler.class, PacketPumpData.class, 2, Side.CLIENT);
        network.registerMessage(com.moremod.network.PacketToggleRsRing.Handler.class, com.moremod.network.PacketToggleRsRing.class, 3, Side.SERVER);
        // Register packet for syncing ring filter from client -> server
        network.registerMessage(com.moremod.network.PacketSyncRingFilter.Handler.class, com.moremod.network.PacketSyncRingFilter.class, 4, Side.SERVER);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(new CommonEventHandler());

        // 初始化经验系统基础设施
        InventoryChangeHandler.initialize();
        ExperienceTankManager.initialize();

        // 注册经验泵控制器的合成配方
        new CraftingExperiencePumpController().registerRecipes();
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
