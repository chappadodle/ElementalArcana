package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.visual.WindSlashRenderer;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Wind Blade's particles, all spawned on the client (see
 * docs/superpowers/specs/2026-09-30-wind-blade-vfx-design.md, step 2): wind curls tinted by look,
 * gusts, dust kicked up from the ground it skims, and glints. Branch signatures: the Boomerang
 * leaves a spiral wake, Tempest Edge dark swirls, Thousand Cuts sharp glints, and the Storm Scythe
 * sparks of lightning.
 */
final class WindBladeEffects {

    /** A look's particles: its wind tint (0xRRGGBB), glint colour and the colour glints fade to. */
    private record Palette(int tint, int glint, int fade) {
    }

    // Indexed by look, in WindBladeSpell's order.
    private static final List<Palette> PALETTES = List.of(
            new Palette(0xE8FFF4, 0xDFFFF0, 0x7FC8A8),
            new Palette(0xC8FFE8, 0xC8FFE0, 0x4FB88A),
            new Palette(0xA0FFD0, 0xB0FFD8, 0x2F9A70),
            new Palette(0xE0FFF0, 0xFFFFFF, 0x3FD69A),
            new Palette(0xD0FFD8, 0xE0FFE8, 0x5FC88A),
            new Palette(0x7AAAB0, 0xC8F0F0, 0x2A5A60),
            new Palette(0xFFFFF0, 0xFFFFF0, 0xA8E8C0),
            new Palette(0xB8C8E8, 0xE0F0FF, 0x3A4A70));

    /** The slash's size in blocks at scale 1, and where its tips sit (see WindSlashRenderer). */
    private static final double RADIUS = 0.6;
    private static final double TIP_SIDE = Math.sin(Math.toRadians(70)) * RADIUS;
    private static final double TIP_BACK = (Math.cos(Math.toRadians(70)) - 0.55) * RADIUS;

    private WindBladeEffects() {
    }

    private static int look(SpellProjectile blade) {
        return WindBladeSpell.look(blade.variant());
    }

    private static Palette palette(SpellProjectile blade) {
        return PALETTES.get(look(blade));
    }

    private static float size(SpellProjectile blade) {
        return Mth.lerp(blade.charge(0f), 0.35f, 1f) * blade.visualScale();
    }

    // ---- held ----

