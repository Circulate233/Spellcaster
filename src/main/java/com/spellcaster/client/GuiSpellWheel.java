package com.spellcaster.client;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.spellcaster.Config;
import com.spellcaster.item.WheelItemBase;
import com.spellcaster.network.SaveWheelConfigMessage;
import com.spellcaster.network.WheelNetwork;
import com.spellcaster.wheel.WheelNbt;

import project.studio.manametalmod.MMM;
import project.studio.manametalmod.inventory.ContainerSkill;
import project.studio.manametalmod.spell.CareerSpell;
import project.studio.manametalmod.spell.GuiSkill;
import project.studio.manametalmod.spell.Spell;
import project.studio.manametalmod.spell.SpellData;
import project.studio.manametalmod.spell.SpellID;

public class GuiSpellWheel extends GuiSkill {

    private static final int BUTTON_TIER_1 = 200;
    private static final int BUTTON_TIER_2 = 201;
    private static final int BUTTON_TIER_3 = 202;
    private static final int BUTTON_DONE = 203;

    private final ItemStack wheelStack;
    private final int capacity;
    private final boolean heldItem;

    public GuiSpellWheel(ItemStack stack) {
        super(
            Minecraft.getMinecraft().thePlayer,
            new ContainerSkill(
                Minecraft.getMinecraft().thePlayer.inventory,
                Minecraft.getMinecraft().thePlayer,
                MMM.getEntityNBT(Minecraft.getMinecraft().thePlayer).warehouse),
            1);
        this.wheelStack = stack;
        this.capacity = Config.clientOnlyMode ? 21
            : stack != null && stack.getItem() instanceof WheelItemBase
                ? ((WheelItemBase) stack.getItem()).getCapacity(stack)
                : 0;
        this.heldItem = stack != null && Minecraft.getMinecraft().thePlayer.getCurrentEquippedItem() == stack;
        this.loadSequence(Config.clientOnlyMode ? Config.clientSequence : WheelNbt.readSequence(stack, this.capacity));
    }

    @Override
    public void initGui() {
        super.initGui();
        int posX = (this.width - this.xSize) / 2;
        int posY = (this.height - this.ySize) / 2;
        for (int i = this.buttonList.size() - 1; i >= 0; --i) {
            GuiButton button = this.buttonList.get(i);
            if (button.id >= 100 && button.id <= 103) {
                this.buttonList.remove(i);
            }
        }
        this.buttonList.add(
            new GuiButton(
                BUTTON_TIER_1,
                posX + 7,
                posY + 258,
                45,
                20,
                StatCollector.translateToLocal("spellcaster.gui.tier1")));
        GuiButton tier2 = new GuiButton(
            BUTTON_TIER_2,
            posX + 57,
            posY + 258,
            45,
            20,
            StatCollector.translateToLocal("spellcaster.gui.tier2"));
        tier2.enabled = this.root.carrer.isTransfer2();
        this.buttonList.add(tier2);
        GuiButton tier3 = new GuiButton(
            BUTTON_TIER_3,
            posX + 107,
            posY + 258,
            45,
            20,
            StatCollector.translateToLocal("spellcaster.gui.tier3"));
        tier3.enabled = this.root.carrer.isTransfer3();
        this.buttonList.add(tier3);
        this.buttonList.add(
            new GuiButton(
                BUTTON_DONE,
                posX + 157,
                posY + 258,
                62,
                20,
                StatCollector.translateToLocal("spellcaster.gui.done")));
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.id >= 0 && button.id < 7) {
            super.actionPerformed(button);
        } else if (button.id == BUTTON_TIER_1) {
            this.setSkillTier(1);
        } else if (button.id == BUTTON_TIER_2 && this.root.carrer.isTransfer2()) {
            this.setSkillTier(2);
        } else if (button.id == BUTTON_TIER_3 && this.root.carrer.isTransfer3()) {
            this.setSkillTier(3);
        } else if (button.id == BUTTON_DONE) {
            this.mc.displayGuiScreen(null);
            this.mc.setIngameFocus();
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int[] before = this.getSequence();
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int[] after = this.getSequence();
        if (after.length - this.capacity >= 0)
            System.arraycopy(before, this.capacity, after, this.capacity, after.length - this.capacity);
        this.loadSequence(after);
    }

