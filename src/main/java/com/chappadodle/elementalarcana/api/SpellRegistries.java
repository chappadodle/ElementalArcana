package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The two registries addons register into, with a plain DeferredRegister:
 * {@code DeferredRegister.create(SpellRegistries.SPELL_KEY, "yourmod")}.
 * Both are synced so the client knows every spell/school id the server has.
 */
public final class SpellRegistries {
    public static final ResourceKey<Registry<SpellSchool>> SCHOOL_KEY = ResourceKey.createRegistryKey(ElementalArcana.id("school"));
    public static final ResourceKey<Registry<Spell>> SPELL_KEY = ResourceKey.createRegistryKey(ElementalArcana.id("spell"));

    public static final Registry<SpellSchool> SCHOOLS = new RegistryBuilder<>(SCHOOL_KEY).sync(true).create();
    public static final Registry<Spell> SPELLS = new RegistryBuilder<>(SPELL_KEY).sync(true).create();

    private SpellRegistries() {
    }
}
