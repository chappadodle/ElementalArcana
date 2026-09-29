package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The fireball's particles, all spawned on the client (see
 * docs/superpowers/specs/2026-09-29-fireball-vfx-design.md, step 2). Each look has its own palette
 * and trail: the heat stages burn longer, brighter and cleaner as they rise, and the branch looks
 * each have a signature (Cluster sputters like a fuse, Meteor sheds smoke and cinders, Sun trails
 * white-gold with a turning corona while held, Phoenix drops feathers).
 */
final class FireballEffects {

    /**
     * A look's particles: the trail's colour and the colour it fades to, the spark colour, the
     * trail flare size (for a full-size fireball) and lifetime, how smoky it is (0 none, 1 light,
     * 2 heavy) and the chance per tick of shedding a spark in flight.
     */
    private record Palette(int core, int fade, int spark, float trailSize, int trailLife, int smoke, float sparkChance) {
    }

    // Indexed by look, in FireballSpell's order (heat 1-4, cluster, meteor, sun, phoenix).
    private static final List<Palette> PALETTES = List.of(
            new Palette(0xFF7A28, 0x7A1405, 0xFFB050, 0.22f, 7, 1, 0f),
            new Palette(0xFFA640, 0x9A2008, 0xFFD070, 0.25f, 9, 1, 0.15f),
            new Palette(0xFFD27A, 0xC03A10, 0xFFF0B0, 0.28f, 11, 0, 0.35f),
            new Palette(0xFFF2D0, 0xE8641A, 0xFFFFFF, 0.32f, 13, 0, 0.6f),
            new Palette(0xFFB050, 0x802010, 0xFFD27A, 0.14f, 5, 1, 0f),
            new Palette(0xFF8C32, 0x601808, 0xFFB050, 0.34f, 10, 2, 0.2f),
            new Palette(0xFFFBE6, 0xFFA030, 0xFFFFFF, 0.3f, 14, 0, 1f),
            new Palette(0xFFD25A, 0xB4142D, 0xFFE08C, 0.28f, 12, 0, 0.3f));

    private FireballEffects() {
    }

    private static Palette palette(SpellProjectile fireball) {
        return PALETTES.get(Math.floorMod(fireball.variant(), PALETTES.size()));
    }

    /** How big the fireball is drawn, matching SpellProjectileRenderer. */
    private static float size(SpellProjectile fireball) {
        return Mth.lerp(fireball.charge(0f), 0.35f, 1f) * fireball.visualScale();
    }

    // ---- held ----

    /** Sparks drawn into the fireball as it grows, a few rising embers, and the look's signature. */
    static void held(SpellProjectile fireball, float charge) {
        Level level = fireball.level();
        RandomSource random = fireball.getRandom();
        Palette palette = palette(fireball);
        float size = size(fireball);

        if (charge < 1f && random.nextFloat() < 0.2f + 0.4f * charge) {
            // Gathering: a spark appears around it and is pulled in.
            Vec3 direction = randomDirection(random);
            double radius = 0.25 * size + 0.35;
            Vec3 from = fireball.position().add(direction.scale(radius));
            Vec3 velocity = direction.scale(-radius / 3.5);
            level.addParticle(glow(ModContent.SPARK.get(), palette.spark(), palette.core(), 0.07f, 6).anchoredTo(fireball),
                    from.x, from.y, from.z, velocity.x, velocity.y, velocity.z);
        }
        if (fireball.tickCount % 4 == 0) {
            fireball.spawnParticleAround(ModContent.EMBER.get(), 0.15 * size, new Vec3(0, 0.03, 0));
        }

        switch (fireball.variant()) {
            case FireballSpell.LOOK_CLUSTER -> {
                if (random.nextFloat() < 0.5f) {
                    fuseSputter(fireball, palette, size, 0.1);
                }
            }
            case FireballSpell.LOOK_METEOR -> {
                if (fireball.tickCount % 8 == 0) {
                    fireball.spawnParticleAround(ModContent.CINDER.get(), 0.15 * size, new Vec3(0, -0.02, 0));
                }
            }
            case FireballSpell.LOOK_SUN -> corona(fireball, palette, size);
            case FireballSpell.LOOK_PHOENIX -> {
                if (fireball.tickCount % 7 == 0) {
                    feather(fireball, fireball.position(), new Vec3(0, -0.01, 0));
                }
            }
            default -> {
            }
        }
    }

