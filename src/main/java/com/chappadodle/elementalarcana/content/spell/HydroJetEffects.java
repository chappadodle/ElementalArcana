package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.HydroStreamOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Hydro Jet's particles, all spawned on the client each tick of a stream (see
 * HydroStreamEmitter and docs/superpowers/specs/2026-09-30-hydro-jet-vfx-design.md, step 2):
 * vanilla-style pixel water. Splashes and droplets swirl at the palm, drips peel off along the
 * stream, and each look adds its own: Tidecutter glints and mist, Torrent heavy spray, Maelstrom
 * droplets flung off its spiral, the Tsunami Lance nautilus swirls, the bright tiers cyan glints.
 * Riding the jet down (Recoil, Lv 8+) sprays out around your feet.
 */
public final class HydroJetEffects {

    private HydroJetEffects() {
    }

    /** One tick of a stream from {@code start} along {@code line}. */
    public static void stream(Level level, HydroStreamOptions stream, Vec3 start, Vec3 line) {
        RandomSource random = level.getRandom();
        double length = line.length();
        if (length < 0.1) {
            return;
        }
        Vec3 dir = line.scale(1 / length);
        Vec3 reference = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(reference).normalize();
        Vec3 up = right.cross(dir).normalize();
        int look = HydroJetSpell.look(stream.look());
        boolean bright = (stream.look() & HydroJetSpell.BRIGHT) != 0;
        boolean torrent = look == HydroJetSpell.LOOK_TORRENT;

        // At the palm: water swirling out as it leaves the hand.
        for (int i = 0; i < (torrent ? 3 : 1); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            Vec3 out = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            add(level, ModContent.HYDRO_DROP.get(), start.add(dir.scale(0.2)), out.scale(0.06).add(dir.scale(0.1)));
        }
        if (random.nextFloat() < (torrent ? 0.8f : 0.3f)) {
            add(level, ParticleTypes.SPLASH, start.add(dir.scale(0.3)), Vec3.ZERO);
        }

        // Along the stream: drips peeling off and falling.
        int drips = torrent ? 4 : look == HydroJetSpell.LOOK_TIDECUTTER ? 1 : 2;
        for (int i = 0; i < drips; i++) {
            Vec3 at = start.add(line.scale(0.2 + 0.8 * random.nextDouble()));
            add(level, ParticleTypes.FALLING_WATER, at, Vec3.ZERO);
        }

        switch (look) {
            case HydroJetSpell.LOOK_TIDECUTTER -> {
                // A fine, glinting mist along the cutting line.
                Vec3 at = start.add(line.scale(random.nextDouble()));
                add(level, glow(0xFFFFFF, 0x9FD8FF, 0.06f, 5), at, dir.scale(0.05));
                if (random.nextFloat() < 0.3f) {
                    add(level, ModContent.FROST_MIST.get(), start.add(line.scale(random.nextDouble())), Vec3.ZERO);
                }
            }
            case HydroJetSpell.LOOK_TORRENT -> {
                // Heavy spray bursting off the sides of the hose.
                for (int i = 0; i < 3; i++) {
                    Vec3 at = start.add(line.scale(0.3 + 0.7 * random.nextDouble()));
                    add(level, ParticleTypes.SPLASH, at, Vec3.ZERO);
                }
                if (random.nextFloat() < 0.4f) {
                    add(level, ModContent.FROST_MIST.get(), start.add(line.scale(random.nextDouble())), up.scale(0.02));
                }
            }
            case HydroJetSpell.LOOK_MAELSTROM -> {
                // Droplets flung off the spiral, once it has wound out (see WaterBeams).
                if (length > 2) {
                    double d = 1.5 + random.nextDouble() * (length - 1.5);
                    double angle = d * 2.0 + level.getGameTime() * 0.4;
                    Vec3 around = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                    Vec3 at = start.add(dir.scale(d)).add(around.scale(0.3));
                    Vec3 tangent = right.scale(-Math.sin(angle)).add(up.scale(Math.cos(angle)));
                    add(level, ModContent.HYDRO_DROP.get(), at, tangent.scale(0.15).add(around.scale(0.08)));
                }
            }
            case HydroJetSpell.LOOK_LANCE -> {
                if (random.nextFloat() < 0.5f) {
                    Vec3 at = start.add(line.scale(random.nextDouble()));
                    add(level, ParticleTypes.NAUTILUS, at, Vec3.ZERO);
                }
            }
            default -> {
            }
        }
        if (bright && random.nextFloat() < 0.4f) {
            add(level, glow(0xBFFFFF, 0x2A70D0, 0.07f, 6), start.add(line.scale(random.nextDouble())), Vec3.ZERO);
        }

        // Recoil: riding the jet down sprays out around your feet.
        Entity caster = level.getEntity(stream.caster());
        if (bright && caster != null && dir.y < -0.6) {
            Vec3 feet = caster.position();
            for (int i = 0; i < 4; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                Vec3 out = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                add(level, ModContent.HYDRO_DROP.get(), feet.add(out.scale(0.3)), out.scale(0.18).add(0, 0.05, 0));
            }
            add(level, ParticleTypes.SPLASH, feet, Vec3.ZERO);
        }
    }

    private static GlowParticleOptions glow(int color, int fade, float size, int lifetime) {
        return GlowParticleOptions.of(ModContent.SPARK.get(), color, fade, size, lifetime);
    }

    private static void add(Level level, ParticleOptions particle, Vec3 at, Vec3 velocity) {
        level.addParticle(particle, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
    }
}
