# Earth (progression step 4a): design

Date: 2026-10-01
Status: approved in conversation. It's step 4 of
`docs/superpowers/specs/2026-10-01-progression-system-design.md`, built in slices: **4a** (this spec)
adds Earth itself, working but with plain visuals and vanilla sounds. The visual and audio passes
follow spell by spell, in the same four steps as the other spells, and the **Crystal** cluster
(Earth's derived element) comes in its own slice after that. All numbers are drafts to tune.

## Earth in the game

Earth is the fourth base element: the heavy, sturdy one. Fire bursts, Wind is fast, Water controls,
Ice freezes; Earth is defense, weight and holding ground.

- A brown element and school (`earth`), Elemental Essence, and a Catalyst, with a recipe like the
  others (8 Earth Essence and a diamond, shapeless). Earth gets along with every element.
- **Awakening:** Earth joins the background roll with an equal share. Its **brush** is surviving a
  falling block, stalagmite or anvil, or suffocation in a wall, at 3 hearts or less.
- **Damage chart:** Earth hits Wind creatures x1.5, and Water hits Earth creatures x1.5. Everything
  else is unchanged, and Earth resists itself (x0.5) like every element.
- **Everyday nature:** a player with Earth takes 25% less damage from falling blocks, stalagmites and
  suffocation (like the other elements' own damage).

## Crystallize

An Earth hit on a burning, wet or frozen creature:
- puts the effect out (it uses up the aura), and
- leaves a **shard** on the ground where the creature stood, tinted the aura's colour.

Whoever walks into the shard (a player within 1.2 blocks) takes it and gets **absorption hearts**:
3 hearts for 10 seconds. A shard fades after 10 seconds. A creature leaves one shard per 5 seconds,
so hitting it again doesn't pile them up. Shards are tracked on the server (no entity) and drawn with
particles.

## Spells

**Boulder** (starter, levels 1 to 10): press R to conjure a heavy rock, click to throw it, like
Fireball and Icicle. It arcs, bounces once and hits hard with a lot of knockback. Mana 18, cooldown
3.5 s at Lv 1.

| Lv | Name | Does |
|---|---|---|
| 1 | Boulder | A thrown rock: arcs, knocks back, cracks on impact |
| 2 | Heavy Stone | More damage, and it bounces once |
| 3 | Quick Hands | Conjures faster, and a quick tap-throw hits harder |
| 4 | Twin Boulders | Two orbit you and fly together |
| 5 | Crossroads | **Landslide**: it shatters into 5 rolling rocks that spread and keep hitting. **Bedrock**: slower and bigger, +60% damage and a 1 s stun |
| 6 | Hardened | +25% damage; Crystallize shards last longer (15 s) |
| 7 | Rolling Stone | After its bounce it rolls on along the ground, hitting what's in the way |
| 8 | Triad | Up to three orbit you |
| 9 | Tectonic | Two hits from one volley stun a creature for 1.5 s |
| 10 | Capstone | **Mountainfall**: hold with a full set and they fuse into one huge boulder that falls on the target area. **Avalanche**: where a thrown boulder lands, rocks rain on the area for 5 s (built this way instead of "hold to rain": it needs no new hold mechanic) |

**Stone Skin** (levels 1 to 10): orbiting stones absorb damage, like Frost Shield. Mana 25, cooldown
20 s.

| Lv | Name | Does |
|---|---|---|
| 1 | Stone Skin | Absorbs 8 damage for 10 s; when it breaks, the fragments knock nearby enemies back |
| 2 | Thicker Stone | Absorbs 14, lasts 12 s |
| 3 | Thorns | Anything that hits you in melee is hurt back |
| 4 | Brace | Raise it just before a hit lands: a perfect block with no knockback that returns the damage |
| 5 | Crossroads | **Bastion**: a thinner shield, but you gain armor and can't be knocked back. **Crystal Carapace**: picking up Crystallize shards adds to your shield, and your thorns Crystallize attackers |
| 6 | Shared Stone | Allies within 5 blocks get half a shield |
| 7 | Rockburst | When it breaks, the fragments cut everything within 4 blocks |
| 8 | Bedrock Skin | It slowly regrows, and you take no fall or suffocation damage |
| 9 | Stonefist | An enemy that hits it three times is rooted for 2 s |
| 10 | Capstone | **Fortress**: a dome that blocks projectiles and pushes enemies out. **Titan**: for 15 s (the shield's duration) you gain +4 hearts, +50% melee damage and no knockback; if the shield breaks first, Titan ends with it |

**Tremor** (one level): a shockwave runs along the ground for 10 blocks, a block a tick, throwing
creatures up and slowing them for 3 s, with a small hit of damage; jump it to dodge. It sets off
Crystallize on anything burning, wet or frozen. Mana 30, cooldown 6 s. (Made to travel, with its
rock spikes, in the Tremor pass: `2026-10-03-tremor-design.md`.)

## Creatures

- **Innate Earth:** iron golems, silverfish and armadillos.
- **Attuned Earth** creatures lean toward mountains and caves (a biome tag for mountains, cave
  biomes and dripstone caves).
- **Their spells:** an Adept throws Boulders, a Magus adds Tremor, and an Archmage adds a **Stone
  Ward** that absorbs damage (like Ice's ward).

## The skill tree

Earth sits in the empty west of the tree. Its start gives Boulder (and the Boulder path of upgrade
nodes and forks hangs off it). Stone Skin and Tremor are spell nodes a few steps along its arms, with
Earth Affinity small nodes (and a few Reservoir, Ward and Vitality ones) along the way. The Stone
Skin path hangs off its spell node. The Crystal cluster comes with the Crystal slice.

## Looks and sounds in this slice

Working but plain: a stone-textured Boulder and stone fragments for Stone Skin, vanilla block-crumble
and dust particles, and vanilla stone and gravel sounds. The full visual and audio passes follow.

## Code

| Unit | Job |
|---|---|
| `api/Element` | `EARTH`; the chart; opposition unchanged |
| `content/ModSchools`, `ModItems`, `ModSpells` | The school, Essence, Catalyst and spells |
| `content/spell/BoulderSpell` | Boulder: conjured projectile, levels and branches |
| `content/spell/StoneSkinSpell` | Stone Skin: a shield spell with levels and branches |
| `content/spell/TremorSpell` | Tremor |
| `content/Crystallize` and `ElementalReactions` | The reaction and the shards |
| `content/mob/EarthMobSpells`, `StoneWards` | Attuned Earth creatures' spells |
| `content/AwakeningEvents` | The Earth brush |
| data | Innate and biome tags, the Earth region of the skill tree (`earth.json`, from `tools/gen_skill_tree.py`), the Catalyst recipe |
| assets | Earth Essence and Catalyst textures and models, Boulder and stone-fragment models, lang |

## Testing

- Unit tests for the chart (Earth against Wind, Water against Earth, Earth against itself).
- A server test (`mobspells_earth.txt`) with an Attuned Earth creature casting, and a skill tree
  check that the Earth region loads.
- A play-test of the spells, Crystallize and the brush.
