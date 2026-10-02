package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Glow;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.visual.ProjectileVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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
 * Draws a spell projectile: with its spell's own visual if it has one (see ProjectileVisuals), or
 * else its 3D model if it has one (see {@link ProjectileSpell#model()}).
 * Held projectiles are drawn at the caster's interpolated hand position every frame and point
 * where the caster looks; thrown ones point along their flight. Everything else is particles.
 */
public class SpellProjectileRenderer extends EntityRenderer<SpellProjectile> {
    private static final float MIN_SCALE = 0.35f;

    public SpellProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpellProjectile projectile, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        ProjectileSpell spell = projectile.spell();
        if (spell == null) {
            return;
        }
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

        ProjectileVisuals.Visual visual = spell instanceof Spell asSpell ? ProjectileVisuals.get(asSpell) : null;
        if (visual != null) {
            poseStack.pushPose();
            poseStack.translate(shift.x, shift.y, shift.z);
            visual.render(projectile, direction, scale, partialTick, poseStack, buffers);
            poseStack.popPose();
            return;
        }
        ResourceLocation modelId = spell.model(projectile.variant());
        if (modelId == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(modelId));

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

    /** A soft halo of light around the projectile (see GlowHalo), with a slow pulse. */
    private void renderGlow(Glow glow, float scale, float time, Vec3 shift, PoseStack poseStack, MultiBufferSource buffers) {
        float size = glow.size() * scale * (1f + 0.06f * Mth.sin(time * 0.25f));
        int red = Math.min(255, Math.round((glow.color() >> 16 & 0xFF) * glow.intensity()));
        int green = Math.min(255, Math.round((glow.color() >> 8 & 0xFF) * glow.intensity()));
        int blue = Math.min(255, Math.round((glow.color() & 0xFF) * glow.intensity()));
        poseStack.pushPose();
        poseStack.translate(shift.x, shift.y, shift.z);
        GlowHalo.draw(poseStack, buffers, entityRenderDispatcher.cameraOrientation(), size, red, green, blue);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SpellProjectile projectile) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
