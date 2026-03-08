package com.rsring.event;

import com.rsring.capability.ExperiencePumpCapability;
import com.rsring.capability.IExperiencePumpCapability;
import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.destroy.DestroyManager;
import com.rsring.item.ItemAbsorbRing;
import com.rsring.item.ItemExperiencePump;
import com.rsring.item.ItemExperiencePumpController;
import com.rsring.network.PacketToggleRsRing;
import com.rsring.rsring.RsRingMod;
import com.rsring.config.ConfigRegistry;
import com.rsring.util.BaublesHelper;
import com.rsring.util.ItemLocationTracker;
import com.rsring.util.XpHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommonEventHandler {

    private static KeyBinding toggleKeyBinding;
    private static final Map<UUID, Integer> lastPlayerXp = new HashMap<>();
    // 左键绑定冷却时间（毫秒）
    private static final long BIND_COOLDOWN_MS = 500;
    private final Map<UUID, Long> lastLeftClickBindTime = new HashMap<>();
    
    // 低电量提醒冷却记录（玩家UUID -> 上次提醒时间戳）
    private static final Map<UUID, Long> lowEnergyWarningCache = new ConcurrentHashMap<>();
    // 低电量提醒检测间隔（tick），每5秒检测一次
    private static final int LOW_ENERGY_CHECK_INTERVAL = 100;
    private int lowEnergyCheckCounter = 0;

    // 缓存清理相关
    private static long lastCacheCleanupTime = 0;
    private static final long CACHE_CLEANUP_INTERVAL_MS = 600000; // 10分钟清理一次

    public CommonEventHandler() {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            registerKeyBindings();
        }
    }

    @SideOnly(Side.CLIENT)
    private void registerKeyBindings() {
        toggleKeyBinding = new KeyBinding("key.rsring.toggle.desc", Keyboard.KEY_K, "key.categories.rsring");
        ClientRegistry.registerKeyBinding(toggleKeyBinding);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (toggleKeyBinding != null && toggleKeyBinding.isPressed()) {
            toggleAbsorbRingFunction();
        }
    }

    @SideOnly(Side.CLIENT)
    private void toggleAbsorbRingFunction() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;
        RsRingMod.network.sendToServer(new PacketToggleRsRing());
    }

    private static ItemStack findHeldRing(EntityPlayer player, Class<? extends Item> ringClass) {
        if (!player.getHeldItemMainhand().isEmpty() && ringClass.isInstance(player.getHeldItemMainhand().getItem())) {
            return player.getHeldItemMainhand();
        }
        if (!player.getHeldItemOffhand().isEmpty() && ringClass.isInstance(player.getHeldItemOffhand().getItem())) {
            return player.getHeldItemOffhand();
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack findAnyRingForToggle(EntityPlayer player) {
        return findRing(player, ItemAbsorbRing.class);
    }

    public static ItemStack findRing(EntityPlayer player, Class<? extends Item> ringClass) {
        if (!player.getHeldItemMainhand().isEmpty() && ringClass.isInstance(player.getHeldItemMainhand().getItem())) {
            return player.getHeldItemMainhand();
        }
        if (!player.getHeldItemOffhand().isEmpty() && ringClass.isInstance(player.getHeldItemOffhand().getItem())) {
            return player.getHeldItemOffhand();
        }
        if (BaublesHelper.isBaublesLoaded()) {
            Object handler = BaublesHelper.getBaublesHandler(player);
            int size = BaublesHelper.getSlots(handler);
            for (int i = 0; i < size; i++) {
                ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) {
                    return stack;
                }
            }
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 查找玩家所有指定类型的戒指（支持多戒指功能）
     * @param player 玩家
     * @param ringClass 戒指类型
     * @return 所有找到的戒指列表
     */
    public static List<ItemStack> findAllRings(EntityPlayer player, Class<? extends Item> ringClass) {
        List<ItemStack> rings = new ArrayList<>();

        // 主手
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && ringClass.isInstance(mainHand.getItem())) {
            rings.add(mainHand);
        }
        // 副手
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && ringClass.isInstance(offHand.getItem())) {
            rings.add(offHand);
        }
        // 饰品栏
        if (BaublesHelper.isBaublesLoaded()) {
            Object handler = BaublesHelper.getBaublesHandler(player);
            int size = BaublesHelper.getSlots(handler);
            for (int i = 0; i < size; i++) {
                ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) {
                    rings.add(stack);
                }
            }
        }
        // 快捷栏和背包（槽位 0-35）
        // 注意：主手物品也在快捷栏中，需要跳过已添加的
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            // 跳过空物品和非戒指
            if (stack.isEmpty() || !ringClass.isInstance(stack.getItem())) {
                continue;
            }
            // 跳过已添加的（避免主手/副手重复）
            if (stack == mainHand || stack == offHand) {
                continue;
            }
            rings.add(stack);
        }
        return rings;
    }

    private ItemStack findExperiencePump(EntityPlayer player) {
        // Prefer empty tanks for filling
        for (EnumHand hand : EnumHand.values()) {
            ItemStack heldStack = player.getHeldItem(hand);
            if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemExperiencePump) {
                IExperiencePumpCapability cap = heldStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                if (cap != null && cap.getXpStored() == 0) {
                    return heldStack;
                }
            }
        }

        if (BaublesHelper.isBaublesLoaded()) {
            Object handler = BaublesHelper.getBaublesHandler(player);
            int size = BaublesHelper.getSlots(handler);
            for (int i = 0; i < size; i++) {
                ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                    IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                    if (cap != null && cap.getXpStored() == 0) {
                        return stack;
                    }
                }
            }
        }

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                if (cap != null && cap.getXpStored() == 0) {
                    return stack;
                }
            }
        }

        // Fallback: return any tank
        for (EnumHand hand : EnumHand.values()) {
            ItemStack heldStack = player.getHeldItem(hand);
            if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemExperiencePump) {
                return heldStack;
            }
        }

        if (BaublesHelper.isBaublesLoaded()) {
            Object handler = BaublesHelper.getBaublesHandler(player);
            int size = BaublesHelper.getSlots(handler);
            for (int i = 0; i < size; i++) {
                ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                    return stack;
                }
            }
        }

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private ItemStack findExperiencePumpController(EntityPlayer player) {
        for (EnumHand hand : EnumHand.values()) {
            ItemStack heldStack = player.getHeldItem(hand);
            if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemExperiencePumpController) {
                return heldStack;
            }
        }

        if (BaublesHelper.isBaublesLoaded()) {
            Object handler = BaublesHelper.getBaublesHandler(player);
            int size = BaublesHelper.getSlots(handler);
            for (int i = 0; i < size; i++) {
                ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePumpController) {
                    return stack;
                }
            }
        }

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePumpController) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private void syncControllerToTank(ItemStack controllerStack, ItemStack tankStack) {
        if (controllerStack.isEmpty() || tankStack.isEmpty()) return;

        ItemExperiencePumpController controllerItem = (ItemExperiencePumpController) controllerStack.getItem();
        int mode = controllerItem.getMode(controllerStack);
        int retainLevel = controllerItem.getRetainLevel(controllerStack);
        boolean useForMending = controllerItem.isUseForMending(controllerStack);

        IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap != null) {
            cap.setMode(mode);
            cap.setRetainLevel(retainLevel);
            cap.setUseForMending(useForMending);
            ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
        }
    }

    /**
     * 检查物品是否在饰品栏中（用于严格模式）
     * @param player 玩家
     * @param targetStack 目标物品
     * @return 是否在饰品栏中
     */
    private boolean isInBaublesSlot(EntityPlayer player, ItemStack targetStack) {
        if (targetStack == null || targetStack.isEmpty()) return false;
        if (!BaublesHelper.isBaublesLoaded()) return false;
        
        Object handler = BaublesHelper.getBaublesHandler(player);
        if (handler == null) return false;
        
        int size = BaublesHelper.getSlots(handler);
        for (int i = 0; i < size; i++) {
            ItemStack slotStack = BaublesHelper.getStackInSlot(handler, i);
            if (slotStack == targetStack) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) return;

        // 戒指功能 - 支持多戒指和严格模式
        List<ItemStack> absorbRings = findAllRings(player, ItemAbsorbRing.class);
        boolean anyRingActive = false;
        
        for (ItemStack absorbRingStack : absorbRings) {
            if (absorbRingStack.isEmpty()) continue;
            
            // 检查严格模式：开启时只有饰品栏中的戒指才能工作
            boolean strictMode = com.rsring.config.RsRingConfig.absorbRing.strictMode;
            boolean inBaubles = isInBaublesSlot(player, absorbRingStack);
            
            if (!strictMode || inBaubles) {
                ((ItemAbsorbRing) absorbRingStack.getItem()).onWornTick(absorbRingStack, player);
                anyRingActive = true;
            }
            
            // 低电量提醒检测（降低检测频率）- 只检查第一个激活的戒指
            if (anyRingActive) {
                checkLowEnergyWarning(player, absorbRingStack);
            }
        }
        
        // 只要有激活的戒指，就执行定时清理
        if (anyRingActive) {
            DestroyManager.onTickCleanup(player);
        }

        // Controller-driven behavior (sync all tanks and pump via central controller)
        ItemStack controllerStack = findExperiencePumpController(player);
        if (!controllerStack.isEmpty()) {
            ItemExperiencePumpController controllerItem = (ItemExperiencePumpController) controllerStack.getItem();
            int mode = controllerItem.getMode(controllerStack);
            int retainLevel = controllerItem.getRetainLevel(controllerStack);
            boolean useForMending = controllerItem.isUseForMending(controllerStack);

            com.rsring.experience.ExperiencePumpController controller = com.rsring.experience.ExperiencePumpController.getInstance();
            com.rsring.experience.TankScanResult scan = controller.scanAllInventories(player);
            List<ItemStack> tanks = scan.getAllTanks();

            int maxManaged = com.rsring.config.ExperienceTankConfig.controller.maxManagedTanks;
            if (maxManaged > 0 && tanks.size() > maxManaged) {
                tanks = tanks.subList(0, maxManaged);
            }

            for (ItemStack tank : tanks) {
                IExperiencePumpCapability cap = tank.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                if (cap == null) continue;
                cap.setMode(mode);
                cap.setRetainLevel(retainLevel);
                cap.setUseForMending(useForMending);
                ItemExperiencePump.syncCapabilityToStack(tank, cap);
            }

            if (mode == IExperiencePumpCapability.MODE_PUMP_TO_PLAYER) {
                int playerTotal = controller.getPlayerTotalExperience(player);
                int targetXp = controller.convertLevelToXP(retainLevel);
                if (playerTotal < targetXp) {
                    int need = targetXp - playerTotal;
                    int available = controller.calculateTotalStored(player);
                    int toMove = Math.min(need, available);
                    if (toMove > 0) {
                        controller.performExperienceOperation(player, toMove, true);
                    }
                }
            } else if (mode == IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER) {
                int canExtract = controller.calculateLevelBasedExtraction(player, retainLevel);
                if (canExtract > 0) {
                    int availableSpace = controller.calculateTotalRemainingCapacity(player);
                    int toMove = Math.min(canExtract, availableSpace);
                    if (toMove > 0) {
                        controller.performExperienceOperation(player, toMove, false);
                    }
                }
            }
        }

        // 储罐功能 - 支持严格模式
        ItemStack pumpStack = findExperiencePump(player);
        if (!pumpStack.isEmpty()) {
            // 检查严格模式：开启时只有饰品栏中的储罐才能工作
            boolean tankStrictMode = com.rsring.config.ExperienceTankConfig.tank.strictMode;
            boolean inBaubles = isInBaublesSlot(player, pumpStack);
            
            if (!tankStrictMode || inBaubles) {
                ((ItemExperiencePump) pumpStack.getItem()).onWornTick(pumpStack, player);
                checkPlayerExperienceChange(player, pumpStack);
            }
        } else {
            lastPlayerXp.remove(player.getUniqueID());
        }
    }

    private void checkPlayerExperienceChange(EntityPlayer player, ItemStack pumpStack) {
        if (pumpStack.isEmpty()) {
            lastPlayerXp.remove(player.getUniqueID());
            return;
        }

        IExperiencePumpCapability cap = pumpStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap == null) return;

        if (cap.getMode() != IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER) {
            lastPlayerXp.remove(player.getUniqueID());
            return;
        }

        int currentXp = XpHelper.getPlayerTotalExperience(player);
        Integer lastXp = lastPlayerXp.get(player.getUniqueID());
        lastPlayerXp.put(player.getUniqueID(), currentXp);

        if (lastXp == null || currentXp <= lastXp) {
            return;
        }

        int retainLevel = cap.getRetainLevel();
        int targetXp = XpHelper.getExperienceForLevel(retainLevel);

        if (currentXp > targetXp) {
            int excess = currentXp - targetXp;
            int canStore = cap.getMaxXp() - cap.getXpStored();
            int toExtract = Math.min(excess, canStore);

            if (toExtract > 0) {
                XpHelper.removeExperienceFromPlayer(player, toExtract);
                cap.addXp(toExtract);
                ItemExperiencePump.syncCapabilityToStack(pumpStack, cap);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != EnumHand.MAIN_HAND) return;

        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if (player == null || world == null || pos == null) return;
        if (world.isRemote) return;
        if (!player.isSneaking()) return;

        boolean isContainer = isChestOrContainer(world, pos);
        boolean isRSController = isRSController(world, pos);

        if (!isContainer && !isRSController) return;

        ItemStack ringStack = findHeldRing(player, ItemAbsorbRing.class);
        if (ringStack.isEmpty()) return;

        IRsRingCapability capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (capability == null) return;

        boolean wasBound = capability.isBound();
        BlockPos oldPos = wasBound ? capability.getTerminalPos() : null;
        int oldDim = wasBound ? capability.getTerminalDimension() : 0;
        int currentDim = world.provider.getDimension();

        // 检测目标类型名称
        String targetType;
        if (isRSController) {
            targetType = "RS控制器";
        } else if (com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.isPlacedBackpack(world, pos)) {
            targetType = "WearableBackpacks背包";
        } else {
            targetType = "容器";
        }

        // 检查是否点击已绑定的位置
        if (wasBound && oldPos != null && oldPos.equals(pos) && oldDim == currentDim) {
            // 取消绑定
            capability.unbindTerminal();
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
            player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "成功解除绑定 " + targetType + ": " +
                pos.getX() + "," + pos.getY() + "," + pos.getZ() + " (" + currentDim + ")"));
            event.setCanceled(true);
            return;
        }

        // 绑定新位置
        capability.bindTerminal(world, pos);
        RsRingCapability.syncCapabilityToStack(ringStack, capability);

        player.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "成功绑定到 " + targetType + ": " +
            pos.getX() + "," + pos.getY() + "," + pos.getZ() + " (" + currentDim + ")"));

        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != EnumHand.MAIN_HAND) return;

        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if (player == null || world == null || pos == null) return;
        if (world.isRemote) return;
        if (!player.isSneaking()) return;

        // 检查冷却时间，防止刷屏
        UUID playerId = player.getUniqueID();
        long currentTime = System.currentTimeMillis();
        Long lastBindTime = lastLeftClickBindTime.get(playerId);
        if (lastBindTime != null && (currentTime - lastBindTime) < BIND_COOLDOWN_MS) {
            event.setCanceled(true);
            return;
        }

        // 检查目标是否为容器或RS控制器
        boolean isContainer = isChestOrContainer(world, pos);
        boolean isRSController = isRSController(world, pos);

        if (!isContainer && !isRSController) return;

        // 检查手持戒指
        ItemStack ringStack = findHeldRing(player, ItemAbsorbRing.class);
        if (ringStack.isEmpty()) return;

        IRsRingCapability capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (capability == null) return;

        boolean wasBound = capability.isTrashCanBound();
        BlockPos oldPos = wasBound ? capability.getTrashCanPos() : null;
        int oldDim = wasBound ? capability.getTrashCanDimension() : 0;
        int currentDim = world.provider.getDimension();

        // 检测目标类型名称
        String targetType;
        if (isRSController) {
            targetType = "RS控制器";
        } else if (com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.isPlacedBackpack(world, pos)) {
            targetType = "WearableBackpacks背包";
        } else {
            targetType = "容器";
        }

        // 检查是否点击已绑定的位置
        if (wasBound && oldPos != null && oldPos.equals(pos) && oldDim == currentDim) {
            // 解除绑定
            capability.unbindTrashCan();
            RsRingCapability.syncCapabilityToStack(ringStack, capability);
            // 记录冷却时间
            lastLeftClickBindTime.put(playerId, System.currentTimeMillis());
            player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "成功解除垃圾箱绑定 " + targetType + ": " +
                pos.getX() + "," + pos.getY() + "," + pos.getZ() + " (" + currentDim + ")"));
            event.setCanceled(true);
            return;
        }

        // 绑定新位置
        capability.bindTrashCan(world, pos);
        RsRingCapability.syncCapabilityToStack(ringStack, capability);
        // 记录冷却时间
        lastLeftClickBindTime.put(playerId, System.currentTimeMillis());

        player.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "成功绑定垃圾箱到 " + targetType + ": " +
            pos.getX() + "," + pos.getY() + "," + pos.getZ() + " (" + currentDim + ")"));

        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != EnumHand.MAIN_HAND) return;
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !player.isSneaking()) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemAbsorbRing)) return;
        if (event.getWorld().isRemote) return;

        net.minecraft.util.math.RayTraceResult hit = player.rayTrace(5.0D, 1.0F);
        if (hit != null && hit.typeOfHit == net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
            return;
        }

        IRsRingCapability capability = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (capability == null) return;

        RsRingCapability.refreshEnergyStorage(capability);
        net.minecraftforge.energy.IEnergyStorage energy = capability.getEnergyStorage();
        int amount = Math.max(0, com.rsring.config.RsRingConfig.absorbRing.manualChargeAmount);
        if (amount > 0) {
            int received = energy.receiveEnergy(amount, false);
            if (received > 0) {
                RsRingCapability.syncCapabilityToStack(stack, capability);
                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "手摇发电中 +" + received + "FE"));
            } else {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "能量已满"));
            }
        }
        event.setCancellationResult(net.minecraft.util.EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event == null) return;
        if (!RsRingMod.MODID.equals(event.getModID())) return;
        ConfigRegistry.syncAllConfig();
    }

    private boolean isChestOrContainer(World world, BlockPos pos) {
        if (world == null || pos == null) return false;

        IBlockState state = world.getBlockState(pos);
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return false;

        if (te instanceof net.minecraft.tileentity.TileEntityChest) return true;
        if (te instanceof net.minecraft.tileentity.TileEntityEnderChest) return true;

        // 检查是否是WearableBackpacks放置的背包
        if (com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.isPlacedBackpack(world, pos)) {
            return true;
        }

        net.minecraft.util.ResourceLocation regName = state.getBlock().getRegistryName();
        if (regName != null) {
            String blockName = regName.toString().toLowerCase();
            return blockName.contains("chest") || blockName.contains("container");
        }

        return false;
    }

    private boolean isRSController(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        IBlockState state = world.getBlockState(pos);
        net.minecraft.util.ResourceLocation regName = state.getBlock().getRegistryName();
        if (regName == null) return false;
        String blockName = regName.toString().toLowerCase();
        return blockName.equals("refinedstorage:controller");
    }
    
    /**
     * 清理过期的缓存数据，防止内存泄漏
     */
    private static void cleanupWarningCaches() {
        long now = System.currentTimeMillis();
        if (now - lastCacheCleanupTime < CACHE_CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCacheCleanupTime = now;

        int cooldownSeconds = com.rsring.config.RsRingConfig.absorbRing.lowEnergyWarningCooldown;
        long maxAgeMs = (cooldownSeconds + 60) * 1000L; // 冷却时间 + 60秒缓冲

        lowEnergyWarningCache.entrySet().removeIf(entry ->
            now - entry.getValue() > maxAgeMs);
    }

    /**
     * 低电量提醒检测
     * - 仅在戒指启用时检测
     * - 有冷却时间，避免刷屏
     * - 电量低于阈值时发送提醒
     */
    private void checkLowEnergyWarning(EntityPlayer player, ItemStack ringStack) {
        // 检查配置是否启用
        if (!com.rsring.config.RsRingConfig.absorbRing.enableLowEnergyWarning) {
            return;
        }
        
        // 降低检测频率：每5秒检测一次
        lowEnergyCheckCounter++;
        if (lowEnergyCheckCounter < LOW_ENERGY_CHECK_INTERVAL) {
            return;
        }
        lowEnergyCheckCounter = 0;
        
        // 定期清理缓存
        cleanupWarningCaches();
        
        // 获取戒指能力
        IRsRingCapability cap = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) return;
        
        // 仅在戒指启用时检测
        if (!cap.isEnabled()) return;
        
        // 获取能量状态
        net.minecraftforge.energy.IEnergyStorage energy = cap.getEnergyStorage();
        if (energy == null) return;
        
        int currentEnergy = energy.getEnergyStored();
        int maxEnergy = energy.getMaxEnergyStored();
        int threshold = com.rsring.config.RsRingConfig.absorbRing.lowEnergyWarningThreshold;
        int cooldownSeconds = com.rsring.config.RsRingConfig.absorbRing.lowEnergyWarningCooldown;
        
        // 计算当前电量百分比
        int percentage = (int) ((currentEnergy * 100.0) / maxEnergy);
        
        // 检查是否低于阈值
        if (percentage > threshold) {
            return;
        }
        
        // 检查冷却时间
        UUID playerId = player.getUniqueID();
        long currentTime = System.currentTimeMillis();
        Long lastWarningTime = lowEnergyWarningCache.get(playerId);
        
        if (lastWarningTime != null) {
            long elapsedSeconds = (currentTime - lastWarningTime) / 1000;
            if (elapsedSeconds < cooldownSeconds) {
                return; // 冷却中，不提醒
            }
        }
        
        // 更新提醒时间
        lowEnergyWarningCache.put(playerId, currentTime);
        
        // 发送提醒消息
        String message = String.format(
            TextFormatting.YELLOW + "⚠ " + TextFormatting.GOLD + "戒指电量不足！" + 
            TextFormatting.GRAY + " 当前: " + TextFormatting.RED + "%d%%" + 
            TextFormatting.GRAY + " (%d/%d FE)",
            percentage, currentEnergy, maxEnergy
        );
        player.sendMessage(new TextComponentString(message));
    }
    
    // 拾取拦截事件已禁用
    // 原因：销毁模式只针对背包模组的背包内容，不涉及玩家物品栏
    // 不再拦截玩家拾取物品
    
    /**
     * 玩家登录事件 - 重新应用彩蛋饰品的幸运属性
     * 原因：玩家登录时饰品已经装备，但onEquipped不会被调用
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) return;
        
        // 延迟一tick执行，确保玩家数据完全加载
        net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance()
            .addScheduledTask(() -> {
                if (player.isEntityAlive()) {
                    ItemAbsorbRing.reapplyAllEasterEggLuck(player);
                    com.rsring.item.ItemExperienceTank10000.reapplyAllEasterEggLuck(player);
                }
            });
    }
    
    /**
     * 玩家维度变化事件 - 重新应用彩蛋饰品的幸运属性
     * 原因：维度变化时属性可能会丢失
     */
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) return;
        
        // 延迟一tick执行
        net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance()
            .addScheduledTask(() -> {
                if (player.isEntityAlive()) {
                    ItemAbsorbRing.reapplyAllEasterEggLuck(player);
                    com.rsring.item.ItemExperienceTank10000.reapplyAllEasterEggLuck(player);
                }
            });
    }
    
    /**
     * 玩家重生事件 - 重新应用彩蛋饰品的幸运属性
     * 原因：玩家死亡重生后属性会丢失
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) return;
        
        // 延迟一tick执行
        net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance()
            .addScheduledTask(() -> {
                if (player.isEntityAlive()) {
                    ItemAbsorbRing.reapplyAllEasterEggLuck(player);
                    com.rsring.item.ItemExperienceTank10000.reapplyAllEasterEggLuck(player);
                }
            });
    }
}
