# Brews

Date: 2026-10-02
Status: built (milestone 8 of `2026-10-02-fantasy-adventure-roadmap.md`).

For when the pool runs dry. Four brews, made in a brewing stand from an Awkward Potion like any
vanilla potion, and like them they can be made splash or lingering (gunpowder, dragon's breath) or
dipped into tipped arrows.

| Brew | Ingredient | Effect | Stronger (glowstone) | Longer (redstone) |
|---|---|---|---|---|
| Mana Draught | an amethyst shard | refills 30% of your mana at once | 60% | |
| Elixir of Clarity | a Wisp Mote | mana regenerates twice as fast, 3:00 | three times as fast, 1:30 | 8:00 |
| Elixir of Focus | lapis lazuli | +6 Focus (shorter cooldowns), 3:00 | +10, 1:30 | 8:00 |
| Elixir of Warding | prismarine crystals | +6 Ward (less elemental damage), 3:00 | +10, 1:30 | 8:00 |

- A splash Mana Draught gives less mana the farther from where it lands, like a splash of healing.
  Mana that still sleeps can't take it.
- Focus and Warding add their points to the same stats as gear and the skill tree (`GearStats`),
  so they show in the Stats window. Clarity multiplies the mana regen (`MagicEvents`), stacking
  with meditation and places of power.
- **Wisp Motes** are what wisps leave behind: half the time a player kills one, more with Looting.
  That gives wisps a second reason to hunt them.
- Arcanists sell Mana Draughts (Apprentice) and Elixirs of Clarity (Journeyman). Mage tower
  laboratories and Arcanist cottages have Mana Draughts in their chests.

## Testing

- `tools/server_tests/brews.txt`: the effects apply to a player-less world without errors, and the
  potions exist.
- `tools/autotest/brews.txt`: a brewing stand brewing each brew, and a Mana Draught refilling an
  emptied pool.