    /** Air drawn in as it forms: wind curls circling in toward the blade. Lighter for a big set. */
    static void held(SpellProjectile blade, float charge) {
        RandomSource random = blade.getRandom();
        int every = blade.formationCount() > 2 ? 3 : 2;
        if (charge < 1f && blade.tickCount % every == 0) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radius = 0.5 + 0.2 * random.nextDouble();
            Vec3 offset = new Vec3(Math.cos(angle) * radius, (random.nextDouble() - 0.5) * 0.3, Math.sin(angle) * radius);
            // Inward and around, so it curls in.
            Vec3 velocity = offset.scale(-0.12).add(new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(0.06));
            add(blade.level(), swirl(palette(blade)), blade.position().add(offset), velocity);
        }
        if (look(blade) == WindBladeSpell.LOOK_SCYTHE && random.nextFloat() < 0.3f) {
            blade.spawnParticleAround(ParticleTypes.ELECTRIC_SPARK, 0.6 * blade.visualScale(), Vec3.ZERO);
        }
    }

    /** Fully formed: a quick ring of wind snapping outward, and a glint. */
    static void grown(SpellProjectile blade) {
        Palette palette = palette(blade);
        Vec3 pos = blade.position();
        for (int i = 0; i < 10; i++) {
            double angle = Mth.TWO_PI * i / 10;
            Vec3 velocity = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(0.14);
            add(blade.level(), swirl(palette), pos, velocity);
        }
        add(blade.level(), glow(ModContent.FLARE.get(), palette.glint(), palette.fade(), 0.35f * size(blade), 3).anchoredTo(blade), pos, Vec3.ZERO);
    }

    // ---- thrown ----

    /** The throw: a small gust at the hand and a ring of wind pushed out around the throw's line. */
    static void released(SpellProjectile blade) {
        Level level = blade.level();
        Palette palette = palette(blade);
        Vec3 pos = blade.position();
        Vec3 forward = throwDirection(blade);
        add(level, ParticleTypes.SMALL_GUST, pos, Vec3.ZERO);
        Vec3 side = sideOf(forward);
        Vec3 up = side.cross(forward).normalize();
        int count = look(blade) == WindBladeSpell.LOOK_SCYTHE ? 16 : 8;
        for (int i = 0; i < count; i++) {
            double angle = Mth.TWO_PI * i / count;
            Vec3 out = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            add(level, swirl(palette), pos, out.scale(0.16).add(forward.scale(0.05)));
        }
    }

    /** In flight: wind peeling off both tips, dust kicked up from the ground it skims, and the look's signature. */
    static void flight(SpellProjectile blade) {
        Level level = blade.level();
        RandomSource random = blade.getRandom();
        Palette palette = palette(blade);
        float size = size(blade);
        Vec3 motion = blade.getDeltaMovement();
        if (motion.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 forward = motion.normalize();
        Vec3 back = motion.scale(-0.05);
        boolean child = blade.visualScale() < 1f && look(blade) == WindBladeSpell.LOOK_THOUSAND_CUTS;

        // Where its tips are this tick: across the blade (tilted by its fan angle), a little behind.
        Vec3 side = sideOf(forward);
        Vec3 up = side.cross(forward).normalize();
        float roll = WindSlashRenderer.fanAngle(blade.formationSlot(), blade.formationCount());
        // (The renderer's local x runs opposite to this side, hence the minus.)
        Vec3 across = side.scale(Math.cos(roll)).subtract(up.scale(Math.sin(roll)));
        Vec3 behind = forward.scale(TIP_BACK * size);
        for (int tip = -1; tip <= 1; tip += 2) {
            if (child && random.nextBoolean()) {
                continue;
            }
            Vec3 at = blade.position().add(across.scale(tip * TIP_SIDE * size)).add(behind);
            add(level, swirl(palette), at, back.add(across.scale(tip * 0.03)));
        }

        // Dust from the ground it skims.
        if (!child && blade.tickCount % 2 == 0) {
            @Nullable BlockHitResult ground = ImpactSurfaces.groundBelow(level, blade.position().subtract(0, 0.4, 0));
            if (ground != null) {
                BlockPos pos = ground.getBlockPos();
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    Vec3 at = ground.getLocation().add((random.nextDouble() - 0.5) * size, 0.05, (random.nextDouble() - 0.5) * size);
                    add(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at,
                            new Vec3(forward.x * 0.1 + (random.nextDouble() - 0.5) * 0.1, 0.12, forward.z * 0.1 + (random.nextDouble() - 0.5) * 0.1));
                }
            }
        }

        switch (look(blade)) {
            case WindBladeSpell.LOOK_BOOMERANG -> {
                // A spiral wake: two curls turning around its path.
                float angle = blade.tickCount * 0.8f;
                for (int arm = 0; arm < 2; arm++) {
                    float a = angle + Mth.PI * arm;
                    Vec3 at = blade.position().add(side.scale(Math.cos(a) * 0.35 * size)).add(up.scale(Math.sin(a) * 0.35 * size));
                    add(level, swirl(palette), at, back.scale(0.5));
                }
            }
            case WindBladeSpell.LOOK_TEMPEST_EDGE -> {
                if (random.nextFloat() < 0.6f) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    Vec3 around = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                    add(level, swirl(palette), blade.position().add(around.scale(0.3 * size)), around.scale(0.06).add(back));
                }
            }
            case WindBladeSpell.LOOK_THOUSAND_CUTS -> {
                if (random.nextFloat() < 0.5f) {
                    Vec3 at = blade.position().add(across.scale((random.nextDouble() - 0.5) * 2 * TIP_SIDE * size));
                    add(level, glow(ModContent.SPARK.get(), palette.glint(), palette.fade(), 0.07f, 5), at, forward.scale(0.05));
                }
            }
            case WindBladeSpell.LOOK_SCYTHE -> {
                for (int i = 0; i < 2; i++) {
                    Vec3 at = blade.position().add(across.scale((random.nextDouble() - 0.5) * 2 * TIP_SIDE * size));
                    add(level, ParticleTypes.ELECTRIC_SPARK, at, randomDirection(random).scale(0.08));
                }
                if (random.nextFloat() < 0.4f) {
                    add(level, glow(ModContent.SPARK.get(), 0xFFFFFF, palette.glint(), 0.12f, 4), blade.position(), randomDirection(random).scale(0.15));
                }
            }
            default -> {
                // The keener tiers shed the odd bright glint off the edge.
                if ((blade.variant() & WindBladeSpell.BRIGHT) != 0 && random.nextFloat() < 0.3f) {
                    Vec3 at = blade.position().add(across.scale((random.nextDouble() - 0.5) * 2 * TIP_SIDE * size));
                    add(level, glow(ModContent.SPARK.get(), palette.glint(), palette.fade(), 0.07f, 5), at, back);
                }
            }
        }
    }

    // ---- pieces ----

    private static ParticleOptions swirl(Palette palette) {
        return ColorParticleOption.create(ModContent.SWIRL.get(), 0xFF000000 | palette.tint());
    }

    private static Vec3 throwDirection(SpellProjectile blade) {
        Vec3 motion = blade.getDeltaMovement();
        if (motion.lengthSqr() > 1.0e-4) {
            return motion.normalize();
        }
        Entity owner = blade.getOwner();
        return owner != null ? owner.getLookAngle() : new Vec3(0, 0, 1);
    }

    /** A horizontal direction across {@code forward} (its right). */
    private static Vec3 sideOf(Vec3 forward) {
        Vec3 side = forward.cross(new Vec3(0, 1, 0));
        return side.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : side.normalize();
    }

    private static GlowParticleOptions glow(ParticleType<GlowParticleOptions> type, int color, int fade, float size, int lifetime) {
        return GlowParticleOptions.of(type, color, fade, size, lifetime);
    }

    private static void add(Level level, ParticleOptions particle, Vec3 at, Vec3 velocity) {
        level.addParticle(particle, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
    }

    private static Vec3 randomDirection(RandomSource random) {
        return new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
    }
}
