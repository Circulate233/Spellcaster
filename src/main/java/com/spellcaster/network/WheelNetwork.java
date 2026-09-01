package com.spellcaster.network;

import com.spellcaster.Spellcaster;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class WheelNetwork {

    private static boolean initialized;

    private WheelNetwork() {}

    public static SimpleNetworkWrapper getInstance() {
        return Holder.INSTANCE;
    }

    public static void init() {
        if (!initialized) {
            getInstance()
                .registerMessage(ActivateWheelMessage.Handler.class, ActivateWheelMessage.class, 0, Side.SERVER);
            getInstance()
                .registerMessage(CycleWheelModeMessage.Handler.class, CycleWheelModeMessage.class, 1, Side.SERVER);
            getInstance()
                .registerMessage(SaveWheelConfigMessage.Handler.class, SaveWheelConfigMessage.class, 2, Side.SERVER);
            getInstance()
                .registerMessage(SyncWheelStateMessage.Handler.class, SyncWheelStateMessage.class, 3, Side.CLIENT);
            getInstance().registerMessage(WheelStatusMessage.Handler.class, WheelStatusMessage.class, 4, Side.CLIENT);
            initialized = true;
        }
    }

    private static class Holder {

        private static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE
            .newSimpleChannel(Spellcaster.MODID);
    }
}
