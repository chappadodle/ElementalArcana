# Universal Level, Stats and XP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Step 1 of `docs/superpowers/specs/2026-10-01-progression-system-design.md`: one level
system (cap 100) for players and creatures, seven stats bought with stat points, XP from kills
(absorbed mana) and reactions, creature levels from fixed zones, and ranks as bonus levels.

**Architecture:** All balance math lives in plain-Java, unit-tested classes in `api/`
(`Progression`, `StatRules`, `ZoneLevels`, `AttunementRank`). Players keep their data in
`MagicData` (now with a `StatPoints` sheet). Creatures get a saved, synced `CREATURE_LEVEL`
attachment, assigned lazily on their first tick from the zone they're in. One damage hook
(`LevelCombat`) applies the level gap, Ward, creature Potency and Insight. Kill XP replaces
XP from spent mana.

**Tech Stack:** NeoForge 21.1.252 (Minecraft 1.21.1), Java 21, JUnit 5,
`tools/server_console.sh` for headless server checks.

## Global Constraints

- Level cap **100**. Each level after the first gives **1 stat point** and (until the tree, step 2)
  **1 skill point**.
- XP to next level: **55 × 1.09^(level − 1)**, rounded; 0 at the cap.
- Kills per level: **8 at level 1, rising linearly to 40 at level 100**.
- XP level gap: creature **below** you −10% per level (0 at 10 or more below); **above** you +5% per
  level, at most +50%.
- Damage level gap: **×1.045 per level** of difference, both ways (dealt and taken), gap capped at
  ±40. A +20 creature is 1.045^40 ≈ 1.09^20 ≈ ×5.8 overall.
- Ranks are bonus levels: **Adept +0, Magus +8, Archmage +20**. Rarity unchanged (5%, 1%, 0.1%,
  ×distance up to ×3), with no player-level gate.
- Stat curve per point: **×1.04 up to 20 points, ×1.02 up to 40, ×1.01 after** (≈×6 at 100).
  Each stat uses the curve to a power: Reservoir 1, Focus 0.75, Potency/Affinity/Ward/Vitality/
  Insight 0.5.
- Zones: Overworld 1–5 + 1 per 150 blocks (max +30), caves/structures/dangerous biomes up to +15;
  Nether 25–50; End 60+.
- Opposed elements unlock at level **50**.
- Affinity is per element **family**: Ice belongs to Water's family.
- Never modify git config. No commits until the user play-tests and says so (project rule).
- Don't launch the game client (the user is remote).

---

## File map

| File | Responsibility |
|---|---|
| `api/Progression.java` (modify) | XP curve, kill XP, level-gap factors, cooldown formula |
| `api/StatRules.java` (new) | Stat curve and every stat's effect; creature stat template |
| `api/Stat.java` (new) | The six non-element stats |
| `api/ZoneLevels.java` (new) | Creature level from where it is |
| `api/AttunementRank.java`, `api/AttunementRules.java`, `api/AttunementRewards.java` (modify) | Ranks as bonus levels and XP multipliers; no level gate; Insight on Essence |
| `api/Element.java`, `api/AffinityRules.java` (modify) | `family()`; opposites at 50 |
| `api/Spell.java` (modify) | `cooldownTicks(spellLevel, cooldownFactor)` |
| `core/StatPoints.java` (new) | A player's spent stat points, saved and synced |
| `core/MagicData.java` (modify) | Cap 100, stats, derived mana/regen/power/cooldown |
| `core/PlayerStats.java` (new) | Applies Vitality to a player's max health |
| `core/CastingService.java`, `core/Conjuring.java` (modify) | Per-spell power, Focus cooldowns, no XP from mana |
| `core/MagicAttachments.java` (modify) | `CREATURE_LEVEL` attachment |
| `content/CreatureLevels.java` (new) | Assigns, reads and applies creature levels |
| `content/LevelCombat.java` (new) | The damage hook |
| `content/ReactionRewards.java` (new) | Reaction marks, Insight hits and reaction XP |
| `api/ElementalReactions.java` (modify) | Calls `ReactionRewards.reacted` |
| `content/CreatureRewards.java` (modify) | Kill XP for everyone; Insight on Essence |
| `content/Attunement.java` (modify) | Refreshes creature stats instead of the health bonus |
| `network/StatPayload.java` (new), `network/ModNetwork.java` (modify) | Spend a stat point |
| `client/StatsScreen.java` (new), `client/StatusScreen.java`, `client/SpellDetailScreen.java`, `client/SpellHudLayer.java`, `client/DevScreen.java` (modify) | UI |
| `compat/jade/CreatureLevelProvider.java` (new), `ArcanaJadePlugin.java` (modify) | Jade shows creature levels |
| `core/ArcanaCommand.java` (modify) | `stats` and `creaturelevel` commands |
| `data/elementalarcana/tags/worldgen/biome/dangerous.json` (new) | Biomes worth +10 levels |
| lang `en_us.json`, `README.md`, `tools/server_tests/levels.txt` | Text, docs, server check |

---

### Task 1: Balance math (pure Java)

**Files:**
- Modify: `src/main/java/com/chappadodle/elementalarcana/api/Progression.java`
- Create: `src/main/java/com/chappadodle/elementalarcana/api/StatRules.java`, `api/ZoneLevels.java`, `api/Stat.java`
- Test: `src/test/java/com/chappadodle/elementalarcana/api/ProgressionTest.java`, `StatRulesTest.java`, `ZoneLevelsTest.java`

