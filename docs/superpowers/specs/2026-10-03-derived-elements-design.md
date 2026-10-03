# Derived elements: Crystal, Lightning and Radiance

Date: 2026-10-03
Status: built (milestone 10 of `2026-10-02-fantasy-adventure-roadmap.md`).

Ice was the only derived element: Water's kin, rarer to wake, with its own cluster of the tree.
Each of the other families now has one too:

| Element | Kin of | Colour | First spell | Feel |
|---|---|---|---|---|
| Ice | Water | pale blue | Icicle | slows, freezes, controls |
| **Crystal** | Earth | violet `#D08CFF` | Prism Bolt | Earth made keen: sharp, precise, hard to break |
| **Lightning** | Wind | yellow `#FFE14D` | Chain Lightning | Wind's fury: leaps from foe to foe |
| **Radiance** | Fire | pale gold `#FFF1B8` | Smite | Fire's pure light: falls from the sky, burns the undead |

`Element.family()` maps each derived element to its kin and `derived()` is `family() != this`.
The new constants come after the old ones, so saved ordinals (a tower mage's element byte, synced
creature magic) still mean what they did.

## How they hit

Every element shrugs off its own kind (×0.5). Strong hits are ×1.5; anything not listed is ×1.

| Spell element | Strong against | Resisted by |
|---|---|---|
| Crystal | Wind, Lightning | Crystal |
| Lightning | Water (it conducts) | Earth, Crystal (they ground it), Lightning |
| Radiance | Ice (like Fire) | Water (like Fire), Radiance |
| Earth | Wind, and now Lightning too | Earth |

Oppositions go by family, so Radiance stands with Fire against Water and Ice (they can't be awakened
together before level 50).

## How a player gets them

- **Awakening:** in each family the base element weighs 100 and its derived element 1, as Ice
  already did, so a newly awakened player is Crystal (or Lightning, or Radiance) about 1 time in 400.
- **A brush with lightning:** surviving a lightning strike at 3 hearts or less is a brush like
  fire or drowning, and it wakes Lightning itself, not Wind.
- **The tree:** like Ice, each has a cluster behind a gate that needs its family's Affinity 10, so a
  mage of the family can learn its spells without waking the element. A player of the derived
  element holds its start for free, and its family's region is open to them.
- No Catalysts for derived elements (as with Ice): a Catalyst wakes a family's base element.

## The spells

