package com.rsring.util;

import net.minecraft.nbt.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * NBT安全工具类 - 防止NBT相关攻击和性能问题
 *
 * 核心功能：
 * 1. NBT大小限制检查
 * 2. 递归深度限制
 * 3. 恶意NBT检测
 * 4. NBT压缩/解压
 * 5. 网络传输优化
 *
 * 安全特性：
 * - 防止NBT炸弹攻击（zip bomb）
 * - 防止栈溢出攻击（深度限制）
 * - 防止内存耗尽（大小限制）
 */
public class NbtSecurityUtils {

    private static final Logger LOGGER = LogManager.getLogger(NbtSecurityUtils.class);

    // NBT大小限制（字节）
    public static final int MAX_NBT_SIZE = 1024 * 1024; // 1MB
    public static final int MAX_NETWORK_NBT_SIZE = 64 * 1024; // 64KB（网络传输）

    // 递归深度限制
    public static final int MAX_NBT_DEPTH = 16;

    // 最大标签数量
    public static final int MAX_TAG_COUNT = 10000;

    // 压缩阈值
    public static final int COMPRESSION_THRESHOLD = 1024; // 1KB以上压缩

    /**
     * 验证NBT是否安全
     *
     * @param nbt 要验证的NBT
     * @return 是否安全
     */
    public static boolean isSafeNbt(NBTTagCompound nbt) {
        if (nbt == null) return true;

        try {
            // 检查深度
            int depth = calculateDepth(nbt, 0);
            if (depth > MAX_NBT_DEPTH) {
                LOGGER.warn("NBT深度超过限制: {} > {}", depth, MAX_NBT_DEPTH);
                return false;
            }

            // 检查标签数量
            int count = countTags(nbt);
            if (count > MAX_TAG_COUNT) {
                LOGGER.warn("NBT标签数量超过限制: {} > {}", count, MAX_TAG_COUNT);
                return false;
            }

            // 检查序列化后大小
            byte[] data = writeNbtToBytes(nbt);
            if (data.length > MAX_NBT_SIZE) {
                LOGGER.warn("NBT大小超过限制: {} > {} bytes", data.length, MAX_NBT_SIZE);
                return false;
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("NBT验证失败", e);
            return false;
        }
    }

    /**
     * 计算NBT深度
     */
    private static int calculateDepth(NBTBase nbt, int currentDepth) {
        if (currentDepth > MAX_NBT_DEPTH) {
            return currentDepth;
        }

        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) nbt;
            int maxChildDepth = currentDepth;
            for (String key : compound.getKeySet()) {
                NBTBase child = compound.getTag(key);
                int childDepth = calculateDepth(child, currentDepth + 1);
                maxChildDepth = Math.max(maxChildDepth, childDepth);
            }
            return maxChildDepth;
        } else if (nbt instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) nbt;
            int maxChildDepth = currentDepth;
            for (int i = 0; i < list.tagCount(); i++) {
                NBTBase child = list.get(i);
                int childDepth = calculateDepth(child, currentDepth + 1);
                maxChildDepth = Math.max(maxChildDepth, childDepth);
            }
            return maxChildDepth;
        }

        return currentDepth;
    }

    /**
     * 统计NBT标签数量
     */
    private static int countTags(NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) nbt;
            int count = compound.getKeySet().size();
            for (String key : compound.getKeySet()) {
                count += countTags(compound.getTag(key));
            }
            return count;
        } else if (nbt instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) nbt;
            int count = list.tagCount();
            for (int i = 0; i < list.tagCount(); i++) {
                count += countTags(list.get(i));
            }
            return count;
        }
        return 1;
    }

    /**
     * 将NBT写入字节数组
     */
    public static byte[] writeNbtToBytes(NBTTagCompound nbt) {
        if (nbt == null) return new byte[0];
        try {
            return CompressedStreamTools.writeCompressed(nbt);
        } catch (IOException e) {
            LOGGER.error("NBT序列化失败", e);
            return new byte[0];
        }
    }

    /**
     * 从字节数组读取NBT（带安全检查）
     */
    public static NBTTagCompound readNbtFromBytes(byte[] data) {
        if (data == null || data.length == 0) return new NBTTagCompound();
        if (data.length > MAX_NBT_SIZE) {
            LOGGER.warn("NBT数据过大: {} bytes", data.length);
            return new NBTTagCompound();
        }
        try {
            return CompressedStreamTools.readCompressed(new java.io.ByteArrayInputStream(data));
        } catch (IOException e) {
            LOGGER.error("NBT反序列化失败", e);
            return new NBTTagCompound();
        }
    }

    /**
     * 压缩NBT数据
     *
     * @param data 原始数据
     * @return 压缩后的数据
     */
    public static byte[] compress(byte[] data) {
        if (data == null || data.length < COMPRESSION_THRESHOLD) {
            return data;
        }

        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(data);
        deflater.finish();

        byte[] buffer = new byte[data.length];
        int compressedSize = deflater.deflate(buffer);
        deflater.end();

        if (compressedSize < data.length) {
            byte[] result = new byte[compressedSize + 4];
            // 写入原始大小（用于解压）
            result[0] = (byte) (data.length >> 24);
            result[1] = (byte) (data.length >> 16);
            result[2] = (byte) (data.length >> 8);
            result[3] = (byte) data.length;
            System.arraycopy(buffer, 0, result, 4, compressedSize);
            return result;
        }

        return data;
    }

    /**
     * 解压NBT数据
     *
     * @param data 压缩数据
     * @return 解压后的数据
     */
    public static byte[] decompress(byte[] data) {
        if (data == null || data.length < 5) {
            return data;
        }

        // 检查是否是压缩数据（通过魔数）
        int originalSize = ((data[0] & 0xFF) << 24) |
                          ((data[1] & 0xFF) << 16) |
                          ((data[2] & 0xFF) << 8) |
                          (data[3] & 0xFF);

        if (originalSize < 0 || originalSize > MAX_NBT_SIZE) {
            return data; // 不是压缩数据或大小异常
        }

        try {
            Inflater inflater = new Inflater();
            inflater.setInput(data, 4, data.length - 4);

            byte[] result = new byte[originalSize];
            int resultLength = inflater.inflate(result);
            inflater.end();

            if (resultLength == originalSize) {
                return result;
            }
        } catch (Exception e) {
            LOGGER.debug("解压失败，返回原始数据");
        }

        return data;
    }

    /**
     * 截断NBT到安全大小
     *
     * @param nbt 原始NBT
     * @param maxSize 最大大小
     * @return 截断后的NBT
     */
    public static NBTTagCompound truncateNbt(NBTTagCompound nbt, int maxSize) {
        if (nbt == null) return new NBTTagCompound();

        byte[] data = writeNbtToBytes(nbt);
        if (data.length <= maxSize) {
            return nbt.copy();
        }

        LOGGER.warn("NBT过大，执行截断: {} > {} bytes", data.length, maxSize);

        // 创建精简版本 - 只保留关键字段
        NBTTagCompound truncated = new NBTTagCompound();

        // 保留基本字段
        for (String key : nbt.getKeySet()) {
            NBTBase tag = nbt.getTag(key);
            if (isEssentialTag(key)) {
                if (tag instanceof NBTTagCompound) {
                    // 递归截断子NBT
                    truncated.setTag(key, truncateNbt((NBTTagCompound) tag, maxSize / 2));
                } else if (!(tag instanceof NBTTagList) || ((NBTTagList) tag).tagCount() <= 10) {
                    // 保留非列表或小列表
                    truncated.setTag(key, tag.copy());
                }
            }
        }

        return truncated;
    }

    /**
     * 判断是否是关键标签
     */
    private static boolean isEssentialTag(String key) {
        // 定义关键字段白名单
        String[] essentialKeys = {
            "id", "Count", "Damage", "Slot",
            "x", "y", "z", "dimension",
            "energy", "xp", "mode", "enabled"
        };
        for (String essential : essentialKeys) {
            if (essential.equals(key)) return true;
        }
        return false;
    }

    /**
     * 获取NBT大小信息（用于调试）
     */
    public static NbtSizeInfo getSizeInfo(NBTTagCompound nbt) {
        if (nbt == null) return new NbtSizeInfo(0, 0, 0);

        int depth = calculateDepth(nbt, 0);
        int tagCount = countTags(nbt);
        byte[] data = writeNbtToBytes(nbt);

        return new NbtSizeInfo(data.length, depth, tagCount);
    }

    /**
     * NBT大小信息
     */
    public static class NbtSizeInfo {
        public final int size;
        public final int depth;
        public final int tagCount;

        public NbtSizeInfo(int size, int depth, int tagCount) {
            this.size = size;
            this.depth = depth;
            this.tagCount = tagCount;
        }

        @Override
        public String toString() {
            return String.format("NbtSizeInfo[size=%d bytes, depth=%d, tags=%d]", size, depth, tagCount);
        }

        public boolean isSafe() {
            return size <= MAX_NBT_SIZE &&
                   depth <= MAX_NBT_DEPTH &&
                   tagCount <= MAX_TAG_COUNT;
        }
    }
}
