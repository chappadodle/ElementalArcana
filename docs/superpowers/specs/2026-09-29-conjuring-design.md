# Conjuring: choose how many projectiles you cast

Date: 2026-09-29
Status: approved in conversation

## Problem

A leveled projectile spell's level fixed its volley: Icicle Lv 2 always made 2 icicles, Lv 4
always 3, and all of them flew at once when R was released. You couldn't cast fewer, fire them
one at a time, or decide on the fly.

## Controls

All of these are rebindable in the Controls menu.

- **R (cast):** each press **conjures one more projectile** of the selected spell, up to the
  spell level's maximum (the old volley size, e.g. Icicle 1/2/3/5).
- **Launch one** (default **left click**): fires the oldest held projectile at the crosshair.
- **Launch all** (default **right click**): fires everything held, keeping each spell's special
  timing (e.g. Icicle Halo's ripple).
- **Mouse priority:** while at least one projectile is held, those clicks fire spells instead of
  swinging or using the held item. With none held, the mouse works normally. Rebinding the launch
  keys elsewhere leaves the mouse alone.
- **Fusion:** with a full set held and every projectile fully grown, **hold R for 1 s**. The
  capstone fusions (Glacial Lance, Sunfire, Scythe) then happen, and the result is launched with
  either click.

## Mana, growth, cooldown

- **Conjure cost:** the first projectile costs the spell's base price, each extra one the old
  per-extra price (Icicle 12 then 4, Fireball 15 then 5, Wind Blade 10 then 3), so a full set
  costs what the old volley did. Each conjure goes through the normal casting rules: overcast
  with health, Mana Sickness, Magic XP and mastery for the mana spent, and the cast event.
- **Growth:** each projectile grows while held, up to full size (the old charge, same speed per
  spell).
- **Upkeep:** holding costs **1 mana per second per projectile**, and projectiles can be held for
  as long as it's paid. Upkeep gives no XP. When a payment can't be made, everything held
  **launches itself** at the crosshair.
- **Cooldown:** it starts when the last held projectile leaves, and blocks conjuring until it
  ends. For every spell it is now `base × (1 − 6% per spell level above 1) × (1 − 1% per Magic
  Level above 1)`. A Lv 10 Icicle at Magic Lv 30 goes from 2.5 s to 0.8 s.
- **Ending early:** switching to another spell while holding launches what's held. Death,
  logout or a dimension change makes it fizzle, and the cooldown still starts. Creative and
  free-cast pay nothing and have no cooldown, as before.

## Scope

- **Switching to conjuring:** Icicle (step 1), Fireball (step 2) and Wind Blade (step 3). Every
  tier and branch keeps working.
- **Unchanged:** Hydro Jet (hold to spray), single-cast spells (Bubble Prison, Frost Nova…) and
  the mobs' spells.

## Structure

- **`api/ConjureSpell`:** what a conjurable spell provides:
  - `maxConjured(level)`
  - `conjureCost(level, alreadyHeld)`
  - `conjure(context, seed)`, which creates one held projectile
  - `launch(caster, projectiles, aim)`, which handles special timing
  - `canFuse(level, branches)` and `fuse(caster, projectiles)`
  - `onFullyGrown(projectile)`, for the ready chime
- **`core/Conjuring`:** the per-player session. It tracks the held list, lays out the formation
  (slot i of n), and handles upkeep, launching one or all, self-launch, fusion timing, cooldown,
  fizzle and spell switching. `CastingService` sends conjurable spells to it, and shares its
  payment code.
- **Network:** `CastSpellPayload` gains `LAUNCH_ONE` and `LAUNCH_ALL`.
- **Client:** `MagicData` gains a synced, unsaved `conjured` count. The client uses it to decide
  whether the mouse launches, by cancelling vanilla attack/use through
  `InputEvent.InteractionKeyMappingTriggered`.
- **Formula:** `Progression.cooldownTicks(base, spellLevel, magicLevel)` (unit tested).
