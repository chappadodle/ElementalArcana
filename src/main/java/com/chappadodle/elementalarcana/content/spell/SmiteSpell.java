package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.SmiteRules;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Radiance's first spell: marks the ground where the caster looks (up to 24 blocks, or the feet of
 * a creature they look at); a moment later a pillar of light strikes it (see Smites): it hurts
 * everything within reach, the undead most, sets them alight and makes them glow. It levels to 10
 * (SmiteRules has the numbers):
 * <pre>
 * Lv1 Smite            2.5 blocks, after 0.75 s      Lv6  Purge         strips foes' boons, players' banes
 * Lv2 Wide Judgment    3.5 blocks                    Lv7  Holy Fire     undead take double; 6 s alight
 * Lv3 Swift Verdict    after 0.5 s                   Lv8  Halo          Regeneration for the caster
 * Lv4 Consecration     holy ground for 4 s           Lv9  Radiant Burst throws back and blinds
 * Lv5 Sunlance | Triple Judgment                     Lv10 Wrath of Heaven | Avatar of Light
 * </pre>
 * Radiance wisps and Attuned creatures cast the Lv 1 Smite (Smites#strike).
 */
public class SmiteSpell extends Spell {
    private static final double AIM_LEEWAY = 0.5;

    public SmiteSpell() {
        super(ModSchools.RADIANCE, 30, 80);
    }

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(SmiteRules.SUNLANCE, SmiteRules.TRIPLE);
            case 10 -> List.of(SmiteRules.WRATH, SmiteRules.AVATAR);
            default -> List.of();
        };
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        int spellLevel = context.spellLevel();
        String fork = spellLevel >= 5 ? context.branch(5) : null;
        String capstone = spellLevel >= 10 ? context.branch(10) : null;
        float power = context.power();
        Smites.Judgment judgment = Smites.Judgment.of(spellLevel);
        Predicate<LivingEntity> affects = target -> SpellTargets.canAffect(caster, target);
        LivingEntity aimed = SpellTargets.underCrosshair(caster, SmiteRules.RANGE, AIM_LEEWAY, target -> true);
        Vec3 at = aimed != null ? aimed.position() : groundAhead(level, caster);
        Consumer<Vec3> onFall = null;
        if (SmiteRules.SUNLANCE.equals(fork)) {
            onFall = foot -> Smites.lance(level, caster, foot, power, judgment, affects);
        }
        if (SmiteRules.WRATH.equals(capstone)) {
            Consumer<Vec3> wrath = foot -> Smites.wrath(level, caster, foot, power, judgment, affects);
            onFall = onFall == null ? wrath : onFall.andThen(wrath);
        }
        int delay = SmiteRules.delay(spellLevel);
        if (SmiteRules.TRIPLE.equals(fork)) {
            // Three pillars along the line of sight, nearest first, the middle one on the mark.
            Vec3 look = caster.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            flat = flat.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, caster.getYRot()) : flat.normalize();
            for (int i = -1; i <= 1; i++) {
                Vec3 spot = i == 0 ? at : Smites.surface(level, at.add(flat.scale(SmiteRules.TRIPLE_SPACING * i)));
                if (spot != null) {
                    Smites.strike(level, spot, caster, power, judgment.narrowed(SmiteRules.TRIPLE_RADIUS), delay,
                            (i + 1) * SmiteRules.TRIPLE_GAP_TICKS, affects, i == 0 ? onFall : null);
                }
            }
        } else {
            Smites.strike(level, at, caster, power, judgment, delay, 0, affects, onFall);
        }
        if (SmiteRules.halo(spellLevel)) {
            caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SmiteRules.HALO_TICKS, 0, false, true, true));
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.8f);
        }
        if (SmiteRules.AVATAR.equals(capstone)) {
            Smites.avatar(level, caster, power, judgment, affects);
        }
        return CastResult.SUCCESS;
    }

    /** Where the caster's look meets a block (or, at the end of range, the ground under that point). */
    private static Vec3 groundAhead(ServerLevel level, LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(SmiteRules.RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation();
        }
        BlockPos pos = BlockPos.containing(end);
        for (int down = 0; down < 24 && level.isEmptyBlock(pos.below()); down++) {
            pos = pos.below();
        }
        return Vec3.atBottomCenterOf(pos);
    }
}
