package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** The client side's own setup: its config and the config screen in the Mods list. */
@Mod(value = ElementalArcana.MODID, dist = Dist.CLIENT)
public class ElementalArcanaClient {
    public ElementalArcanaClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ArcanaClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
