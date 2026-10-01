# Progression system: design

Date: 2026-10-01
Status: foundation agreed in conversation. All numbers are drafts to tune later. The user has
more ideas that will be added.

## Principles

1. **Every living creature has some magic.** Mana is universal, down to the tiniest bit. Only
   some creatures *know how* to use it. This holds for every mob and for the player, until an
   element awakens.
2. **The magic chooses you.** You don't pick an element at spawn. It is random, nudged by what
   you live through. One element is normal, two is rare, more is extremely rare, and it gets
   easier the higher your level.
3. **Growth is exponential, not linear.** Power per level compounds, so enemies have to follow
   the same curve. An Archmage must be lethal.
4. **Everyone plays by the same rules.** Players and creatures share one level system and one set
   of stat formulas.
5. **Stats interact.** No stat is useless, and each one has a cost.

## Elements

- Elements are **data**, not a fixed list. Each has a name, a rarity and links to related elements.
- **Base elements** (the usual starting set): Fire, Wind, Water, Earth. More can be added later, each
  with its own tree region and entrance.
- **Derived elements** are usually reached from a related element, as clusters on the outer edge of
  its region of the tree: Ice from Water, Crystal from Earth, others from Wind.
- **Relations run both ways, and nothing is linear.** A link between two elements (Water and Ice)
  isn't a one-way parent and child. Very rarely, a derived element presents itself as your *first*
  element: with no element, something happens and you wake with Ice instead of Water. From Ice you
  can then reach Water, the element it normally comes from. Whichever one you start with, the other
  is the one you unlock.
- **Affinity is per element family** (a family is a group of linked elements, such as Water and Ice),
  so a family is raised as a whole whichever element you hold first.
- **Opposed pairs** keep today's rule: opposed elements (Fire vs Water, and Fire vs Ice) can't
  coexist until **level 50**. Opposition belongs to the whole family.

## Levels and stats

- Everyone has a level. **The cap is 100.**
- Each level gives **one stat point** (spent freely, Elden Ring style) and **one tree point**
  (spent on the skill tree, Path of Exile style).
- **Seven stats:**

| Stat | Effect | Interacts with |
|---|---|---|
| Reservoir | Mana pool and regen | Conjured spells reserve mana (1 per second each) |
| Potency | Spell damage | Multiplies with the matching Affinity |
| Affinity (per element family) | The family's damage, reaction strength, and the requirement for its tree nodes | Linked elements (Water and Ice) share one |
| Focus | Cooldowns, charge speed, conjure limit | Trades off against Potency |
| Ward | Resistance to each element | Meets the attacker's Affinity |
| Vitality | Health | Grows slower than mana |
| Insight | Reaction strength (Vaporize, Freeze, Swirl) and Essence finds | Rewards combos |

- **Scaling:** each point multiplies the stat's effect by about 4% up to 20 points, 2% up to 40 and
  1% after that (roughly x6 from one stat maxed). Tree nodes add on top.
- Spells and tree nodes have **stat requirements**, such as Fire Affinity 15.
- Creatures use the same formulas, with stats from their level, rank and element.

## XP and creature levels

- **Level curve:** XP to the next level grows about 9% per level: about 55 at level 1, 660 at level
  30 and 255,000 at level 99.
- **XP source:** kills, plus bonuses for reactions and feats. Spending mana gives no XP. A kill's XP
  is **absorbed mana** (see "The world and its mana"), which is why it grows exponentially with the
  creature's level.
- **Per kill:** about 8 same-level kills per level early, rising to about 40 late.
- **Level gap:** a creature below you pays 10% less per level of gap, and nothing at 10 or more
  below. A creature above you pays 5% more per level of gap, up to +50%.
- **Fixed zones**, set by the place and never by the player's level:

| Place | Base level |
|---|---|
| Overworld near spawn | 1 to 5, +1 per about 150 blocks |
| Deeper biomes, caves, structures | +5 to +15 |
| Nether | 25 to 50 |
| End | 60+ |

- **Ranks are bonus levels:** Adept +0, Magus +8, Archmage +20. With 9% growth per level, +20 is
  about x5.6 power. Spells known stay 1, 2 and 3. Rarity stays as today.

## The world and its mana

Five ideas that grow out of the principle that every creature has mana.

**1. Mana is the world's currency.**
- A creature's mana pool is set by its level (and rank), on the same exponential curve.
- When a creature dies, its mana is released and you absorb part of it. That absorbed mana is the
  XP. A chicken gives a speck and an Archmage gives a flood.
- The level-gap rule above is how much of it you can absorb.
- **Elemental Essence and Catalysts are crystallized mana**: the same thing in item form, so loot, XP
  and awakening all come from one source.

**2. Mana sense.**
- Everyone has an aura. **Insight** lets you read a creature's level and element from it (today the
  game never says), so the stat has real use. Low Insight reads only a rough sense of danger.
- **Pressure:** a creature far above your level leaks pressure. An Archmage's presence unsettles a
  low-level player (a debuff that grows with the level gap and is countered by Ward).
- **Suppression:** you can hide your own aura, so strong mobs notice you less. The cost is slower
  mana regen while hidden.

**3. Mana weather.**
- Ambient mana varies by place. **Rich zones** (the high-level ones) raise mana regen and breed more
  Attuned creatures. **Dead zones** weaken casters and push players toward fighting without spells.
- **Mana tides** surge across the world from time to time. During a tide, awakening is more likely
  and creatures are stronger.

