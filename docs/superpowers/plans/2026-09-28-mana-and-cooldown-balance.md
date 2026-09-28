# Mana Regen and Cooldown Balance: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:**
- Spell cooldowns shrink 6% per spell level (Lv 10 = 46%).
- The basic attacks get real Lv 1 cooldowns.
- Mana regen becomes 2.5/s, +0.25/s per Magic Level.

**Architecture:**
- **The formulas:** they live in a pure-Java `api/Progression` class (unit tested).
- **Cooldowns:** `Spell#cooldownTicks(int spellLevel)` applies the formula, and every place that starts or shows a cooldown switches to it.
- **Regen:** `MagicData#regenPerSecond()` uses the regen formula.

**Tech Stack:** NeoForge 21.1.252 (MC 1.21.1), Java 21, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-28-mana-and-cooldown-balance-design.md`

## Global Constraints

- Cooldown = `round(base × (1 − 0.06 × (level − 1)))`, with the level clamped to at least 1.
- New base cooldowns: Fireball 60, Icicle 50, Wind Blade 40, Hydro Jet 80 (ticks).
- Regen = `2.5 + 0.25 × (magicLevel − 1)` per second.
- Git: never modify git config. Commit only after the user play-tests and confirms, with the noreply env vars `GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com` and the `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` trailer.
- `.../` = `src/main/java/com/chappadodle/elementalarcana/`.

---

### Task 1: The formulas (pure, TDD)

**Files:**
- Create: `.../api/Progression.java`
- Test: `src/test/java/com/chappadodle/elementalarcana/api/ProgressionTest.java`

**Interfaces:**
- Produces:
  - `Progression.cooldownTicks(int baseTicks, int spellLevel) : int`
  - `Progression.regenPerSecond(int magicLevel) : float`

- [ ] **Step 1: Failing test.** Create `src/test/java/com/chappadodle/elementalarcana/api/ProgressionTest.java`:

```java
package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static com.chappadodle.elementalarcana.api.Progression.cooldownTicks;
import static com.chappadodle.elementalarcana.api.Progression.regenPerSecond;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressionTest {

    @Test
    void cooldownsShrinkSixPercentPerSpellLevel() {
        assertEquals(60, cooldownTicks(60, 1));
        assertEquals(46, cooldownTicks(60, 5));   // 60 x 0.76 = 45.6
        assertEquals(28, cooldownTicks(60, 10));  // 60 x 0.46 = 27.6
        assertEquals(23, cooldownTicks(50, 10));
        assertEquals(18, cooldownTicks(40, 10));
        assertEquals(37, cooldownTicks(80, 10));
        assertEquals(184, cooldownTicks(400, 10));
    }

    @Test
    void levelBelowOneCountsAsOne() {
        assertEquals(60, cooldownTicks(60, 0));
    }

    @Test
    void regenGrowsWithMagicLevel() {
        assertEquals(2.5f, regenPerSecond(1), 1e-6f);
        assertEquals(4.75f, regenPerSecond(10), 1e-6f);
        assertEquals(9.75f, regenPerSecond(30), 1e-6f);
    }
}
```

- [ ] **Step 2:** Run `./gradlew test --console=plain`. Expected: FAIL, `cannot find symbol` for `Progression`.

- [ ] **Step 3: Implement.** Create `src/main/java/com/chappadodle/elementalarcana/api/Progression.java`:

```java
package com.chappadodle.elementalarcana.api;

/**
 * How getting better makes magic faster. Plain Java, no Minecraft types (unit tested). A spell's
 * own level shortens its cooldown; your Magic Level speeds up mana regeneration.
 */
public final class Progression {
    // Each spell level above 1 takes this share of the Lv 1 cooldown off (Lv 10 = 46%).
    private static final float COOLDOWN_CUT_PER_LEVEL = 0.06f;
    private static final float BASE_REGEN_PER_SECOND = 2.5f;
    private static final float REGEN_PER_SECOND_PER_LEVEL = 0.25f;

    private Progression() {
    }

    /** A spell's cooldown at {@code spellLevel}, from its Lv 1 cooldown {@code baseTicks}. */
    public static int cooldownTicks(int baseTicks, int spellLevel) {
        int level = Math.max(1, spellLevel);
        return Math.round(baseTicks * (1f - COOLDOWN_CUT_PER_LEVEL * (level - 1)));
    }

    /** Mana regenerated per second at {@code magicLevel}. */
    public static float regenPerSecond(int magicLevel) {
        return BASE_REGEN_PER_SECOND + REGEN_PER_SECOND_PER_LEVEL * (Math.max(1, magicLevel) - 1);
    }
}
```

- [ ] **Step 4:** Run `./gradlew test --console=plain`. Expected: `BUILD SUCCESSFUL`, and `grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' build/test-results/test/TEST-*.xml` shows ProgressionTest `tests="3" … failures="0"` (31 tests in total).

---

### Task 2: Use them everywhere