    /** Sunfire's corona: two bright motes circling the sun, their fading flares drawing the ring. */
    private static void corona(SpellProjectile fireball, Palette palette, float size) {
        double radius = 0.45 * size;
        float angle = fireball.tickCount * 0.35f;
        for (int i = 0; i < 2; i++) {
            float at = angle + Mth.PI * i;
            // A ring tilted toward the viewer, so it reads from the caster's own view too.
            Vec3 offset = new Vec3(Mth.cos(at) * radius, Mth.sin(at) * radius * 0.5, Mth.sin(at) * radius * 0.85);
            Vec3 pos = fireball.position().add(offset);
            fireball.level().addParticle(glow(ModContent.FLARE.get(), palette.spark(), palette.fade(), 0.18f, 8).anchoredTo(fireball),
                    pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    /** Fully grown: a flash and a ring of sparks bursting outward. */
    static void grown(SpellProjectile fireball) {
        Level level = fireball.level();
        RandomSource random = fireball.getRandom();
        Palette palette = palette(fireball);
        float size = size(fireball);
        Vec3 pos = fireball.position();
        level.addParticle(glow(ModContent.FLARE.get(), palette.spark(), palette.core(), 0.7f * size, 4).anchoredTo(fireball),
                pos.x, pos.y, pos.z, 0, 0, 0);
        for (int i = 0; i < 10; i++) {
            Vec3 velocity = randomDirection(random).scale(0.12 + 0.04 * random.nextDouble());
            level.addParticle(glow(ModContent.SPARK.get(), palette.spark(), palette.fade(), 0.08f, 6),
                    pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        }
    }

    // ---- thrown ----

    /** The throw: a flash at the hand and a ring of sparks flung out around the throw's line. */
    static void released(SpellProjectile fireball) {
        Level level = fireball.level();
        RandomSource random = fireball.getRandom();
        Palette palette = palette(fireball);
        float size = size(fireball);
        Vec3 pos = fireball.position();
        Vec3 forward = throwDirection(fireball);
        level.addParticle(glow(ModContent.FLARE.get(), palette.spark(), palette.core(), 0.9f * size, 4),
                pos.x, pos.y, pos.z, forward.x * 0.1, forward.y * 0.1, forward.z * 0.1);
        Vec3 side = forward.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0e-4) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(forward).normalize();
        int sparks = 8 + 2 * Math.min(4, fireball.variant() + 1);
        for (int i = 0; i < sparks; i++) {
            double angle = Mth.TWO_PI * i / sparks + random.nextDouble() * 0.4;
            Vec3 out = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 velocity = out.scale(0.14 + 0.06 * random.nextDouble()).add(forward.scale(-0.04));
            level.addParticle(glow(ModContent.SPARK.get(), palette.spark(), palette.fade(), 0.09f, 7),
                    pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        }
    }

    /** Which way it was thrown: its motion, or where its caster is looking if that hasn't arrived yet. */
    private static Vec3 throwDirection(SpellProjectile fireball) {
        Vec3 motion = fireball.getDeltaMovement();
        if (motion.lengthSqr() > 1.0e-4) {
            return motion.normalize();
        }
        Entity owner = fireball.getOwner();
        return owner != null ? owner.getLookAngle() : new Vec3(0, 0, 1);
    }

    /** In flight: a continuous ribbon of flares along its path, embers, and the look's signature. */
    static void flight(SpellProjectile fireball) {
        Level level = fireball.level();
        RandomSource random = fireball.getRandom();
        Palette palette = palette(fireball);
        float size = size(fireball);
        Vec3 from = new Vec3(fireball.xo, fireball.yo, fireball.zo);
        Vec3 to = fireball.position();
        Vec3 path = to.subtract(from);
        Vec3 back = fireball.getDeltaMovement().scale(-0.05);
        boolean cluster = fireball.variant() == FireballSpell.LOOK_CLUSTER;

        if (cluster) {
            // A lit fuse rather than a flame: sputtering sparks and a thin line of smoke.
            fuseSputter(fireball, palette, size, 0.15);
            fuseSputter(fireball, palette, size, 0.15);
            level.addParticle(glow(ModContent.FLARE.get(), palette.core(), palette.fade(), palette.trailSize() * size, palette.trailLife()),
                    to.x, to.y, to.z, 0, 0, 0);
        } else {
            // Flares spaced along the whole path since the last tick, so fast throws leave no gaps.
            double spacing = Math.max(0.08, 0.3 * palette.trailSize() * size);
            int count = Mth.clamp(Mth.ceil(path.length() / spacing), 1, 12);
            float flareSize = palette.trailSize() * size;
            for (int i = 0; i < count; i++) {
                Vec3 at = from.add(path.scale((i + random.nextDouble()) / count));
                double jitter = 0.06 * size;
                level.addParticle(glow(ModContent.FLARE.get(), palette.core(), palette.fade(), flareSize, palette.trailLife()),
                        at.x + (random.nextDouble() - 0.5) * jitter, at.y + (random.nextDouble() - 0.5) * jitter,
                        at.z + (random.nextDouble() - 0.5) * jitter,
                        back.x * 0.3 + (random.nextDouble() - 0.5) * 0.02, back.y * 0.3 + 0.01, back.z * 0.3 + (random.nextDouble() - 0.5) * 0.02);
            }
        }

        if (fireball.tickCount % 2 == 0) {
            fireball.spawnParticleAround(ModContent.EMBER.get(), 0.12 * size, back.add(0, 0.02, 0));
        }
        if (random.nextFloat() < palette.sparkChance()) {
            Vec3 velocity = randomDirection(random).scale(0.1).add(back.scale(1.5));
            level.addParticle(glow(ModContent.SPARK.get(), palette.spark(), palette.fade(), 0.08f, 8),
                    to.x, to.y, to.z, velocity.x, velocity.y, velocity.z);
        }
        switch (palette.smoke()) {
            case 1 -> {
                if (fireball.tickCount % 2 == 0) {
                    fireball.spawnParticleAround(ParticleTypes.SMOKE, 0.1 * size, back.scale(0.5));
                }
            }
            case 2 -> fireball.spawnParticleAround(ParticleTypes.LARGE_SMOKE, 0.2 * size, back.scale(0.3).add(0, 0.02, 0));
            default -> {
            }
        }

        switch (fireball.variant()) {
            case FireballSpell.LOOK_METEOR -> {
                if (fireball.tickCount % 2 == 0) {
                    Vec3 velocity = randomDirection(random).scale(0.08).add(back);
                    fireball.spawnParticleAround(ModContent.CINDER.get(), 0.15 * size, velocity);
                }
                if (fireball.tickCount % 4 == 0) {
                    fireball.spawnParticleAround(ParticleTypes.FALLING_LAVA, 0.15 * size, Vec3.ZERO);
                }
            }
            case FireballSpell.LOOK_SUN -> {
                Vec3 velocity = randomDirection(random).scale(0.12).add(back);
                level.addParticle(glow(ModContent.SPARK.get(), palette.spark(), palette.core(), 0.1f, 8),
                        to.x, to.y, to.z, velocity.x, velocity.y, velocity.z);
            }
            case FireballSpell.LOOK_PHOENIX -> {
                if (fireball.tickCount % 3 == 0) {
                    feather(fireball, to, back.scale(0.5).add(randomDirection(random).scale(0.03)));
                }
            }
            default -> {
            }
        }
    }

    // ---- pieces ----

    /** One sputtering fuse spark, flying off the top of a Cluster fireball, and a wisp of smoke. */
    private static void fuseSputter(SpellProjectile fireball, Palette palette, float size, double speed) {
        RandomSource random = fireball.getRandom();
        Vec3 top = fireball.position().add(0, 0.2 * size, 0);
        Vec3 velocity = randomDirection(random).add(0, 0.8, 0).normalize().scale(speed * (0.6 + random.nextDouble()));
        fireball.level().addParticle(glow(ModContent.SPARK.get(), 0xFFFFFF, palette.spark(), 0.06f, 5),
                top.x, top.y, top.z, velocity.x, velocity.y, velocity.z);
        if (random.nextFloat() < 0.3f) {
            fireball.level().addParticle(ParticleTypes.SMOKE, top.x, top.y, top.z, 0, 0.02, 0);
        }
    }

    private static void feather(SpellProjectile fireball, Vec3 at, Vec3 velocity) {
        fireball.level().addParticle(glow(ModContent.FEATHER.get(), 0xFFD25A, 0xB4142D, 0.16f, 30),
                at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
    }

    private static GlowParticleOptions glow(ParticleType<GlowParticleOptions> type, int color, int fade, float size, int lifetime) {
        return GlowParticleOptions.of(type, color, fade, size, lifetime);
    }

    private static Vec3 randomDirection(RandomSource random) {
        return new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
    }
}