**4. An element shapes your body.**
- Each element has a nature and a weakness. For example, Fire runs warm and resists heat but regains
  mana slowly in the cold; Water is at home near rain and oceans.
- Biome comfort affects your regen, so an element changes how you live and where you want to go.

**6. Equivalent exchange (limited).**
- Only some spells and keystones carry a real, lasting cost, such as spending max health for a surge
  or a spell with a permanent price. It is not a general rule: most spells stay mana-only.

**Not doing:** a hidden "talent grade" rolled at awakening. It would make characters unequal and is
likely to be unpopular.

## The skill tree

- **One big graph:** a shared ring of general stat nodes in the middle, a region for each base
  element around it, and derived clusters on the outer edge of their related region. Every link
  can be walked **from either side**.
- **Starting point:** your first element sets your starting node, inside its region or cluster, even
  if that is a derived element like Ice. Elements linked to it (Water, from an Ice start) open as you
  reach them. Unrelated regions are sealed until you awaken to that element.
- **Node types:**

| Type | What it does |
|---|---|
| Small | A little of a stat |
| Notable | A named bonus (designed per region) |
| Keystone | Changes a rule and carries a drawback |
| Spell | Unlocks a spell |
| Upgrade | A path of nodes out of a spell. It replaces the spell levels and the Lv 5 and 10 branches, with forks for the branch choices |

- About 100 tree points are spent in a tree of 400+ nodes, so a build uses about a quarter of it.
- Nodes can have Affinity or stat requirements.
- Respec is possible but costs something (to be set).
- The tree is **data** (files), so it can be tuned without rebuilding and extended by addons. It is
  shown on a pannable, zoomable screen.

## Awakening

- **First element:** it can happen at any moment, but it can't be put off for long.
  - **From the start:** brushes with an element can wake the magic: nearly burning to death, nearly
    drowning, freezing, a long fall, being blown around. The brush biases the roll toward its
    element, about one chance in three per event. A small background chance each Minecraft day also
    applies, so it can happen anywhere.
  - **After day 10, the odds rise sharply.** From day 10 the daily chance starts at about 25% and
    grows about 15 points each further day, brushes become about 3x as likely, and it is
    **guaranteed by about day 16**. The result is a hard limit of roughly 5 hours of play, never 100
    hours with nothing.
  - **Days are counted from when you first joined**, not from the world's first day, so someone
    joining an old world isn't awakened at once. Sleeping through the night counts, and a normal
    play pace puts day 10 at about 3 to 4 hours.
  - Every roll is weighted by each element's rarity, with a derived-first awakening 1/100 as likely
    as a normal one (see below).
- **Extra elements:** need a **Catalyst** of that element (a new item that takes Essence's place
  here). Each extra element is about 4x rarer than the last: about 25%, 6%, then 1.5%. The chance
  is multiplied by about `1 + level/25`. A failure uses up the Catalyst but adds a small bonus to the
  next try.
- Rare elements have lower weights, or only come from a Catalyst or a special event.
- **Derived elements can be the first element, very rarely.** A derived-first awakening is about
  **1/100 as likely** as a normal one: if a normal first awakening gives Water with weight 100, Ice
  gets weight 1. Its relatives are then
  reached through the tree, so the order isn't fixed.
- **Creatures:** Attuned creatures keep their rarity, now by level and rank.

## What changes in the current game

- Magic Level (cap 30) becomes the universal Level (cap 100) for players and creatures.
- Max mana, regen and spell power from level become the Reservoir, Potency and Affinity stats.
- Linear per-level bonuses (+10 mana, +2% power, -1% cooldown) become the exponential stat curve.
- Spell levels, branches and skill points become tree nodes and tree points.
- The Awakening choice screen becomes the random roll and its brushes.
- XP from mana spent is removed.
- Ice stops being a starter element and moves into Water's region.
- Earth, Crystal and the extra Wind children are new content.
- Attuned creatures' health and damage come from the shared level curve.

## Build order

Each step gets its own plan and play-test.

1. **Universal level, stats and XP**, with creature levels and ranks. The formulas live in the
   unit-tested `Progression` class.
2. **The tree engine and screen.** Data format, point spending, requirements and the viewer.
3. **Awakening:** brushes, the roll, Catalysts and the opposites rule.
4. **New content:** Earth, then the derived elements.
5. **Tree regions,** one at a time, starting with Fire.

## Open questions

- What happens to **mastery bars and Essence** once the tree replaces spell levels.
- **Respec** cost and rules.
- Wind's and Fire's derived elements, and any extra base elements.
- Where **Catalysts** come from.
- Exact numbers for the background chance, the day-10 ramp and the guarantee day (all draft).
- Whether the brush events can cause a derived-first awakening (for example, freezing leaning toward
  Ice before Water), or only the rare roll can.
- Which elements are **linked** to which. These are **yet to be defined** by the user; Water-Ice and
  Earth-Crystal are only examples, so families are not fixed until then.
- How **non-Attuned vanilla mobs** get their stats from the shared formulas.
- Feats and titles, places of power and mana compression (earlier ideas, not yet placed).
- Numbers for the world and its mana: how much of a creature's mana you absorb, how strong
  pressure is, how often tides come and how strong they are, and the nature and weakness of each
  element.
- Which spells and keystones carry a lasting cost.
- Multiplayer balance between players of different levels in the same zone.
- All numbers above are drafts.
