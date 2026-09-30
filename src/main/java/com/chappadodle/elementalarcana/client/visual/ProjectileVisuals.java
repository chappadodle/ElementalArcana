package com.chappadodle.elementalarcana.client.visual;

import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Spells whose projectiles are drawn by their own client code instead of a block model (e.g. the
 * Wind Blade's slash of air). SpellProjectileRenderer asks here first.
 */
public final class ProjectileVisuals {

    /** Draws a projectile. The pose is at its (hand) position; {@code direction} is where it points. */
    @FunctionalInterface
    public interface Visual {
        void render(SpellProjectile projectile, Vec3 direction, float scale, float partialTick, PoseStack poseStack, MultiBufferSource buffers);
    }

    private static final Map<ResourceLocation, Visual> VISUALS = new HashMap<>();

    private ProjectileVisuals() {
    }

    public static void register(Spell spell, Visual visual) {
        VISUALS.put(spell.id(), visual);
    }

    @Nullable
    public static Visual get(Spell spell) {
        return VISUALS.get(spell.id());
    }
}
