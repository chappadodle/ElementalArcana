package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Radiance's first spell: marks the ground where the caster looks (up to 24 blocks, or the feet of
 * a creature they look at); three quarters of a second later a pillar of light strikes it (see
 * Smites): it hurts everything within 2.5 blocks, half again as much for the undead, sets them
 * alight and makes them glow. Radiance wisps cast it too.
 */
public class SmiteSpell extends Spell {
    private static final double RANGE = 24;
    private static final double AIM_LEEWAY = 0.5;

    public SmiteSpell() {
        super(ModSchools.RADIANCE, 30, 80);
    }

    @Override
    public CastResult cast(CastContext context) {
        LivingEntity aimed = SpellTargets.underCrosshair(context.caster(), RANGE, AIM_LEEWAY, target -> true);
        Vec3 at = aimed != null ? aimed.position() : groundAhead(context.level(), context.caster());
        Smites.strike(context.level(), at, context.caster(), context.power(), target -> SpellTargets.canAffect(context.caster(), target));
        return CastResult.SUCCESS;
    }

    /** Where the caster's look meets a block (or, at the end of range, the ground under that point). */
    private static Vec3 groundAhead(ServerLevel level, LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(RANGE));
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
