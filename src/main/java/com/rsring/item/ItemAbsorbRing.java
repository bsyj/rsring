package com.rsring.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import java.util.List;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.IEnergyStorage;
import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Optional;
import baubles.api.IBauble;
import baubles.api.BaubleType;
import org.lwjgl.input.Keyboard;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemAbsorbRing extends Item implements IBauble {

    public ItemAbsorbRing() {
        super();
        this.setTranslationKey("rsring.item_absorb_ring");
        this.setRegistryName(new ResourceLocation("rsring", "item_absorb_ring"));
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) return;

        IEnergyStorage energy = cap.getEnergyStorage();
        tooltip.add(TextFormatting.GRAY + "能量: " + TextFormatting.YELLOW + formatFe(energy.getEnergyStored())
            + TextFormatting.GRAY + " / " + formatFe(energy.getMaxEnergyStored()) + " FE");
        tooltip.add(TextFormatting.GRAY + "状态: " + (cap.isEnabled() ? TextFormatting.GREEN + "已启用"
            : TextFormatting.RED + "已禁用"));
        if (cap.isBound()) {
            BlockPos pos = cap.getTerminalPos();
            String dim = getDimensionName(cap.getTerminalDimension());
            tooltip.add(TextFormatting.GRAY + "已绑定: " + TextFormatting.AQUA + pos.getX() + "," + pos.getY()
                + "," + pos.getZ() + TextFormatting.GRAY + " (" + TextFormatting.AQUA + dim + TextFormatting.GRAY + ")");
            tooltip.add(TextFormatting.GRAY + "过滤模式: " + (cap.isWhitelistMode() ? TextFormatting.AQUA + "白名单"
                : TextFormatting.AQUA + "黑名单"));
        } else {
            tooltip.add(TextFormatting.GRAY + "未绑定");
        }

        boolean showDetail = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (!showDetail) {
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " + TextFormatting.YELLOW + "Shift"
                + TextFormatting.DARK_GRAY + " 查看详情");
            return;
        }

        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "功能特点:");
        tooltip.add(TextFormatting.GRAY + "  - 吸收附近的掉落物到绑定的目标");
        tooltip.add(TextFormatting.GRAY + "  - 支持白名单/黑名单过滤");
        tooltip.add(TextFormatting.GRAY + "  - 每次吸收消耗能量");

        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "使用方法:");
        tooltip.add(TextFormatting.GRAY + "  1. 右键空气点击打开过滤设置");
        tooltip.add(TextFormatting.GRAY + "  2. 潜行右键rs控制器/箱子绑定");
        tooltip.add(TextFormatting.GRAY + "  3. 按K键切换吸收开关");
        tooltip.add(TextFormatting.GRAY + "  4. 可在背包/饰品槽中使用");

        // 显示绑定目标类型
        if (cap.isBound() && worldIn != null) {
            BlockPos pos = cap.getTerminalPos();
            int dim = cap.getTerminalDimension();
            if (worldIn.provider.getDimension() == dim) {
                if (isRSController(worldIn, pos)) {
                    tooltip.add(TextFormatting.GREEN + "目标: RS控制器");
                } else {
                    tooltip.add(TextFormatting.GREEN + "目标: 容器");
                }
            }
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) return false;
        int stored = cap.getEnergyStorage().getEnergyStored();
        int max = cap.getEnergyStorage().getMaxEnergyStored();
        return max > 0 && stored < max;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) return 1.0;
        int stored = cap.getEnergyStorage().getEnergyStored();
        int max = cap.getEnergyStorage().getMaxEnergyStored();
        if (max <= 0) return 0.0;
        return 1.0 - ((double) stored / (double) max);
    }

    private static String formatFe(int fe) {
        if (fe >= 1_000_000) return String.format("%.1fM", fe / 1_000_000.0);
        if (fe >= 1_000) return String.format("%.1fK", fe / 1_000.0);
        return String.valueOf(fe);
    }

    private static int getEnergyCostPerItem() {
        int base = com.rsring.config.RsRingConfig.absorbRing.energyCostPerItem;
        double mult = com.rsring.config.RsRingConfig.absorbRing.energyCostMultiplier;
        if (base < 0) base = 0;
        if (mult < 0) mult = 0.0;
        double cost = base * mult;
        if (cost <= 0) return 0;
        return (int) Math.ceil(cost);
    }

    private static String getDimensionName(int dim) {
        switch (dim) {
            case 0: return "主世界";
            case -1: return "下界";
            case 1: return "末地";
            default: return "维度 " + dim;
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
        }
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        if (world.isRemote) {
            com.rsring.rsring.RsRingMod.proxy.openAbsorbRingGui(stack);
        } else {
            IRsRingCapability capability = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
            if (capability != null) {
                String mode = capability.isWhitelistMode() ? "白名单" : "黑名单";
                String status = capability.isEnabled() ? "已启用" : "已禁用";
                String bindInfo;
                if (capability.isBound()) {
                    BlockPos pos = capability.getTerminalPos();
                    int dim = capability.getTerminalDimension();
                    bindInfo = "已绑定: " + pos.getX() + "," + pos.getY() + "," + pos.getZ() + " (" + dim + ")";
                } else {
                    bindInfo = "未绑定";
                }
                String msg = net.minecraft.util.text.TextFormatting.GREEN + bindInfo + " | " + mode + " | " + status;
                player.sendMessage(new TextComponentString(msg));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                     EnumFacing facing, float hitX, float hitY, float hitZ) {
        return EnumActionResult.PASS;
    }

    public static boolean tryOpenAbsorbRingGui(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        com.rsring.service.RingDetectionSystem ringSystem = com.rsring.service.RingDetectionSystem.getInstance();
        com.rsring.experience.RingDetectionResult result = ringSystem.scanForRings(player);

        if (!result.hasRings()) {
            if (player.world.isRemote) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "未找到吸收戒指。"));
            }
            return false;
        }

        ItemStack absorbRing = findAbsorbRingInResults(result);

        if (absorbRing.isEmpty()) {
            if (player.world.isRemote) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "吸收戒指无法访问。"));
            }
            return false;
        }

        if (player.world.isRemote) {
            com.rsring.rsring.RsRingMod.proxy.openAbsorbRingGui(absorbRing);
            com.rsring.experience.RingDetectionResult.InventoryLocation location = findRingLocation(result, absorbRing);
            String locationName = location != null ? location.getDisplayName() : "未知";
            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "吸收戒指界面已打开。位置: " + locationName));
        } else {
            IRsRingCapability capability = absorbRing.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
            if (capability != null) {
                String mode = capability.isWhitelistMode() ? "白名单" : "黑名单";
                String msg = "戒指信息 | 过滤模式: " + mode + " | 能量: "
                           + capability.getEnergyStorage().getEnergyStored() + "/"
                           + capability.getEnergyStorage().getMaxEnergyStored() + " FE";
                player.sendMessage(new TextComponentString(msg));
            }
        }

        return true;
    }

    private static ItemStack findAbsorbRingInResults(com.rsring.experience.RingDetectionResult result) {
        for (ItemStack ring : result.getFoundRings()) {
            if (!ring.isEmpty() && ring.getItem() instanceof ItemAbsorbRing) {
                return ring;
            }
        }
        return ItemStack.EMPTY;
    }

    private static com.rsring.experience.RingDetectionResult.InventoryLocation findRingLocation(
            com.rsring.experience.RingDetectionResult result, ItemStack targetRing) {

        // Check each location for the target ring
        for (com.rsring.experience.RingDetectionResult.InventoryLocation location :
             com.rsring.experience.RingDetectionResult.InventoryLocation.values()) {

            for (ItemStack ring : result.getRingsFromLocation(location)) {
                if (ring == targetRing) {
                    return location;
                }
            }
        }

        return null;
    }

    public static boolean openAbsorbRingGuiFromAnyLocation(EntityPlayer player) {
        return tryOpenAbsorbRingGui(player);
    }

    public static boolean hasAccessibleAbsorbRing(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        // Use the RingDetectionSystem to check for absorb rings
        com.rsring.service.RingDetectionSystem ringSystem = com.rsring.service.RingDetectionSystem.getInstance();
        com.rsring.experience.RingDetectionResult result = ringSystem.scanForRings(player);

        if (!result.hasRings()) {
            return false;
        }

        // Check if any of the found rings are absorb rings
        for (ItemStack ring : result.getFoundRings()) {
            if (!ring.isEmpty() && ring.getItem() instanceof ItemAbsorbRing) {
                return true;
            }
        }

        return false;
    }

    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer)) return;

        EntityPlayer entityPlayer = (EntityPlayer) player;
        IRsRingCapability capability = itemstack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        RsRingCapability.refreshEnergyStorage(capability);
        if (capability != null && capability.isBound()) {
            com.rsring.capability.RsRingCapability.syncCapabilityToStack(itemstack, capability);
        }

        if (capability == null || !capability.isEnabled() || !capability.isBound()) return;
        IEnergyStorage energyStorage = capability.getEnergyStorage();
        int costPerItem = getEnergyCostPerItem();
        if (costPerItem > 0 && energyStorage.getEnergyStored() < costPerItem) return;

        int interval = Math.max(1, com.rsring.config.RsRingConfig.absorbRing.absorptionInterval);
        if (entityPlayer.ticksExisted % interval == 0) {
            absorbItemsToChest(entityPlayer, capability);
        }
    }

    public ICapabilityProvider initCapabilities(ItemStack stack, net.minecraft.nbt.NBTTagCompound nbt) {
        RsRingCapability.RsRingCapabilityProvider provider = new RsRingCapability.RsRingCapabilityProvider();
        net.minecraft.nbt.NBTTagCompound data = nbt;
        if ((data == null || data.getKeySet().isEmpty()) && stack.getTagCompound() != null) {
            if (stack.getTagCompound().hasKey("RsRingData")) {
                data = stack.getTagCompound().getCompoundTag("RsRingData");
            } else if (stack.getTagCompound().hasKey("ForgeCaps")) {
                net.minecraft.nbt.NBTTagCompound caps = stack.getTagCompound().getCompoundTag("ForgeCaps");
                if (caps.hasKey("rsring:chestring")) {
                    data = caps.getCompoundTag("rsring:chestring");
                } else if (caps.hasKey("rsring:rsring")) {
                    data = caps.getCompoundTag("rsring:rsring");
                }
            }
        }
        provider.initFromNBT(data);
        return provider;
    }

    @Override
    public net.minecraft.nbt.NBTTagCompound getNBTShareTag(ItemStack stack) {
        net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound() != null ? stack.getTagCompound().copy() : new net.minecraft.nbt.NBTTagCompound();
        IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap != null && RsRingCapability.RS_RING_CAPABILITY != null) {
            net.minecraft.nbt.NBTBase capNbt = RsRingCapability.RS_RING_CAPABILITY.getStorage().writeNBT(RsRingCapability.RS_RING_CAPABILITY, cap, null);
            if (capNbt instanceof net.minecraft.nbt.NBTTagCompound) {
                tag.setTag("RsRingData", (net.minecraft.nbt.NBTTagCompound) capNbt);
            }
        }
        return tag;
    }

    @Override
    public void readNBTShareTag(ItemStack stack, net.minecraft.nbt.NBTTagCompound nbt) {
        stack.setTagCompound(nbt);
        if (nbt != null && nbt.hasKey("RsRingData") && RsRingCapability.RS_RING_CAPABILITY != null) {
            net.minecraft.nbt.NBTTagCompound data = nbt.getCompoundTag("RsRingData");
            IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
            if (cap != null) {
                RsRingCapability.RS_RING_CAPABILITY.getStorage().readNBT(RsRingCapability.RS_RING_CAPABILITY, cap, null, data);
            }
        }
    }



    private void absorbItemsToChest(EntityPlayer player, IRsRingCapability capability) {
        if (capability == null || !capability.isBound()) return;
        if (capability.isWhitelistMode() && !hasAnyFilter(capability)) return;

        net.minecraft.world.World targetWorld = capability.getTerminalWorld();
        BlockPos targetPos = capability.getTerminalPos();
        if (targetWorld == null) {
            int dim = capability.getTerminalDimension();
            targetWorld = DimensionManager.getWorld(dim);
        }
        if (targetWorld == null || targetPos == null) return;

        targetWorld.getChunk(targetPos);
        if (!targetWorld.isBlockLoaded(targetPos)) return;

        double range = Math.max(1.0D, com.rsring.config.RsRingConfig.absorbRing.absorptionRange);
        List<net.minecraft.entity.item.EntityItem> items = player.world.getEntitiesWithinAABB(
            net.minecraft.entity.item.EntityItem.class,
            player.getEntityBoundingBox().grow(range)
        );

        IEnergyStorage energyStorage = capability.getEnergyStorage();
        int costPerItem = getEnergyCostPerItem();

        for (net.minecraft.entity.item.EntityItem item : items) {
            if (item.isDead) continue;
            ItemStack itemStack = item.getItem();
            if (itemStack.isEmpty()) continue;
            if (costPerItem > 0 && energyStorage.getEnergyStored() < costPerItem) continue;

            if (shouldFilterItem(capability, itemStack)) continue;

            int originalCount = itemStack.getCount();
            int maxAffordable = costPerItem > 0 ? Math.max(1, energyStorage.getEnergyStored() / costPerItem) : Integer.MAX_VALUE;
            int attemptCount = Math.min(originalCount, maxAffordable);
            ItemStack attemptStack = itemStack.copy();
            attemptStack.setCount(attemptCount);

            int inserted = 0;
            boolean isRSController = isRSController(targetWorld, targetPos);

            if (isRSController) {
                inserted = insertIntoRSNetwork(targetWorld, targetPos, attemptStack);
            } else {
                // 对于非RS控制器（普通箱子等），使用insertIntoChest方法
                inserted = insertIntoChest(targetWorld, targetPos, attemptStack);
            }

            if (inserted > 0) {
                if (costPerItem > 0) {
                    int energyToUse = Math.min(energyStorage.getEnergyStored(), inserted * costPerItem);
                    energyStorage.extractEnergy(energyToUse, false);
                }

                int remaining = originalCount - inserted;
                if (remaining <= 0) {
                    item.setDead();
                } else {
                    itemStack.setCount(remaining);
                    item.setItem(itemStack);
                }
            }

        }
    }


    private boolean hasAnyFilter(IRsRingCapability capability) {
        if (hasAnyDefaultFilter(capability.isWhitelistMode())) {
            return true;
        }
        for (int i = 0; i < 9; i++) {
            String filterName = capability.getFilterSlot(i);
            if (filterName != null && !filterName.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldFilterItem(IRsRingCapability capability, ItemStack itemStack) {
        boolean isWhitelistMode = capability.isWhitelistMode();
        String itemName = itemStack.getItem().getRegistryName().toString();
        boolean isInList = isInDefaultList(itemName, isWhitelistMode);

        for (int i = 0; i < 9; i++) {
            String filterName = capability.getFilterSlot(i);
            if (filterName != null && !filterName.isEmpty() && filterName.equals(itemName)) {
                isInList = true;
                break;
            }
        }

        if (isWhitelistMode) {
            return !isInList;
        } else {
            return isInList;
        }
    }

    private boolean hasAnyDefaultFilter(boolean whitelistMode) {
        String[] items = whitelistMode
            ? com.rsring.config.RsRingConfig.absorbRing.defaultWhitelistItems
            : com.rsring.config.RsRingConfig.absorbRing.defaultBlacklistItems;
        if (items == null) return false;
        for (String item : items) {
            if (item != null && !item.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isInDefaultList(String itemName, boolean whitelistMode) {
        if (itemName == null || itemName.isEmpty()) return false;
        String[] items = whitelistMode
            ? com.rsring.config.RsRingConfig.absorbRing.defaultWhitelistItems
            : com.rsring.config.RsRingConfig.absorbRing.defaultBlacklistItems;
        if (items == null) return false;
        for (String item : items) {
            if (item == null) continue;
            String formatted = item.trim();
            if (formatted.isEmpty()) continue;
            if (!formatted.contains(":")) {
                formatted = "minecraft:" + formatted;
            }
            if (formatted.equals(itemName)) return true;
        }
        return false;
    }

    private int insertIntoChest(World world, BlockPos pos, ItemStack stack) {
        if (world == null || pos == null || stack.isEmpty()) return 0;
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return 0;

        for (EnumFacing f : EnumFacing.VALUES) {
            int inserted = tryInsert(te, f, stack);
            if (inserted > 0) return inserted;
        }
        return tryInsert(te, null, stack);
    }

    private int tryInsert(TileEntity te, EnumFacing facing, ItemStack stack) {
        if (!te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) return 0;
        IItemHandler h = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        if (h == null || h.getSlots() == 0) return 0;
        int before = stack.getCount();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(h, stack.copy(), false);
        int inserted = before - remainder.getCount();
        if (inserted > 0) stack.setCount(remainder.getCount());
        return inserted;
    }

    private boolean isRSController(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        net.minecraft.util.ResourceLocation regName = world.getBlockState(pos).getBlock().getRegistryName();
        if (regName == null) return false;
        String blockName = regName.toString().toLowerCase();
        return blockName.equals("refinedstorage:controller");
    }

    private int insertIntoRSNetwork(World world, BlockPos pos, ItemStack stack) {
        if (world == null || pos == null || stack.isEmpty()) return 0;
        try {
            Class<?> apiClass = Class.forName("com.raoulvdberge.refinedstorage.apiimpl.API");
            Object api = apiClass.getMethod("instance").invoke(null);
            Object network = getNetworkFromNodeManager(api, world, pos);
            if (network == null) {
                network = getNetworkFromTile(world, pos);
            }
            if (network == null) {
                return 0;
            }

            // Preferred path for RS 1.6.x: INetwork.insertItem(ItemStack, int/long, Action) -> remainder
            Class<?> actionClass = Class.forName("com.raoulvdberge.refinedstorage.api.util.Action");
            Object perform = java.lang.Enum.valueOf((Class<? extends java.lang.Enum>) actionClass, "PERFORM");
            try {
                java.lang.reflect.Method insert;
                Object remainderObj;
                try {
                    insert = network.getClass().getMethod("insertItem", ItemStack.class, int.class, actionClass);
                    remainderObj = insert.invoke(network, stack.copy(), stack.getCount(), perform);
                } catch (NoSuchMethodException e) {
                    insert = network.getClass().getMethod("insertItem", ItemStack.class, long.class, actionClass);
                    remainderObj = insert.invoke(network, stack.copy(), (long) stack.getCount(), perform);
                }
                if (remainderObj == null) {
                    return stack.getCount();
                }
                if (remainderObj instanceof ItemStack) {
                    ItemStack remainder = (ItemStack) remainderObj;
                    int inserted = Math.max(0, stack.getCount() - remainder.getCount());
                    return inserted;
                }
            } catch (NoSuchMethodException ignored) {
                return 0;
            }
        } catch (Throwable t) {
            return 0;
        }
        return 0;
    }

    private Object getNetworkFromNodeManager(Object api, World world, BlockPos pos) {
        try {
            java.lang.reflect.Method getNodeManager = api.getClass().getMethod("getNetworkNodeManager", World.class);
            Object nodeManager = getNodeManager.invoke(api, world);
            if (nodeManager == null) return null;

            java.lang.reflect.Method getNode = nodeManager.getClass().getMethod("getNode", BlockPos.class);
            Object node = getNode.invoke(nodeManager, pos);
            if (node == null) return null;

            java.lang.reflect.Method getNetwork = node.getClass().getMethod("getNetwork");
            return getNetwork.invoke(node);
        } catch (Throwable t) {
            return null;
        }
    }

    private Object getNetworkFromTile(World world, BlockPos pos) {
        try {
            TileEntity te = world.getTileEntity(pos);
            if (te == null) return null;

            // Try capability INetworkNodeProxy -> getNode() -> getNetwork()
            try {
                Class<?> capClass = Class.forName("com.raoulvdberge.refinedstorage.capability.CapabilityNetworkNodeProxy");
                java.lang.reflect.Field capField = capClass.getField("NETWORK_NODE_PROXY_CAPABILITY");
                Object cap = capField.get(null);
                if (cap != null) {
                    java.lang.reflect.Method getCap = te.getClass().getMethod("getCapability",
                        net.minecraftforge.common.capabilities.Capability.class, net.minecraft.util.EnumFacing.class);
                    Object proxy = getCap.invoke(te, cap, null);
                    if (proxy != null) {
                        java.lang.reflect.Method getNode = proxy.getClass().getMethod("getNode");
                        Object node = getNode.invoke(proxy);
                        if (node != null) {
                            java.lang.reflect.Method getNetwork = node.getClass().getMethod("getNetwork");
                            Object net = getNetwork.invoke(node);
                            if (net != null) return net;
                        }
                    }
                }
            } catch (Throwable t) {
                // ignore
            }

            // Direct getNetwork() on tile
            for (java.lang.reflect.Method m : te.getClass().getMethods()) {
                if ("getNetwork".equals(m.getName()) && m.getParameterTypes().length == 0) {
                    Object net = m.invoke(te);
                    if (net != null) return net;
                }
            }

            // getNode() -> getNetwork()
            for (java.lang.reflect.Method m : te.getClass().getMethods()) {
                if (!"getNode".equals(m.getName()) || m.getParameterTypes().length != 0) continue;
                Object node = m.invoke(te);
                if (node == null) continue;
                for (java.lang.reflect.Method nm : node.getClass().getMethods()) {
                    if ("getNetwork".equals(nm.getName()) && nm.getParameterTypes().length == 0) {
                        Object net = nm.invoke(node);
                        if (net != null) return net;
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public void onPlayerBaubleRender(ItemStack stack, EntityPlayer player, float partialTicks) {
        GlStateManager.pushMatrix();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
    }
}
