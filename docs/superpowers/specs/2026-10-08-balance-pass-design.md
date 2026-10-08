# Balance pass I: the story's bosses

## Why

The last stretch to 1.0 is balance. A human play-test is still the final word, but the numbers can
be measured first: how long a boss takes to fall, and how soon it would bring a player down, for a
player whose level fits the place the story sends them.

## The gauge

- **`/arcana gauge start|stop`** (`content/Gauge`): while it runs, it counts the damage a player
  deals and takes. It keeps them standing (their health is topped up after each hit) and notes
  when the damage taken first passed their max health: the moment they would have fallen. Stopping
  it reports the fight against the biggest foe they hit. Anyone can use it in their own play-tests.
- **The AutoTest `fight` step** (`fight <entity type> <seconds> <hold> <gap>`): the test player
  faces the nearest foe of that type and casts the selected spell at it again and again (press,
  hold, release, throw what's held, wait), until it falls or time runs out.
- **`tools/gen_gauge.py`** writes `tools/autotest/gauge.txt`. Each fight gives the player a build
  that fits their level:
  - stat points: 40% Potency, 30% Vitality, 20% Ward, 10% Focus;
  - their element's spell at level 1 + L/5;
  - the remaining tree points on the stat nodes nearest their start;
  - iron armour from 10, diamond from 30.

  It then summons the foe 12 blocks ahead at a set level.
- **Standing still is the worst case.** A real player dodges, kites, drinks potions and brings
  allies. So the gauge is read as a ratio: a fair boss lets a standing player survive a good share
  of the time it takes to fall.

## What it found (before)

| Boss | Its level | Player | Would fall at | Boss after |
|---|---|---|---|---|
| Revenant (crypt, zone 15) | 35 | 15 | 5 s | untouched, 67 s |
| Magister (tower, zone 15) | 35 | 15 | 9 s | 59% left, 150 s |
| Forgewarden (Nether, zone 35) | 35 | 30 | 8 s | 47% left, 45 s |
| Vulkhar (weakest Sovereign) | 50 | 35 | 1 s | 94% left |

Every story boss carried an Archmage's +20 bonus levels on top of its place. Against a player of
that place, the gap alone made it about 8× harder than an even fight (×1.045 per level each way,
plus its stats). The players who could fight it (level 35 for a level-15 crypt) can't get there:
creatures 10 levels below you give no XP. And the master and archmage gear that the Magister's
and Sovereigns' drops make asks for level 35 and 50.

## Changes

- **Story bosses stand a fixed 10 levels above their place, whatever their rank**
  (`Progression.STORY_BOSS_BONUS_LEVELS`, the `elementalarcana:story_bosses` entity tag: the
  Revenant, the Magister, the four Sovereigns, the Hollow). They keep their Archmage rank for
  their spells, their boss bar and their XP. The mod's other Archmages (rare elites, rift
  wardens) keep their +20.
  - A crypt's Revenant or a tower's Magister in a level-15 place is now level 25 (was 35).
  - The Sovereigns: level 40 and up (was 50), and their own spells' power 0.8 (was 1).
  - The Hollow: level 60 (was 70), with about 750 health (was 1020; 520 before Vitality, was 670).
- **The Magister** blinks away when hurt at most every 8 seconds (was 4): a blink after every hit
  left slow spells nothing to land on.
- **The Forgewarden** is a slow brute meant to be kited, but standing in its reach was death in
  8 seconds at its own level. Its slam now does 9 (was 12), its vents 4 (was 6), and they burn for
  3 seconds (was 5).

## What it found (after)

In a walled arena (a first run had the player knocked off a bare platform into the sea below, which
read as drowning), with the build above and its gear:

| Boss | Its level | Player | Falls in | Would fall at |
|---|---|---|---|---|
| Revenant (Radiance) | 25 | 20 | 23 s | 13.8 s |
| Revenant (Fire) | 25 | 20 | 32 s | 12.3 s |
| Revenant (Crystal, with fire-Attuned dead) | 25 | 20 | 41 s | 4.8 s |
| Magister (Earth) | 25 | 20 | 76 s | 15.0 s |
| Magister (Wind) | 25 | 20 | 63 s | 23.5 s |
| Forgewarden | 35 | 30 | 37.5 s | 10.9 s |
| Vulkhar | 40 | 35 | 58.5 s | 9.6 s (most of it burning: bring Fire Resistance) |
| Caelum | 40 | 35 | 51 s | 13.8 s |
| The Hollow | 60 | 50 | about 4½ minutes (87% at 4) | 9.7 s |

Standing still, every story boss now falls to a player of the level the story brings them there
at; moving, dodging and healing make the difference, as they should. The Hollow stays the long
fight it's meant to be (and goes faster with friends).

A Revenant's or Magister's element is random, and some are harder than others (a Crystal Revenant
whose risen dead are fire-Attuned most of all).

## Not changed

- The level-gap rule (×1.045 a level) and the zones are the progression design's core and stay.
  The bosses were the outliers.
- Ordinary Archmage creatures stay at +20: they're rare, marked by their aura, and meant to be
  something to run from.

## Also

- **The End's levels:** its central island, where the dragon waits, was level 60 like the rest, so
  a player who went for the dragon at the point vanilla Minecraft expects met it 20 to 30 levels
  up. Now the central island is 40, and the End rises 1 level every 40 blocks out, to 65 on the
  outer islands 1000 blocks away and 85 at most (was 60 + 1 per 100 blocks, to 90).
- **A crash fixed** while measuring: at Hydro Jet Lv 8 and up (the bright stream), with bloom on,
  the stream's glow drew into a buffer the bloom layer had already closed ("Not building!"). Each
  layer is now drawn in its own pass.