    @Override
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.drawStringSuper(
            MMM.getTranslateText("Skill.cant.type." + this.CareerID.toString() + "." + this.skillType) + " / "
                + MMM.getTranslateText("Skill.cant.point")
                + this.point,
            13,
            8,
            201,
            16777215);
        for (int slot = 0; slot < 7; ++slot) {
            SpellID spellId = this.spellIDs[slot];
            Spell spell = SpellData.getData(this.CareerID, spellId);
            if (spell != null && spellId != null) {
                this.drawStringSuper(
                    MMM.getTranslateText(spellId.toString()) + " "
                        + MMM.getTranslateText("GuiPotionMake2")
                        + this.spellLV[slot],
                    35,
                    21 + slot * 22,
                    119,
                    16777215);
                this.drawStringSuper(
                    EnumChatFormatting.GRAY + MMM.getTranslateText("GuiSkillSet.s" + this.skillType)
                        + ","
                        + MMM.getTranslateText("spellClass." + spell.spellClass.toString()),
                    35,
                    31 + slot * 22,
                    119,
                    16777215);
            } else {
                this.drawStringSuper(
                    "unknown skill " + MMM.getTranslateText("GuiPotionMake2") + this.spellLV[slot],
                    35,
                    21 + slot * 22,
                    119,
                    16777215);
            }
        }

        String[] ranges = { "1~7", "8~14", "15~21" };
        for (int row = 0; row < ranges.length; ++row) {
            int color = row * 7 < this.capacity ? 16777215 : 5592405;
            this.drawStringSuper(ranges[row], 10, 188 + row * 22, 50, color);
        }
    }

    @Override
    public void drawKeySettingInfo(int mouseX, int mouseY, int skillID, int key, int keytype) {
        int index = (key - 1) * 7 + keytype;
        if (index >= this.capacity) {
            this.drawHoveringText(
                Collections.singletonList(
                    EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal("spellcaster.gui.locked")),
                mouseX,
                mouseY,
                this.fontRendererObj);
            return;
        }
        super.drawKeySettingInfo(mouseX, mouseY, skillID, key, keytype);
    }

    @Override
    public void onGuiClosed() {
        if (this.mc.thePlayer != null) {
            this.inventorySlots.onContainerClosed(this.mc.thePlayer);
        }
        int[] sequence = Arrays.copyOf(this.getSequence(), this.capacity);
        if (Config.clientOnlyMode) {
            Config.clientSequence = Arrays.copyOf(sequence, 21);
            Config.saveClientState();
        } else if (this.wheelStack != null) {
            WheelNetwork.getInstance()
                .sendToServer(new SaveWheelConfigMessage(this.heldItem, sequence));
        }
    }

    private void setSkillTier(int tier) {
        this.skillType = tier;
        CareerSpell careerSpells = SpellData.spell_array.get(this.CareerID);
        if (tier == 1) {
            this.spellLV = this.root.carrer.getSpellLV_1();
            this.spellIDs = careerSpells.LV1;
        } else if (tier == 2) {
            this.spellLV = this.root.carrer.getSpellLV_2();
            this.spellIDs = careerSpells.LV2;
        } else {
            this.spellLV = this.root.carrer.getSpellLV_3();
            this.spellIDs = careerSpells.LV3;
        }
        this.useskill = -1;
    }

    private int[] getSequence() {
        int[] sequence = new int[21];
        System.arraycopy(this.spellKey_1, 0, sequence, 0, 7);
        System.arraycopy(this.spellKey_2, 0, sequence, 7, 7);
        System.arraycopy(this.spellKey_3, 0, sequence, 14, 7);
        return sequence;
    }

    private void loadSequence(int[] sequence) {
        int[] data = new int[21];
        Arrays.fill(data, -1);
        if (sequence != null) {
            System.arraycopy(sequence, 0, data, 0, Math.min(sequence.length, data.length));
        }
        this.spellKey_1 = Arrays.copyOfRange(data, 0, 7);
        this.spellKey_2 = Arrays.copyOfRange(data, 7, 14);
        this.spellKey_3 = Arrays.copyOfRange(data, 14, 21);
    }
}
