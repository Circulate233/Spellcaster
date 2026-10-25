package com.spellcaster.wheel;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.spellcaster.api.WheelCoreApi;
import com.spellcaster.api.WheelCoreProvider;

import project.studio.manametalmod.MMM;
import project.studio.manametalmod.core.CareerCore;
import project.studio.manametalmod.entity.nbt.ManaMetalModRoot;
import project.studio.manametalmod.entity.nbt.NbtCareer;
import project.studio.manametalmod.items.armor.ArmorFXCore;
import project.studio.manametalmod.network.MessageSkill;
import project.studio.manametalmod.network.PacketHandlerMana;
import project.studio.manametalmod.spell.EventSpell;

public final class ManaMetalBridge {

    public static final int CORE_SLOT = 36;

    private ManaMetalBridge() {}

    public static ManaMetalModRoot getRoot(EntityPlayer player) {
        return player == null ? null : MMM.getEntityNBT(player);
    }

    public static ItemStack getCoreWheel(EntityPlayer player) {
        ManaMetalModRoot root = getRoot(player);
        if (root == null || root.item == null) {
            return null;
        }
        ItemStack stack = root.item.getStackInSlot(CORE_SLOT);
        return WheelCoreApi.getProvider(stack) == null ? null : stack;
    }

    public static boolean canUseWheel(EntityPlayer player, ItemStack stack) {
        ManaMetalModRoot root = getRoot(player);
        WheelCoreProvider provider = WheelCoreApi.getProvider(stack);
        return root != null && root.carrer != null && provider != null
            && WheelCoreApi.isEnabled(stack, player)
            && root.carrer.getLv() >= provider.getNeedLV(stack, player);
    }

    public static int getCooldown(NbtCareer career, int skillId) {
        if (career == null || !WheelNbt.isSkillIdValid(skillId)) {
            return Integer.MAX_VALUE;
        }
        int tier = skillId / 100;
        int slot = skillId % 100;
        int[] cooldowns = tier == 1 ? career.getSpellCD_LV1()
            : tier == 2 ? career.getSpellCD_LV2() : tier == 3 ? career.getSpellCD_LV3() : null;
        return cooldowns == null || slot < 0 || slot >= cooldowns.length ? Integer.MAX_VALUE : cooldowns[slot];
    }

    public static boolean isSkillAvailable(final EntityPlayer player, final int skillId) {
        final ManaMetalModRoot root = getRoot(player);
        if (root == null || root.carrer == null || !isSkillConfigurable(player, skillId)) {
            return false;
        }
        return ManaMetalSkillRules.isAvailable(player, root, skillId);
    }

    public static boolean isSkillConfigurable(EntityPlayer player, int skillId) {
        ManaMetalModRoot root = getRoot(player);
        if (root == null || root.carrer == null || !WheelNbt.isSkillIdValid(skillId)) {
            return false;
        }
        int tier = skillId / 100;
        int slot = skillId % 100;
        boolean configurable = tier == 1 ? slot >= 2 && slot <= 4 : (slot >= 1 && slot <= 3) || slot == 6;
        int[] levels = tier == 1 ? root.carrer.getSpellLV_1()
            : tier == 2 ? root.carrer.getSpellLV_2() : root.carrer.getSpellLV_3();
        return configurable && levels != null && slot < levels.length && levels[slot] > 0
            && ManaMetalSkillRules.getSpell(CareerCore.getPlayerCarrer(root), skillId) != null;
    }

    public static SkillSelection select(EntityPlayer player, int[] sequence, int cursor, WheelMode mode) {
        return SkillSequenceSelector.select(
            sequence,
            cursor,
            mode,
            skillId -> isSkillAvailable(player, skillId),
            player == null || player.worldObj == null ? null : player.worldObj.rand);
    }

    public static void sendSkillClient(int skillId) {
        PacketHandlerMana.INSTANCE.sendToServer(new MessageSkill(skillId));
    }

    public static boolean castServer(EntityPlayerMP player, int skillId) {
        ManaMetalModRoot root = getRoot(player);
        if (root != null && root.carrer != null && isSkillAvailable(player, skillId)) {
            ManaMetalSkillRules.CastState before = new ManaMetalSkillRules.CastState(player, root, skillId);
            EventSpell.doSkill_message(
                skillId,
                CareerCore.getPlayerCarrer(root),
                root.carrer,
                player,
                player.worldObj,
                true,
                false,
                root);
            boolean cast = before.didCast(root);
            if (cast) {
                ArmorFXCore.skillEvent(player, root);
            }
            root.carrer.send2();
            return cast;
        }
        return false;
    }
}
