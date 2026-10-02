# Creatures I: elemental wisps

Date: 2026-10-02
Status: built (milestone 5 of `2026-10-02-fantasy-adventure-roadmap.md`).

The first creatures that are magic through and through. Vanilla mobs can be Attuned now and then,
but a wisp is nothing but its element: a small floating orb of it, drawn to mana. They make the wild
feel alive, guard the shrines, and are a steady source of Essence.

## The five wisps

One per element: Fire, Water, Ice, Wind and Earth. Each is a glowing core in a slowly turning shell
of its element, with little motes circling it, and it trails its element as it drifts (embers,
drips, snowflakes, wisps of wind, dust). The Earth wisp's shell is stone with light showing through
its cracks.

- Small (0.6 blocks) and quick in the air. They hover 1.5 to 3 blocks above the ground or water,
  never fall and never take fall damage.
- 12 health at level 1. Like every creature they get a level from where they are
  (`CreatureLevels`) and put their points into Vitality, Ward and Potency.
- Innate to their element (the `innate/<element>` entity tags), so the element chart applies:
  their own element does half damage to them, their weakness 1.5 times.
- Fire wisps can't burn, and water and rain hurt them, as they hurt blazes. Ice wisps can't freeze.
  None of them drown or suffocate.
- They pop when they die: a burst of their element and a crystal chime.

## Ranks

Every wisp is Attuned to its own element, at least an Adept, so it casts (`CastMobSpellGoal`) and
is worth Attuned XP (×3). A few are greater, on the usual Attuned roll (`AttunementRules#rollRank`,
rarer near world spawn):

| Wisp | Odds | Size | |
|---|---|---|---|
| Adept | the rest | 1× | one spell |
| Magus | 1% (up to 3% far out) | 1.35× | two spells, 8 levels above the zone |
| Archmage | 0.1% (up to 0.3%) | 1.75× | three spells, 20 levels above, a boss bar |

## Who they fight

Wisps are drawn to mana, and they see other mages as rivals:

- A player whose magic still sleeps is no threat: wisps drift around them, curious.
- A mage who holds the wisp's element family is kin: wisps leave them be.
- Any other mage is a rival: wisps attack on sight.
- Anyone who hurts a wisp is fought back, kin or not, and nearby wisps join in.

In a fight a wisp keeps 5 to 8 blocks away, circling the target a little above its head and casting
its element's spells (the Attuned spell lists: Fire throws fireballs, Water sprays a jet, Ice
throws icicles, Wind slashes with wind blades, Earth throws boulders). Before each cast its core
flares for half a second, with a warning sound. Without a target it wanders near where it spawned
(or the shrine it guards).

## Where they are

- **In the wild:** a custom spawner (like phantoms and patrols, not the mob cap) checks around each
  player once a minute. With a 1 in 4 chance, a wisp appears 16 to 32 blocks away, in the open air
  above the ground. Its element comes from the biome, and there are never more than 2 wisps within
  48 blocks. Biomes come from the `wisps/<element>` biome tags: the shrine biomes, plus oceans for
  Water and the Nether for Fire. About one wisp every four minutes in wisp country, by day or
  night. They despawn like monsters when no one is near.
- **At shrines:** a shrine is guarded by two wisps of its element. They appear when someone (not a
  spectator) comes within 32 blocks, and every 3 to 5 minutes after that, while someone is still
  near, the shrine tops them up to two again. Guardians stay within 8 blocks of the shrine. Kin can
  use their own element's shrines in peace; other shrines have to be won.
- Spawn eggs for all five are in the creative tab, and `/arcana wisp spawn` calls one near you now
  (operators; for testing).

## Rewards

Kill XP as an Attuned creature (Adept ×3, Magus ×5, Archmage ×12), and Essence of the wisp's
element: the Attuned drops (an Adept half the time, a Magus 1 to 2, an Archmage 3 to 5, more with
Insight), plus one more Essence from the wisp itself (its loot table, +1 per level of Looting).

## Testing

- `tools/server_tests/wisps.txt`: all five summon, level, attune only to their own element, drop
  their loot and die cleanly.
- `tools/autotest/wisps.txt`: the five in a row, an Archmage, a fight (a Wind mage against a Fire
  wisp), and a shrine's guardians.
- `tools/autotest/guardians.txt`: guardians appear at a shrine no one has been near once a player
  (not a spectator) comes close, and a visited shrine keeps its timer.
