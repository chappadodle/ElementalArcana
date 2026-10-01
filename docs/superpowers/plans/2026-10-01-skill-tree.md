# Skill Tree Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Progression step 2 (`docs/superpowers/specs/2026-10-01-skill-tree-design.md`): a
data-driven, Path of Exile-style skill tree that replaces spell levels, branches and skill points,
with a starter tree and a pannable, zoomable screen.

**Architecture:** The graph and every rule live in plain-Java `api/SkillTree` (unit tested), with
node ids and spell ids as strings. `content/SkillTreeLoader` reads `data/*/skill_tree/*.json`,
expands each spell `path` entry into its chain of upgrade and fork nodes from the spell's own
`maxLevel()`/`branchOptions()`, publishes the tree to `SkillTrees.current()` and syncs it to
clients. `MagicData` stores the nodes a player took; their starts come from their awakened
elements. Spell levels, branches, castable spells and tree stat bonuses are all derived (and
cached) from the held set, so spells keep reading `CastContext.spellLevel()`/`branch()` untouched.

**Tech Stack:** NeoForge 21.1.252 (Minecraft 1.21.1), Java 21, JUnit 5, Python 3 for the starter
tree generator.

## Global Constraints

- Tree points = (level − 1) + bonus (condensed) points − nodes taken; starts are free.
- Small node: +2 to one stat key (`reservoir`… or `affinity/<family>`), added to stat points on
  the same curve.
- Upgrade/fork nodes need the spell's mastery bar full and reset it; forks of one spell level lock
  each other.
- Refund: one node or stat point at a time, costs **1 + level / 20** Essence of any element; the
  rest of the tree must stay connected to a start and keep its requirements.
- A start is free and auto-held for players of that exact element; for players of the same family
  it's a 1-point spell node (so Water players can reach Icicle and Ice players Hydro Jet).
  *(Implementation detail added to the spec: without it, a Water player could never get Icicle.)*
- Regions: Wind north, Fire east, Water south, west empty (Earth), Ice cluster off Water's outer
  edge behind **Water Affinity 10**.
- Spells are castable only through the tree (spell node or start); levels no longer unlock spells.
- Never modify git config; no commits until the user play-tests (project rule).

---

## File map

| File | Responsibility |
|---|---|
| `api/SkillTree.java` (new) | Node record, graph, `canTake`, `canRefund`, `requirementsMet`, `grants` |
| `api/Progression.java` (modify) | `refundCost(level)` |
| `content/SkillTreeLoader.java` (new) | JSON reload listener, path expansion, sync on datapack sync |
| `content/SkillTrees.java` (new) | Holder of the current tree (server-loaded or client-synced) and a version counter |
| `network/SkillTreeSyncPayload.java` (new) | Server → client: the whole expanded tree |
| `network/TreePayload.java` (new) | Client → server: take, refund node, refund stat point |
| `core/MagicData.java` (modify) | Held nodes, derived grants (cached), tree points, stat totals, take/refund |
| `core/SpellProgress.java` (modify) | Mastery only |
| `core/CastingService.java`, `core/Conjuring.java` (modify) | `data.branches(spell)` |
| `content/LevelCombat.java`, `core/PlayerStats.java`, `content/CreatureRewards.java` (modify) | Use stat totals |
| `client/SkillTreeScreen.java` (new) | The viewer |
| `client/StatusScreen.java`, `client/StatsScreen.java`, `client/DevScreen.java` (modify) | Open the tree, stat refunds, dev tree actions |
| `client/SpellDetailScreen.java`, `network/SpellProgressPayload.java` (delete) | Replaced by the tree |
| `core/ArcanaCommand.java` (modify) | `/arcana tree info|take|refund|reset` |
| `tools/gen_skill_tree.py` (new) → `data/elementalarcana/skill_tree/{core,fire,water,wind,ice}.json` | Starter tree |
| lang, README, `tools/server_tests/skill_tree.txt` | Text, docs, server check |

---

### Task 1: `SkillTree` graph and rules (pure Java)

**Files:** Create `api/SkillTree.java`; test `src/test/java/.../api/SkillTreeTest.java`; modify
`api/Progression.java` (+ `ProgressionTest`).

