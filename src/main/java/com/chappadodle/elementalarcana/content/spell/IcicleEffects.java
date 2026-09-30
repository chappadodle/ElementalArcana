package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.decal.Decals;
import com.chappadodle.elementalarcana.client.particle.IceCrystalParticle;
import com.chappadodle.elementalarcana.client.particle.IceSpikeParticle;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

/**
 * The icicle's particles, all spawned on the client (see
 * docs/superpowers/specs/2026-09-30-icicle-vfx-design.md, step 2). Each look has its own cold
 * palette: the colder the ice, the longer and brighter its trail and the less it mists. Branch
 * signatures: Piercing Cold streaks, Shatterburst crackles, Endless Winter trails snow, and the
 * Glacial Lance leaves a wide trail with glints spiralling around it. Shatters (step 3) are played
 * out on each client by IceShatterEmitter, a tick at a time.
 */
public final class IcicleEffects {
    /** How many ticks a shatter takes to play out. */
    public static final int SHATTER_TICKS = 8;

    /**
     * A look's particles: the trail's colour and the colour it fades to, the glint colour, the trail
     * flare size (for a full-size icicle) and lifetime, how misty it is (0 none, 1 light, 2 heavy)
     * and the chance per tick of shedding a sparkle in flight.
     */
    private record Palette(int core, int fade, int glint, float trailSize, int trailLife, int mist, float sparkleChance) {
    }

    // Indexed by look, in IcicleSpell's order (frost 1-4, piercing, shatterburst, winter, lance).
    private static final List<Palette> PALETTES = List.of(
            new Palette(0xDDEEFF, 0x7FA8C8, 0xFFFFFF, 0.14f, 6, 2, 0.3f),
            new Palette(0xBFE6FF, 0x4E8FD0, 0xE8F8FF, 0.16f, 8, 1, 0.45f),
            new Palette(0x8FC8FF, 0x2A5FB8, 0xCFEFFF, 0.18f, 9, 1, 0.55f),
            new Palette(0xE8FFFF, 0x5ACBFF, 0xFFFFFF, 0.2f, 11, 0, 0.8f),
            new Palette(0xF0FFFF, 0x7FDFFF, 0xFFFFFF, 0.1f, 12, 0, 0.6f),
            new Palette(0x9FEFFF, 0x2F7FD0, 0xBFFFFF, 0.16f, 8, 1, 0.5f),
            new Palette(0xC8D8F0, 0x3A4A66, 0xFFFFFF, 0.18f, 10, 2, 0.4f),
            new Palette(0xE0FAFF, 0x4FA8E8, 0xFFFFFF, 0.24f, 12, 0, 1f));

    private IcicleEffects() {
    }

    private static int look(SpellProjectile icicle) {
        return IcicleSpell.look(icicle.variant());
    }

    private static boolean bright(SpellProjectile icicle) {
        return (icicle.variant() & IcicleSpell.BRIGHT) != 0;
    }

    private static Palette palette(SpellProjectile icicle) {
        return PALETTES.get(look(icicle));
    }

    /** How big the icicle is drawn, matching SpellProjectileRenderer. */
    private static float size(SpellProjectile icicle) {
        return Mth.lerp(icicle.charge(0f), 0.35f, 1f) * icicle.visualScale();
    }

    // ---- held ----

