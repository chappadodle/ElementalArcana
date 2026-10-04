package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.creature.GolemEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Draws an Elemental Golem with its element's texture, and its core and cracks glowing on top
 * (a second, emissive texture where everything but the glow is black).
 */
public class GolemRenderer extends MobRenderer<GolemEntity, GolemModel> {
    private static final Map<Element, ResourceLocation> TEXTURES = new EnumMap<>(Element.class);
    private static final Map<Element, RenderType> GLOWS = new EnumMap<>(Element.class);

    static {
        for (Element element : Element.values()) {
            String name = element.name().toLowerCase(Locale.ROOT);
            TEXTURES.put(element, ElementalArcana.id("textures/entity/golem/" + name + ".png"));
            GLOWS.put(element, RenderType.eyes(ElementalArcana.id("textures/entity/golem/" + name + "_glow.png")));
        }
    }

    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(GolemModel.LAYER)), 1.0f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                // Unused: render() below picks the golem's own element.
                return GLOWS.get(Element.FIRE);
            }

            @Override
            public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers,
                               int packedLight, GolemEntity golem, float limbSwing, float limbSwingAmount, float partialTick,
                               float ageInTicks, float netHeadYaw, float headPitch) {
                getParentModel().renderToBuffer(poseStack, buffers.getBuffer(GLOWS.get(golem.element())), 0xF00000,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(GolemEntity golem) {
        return TEXTURES.get(golem.element());
    }
}
