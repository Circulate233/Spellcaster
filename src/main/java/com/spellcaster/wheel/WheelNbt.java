package com.spellcaster.wheel;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class WheelNbt {

    public static final int CURRENT_VERSION = 1;

    private static final String ROOT = "Spellcaster";
    private static final String VERSION = "DataVersion";
    private static final String SEQUENCE = "Sequence";
    private static final String CURSOR = "Cursor";
    private static final String MODE = "Mode";

    private WheelNbt() {}

    public static int[] readSequence(ItemStack stack, int capacity) {
        int[] result = new int[Math.max(0, capacity)];
        Arrays.fill(result, -1);

        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(ROOT, 10)) {
            return result;
        }

        NBTTagCompound data = stack.getTagCompound()
            .getCompoundTag(ROOT);
        int[] saved = data.getIntArray(SEQUENCE);
        for (int i = 0; i < result.length && i < saved.length; ++i) {
            result[i] = isSkillIdValid(saved[i]) ? saved[i] : -1;
        }
        return result;
    }

    public static int readCursor(ItemStack stack, int capacity) {
        if (capacity <= 0 || stack == null
            || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(ROOT, 10)) {
            return 0;
        }
        return normalize(
            stack.getTagCompound()
                .getCompoundTag(ROOT)
                .getInteger(CURSOR),
            capacity);
    }

    public static WheelMode readMode(ItemStack stack, WheelType type) {
        if (type == WheelType.CHAOS) {
            return WheelMode.CHAOS;
        }
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(ROOT, 10)) {
            return WheelMode.FATE_NORMAL;
        }
        int ordinal = stack.getTagCompound()
            .getCompoundTag(ROOT)
            .getInteger(MODE);
        return ordinal == WheelMode.FATE_STRICT.ordinal() ? WheelMode.FATE_STRICT : WheelMode.FATE_NORMAL;
    }

    public static void write(ItemStack stack, int[] sequence, int cursor, WheelMode mode) {
        if (stack == null) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger(VERSION, CURRENT_VERSION);
        int[] saved = sequence == null ? new int[0] : sequence.clone();
        for (int i = 0; i < saved.length; ++i) {
            if (!isSkillIdValid(saved[i])) {
                saved[i] = -1;
            }
        }
        data.setIntArray(SEQUENCE, saved);
        data.setInteger(CURSOR, saved.length == 0 ? 0 : normalize(cursor, saved.length));
        data.setInteger(MODE, mode == null ? WheelMode.FATE_NORMAL.ordinal() : mode.ordinal());
        stack.getTagCompound()
            .setTag(ROOT, data);
    }

    public static void writeMode(ItemStack stack, int capacity, WheelMode mode) {
        int[] sequence = readSequence(stack, capacity);
        write(stack, sequence, readCursor(stack, sequence.length), mode);
    }

    public static boolean migrate(ItemStack stack, int capacity, WheelType type) {
        if (stack == null || capacity < 0
            || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(ROOT, 10)) {
            return false;
        }
        NBTTagCompound data = stack.getTagCompound()
            .getCompoundTag(ROOT);
        int version = data.hasKey(VERSION) ? data.getInteger(VERSION) : 0;
        if (version > CURRENT_VERSION) {
            return false;
        }

        int[] rawSequence = data.getIntArray(SEQUENCE);
        int[] sequence = readSequence(stack, capacity);
        int cursor = readCursor(stack, capacity);
        WheelMode mode = readMode(stack, type);
        boolean dirty = version != CURRENT_VERSION || rawSequence.length != capacity
            || data.getInteger(CURSOR) != cursor
            || data.getInteger(MODE) != mode.ordinal();
        for (int skillId : rawSequence) {
            if (skillId != -1 && !isSkillIdValid(skillId)) {
                dirty = true;
                break;
            }
        }
        if (dirty) {
            write(stack, sequence, cursor, mode);
        }
        return dirty;
    }

    public static int readVersion(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(ROOT, 10)) {
            return 0;
        }
        return stack.getTagCompound()
            .getCompoundTag(ROOT)
            .getInteger(VERSION);
    }

    public static boolean isSkillIdValid(int skillId) {
        int tier = skillId / 100;
        int slot = skillId % 100;
        return tier >= 1 && tier <= 3 && slot >= 0 && slot < 7;
    }

    private static int normalize(int cursor, int capacity) {
        int value = cursor % capacity;
        return value < 0 ? value + capacity : value;
    }
}
