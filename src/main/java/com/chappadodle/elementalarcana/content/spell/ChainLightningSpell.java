package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.ChainLightningRules;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Lightning's first spell: a bolt leaps from the caster's hand to the creature under the crosshair,
 * then on from creature to creature, each jump a little weaker; a wet creature conducts it (harder,
 * and further). No target: the cast fails and costs nothing. It levels to 10
 * (ChainLightningRules has the numbers; ChainLightnings does the work):
 * <pre>
 * Lv1 Chain Lightning  3 jumps                      Lv6  Conductor      wet: double, 3 more jumps
 * Lv2 Long Arc         longer jumps and reach       Lv7  Thunderstruck  a bolt from the sky first
 * Lv3 Static           each strike stuns            Lv8  Live Wire      5 jumps; struck foes arc
 * Lv4 Forked Chain     4 jumps, gentler falloff     Lv9  Supercharge    no falloff
 * Lv5 Storm Fork | Overload                         Lv10 Ball Lightning | Thunder Lord
 * </pre>
 * Lightning wisps, Attuned creatures, Thunderclap and the Wind Sovereign use the Lv 1 chain
 * ({@link #chain}) and its bolt ({@link #bolt}).
 */
public class ChainLightningSpell extends Spell {
    private static final double AIM_LEEWAY = 0.6;
    /** The caster's own bolt is drawn from this far past the hand, so nothing balloons in front of their eyes. */
    static final double FIRST_PERSON_SKIP = 1.0;

    public ChainLightningSpell() {
        super(ModSchools.LIGHTNING, 28, 70);
    }

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(ChainLightningRules.STORM_FORK, ChainLightningRules.OVERLOAD);
            case 10 -> List.of(ChainLightningRules.BALL_LIGHTNING, ChainLightningRules.THUNDER_LORD);
            default -> List.of();
        };
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        int level = context.spellLevel();
        String fork = level >= 5 ? context.branch(5) : null;
        String capstone = level >= 10 ? context.branch(10) : null;
        boolean ball = ChainLightningRules.BALL_LIGHTNING.equals(capstone);
        LivingEntity first = aim(caster, level);
        if (first == null && !ball) {
            return CastResult.fail(Component.translatable("message.elementalarcana.chain_lightning.no_target"));
        }
        ChainLightnings.Chain chain = ChainLightnings.Chain.of(level, fork);
        Predicate<LivingEntity> affects = target -> SpellTargets.canAffect(caster, target);
        if (first != null) {
            ChainLightnings.chain(context.level(), caster, hand(caster), FIRST_PERSON_SKIP, first, context.power(), chain, affects);
        }
        if (ball) {
            ChainLightnings.loose(context.level(), caster, hand(caster), caster.getLookAngle(), context.power(), chain, affects);
        }
        if (ChainLightningRules.THUNDER_LORD.equals(capstone)) {
            context.holdUntilRelease(ChainLightnings.lord(context, chain, affects));
        }
        ChainLightnings.thunder(context.level(), caster);
        return CastResult.SUCCESS;
    }

    /** The creature under the caster's crosshair within the first strike's reach (anyone, like a projectile), or null. */
    @Nullable
    static LivingEntity aim(LivingEntity caster, int level) {
        return SpellTargets.underCrosshair(caster, ChainLightningRules.range(level), AIM_LEEWAY,
                target -> SpellTargets.canAffect(caster, target) || target instanceof Player);
    }

    /** Where the bolt leaves a caster: a little in front of the right hand. */
    public static Vec3 hand(LivingEntity caster) {
        float yaw = caster.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        return caster.getEyePosition().add(caster.getLookAngle().scale(0.6)).add(right.scale(0.3)).add(0, -0.3, 0);
    }

    /**
     * The Lv 1 chain, for other casters: from {@code from} to {@code first} (drawn from {@code skip}
     * blocks along), then from creature to creature that {@code jumpsTo} allows.
     */
    public static void chain(ServerLevel level, LivingEntity caster, Vec3 from, double skip, LivingEntity first, float power,
                             Predicate<LivingEntity> jumpsTo) {
        ChainLightnings.chain(level, caster, from, skip, first, power, ChainLightnings.Chain.BASE, jumpsTo);
        ChainLightnings.thunder(level, caster);
    }

    /** A bolt from {@code a} to {@code b}, none of the first {@code skip} blocks drawn. */
    public static void bolt(ServerLevel level, Vec3 a, Vec3 b, double skip) {
        ChainLightnings.bolt(level, a, b, skip, 1f);
    }
}
