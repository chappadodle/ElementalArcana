package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.infusion.Infusions;
import com.chappadodle.elementalarcana.content.infusion.ModInfusion;
import com.chappadodle.elementalarcana.content.mob.MobCasting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Infusion on the client (see the Arcane Infusion spec): "Infused: <element>" under an infused
 * item's name, and a faint trace of the element from an infused weapon in someone's hand (not your
 * own in first person, where it would only get in the way).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class InfusionClient {
    private static final double TRACE_RANGE = 24;

    private InfusionClient() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Element element = ModInfusion.infusionOf(event.getItemStack());
        if (element != null) {
            event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                    Component.translatable("tooltip.elementalarcana.infused", Infusions.elementName(element)).withColor(element.color()));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || level.getGameTime() % 4 != 0 || minecraft.player == null) {
            return;
        }
        for (Player player : level.players()) {
            if (player.distanceToSqr(minecraft.player) > TRACE_RANGE * TRACE_RANGE || player.isInvisible()
                    || player == minecraft.player && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
                continue;
            }
            Element element = ModInfusion.infusionOf(player.getMainHandItem());
            if (element == null) {
                continue;
            }
            float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
            double side = player.getMainArm() == HumanoidArm.RIGHT ? -1 : 1;
            Vec3 hand = player.position().add(Mth.cos(yaw) * 0.35 * side - Mth.sin(yaw) * 0.25, 0.75,
                    Mth.sin(yaw) * 0.35 * side + Mth.cos(yaw) * 0.25);
            level.addParticle(MobCasting.handsParticle(element), hand.x + (level.random.nextDouble() - 0.5) * 0.2, hand.y + level.random.nextDouble() * 0.4,
                    hand.z + (level.random.nextDouble() - 0.5) * 0.2, 0, 0.01, 0);
        }
    }
}
