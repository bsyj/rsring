package com.rsring.network;

import com.rsring.capability.IExperiencePumpCapability;
import com.rsring.capability.ExperiencePumpCapability;
import com.rsring.item.ItemExperiencePump;
import com.rsring.util.BaublesHelper;
import com.rsring.network.PacketSyncTankSlots;
import com.rsring.rsring.RsRingMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/** 处理泵控制器操作。 */
public class PacketPumpAction implements IMessage {

    public static final int ACTION_MODE = 0;
    public static final int ACTION_RETAIN_UP = 1;
    public static final int ACTION_RETAIN_DOWN = 2;
    public static final int ACTION_TAKE_ALL = 3;
    public static final int ACTION_TAKE_ONE = 4;
    public static final int ACTION_STORE_ONE = 5;
    public static final int ACTION_STORE_ALL = 6;
    public static final int ACTION_MENDING = 7;

    private int handOrdinal;
    private int action;
    private int value; // Used for retain/take/store amount
    public PacketPumpAction() {}

    public PacketPumpAction(EnumHand hand, int action) {
        this.handOrdinal = hand == EnumHand.MAIN_HAND ? 0 : 1;
        this.action = action;
        this.value = 0;
    }

    public PacketPumpAction(EnumHand hand, int action, int value) {
        this.handOrdinal = hand == EnumHand.MAIN_HAND ? 0 : 1;
        this.action = action;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        action = buf.readByte();
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeByte(action);
        buf.writeInt(value);
    }

    public static class Handler implements IMessageHandler<PacketPumpAction, IMessage> {
    @Override
    public IMessage onMessage(PacketPumpAction msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            EnumHand hand = msg.handOrdinal == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack controllerStack = player.getHeldItem(hand);

                if (controllerStack.isEmpty() || !(controllerStack.getItem() instanceof com.rsring.item.ItemExperiencePumpController)) {
                    return;
                }

                java.util.List<ItemStack> tankStacks = findAllExperienceTanks(player);
                // Record tank locations for sync-back
                java.util.Map<ItemStack, TankLocationInfo> tankLocations = new java.util.HashMap<>();
                recordTankLocations(player, tankStacks, tankLocations);
                if (tankStacks.isEmpty()) {
                    if (msg.action == ACTION_MENDING) {
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            net.minecraft.util.text.TextFormatting.RED + "未找到经验储罐"));
                    }
                    return;
                }

                java.util.List<ItemStack> prioritized = new java.util.ArrayList<>();
                if (msg.action == ACTION_STORE_ONE || msg.action == ACTION_STORE_ALL) {
                    for (ItemStack t : tankStacks) {
                        int stored = ItemExperiencePump.getXpStoredFromNBT(t);
                        if (stored == 0) prioritized.add(t);
                    }
                    for (ItemStack t : tankStacks) {
                        int stored = ItemExperiencePump.getXpStoredFromNBT(t);
                        if (stored > 0) prioritized.add(t);
                    }
                } else if (msg.action == ACTION_TAKE_ONE || msg.action == ACTION_TAKE_ALL) {
                    for (ItemStack t : tankStacks) {
                        int stored = ItemExperiencePump.getXpStoredFromNBT(t);
                        if (stored > 0) prioritized.add(t);
                    }
                    for (ItemStack t : tankStacks) {
                        int stored = ItemExperiencePump.getXpStoredFromNBT(t);
                        if (stored == 0) prioritized.add(t);
                    }
                } else {
                    prioritized.addAll(tankStacks);
                }
                java.util.List<ItemStack> controllerManaged = new java.util.ArrayList<>();
                java.util.List<ItemStack> unmanaged = new java.util.ArrayList<>();
                com.rsring.experience.ExperiencePumpController pumpController = com.rsring.experience.ExperiencePumpController.getInstance();
                for (ItemStack t : prioritized) {
                    if (pumpController.isTankManagedByController(t)) controllerManaged.add(t);
                    else unmanaged.add(t);
                }
                prioritized.clear();
                prioritized.addAll(controllerManaged);
                prioritized.addAll(unmanaged);

