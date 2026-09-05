package com.spellcaster.content;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import project.studio.manametalmod.Lapuda.LapudaCore;
import project.studio.manametalmod.ManaMetalMod;
import project.studio.manametalmod.itemAndBlockCraft.ItemCraft2;

public final class SpellcasterRecipes {

    private SpellcasterRecipes() {}

    public static void register() {
        ItemStack plate = new ItemStack(ManaMetalMod.ManaSDoublePlate);
        ItemStack ingot = new ItemStack(ManaMetalMod.ingotRoseGold);
        ItemStack skyPower = new ItemStack(LapudaCore.ingotSkyPower);
        ItemStack emerald = new ItemStack(ItemCraft2.UnlimitedEmerald);

        GameRegistry.addRecipe(
            new ItemStack(SpellcasterItems.fateWheel, 1, 0),
            "APA",
            "PXP",
            "APA",
            'A',
            plate,
            'P',
            ingot,
            'X',
            new ItemStack(ManaMetalMod.Neutron));
        GameRegistry.addRecipe(
            new ItemStack(SpellcasterItems.chaosWheel, 1, 0),
            "APA",
            "PXP",
            "APA",
            'A',
            plate,
            'P',
            ingot,
            'X',
            new ItemStack(ManaMetalMod.DarkMatter));

        GameRegistry.addShapelessRecipe(
            new ItemStack(SpellcasterItems.fateWheel, 1, 1),
            new ItemStack(SpellcasterItems.fateWheel, 1, 0),
            skyPower);
        GameRegistry.addShapelessRecipe(
            new ItemStack(SpellcasterItems.chaosWheel, 1, 1),
            new ItemStack(SpellcasterItems.chaosWheel, 1, 0),
            skyPower);
        GameRegistry.addShapelessRecipe(
            new ItemStack(SpellcasterItems.fateWheel, 1, 2),
            new ItemStack(SpellcasterItems.fateWheel, 1, 1),
            emerald);
        GameRegistry.addShapelessRecipe(
            new ItemStack(SpellcasterItems.chaosWheel, 1, 2),
            new ItemStack(SpellcasterItems.chaosWheel, 1, 1),
            emerald);
    }
}
