package com.rsring.item;

import com.rsring.capability.IExperiencePumpCapability;
import com.rsring.capability.ExperiencePumpCapability;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import baubles.api.BaubleType;
import baubles.api.IBauble;
import org.lwjgl.input.Keyboard;

import java.util.List;


@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemExperiencePump extends Item implements IBauble {

    public static final String XP_TAG = "ExperiencePumpData";

    private static final int XP_PER_BOTTLE = 36;
    private static final int DEFAULT_RETAIN_LEVEL = 1;

    // NBT keys
    private static final String CAPACITY_LEVELS_KEY = "capacityLevels";
    private static final String XP_KEY = "xp";

    private static final int FILL_LEVEL_EMPTY = 0;
    private static final int FILL_LEVEL_QUARTER = 1;
    private static final int FILL_LEVEL_HALF = 2;
    private static final int FILL_LEVEL_THREE_QUARTERS = 3;
    private static final int FILL_LEVEL_FULL = 4;

    
    public static net.minecraft.nbt.NBTTagCompound getDataFromNBT(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound() || !stack.getTagCompound().hasKey(XP_TAG))
            return null;
        return stack.getTagCompound().getCompoundTag(XP_TAG);
    }

    
    public static int getXpStoredFromNBT(ItemStack stack) {
        if (stack == null) return 0;
        net.minecraft.nbt.NBTTagCompound data = getDataFromNBT(stack);
        return data != null ? data.getInteger(XP_KEY) : 0;
    }

    
    public static int getCapacityLevelsFromNBT(ItemStack stack) {
        if (stack == null) return DEFAULT_RETAIN_LEVEL;
        net.minecraft.nbt.NBTTagCompound data = getDataFromNBT(stack);
        return data != null && data.hasKey(CAPACITY_LEVELS_KEY) ? data.getInteger(CAPACITY_LEVELS_KEY) : DEFAULT_RETAIN_LEVEL;
    }

    
    public static int getMaxXpFromNBT(ItemStack stack) {
        if (stack == null) {
            return (int)(IExperiencePumpCapability.BASE_XP_PER_LEVEL * Math.pow(2, DEFAULT_RETAIN_LEVEL - 1));
        }
        
        if (stack.getItem() instanceof ItemExperienceTank100) {
            return 30970;
        } else if (stack.getItem() instanceof ItemExperienceTank500) {
            return 1045970;
        } else if (stack.getItem() instanceof ItemExperienceTank1000) {
            return 4339720;
        } else if (stack.getItem() instanceof ItemExperienceTank2000) {
            return 17677220;
        }
        
        int capacityLevels = getCapacityLevelsFromNBT(stack);

        try {
            long maxCapacity = (long) IExperiencePumpCapability.BASE_XP_PER_LEVEL * (1L << (capacityLevels - 1));
            if (maxCapacity > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) maxCapacity;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    
    public ItemExperiencePump(String registryName, String translationKey) {
        super();
        setTranslationKey(translationKey);
        setRegistryName(new ResourceLocation("rsring", registryName));
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    
    public ItemExperiencePump() {
        this("experience_pump", "rsring.experience_pump");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        net.minecraft.nbt.NBTTagCompound data = getDataFromNBT(stack);
        if (data == null) {
            int initialCapacity = IExperiencePumpCapability.BASE_XP_PER_LEVEL;
            tooltip.add(TextFormatting.GRAY + "等级: " + TextFormatting.AQUA + "1");
            tooltip.add(TextFormatting.GRAY + "经验: " + TextFormatting.GREEN + "0" + TextFormatting.GRAY
                + " / " + initialCapacity + " mb");

            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                tooltip.add("");
                tooltip.add(TextFormatting.GOLD + "功能介绍:");
                tooltip.add(TextFormatting.GRAY + "  - 吸收附近经验球");
                tooltip.add(TextFormatting.GRAY + "  - 存储经验（需配合经验泵控制器使用）");
                tooltip.add(TextFormatting.GRAY + "  - 自动修复附魔装备");
                tooltip.add(TextFormatting.GOLD + "使用方法:");
                tooltip.add(TextFormatting.GRAY + "  - 与经验泵控制器配合使用");
            } else {
                tooltip.add(TextFormatting.DARK_GRAY + "按住 " + TextFormatting.YELLOW + "Shift"
                    + TextFormatting.DARK_GRAY + " 查看详细信息");
            }
            return;
        }

        int xp = data.getInteger(XP_KEY);
        int capacityLevels = data.hasKey(CAPACITY_LEVELS_KEY) ? data.getInteger(CAPACITY_LEVELS_KEY) : DEFAULT_RETAIN_LEVEL;
        int max = (int)(IExperiencePumpCapability.BASE_XP_PER_LEVEL * Math.pow(2, capacityLevels - 1));

        tooltip.add(TextFormatting.GRAY + "等级: " + TextFormatting.AQUA + capacityLevels);
        tooltip.add(TextFormatting.GRAY + "经验: " + TextFormatting.GREEN + xp + TextFormatting.GRAY
            + " / " + max + " mb");

        boolean showDetail = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (!showDetail) {
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " + TextFormatting.YELLOW + "Shift"
                + TextFormatting.DARK_GRAY + " 查看详细信息");
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "功能介绍:");
            tooltip.add(TextFormatting.GRAY + "  - 自动吸收附近经验球和经验瓶");
            tooltip.add(TextFormatting.GRAY + "  - 智能经验存储与溢出处理");
            tooltip.add(TextFormatting.GRAY + "  - 自动修复附魔装备（经验修补）");
            tooltip.add(TextFormatting.GRAY + "  - 与经验泵控制器协同工作");
            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "配置信息:");
            tooltip.add(TextFormatting.GRAY + "  - 抽取速度: " + TextFormatting.AQUA
                + com.rsring.config.ExperienceTankConfig.tank.xpExtractionRate + " XP/秒");
            tooltip.add(TextFormatting.GRAY + "  - 抽取范围: " + TextFormatting.AQUA
                + com.rsring.config.ExperienceTankConfig.tank.xpExtractionRange + " 格");
            tooltip.add(TextFormatting.GRAY + "  - 溢出保护: "
                + (com.rsring.config.ExperienceTankConfig.tank.enableOverflowBottles ? TextFormatting.GREEN + "开启"
                : TextFormatting.RED + "关闭"));
        }
    }

    
    private static String getModeText(int mode) {
        switch (mode) {
            case IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER: return TextFormatting.AQUA + "从玩家抽取";
            case IExperiencePumpCapability.MODE_PUMP_TO_PLAYER: return TextFormatting.GOLD + "向玩家注入";
            default: return TextFormatting.GRAY + "关闭";
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }
        net.minecraft.nbt.NBTTagCompound data = getDataFromNBT(stack);
        if (data == null) {
            return false;
        }
        int maxCapacity = getMaxXpFromNBT(stack);
        return maxCapacity > 0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return 1.0;
        }
        net.minecraft.nbt.NBTTagCompound data = getDataFromNBT(stack);
        if (data == null) {
            return 1.0;
        }
        int maxCapacity = getMaxXpFromNBT(stack);
        if (maxCapacity <= 0) {
            return 1.0;
        }
        int stored = data.getInteger(XP_KEY);
        return 1.0 - (double) stored / (double) maxCapacity;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0x80FF20;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.CHARM;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            int xpStored = getXpStoredFromNBT(stack);
            int capacityLevels = getCapacityLevelsFromNBT(stack);
            int maxXp = getMaxXpFromNBT(stack);

            String message = TextFormatting.AQUA + "等级 " + capacityLevels +
                           TextFormatting.GRAY + " - " +
                           TextFormatting.GREEN + xpStored +
                           TextFormatting.GRAY + " / " +
                           TextFormatting.YELLOW + maxXp +
                           TextFormatting.GRAY + " mb";

            player.sendMessage(new TextComponentString(message));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    
    public static int storeExperienceLevels(ItemStack stack, EntityPlayer player, int levelsToStore) {
        if (levelsToStore <= 0) {
            return 0;
        }

        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap == null) {
            return 0;
        }

        int xpToStore = com.rsring.util.XpHelper.extractExperienceLevels(player, levelsToStore);
        if (xpToStore <= 0) {
            return 0;
        }

        int actualStored = cap.addXp(xpToStore);
        if (actualStored > 0) {
            syncCapabilityToStack(stack, cap);
        }

        return actualStored;
    }

    
    public static int extractExperienceLevels(ItemStack stack, EntityPlayer player, int levelsToExtract) {
        if (levelsToExtract <= 0) {
            return 0;
        }

        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap == null) {
            return 0;
        }

        int currentLevel = player.experienceLevel;
        int targetLevel = currentLevel + levelsToExtract;
        int xpNeeded = com.rsring.util.XpHelper.getExperienceBetweenLevels(currentLevel, targetLevel);

        if (xpNeeded <= 0) {
            return 0;
        }

        int actualExtracted = cap.takeXp(xpNeeded);
        if (actualExtracted > 0) {
            com.rsring.util.XpHelper.addExperienceToPlayer(player, actualExtracted);
            syncCapabilityToStack(stack, cap);
        }

        return actualExtracted;
    }

    
    public static int storeAllExperience(ItemStack stack, EntityPlayer player) {
        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap == null) {
            return 0;
        }

        int playerTotalXP = com.rsring.util.XpHelper.getPlayerTotalExperience(player);
        if (playerTotalXP <= 0) {
            return 0;
        }

        int actualStored = cap.addXp(playerTotalXP);
        if (actualStored > 0) {
            com.rsring.util.XpHelper.removeExperienceFromPlayer(player, actualStored);
            syncCapabilityToStack(stack, cap);
        }

        return actualStored;
    }

    
    public static int extractAllExperience(ItemStack stack, EntityPlayer player) {
        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap == null) {
            return 0;
        }

        int storedXP = cap.getXpStored();
        if (storedXP <= 0) {
            return 0;
        }

        int actualExtracted = cap.takeXp(storedXP);
        if (actualExtracted > 0) {
            com.rsring.util.XpHelper.addExperienceToPlayer(player, actualExtracted);
            syncCapabilityToStack(stack, cap);
        }

        return actualExtracted;
    }

    
    public static void syncCapabilityToStack(ItemStack stack, IExperiencePumpCapability cap) {
        if (cap == null || ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY == null) return;
        net.minecraft.nbt.NBTBase nbt = ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY.getStorage()
            .writeNBT(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, cap, null);
        if (nbt instanceof net.minecraft.nbt.NBTTagCompound) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            stack.getTagCompound().setTag(XP_TAG, (net.minecraft.nbt.NBTTagCompound) nbt);
        }
    }

    @Override
    public net.minecraft.nbt.NBTTagCompound getNBTShareTag(ItemStack stack) {
        net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound() != null ? stack.getTagCompound().copy() : new net.minecraft.nbt.NBTTagCompound();
        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap != null && ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY != null) {
            net.minecraft.nbt.NBTBase nbt = ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY.getStorage()
                .writeNBT(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, cap, null);
            if (nbt instanceof net.minecraft.nbt.NBTTagCompound)
                tag.setTag(XP_TAG, (net.minecraft.nbt.NBTTagCompound) nbt);
        }
        return tag;
    }

    @Override
    public void readNBTShareTag(ItemStack stack, net.minecraft.nbt.NBTTagCompound nbt) {
        stack.setTagCompound(nbt);
        if (nbt != null && nbt.hasKey(XP_TAG) && ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY != null) {
            IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            if (cap != null)
                ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY.getStorage()
                    .readNBT(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, cap, null, nbt.getCompoundTag(XP_TAG));
        }
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, net.minecraft.nbt.NBTTagCompound nbt) {
        ExperiencePumpCapability.Provider provider = new ExperiencePumpCapability.Provider();
        net.minecraft.nbt.NBTTagCompound data = nbt;
        if ((data == null || data.getKeySet().isEmpty()) && stack.getTagCompound() != null && stack.getTagCompound().hasKey(XP_TAG))
            data = stack.getTagCompound().getCompoundTag(XP_TAG);
        provider.initFromNBT(data);
        return provider;
    }

    
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        if (entity == null || entity.world == null || entity.world.isRemote || !(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;
        if (player.world == null) return;
        IExperiencePumpCapability cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
        if (cap == null) return;

        if (!isUpgradeActive(stack, cap) || getExtractionRate() <= 0) {
            return;
        }

        int extractedXp = 0;
        if (player.ticksExisted % com.rsring.config.ExperienceTankConfig.tank.extractionInterval == 0) {
            extractedXp = extractXpFromSurroundings(player, stack, cap);
        }

        if (extractedXp > 0) {
            storeExtractedXp(player, stack, cap, extractedXp);
        }

        int pumpInterval = Math.max(1, com.rsring.config.ExperienceTankConfig.tank.pumpingInterval);
        if (com.rsring.config.ExperienceTankConfig.tank.enableAutoPumping &&
            cap.getMode() != IExperiencePumpCapability.MODE_OFF && player.ticksExisted % pumpInterval == 0) {
            com.rsring.experience.ExperiencePumpController controller = com.rsring.experience.ExperiencePumpController.getInstance();
            if (!controller.isTankManagedByController(stack)) {
                pumpExperienceBetweenPlayerAndTank(player, stack, cap);
            }
        }

        if (com.rsring.config.ExperienceTankConfig.tank.mendingOn && cap.isUseForMending() &&
            cap.getXpStored() > 0 && player.ticksExisted % com.rsring.config.ExperienceTankConfig.tank.mendingInterval == 0) {
            tryRepairMending(player, stack, cap);
        }

        syncCapabilityToStack(stack, cap);
    }

    
    private void storeExtractedXp(EntityPlayer player, ItemStack stack, IExperiencePumpCapability cap, int xpAmount) {
        if (xpAmount <= 0) return;

        int currentXp = cap.getXpStored();
        int maxXp = cap.getMaxXp();
        int newXp = currentXp + xpAmount;

        if (newXp > maxXp) {
            int overflowXp = newXp - maxXp;
            int xpPerBottle = getXpPerBottle();
            int overflowBottles = overflowXp / xpPerBottle;

            if (overflowBottles > 0 && com.rsring.config.ExperienceTankConfig.tank.enableOverflowBottles) {
                ItemStack overflowStack = new ItemStack(Items.EXPERIENCE_BOTTLE, overflowBottles);
                net.minecraft.entity.item.EntityItem itemEntity = new net.minecraft.entity.item.EntityItem(
                    player.world,
                    player.posX,
                    player.posY + 0.5,
                    player.posZ,
                    overflowStack
                );
                player.world.spawnEntity(itemEntity);
                newXp = maxXp;
            } else {
                newXp = maxXp;
            }
        }

        cap.setXpStored(newXp);
        syncCapabilityToStack(stack, cap);
    }

    
    private void pumpExperienceBetweenPlayerAndTank(EntityPlayer player, ItemStack stack, IExperiencePumpCapability cap) {
        com.rsring.experience.ExperiencePumpController controller = com.rsring.experience.ExperiencePumpController.getInstance();
        if (controller.isTankManagedByController(stack)) {
            return;
        }

        int retain = cap.getRetainLevel();
        int playerTotal = getPlayerTotalXp(player);
        int targetXp = getTotalXpForLevel(retain);

        if (cap.getMode() == IExperiencePumpCapability.MODE_PUMP_FROM_PLAYER) {
            int excess = playerTotal - targetXp;
            if (excess > 0) {
                int canStore = cap.getMaxXp() - cap.getXpStored();
                int take = Math.min(excess, canStore);
                if (take > 0) {
                    addPlayerXp(player, -take);
                    cap.addXp(take);
                    syncCapabilityToStack(stack, cap);
                }
            }
        } else if (cap.getMode() == IExperiencePumpCapability.MODE_PUMP_TO_PLAYER) {
            int need = targetXp - playerTotal;
            if (need > 0) {
                int perTickLimit = Math.max(1, getExtractionRate());
                int toTake = Math.min(need, perTickLimit);
                int give = cap.takeXp(toTake);
                if (give > 0) {
                    addPlayerXp(player, give);
                    syncCapabilityToStack(stack, cap);
                }
            }
        }
    }

    
    private boolean isUpgradeActive(ItemStack stack, IExperiencePumpCapability cap) {
        return stack != null && !stack.isEmpty() &&
               com.rsring.config.ExperienceTankConfig.tank.enabled &&
               cap != null;
    }

    
    private int getExtractionRate() {
        return com.rsring.config.ExperienceTankConfig.tank.xpExtractionRate;
    }

    private int getXpPerBottle() {
        double efficiency = com.rsring.config.ExperienceTankConfig.tank.xpToBottleEfficiency;
        if (efficiency <= 0) efficiency = 1.0;
        int xpPerBottle = (int) Math.round(XP_PER_BOTTLE * efficiency);
        return Math.max(1, xpPerBottle);
    }

    
    private int getMaxStorage(IExperiencePumpCapability cap) {
        return cap.getMaxXp();
    }

    
    private int extractXpFromSurroundings(EntityPlayer player, ItemStack stack, IExperiencePumpCapability cap) {
        if (cap.getXpStored() >= cap.getMaxXp()) {
            return 0;
        }

        int baseRate = getExtractionRate();
        int interval = Math.max(1, com.rsring.config.ExperienceTankConfig.tank.extractionInterval);
        int maxExtract = baseRate * interval;
        int extractedTotal = 0;
        double range = com.rsring.config.ExperienceTankConfig.tank.xpExtractionRange;

        net.minecraft.util.math.AxisAlignedBB extractArea = player.getEntityBoundingBox().grow(range);

        extractedTotal = extractXpFromOrbs(player, extractArea, maxExtract, cap);

        if (extractedTotal < maxExtract && com.rsring.config.ExperienceTankConfig.tank.extractXpBottles) {
            extractedTotal += extractXpFromBottleItems(player, extractArea, maxExtract, extractedTotal, cap);
        }

        if (extractedTotal < maxExtract && com.rsring.config.ExperienceTankConfig.tank.extractXpBottles) {
            extractedTotal += extractXpFromThrownBottles(player, extractArea, maxExtract, extractedTotal, cap);
        }

        return extractedTotal;
    }

    
    private int extractXpFromOrbs(EntityPlayer player, net.minecraft.util.math.AxisAlignedBB extractArea,
                                  int maxExtract, IExperiencePumpCapability cap) {
        int extractedTotal = 0;

        java.util.List<net.minecraft.entity.item.EntityXPOrb> xpOrbs =
            player.world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityXPOrb.class, extractArea);

        for (net.minecraft.entity.item.EntityXPOrb orb : xpOrbs) {
            if (extractedTotal >= maxExtract) {
                break;
            }
            if (orb.isDead || orb.xpValue <= 0) {
                continue;
            }

            int currentStored = cap.getXpStored();
            int maxCanStore = cap.getMaxXp() - currentStored;
            int extractAmount = Math.min(maxExtract - extractedTotal, Math.min(orb.xpValue, maxCanStore));

            if (extractAmount > 0) {
                orb.xpValue -= extractAmount;
                if (orb.xpValue <= 0) {
                    orb.setDead();
                }
                extractedTotal += extractAmount;
            }
        }

        return extractedTotal;
    }

    
    private int extractXpFromBottleItems(EntityPlayer player, net.minecraft.util.math.AxisAlignedBB extractArea,
                                         int maxExtract, int currentExtracted, IExperiencePumpCapability cap) {
        int extractedTotal = currentExtracted;
        int xpPerBottle = getXpPerBottle();

        java.util.List<net.minecraft.entity.item.EntityItem> xpBottleItems = player.world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, extractArea);

        for (net.minecraft.entity.item.EntityItem itemEntity : xpBottleItems) {
            if (extractedTotal >= maxExtract) {
                break;
            }

            if (itemEntity == null || itemEntity.isDead) continue;

            net.minecraft.item.ItemStack bottleStack = itemEntity.getItem();
            if (bottleStack.isEmpty() || bottleStack.getItem() != net.minecraft.init.Items.EXPERIENCE_BOTTLE) continue;

            int bottleCount = bottleStack.getCount();
            int currentStored = cap.getXpStored();
            int maxCanStore = cap.getMaxXp() - currentStored;

            if (maxCanStore <= 0) {
                break;
            }

            int maxBottlesFromCapacity = maxCanStore / xpPerBottle;
            int partialBottleXp = maxCanStore % xpPerBottle;

            int bottlesToConvert = Math.min(bottleCount, maxBottlesFromCapacity);
            if (bottlesToConvert > 0) {
                extractedTotal += bottlesToConvert * xpPerBottle;
                bottleStack.shrink(bottlesToConvert);
                bottleCount -= bottlesToConvert;
                maxCanStore -= bottlesToConvert * xpPerBottle;
            }

            if (partialBottleXp > 0 && bottleCount > 0 && maxCanStore > 0) {
                int takeXp = Math.min(partialBottleXp, maxCanStore);
                extractedTotal += takeXp;
                bottleStack.shrink(1);
                bottleCount--;
                maxCanStore -= takeXp;
            }

            if (bottleStack.isEmpty() || bottleStack.getCount() <= 0) {
                itemEntity.setDead();
            } else {
                itemEntity.setItem(bottleStack);
            }
        }

        return extractedTotal - currentExtracted;
    }

    
    private int extractXpFromThrownBottles(EntityPlayer player, net.minecraft.util.math.AxisAlignedBB extractArea,
                                           int maxExtract, int currentExtracted, IExperiencePumpCapability cap) {
        int extractedTotal = currentExtracted;
        int xpPerBottle = getXpPerBottle();

        java.util.List<EntityExpBottle> thrownBottles = player.world.getEntitiesWithinAABB(EntityExpBottle.class, extractArea);
        for (EntityExpBottle thrown : thrownBottles) {
            if (extractedTotal >= maxExtract) break;
            if (thrown.isDead) continue;

            int currentStored = cap.getXpStored();
            int maxCanStore = cap.getMaxXp() - currentStored;
            int canTakeForThis = Math.min(xpPerBottle, Math.min(maxExtract - extractedTotal, maxCanStore));
            if (canTakeForThis > 0) {
                extractedTotal += canTakeForThis;
                thrown.setDead();
            }
        }

        return extractedTotal - currentExtracted;
    }


    
    private void tryRepairMending(EntityPlayer player, ItemStack pump, IExperiencePumpCapability cap) {
        if (player == null || pump == null || cap == null || !com.rsring.config.ExperienceTankConfig.tank.mendPlayerItems) return;

        int availableXp = cap.getXpStored();
        if (availableXp <= 0) return;

        if (pump.isItemDamaged()) {
            availableXp = mendSingleItem(pump, availableXp, cap);
        }

        if (availableXp > 0 && player.inventory != null && player.inventory.mainInventory != null) {
            for (ItemStack stack : player.inventory.mainInventory) {
                if (stack == null || availableXp <= 0) break;
                if (stack.isItemDamaged() && net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.MENDING, stack) > 0) {
                    availableXp = mendSingleItem(stack, availableXp, cap);
                }
            }
        }

        if (availableXp > 0) {
            ItemStack off = player.getHeldItemOffhand();
            if (off != null && !off.isEmpty() && off.isItemDamaged() &&
                net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.MENDING, off) > 0) {
                mendSingleItem(off, availableXp, cap);
            }
        }

        syncCapabilityToStack(pump, cap);
    }

    
    private int mendSingleItem(ItemStack stack, int availableXp, IExperiencePumpCapability cap) {
        if (stack == null || cap == null || availableXp <= 0) {
            return availableXp;
        }

        if (!stack.isItemDamaged()) {
            return availableXp;
        }

        int damage = stack.getItemDamage();
        int maxDurability = stack.getMaxDamage();

        if (maxDurability <= 0) {
            return availableXp;
        }

        int needRepair = damage;
        double efficiency = com.rsring.config.ExperienceTankConfig.tank.xpMendingEfficiency;
        if (efficiency <= 0) efficiency = 1.0;
        int needXp = (int) Math.ceil(needRepair / (2.0 * efficiency));
        int consumeXp = Math.min(needXp, availableXp);
        int repairDurability = (int) Math.floor(consumeXp * 2.0 * efficiency);
        if (consumeXp > 0 && repairDurability <= 0) {
            repairDurability = 1;
        }
        int newDamage = Math.max(damage - repairDurability, 0);

        stack.setItemDamage(newDamage);
        cap.takeXp(consumeXp);
        return availableXp - consumeXp;
    }

    private static int getPlayerTotalXp(EntityPlayer player) {
        return com.rsring.util.XpHelper.getPlayerTotalExperience(player);
    }

    private static int getTotalXpForLevel(int level) {
        return com.rsring.util.XpHelper.getExperienceForLevel(level);
    }

    

    private static void addPlayerXp(EntityPlayer player, int amount) {
        if (amount == 0) return;
        if (amount > 0) {
            com.rsring.util.XpHelper.addExperienceToPlayer(player, amount);
            return;
        }

        // amount < 0 : remove XP safely using XpHelper
        int take = -amount;
        com.rsring.util.XpHelper.removeExperienceFromPlayer(player, take);
    }

    
    public static boolean hasXp(ItemStack stack) {
        return getXpStoredFromNBT(stack) > 0;
    }

    
    public static int getXpFillLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FILL_LEVEL_EMPTY;
        }

        int stored = 0;
        int max = 0;

        net.minecraft.nbt.NBTTagCompound data = getDataFromNBT(stack);
        if (data != null) {
            stored = data.getInteger(XP_KEY);
            max = getMaxXpFromNBT(stack);
        } else {
            IExperiencePumpCapability cap = null;
            try {
                cap = stack.getCapability(ExperiencePumpCapability.EXPERIENCE_PUMP_CAPABILITY, null);
            } catch (Throwable ignored) {}

            if (cap != null) {
                try {
                    stored = cap.getXpStored();
                    max = cap.getMaxXp();
                } catch (Throwable ignored) {
                    return FILL_LEVEL_EMPTY;
                }
            } else {
                return FILL_LEVEL_EMPTY;
            }
        }

        if (stored <= 0 || max <= 0) return FILL_LEVEL_EMPTY;
        if (stored >= max) return FILL_LEVEL_FULL;

        long fillRatio = (long) stored * 10000 / max;
        
        if (fillRatio < 3000) return FILL_LEVEL_QUARTER;
        if (fillRatio < 6000) return FILL_LEVEL_HALF;
        if (fillRatio < 9000) return FILL_LEVEL_THREE_QUARTERS;
        return FILL_LEVEL_FULL;
    }
}
