# Shrines of the kin elements

## Why

Crystal, Lightning and Radiance now have full spell ladders, but no places of their own: their
mages bless themselves at their family's shrines, and the ley lines (2026-10-04-ley-lines-design.md)
have no stops in their lands. Each gets a shrine, built in its own look and standing where it feels
at home, so the network grows by three kinds and the world by three more things to find.

## The shrines

Built like the others (`ShrinePiece`: a round platform, the Shrine Core on its pedestal, four
pillars, an offering chest), in their own stone:

| Shrine | Look | Where |
|---|---|---|
| Crystal | a geode opened to the sky: calcite round amethyst, basalt trim, amethyst pillars crowned with crystals | stony peaks, jagged peaks, stony shores, gravelly windswept hills |
| Lightning | copper and tuff: cut copper floor, oxidized trim, tuff bricks, copper pillars under lightning rods | windswept hills, savanna plateaus, windswept savannas and forests |
| Radiance | white quartz round a ring of shroomlight, glowstone burning on quartz pillars | sunflower plains, meadows, cherry groves, flower forests |

- They work like every shrine: faster mana nearby, a daily blessing (their family's, as before),
  a chance to wake the magic of one who still sleeps, guardian wisps of their own element, an
  offering chest (their own Essence, their family's Catalyst now and then), and they're ley stops.
- They share the shrines' placement (one structure set), so they don't make the others rarer.
- Mage towers of these elements keep their family's heart, acolytes and Magister (their Guardian
  Cores feed the master staffs' recipes).

## Testing

- A server test (`derived_shrines`): each located from spawn, their chests' loot rolled and listed.
- `tools/autotest/derived_shrines.txt`: one of each found in fresh land and viewed.
- The server suite (`shrines` still finds and blesses at the base ones).
