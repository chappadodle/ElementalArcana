package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.content.spell.BoulderSpell;
import com.chappadodle.elementalarcana.content.spell.BubblePrisonSpell;
import com.chappadodle.elementalarcana.content.spell.ChainLightningSpell;
import com.chappadodle.elementalarcana.content.spell.EmberSpriteSpell;
import com.chappadodle.elementalarcana.content.spell.FireballSpell;
import com.chappadodle.elementalarcana.content.spell.FrostNovaSpell;
import com.chappadodle.elementalarcana.content.spell.FrostShieldSpell;
import com.chappadodle.elementalarcana.content.spell.GaleDashSpell;
import com.chappadodle.elementalarcana.content.spell.HealingRainSpell;
import com.chappadodle.elementalarcana.content.spell.HydroJetSpell;
import com.chappadodle.elementalarcana.content.spell.IcicleSpell;
import com.chappadodle.elementalarcana.content.spell.PrismBoltSpell;
import com.chappadodle.elementalarcana.content.spell.PrismWardSpell;
import com.chappadodle.elementalarcana.content.spell.PyronadoSpell;
import com.chappadodle.elementalarcana.content.spell.SanctuarySpell;
import com.chappadodle.elementalarcana.content.spell.StormcallSpell;
import com.chappadodle.elementalarcana.content.spell.GeodeSentinelSpell;
import com.chappadodle.elementalarcana.content.spell.SmiteSpell;
import com.chappadodle.elementalarcana.content.spell.StoneSkinSpell;
import com.chappadodle.elementalarcana.content.spell.ThunderclapSpell;
import com.chappadodle.elementalarcana.content.spell.TsunamiSpell;
import com.chappadodle.elementalarcana.content.spell.TremorSpell;
import com.chappadodle.elementalarcana.content.spell.SkywardLeapSpell;
import com.chappadodle.elementalarcana.content.spell.StormeyeSpell;
import com.chappadodle.elementalarcana.content.spell.WindBladeSpell;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.chappadodle.elementalarcana.api.CantripRules;
import com.chappadodle.elementalarcana.content.cantrip.EffectCantripSpell;
import com.chappadodle.elementalarcana.content.cantrip.MageLightSpell;
import com.chappadodle.elementalarcana.content.cantrip.MendSpell;
import com.chappadodle.elementalarcana.content.cantrip.ProspectSpell;
import com.chappadodle.elementalarcana.content.cantrip.RecallSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;

/** Registered exactly the way an addon would register its own spells. Order = order in the wheel. */
public final class ModSpells {
    public static final DeferredRegister<Spell> SPELLS = DeferredRegister.create(SpellRegistries.SPELL_KEY, ElementalArcana.MODID);

    public static final DeferredHolder<Spell, FireballSpell> FIREBALL = SPELLS.register("fireball", FireballSpell::new);
    // Pyronado keeps the ring of fire's id, so those who learned Flame Burst have it now.
    public static final DeferredHolder<Spell, PyronadoSpell> PYRONADO = SPELLS.register("flame_burst", PyronadoSpell::new);
    public static final DeferredHolder<Spell, EmberSpriteSpell> EMBER_SPRITE = SPELLS.register("ember_sprite", EmberSpriteSpell::new);
    // Hydro Jet is registered first so it's water's starter spell on the Awakening screen.
    public static final DeferredHolder<Spell, HydroJetSpell> HYDRO_JET = SPELLS.register("hydro_jet", HydroJetSpell::new);
    // Tsunami keeps the cone of water's id, so those who learned Tidal Wave have it now.
    public static final DeferredHolder<Spell, TsunamiSpell> TSUNAMI = SPELLS.register("tidal_wave", TsunamiSpell::new);
    public static final DeferredHolder<Spell, BubblePrisonSpell> BUBBLE_PRISON = SPELLS.register("bubble_prison", BubblePrisonSpell::new);
    public static final DeferredHolder<Spell, HealingRainSpell> HEALING_RAIN = SPELLS.register("healing_rain", HealingRainSpell::new);
    public static final DeferredHolder<Spell, IcicleSpell> ICICLE = SPELLS.register("icicle", IcicleSpell::new);
    public static final DeferredHolder<Spell, FrostNovaSpell> FROST_NOVA = SPELLS.register("frost_nova", FrostNovaSpell::new);
    public static final DeferredHolder<Spell, FrostShieldSpell> FROST_SHIELD = SPELLS.register("frost_shield", FrostShieldSpell::new);
    // Wind Blade is registered first so it's wind's starter spell on the Awakening screen.
    public static final DeferredHolder<Spell, WindBladeSpell> WIND_BLADE = SPELLS.register("wind_blade", WindBladeSpell::new);
    public static final DeferredHolder<Spell, GaleDashSpell> GALE_DASH = SPELLS.register("gale_dash", GaleDashSpell::new);
    // Skyward Leap keeps the throw upward's id, so those who learned Updraft have it now.
    public static final DeferredHolder<Spell, SkywardLeapSpell> SKYWARD_LEAP = SPELLS.register("updraft", SkywardLeapSpell::new);
    public static final DeferredHolder<Spell, StormeyeSpell> STORMEYE = SPELLS.register("stormeye", StormeyeSpell::new);

