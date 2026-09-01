package com.spellcaster.item;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.spellcaster.Spellcaster;
import com.spellcaster.wheel.WheelNbt;
import com.spellcaster.wheel.WheelType;

import lombok.Getter;
import project.studio.manametalmod.api.weapon.IMagicItem;
import project.studio.manametalmod.core.ManaItemType;
import project.studio.manametalmod.magic.magicItem.IMagicEffect;

public abstract class WheelItemBase extends IMagicItem {

    @Getter
    private final WheelType wheelType;

    protected WheelItemBase(String name, WheelType wheelType) {
        super(name);
        this.wheelType = wheelType;
    }

    public String getMODID() {
        return Spellcaster.MODID;
    }

    public int getCapacity(ItemStack stack) {
        int variant = this.getVariant(stack);
        return variant < 0 ? 0 : 7 * (variant + 1);
    }

    public int getNeedLV(ItemStack stack, EntityPlayer player) {
        int variant = this.getVariant(stack);
        return variant < 0 ? Integer.MAX_VALUE : variant == 0 ? 1 : variant == 1 ? 70 : 130;
    }

    private int getVariant(ItemStack stack) {
        if (stack == null) {
            return 0;
        }
        int variant = stack.getItemDamage();
        return variant >= 0 && variant < this.TypeCount() ? variant : -1;
    }

    public ManaItemType getType(ItemStack stack) {
        return ManaItemType.Core;
    }

    public int TypeCount() {
        return 3;
    }

    public List<IMagicEffect> getItemEffect(ItemStack stack) {
        return Collections.emptyList();
    }

    public void onEquipment(ItemStack stack, EntityLivingBase entity) {}

    public void onDisrobe(ItemStack stack, EntityLivingBase entity) {}

    @Deprecated
    public void onBeAttack(ItemStack stack, EntityPlayer player, EntityLivingBase entity, DamageSource source) {}

    @Deprecated
    public void onAttack(ItemStack stack, EntityPlayer player, EntityLivingBase target, DamageSource source) {}

    @Deprecated
    public void onCrit(ItemStack stack, EntityPlayer player, EntityLivingBase entity, DamageSource source) {}

    public long getValue(ItemStack stack) {
        return 0L;
    }

    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            Spellcaster.proxy.openWheelGui(stack);
        }
        return stack;
    }

    public boolean isConfigured(ItemStack stack) {
        for (int skillId : WheelNbt.readSequence(stack, getCapacity(stack))) {
            if (WheelNbt.isSkillIdValid(skillId)) {
                return true;
            }
        }
        return false;
    }
}
