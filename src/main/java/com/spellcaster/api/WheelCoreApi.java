package com.spellcaster.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.spellcaster.wheel.WheelType;

/** Shared validation and lookup for built-in and third-party wheel core items. */
public final class WheelCoreApi {

    public static final int MAX_CAPACITY = 21;

    private WheelCoreApi() {}

    public static boolean isEnabled(ItemStack stack, EntityPlayer player) {
        WheelCoreProvider provider = getProvider(stack);
        return provider != null && provider.isEnabled(stack, player);
    }

    public static WheelCoreProvider getProvider(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof WheelCoreProvider provider)) {
            return null;
        }
        WheelType type = provider.getWheelType(stack);
        int capacity = provider.getCapacity(stack);
        return type != null && capacity > 0 && capacity <= MAX_CAPACITY ? provider : null;
    }

    public static int getCapacity(ItemStack stack) {
        WheelCoreProvider provider = getProvider(stack);
        return provider == null ? 0 : provider.getCapacity(stack);
    }

    public static WheelType getWheelType(ItemStack stack) {
        WheelCoreProvider provider = getProvider(stack);
        return provider == null ? null : provider.getWheelType(stack);
    }
}
