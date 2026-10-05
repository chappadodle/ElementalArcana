# Server Settings

## Why

Whoever runs a server or builds a modpack has to be able to tune a mod to their world: fewer
rifts on a building server, no wild drakes on a peaceful one, the questline off where it doesn't
fit. Until now everything was fixed in code. This puts the world's events and creatures, and a few
choices about magic, in a settings file.

## The settings

NeoForge server settings, in `config/elementalarcana-server.toml` (a copy in a world's
`serverconfig/` folder overrides them for that world), also on the Mods screen's config button in
single player. Rates are multiples of the usual chance: 1 as the mod is made, 0 never, more for
more (a chance never passes certainty).

| Section | Setting | Default | What it does |
|---|---|---|---|
| events | `rifts` | 1 (0 to 10) | How often elemental rifts tear open near players |
| events | `starfall` | 1 (0 to 3) | How often a star falls near an awakened player at night (3: every night) |
| events | `manaTides` | on | Whether mana tides rise every few nights |
| creatures | `wisps` | 1 (0 to 10) | How often wisps appear |
| creatures | `wildCreatures` | 1 (0 to 10) | How often the Creatures of the Wild appear |
| creatures | `drakes` | 1 (0 to 10) | How often a wild drake flies in (nests keep theirs) |
| creatures | `golems` | 1 (0 to 10) | How often golems rise |
| creatures | `wanderingMage` | 1 (0 to 3) | How often a Wandering Mage comes of a morning (added with it) |
| magic | `naturalAwakening` | 1 (0 to 10) | How quickly magic wakes on its own (0: only a brush with an element, a Catalyst or a command) |
| magic | `sendingStone` | on | Whether awakened players get the Sending Stone (the questline) |

The commands that call these things up (`/arcana rift`, `/arcana starfall`, `/arcana wild`...)
ignore them. `/arcana config` shows the settings in force.

## Code

| Piece | What it does |
|---|---|
| `api/ConfigRates` | A chance at a rate, kept a chance (tested) |
| `core/ArcanaServerConfig` | The settings |
| `Rifts`, `Starfalls`, `ManaTides`, `WispSpawner`, `WildSpawner`, `DrakeSpawner`, `GolemSpawner`, `AwakeningEvents`, `Mentor` | Each reads its setting where its chance is rolled |
| `/arcana config` | The settings in force |

## Testing

- `ConfigRatesTest`: rates scale chances, and chances stay between 0 and 1.
- `tools/server_tests/config.txt`: the file written on the server's first start with it, with its
  defaults; `/arcana config` shows them.
