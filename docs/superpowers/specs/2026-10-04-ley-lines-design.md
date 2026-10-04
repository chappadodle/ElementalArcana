# Ley Lines

## Why

The adventure now spans shrines, towers, sanctums, ruins and rifts across a large world, and the
only way between them is walking. Fantasy worlds let their mages travel along the lines of power
that join places like these. Ley lines make the shrines a network: once you've touched two, you can
step from one to the other for a little mana, which makes the shrines worth finding twice over and
the world easier to cross without making it smaller.

## How it works

- **Remembering:** every shrine a mage touches (right-clicks its core, whether it blesses them or
  not) is remembered: where it is and its element. The memory is the mage's own and survives death.
- **The ley menu:** sneak and right-click a Shrine Core (your magic must have woken) and the chat
  shows the shrines you remember, nearest first (up to twelve), each with its element, how far and
  which way: "Fire Shrine, 1,240 blocks north-east". Click one to travel.
- **Travelling:** you must still stand within 6 blocks of a Shrine Core. It costs mana: 10, plus 1
  for every 100 blocks, at most 50 (Blood Magic pays in health as usual; creative and free casting
  pay nothing). A pillar of the element's light takes you at the one shrine and sets you down at the
  other, in front of its core, facing it, with a breath of slow falling. Then the lines need a minute
  to settle before you travel again.
- A shrine that's gone (or a remembered spot with no core any more) is forgotten when you try it.
- Overworld only, like the shrines.
- An advancement, **Ley Walker**: travel along a ley line.

`/ley <x> <y> <z>` is the command the menu's entries run; anyone may use it, but only at a shrine
and only to a shrine they remember.

## Code

| Piece | What it does |
|---|---|
| `api/LeyRules` | The mana cost by distance, the cooldown, the reach, compass directions (tested) |
| `content/world/LeyLines` | The memory (a player attachment), the menu, `/ley` and the journey |
| `content/world/ShrineCoreBlock` | Sneak + use opens the menu; any use remembers the shrine |
| lang | The menu, messages, the advancement, a Journal page |

## Testing

- `LeyRulesTest`: costs by distance and their cap, compass directions.
- `tools/autotest/ley_lines.txt`: two shrines placed far apart, both touched, the menu shown, a
  journey taken (positions and mana before and after), the cooldown refusing a second.
