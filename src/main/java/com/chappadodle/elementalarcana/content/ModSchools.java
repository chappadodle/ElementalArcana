package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSchools {
    public static final DeferredRegister<SpellSchool> SCHOOLS = DeferredRegister.create(SpellRegistries.SCHOOL_KEY, ElementalArcana.MODID);

    public static final DeferredHolder<SpellSchool, SpellSchool> FIRE =
            SCHOOLS.register("fire", () -> new SpellSchool(0xFF7A1F, () -> SoundEvents.FIRECHARGE_USE));
    public static final DeferredHolder<SpellSchool, SpellSchool> WATER =
            SCHOOLS.register("water", () -> new SpellSchool(0x3B82F6, () -> SoundEvents.PLAYER_SPLASH_HIGH_SPEED));
    public static final DeferredHolder<SpellSchool, SpellSchool> ICE =
            SCHOOLS.register("ice", () -> new SpellSchool(0x9EE6FF, () -> SoundEvents.AMETHYST_BLOCK_CHIME));
    public static final DeferredHolder<SpellSchool, SpellSchool> WIND =
            SCHOOLS.register("wind", () -> new SpellSchool(0xCFEFE0, () -> SoundEvents.WIND_CHARGE_THROW));

    public static final DeferredHolder<SpellSchool, SpellSchool> EARTH =
            SCHOOLS.register("earth", () -> new SpellSchool(0xB5895A, () -> SoundEvents.STONE_PLACE));

    // The derived elements (docs/superpowers/specs/2026-10-03-derived-elements-design.md).
    public static final DeferredHolder<SpellSchool, SpellSchool> CRYSTAL =
            SCHOOLS.register("crystal", () -> new SpellSchool(0xD08CFF, () -> SoundEvents.AMETHYST_BLOCK_HIT));
    public static final DeferredHolder<SpellSchool, SpellSchool> LIGHTNING =
            SCHOOLS.register("lightning", () -> new SpellSchool(0xFFE14D, () -> SoundEvents.LIGHTNING_BOLT_IMPACT));
    public static final DeferredHolder<SpellSchool, SpellSchool> RADIANCE =
            SCHOOLS.register("radiance", () -> new SpellSchool(0xFFF1B8, () -> SoundEvents.BEACON_POWER_SELECT));

    // Cantrips (docs/superpowers/specs/2026-10-04-cantrips-design.md): everyday magic of no element,
    // learned from scrolls by any awakened mage.
    public static final DeferredHolder<SpellSchool, SpellSchool> ARCANE =
            SCHOOLS.register("arcane", () -> new SpellSchool(0xC9A8FF, () -> SoundEvents.ENCHANTMENT_TABLE_USE));

    private ModSchools() {
    }
}