**Interfaces (produces):**
```java
public final class SkillTree {
    public enum Type { START, SMALL, SPELL, UPGRADE, FORK }
    public record Node(String id, Type type, int x, int y, @Nullable String element,
                       @Nullable String stat, int amount, @Nullable String spell, int spellLevel,
                       @Nullable String branch, @Nullable String requiresStat, int requiresMin) {}
    public interface Player {          // what a rule needs to know about the player
        int freePoints();
        boolean holdsElement(String element);   // exact element (start ownership)
        boolean holdsFamily(String element);    // any held element of element's family
        int statPoints(String key);             // spent stat points (no tree bonus)
        boolean masteryFull(String spell, int currentLevel);
    }
    public enum Check { OK, HELD, NO_POINTS, NOT_LINKED, SEALED, REQUIREMENT, MASTERY, FORK_TAKEN, START, DISCONNECTS, NEEDED, UNKNOWN }
    public record Grants(Map<String, Integer> stats, Map<String, Integer> spellLevels,
                         Map<String, Map<Integer, String>> branches, Set<String> spells) {}
    public SkillTree(Collection<Node> nodes, Collection<List<String>> links);
    public static SkillTree EMPTY;
    public @Nullable Node node(String id);
    public Collection<Node> nodes();
    public Set<String> links(String id);
    public List<List<String>> linkPairs();
    public Set<String> heldStarts(Player player);            // starts of the player's exact elements
    public Set<String> held(Set<String> taken, Player player); // taken ∪ heldStarts
    public Grants grants(Set<String> held);
    public Check canTake(Set<String> taken, String id, Player player);
    public Check canRefund(Set<String> taken, String id, Player player);
    public boolean requirementsMet(Set<String> held, ToIntFunction<String> statPoints);
}
```
`Progression.refundCost(int level)` = `1 + level / 20`.

Rules (exact):
- `grants(held)`: stats = sum of SMALL amounts by stat key. spells = spell of every held SPELL and
  START. spellLevels[spell] = 1 + held UPGRADE/FORK of that spell (only for spells in `spells`).
  branches[spell][level] = branch of held FORK nodes.
- `canTake`: UNKNOWN if no node; HELD if in held; START if START and the player holds its exact
  element (auto-held, nothing to take); NO_POINTS if `freePoints() <= 0`; NOT_LINKED if no link to
  a held node; SEALED if `element != null && !holdsFamily(element)`; REQUIREMENT if
  `requiresStat != null` and `statPoints(key) + grants(held).stats[key] < requiresMin`;
  FORK_TAKEN if FORK and a held FORK has the same spell and spellLevel; MASTERY if UPGRADE/FORK and
  `!masteryFull(spell, grants(held).spellLevels[spell])`.
- `canRefund`: UNKNOWN; NOT held → `HELD` is wrong, return `UNKNOWN`… use: if not in `taken` →
  START if it's an auto-held start else UNKNOWN. After removal, every node of `held − {id}` must be
  reachable (through held nodes) from a held START → else DISCONNECTS; `requirementsMet(rest)` →
  else NEEDED.
- `requirementsMet(held, statPoints)`: every held node with a requirement has
  `statPoints(key) + grants(held).stats[key] >= min`.

- [ ] **Step 1: Write the failing tests** — `SkillTreeTest` builds this tree in code:
  `fire_start(START, fire, spell=fireball)` — `fb2(UPGRADE fireball 2)` — `fb3a(FORK fireball 3
  branch a)` / `fb3b(FORK fireball 3 branch b)` — `fb4(UPGRADE fireball 4)` linked to both forks;
  `fire_start` — `core1(SMALL potency 2)` — `core2(SMALL affinity/water 2)` — `ice_gate(SMALL
  affinity/water 2, element ice, requires affinity/water 4)` — `ice_start(START, ice, spell=icicle)`.
  A test `Player` record with fields for each answer. Tests:
  1. `startsAreHeldForTheirElement`: fire player → `held(∅)` = {fire_start}; grants spells {fireball}, level 1.
  2. `takingNeedsALinkAPointAndAFamily`: core2 → NOT_LINKED; core1 → OK; with 0 points → NO_POINTS;
     ice_gate from a fire player holding core1, core2 → SEALED.
  3. `upgradesNeedFullMasteryAndCountLevels`: fb2 with mastery not full → MASTERY; full → OK;
     held {fb2, fb3a} → fireball level 3, branch {3: a}.
  4. `forksLockEachOther`: holding fb2, fb3a → fb3b FORK_TAKEN; fb4 is linked to both → OK.
  5. `requirementsUseTreeBonusesToo`: water player (holdsFamily water) holding core1, core2 with
     2 stat points in affinity/water → ice_gate OK (2 + 2 ≥ 4); with 1 → REQUIREMENT.
  6. `refundsKeepTheTreeConnected`: held {core1, core2} → refund core1 DISCONNECTS, core2 OK;
     refund fire_start → START.
  7. `refundsKeepRequirements`: held {core1, core2, ice_gate} with 2 stat points → refund core2
     NEEDED (ice_gate would need 4, has 2).
  8. `familyMembersBuyTheStart`: water player holding core1, core2, ice_gate (enough Affinity) →
     ice_start OK (costs a point, gives icicle); an ice player → START.
  Plus `ProgressionTest.refundCostRisesEveryTwentyLevels`: 1→1, 19→1, 20→2, 100→6.
