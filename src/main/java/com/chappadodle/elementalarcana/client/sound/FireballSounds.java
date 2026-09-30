package com.chappadodle.elementalarcana.client.sound;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.ArcanaClientConfig;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.spell.FireballSpell;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The fireball's sounds, played on each client from the same hooks as its particles (see
 * FireballEffects): a mix of layered vanilla sounds per look for each moment (conjured, held,
 * fully grown, thrown, in flight, exploding). Held and flight sounds are loops that follow the
 * fireball (ProjectileLoopSound). See docs/superpowers/specs/2026-09-29-fireball-vfx-design.md,
 * step 4.
 */
public final class FireballSounds {

    /** Everything a look sounds like. */
    private record Mix(List<SoundLayer> conjure, List<SoundLayer> heldLoop, List<SoundLayer> grown, List<SoundLayer> thrown,
                       List<SoundLayer> flightLoop, List<SoundLayer> blast) {
    }

    private static SoundLayer layer(SoundEvent sound, float volume, float pitch) {
        return SoundLayer.of(sound, volume, pitch);
    }

    private static final SoundEvent EXPLODE = SoundEvents.GENERIC_EXPLODE.value();

    private static final Mix HEAT_LOW = new Mix(
            List.of(layer(SoundEvents.FLINTANDSTEEL_USE, 0.8f, 1.0f), layer(SoundEvents.FIRECHARGE_USE, 0.35f, 1.3f)),
            List.of(layer(SoundEvents.BLAZE_BURN, 0.25f, 1.2f)),
            List.of(layer(SoundEvents.FIRECHARGE_USE, 0.6f, 1.3f)),
            List.of(layer(SoundEvents.BLAZE_SHOOT, 0.8f, 1.1f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.25f, 1.5f)),
            List.of(layer(EXPLODE, 0.6f, 1.5f), layer(SoundEvents.FIRECHARGE_USE, 0.6f, 0.8f)));
    private static final Mix HEAT_HIGH = new Mix(
            List.of(layer(SoundEvents.FLINTANDSTEEL_USE, 0.7f, 0.9f), layer(SoundEvents.FIRECHARGE_USE, 0.5f, 1.0f)),
            List.of(layer(SoundEvents.BLAZE_BURN, 0.45f, 0.9f)),
            List.of(layer(SoundEvents.FIRECHARGE_USE, 0.6f, 1.1f), layer(SoundEvents.BLAZE_SHOOT, 0.25f, 1.5f)),
            List.of(layer(SoundEvents.BLAZE_SHOOT, 0.9f, 1.0f), layer(SoundEvents.GHAST_SHOOT, 0.35f, 1.2f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.35f, 1.3f), layer(SoundEvents.BLAZE_BURN, 0.3f, 0.9f)),
            List.of(layer(EXPLODE, 1.1f, 1.05f), layer(SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 0.8f)));
    private static final Mix CLUSTER = new Mix(
            List.of(layer(SoundEvents.FLINTANDSTEEL_USE, 0.7f, 1.0f), layer(SoundEvents.TNT_PRIMED, 0.25f, 1.6f)),
            List.of(layer(SoundEvents.TNT_PRIMED, 0.2f, 1.4f)),
            List.of(layer(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.4f, 1.4f)),
            List.of(layer(SoundEvents.SNOWBALL_THROW, 0.8f, 0.6f), layer(SoundEvents.BLAZE_SHOOT, 0.4f, 1.3f)),
            List.of(layer(SoundEvents.TNT_PRIMED, 0.25f, 1.5f)),
            List.of(layer(EXPLODE, 1.0f, 1.1f), layer(SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 0.8f)));
    private static final Mix METEOR = new Mix(
            List.of(layer(SoundEvents.FIRECHARGE_USE, 0.5f, 0.8f), layer(SoundEvents.GRINDSTONE_USE, 0.4f, 0.6f)),
            List.of(layer(SoundEvents.BLAZE_BURN, 0.45f, 0.6f)),
            List.of(layer(SoundEvents.FIRECHARGE_USE, 0.6f, 0.8f)),
            List.of(layer(SoundEvents.GHAST_SHOOT, 0.7f, 0.7f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.6f, 0.8f), layer(SoundEvents.BLAZE_BURN, 0.5f, 0.5f)),
            List.of(layer(EXPLODE, 3.0f, 0.6f), layer(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.5f, 0.5f),
                    layer(SoundEvents.DEEPSLATE_BREAK, 1.2f, 0.6f)));
    private static final Mix SUN = new Mix(
            List.of(),
            List.of(layer(SoundEvents.BLAZE_BURN, 0.8f, 0.5f)),
            List.of(),
            List.of(layer(SoundEvents.GHAST_SHOOT, 1.2f, 0.6f), layer(SoundEvents.BLAZE_SHOOT, 1.0f, 0.7f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.7f, 0.7f), layer(SoundEvents.BLAZE_BURN, 0.9f, 0.45f)),
            List.of(layer(EXPLODE, 4.0f, 0.5f), layer(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 2.5f, 0.6f),
                    layer(SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0f, 0.8f)));
    private static final Mix PHOENIX = new Mix(
            List.of(layer(SoundEvents.FIRECHARGE_USE, 0.4f, 1.2f), layer(SoundEvents.PHANTOM_FLAP, 0.4f, 1.6f)),
            List.of(layer(SoundEvents.BLAZE_BURN, 0.4f, 1.0f)),
            List.of(layer(SoundEvents.FIRECHARGE_USE, 0.6f, 1.3f)),
            List.of(layer(SoundEvents.BLAZE_SHOOT, 0.8f, 1.1f), layer(SoundEvents.PHANTOM_SWOOP, 0.6f, 1.3f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.35f, 1.2f), layer(SoundEvents.PHANTOM_FLAP, 0.4f, 1.4f)),
            List.of(layer(EXPLODE, 1.0f, 1.1f), layer(SoundEvents.PHANTOM_AMBIENT, 0.8f, 1.5f)));
    // With signature sounds on (ArcanaClientConfig#SIGNATURE_SOUNDS), the synthesized ones take over
    // the biggest moments; the vanilla mixes above stay as the alternative.
    private static final Mix SUN_SIGNATURE = new Mix(
            List.of(),
            List.of(layer(ModContent.FIREBALL_SUN_HUM.get(), 0.9f, 1.0f), layer(SoundEvents.BLAZE_BURN, 0.35f, 0.5f)),
            List.of(),
            List.of(layer(ModContent.FIREBALL_SUN_LAUNCH.get(), 1.5f, 1.0f), layer(SoundEvents.BLAZE_SHOOT, 0.5f, 0.7f)),
            List.of(layer(ModContent.FIREBALL_SUN_ROAR.get(), 1.2f, 1.0f), layer(SoundEvents.ELYTRA_FLYING, 0.4f, 0.7f)),
            List.of(layer(ModContent.FIREBALL_SUN_BLAST.get(), 4.0f, 1.0f), layer(EXPLODE, 2.0f, 0.5f)));
    private static final Mix METEOR_SIGNATURE = new Mix(
            METEOR.conjure(), METEOR.heldLoop(), METEOR.grown(), METEOR.thrown(),
            List.of(layer(ModContent.FIREBALL_METEOR_ROAR.get(), 1.0f, 1.0f), layer(SoundEvents.ELYTRA_FLYING, 0.4f, 0.8f)),
            List.of(layer(ModContent.FIREBALL_METEOR_IMPACT.get(), 3.0f, 1.0f), layer(EXPLODE, 1.5f, 0.6f)));
    /** A Cluster Bomb bomblet going off: firecracker pops. */
    private static final List<SoundLayer> BOMBLET_BLAST = List.of(
            layer(SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.2f), layer(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.5f, 1.1f));

