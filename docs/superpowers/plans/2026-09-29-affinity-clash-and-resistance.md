# Clashing Affinities and Elemental Resistance: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:**
- Opposed elements (Fire vs Water, Fire vs Ice) can't be awakened together before Magic Level 30.
- Players take 25% less spell damage of their own elements.

**Architecture:**
- **Rules:** pure `AffinityRules` (unit tested).
- **School to element:** `SchoolElements` (shared).
- **Magic data:** `MagicData#awaken` enforces the lock.
- **Screen:** `AwakeningScreen` shows locked cards.
- **Damage:** `ElementalMatchups` applies the resistance to players.

**Spec:** `docs/superpowers/specs/2026-09-29-affinity-clash-and-resistance-design.md`

## Global Constraints

- **Unlock level:** 30.
- **Resistance:** ×0.75, for spell damage only.
- **Admin commands and the dev menu keep bypassing the lock.** Nothing is removed from existing players.
- **Git:**
  - Never modify git config.
  - Commit only after the user play-tests and confirms.
  - Commit with the noreply env vars `GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com`, with the `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` trailer.
- `.../` = `src/main/java/com/chappadodle/elementalarcana/`.

### Task 1: `AffinityRules` (pure, TDD)

**Files:** create `.../api/AffinityRules.java` and `src/test/java/com/chappadodle/elementalarcana/api/AffinityRulesTest.java`.

**Produces:**
- `AffinityRules.OPPOSITES_UNLOCK_LEVEL = 30`
- `@Nullable AffinityRules.blockingOpposite(Collection<Element> owned, Element candidate, int magicLevel) : Element`
- `AffinityRules.spellDamageTaken(Collection<Element> owned, Element spell) : float`

- [ ] **Step 1:** Write the test.
  - Fire owned: Water and Ice are blocked by Fire at Lv 29 and free at Lv 30; Wind is never blocked.
  - Water owned: Fire is blocked by Water; Ice is free.
  - Nothing owned: nothing is blocked.
  - `spellDamageTaken({FIRE}, FIRE) = 0.75` and `spellDamageTaken({FIRE}, WATER) = 1`.
- [ ] **Step 2:** Run `./gradlew test`. It should FAIL, because the class doesn't exist yet.
- [ ] **Step 3:** Implement the class.
- [ ] **Step 4:** Run `./gradlew test`. It should PASS.

### Task 2: School mapping, the lock, and the screen

**Files:**
- Create: `.../api/SchoolElements.java`
- Modify:
  - `.../content/EssenceService.java` (`elementOf` delegates to `SchoolElements`)
  - `.../core/MagicData.java` (`affinityElements`, `opposedBy`, and `awaken` refuses a locked element)
  - `.../client/AwakeningScreen.java` (a locked card is dimmed, unclickable, and shows the "opposed" line in place of "Starts with")
  - `lang/en_us.json` (`screen.elementalarcana.awakening.opposed`: "Opposes %s: unlocks at Magic Level %s")

### Task 3: Resistance, docs, verify, play-test, commit

**Files:**
- `.../content/ElementalMatchups.java`: a player target's multiplier comes from `AffinityRules.spellDamageTaken(MagicAttachments.get(player).affinityElements(), spell)`.
- `README.md`: the affinity rules.
- The creature magic spec: its open questions are now resolved.

**Verify:**
- `./gradlew build` passes (45 tests).
- `tools/server_console.sh` passes with `attunement.txt` and `mobspells_fire.txt`, with no errors.
- `runClient` starts, and its log has no errors.

**Play-test:**
1. **Lock:** with the dev menu, reset elements, then set Magic Lv 10. Awaken Fire, then open Awaken. Water and Ice are dimmed with "Opposes Fire: unlocks at Magic Level 30", and only Wind can be picked.
2. **Unlock:** set Lv 30. Water and Ice can now be awakened.
3. **Resistance:** as a Fire mage, a Fire Adept's fireball hits you for less, with a dull thud. An Ice Adept's icicle hits you normally.
