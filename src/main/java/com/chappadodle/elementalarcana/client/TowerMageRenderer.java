package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.tower.TowerMageEntity;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * A mage tower's Acolyte or Magister: the vanilla illager model (as the evoker's) with its hood on,
 * in robes of the tower's element (textures from tools/gen_towers.py). Its arms rise to cast
 * (TowerMageEntity#getArmPose).
 */
public class TowerMageRenderer extends IllagerRenderer<TowerMageEntity> {

    public TowerMageRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.EVOKER)), 0.5f);
        model.getHat().visible = true;
    }

    @Override
    public ResourceLocation getTextureLocation(TowerMageEntity mage) {
        Element element = mage.element();
        String kind = mage.isMagister() ? "magister" : "acolyte";
        return ElementalArcana.id("textures/entity/tower_mage/" + kind + "_" + element.name().toLowerCase(Locale.ROOT) + ".png");
    }
}
