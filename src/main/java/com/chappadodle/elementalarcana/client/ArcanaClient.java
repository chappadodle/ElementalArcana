package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.client.particle.FrostMistParticle;
import com.chappadodle.elementalarcana.client.particle.FrostSparkleParticle;
import com.chappadodle.elementalarcana.client.particle.IceShardParticle;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.network.CastSpellPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class ArcanaClient {
    private static final String CATEGORY = "key.categories.elementalarcana";
    public static final KeyMapping CAST = key("cast", GLFW.GLFW_KEY_R);
    public static final KeyMapping SPELL_WHEEL = key("spell_wheel", GLFW.GLFW_KEY_V);
    public static final KeyMapping STATUS = key("status", GLFW.GLFW_KEY_K);
    public static final KeyMapping DEV_MENU = key("dev_menu", GLFW.GLFW_KEY_F6);

    // The awakening screen is offered once per player instance (i.e. per join/respawn).
    private static LocalPlayer awakeningOfferedTo;
    // Whether the server currently thinks the cast key is down (for hold-to-cast spells).
    private static boolean castKeyHeld;

    private ArcanaClient() {
    }

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.elementalarcana." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CAST);
        event.register(SPELL_WHEEL);
        event.register(STATUS);
        event.register(DEV_MENU);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        MagicData data = MagicAttachments.get(player);

        // Send the key going down and coming back up; a tap within one tick sends both.
        boolean clicked = false;
        while (CAST.consumeClick()) {
            clicked = true;
        }
        boolean down = CAST.isDown();
        if (!castKeyHeld && (down || clicked)) {
            PacketDistributor.sendToServer(CastSpellPayload.PRESS);
            castKeyHeld = true;
        }
        if (castKeyHeld && !down) {
            PacketDistributor.sendToServer(CastSpellPayload.RELEASE);
            castKeyHeld = false;
        }
        while (SPELL_WHEEL.consumeClick()) {
            if (minecraft.screen != null) {
                continue;
            }
            if (data.castableSpells().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.elementalarcana.not_awakened").withStyle(ChatFormatting.RED), true);
            } else {
                minecraft.setScreen(new SpellWheelScreen());
            }
        }
        while (STATUS.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(data.isAwakened() ? new StatusScreen() : new AwakeningScreen());
            }
        }

        while (DEV_MENU.consumeClick()) {
            if (minecraft.screen == null) {
                openDevMenu(minecraft);
            }
        }

        // Wait a moment after joining so the synced magic data has arrived before deciding.
        if (awakeningOfferedTo != player && player.tickCount > 40 && minecraft.screen == null) {
            awakeningOfferedTo = player;
            if (!data.isAwakened()) {
                minecraft.setScreen(new AwakeningScreen());
            }
        }
    }

    /** The dev menu is for operators / cheat-enabled worlds only; the server re-checks every action. */
    static void openDevMenu(Minecraft minecraft) {
        if (minecraft.player.hasPermissions(2)) {
            minecraft.setScreen(new DevScreen());
        } else {
            minecraft.player.displayClientMessage(Component.translatable("message.elementalarcana.dev_no_permission").withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ElementalArcana.id("magic_hud"), new SpellHudLayer());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModContent.FROST_SPARKLE.get(), FrostSparkleParticle.Provider::new);
        event.registerSpriteSet(ModContent.ICE_SHARD.get(), IceShardParticle.Provider::new);
        event.registerSpriteSet(ModContent.FROST_MIST.get(), FrostMistParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.SPELL_PROJECTILE.get(), SpellProjectileRenderer::new);
    }

    // Load every projectile spell's 3D model, including models from addon spells.
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (Spell spell : SpellRegistries.SPELLS) {
            if (spell instanceof ProjectileSpell projectile && projectile.model() != null) {
                event.register(ModelResourceLocation.standalone(projectile.model()));
            }
        }
    }
}