                // Use centralized ExperiencePumpController for XP/level calculations to avoid inconsistencies
                java.util.function.Function<Integer, Integer> levelToTotalXp = (lvl) -> pumpController.convertLevelToXP(lvl);
                java.util.function.Supplier<Integer> getPlayerTotalXp = () -> pumpController.getPlayerTotalExperience(player);

                switch (msg.action) {
                    case ACTION_MODE:
                    case ACTION_RETAIN_UP:
                    case ACTION_RETAIN_DOWN:
                    case ACTION_MENDING:
                        // Update controller item NBT on server so changes persist
                        if (!controllerStack.isEmpty() && controllerStack.getItem() instanceof com.rsring.item.ItemExperiencePumpController) {
                            int ctrlMode = com.rsring.item.ItemExperiencePumpController.getMode(controllerStack);
                            int ctrlRetain = com.rsring.item.ItemExperiencePumpController.getRetainLevel(controllerStack);
                            boolean ctrlMend = com.rsring.item.ItemExperiencePumpController.isUseForMending(controllerStack);

                            switch (msg.action) {
                                case ACTION_MODE:
                                    ctrlMode = (ctrlMode + 1) % 3;
                                    break;
                                case ACTION_RETAIN_UP:
                                    ctrlRetain = ctrlRetain + (msg.value > 0 ? msg.value : 1);
                                    break;
                                case ACTION_RETAIN_DOWN:
                                    ctrlRetain = Math.max(0, ctrlRetain - (msg.value > 0 ? msg.value : 1));
                                    break;
                                case ACTION_MENDING:
                                    ctrlMend = !ctrlMend;
                                    break;
                            }

                            // Persist updated controller data to the held controller item
                            com.rsring.item.ItemExperiencePumpController.setControllerData(controllerStack, ctrlMode, ctrlRetain, ctrlMend);
                        }

                        for (ItemStack tankStack : prioritized) {
                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;
                            switch (msg.action) {
                                case ACTION_MODE:
                                    cap.setMode((cap.getMode() + 1) % 3);
                                    // Apply once after mode switch
                                    pumpExperienceBetweenPlayerAndTank(player, cap);
                                    break;
                                case ACTION_RETAIN_UP:
                                    cap.setRetainLevel(cap.getRetainLevel() + (msg.value > 0 ? msg.value : 1));
                                    break;
                                case ACTION_RETAIN_DOWN:
                                    cap.setRetainLevel(Math.max(0, cap.getRetainLevel() - (msg.value > 0 ? msg.value : 1)));
                                    break;
                                case ACTION_MENDING:
                                    cap.setUseForMending(!cap.isUseForMending());
                                    break;
                            }
                            ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                            syncTankBack(tankStack, tankLocations.get(tankStack), player);
                        }
                        break;

                    case ACTION_TAKE_ALL: {
                        // Extract all XP
                        int totalExtracted = 0;

                        for (ItemStack tankStack : prioritized) {
                            int extracted = ItemExperiencePump.extractAllExperience(tankStack, player);
                            if (extracted > 0) {
                                totalExtracted += extracted;
                                syncTankBack(tankStack, tankLocations.get(tankStack), player);
                            }
                        }
                        break;
                    }

                    case ACTION_TAKE_ONE: {
                        int levelsToTake = msg.value > 0 ? msg.value : 1;
                        if (levelsToTake <= 0) break;

                        int currentLevel = player.experienceLevel;
                        int targetLevel = currentLevel + levelsToTake;
                        int totalXPNeeded = com.rsring.util.XpHelper.getExperienceBetweenLevels(currentLevel, targetLevel);
                        if (totalXPNeeded <= 0) break;

                        int totalExtracted = 0;

                        for (ItemStack tankStack : prioritized) {
                            if (totalXPNeeded <= 0) break;

                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;

                            int available = cap.getXpStored();
                            if (available <= 0) continue;

                            int take = Math.min(available, totalXPNeeded);
                            int extracted = cap.takeXp(take);
                            if (extracted > 0) {
                                totalExtracted += extracted;
                                totalXPNeeded -= extracted;
                                com.rsring.util.XpHelper.addExperienceToPlayer(player, extracted);
                                ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                                syncTankBack(tankStack, tankLocations.get(tankStack), player);
                            }
                        }
                        break;
                    }

                    case ACTION_STORE_ALL: {
                        // Store all XP
                        int totalStored = 0;

                        for (ItemStack tankStack : prioritized) {
                            int stored = ItemExperiencePump.storeAllExperience(tankStack, player);
                            if (stored > 0) {
                                totalStored += stored;
                                syncTankBack(tankStack, tankLocations.get(tankStack), player);
                            }
                        }
                        break;
                    }

                    case ACTION_STORE_ONE: {
                        // Store N levels (default 1)
                        int levelsToStore = msg.value > 0 ? msg.value : 1;
                        if (levelsToStore <= 0) break;

                        int currentLevel = player.experienceLevel;
                        int targetLevel = Math.max(0, currentLevel - levelsToStore);
                        int totalXPToStore = com.rsring.util.XpHelper.getExperienceBetweenLevels(targetLevel, currentLevel);
                        if (totalXPToStore <= 0) break;

                        int playerTotalXP = com.rsring.util.XpHelper.getPlayerTotalExperience(player);
                        int targetTotalXP = com.rsring.util.XpHelper.getExperienceForLevel(targetLevel);
                        if (playerTotalXP <= targetTotalXP) break;
                        totalXPToStore = Math.min(totalXPToStore, playerTotalXP - targetTotalXP);
                        if (totalXPToStore <= 0) break;

                        int totalStored = 0;

                        for (ItemStack tankStack : prioritized) {
                            if (totalXPToStore <= 0) break;

                            IExperiencePumpCapability cap = tankStack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
                            if (cap == null) continue;

                            int availableSpace = cap.getMaxXp() - cap.getXpStored();
                            if (availableSpace <= 0) continue;

                            int store = Math.min(availableSpace, totalXPToStore);
                            int stored = cap.addXp(store);
                            if (stored > 0) {
                                totalStored += stored;
                                totalXPToStore -= stored;
                                com.rsring.util.XpHelper.removeExperienceFromPlayer(player, stored);
                                ItemExperiencePump.syncCapabilityToStack(tankStack, cap);
                                syncTankBack(tankStack, tankLocations.get(tankStack), player);
                            }
                        }
                        break;
                    }

                    default:
                        break;
                }

