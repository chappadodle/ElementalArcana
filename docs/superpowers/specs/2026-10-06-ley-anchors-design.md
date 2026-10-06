# Ley Anchors

## Why

Ley lines join the shrines, but a mage's own places (a home, a mine, a camp by a drake's nest)
are off the network, so every trip home is a walk. Fantasy mages keep waystones. A **Ley Anchor**
is one: a standing stone a mage makes and sets down, which the ley lines then run to.

## The anchor

- **Making one**: four stone bricks at the corners, four amethyst shards at the sides, an ender pearl
  in the middle. Rename it in an anvil to name the place ("Home"); it keeps the name when broken and
  set down again.
- **Touching it** (use): it's remembered, as a shrine is ("The anchor knows you: Home"). Anyone may
  touch it, so friends can share a home on the network.
- **The ley menu**: sneak and use it, as at a Shrine Core: the shrines and anchors you remember,
  nearest first; travel from it to any of them, or from any shrine to it, at the usual cost (mana by
  distance) and with the usual minute for the lines to settle. An anchor shows by its name, in the
  amethyst's violet.
- You arrive next to it. Broken or gone, it's forgotten when someone tries to travel there.
- Overworld only, like the shrines (elsewhere it stays a stone; touching it says so).
- It glows a little (light 7) and sheds violet sparks, brighter when someone near remembers it.
- An advancement, **A Place of Your Own**: touch a Ley Anchor.

## Also: the Seeker's Compass and the Wandering Mage

- The compass also seeks **Hollowed camps** and **wisp rings**.
- The Wandering Mage also sells a **rumour of a Hollowed camp**.

## Code

| Piece | What it does |
|---|---|
| `content/world/LeyLines` | A remembered place is a shrine or an anchor (with its name); either is a place to travel from |
| `content/world/LeyAnchorBlock`, `LeyAnchorBlockEntity` | The stone, its name, touching and the menu |
| `api/SeekerRules` | The compass's two new kinds |
| `tools/gen_world.py` or `tools/gen_anchors.py` | Its art, model, loot (keeping the name) and recipe |

## Testing

- `LeyRulesTest` (unchanged), `SeekerRulesTest` (the kinds go round).
- `tools/server_tests/ley_anchors.txt`: an anchor set, its name, broken (its drop keeps the name).
- `tools/autotest/ley_anchors.txt`: an anchor named "Home" set far from a shrine, both touched, the
  menu at the shrine (listing Home), the journey (positions and mana logged), arriving by the anchor.