    /**
     * Frost drawn into the icicle as it forms, and cold vapour sinking off it (cold air falls).
     * Kept light when several icicles are held, so a full Halo doesn't cloud the view.
     */
    static void held(SpellProjectile icicle, float charge) {
        Level level = icicle.level();
        RandomSource random = icicle.getRandom();
        Palette palette = palette(icicle);
        float size = size(icicle);
        float crowd = icicle.formationCount() > 2 ? 0.5f : 1f;

        if (charge < 1f && random.nextFloat() < (0.2f + 0.4f * charge) * crowd) {
            Vec3 direction = randomDirection(random);
            double radius = 0.25 * size + 0.4;
            Vec3 from = icicle.position().add(direction.scale(radius));
            Vec3 velocity = direction.scale(-radius / 3.5);
            level.addParticle(glow(ModContent.SPARK.get(), palette.glint(), palette.core(), 0.06f, 6).anchoredTo(icicle),
                    from.x, from.y, from.z, velocity.x, velocity.y, velocity.z);
        }
        if (icicle.tickCount % (crowd < 1f ? 8 : 4) == 0) {
            icicle.spawnParticleAround(ModContent.FROST_MIST.get(), 0.1 * size, new Vec3(0, -0.03, 0));
        }
        if (charge >= 1f && random.nextFloat() < 0.15f * crowd) {
            // Fully formed: the odd glint catching the light.
            icicle.spawnParticleAround(ModContent.FROST_SPARKLE.get(), 0.2 * size, Vec3.ZERO);
        }
        if (look(icicle) == IcicleSpell.LOOK_WINTER && icicle.tickCount % 5 == 0) {
            icicle.spawnParticleAround(ParticleTypes.SNOWFLAKE, 0.2 * size, new Vec3(0, -0.02, 0));
        }
    }

