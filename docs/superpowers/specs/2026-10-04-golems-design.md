# Elemental Golems

## Why

The wilds' foes are vanilla creatures (some Attuned) and wisps. Nothing out there is big, slow and
made of its element: the kind of creature that makes a fantasy land feel dangerous and old. Golems
are the elements given a body, standing guard over the lands where their element runs strong. They
are heavy hitters with a telegraphed slam, a fight to plan for rather than swat.

## The golems

Four golems, one per base element, sharing one blocky body: about 2.3 blocks tall, broad shoulders,
long arms that nearly reach the ground, a small sunken head and a glowing core in the chest showing
its element.

| Golem | Element | Body | Its slam |
|---|---|---|---|
| Magma Golem | Fire | blackstone and magma cracks | sets a ring of burning ground and everything in it alight; immune to fire and lava |
| Tide Golem | Water | prismarine and sea-glass | a wave that throws foes back and soaks them (Wet) |
| Gale Golem | Wind | pale quartz wrapped in wind | throws foes high into the air (Airborne) |
| Stone Golem | Earth | mossy stone | sends a Tremor along the ground at its target |

- Each is born to its element (the element chart applies: water beats a Magma Golem, and so on) and
  grows with its place like any creature (level zones). 60 health, 6 armour, slow, hard to knock
  back.
- **The slam:** within reach of its target it stops, raises both arms for three quarters of a second
  (the warning), then brings them down: 8 damage to everything it may hurt within 2.5 blocks, plus
  its element's effect. Two seconds between slams. Out of reach, it walks toward its target.
- **Territorial:** it ignores the dormant and its own element's kin, but a mage of another element
  within 12 blocks is a trespasser, and anyone who hurts it is a foe.
- **Where:** now and then near a player (a check every half minute: 3% by day, 6% by night), on
  open ground 24–40 blocks away in its element's lands (the same lands Attuned creatures lean to),
  never more than one within 64 blocks. Not on Peaceful, not with mob spawning off.
- **Drops:** 2–4 of its element's Essence, a few of its stuff (magma blocks, prismarine shards,
  quartz, mossy cobblestone) and plenty of experience.
- An advancement, **Golem Breaker**: defeat an elemental golem.

## Code

| Piece | What it does |
|---|---|
| `api/GolemRules` | Stats, the slam's timings and reach, spawn chances (tested) |
| `content/creature/GolemEntity` | The golem: territory, the slam goal, its element's slam |
| `content/creature/GolemSpawner` | Bringing golems in near players |
| `client/GolemModel`, `client/GolemRenderer` | The body, its walk and its slam; the core drawn glowing |
| `tools/gen_creatures.py` | The four golems' textures |
| data | Loot tables, the innate element tags, the advancement, lang |

## Testing

- `GolemRulesTest`: chances by day and night, the slam's numbers.
- `tools/autotest/golems.txt`: each golem summoned beside slowed husks and a mage of another element:
  the warning, the slam and its effect (health, positions, effects logged), shots of all four.
- The server suite (a server test summons each golem and checks its element and drops).
