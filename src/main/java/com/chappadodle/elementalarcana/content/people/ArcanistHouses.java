package com.chappadodle.elementalarcana.content.people;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

/**
 * Villages can build an Arcanist's cottage (data/elementalarcana/structure/village/<village>/
 * arcanist_cottage.nbt, from tools/gen_people.py): when the server starts, the cottage is added to
 * each village type's house pool. Vanilla has no hook for this, so it goes into the pool's list of
 * templates directly (the list the village generator draws from), once per point of weight.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ArcanistHouses {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] VILLAGES = {"plains", "desert", "savanna", "snowy", "taiga"};
    /** Each house pool weighs about 70 to 90 in all: about one village in three builds a cottage. */
    private static final int WEIGHT = 1;

    private ArcanistHouses() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Registry<StructureTemplatePool> pools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        int added = 0;
        for (String village : VILLAGES) {
            StructureTemplatePool pool = pools.get(ResourceLocation.withDefaultNamespace("village/" + village + "/houses"));
            if (pool == null) {
                LOGGER.warn("No {} village house pool: no Arcanist's cottage there", village);
                continue;
            }
            StructurePoolElement cottage = StructurePoolElement.single(ElementalArcana.MODID + ":village/" + village + "/arcanist_cottage")
                    .apply(StructureTemplatePool.Projection.RIGID);
            try {
                ObjectArrayList<StructurePoolElement> templates = ObfuscationReflectionHelper.getPrivateValue(
                        StructureTemplatePool.class, pool, "templates");
                for (int i = 0; i < WEIGHT; i++) {
                    templates.add(cottage);
                }
                added++;
            } catch (RuntimeException e) {
                LOGGER.error("Couldn't add the Arcanist's cottage to the {} village houses", village, e);
            }
        }
        LOGGER.info("Villages can build Arcanist's cottages ({} of {} village types)", added, VILLAGES.length);
    }
}
