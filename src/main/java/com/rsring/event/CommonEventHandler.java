package com.rsring.event;

import com.rsring.capability.ExperiencePumpCapability;
import com.rsring.capability.IExperiencePumpCapability;
import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.item.ItemAbsorbRing;
import com.rsring.item.ItemExperiencePump;
import com.rsring.item.ItemExperiencePumpController;
import com.rsring.network.PacketToggleRsRing;
import com.rsring.rsring.RsRingMod;
import com.rsring.util.BaublesHelper;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommonEventHandler {

    private static KeyBinding toggleKeyBinding;
    private static final Map<UUID, Integer> lastPlayerXp = new HashMap<>();

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

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) return;

        ItemStack absorbRingStack = findRing(player, ItemAbsorbRing.class);
        if (!absorbRingStack.isEmpty()) {
            ((ItemAbsorbRing) absorbRingStack.getItem()).onWornTick(absorbRingStack, player);
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

        ItemStack pumpStack = findExperiencePump(player);
        if (!pumpStack.isEmpty()) {
            ((ItemExperiencePump) pumpStack.getItem()).onWornTick(pumpStack, player);
        }

        checkPlayerExperienceChange(player, pumpStack);
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

        capability.bindTerminal(world, pos);
        RsRingCapability.syncCapabilityToStack(ringStack, capability);

        BlockPos newPos = capability.getTerminalPos();
        int newDim = capability.getTerminalDimension();

        if (!wasBound || (oldPos != null && (!oldPos.equals(newPos) || oldDim != newDim))) {
            int dim = world.provider.getDimension();
            String targetType = isRSController ? "RS Controller" : "Container";
            String statusMsg = TextFormatting.GREEN + "Bound to " + targetType + ": " +
                pos.getX() + "," + pos.getY() + "," + pos.getZ() + " (" + dim + ")";
            player.sendMessage(new TextComponentString(statusMsg));
        }

        event.setCanceled(true);
    }

    private boolean isChestOrContainer(World world, BlockPos pos) {
        if (world == null || pos == null) return false;

        IBlockState state = world.getBlockState(pos);
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return false;

        if (te instanceof net.minecraft.tileentity.TileEntityChest) return true;
        if (te instanceof net.minecraft.tileentity.TileEntityEnderChest) return true;

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
}



