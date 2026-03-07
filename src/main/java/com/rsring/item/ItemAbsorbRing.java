package com.rsring.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.resources.I18n;
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
import com.rsring.filter.FilterMode;
import com.rsring.filter.ItemAttribute;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Optional;
import baubles.api.IBauble;
import baubles.api.BaubleType;
import org.lwjgl.input.Keyboard;
import com.rsring.compat.CompatManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemAbsorbRing extends Item implements IBauble {
    private static final Logger LOGGER = LogManager.getLogger(ItemAbsorbRing.class);
    
    // 垃圾箱警告冷却机制：每个玩家60秒只提示一次
    private static final Map<UUID, Long> TRASH_CAN_WARNING_COOLDOWN = new HashMap<>();
    private static final long WARNING_COOLDOWN_MS = 60000; // 60秒
    
    // 吸收箱警告冷却机制：每个玩家60秒只提示一次
    private static final Map<UUID, Long> TERMINAL_WARNING_COOLDOWN = new HashMap<>();
    private static final long TERMINAL_WARNING_COOLDOWN_MS = 60000; // 60秒
    
    /**
     * 发送垃圾箱警告（带冷却）
     */
    private static void sendTrashCanWarning(EntityPlayer player, String message) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueID();
        long lastWarning = TRASH_CAN_WARNING_COOLDOWN.getOrDefault(playerId, 0L);
        
        if (now - lastWarning > WARNING_COOLDOWN_MS) {
            player.sendMessage(new TextComponentString(TextFormatting.YELLOW + message));
            TRASH_CAN_WARNING_COOLDOWN.put(playerId, now);
        }
    }
    
    /**
     * 发送吸收箱警告（带冷却）
     */
    private static void sendTerminalWarning(EntityPlayer player, String message) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueID();
        long lastWarning = TERMINAL_WARNING_COOLDOWN.getOrDefault(playerId, 0L);
        
        if (now - lastWarning > TERMINAL_WARNING_COOLDOWN_MS) {
            player.sendMessage(new TextComponentString(TextFormatting.YELLOW + message));
            TERMINAL_WARNING_COOLDOWN.put(playerId, now);
        }
    }

    public ItemAbsorbRing() {
        super();
        this.setTranslationKey("rsring.item_absorb_ring");
        this.setRegistryName(new ResourceLocation("rsring", "item_absorb_ring"));
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.MISC);
    }
    
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap != null && cap.isEasterEgg()) {
            // 彩蛋戒指始终显示彩蛋名称，忽略重命名
            return TextFormatting.GOLD + "至尊狂傲暴龙灭杀战神";
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        if (cap == null) return;
        
        // 彩蛋戒指特殊标识
        if (cap.isEasterEgg()) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + "[ 彩蛋觉醒 ]");
            tooltip.add(TextFormatting.YELLOW + "  +5 幸运值 (装备时)");
            tooltip.add("");
        }

        IEnergyStorage energy = cap.getEnergyStorage();
        tooltip.add(TextFormatting.GRAY + "能量: " + TextFormatting.YELLOW + formatFe(energy.getEnergyStored())
            + TextFormatting.GRAY + " / " + formatFe(energy.getMaxEnergyStored()) + " FE");
        tooltip.add(TextFormatting.GRAY + "状态: " + (cap.isEnabled() ? TextFormatting.GREEN + "已启用"
            : TextFormatting.RED + "已禁用"));
        tooltip.add(TextFormatting.GRAY + "密封: " + (cap.isSealed() ? TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.rsring.sealed.yes")
            : TextFormatting.DARK_GRAY + I18n.format("tooltip.rsring.sealed.no")));
        // 绑定状态显示
        if (cap.isBound()) {
            BlockPos pos = cap.getTerminalPos();
            String dim = getDimensionName(cap.getTerminalDimension());
            tooltip.add(TextFormatting.GRAY + "已绑定: " + TextFormatting.AQUA + pos.getX() + "," + pos.getY()
                + "," + pos.getZ() + TextFormatting.GRAY + " (" + TextFormatting.AQUA + dim + TextFormatting.GRAY + ")");
        } else {
            tooltip.add(TextFormatting.GRAY + "吸收箱: " + TextFormatting.DARK_GRAY + "未绑定");
        }
        
        // 垃圾箱绑定状态
        if (cap.isTrashCanBound()) {
            BlockPos trashPos = cap.getTrashCanPos();
            String trashDim = getDimensionName(cap.getTrashCanDimension());
            tooltip.add(TextFormatting.GRAY + "垃圾箱: " + TextFormatting.RED + trashPos.getX() + "," + trashPos.getY()
                + "," + trashPos.getZ() + TextFormatting.GRAY + " (" + TextFormatting.RED + trashDim + TextFormatting.GRAY + ")");
        } else {
            tooltip.add(TextFormatting.GRAY + "垃圾箱: " + TextFormatting.DARK_GRAY + "未绑定");
        }
        
        // 过滤模式（始终显示，与绑定状态无关）
        FilterMode filterMode = cap.getFilterMode();
        String modeType;
        switch (filterMode) {
            case MOD:
                modeType = "模组";
                break;
            case ATTRIBUTE:
                modeType = "属性";
                break;
            case ITEM:
            default:
                modeType = "ID";
                break;
        }
        String listMode = cap.isWhitelistMode() ? "白名单" : "黑名单";
        tooltip.add(TextFormatting.GRAY + "过滤模式: " + TextFormatting.AQUA + modeType + "-" + listMode);
        
        // 销毁模式（始终显示，与绑定状态无关）
        FilterMode destroyFilterMode = cap.getDestroyFilterMode();
        String destroyModeType;
        switch (destroyFilterMode) {
            case MOD:
                destroyModeType = "模组";
                break;
            case ATTRIBUTE:
                destroyModeType = "属性";
                break;
            case ITEM:
            default:
                destroyModeType = "ID";
                break;
        }
        String destroyListMode = cap.isDestroyWhitelistMode() ? "白名单" : "黑名单";
        String destroyStatus = cap.isDestroyEnabled() ? "开" : "关";
        TextFormatting statusColor = cap.isDestroyEnabled() ? TextFormatting.GREEN : TextFormatting.RED;
        tooltip.add(TextFormatting.GRAY + "销毁模式: " + TextFormatting.RED + destroyModeType + "-" + destroyListMode 
            + TextFormatting.GRAY + " (" + statusColor + destroyStatus + TextFormatting.GRAY + ")");

        boolean showDetail = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (!showDetail) {
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " + TextFormatting.YELLOW + "Shift"
                + TextFormatting.DARK_GRAY + " 查看详情");
            return;
        }

        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "[ 功能特点 ]");
        tooltip.add(TextFormatting.GRAY + "  - 自动吸收附近掉落物");
        tooltip.add(TextFormatting.GRAY + "  - 三种过滤模式: ID/模组/属性");
        tooltip.add(TextFormatting.GRAY + "  - 独立垃圾箱与销毁系统");
        tooltip.add(TextFormatting.GRAY + "  - 支持模组背包(饰品槽)");

        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "[ 快捷操作 ]");
        tooltip.add(TextFormatting.AQUA + "  右键空气" + TextFormatting.DARK_GRAY + " - 打开过滤设置");
        tooltip.add(TextFormatting.AQUA + "  潜行+右键" + TextFormatting.DARK_GRAY + " - 绑定吸收箱");
        tooltip.add(TextFormatting.AQUA + "  潜行+左键" + TextFormatting.DARK_GRAY + " - 绑定垃圾箱");
        tooltip.add(TextFormatting.AQUA + "  K键" + TextFormatting.DARK_GRAY + " - 切换开关");
        tooltip.add(TextFormatting.AQUA + "  工作台" + TextFormatting.DARK_GRAY + " - 切换密封模式");

        // 显示绑定目标类型
        if (cap.isBound() && worldIn != null) {
            BlockPos pos = cap.getTerminalPos();
            int dim = cap.getTerminalDimension();
            if (worldIn.provider.getDimension() == dim) {
                String targetType;
                if (isRSController(worldIn, pos)) {
                    targetType = "RS控制器";
                } else if (com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.isPlacedBackpack(worldIn, pos)) {
                    targetType = "背包";
                } else {
                    targetType = "容器";
                }
                tooltip.add("");
                tooltip.add(TextFormatting.GREEN + "[OK] 目标类型: " + targetType);
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

    public static int getEnergyCostPerItem() {
        int base = com.rsring.config.RsRingConfig.absorbRing.energyCostPerItem;
        double mult = com.rsring.config.RsRingConfig.absorbRing.energyCostMultiplier;
        // 确保最小成本为1，防止免费吸收/销毁
        if (base < 1) base = 1;
        if (mult < 0.1) mult = 0.1;
        double cost = base * mult;
        if (cost < 1) return 1; // 最小成本为1
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
    @Optional.Method(modid = "baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer)) return;

        EntityPlayer entityPlayer = (EntityPlayer) player;
        IRsRingCapability capability = itemstack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);

        // 装备彩蛋戒指时添加幸运属性
        if (capability != null && capability.isEasterEgg()) {
            int slot = findBaubleSlot(entityPlayer, itemstack);
            applyLuckAttribute(entityPlayer, slot);
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer)) return;
        
        EntityPlayer entityPlayer = (EntityPlayer) player;
        IRsRingCapability capability = itemstack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
        
        // 卸下彩蛋戒指时移除幸运属性
        if (capability != null && capability.isEasterEgg()) {
            removeLuckAttribute(entityPlayer);
        }
    }
    
    // 彩蛋戒指幸运属性基础UUID（用于生成槽位相关UUID）
    private static final java.util.UUID LUCK_ATTRIBUTE_BASE_UUID = java.util.UUID.fromString("e3b4a5c7-8d9e-4f0a-1b2c-3d4e5f6a7b8c");
    private static final String LUCK_ATTRIBUTE_NAME = "rsring.easter_egg.luck";
    
    /**
     * 查找物品在饰品栏中的槽位索引
     */
    private int findBaubleSlot(EntityPlayer player, ItemStack targetStack) {
        if (!com.rsring.util.BaublesHelper.isBaublesLoaded()) return -1;
        
        Object handler = com.rsring.util.BaublesHelper.getBaublesHandler(player);
        if (handler == null) return -1;
        
        int size = com.rsring.util.BaublesHelper.getSlots(handler);
        for (int i = 0; i < size; i++) {
            ItemStack stack = com.rsring.util.BaublesHelper.getStackInSlot(handler, i);
            if (stack == targetStack) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 生成槽位相关的UUID
     */
    private java.util.UUID getSlotUUID(int slot) {
        // 基于基础UUID和槽位索引生成唯一UUID
        long most = LUCK_ATTRIBUTE_BASE_UUID.getMostSignificantBits();
        long least = LUCK_ATTRIBUTE_BASE_UUID.getLeastSignificantBits() + slot;
        return new java.util.UUID(most, least);
    }
    
    /**
     * 应用幸运属性修饰符
     * @param slot 饰品槽位索引，-1表示使用基础UUID
     */
    private void applyLuckAttribute(EntityPlayer player, int slot) {
        net.minecraft.entity.ai.attributes.IAttributeInstance luckAttribute = player.getEntityAttribute(
            net.minecraft.entity.SharedMonsterAttributes.LUCK);
        if (luckAttribute == null) return;
        
        java.util.UUID uuid = getSlotUUID(slot >= 0 ? slot : 0);
        
        // 检查是否已存在修饰符
        if (luckAttribute.getModifier(uuid) != null) return;
        
        // 添加幸运+5修饰符 (0 = ADDITION)
        net.minecraft.entity.ai.attributes.AttributeModifier modifier = new net.minecraft.entity.ai.attributes.AttributeModifier(
            uuid, LUCK_ATTRIBUTE_NAME + "_" + slot, 5.0, 0);
        luckAttribute.applyModifier(modifier);
    }
    
    /**
     * 移除所有彩蛋戒指的幸运属性修饰符
     */
    private void removeLuckAttribute(EntityPlayer player) {
        net.minecraft.entity.ai.attributes.IAttributeInstance luckAttribute = player.getEntityAttribute(
            net.minecraft.entity.SharedMonsterAttributes.LUCK);
        if (luckAttribute == null) return;
        
        // 移除所有可能的槽位修饰符（最多50个槽位）
        for (int i = 0; i < 50; i++) {
            java.util.UUID uuid = getSlotUUID(i);
            net.minecraft.entity.ai.attributes.AttributeModifier modifier = luckAttribute.getModifier(uuid);
            if (modifier != null) {
                luckAttribute.removeModifier(modifier);
            }
        }
    }
    
    /**
     * 重新应用所有彩蛋戒指的幸运属性（用于玩家登录时）
     */
    public static void reapplyAllEasterEggLuck(EntityPlayer player) {
        if (player.world.isRemote) return;
        if (!com.rsring.util.BaublesHelper.isBaublesLoaded()) return;
        
        Object handler = com.rsring.util.BaublesHelper.getBaublesHandler(player);
        if (handler == null) return;
        
        int size = com.rsring.util.BaublesHelper.getSlots(handler);
        for (int i = 0; i < size; i++) {
            ItemStack stack = com.rsring.util.BaublesHelper.getStackInSlot(handler, i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemAbsorbRing)) continue;
            
            IRsRingCapability cap = stack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
            if (cap != null && cap.isEasterEgg()) {
                ItemAbsorbRing ring = (ItemAbsorbRing) stack.getItem();
                ring.applyLuckAttribute(player, i);
            }
        }
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

        // 背包模式：未绑定吸收箱时也可以吸收到背包
        if (capability == null || !capability.isEnabled()) return;

        // 检查是否在GUI内工作
        if (!capability.shouldWorkInGUI() && entityPlayer.openContainer != entityPlayer.inventoryContainer) {
            return;
        }

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
        if (capability == null) return;

        // 检查是否启用了背包优先模式
        boolean preferUsefulBackpacks = com.rsring.config.RsRingConfig.usefulBackpacksCompat.preferBackpacks;
        boolean preferWearableBackpacks = com.rsring.config.RsRingConfig.wearableBackpacksCompat.preferBackpacks;
        boolean preferBackpacks = preferUsefulBackpacks || preferWearableBackpacks;
        boolean isBound = capability.isBound();

        // 如果没有绑定目标且没有启用任何背包兼容，直接返回
        if (!isBound && !CompatManager.isAnyBackpackModAvailable()) {
            return;
        }

        // 如果绑定了目标且没有启用优先背包模式，检查过滤条件
        if (isBound && !preferBackpacks && capability.isWhitelistMode() && !hasAnyFilter(capability)) return;

        double range = Math.max(1.0D, com.rsring.config.RsRingConfig.absorbRing.absorptionRange);
        List<net.minecraft.entity.item.EntityItem> items = player.world.getEntitiesWithinAABB(
            net.minecraft.entity.item.EntityItem.class,
            player.getEntityBoundingBox().grow(range)
        );

        IEnergyStorage energyStorage = capability.getEnergyStorage();
        int costPerItem = getEnergyCostPerItem();

        // 检查玩家是否有背包模组的背包
        boolean hasBackpack = CompatManager.isAnyBackpackModAvailable() && CompatManager.hasAnyBackpack(player);

        for (net.minecraft.entity.item.EntityItem item : items) {
            // 并发安全：对物品实体加锁处理
            synchronized (item) {
                // 二次检查：可能在等待锁期间被其他线程处理
                if (item.isDead) continue;
                ItemStack itemStack = item.getItem();
                if (itemStack.isEmpty()) continue;
                
                // 再次检查，防止获取物品后状态变化
                if (item.isDead) continue;
                
                if (costPerItem > 0 && energyStorage.getEnergyStored() < costPerItem) continue;

                // 检查销毁模式
                if (capability.isDestroyEnabled() && shouldDestroyItem(capability, itemStack)) {
                    // 有背包的情况：STORAGE_OVERFLOW模式需要先吸收再销毁剩余
                    if (hasBackpack && capability.getDestroyModeType() == com.rsring.capability.DestroyModeType.STORAGE_OVERFLOW) {
                        // 先尝试吸收到背包，剩余物品销毁
                        // 注意：只存入背包，不存入绑定吸收箱
                        handleItemAbsorptionForDestroyFlow(player, capability, item, itemStack, energyStorage, costPerItem);
                    } else {
                        // 无背包 或 ALWAYS/SLOT_OVERFLOW模式：直接销毁
                        handleDestroyForGroundItem(player, capability, item, itemStack, energyStorage, costPerItem);
                    }
                    continue;
                }

                // 检查吸收过滤（所有模式都要遵循，包括背包模式）
                if (shouldFilterItem(capability, itemStack)) continue;

                // 处理物品吸收
                handleItemAbsorption(player, capability, item, itemStack, energyStorage, costPerItem, isBound, preferBackpacks);
            }
        }
    }

    /**
     * 处理地上物品的销毁流程
     * 
     * 地面物品没有容量限制问题，匹配销毁过滤器的地上物品直接销毁：
     * 1. 优先送入垃圾箱
     * 2. 垃圾箱无法接收则真正销毁
     * 
     * 注意：销毁类型判断(ALWAYS/SLOT_OVERFLOW/STORAGE_OVERFLOW)只适用于
     * 定时清理背包内物品，不适用于地面物品！
     * 
     * @param player 玩家（未使用，保留用于未来扩展）
     * @param capability 戒指能力
     * @param item 地上物品实体
     * @param itemStack 物品堆
     * @param energyStorage 能量存储
     * @param costPerItem 每个物品的能量消耗
     */
    private void handleDestroyForGroundItem(EntityPlayer player, IRsRingCapability capability,
                                            net.minecraft.entity.item.EntityItem item, ItemStack itemStack,
                                            IEnergyStorage energyStorage, int costPerItem) {
        int originalCount = itemStack.getCount();
        
        // 检查垃圾箱是否可访问（绑定且未被破坏）
        if (capability.isTrashCanBound() && !isTrashCanAccessible(capability)) {
            // 垃圾箱被破坏，保留物品并提示玩家
            sendTrashCanWarning(player, "垃圾箱已失效，请重新绑定！物品保留在地上。");
            return; // 不销毁，保留在地上
        }
        
        // 地面物品直接销毁，不进行销毁类型判断
        // 优先尝试送入垃圾箱
        int sentToTrash = trySendToTrashCan(capability, itemStack);
        
        if (sentToTrash >= originalCount) {
            // 全部送入垃圾箱
            if (costPerItem > 0) {
                energyStorage.extractEnergy(costPerItem * originalCount, false);
            }
            item.setDead();
            return;
        }
        
        if (sentToTrash > 0) {
            // 部分送入垃圾箱，剩余部分销毁
            // 扣除全部物品的能量（送入垃圾箱的 + 销毁的）
            if (costPerItem > 0) {
                energyStorage.extractEnergy(costPerItem * originalCount, false);
            }
            item.setDead();
            return;
        }
        
        // 未绑定垃圾箱或垃圾箱满了，真正销毁
        if (costPerItem > 0) {
            energyStorage.extractEnergy(costPerItem * originalCount, false);
        }
        item.setDead();
    }


    /**
     * 处理物品吸收（正常吸收流程）
     */
    private void handleItemAbsorption(EntityPlayer player, IRsRingCapability capability,
                                      net.minecraft.entity.item.EntityItem item, ItemStack itemStack,
                                      IEnergyStorage energyStorage, int costPerItem,
                                      boolean isBound, boolean preferBackpacks) {
        int originalCount = itemStack.getCount();
        
        int maxAffordable = costPerItem > 0 ? Math.max(1, energyStorage.getEnergyStored() / costPerItem) : Integer.MAX_VALUE;
        int attemptCount = Math.min(originalCount, maxAffordable);
        ItemStack attemptStack = itemStack.copy();
        attemptStack.setCount(attemptCount);

        int inserted = 0;

        if (preferBackpacks) {
            // 优先使用背包
            inserted = CompatManager.absorbToAnyBackpack(player, attemptStack, capability, true);

            // 如果背包存不下且有绑定目标，尝试存入绑定目标
            if (inserted < attemptStack.getCount() && isBound) {
                // 真正需要存入吸收箱时才检查可访问性
                if (!isTerminalAccessible(capability)) {
                    // 吸收箱被破坏，提示玩家，物品保留在地上
                    sendTerminalWarning(player, "吸收箱已失效，请重新绑定！物品保留在地上。");
                    // 不存入吸收箱，让物品保留在地上
                } else {
                    int remainingToInsert = attemptStack.getCount() - inserted;
                    ItemStack remainingStack = attemptStack.copy();
                    remainingStack.setCount(remainingToInsert);
                    inserted += insertToBoundTarget(capability, remainingStack);
                }
            }
        } else if (isBound) {
            // 优先使用绑定目标
            // 先检查吸收箱是否可访问
            if (!isTerminalAccessible(capability)) {
                // 吸收箱被破坏，检查是否有背包
                boolean hasBackpack = CompatManager.isAnyBackpackModAvailable() && CompatManager.hasAnyBackpack(player);
                if (hasBackpack) {
                    sendTerminalWarning(player, "吸收箱已失效，请重新绑定！尝试存入背包...");
                } else {
                    sendTerminalWarning(player, "吸收箱已失效，请重新绑定！物品保留在地上。");
                }
                // 降级为存入背包（如果没有背包会返回0）
                inserted = CompatManager.absorbToAnyBackpack(player, attemptStack, capability, false);
            } else {
                inserted = insertToBoundTarget(capability, attemptStack);

                // 如果绑定目标存不下，尝试存入背包
                if (inserted < attemptStack.getCount()) {
                    int remainingToInsert = attemptStack.getCount() - inserted;
                    ItemStack remainingStack = attemptStack.copy();
                    remainingStack.setCount(remainingToInsert);
                    inserted += CompatManager.absorbToAnyBackpack(player, remainingStack, capability, false);
                }
            }
        } else {
            // 没有绑定目标，只使用背包
            inserted = CompatManager.absorbToAnyBackpack(player, attemptStack, capability, false);
        }

        if (inserted > 0) {
            if (costPerItem > 0) {
                int energyToUse = Math.min(energyStorage.getEnergyStored(), inserted * costPerItem);
                energyStorage.extractEnergy(energyToUse, false);
            }
        }

        // 处理剩余物品：保留在地上
        int remaining = originalCount - inserted;
        if (remaining <= 0) {
            item.setDead();
        } else {
            itemStack.setCount(remaining);
            item.setItem(itemStack);
        }
    }

    /**
     * 处理物品吸收（STORAGE_OVERFLOW销毁流程专用）
     * 只尝试存入背包，不尝试存入绑定吸收箱
     * 剩余物品直接销毁
     */
    private void handleItemAbsorptionForDestroyFlow(EntityPlayer player, IRsRingCapability capability,
                                                     net.minecraft.entity.item.EntityItem item, ItemStack itemStack,
                                                     IEnergyStorage energyStorage, int costPerItem) {
        int originalCount = itemStack.getCount();
        int maxAffordable = costPerItem > 0 ? Math.max(1, energyStorage.getEnergyStored() / costPerItem) : Integer.MAX_VALUE;
        int attemptCount = Math.min(originalCount, maxAffordable);
        ItemStack attemptStack = itemStack.copy();
        attemptStack.setCount(attemptCount);

        // 只尝试存入背包
        int inserted = CompatManager.absorbToAnyBackpack(player, attemptStack, capability, false);

        if (inserted > 0) {
            if (costPerItem > 0) {
                int energyToUse = Math.min(energyStorage.getEnergyStored(), inserted * costPerItem);
                energyStorage.extractEnergy(energyToUse, false);
            }
        }

        // 处理剩余物品：直接销毁
        int remaining = originalCount - inserted;
        if (remaining <= 0) {
            item.setDead();
        } else {
            // 销毁剩余物品
            ItemStack remainingStack = itemStack.copy();
            remainingStack.setCount(remaining);
            handleDestroyForGroundItem(player, capability, item, remainingStack, energyStorage, costPerItem);
        }
    }

    /**
     * 插入到绑定的目标
     */
    private int insertToBoundTarget(IRsRingCapability capability, ItemStack stack) {
        net.minecraft.world.World targetWorld = capability.getTerminalWorld();
        BlockPos targetPos = capability.getTerminalPos();
        if (targetWorld == null) {
            int dim = capability.getTerminalDimension();
            targetWorld = net.minecraftforge.common.DimensionManager.getWorld(dim);
        }
        if (targetWorld == null || targetPos == null) return 0;

        targetWorld.getChunk(targetPos);
        if (!targetWorld.isBlockLoaded(targetPos)) return 0;

        // 检查是否是WearableBackpacks放置的背包
        if (com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.isPlacedBackpack(targetWorld, targetPos)) {
        
            return insertIntoWearableBackpack(targetWorld, targetPos, stack);
        }

        boolean isRSController = isRSController(targetWorld, targetPos);

        if (isRSController) {
            return insertIntoRSNetwork(targetWorld, targetPos, stack);
        } else {
            return insertIntoChest(targetWorld, targetPos, stack);
        }
    }

    /**
     * 插入物品到WearableBackpacks放置的背包
     */
    private int insertIntoWearableBackpack(World world, BlockPos pos, ItemStack stack) {
        if (world == null || pos == null || stack.isEmpty()) return 0;

        net.minecraftforge.items.IItemHandler handler = com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.getPlacedBackpackItems(world, pos);
        if (handler == null || handler.getSlots() == 0) return 0;

        int before = stack.getCount();
        ItemStack remainder = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
        int inserted = before - remainder.getCount();
        if (inserted > 0) {
            stack.setCount(remainder.getCount());
            // 标记TileEntity为脏以触发保存
            com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.markPlacedBackpackDirty(world, pos);
        }
        return inserted;
    }
    
    /**
     * 检查吸收箱是否可访问（绑定且未被破坏）
     * @return true-可访问，false-吸收箱被破坏或不存在
     */
    public static boolean isTerminalAccessible(IRsRingCapability capability) {
        if (!capability.isBound()) return false;
        
        World targetWorld = capability.getTerminalWorld();
        BlockPos targetPos = capability.getTerminalPos();
        
        if (targetWorld == null) {
            int dim = capability.getTerminalDimension();
            targetWorld = DimensionManager.getWorld(dim);
        }
        
        if (targetWorld == null || targetPos == null) return false;
        
        // 强制加载区块
        targetWorld.getChunk(targetPos);
        
        // 检查吸收箱是否存在（未被破坏）
        TileEntity te = targetWorld.getTileEntity(targetPos);
        if (te == null) return false;
        
        // RS控制器不需要物品栏能力检查
        if (isRSController(targetWorld, targetPos)) return true;
        
        // WearableBackpacks放置的背包：即使没有Capability也可访问（通过反射操作）
        if (com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.isPlacedBackpack(targetWorld, targetPos)) {
            return true;
        }
        
        // 检查是否有物品栏能力
        IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        return handler != null;
    }
    
    /**
     * 检查垃圾箱是否可访问（绑定且未被破坏）
     * @return true-可访问，false-垃圾箱被破坏或不存在
     */
    public static boolean isTrashCanAccessible(IRsRingCapability capability) {
        if (!capability.isTrashCanBound()) return false;

        World trashWorld = capability.getTrashCanWorld();
        BlockPos trashPos = capability.getTrashCanPos();

        if (trashWorld == null || trashPos == null) return false;

        // 强制加载区块（与吸收箱保持一致）
        trashWorld.getChunk(trashPos);

        // 检查是否是WearableBackpacks放置的背包
        if (com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.isPlacedBackpack(trashWorld, trashPos)) {
            net.minecraftforge.items.IItemHandler handler = com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.getPlacedBackpackItems(trashWorld, trashPos);
            return handler != null;
        }

        // RS控制器不需要物品栏能力检查（与吸收箱保持一致）
        if (isRSController(trashWorld, trashPos)) return true;

        // 检查垃圾箱是否存在（未被破坏）
        TileEntity te = trashWorld.getTileEntity(trashPos);
        if (te == null) return false;

        // 检查是否有物品栏能力
        IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        return handler != null;
    }
    
    /**
     * 尝试将物品送入垃圾箱
     * @return 成功送入的物品数量（0表示未绑定、无法放入或垃圾箱已满）
     */
    public static int trySendToTrashCan(IRsRingCapability capability, ItemStack itemStack) {
        if (!capability.isTrashCanBound()) return 0;

        World trashWorld = capability.getTrashCanWorld();
        BlockPos trashPos = capability.getTrashCanPos();

        if (trashWorld == null || trashPos == null) return 0;

        // 强制加载区块（与吸收箱保持一致）
        trashWorld.getChunk(trashPos);

        // 检查是否是RS控制器
        if (isRSController(trashWorld, trashPos)) {
            return insertIntoRSNetworkStatic(trashWorld, trashPos, itemStack);
        }

        IItemHandler handler = null;
        boolean isPlacedBackpack = false;

        // 检查是否是WearableBackpacks放置的背包
        if (com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.isPlacedBackpack(trashWorld, trashPos)) {
            handler = com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.getPlacedBackpackItems(trashWorld, trashPos);
            isPlacedBackpack = true;
        } else {
            // 普通容器
            TileEntity te = trashWorld.getTileEntity(trashPos);
            if (te != null) {
                handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            }
        }

        if (handler == null) return 0;

        // 尝试放入物品
        ItemStack remainder = ItemHandlerHelper.insertItem(handler, itemStack.copy(), false);

        // 返回成功放入的数量
        int inserted;
        if (remainder.isEmpty()) {
            inserted = itemStack.getCount();
        } else {
            inserted = itemStack.getCount() - remainder.getCount();
        }

        // 如果是放置的背包，标记为脏
        if (inserted > 0 && isPlacedBackpack) {
            com.rsring.compat.wearablebackpacks.WearableBackpacksCompat.markPlacedBackpackDirty(trashWorld, trashPos);
        }

        return inserted;
    }

    /**
     * 静态版本的RS网络插入方法（供trySendToTrashCan使用）
     */
    private static int insertIntoRSNetworkStatic(World world, BlockPos pos, ItemStack stack) {
        if (world == null || pos == null || stack.isEmpty()) return 0;
        try {
            Class<?> apiClass = Class.forName("com.raoulvdberge.refinedstorage.apiimpl.API");
            Object api = apiClass.getMethod("instance").invoke(null);
            Object network = getNetworkFromNodeManagerStatic(api, world, pos);
            if (network == null) {
                network = getNetworkFromTileStatic(world, pos);
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

    private static Object getNetworkFromNodeManagerStatic(Object api, World world, BlockPos pos) {
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

    private static Object getNetworkFromTileStatic(World world, BlockPos pos) {
        try {
            TileEntity te = world.getTileEntity(pos);
            if (te == null) return null;

            java.lang.reflect.Method getNetwork = te.getClass().getMethod("getNetwork");
            return getNetwork.invoke(te);
        } catch (Throwable t) {
            return null;
        }
    }
    
    /**
     * 检查物品是否应该被销毁
     * 销毁模式使用独立的过滤系统，与吸收模式完全独立
     */
    public static boolean shouldDestroyItem(IRsRingCapability capability, ItemStack itemStack) {
        FilterMode destroyFilterMode = capability.getDestroyFilterMode();
        boolean isDestroyWhitelist = capability.isDestroyWhitelistMode();
        
        // 检查销毁过滤条件是否存在
        if (isDestroyWhitelist && !hasAnyDestroyFilter(capability)) {
            // 白名单模式但没有过滤条件：不销毁任何物品
            return false;
        }
        
        switch (destroyFilterMode) {
            case MOD:
                return shouldDestroyByMod(capability, itemStack, isDestroyWhitelist);
            case ATTRIBUTE:
                return shouldDestroyByAttribute(capability, itemStack, isDestroyWhitelist);
            case ITEM:
            default:
                return shouldDestroyByItem(capability, itemStack, isDestroyWhitelist);
        }
    }
    
    /**
     * 检查销毁模式是否有任何过滤条件
     */
    public static boolean hasAnyDestroyFilter(IRsRingCapability capability) {
        FilterMode filterMode = capability.getDestroyFilterMode();
        
        switch (filterMode) {
            case ATTRIBUTE:
                java.util.List<com.rsring.util.Pair<ItemAttribute, Boolean>> attrs = capability.getDestroyFilterAttributes();
                return attrs != null && !attrs.isEmpty();
                
            case MOD:
                // 检查模组过滤槽位
                for (int i = 0; i < 9; i++) {
                    String modId = capability.getDestroyModFilterSlot(i);
                    if (modId != null && !modId.isEmpty()) {
                        return true;
                    }
                }
                // 兼容旧数据：检查模组过滤列表
                List<String> filterMods = capability.getDestroyFilterMods();
                if (filterMods != null && !filterMods.isEmpty()) {
                    return true;
                }
                return false;
                
            case ITEM:
            default:
                for (int i = 0; i < 9; i++) {
                    String filterName = capability.getDestroyFilterSlot(i);
                    if (filterName != null && !filterName.isEmpty()) {
                        return true;
                    }
                }
                return false;
        }
    }
    
    /**
     * 销毁模式 - 物品ID过滤
     */
    public static boolean shouldDestroyByItem(IRsRingCapability capability, ItemStack itemStack, boolean isWhitelist) {
        ResourceLocation registryName = itemStack.getItem().getRegistryName();
        if (registryName == null) return false;
        String itemName = registryName.toString();
        boolean matchNbt = capability.shouldDestroyMatchNbt();
        boolean matchDurability = capability.shouldDestroyMatchDurability();
        
        boolean isInList = false;
        for (int i = 0; i < 9; i++) {
            String filterName = capability.getDestroyFilterSlot(i);
            if (filterName != null && !filterName.isEmpty()) {
                if (matchesDestroyItemFilter(capability, i, filterName, itemStack, itemName, matchNbt, matchDurability)) {
                    isInList = true;
                    break;
                }
            }
        }
        
        return isWhitelist ? isInList : !isInList;
    }
    
    /**
     * 销毁模式 - 物品匹配检查
     */
    public static boolean matchesDestroyItemFilter(IRsRingCapability capability, int slotIndex, String filterName, ItemStack itemStack, String itemName, boolean matchNbt, boolean matchDurability) {
        if (!filterName.equals(itemName)) {
            return false;
        }
        
        if (!matchNbt && !matchDurability) {
            return true;
        }
        
        // 获取存储的参考NBT
        net.minecraft.nbt.NBTTagCompound storedNbt = capability.getDestroyFilterSlotNBT(slotIndex);
        
        if (matchDurability) {
            int itemDamage = itemStack.getItemDamage();
            int filterDamage = (storedNbt != null && storedNbt.hasKey("rsring_filter_damage")) ? storedNbt.getInteger("rsring_filter_damage") : 0;
            if (itemStack.getItem().isDamageable() && filterDamage != itemDamage) {
                return false;
            }
        }
        
        if (matchNbt) {
            net.minecraft.nbt.NBTTagCompound itemNbt = itemStack.getTagCompound();
            
            if (itemNbt == null && storedNbt == null) return true;
            if (itemNbt == null || storedNbt == null) return false;
            
            net.minecraft.nbt.NBTTagCompound itemNbtCopy = itemNbt.copy();
            net.minecraft.nbt.NBTTagCompound filterNbtCopy = storedNbt.copy();
            // 移除内部使用的耐久度标记，不作为NBT匹配的一部分
            itemNbtCopy.removeTag("rsring_filter_damage");
            filterNbtCopy.removeTag("rsring_filter_damage");
            return itemNbtCopy.equals(filterNbtCopy);
        }
        
        return true;
    }
    
    /**
     * 销毁模式 - 模组过滤
     * 模组过滤槽位存储完整物品ID（modId:itemName格式），过滤时提取模组ID
     * 支持NBT和耐久匹配选项
     */
    public static boolean shouldDestroyByMod(IRsRingCapability capability, ItemStack itemStack, boolean isWhitelist) {
        String itemModId = itemStack.getItem().getRegistryName().getNamespace();
        boolean matchNbt = capability.shouldDestroyMatchNbt();
        boolean matchDurability = capability.shouldDestroyMatchDurability();
        
        // 检查模组过滤槽位
        boolean isInList = false;
        for (int i = 0; i < 9; i++) {
            String slotData = capability.getDestroyModFilterSlot(i);
            if (slotData != null && !slotData.isEmpty()) {
                // 提取模组ID（支持完整物品ID格式 modId:itemName 或纯modId格式）
                String filterModId;
                if (slotData.contains(":")) {
                    filterModId = slotData.substring(0, slotData.indexOf(":"));
                } else {
                    filterModId = slotData;
                }
                if (filterModId.equals(itemModId)) {
                    // 模组ID匹配，进一步检查NBT和耐久（如果启用）
                    if (matchesDestroyModFilterItem(capability, i, slotData, itemStack, matchNbt, matchDurability)) {
                        isInList = true;
                        break;
                    }
                }
            }
        }
        
        // 兼容旧数据：检查模组过滤列表
        if (!isInList) {
            List<String> filterMods = capability.getDestroyFilterMods();
            for (String modId : filterMods) {
                if (modId != null && !modId.isEmpty()) {
                    String filterModId = modId.contains(":") ? modId.substring(0, modId.indexOf(":")) : modId;
                    if (filterModId.equals(itemModId)) {
                        isInList = true;
                        break;
                    }
                }
            }
        }
        
        return isWhitelist ? isInList : !isInList;
    }
    
    /**
     * 销毁模式 - 模组过滤的NBT和耐久匹配
     * 使用精确耐久值匹配（与精妙背包一致）
     */
    public static boolean matchesDestroyModFilterItem(IRsRingCapability capability, int slotIndex, String filterItemId, ItemStack itemStack, boolean matchNbt, boolean matchDurability) {
        if (!matchNbt && !matchDurability) {
            return true;
        }
        
        // 获取存储的参考NBT
        net.minecraft.nbt.NBTTagCompound storedNbt = capability.getDestroyModFilterSlotNBT(slotIndex);
        
        // 检查耐久（精确值匹配）
        if (matchDurability && itemStack.getItem().isDamageable()) {
            int itemDamage = itemStack.getItemDamage();
            int filterDamage = (storedNbt != null && storedNbt.hasKey("rsring_filter_damage")) ? storedNbt.getInteger("rsring_filter_damage") : 0;
            if (filterDamage != itemDamage) {
                return false;
            }
        }
        
        // 检查NBT
        if (matchNbt) {
            net.minecraft.nbt.NBTTagCompound itemNbt = itemStack.getTagCompound();
            
            if (itemNbt == null && storedNbt == null) return true;
            if (itemNbt == null || storedNbt == null) return false;
            
            net.minecraft.nbt.NBTTagCompound itemNbtCopy = itemNbt.copy();
            net.minecraft.nbt.NBTTagCompound filterNbtCopy = storedNbt.copy();
            // 移除内部使用的耐久度标记，不作为NBT匹配的一部分
            itemNbtCopy.removeTag("rsring_filter_damage");
            filterNbtCopy.removeTag("rsring_filter_damage");
            
            if (!itemNbtCopy.equals(filterNbtCopy)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 销毁模式 - 属性过滤
     */
    public static boolean shouldDestroyByAttribute(IRsRingCapability capability, ItemStack itemStack, boolean isWhitelist) {
        java.util.List<com.rsring.util.Pair<ItemAttribute, Boolean>> attributeTests = capability.getDestroyFilterAttributes();
        boolean matchAllMode = capability.isDestroyMatchAllMode();
        
        if (attributeTests == null || attributeTests.isEmpty()) {
            return isWhitelist ? false : true;
        }
        
        for (com.rsring.util.Pair<ItemAttribute, Boolean> test : attributeTests) {
            ItemAttribute attribute = test.getKey();
            boolean inverted = test.getValue();
            boolean matches = attribute.appliesTo(itemStack) != inverted;
            
            if (matches) {
                if (!isWhitelist) {
                    return true;
                } else if (!matchAllMode) {
                    return false;
                }
            } else {
                if (isWhitelist && matchAllMode) {
                    return true;
                }
            }
        }
        
        if (isWhitelist) {
            return matchAllMode ? false : true;
        } else {
            return false;
        }
    }


    private boolean hasAnyFilter(IRsRingCapability capability) {
        FilterMode filterMode = capability.getFilterMode();
        
        switch (filterMode) {
            case ATTRIBUTE:
                // 属性过滤模式：检查属性列表是否为空
                java.util.List<com.rsring.util.Pair<ItemAttribute, Boolean>> attrs = capability.getFilterAttributes();
                return attrs != null && !attrs.isEmpty();
                
            case MOD:
                // 模组过滤模式：检查模组过滤槽位
                for (int i = 0; i < 9; i++) {
                    String modId = capability.getModFilterSlot(i);
                    if (modId != null && !modId.isEmpty()) {
                        return true;
                    }
                }
                // 兼容旧数据：检查模组过滤列表
                List<String> filterMods = capability.getFilterMods();
                if (filterMods != null && !filterMods.isEmpty()) {
                    return true;
                }
                return false;
                
            case ITEM:
            default:
                // 物品过滤模式：检查槽位
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
    }

    private boolean shouldFilterItem(IRsRingCapability capability, ItemStack itemStack) {
        boolean isWhitelistMode = capability.isWhitelistMode();
        FilterMode filterMode = capability.getFilterMode();
        
        switch (filterMode) {
            case MOD:
                return shouldFilterByMod(capability, itemStack, isWhitelistMode);
            case ATTRIBUTE:
                return shouldFilterByAttribute(capability, itemStack, isWhitelistMode);
            case ITEM:
            default:
                return shouldFilterByItem(capability, itemStack, isWhitelistMode);
        }
    }
    
    /**
     * 物品ID过滤匹配逻辑
     * 支持NBT和耐久匹配选项
     */
    private boolean shouldFilterByItem(IRsRingCapability capability, ItemStack itemStack, boolean isWhitelistMode) {
        ResourceLocation registryName = itemStack.getItem().getRegistryName();
        if (registryName == null) return false;
        String itemName = registryName.toString();
        boolean matchNbt = capability.shouldMatchNbt();
        boolean matchDurability = capability.shouldMatchDurability();
        
        boolean isInList = isInDefaultList(itemName, isWhitelistMode);

        // 遍历过滤槽位进行匹配
        for (int i = 0; i < 9; i++) {
            String filterName = capability.getFilterSlot(i);
            if (filterName != null && !filterName.isEmpty()) {
                if (matchesItemFilter(capability, i, filterName, itemStack, itemName, matchNbt, matchDurability)) {
                    isInList = true;
                    break;
                }
            }
        }

        if (isWhitelistMode) {
            return !isInList;
        } else {
            return isInList;
        }
    }
    
    /**
     * 检查物品是否匹配过滤条件
     * @param capability 用于获取存储的NBT数据
     * @param slotIndex 槽位索引，用于获取对应的NBT数据
     */
    private boolean matchesItemFilter(IRsRingCapability capability, int slotIndex, String filterName, ItemStack itemStack, String itemName, boolean matchNbt, boolean matchDurability) {
        // 简单ID匹配
        if (!filterName.equals(itemName)) {
            return false;
        }
        
        // 如果不需要匹配NBT和耐久，ID匹配即成功
        if (!matchNbt && !matchDurability) {
            return true;
        }
        
        // 获取存储的参考NBT（用户放入物品时的NBT）
        net.minecraft.nbt.NBTTagCompound storedNbt = capability.getFilterSlotNBT(slotIndex);
        
        // 耐久匹配
        if (matchDurability) {
            int itemDamage = itemStack.getItemDamage();
            // 从存储的NBT中读取耐久值
            int filterDamage = (storedNbt != null && storedNbt.hasKey("rsring_filter_damage")) ? storedNbt.getInteger("rsring_filter_damage") : 0;
            if (itemStack.getItem().isDamageable() && filterDamage != itemDamage) {
                return false;
            }
        }
        
        // NBT匹配
        if (matchNbt) {
            net.minecraft.nbt.NBTTagCompound itemNbt = itemStack.getTagCompound();
            
            // 如果两者都没有NBT，认为匹配
            if (itemNbt == null && storedNbt == null) {
                return true;
            }
            // 如果一个有NBT一个没有，不匹配
            if (itemNbt == null || storedNbt == null) {
                return false;
            }
            // 比较NBT数据
            net.minecraft.nbt.NBTTagCompound itemNbtCopy = itemNbt.copy();
            net.minecraft.nbt.NBTTagCompound filterNbtCopy = storedNbt.copy();
            // 移除内部使用的耐久度标记，不作为NBT匹配的一部分
            itemNbtCopy.removeTag("rsring_filter_damage");
            filterNbtCopy.removeTag("rsring_filter_damage");
            if (!itemNbtCopy.equals(filterNbtCopy)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 模组过滤匹配逻辑
     * 模组过滤槽位存储完整物品ID（modId:itemName格式），过滤时提取模组ID
     * 支持NBT和耐久匹配选项
     */
    private boolean shouldFilterByMod(IRsRingCapability capability, ItemStack itemStack, boolean isWhitelistMode) {
        String itemModId = itemStack.getItem().getRegistryName().getNamespace();
        boolean matchNbt = capability.shouldMatchNbt();
        boolean matchDurability = capability.shouldMatchDurability();
        
        // 检查模组过滤槽位
        boolean isInList = false;
        for (int i = 0; i < 9; i++) {
            String slotData = capability.getModFilterSlot(i);
            if (slotData != null && !slotData.isEmpty()) {
                // 提取模组ID（支持完整物品ID格式 modId:itemName 或纯modId格式）
                String filterModId;
                if (slotData.contains(":")) {
                    filterModId = slotData.substring(0, slotData.indexOf(":"));
                } else {
                    filterModId = slotData;
                }
                if (filterModId.equals(itemModId)) {
                    // 模组ID匹配，进一步检查NBT和耐久（如果启用）
                    if (matchesModFilterItem(capability, i, slotData, itemStack, matchNbt, matchDurability)) {
                        isInList = true;
                        break;
                    }
                }
            }
        }
        
        // 兼容旧数据：检查模组过滤列表
        if (!isInList) {
            List<String> filterMods = capability.getFilterMods();
            for (String modId : filterMods) {
                if (modId != null && !modId.isEmpty()) {
                    String filterModId = modId.contains(":") ? modId.substring(0, modId.indexOf(":")) : modId;
                    if (filterModId.equals(itemModId)) {
                        // 兼容旧数据不支持NBT/耐久匹配
                        isInList = true;
                        break;
                    }
                }
            }
        }
        
        if (isWhitelistMode) {
            return !isInList;
        } else {
            return isInList;
        }
    }
    
    /**
     * 检查物品是否匹配模组过滤槽位的参考物品（支持NBT和耐久匹配）
     * 注意：模组过滤的耐久匹配使用精确值匹配（与精妙背包一致）
     */
    private boolean matchesModFilterItem(IRsRingCapability capability, int slotIndex, String filterItemId, ItemStack itemStack, boolean matchNbt, boolean matchDurability) {
        // 如果不需要匹配NBT和耐久，直接返回true（模组ID已匹配）
        if (!matchNbt && !matchDurability) {
            return true;
        }
        
        // 获取存储的参考NBT
        net.minecraft.nbt.NBTTagCompound storedNbt = capability.getModFilterSlotNBT(slotIndex);
        
        // 检查耐久（精确值匹配）
        if (matchDurability && itemStack.getItem().isDamageable()) {
            int itemDamage = itemStack.getItemDamage();
            int filterDamage = (storedNbt != null && storedNbt.hasKey("rsring_filter_damage")) ? storedNbt.getInteger("rsring_filter_damage") : 0;
            if (filterDamage != itemDamage) {
                return false;
            }
        }
        
        // 检查NBT
        if (matchNbt) {
            net.minecraft.nbt.NBTTagCompound itemNbt = itemStack.getTagCompound();
            
            if (itemNbt == null && storedNbt == null) {
                return true;
            }
            if (itemNbt == null || storedNbt == null) {
                return false;
            }
            
            // 比较NBT（移除内部使用的耐久度标记）
            net.minecraft.nbt.NBTTagCompound itemNbtCopy = itemNbt.copy();
            net.minecraft.nbt.NBTTagCompound filterNbtCopy = storedNbt.copy();
            itemNbtCopy.removeTag("rsring_filter_damage");
            filterNbtCopy.removeTag("rsring_filter_damage");
            
            if (!itemNbtCopy.equals(filterNbtCopy)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 属性过滤匹配逻辑
     * 参照机械动力的 AttributeFilterItemStack.test() 实现
     * 支持三种匹配模式：白名单-或(WHITELIST_DISJ)、白名单-与(WHITELIST_CONJ)、黑名单(BLACKLIST)
     */
    private boolean shouldFilterByAttribute(IRsRingCapability capability, ItemStack itemStack, boolean isWhitelistMode) {
        java.util.List<com.rsring.util.Pair<ItemAttribute, Boolean>> attributeTests = capability.getFilterAttributes();
        boolean matchAllMode = capability.isMatchAllMode();
        
        // 如果没有属性过滤器，根据黑白名单模式决定行为
        // 白名单模式：没有属性 = 没有物品被允许 = 拒绝所有
        // 黑名单模式：没有属性 = 没有物品被禁止 = 放行所有
        if (attributeTests == null || attributeTests.isEmpty()) {
            return isWhitelistMode; // 白名单返回true（拒绝），黑名单返回false（放行）
        }
        
        // 遍历所有属性测试
        for (com.rsring.util.Pair<ItemAttribute, Boolean> test : attributeTests) {
            ItemAttribute attribute = test.getKey();
            boolean inverted = test.getValue();
            boolean matches = attribute.appliesTo(itemStack) != inverted;
            
            if (matches) {
                // 属性匹配成功
                if (!isWhitelistMode) {
                    // 黑名单模式：匹配成功则拒绝
                    return true;
                } else if (!matchAllMode) {
                    // 白名单-或模式：任意匹配成功即可通过
                    return false;
                }
                // 白名单-与模式：继续检查其他属性
            } else {
                // 属性匹配失败
                if (isWhitelistMode && matchAllMode) {
                    // 白名单-与模式：任意失败则拒绝
                    return true;
                }
                // 黑名单模式或白名单-或模式：继续检查
            }
        }
        
        // 所有属性检查完毕
        if (isWhitelistMode) {
            // 白名单模式：全部匹配成功（与模式）或至少有一个匹配（或模式）
            return matchAllMode ? false : true;
        } else {
            // 黑名单模式：没有匹配成功，允许通过
            return false;
        }
    }
    
    /**
     * 属性过滤的备用物品ID匹配
     */
    private boolean shouldFilterByItemFallback(IRsRingCapability capability, ItemStack itemStack, boolean isWhitelistMode) {
        boolean isInList = false;
        
        for (int i = 0; i < 9; i++) {
            String filterName = capability.getFilterSlot(i);
            if (filterName != null && !filterName.isEmpty()) {
                if (matchesItemWithNBT(filterName, itemStack)) {
                    isInList = true;
                    break;
                }
            }
        }
        
        return isWhitelistMode ? !isInList : isInList;
    }
    
    private boolean matchesItemWithNBT(String filterName, ItemStack itemStack) {
        ResourceLocation registryName = itemStack.getItem().getRegistryName();
        if (registryName == null) return false;
        String itemName = registryName.toString();
        
        // 简单匹配：只比较物品ID
        if (filterName.equals(itemName)) {
            return true;
        }
        
        // TODO: 实现NBT匹配逻辑
        // 如果filterName包含NBT数据，需要解析并比较
        
        return false;
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

    private static boolean isRSController(World world, BlockPos pos) {
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
