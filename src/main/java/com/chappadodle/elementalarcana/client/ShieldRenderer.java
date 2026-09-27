package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.ShieldSpell;
import com.chappadodle.elementalarcana.api.SpellShield;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Draws a shielded player's orbiting shards: the spell's shard model, circling at chest height.
 * Shards break off as the shield weakens, so you can read its strength at a glance.
 */
final class ShieldRenderer {
    private static final float SHARD_SCALE = 0.4f;
    private static final double ORBIT_RADIUS = 0.95;

    private ShieldRenderer() {
    }

    static void renderShards(Player player, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        SpellShield shield = SpellShield.of(player);
        ShieldSpell spell = shield.shieldSpell();
        if (!shield.isActive() || spell == null || spell.shardModel() == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(spell.shardModel()));
        int shards = Math.max(1, Mth.ceil(spell.shardCount() * shield.fraction()));
        float time = player.tickCount + partialTick;
        for (int i = 0; i < shards; i++) {
            float angle = time * 0.08f + i * Mth.TWO_PI / shards;
            double bob = Math.sin(time * 0.1 + i) * 0.12;
            poseStack.pushPose();
            poseStack.translate(Math.cos(angle) * ORBIT_RADIUS, player.getBbHeight() * 0.55 + bob, Math.sin(angle) * ORBIT_RADIUS);
            // Stand each shard point-up, turning slowly as it orbits.
            poseStack.mulPose(Axis.YP.rotation(-angle));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            poseStack.scale(SHARD_SCALE, SHARD_SCALE, SHARD_SCALE);
            poseStack.translate(-0.5, -0.5, -0.5);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                    buffers.getBuffer(Sheets.translucentCullBlockSheet()), null, model, 1f, 1f, 1f,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}
