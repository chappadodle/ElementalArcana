package com.chappadodle.elementalarcana.client.sound;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.ArcanaClientConfig;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.WindCutOptions;
import com.chappadodle.elementalarcana.content.spell.WindBladeSpell;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Wind Blade's sounds, played on each client from the same hooks as its particles (see
 * WindBladeEffects): layered vanilla breeze and wind-charge sounds per look, a soft wind while held
 * and a rushing wind in flight (loops that follow it, see SpellLoops), and signature sounds for the
 * Storm Scythe and Thousand Cuts (swapped for vanilla mixes with ArcanaClientConfig#SIGNATURE_SOUNDS
 * off). See docs/superpowers/specs/2026-09-30-wind-blade-vfx-design.md, step 4.
 */
public final class WindBladeSounds {

    /** Everything a look sounds like (the wind-charge burst, sized by the blade, is always under its impact). */
    private record Mix(List<SoundLayer> conjure, List<SoundLayer> heldLoop, List<SoundLayer> grown, List<SoundLayer> thrown,
                       List<SoundLayer> flightLoop, List<SoundLayer> impact) {
    }

    private static SoundLayer layer(SoundEvent sound, float volume, float pitch) {
        return SoundLayer.of(sound, volume, pitch);
    }

    private static final Mix LOW = new Mix(
            List.of(layer(SoundEvents.BREEZE_INHALE, 0.8f, 1.3f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.12f, 1.6f)),
            List.of(layer(SoundEvents.BREEZE_CHARGE, 0.6f, 1.4f)),
            List.of(layer(SoundEvents.BREEZE_SHOOT, 0.9f, 1.2f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.3f, 1.5f)),
            List.of());
    private static final Mix HIGH = new Mix(
            List.of(layer(SoundEvents.BREEZE_INHALE, 0.9f, 1.1f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.18f, 1.4f)),
            List.of(layer(SoundEvents.BREEZE_CHARGE, 0.7f, 1.2f)),
            List.of(layer(SoundEvents.BREEZE_SHOOT, 1.0f, 1.1f), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f, 1.4f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.35f, 1.3f)),
            List.of(layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.2f)));
    private static final Mix BOOMERANG = new Mix(
            HIGH.conjure(), HIGH.heldLoop(), HIGH.grown(), HIGH.thrown(),
            // A whirring as it turns.
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.3f, 1.4f), layer(SoundEvents.BREEZE_WHIRL, 0.25f, 1.6f)),
            HIGH.impact());
    private static final Mix TEMPEST_EDGE = new Mix(
            List.of(layer(SoundEvents.BREEZE_INHALE, 0.9f, 0.9f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.2f, 0.9f)),
            List.of(layer(SoundEvents.BREEZE_CHARGE, 0.7f, 1.0f)),
            List.of(layer(SoundEvents.BREEZE_SHOOT, 1.0f, 0.9f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.35f, 1.0f)),
            List.of(layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f, 0.9f)));
    private static final Mix THOUSAND_CUTS = new Mix(
            HIGH.conjure(), HIGH.heldLoop(), HIGH.grown(),
            List.of(layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.6f), layer(SoundEvents.BREEZE_SHOOT, 0.6f, 1.5f)),
            // A thin, high whistle.
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.25f, 1.9f)),
            List.of(layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f, 1.8f)));
    private static final Mix SCYTHE = new Mix(
            List.of(),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.3f, 0.6f), layer(SoundEvents.BREEZE_WHIRL, 0.4f, 0.7f)),
            List.of(),
            List.of(layer(SoundEvents.BREEZE_SHOOT, 1.3f, 0.6f), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.6f, 0.7f)),
            List.of(layer(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5f, 1.2f), layer(SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.5f)));

    // With signature sounds on, the synthesized ones take over; the vanilla mixes above stay as the alternative.
    private static final Mix SCYTHE_SIGNATURE = new Mix(
            SCYTHE.conjure(), SCYTHE.heldLoop(), SCYTHE.grown(), SCYTHE.thrown(),
            List.of(layer(ModContent.WIND_SCYTHE_ROAR.get(), 1.0f, 1.0f), layer(SoundEvents.ELYTRA_FLYING, 0.3f, 0.7f)),
            List.of(layer(ModContent.WIND_SCYTHE_STRIKE.get(), 2.5f, 1.0f)));
    private static final Mix THOUSAND_CUTS_SIGNATURE = new Mix(
            THOUSAND_CUTS.conjure(), THOUSAND_CUTS.heldLoop(), THOUSAND_CUTS.grown(),
            List.of(layer(ModContent.WIND_THOUSAND_SLASH.get(), 1.0f, 1.0f), layer(SoundEvents.BREEZE_SHOOT, 0.5f, 1.5f)),
            THOUSAND_CUTS.flightLoop(),
            List.of(layer(ModContent.WIND_THOUSAND_SLASH.get(), 0.7f, 1.3f)));

    /** A fanned set merging into the Storm Scythe. */
    private static final List<SoundLayer> MERGE = List.of(layer(SoundEvents.BREEZE_WHIRL, 1.2f, 0.7f));

    private WindBladeSounds() {
    }

    private static boolean signature() {
        return ArcanaClientConfig.SIGNATURE_SOUNDS.get();
    }

    private static Mix mix(int variant) {
        return switch (WindBladeSpell.look(variant)) {
            case WindBladeSpell.LOOK_GUST, WindBladeSpell.LOOK_BREEZE -> LOW;
            case WindBladeSpell.LOOK_BOOMERANG -> BOOMERANG;
            case WindBladeSpell.LOOK_TEMPEST_EDGE -> TEMPEST_EDGE;
            case WindBladeSpell.LOOK_THOUSAND_CUTS -> signature() ? THOUSAND_CUTS_SIGNATURE : THOUSAND_CUTS;
            case WindBladeSpell.LOOK_SCYTHE -> signature() ? SCYTHE_SIGNATURE : SCYTHE;
            default -> HIGH;
        };
    }

    /**
     * Every tick while held: the soft wind (restarted if its look changed); the conjure sound the
     * first time, and the whirl when a set merges into the Storm Scythe.
     */
    public static void held(SpellProjectile blade) {
        int before = SpellLoops.lastLook(blade);
        if (!SpellLoops.update(blade, mix(blade.variant()).heldLoop())) {
            return;
        }
        if (before < 0) {
            SoundLayer.play(blade.level(), blade.position(), mix(blade.variant()).conjure(), 1f, 1f);
        } else if (WindBladeSpell.look(before) != WindBladeSpell.LOOK_SCYTHE && WindBladeSpell.look(blade.variant()) == WindBladeSpell.LOOK_SCYTHE) {
            SoundLayer.play(blade.level(), blade.position(), MERGE, 1f, 1f);
        }
    }

    /** Every tick in flight: the rushing wind; the throw sound when it's just been thrown or shot. */
    public static void flight(SpellProjectile blade) {
        if (SpellLoops.update(blade, mix(blade.variant()).flightLoop())) {
            // Quieter when a whole volley goes at once, and for Thousand Cuts' split blades.
            float volume = blade.formationCount() > 1 || blade.visualScale() < 1f ? 0.6f : 1f;
            SoundLayer.play(blade.level(), blade.position(), mix(blade.variant()).thrown(), volume, 1f);
        }
    }

    public static void grown(SpellProjectile blade) {
        SoundLayer.play(blade.level(), blade.position(), mix(blade.variant()).grown(), 1f, 1f);
    }

    /** An impact (see WindCutEmitter): the wind-charge burst, sized by the blade, with the look's layers. */
    public static void impact(Level level, Vec3 at, WindCutOptions cut) {
        float size = cut.size();
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS,
                Math.min(1.5f, 0.5f * size), 1.4f / (float) Math.sqrt(size), false);
        SoundLayer.play(level, at, mix(cut.look()).impact(), 1f, 1f);
    }
}
