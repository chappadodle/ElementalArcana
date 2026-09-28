# Elemental Essence: spell progression uses

Date: 2026-09-28
Status: approved in conversation

## Goal

Give Elemental Essence (dropped by elemental creatures, see the creature magic spec) a use: it speeds up **spell progression**, in two ways the user chose:

1. **Infuse:** Essence fills the mastery bar of a spell of the same element.
2. **Condense:** Essence becomes **bonus skill points**. Magic Levels give 29 points, and maxing all five leveled spells takes 45, so skill points are the real bottleneck.

## Infuse (the spell detail screen)

- One Essence of the spell's element fills **50% × 0.8^(level − 1)** of that level's mastery bar. It's exponential: the higher the spell's level, the less each Essence fills.
- The amount is `round(bar × fraction)`, and always at least 1 mastery.

| Spell level | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| One Essence fills | 50% | 40% | 32% | 26% | 20% | 16% | 13% | 10% | 8% |
| Essence for a full bar | 2 | 3 | 4 | 4 | 5 | 7 | 8 | 10 | 12 |

  In total, 55 Essence takes a spell from Lv 1 to 10 with no casting.

- **The button** sits under the mastery bar. It reads "Infuse 1 Fire Essence (+32%)", or, while Shift is held, "Infuse N Fire Essence (fill)" (just enough to fill the bar). Mastery is still capped at a full bar.
- **Disabled when:** the spell is locked (you can't cast it), it's at max level, its bar is full, or you have no Essence of its element. The tooltip says which.

## Condense (the Status screen)

- Essence of **any mix of elements** becomes **+1 bonus skill point**.
- The cost **rises**: 8 for the first bonus point, then +4 each time (8, 12, 16, 20…), so the 16 points needed to max everything cost 608 Essence.
- The Essence you have the most of is used first.
- Bonus points are saved and synced with your magic data, add to the points from Magic Level, and survive death, like the rest of your magic data.
- **The button** sits under the affinities. It reads "Condense 12 Essence → +1 skill point", its tooltip shows how many Essence you have, and it's disabled when you don't have enough.

## Rules and safety

- The server re-checks everything, and it is the server that counts and removes the Essence from the inventory (main inventory, hotbar and offhand).
- A spell's element is its school (fire, water, ice or wind). Spells of addon schools can't be infused.
- Feedback: a vanilla chime (`AMETHYST_BLOCK_RESONATE`) with element-colored particles on infuse, and `ENCHANTMENT_TABLE_USE` with the magic level-up style burst on condense.
- The Essence tooltip now says how to use it.

## Testing

The formulas (`essenceBarFraction`, `essenceMastery`, `essenceToFill`, `condenseCost`) live in the unit-tested `Progression` class. The screens and the item handling are play-tested; test Essence comes from `/give`.
