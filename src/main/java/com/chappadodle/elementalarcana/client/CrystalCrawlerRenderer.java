package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.CrystalCrawlerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws a Crystal Crawler, its crystals and eyes glowing in the dark. */
public class CrystalCrawlerRenderer extends MobRenderer<CrystalCrawlerEntity, CrystalCrawlerModel> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/wild/crystal_crawler.png");
    private static final RenderType GLOW = RenderType.eyes(ElementalArcana.id("textures/entity/wild/crystal_crawler_glow.png"));

    public CrystalCrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, new CrystalCrawlerModel(context.bakeLayer(CrystalCrawlerModel.LAYER)), 0.7f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(CrystalCrawlerEntity crawler) {
        return TEXTURE;
    }
}