**Interfaces (produces):**
- `Progression.MAX_LEVEL = 100`
- `int Progression.xpToNextLevel(int level)`
- `int Progression.creatureXp(int creatureLevel)`
- `double Progression.xpGapFactor(int playerLevel, int creatureLevel)`
- `int Progression.killXp(int playerLevel, int creatureLevel, double sizeFactor, double rankMultiplier)`
- `double Progression.sizeFactor(double baseMaxHealth)`
- `float Progression.damageLevelFactor(int attackerLevel, int targetLevel)`
- `int Progression.cooldownTicks(int baseTicks, int spellLevel, float cooldownFactor)` (replaces the magic-level overload)
- `enum Stat { RESERVOIR, POTENCY, FOCUS, WARD, VITALITY, INSIGHT }` with `String key()` and `double exponent()`
- `StatRules.curve(int)`, `effect(int, double)`, `maxMana(int level, int reservoir)`, `regenPerSecond(int level, int reservoir)`, `spellPower(int potency, int affinity)`, `cooldownFactor(int focus)`, `wardFactor(int ward)`, `healthMultiplier(int vitality)`, `insightFactor(int insight)`, `creaturePoints(int level)`, `AFFINITY_EXPONENT`
- `ZoneLevels.Dimension { OVERWORLD, NETHER, END }`, `int ZoneLevels.level(Dimension, double distance, int y, boolean underground, boolean inStructure, boolean dangerousBiome, int spread)`, `SPREAD = 5`

- [ ] **Step 1: Write the failing tests**

`StatRulesTest.java`:
```java
package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatRulesTest {

    @Test
    void curveSoftCapsAtTwentyAndForty() {
        assertEquals(1.0, StatRules.curve(0), 1e-9);
        assertEquals(Math.pow(1.04, 20), StatRules.curve(20), 1e-9);
        assertEquals(Math.pow(1.04, 20) * Math.pow(1.02, 20), StatRules.curve(40), 1e-9);
        assertEquals(5.92, StatRules.curve(100), 0.01);
    }

    @Test
    void manaAndRegenGrowWithLevelAndReservoir() {
        assertEquals(100f, StatRules.maxMana(1, 0), 1e-4f);
        assertEquals(245f, StatRules.maxMana(30, 0), 1e-4f);
        assertEquals(245f * (float) Math.pow(1.04, 10), StatRules.maxMana(30, 10), 1e-2f);
        assertEquals(2.5f, StatRules.regenPerSecond(1, 0), 1e-4f);
        assertEquals(5.4f, StatRules.regenPerSecond(30, 0), 1e-4f);
    }

    @Test
    void halfStrengthStatsUseTheSquareRoot() {
        assertEquals(Math.pow(1.04, 10), StatRules.spellPower(10, 10), 1e-5);
        assertEquals(Math.pow(1.04, 5), StatRules.spellPower(10, 0), 1e-5);
        assertEquals(1 / Math.pow(1.04, 5), StatRules.wardFactor(10), 1e-5);
        assertEquals(Math.pow(1.04, 5), StatRules.healthMultiplier(10), 1e-5);
        assertEquals(Math.pow(1.04, 5), StatRules.insightFactor(10), 1e-5);
    }

    @Test
    void focusShortensCooldowns() {
        assertEquals(1f, StatRules.cooldownFactor(0), 1e-6f);
        assertEquals(1 / Math.pow(Math.pow(1.04, 20), 0.75), StatRules.cooldownFactor(20), 1e-5);
    }

    @Test
    void creaturesSplitTheirPointsInThree() {
        assertEquals(0, StatRules.creaturePoints(1));
        assertEquals(1, StatRules.creaturePoints(4));
        assertEquals(13, StatRules.creaturePoints(40));
        assertEquals(33, StatRules.creaturePoints(100));
    }
}
```

`ZoneLevelsTest.java`:
```java
package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static com.chappadodle.elementalarcana.api.ZoneLevels.Dimension.END;
import static com.chappadodle.elementalarcana.api.ZoneLevels.Dimension.NETHER;
import static com.chappadodle.elementalarcana.api.ZoneLevels.Dimension.OVERWORLD;
import static com.chappadodle.elementalarcana.api.ZoneLevels.level;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoneLevelsTest {

    @Test
    void overworldStartsLowAndGrowsWithDistance() {
        assertEquals(1, level(OVERWORLD, 0, 70, false, false, false, 0));
        assertEquals(5, level(OVERWORLD, 0, 70, false, false, false, 4));
        assertEquals(11, level(OVERWORLD, 1500, 70, false, false, false, 0));
        assertEquals(31, level(OVERWORLD, 100_000, 70, false, false, false, 0));
    }

    @Test
    void cavesStructuresAndDangerousBiomesAddUpToFifteen() {
        assertEquals(1, level(OVERWORLD, 0, 40, false, false, false, 0));   // open sky: no cave bonus
        assertEquals(3, level(OVERWORLD, 0, 40, true, false, false, 0));    // (63 - 40) / 10 = 2
        assertEquals(11, level(OVERWORLD, 0, -60, true, false, false, 0));  // cave bonus capped at 10
        assertEquals(6, level(OVERWORLD, 0, 70, false, true, false, 0));
        assertEquals(16, level(OVERWORLD, 0, -60, true, true, true, 0));    // 10 + 5 + 10 capped at 15
    }

    @Test
    void netherIsTwentyFiveToFifty() {
        assertEquals(25, level(NETHER, 0, 70, true, false, false, 0));
        assertEquals(35, level(NETHER, 500, 70, true, false, false, 0));
        assertEquals(50, level(NETHER, 100_000, 70, true, true, false, 4));
    }

    @Test
    void endIsSixtyAndUp() {
        assertEquals(60, level(END, 0, 70, false, false, false, 0));
        assertEquals(74, level(END, 1000, 70, false, false, false, 4));
        assertEquals(100, level(END, 100_000, 70, false, false, false, 4));
    }
}
```

