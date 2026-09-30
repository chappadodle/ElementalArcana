package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.decal.Decals;
import com.chappadodle.elementalarcana.client.particle.DebrisParticle;
import com.chappadodle.elementalarcana.client.particle.FireSphereParticle;
import com.chappadodle.elementalarcana.client.particle.FireWaveParticle;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The fireball's particles, all spawned on the client (see
 * docs/superpowers/specs/2026-09-29-fireball-vfx-design.md, step 2). Each look has its own palette
 * and trail: the heat stages burn longer, brighter and cleaner as they rise, and the branch looks
 * each have a signature (Cluster sputters like a fuse, Meteor sheds smoke and cinders, Sun trails
 * white-gold with a turning corona while held, Phoenix drops feathers and beats flame wings).
 * Explosions (step 3) are played out on each client by FireBlastEmitter, a tick at a time.
 */
public final class FireballEffects {
    /** How many ticks an explosion takes to play out. */
    public static final int BLAST_TICKS = 14;


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
                wingTrails(fireball, palette, size, from, to);
                if (fireball.tickCount % 3 == 0) {
                    feather(fireball, to, back.scale(0.5).add(randomDirection(random).scale(0.03)));
                }
            }
            default -> {
            }
        }
    }

    /** Phoenix in flight: two ribbons of flame at its sides that beat up and down like wings. */
    private static void wingTrails(SpellProjectile fireball, Palette palette, float size, Vec3 from, Vec3 to) {
        Vec3 path = to.subtract(from);
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
        for (int i = 0; i < 3; i++) {
            float t = (i + 0.5f) / 3;
            float beat = Mth.sin((fireball.tickCount + t) * 0.7f);
            Vec3 center = from.add(path.scale(t));
            for (int wing = -1; wing <= 1; wing += 2) {
                // The wingtip rises and falls; the flare nearer the body follows less.
                Vec3 tip = center.add(side.scale(wing * 0.6 * size)).add(up.scale(beat * 0.3 * size));
                Vec3 mid = center.add(side.scale(wing * 0.3 * size)).add(up.scale(beat * 0.12 * size));
                for (Vec3 at : new Vec3[] {mid, tip}) {
                    fireball.level().addParticle(glow(ModContent.FLARE.get(), palette.core(), palette.fade(), 0.16f * size, 9),
                            at.x, at.y, at.z, 0, 0.01, 0);
                }
            }
        }
    }

    // ---- explosions (client, played out by FireBlastEmitter) ----

    /** How bright an explosion's flash of light is (with a dynamic lights mod), by look. */
    public static int blastLight(int look, boolean bomblet) {
        if (bomblet) {
            return 10;
        }
        return switch (heatOf(look)) {
            case 1 -> 12;
            case 2 -> 13;
            default -> look == FireballSpell.LOOK_SUN || look == FireballSpell.LOOK_METEOR || heatOf(look) >= 4 ? 15 : 14;
        };
    }

    /** Whether this look's blast shakes the camera (Meteor). */
    public static boolean shakes(int look) {
        return look == FireballSpell.LOOK_METEOR;
    }

    /** Whether this look's blast flashes the screen (Sunfire). */
    public static boolean flashes(int look) {
        return look == FireballSpell.LOOK_SUN;
    }

    /** How hot a look's blast burns, 1-4: the heat stages, and the branch looks' place among them. */
    private static int heatOf(int look) {
        return switch (look) {
            case FireballSpell.LOOK_HEAT_1 -> 1;
            case FireballSpell.LOOK_HEAT_2 -> 2;
            case FireballSpell.LOOK_HEAT_4, FireballSpell.LOOK_SUN -> 4;
            default -> 3;
        };
    }

    /**
     * One tick ({@code age}, from 0) of an explosion at {@code at}. {@code toBlast} runs from the
     * viewer to the blast, so Phoenix's wings open across their screen.
     */
    public static void blast(Level level, Vec3 at, int look, float radius, boolean bomblet, int age, Vec3 toBlast) {
        RandomSource random = level.getRandom();
        Palette palette = PALETTES.get(Math.floorMod(look, PALETTES.size()));
        if (bomblet) {
            if (age == 0) {
                firecracker(level, at, palette, radius, random);
            }
            return;
        }
        int heat = heatOf(look);
        boolean meteor = look == FireballSpell.LOOK_METEOR;
        boolean sun = look == FireballSpell.LOOK_SUN;
        @Nullable BlockHitResult ground = groundBelow(level, at);
        Vec3 floor = ground != null ? ground.getLocation() : at;

        if (age == 0) {
            // The flash, then the fireball itself swelling out (a 3D ball of fire, and a hotter core
            // inside it for the hot looks), with flares billowing off it and sparks flying through it.
            add(level, glow(ModContent.FLARE.get(), palette.spark(), palette.core(), radius * (heat >= 4 ? 0.8f : 0.6f), 3), at, Vec3.ZERO);
            ResourceLocation fire = blastTexture(look);
            float ball = sun ? 0.75f : meteor ? 0.6f : 0.5f + 0.05f * heat;
            spawn(new FireSphereParticle((ClientLevel) level, at, fire, radius * ball, 0.3f, sun ? 16 : 9 + heat, palette.core(), palette.fade(), 0.85f));
            if (heat >= 3) {
                spawn(new FireSphereParticle((ClientLevel) level, at, fire, radius * 0.35f, 0.5f, 6, palette.spark(), palette.core(), 1f));
            }
            int body = Math.round(4 + 3 * radius);
            for (int i = 0; i < body; i++) {
                Vec3 velocity = randomDirection(random).scale(radius * (0.07 + 0.07 * random.nextDouble()));
                add(level, glow(ModContent.FLARE.get(), palette.core(), palette.fade(), 0.3f + 0.1f * radius, 12), at, velocity);
            }
            int sparks = heat * 5 + (sun ? 30 : 0) + (meteor ? 10 : 0);
            for (int i = 0; i < sparks; i++) {
                Vec3 velocity = randomDirection(random).scale((0.25 + 0.25 * random.nextDouble()) * Math.sqrt(radius));
                add(level, glow(ModContent.SPARK.get(), palette.spark(), palette.fade(), 0.1f, 10), at, velocity);
            }
            shockwave(level, floor.add(0, 0.05, 0), palette, radius, 8);
            if (ground != null) {
                // A wall of fire rolling out along the ground.
                spawn(new FireWaveParticle((ClientLevel) level, floor, fire, radius * 1.1f, 0.3f + radius * (meteor ? 0.45f : 0.3f), false,
                        9, palette.core(), 0.8f));
            }
            for (int i = 0; i < 6 + 2 * radius; i++) {
                add(level, ModContent.EMBER.get(), at, randomDirection(random).scale(0.15).add(0, 0.05, 0));
            }
            if (ground != null) {
                BlockState surface = level.getBlockState(ground.getBlockPos());
                if ((heat >= 3 || meteor) && !surface.isAir()) {
                    // Dust thrown up around the blast, and real chunks of the ground (3D cubes).
                    int dust = meteor ? 12 : Math.round(3 + radius);
                    for (int i = 0; i < dust; i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        double distance = radius * 0.6 * Math.sqrt(random.nextDouble());
                        Vec3 spot = floor.add(Math.cos(angle) * distance, 0.1, Math.sin(angle) * distance);
                        add(level, new BlockParticleOption(ParticleTypes.DUST_PILLAR, surface), spot,
                                new Vec3(0, (meteor ? 0.5 : 0.25) + 0.2 * random.nextDouble(), 0));
                    }
                    if (Minecraft.getInstance().options.particles().get() != ParticleStatus.MINIMAL) {
                        debris(level, floor, ground.getBlockPos(), surface, radius, meteor, sun, heat, random);
                    }
                }
            }
            // Marks on whatever it hit (the ground, a wall, a ceiling), painted onto the blocks.
            @Nullable BlockHitResult surface = ground != null ? ground : surfaceNear(level, at);
            if (surface != null && (heat >= 4 || meteor)) {
                Decals.add(Decals.Kind.SCORCH, surface.getLocation(), surface.getDirection(), radius * 0.8f, meteor ? 100 : 60, 0xFFFFFF);
                if (meteor) {
                    Decals.add(Decals.Kind.CRACKS, surface.getLocation(), surface.getDirection(), radius * 0.7f, 100, palette.core());
                }
            }
            if (meteor) {
                // Molten rock flung out of the crater.
                for (int i = 0; i < 16; i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    Vec3 velocity = new Vec3(Math.cos(angle) * 0.25, 0.35 + 0.25 * random.nextDouble(), Math.sin(angle) * 0.25);
                    add(level, ModContent.CINDER.get(), floor.add(0, 0.2, 0), velocity);
                }
            }
            if (sun) {
                add(level, glow(ModContent.CORONA.get(), 0xFFFFFF, palette.fade(), radius * 1.4f, 16), at, Vec3.ZERO);
            }
        } else if (age == 2 && heat >= 2) {
            // The core flares a second time as the fire rolls over itself.
            add(level, glow(ModContent.FLARE.get(), palette.spark(), palette.fade(), radius * 0.35f, 5), at, new Vec3(0, 0.02, 0));
        } else if (age == 3 && (heat >= 4)) {
            // A second ring: a slow, wide one for Sunfire, under a swelling dome of light.
            shockwave(level, floor.add(0, 0.06, 0), palette, sun ? radius * 1.5f : radius * 0.7f, sun ? 14 : 9);
            if (sun) {
                spawn(new FireWaveParticle((ClientLevel) level, floor, blastTexture(look), radius * 1.3f, 0f, true, 16, palette.spark(), 0.9f));
            }
        }

        if (look == FireballSpell.LOOK_PHOENIX && age < 4) {
            phoenixWings(level, at, palette, radius, age, toBlast);
        }

        if (age >= 4 && age % 2 == 0) {
            // Smoke rising from where it burned: heavy for cool fire and Meteor, a wisp for hot fire.
            int puffs = meteor ? 3 : palette.smoke() > 0 ? 2 : 1;
            for (int i = 0; i < puffs; i++) {
                Vec3 spot = at.add((random.nextDouble() - 0.5) * radius * 0.6, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * radius * 0.6);
                add(level, heat <= 2 || meteor ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                        spot, new Vec3(0, 0.04 + 0.03 * random.nextDouble(), 0));
            }
            if (meteor && age == 4) {
                add(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, floor.add(0, 0.2, 0), new Vec3(0, 0.08, 0));
            }
            add(level, ModContent.EMBER.get(), at.add((random.nextDouble() - 0.5) * radius, 0, (random.nextDouble() - 0.5) * radius), new Vec3(0, 0.04, 0));
        }
    }

    /** Chunks of the ground block blown up and out: 3D cubes that tumble, bounce and shrink away. */
    private static void debris(Level level, Vec3 floor, BlockPos pos, BlockState state, float radius, boolean meteor,
                               boolean sun, int heat, RandomSource random) {
        int count = meteor ? 18 : sun ? 14 : heat >= 4 ? Math.round(6 + 2 * radius) : Math.round(4 + 1.5f * radius);
        float minSize = meteor ? 0.2f : 0.12f;
        float maxSize = meteor ? 0.35f : sun ? 0.3f : 0.2f;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = radius * 0.4 * Math.sqrt(random.nextDouble());
            Vec3 spot = floor.add(Math.cos(angle) * distance, 0.15, Math.sin(angle) * distance);
            double out = (0.1 + 0.2 * random.nextDouble()) * Math.sqrt(radius);
            double up = meteor ? 0.45 + 0.25 * random.nextDouble() : 0.3 + 0.2 * random.nextDouble();
            Vec3 velocity = new Vec3(Math.cos(angle) * out, up, Math.sin(angle) * out);
            float size = minSize + random.nextFloat() * (maxSize - minSize);
            spawn(new DebrisParticle((ClientLevel) level, spot, velocity, state, pos, size));
        }
    }

    /** The fire texture an explosion's 3D shapes wear, by look. */
    private static ResourceLocation blastTexture(int look) {
        String name = switch (look) {
            case FireballSpell.LOOK_HEAT_1 -> "heat1";
            case FireballSpell.LOOK_HEAT_2, FireballSpell.LOOK_METEOR -> "heat2";
            case FireballSpell.LOOK_HEAT_4 -> "heat4";
            case FireballSpell.LOOK_SUN -> "sun";
            case FireballSpell.LOOK_PHOENIX -> "phoenix";
            default -> "heat3";
        };
        return ElementalArcana.id("block/fireball_" + name);
    }

    /** Adds a particle made here directly (the 3D shapes aren't sent or spawned by type). */
    private static void spawn(Particle particle) {
        Minecraft.getInstance().particleEngine.add(particle);
    }

    /** A flat ring of light racing out along the ground. */
    private static void shockwave(Level level, Vec3 at, Palette palette, float radius, int lifetime) {
        add(level, glow(ModContent.SHOCKWAVE.get(), palette.spark(), palette.fade(), radius, lifetime), at, Vec3.ZERO);
    }

    /** A Cluster Bomb bomblet going off: a sharp pop, a starburst of sparks and a tiny ring. */
    private static void firecracker(Level level, Vec3 at, Palette palette, float radius, RandomSource random) {
        add(level, glow(ModContent.FLARE.get(), 0xFFFFFF, palette.spark(), 0.8f, 2), at, Vec3.ZERO);
        spawn(new FireSphereParticle((ClientLevel) level, at, blastTexture(FireballSpell.LOOK_HEAT_3), radius * 0.3f, 0.4f, 4,
                palette.spark(), palette.fade(), 0.9f));
        for (int i = 0; i < 18; i++) {
            Vec3 velocity = randomDirection(random).scale(0.35 + 0.1 * random.nextDouble());
            int color = random.nextBoolean() ? 0xFFFFFF : palette.spark();
            add(level, glow(ModContent.SPARK.get(), color, palette.fade(), 0.09f, 7), at, velocity);
        }
        add(level, glow(ModContent.SHOCKWAVE.get(), palette.spark(), palette.fade(), radius * 0.7f, 5), at, Vec3.ZERO);
    }

    /** Phoenix's impact: two wings of flame sweep open across the viewer's screen, shedding feathers. */
    private static void phoenixWings(Level level, Vec3 at, Palette palette, float radius, int age, Vec3 toBlast) {
        Vec3 right = toBlast.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0e-4) {
            right = new Vec3(1, 0, 0);
        }
        right = right.normalize();
        int steps = 16;
        int perTick = steps / 4;
        for (int k = age * perTick; k < (age + 1) * perTick; k++) {
            float t = (k + 0.5f) / steps;
            for (int wing = -1; wing <= 1; wing += 2) {
                // Out and up in an arc, drooping at the tip.
                Vec3 point = at.add(right.scale(wing * t * 1.4 * radius)).add(0, Mth.sin(t * Mth.PI) * 0.6 * radius - t * 0.2 * radius, 0);
                Vec3 velocity = right.scale(wing * 0.04).add(0, 0.02, 0);
                add(level, glow(ModContent.FLARE.get(), palette.core(), palette.fade(), 0.35f + 0.35f * (1 - t), 12), point, velocity);
                if (k % 3 == 0) {
                    add(level, glow(ModContent.FEATHER.get(), 0xFFD25A, 0xB4142D, 0.16f, 30), point, velocity.scale(0.5));
                }
            }
        }
    }

    /** The ground right below a blast (within 1.5 blocks), or null in mid-air. */
    @Nullable
    private static BlockHitResult groundBelow(Level level, Vec3 at) {
        BlockHitResult hit = level.clip(new ClipContext(at.add(0, 0.3, 0), at.subtract(0, 1.5, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP ? hit : null;
    }

    /** The closest block face within reach of a blast that isn't over ground (a wall, a ceiling), or null. */
    @Nullable
    private static BlockHitResult surfaceNear(Level level, Vec3 at) {
        BlockHitResult best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            Vec3 toward = Vec3.atLowerCornerOf(direction.getNormal());
            BlockHitResult hit = level.clip(new ClipContext(at.subtract(toward.scale(0.2)), at.add(toward.scale(0.8)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceToSqr(at) < bestDistance) {
                bestDistance = hit.getLocation().distanceToSqr(at);
                best = hit;
            }
        }
        return best;
    }

    private static void add(Level level, ParticleOptions particle, Vec3 at, Vec3 velocity) {
        level.addParticle(particle, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
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
