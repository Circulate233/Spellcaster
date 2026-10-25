package com.spellcaster.wheel;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import project.studio.manametalmod.MMM;
import project.studio.manametalmod.card.UseWeaponResult;
import project.studio.manametalmod.config.M3Config;
import project.studio.manametalmod.core.BlockPos;
import project.studio.manametalmod.core.CareerCore;
import project.studio.manametalmod.core.Pos;
import project.studio.manametalmod.core.SpellItemType;
import project.studio.manametalmod.core.TargetHelp;
import project.studio.manametalmod.entity.nbt.ManaMetalModRoot;
import project.studio.manametalmod.entity.nbt.NbtCareer;
import project.studio.manametalmod.itemAndBlockCraft.ItemCraft10;
import project.studio.manametalmod.items.ItemToolHammer;
import project.studio.manametalmod.manor.ManorPermission;
import project.studio.manametalmod.manor.ManorPermissionType;
import project.studio.manametalmod.mob.EntityMagicObject;
import project.studio.manametalmod.mob.EntitySoulPhantom;
import project.studio.manametalmod.mob.EntityWolfSnowSummon;
import project.studio.manametalmod.optool.OpToolCore;
import project.studio.manametalmod.potion.PotionEffectM3;
import project.studio.manametalmod.potion.PotionM3;
import project.studio.manametalmod.produce.textile.ItemToolMagicBag;
import project.studio.manametalmod.spell.CareerSpell;
import project.studio.manametalmod.spell.EntitySummonTotem;
import project.studio.manametalmod.spell.EventSpell;
import project.studio.manametalmod.spell.Spell;
import project.studio.manametalmod.spell.SpellData;
import project.studio.manametalmod.spell.SpellID;

/** Read-only selection checks matching M3's MessageSkill / EventSpell entry points. */
final class ManaMetalSkillRules {

    private ManaMetalSkillRules() {}

    static Spell getSpell(CareerCore career, int skillId) {
        if (career == null || !WheelNbt.isSkillIdValid(skillId) || SpellData.spell_array == null) {
            return null;
        }
        CareerSpell spells = SpellData.spell_array.get(career);
        if (spells == null) {
            return null;
        }
        int tier = skillId / 100;
        SpellID[] ids = tier == 1 ? spells.LV1 : tier == 2 ? spells.LV2 : spells.LV3;
        int slot = skillId % 100;
        return ids == null || slot >= ids.length ? null : SpellData.getData(career, ids[slot]);
    }

    static boolean isAvailable(EntityPlayer player, ManaMetalModRoot root, int skillId) {
        if (player.worldObj == null || root.mana == null
            || MMM.getDimensionID(player.worldObj) == M3Config.WorldFutureID) {
            return false;
        }
        NbtCareer nbt = root.carrer;
        CareerCore career = CareerCore.getPlayerCarrer(root);
        Spell spell = getSpell(career, skillId);
        boolean unlimited = hasUnlimitedCooldown(player);
        int cooldown = unlimited ? 0 : ManaMetalBridge.getCooldown(nbt, skillId);
        boolean assassinFollowup = career == CareerCore.Assassin && skillId == 203
            && PotionEffectM3.isPotion(root, PotionM3.potionDagger3)
            && !PotionEffectM3.isPotion(root, PotionM3.potionDagger4);
        if (spell == null || !isReady(career, nbt, skillId, cooldown, player.isSneaking(), assassinFollowup)) {
            return false;
        }
        if (!CareerCore.testPlayerWeapon(player, career) || MMM.playerCanUseWeapon(player, root) == UseWeaponResult.fail
            || !ManorPermission.playerHasManorPermission(player, ManorPermissionType.CAST_SKILL)) {
            return false;
        }
        if (PotionEffectM3.isPotion(root, PotionM3.potionSilence) && !canCastSilenced(career, nbt, skillId)) {
            return false;
        }
        ItemStack held = player.getCurrentEquippedItem();
        if (career == CareerCore.Knight && player.isUsingItem() && held != null
            && held.getItem() instanceof ItemToolHammer) {
            return false;
        }
        // M3's Ranger 201 loading branch charges the Lightning Arrow cost.
        Spell cost = career == CareerCore.Ranger && skillId == 201 ? getSpell(career, 104) : spell;
        if (needsMana(career, nbt, skillId, player.isSneaking())
            && (cost == null || (unlimited ? root.mana.getMagicMax() : root.mana.getMana()) < cost.needMagic)) {
            return false;
        }
        return hasSpellItem(player, spell.needItem) && meetsCareerConditions(player, root, career, skillId, spell);
    }

