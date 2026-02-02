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
import com.moremod.item.ItemChestRing;
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
            toggleChestRingFunction();
        }
    }

    @SideOnly(Side.CLIENT)
    private void toggleChestRingFunction() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;
        // 发送数据包到服务器处理切换（服务器端修改capability并同步回客户端）
        RsRingMod.network.sendToServer(new PacketToggleRsRing());
    }

    // 查找玩家手持的戒指（仅主手、副手，用于绑定）
    private ItemStack findHeldRing(EntityPlayer player, Class<? extends Item> ringClass) {
        if (!player.getHeldItemMainhand().isEmpty() && ringClass.isInstance(player.getHeldItemMainhand().getItem())) {
            return player.getHeldItemMainhand();
        }
        if (!player.getHeldItemOffhand().isEmpty() && ringClass.isInstance(player.getHeldItemOffhand().getItem())) {
            return player.getHeldItemOffhand();
        }
        return ItemStack.EMPTY;
    }

    /** 供 K 键切换使用：与 onPlayerTick 相同的查找顺序，确保饰品栏戒指能被找到 */
    public static ItemStack findAnyRingForToggle(EntityPlayer player) {
        return findRing(player, ItemChestRing.class);
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
     * 查找经验泵控制器
     */
    private ItemStack findExperiencePumpController(EntityPlayer player) {
        // 检查主手和副手
        for (EnumHand hand : EnumHand.values()) {
            ItemStack heldStack = player.getHeldItem(hand);
            if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.moremod.item.ItemExperiencePumpController) {
                return heldStack;
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

        ItemStack chestRingStack = findRing(player, ItemChestRing.class);
        if (!chestRingStack.isEmpty()) {
            ((ItemChestRing) chestRingStack.getItem()).onWornTick(chestRingStack, player);
        }

        // 查找经验泵控制器和储罐
        ItemStack controllerStack = findExperiencePumpController(player);
        ItemStack pumpStack = findExperiencePump(player);

        // 如果同时持有控制器和储罐，则立即同步配置
        if (!controllerStack.isEmpty() && !pumpStack.isEmpty()) {
            syncControllerToTank(player, controllerStack, pumpStack);
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

        // 箱子戒指 + 箱子/容器（仅手持时绑定）
        if (isChestOrContainer(world, pos)) {
            ItemStack ringStack = findHeldRing(player, ItemChestRing.class);
            if (!ringStack.isEmpty()) {
                IRsRingCapability capability = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
                if (capability != null) {
                    capability.bindTerminal(world, pos);
                    RsRingCapability.syncCapabilityToStack(ringStack, capability);
                    if (!world.isRemote) {
                        player.sendMessage(new TextComponentString(TextFormatting.GREEN + "箱子戒指已绑定 " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
                    }
                    event.setCanceled(true);
                }
            }
        }
    }

    private boolean isChestOrContainer(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return false;
        for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.values()) {
            if (te.hasCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f)) return true;
        }
        return te.hasCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
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