Add to `ProgressionTest.java` (and replace the two magic-level tests and the regen test, whose
functions are removed):
```java
    @Test
    void xpCurveGrowsNinePercentPerLevel() {
        assertEquals(55, xpToNextLevel(1));
        assertEquals(669, xpToNextLevel(30));   // 55 x 1.09^29 = 669.3
        assertEquals(0, xpToNextLevel(100));
        assertEquals(255_427, xpToNextLevel(99), 300);
    }

    @Test
    void sameLevelKillsPerLevelRiseFromEightToForty() {
        assertEquals(7, creatureXp(1));          // 55 / 8 = 6.9
        assertEquals(xpToNextLevel(99) / 40.0, creatureXp(100), 200);
    }

    @Test
    void lowerCreaturesPayLessAndHigherOnesMore() {
        assertEquals(1.0, xpGapFactor(10, 10), 1e-9);
        assertEquals(0.5, xpGapFactor(10, 5), 1e-9);
        assertEquals(0.0, xpGapFactor(20, 10), 1e-9);
        assertEquals(0.0, xpGapFactor(30, 10), 1e-9);
        assertEquals(1.25, xpGapFactor(10, 15), 1e-9);
        assertEquals(1.5, xpGapFactor(10, 40), 1e-9);
    }

    @Test
    void killXpScalesBySizeAndRank() {
        assertEquals(7, killXp(1, 1, 1.0, 1.0));
        assertEquals(1, killXp(1, 1, 0.2, 1.0));     // a chicken: 1.4 -> 1
        assertEquals(84, killXp(1, 1, 1.0, 12.0));
        assertEquals(0, killXp(20, 1, 5.0, 12.0));
        assertEquals(0.1, sizeFactor(1), 1e-9);
        assertEquals(1.0, sizeFactor(20), 1e-9);
        assertEquals(5.0, sizeFactor(300), 1e-9);
    }

    @Test
    void levelGapScalesDamageBothWays() {
        assertEquals(1f, damageLevelFactor(10, 10), 1e-6f);
        assertEquals((float) Math.pow(1.045, 20), damageLevelFactor(30, 10), 1e-4f);
        assertEquals((float) Math.pow(1.045, -20), damageLevelFactor(10, 30), 1e-6f);
        assertEquals(damageLevelFactor(41, 1), damageLevelFactor(100, 1), 1e-6f);
    }

    @Test
    void focusFactorShortensCooldowns() {
        assertEquals(60, cooldownTicks(60, 1, 1f));
        assertEquals(30, cooldownTicks(60, 1, 0.5f));
        assertEquals(14, cooldownTicks(60, 10, 0.5f));  // 60 x 0.46 x 0.5 = 13.8
    }
```
Static imports to add: `creatureXp`, `damageLevelFactor`, `killXp`, `sizeFactor`, `xpGapFactor`,
`xpToNextLevel`.

- [ ] **Step 2: Run the tests to see them fail**