**Prism Bolt** (Crystal, 22 mana, 2 s). A fast bolt of violet crystal (speed 2, 2 seconds' flight):
6 damage. Whatever it strikes, it bursts into three shards fanned 25° apart that fly on, 2.5 damage
each; off a wall the shards fly back out along the bolt's reflection. Like an Earth hit, the bolt
Crystallizes a burning, wet or frozen creature: the aura goes out and leaves a shard that gives
absorption hearts to whoever walks into it. (It levels to 10, with forks and a Crystal Spire, since
`2026-10-03-prism-bolt-design.md`.)

**Chain Lightning** (Lightning, 28 mana, 3.5 s). The bolt leaps from the caster's hand to the
creature under the crosshair (up to 20 blocks), then on to the nearest creature within 6 blocks of
the last that the caster may hurt, up to 3 jumps, each a fifth weaker: 5, 4, 3.2, 2.6 damage. A wet
creature conducts it: it takes half again as much, and the bolt can jump twice more from it (never
more than 6 jumps in all). With no target the cast fails and costs nothing. It's drawn as a jagged
line of sparks, and each hit cracks with a thunderclap. (It levels to 10, with forks and a client-drawn bolt, since
`2026-10-03-chain-lightning-design.md`.)

**Smite** (Radiance, 30 mana, 4 s). Marks the ground where the caster looks (up to 24 blocks, or a
creature's feet when one is under the crosshair) with a ring of rising light. Three quarters of a
second later a pillar of light falls on it: 7 damage within 2.5 blocks, half again as much for the
undead, 3 seconds alight and 5 seconds glowing. The delay is the counterplay: a moving target can
step out. (It levels to 10, with forks and client-drawn light, since `2026-10-04-smite-design.md`.)

All three scale with Potency and their family's Affinity like every spell. Their damage types
(`elementalarcana:<element>_spell`) are in the same tags as the others (magic, bypasses armour,
witches resist it) and have their own death messages.

## Second spells (milestone 12)

Each cluster's short arm ends in a second spell (level 10). Their wisps of Magus rank cast them too.

**Prism Ward** (Crystal, 35 mana, 20 s). Crystal facets circle the caster for 6 seconds (times its
power). Every projectile that would hit them (arrows, fireballs, any spell) is turned back at
whoever shot it, a tenth faster, and is the caster's now (`PrismWards`). A crystal wisp raises one
when its foe keeps its distance.

**Thunderclap** (Lightning, 30 mana, 12 s). Everything the caster may hurt within 5 blocks takes 4
damage times its power, is thrown back and stunned for a second and a half (slowed to a crawl,
weakened), a spark arcing out to each. A wet foe takes half again as much and stays stunned twice as
long. A lightning wisp claps when its foe comes within 4 blocks.

**Sanctuary** (Radiance, 40 mana, 30 s). A circle of light 4 blocks across at the caster's feet for
8 seconds (`Sanctuaries`). Each second it mends everyone inside who isn't the caster's foe (the
caster too) by a heart times its power, and sears the caster's undead foes inside (as much radiant
damage, and 2 seconds alight). A radiance wisp raises one at its own feet once it's down to 70%
health.

## Wisps

A wisp of each, innate to its element, the same size and rank rules as the others, guarding nothing
(there are no derived shrines). Each casts its element's spell and close burst at range
(`content/mob/DerivedMobSpells`) and drops its Essence and sometimes a Wisp Mote.

- **Crystal wisps** drift on the stony and jagged peaks. Their shell is faceted amethyst.
- **Lightning wisps** ride thunderstorms: a storm stirs wisps up (the minute's chance of one goes
  from 1 in 4 to 1 in 2), and under its open sky half of them are Lightning's, whatever the biome. Their shell is faint glass with an arc crackling across
  it. (`wisps/lightning` is empty, for packs to add biomes.)
- **Radiance wisps** gather in meadows, sunflower plains and cherry groves, pale gold glass.

No other creature is Attuned to a derived element (`AttunementRules.ATTUNABLE` is the five older
elements), so derived Essence comes from wisps, and now and then from a mage tower's laboratory
or sanctum chest (weight 1 beside each older Essence's 2 or 3). Mage towers stay the five older elements
(`MageTowerStructure.TOWER_ELEMENTS`) and shrine kinds fall back to the family (`ShrineKind.of`).

## Items

- **Crystal, Lightning and Radiance Essence**, drawn like the others.
- **Foci:** an Apprentice Wand and an Adept Staff of each element from its Essence, like the others.
  The Master Staff needs a Guardian Core of the element's *family* (towers hold only the older
  elements). A focus's Affinity goes to its family.
- The Arcanist's Scroll of Unbinding trade only ever asks for Essence of the five older elements.

## The tree

Each cluster leaves the end of one of its family's arms on the diagonal: two of the family's nodes,
the gate, then the start and two arms.

| Cluster | Leaves | Arms |
|---|---|---|
| Lightning | Wind's Gale Dash, to the north-east | storm: Potency, Wind Affinity, Focus, **Static Charge** (+6 Potency, +4 Wind Affinity); spark: Focus, Wind Affinity |
| Radiance | Fire's Flame Burst, to the south-east | dawn: Vitality, Fire Affinity, Reservoir, **Inner Light** (+6 Vitality, +4 Fire Affinity); halo: Ward, Fire Affinity |
| Crystal | Earth's quake arm, to the south-west (pointing away from Ice) | facet: Ward, Earth Affinity, Focus, **Prismatic Ward** (+6 Ward, +4 Earth Affinity); geode: Potency, Earth Affinity |

The short arms are where each element's later spells will hang.

## Art

- Spell icons (`tools/gen_textures.py`): a faceted violet crystal bursting into three shards, a
  jagged bolt, a pillar of light striking the ground.
- Essence gems and wisp textures (`tools/gen_creatures.py`), added after the existing ones so their
  seeds, and so their pixels, don't change.
- Particles are vanilla: amethyst block dust, electric sparks, end rods and flashes.

## Testing

- Unit: `ElementTest` pins the families and the whole chart, `AwakeningRulesTest` the weights and
  `AttunementRulesTest` that creatures roll only the five older elements.
- Server: `tools/server_tests/derived_elements.txt`: the wisps summon, get a level, refuse another
  element and die cleanly; still zombies Attuned (by command) to each element cast Prism Bolt, Chain
  Lightning and Smite at villagers 8 blocks away.
- In game (`tools/autotest/derived_elements.txt`): each spell cast at a creature, the three wisps,
  the clusters in the tree view.
