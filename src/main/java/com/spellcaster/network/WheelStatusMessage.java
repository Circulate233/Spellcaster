package com.spellcaster.network;

import com.spellcaster.Spellcaster;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

@Getter
public class WheelStatusMessage implements IMessage {

    public static final int MODE_CHANGED = 0;
    public static final int CONFIG_SAVED = 1;
    public static final int NO_WHEEL = 2;
    public static final int NO_AVAILABLE_SKILL = 3;
    public static final int INVALID_CONFIG = 4;
    public static final int MODE_UNAVAILABLE = 5;

    private int status;

    public WheelStatusMessage() {}

    public WheelStatusMessage(int status) {
        this.status = normalize(status);
    }

    public void fromBytes(ByteBuf buffer) {
        this.status = normalize(buffer.readInt());
    }

    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.status);
    }

    private static int normalize(int status) {
        return status >= MODE_CHANGED && status <= MODE_UNAVAILABLE ? status : INVALID_CONFIG;
    }

    public static class Handler implements IMessageHandler<WheelStatusMessage, IMessage> {

        public IMessage onMessage(WheelStatusMessage message, MessageContext context) {
            Spellcaster.proxy.handleWheelStatus(message.getStatus());
            return null;
        }
    }
}