    static boolean isReady(CareerCore career, NbtCareer nbt, int skillId, int cooldown, boolean sneaking,
        boolean assassinFollowup) {
        if (career == CareerCore.Hunter && isJavelin(skillId)) {
            return nbt.getCareerUse1() == 0 && cooldown <= hunterCooldownLimit(skillId);
        }
        if (career == CareerCore.BlowingArrows && skillId == 201 && sneaking) {
            return true;
        }
        if (career == CareerCore.Assassin && skillId == 203 && cooldown > 0) {
            return assassinFollowup;
        }
        if (career == CareerCore.Samurai && skillId == 303 && nbt.careerUseLV3_1 > 0) {
            return nbt.careerUseLV3_2 <= 0;
        }
        return cooldown <= 0 && (career != CareerCore.Ranger || canLoadArrow(skillId, nbt.getCareerUse2()));
    }

    static boolean isJavelin(int skillId) {
        return skillId == 102 || skillId == 103 || skillId == 201 || skillId == 202 || skillId == 206;
    }

    private static int hunterCooldownLimit(int skillId) {
        // These are M3's live reserve thresholds, not seconds or a separate ammunition count.
        return switch (skillId) {
            case 102 -> EventSpell.A102_SilverGun;
            case 103 -> EventSpell.A103_BurstJavelin;
            case 201 -> EventSpell.A201_SpiralJavelin;
            case 202 -> EventSpell.A202_ThornSpear;
            case 206 -> EventSpell.B206_BoundaryBreaker;
            default -> 0;
        };
    }

    private static boolean canLoadArrow(int skillId, int loaded) {
        return switch (skillId) {
            case 102, 206 -> loaded == 0;
            case 103, 104, 202 -> loaded == 0 || loaded == 201;
            case 201 -> loaded == 0 || loaded == 103 || loaded == 104 || loaded == 202;
            default -> true;
        };
    }

    static boolean needsMana(CareerCore career, NbtCareer nbt, int skillId, boolean sneaking) {
        return !(career == CareerCore.BlowingArrows && skillId == 201 && sneaking)
            && !(career == CareerCore.Samurai && skillId == 303 && nbt.careerUseLV3_1 > 0);
    }

    private static boolean canCastSilenced(CareerCore career, NbtCareer nbt, int skillId) {
        if (career == CareerCore.Priest && skillId == 103 || career == CareerCore.Knight && skillId == 102) {
            return ManaMetalBridge.getCooldown(nbt, skillId) <= 0;
        }
        // Match M3's LV1[6] check for this exception even though the skill is in tier 2.
        if (career == CareerCore.Priest && skillId == 206) {
            return nbt.getSpellCD_LV1()[6] <= 0 && nbt.getSpellLV_1()[6] > 0;
        }
        return career == CareerCore.Hunter && isJavelin(skillId) && nbt.getSpellLV_2()[0] > 0;
    }

