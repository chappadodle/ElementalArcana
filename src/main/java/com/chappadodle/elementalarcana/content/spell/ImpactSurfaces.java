package com.chappadodle.elementalarcana.content.spell;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Finding the surface a spell hit, for effects that sit on it (rings, marks, debris). An impact
 * point lies exactly on a block's face, so some rays start inside that block; vanilla reports those
 * as instant hits facing the wrong way (it would take a ceiling for a floor), so they're ignored.
 */
public final class ImpactSurfaces {
    private ImpactSurfaces() {
    }

    /** The ground right below an impact (within 1.5 blocks), or null in mid-air. */
    @Nullable
    public static BlockHitResult groundBelow(Level level, Vec3 at) {
        BlockHitResult hit = level.clip(new ClipContext(at.add(0, 0.3, 0), at.subtract(0, 1.5, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.BLOCK && !hit.isInside() && hit.getDirection() == Direction.UP ? hit : null;
    }

    /** The closest block face within reach of an impact that isn't over ground (a wall, a ceiling), or null. */
    @Nullable
    static BlockHitResult surfaceNear(Level level, Vec3 at) {
        BlockHitResult best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            Vec3 toward = Vec3.atLowerCornerOf(direction.getNormal());
            BlockHitResult hit = level.clip(new ClipContext(at.subtract(toward.scale(0.2)), at.add(toward.scale(0.8)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.BLOCK && !hit.isInside() && hit.getLocation().distanceToSqr(at) < bestDistance) {
                bestDistance = hit.getLocation().distanceToSqr(at);
                best = hit;
            }
        }
        return best;
    }
}
