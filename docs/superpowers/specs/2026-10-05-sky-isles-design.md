# Sky Isles

## Why

Drakes and the glide make the sky reachable, but there's nothing up there to reach. Islands
floating high over the land, seen from far off, give flying a destination and the horizon a
promise: something you can see but not yet get to.

## The isles

- Rare (about one every 750 blocks), over any Overworld land or sea, floating 70 to 100 blocks above
  the ground (and never under y 170).
- **Shape**: an upside-down mountain: a grassy top 9 to 13 blocks across, its underside tapering
  down in rough stone and dirt to a point a dozen blocks below, ores and a few amethyst glints
  showing in the rock. Moss and vines hang from the edges.
- **On top**: two or three trees (oak, birch or cherry), flowers, and a ruined **sky pavilion**:
  a ring of calcite pillars, some broken, round a mosaic floor, with a chest of sky loot.
- **Guardians**: two or three wind wisps circle the isle.
- **Satellites**: one isle in two has a smaller islet floating nearby, linked by nothing (a jump,
  a glide, or a drake).
- **Sky loot**: Wind Essence, feathers, golden carrots and apples, emeralds, the Feather of the Gale
  relic now and then, a Cantrip Scroll (one chest in four), an elytra very rarely.

## Code

| Piece | What it does |
|---|---|
| `api/SkyIsleRules` | Sizes, heights, the island's shape (tested) |
| `content/world/SkyIsleStructure`, `SkyIslePiece` | The structure (placed by height over the land) and its code-built island, trees, pavilion, chest |
| `tools/gen_sky_isles.py` | The structure, set and tags; the loot table |

## Testing

- `SkyIsleRulesTest`: the shape (radius by depth), the height rule.
- `tools/server_tests/sky_isles.txt`: an isle located (`/locate`), placed by command, its chest's
  loot rolled.
- `tools/autotest/sky_isles.txt`: an isle from below and from the side (shots), its top.
