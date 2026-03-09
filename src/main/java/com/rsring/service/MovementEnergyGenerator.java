package com.rsring.service;

import com.rsring.capability.IRsRingCapability;
import com.rsring.capability.RsRingCapability;
import com.rsring.config.RsRingConfig;
import com.rsring.item.ItemAbsorbRing;
import com.rsring.util.BaublesHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 移动发电管理器
 * 根据玩家移动状态（走路、飞行、游泳等）为戒指充能
 */
public class MovementEnergyGenerator {

    // 单例实例
    private static final MovementEnergyGenerator INSTANCE = new MovementEnergyGenerator();

    // 玩家上次位置记录
    private final Map<UUID, PlayerPosition> lastPositions = new HashMap<>();

    // 移动发电冷却记录（防止每tick都发电）
    private final Map<UUID, Integer> energyCooldowns = new HashMap<>();

    // 移动类型定义
    public enum MovementType {
        WALKING,      // 走路
        SPRINTING,    // 疾跑
        FLYING,       // 飞行
        SWIMMING,     // 游泳
        JUMPING,      // 跳跃
        FALLING,      // 坠落
        RIDING,       // 骑乘
        CLIMBING      // 攀爬
    }

    // 玩家位置记录
    private static class PlayerPosition {
        double x, y, z;
        boolean onGround;
        boolean isFlying;
        boolean isInWater;
        boolean isRiding;

        PlayerPosition(EntityPlayer player) {
            this.x = player.posX;
            this.y = player.posY;
            this.z = player.posZ;
            this.onGround = player.onGround;
            this.isFlying = player.capabilities.isFlying;
            this.isInWater = player.isInWater();
            this.isRiding = player.isRiding();
        }
    }

    private MovementEnergyGenerator() {}

    public static MovementEnergyGenerator getInstance() {
        return INSTANCE;
    }

    /**
     * 处理玩家移动发电
     * 每tick调用一次
     */
    public void onPlayerTick(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;

        // 检查是否启用移动发电
        if (!RsRingConfig.absorbRing.enableMovementGeneration) {
            return;
        }

        UUID playerId = player.getUniqueID();
        PlayerPosition lastPos = lastPositions.get(playerId);
        PlayerPosition currentPos = new PlayerPosition(player);

        // 首次记录位置
        if (lastPos == null) {
            lastPositions.put(playerId, currentPos);
            return;
        }

        // 计算移动距离
        double deltaX = currentPos.x - lastPos.x;
        double deltaY = currentPos.y - lastPos.y;
        double deltaZ = currentPos.z - lastPos.z;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        // 只有移动距离超过阈值才计算发电
        if (distance < RsRingConfig.absorbRing.movementMinDistance) {
            return;
        }

        // 检查冷却
        int cooldown = energyCooldowns.getOrDefault(playerId, 0);
        if (cooldown > 0) {
            energyCooldowns.put(playerId, cooldown - 1);
            return;
        }

        // 判断移动类型并发电
        MovementType movementType = detectMovementType(player, lastPos, currentPos, distance);
        if (movementType != null) {
            generateEnergy(player, movementType);
            // 设置冷却
            energyCooldowns.put(playerId, RsRingConfig.absorbRing.movementGenerationInterval);
        }

        // 更新位置记录
        lastPositions.put(playerId, currentPos);
    }

    /**
     * 检测玩家的移动类型
     */
    private MovementType detectMovementType(EntityPlayer player, PlayerPosition lastPos,
                                            PlayerPosition currentPos, double distance) {
        // 骑乘状态
        if (currentPos.isRiding) {
            return MovementType.RIDING;
        }

        // 飞行状态
        if (currentPos.isFlying) {
            return MovementType.FLYING;
        }

        // 水中状态
        if (currentPos.isInWater) {
            return MovementType.SWIMMING;
        }

        // 攀爬状态（在梯子等）
        if (player.isOnLadder()) {
            return MovementType.CLIMBING;
        }

        // 坠落状态
        if (!currentPos.onGround && currentPos.y < lastPos.y) {
            return MovementType.FALLING;
        }

        // 跳跃状态
        if (!currentPos.onGround && currentPos.y > lastPos.y) {
            return MovementType.JUMPING;
        }

        // 地面移动
        if (currentPos.onGround && distance > 0.01) {
            if (player.isSprinting()) {
                return MovementType.SPRINTING;
            }
            return MovementType.WALKING;
        }

        return null;
    }

    /**
     * 根据移动类型为戒指充能
     */
    private void generateEnergy(EntityPlayer player, MovementType movementType) {
        // 获取发电数量
        int energyAmount = getEnergyAmount(movementType);
        if (energyAmount <= 0) return;

        // 查找所有戒指
        java.util.List<ItemStack> rings = findAllRings(player);

        for (ItemStack ringStack : rings) {
            if (ringStack.isEmpty()) continue;

            IRsRingCapability cap = ringStack.getCapability(RsRingCapability.RS_RING_CAPABILITY, null);
            if (cap == null) continue;

            IEnergyStorage energy = cap.getEnergyStorage();
            if (energy == null) continue;

            // 尝试充能
            int received = energy.receiveEnergy(energyAmount, false);
            if (received > 0) {
                RsRingCapability.syncCapabilityToStack(ringStack, cap);
            }
        }
    }

    /**
     * 获取不同移动类型的发电数量
     */
    private int getEnergyAmount(MovementType movementType) {
        switch (movementType) {
            case WALKING:
                return RsRingConfig.absorbRing.energyPerWalk;
            case SPRINTING:
                return RsRingConfig.absorbRing.energyPerSprint;
            case FLYING:
                return RsRingConfig.absorbRing.energyPerFly;
            case SWIMMING:
                return RsRingConfig.absorbRing.energyPerSwim;
            case JUMPING:
                return RsRingConfig.absorbRing.energyPerJump;
            case FALLING:
                return RsRingConfig.absorbRing.energyPerFall;
            case RIDING:
                return RsRingConfig.absorbRing.energyPerRide;
            case CLIMBING:
                return RsRingConfig.absorbRing.energyPerClimb;
            default:
                return 0;
        }
    }

    /**
     * 查找玩家所有吸收戒指
     */
    private java.util.List<ItemStack> findAllRings(EntityPlayer player) {
        java.util.List<ItemStack> rings = new java.util.ArrayList<>();

        // 主手
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof ItemAbsorbRing) {
            rings.add(mainHand);
        }

        // 副手
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && offHand.getItem() instanceof ItemAbsorbRing) {
            rings.add(offHand);
        }

        // 饰品栏
        if (BaublesHelper.isBaublesLoaded()) {
            Object handler = BaublesHelper.getBaublesHandler(player);
            int size = BaublesHelper.getSlots(handler);
            for (int i = 0; i < size; i++) {
                ItemStack stack = BaublesHelper.getStackInSlot(handler, i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemAbsorbRing) {
                    rings.add(stack);
                }
            }
        }

        // 背包
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemAbsorbRing)) {
                continue;
            }
            // 跳过已添加的
            if (stack == mainHand || stack == offHand) {
                continue;
            }
            rings.add(stack);
        }

        return rings;
    }

    /**
     * 清理玩家数据（玩家下线时调用）
     */
    public void cleanupPlayer(UUID playerId) {
        lastPositions.remove(playerId);
        energyCooldowns.remove(playerId);
    }
}
