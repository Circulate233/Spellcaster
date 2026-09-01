package com.spellcaster.content;

import net.minecraft.item.Item;

import com.spellcaster.item.ChaosWheelItem;
import com.spellcaster.item.FateWheelItem;

import cpw.mods.fml.common.registry.GameRegistry;

public final class SpellcasterItems {

    public static Item fateWheel;
    public static Item chaosWheel;

    private SpellcasterItems() {}

    public static void register() {
        fateWheel = new FateWheelItem();
        chaosWheel = new ChaosWheelItem();
        GameRegistry.registerItem(fateWheel, "fateWheel");
        GameRegistry.registerItem(chaosWheel, "chaosWheel");
    }
}
