# Clashing affinities and elemental resistance: design

Date: 2026-09-29
Status: approved in conversation

This resolves two open questions from `2026-09-28-elements-and-creature-magic-design.md`: what an opposed affinity pair means for a player, and whether players resist their own elements.

## Opposed elements unlock at Magic Level 30

The opposed pairs are Fire vs Water and Fire vs Ice (`Element#opposes`).

- **The lock:** below Magic Level 30, a player can't awaken an element that opposes one they already hold. From Lv 30 on they can, into **any** free slot. So a Fire-first player takes Wind at Lv 10, leaves the Lv 20 slot open, and fills both remaining slots with Water and Ice at Lv 30.
- **The awakening screen** still shows a locked element's card, dimmed and unclickable, with "Opposes Fire: unlocks at Magic Level 30".
- **Server-side:** `MagicData#awaken` enforces the rule, and both the screen and the server go through it.
- **Admin overrides:** `/arcana affinity add` and the dev menu toggle force an affinity and bypass the rule on purpose.
- **Existing players** who already hold an opposed pair keep it.

## Players resist their own elements

- Damage of an element the player has awakened hits them for **25% less**. That covers:
  - its spells (the `elementalarcana:<element>_spell` damage types), which get the "resisted"
    feedback (a dull thud and a puff);
  - its everyday damage (changed after the first play-test), with no feedback, since burning
    ticks every second:

    | Affinity | Everyday damage (vanilla damage-type tag) |
    |---|---|
    | Fire | `#minecraft:is_fire`: burning, fire, lava, magma, fire charges |
    | Ice | `#minecraft:is_freezing`: powder snow and frost |
    | Water | `#minecraft:is_drowning`: drowning |
    | Wind | `#minecraft:is_fall`: falling |

- **Why the change:** at first only spell damage counted. A measured close-range fight with a Fire
  Adept dealt 21 punch, 10 spell and 4 burning damage, so a Fire mage took only about 7% less
  overall. The difference was too small to notice.
- **Where it happens:** the existing matchup handler.

## Structure

- **`AffinityRules`** (pure Java, unit tested):
  - `blockingOpposite(owned, candidate, magicLevel)` returns the held element that blocks the candidate, or null.
  - `damageTaken(owned, element)` returns 0.75 or 1.
- **`SchoolElements`** maps a spell school to its element, or null for addon schools. The Essence code reuses it.
- **`MagicData#affinityElements()` and `#opposedBy(school)`** are used by `awaken` and by the awakening screen.
