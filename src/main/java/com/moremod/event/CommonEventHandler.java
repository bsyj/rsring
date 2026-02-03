package com.moremod.event;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.lwjgl.input.Keyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import com.moremod.rsring.RsRingMod;
import com.moremod.item.ItemAbsorbRing;
import com.moremod.item.ItemExperiencePump;
import com.moremod.capability.IRsRingCapability;
import com.moremod.capability.RsRingCapability;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import com.moremod.network.PacketToggleRsRing;

public class CommonEventHandler {

    // 客户端按键绑定
    @SideOnly(Side.CLIENT)
    private static KeyBinding toggleKeyBinding;

    public CommonEventHandler() {
        // 注册按键
        registerKeyBindings();
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
        // 发送数据包到服务器处理切换（服务器端修改capability并同步回客户端）
        RsRingMod.network.sendToServer(new PacketToggleRsRing());
    }

    // 查找玩家手持的戒指（仅主手，用于绑定）
    private ItemStack findHeldRing(EntityPlayer player, Class<? extends Item> ringClass) {
        if (!player.getHeldItemMainhand().isEmpty() && ringClass.isInstance(player.getHeldItemMainhand().getItem())) {
            return player.getHeldItemMainhand();
        }
        return ItemStack.EMPTY;
    }

    /** 供 K 键切换使用：与 onPlayerTick 相同的查找顺序，确保饰品栏戒指能被找到 */
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
        if (Loader.isModLoaded("baubles")) {
            try {
                Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);
                if (handler instanceof net.minecraft.inventory.IInventory) {
                    net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                    for (int i = 0; i < baubles.getSizeInventory(); i++) {
                        ItemStack stack = baubles.getStackInSlot(i);
                        if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) return stack;
                    }
                }
            } catch (Throwable ignored) {}
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ringClass.isInstance(stack.getItem())) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 查找经验泵，优先使用空的经验泵
     * 优先级：手持 > 饰品栏 > 背包，相同位置优先使用空的经验泵
     */
    private ItemStack findExperiencePump(EntityPlayer player) {
        // 检查主手和副手
        for (EnumHand hand : EnumHand.values()) {
            ItemStack heldStack = player.getHeldItem(hand);
            if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemExperiencePump) {
                // 检查是否为空的经验泵
                com.moremod.capability.IExperiencePumpCapability cap = heldStack.getCapability(com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                if (cap != null && cap.getXpStored() == 0) {
                    return heldStack; // 返回空的经验泵
                }
            }
        }

        // 检查饰品栏
        if (Loader.isModLoaded("baubles")) {
            try {
                Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class).invoke(null, player);
                if (handler instanceof net.minecraft.inventory.IInventory) {
                    net.minecraft.inventory.IInventory baubles = (net.minecraft.inventory.IInventory) handler;
                    for (int i = 0; i < baubles.getSizeInventory(); i++) {
                        ItemStack stack = baubles.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                            com.moremod.capability.IExperiencePumpCapability cap = stack.getCapability(com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap != null && cap.getXpStored() == 0) {
                                return stack; // 返回空的经验泵
                            }
                        }
                    }
                    // 如果没找到空的经验泵，返回第一个经验泵
                    for (int i = 0; i < baubles.getSizeInventory(); i++) {
                        ItemStack stack = baubles.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                            return stack;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 检查背包
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                com.moremod.capability.IExperiencePumpCapability cap = stack.getCapability(com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                if (cap != null && cap.getXpStored() == 0) {
                    return stack; // 返回空的经验泵
                }
            }
        }

        // 如果没找到空的经验泵，返回第一个经验泵
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemExperiencePump) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 查找经验泵控制器（扫描整个背包）
     * 优先级：主手 > 副手 > 背包 > 快捷栏
     */
    private ItemStack findExperiencePumpController(EntityPlayer player) {
        // 1. 检查主手和副手（优先级最高）
        for (EnumHand hand : EnumHand.values()) {
            ItemStack heldStack = player.getHeldItem(hand);
            if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.moremod.item.ItemExperiencePumpController) {
                return heldStack;
            }
        }
        
        // 2. 检查主背包（9-35槽位）
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePumpController) {
                return stack;
            }
        }
        
        // 3. 检查快捷栏（0-8槽位）
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof com.moremod.item.ItemExperiencePumpController) {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }

    /**
     * 将控制器配置同步到储罐
     */
    private void syncControllerToTank(EntityPlayer player, ItemStack controllerStack, ItemStack tankStack) {
        if (controllerStack.isEmpty() || tankStack.isEmpty()) return;

        // 从控制器获取配置
        com.moremod.item.ItemExperiencePumpController controllerItem = (com.moremod.item.ItemExperiencePumpController) controllerStack.getItem();
        int mode = controllerItem.getMode(controllerStack);
        int retainLevel = controllerItem.getRetainLevel(controllerStack);
        boolean useForMending = controllerItem.isUseForMending(controllerStack);

        // 应用到储罐
        com.moremod.capability.IExperiencePumpCapability cap = tankStack.getCapability(
            com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap != null) {
            cap.setMode(mode);
            cap.setRetainLevel(retainLevel);
            cap.setUseForMending(useForMending);
            com.moremod.item.ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
        }
    }

    // 每tick执行戒指功能（吸收物品）
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        ItemStack absorbRingStack = findRing(player, ItemAbsorbRing.class);
        if (!absorbRingStack.isEmpty()) {
            ((ItemAbsorbRing) absorbRingStack.getItem()).onWornTick(absorbRingStack, player);
        }

        // 查找经验泵控制器和储罐
        ItemStack controllerStack = findExperiencePumpController(player);
        ItemStack pumpStack = findExperiencePump(player);

        // 如果持有控制器，则将控制器配置应用到玩家所有检测到的储罐（饰品栏/背包/手持）
        if (!controllerStack.isEmpty()) {
            // 从控制器获取配置并应用到所有储罐
            com.moremod.item.ItemExperiencePumpController controllerItem = (com.moremod.item.ItemExperiencePumpController) controllerStack.getItem();
            int mode = controllerItem.getMode(controllerStack);
            int retainLevel = controllerItem.getRetainLevel(controllerStack);
            boolean useForMending = controllerItem.isUseForMending(controllerStack);

            // 扫描并应用到每个储罐
            com.moremod.experience.ExperiencePumpController pumpController = com.moremod.experience.ExperiencePumpController.getInstance();
            com.moremod.experience.TankScanResult scan = pumpController.scanAllInventories(player);
            java.util.List<ItemStack> tanks = scan.getAllTanks();
            for (ItemStack tank : tanks) {
                com.moremod.capability.IExperiencePumpCapability cap = tank.getCapability(com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                if (cap == null) continue;
                cap.setMode(mode);
                cap.setRetainLevel(retainLevel);
                cap.setUseForMending(useForMending);
                com.moremod.item.ItemExperiencePump.syncCapabilityToStack(tank, cap);
            }
            // 控制器应主动执行泵送行为：如果模式为罐->人或人->罐，调用中央控制器执行一次按等级计算的泵送
            if (mode == com.moremod.capability.IExperiencePumpCapability.MODE_PUMP_TO_PLAYER ||
                mode == com.moremod.capability.IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER) {
                // 计算需要移动的经验量（基于保留等级）
                com.moremod.experience.ExperiencePumpController controller = com.moremod.experience.ExperiencePumpController.getInstance();
                if (mode == com.moremod.capability.IExperiencePumpCapability.MODE_PUMP_TO_PLAYER) {
                    int playerTotal = controller.getPlayerTotalExperience(player);
                    int targetXp = controller.convertLevelToXP(retainLevel);
                    if (playerTotal < targetXp) {
                        int need = targetXp - playerTotal;
                        int available = controller.calculateTotalStored(player);
                        int toMove = Math.min(need, available);
                        if (toMove > 0) {
                            // 直接按需要量/可用量进行转移，确保玩家能被补足到保留等级
                            controller.performExperienceOperation(player, toMove, true);
                        }
                    }
                } else if (mode == com.moremod.capability.IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER) {
                    // 计算玩家超过保留等级可抽取的经验
                    int canExtract = controller.calculateLevelBasedExtraction(player, retainLevel);
                    if (canExtract > 0) {
                        int availableSpace = controller.calculateTotalRemainingCapacity(player);
                        int toMove = Math.min(canExtract, availableSpace);
                        if (toMove > 0) {
                            // 从玩家到罐（isExtraction = false 表示注入到罐）
                            controller.performExperienceOperation(player, toMove, false);
                        }
                    }
                }
            }
        }

        if (!pumpStack.isEmpty()) {
            ((ItemExperiencePump) pumpStack.getItem()).onWornTick(pumpStack, player);
        }

        // 检查玩家经验变化，触发保留等级机制
        checkPlayerExperienceChange(player, pumpStack);
    }
    
    // 存储玩家上一次的经验值，用于检测变化
    private static final java.util.Map<java.util.UUID, Integer> lastPlayerXp = new java.util.HashMap<>();
    
    /**
     * 检查玩家经验变化，自动触发保留等级机制
     * 当玩家经验增加时（吸收经验球、获得经验、/xp等），自动抽取超过保留等级的经验
     */
    private void checkPlayerExperienceChange(EntityPlayer player, ItemStack pumpStack) {
        if (pumpStack.isEmpty()) {
            lastPlayerXp.remove(player.getUniqueID());
            return;
        }
        
        com.moremod.capability.IExperiencePumpCapability cap = pumpStack.getCapability(
            com.moremod.capability.ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap == null) return;
        
        // 只在"从玩家泵入"模式下检测
        if (cap.getMode() != com.moremod.capability.IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER) {
            lastPlayerXp.remove(player.getUniqueID());
            return;
        }
        
        // 计算玩家当前总经验
        int currentXp = getPlayerTotalXp(player);
        Integer lastXp = lastPlayerXp.get(player.getUniqueID());
        
        // 如果经验增加了，检查是否超过保留等级
        if (lastXp != null && currentXp > lastXp) {
            int retainLevel = cap.getRetainLevel();
            int targetXp = levelToTotalXp(retainLevel);
            
            // 如果超过保留等级，立即抽取
            if (currentXp > targetXp && cap.getXpStored() < cap.getMaxXp()) {
                int excess = currentXp - targetXp;
                int canStore = cap.getMaxXp() - cap.getXpStored();
                int toExtract = Math.min(excess, canStore);
                
                if (toExtract > 0) {
                    addPlayerXp(player, -toExtract);
                    cap.addXp(toExtract);
                    ItemExperiencePump.syncCapabilityToStack(pumpStack, cap);
                    currentXp -= toExtract; // 更新当前经验
                }
            }
        }
        
        // 更新记录的经验值
        lastPlayerXp.put(player.getUniqueID(), currentXp);
    }
    
    /**
     * 获取玩家总经验值
     */
    private static int getPlayerTotalXp(EntityPlayer player) {
        return (int) (player.experience * (float) player.xpBarCap()) + getTotalXpForLevel(player.experienceLevel);
    }

    /**
     * 获取指定等级所需的总经验值
     */
    private static int getTotalXpForLevel(int level) {
        if (level <= 0) return 0;
        if (level < 16) return level * (6 + level);
        if (level < 31) return (int) (level * (2.5 * level - 40.5) + 360);
        return (int) (level * (4.5 * level - 162.5) + 2220);
    }

    /**
     * 等级转换为总经验值
     */
    private static int levelToTotalXp(int level) {
        return getTotalXpForLevel(level);
    }
    
    /**
     * 增加或减少玩家经验
     */
    private static void addPlayerXp(EntityPlayer player, int amount) {
        if (amount <= 0) {
            int take = -amount;
            int current = getPlayerTotalXp(player);
            take = Math.min(take, current);
            if (take <= 0) return;
            int remain = current - take;
            player.experienceLevel = 0;
            player.experienceTotal = 0;
            player.experience = 0;
            if (remain > 0) player.addExperience(remain);
        } else {
            player.addExperience(amount);
        }
    }

    // 处理玩家与方块的交互事件（蹲下+戒指右键绑定）
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if (!player.isSneaking()) return;
        
        // 只在服务端处理绑定逻辑，避免客户端和服务端都发送消息
        if (world.isRemote) return;

        // 物品吸收戒指 + 箱子/容器（仅手持时绑定）
        if (isChestOrContainer(world, pos)) {
            ItemStack ringStack = findHeldRing(player, ItemAbsorbRing.class);
            if (!ringStack.isEmpty()) {
                IRsRingCapability capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
                if (capability != null) {
                    // 检查绑定前的状态
                    boolean wasBound = capability.isBound();
                    BlockPos oldPos = null;
                    int oldDim = 0;
                    if (wasBound) {
                        oldPos = capability.getTerminalPos();
                        oldDim = capability.getTerminalDimension();
                    }
                    
                    // 执行绑定
                    capability.bindTerminal(world, pos);
                    RsRingCapability.syncCapabilityToStack(ringStack, capability);
                    
                    // 只有当绑定状态发生变化时才发送消息
                    boolean isBound = capability.isBound();
                    BlockPos newPos = capability.getTerminalPos();
                    int newDim = capability.getTerminalDimension();
                    
                    if (!wasBound || (oldPos != null && (!oldPos.equals(newPos) || oldDim != newDim))) {
                        int dim = world.provider.getDimension();
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            net.minecraft.util.text.TextFormatting.GREEN + "已绑定坐标：" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " 维度：" + dim));
                    }
                    
                    event.setCanceled(true);
                }
            }
        }
    }

    private boolean isChestOrContainer(World world, BlockPos pos) {
        // 检查是否为箱子类方块
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return false;
        
        // 只接受箱子类的TileEntity
        if (te instanceof net.minecraft.tileentity.TileEntityChest) return true;
        if (te instanceof net.minecraft.tileentity.TileEntityEnderChest) return true;
        
        // 检查方块类型名称，确保包含箱子相关的名称
        IBlockState state = world.getBlockState(pos);
        net.minecraft.util.ResourceLocation regName = state.getBlock().getRegistryName();
        if (regName != null) {
            String blockName = regName.toString().toLowerCase();
            if (blockName.contains("chest") || blockName.contains("container")) {
                return true;
            }
        }
        
        return false;
    }

    // 检查方块是否为RS终端/接口/控制器
    private boolean isRSTerminal(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        net.minecraft.util.ResourceLocation regName = state.getBlock().getRegistryName();
        if (regName == null) return false;
        String blockName = regName.toString();

        return blockName.contains("refinedstorage") && (
            blockName.contains("grid") ||
            blockName.contains("terminal") ||
            blockName.contains("wireless") ||
            blockName.contains("interface") ||
            blockName.contains("controller")
        );
    }
}