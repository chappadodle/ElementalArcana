package com.chappadodle.elementalarcana.compat.dynamiclights;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Range;

/**
 * An explosion's flash of light for LambDynamicLights: a point of light at the blast that starts at
 * full strength and fades to nothing over a few ticks, then removes itself. Its brightness is worked
 * out from the game time whenever it's asked, so it needs no ticking of its own.
 */
public class BlastLight implements DynamicLightBehavior {
    private final double x, y, z;
    private final int luminance;
    private final long born;
    private final int ticks;
    private final BoundingBox box;
    private int lastLevel = -1;

    public BlastLight(Vec3 at, int luminance, int ticks) {
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
        this.luminance = luminance;
        this.ticks = ticks;
        this.born = now();
        BlockPos pos = BlockPos.containing(at);
        this.box = new BoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    private static long now() {
        return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
    }

    /** The light's strength right now: full at first, fading out. */
    private double strength() {
        double life = (now() - born) / (double) ticks;
        return life >= 1 ? 0 : luminance * (1 - life * life);
    }

    @Override
    public @Range(from = 0, to = 15) double lightAtPos(BlockPos pos, double falloffRatio) {
        double dx = pos.getX() + 0.5 - x;
        double dy = pos.getY() + 0.5 - y;
        double dz = pos.getZ() + 0.5 - z;
        return Math.max(strength() - Math.sqrt(dx * dx + dy * dy + dz * dz) * falloffRatio, 0.0);
    }

    @Override
    public BoundingBox getBoundingBox() {
        return box;
    }

    @Override
    public boolean hasChanged() {
        int level = (int) Math.ceil(strength());
        if (level != lastLevel) {
            lastLevel = level;
            return true;
        }
        return false;
    }

    @Override
    public boolean isRemoved() {
        return now() - born >= ticks;
    }
}
