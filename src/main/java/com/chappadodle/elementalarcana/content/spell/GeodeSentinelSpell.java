package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crystal's third spell, Geode Sentinel (docs/superpowers/specs/2026-10-05-geode-sentinel-design.md):
 * a crystal sentinel grows where the caster looks (up to 16 blocks, pulled back off a wall) and
 * draws the foes round it onto itself for 12 seconds (see GeodeSentinel).
 */
public class GeodeSentinelSpell extends Spell {
    private static final double RANGE = 16;

    public GeodeSentinelSpell() {
        super(ModSchools.CRYSTAL, 50, 500, 15);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        Vec3 eye = caster.getEyePosition();
        Vec3 reach = eye.add(caster.getLookAngle().scale(RANGE));
        BlockHitResult hit = context.level().clip(new ClipContext(eye, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 at = hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.5))
                : reach;
        GeodeSentinel.grow(context.level(), caster, at, context.power());
        return CastResult.SUCCESS;
    }
}
