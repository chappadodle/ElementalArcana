# Creatures of the Wild II

## Why

The forests, the snows and the badlands have their creatures now. The mountains' wind, the caves
under them and the swamps are still empty but for vanilla's mobs. Three more, each with its own
trick, so the world past the plains keeps surprising you.

## Gale Harpy (Wind; windswept hills, stony and jagged peaks, meadows)

- A winged hunter of the heights: grey-blue feathers, wings for arms, a bird's legs with talons,
  a fierce face under a crest. It flies, circling high above its land.
- **Snatch**: it dives at its prey (a screech as it comes), grabs them in its talons, climbs about
  6 blocks with them, and lets go (the fall does the rest). Every 8 seconds at most; hitting it
  while it carries you makes it drop you sooner.
- **Gust**: from the air it beats its wings at you: a short push away (and Airborne when it lands
  on a creature already in the air).
- 28 health, quick flier; Wind (earth grounds it: the chart). Spawns by day in its lands, up high.
- Drops: Wind Essence, feathers, now and then a **Harpy Plume**.

## Crystal Crawler (Crystal; caves, amethyst geodes, lush and dripstone caves)

- A six-legged crawler of the deep, its back a ridge of violet crystals that glow in the dark. It
  climbs walls (like a spider).
- **Shard volley**: from a distance it hunches and flings three crystal shards (they fly straight,
  a few hearts each, and shatter).
- When it dies its crystals burst: shards fly out in a ring (they hurt anyone close).
- 24 health, armour 6; Crystal (wind and lightning split on it: the chart). Spawns underground,
  below sea level, in the dark.
- Drops: Crystal Essence, amethyst shards, now and then a **Prism Core**.

## Bog Lurker (Water; swamps and mangrove swamps)

- A broad, flat swamp beast, half frog half crocodile, mottled green and brown. It lies in the
  water with only its eyes and back above the surface, still.
- **Lunge**: someone at the water's edge within 5 blocks: it bursts out, bites (6 damage) and holds
  on, dragging its prey back into the water for 3 seconds (Rooted on land, pulled under in water);
  hitting it hard (8 or more at once) breaks its hold.
- **Mud spit**: from the water it spits mud: slowness and blindness for a moment.
- 36 health; Water (fire is quenched by it, earth wears it down: the chart). Swims, breathes
  under water; slow on land. Spawns in swamps, day or night, in the water.
- Drops: Water Essence, slimeballs, clay, now and then a **Bog Pearl**.

## All three

- Born to their elements, Attuned as usual, with spawn eggs; they spawn per player like the first
  three (WildSpawner), never two of a kind within 48 blocks.
- The **Into the Wild** challenge stays the first three; a new one, **Wild Things**, for these.
- Their materials are for later (Trophies II).

## Code

| Piece | What it does |
|---|---|
| `api/WildRules` | Their numbers (tested) |
| `content/wild/HarpyEntity` (+model, renderer) | Flight, snatch, gust |
| `content/wild/CrystalCrawlerEntity` (+model, renderer, `CrystalShardEntity`) | Climbing, the shard volley, the burst |
| `content/wild/BogLurkerEntity` (+model, renderer) | Lying in wait, lunge and drag, mud spit |
| `content/wild/WildSpawner` | Three more kinds: up high, underground, in the water |
| `tools/gen_wild.py` | Their textures, loot, tags |

## Testing

- `WildRulesTest` (more cases).
- `tools/server_tests/wild.txt`: the three summoned (element, health), their drops, the spawner
  bringing each in.
- `tools/autotest/wild_2.txt`: a harpy snatching a player and dropping them (heights logged), a
  crawler's volley and its burst, a lurker lunging from the water and dragging a player in.
