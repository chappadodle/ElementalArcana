package com.chappadodle.elementalarcana.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * Glowing particles: drawn from the particle atlas and added on top of what's behind them, so
 * light builds up where they overlap (a dense trail gets a white-hot core) and black adds nothing.
 * They don't write depth, so they never hide each other. Drawn in the translucent particle pass;
 * vanilla doesn't reset the blend function afterwards, so ArcanaClient#afterParticles does.
 */
public final class AdditiveParticles {
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "ELEMENTALARCANA_ADDITIVE";
        }
    };

    /**
     * The same, drawn from the block atlas (so the fireballs' animated fire textures can wrap 3D
     * shapes), and without back-face culling so a shape shows its inside too. ArcanaClient turns
     * culling back on after the particles.
     */
    public static final ParticleRenderType BLOCKS_RENDER_TYPE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "ELEMENTALARCANA_ADDITIVE_BLOCKS";
        }
    };

    private AdditiveParticles() {
    }
}
