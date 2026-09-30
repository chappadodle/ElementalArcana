package com.chappadodle.elementalarcana.compat.dynamiclights;

import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.content.ModContent;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;

/**
 * Spell projectiles as dynamic light sources, for LambDynamicLights and Sodium Dynamic Lights.
 * <p>
 * Both mods still read this older handler registration on 1.21.1 (Sodium Dynamic Lights is built on
 * it; LambDynamicLights bridges it into its current system), so one registration covers both. Only
 * called when one of them is loaded (see client/DynamicLights).
 */
@SuppressWarnings("removal")
public final class SpellLightHandlers {
    private SpellLightHandlers() {
    }

    public static void register() {
        DynamicLightHandlers.registerDynamicLightHandler(ModContent.SPELL_PROJECTILE.get(), projectile -> {
            ProjectileSpell spell = projectile.spell();
            return spell == null ? 0 : spell.luminance(projectile);
        });
    }
}
