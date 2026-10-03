package com.chappadodle.elementalarcana.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

/**
 * A chunk of the ground blown out by an explosion: a small 3D cube cut from the block's own
 * texture (a random patch of it, tinted like the block, e.g. grass), shaded per face like blocks
 * are. It tumbles and bounces (see TumblingParticle) and shrinks away.
 */
public class DebrisParticle extends TumblingParticle {
    private final float u0, u1, v0, v1;

    public DebrisParticle(ClientLevel level, Vec3 at, Vec3 velocity, BlockState state, BlockPos pos, float size) {
        this(level, at, velocity, Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state), size);
        if (IClientBlockExtensions.of(state).areBreakingParticlesTinted(state, level, pos)) {
            int tint = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
            rCol = (tint >> 16 & 0xFF) / 255f;
            gCol = (tint >> 8 & 0xFF) / 255f;
            bCol = (tint & 0xFF) / 255f;
        }
    }

    /** A chunk of any texture on the block sheet, untinted. */
    public DebrisParticle(ClientLevel level, Vec3 at, Vec3 velocity, TextureAtlasSprite sprite, float size) {
        super(level, at, velocity, size, 40 + level.random.nextInt(30));
        // A random quarter of the texture, like vanilla's block-breaking bits.
        float uo = random.nextFloat() * 3f;
        float vo = random.nextFloat() * 3f;
        this.u0 = sprite.getU(uo / 4f);
        this.u1 = sprite.getU((uo + 1f) / 4f);
        this.v0 = sprite.getV(vo / 4f);
        this.v1 = sprite.getV((vo + 1f) / 4f);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float half = size / 2f * fade(partialTicks);
        if (half <= 0f) {
            return;
        }
        BlockMesh.cube(buffer, center(camera.getPosition(), partialTicks), rotation(partialTicks), half,
                u0, u1, v0, v1, rCol, gCol, bCol, getLightColor(partialTicks));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}
