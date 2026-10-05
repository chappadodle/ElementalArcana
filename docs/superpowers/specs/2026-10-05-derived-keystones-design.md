# Keystones of the Derived Elements

## Why

Every base element's arm of the skill tree ends in a keystone, a rule of magic changed for better
and for worse (Glass Cannon, Wellspring, Gale Step, Mountain Heart, Winter's Grasp, and the core's
Blood Magic). The derived elements' arms end in a notable and stop. Each gets its keystone now.

## The keystones

Each sits after its element's notable (Prismatic Ward, Static Charge, Inner Light), at the end of
that arm.

- **Refraction** (Crystal): every third projectile that would hit you turns back at its shooter, as
  a Prism Ward turns them; melee blows hurt you 20% more.
- **Conductor** (Lightning): every time one of your spells hurts something, a spark arcs on to the
  nearest other foe within 5 blocks for 35% of the damage (an arc doesn't arc again); you have 20%
  less max mana.
- **Sunborn** (Radiance): in bright light (light level 12 or more, or a Dawnbreak's light) your
  spells hit 25% harder and you mend 1 health every 3 seconds; in dim light (below 8) they hit 20%
  weaker.

## Code

| Piece | What it does |
|---|---|
| `api/Keystones` | The three, their numbers, Sunborn's light rule, Refraction's count (tested) |
| `content/KeystoneEvents` | Refraction (projectiles, melee), Conductor's arcs, Sunborn's mending and power |
| `core/CastingService`, `core/Conjuring` | Sunborn's power, with mana weather's |
| `content/spell/PrismWards` | Its turning-back, shared with Refraction |
| `content/spell/Dawnbreaks` | Whether a place is in a Dawnbreak's light |
| `tools/gen_skill_tree.py` | The three nodes |

## Testing

- `NotablesAndKeystonesTest`: Conductor's mana, Sunborn by light, Refraction's every third.
- `tools/autotest/derived_keystones.txt`: Refraction against a skeleton's arrows (its health logged:
  its own arrows come back); Conductor, a Fireball at one of two husks standing together (both
  hurt); Sunborn, the same Fireball at noon and at midnight (the damage logged: more at noon).
