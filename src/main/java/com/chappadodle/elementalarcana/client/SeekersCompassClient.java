package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SeekerRules;
import com.chappadodle.elementalarcana.content.seeker.ModSeeker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The Seeker's Compass on the client (see its spec): its needle, vanilla's compass angle pointed at
 * what it found (spinning when there's nothing, or it's in another dimension), and how far that
 * is under its name.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class SeekersCompassClient {

    private SeekersCompassClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModSeeker.SEEKERS_COMPASS.get(), ResourceLocation.withDefaultNamespace("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> stack.get(ModSeeker.SOUGHT.get()))));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        GlobalPos sought = stack.is(ModSeeker.SEEKERS_COMPASS) ? stack.get(ModSeeker.SOUGHT.get()) : null;
        if (sought == null || player == null || sought.dimension() != player.level().dimension()) {
            return;
        }
        int distance = SeekerRules.roughDistance(sought.pos().getX() + 0.5 - player.getX(), sought.pos().getZ() + 0.5 - player.getZ());
        event.getToolTip().add(Math.min(2, event.getToolTip().size()), Component.translatable("item.elementalarcana.seekers_compass.distance",
                ModSeeker.oneName(ModSeeker.seeking(stack)), distance).withStyle(ChatFormatting.GRAY));
    }
}
