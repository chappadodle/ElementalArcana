package com.chappadodle.elementalarcana;

import com.chappadodle.elementalarcana.content.wanderer.ModWanderer;
import com.chappadodle.elementalarcana.core.ArcanaServerConfig;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import com.chappadodle.elementalarcana.content.drake.ModDrakes;
import com.chappadodle.elementalarcana.content.wild.ModWild;
import com.chappadodle.elementalarcana.content.infusion.ModInfusion;
import com.chappadodle.elementalarcana.content.cantrip.ModCantrips;
import com.chappadodle.elementalarcana.content.relic.ModRelics;
import com.chappadodle.elementalarcana.content.crypt.ModCrypts;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.ModTabs;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import com.chappadodle.elementalarcana.content.creature.ModCreatures;
import com.chappadodle.elementalarcana.content.people.ModPeople;
import com.chappadodle.elementalarcana.content.hollow.ModHollow;
import com.chappadodle.elementalarcana.content.sanctum.ModSanctums;
import com.chappadodle.elementalarcana.content.tower.ModTowers;
import com.chappadodle.elementalarcana.content.brew.ModBrews;
import com.chappadodle.elementalarcana.content.world.ModWorld;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.network.ModNetwork;
import com.chappadodle.elementalarcana.content.star.ModStars;
import com.chappadodle.elementalarcana.content.pouch.ModPouch;
import com.chappadodle.elementalarcana.content.seeker.ModSeeker;
import com.chappadodle.elementalarcana.content.mentor.ModMentor;
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

    public ElementalArcana(IEventBus modEventBus, ModContainer modContainer) {
        // The server's settings (see core/ArcanaServerConfig).
        modContainer.registerConfig(ModConfig.Type.SERVER, ArcanaServerConfig.SPEC);
        modEventBus.addListener(NewRegistryEvent.class, event -> {
            event.register(SpellRegistries.SCHOOLS);
            event.register(SpellRegistries.SPELLS);
        });
        modEventBus.addListener(ModNetwork::register);

        ModSchools.SCHOOLS.register(modEventBus);
        ModSpells.SPELLS.register(modEventBus);
        MagicAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModContent.register(modEventBus);
        MagicTriggers.register(modEventBus);
        ModItems.register(modEventBus);
        ModGear.register(modEventBus);
        ModWorld.register(modEventBus);
        ModCreatures.register(modEventBus);
        ModPeople.register(modEventBus);
        ModTowers.register(modEventBus);
        ModSanctums.register(modEventBus);
        ModHollow.register(modEventBus);
        ModCrypts.register(modEventBus);
        ModRelics.register(modEventBus);
        ModDrakes.register(modEventBus);
        ModWild.register(modEventBus);
        ModInfusion.register(modEventBus);
        ModCantrips.register(modEventBus);
        ModStars.register(modEventBus);
        ModPouch.register(modEventBus);
        ModSeeker.register(modEventBus);
        ModMentor.register(modEventBus);
        ModWanderer.register(modEventBus);
        ModBrews.register(modEventBus);
        ModTabs.TABS.register(modEventBus);
    }
}
