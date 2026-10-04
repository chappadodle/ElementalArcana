# Elemental Drakes

## Why

The world's dangers walk or float a little off the ground; the sky is empty. A fantasy world needs
dragons: something huge you see from far off, circling a peak, that makes you think twice about
climbing it. Drakes are the elements given wings. They guard nests on the high places, swoop and
breathe their element, and fall for good loot. (A later step can let mages hatch and ride them.)

## The drakes

One creature, five elements, each with its own colours and breath:

| Drake | Where | Its breath |
|---|---|---|
| **Fire Drake** | badlands, savanna plateaus, windswept savannas | a cone of fire: burns, sets alight |
| **Frost Drake** | snowy slopes, frozen and jagged peaks, ice spikes | a cone of frost: slows, frosts, freezes the wet |
| **Storm Drake** | windswept hills and forests, stony peaks; anywhere in a thunderstorm | a crackling cone: hurts more in rain or water, sparks leap to one more foe |
| **Tide Drake** | over the sea (oceans, beaches), skimming low | a torrent: shoves back hard and soaks |
| **Gale Drake** | meadows, cherry groves, windswept gravelly hills | a gust cone: throws you back and up (Airborne) |

- A wyvern's shape, Minecraft style: a long body, a neck and horned head with jaws that open, a
  tail in segments, two legs and two great wings (about 9 blocks across), all in blocks, its belly
  and wing membranes in its element's colours.
- Born to its element (the element chart applies), at its place's level plus 10, with 140 health
  (more with its level), 8 armour, and hard to knock about. Fire drakes don't burn; tide drakes
  don't drown.
- **Soaring:** it circles its nest (or where it was born) 18 to 28 blocks up, in wide loops, its
  wings beating.
- **Hunting:** a player within 40 blocks it can see (not its kin, who share its element's family,
  unless they hurt it) becomes its prey. It never chases more than 64 blocks from home.
- **Swoop:** it dives at its prey, rakes with its claws (10 damage and a shove) and climbs away.
  Every 6 seconds at most.
- **Breath:** it comes round to hover 8 to 12 blocks off, faces its prey, opens its jaws (a warning
  roar and glow, a second) and breathes for 2 seconds: a cone 10 blocks long. Every 9 seconds at
  most.
- **Resting:** with no prey for a while it lands at its nest (or on the ground below) for a time,
  head down, and heals; anything that comes near or hurts it wakes it.
- **Falling:** at a quarter health it can no longer climb: it fights on the ground, still breathing.
- Drops: 2 to 4 **Drake Scales** of its element, 2 to 4 of its Essence, plenty of experience.

## Nests

- On high, open ground in each drake's lands (90 or more blocks up, or on the coast for tide drakes),
  about one every 1000 blocks, never within 1000 of the world's centre: a ring of tumbled stone
  and charred wood round a hollow lined with bones, gravel and its element's stuff (magma, snow,
  copper, prismarine, calcite), a few eggs-shaped stones, and a **hoard** chest (gold, emeralds,
  scales, Essence, now and then a relic of its element).
- The nest's drake is born when the nest is first approached (within 64 blocks), circling overhead.
  Each nest has one, once.
- Drakes are also born, rarely, away from nests: one in a while near a player in its lands at
  high altitude, by day, never two within 128 blocks.

## Drake Scales

Tough and light, each of its element. For now they're a trophy and a trade (Arcanists buy them for
emeralds) and an ingredient for:
- the **Drakescale Charm** (4 scales of one element and a gold ingot): carried, its element's
  everyday harm can't hurt you (fire and burning, freezing, drowning, falling, lightning) — the
  comfort a mage of that element would have, for anyone;
- later, saddles and egg warmers (the riding step).

## Advancements, Journal

- **Dragon Slayer**: defeat a drake. **Hoard**: open a drake's hoard.
- Journal page: Drakes.

## Code

| Piece | What it does |
|---|---|
| `api/DrakeRules` | Its numbers: stats, ranges, cooldowns, the breath cone, spawning (tested) |
| `content/drake/DrakeEntity` | The drake: states (soar, hunt, swoop, breathe, rest, grounded), flight, attacks |
| `content/drake/DrakeBreath` | The cone: who it catches and each element's touch |
| `content/drake/DrakeSpawner` | Nest drakes and the rare wild ones |
| `content/drake/NestStructure`, `NestPiece` | Nests on the heights |
| `content/drake/DrakeScaleItem`, `DrakescaleCharmItem` | The scales and the charm |
| `client/DrakeModel`, `DrakeRenderer` | The wyvern: wings, neck, jaw, tail, legs; flapping, gliding, breathing, resting |
| `client/particle/DrakeBreathOptions` | The breath drawn on the client from one particle |
| `tools/gen_drakes.py` | Textures for the five drakes, scales, charm; loot |

## Testing

- `DrakeRulesTest`: the cone, cooldowns, spawn rules.
- `tools/server_tests/drakes.txt`: one of each summoned (element, level, health), its loot, a nest
  located and its hoard rolled.
- `tools/autotest/drakes.txt`: a drake circling (shots), swooping at and breathing on slowed
  husks and a player (health before and after), resting, grounded at a quarter health; a nest.
