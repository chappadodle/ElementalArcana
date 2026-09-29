package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Bubble;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.ShieldSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellShield;
import com.chappadodle.elementalarcana.client.particle.CinderParticle;
import com.chappadodle.elementalarcana.client.particle.EmberParticle;
import com.chappadodle.elementalarcana.client.particle.GlowParticle;
import com.chappadodle.elementalarcana.client.particle.HydroDropParticle;
import com.chappadodle.elementalarcana.client.particle.HydroStreamEmitter;
import com.chappadodle.elementalarcana.client.particle.FrostMistParticle;
import com.chappadodle.elementalarcana.client.particle.FrostSparkleParticle;
import com.chappadodle.elementalarcana.client.particle.IceShardParticle;
import com.chappadodle.elementalarcana.client.particle.WindStreakParticle;
import com.chappadodle.elementalarcana.content.BubblePrisons;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.spell.BubblePrisonSpell;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.network.CastSpellPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class ArcanaClient {
    private static final String CATEGORY = "key.categories.elementalarcana";
    public static final KeyMapping CAST = key("cast", GLFW.GLFW_KEY_R);
    public static final KeyMapping SPELL_WHEEL = key("spell_wheel", GLFW.GLFW_KEY_V);
    public static final KeyMapping STATUS = key("status", GLFW.GLFW_KEY_K);
    public static final KeyMapping DEV_MENU = key("dev_menu", GLFW.GLFW_KEY_F6);
    // Launching conjured projectiles. They share the mouse buttons with attack/use on purpose: while
    // something is conjured, the click launches instead (see onInteraction). Never reported as a
    // conflict in the Controls menu.
    private static final IKeyConflictContext SHARES_MOUSE = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return KeyConflictContext.IN_GAME.isActive();
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return false;
        }
    };
    public static final KeyMapping LAUNCH_ONE = new KeyMapping("key.elementalarcana.launch_one", SHARES_MOUSE,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT, CATEGORY);
    public static final KeyMapping LAUNCH_ALL = new KeyMapping("key.elementalarcana.launch_all", SHARES_MOUSE,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, CATEGORY);

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
        event.register(LAUNCH_ONE);
        event.register(LAUNCH_ALL);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        MagicData data = MagicAttachments.get(player);

        // Trapped in a Bubble Prison: players move on their own client, so this is where they're held.
        if (BubblePrisons.isTrapped(player)) {
            Bubble bubble = player.getData(MagicAttachments.BUBBLE);
            player.setPos(bubble.holdPoint(player.level().getGameTime()));
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
        }

        // Launch keys only do something while projectiles are conjured; otherwise their clicks are dropped.
        boolean conjuring = data.conjured() > 0;
        while (LAUNCH_ONE.consumeClick()) {
            if (conjuring) {
                PacketDistributor.sendToServer(CastSpellPayload.LAUNCH_ONE);
            }
        }
        while (LAUNCH_ALL.consumeClick()) {
            if (conjuring) {
                PacketDistributor.sendToServer(CastSpellPayload.LAUNCH_ALL);
            }
        }

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

        if (!minecraft.isPaused() && minecraft.level != null) {
            for (Player shielded : minecraft.level.players()) {
                SpellShield shield = SpellShield.of(shielded);
                ShieldSpell spell = shield.shieldSpell();
                if (shield.isActive() && spell != null) {
                    spell.shieldParticles(shielded, shield);
                }
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
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, ElementalArcana.id("shield_hearts"), new ShieldHeartsLayer());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModContent.FROST_SPARKLE.get(), FrostSparkleParticle.Provider::new);
        event.registerSpriteSet(ModContent.ICE_SHARD.get(), IceShardParticle.Provider::new);
        event.registerSpriteSet(ModContent.FROST_MIST.get(), FrostMistParticle.Provider::new);
        event.registerSpriteSet(ModContent.WIND_STREAK.get(), WindStreakParticle.Provider::new);
        event.registerSpriteSet(ModContent.EMBER.get(), EmberParticle.Provider::new);
        event.registerSpriteSet(ModContent.HYDRO_DROP.get(), HydroDropParticle.Provider::new);
        event.registerSpecial(ModContent.HYDRO_STREAM.get(), new HydroStreamEmitter.Provider(1f));
        event.registerSpecial(ModContent.HYDRO_STREAM_THIN.get(), new HydroStreamEmitter.Provider(0.35f));
        event.registerSpecial(ModContent.HYDRO_STREAM_WIDE.get(), new HydroStreamEmitter.Provider(2.5f));
        event.registerSpriteSet(ModContent.SWIRL.get(), WindStreakParticle.SwirlProvider::new);
        event.registerSpriteSet(ModContent.FLARE.get(), sprites -> new GlowParticle.Provider(GlowParticle.Kind.FLARE, sprites));
        event.registerSpriteSet(ModContent.SPARK.get(), sprites -> new GlowParticle.Provider(GlowParticle.Kind.SPARK, sprites));
        event.registerSpriteSet(ModContent.FEATHER.get(), sprites -> new GlowParticle.Provider(GlowParticle.Kind.FEATHER, sprites));
        event.registerSpriteSet(ModContent.CINDER.get(), CinderParticle.Provider::new);
    }

    // Additive particles change the blend function and vanilla doesn't set it back; put it back
    // before anything drawn later assumes the default.
    @SubscribeEvent
    public static void afterParticles(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            RenderSystem.defaultBlendFunc();
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.SPELL_PROJECTILE.get(), SpellProjectileRenderer::new);
    }

    // Load every projectile spell's 3D model, including models from addon spells.
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        Set<ResourceLocation> models = new HashSet<>();
        for (Spell spell : SpellRegistries.SPELLS) {
            if (spell instanceof ProjectileSpell projectile) {
                models.addAll(projectile.models());
            }
            if (spell instanceof ShieldSpell shield && shield.shardModel() != null) {
                models.add(shield.shardModel());
            }
        }
        models.add(BubblePrisonSpell.BUBBLE_MODEL);
        models.forEach(model -> event.register(ModelResourceLocation.standalone(model)));
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Player player) {
            ShieldRenderer.renderShards(player, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource());
        }
        BubbleRenderer.render(event.getEntity(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource());
    }

    /** While projectiles are conjured, a click bound to a launch key launches instead of attacking/using. */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || MagicAttachments.get(minecraft.player).conjured() <= 0) {
            return;
        }
        InputConstants.Key key = event.getKeyMapping().getKey();
        if (key.equals(LAUNCH_ONE.getKey()) || key.equals(LAUNCH_ALL.getKey())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** A player trapped in a bubble can't walk, jump or sneak. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (BubblePrisons.isTrapped(event.getEntity())) {
            Input input = event.getInput();
            input.leftImpulse = 0;
            input.forwardImpulse = 0;
            input.up = input.down = input.left = input.right = false;
            input.jumping = false;
            input.shiftKeyDown = false;
        }
    }
}
