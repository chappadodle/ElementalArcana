package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.content.ModContent;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A shard of ice from a shattered icicle: a small 3D crystal in the icicle's own ice texture,
 * tumbling and bouncing (see TumblingParticle), glinting now and then, and melting away. Lit by the
 * world, but never darker than a faint glow, so it still reads in the dark.
 */
public class IceCrystalParticle extends TumblingParticle {
    private final TextureAtlasSprite sprite;

    public IceCrystalParticle(ClientLevel level, Vec3 at, Vec3 velocity, ResourceLocation texture, float size) {
        super(level, at, velocity, size, 30 + level.random.nextInt(25));
        this.sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed && random.nextFloat() < 0.06f) {
            level.addParticle(ModContent.FROST_SPARKLE.get(), x, y + size / 2, z, 0, 0.01, 0);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float scale = fade(partialTicks);
        if (scale <= 0f) {
            return;
        }
        Vector3f center = center(camera.getPosition(), partialTicks);
        int light = getLightColor(partialTicks);
        light = LightTexture.pack(Math.max(LightTexture.block(light), 9), LightTexture.sky(light));
        IceMesh.crystal(buffer, center, rotation(partialTicks), size * scale, size * 0.6f * scale, size * 0.28f * scale,
                sprite, 1f, 1f, 1f, 0.9f, light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}
