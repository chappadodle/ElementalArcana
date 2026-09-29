# Conjuring: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** the conjure-and-launch controls from `docs/superpowers/specs/2026-09-29-conjuring-design.md`, delivered in three play-tested steps:
1. The framework, with Icicle.
2. Fireball.
3. Wind Blade.

**Constraints:**
- **Git:**
  - Never modify git config.
  - After each step, the user play-tests. Commit only after they confirm.
  - Commit with the noreply env vars `GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com`, with the `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` trailer.
- **Sounds:** vanilla only.
- **Existing behavior:** Hydro Jet and the single-cast spells must behave exactly as before.

## Step 1: framework and Icicle

1. **Cooldown formula (TDD):**
   - Add `Progression.cooldownTicks(base, spellLevel, magicLevel)` and extend `ProgressionTest`: Icicle 50 t at Lv 10, Magic 30 → `round(50 × 0.46 × 0.71)` = 16.
   - `Spell.cooldownTicks(spellLevel, magicLevel)` replaces the one-argument version.
   - Every caller passes the Magic Level: `CastingService`, `SpellHudLayer`, `StatusScreen` and `SpellDetailScreen`.
2. **`api/ConjureSpell`**, as specified.
3. **`core/Conjuring`**, keyed by player UUID:
   - **Session state:** the spell, a `CastContext`, the held projectiles, a seed, the next upkeep tick, the R-held flag with its fuse-progress start, and a fused flag.
   - **Entry points:**
     - `press(player)`: conjure, or start fuse timing.
     - `releaseKey(player)`
     - `launchOne(player)` and `launchAll(player)`
     - `tick(player)`, called from the player tick next to `tickHold`
     - `onSpellSwitched(player)`
     - `cancel(player)`: fizzle, called from `CastingService.forget` and `cancelHold` paths
     - `heldCount(player)`
   - **Formation:** after any change, re-slot the projectiles `setFormation(i, n)`.
   - **When the session ends** (nothing held): start the cooldown with the new formula, unless creative or free-cast.
   - **Keeping the client informed:** set `MagicData.conjured` and sync on every change.
   - **Upkeep:** every 20 ticks, pay the number of projectiles held. If there isn't enough mana, launch all. Creative and free-cast skip upkeep.
   - **Fusion:** R held, a full set, every projectile at charge ≥ 1, the spell `canFuse`, not yet fused, and 20 ticks held → `fuse`. Fusing leaves a single projectile, and the set counts as full.
4. **`CastingService`:**
   - `tryCast` hands a `ConjureSpell` to `Conjuring.press`.
   - Extract the payment step (the cast event, overcast, sickness, XP, mastery, sound and swing) into a helper that both paths use.
   - `release` also calls `Conjuring.releaseKey`.
5. **Network:**
   - `CastSpellPayload` becomes an action enum: `PRESS`, `RELEASE`, `LAUNCH_ONE`, `LAUNCH_ALL`.
   - `SelectSpellPayload` calls `Conjuring.onSpellSwitched` before selecting.
   - Bump the protocol to `"4"`.
6. **`MagicData`:** a transient `conjured` int, written in the stream codec only (never saved), with a getter and a setter.
7. **Icicle** becomes a `ConjureSpell`:
   - `maxConjured` = the old `icicleCount`.
   - Conjure costs are 12, then 4 each.
   - `conjure` tags a held icicle with the level, branches and volley (the session seed).
   - `launch` releases the icicles, rippling 2 ticks apart when 5 go at once (Halo).
   - `canFuse` is true for Glacial Lance with more than one icicle, and `fuse` keeps the old forge code.
   - `onFullyGrown` plays the old chime and sparkles.
   - The `Hold` class is removed, and `cast` is never called for conjure spells.
8. **Client (`ArcanaClient`):**
   - `LAUNCH_ONE` and `LAUNCH_ALL` key mappings: mouse left and mouse right, in the Elemental Arcana category.
   - The client tick drains their clicks, and sends them only while `conjured > 0`.
   - `InputEvent.InteractionKeyMappingTriggered`: while `conjured > 0` and the triggered key matches a launch key, cancel it and don't swing.
   - Lang for both keys.
9. **Docs:** the README controls and cooldown text.
10. **Verify:**
    - `./gradlew build` passes.
    - Server regressions: `attunement.txt` and `mobspells_ice.txt` (the mob icicle uses `shootFrom`, which is unchanged).
    - `runClient`.

**Play-test:**
- Conjuring 1..max with R (it caps at max) and launching one or all.
- Mouse priority, and the normal mouse with nothing held.
- Upkeep draining 1/s per icicle, and self-launch when mana runs out.
- Glacial Lance: a full set, then hold R for 1 s.
- Halo's ripple.
- Cooldown after the last icicle.
- Switching spells launches what's held.
- Hydro Jet unchanged.

## Step 2: Fireball

`FireballSpell` becomes a `ConjureSpell`:
- **Max count:** `fireballCount`.
- **Costs:** 15, then 5 each.
- **Launch:** a Meteor arc per fireball.
- **Fusion:** Sunfire, keeping `forgeSun`.
- **Ready chime:** Fireball's.
- The `Hold` class is removed.

**Play-test**, then commit.

## Step 3: Wind Blade

`WindBladeSpell` becomes a `ConjureSpell`:
- **Max count:** `bladeCount`.
- **Costs:** 10, then 3 each.
- **Launch:** keeps the fan spread and boomerang.
- **Fusion:** Scythe, keeping `forgeScythe`.
- The `Hold` class is removed.

**Play-test**, then commit.
