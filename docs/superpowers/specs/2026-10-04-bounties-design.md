# Arcanist Bounties

## Why

The world now has plenty to fight: Attuned creatures, wisps, rifts, towers. What it lacks is a
reason to go after any particular one, and a voice asking you to. Arcanists already trade in magic;
now they also post bounties. A bounty is a small, concrete goal ("Slay five creatures Attuned to
Fire") with a reward waiting at the end, and it uses only what's already there: villager trades to
take one, the inventory to carry it, and the Arcane Lectern to hand it in. No new screens.

## Taking a bounty

- An Arcanist sells **Bounty Contracts** among its trades: one at Novice, one at Apprentice,
  Journeyman and Expert, two at Master. A contract costs a single emerald; its task is rolled when
  the Arcanist offers it, so every Arcanist's board is different, and harder tasks come at higher
  levels.
- A contract is a rolled-up parchment item. Its name says the task, its tooltip shows the progress
  and the reward ("Slay creatures Attuned to Fire: 2/5 — reward: 10 emeralds, 4 Fire Essence").
  A finished contract glints like an enchanted item.

## The tasks

| Task | Count | Offered from | Reward |
|---|---|---|---|
| Slay creatures Attuned to an element (the Arcanist's land's, or any base element) | 5–8 | Novice | 8–12 emeralds, 4 of that Essence |
| Defeat wisps | 3–5 | Apprentice | 8–12 emeralds, 2 Wisp Motes |
| Slay a Magus (any element) | 1 | Journeyman | 16 emeralds, a Tome of Insight |
| Close an elemental rift | 1 | Expert | 20 emeralds, a Catalyst of the Arcanist's land's element |
| Slay an Archmage (any element) | 1 | Master | 32 emeralds, a Scroll of Unbinding, 8 Essence |

- Progress counts for every contract in the player's inventory that the deed fits (a Fire Adept
  counts for "Attuned to Fire" and, if it's a Magus, for "Slay a Magus" too). Kills count when the
  player (or their spell) lands the killing blow; a rift counts for everyone near it when it closes.
- Contracts stack only if they're identical (they almost never are).

## Handing one in

Right-click any **Arcane Lectern** holding a finished contract: the reward pops out on top of the
lectern with the villager trade sound and some experience, and the contract is used up. The
Arcanist's lectern is where you'd find them, but any lectern will do, so a player can keep one at
home.

A **Bounty Hunter** advancement (under The Arcanist) marks the first one handed in.

While testing this, Attuned creatures turned out to drop their Essence even with the `doMobLoot`
rule off; like any loot, it now follows the rule.

## Code

| Piece | What it does |
|---|---|
| `api/BountyRules` | Tasks, counts and rewards by trade level (tested) |
| `content/people/BountyContractItem` | The contract: its task, progress and reward in a data component; name, tooltip, glint |
| `content/people/Bounties` | Counting deeds (kills, rifts), handing in at a lectern |
| `content/people/ArcanistTrades` | The contracts among the Arcanist's trades (two chances at Novice and Apprentice, one after) |
| `content/people/ArcaneLecternBlock` | Takes finished contracts |
| assets | The contract's texture (`tools/gen_textures.py`), lang |

## Testing

- `BountyRulesTest`: tasks by level, counts and rewards in range.
- The server suite can't make a player kill anything, so the counting and the hand-in are checked
  in game (below); `/arcana bounty <task>` gives a contract for testing.
- `tools/autotest/bounties.txt`: a contract bought from an Arcanist, its tooltip, the kills, the
  glint, the hand-in at a lectern and the reward.
