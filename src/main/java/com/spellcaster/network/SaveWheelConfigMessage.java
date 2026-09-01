package com.spellcaster.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.spellcaster.item.WheelItemBase;
import com.spellcaster.wheel.ManaMetalBridge;
import com.spellcaster.wheel.WheelMode;
import com.spellcaster.wheel.WheelNbt;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SaveWheelConfigMessage implements IMessage {

    private static final int MAX_CAPACITY = 21;

    private boolean heldItem;
    private int[] sequence = new int[MAX_CAPACITY];

    public SaveWheelConfigMessage() {}

    public SaveWheelConfigMessage(boolean heldItem, int[] sequence) {
        this.heldItem = heldItem;
        this.sequence = copySequence(sequence);
    }

    public void fromBytes(ByteBuf buffer) {
        this.heldItem = buffer.readBoolean();
        this.sequence = new int[MAX_CAPACITY];
        for (int i = 0; i < this.sequence.length; ++i) {
            this.sequence[i] = buffer.readInt();
        }
    }

    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(this.heldItem);
        int[] data = copySequence(this.sequence);
        for (int skillId : data) {
            buffer.writeInt(skillId);
        }
    }

    private static int[] copySequence(int[] sequence) {
        int[] result = new int[MAX_CAPACITY];
        for (int i = 0; i < result.length; ++i) {
            int skillId = sequence != null && i < sequence.length ? sequence[i] : -1;
            result[i] = WheelNbt.isSkillIdValid(skillId) ? skillId : -1;
        }
        return result;
    }

    public static class Handler implements IMessageHandler<SaveWheelConfigMessage, IMessage> {

        public IMessage onMessage(final SaveWheelConfigMessage message, MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().playerEntity;
            final boolean heldItem = message.heldItem;
            final int[] sequence = message.sequence.clone();
            ServerTaskQueue.enqueue(() -> save(player, heldItem, sequence));
            return null;
        }

        private static void save(EntityPlayerMP player, boolean heldItem, int[] sequence) {
            if (player == null || player.worldObj == null || player.worldObj.isRemote) {
                return;
            }
            ItemStack stack = heldItem ? player.getCurrentEquippedItem() : ManaMetalBridge.getCoreWheel(player);
            if (stack == null || !(stack.getItem() instanceof WheelItemBase item)) {
                WheelNetwork.getInstance()
                    .sendTo(new WheelStatusMessage(WheelStatusMessage.NO_WHEEL), player);
                return;
            }
            int capacity = item.getCapacity(stack);
            if (capacity <= 0) {
                WheelNetwork.getInstance()
                    .sendTo(new WheelStatusMessage(WheelStatusMessage.INVALID_CONFIG), player);
                return;
            }
            WheelNbt.migrate(stack, capacity, item.getWheelType());
            int[] validated = new int[capacity];
            for (int i = 0; i < capacity; ++i) {
                validated[i] = ManaMetalBridge.isSkillConfigurable(player, sequence[i]) ? sequence[i] : -1;
            }
            WheelMode mode = WheelNbt.readMode(stack, item.getWheelType());
            int cursor = WheelNbt.readCursor(stack, capacity);
            WheelNbt.write(stack, validated, cursor, mode);
            if (heldItem) {
                player.inventory.markDirty();
                player.inventoryContainer.detectAndSendChanges();
            } else {
                ManaMetalBridge.getRoot(player).item.synchronous();
            }
            WheelNetwork.getInstance()
                .sendTo(new SyncWheelStateMessage(heldItem, capacity, validated, cursor, mode), player);
            WheelNetwork.getInstance()
                .sendTo(new WheelStatusMessage(WheelStatusMessage.CONFIG_SAVED), player);
        }
    }
}
