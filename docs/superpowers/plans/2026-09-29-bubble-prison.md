# Bubble Prison: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** the Bubble Prison spell as specified in `docs/superpowers/specs/2026-09-29-bubble-prison-design.md`.

**Architecture:**
- **Data:** a synced attachment `Bubble`, a record in `api`.
- **Server logic:** `BubblePrisons` handles trapping, holding, popping, releasing, blocking a trapped player's actions, and turning a mob's AI back on after a restart.
- **The spell:** `BubblePrisonSpell` targets the creature under the crosshair and calls `BubblePrisons.trap`.
- **Client:** `BubbleRenderer` draws the sphere through `RenderLivingEvent.Post`, and the client tick holds the local player when they're trapped.
- **Casting:** `CastingService.whyNotCastable` refuses spells while trapped.

**Constraints:**
- The numbers are in the spec.
- Vanilla sounds only.
- **Git:**
  - Never modify git config.
  - Commit only after the user play-tests and confirms.
  - Commit with the noreply env vars `GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com`, with the `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` trailer.

## Tasks

1. **Data and server logic:**
   - the `api/Bubble` record, with its stream codec;
   - `MagicAttachments.BUBBLE`, synced and not saved;
   - `content/BubblePrisons`: `isTrapped`, `holdPoint`, `trap`, `release`, the tick hold, the pop on a player's hit, the AI restore on join, and the cancelled actions;
   - `CastingService.whyNotCastable` refuses while trapped.
2. **The spell:**
   - `content/spell/BubblePrisonSpell`, registered in `ModSpells` after `tidal_wave`;
   - its lang (name, description, and the no-target and trapped messages);
   - its icon, drawn with `tools/gen_textures.py`.
3. **Visuals:**
   - `models/spell/bubble.json`, a full-block rounded sphere;
   - `textures/block/bubble.png`, translucent;
   - `client/BubbleRenderer`;
   - model registration, and the render hook for every living entity;
   - the client tick hold for the local player.
4. **Verify:**
   - `./gradlew build` passes.
   - The server test `tools/server_tests/bubble_prison.txt` can only check what doesn't need a player, because casting needs one. It runs the attunement regression, and `/summon` and `/kill` of a mob with a leftover AI-off marker, to confirm the AI comes back on.
   - `runClient`.
   - Then the user's play-test.

## Play-test

1. **Unlock:** reach Magic Lv 3 with Water; Bubble Prison is in the wheel.
2. **Trap a zombie:** it rises in a wobbling bubble, can't move or hit you, and drips (it's Wet). After 4 s it drops.
3. **Pop it:** hit a trapped zombie. It pops with a splash, the hit does +4, and it drops.
4. **Combos:** bubble then Icicle freezes it; bubble then Fireball gives steam (Vaporize).
5. **Attuned mobs:** a trapped Attuned mob doesn't cast.
6. **No target:** casting at nothing says so, and costs no mana or cooldown.
7. **PvP (optional, needs a second player):** a trapped player can't move, hit, use items or cast for 4 s.
