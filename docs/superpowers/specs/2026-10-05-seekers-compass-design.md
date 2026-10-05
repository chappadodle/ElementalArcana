# Seeker's Compass

## Why

The world is full of places worth finding now (shrines, ruins, crypts, mage towers, sanctums, drake
nests, sky isles), but most are rare, and finding one is luck. A fantasy adventure gives you a way
to go looking: a compass whose needle swings toward the nearest place of the kind you choose turns
"wander until something turns up" into a journey with a heading.

## The compass

- **Seeker's Compass**: a compass, two amethyst shards and any Essence (shapeless). One to a stack.
- **What it seeks**: sneak and use it to choose: Shrines, Ruins, Crypts, Mage Towers, Sanctums,
  Drake Nests, Sky Isles (in that order, round again).
- **Seeking**: use it, and the needle swings toward the nearest place of that kind (as far as
  `/locate` would look, thousands of blocks for the rarest), with a chime and a word on which way
  and how far ("The needle swings north-east: a crypt, about 640 blocks away."). Nothing in reach:
  "The needle finds no crypt within reach." Only in the Overworld; elsewhere the needle spins. A
  few seconds between seeks.
- **The needle** points the way from then on, like a lodestone compass's (it spins in another
  dimension, or when there's nothing to point to).
- **Arriving**: within 32 blocks of what it found, the compass settles: a chime, "You have found
  the crypt.", and the needle spins again until you seek once more.
- **Its tooltip**: what it seeks, and, while it points somewhere, how far that is.
- **Art**: a brass-rimmed face set with an amethyst, a violet needle glowing at its tip (32 frames,
  like a compass's).
- **Advancement**: "Where the Needle Points", find a place with a Seeker's Compass.

## Code

| Piece | What it does |
|---|---|
| `api/SeekerRules` | The kinds and their order, the arrival distance, the seek cooldown (tested) |
| `content/seeker/ModSeeker` | The item, its components (what it seeks, where it points), the structure tags |
| `content/seeker/SeekersCompassItem` | Choosing, seeking (the server's structure search), arriving, the tooltip |
| `client/SeekersCompassClient` | The needle (vanilla's compass angle, pointed at the found place) |
| `tools/gen_seeker.py` | The 32 frames and the model, the recipe, a structure tag per kind |

## Testing

- `SeekerRulesTest`: the kinds cycle, arrival is by distance on the ground (not height).
- `tools/server_tests/seekers_compass.txt`: each kind's structure tag located from spawn.
- `tools/autotest/seekers_compass.txt`: choosing round to Sky Isles (the compass logged), seeking
  (the found place logged, shots of the needle facing four ways), going near it (the arrival and its
  advancement logged, the target gone).
