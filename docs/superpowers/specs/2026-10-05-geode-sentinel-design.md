# Geode Sentinel

## Why

Crystal is the warding element: Prism Ward turns arrows back at their shooters, and Crystallize
leaves shields behind. Its signature spell, at the scale of Pyronado, Tsunami, Stormeye and
Stormcall, should be a guardian: something to put between a mage and the crowd. Nothing in the mod
draws enemies' attention yet; this does.

## The spell

- **Geode Sentinel** (Crystal; Magic level 15; 50 mana; 25 seconds' cooldown; one level). Learned
  from a new branch of the Crystal tree (two small nodes down and right of its start, then the
  spell).
- **Growing**: a sentinel of violet crystal grows out of the ground where you look (up to 16
  blocks; on the ground under that point, or at your feet if there's none within 32 blocks under
  it): a cluster of shafts two and a half blocks tall, three small shards circling its middle.
- **Taunt**: for 12 seconds, every hostile creature within 10 blocks of it that you may hurt turns
  on it, once a second, as a violet ring pulses out over the ground. Bosses pay it no mind.
- **Sturdy**: 40 health plus 20 times your power, some armour; nothing pushes it or knocks it back;
  you and your allies can't hurt it.
- **Thorns**: whatever hits it takes a third of the blow back (at least 1). The thorns are the
  sentinel's own, not its caster's, so whatever it hurts stays turned on it.
- **Shatter**: when it breaks, or its 12 seconds are up, it bursts: 6 damage times your power to
  the foes within 4 blocks, thrown back, and a Crystallize shield shard left at its foot (absorption
  hearts for whoever walks into it).
- One at a time: casting again shatters the old one.
- Sounds: amethyst growing, chiming with each pulse, breaking.

## Bosses

The mod's bosses (the four Sovereigns, the Hollow, the Magisters, the Revenants, the wild drakes)
join the common `c:bosses` entity tag. Bubble Prison already refuses that tag ("bosses can't be
trapped"), but none of the mod's own bosses were in it; the Sentinel uses it too, and other mods
will see them as bosses.

## Code

| Piece | What it does |
|---|---|
| `content/spell/GeodeSentinelSpell` | The spell: where it grows |
| `content/spell/GeodeSentinel` | The sentinel (a living entity, never saved): taunt, thorns, shatter |
| `client/GeodeSentinelRenderer` | The crystal cluster (Crystal Spire's shafts), its circling shards, the flash when it's hit |
| `data/c/tags/entity_type/bosses.json` | The mod's bosses |
| `tools/gen_skill_tree.py`, `tools/gen_textures.py` | The new branch; the icon |

## Testing

- `tools/server_tests/bosses.txt`: a Sovereign, a Magister and a zombie: `/arcana bubble` traps
  only the zombie.
- `tools/autotest/geode_sentinel.txt`: three husks closing on a Crystal mage; the Sentinel grown
  between them (shots); the husks turn on it (the mage's health stays full, the Sentinel's falls,
  the husks are hurt by its thorns, all logged); it shatters (a shot), leaving its shard.
