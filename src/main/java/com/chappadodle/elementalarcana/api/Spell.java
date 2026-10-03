package com.chappadodle.elementalarcana.api;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
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

    /** Cooldown at Lv 1, in ticks. */
    public int cooldownTicks() {
        return cooldownTicks;
    }

    /**
     * Cooldown with this spell at {@code spellLevel}, for a caster whose Focus gives
     * {@code cooldownFactor} (see StatRules#cooldownFactor): 6% of the Lv 1 cooldown shorter per
     * spell level, times the factor.
     */
    public int cooldownTicks(int spellLevel, float cooldownFactor) {
        return Progression.cooldownTicks(cooldownTicks(), spellLevel, cooldownFactor);
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

    // ---- spell levels ----
    // A spell levels up when its mastery bar is full (mastery = mana spent casting it) and the
    // player spends a skill point. Some levels offer a permanent branch choice.

    /** Highest level this spell can reach. 1 = the spell doesn't level. */
    public int maxLevel() {
        return 1;
    }

    /** Mastery needed to go from {@code level} to {@code level + 1}. */
    public int masteryToNextLevel(int level) {
        return 60 * level;
    }

    /** The branch choices offered on reaching {@code level}, or empty when that level has no choice. */
    public List<String> branchOptions(int level) {
        return List.of();
    }

    /**
     * Whether pressing cast right now uses this spell for free (Skyward Leap's Rising Current: an
     * updraft while gliding): no mana and no cooldown, even while it recharges. {@link #cast} is
     * still what runs, and tells the two apart. Asked on the server.
     */
    public boolean hasFreeUse(ServerPlayer player) {
        return false;
    }

    /**
     * How many uses this spell holds at {@code spellLevel}, each coming back a cooldown after the
     * last (see SpellCharges). 1: an ordinary cooldown.
     */
    public int charges(int spellLevel) {
        return 1;
    }

    /** Mana cost at a given spell level. */
    public int manaCost(int spellLevel) {
        return manaCost();
    }

    /** Lang key {@code spell.<namespace>.<path>.tier.<level>}. */
    public Component tierName(int level) {
        return Component.translatable(Util.makeDescriptionId("spell", id()) + ".tier." + level);
    }

    /** Lang key {@code spell.<namespace>.<path>.tier.<level>.desc}. */
    public Component tierDescription(int level) {
        return Component.translatable(Util.makeDescriptionId("spell", id()) + ".tier." + level + ".desc");
    }

    /** Lang key {@code spell.<namespace>.<path>.branch.<branch>}. */
    public Component branchName(String branch) {
        return Component.translatable(Util.makeDescriptionId("spell", id()) + ".branch." + branch);
    }

    /** Lang key {@code spell.<namespace>.<path>.branch.<branch>.desc}. */
    public Component branchDescription(String branch) {
        return Component.translatable(Util.makeDescriptionId("spell", id()) + ".branch." + branch + ".desc");
    }

    public ResourceLocation iconTexture() {
        return id().withPath(path -> "textures/spell/" + path + ".png");
    }
}
