package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.api.AffinityRules;
import com.chappadodle.elementalarcana.api.AwakeningRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Keystones;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.SchoolElements;
import com.chappadodle.elementalarcana.api.SkillTree;
import com.chappadodle.elementalarcana.api.SkillTrees;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellCharges;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.api.Stat;
import com.chappadodle.elementalarcana.api.StatRules;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Everything about a player's magic: level and XP, stat points, awakened elements (affinities),
 * mana, the selected spell and cooldowns. Max mana, regen, spell power and cooldowns are derived
 * from the level and stats (see StatRules), so balance lives there. Spells and schools are stored by id, so
 * removing an addon never corrupts a save.
 */
public final class MagicData {
    public static final int MAX_LEVEL = Progression.MAX_LEVEL;
    private static final float START_MANA = 100f;

    public static final Codec<MagicData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("mana", START_MANA).forGetter(data -> data.mana),
            Codec.INT.optionalFieldOf("level", 1).forGetter(data -> data.level),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(data -> data.xp),
            ResourceLocation.CODEC.listOf().optionalFieldOf("affinities", List.of()).forGetter(data -> data.affinities),
            ResourceLocation.CODEC.optionalFieldOf("selected").forGetter(data -> Optional.ofNullable(data.selected)),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).optionalFieldOf("cooldowns", Map.of()).forGetter(data -> data.cooldownEnds),
            Codec.BOOL.optionalFieldOf("fall_immune", false).forGetter(data -> data.fallImmune),
            Codec.BOOL.optionalFieldOf("free_cast", false).forGetter(data -> data.freeCast),
            Codec.unboundedMap(ResourceLocation.CODEC, SpellProgress.CODEC).optionalFieldOf("spells", Map.of()).forGetter(data -> data.spells),
            // Bonus tree points condensed from Essence (saved under its old name).
            Codec.INT.optionalFieldOf("bonus_skill_points", 0).forGetter(data -> data.bonusTreePoints),
            StatPoints.CODEC.optionalFieldOf("stats", new StatPoints()).forGetter(data -> data.stats),
            Codec.STRING.listOf().optionalFieldOf("tree", List.of()).forGetter(data -> List.copyOf(data.treeNodes)),
            AwakeningState.CODEC.optionalFieldOf("awakening", new AwakeningState()).forGetter(data -> data.awakening),
            ResourceLocation.CODEC.listOf().optionalFieldOf("cantrips", List.of()).forGetter(data -> List.copyOf(data.cantrips))
    ).apply(instance, (mana, level, xp, affinities, selected, cooldowns, fallImmune, freeCast, spells, bonusTreePoints, stats, tree, awakening,
                       cantrips) ->
            new MagicData(mana, level, xp, affinities, selected.orElse(null), cooldowns, fallImmune, freeCast, spells,
                    bonusTreePoints, stats, tree, awakening, false, 0, Map.of()).withCantrips(cantrips)));

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
                buf.writeVarInt(data.bonusTreePoints);
                StatPoints.write(buf, data.stats);
                buf.writeCollection(data.treeNodes, FriendlyByteBuf::writeUtf);
                AwakeningState.write(buf, data.awakening);
                buf.writeBoolean(data.meditating);
                buf.writeVarInt(data.conjured);
                buf.writeMap(data.gearStats, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
                buf.writeBoolean(data.auraHidden);
                buf.writeFloat(data.weather);
                buf.writeCollection(data.cantrips, FriendlyByteBuf::writeResourceLocation);
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
                    buf.readVarInt(),
                    StatPoints.read(buf),
                    buf.readList(FriendlyByteBuf::readUtf),
                    AwakeningState.read(buf),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt)).withAura(buf.readBoolean(), buf.readFloat())
                    .withCantrips(buf.readList(FriendlyByteBuf::readResourceLocation)));

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
    // Tree points condensed from Elemental Essence, on top of those from levels.
    private int bonusTreePoints;
    private final StatPoints stats;
    // The skill tree nodes this player took (their starts come from their elements).
    private final Set<String> treeNodes;
    // The awakening clock and Catalyst failures.
    private final AwakeningState awakening;
    // What the held nodes give, cached until the nodes, the elements or the tree change.
    @Nullable
    private SkillTree.Grants grants;
    @Nullable
    private Set<String> held;
    private int grantsVersion = -1;

    // Server-side meditation tracking; only `meditating` is synced.
    private boolean meditating;
    // How many projectiles are conjured and held right now (see Conjuring). Synced, never saved:
    // the client uses it to know whether the mouse launches spells.
    private int conjured;
    // Stat points from worn and held gear (see content/gear/GearStats). Synced, never saved: the
    // server works it out from the equipment every second.
    private final Map<String, Integer> gearStats;
    // Mana sense and weather (see content/ManaWeather). Synced, never saved: whether the player hides
    // their aura, and how rich the air is where they stand (a multiplier on regeneration).
    private boolean auraHidden;
    private float weather = 1f;
    // The cantrips learned from scrolls (docs/superpowers/specs/2026-10-04-cantrips-design.md): castable
    // whatever the tree grants.
    private final Set<ResourceLocation> cantrips = new LinkedHashSet<>();
    private int stillTicks;
    private double lastX;
    private double lastZ;

    public MagicData() {
        this(START_MANA, 1, 0, List.of(), null, Map.of(), false, false, Map.of(), 0, new StatPoints(), List.of(), new AwakeningState(), false, 0, Map.of());
    }

    private MagicData(float mana, int level, int xp, List<ResourceLocation> affinities, @Nullable ResourceLocation selected,
                      Map<ResourceLocation, Long> cooldownEnds, boolean fallImmune, boolean freeCast,
                      Map<ResourceLocation, SpellProgress> spells, int bonusTreePoints, StatPoints stats, List<String> treeNodes,
                      AwakeningState awakening, boolean meditating, int conjured, Map<String, Integer> gearStats) {
        this.level = Mth.clamp(level, 1, MAX_LEVEL);
        this.xp = xp;
        this.affinities = new ArrayList<>(affinities);
        this.selected = selected;
        this.cooldownEnds = new HashMap<>(cooldownEnds);
        this.fallImmune = fallImmune;
        this.freeCast = freeCast;
        this.spells = new HashMap<>(spells);
        this.bonusTreePoints = Math.max(0, bonusTreePoints);
        this.stats = stats;
        this.treeNodes = new HashSet<>(treeNodes);
        this.awakening = awakening;
        this.meditating = meditating;
        this.conjured = conjured;
        this.gearStats = new HashMap<>(gearStats);
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
        return Progression.xpToNextLevel(level);
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
        // Fewer levels than points spent (an admin lowered it): hand the points back.
        if (stats.total() > this.level - 1) {
            stats.clear();
        }
        this.mana = Math.min(mana, maxMana());
    }

    private MagicData withCantrips(List<ResourceLocation> learned) {
        cantrips.addAll(learned);
        return this;
    }

    private MagicData withAura(boolean auraHidden, float weather) {
        this.auraHidden = auraHidden;
        this.weather = weather;
        return this;
    }

    public boolean auraHidden() {
        return auraHidden;
    }

    public void setAuraHidden(boolean auraHidden) {
        this.auraHidden = auraHidden;
    }

    /** How rich the air is where the player stands: a multiplier on mana regeneration. */
    public float weather() {
        return weather;
    }

    /** Returns whether it changed enough to tell the client. */
    public boolean setWeather(float weather) {
        boolean changed = Math.abs(this.weather - weather) > 0.005f;
        this.weather = weather;
        return changed;
    }

    public float maxMana() {
        return StatRules.maxMana(level, stat(Stat.RESERVOIR)) * Keystones.maxManaFactor(keystones());
    }

    public float regenPerSecond() {
        return StatRules.regenPerSecond(level, stat(Stat.RESERVOIR)) * Keystones.regenFactor(keystones());
    }

    /** Multiplier a spell applies to damage, knockback and durations: Potency, and its element family's Affinity. */
    public float spellPower(Spell spell) {
        Element element = SchoolElements.of(spell.school());
        return StatRules.spellPower(stat(Stat.POTENCY), element == null ? 0 : affinity(element)) * Keystones.powerFactor(keystones());
    }

    /** Multiplier on cooldowns, from Focus. */
    public float cooldownFactor() {
        return StatRules.cooldownFactor(stat(Stat.FOCUS));
    }

    // ---- stat points ----
    // Every level after the first earns a stat point. Spent points are stored, unspent ones derived.

    /** Stat points spent (without tree bonuses). */
    public StatPoints stats() {
        return stats;
    }

    /** A stat's total: points spent plus the skill tree's bonus. */
    public int statTotal(String key) {
        return stats.get(key) + statBonus(key);
    }

    /** What the skill tree and gear add to a stat, on top of its stat points. */
    public int statBonus(String key) {
        return grants().stat(key) + gearStats.getOrDefault(key, 0);
    }

    public Map<String, Integer> gearStats() {
        return Collections.unmodifiableMap(gearStats);
    }

    /** Replaces the stat points gear adds; returns whether anything changed. */
    public boolean setGearStats(Map<String, Integer> stats) {
        if (gearStats.equals(stats)) {
            return false;
        }
        gearStats.clear();
        gearStats.putAll(stats);
        mana = Math.min(mana, maxMana());
        return true;
    }

    public int stat(Stat stat) {
        return statTotal(stat.key());
    }

    /** The total Affinity of {@code element}'s family. */
    public int affinity(Element element) {
        return statTotal(StatPoints.affinityKey(element));
    }

    public int statPoints() {
        return Math.max(0, (level - 1) - stats.total());
    }

    /** Whether {@code key} is a stat this player can raise: any Stat, or the Affinity of an awakened element's family. */
    public boolean canRaise(String key) {
        for (Stat stat : Stat.values()) {
            if (stat.key().equals(key)) {
                return true;
            }
        }
        for (Element element : affinityElements()) {
            if (StatPoints.affinityKey(element).equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** Spends one stat point on {@code key}; false if none is free or the key can't be raised. */
    public boolean spend(String key) {
        if (statPoints() <= 0 || !canRaise(key)) {
            return false;
        }
        stats.add(key, 1);
        return true;
    }

    /** Whether one point of {@code key} can be given back: one is spent, and no tree node needs it. */
    public boolean canRefundStat(String key) {
        return stats.get(key) > 0
                && SkillTrees.current().requirementsMet(heldNodes(), other -> stats.get(other) - (other.equals(key) ? 1 : 0));
    }

    /** Gives one point of {@code key} back (check canRefundStat first). */
    public void refundStat(String key) {
        if (canRefundStat(key)) {
            stats.add(key, -1);
            mana = Math.min(mana, maxMana());
        }
    }

    /** Hands every spent stat point back. */
    public void resetStats() {
        stats.clear();
        mana = Math.min(mana, maxMana());
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

    public boolean hasAffinity(SpellSchool school) {
        return affinities.contains(school.id());
    }

    /** The elements of the schools you've awakened (schools that aren't one of the four are left out). */
    public Set<Element> affinityElements() {
        Set<Element> elements = EnumSet.noneOf(Element.class);
        for (ResourceLocation id : affinities) {
            Element element = SchoolElements.of(id);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
    }

    /**
     * The element you hold that {@code school} opposes while opposites are still locked (below
     * level 50), or null if {@code school} can be awakened as far as opposites go.
     */
    @Nullable
    public Element opposedBy(SpellSchool school) {
        Element element = SchoolElements.of(school);
        return element == null ? null : AffinityRules.blockingOpposite(affinityElements(), element, level);
    }

    /**
     * Awakens a new element unless you hold it or one you hold opposes it (until level 50). Selects
     * its first spell if nothing is selected.
     */
    public boolean awaken(SpellSchool school) {
        if (hasAffinity(school) || opposedBy(school) != null) {
            return false;
        }
        affinities.add(school.id());
        invalidateTree();
        if (selectedSpell() == null) {
            castableSpells().stream().findFirst().ifPresent(spell -> selected = spell.id());
        }
        return true;
    }

    public void forceAffinity(SpellSchool school) {
        if (!hasAffinity(school)) {
            affinities.add(school.id());
            invalidateTree();
        }
    }

    public void removeAffinity(SpellSchool school) {
        affinities.remove(school.id());
        invalidateTree();
    }

    public void clearAffinities() {
        affinities.clear();
        selected = null;
        invalidateTree();
    }

    // ---- awakening ----

    public AwakeningState awakening() {
        return awakening;
    }

    /** Starts the awakening day count at {@code dayTime} (the world's day clock) unless it has started. */
    public void startAwakeningClock(long dayTime) {
        if (awakening.start() < 0) {
            awakening.setStart(dayTime);
        }
    }

    /** Dev: makes today day {@code day} of the count. */
    public void setAwakeningDay(long dayTime, int day) {
        awakening.setStart(dayTime - (long) day * AwakeningRules.DAY_TICKS);
        awakening.setLastRolledDay(-1);
    }

    /** Which day of the awakening count it is at {@code dayTime}. */
    public int awakeningDay(long dayTime) {
        return awakening.start() < 0 ? 0 : AwakeningRules.day(dayTime, awakening.start());
    }

    public void setLastRolledDay(int day) {
        awakening.setLastRolledDay(day);
    }

    /** How many element families you hold (Water and Ice count once). */
    public int familiesHeld() {
        Set<Element> families = EnumSet.noneOf(Element.class);
        affinityElements().forEach(element -> families.add(element.family()));
        return families.size();
    }

    public boolean holdsFamily(Element element) {
        return affinityElements().stream().anyMatch(held -> held.family() == element.family());
    }

    public void addCatalystFailure() {
        int held = familiesHeld();
        awakening.setFailures(held, awakening.failures(held) + 1);
    }

    public void resetCatalystFailures() {
        awakening.setFailures(familiesHeld(), 0);
    }

    // ---- spells ----

    public boolean canCast(Spell spell) {
        return grants().spells().contains(spell.id().toString()) || cantrips.contains(spell.id());
    }

    /** Whether this mage has learned the cantrip {@code spell} from its scroll. */
    public boolean knowsCantrip(Spell spell) {
        return cantrips.contains(spell.id());
    }

    /** Learns a cantrip for good. Returns false if it was already known. */
    public boolean learnCantrip(ResourceLocation spell) {
        return cantrips.add(spell);
    }

    public Set<ResourceLocation> cantrips() {
        return Collections.unmodifiableSet(cantrips);
    }

    /** Dev/admin: forgets every cantrip. */
    public void forgetCantrips() {
        cantrips.clear();
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

    // ---- skill tree, spell levels & mastery ----
    // Every level after the first earns a tree point; every node taken (but not the starts your
    // elements give you) costs one. Spell levels and branches come from the nodes you hold. A
    // spell's next level also needs its mastery bar full (mastery = mana spent casting it).

    private static final SpellProgress NO_PROGRESS = new SpellProgress();

    /** The rule-facing view of this player for the skill tree. */
    private final SkillTree.Player treePlayer = new SkillTree.Player() {
        @Override
        public int freePoints() {
            return treePoints();
        }

        @Override
        public boolean holdsElement(String element) {
            Element held = elementNamed(element);
            return held != null && affinityElements().contains(held);
        }

        @Override
        public boolean holdsFamily(String element) {
            Element wanted = elementNamed(element);
            return wanted != null && affinityElements().stream().anyMatch(held -> held.family() == wanted.family());
        }

        @Override
        public int statPoints(String key) {
            return stats.get(key);
        }

        @Override
        public boolean masteryFull(String spellId, int currentLevel) {
            ResourceLocation id = ResourceLocation.tryParse(spellId);
            Spell spell = id == null ? null : SpellRegistries.SPELLS.get(id);
            if (spell == null || currentLevel >= spell.maxLevel()) {
                return false;
            }
            int needed = spell.masteryToNextLevel(currentLevel);
            return progress(spell).mastery() >= needed;
        }
    };

    @Nullable
    private static Element elementNamed(String name) {
        for (Element element : Element.values()) {
            if (element.name().equalsIgnoreCase(name)) {
                return element;
            }
        }
        return null;
    }

    private void invalidateTree() {
        grants = null;
        held = null;
    }

    /** Every node this player holds: the ones they took, plus the starts of their elements. */
    public Set<String> heldNodes() {
        if (held == null || grantsVersion != SkillTrees.version()) {
            held = SkillTrees.current().held(treeNodes, treePlayer);
            grants = null;
            grantsVersion = SkillTrees.version();
        }
        return held;
    }

    /** What the held nodes give: stat bonuses, castable spells, spell levels and branches. */
    public SkillTree.Grants grants() {
        Set<String> nodes = heldNodes();
        if (grants == null) {
            grants = SkillTrees.current().grants(nodes);
        }
        return grants;
    }

    /** The keystones this player's tree holds (see Keystones). */
    public Set<String> keystones() {
        return grants().keystones();
    }

    public boolean hasKeystone(String keystone) {
        return grants().hasKeystone(keystone);
    }

    /** The nodes this player took (not counting the starts their elements give them). */
    public Set<String> treeNodes() {
        return Collections.unmodifiableSet(treeNodes);
    }

    public int treePoints() {
        SkillTree tree = SkillTrees.current();
        long spent = treeNodes.stream().filter(id -> tree.node(id) != null).count();
        return (int) Math.max(0, (level - 1) + bonusTreePoints - spent);
    }

    /** Tree points condensed from Elemental Essence, on top of those from levels. */
    public int bonusTreePoints() {
        return bonusTreePoints;
    }

    public void addBonusTreePoint() {
        bonusTreePoints++;
    }

    public SkillTree.Check checkTake(String id) {
        return SkillTrees.current().canTake(treeNodes, id, treePlayer);
    }

    /** Takes a node if the rules allow it. Taking a spell's next level empties its mastery bar. */
    public SkillTree.Check take(String id) {
        SkillTree.Check check = checkTake(id);
        if (check == SkillTree.Check.OK) {
            treeNodes.add(id);
            SkillTree.Node node = SkillTrees.current().node(id);
            if (node != null && (node.type() == SkillTree.Type.UPGRADE || node.type() == SkillTree.Type.FORK)) {
                Spell spell = spellOf(node.spell());
                if (spell != null) {
                    editProgress(spell).setMastery(0);
                }
            }
            invalidateTree();
        }
        return check;
    }

    public SkillTree.Check checkRefund(String id) {
        return SkillTrees.current().canRefund(treeNodes, id, treePlayer);
    }

    /** Gives a node back if the rules allow it (the Essence cost is paid by the caller). */
    public SkillTree.Check refund(String id) {
        SkillTree.Check check = checkRefund(id);
        if (check == SkillTree.Check.OK) {
            treeNodes.remove(id);
            invalidateTree();
            mana = Math.min(mana, maxMana());
        }
        return check;
    }

    /** Dev/admin: gives back every node. */
    public void resetTree() {
        treeNodes.clear();
        invalidateTree();
        mana = Math.min(mana, maxMana());
    }

    /** Dev/admin: takes {@code spell}'s node and its whole path for free (the first branch at each fork). */
    public void takeWholePath(Spell spell) {
        String spellId = spell.id().toString();
        for (SkillTree.Node node : SkillTrees.current().nodes()) {
            if (node.type() == SkillTree.Type.SPELL && spellId.equals(node.spell())) {
                treeNodes.add(node.id());
            }
        }
        for (int target = 2; target <= spell.maxLevel(); target++) {
            int spellLevel = target;
            boolean has = SkillTrees.current().nodes().stream().anyMatch(node -> spellId.equals(node.spell())
                    && node.spellLevel() == spellLevel && treeNodes.contains(node.id()));
            if (!has) {
                SkillTrees.current().nodes().stream()
                        .filter(node -> spellId.equals(node.spell()) && node.spellLevel() == spellLevel)
                        .findFirst().ifPresent(node -> treeNodes.add(node.id()));
            }
        }
        invalidateTree();
    }

    /**
     * Dev/admin: puts {@code spell} at {@code level} through its tree path for free: its node, then
     * one node per level up to {@code level} (none past it), taking at each fork the first of
     * {@code branches} it offers, or else its first.
     */
    public void setSpellPath(Spell spell, int level, List<String> branches) {
        String spellId = spell.id().toString();
        SkillTree tree = SkillTrees.current();
        treeNodes.removeIf(id -> {
            SkillTree.Node node = tree.node(id);
            return node != null && spellId.equals(node.spell()) && node.spellLevel() >= 2;
        });
        for (SkillTree.Node node : tree.nodes()) {
            if (node.type() == SkillTree.Type.SPELL && spellId.equals(node.spell())) {
                treeNodes.add(node.id());
            }
        }
        for (int target = 2; target <= Math.min(level, spell.maxLevel()); target++) {
            int spellLevel = target;
            List<SkillTree.Node> choices = tree.nodes().stream()
                    .filter(node -> spellId.equals(node.spell()) && node.spellLevel() == spellLevel).toList();
            choices.stream().filter(node -> node.branch() != null && branches.contains(node.branch())).findFirst()
                    .or(() -> choices.stream().findFirst())
                    .ifPresent(node -> treeNodes.add(node.id()));
        }
        invalidateTree();
    }

    @Nullable
    private static Spell spellOf(@Nullable String id) {
        ResourceLocation location = id == null ? null : ResourceLocation.tryParse(id);
        return location == null ? null : SpellRegistries.SPELLS.get(location);
    }

    public SpellProgress progress(Spell spell) {
        return spells.getOrDefault(spell.id(), NO_PROGRESS);
    }

    private SpellProgress editProgress(Spell spell) {
        return spells.computeIfAbsent(spell.id(), id -> new SpellProgress());
    }

    public int spellLevel(Spell spell) {
        int level = Math.min(grants().spellLevel(spell.id().toString()), spell.maxLevel());
        // A cantrip isn't in the tree: once learned, it's simply known.
        return cantrips.contains(spell.id()) ? Math.max(1, level) : level;
    }

    /** The branches chosen for {@code spell}, by level. */
    public Map<Integer, String> branches(Spell spell) {
        return grants().branches().getOrDefault(spell.id().toString(), Map.of());
    }

    /** Mastery needed for the next level of {@code spell}; 0 at max level. */
    public int masteryToNextLevel(Spell spell) {
        int spellLevel = spellLevel(spell);
        return spellLevel >= spell.maxLevel() ? 0 : spell.masteryToNextLevel(spellLevel);
    }

    /** Mastery fills up to the next level's requirement, then waits for that level's node. */
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

    /** Dev/admin: fill the mastery bar toward the next level. */
    public void fillMastery(Spell spell) {
        addMastery(spell, masteryToNextLevel(spell));
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

    /** A use of a spell with charges (see SpellCharges): its clock runs a charge's worth longer. */
    public void useCharge(ResourceLocation spell, long gameTime, int perCharge) {
        long remaining = cooldownRemaining(spell, gameTime);
        cooldownEnds.values().removeIf(end -> end <= gameTime);
        if (perCharge > 0) {
            cooldownEnds.put(spell, gameTime + SpellCharges.afterUse(remaining, perCharge));
        }
    }

    /** How many charges of {@code spell} are ready (1 or 0 for a spell without charges). */
    public int chargesReady(Spell spell, long gameTime) {
        int level = spellLevel(spell);
        return SpellCharges.ready(spell.charges(level), cooldownRemaining(spell.id(), gameTime), spell.cooldownTicks(level, cooldownFactor()));
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

    /** How many projectiles are conjured and held right now. */
    public int conjured() {
        return conjured;
    }

    public void setConjured(int conjured) {
        this.conjured = conjured;
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
