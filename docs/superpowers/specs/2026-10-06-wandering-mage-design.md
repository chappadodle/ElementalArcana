# The Wandering Mage

## Why

A fantasy road has strangers on it. Villages have their Arcanists, but nothing comes to find the
player out in the wild, and nothing hands them a lead ("there's a crypt to the north..."). A
wandering mage, rare as the wandering trader, brings both: odd wares from far away, and rumours,
maps that mark a place worth the journey.

## The mage

- **Who**: a wandering mage in a midnight-blue robe and wide hat, silver at the band and the hem
  (a wandering trader's frame, its own clothes). Wanders, drinks an invisibility potion at night as
  the wandering trader does, and leaves after two days.
- **Coming**: each morning in the Overworld, if no wandering mage is about, one may come (a chance
  in three, by the server's `wanderingMage` rate) to an awakened player: on open ground 24 to 40
  blocks away. The player senses it: "You sense a wandering mage nearby, to the north-east."
- **Rumours** (three of these each visit, each a map with a red X, the nearest one of its kind):
  a crypt, a mage tower, a sky isle, a drake nest, a ruin. 10 to 16 emeralds and a compass (the
  map is drawn on it, as a cartographer's are).
- **Wares** (four each visit): a Cantrip Scroll, a Wishing Star, Star Fragments, Essence of
  Crystal, Lightning or Radiance, a Seeker's Compass, a Charm Pouch, a Binding Charm, Mana Draughts.
- **Buys**: Wisp Motes and Star Fragments.
- **Advancement**: "Rumours on the Road", trade with a Wandering Mage.

## Code

| Piece | What it does |
|---|---|
| `content/wanderer/WanderingMageEntity` | A wandering trader with its own trades |
| `content/wanderer/WanderingMages` | The morning's chance, where it comes, the sense of it |
| `content/wanderer/ModWanderer` | The entity type and its attributes |
| `client/WanderingMageRenderer` | The wandering trader drawn with the mage's clothes over it |
| `core/ArcanaServerConfig` | `wanderingMage` (creatures) |
| `/arcana mage` | Calls one to the player now (for testing) |
| `tools/gen_people.py` | The clothes |

## Testing

- `tools/server_tests/wandering_mage.txt`: a mage summoned, its offers listed (rumours and wares).
- `tools/autotest/wandering_mage.txt`: one called by command (the sense logged), another summoned in
  front of the player and traded with (its screen, a shot), a close look at it (a shot).
