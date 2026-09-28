package com.chappadodle.elementalarcana.api;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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

    /** Cooldown at a given spell level: 6% of the Lv 1 cooldown shorter per level (Lv 10 = 46%). */
    public int cooldownTicks(int spellLevel) {
        return Progression.cooldownTicks(cooldownTicks(), spellLevel);
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
