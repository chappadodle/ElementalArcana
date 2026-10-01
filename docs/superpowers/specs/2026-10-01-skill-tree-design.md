# Skill tree (progression step 2): design

Date: 2026-10-01
Status: approved in conversation. It's step 2 of
`docs/superpowers/specs/2026-10-01-progression-system-design.md`, and its numbers are drafts to tune.

## Decisions

- **Mastery gates a spell's path.** A spell's upgrade nodes cost tree points, and the next one can
  only be taken once that spell's mastery bar is full (cast it to fill it). Infuse still fills the
  bar with Essence. Condense turns Essence into bonus tree points.
- **Refunds go one at a time and cost Essence.** That covers tree nodes and stat points alike.
- **Step 2 is the engine plus a starter tree.** Today's spells become paths, with a central ring
  of small stat nodes and the Fire, Water and Wind regions, plus Ice as a cluster on Water's edge.
  Notables and keystones come in step 5, designed region by region.

## Points

- Each level gives one **tree point**, which replaces the skill point. Condensed Essence gives
  bonus tree points, at the same costs as today (8, 12, 16 and so on).
- Unspent tree points = (level − 1) + bonus points − nodes taken (start nodes are free).
- **Existing saves:** bought spell levels are refunded as tree points and branch choices are
  cleared. Mastery bars are kept.

## Nodes

| Type | Cost | Does |
|---|---|---|
| Start | free | One per element (Fire, Water, Ice, Wind). It's taken for you when you hold that element, and it gives that element's starter spell. |
| Small | 1 point | +2 points to one stat (any Stat, or an Affinity), added on top of stat points on the same curve |
| Spell | 1 point | Unlocks a spell |
| Upgrade | 1 point | The next level of a spell (Lv 2 to 10), in a chain behind its spell node. It needs that spell's mastery bar full, and taking it resets the bar. |
| Fork | 1 point | At Lv 5 and Lv 10 a spell's chain splits into one node per branch. Taking one gives that level and that branch, and locks the others. |

- **Starter spells, given by the start node:** Fire gives Fireball, Water gives Hydro Jet, Ice gives
  Icicle and Wind gives Wind Blade.
- **Spell nodes:** the other spells sit at a depth from their start that matches their old level
  requirement:
  - Water: Tidal Wave (next to the start), Bubble Prison (about 3 deep), Healing Rain (about 5 deep)
  - Fire: Flame Burst (about 5)
  - Ice: Frost Shield (about 3) and Frost Nova (about 5)
  - Wind: Gale Dash (next to the start) and Updraft (about 5)
- **Leveled spells** (Fireball, Hydro Jet, Icicle, Frost Shield, Wind Blade) have a path of upgrade
  nodes: Lv 2–4, a fork at Lv 5, Lv 6–9, then a fork at Lv 10.
  - The Lv 6 node links to every Lv 5 fork, so the chain continues whichever branch you took.
- A spell's level is 1 + the upgrade and fork nodes you hold on its path. Its branches come from the
  fork nodes you hold. Spells read these exactly as before, so no spell code changes.

## Rules

- **Taking a node** requires:
  - a free tree point
  - a link to a node you already hold
  - the node's element family is one you hold an element of (the core ring has no element)
  - its stat requirement, if any, met by your total stats (points plus tree bonuses)
  - for upgrades and forks, a full mastery bar
  - for forks, no other fork of the same spell level taken
- **Refunding a node** (right-click) requires:
  - every node you still hold stays connected to one of your starts
  - no node you still hold loses a stat requirement it needs
  - start nodes can't be refunded
  - it costs Essence of any element: **1 + level / 20** (1 at levels 1 to 19, up to 6 at level 100)
- **Refunding a stat point** works the same way: one point at a time, for the same Essence cost,
  from the Stats window.
- A spell is castable when you hold its spell node or the start that gives it. Spells no longer
  unlock by level.

## Starter tree layout

- **Core ring:** about 30 small nodes around the centre, at least 4 of each Stat. Short spokes run
  to each region's start.
- **Regions** sit around the ring: Fire to the east, Water to the south, Wind to the north, and the
  west left empty for Earth (step 4). Each holds:
  - its start
  - its spell nodes and spell paths
  - small nodes along the way (mostly its own Affinity, plus Potency, Focus, Reservoir)
- **Ice** is a cluster on Water's outer edge, linked both ways. Its first node needs **Water
  Affinity 10**, and an Ice start sits inside the cluster for players whose element is Ice.
- About 100 to 130 nodes in total.

## Data

- One file per region in `data/elementalarcana/skill_tree/` (`core.json`, `fire.json`,
  `water.json`, `wind.json`, `ice.json`), loaded on server start and `/reload`, then synced to
  players. Addons can add files.
- A node has an `id`, a `type`, a position (`x`, `y`), its `links` (ids, in either direction),
  an optional `element` (its region's family), an optional `requires` (stat key and minimum), and
  by type a `stat` and `amount`, or a `spell`.
- A **spell path** entry (`"type": "path"`, with a spell, a start position and a direction) is
  expanded by the loader into the spell node's upgrade chain and forks, using the spell's own max
  level and branch options. The data stays short and always matches the spell code.
- The starter files are written by a script, `tools/gen_skill_tree.py`.

## Code

| Unit | Job |
|---|---|
| `api/SkillTree` (pure Java, unit tested) | The graph; whether a node can be taken or refunded; what a set of nodes gives (stat bonuses, spell levels, branches, unlocked spells) |
| `content/SkillTreeLoader` | Reads the data files (and expands paths); syncs the tree to clients |
| `core/MagicData` | Stores the nodes a player holds; stats, spell levels, branches and castable spells come from them |
| `network/TreePayload` | Client asks to take or refund a node (or refund a stat point); the server checks everything |
| `client/SkillTreeScreen` | The viewer |

## Screen

- Opened from the Status window. It replaces each spell's own skill-tree screen, and clicking a
  spell in the Status window jumps the view to it.
- Drag to move around and scroll to zoom. Links you hold glow, and the nodes you can take next are
  lit.
- **Click** takes a node and **right-click** refunds it.
- Hovering shows the node's name, what it does, its cost and anything blocking it, such as
  "mastery not full" or "needs Water Affinity 10". For a spell's next upgrade, the tooltip panel
  also shows the mastery bar and the Infuse button.
- Spell nodes show the spell's icon, and small nodes are dots colored by stat.

## Testing

- Unit tests for `SkillTree`: taking, connectivity, refunds, forks, requirements, and what a set
  of nodes gives.
- A server test that loads the starter tree and takes and refunds nodes through new
  `/arcana tree` commands.
- A play-test of the screen.
