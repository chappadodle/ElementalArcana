# Drake Riding

## Why

The drakes are the biggest thing in the sky, and every fantasy hero dreams of flying one. Their
nests already hold hoards; now they hold an egg. Hatching it, raising the hatchling and finally
riding it into the air turns the drakes from a danger into a goal, and gives a mage a way to
cross the world that's worth the work.

## Eggs

- Each nest's hoard holds one **Drake Egg** of its element (fire, frost, storm, gale); tide drakes
  don't nest, so their eggs come from tide drakes themselves, one time in ten.
- An egg is a block. Placed, it needs **warmth of its element** to hatch, judged every few seconds:
  - Fire: lava or fire within 2 blocks, or magma under it;
  - Frost: snow, ice or powder snow under it or touching it;
  - Storm: a lightning rod or copper block within 2 blocks, and twice as fast in a thunderstorm;
  - Tide: in water;
  - Gale: at a height of 120 or more, open to the sky.
- Kept warm, it stirs (it wobbles and cracks show, with sounds) and hatches after 20 minutes of
  warmth (it doesn't lose progress when the warmth goes, it only stops). The hatchling belongs to
  the nearest player when it hatches (within 16 blocks), or to no one.

## Raising

- A **hatchling** is a drake a fifth of the size: it follows its owner (like a tamed wolf, and
  teleports to them when far), sits when told (sneak and use with an empty hand), and fights
  beside them with little bites and puffs of its breath.
- It grows with time and food: a full day's play (20 minutes) to become a **juvenile** (half size;
  it can glide down after you and breathes properly), and two more to become an **adult**. Feeding
  it its element's Essence (or raw meat, half as well) speeds that up; each feeding takes a few
  minutes off and heals it.
- A grown drake keeps its wild strength (but answers only to its owner) and never attacks its
  owner's friends' pets or villagers.

## Riding

- A **Drake Saddle** (crafted: a saddle, 4 drake scales of any element and 2 gold ingots) on an
  adult drake lets its owner ride it (use it with an empty hand).
- In the air: it flies where you look; forward speeds it up, back slows it to a hover; jump climbs,
  sneak dives; the cast key (R) makes it breathe its element ahead (with the usual warning glow,
  every 8 seconds). On the ground it walks, and jumping takes off.
- It tires: a stamina bar shows over the hotbar while riding; flying spends it, landing restores
  it. Out of stamina, it glides down and must land before climbing again.
- Dismount with sneak while on the ground (or with the normal dismount key in the air, falling
  with slow fall for a moment).

## Code

| Piece | What it does |
|---|---|
| `api/DrakeRidingRules` | Growth times, warmth, stamina, speeds (tested) |
| `content/drake/DrakeEggBlock` (+BE) | Warmth, the stir, hatching |
| `content/drake/TamedDrakeEntity` | The owned drake: growth stages, following, sitting, fighting beside, riding |
| `content/drake/DrakeSaddleItem` | The saddle |
| `client/DrakeRiding` | The rider's controls and the stamina bar |
| `network/DrakeBreathPayload` | The rider asks for a breath |

## Testing

- `DrakeRidingRulesTest`: growth, warmth rules, stamina.
- `tools/server_tests/drake_eggs.txt`: eggs warmed and not, hatching (with time sped up), growth.
- `tools/autotest/drake_riding.txt`: an egg hatching, a hatchling following, a grown drake saddled,
  ridden (climb, dive, breathe), stamina running out.
