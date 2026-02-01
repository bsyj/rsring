package com.moremod.item;

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
import com.moremod.capability.IRsRingCapability;
import com.moremod.capability.RsRingCapability;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Optional;
import baubles.api.IBauble;
import baubles.api.BaubleType;
import org.lwjgl.input.Keyboard;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemChestRing extends Item implements IBauble {

    public ItemChestRing() {
        super();
        this.setTranslationKey("rsring.chestring");
        this.setRegistryName(new ResourceLocation("rsring", "chestring"));
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) return;

        IEnergyStorage energy = cap.getEnergyStorage();
        tooltip.add(TextFormatting.GRAY + "能量: " + TextFormatting.YELLOW + formatFe(energy.getEnergyStored()) + TextFormatting.GRAY + " / " + formatFe(energy.getMaxEnergyStored()) + " FE");
        tooltip.add(TextFormatting.GRAY + "状态: " + (cap.isEnabled() ? TextFormatting.GREEN + "已开启" : TextFormatting.RED + "已关闭"));
        if (cap.isBound()) {
            tooltip.add(TextFormatting.GRAY + "绑定: " + TextFormatting.AQUA + cap.getTerminalPos().getX() + ", " + cap.getTerminalPos().getY() + ", " + cap.getTerminalPos().getZ());
            tooltip.add(TextFormatting.GRAY + "维度: " + TextFormatting.AQUA + getDimensionName(cap.getTerminalDimension()));
        } else {
            tooltip.add(TextFormatting.GRAY + "未绑定");
        }
        boolean showDetail = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (!showDetail) {
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " + TextFormatting.YELLOW + "Shift" + TextFormatting.DARK_GRAY + " 查看功能介绍");
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "功能介绍:");
            tooltip.add(TextFormatting.GRAY + "  · 吸收8格内掉落物到绑定箱子");
            tooltip.add(TextFormatting.GRAY + "  · 支持跨维度传输");
            tooltip.add(TextFormatting.GRAY + "  · 1 FE/每个物品");
            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "使用方法:");
            tooltip.add(TextFormatting.GRAY + "  1. 蹲下+右键箱子/容器绑定");
            tooltip.add(TextFormatting.GRAY + "  2. 用FE充能器充电 (最大10M FE)");
            tooltip.add(TextFormatting.GRAY + "  3. 按K键开启/关闭吸收功能");
            tooltip.add(TextFormatting.GRAY + "  4. 戒指在背包/饰品栏/手持均可生效");
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
        ItemStack stack = player.getHeldItem(hand);
        
        // 修复：蹲下时不打开GUI（可能是在准备绑定箱子）
        if (player.isSneaking()) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        
        // 只有在右键空气时才打开GUI，避免与绑定功能冲突
        // onItemRightClick只在右键空气时调用
        if (world.isRemote) {
            // 使用proxy打开黑白名单GUI
            com.moremod.rsring.RsRingMod.proxy.openChestRingGui(stack);
        } else {
            IRsRingCapability capability = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
            if (capability != null) {
                String mode = capability.isWhitelistMode() ? "白名单" : "黑名单";
                String msg = capability.isBound()
                    ? "已绑定 | 当前模式: " + mode + " | 能量: " + capability.getEnergyStorage().getEnergyStored() + "/" + capability.getEnergyStorage().getMaxEnergyStored() + " FE | " + (capability.isEnabled() ? "已开启" : "已关闭")
                    : "蹲下 + 右键箱子绑定 | 当前模式: " + mode + " | 按K切换功能";
                player.sendMessage(new TextComponentString(msg));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, 
                                     EnumFacing facing, float hitX, float hitY, float hitZ) {
        // 修复Bug 7: 蹲下右键绑定箱子时不打开GUI
        ItemStack stack = player.getHeldItem(hand);
        
        // 如果玩家蹲下，执行绑定逻辑（不打开GUI）
        if (player.isSneaking()) {
            if (!world.isRemote) {
                IRsRingCapability capability = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
                if (capability != null) {
                    // 绑定箱子
                    capability.bindTerminal(world, pos);
                    RsRingCapability.syncCapabilityToStack(stack, capability);
                    player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.GREEN + "已绑定箱子位置: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
                }
            }
            return EnumActionResult.SUCCESS; // 成功绑定，不打开GUI
        }
        
        // 如果玩家没有蹲下，返回PASS让其他处理器处理
        return EnumActionResult.PASS;
    }
    
    /**
     * Provides improved GUI access for chest rings regardless of their inventory location.
     * Implements Requirements 2.1, 2.2, 2.3, 2.4 for enhanced ring GUI access.
     * 
     * This method allows accessing the chest ring GUI from any valid inventory location:
     * - When held in main hand or off hand
     * - When equipped in Baubles ring slots
     * - When stored in player inventory or hotbar
     * 
     * @param player The player attempting to access the ring GUI
     * @return true if GUI was successfully opened, false otherwise
     */
    public static boolean tryOpenChestRingGui(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        
        // Use the RingDetectionSystem to find any chest ring in player's inventory
        com.moremod.service.RingDetectionSystem ringSystem = com.moremod.service.RingDetectionSystem.getInstance();
        com.moremod.experience.RingDetectionResult result = ringSystem.scanForRings(player);
        
        if (!result.hasRings()) {
            // No rings found - provide feedback
            if (player.world.isRemote) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "未找到箱子戒指！请确保戒指在背包、快捷栏或饰品栏中。"));
            }
            return false;
        }
        
        // Find the first chest ring in the detection results
        ItemStack chestRing = findChestRingInResults(result);
        
        if (chestRing.isEmpty()) {
            // No chest rings found (might have other ring types)
            if (player.world.isRemote) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "未找到箱子戒指！找到了其他类型的戒指，但需要箱子戒指才能打开黑白名单界面。"));
            }
            return false;
        }
        
        // Open the blacklist/whitelist GUI for the found chest ring
        if (player.world.isRemote) {
            // 使用proxy打开黑白名单GUI
            com.moremod.rsring.RsRingMod.proxy.openChestRingGui(chestRing);
            
            // Provide feedback about successful access
            com.moremod.experience.RingDetectionResult.InventoryLocation location = findRingLocation(result, chestRing);
            String locationName = location != null ? location.getDisplayName() : "未知位置";
            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "已打开箱子戒指黑白名单界面！位置：" + locationName));
        } else {
            // Server-side feedback
            IRsRingCapability capability = chestRing.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
            if (capability != null) {
                String mode = capability.isWhitelistMode() ? "白名单" : "黑名单";
                String msg = "箱子戒指黑白名单已激活 | 当前模式: " + mode + " | 能量: " + 
                           capability.getEnergyStorage().getEnergyStored() + "/" + 
                           capability.getEnergyStorage().getMaxEnergyStored() + " FE";
                player.sendMessage(new TextComponentString(msg));
            }
        }
        
        return true;
    }
    
    /**
     * Finds the first chest ring in the detection results.
     * 
     * @param result The ring detection result
     * @return The first chest ring found, or ItemStack.EMPTY if none found
     */
    private static ItemStack findChestRingInResults(com.moremod.experience.RingDetectionResult result) {
        for (ItemStack ring : result.getFoundRings()) {
            if (!ring.isEmpty() && ring.getItem() instanceof ItemChestRing) {
                return ring;
            }
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Finds the location of a specific ring in the detection results.
     * 
     * @param result The ring detection result
     * @param targetRing The ring to find the location for
     * @return The location of the ring, or null if not found
     */
    private static com.moremod.experience.RingDetectionResult.InventoryLocation findRingLocation(
            com.moremod.experience.RingDetectionResult result, ItemStack targetRing) {
        
        // Check each location for the target ring
        for (com.moremod.experience.RingDetectionResult.InventoryLocation location : 
             com.moremod.experience.RingDetectionResult.InventoryLocation.values()) {
            
            for (ItemStack ring : result.getRingsFromLocation(location)) {
                if (ring == targetRing) {
                    return location;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Alternative GUI access method that can be triggered by key bindings or other events.
     * Implements Requirements 2.1, 2.2, 2.3 for multiple access methods.
     * 
     * This method provides an alternative way to access the chest ring GUI without
     * requiring the player to hold the ring and right-click air.
     * 
     * @param player The player attempting to access the ring GUI
     * @return true if GUI was successfully opened, false otherwise
     */
    public static boolean openChestRingGuiFromAnyLocation(EntityPlayer player) {
        return tryOpenChestRingGui(player);
    }
    
    /**
     * Checks if the player has any accessible chest rings in their inventory.
     * Implements Requirement 2.4 for location-independent ring access.
     * 
     * @param player The player to check
     * @return true if the player has accessible chest rings, false otherwise
     */
    public static boolean hasAccessibleChestRing(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        
        // Use the RingDetectionSystem to check for chest rings
        com.moremod.service.RingDetectionSystem ringSystem = com.moremod.service.RingDetectionSystem.getInstance();
        com.moremod.experience.RingDetectionResult result = ringSystem.scanForRings(player);
        
        if (!result.hasRings()) {
            return false;
        }
        
        // Check if any of the found rings are chest rings
        for (ItemStack ring : result.getFoundRings()) {
            if (!ring.isEmpty() && ring.getItem() instanceof ItemChestRing) {
                return true;
            }
        }
        
        return false;
    }

    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer)) return;

        EntityPlayer entityPlayer = (EntityPlayer) player;
        IRsRingCapability capability = itemstack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);

        if (capability != null && capability.isBound()) {
            com.moremod.capability.RsRingCapability.syncCapabilityToStack(itemstack, capability);
        }

        if (capability == null || !capability.isEnabled() || !capability.isBound()) return;
        IEnergyStorage energyStorage = capability.getEnergyStorage();
        if (energyStorage.getEnergyStored() < 1) return;

        if (entityPlayer.ticksExisted % 5 == 0) {
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
                if (caps.hasKey("rsring:chestring")) data = caps.getCompoundTag("rsring:chestring");
                else if (caps.hasKey("rsring:rsring")) data = caps.getCompoundTag("rsring:rsring");
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

    /**
     * 吸收物品到绑定的箱子
     * 修复Bug 6: 支持跨维度吸取功能
     */
    private void absorbItemsToChest(EntityPlayer player, IRsRingCapability capability) {
        if (!capability.isBound()) return;

        // 修复Bug 6: 支持跨维度传输
        World targetWorld = capability.getTerminalWorld();
        BlockPos targetPos = capability.getTerminalPos();
        
        // 如果目标世界为null，尝试通过维度ID获取
        if (targetWorld == null) {
            int targetDim = capability.getTerminalDimension();
            targetWorld = net.minecraftforge.common.DimensionManager.getWorld(targetDim);
        }
        
        if (targetWorld == null || targetPos == null) return;
        
        // 确保目标区块已加载（跨维度时尤其重要）
        if (!targetWorld.isBlockLoaded(targetPos)) {
            targetWorld.getChunk(targetPos);
            if (!targetWorld.isBlockLoaded(targetPos)) return;
        }

        List<net.minecraft.entity.item.EntityItem> items = player.world.getEntitiesWithinAABB(
            net.minecraft.entity.item.EntityItem.class,
            player.getEntityBoundingBox().grow(8.0)
        );

        IEnergyStorage energyStorage = capability.getEnergyStorage();
        for (net.minecraft.entity.item.EntityItem item : items) {
            if (item.isDead) continue;
            ItemStack itemStack = item.getItem();
            if (itemStack.isEmpty() || energyStorage.getEnergyStored() < 1) continue;

            // 检查黑白名单（修复Bug 6）
            if (shouldFilterItem(capability, itemStack)) continue;

            int inserted = insertIntoChest(targetWorld, targetPos, itemStack);
            if (inserted > 0 && energyStorage.getEnergyStored() >= inserted) {
                energyStorage.extractEnergy(inserted, false);
                if (itemStack.isEmpty()) item.setDead();
            }
        }
    }

    /**
     * 检查物品是否应该被过滤（根据黑白名单）
     * 修复Bug 6: 黑白名单功能修复
     */
    private boolean shouldFilterItem(IRsRingCapability capability, ItemStack itemStack) {
        boolean isWhitelistMode = capability.isWhitelistMode();
        java.util.List<String> filterItems = capability.getBlacklistItems();

        String itemName = itemStack.getItem().getRegistryName().toString();
        boolean isInList = false;

        // 检查是否在过滤列表中（前9个槽位是过滤槽）
        for (int i = 0; i < Math.min(9, filterItems.size()); i++) {
            String filterName = capability.getFilterSlot(i);
            if (filterName != null && !filterName.isEmpty() && filterName.equals(itemName)) {
                isInList = true;
                break;
            }
        }

        // 白名单模式：只接受列表中的物品
        if (isWhitelistMode) {
            return !isInList; // 如果不在列表中，则过滤掉
        } else {
            // 黑名单模式：拒绝列表中的物品
            return isInList; // 如果在列表中，则过滤掉
        }
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

    @SideOnly(Side.CLIENT)
    public void onPlayerBaubleRender(ItemStack stack, EntityPlayer player, float partialTicks) {
        GlStateManager.pushMatrix();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
    }
}