    // Boulder is registered first so it's earth's starter spell.
    public static final DeferredHolder<Spell, BoulderSpell> BOULDER = SPELLS.register("boulder", BoulderSpell::new);
    public static final DeferredHolder<Spell, StoneSkinSpell> STONE_SKIN = SPELLS.register("stone_skin", StoneSkinSpell::new);
    public static final DeferredHolder<Spell, TremorSpell> TREMOR = SPELLS.register("tremor", TremorSpell::new);

    // The derived elements' first spells (docs/superpowers/specs/2026-10-03-derived-elements-design.md).
    public static final DeferredHolder<Spell, PrismBoltSpell> PRISM_BOLT = SPELLS.register("prism_bolt", PrismBoltSpell::new);
    public static final DeferredHolder<Spell, ChainLightningSpell> CHAIN_LIGHTNING = SPELLS.register("chain_lightning", ChainLightningSpell::new);
    public static final DeferredHolder<Spell, SmiteSpell> SMITE = SPELLS.register("smite", SmiteSpell::new);
    public static final DeferredHolder<Spell, PrismWardSpell> PRISM_WARD = SPELLS.register("prism_ward", PrismWardSpell::new);
    public static final DeferredHolder<Spell, ThunderclapSpell> THUNDERCLAP = SPELLS.register("thunderclap", ThunderclapSpell::new);
    public static final DeferredHolder<Spell, SanctuarySpell> SANCTUARY = SPELLS.register("sanctuary", SanctuarySpell::new);
    // The derived elements' third spells.
    public static final DeferredHolder<Spell, StormcallSpell> STORMCALL = SPELLS.register("stormcall", StormcallSpell::new);
    public static final DeferredHolder<Spell, GeodeSentinelSpell> GEODE_SENTINEL = SPELLS.register("geode_sentinel", GeodeSentinelSpell::new);

    // Cantrips (docs/superpowers/specs/2026-10-04-cantrips-design.md): the Arcane school, learned
    // from scrolls; last, so they follow a mage's own elements in the wheel.
    public static final DeferredHolder<Spell, MageLightSpell> MAGE_LIGHT = SPELLS.register("mage_light", MageLightSpell::new);
    public static final DeferredHolder<Spell, ProspectSpell> PROSPECT = SPELLS.register("prospect", ProspectSpell::new);
    public static final DeferredHolder<Spell, RecallSpell> RECALL = SPELLS.register("recall", RecallSpell::new);
    public static final DeferredHolder<Spell, MendSpell> MEND = SPELLS.register("mend", MendSpell::new);
    public static final DeferredHolder<Spell, EffectCantripSpell> WATER_BREATHING = SPELLS.register("water_breathing",
            () -> new EffectCantripSpell(15, 1200, MobEffects.WATER_BREATHING, CantripRules.WATER_BREATHING_TICKS, ParticleTypes.BUBBLE_POP,
                    SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE));
    public static final DeferredHolder<Spell, EffectCantripSpell> FEATHERFALL = SPELLS.register("featherfall",
            () -> new EffectCantripSpell(10, 600, MobEffects.SLOW_FALLING, CantripRules.FEATHERFALL_TICKS, ParticleTypes.CLOUD,
                    SoundEvents.PHANTOM_FLAP));
    public static final DeferredHolder<Spell, EffectCantripSpell> NIGHT_EYE = SPELLS.register("night_eye",
            () -> new EffectCantripSpell(10, 1200, MobEffects.NIGHT_VISION, CantripRules.NIGHT_EYE_TICKS, ParticleTypes.GLOW,
                    SoundEvents.AMETHYST_BLOCK_CHIME));

    private ModSpells() {
    }
}
