package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.content.spell.BoulderSpell;
import com.chappadodle.elementalarcana.content.spell.BubblePrisonSpell;
import com.chappadodle.elementalarcana.content.spell.ChainLightningSpell;
import com.chappadodle.elementalarcana.content.spell.FireballSpell;
import com.chappadodle.elementalarcana.content.spell.FlameBurstSpell;
import com.chappadodle.elementalarcana.content.spell.FrostNovaSpell;
import com.chappadodle.elementalarcana.content.spell.FrostShieldSpell;
import com.chappadodle.elementalarcana.content.spell.GaleDashSpell;
import com.chappadodle.elementalarcana.content.spell.HealingRainSpell;
import com.chappadodle.elementalarcana.content.spell.HydroJetSpell;
import com.chappadodle.elementalarcana.content.spell.IcicleSpell;
import com.chappadodle.elementalarcana.content.spell.PrismBoltSpell;
import com.chappadodle.elementalarcana.content.spell.PrismWardSpell;
import com.chappadodle.elementalarcana.content.spell.SanctuarySpell;
import com.chappadodle.elementalarcana.content.spell.SmiteSpell;
import com.chappadodle.elementalarcana.content.spell.StoneSkinSpell;
import com.chappadodle.elementalarcana.content.spell.ThunderclapSpell;
import com.chappadodle.elementalarcana.content.spell.TidalWaveSpell;
import com.chappadodle.elementalarcana.content.spell.TremorSpell;
import com.chappadodle.elementalarcana.content.spell.UpdraftSpell;
import com.chappadodle.elementalarcana.content.spell.WindBladeSpell;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registered exactly the way an addon would register its own spells. Order = order in the wheel. */
public final class ModSpells {
    public static final DeferredRegister<Spell> SPELLS = DeferredRegister.create(SpellRegistries.SPELL_KEY, ElementalArcana.MODID);

    public static final DeferredHolder<Spell, FireballSpell> FIREBALL = SPELLS.register("fireball", FireballSpell::new);
    public static final DeferredHolder<Spell, FlameBurstSpell> FLAME_BURST = SPELLS.register("flame_burst", FlameBurstSpell::new);
    // Hydro Jet is registered first so it's water's starter spell on the Awakening screen.
    public static final DeferredHolder<Spell, HydroJetSpell> HYDRO_JET = SPELLS.register("hydro_jet", HydroJetSpell::new);
    public static final DeferredHolder<Spell, TidalWaveSpell> TIDAL_WAVE = SPELLS.register("tidal_wave", TidalWaveSpell::new);
    public static final DeferredHolder<Spell, BubblePrisonSpell> BUBBLE_PRISON = SPELLS.register("bubble_prison", BubblePrisonSpell::new);
    public static final DeferredHolder<Spell, HealingRainSpell> HEALING_RAIN = SPELLS.register("healing_rain", HealingRainSpell::new);
    public static final DeferredHolder<Spell, IcicleSpell> ICICLE = SPELLS.register("icicle", IcicleSpell::new);
    public static final DeferredHolder<Spell, FrostNovaSpell> FROST_NOVA = SPELLS.register("frost_nova", FrostNovaSpell::new);
    public static final DeferredHolder<Spell, FrostShieldSpell> FROST_SHIELD = SPELLS.register("frost_shield", FrostShieldSpell::new);
    // Wind Blade is registered first so it's wind's starter spell on the Awakening screen.
    public static final DeferredHolder<Spell, WindBladeSpell> WIND_BLADE = SPELLS.register("wind_blade", WindBladeSpell::new);
    public static final DeferredHolder<Spell, GaleDashSpell> GALE_DASH = SPELLS.register("gale_dash", GaleDashSpell::new);
    public static final DeferredHolder<Spell, UpdraftSpell> UPDRAFT = SPELLS.register("updraft", UpdraftSpell::new);

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

    private ModSpells() {
    }
}