    /** Fully formed: a flash and a ring of glints bursting outward. */
    static void grown(SpellProjectile icicle) {
        Level level = icicle.level();
        RandomSource random = icicle.getRandom();
        Palette palette = palette(icicle);
        float size = size(icicle);
        Vec3 pos = icicle.position();
        level.addParticle(glow(ModContent.FLARE.get(), palette.glint(), palette.core(), 0.5f * size, 4).anchoredTo(icicle),
                pos.x, pos.y, pos.z, 0, 0, 0);
        for (int i = 0; i < 8; i++) {
            Vec3 velocity = randomDirection(random).scale(0.1 + 0.03 * random.nextDouble());
            level.addParticle(glow(ModContent.SPARK.get(), palette.glint(), palette.fade(), 0.07f, 6),
                    pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        }
    }

    // ---- thrown ----

    /** The throw: a cold flash and a ring of crystal sparks flung out around the throw's line. */
    static void released(SpellProjectile icicle) {
        Level level = icicle.level();
        RandomSource random = icicle.getRandom();
        Palette palette = palette(icicle);
        float size = size(icicle);
        Vec3 pos = icicle.position();
        Vec3 forward = throwDirection(icicle);
        level.addParticle(glow(ModContent.FLARE.get(), palette.glint(), palette.core(), 0.6f * size, 3),
                pos.x, pos.y, pos.z, forward.x * 0.1, forward.y * 0.1, forward.z * 0.1);
        Vec3 side = forward.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0e-4) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(forward).normalize();
        int sparks = look(icicle) == IcicleSpell.LOOK_LANCE ? 18 : 8;
        for (int i = 0; i < sparks; i++) {
            double angle = Mth.TWO_PI * i / sparks + random.nextDouble() * 0.4;
            Vec3 out = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 velocity = out.scale(0.12 + 0.05 * random.nextDouble()).add(forward.scale(-0.03));
            level.addParticle(glow(ModContent.SPARK.get(), palette.glint(), palette.fade(), 0.07f, 6),
                    pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        }
    }

    /** Which way it was thrown: its motion, or where its caster is looking if that hasn't arrived yet. */
    private static Vec3 throwDirection(SpellProjectile icicle) {
        Vec3 motion = icicle.getDeltaMovement();
        if (motion.lengthSqr() > 1.0e-4) {
            return motion.normalize();
        }
        Entity owner = icicle.getOwner();
        return owner != null ? owner.getLookAngle() : new Vec3(0, 0, 1);
    }

    /** In flight: a ribbon of frost along its path, sparkles and mist, and the look's signature. */
    static void flight(SpellProjectile icicle) {
        if (icicle.visualScale() < 0.5f) {
            // Shatterburst shrapnel: just a glint now and then.
            if (icicle.tickCount % 2 == 0) {
                icicle.spawnParticleAround(ModContent.FROST_SPARKLE.get(), 0.03, Vec3.ZERO);
            }
            return;
        }
        Level level = icicle.level();
        RandomSource random = icicle.getRandom();
        Palette palette = palette(icicle);
        float size = size(icicle);
        float boost = bright(icicle) ? 1.2f : 1f;
        Vec3 from = new Vec3(icicle.xo, icicle.yo, icicle.zo);
        Vec3 to = icicle.position();
        Vec3 path = to.subtract(from);
        Vec3 back = icicle.getDeltaMovement().scale(-0.05);
        int look = look(icicle);

        // Frost spaced along the whole path since the last tick, so fast throws leave no gaps.
        float flareSize = palette.trailSize() * size * boost;
        double spacing = Math.max(0.08, 0.35 * flareSize);
        int count = Mth.clamp(Mth.ceil(path.length() / spacing), 1, 12);
        for (int i = 0; i < count; i++) {
            Vec3 at = from.add(path.scale((i + random.nextDouble()) / count));
            level.addParticle(glow(ModContent.FLARE.get(), palette.core(), palette.fade(), flareSize, Math.round(palette.trailLife() * boost)),
                    at.x, at.y, at.z, back.x * 0.2, back.y * 0.2 - 0.005, back.z * 0.2);
        }
        if (random.nextFloat() < palette.sparkleChance() * boost) {
            icicle.spawnParticleAround(ModContent.FROST_SPARKLE.get(), 0.1 * size, back);
        }
        if (palette.mist() > 0 && icicle.tickCount % (palette.mist() == 2 ? 2 : 4) == 0) {
            icicle.spawnParticleAround(ModContent.FROST_MIST.get(), 0.08 * size, back.scale(0.3).add(0, -0.01, 0));
        }

        switch (look) {
            case IcicleSpell.LOOK_PIERCING -> {
                // Bright streaks left along its line, like it's cutting the air.
                if (path.lengthSqr() > 1.0e-4) {
                    Vec3 along = path.normalize().scale(0.02);
                    for (int i = 0; i < 2; i++) {
                        Vec3 at = from.add(path.scale(random.nextDouble()));
                        level.addParticle(glow(ModContent.SPARK.get(), 0xFFFFFF, palette.fade(), 0.14f * size, 7),
                                at.x, at.y, at.z, along.x, along.y, along.z);
                    }
                }
            }
            case IcicleSpell.LOOK_SHATTER -> {
                // Crackling: now and then a few sparks snap off it.
                if (random.nextFloat() < 0.35f) {
                    for (int i = 0; i < 2; i++) {
                        Vec3 velocity = randomDirection(random).scale(0.12);
                        level.addParticle(glow(ModContent.SPARK.get(), palette.glint(), palette.fade(), 0.06f, 5),
                                to.x, to.y, to.z, velocity.x, velocity.y, velocity.z);
                    }
                }
            }
            case IcicleSpell.LOOK_WINTER -> {
                for (int i = 0; i < 2; i++) {
                    icicle.spawnParticleAround(ParticleTypes.SNOWFLAKE, 0.25 * size, back.scale(0.5).add(0, -0.02, 0));
                }
            }
            case IcicleSpell.LOOK_LANCE -> lanceSpiral(icicle, palette, size, from, path);
            default -> {
                if (look == IcicleSpell.LOOK_FROST_1 && icicle.tickCount % 3 == 0) {
                    icicle.spawnParticleAround(ParticleTypes.SNOWFLAKE, 0.1, Vec3.ZERO);
                }
            }
        }
    }

    /** Glacial Lance: two glints spiral around its path as it flies. */
    private static void lanceSpiral(SpellProjectile icicle, Palette palette, float size, Vec3 from, Vec3 path) {
        if (path.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 forward = path.normalize();
        Vec3 side = forward.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0e-4) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(forward).normalize();
        double radius = 0.35 * size;
        for (int step = 0; step < 3; step++) {
            float t = (step + 0.5f) / 3;
            float angle = (icicle.tickCount + t) * 0.9f;
            Vec3 centre = from.add(path.scale(t));
            for (int arm = 0; arm < 2; arm++) {
                float a = angle + Mth.PI * arm;
                Vec3 at = centre.add(side.scale(Math.cos(a) * radius)).add(up.scale(Math.sin(a) * radius));
                icicle.level().addParticle(glow(ModContent.FLARE.get(), palette.glint(), palette.fade(), 0.12f * size, 10),
                        at.x, at.y, at.z, 0, 0, 0);
            }
        }
    }

    // ---- shatters (client, played out by IceShatterEmitter) ----

    /** The block texture of a look, for the 3D shards and spikes. */
    private static ResourceLocation iceTexture(int look) {
        String name = switch (look) {
            case IcicleSpell.LOOK_FROST_2 -> "frost2";
            case IcicleSpell.LOOK_FROST_3 -> "frost3";
            case IcicleSpell.LOOK_FROST_4 -> "frost4";
            case IcicleSpell.LOOK_PIERCING -> "piercing";
            case IcicleSpell.LOOK_SHATTER -> "shatter";
            case IcicleSpell.LOOK_WINTER -> "winter";
            case IcicleSpell.LOOK_LANCE -> "lance";
            default -> "frost1";
        };
        return ElementalArcana.id("block/icicle_" + name);
    }

    /**
     * One tick ({@code age}, from 0) of an icicle shattering at {@code at}: a flash, glints and
     * 3D ice shards bursting out, cold mist, a frost ring and frost left on the surface it hit.
     * The Glacial Lance splits the ground with ice spikes; a full-grown Endless Winter icicle leaves
     * a wide frost patch in a snow flurry. Shrapnel only tinkles into glints.
     */
    public static void shatter(Level level, Vec3 at, int variant, float size, float charge, int age) {
        RandomSource random = level.getRandom();
        int look = IcicleSpell.look(variant);
        Palette palette = PALETTES.get(look);
        boolean bright = (variant & IcicleSpell.BRIGHT) != 0;
        boolean lance = look == IcicleSpell.LOOK_LANCE;
        boolean winter = look == IcicleSpell.LOOK_WINTER && charge >= 1f;
        if (size < 0.5f) {
            // Shrapnel: a few glints.
            if (age == 0) {
                for (int i = 0; i < 4; i++) {
                    add(level, ModContent.FROST_SPARKLE.get(), at, randomDirection(random).scale(0.08));
                }
            }
            return;
        }
        float scale = size * (0.6f + 0.4f * charge);
        @Nullable BlockHitResult ground = ImpactSurfaces.groundBelow(level, at);
        Vec3 floor = ground != null ? ground.getLocation() : at;

        if (age == 0) {
            add(level, glow(ModContent.FLARE.get(), palette.glint(), palette.core(), 0.5f * scale, 3), at, Vec3.ZERO);
            int glints = Math.round((8 + 10 * charge) * size);
            for (int i = 0; i < glints; i++) {
                Vec3 velocity = randomDirection(random).scale(0.1 + 0.12 * random.nextDouble());
                add(level, glow(ModContent.SPARK.get(), palette.glint(), palette.fade(), 0.07f, 8), at, velocity);
            }
            for (int i = 0; i < Math.round(3 * size); i++) {
                add(level, ModContent.FROST_MIST.get(), at, randomDirection(random).scale(0.03).add(0, -0.01, 0));
            }
            // Real 3D shards of the icicle's own ice.
            ResourceLocation texture = iceTexture(look);
            int shards = lance ? 14 : Math.round((3 + 4 * charge) * Math.min(size, 1.5f)) + (bright ? 2 : 0);
            float minSize = lance ? 0.2f : 0.1f;
            float maxSize = lance ? 0.4f : 0.2f;
            for (int i = 0; i < shards; i++) {
                Vec3 direction = randomDirection(random);
                Vec3 velocity = new Vec3(direction.x * 0.25, 0.15 + Math.abs(direction.y) * 0.25, direction.z * 0.25).scale(lance ? 1.4 : 1.0);
                float shardSize = minSize + random.nextFloat() * (maxSize - minSize);
                spawn(new IceCrystalParticle((ClientLevel) level, at, velocity, texture, shardSize));
            }
            // A ring of frost racing out along the ground (a halo in mid-air).
            float ring = lance ? 3.5f : (0.8f + 0.8f * charge) * size;
            add(level, glow(ModContent.SHOCKWAVE.get(), palette.glint(), palette.fade(), ring, lance ? 12 : 8), floor.add(0, 0.05, 0), Vec3.ZERO);
            // Frost left on whatever it hit.
            @Nullable BlockHitResult surface = ground != null ? ground : ImpactSurfaces.surfaceNear(level, at);
            if (surface != null) {
                float radius = lance ? 3f : winter ? 2.5f : (0.5f + 0.6f * charge) * size;
                int ticks = lance ? 140 : winter ? 100 : charge >= 1f ? 100 : 60;
                Decals.add(Decals.Kind.FROST, surface.getLocation(), surface.getDirection(), radius, ticks, 0xFFFFFF);
            }
            if (lance && ground != null) {
                spikes(level, floor, texture, random);
            }
            if (winter) {
                // A snow flurry over the frost patch.
                for (int i = 0; i < 30; i++) {
                    Vec3 velocity = new Vec3((random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.3);
                    add(level, ParticleTypes.SNOWFLAKE, floor.add(0, 0.3, 0), velocity);
                }
                for (int i = 0; i < 8; i++) {
                    add(level, ModContent.FROST_MIST.get(), floor.add((random.nextDouble() - 0.5) * 3, 0.2, (random.nextDouble() - 0.5) * 3), Vec3.ZERO);
                }
            }
        } else if (age % 2 == 0) {
            // Glints drifting down as the dust settles.
            add(level, ModContent.FROST_SPARKLE.get(),
                    at.add((random.nextDouble() - 0.5) * scale, random.nextDouble() * 0.3 * scale, (random.nextDouble() - 0.5) * scale),
                    new Vec3(0, -0.02, 0));
        }
    }

    /** Glacial Lance: a ring of ice spikes erupting from the ground, leaning outward. */
    private static void spikes(Level level, Vec3 floor, ResourceLocation texture, RandomSource random) {
        int count = 9;
        for (int i = 0; i < count; i++) {
            double angle = Mth.TWO_PI * i / count + random.nextDouble() * 0.5;
            double distance = 1.2 + random.nextDouble() * 2.0;
            Vec3 base = floor.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            // Stand them on whatever ground is there (it may step up or down).
            @Nullable BlockHitResult ground = ImpactSurfaces.groundBelow(level, base.add(0, 1, 0));
            if (ground == null) {
                continue;
            }
            float height = 0.8f + random.nextFloat() * 1.4f;
            // Lean away from the impact, by 10-25 degrees.
            float tilt = (float) Math.toRadians(10 + random.nextDouble() * 15);
            Quaternionf lean = new Quaternionf().rotateAxis(tilt, (float) Math.sin(angle), 0f, (float) -Math.cos(angle));
            spawn(new IceSpikeParticle((ClientLevel) level, ground.getLocation(), texture, height, lean, 50 + random.nextInt(20)));
        }
    }

    private static void add(Level level, ParticleOptions particle, Vec3 at, Vec3 velocity) {
        level.addParticle(particle, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
    }

    /** Adds a particle made here directly (the 3D shapes aren't sent or spawned by type). */
    private static void spawn(Particle particle) {
        Minecraft.getInstance().particleEngine.add(particle);
    }

    // ---- pieces ----

    private static GlowParticleOptions glow(ParticleType<GlowParticleOptions> type, int color, int fade, float size, int lifetime) {
        return GlowParticleOptions.of(type, color, fade, size, lifetime);
    }

    private static Vec3 randomDirection(RandomSource random) {
        return new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
    }
}
