package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.pouch.CharmPouchItem;
import com.chappadodle.elementalarcana.content.pouch.CharmPouchMenu;
import com.chappadodle.elementalarcana.content.pouch.CharmPouches;
import com.chappadodle.elementalarcana.content.pouch.ModPouch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * An open Charm Pouch (see its spec): nine slots on a stitched leather patch over the inventory.
 * Also where the pouch's client side is registered: this screen, its tooltip's grid, and the
 * "filled" look a pouch with charms in it has.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public class CharmPouchScreen extends AbstractContainerScreen<CharmPouchMenu> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/gui/charm_pouch.png");

    public CharmPouchScreen(CharmPouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 172;
        inventoryLabelY = imageHeight - 94;
    }

    @SubscribeEvent
    public static void registerScreen(RegisterMenuScreensEvent event) {
        event.register(ModPouch.MENU.get(), CharmPouchScreen::new);
    }

    @SubscribeEvent
    public static void registerTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CharmPouchItem.Contents.class, contents -> new CharmPouchTooltip(contents.items()));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModPouch.CHARM_POUCH.get(), ElementalArcana.id("filled"),
                (stack, level, entity, seed) -> CharmPouches.count(stack) > 0 ? 1 : 0));
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
