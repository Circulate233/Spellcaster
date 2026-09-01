package com.spellcaster;

import java.util.Map;

import com.spellcaster.proxy.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = Spellcaster.MODID, version = Tags.VERSION, name = "Spellcaster", acceptedMinecraftVersions = "[1.7.10]")
public class Spellcaster {

    public static final String MODID = "spellcaster";

    @SidedProxy(clientSide = "com.spellcaster.proxy.ClientProxy", serverSide = "com.spellcaster.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @NetworkCheckHandler
    public boolean checkRemoteVersions(Map<String, String> remoteVersions, Side remoteSide) {
        return Config.clientOnlyMode || Tags.VERSION.equals(remoteVersions.get(MODID));
    }
}