- [ ] **Step 2:** `./gradlew test` → compile failure.
- [ ] **Step 3:** Implement `SkillTree` per the rules above (adjacency `Map<String, Set<String>>`,
  BFS for connectivity) and `refundCost`.
- [ ] **Step 4:** `./gradlew test` → all pass.

### Task 2: Loader, sync and the starter tree data

**Files:** Create `content/SkillTrees.java`, `content/SkillTreeLoader.java`,
`network/SkillTreeSyncPayload.java`, `tools/gen_skill_tree.py`, the five JSON files; modify
`network/ModNetwork.java`, `core/ArcanaCommand.java` (`tree info`).

**Interfaces:** `SkillTrees.current()` → `SkillTree`; `SkillTrees.set(SkillTree)` bumps
`SkillTrees.version()` (int). `SkillTreeSyncPayload(List<Node> nodes, List<List<String>> links)`.

- JSON node: `{"id","type":"start|small|spell|path","x","y","links":[...],"element"?,"stat"?,
  "amount"?,"spell"?,"requires"?:{"stat","min"}}`. Path entry: `{"type":"path","id","spell",
  "x","y","dx","dy","from"?,"element"?,"links"?}` — `from` = an existing node the chain hangs off
  (a start); without it a SPELL node `id` is created at (x, y) with `links`. Expansion: for
  level L = 2..maxLevel, the options are `branchOptions(L)`; no options → one UPGRADE node
  `<id>/<L>` at `(x + dx·(L−1), y + dy·(L−1))`; options → one FORK node `<id>/<L>/<branch>` per
  option, spread perpendicular (offset `(−dy, dx)` normalised × 26 × (i − (n−1)/2)). Every node of
  level L links to every node of level L − 1 (or to the spell node / `from` for L = 2). Path nodes
  inherit `element`.
- Loader: `SimpleJsonResourceReloadListener` on folder `skill_tree`, registered in
  `AddReloadListenerEvent`; links are merged across files; unknown spells or dangling links are
  logged as errors and skipped. `OnDatapackSyncEvent`: send the tree to `event.getPlayer()` or, on
  reload, to all players. The client handler calls `SkillTrees.set`.
- `tools/gen_skill_tree.py` (geometry; 1 unit = 1 screen pixel at zoom 1):
  - Core ring: 30 SMALL nodes at radius 140, stats cycling reservoir, potency, focus, ward,
    vitality, insight (amount 2), each linked to its neighbours.
  - Regions at angle θ (Wind −90°, Fire 0°, Water 90°): a spoke SMALL `affinity/<e>` at r = 200
    linked to the ring node nearest θ, then the START at r = 260. Arms leave the start at
    θ + offset with 40-unit steps:
    - Wind: path wind_blade (0°, from start); arm +45° [spell gale_dash]; arm −45° [affinity/wind,
      focus, affinity/wind, potency, spell updraft].
    - Fire: path fireball (0°); arm +45° [affinity/fire, potency, affinity/fire, reservoir, spell
      flame_burst]; arm −45° [affinity/fire, focus, potency].
    - Water: path hydro_jet (0°); arm +40° [spell tidal_wave]; arm −40° [affinity/water, reservoir,
      spell bubble_prison, affinity/water, spell healing_rain]; arm +75° [affinity/water, ward,
      affinity/water] leading to Ice.
    - Ice (element ice, centred at 135°, r = 640): `ice_gate` SMALL affinity/water requires
      affinity/water 10, linked to Water's +75° arm end; `ice_start` START (spell icicle) next to
      it; path icicle outward; arm [ward, affinity/water, spell frost_shield] then path
      frost_shield from frost_shield; arm [focus, affinity/water, ward, potency, spell frost_nova].
  - Region nodes carry `element`; the script asserts no two nodes are closer than 18 units and
    writes the five files.
- `/arcana tree info` (console-safe): prints node count by type and link count.

- [ ] Steps: write the generator, run it, implement loader/holder/payload, register, build, run a
  server test (`tools/server_tests/skill_tree.txt`: `arcana tree info`) → expect about 110–130
  nodes and no errors.

### Task 3: MagicData on the tree

