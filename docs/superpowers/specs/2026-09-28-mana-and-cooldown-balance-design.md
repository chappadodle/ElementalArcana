# Mana regen and cooldown balance: design

Date: 2026-09-28
Status: approved in conversation

## Problem

- Mana regenerates too slowly: 1/s at Magic Level 1, so a full bar takes 100 s.
- The basic attacks can be repeated almost instantly (Fireball 0.8 s, Icicle 0.6 s, Wind Blade 0.5 s).
- Getting better at a spell doesn't make it any quicker to use again.

## Decisions (approved by the user)

"Proficiency" means two things:
- **A spell's own level (1–10)** shortens that spell's cooldown.
- **Your Magic Level (1–30)** speeds up mana regeneration, since mana is shared by all spells.

Casting stays instant. The only brake is the cooldown, which starts long and shrinks as you level the spell.

### Cooldowns

`cooldown(level) = base × (1 − 0.06 × (level − 1))`, rounded to whole ticks. Lv 10 is 46% of Lv 1. Spells that don't level stay at `base`.

New Lv 1 cooldowns for the basic attacks:

| Spell | Before | Lv 1 | Lv 5 | Lv 10 |
|---|---|---|---|---|
| Fireball | 16 t | 60 t (3.0 s) | 46 t | 28 t (1.4 s) |
| Icicle | 12 t | 50 t (2.5 s) | 38 t | 23 t |
| Wind Blade | 10 t | 40 t (2.0 s) | 30 t | 18 t |
| Hydro Jet (after spraying) | 30 t | 80 t (4.0 s) | 61 t | 37 t |

Everything else keeps its base cooldown. Frost Shield, the other leveled spell, now shrinks too: 400 t at Lv 1, 184 t at Lv 10.

Everywhere a cooldown is used or shown goes through the same rule: the cast itself, the HUD's cooldown shading, the Status tooltip and the spell detail screen.

### Mana regen

`regen = 2.5 + 0.25 × (Magic Level − 1)` mana per second:

| Magic Level | 1 | 10 | 30 |
|---|---|---|---|
| Regen | 2.5/s | 4.75/s | 9.75/s |

Max mana is unchanged (100 + 10 per level), so a full bar always refills in about 40 s. Meditation (×3) and Mana Sickness (×0.5) are unchanged.

## Testing

Both formulas live in a pure-Java `Progression` class and are unit tested (JUnit). The feel is play-tested in game.
