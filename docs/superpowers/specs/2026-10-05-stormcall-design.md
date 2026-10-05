# Stormcall

## Why

Fire, Water, Ice, Wind and Earth each have three or four spells; the elements they make together
(Crystal, Lightning, Radiance) have two. Each deserves a signature spell of its own, at the scale of
Pyronado, Tsunami and Stormeye. Lightning's is the storm itself.

## The spell

- **Stormcall** (Lightning; Magic level 15; 60 mana; 30 seconds' cooldown; one level, like
  Pyronado). Learned from a new branch of the Lightning tree (two small nodes up and right of its
  start, then the spell).
- **The cloud**: a storm cloud gathers seven blocks over the caster (lower under a roof, never in
  the rock) and follows them for 10 seconds. It's built like Minecraft's own clouds, of flat-shaded
  boxes, but storm-grey and heaped up, and it flashes white as each bolt falls.
- **The rain**: it rains under the cloud (streaks like Minecraft's rain, and drops that splash).
  Whatever the caster may hurt that stands under it (within 3.5 blocks across) is soaked (Wet) and
  stops burning.
- **The strikes**: every three quarters of a second, a bolt leaps from the cloud to a foe within 12
  blocks of the caster: the one struck least lately (the bolts spread among the foes), a soaked one
  first, then the nearest. 5 damage times the caster's power, half again as much if Wet (and slowed
  for a second), 2 to whatever else stands within a block and a half. Nothing in reach: the cloud
  only rumbles.
- **Sounds**: vanilla thunder and lightning-impact sounds, softer than a real storm's; rain sounds
  under the cloud.
- The caster fights on while it lasts (it isn't channelled). Cast again: the old cloud gives way.

## Code

| Piece | What it does |
|---|---|
| `content/spell/StormcallSpell` | The spell |
| `content/spell/Stormcalls` | The storms: soaking and striking (server, never saved) |
| `content/StormcloudOptions`, `client/particle/StormcloudParticle` | The cloud and its rain, drawn on each client from one particle |
| `tools/gen_skill_tree.py` | The new branch (`lightning.json`) |
| `tools/gen_textures.py` | The spell's icon |

## Testing

- `tools/autotest/stormcall.txt`: Stormcall cast among three husks of the caster's level, one beside
  them, one 8 blocks off, one soaked 11 blocks off (shots of the cloud from below and behind, a
  strike; the husks' health logged over the 10 seconds: the soaked one struck first and hardest, the
  bolts spread among all three; the cloud gone after).
