package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.spell.EmberSprite;
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

    /** Fire's Ember Sprite (content/spell/EmberSprite): a little fire spirit that spits bolts. */
    public static final DeferredHolder<EntityType<?>, EntityType<EmberSprite>> EMBER_SPRITE =
            ENTITY_TYPES.register("ember_sprite", () -> EntityType.Builder.<EmberSprite>of(EmberSprite::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(4).fireImmune()
                    .build(ElementalArcana.MODID + ":ember_sprite"));
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
    // Fireball's signature sounds (synthesized by tools/gen_spell_sounds.py).
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL_SUN_HUM = sound("spell.fireball.sun_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL_SUN_LAUNCH = sound("spell.fireball.sun_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL_SUN_ROAR = sound("spell.fireball.sun_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL_SUN_BLAST = sound("spell.fireball.sun_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL_METEOR_ROAR = sound("spell.fireball.meteor_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL_METEOR_IMPACT = sound("spell.fireball.meteor_impact");
    // Icicle's signature sounds (synthesized by tools/gen_spell_sounds.py).
    public static final DeferredHolder<SoundEvent, SoundEvent> ICICLE_LANCE_FORGE = sound("spell.icicle.lance_forge");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICICLE_LANCE_LAUNCH = sound("spell.icicle.lance_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICICLE_LANCE_QUAKE = sound("spell.icicle.lance_quake");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICICLE_WINTER_BLIZZARD = sound("spell.icicle.winter_blizzard");
    // Wind Blade's signature sounds (synthesized by tools/gen_spell_sounds.py).
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_SCYTHE_ROAR = sound("spell.wind_blade.scythe_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_SCYTHE_STRIKE = sound("spell.wind_blade.scythe_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_THOUSAND_SLASH = sound("spell.wind_blade.thousand_slash");
    // Hydro Jet's signature sounds (synthesized by tools/gen_spell_sounds.py).
    public static final DeferredHolder<SoundEvent, SoundEvent> HYDRO_LANCE_SURGE = sound("spell.hydro_jet.lance_surge");
    public static final DeferredHolder<SoundEvent, SoundEvent> HYDRO_LANCE_CRASH = sound("spell.hydro_jet.lance_crash");
    public static final DeferredHolder<SoundEvent, SoundEvent> HYDRO_MAELSTROM_SWIRL = sound("spell.hydro_jet.maelstrom_swirl");

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
     * Hydro Jet's stream, one tick of it in one packet: sent with count 0, its "velocity" is the
     * vector from the hand to where the stream lands (see HydroStreamOptions).
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<HydroStreamOptions>> HYDRO_STREAM =
            PARTICLES.register("hydro_stream", HydroStreamOptions::newType);
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

    /** A soft glowing blob that shrinks, shifts colour and dissolves: the body of fire trails. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<GlowParticleOptions>> FLARE =
            PARTICLES.register("flare", GlowParticleOptions::newType);
    /** A glowing streak that points along its motion and slows down. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<GlowParticleOptions>> SPARK =
            PARTICLES.register("spark", GlowParticleOptions::newType);
    /** A glowing feather that flutters down (Phoenix). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<GlowParticleOptions>> FEATHER =
            PARTICLES.register("feather", GlowParticleOptions::newType);
    /** A dark chip of rock with a glowing edge that falls and bounces (Meteor). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CINDER =
            PARTICLES.register("cinder", () -> new SimpleParticleType(false));

    /** A flat glowing ring that expands along the ground and fades: an explosion's shockwave. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<GlowParticleOptions>> SHOCKWAVE =
            PARTICLES.register("shockwave", GlowParticleOptions::newType);
    /** A soft round glow that swells and fades (Sunfire's corona). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<GlowParticleOptions>> CORONA =
            PARTICLES.register("corona", GlowParticleOptions::newType);
    /** A fireball's whole explosion in one particle (see FireBlastOptions). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<FireBlastOptions>> FIRE_BLAST =
            PARTICLES.register("fire_blast", FireBlastOptions::newType);
    /** Pyronado's wheels, for as long as they spin, in one particle (see PyronadoOptions). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<PyronadoOptions>> PYRONADO =
            PARTICLES.register("pyronado", PyronadoOptions::newType);

    /** An icicle's whole shatter in one particle (see IceShatterOptions). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<IceShatterOptions>> ICE_SHATTER =
            PARTICLES.register("ice_shatter", IceShatterOptions::newType);

    /** A Wind Blade's whole impact in one particle (see WindCutOptions). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<WindCutOptions>> WIND_CUT =
            PARTICLES.register("wind_cut", WindCutOptions::newType);

    /** A Bubble Prison cast: water cubes zipping to the target (see BubbleCastEmitter). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BUBBLE_CAST =
            PARTICLES.register("bubble_cast", () -> new SimpleParticleType(false));

    /** A whirlpool or a crashing wave, drawn out of water cubes (see WaterBurstOptions). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<WaterBurstOptions>> WATER_BURST =
            PARTICLES.register("water_burst", WaterBurstOptions::newType);

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

    /** Standing near a Shrine Core: faster mana (amplifier 1 when the shrine is your element's kin). */
    // A creature far above you weighs on your magic (see ManaWeather): less spell power and regeneration.
    public static final DeferredHolder<MobEffect, MobEffect> PRESSURE = EFFECTS.register("pressure",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x6A2A8A) {
            });
    public static final DeferredHolder<MobEffect, MobEffect> PLACE_OF_POWER = EFFECTS.register("place_of_power",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xB070FF) {
            });
    // A shrine's blessing, one per element family: +5 Affinity of that family and +2 Potency (see GearStats).
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_FIRE = blessingEffect("fire_blessing", Element.FIRE);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_WATER = blessingEffect("water_blessing", Element.WATER);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_WIND = blessingEffect("wind_blessing", Element.WIND);
    public static final DeferredHolder<MobEffect, MobEffect> BLESSING_EARTH = blessingEffect("earth_blessing", Element.EARTH);

    private static DeferredHolder<MobEffect, MobEffect> blessingEffect(String name, Element element) {
        return EFFECTS.register(name, () -> new MobEffect(MobEffectCategory.BENEFICIAL, element.color()) {
        });
    }

    /** The blessing of {@code family} (an element family: Fire, Water, Wind or Earth). */
    public static DeferredHolder<MobEffect, MobEffect> blessing(Element family) {
        return switch (family.family()) {
            case FIRE -> BLESSING_FIRE;
            case WIND -> BLESSING_WIND;
            case EARTH -> BLESSING_EARTH;
            default -> BLESSING_WATER;
        };
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        SOUNDS.register(modEventBus);
        EFFECTS.register(modEventBus);
        PARTICLES.register(modEventBus);
    }
}
