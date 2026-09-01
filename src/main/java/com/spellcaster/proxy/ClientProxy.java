package com.spellcaster.proxy;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

import com.spellcaster.client.ClientWheelController;
import com.spellcaster.client.GuiSpellWheel;
import com.spellcaster.item.WheelItemBase;
import com.spellcaster.network.SyncWheelStateMessage;
import com.spellcaster.network.WheelStatusMessage;
import com.spellcaster.wheel.ManaMetalBridge;
import com.spellcaster.wheel.WheelMode;
import com.spellcaster.wheel.WheelNbt;
import com.spellcaster.wheel.WheelType;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import project.studio.manametalmod.event.EventGUI;

public class ClientProxy extends CommonProxy {

    private final EventGUI eventGUI = new EventGUI();
    private final InputEvent.KeyInputEvent event = new InputEvent.KeyInputEvent();
    private ClientWheelController wheelController;

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        this.wheelController = new ClientWheelController();
    }

    @SubscribeEvent
    public void onWheelKeyInput(InputEvent.KeyInputEvent event) {
        if (this.wheelController != null) {
            this.wheelController.onKeyInput();
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.MouseInputEvent event) {
        eventGUI.onKeyInput(this.event);
        if (this.wheelController != null) {
            this.wheelController.onKeyInput();
        }
    }

    @Override
    public void openWheelGui(ItemStack stack) {
        Minecraft.getMinecraft()
            .displayGuiScreen(new GuiSpellWheel(stack));
    }

    @Override
    public void handleWheelState(final SyncWheelStateMessage message) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                ItemStack stack = message.isHeldItem() ? Minecraft.getMinecraft().thePlayer.getCurrentEquippedItem()
                    : ManaMetalBridge.getCoreWheel(Minecraft.getMinecraft().thePlayer);
                if (stack == null || !(stack.getItem() instanceof WheelItemBase item)) {
                    return;
                }
                int capacity = item.getCapacity(stack);
                if (capacity != message.getCapacity()) {
                    return;
                }
                WheelMode mode = item.getWheelType() == WheelType.CHAOS ? WheelMode.CHAOS : message.getMode();
                WheelNbt.write(stack, Arrays.copyOf(message.getSequence(), capacity), message.getCursor(), mode);
            });
    }

    @Override
    public void handleWheelStatus(final int status) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                String key = switch (status) {
                    case WheelStatusMessage.MODE_CHANGED -> "spellcaster.status.mode_changed";
                    case WheelStatusMessage.CONFIG_SAVED -> "spellcaster.status.config_saved";
                    case WheelStatusMessage.NO_WHEEL -> "spellcaster.status.no_wheel";
                    case WheelStatusMessage.NO_AVAILABLE_SKILL -> "spellcaster.status.no_available_skill";
                    case WheelStatusMessage.MODE_UNAVAILABLE -> "spellcaster.status.mode_unavailable";
                    default -> "spellcaster.status.invalid_config";
                };
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer
                        .addChatMessage(new ChatComponentText("[Spellcaster] " + StatCollector.translateToLocal(key)));
                }
            });
    }

}
