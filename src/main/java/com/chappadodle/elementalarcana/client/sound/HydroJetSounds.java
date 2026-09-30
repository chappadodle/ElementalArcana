package com.chappadodle.elementalarcana.client.sound;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.ArcanaClientConfig;
import com.chappadodle.elementalarcana.client.visual.WaterBeams;
import com.chappadodle.elementalarcana.content.HydroStreamOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.spell.HydroJetSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * The Hydro Jet's sounds, played on each client (see
 * docs/superpowers/specs/2026-09-30-hydro-jet-vfx-design.md, step 4): a pour when a stream starts,
 * a rushing-water loop at the hand (pitched up by Pressure Build) and a splashing loop where it
 * lands, both following the stream (PointLoopSound) until it stops; the Tsunami Lance's throw and
 * flight; the wave where it lands; and the whirlpool's churn. Signature sounds for the Tsunami
 * Lance and the whirlpool swap for vanilla mixes with ArcanaClientConfig#SIGNATURE_SOUNDS off.
 */
public final class HydroJetSounds {

    /** A stream look's sounds: its start, the loop at the hand, the loop where it lands. */
    private record StreamMix(List<SoundLayer> start, List<SoundLayer> rush, List<SoundLayer> splash) {
    }

    private static SoundLayer layer(SoundEvent sound, float volume, float pitch) {
        return SoundLayer.of(sound, volume, pitch);
    }

    private static final StreamMix LIGHT = new StreamMix(
            List.of(layer(SoundEvents.BUCKET_EMPTY, 1.0f, 1.3f), layer(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.5f, 1.4f)),
            List.of(layer(SoundEvents.WEATHER_RAIN, 0.35f, 1.6f)),
            List.of(layer(SoundEvents.GENERIC_SPLASH, 0.3f, 1.5f)));
    private static final StreamMix DEEP = new StreamMix(
            List.of(layer(SoundEvents.BUCKET_EMPTY, 1.0f, 1.1f), layer(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.6f, 1.2f)),
            List.of(layer(SoundEvents.WEATHER_RAIN, 0.35f, 1.3f), layer(SoundEvents.WATER_AMBIENT, 0.4f, 1.2f)),
            List.of(layer(SoundEvents.GENERIC_SPLASH, 0.35f, 1.3f)));
    private static final StreamMix TIDECUTTER = new StreamMix(
            List.of(layer(SoundEvents.BUCKET_EMPTY, 0.6f, 1.8f)),
            // A thin, high-pressure hiss.
            List.of(layer(SoundEvents.WEATHER_RAIN, 0.3f, 2.0f)),
            List.of(layer(SoundEvents.GENERIC_SPLASH, 0.15f, 1.9f)));
    private static final StreamMix TORRENT = new StreamMix(
            List.of(layer(SoundEvents.BUCKET_EMPTY, 1.0f, 0.8f), layer(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.8f, 0.9f)),
            List.of(layer(SoundEvents.WEATHER_RAIN, 0.6f, 1.0f), layer(SoundEvents.WATER_AMBIENT, 0.5f, 0.8f)),
            List.of(layer(SoundEvents.GENERIC_SPLASH, 0.5f, 1.0f)));
    private static final StreamMix MAELSTROM = new StreamMix(
            DEEP.start(),
            List.of(layer(SoundEvents.WEATHER_RAIN, 0.35f, 1.3f), layer(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 0.3f, 1.2f)),
            DEEP.splash());

    private static final List<SoundLayer> LANCE_THROW = List.of(
            layer(SoundEvents.TRIDENT_THROW.value(), 1.0f, 0.8f), layer(SoundEvents.TRIDENT_RIPTIDE_3.value(), 0.8f, 1.2f));
    private static final List<SoundLayer> LANCE_FLIGHT = List.of(layer(SoundEvents.WATER_AMBIENT, 0.5f, 1.0f));
    private static final List<SoundLayer> WAVE = List.of(
            layer(SoundEvents.GENERIC_SPLASH, 1.5f, 0.8f), layer(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.7f));
    private static final List<SoundLayer> WHIRLPOOL_START = List.of(layer(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.5f, 0.8f));
    private static final SoundLayer WHIRLPOOL_LOOP = layer(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.2f, 0.9f);