    private FireballSounds() {
    }

    private static Mix mix(int look) {
        boolean signature = ArcanaClientConfig.SIGNATURE_SOUNDS.get();
        return switch (look) {
            case FireballSpell.LOOK_HEAT_1, FireballSpell.LOOK_HEAT_2 -> HEAT_LOW;
            case FireballSpell.LOOK_CLUSTER -> CLUSTER;
            case FireballSpell.LOOK_METEOR -> signature ? METEOR_SIGNATURE : METEOR;
            case FireballSpell.LOOK_SUN -> signature ? SUN_SIGNATURE : SUN;
            case FireballSpell.LOOK_PHOENIX -> PHOENIX;
            default -> HEAT_HIGH;
        };
    }

    /** Every tick while held: the held loop (restarted if its look changed); the conjure sound the first time. */
    public static void held(SpellProjectile fireball) {
        boolean first = SpellLoops.lastLook(fireball) < 0;
        if (SpellLoops.update(fireball, mix(fireball.variant()).heldLoop()) && first) {
            SoundLayer.play(fireball.level(), fireball.position(), mix(fireball.variant()).conjure(), 1f, 1f);
        }
    }

    /** Every tick in flight: the flight loop; the throw sound when it's just been thrown or shot. */
    public static void flight(SpellProjectile fireball) {
        if (SpellLoops.update(fireball, mix(fireball.variant()).flightLoop()) && !isBomblet(fireball)) {
            SoundLayer.play(fireball.level(), fireball.position(), mix(fireball.variant()).thrown(), 1f, 1f);
        }
    }

    public static void grown(SpellProjectile fireball) {
        SoundLayer.play(fireball.level(), fireball.position(), mix(fireball.variant()).grown(), 1f, 1f);
    }

    /** An explosion (see FireBlastEmitter). */
    public static void blast(Level level, Vec3 at, int look, boolean bomblet) {
        SoundLayer.play(level, at, bomblet ? BOMBLET_BLAST : mix(look).blast(), 1f, 1f);
    }

    private static boolean isBomblet(SpellProjectile fireball) {
        return fireball.variant() == FireballSpell.LOOK_CLUSTER && fireball.visualScale() < 0.5f;
    }
}
