package com.spellcaster.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.spellcaster.item.WheelItemBase;
import com.spellcaster.wheel.ManaMetalBridge;
import com.spellcaster.wheel.SkillSelection;
import com.spellcaster.wheel.WheelMode;
import com.spellcaster.wheel.WheelNbt;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ActivateWheelMessage implements IMessage {

    public void fromBytes(ByteBuf buffer) {}

    public void toBytes(ByteBuf buffer) {}

    public static class Handler implements IMessageHandler<ActivateWheelMessage, IMessage> {

        public IMessage onMessage(ActivateWheelMessage message, MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().playerEntity;
            ServerTaskQueue.enqueue(() -> activate(player));
            return null;
        }

        private static void activate(EntityPlayerMP player) {
            if (player == null || player.worldObj == null || player.worldObj.isRemote) {
                return;
            }
            ItemStack stack = ManaMetalBridge.getCoreWheel(player);
            if (stack == null || !(stack.getItem() instanceof WheelItemBase item)) {
                sendStatus(player, WheelStatusMessage.NO_WHEEL);
                return;
            }
            if (!ManaMetalBridge.canUseWheel(player, stack)) {
                sendStatus(player, WheelStatusMessage.INVALID_CONFIG);
                return;
            }
            int capacity = item.getCapacity(stack);
            WheelNbt.migrate(stack, capacity, item.getWheelType());
            WheelMode mode = WheelNbt.readMode(stack, item.getWheelType());
            int[] sequence = WheelNbt.readSequence(stack, capacity);
            SkillSelection selection = ManaMetalBridge
                .select(player, sequence, WheelNbt.readCursor(stack, capacity), mode);
            if (selection.isSelected() && ManaMetalBridge.castServer(player, selection.getSkillId())) {
                WheelNbt.write(stack, sequence, selection.getNextCursor(), mode);
                ManaMetalBridge.getRoot(player).item.synchronous();
                WheelNetwork.getInstance()
                    .sendTo(
                        new SyncWheelStateMessage(false, capacity, sequence, selection.getNextCursor(), mode),
                        player);
            } else if (!selection.isSelected()) {
                sendStatus(player, WheelStatusMessage.NO_AVAILABLE_SKILL);
            }
        }

        private static void sendStatus(EntityPlayerMP player, int status) {
            WheelNetwork.getInstance()
                .sendTo(new WheelStatusMessage(status), player);
        }
    }
}