**Files:** modify `core/MagicData.java`, `core/SpellProgress.java`, `core/CastingService.java`,
`core/Conjuring.java`, `content/LevelCombat.java`, `core/PlayerStats.java`,
`content/CreatureRewards.java`, `client/StatsScreen.java`, `network/DevActionPayload.java`,
`core/ArcanaCommand.java`, `network/EssencePayload.java`, `content/EssenceService.java`; delete
`network/SpellProgressPayload.java`, `client/SpellDetailScreen.java`.

**Interfaces (produces on MagicData):** `Set<String> treeNodes()`, `Set<String> heldNodes()`,
`SkillTree.Grants grants()`, `int treePoints()`, `int stat(Stat)`, `int affinity(Element)`,
`int statTotal(String key)`, `Map<Integer, String> branches(Spell)`,
`SkillTree.Check checkTake(String id)`, `SkillTree.Check take(String id)`,
`SkillTree.Check checkRefund(String id)`, `void refund(String id)`, `boolean canRefundStat(String key)`,
`void refundStat(String key)`, `void resetTree()`, dev `void takeWholePath(Spell)`.

- Save `tree` (list of node ids) and sync it. Grants are cached by (tree version, held set).
- `spellLevel(spell)` = grants level (default 1) capped at `maxLevel()`; `canCast(spell)` =
  `grants().spells().contains(id)`; `branches(spell)` from grants.
- `SpellProgress` keeps only mastery (save `mastery`; old `level`/`branches` fields are ignored,
  which refunds old spell levels as tree points automatically).
- `take`: on OK add the id; for UPGRADE/FORK set that spell's mastery to 0.
- Stat totals (`statTotal`) = stat points + tree bonus; max mana, regen, spell power, cooldowns,
  Ward, Vitality and Insight use totals (`stat(Stat)`, `affinity(Element)`).
- `canRefundStat(key)`: points > 0 and requirements still met with one point fewer.
- Remove `skillPoints`, `canLevelUp`, `levelUp`, `pendingBranchLevel`, `chooseBranch`, `respec`,
  `respecReadyAt` (field stays in the codec, unused), `setSpellLevel`, `clearBranches`.
- Grants are invalidated when affinities change (starts depend on them).
- Dev actions: `ADD_SPELL_LEVELS`/`CLEAR_BRANCHES` → `MAX_SPELL` (takes the selected spell's whole
  path for free, first branch at each fork) and `RESET_TREE`.

### Task 4: Take/refund over the network, commands, Essence refunds

**Files:** create `network/TreePayload.java`; modify `ModNetwork`, `ArcanaCommand`.
- `TreePayload(Action TAKE|REFUND|REFUND_STAT, String id)`. REFUND/REFUND_STAT: cost
  `Progression.refundCost(level)`; needs that much Essence (EssenceService.total), removes it
  (largest stacks first, as Condense does), then refunds. Afterwards `PlayerStats.apply` and sync;
  sounds: take = amethyst chime, refund = grindstone.
- Commands: `/arcana tree take <id>`, `/arcana tree refund <id>` (no Essence), `/arcana tree reset`.

### Task 5: The screen

**Files:** create `client/SkillTreeScreen.java`; modify `StatusScreen`, `StatsScreen`, lang.
- World-to-screen: `sx = cx + (x + panX) · zoom`; drag (left button, > 3 px) pans; scroll zooms
  0.4–2.0 around the cursor. Opened centred on the focused spell's node, else on the player's
  first start.
- Links: thick quads via `graphics.bufferSource().getBuffer(RenderType.gui())` (held–held bright
  gold, held–available pale, others dark).
- Nodes (blocky squares): small 8 px coloured by stat; upgrade 10 px, fork 12 px with a gold
  border; spell/start 20 px with the spell icon (`ArcanaDraw.icon`). Held = bright with border,
  takeable = lit and pulsing, locked = dim.
- Click (no drag) → `TreePayload` TAKE; right click → REFUND (with the cost in the tooltip).
- Tooltip: name, effect, "Costs 1 tree point" / "Refund: N Essence", the blocker (one line per
  `Check`).
- Header: "Tree points: N". Bottom panel for the focused spell (last hovered spell-path node or the
  one opened with): icon, name, Lv x/max, mastery bar, Infuse button (Shift = fill) sending
  `EssencePayload.infuse`.
- StatusScreen: a "Tree" button (top left, next to Stats); clicking a spell row opens the tree
  focused on it; the row shows "▲ Ready" when mastery is full and a point is free, "Locked" when not
  castable; tooltip says "Unlock it in the skill tree" for uncastable spells; points label →
  "Tree points". StatsScreen: right-click a stat row refunds a point (tooltip shows the cost).

### Task 6: Docs, server checks, memory

- README: the tree replaces spell levels/skill points; refunds; commands.
- Run every server test plus `skill_tree.txt`; zero errors. Build. Hand off for play-testing.
