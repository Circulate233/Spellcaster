package com.spellcaster.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import com.spellcaster.Config;
import com.spellcaster.Spellcaster;
import com.spellcaster.network.ActivateWheelMessage;
import com.spellcaster.network.CycleWheelModeMessage;
import com.spellcaster.network.WheelNetwork;
import com.spellcaster.wheel.ManaMetalBridge;
import com.spellcaster.wheel.SkillSelection;
import com.spellcaster.wheel.SkillSequenceSelector;
import com.spellcaster.wheel.WheelMode;

import cpw.mods.fml.client.registry.ClientRegistry;

public final class ClientWheelController {

    private static final String CATEGORY = "key.categories.spellcaster";

    private final KeyBinding activateKey = new KeyBinding("key.spellcaster.activate", Keyboard.KEY_R, CATEGORY);
    private final KeyBinding cycleModeKey = new KeyBinding("key.spellcaster.cycle_mode", Keyboard.KEY_C, CATEGORY);
    private final KeyBinding openConfigKey = new KeyBinding("key.spellcaster.open_config", Keyboard.KEY_O, CATEGORY);

    public ClientWheelController() {
        ClientRegistry.registerKeyBinding(this.activateKey);
        ClientRegistry.registerKeyBinding(this.cycleModeKey);
        ClientRegistry.registerKeyBinding(this.openConfigKey);
    }

    public void onKeyInput() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null) {
            return;
        }

        if (this.activateKey.isPressed()) {
            this.activate(minecraft.thePlayer);
        }
        if (this.cycleModeKey.isPressed()) {
            this.cycleMode(minecraft.thePlayer);
        }
        if (this.openConfigKey.isPressed()) {
            ItemStack stack = Config.clientOnlyMode ? null : ManaMetalBridge.getCoreWheel(minecraft.thePlayer);
            if (Config.clientOnlyMode || stack != null) {
                Spellcaster.proxy.openWheelGui(stack);
            }
        }
    }

    private void activate(EntityPlayer player) {
        if (!Config.clientOnlyMode) {
            WheelNetwork.getInstance()
                .sendToServer(new ActivateWheelMessage());
            return;
        }

        final WheelMode mode = getClientMode();
        final int[] sequence = Config.clientSequence.clone();
        SkillSelection selection = SkillSequenceSelector.select(
            sequence,
            Config.clientCursor,
            mode,
            skillId -> ManaMetalBridge.isSkillAvailable(Minecraft.getMinecraft().thePlayer, skillId),
            player.worldObj.rand);
        if (selection.isSelected()) {
            ManaMetalBridge.sendSkillClient(selection.getSkillId());
            Config.clientCursor = selection.getNextCursor();
            Config.saveClientState();
        } else {
            showStatus("spellcaster.status.no_available_skill");
        }
    }

    private void cycleMode(EntityPlayer player) {
        if (!Config.clientOnlyMode) {
            WheelNetwork.getInstance()
                .sendToServer(new CycleWheelModeMessage());
            return;
        }

        Config.clientMode = getClientMode().nextClientMode()
            .ordinal();
        Config.saveClientState();
        showStatus("spellcaster.status.mode_changed");
    }

    private WheelMode getClientMode() {
        if (Config.clientMode < 0 || Config.clientMode >= WheelMode.values().length) {
            return WheelMode.FATE_NORMAL;
        }
        return WheelMode.values()[Config.clientMode];
    }

    private void showStatus(String key) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer != null) {
            minecraft.thePlayer
                .addChatMessage(new ChatComponentText("[Spellcaster] " + StatCollector.translateToLocal(key)));
        }
    }
}
