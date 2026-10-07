# The Ember Reaches, part 2: the Ember Anvil and tempering

## Why

Part 1 gave the Nether its Cinder Forges and the Forgewarden, whose Ember Core so far makes only a
charm (and good fuel). A mage's gear (foci and robes) has four tiers and then stops: once a player
wears what the Sovereigns guard, there's nothing more to do with it. **Tempering** gives the forges
a reason to be sought again and again: an Ember Anvil works an Ember Core into a focus or a piece of
robe, and each temper makes the piece a little stronger.

## The Ember Anvil

- Stands on every Cinder Forge's dais (where part 1 had a plain anvil), and can be made: an anvil,
  an Ember Core, two polished blackstone and three magma blocks. It has an anvil's shape in blackstone
  and dark iron with a glowing ember inlay in its face, gives off a little light (7), and is mined
  with a pickaxe like an anvil (it doesn't fall).
- It does one thing: **tempering**. Use it holding a focus (a wand or a staff) or a piece of a mage's
  robe, carrying an Ember Core: the core and five experience levels are spent, the piece is
  tempered once more (sparks, a hiss, the anvil's ring), and its tooltip says so ("Tempered 2/3").
  - A piece takes three tempers at most.
  - Short of a core, of levels, or with a piece that can't take more, the anvil says why (above the
    hotbar) and nothing is spent. In creative mode it costs nothing.
- Holding anything else, using it does nothing.

## What a temper does

- Each temper adds **one point to each of the piece's stats**: an Adept Staff of Fire (Potency +4,
  Focus +2, Fire Affinity +6) tempered three times gives Potency +7, Focus +5, Fire Affinity +9; a
  master's robe (Reservoir +8, Ward +5) tempered twice, Reservoir +10, Ward +7. The tooltip shows the
  tempered numbers.
- It changes nothing else (the level the piece needs, which of two held foci counts: still the
  better tier).

## Also

- Cinder Forges' storerooms now hold an Ember Core now and then.
- An advancement, **Tempered** (under Into the Forge): temper a piece of gear.
- A journal page on tempering.

## Code

| Piece | What it does |
|---|---|
| `api/TemperRules` | The most tempers, the cost, a piece's stats tempered (tested) |
| `content/forge/EmberAnvilBlock` | The anvil: its shape, the tempering |
| `content/forge/ModForge` | The anvil, its item, the `elementalarcana:tempered` component |
| `content/gear/GearStats`, `GearTooltips` | A piece's stats counted and shown tempered |
| `content/forge/CinderForgePiece` | The Ember Anvil on the dais |
| `tools/gen_forge.py` | The anvil's textures, model, loot, recipe; a core in the storerooms' loot |

## Testing

- `TemperRulesTest`.
- `tools/autotest/tempering.txt`: an Ember Anvil set down; a Fire Adept Staff held, three cores and
  levels carried: tempered three times (the staff's component logged after each; levels and cores
  counted), the fourth refused; the tooltip (a shot); the player's Potency with the staff held,
  before and after (logged).
