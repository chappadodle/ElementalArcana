# Trophies of the Wild

## Why

The Creatures of the Wild each have a trick: the treant rests in the sun and holds you fast with
roots, the wraith flickers out of reach, the salamander runs on lava. They leave Heartwood, Wraith
Silk and Salamander Scales "for later". Later is now: each material makes a charm that teaches its
creature's trick, so hunting them is worth it, and a mage who has met all three carries the wild
with them. (Like the Drakescale Charms, they work from anywhere in your inventory.)

## The charms

### Heartwood Talisman (Earth)

Two Heartwood, an Earth Essence and a string. Carried:
- **The forest's rest**: standing on grass, dirt, moss or podzol by day under the open sky, when
  nothing has hurt you for 5 seconds, you heal 1 health every 3 seconds (green motes rise).
- **Rootbound no more**: Rooted can't take hold of you.

### Wraithsilk Veil (Ice)

Two Wraith Silk, an Ice Essence and a phantom membrane. Carried:
- **Flicker**: when a creature or a projectile hurts you, the hit lands at half strength and you
  flicker 4 to 6 blocks away from it (a puff of frost, the wraith's sound), onto safe ground. Then
  the veil rests for 12 seconds (the item's cooldown shows on it).

### Salamander Charm (Fire)

Two Salamander Scales, a Fire Essence and a gold nugget. Carried:
- **Lava walking**: as you walk, still lava within 2 blocks under your feet cools to a **Cooling
  Crust** (like Frost Walker on water). The crust cracks as it ages and melts back to lava after a
  few seconds; under the charm's bearer it stays whole.
- **Hot feet**: magma blocks and the crust don't burn your feet.
- The crust is hot to anyone else (it burns like magma unless they sneak).

## Code

| Piece | What it does |
|---|---|
| `api/WildTrophyRules` | Heal interval and calm time, flicker distance and cooldown, crust ageing (tested) |
| `content/wild/WildCharmItem` | The three charms (tooltips) |
| `content/wild/WildCharms` | Their effects: resting, Rooted refused, flicker, lava walking, hot feet |
| `content/wild/LavaCrustBlock` | The Cooling Crust: ages and melts back to lava; hot underfoot |
| `tools/gen_wild.py` | Icons, the crust's textures and models, recipes |

## Testing

- `WildTrophyRulesTest`.
- `tools/server_tests/wild_trophies.txt`: a crust placed on lava melts back in a few seconds;
  the recipes are known.
- `tools/autotest/wild_trophies.txt`: walking across a lava pool with the Salamander Charm
  (shots, crust behind melting), a zombie's hit with the veil (positions before and after), and
  healing on grass at noon with the talisman (health logged), Rooted refused.
