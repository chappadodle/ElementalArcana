package com.chappadodle.elementalarcana;

import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@Mod(ElementalArcana.MODID)
public class ElementalArcana {
    public static final String MODID = "elementalarcana";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public ElementalArcana(IEventBus modEventBus) {
        modEventBus.addListener(NewRegistryEvent.class, event -> {
            event.register(SpellRegistries.SCHOOLS);
            event.register(SpellRegistries.SPELLS);
        });
        modEventBus.addListener(ModNetwork::register);

        ModSchools.SCHOOLS.register(modEventBus);
        ModSpells.SPELLS.register(modEventBus);
        MagicAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModContent.register(modEventBus);
        ModItems.register(modEventBus);
    }
}
