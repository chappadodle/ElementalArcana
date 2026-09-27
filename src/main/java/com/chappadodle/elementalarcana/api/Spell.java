package com.chappadodle.elementalarcana.api;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Base class for every spell. Mana, cooldowns, affinity/level checks, XP, sounds and syncing
 * are all handled by the casting pipeline - a spell only describes its effect.
 *
 * <p>A player can cast a spell once they have awakened its school and reached its
 * {@link #requiredLevel()}. To add one: subclass this, register it on
 * {@link SpellRegistries#SPELL_KEY}, add the lang keys {@code spell.<namespace>.<path>} and
 * {@code spell.<namespace>.<path>.desc}, and a 16x16 icon at
 * {@code assets/<namespace>/textures/spell/<path>.png}. Projectile spells also implement
 * {@link ProjectileSpell} and reuse the shared {@link SpellProjectile}.
 */
public abstract class Spell {
    private final Supplier<? extends SpellSchool> school;
    private final int manaCost;
    private final int cooldownTicks;
    private final int requiredLevel;

    protected Spell(Supplier<? extends SpellSchool> school, int manaCost, int cooldownTicks) {
        this(school, manaCost, cooldownTicks, 1);
    }

    protected Spell(Supplier<? extends SpellSchool> school, int manaCost, int cooldownTicks, int requiredLevel) {
        this.school = school;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.requiredLevel = requiredLevel;
    }

    /** Magic level needed (within the spell's school) before it can be cast. */
    public int requiredLevel() {
        return requiredLevel;
    }

    /** Runs server-side, after all checks passed. Return a failure to refund the cast. */
    public abstract CastResult cast(CastContext context);

    public SpellSchool school() {
        return school.get();
    }

    public int manaCost() {
        return manaCost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public ResourceLocation id() {
        return SpellRegistries.SPELLS.getKey(this);
    }

    public Component displayName() {
        return Component.translatable(Util.makeDescriptionId("spell", id()));
    }

    /** One short line for tooltips and the spellbook: lang key {@code spell.<namespace>.<path>.desc}. */
    public Component description() {
        return Component.translatable(Util.makeDescriptionId("spell", id()) + ".desc");
    }

    public ResourceLocation iconTexture() {
        return id().withPath(path -> "textures/spell/" + path + ".png");
    }
}
