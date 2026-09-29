package com.chappadodle.elementalarcana.client.sound;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.ArcanaClientConfig;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.spell.FireballSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The fireball's sounds, played on each client from the same hooks as its particles (see
 * FireballEffects): a mix of layered vanilla sounds per look for each moment (conjured, held,
 * fully grown, thrown, in flight, exploding). Held and flight sounds are loops that follow the
 * fireball (ProjectileLoopSound). See docs/superpowers/specs/2026-09-29-fireball-vfx-design.md,
 * step 4.
 */
public final class FireballSounds {

    /** One sound in a mix, at its volume and pitch. */
    private record Layer(SoundEvent sound, float volume, float pitch) {
    }

    /** Everything a look sounds like. */
    private record Mix(List<Layer> conjure, List<Layer> heldLoop, List<Layer> grown, List<Layer> thrown,
                       List<Layer> flightLoop, List<Layer> blast) {
    }

    private static Layer layer(SoundEvent sound, float volume, float pitch) {
        return new Layer(sound, volume, pitch);
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
    private static final List<Layer> BOMBLET_BLAST = List.of(
            layer(SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.2f), layer(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.5f, 1.1f));

    /** Loops playing for each fireball, and what they were started for. */
    private record Playing(boolean held, int look, List<ProjectileLoopSound> loops) {
    }

    private static final Map<SpellProjectile, Playing> PLAYING = new WeakHashMap<>();

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
        Playing before = PLAYING.get(fireball);
        if (update(fireball, mix(fireball.variant()).heldLoop()) && before == null) {
            play(fireball.level(), fireball.position(), mix(fireball.variant()).conjure(), 1f);
        }
    }

    /** Every tick in flight: the flight loop; the throw sound when it's just been thrown or shot. */
    public static void flight(SpellProjectile fireball) {
        if (update(fireball, mix(fireball.variant()).flightLoop()) && !isBomblet(fireball)) {
            play(fireball.level(), fireball.position(), mix(fireball.variant()).thrown(), 1f);
        }
    }

    public static void grown(SpellProjectile fireball) {
        play(fireball.level(), fireball.position(), mix(fireball.variant()).grown(), 1f);
    }

    /** An explosion (see FireBlastEmitter). */
    public static void blast(Level level, Vec3 at, int look, boolean bomblet) {
        play(level, at, bomblet ? BOMBLET_BLAST : mix(look).blast(), 1f);
    }

    /** Starts the right loops if the fireball's phase or look changed; true if it did. */
    private static boolean update(SpellProjectile fireball, List<Layer> layers) {
        Playing playing = PLAYING.get(fireball);
        if (playing != null && playing.held() == fireball.isHeld() && playing.look() == fireball.variant()) {
            return false;
        }
        if (playing != null) {
            playing.loops().forEach(ProjectileLoopSound::end);
        }
        List<ProjectileLoopSound> loops = new ArrayList<>();
        for (Layer layer : layers) {
            ProjectileLoopSound loop = new ProjectileLoopSound(fireball, layer.sound(), layer.volume(), layer.pitch());
            Minecraft.getInstance().getSoundManager().play(loop);
            loops.add(loop);
        }
        PLAYING.put(fireball, new Playing(fireball.isHeld(), fireball.variant(), loops));
        return true;
    }

    private static boolean isBomblet(SpellProjectile fireball) {
        return fireball.variant() == FireballSpell.LOOK_CLUSTER && fireball.visualScale() < 0.5f;
    }

    private static void play(Level level, Vec3 at, List<Layer> layers, float volumeScale) {
        RandomSource random = level.getRandom();
        for (Layer layer : layers) {
            float pitch = layer.pitch() * (0.95f + random.nextFloat() * 0.1f);
            level.playLocalSound(at.x, at.y, at.z, layer.sound(), SoundSource.PLAYERS, layer.volume() * volumeScale, pitch, false);
        }
    }
}
