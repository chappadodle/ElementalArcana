package com.chappadodle.elementalarcana.compat.veil;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.compat.IrisCompat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Bloom through Veil (optional: only client/Bloom calls this, and only when Veil is loaded).
 * Whatever is drawn into Veil's bloom buffer is blurred and added over the screen at the end of
 * the frame, so it bleeds light around itself. Iris shader packs do their own thing, so there's no
 * bloom while one is on.
 * <p>
 * Only colour is written into the bloom buffer, never alpha: Veil treats pixels there with alpha
 * as solid emissive surfaces and brightens the whole scene behind them, which turned each glow
 * quad (black but opaque at its edges) into a bright square. Colour alone just adds the blur.
 */
public final class VeilBloom {
    /** Veil's bloom output, writing colour but not alpha. */
    private static final RenderStateShard.OutputStateShard BLOOM_COLOR_ONLY = new RenderStateShard.OutputStateShard(
            "elementalarcana:bloom_color_only",
            () -> {
                VeilRenderSystem.BLOOM_SHARD.setupRenderState();
                RenderSystem.colorMask(true, true, true, false);
            },
            () -> {
                RenderSystem.colorMask(true, true, true, true);
                VeilRenderSystem.BLOOM_SHARD.clearRenderState();
            });

    /** Like vanilla's "eyes" (additive, full bright), but drawn into the bloom buffer. */
    private static final Function<ResourceLocation, RenderType> GLOW = Util.memoize(texture -> RenderType.create(
            "elementalarcana_bloom_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOutputState(BLOOM_COLOR_ONLY)
                    .createCompositeState(false)));

    private VeilBloom() {
    }

    public static boolean usable() {
        return IrisCompat.INSTANCE == null || !IrisCompat.INSTANCE.areShadersLoaded();
    }

    public static RenderType glow(ResourceLocation texture) {
        return GLOW.apply(texture);
    }

    /** Draws already-built particle quads (the particle vertex format) into the bloom buffer, additively. */
    public static void drawParticles(MeshData mesh) {
        BLOOM_COLOR_ONLY.setupRenderState();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        BufferUploader.drawWithShader(mesh);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        BLOOM_COLOR_ONLY.clearRenderState();
    }
}
