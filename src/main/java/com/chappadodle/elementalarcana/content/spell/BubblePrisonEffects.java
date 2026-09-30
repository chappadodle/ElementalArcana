package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.client.decal.Decals;
import com.chappadodle.elementalarcana.client.particle.WaterCubeParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Bubble Prison's moments, spawned on the client (see BubbleRenderer and
 * docs/superpowers/specs/2026-09-30-bubble-prison-vfx-design.md): when a bubble pops, its water
 * cubes burst outward and splash where they land, leaving a wet mark on the ground below.
 */
public final class BubblePrisonEffects {
    private static final int WATER = 0x4FA8FF;

    private BubblePrisonEffects() {
    }

    /** A bubble of {@code radius} around {@code center} pops ({@code feet}: the trapped creature's). */
    public static void pop(ClientLevel level, Vec3 feet, Vec3 center, float radius) {
        RandomSource random = level.getRandom();
        int cubes = 28 + Math.round(radius * 12);
        for (int i = 0; i < cubes; i++) {
            // A random point on the shell, flung outward from it.
            double y = random.nextDouble() * 2 - 1;
            double angle = random.nextDouble() * Mth.TWO_PI;
            double ring = Math.sqrt(1 - y * y);
            Vec3 dir = new Vec3(Math.cos(angle) * ring, y, Math.sin(angle) * ring);
            Vec3 velocity = dir.scale(0.12 + random.nextDouble() * 0.16).add(0, 0.12, 0);
            float size = Mth.clamp(radius * 0.12f, 0.07f, 0.2f) * (0.8f + 0.4f * random.nextFloat());
            Minecraft.getInstance().particleEngine.add(new WaterCubeParticle(level, center.add(dir.scale(radius)), velocity, size, WATER));
        }
        for (int i = 0; i < 16; i++) {
            level.addParticle(ParticleTypes.SPLASH, center.x + (random.nextDouble() - 0.5) * radius * 2, center.y + (random.nextDouble() - 0.5) * radius,
                    center.z + (random.nextDouble() - 0.5) * radius * 2, 0, 0.1, 0);
        }
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.BUBBLE_POP, center.x + (random.nextDouble() - 0.5) * radius * 1.5, center.y + (random.nextDouble() - 0.5) * radius * 1.5,
                    center.z + (random.nextDouble() - 0.5) * radius * 1.5, 0, 0.02, 0);
        }
        // A wet mark on the ground below (the bubble floats a little over it).
        @Nullable BlockHitResult ground = ImpactSurfaces.groundBelow(level, feet);
        if (ground == null) {
            ground = ImpactSurfaces.groundBelow(level, feet.subtract(0, 1.2, 0));
        }
        if (ground != null) {
            Decals.add(Decals.Kind.WET, ground.getLocation(), ground.getDirection(), Math.max(0.8f, radius * 1.3f), 200, 0xFFFFFF);
        }
    }
}