Run: `./gradlew test --console=plain -q`
Expected: compile failure (new functions and classes don't exist).

- [ ] **Step 3: Implement**

`Stat.java`:
```java
package com.chappadodle.elementalarcana.api;

import java.util.Locale;

/**
 * The stats every creature has, besides one Affinity per element family (see StatRules). Each
 * level gives a player a stat point to put into one of them. {@code exponent} is how hard the stat
 * leans on the shared curve: 1 = full strength, 0.5 = half (its square root).
 */
public enum Stat {
    RESERVOIR(1.0),
    POTENCY(0.5),
    FOCUS(0.75),
    WARD(0.5),
    VITALITY(0.5),
    INSIGHT(0.5);

    private final double exponent;

    Stat(double exponent) {
        this.exponent = exponent;
    }

    public double exponent() {
        return exponent;
    }

    /** Its save and network key, e.g. "reservoir". */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
```

`StatRules.java`:
```java
package com.chappadodle.elementalarcana.api;

/**
 * What stat points do (see docs/superpowers/specs/2026-10-01-progression-system-design.md). Plain
 * Java, unit tested. Every stat rides one soft-capped curve: each point is worth x1.04 up to 20
 * points, x1.02 up to 40 and x1.01 after that (about x6 at 100). A stat uses the curve to its
 * exponent (see Stat). Creatures use the same rules, with their points spread evenly over Vitality,
 * Ward and Potency.
 */
public final class StatRules {
    public static final double AFFINITY_EXPONENT = 0.5;
    private static final float BASE_MANA = 100f;
    private static final float MANA_PER_LEVEL = 5f;
    private static final float BASE_REGEN = 2.5f;
    private static final float REGEN_PER_LEVEL = 0.1f;

    private StatRules() {
    }

    /** The shared curve: x1.04 a point up to 20, x1.02 up to 40, x1.01 after. */
    public static double curve(int points) {
        int p = Math.max(0, points);
        return Math.pow(1.04, Math.min(p, 20)) * Math.pow(1.02, Math.clamp(p - 20, 0, 20)) * Math.pow(1.01, Math.max(0, p - 40));
    }

    /** The curve to {@code exponent}. */
    public static double effect(int points, double exponent) {
        return Math.pow(curve(points), exponent);
    }

    public static float maxMana(int level, int reservoir) {
        return (float) ((BASE_MANA + MANA_PER_LEVEL * (Math.max(1, level) - 1)) * effect(reservoir, Stat.RESERVOIR.exponent()));
    }

    public static float regenPerSecond(int level, int reservoir) {
        return (float) ((BASE_REGEN + REGEN_PER_LEVEL * (Math.max(1, level) - 1)) * effect(reservoir, Stat.RESERVOIR.exponent()));
    }

    /** Spell power: Potency for every spell, times the Affinity of the spell's element family. */
    public static float spellPower(int potency, int affinity) {
        return (float) (effect(potency, Stat.POTENCY.exponent()) * effect(affinity, AFFINITY_EXPONENT));
    }

    /** Multiplier on cooldowns. */
    public static float cooldownFactor(int focus) {
        return (float) (1 / effect(focus, Stat.FOCUS.exponent()));
    }

    /** Multiplier on elemental damage taken. */
    public static float wardFactor(int ward) {
        return (float) (1 / effect(ward, Stat.WARD.exponent()));
    }

    /** Multiplier on max health. */
    public static float healthMultiplier(int vitality) {
        return (float) effect(vitality, Stat.VITALITY.exponent());
    }

    /** Multiplier on reaction hits and Essence chances. */
    public static float insightFactor(int insight) {
        return (float) effect(insight, Stat.INSIGHT.exponent());
    }

    /** A creature's points in each of Vitality, Ward and Potency: a third of its level's points. */
    public static int creaturePoints(int level) {
        return (Math.max(1, level) - 1) / 3;
    }
}
```

`ZoneLevels.java`:
```java
package com.chappadodle.elementalarcana.api;

/**
 * A creature's level from where it spawned (fixed zones, never from the player's level). Plain
 * Java, unit tested; CreatureLevels feeds it the world. {@code spread} is a random 0..SPREAD-1 so
 * creatures in one place aren't all the same.
 */
public final class ZoneLevels {
    public static final int SPREAD = 5;
    private static final int SEA_LEVEL = 63;

    public enum Dimension { OVERWORLD, NETHER, END }

    private ZoneLevels() {
    }

    public static int level(Dimension dimension, double distance, int y, boolean underground, boolean inStructure,
                            boolean dangerousBiome, int spread) {
        int level = switch (dimension) {
            case OVERWORLD -> {
                int cave = underground ? Math.clamp((SEA_LEVEL - y) / 10, 0, 10) : 0;
                int extra = Math.min(15, cave + (inStructure ? 5 : 0) + (dangerousBiome ? 10 : 0));
                yield 1 + spread + Math.min(30, (int) (distance / 150)) + extra;
            }
            case NETHER -> Math.clamp(25 + spread + (int) (distance / 50) + (inStructure ? 5 : 0), 25, 50);
            case END -> 60 + spread + Math.min(30, (int) (distance / 100));
        };
        return Math.clamp(level, 1, Progression.MAX_LEVEL);
    }
}
```

`Progression.java`: add (keep the Essence functions; replace the regen function and the
magic-level cooldown overload):
```java
    public static final int MAX_LEVEL = 100;
    private static final double XP_BASE = 55;
    private static final double XP_GROWTH = 1.09;
    private static final double FIRST_KILLS_PER_LEVEL = 8;
    private static final double LAST_KILLS_PER_LEVEL = 40;
    private static final double DAMAGE_PER_LEVEL = 1.045;
    private static final int MAX_DAMAGE_GAP = 40;

    /** XP from {@code level} to the next; 0 at the cap. */
    public static int xpToNextLevel(int level) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return (int) Math.round(XP_BASE * Math.pow(XP_GROWTH, Math.max(1, level) - 1));
    }

    /** Same-level kills needed for a level at {@code level}: 8 at 1, 40 at 100. */
    public static double killsPerLevel(int level) {
        int l = Math.clamp(level, 1, MAX_LEVEL);
        return FIRST_KILLS_PER_LEVEL + (LAST_KILLS_PER_LEVEL - FIRST_KILLS_PER_LEVEL) * (l - 1) / (MAX_LEVEL - 1);
    }

    /** XP (absorbed mana) of an ordinary creature of {@code level} killed by a same-level player. */
    public static int creatureXp(int level) {
        int l = Math.clamp(level, 1, MAX_LEVEL);
        return Math.max(1, (int) Math.round(xpToNextLevel(Math.min(l, MAX_LEVEL - 1)) / killsPerLevel(l)));
    }

    /** Share of a kill's XP a player absorbs: less from weaker creatures, more from stronger. */
    public static double xpGapFactor(int playerLevel, int creatureLevel) {
        int gap = creatureLevel - playerLevel;
        return gap >= 0 ? 1 + Math.min(0.5, 0.05 * gap) : Math.max(0, 1 + 0.1 * gap);
    }

    /** XP for a kill: the creature's mana, by its size and rank, by the level gap. */
    public static int killXp(int playerLevel, int creatureLevel, double sizeFactor, double rankMultiplier) {
        double factor = xpGapFactor(playerLevel, creatureLevel);
        if (factor <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(creatureXp(creatureLevel) * sizeFactor * rankMultiplier * factor));
    }

    /** How much mana a creature's body holds, by its base max health (a zombie's 20 = 1). */
    public static double sizeFactor(double baseMaxHealth) {
        return Math.clamp(baseMaxHealth / 20, 0.1, 5);
    }

    /** Damage multiplier for an attacker of {@code attackerLevel} hitting a target of {@code targetLevel}. */
    public static float damageLevelFactor(int attackerLevel, int targetLevel) {
        int gap = Math.clamp(attackerLevel - targetLevel, -MAX_DAMAGE_GAP, MAX_DAMAGE_GAP);
        return (float) Math.pow(DAMAGE_PER_LEVEL, gap);
    }

    /** A spell's cooldown at {@code spellLevel} (6% shorter each), times the caster's Focus factor. */
    public static int cooldownTicks(int baseTicks, int spellLevel, float cooldownFactor) {
        int level = Math.max(1, spellLevel);
        return Math.round(baseTicks * (1f - COOLDOWN_CUT_PER_LEVEL * (level - 1)) * cooldownFactor);
    }
```
Remove `COOLDOWN_CUT_PER_MAGIC_LEVEL`, `BASE_REGEN_PER_SECOND`, `REGEN_PER_SECOND_PER_LEVEL`,
`regenPerSecond` and `cooldownTicks(int, int, int)`; the two-argument `cooldownTicks` calls
`cooldownTicks(baseTicks, spellLevel, 1f)`. Update the class Javadoc.

- [ ] **Step 4: Run the tests**

Run: `./gradlew test --console=plain -q` (the main code won't compile until Task 2 removes the
old callers, so run Task 2 first if needed, then all tests).
Expected: the new tests pass.

---

### Task 2: Player level, stats and spell power

**Files:**
- Modify: `api/Element.java`, `api/AffinityRules.java`, `api/Spell.java`, `core/MagicData.java`,
  `core/CastingService.java`, `core/Conjuring.java`, `client/StatusScreen.java`,
  `client/SpellDetailScreen.java`, `client/SpellHudLayer.java`, `core/MagicEvents.java`,
  `core/ArcanaCommand.java`, `network/ModNetwork.java`, lang
- Create: `core/StatPoints.java`, `core/PlayerStats.java`, `network/StatPayload.java`
- Test: `AffinityRulesTest.java`, `ElementTest.java`

**Interfaces:**
- Consumes: Task 1.
- Produces: `Element.family()`; `MagicData.statPoints()`, `stat(Stat)`, `affinity(Element)`,
  `spend(String key)`, `resetStats()`, `spellPower(Spell)`, `cooldownFactor()`;
  `StatPoints.affinityKey(Element)`; `PlayerStats.apply(ServerPlayer)`.

- [ ] **Step 1: Tests.** `ElementTest`: `assertEquals(WATER, ICE.family()); assertEquals(FIRE, FIRE.family());`.
  `AffinityRulesTest`: rename to `fireBlocksWaterAndIceUntilLevel50`, levels 29/30 → 49/50.
- [ ] **Step 2: `Element.family()`**: `return this == ICE ? WATER : this;` with Javadoc
  ("Ice belongs to Water's family; Affinity is per family"). `AffinityRules.OPPOSITES_UNLOCK_LEVEL = 50`.
- [ ] **Step 3: `StatPoints`** (core):
```java
package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Stat;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A player's spent stat points, by key: a Stat's key ("reservoir") or an element family's
 * Affinity ("affinity/water"). Keys are strings so new elements and stats never break a save.
 */
public final class StatPoints {
    public static final Codec<StatPoints> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(StatPoints::new, points -> points.points);

    private final Map<String, Integer> points;

    public StatPoints() {
        this(Map.of());
    }

    private StatPoints(Map<String, Integer> points) {
        this.points = new HashMap<>(points);
    }

    public static String affinityKey(Element element) {
        return "affinity/" + element.family().name().toLowerCase(Locale.ROOT);
    }

    public int get(String key) {
        return points.getOrDefault(key, 0);
    }

    public int get(Stat stat) {
        return get(stat.key());
    }

    public int affinity(Element element) {
        return get(affinityKey(element));
    }

    public int total() {
        return points.values().stream().mapToInt(Integer::intValue).sum();
    }

    void add(String key, int amount) {
        points.merge(key, amount, Integer::sum);
    }

    void clear() {
        points.clear();
    }

    static void write(FriendlyByteBuf buf, StatPoints stats) {
        buf.writeMap(stats.points, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
    }

    static StatPoints read(FriendlyByteBuf buf) {
        return new StatPoints(buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt));
    }
}
```
- [ ] **Step 4: `MagicData`**
  - `MAX_LEVEL = Progression.MAX_LEVEL`; delete `BASE_MAX_MANA`, `MAX_MANA_PER_LEVEL`, `POWER_PER_LEVEL`.
  - New field `private final StatPoints stats;` saved as `optionalFieldOf("stats", new StatPoints())`
    (the codec group grows to 12 fields), written/read in `STREAM_CODEC` after `bonusSkillPoints`.
  - `xpToNextLevel()` → `Progression.xpToNextLevel(level)`.
  - `maxMana()` → `StatRules.maxMana(level, stats.get(Stat.RESERVOIR))`;
    `regenPerSecond()` → `StatRules.regenPerSecond(level, stats.get(Stat.RESERVOIR))`.
  - Replace `power()` with:
```java
    /** Multiplier a spell applies to damage, knockback and durations: Potency and its family's Affinity. */
    public float spellPower(Spell spell) {
        Element element = SchoolElements.of(spell.school());
        return StatRules.spellPower(stats.get(Stat.POTENCY), element == null ? 0 : stats.affinity(element));
    }

    /** Multiplier on cooldowns, from Focus. */
    public float cooldownFactor() {
        return StatRules.cooldownFactor(stats.get(Stat.FOCUS));
    }
```
  - Stat points section:
```java
    // ---- stat points ----
    // Every level after the first earns a stat point; spent points are stored, unspent derived.

    public StatPoints stats() {
        return stats;
    }

    public int statPoints() {
        return Math.max(0, (level - 1) - stats.total());
    }

    /** Whether {@code key} is a stat this player can raise: any Stat, or the Affinity of an awakened family. */
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

    public void resetStats() {
        stats.clear();
        mana = Math.min(mana, maxMana());
    }
```
  - `setLevel`: after clamping, `if (stats.total() > level - 1) stats.clear();` before the mana clamp.
  - Constructor `this.mana = ...maxMana()` must come after `this.stats = ...`.
  - Default constructor passes `100f` mana and `new StatPoints()`.
- [ ] **Step 5: `Spell.cooldownTicks(int spellLevel, float cooldownFactor)`** →
  `Progression.cooldownTicks(cooldownTicks(), spellLevel, cooldownFactor)`. Update callers to pass
  `data.cooldownFactor()`: `CastingService` (2 places), `SpellDetailScreen:203`,
  `SpellHudLayer:64`, `StatusScreen` tooltip.
- [ ] **Step 6: Casting.** `CastingService` and `Conjuring`: `data.power()` → `data.spellPower(spell)`.
  In `CastingService.pay`, delete the `addXp(cost)` block and the `oldLevel` local; update the
  Javadoc ("then grants mastery"). In `onLevelUp`, the subtitle becomes
  `Component.translatable("title.elementalarcana.level_up.sub", data.statPoints())`.
- [ ] **Step 7: `PlayerStats`** (core):
```java
package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Stat;
import com.chappadodle.elementalarcana.api.StatRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Stats that live on the player's vanilla attributes: Vitality's max health. */
public final class PlayerStats {
    private static final ResourceLocation VITALITY = ElementalArcana.id("vitality");

    private PlayerStats() {
    }

    /** Re-applies Vitality (after a stat change, login, respawn or level change). */
    public static void apply(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        int vitality = MagicAttachments.get(player).stats().get(Stat.VITALITY);
        health.removeModifier(VITALITY);
        double bonus = StatRules.healthMultiplier(vitality) - 1;
        if (bonus > 0) {
            health.addTransientModifier(new AttributeModifier(VITALITY, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
```
  Call it from `MagicEvents.onLogin` and `onRespawn` (before sync), and wherever stats or level
  change (StatPayload, commands, DevActionPayload level actions).
- [ ] **Step 8: `StatPayload`** (client → server, register in `ModNetwork` with `playToServer`):
```java
package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.PlayerStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: spend one stat point on a stat key (see StatPoints). The server checks it. */
public record StatPayload(String key) implements CustomPacketPayload {
    public static final Type<StatPayload> TYPE = new Type<>(ElementalArcana.id("stat"));
    public static final StreamCodec<ByteBuf, StatPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(StatPayload::new, StatPayload::key);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StatPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && MagicAttachments.get(player).spend(payload.key())) {
            PlayerStats.apply(player);
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
            MagicAttachments.sync(player);
        }
    }
}
```
- [ ] **Step 9: Commands** (`ArcanaCommand`): `/arcana stats reset` (self), `/arcana stats spend <key> <count>`
  (spends up to count, reports how many). Both call `PlayerStats.apply` and sync. `level set` also
  calls `PlayerStats.apply`.
- [ ] **Step 10: Lang**: `title.elementalarcana.level_up` → "Level %s";
  `title.elementalarcana.level_up.sub` → "%s stat points to spend";
  `screen.elementalarcana.status.level` → "Level %s".
- [ ] **Step 11: Build and test.** `./gradlew build --console=plain -q`. Expected: all tests pass.

---

### Task 3: Creature levels and ranks

**Files:**
- Modify: `api/AttunementRank.java`, `api/AttunementRules.java`, `core/MagicAttachments.java`,
  `content/Attunement.java`, `core/ArcanaCommand.java`
- Create: `content/CreatureLevels.java`, `data/elementalarcana/tags/worldgen/biome/dangerous.json`
- Test: `AttunementRulesTest.java`, new `AttunementRankTest.java`

**Interfaces:**
- Produces: `AttunementRank.bonusLevels()`, `xpMultiplier()`; `AttunementRules.rollRank(double distance, DoubleSupplier)`;
  `MagicAttachments.CREATURE_LEVEL` (Integer); `CreatureLevels.levelOf(LivingEntity)`,
  `displayLevel(LivingEntity)` (client-safe, may return 0 = unknown), `setBaseLevel(LivingEntity, int)`,
  `refreshStats(LivingEntity)`, `creaturePoints(LivingEntity)`.

- [ ] **Step 1: Tests.** `AttunementRulesTest`: replace `ranksNeedTheirMagicLevel` with
  `rarestRankWinsWithoutALevelGate` (`assertEquals(ARCHMAGE, rollRank(0, always(0)))`), drop the
  first argument everywhere else. `AttunementRankTest`:
```java
    @Test
    void ranksAreBonusLevelsWithMoreMana() {
        assertEquals(0, ADEPT.bonusLevels());
        assertEquals(8, MAGUS.bonusLevels());
        assertEquals(20, ARCHMAGE.bonusLevels());
        assertEquals(3.0, ADEPT.xpMultiplier(), 1e-9);
        assertEquals(5.0, MAGUS.xpMultiplier(), 1e-9);
        assertEquals(12.0, ARCHMAGE.xpMultiplier(), 1e-9);
    }
```
- [ ] **Step 2: `AttunementRank`** becomes `ADEPT(0.05, 0, 3.0), MAGUS(0.01, 8, 5.0), ARCHMAGE(0.001, 20, 12.0)`
  with `baseChance()`, `bonusLevels()`, `xpMultiplier()` (Javadoc: ranks are bonus levels on the
  shared curve; Attuned creatures hold more mana). `AttunementRules.rollRank(distance, random)`
  rolls every rank, rarest first.
- [ ] **Step 3: Attachment**:
```java
    // A creature's level from the zone it spawned in (ranks add bonus levels on top; see
    // CreatureLevels). Assigned on its first tick. Saved, and synced so Jade can show it.
    public static final Supplier<AttachmentType<Integer>> CREATURE_LEVEL = ATTACHMENT_TYPES.register("creature_level",
            () -> AttachmentType.builder(() -> 1)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());
```
- [ ] **Step 4: `CreatureLevels`** (content, `@EventBusSubscriber`):
```java
/**
 * Every creature has a level (see ZoneLevels): set from where it is on its first tick, plus its
 * rank's bonus levels if it's Attuned. Players use their own level. A creature's stats follow the
 * shared rules (StatRules): a third of its points each in Vitality, Ward and Potency.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class CreatureLevels {
    public static final TagKey<Biome> DANGEROUS = TagKey.create(Registries.BIOME, ElementalArcana.id("dangerous"));
    private static final ResourceLocation VITALITY = ElementalArcana.id("creature_vitality");
    // The health bonus Attuned creatures had before ranks became bonus levels; removed on sight.
    private static final ResourceLocation LEGACY_HEALTH = ElementalArcana.id("attunement_health");

    public static int levelOf(LivingEntity entity) {
        if (entity instanceof Player player) {
            return MagicAttachments.get(player).level();
        }
        if (!entity.hasData(MagicAttachments.CREATURE_LEVEL) && entity.level() instanceof ServerLevel level) {
            assign(level, entity);
        }
        return withRank(entity, entity.getData(MagicAttachments.CREATURE_LEVEL));
    }

    /** Client-safe: the level from synced data only; 0 if not known yet (or a player). */
    public static int displayLevel(LivingEntity entity) {
        if (entity instanceof Player || !entity.hasData(MagicAttachments.CREATURE_LEVEL)) {
            return 0;
        }
        return withRank(entity, entity.getData(MagicAttachments.CREATURE_LEVEL));
    }

    private static int withRank(LivingEntity entity, int base) {
        CreatureMagic magic = entity.hasData(MagicAttachments.CREATURE_MAGIC) ? entity.getData(MagicAttachments.CREATURE_MAGIC) : null;
        return Math.clamp(base + (magic == null ? 0 : magic.rank().bonusLevels()), 1, Progression.MAX_LEVEL);
    }

    public static void setBaseLevel(LivingEntity entity, int level) {
        entity.setData(MagicAttachments.CREATURE_LEVEL, Math.clamp(level, 1, Progression.MAX_LEVEL));
        refreshStats(entity);
    }

    /** A creature's points in each of Vitality, Ward and Potency. */
    public static int creaturePoints(LivingEntity entity) {
        return StatRules.creaturePoints(levelOf(entity));
    }

    /** Applies Vitality to max health, keeping the creature's share of health. */
    public static void refreshStats(LivingEntity entity) {
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || entity instanceof Player) {
            return;
        }
        float share = entity.getMaxHealth() > 0 ? entity.getHealth() / entity.getMaxHealth() : 1f;
        health.removeModifier(LEGACY_HEALTH);
        health.removeModifier(VITALITY);
        double bonus = StatRules.healthMultiplier(creaturePoints(entity)) - 1;
        if (bonus > 0) {
            health.addPermanentModifier(new AttributeModifier(VITALITY, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        entity.setHealth(entity.getMaxHealth() * share);
    }

    private static void assign(ServerLevel level, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        ZoneLevels.Dimension dimension = level.dimension() == Level.NETHER ? ZoneLevels.Dimension.NETHER
                : level.dimension() == Level.END ? ZoneLevels.Dimension.END : ZoneLevels.Dimension.OVERWORLD;
        BlockPos center = dimension == ZoneLevels.Dimension.OVERWORLD ? level.getSharedSpawnPos() : BlockPos.ZERO;
        double distance = Math.hypot(pos.getX() - center.getX(), pos.getZ() - center.getZ());
        boolean inStructure = !level.structureManager().getAllStructuresAt(pos).isEmpty();
        int zone = ZoneLevels.level(dimension, distance, pos.getY(), !level.canSeeSky(pos), inStructure,
                level.getBiome(pos).is(DANGEROUS), entity.getRandom().nextInt(ZoneLevels.SPREAD));
        entity.setData(MagicAttachments.CREATURE_LEVEL, zone);
        refreshStats(entity);
    }

    /** A creature without a level gets one on its first tick, wherever it came from. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)
                && living.level() instanceof ServerLevel level && !living.hasData(MagicAttachments.CREATURE_LEVEL)) {
            assign(level, living);
        }
    }
}
```
  `dangerous.json`: `{"values": ["minecraft:deep_dark"]}`.
- [ ] **Step 5: `Attunement`**: in `attune`, replace the `HEALTH_BONUS` line with
  `CreatureLevels.refreshStats(mob);` placed after `setData` and before `setHealth(getMaxHealth())`;
  in `clear`, replace the `HEALTH_BONUS` line with `CreatureLevels.refreshStats(mob)` after
  `removeData`. Delete the `HEALTH_BONUS` constant. `onFinalizeSpawn` calls
  `AttunementRules.rollRank(distance, ...)`.
- [ ] **Step 6: Command** `/arcana creaturelevel <targets>` (prints each target's name, level and
  max health) and `/arcana creaturelevel <targets> set <1-100>` (base level, players excluded).
- [ ] **Step 7: Build and test.** `./gradlew build --console=plain -q`. Expected: all tests pass.

---

### Task 4: Combat, reactions and kill XP

**Files:**
- Create: `content/LevelCombat.java`, `content/ReactionRewards.java`
- Modify: `api/ElementalReactions.java`, `content/CreatureRewards.java`, `api/AttunementRewards.java`
- Test: `AttunementRewardsTest.java`

**Interfaces:**
- Consumes: Tasks 1–3.
- Produces: `ReactionRewards.reacted(LivingEntity target, @Nullable Entity attacker)`,
  `ReactionRewards.reactedThisTick(LivingEntity)`; `AttunementRewards.essenceDrops(rank, double chanceMultiplier, DoubleSupplier)`.

- [ ] **Step 1: Tests.** `AttunementRewardsTest`: delete `magicXpByRank`; existing Essence tests
  call `essenceDrops(rank, 1.0, random)`; add
  `assertEquals(1, essenceDrops(null, 2.0, always(0.09)))` and
  `assertEquals(0, essenceDrops(null, 2.0, always(0.1)))`.
- [ ] **Step 2: `AttunementRewards`**: delete `magicXp`; `essenceDrops(rank, chanceMultiplier, random)`
  multiplies the plain (0.05), Adept (0.5) and Magus-two (0.5) chances, each capped at 1; Archmage
  stays 3 + min(2, (int) (r × 3)).
- [ ] **Step 3: `ReactionRewards`**:
```java
/**
 * Reactions pay: a hit that reacts (Melt, Vaporize, Freeze, Swirl) is boosted by the attacker's
 * Insight (see LevelCombat), and a player who sets off a reaction on a creature absorbs a fifth of
 * its kill XP, at most once per creature every 5 seconds.
 */
public final class ReactionRewards {
    private static final String TAG_REACTED_AT = "ea_reacted_at";
    private static final String TAG_XP_AT = "ea_reaction_xp_at";
    private static final int XP_GAP_TICKS = 100;
    private static final double XP_SHARE = 0.2;

    /** Called by ElementalReactions when {@code target} reacts; {@code attacker} if known. */
    public static void reacted(LivingEntity target, @Nullable Entity attacker) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        target.getPersistentData().putLong(TAG_REACTED_AT, now);
        ServerPlayer player = attacker instanceof ServerPlayer p ? p
                : target.getLastHurtByMob() instanceof ServerPlayer p && target.tickCount - target.getLastHurtByMobTimestamp() < XP_GAP_TICKS ? p : null;
        if (player == null || target instanceof Player || now < target.getPersistentData().getLong(TAG_XP_AT)) {
            return;
        }
        target.getPersistentData().putLong(TAG_XP_AT, now + XP_GAP_TICKS);
        int xp = (int) Math.round(CreatureRewards.killXp(player, target) * XP_SHARE);
        if (xp > 0) {
            CastingService.grantXp(player, xp);
        }
    }

    public static boolean reactedThisTick(LivingEntity target) {
        return target.getPersistentData().getLong(TAG_REACTED_AT) == target.level().getGameTime();
    }
}
```
- [ ] **Step 4: `ElementalReactions`**: call `ReactionRewards.reacted(target, null)` in `melt`
  (after the CRYO check), `vaporize`, and `freeze` (after it succeeds); in `swirl`,
  `ReactionRewards.reacted(target, attacker)` once after the aura check.
- [ ] **Step 5: `LevelCombat`** (`@EventBusSubscriber`, `LivingIncomingDamageEvent`):
```java
/**
 * Everyone plays by the same rules: damage between two creatures (players included) is scaled by
 * their level gap, x1.045 per level either way (Progression#damageLevelFactor). Elemental damage
 * is softened by the target's Ward and, from creatures, raised by their Potency (players' Potency
 * is already in their spell power). A hit that set off a reaction is raised by a player's Insight.
 */
@SubscribeEvent
public static void onIncomingDamage(LivingIncomingDamageEvent event) {
    LivingEntity target = event.getEntity();
    if (target.level().isClientSide()) {
        return;
    }
    DamageSource source = event.getSource();
    float amount = event.getAmount();
    LivingEntity attacker = source.getEntity() instanceof LivingEntity living && living != target ? living : null;
    if (attacker != null) {
        amount *= Progression.damageLevelFactor(CreatureLevels.levelOf(attacker), CreatureLevels.levelOf(target));
    }
    if (SpellDamage.elementOf(source) != null) {
        int ward = target instanceof Player player ? MagicAttachments.get(player).stats().get(Stat.WARD) : CreatureLevels.creaturePoints(target);
        amount *= StatRules.wardFactor(ward);
        if (attacker != null && !(attacker instanceof Player)) {
            amount *= (float) StatRules.effect(CreatureLevels.creaturePoints(attacker), Stat.POTENCY.exponent());
        }
    }
    if (attacker instanceof Player player && ReactionRewards.reactedThisTick(target)) {
        amount *= StatRules.insightFactor(MagicAttachments.get(player).stats().get(Stat.INSIGHT));
    }
    event.setAmount(amount);
}
```
- [ ] **Step 6: `CreatureRewards`**: add
```java
    /** XP {@code player} absorbs from killing {@code dead}: its mana by level, size and rank (see Progression#killXp). */
    public static int killXp(ServerPlayer player, LivingEntity dead) {
        CreatureMagic magic = Attunement.get(dead);
        AttributeInstance health = dead.getAttribute(Attributes.MAX_HEALTH);
        double size = Progression.sizeFactor(health == null ? 20 : health.getBaseValue());
        return Progression.killXp(MagicAttachments.get(player).level(), CreatureLevels.levelOf(dead), size,
                magic == null ? 1.0 : magic.rank().xpMultiplier());
    }
```
  `onDeath`: any non-player creature killed by a `ServerPlayer` grants `killXp` (no awakening
  check), with the existing action-bar message. `onDrops`: pass
  `StatRules.insightFactor(stats INSIGHT)` of the killer as the chance multiplier.
  Lang: `message.elementalarcana.magic_xp` → "+%s XP".
- [ ] **Step 7: Build and test.** `./gradlew build --console=plain -q`. Expected: all tests pass.

---

### Task 5: Screens and Jade

**Files:**
- Create: `client/StatsScreen.java`, `compat/jade/CreatureLevelProvider.java`
- Modify: `client/StatusScreen.java`, `client/DevScreen.java`, `compat/jade/ArcanaJadePlugin.java`, lang

- [ ] **Step 1: `StatsScreen`**: a panel (ArcanaDraw style, like StatusScreen), titled "Stats",
  with the level and unspent points in the header, then one row per `Stat` and one per awakened
  family's Affinity: name, points, the current effect, and a "+" button (active while points are
  free) that sends `StatPayload`. Hovering a row shows what the stat does. "Back" returns to the
  StatusScreen. Effects shown:
  - Reservoir: "Mana %s · Regen %s/s"
  - Potency: "Spell power ×%s"
  - Affinity: "%s spells ×%s" (the family's name)
  - Focus: "Cooldowns −%s%%"
  - Ward: "Elemental damage taken −%s%%"
  - Vitality: "Health ×%s"
  - Insight: "Reactions and Essence ×%s"
- [ ] **Step 2: `StatusScreen`**: the "Power" line becomes "Stat points: N" (gold when N > 0); a
  "Stats" button at the top left opens StatsScreen.
- [ ] **Step 3: `DevScreen`**: level buttons gain "Lv 50"; add a "Reset stats" dev action
  (`DevActionPayload.Action.RESET_STATS`, which calls `resetStats()` and `PlayerStats.apply`).
- [ ] **Step 4: Jade**: `CreatureLevelProvider` adds "Level %s" for non-player creatures with a
  known `displayLevel`; registered in `ArcanaJadePlugin`.
- [ ] **Step 5: Build.** `./gradlew build --console=plain -q`.

---

### Task 6: Docs and server checks

- [ ] **Step 1:** `tools/server_tests/levels.txt`:
```
time set midnight
forceload add 0 0
summon zombie 0.5 100 0.5 {NoAI:1b,Tags:["eatest"]}
sleep 1
arcana creaturelevel @e[tag=eatest]
arcana creaturelevel @e[tag=eatest] set 40
arcana creaturelevel @e[tag=eatest]
arcana attune @e[tag=eatest] fire archmage
arcana creaturelevel @e[tag=eatest]
arcana attune @e[tag=eatest] none
arcana creaturelevel @e[tag=eatest]
kill @e[tag=eatest]
forceload remove 0 0
```
  Expected: a level 1–6 zombie with 20 health; level 40 → max health ≈ 25.9 (20 × 1.04^6.5);
  Archmage → level 60, ≈ 29.0; cleared → level 40, ≈ 25.9.
- [ ] **Step 2:** Run `smoke`, `attunement`, `levels`, `bubble_prison`, `bubble_combos`, the
  `mobspells_*` tests and `essence_items`; zero `Exception|/ERROR]` lines in each.
- [ ] **Step 3:** README: "Mana and growth" becomes "Level and stats" (cap 100, XP from kills and
  reactions, stat points and the seven stats); "Creature magic" mentions zone levels, ranks as
  bonus levels and the level gap; commands list adds `stats` and `creaturelevel`; opposites at 50.
- [ ] **Step 4:** Full `./gradlew build`. Hand off for play-testing (the user launches the game).
