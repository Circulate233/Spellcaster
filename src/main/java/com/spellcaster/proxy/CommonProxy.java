package com.spellcaster.proxy;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.spellcaster.Config;
import com.spellcaster.content.SpellcasterItems;
import com.spellcaster.content.SpellcasterRecipes;
import com.spellcaster.network.ServerTaskQueue;
import com.spellcaster.network.SyncWheelStateMessage;
import com.spellcaster.network.WheelNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class CommonProxy {

    public CommonProxy() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        if (!Config.clientOnlyMode) {
            WheelNetwork.init();
            SpellcasterItems.register();
            SpellcasterRecipes.register();
        }
    }

    public void init(FMLInitializationEvent event) {}

    public void openWheelGui(ItemStack stack) {}

    public void handleWheelState(SyncWheelStateMessage message) {}

    public void handleWheelStatus(int status) {}

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ServerTaskQueue.drain();
        }
    }
}
