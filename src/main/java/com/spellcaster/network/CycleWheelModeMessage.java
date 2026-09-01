package com.spellcaster.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.spellcaster.item.WheelItemBase;
import com.spellcaster.wheel.ManaMetalBridge;
import com.spellcaster.wheel.WheelMode;
import com.spellcaster.wheel.WheelNbt;
import com.spellcaster.wheel.WheelType;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CycleWheelModeMessage implements IMessage {

    public void fromBytes(ByteBuf buffer) {}

    public void toBytes(ByteBuf buffer) {}

    public static class Handler implements IMessageHandler<CycleWheelModeMessage, IMessage> {

        public IMessage onMessage(CycleWheelModeMessage message, MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().playerEntity;
            ServerTaskQueue.enqueue(() -> cycle(player));
            return null;
        }

        private static void cycle(EntityPlayerMP player) {
            if (player == null || player.worldObj == null || player.worldObj.isRemote) {
                return;
            }
            ItemStack stack = ManaMetalBridge.getCoreWheel(player);
            if (stack == null || !(stack.getItem() instanceof WheelItemBase item)) {
                WheelNetwork.getInstance()
                    .sendTo(new WheelStatusMessage(WheelStatusMessage.NO_WHEEL), player);
                return;
            }
            if (!ManaMetalBridge.canUseWheel(player, stack)) {
                WheelNetwork.getInstance()
                    .sendTo(new WheelStatusMessage(WheelStatusMessage.INVALID_CONFIG), player);
                return;
            }
            if (item.getWheelType() != WheelType.FATE) {
                WheelNetwork.getInstance()
                    .sendTo(new WheelStatusMessage(WheelStatusMessage.MODE_UNAVAILABLE), player);
                return;
            }
            int capacity = item.getCapacity(stack);
            WheelNbt.migrate(stack, capacity, item.getWheelType());
            WheelMode current = WheelNbt.readMode(stack, item.getWheelType());
            WheelMode next = current == WheelMode.FATE_STRICT ? WheelMode.FATE_NORMAL : WheelMode.FATE_STRICT;
            WheelNbt.writeMode(stack, capacity, next);
            ManaMetalBridge.getRoot(player).item.synchronous();
            WheelNetwork.getInstance()
                .sendTo(
                    new SyncWheelStateMessage(
                        false,
                        capacity,
                        WheelNbt.readSequence(stack, capacity),
                        WheelNbt.readCursor(stack, capacity),
                        next),
                    player);
            WheelNetwork.getInstance()
                .sendTo(new WheelStatusMessage(WheelStatusMessage.MODE_CHANGED), player);
        }
    }
}
