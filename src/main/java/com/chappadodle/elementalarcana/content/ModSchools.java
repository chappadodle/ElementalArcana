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

    private ModSchools() {
    }
}
