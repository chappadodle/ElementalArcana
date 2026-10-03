# Prism Bolt, Lv 1–10

## Why

Like Chain Lightning before it, Crystal's first spell has one level while every base element's
levels to 10 with two forks. This gives it the same depth, around Crystal's character: Earth made
keen, light split and turned, shards that find their mark, and crystal that shields its mage.

## The ladder

Lv 1 is the spell as it is: a fast bolt of violet crystal (speed 2, 2 seconds' flight), 6 damage;
whatever it strikes, it bursts into three shards fanned 25° apart that fly on (2.5 damage each), and
off a wall the shards fly back out along the bolt's reflection. It Crystallizes a burning, wet or
frozen creature. 22 mana, 2 s.

| Lv | Tier | What changes |
|---|---|---|
| 1 | Prism Bolt | As above |
| 2 | Keen Edge | Bolt and shards hit a fifth harder |
| 3 | Refraction | Five shards, 18° apart |
| 4 | Piercing Light | The bolt bursts through the first creature it strikes and flies on, bursting again on what it hits next |
| 5 | Fork | **Prismatic Lance:** the bolt flies half again as fast and pierces everything in its way, bursting only where it stops (bursting on every creature it passed made long lines take damage that grew with their length squared). **Geode Burst:** no piercing, but its burst throws ten shards all around, and they hit half again as hard |
| 6 | Crystal Shell | Every bolt that strikes a creature hardens a shell on you: one absorption heart for 15 s, up to four |
| 7 | Twin Prisms | Each cast throws two bolts side by side |
| 8 | Resonance | A shard that strikes a creature its bolt already struck hits half again as hard |
| 9 | Brilliance | Shards split once more when they strike: two small shards, half the damage, that split no further |
| 10 | Capstone | **Crystal Spire:** where a bolt first strikes, a spire of crystal grows out of the ground and stands for 6 s, throwing a shard at the nearest creature within 10 blocks every half second (one spire at a time). **Prism Barrage:** hold cast to keep firing: a bolt every 6 ticks for up to 2 s (one bolt, at 40%), 6 mana each after the first |

## Looks and sounds

The bolt and its shards keep their look (glowing violet, amethyst crumbs and amethyst sounds). The
Crystal Spire is a cluster of violet crystal shafts (the shrines' crystal texture, tinted), rising out
of the ground over half a second, glinting; it fires with the amethyst chime and shatters into
amethyst crumbs when it ends. A shell hardening on you chimes once.

## Code

| Piece | What it does |
|---|---|
| `api/PrismBoltRules` | The numbers by level and fork (tested) |
| `content/spell/PrismBoltSpell` | Levels, forks, bolts, bursts, shells, the barrage hold (a bolt carries its level, forks, cast and damage share in its data; shards copy them) |
| `content/spell/CrystalSpire` and `client/CrystalSpireRenderer` | The spire |
| Tree | A `crystal_prism_bolt` path off Crystal's start |

## Testing

- `PrismBoltRulesTest`: the numbers by level and fork.
- `tools/autotest/prism_bolt.txt`: rows and blocks of husks; Lv 1, Lv 4's pierce, both forks, Twin
  Prisms, the shell (absorption logged), both capstones, with health logged.
- The server suite (`derived_elements` and the mob tests cast the Lv 1 bolt).
