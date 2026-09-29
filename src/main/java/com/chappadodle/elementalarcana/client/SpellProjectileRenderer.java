package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Glow;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a spell projectile's 3D model if its spell has one (see {@link ProjectileSpell#model()}).
 * Held projectiles are drawn at the caster's interpolated hand position every frame and point
 * where the caster looks; thrown ones point along their flight. Everything else is particles.
 */
public class SpellProjectileRenderer extends EntityRenderer<SpellProjectile> {
    private static final float MIN_SCALE = 0.35f;
    private static final ResourceLocation GLOW_TEXTURE = ElementalArcana.id("textures/misc/glow.png");

    public SpellProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpellProjectile projectile, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        ProjectileSpell spell = projectile.spell();
        ResourceLocation modelId = spell == null ? null : spell.model(projectile.variant());
        if (modelId == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(modelId));

        Vec3 direction;
        Vec3 shift = Vec3.ZERO;
        Entity owner = projectile.getOwner();
        if (projectile.isHeld() && owner != null) {
            // Draw exactly at the hand position for this frame, not where the entity was last tick.
            shift = projectile.holdPosition(owner, spell, partialTick).subtract(projectile.getPosition(partialTick));
            direction = owner.getViewVector(partialTick);
        } else {
            direction = projectile.getDeltaMovement();
            if (direction.lengthSqr() < 1.0e-6) {
                direction = projectile.getViewVector(partialTick);
            }
        }
        direction = direction.normalize();
        float yaw = (float) Mth.atan2(direction.x, direction.z);
        float pitch = (float) -Mth.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z));
        float scale = Mth.lerp(projectile.charge(partialTick), MIN_SCALE, 1f) * projectile.visualScale();

        poseStack.pushPose();
        poseStack.translate(shift.x, shift.y, shift.z);
        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(pitch));
        if (projectile.isHeld()) {
            // A slow spin while it forms.
            poseStack.mulPose(Axis.ZP.rotationDegrees((projectile.tickCount + partialTick) * 12f));
        }
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                buffers.getBuffer(Sheets.translucentCullBlockSheet()), null, model, 1f, 1f, 1f,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        Glow glow = spell.glow(projectile.variant());
        if (glow != null) {
            renderGlow(glow, scale, projectile.tickCount + partialTick, shift, poseStack, buffers);
        }
    }

    /**
     * A soft halo of light around the projectile: a camera-facing quad of the glow sprite, added on
     * top of what's behind it (the "eyes" render type blends additively, like spider eyes), with a
     * slow pulse.
     */
    private void renderGlow(Glow glow, float scale, float time, Vec3 shift, PoseStack poseStack, MultiBufferSource buffers) {
        float size = glow.size() * scale * (1f + 0.06f * Mth.sin(time * 0.25f));
        int red = Math.min(255, Math.round((glow.color() >> 16 & 0xFF) * glow.intensity()));
        int green = Math.min(255, Math.round((glow.color() >> 8 & 0xFF) * glow.intensity()));
        int blue = Math.min(255, Math.round((glow.color() & 0xFF) * glow.intensity()));
        poseStack.pushPose();
        poseStack.translate(shift.x, shift.y, shift.z);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(GLOW_TEXTURE));
        glowVertex(consumer, pose, -0.5f, -0.5f, 0f, 1f, red, green, blue);
        glowVertex(consumer, pose, 0.5f, -0.5f, 1f, 1f, red, green, blue);
        glowVertex(consumer, pose, 0.5f, 0.5f, 1f, 0f, red, green, blue);
        glowVertex(consumer, pose, -0.5f, 0.5f, 0f, 0f, red, green, blue);
        poseStack.popPose();
    }

    private static void glowVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int red, int green, int blue) {
        consumer.addVertex(pose, x, y, 0f)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0f, 1f, 0f);
    }

    @Override
    public ResourceLocation getTextureLocation(SpellProjectile projectile) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
