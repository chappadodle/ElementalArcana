package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Bubble;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.ShieldSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellShield;
import com.chappadodle.elementalarcana.client.decal.Decals;
import com.chappadodle.elementalarcana.client.particle.CinderParticle;
import com.chappadodle.elementalarcana.client.particle.EmberParticle;
import com.chappadodle.elementalarcana.client.particle.FireBlastEmitter;
import com.chappadodle.elementalarcana.client.particle.LightningArcParticle;
import com.chappadodle.elementalarcana.client.particle.PyronadoEmitter;
import com.chappadodle.elementalarcana.client.particle.SmiteParticle;
import com.chappadodle.elementalarcana.client.particle.TremorEmitter;
import com.chappadodle.elementalarcana.client.particle.TsunamiParticle;
import com.chappadodle.elementalarcana.client.particle.GlowParticle;
import com.chappadodle.elementalarcana.client.particle.HydroDropParticle;
import com.chappadodle.elementalarcana.client.particle.IceShatterEmitter;
import com.chappadodle.elementalarcana.client.particle.BubbleCastEmitter;
import com.chappadodle.elementalarcana.client.particle.WaterBurstParticle;
import com.chappadodle.elementalarcana.client.particle.WindCutEmitter;
import com.chappadodle.elementalarcana.client.particle.HydroStreamEmitter;
import com.chappadodle.elementalarcana.client.particle.FrostMistParticle;
import com.chappadodle.elementalarcana.client.particle.FrostSparkleParticle;
import com.chappadodle.elementalarcana.client.particle.IceShardParticle;
import com.chappadodle.elementalarcana.client.particle.WindStreakParticle;
import com.chappadodle.elementalarcana.client.visual.ProjectileVisuals;
import com.chappadodle.elementalarcana.client.visual.WaterBeams;
import com.chappadodle.elementalarcana.client.visual.WaterSpearRenderer;
import com.chappadodle.elementalarcana.client.visual.WindSlashRenderer;
import com.chappadodle.elementalarcana.content.BubblePrisons;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.creature.ModCreatures;
import com.chappadodle.elementalarcana.content.hollow.ModHollow;
import com.chappadodle.elementalarcana.content.sanctum.ModSanctums;
import com.chappadodle.elementalarcana.content.tower.ModTowers;
import com.chappadodle.elementalarcana.content.tower.TowerHeartBlock;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.network.AuraPayload;
import com.chappadodle.elementalarcana.network.CastSpellPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import com.chappadodle.elementalarcana.content.gear.FocusItem;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import com.chappadodle.elementalarcana.content.world.ModWorld;
import com.chappadodle.elementalarcana.content.world.ShrineCoreBlock;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.util.FastColor;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SovereignRules;
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
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
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
    public static final KeyMapping HIDE_AURA = key("hide_aura", GLFW.GLFW_KEY_H);
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

    // Whether the server currently thinks the cast key is down (for hold-to-cast spells).
    private static boolean castKeyHeld;

    private ArcanaClient() {
    }

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.elementalarcana." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            DynamicLights.init();
            ProjectileVisuals.register(ModSpells.WIND_BLADE.get(), WindSlashRenderer::render);
            ProjectileVisuals.register(ModSpells.HYDRO_JET.get(), WaterSpearRenderer::render);
        });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CAST);
        event.register(SPELL_WHEEL);
        event.register(STATUS);
        event.register(DEV_MENU);
        event.register(HIDE_AURA);
        event.register(LAUNCH_ONE);
        event.register(LAUNCH_ALL);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ScreenEffects.tick();
        FrozenShells.tick();
        BubbleRenderer.tick();
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
                minecraft.setScreen(new StatusScreen());
            }
        }

        while (HIDE_AURA.consumeClick()) {
            if (minecraft.player != null && MagicAttachments.get(minecraft.player).isAwakened()) {
                PacketDistributor.sendToServer(AuraPayload.TOGGLE);
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
        event.registerAboveAll(ElementalArcana.id("screen_flash"), ScreenEffects.FLASH_LAYER);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ElementalArcana.id("mana_sense"), new ManaSenseLayer());
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ScreenEffects.onCameraAngles(event);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModContent.FROST_SPARKLE.get(), FrostSparkleParticle.Provider::new);
        event.registerSpriteSet(ModContent.ICE_SHARD.get(), IceShardParticle.Provider::new);
        event.registerSpriteSet(ModContent.FROST_MIST.get(), FrostMistParticle.Provider::new);
        event.registerSpriteSet(ModContent.WIND_STREAK.get(), WindStreakParticle.Provider::new);
        event.registerSpriteSet(ModContent.EMBER.get(), EmberParticle.Provider::new);
        event.registerSpriteSet(ModContent.HYDRO_DROP.get(), HydroDropParticle.Provider::new);
        event.registerSpecial(ModContent.HYDRO_STREAM.get(), new HydroStreamEmitter.Provider());
        event.registerSpriteSet(ModContent.SWIRL.get(), WindStreakParticle.SwirlProvider::new);
        event.registerSpriteSet(ModContent.FLARE.get(), sprites -> new GlowParticle.Provider(GlowParticle.Kind.FLARE, sprites));
        event.registerSpriteSet(ModContent.SPARK.get(), sprites -> new GlowParticle.Provider(GlowParticle.Kind.SPARK, sprites));
        event.registerSpriteSet(ModContent.FEATHER.get(), sprites -> new GlowParticle.Provider(GlowParticle.Kind.FEATHER, sprites));
        event.registerSpriteSet(ModContent.CINDER.get(), CinderParticle.Provider::new);
        event.registerSpriteSet(ModContent.SHOCKWAVE.get(), sprites -> new GlowParticle.Provider(GlowParticle.Kind.RING, sprites));
        event.registerSpriteSet(ModContent.CORONA.get(), sprites -> new GlowParticle.Provider(GlowParticle.Kind.CORONA, sprites));
        event.registerSpecial(ModContent.FIRE_BLAST.get(), new FireBlastEmitter.Provider());
        event.registerSpecial(ModContent.PYRONADO.get(), new PyronadoEmitter.Provider());
        event.registerSpecial(ModContent.TSUNAMI.get(), new TsunamiParticle.Provider());
        event.registerSpecial(ModContent.TREMOR.get(), new TremorEmitter.Provider());
        event.registerSpriteSet(ModContent.ARC.get(), LightningArcParticle.Provider::new);
        event.registerSpriteSet(ModContent.SMITE.get(), SmiteParticle.Provider::new);
        event.registerSpecial(ModContent.ICE_SHATTER.get(), new IceShatterEmitter.Provider());
        event.registerSpecial(ModContent.WIND_CUT.get(), new WindCutEmitter.Provider());
        event.registerSpecial(ModContent.WATER_BURST.get(), new WaterBurstParticle.Provider());
        event.registerSpecial(ModContent.BUBBLE_CAST.get(), new BubbleCastEmitter.Provider());
    }

    // Additive particles change the blend function and vanilla doesn't set it back; put it back
    // before anything drawn later assumes the default.
    @SubscribeEvent
    public static void afterParticles(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            Decals.render(event);
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            WaterBeams.render(event);
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            Bloom.renderParticles(event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    /** A Shrine Core's crystal takes its element's colour. */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0
                ? FastColor.ARGB32.opaque(state.getValue(ShrineCoreBlock.KIND).element().color()) : -1, ModWorld.SHRINE_CORE.get());
        // A tower's heart: its element's colour, dimmed once the tower is conquered.
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            int color = state.getValue(TowerHeartBlock.KIND).element().color();
            return FastColor.ARGB32.opaque(state.getValue(TowerHeartBlock.LIT) ? color
                    : FastColor.ARGB32.color(0, (color >> 16 & 0xFF) / 3, (color >> 8 & 0xFF) / 3, (color & 0xFF) / 3));
        }, ModTowers.TOWER_HEART.get());
    }

    /** A focus's gem (its second texture layer) takes its element's colour. */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            Element element = FocusItem.elementOf(stack);
            return tintIndex == 1 && element != null ? FastColor.ARGB32.opaque(element.color()) : -1;
        }, ModGear.APPRENTICE_WAND.get(), ModGear.ADEPT_STAFF.get(), ModGear.MASTER_STAFF.get(), ModGear.ARCHMAGE_STAFF.get(),
                ModTowers.GUARDIAN_CORE.get(), ModSanctums.SOVEREIGN_HEART.get());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.SPELL_PROJECTILE.get(), SpellProjectileRenderer::new);
        event.registerEntityRenderer(ModContent.EMBER_SPRITE.get(), EmberSpriteRenderer::new);
        event.registerEntityRenderer(ModContent.STORMEYE.get(), StormeyeRenderer::new);
        event.registerEntityRenderer(ModContent.RIFT.get(), RiftRenderer::new);
        event.registerEntityRenderer(ModContent.CRYSTAL_SPIRE.get(), CrystalSpireRenderer::new);
        for (Element element : Element.values()) {
            event.registerEntityRenderer(ModCreatures.wisp(element), WispRenderer::new);
            event.registerEntityRenderer(ModCreatures.familiar(element), WispRenderer::new);
        }
        event.registerEntityRenderer(ModTowers.ACOLYTE.get(), TowerMageRenderer::new);
        event.registerEntityRenderer(ModTowers.MAGISTER.get(), TowerMageRenderer::new);
        for (Element element : SovereignRules.ELEMENTS) {
            event.registerEntityRenderer(ModSanctums.sovereign(element), SovereignRenderer::new);
        }
        event.registerEntityRenderer(ModHollow.HOLLOW.get(), HollowRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WispModel.LAYER, WispModel::createLayer);
        event.registerLayerDefinition(SovereignModel.LAYER, SovereignModel::createLayer);
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
        models.forEach(model -> event.register(ModelResourceLocation.standalone(model)));
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Player player) {
            ShieldRenderer.renderShards(player, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource());
        }
        BubbleRenderer.render(event.getEntity(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        FrozenShells.render(event.getEntity(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource());
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
