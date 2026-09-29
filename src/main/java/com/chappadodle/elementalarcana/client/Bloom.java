package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.compat.veil.VeilBloom;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Bloom: light bleeding around the glowing spell effects. It needs Veil (an optional dependency)
 * and can be turned off in the client config. Nothing here touches Veil unless it's loaded.
 * <p>
 * Glowing particles register themselves as they're made; after the particles are drawn, they're
 * drawn a second time into Veil's bloom buffer. Projectile glow halos draw into it with
 * {@link #glowType}.
 */
public final class Bloom {
    private static final boolean VEIL_LOADED = ModList.get().isLoaded("veil");

    /** A live glowing particle, and the tick after which it must be gone. */
    private record Tracked(Particle particle, ClientLevel level, long expires) {
    }

    private static final List<Tracked> PARTICLES = new ArrayList<>();

    private Bloom() {
    }

    public static boolean active() {
        return VEIL_LOADED && ArcanaClientConfig.BLOOM.get() && VeilBloom.usable();
    }

    /** Called by glowing particles when they're made. */
    public static void track(Particle particle, ClientLevel level, int lifetime) {
        if (active()) {
            PARTICLES.add(new Tracked(particle, level, level.getGameTime() + lifetime + 2));
        }
    }

    /** The render type for a projectile's glow halo in the bloom buffer, or null without bloom. */
    @Nullable
    public static RenderType glowType(ResourceLocation texture) {
        return active() ? VeilBloom.glow(texture) : null;
    }

    /** After the particles: draw the glowing ones again, into the bloom buffer. */
    static void renderParticles(Camera camera, float partialTick) {
        if (PARTICLES.isEmpty()) {
            return;
        }
        ClientLevel current = Minecraft.getInstance().level;
        boolean on = active();
        BufferBuilder buffer = on ? Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE) : null;
        for (Iterator<Tracked> it = PARTICLES.iterator(); it.hasNext(); ) {
            Tracked tracked = it.next();
            // Gone: ended, from another world, or dropped by the particle engine when it was full.
            if (!tracked.particle().isAlive() || tracked.level() != current || current.getGameTime() > tracked.expires()) {
                it.remove();
            } else if (buffer != null) {
                tracked.particle().render(buffer, camera, partialTick);
            }
        }
        if (!on) {
            PARTICLES.clear();
            return;
        }
        MeshData mesh = buffer.build();
        if (mesh != null) {
            VeilBloom.drawParticles(mesh);
        }
    }
}
