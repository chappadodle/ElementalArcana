package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.creature.WispEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Draws a wisp at full brightness with its element's texture and a soft, pulsing halo of its
 * element's light (GlowHalo), and without tipping over when it dies.
 */
public class WispRenderer extends MobRenderer<WispEntity, WispModel> {
    private static final Map<Element, ResourceLocation> TEXTURES = new EnumMap<>(Element.class);
    private static final float HALO_SIZE = 1.4f;
    private static final float HALO_STRENGTH = 0.55f;

    static {
        for (Element element : Element.values()) {
            TEXTURES.put(element, ElementalArcana.id("textures/entity/wisp/" + element.name().toLowerCase(Locale.ROOT) + ".png"));
        }
    }

    public WispRenderer(EntityRendererProvider.Context context) {
        super(context, new WispModel(context.bakeLayer(WispModel.LAYER)), 0.2f);
    }

    @Override
    public void render(WispEntity wisp, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(wisp, entityYaw, partialTick, poseStack, buffers, packedLight);
        float time = wisp.tickCount + partialTick;
        // The model's middle, bobbing with it (WispModel: 5 pixels up, 1.2 pixels of bob).
        float middle = (5f - Mth.sin(time * 0.1f) * 1.2f) / 16f * wisp.getScale();
        float size = HALO_SIZE * wisp.getScale() * (1f + 0.08f * Mth.sin(time * 0.2f));
        int color = haloColor(wisp.element());
        poseStack.pushPose();
        poseStack.translate(0, middle, 0);
        GlowHalo.draw(poseStack, buffers, entityRenderDispatcher.cameraOrientation(), size,
                Math.round((color >> 16 & 0xFF) * HALO_STRENGTH), Math.round((color >> 8 & 0xFF) * HALO_STRENGTH),
                Math.round((color & 0xFF) * HALO_STRENGTH));
        poseStack.popPose();
    }

    private static int haloColor(Element element) {
        return switch (element) {
            case FIRE -> 0xFF8A30;
            case WATER -> 0x4AA0FF;
            case ICE -> 0xA8E4FF;
            case WIND -> 0x90F0C8;
            case EARTH -> 0xFFB650;
            case CRYSTAL -> 0xD08CFF;
            case LIGHTNING -> 0xFFE14D;
            case RADIANCE -> 0xFFEBB0;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(WispEntity wisp) {
        return TEXTURES.get(wisp.element());
    }

    @Override
    protected int getBlockLightLevel(WispEntity wisp, BlockPos pos) {
        return 15;
    }

    @Override
    protected float getFlipDegrees(WispEntity wisp) {
        return 0;
    }
}
