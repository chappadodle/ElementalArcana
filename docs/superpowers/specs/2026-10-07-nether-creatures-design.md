# The Ember Reaches, part 3: Creatures of the Nether

## Why

The Nether now has its forges, the Forgewarden, fire wisps, salamanders and the emberbloom, but its
creatures are still vanilla's. Two of its own, one for the soul sand valleys and one for the hot
wastes, make its biomes feel like the rest of the mod's world, and the trophies they leave are worth
carrying there (as the Creatures of the Wild's are in the Overworld).

## The Ash Wraith (soul sand valleys)

- A wraith of soul fire: the Frost Wraith's shape in ash-grey tatters, half seen, its eyes burning
  soul-blue, soul-fire flames drifting off it.
- It drifts over the soul sand valleys (one at a time, as frost wraiths come), circles its prey a
  few blocks off and casts fire spells (it's always Attuned to Fire, at the rank it was born to).
- Once its prey **burns**, it swoops in, and its touch **withers** them (Wither for four seconds,
  Slowness for two) and puts their fire out with a gust of ash.
- Hit, it flickers a few blocks away in soul fire. Water and ice tear it (double damage); fire and
  lava don't touch it.
- It leaves **Soul Ash** (one or two).

## The Cinder Hound (nether wastes, crimson forests, basalt deltas)

- A lean hound of cinders, blackstone-grey, its hide cracked with glowing seams, its eyes coals.
- It hunts in **packs of two to four**: they run their prey down and leap at it, and their bite sets
  it alight. Hurt one, and it howls: the pack within 16 blocks comes for whoever hurt it.
- Fire and lava don't touch it; water hurts it (one damage a second while it's in water or rain).
- It leaves a **Cinder Fang** now and then (a third of the time, more with Looting).

## Their trophies

- **Ashen Shroud** (four Soul Ash round a Phantom Membrane): carried, the **undead don't notice
  you** (skeletons, wither skeletons, zombies, zombified piglins...) unless you strike them first;
  bosses always do.
- **Houndstooth Charm** (three Cinder Fangs and a gold ingot on a chain): carried, you **run a
  sixth faster in the Nether**.
- Both go in the Charm Pouch.

## Also

- Both come as the Creatures of the Wild do (around a player, at the server's wild-creature rate),
  their levels the Nether's (25 to 50).
- `/arcana wild ash_wraith|cinder_hound`.
- An advancement, **Beasts of the Burning Lands**: defeat an Ash Wraith and a Cinder Hound.
- A journal page.

## Code

| Piece | What it does |
|---|---|
| `content/wild/AshWraithEntity` | The wraith: circling, casting, the withering touch, the flicker |
| `content/wild/CinderHoundEntity` | The hound: the pack, the leap, the burning bite, the howl |
| `content/wild/WildSpawner` | Two new kinds in the Nether, packs for the hounds |
| `api/NetherCreatureRules` | Their numbers (tested) |
| `client/FrostWraithModel` | Made for any wraith; `client/AshWraithRenderer` draws the Ash Wraith with it |
| `client/CinderHoundModel`, `CinderHoundRenderer` | The hound |
| `content/wild/NetherCharms` | The trophies' effects |
| `tools/gen_nether.py` | Their skins (the Ash Wraith's from gen_wild's wraith), the trophies' icons, loot, recipes, spawn lands |

## Testing

- `NetherCreatureRulesTest`.
- `tools/server_tests/wild_nether.txt`: a hound called by the test command comes with its pack
  (counted), Attuned to Fire (logged); an Ash Wraith takes a plain blow, then a drowning one, doubled
  (health logged); a hound beside an iron golem bites back and the golem burns (logged); a hound in
  water is hurt (logged).
- `tools/autotest/wild_nether.txt`: both on soul soil in the Nether (a shot); a skeleton near a
  player carrying the Ashen Shroud: no arrows; without it, arrows (health logged); the Houndstooth's
  speed in the Nether (logged).
