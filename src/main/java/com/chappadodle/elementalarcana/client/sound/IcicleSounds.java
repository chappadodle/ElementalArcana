package com.chappadodle.elementalarcana.client.sound;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.ArcanaClientConfig;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.spell.IcicleSpell;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The icicle's sounds, played on each client from the same hooks as its particles (see
 * IcicleEffects): layered vanilla sounds per look (amethyst for forming, glass and ice for
 * shattering) around the synthesized shatter, a crystalline hum while held and a whistle in flight
 * (loops that follow it, see SpellLoops), and signature sounds for the Glacial Lance and Endless
 * Winter (swapped for vanilla mixes with ArcanaClientConfig#SIGNATURE_SOUNDS off). See
 * docs/superpowers/specs/2026-09-30-icicle-vfx-design.md, step 4.
 */
public final class IcicleSounds {

    /** Everything a look sounds like (the shatter's own crack, ICICLE_IMPACT, is always under it). */
    private record Mix(List<SoundLayer> conjure, List<SoundLayer> heldLoop, List<SoundLayer> grown, List<SoundLayer> thrown,
                       List<SoundLayer> flightLoop, List<SoundLayer> shatter) {
    }

    private static SoundLayer layer(SoundEvent sound, float volume, float pitch) {
        return SoundLayer.of(sound, volume, pitch);
    }

    private static final Mix FROST_LOW = new Mix(
            List.of(layer(SoundEvents.AMETHYST_CLUSTER_PLACE, 1.0f, 1.3f), layer(SoundEvents.POWDER_SNOW_PLACE, 0.5f, 1.2f)),
            List.of(layer(SoundEvents.BEACON_AMBIENT, 0.15f, 1.8f)),
            List.of(layer(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.6f)),
            List.of(layer(SoundEvents.TRIDENT_THROW.value(), 0.7f, 1.5f), layer(SoundEvents.AMETHYST_BLOCK_HIT, 0.4f, 1.6f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.2f, 1.8f)),
            List.of(layer(SoundEvents.GLASS_BREAK, 0.3f, 1.6f)));
    private static final Mix FROST_HIGH = new Mix(
            List.of(layer(SoundEvents.AMETHYST_CLUSTER_PLACE, 1.0f, 1.1f), layer(SoundEvents.AMETHYST_BLOCK_CHIME, 0.4f, 1.8f)),
            List.of(layer(SoundEvents.BEACON_AMBIENT, 0.2f, 1.6f)),
            List.of(layer(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f), layer(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4f, 1.8f)),
            List.of(layer(SoundEvents.TRIDENT_THROW.value(), 0.8f, 1.4f), layer(SoundEvents.AMETHYST_BLOCK_HIT, 0.5f, 1.4f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.3f, 1.6f)),
            List.of(layer(SoundEvents.GLASS_BREAK, 0.5f, 1.3f), layer(SoundEvents.AMETHYST_BLOCK_BREAK, 0.5f, 1.4f)));
    private static final Mix PIERCING = new Mix(
            FROST_HIGH.conjure(), FROST_HIGH.heldLoop(), FROST_HIGH.grown(),
            List.of(layer(SoundEvents.TRIDENT_THROW.value(), 1.0f, 1.8f), layer(SoundEvents.ARROW_SHOOT, 0.5f, 1.6f)),
            // A thin, high whistle.
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.35f, 2.0f)),
            List.of(layer(SoundEvents.GLASS_BREAK, 0.4f, 1.6f)));
    private static final Mix SHATTER = new Mix(
            FROST_HIGH.conjure(), FROST_HIGH.heldLoop(),
            List.of(layer(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f), layer(SoundEvents.GLASS_HIT, 0.4f, 1.5f)),
            FROST_HIGH.thrown(), FROST_HIGH.flightLoop(),
            List.of(layer(SoundEvents.GLASS_BREAK, 0.8f, 1.0f), layer(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.7f, 1.2f)));
    private static final Mix WINTER = new Mix(
            List.of(layer(SoundEvents.POWDER_SNOW_PLACE, 1.0f, 0.8f), layer(SoundEvents.AMETHYST_CLUSTER_PLACE, 0.6f, 1.0f)),
            // Crystal hum under a low wind.
            List.of(layer(SoundEvents.BEACON_AMBIENT, 0.15f, 1.3f), layer(SoundEvents.ELYTRA_FLYING, 0.15f, 0.6f)),
            FROST_HIGH.grown(),
            List.of(layer(SoundEvents.TRIDENT_THROW.value(), 0.8f, 1.2f), layer(SoundEvents.POWDER_SNOW_BREAK, 0.6f, 0.9f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.4f, 0.7f)),
            List.of(layer(SoundEvents.POWDER_SNOW_BREAK, 1.0f, 0.8f), layer(SoundEvents.GLASS_BREAK, 0.4f, 1.2f)));
    private static final Mix LANCE = new Mix(
            List.of(),
            List.of(layer(SoundEvents.BEACON_AMBIENT, 0.5f, 1.0f)),
            List.of(),
            List.of(layer(SoundEvents.TRIDENT_THROW.value(), 1.2f, 0.8f), layer(SoundEvents.TRIDENT_RIPTIDE_3.value(), 0.6f, 1.2f)),
            List.of(layer(SoundEvents.ELYTRA_FLYING, 0.5f, 1.0f)),
            List.of(layer(SoundEvents.GLASS_BREAK, 1.5f, 0.6f), layer(SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 1.4f),
                    layer(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5f, 0.7f)));

    // With signature sounds on, the synthesized ones take over; the vanilla mixes above stay as the alternative.
    private static final Mix LANCE_SIGNATURE = new Mix(
            LANCE.conjure(), LANCE.heldLoop(), LANCE.grown(),
            List.of(layer(ModContent.ICICLE_LANCE_LAUNCH.get(), 1.5f, 1.0f), layer(SoundEvents.TRIDENT_THROW.value(), 0.6f, 0.8f)),
            LANCE.flightLoop(),
            List.of(layer(ModContent.ICICLE_LANCE_QUAKE.get(), 3.0f, 1.0f)));
    private static final Mix WINTER_SIGNATURE = new Mix(
            WINTER.conjure(),
            List.of(layer(SoundEvents.BEACON_AMBIENT, 0.15f, 1.3f), layer(ModContent.ICICLE_WINTER_BLIZZARD.get(), 0.4f, 1.0f)),
            WINTER.grown(), WINTER.thrown(),
            List.of(layer(ModContent.ICICLE_WINTER_BLIZZARD.get(), 0.9f, 1.0f), layer(SoundEvents.ELYTRA_FLYING, 0.3f, 0.7f)),
            WINTER.shatter());

    /** A full set fusing into the Glacial Lance. */
    private static final List<SoundLayer> FORGE = List.of(
            layer(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.6f), layer(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.8f));
    private static final List<SoundLayer> FORGE_SIGNATURE = List.of(
            layer(ModContent.ICICLE_LANCE_FORGE.get(), 1.5f, 1.0f), layer(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f, 0.6f));

    private IcicleSounds() {
    }

    private static boolean signature() {
        return ArcanaClientConfig.SIGNATURE_SOUNDS.get();
    }

    private static Mix mix(int variant) {
        return switch (IcicleSpell.look(variant)) {
            case IcicleSpell.LOOK_FROST_1, IcicleSpell.LOOK_FROST_2 -> FROST_LOW;
            case IcicleSpell.LOOK_PIERCING -> PIERCING;
            case IcicleSpell.LOOK_SHATTER -> SHATTER;
            case IcicleSpell.LOOK_WINTER -> signature() ? WINTER_SIGNATURE : WINTER;
            case IcicleSpell.LOOK_LANCE -> signature() ? LANCE_SIGNATURE : LANCE;
            default -> FROST_HIGH;
        };
    }

    /**
     * Every tick while held: the hum (restarted if its look changed); the conjure sound the first
     * time, and the forging sound when a set fuses into the Glacial Lance.
     */
    public static void held(SpellProjectile icicle) {
        int before = SpellLoops.lastLook(icicle);
        if (!SpellLoops.update(icicle, mix(icicle.variant()).heldLoop())) {
            return;
        }
        if (before < 0) {
            SoundLayer.play(icicle.level(), icicle.position(), mix(icicle.variant()).conjure(), 1f, 1f);
        } else if (IcicleSpell.look(before) != IcicleSpell.LOOK_LANCE && IcicleSpell.look(icicle.variant()) == IcicleSpell.LOOK_LANCE) {
            SoundLayer.play(icicle.level(), icicle.position(), signature() ? FORGE_SIGNATURE : FORGE, 1f, 1f);
        }
    }

    /** Every tick in flight: the whistle; the throw sound when it's just been thrown or shot (not shrapnel). */
    public static void flight(SpellProjectile icicle) {
        boolean shrapnel = icicle.visualScale() < 0.5f;
        if (SpellLoops.update(icicle, shrapnel ? List.of() : mix(icicle.variant()).flightLoop()) && !shrapnel) {
            // Quieter when a whole volley goes at once.
            SoundLayer.play(icicle.level(), icicle.position(), mix(icicle.variant()).thrown(), icicle.formationCount() > 1 ? 0.6f : 1f, 1f);
        }
    }

    public static void grown(SpellProjectile icicle) {
        SoundLayer.play(icicle.level(), icicle.position(), mix(icicle.variant()).grown(), 1f, 1f);
    }

    /**
     * A shatter (see IceShatterEmitter): the synthesized crack, bigger and deeper for bigger and
     * fuller icicles, with the look's layers over it. Shrapnel only tinkles.
     */
    public static void shatter(Level level, Vec3 at, int variant, float size, float charge) {
        float volume = Math.min(2f, (0.7f + 0.4f * charge) * size);
        float pitch = (1.15f - 0.25f * charge) / (float) Math.sqrt(size) + (level.getRandom().nextFloat() - 0.5f) * 0.1f;
        level.playLocalSound(at.x, at.y, at.z, ModContent.ICICLE_IMPACT.get(), SoundSource.PLAYERS, volume, pitch, false);
        if (size >= 0.5f) {
            SoundLayer.play(level, at, mix(variant).shatter(), Math.min(1.5f, 0.6f + 0.4f * charge), 1f);
        }
    }
}
