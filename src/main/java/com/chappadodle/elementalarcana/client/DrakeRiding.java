package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.DrakeRidingRules;
import com.chappadodle.elementalarcana.content.drake.TamedDrakeEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Riding a drake (see the Drake Riding spec), on the rider's client: it hears whether the rider
 * holds jump (to take off and climb; TamedDrakeEntity#travel flies it), and while riding a stamina
 * bar in the drake's colour covers the experience bar.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class DrakeRiding {
    private static final int BAR_WIDTH = 182;

    /** The stamina bar, drawn while the local player rides a drake. */
    public static final LayeredDraw.Layer STAMINA = DrakeRiding::drawStamina;

    private DrakeRiding() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof LocalPlayer player && player.getVehicle() instanceof TamedDrakeEntity drake) {
            drake.setRiderClimb(player.input.jumping);
        }
    }

    private static void drawStamina(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !(minecraft.player != null && minecraft.player.getVehicle() instanceof TamedDrakeEntity drake)
                || !drake.isAdult()) {
            return;
        }
        // Over the experience bar, as a horse's jump bar is.
        int x = graphics.guiWidth() / 2 - BAR_WIDTH / 2;
        int y = graphics.guiHeight() - 29;
        int filled = Math.round(BAR_WIDTH * drake.stamina() / (float) DrakeRidingRules.MAX_STAMINA);
        int color = FastColor.ARGB32.opaque(drake.element().color());
        graphics.fill(x, y, x + BAR_WIDTH, y + 5, 0xE0101010);
        graphics.fill(x + 1, y + 1, x + 1 + Math.max(0, filled - 2), y + 4, color);
    }
}
