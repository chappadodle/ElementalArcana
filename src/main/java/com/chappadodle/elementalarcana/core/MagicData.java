package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything about a player's magic: level and XP, awakened elements (affinities), mana,
 * the selected spell and cooldowns. Stats like max mana, regen and power are derived from the
 * level, so balance lives in the constants below. Spells and schools are stored by id, so
 * removing an addon never corrupts a save.
 */
public final class MagicData {
    public static final int MAX_LEVEL = 30;
    public static final int[] AFFINITY_SLOT_LEVELS = {1, 10, 20, 30};
    private static final float BASE_MAX_MANA = 100f;
    private static final float MAX_MANA_PER_LEVEL = 10f;
    private static final float POWER_PER_LEVEL = 0.02f;

    public static final Codec<MagicData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("mana", BASE_MAX_MANA).forGetter(data -> data.mana),
            Codec.INT.optionalFieldOf("level", 1).forGetter(data -> data.level),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(data -> data.xp),
            ResourceLocation.CODEC.listOf().optionalFieldOf("affinities", List.of()).forGetter(data -> data.affinities),
            ResourceLocation.CODEC.optionalFieldOf("selected").forGetter(data -> Optional.ofNullable(data.selected)),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).optionalFieldOf("cooldowns", Map.of()).forGetter(data -> data.cooldownEnds),
            Codec.BOOL.optionalFieldOf("fall_immune", false).forGetter(data -> data.fallImmune),
            Codec.BOOL.optionalFieldOf("free_cast", false).forGetter(data -> data.freeCast),
            Codec.unboundedMap(ResourceLocation.CODEC, SpellProgress.CODEC).optionalFieldOf("spells", Map.of()).forGetter(data -> data.spells),
            Codec.LONG.optionalFieldOf("respec_ready_at", 0L).forGetter(data -> data.respecReadyAt)
    ).apply(instance, (mana, level, xp, affinities, selected, cooldowns, fallImmune, freeCast, spells, respecReadyAt) ->
            new MagicData(mana, level, xp, affinities, selected.orElse(null), cooldowns, fallImmune, freeCast, spells, respecReadyAt, false)));

    // What the owning client needs: no fall flag, plus the live meditation state for the HUD.
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeFloat(data.mana);
                buf.writeVarInt(data.level);
                buf.writeVarInt(data.xp);
                buf.writeCollection(data.affinities, FriendlyByteBuf::writeResourceLocation);
                buf.writeNullable(data.selected, FriendlyByteBuf::writeResourceLocation);
                buf.writeMap(data.cooldownEnds, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeVarLong);
                buf.writeBoolean(data.freeCast);
                buf.writeMap(data.spells, FriendlyByteBuf::writeResourceLocation, SpellProgress::write);
                buf.writeVarLong(data.respecReadyAt);
                buf.writeBoolean(data.meditating);
            },
            buf -> new MagicData(
                    buf.readFloat(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readList(FriendlyByteBuf::readResourceLocation),
                    buf.readNullable(FriendlyByteBuf::readResourceLocation),
                    buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readVarLong),
                    false,
                    buf.readBoolean(),
                    buf.readMap(FriendlyByteBuf::readResourceLocation, SpellProgress::read),
                    buf.readVarLong(),
                    buf.readBoolean()));

    private float mana;
    private int level;
    private int xp;
    private final List<ResourceLocation> affinities;
    @Nullable
    private ResourceLocation selected;
    private final HashMap<ResourceLocation, Long> cooldownEnds;
    private boolean fallImmune;
    // Dev-menu toggle: casting costs no mana and triggers no cooldowns.
    private boolean freeCast;
    private final HashMap<ResourceLocation, SpellProgress> spells;
    private long respecReadyAt;

    // Server-side meditation tracking; only `meditating` is synced.
    private boolean meditating;
    private int stillTicks;
    private double lastX;
    private double lastZ;

    public MagicData() {
        this(BASE_MAX_MANA, 1, 0, List.of(), null, Map.of(), false, false, Map.of(), 0L, false);
    }

    private MagicData(float mana, int level, int xp, List<ResourceLocation> affinities, @Nullable ResourceLocation selected,
                      Map<ResourceLocation, Long> cooldownEnds, boolean fallImmune, boolean freeCast,
                      Map<ResourceLocation, SpellProgress> spells, long respecReadyAt, boolean meditating) {
        this.level = Mth.clamp(level, 1, MAX_LEVEL);
        this.xp = xp;
        this.affinities = new ArrayList<>(affinities);
        this.selected = selected;
        this.cooldownEnds = new HashMap<>(cooldownEnds);
        this.fallImmune = fallImmune;
        this.freeCast = freeCast;
        this.spells = new HashMap<>(spells);
        this.respecReadyAt = respecReadyAt;
        this.meditating = meditating;
        this.mana = Mth.clamp(mana, 0f, maxMana());
    }

    // ---- level & stats ----

    public int level() {
        return level;
    }

    public int xp() {
        return xp;
    }

    public int xpToNextLevel() {
        return level >= MAX_LEVEL ? 0 : 40 + 20 * level;
    }

    /** Adds XP and returns how many levels were gained. */
    public int addXp(int amount) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        xp += amount;
        int gained = 0;
        while (level < MAX_LEVEL && xp >= xpToNextLevel()) {
            xp -= xpToNextLevel();
            level++;
            gained++;
        }
        if (level >= MAX_LEVEL) {
            xp = 0;
        }
        return gained;
    }

    public void setLevel(int level) {
        this.level = Mth.clamp(level, 1, MAX_LEVEL);
        this.xp = 0;
        this.mana = Math.min(mana, maxMana());
    }

    public float maxMana() {
        return BASE_MAX_MANA + MAX_MANA_PER_LEVEL * (level - 1);
    }

    public float regenPerSecond() {
        return Progression.regenPerSecond(level);
    }

    /** Multiplier spells apply to damage, knockback and durations. */
    public float power() {
        return 1f + POWER_PER_LEVEL * (level - 1);
    }

    // ---- mana ----

    public float mana() {
        return mana;
    }

    public void setMana(float mana) {
        this.mana = Mth.clamp(mana, 0f, maxMana());
    }

    public void fillMana() {
        mana = maxMana();
    }

    /** Returns true when the whole-number value changed, i.e. when the client needs an update. */
    public boolean regenerate(float amount) {
        if (mana >= maxMana()) {
            return false;
        }
        int before = (int) mana;
        setMana(mana + amount);
        return (int) mana != before;
    }

    // ---- affinities ----

    public List<ResourceLocation> affinities() {
        return Collections.unmodifiableList(affinities);
    }

    public boolean isAwakened() {
        return !affinities.isEmpty();
    }

    public int affinitySlots() {
        int slots = 0;
        for (int slotLevel : AFFINITY_SLOT_LEVELS) {
            if (level >= slotLevel) {
                slots++;
            }
        }
        return slots;
    }

    public boolean hasFreeAffinitySlot() {
        return affinities.size() < Math.min(affinitySlots(), SpellRegistries.SCHOOLS.size());
    }

    public boolean hasAffinity(SpellSchool school) {
        return affinities.contains(school.id());
    }

    /** Awakens a new element if there's a free slot. Selects its first spell if nothing is selected. */
    public boolean awaken(SpellSchool school) {
        if (hasAffinity(school) || !hasFreeAffinitySlot()) {
            return false;
        }
        affinities.add(school.id());
        if (selectedSpell() == null) {
            castableSpells().stream().findFirst().ifPresent(spell -> selected = spell.id());
        }
        return true;
    }

    public void forceAffinity(SpellSchool school) {
        if (!hasAffinity(school)) {
            affinities.add(school.id());
        }
    }

    public void removeAffinity(SpellSchool school) {
        affinities.remove(school.id());
    }

    public void clearAffinities() {
        affinities.clear();
        selected = null;
    }

    // ---- spells ----

    public boolean canCast(Spell spell) {
        return hasAffinity(spell.school()) && level >= spell.requiredLevel();
    }

    /** Castable spells in registry order - the order the wheel and status window use. */
    public List<Spell> castableSpells() {
        List<Spell> spells = new ArrayList<>();
        for (Spell spell : SpellRegistries.SPELLS) {
            if (canCast(spell)) {
                spells.add(spell);
            }
        }
        return spells;
    }

    @Nullable
    public Spell selectedSpell() {
        Spell spell = selected == null ? null : SpellRegistries.SPELLS.get(selected);
        return spell != null && canCast(spell) ? spell : null;
    }

    public boolean select(ResourceLocation spellId) {
        Spell spell = SpellRegistries.SPELLS.get(spellId);
        if (spell == null || !canCast(spell)) {
            return false;
        }
        selected = spellId;
        return true;
    }

    // ---- spell levels & skill points ----
    // Every Magic Level after the first earns a skill point; every spell level after the first
    // costs one. Points are derived from those, so they can never drift out of sync.

    private static final SpellProgress NO_PROGRESS = new SpellProgress();

    public SpellProgress progress(Spell spell) {
        return spells.getOrDefault(spell.id(), NO_PROGRESS);
    }

    private SpellProgress editProgress(Spell spell) {
        return spells.computeIfAbsent(spell.id(), id -> new SpellProgress());
    }

    public int spellLevel(Spell spell) {
        return Math.min(progress(spell).level(), spell.maxLevel());
    }

    public int skillPoints() {
        int spent = 0;
        for (SpellProgress progress : spells.values()) {
            spent += progress.level() - 1;
        }
        return Math.max(0, (level - 1) - spent);
    }

    /** Mastery needed for the next level of {@code spell}; 0 at max level. */
    public int masteryToNextLevel(Spell spell) {
        int spellLevel = spellLevel(spell);
        return spellLevel >= spell.maxLevel() ? 0 : spell.masteryToNextLevel(spellLevel);
    }

    /** Mastery fills up to the next level's requirement, then waits for a skill point. */
    public void addMastery(Spell spell, int amount) {
        int needed = masteryToNextLevel(spell);
        if (needed > 0 && amount > 0) {
            SpellProgress progress = editProgress(spell);
            progress.setMastery(Math.min(needed, progress.mastery() + amount));
        }
    }

    public boolean isMasteryFull(Spell spell) {
        int needed = masteryToNextLevel(spell);
        return needed > 0 && progress(spell).mastery() >= needed;
    }

    public boolean canLevelUp(Spell spell) {
        return canCast(spell) && isMasteryFull(spell) && skillPoints() > 0;
    }

    public boolean levelUp(Spell spell) {
        if (!canLevelUp(spell)) {
            return false;
        }
        SpellProgress progress = editProgress(spell);
        progress.setLevel(progress.level() + 1);
        progress.setMastery(0);
        return true;
    }

    /** The lowest reached level whose branch choice hasn't been made yet, or -1. */
    public int pendingBranchLevel(Spell spell) {
        int spellLevel = spellLevel(spell);
        for (int level = 1; level <= spellLevel; level++) {
            if (!spell.branchOptions(level).isEmpty() && !progress(spell).branches().containsKey(level)) {
                return level;
            }
        }
        return -1;
    }

    public boolean chooseBranch(Spell spell, int level, String branch) {
        if (level > spellLevel(spell) || !spell.branchOptions(level).contains(branch) || progress(spell).branches().containsKey(level)) {
            return false;
        }
        editProgress(spell).setBranch(level, branch);
        return true;
    }

    public long respecReadyAt() {
        return respecReadyAt;
    }

    /** Clears this spell's branch choices so they can be picked again; starts the respec cooldown. */
    public void respec(Spell spell, long gameTime, long cooldownTicks) {
        editProgress(spell).clearBranches();
        respecReadyAt = gameTime + cooldownTicks;
    }

    /** Dev/admin: set a spell's level directly (no skill points), dropping choices above it. */
    public void setSpellLevel(Spell spell, int spellLevel) {
        SpellProgress progress = editProgress(spell);
        progress.setLevel(Mth.clamp(spellLevel, 1, spell.maxLevel()));
        progress.setMastery(0);
        progress.clearBranchesAbove(progress.level());
    }

    /** Dev/admin: fill the mastery bar toward the next level. */
    public void fillMastery(Spell spell) {
        addMastery(spell, masteryToNextLevel(spell));
    }

    /** Dev/admin: clear branch choices without the respec cost. */
    public void clearBranches(Spell spell) {
        editProgress(spell).clearBranches();
    }

    // ---- cooldowns ----

    public long cooldownRemaining(ResourceLocation spell, long gameTime) {
        return Math.max(0, cooldownEnds.getOrDefault(spell, 0L) - gameTime);
    }

    public void startCooldown(ResourceLocation spell, long gameTime, int ticks) {
        cooldownEnds.values().removeIf(end -> end <= gameTime);
        if (ticks > 0) {
            cooldownEnds.put(spell, gameTime + ticks);
        }
    }

    public void clearCooldowns() {
        cooldownEnds.clear();
    }

    /** Takes {@code fraction} off the remaining cooldown of {@code spell}. */
    public void reduceCooldown(ResourceLocation spell, long gameTime, float fraction) {
        Long end = cooldownEnds.get(spell);
        if (end != null && end > gameTime) {
            cooldownEnds.put(spell, end - (long) ((end - gameTime) * fraction));
        }
    }

    // ---- movement-related state ----

    public boolean fallImmune() {
        return fallImmune;
    }

    public void setFallImmune(boolean fallImmune) {
        this.fallImmune = fallImmune;
    }

    public boolean freeCast() {
        return freeCast;
    }

    public void setFreeCast(boolean freeCast) {
        this.freeCast = freeCast;
    }

    public boolean meditating() {
        return meditating;
    }

    /**
     * Server-side, once per tick. Meditation starts after standing still while sneaking for
     * {@code ticksToMeditate}. Returns true when the meditating state flipped.
     */
    public boolean updateMeditation(double x, double z, boolean sneaking, boolean onGround, int ticksToMeditate) {
        boolean still = sneaking && onGround && Math.abs(x - lastX) < 0.01 && Math.abs(z - lastZ) < 0.01;
        lastX = x;
        lastZ = z;
        stillTicks = still ? stillTicks + 1 : 0;
        boolean now = stillTicks >= ticksToMeditate;
        boolean changed = now != meditating;
        meditating = now;
        return changed;
    }

    public void interruptMeditation() {
        stillTicks = 0;
        meditating = false;
    }
}