                if (!tankStacks.isEmpty()) {
                    RsRingMod.network.sendTo(new PacketPumpData(tankStacks.get(0)), player);
                }
            });
            return null;
        }


    private ItemStack findExperienceTank(EntityPlayer player) {
            if (player == null) return net.minecraft.item.ItemStack.EMPTY;
            if (BaublesHelper.isBaublesLoaded()) {
                Object handler = BaublesHelper.getBaublesHandler(player);
                int size = BaublesHelper.getSlots(handler);
                for (int i = 0; i < size; i++) {
                    ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                    if (!stack.isEmpty() && stack.getItem() instanceof com.rsring.item.ItemExperiencePump) return stack;
                }
            }

            for (EnumHand h : EnumHand.values()) {
                ItemStack heldStack = player.getHeldItem(h);
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                    return heldStack;
                }
            }

            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                    return stack;
                }
            }

            return net.minecraft.item.ItemStack.EMPTY;
        }





    private void pumpExperienceBetweenPlayerAndTank(EntityPlayer player, IExperiencePumpCapability cap) {
            int retain = cap.getRetainLevel();
            int playerTotal = com.rsring.util.XpHelper.getPlayerTotalExperience(player);
            int targetXp = com.rsring.util.XpHelper.getExperienceForLevel(retain);

            if (cap.getMode() == IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER) {
                int take = Math.min(playerTotal - targetXp, cap.getMaxXp() - cap.getXpStored());
                if (take > 0) {
                    com.rsring.util.XpHelper.removeExperienceFromPlayer(player, take);
                    cap.addXp(take);
                }
            } else if (cap.getMode() == IExperiencePumpCapability.MODE_PUMP_TO_PLAYER) {
                int need = targetXp - playerTotal;
                int give = cap.takeXp(Math.min(need, 100));
                if (give > 0) {
                    com.rsring.util.XpHelper.addExperienceToPlayer(player, give);
                }
            }
        }




    private List<ItemStack> findAllExperienceTanks(EntityPlayer player) {
            List<ItemStack> tanks = new ArrayList<>();
            java.util.Set<ItemStack> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            if (player == null) return tanks;

            if (BaublesHelper.isBaublesLoaded()) {
                Object handler = BaublesHelper.getBaublesHandler(player);
                int size = BaublesHelper.getSlots(handler);
                for (int i = 0; i < size; i++) {
                    ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                    if (!stack.isEmpty() && stack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                        if (seen.add(stack)) {
                            tanks.add(stack);
                        }
                    }
                }
            }

            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                    if (seen.add(stack)) {
                        tanks.add(stack);
                    }
                }
            }

            for (EnumHand h : EnumHand.values()) {
                ItemStack heldStack = player.getHeldItem(h);
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof com.rsring.item.ItemExperiencePump) {
                    if (seen.add(heldStack)) {
                        tanks.add(heldStack);
                    }
                }
            }

            int maxManaged = com.rsring.config.ExperienceTankConfig.controller.maxManagedTanks;
            if (maxManaged > 0 && tanks.size() > maxManaged) {
                return new java.util.ArrayList<>(tanks.subList(0, maxManaged));
            }
            return tanks;
        }



    private static class TankLocationInfo {
            final String locationType; // "baubles", "inventory", "hand"
            final int slotIndex;
            final Object inventory;

            TankLocationInfo(String locationType, int slotIndex, Object inventory) {
                this.locationType = locationType;
                this.slotIndex = slotIndex;
                this.inventory = inventory;
            }
        }


    private void recordTankLocations(EntityPlayer player, List<ItemStack> tanks, Map<ItemStack, TankLocationInfo> locations) {
            java.util.Set<ItemStack> mapped = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

            // Check baubles
            if (BaublesHelper.isBaublesLoaded()) {
                Object handler = BaublesHelper.getBaublesHandler(player);
                int size = BaublesHelper.getSlots(handler);
                for (int i = 0; i < size; i++) {
                    ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                    if (stack.isEmpty() || !(stack.getItem() instanceof com.rsring.item.ItemExperiencePump)) {
                        continue;
                    }
                    for (ItemStack tank : tanks) {
                        if (mapped.contains(tank)) continue;
                        if (tank == stack || ItemStack.areItemStacksEqual(tank, stack)) {
                            locations.put(tank, new TankLocationInfo("baubles", i, handler));
                            mapped.add(tank);
                            break;
                        }
                    }
                }
            }

            // Check inventory
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof com.rsring.item.ItemExperiencePump)) {
                    continue;
                }
                for (ItemStack tank : tanks) {
                    if (mapped.contains(tank)) continue;
                    if (tank == stack || ItemStack.areItemStacksEqual(tank, stack)) {
                        locations.put(tank, new TankLocationInfo("inventory", i, player.inventory));
                        mapped.add(tank);
                        break;
                    }
                }
            }
        }



    private void syncTankBack(ItemStack tank, TankLocationInfo location, EntityPlayerMP player) {
            if (location == null) {
                return;
            }

            if ("baubles".equals(location.locationType)) {
                if (BaublesHelper.setStackInSlot(location.inventory, location.slotIndex, tank)) {
                    RsRingMod.network.sendTo(new PacketSyncTankSlots("baubles", location.slotIndex, tank), player);
                }
            } else if ("inventory".equals(location.locationType) && location.inventory instanceof net.minecraft.inventory.IInventory) {
                net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory) location.inventory;
                inv.setInventorySlotContents(location.slotIndex, tank);
                inv.markDirty();
                RsRingMod.network.sendTo(new PacketSyncTankSlots("inventory", location.slotIndex, tank), player);
            }
            // Hand items sync automatically
        }



    }
}

