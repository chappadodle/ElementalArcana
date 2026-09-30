package com.chappadodle.elementalarcana.compat.dynamiclights;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * LambDynamicLights calls this when it starts (declared as its "lambdynlights:initializer"
 * entrypoint in neoforge.mods.toml). It hands over the manager for custom light sources, which
 * explosion flashes use (see BlastLight). Sodium Dynamic Lights has no equivalent, so there only
 * the fireballs themselves give light.
 */
public class ArcanaLightsInitializer implements DynamicLightsInitializer {
    @Nullable
    private static DynamicLightBehaviorManager behaviors;

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        behaviors = context.dynamicLightBehaviorManager();
    }

    /** Only called by versions of the API older than ours; nothing to do. */
    @Override
    @SuppressWarnings("removal")
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
    }

    /** A burst of light at {@code at} that fades over {@code ticks}. */
    public static void flash(Vec3 at, int luminance, int ticks) {
        if (behaviors != null) {
            behaviors.add(new BlastLight(at, luminance, ticks));
        }
    }
}