    /** The loops playing for each caster's stream. */
    private static final Map<Integer, List<SoundInstance>> STREAMS = new HashMap<>();

    private HydroJetSounds() {
    }

    private static boolean signature() {
        return ArcanaClientConfig.SIGNATURE_SOUNDS.get();
    }

    private static StreamMix mix(int variant) {
        return switch (HydroJetSpell.look(variant)) {
            case HydroJetSpell.LOOK_SPRING, HydroJetSpell.LOOK_CURRENT -> LIGHT;
            case HydroJetSpell.LOOK_TIDECUTTER -> TIDECUTTER;
            case HydroJetSpell.LOOK_TORRENT -> TORRENT;
            case HydroJetSpell.LOOK_MAELSTROM -> MAELSTROM;
            default -> DEEP;
        };
    }

    /** Each tick of a stream (see HydroStreamEmitter): the first one starts its sounds. */
    public static void stream(Level level, HydroStreamOptions stream, Vec3 start) {
        List<SoundInstance> loops = STREAMS.get(stream.caster());
        if (loops != null && loops.stream().anyMatch(loop -> Minecraft.getInstance().getSoundManager().isActive(loop))) {
            return;
        }
        int caster = stream.caster();
        StreamMix mix = mix(stream.look());
        SoundLayer.play(level, start, mix.start(), 1f, 1f);
        List<SoundInstance> started = new ArrayList<>();
        for (SoundLayer layer : mix.rush()) {
            // The rush rises in pitch as the pressure builds.
            started.add(new PointLoopSound(layer.sound(), layer.volume(), layer.pitch(),
                    () -> WaterBeams.start(caster), () -> 1f + 0.25f * WaterBeams.pressure(caster)));
        }
        for (SoundLayer layer : mix.splash()) {
            started.add(new PointLoopSound(layer.sound(), layer.volume(), layer.pitch(), () -> WaterBeams.end(caster), () -> 1f));
        }
        started.forEach(Minecraft.getInstance().getSoundManager()::play);
        STREAMS.put(caster, started);
    }

    /** Every tick of a Tsunami Lance in flight: its rush of water, and its throw the first time. */
    public static void lanceFlight(SpellProjectile lance) {
        if (SpellLoops.update(lance, LANCE_FLIGHT)) {
            List<SoundLayer> thrown = signature()
                    ? List.of(layer(ModContent.HYDRO_LANCE_SURGE.get(), 1.5f, 1.0f), layer(SoundEvents.TRIDENT_THROW.value(), 0.5f, 0.8f))
                    : LANCE_THROW;
            SoundLayer.play(lance.level(), lance.position(), thrown, 1f, 1f);
        }
    }

    /** A Tsunami Lance's wave crashing where it landed. */
    public static void wave(Level level, Vec3 at) {
        SoundLayer.play(level, at, signature() ? List.of(layer(ModContent.HYDRO_LANCE_CRASH.get(), 2.5f, 1.0f)) : WAVE, 1f, 1f);
    }

    /** A whirlpool opening at {@code at}, churning for as long as {@code alive} says. */
    public static void whirlpool(Level level, Vec3 at, BooleanSupplier alive) {
        SoundLayer.play(level, at, WHIRLPOOL_START, 1f, 1f);
        SoundLayer loop = signature() ? layer(ModContent.HYDRO_MAELSTROM_SWIRL.get(), 1.3f, 1.0f) : WHIRLPOOL_LOOP;
        Minecraft.getInstance().getSoundManager().play(new PointLoopSound(loop.sound(), loop.volume(), loop.pitch(),
                () -> alive.getAsBoolean() ? at : null, () -> 1f));
    }
}