**Files:**
- Modify: `.../api/Spell.java`
- Modify: `.../core/MagicData.java` (`regenPerSecond`, and remove the old regen constants)
- Modify: `.../core/CastingService.java` (the two `startCooldown` calls)
- Modify: `.../client/SpellHudLayer.java`, `.../client/StatusScreen.java`, `.../client/SpellDetailScreen.java`
- Modify: `.../content/spell/FireballSpell.java`, `IcicleSpell.java`, `WindBladeSpell.java`, `HydroJetSpell.java` (base cooldowns)
- Modify: `README.md`

**Interfaces:**
- Consumes: `Progression` (Task 1).
- Produces: `Spell#cooldownTicks(int spellLevel) : int`.

- [ ] **Step 1: `Spell`.** In `.../api/Spell.java`, add after `cooldownTicks()`:

```java
    /** Cooldown at a given spell level: 6% of the Lv 1 cooldown shorter per level (Lv 10 = 46%). */
    public int cooldownTicks(int spellLevel) {
        return Progression.cooldownTicks(cooldownTicks(), spellLevel);
    }
```

In the same file, change the `cooldownTicks()` Javadoc (add one if it has none) to `/** Cooldown at Lv 1, in ticks. */`.

- [ ] **Step 2: Regen.** In `.../core/MagicData.java`:
  - Make `regenPerSecond()` return `Progression.regenPerSecond(level);`.
  - Delete the constants `BASE_REGEN_PER_SECOND` and `REGEN_PER_SECOND_PER_LEVEL`.
  - Add `import com.chappadodle.elementalarcana.api.Progression;`.

- [ ] **Step 3: Casting.** In `.../core/CastingService.java`:
  - Replace `spell.cooldownTicks()` with `spell.cooldownTicks(data.spellLevel(spell))`.
  - In `endHold`, replace `active.spell().cooldownTicks()` with `active.spell().cooldownTicks(data.spellLevel(active.spell()))`.

- [ ] **Step 4: UI.** Every displayed cooldown uses the player's level in that spell:
  - `.../client/SpellHudLayer.java`: the two `spell.cooldownTicks()` calls become `spell.cooldownTicks(data.spellLevel(spell))`. Use the `MagicData` variable already in scope in that method. If it has another name, use that; if there is none, add `MagicData data = MagicAttachments.get(minecraft.player);` before the loop.
  - `.../client/StatusScreen.java`: `spell.cooldownTicks()` in the tooltip becomes `spell.cooldownTicks(data.spellLevel(spell))`, with the `MagicData` in scope there, under whatever name it has.
  - `.../client/SpellDetailScreen.java`: `spell.cooldownTicks()` becomes `spell.cooldownTicks(level)`, the same `level` already passed to `spell.manaCost(level)` on that line.

- [ ] **Step 5: Base cooldowns.** Change the constructors:
  - `FireballSpell`: `super(ModSchools.FIRE, 15, 16)` → `super(ModSchools.FIRE, 15, 60)`
  - `IcicleSpell`: `super(ModSchools.ICE, 12, 12)` → `super(ModSchools.ICE, 12, 50)`
  - `WindBladeSpell`: `super(ModSchools.WIND, 10, 10)` → `super(ModSchools.WIND, 10, 40)`
  - `HydroJetSpell`: `super(ModSchools.WATER, 20, 30)` → `super(ModSchools.WATER, 20, 80)`

- [ ] **Step 6: README.** In the **Mana and growth** list:
  - Change "Each level adds +10 max mana, faster regen and +2% spell power (max level 30)." to "Each level adds +10 max mana, +0.25 mana/s regen (2.5/s at level 1) and +2% spell power (max level 30)."
  - In the paragraph starting "Fireball, Hydro Jet, Icicle, Frost Shield and Wind Blade level from 1 to 10.", add the sentence "Each level also shortens the spell's cooldown (Lv 10: under half of Lv 1)." after the first sentence.

- [ ] **Step 7: Verify.** Run `./gradlew build --console=plain` (`BUILD SUCCESSFUL`, 31 tests), then `grep -rn "cooldownTicks()" src/main/java`. The only matches allowed are the declaration in `Spell.java` and the `Progression.cooldownTicks(cooldownTicks(), ...)` call. Then run `tools/server_console.sh tools/server_tests/smoke.txt` (it prints `The time is …`) and launch `runClient` for the user.

- [ ] **Step 8: Play-test checklist (for the user)**
  - At Magic Lv 1, the Status screen shows regen **2.5/s**; at Lv 10 (dev menu), **4.8/s**.
  - Fireball at Lv 1 can be thrown about every 3 s (the HUD icon shades over for 3 s). With dev menu +1 levels, the tooltip and detail screen cooldowns drop, down to about 1.4 s at Lv 10.
  - Icicle, Wind Blade and Hydro Jet feel slower at Lv 1 and quicker as they level.
  - Emptying the bar and waiting refills it in about 40 s.

- [ ] **Step 9: Commit and push (after the user confirms)**

```bash
git add README.md docs src
GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com git commit -F - <<'EOF'
Balance mana regen and spell cooldowns

Mana regen is now 2.5/s at Magic Level 1, +0.25/s per level (a full bar
in about 40 s at any level). Spell cooldowns shrink 6% per spell level
(Lv 10 = 46%), and the basic attacks get real Lv 1 cooldowns: Fireball
3 s, Icicle 2.5 s, Wind Blade 2 s, Hydro Jet 4 s.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git push
```