    private static boolean meetsCareerConditions(EntityPlayer player, ManaMetalModRoot root, CareerCore career,
        int skillId, Spell spell) {
        NbtCareer nbt = root.carrer;
        switch (career) {
            case Hunter:
                return skillId != 301 || nbt.career_resource[0] >= 10;
            case Samurai:
                if (skillId == 303 && nbt.careerUseLV3_1 > 0) {
                    return hasFlurryDestination(player);
                }
                if (skillId != 303 && skillId != 306 && nbt.career_resource[0] <= 0) {
                    return false;
                }
                if (skillId == 103) {
                    if (nbt.career_resource[1] == 0) {
                        return player.onGround;
                    }
                    Entity target = TargetHelp.getTargetEntityLife(player.worldObj, player, spell.aoe_range);
                    return target instanceof EntityLivingBase
                        && target instanceof IMob;
                }
                if (skillId == 104 && nbt.career_resource[1] == 0 || skillId == 203) {
                    return !player.onGround;
                }
                return skillId != 202 || nbt.career_resource[1] == 0 || player.onGround;
            case Wizard:
                return (skillId != 201 || !PotionEffectM3.isPotion(root, PotionM3.potionAnnihilationBeam))
                    && (skillId != 301 || !PotionEffectM3.isPotion(root, PotionM3.potionWand1));
            case Ranger:
                return skillId != 302 || getGroundTarget(player) != null;
            case BlowingArrows:
                if (skillId == 201 && player.isSneaking() || skillId == 301) {
                    return getGroundTarget(player) != null && findTotem(player) != null;
                }
                return skillId != 303 || hasHostileTarget(player, spell);
            case Curseman:
                if (skillId == 301 || skillId == 306) {
                    return getGroundTarget(player) != null;
                }
                return skillId != 302 || hasHostileTarget(player, spell);
            case Priest:
                if (skillId == 301
                    && TargetHelp.getTargetEntityLife(player.worldObj, player, spell.aoe_range) == null) {
                    for (EntityLivingBase mob : MMM.findMobs(player, spell.aoe_range)) {
                        if (player.canEntityBeSeen(mob)) {
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            case BeastTrainer:
                if (skillId == 206) {
                    int x = (int) player.posX;
                    int y = (int) player.posY;
                    int z = (int) player.posZ;
                    for (EntityWolfSnowSummon wolf : player.worldObj.getEntitiesWithinAABB(
                        EntityWolfSnowSummon.class,
                        AxisAlignedBB.getBoundingBox(x - 24, y - 24, z - 24, x + 24, y + 24, z + 24))) {
                        if (wolf.getOwner() == player) {
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            default:
                return true;
        }
    }

    private static boolean hasHostileTarget(EntityPlayer player, Spell spell) {
        Entity target = TargetHelp.getTargetEntityLife(player.worldObj, player, spell.aoe_range);
        return target instanceof IMob && !(target instanceof EntitySoulPhantom);
    }

    private static boolean hasFlurryDestination(EntityPlayer player) {
        // Mirror teleportForwardNoClipWall's destination check without moving the player during selection.
        World world = player.worldObj;
        float yaw = (float) Math.toRadians(player.rotationYaw);
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        Vec3 from = Vec3.createVectorHelper(player.posX, player.posY + player.height * 0.5, player.posZ);
        Vec3 to = Vec3.createVectorHelper(player.posX + dx * 12.0, from.yCoord, player.posZ + dz * 12.0);
        MovingObjectPosition hit = world.func_147447_a(from, to, false, true, false);
        int x = MathHelper.floor_double(hit != null && hit.hitVec != null ? hit.hitVec.xCoord - dx * 0.35 : to.xCoord);
        int z = MathHelper.floor_double(hit != null && hit.hitVec != null ? hit.hitVec.zCoord - dz * 0.35 : to.zCoord);
        int baseY = MathHelper.floor_double(player.posY);
        for (int offset = -4; offset <= 4; offset++) {
            int y = baseY + offset;
            if (world.getBlock(x, y - 1, z).getMaterial().blocksMovement()
                && !world.getBlock(x, y, z).getMaterial().blocksMovement()
                && !world.getBlock(x, y + 1, z).getMaterial().blocksMovement()) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos getGroundTarget(EntityPlayer player) {
        BlockPos target;
        if (player.worldObj.isRemote) {
            // M3's helper deliberately returns null on the client; predict the same ray locally.
            Vec3 from = Vec3.createVectorHelper(
                player.posX,
                player.posY + player.getEyeHeight() - player.yOffset,
                player.posZ);
            Vec3 look = player.getLook(1.0F);
            MovingObjectPosition hit = player.worldObj.rayTraceBlocks(
                from, from.addVector(look.xCoord * 32.0, look.yCoord * 32.0, look.zCoord * 32.0), false);
            target = hit == null ? null : new BlockPos(
                player.worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ), new Pos(hit),
                player.worldObj.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ));
        } else {
            target = TargetHelp.getBlockAimingAt(player, player.worldObj, 32.0);
        }
        if (target == null) {
            return null;
        }
        for (int height = 1; height <= 3; height++) {
            if (target.pos.getPosBlock(player.worldObj, 0, height, 0) != Blocks.air) {
                return null;
            }
        }
        return target;
    }

    private static EntitySummonTotem findTotem(EntityPlayer player) {
        for (EntityLivingBase entity : MMM.findEntityLivingBase(
            player.worldObj,
            player.posX,
            player.posY,
            player.posZ,
            64)) {
            if (entity instanceof EntitySummonTotem && ((EntitySummonTotem) entity).getOwner() == player) {
                return (EntitySummonTotem) entity;
            }
        }
        return null;
    }

    private static EntityMagicObject findCharm(EntityPlayer player) {
        for (Entity entity : MMM.findEntity(player, 64)) {
            if (entity instanceof EntityMagicObject charm) {
                if (charm.type == 49 && charm.player == player && charm.time > 39) {
                    return charm;
                }
            }
        }
        return null;
    }

    private static boolean hasUnlimitedCooldown(EntityPlayer player) {
        return player.inventory.hasItem(OpToolCore.ItemUnlimitedCD);
    }

    private static boolean hasSpellItem(EntityPlayer player, SpellItemType type) {
        if (type == null) {
            return true;
        }
        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            if (isSpellItem(stack, type)) {
                return true;
            }
            if (stack != null && stack.getItem() instanceof ItemToolMagicBag && stack.hasTagCompound()) {
                NBTTagList items = stack.getTagCompound().getTagList("Items", 10);
                for (int index = 0; index < items.tagCount(); index++) {
                    if (isSpellItem(ItemStack.loadItemStackFromNBT(items.getCompoundTagAt(index)), type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isSpellItem(ItemStack stack, SpellItemType type) {
        return stack != null && stack.stackSize > 0 && stack.getItem() == ItemCraft10.ItemSpell
            && stack.getItemDamage() == type.ordinal();
    }

    /** M3 returns void: observe the skill's own commit state before armor callbacks run. */
    static final class CastState {

        private final SkillProgress progress;
        private final EntitySummonTotem totem;
        private final BlockPos totemTarget;
        private final EntityMagicObject charm;

        CastState(EntityPlayer player, ManaMetalModRoot root, int skillId) {
            CareerCore career = CareerCore.getPlayerCarrer(root);
            boolean unlimited = hasUnlimitedCooldown(player);
            this.progress = new SkillProgress(
                career, root.carrer, skillId, unlimited ? root.mana.getMagicMax() : root.mana.getMana(),
                hasDaggerFollowup(career, skillId, root), unlimited);
            boolean moveTotem = career == CareerCore.BlowingArrows && skillId == 201 && player.isSneaking();
            this.totem = moveTotem ? findTotem(player) : null;
            this.totemTarget = moveTotem ? getGroundTarget(player) : null;
            this.charm = career == CareerCore.Curseman && skillId == 201 ? findCharm(player) : null;
        }

        boolean didCast(ManaMetalModRoot root) {
            return progress.didCast(
                root.carrer,
                root.mana.getMana(),
                hasDaggerFollowup(progress.career, progress.skillId, root))
                || charm != null && charm.isDead
                || totem != null && totemTarget != null && !totem.isDead
                    && totem.posX == totemTarget.pos.X + 0.5F && totem.posY == totemTarget.pos.Y + 1.0F
                    && totem.posZ == totemTarget.pos.Z + 0.5F;
        }

        private static boolean hasDaggerFollowup(CareerCore career, int skillId, ManaMetalModRoot root) {
            return career == CareerCore.Assassin && skillId == 203
                && PotionEffectM3.isPotion(root, PotionM3.potionDagger4);
        }
    }

    /** Snapshots values, never the mutable NBT arrays, so delayed-cooldown skills can advance the wheel. */
    static final class SkillProgress {

        private final CareerCore career;
        private final int skillId;
        private final int cooldown;
        private final int loadedJavelin;
        private final int loadedArrow;
        private final int secondaryArrow;
        private final int flurryCharges;
        private final int flurryDelay;
        private final boolean daggerFollowup;
        private final float mana;

        SkillProgress(CareerCore career, NbtCareer nbt, int skillId, float mana, boolean daggerFollowup,
            boolean unlimited) {
            this.career = career;
            this.skillId = skillId;
            this.cooldown = unlimited ? 0 : ManaMetalBridge.getCooldown(nbt, skillId);
            this.loadedJavelin = nbt.getCareerUse1();
            this.loadedArrow = nbt.getCareerUse2();
            this.secondaryArrow = nbt.careerUseLV2_1;
            this.flurryCharges = nbt.careerUseLV3_1;
            this.flurryDelay = nbt.careerUseLV3_2;
            this.daggerFollowup = daggerFollowup;
            this.mana = mana;
        }

        boolean didCast(NbtCareer nbt, float remainingMana, boolean hasDaggerFollowup) {
            if (ManaMetalBridge.getCooldown(nbt, skillId) > cooldown) {
                return true;
            }
            return switch (career) {
                case Hunter -> isJavelin(skillId) && nbt.getCareerUse1() != loadedJavelin;
                case Ranger ->
                    // During multi-arrow loading M3 can accept the same secondary arrow again.
                    (isJavelin(skillId) || skillId == 104)
                        && (nbt.getCareerUse2() != loadedArrow || nbt.careerUseLV2_1 != secondaryArrow
                        || remainingMana < mana);
                case Assassin ->
                    skillId == 203 && !daggerFollowup && hasDaggerFollowup;
                case Samurai -> skillId == 303
                    && (nbt.careerUseLV3_1 < flurryCharges || nbt.careerUseLV3_2 > flurryDelay);
                case Curseman -> skillId == 202 && (nbt.careerUseLV2_1 != secondaryArrow || remainingMana < mana);
                default -> false;
            };
        }
    }
}
