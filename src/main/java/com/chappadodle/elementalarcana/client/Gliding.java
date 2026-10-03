package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SkywardRules;
import com.chappadodle.elementalarcana.client.sound.PointLoopSound;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.network.GlidePayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The glide (see the Skyward Leap spec; the rest of the spell is the server's, SkywardLeaps). A
 * player's movement is their own client's, so the local player glides here: each tick before they
 * move, if they're Skyward, in the air, falling and holding jump, their motion is steered toward
 * where they look at the glide speed and their fall held to the sink rate. Sneaking dives instead
 * (from level 4; the server sees the sneak and sets off the shockwave where they land). The server
 * is told when a glide starts and stops (GlidePayload) and tells everyone who can see the player;
 * every client draws a gliding player leaning into the wind, with wind streaming off their hands,
 * and the glider hears the air rush past.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class Gliding {
    /** How much of the way to the glide speed a glider is steered each tick (air drag takes some back). */
    private static final double STEER = 0.3;
    /** How far forward a glider leans, in degrees. */
    private static final float LEAN = 70f;

    /** Whether the local player is gliding (what the server was last told). */
    private static boolean gliding;
    private static final Set<Player> LEANING = Collections.newSetFromMap(new WeakHashMap<>());

    private Gliding() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        boolean glide = false;
        if (player.hasEffect(ModContent.SKYWARD) && !player.onGround() && !player.isInWater() && !player.isInLava()
                && !player.getAbilities().flying && !player.isPassenger() && !player.isFallFlying()) {
            int level = Math.max(1, MagicAttachments.get(player).spellLevel(ModSpells.SKYWARD_LEAP.get()));
            Vec3 motion = player.getDeltaMovement();
            if (level >= 4 && player.isShiftKeyDown()) {
                // The plunge: straight down.
                player.setDeltaMovement(motion.x * 0.4, Math.min(motion.y, -SkywardRules.DIVE), motion.z * 0.4);
            } else if (player.input.jumping && motion.y < 0) {
                glide = true;
                Vec3 look = player.getLookAngle();
                Vec3 ahead = new Vec3(look.x, 0, look.z);
                ahead = ahead.lengthSqr() < 1.0e-4 ? Vec3.ZERO : ahead.normalize().scale(SkywardRules.glideSpeed(level));
                Vec3 flat = new Vec3(motion.x, 0, motion.z).lerp(ahead, STEER);
                player.setDeltaMovement(flat.x, Math.max(motion.y, -SkywardRules.sinkRate(level)), flat.z);
            }
        }
        if (glide != gliding) {
            gliding = glide;
            PacketDistributor.sendToServer(new GlidePayload(glide));
            if (glide) {
                Minecraft.getInstance().getSoundManager().play(new PointLoopSound(SoundEvents.ELYTRA_FLYING, 0.5f, 1.1f,
                        () -> gliding && player.isAlive() ? player.position() : null, () -> 1f));
            }
        }
    }

    /** Whether {@code player} is gliding, as this client knows it. */
    public static boolean isGliding(Player player) {
        return player instanceof LocalPlayer ? gliding : player.getData(MagicAttachments.GLIDING);
    }

    /** Wind streaming off every glider's hands. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || Minecraft.getInstance().isPaused()) {
            return;
        }
        for (Player player : level.players()) {
            if (!isGliding(player)) {
                continue;
            }
            float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
            Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
            Vec3 trail = player.getDeltaMovement().scale(-0.3);
            for (int hand = -1; hand <= 1; hand += 2) {
                Vec3 at = player.position().add(0, 0.9, 0).add(side.scale(0.7 * hand));
                level.addParticle(ModContent.WIND_STREAK.get(), at.x, at.y, at.z, trail.x, trail.y + 0.02, trail.z);
            }
        }
    }

    /** A glider leans into the wind. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!isGliding(player)) {
            return;
        }
        float yaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, 0.9, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(LEAN));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(0, -0.9, 0);
        LEANING.add(player);
    }

    @SubscribeEvent
    public static void afterRenderPlayer(RenderPlayerEvent.Post event) {
        if (LEANING.remove(event.getEntity())) {
            event.getPoseStack().popPose();
        }
    }
}
