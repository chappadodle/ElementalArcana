package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The shared spell projectile, custom sounds and particles, and the mod's mob effects. */
public final class ModContent {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ElementalArcana.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ElementalArcana.MODID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, ElementalArcana.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SpellProjectile>> SPELL_PROJECTILE =
            ENTITY_TYPES.register("spell_projectile", () -> EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ElementalArcana.MODID + ":spell_projectile"));

    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKEN_SOUND = sound("magic.awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVEL_UP_SOUND = sound("magic.level_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIZZLE_SOUND = sound("spell.fizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICICLE_IMPACT = sound("spell.icicle.impact");

    /** A glowing 4-point frost glint that twinkles out. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FROST_SPARKLE =
            PARTICLES.register("frost_sparkle", () -> new SimpleParticleType(false));
    /** A small ice fragment that tumbles and falls. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ICE_SHARD =
            PARTICLES.register("ice_shard", () -> new SimpleParticleType(false));
    /** A soft cold puff that spreads and fades. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FROST_MIST =
            PARTICLES.register("frost_mist", () -> new SimpleParticleType(false));
    /** A glowing spark that drifts up and cools as it fades. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EMBER =
            PARTICLES.register("ember", () -> new SimpleParticleType(false));
    /** A water droplet that flies along its velocity, sags a little and fades. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HYDRO_DROP =
            PARTICLES.register("hydro_drop", () -> new SimpleParticleType(false));
    /**
     * Hydro Jet's stream, drawn client-side in one packet: sent with count 0, its "velocity" is
     * the vector from the hand to where the stream lands. Normal, thin (Tidecutter) and wide (Torrent).
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HYDRO_STREAM =
            PARTICLES.register("hydro_stream", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HYDRO_STREAM_THIN =
            PARTICLES.register("hydro_stream_thin", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HYDRO_STREAM_WIDE =
            PARTICLES.register("hydro_stream_wide", () -> new SimpleParticleType(true));
    /** A pale curl of wind that glides along its velocity and fades. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WIND_STREAK =
            PARTICLES.register("wind_streak", () -> new SimpleParticleType(false));
    /** A wind curl tinted with an element's color, for Swirl rings. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> SWIRL =
            PARTICLES.register("swirl", () -> new ParticleType<ColorParticleOption>(false) {
                @Override
                public MapCodec<ColorParticleOption> codec() {
                    return ColorParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                    return ColorParticleOption.streamCodec(this);
                }
            });

    /** Moderate exhaustion from emptying your mana: slower, weaker, and slower mana regen. */
    public static final DeferredHolder<MobEffect, MobEffect> MANA_SICKNESS = EFFECTS.register("mana_sickness",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x7A3FA0) {
            }
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, ElementalArcana.id("mana_sickness_speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, ElementalArcana.id("mana_sickness_damage"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Frozen solid (Icicle's Deep Freeze): rooted in place, no jumping or melee damage. */
    public static final DeferredHolder<MobEffect, FrozenEffect> FROZEN = EFFECTS.register("frozen", FrozenEffect::new);

    /** Launched by wind: takes 25% more damage while off the ground (see ElementalReactions). */
    public static final DeferredHolder<MobEffect, MobEffect> AIRBORNE = EFFECTS.register("airborne",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xCFEFE0) {
            });

    /** Soaked by water (the Hydro aura): drips, can't stay on fire, and sets up Vaporize and Freeze. */
    public static final DeferredHolder<MobEffect, WetEffect> WET = EFFECTS.register("wet", WetEffect::new);

    /** Hydro Jet's Riptide mark: the next hit from a player bursts for bonus damage (see WaterEvents). */
    public static final DeferredHolder<MobEffect, RiptideEffect> RIPTIDE = EFFECTS.register("riptide", RiptideEffect::new);

    private ModContent() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ElementalArcana.id(name)));
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        SOUNDS.register(modEventBus);
        EFFECTS.register(modEventBus);
        PARTICLES.register(modEventBus);
    }
}
