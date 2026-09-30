package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.compat.dynamiclights.ArcanaLightsInitializer;
import com.chappadodle.elementalarcana.compat.dynamiclights.SpellLightHandlers;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * Dynamic lights (optional): with LambDynamicLights or Sodium Dynamic Lights installed, spell
 * projectiles light up what's around them (ProjectileSpell#luminance), and with LambDynamicLights,
 * explosions flash light too. Without either mod, nothing here touches their code.
 */
public final class DynamicLights {
    private static final boolean LAMBDYNAMICLIGHTS = ModList.get().isLoaded("lambdynlights");
    private static final boolean SODIUM_DYNAMIC_LIGHTS = ModList.get().isLoaded("sodiumdynamiclights");

    private DynamicLights() {
    }

    /** At client setup: make spell projectiles light sources. */
    static void init() {
        if (LAMBDYNAMICLIGHTS || SODIUM_DYNAMIC_LIGHTS) {
            SpellLightHandlers.register();
        }
    }

    /** An explosion's burst of light, fading over {@code ticks} (LambDynamicLights only). */
    public static void flash(Vec3 at, int luminance, int ticks) {
        if (LAMBDYNAMICLIGHTS) {
            ArcanaLightsInitializer.flash(at, luminance, ticks);
        }
    }
}
