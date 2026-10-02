# From sandbox to fantasy adventure: roadmap

Date: 2026-10-02
Status: decided. The user handed over every decision ("make all of the decisions and take all of the
necessary steps") to turn Minecraft into a real fantasy adventure. This is the plan the work follows;
each milestone gets its own spec section or file when it's built.

## The vision

You start as an ordinary person with dormant mana. One day an element nearly takes you, and your
magic wakes. From there the world opens up:

- **Grow:** levels, stats and the skill tree turn a frightened apprentice into an archmage.
- **Explore:** shrines, mage towers and ruins are scattered across the world, each one a reason to
  travel.
- **Fight:** elemental creatures, Attuned casters and guardians stand in the way.
- **Gear up:** wands, staves and robes, made from what you find and what you defeat.
- **Meet people:** arcanists in villages trade in magic, and a journal guides you.
- **Pursue a goal:** an old story asks you to restore what was broken.

## The story (kept light, told through the Journal and places)

Magic was once one thing, the Prime. When the Hollow, a hunger that devours mana, broke into the
world, the first mages split the Prime into the elements and bound the Hollow with them. Four
Sovereigns, one per base element, keep the seals. The seals are fading: mana stirs in ordinary people
again (awakening), elemental creatures wander, and Attuned casters grow bolder. The player's path
leads from shrine to tower to sanctum, gathering what the Sovereigns guard, until the seals can be
remade or the Hollow faced.

## Pillars and milestones

Built roughly in this order. Each milestone is verified (build, unit tests, server tests and the
hidden visual test client, `tools/autotest.sh`) and committed when done.

| # | Milestone | What it adds |
|---|---|---|
| 1 | Earth and the test client | Earth (step 4a); `tools/autotest.sh` for visual checks without anyone at the keyboard |
| 2 | The Arcanist's Journal | A book given when your magic wakes: what happened to you, how magic works, where to go next; plus the lore |
| 3 | Gear I: foci and robes | Wands, staves and orbs per element (Potency and Affinity), apprentice and adept robes (Reservoir, Ward, Focus); crafted from Essence; stats from gear feed the stat system |
| 4 | World I: elemental shrines | Small shrines in matching biomes: places of power (faster mana regen nearby, a daily blessing), a way for the dormant to seek an element, and a small cache |
| 5 | Creatures I: wisps | Elemental wisps, one per element: small floating casters that spawn near shrines and in matching biomes, a steady source of Essence |
| 6 | People: the Arcanist | A village profession with a workstation (the Arcane Lectern), trading Essence, Catalysts, potions and gear |
| 7 | World II: mage towers | Rare multi-floor towers with loot, guarded by Attuned casters and a tower guardian |
| 8 | Brews | Mana draughts and elixirs (brewed), for when the pool runs dry |
| 9 | Skill tree: notables and keystones | Named bonuses and rule-changing keystones in every region, so builds really differ |
| 10 | Derived elements | Crystal (from Earth), Lightning (from Wind), Radiance (from Fire), each a cluster of the tree with its own spells |
| 11 | Sovereigns and the Hollow | Elemental sanctums with a Sovereign boss each; their hearts unlock archmage gear and the way to the Hollow |
| 12 | Polish passes | Visual and sound passes for the remaining spells, mana weather and mana sense from the progression spec |

## Decisions that shape everything

- **Minecraft style:** pixel art and blocky models, drawn by our own generators (`tools/gen_*.py`);
  vanilla sounds unless a custom one clearly earns its place.
- **Original content only:** inspiration from games and other mods is welcome, but no copied assets
  or code, and no new required dependencies unless one is clearly needed.
- **One stat system:** gear, tree nodes and stat points all add to the same stats, so the numbers
  stay balanced in one place.
- **Data where it helps:** structures, loot, trades and the tree are data or generated, so they can be
  tuned without rewriting code.
- **Danger scales with place:** the level-zone system decides how hard every new creature and
  structure is.
