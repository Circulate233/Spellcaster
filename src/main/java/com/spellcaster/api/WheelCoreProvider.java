package com.spellcaster.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.spellcaster.wheel.WheelType;

/** Implement on a core-slot item to expose it to Spellcaster's wheel features. */
public interface WheelCoreProvider {

    boolean isEnabled(ItemStack stack, EntityPlayer player);

    WheelType getWheelType(ItemStack stack);

    int getCapacity(ItemStack stack);

    int getNeedLV(ItemStack stack, EntityPlayer player);
}
