# Creatures of the Wild

## Why

Golems guard the elements' lands, drakes hold the heights, wisps drift everywhere; but walking
through a forest, a snowfield or a badland, you mostly meet vanilla's zombies and skeletons. A
fantasy world's wilds have their own creatures, each of its place, with a trick worth learning.
Three, one for each kind of land that has none of its own yet:

## Thornwood Treant (Earth, forests)

- In old forests (dark forests, old-growth taiga, forests, birch forests), now and then a tree
  isn't one: a **Thornwood Treant** stands among the trees, still, its crown of leaves and bark body
  passing for a tree, of its forest's wood (oak, birch, spruce or dark oak). (It doesn't move,
  makes no sound and casts no shadow while it waits; its arms are raised into its crown as boughs.)
- Come within 6 blocks (not as its kin, the Earth family's mages, unless you hurt it) or hit it,
  and it wakes with a groan, uproots and walks after you: slow, 3 blocks tall, 80 health, heavy
  armour, hard to shove.
- **Grasp:** up close it sweeps both branch-arms (8 damage, a shove).
- **Rooting:** every so often it stamps: roots burst up under its target (a ring of spikes from
  the ground), holding them in place for 2 seconds (no moving, no jumping) and pricking them.
- Fire burns it badly (×1.5 from fire, and from Fire-family spells); it heals slowly in sunlight on
  grass or dirt.
- When it loses its target it walks back and roots itself as a tree again.
- Drops: Earth Essence, logs and saplings of its wood, sticks; now and then a **Heartwood**
  (a material for later).

## Frost Wraith (Ice, snowy lands at night)

- Over snowy plains, slopes, groves and frozen rivers at night: a hooded spirit of frost in tattered
  robes, its lower body trailing into mist, eyes cold blue. It drifts a few blocks off the ground
  and through the snowfall, half seen (translucent).
- It circles its prey and **chills**: it's always Attuned to Ice (an Adept, or more on the usual
  roll), so it casts its rank's ice spells, the Icicle first (a frosting hit); when you're frosted
  through, it swoops in and its touch freezes you (Frozen, briefly). It flickers away (a short
  blink) when hit.
- Daylight burns it away: at dawn wraiths fade.
- 30 health, fast (it outflies a walking player), flies; immune to cold, freezing and falling,
  weak to fire and Radiance (×1.5).
- Drops: Ice Essence, snowballs, and now and then **Wraith Silk** (a material for later).

## Ember Salamander (Fire, badlands, deserts, the Nether)

- A long, low lizard of glowing scales, basking in badlands and deserts by day and in the Nether's
  wastes and basalt deltas. Neutral until provoked or approached within 4 blocks.
- Quick on its feet; it **spits** fire (its throat glows, then a small fireball that sets the ground
  and you alight) and bites (6 damage, burning). It runs on lava as on stone (as a strider does),
  and heals there.
- Water hurts it: the element chart (Water spells hit it hard), and standing in water or rain.
- 40 health; immune to fire and lava.
- Drops: Fire Essence, magma cream, and now and then a **Salamander Scale** (a material for
  later).

## All three

- Born to their elements (the element chart applies), at their place's level, Attuned as usual
  (they can come as Adepts, Magi or Archmages like any creature).
- Spawn like monsters of their places (the treant and salamander also by day), with spawn eggs:
  every 20 seconds each player rolls for each (treant 6%, wraith 12% at night, salamander 8%), and
  one is born 20 to 40 blocks off in its land, never two of a kind within 48 blocks.
- An advancement, **Into the Wild**: defeat one of each.
- A Journal page.

## Code

| Piece | What it does |
|---|---|
| `api/WildRules` | Their numbers: ranges, cooldowns, durations (tested) |
| `content/wild/TreantEntity`, `TreantModel/Renderer` | The treant: disguise, waking, grasp, rooting, returning |
| `content/wild/FrostWraithEntity`, model/renderer | The wraith: flight, chill, freezing touch, blink, dawn |
| `content/wild/SalamanderEntity`, model/renderer | The salamander: bask, spit, bite, lava |
| `content/wild/ModWild`, `RootedEffect` | Registration; Rooted |
| `content/wild/WildSpawner` | Spawning (per player, by biome tag), and `/arcana wild` for tests |
| `tools/gen_wild.py` | Textures, loot, tags (the advancement is in `tools/gen_advancements.py`) |

## Testing

- `WildRulesTest`.
- `tools/server_tests/wild.txt`: each summoned (element, health), their drops.
- `tools/autotest/wild.txt`: a treant standing as a tree and waking, rooting a player; a wraith
  chilling and freezing; a salamander spitting fire and swimming in lava.
