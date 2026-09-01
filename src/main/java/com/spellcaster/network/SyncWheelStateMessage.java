package com.spellcaster.network;

import java.util.Arrays;

import com.spellcaster.Spellcaster;
import com.spellcaster.wheel.WheelMode;
import com.spellcaster.wheel.WheelNbt;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

public class SyncWheelStateMessage implements IMessage {

    private static final int MAX_CAPACITY = 21;

    @Getter
    private boolean heldItem;
    @Getter
    private int capacity;
    @Getter
    private int cursor;
    private int mode;
    private int[] sequence = new int[MAX_CAPACITY];

    public SyncWheelStateMessage() {}

    public SyncWheelStateMessage(boolean heldItem, int capacity, int[] sequence, int cursor, WheelMode mode) {
        this.heldItem = heldItem;
        this.capacity = Math.max(0, Math.min(MAX_CAPACITY, capacity));
        this.cursor = cursor;
        this.mode = mode == null ? WheelMode.FATE_NORMAL.ordinal() : mode.ordinal();
        this.sequence = copySequence(sequence);
    }

    public void fromBytes(ByteBuf buffer) {
        this.heldItem = buffer.readBoolean();
        this.capacity = Math.max(0, Math.min(MAX_CAPACITY, buffer.readInt()));
        this.cursor = buffer.readInt();
        this.mode = buffer.readInt();
        this.sequence = new int[MAX_CAPACITY];
        for (int i = 0; i < this.sequence.length; ++i) {
            this.sequence[i] = buffer.readInt();
        }
    }

    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(this.heldItem);
        buffer.writeInt(this.capacity);
        buffer.writeInt(this.cursor);
        buffer.writeInt(this.mode);
        for (int skillId : this.sequence) {
            buffer.writeInt(skillId);
        }
    }

    private static int[] copySequence(int[] sequence) {
        int[] result = new int[MAX_CAPACITY];
        Arrays.fill(result, -1);
        if (sequence != null) {
            for (int i = 0; i < result.length && i < sequence.length; ++i) {
                result[i] = WheelNbt.isSkillIdValid(sequence[i]) ? sequence[i] : -1;
            }
        }
        return result;
    }

    public static class Handler implements IMessageHandler<SyncWheelStateMessage, IMessage> {

        public IMessage onMessage(final SyncWheelStateMessage message, MessageContext context) {
            Spellcaster.proxy.handleWheelState(message);
            return null;
        }
    }

    public WheelMode getMode() {
        return this.mode == WheelMode.FATE_STRICT.ordinal() ? WheelMode.FATE_STRICT
            : this.mode == WheelMode.CHAOS.ordinal() ? WheelMode.CHAOS : WheelMode.FATE_NORMAL;
    }

    public int[] getSequence() {
        return this.sequence.clone();
    }
}
