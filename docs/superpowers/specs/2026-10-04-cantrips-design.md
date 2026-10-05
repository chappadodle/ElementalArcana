# Cantrips

## Why

Every spell so far is for a fight. A mage's life should be easier than a farmer's in more ways than
that: lighting a cave without torches, finding ore, getting home, mending a worn pick. **Cantrips**
are small, everyday magics any awakened mage can learn, whatever their elements: the Arcane school.
They're learned from **Cantrip Scrolls** found in the world's old places and sold by Arcanists, so
exploring teaches you magic that changes how you play the sandbox.

## The Arcane school

- A new school, **Arcane** (pale violet), with no element: no affinity, no tree, no chart. Its
  spells are cantrips, which never level; any awakened mage can cast the ones they've learned.
- The Status window shows the Arcane school (lit once you're awakened) and its cantrips; a cantrip
  not yet learned says to find its scroll. Learned cantrips join the spell wheel.

## Cantrip Scrolls

- One scroll per cantrip ("Cantrip Scroll: Mage Light"). Use it to learn the cantrip for good (the
  scroll crumbles into motes). Asleep, its words mean nothing to you; known, it's kept.
- Found in ruins, crypts, mage towers and drake hoards (one in a few chests); Arcanists sell one
  now and then.

## The cantrips

| Cantrip | Mana | Cooldown | What it does |
|---|---|---|---|
| **Mage Light** | 4 | 0.5 s | Sets a small floating orb of light (light 15) where you look, up to 24 blocks; it stays until punched out |
| **Prospect** | 20 | 30 s | For 8 seconds you see the ores within 12 blocks through the stone, outlined in their colours |
| **Recall** | 50 | 10 min | After 4 seconds standing still (moving or being hurt breaks it, swirling motes), you're taken to your bed or respawn anchor (without using its charge), or the world's spawn |
| **Mend** | 25 | 3 s | Mends the item in your hand by a fifth of its durability (at least 25) |
| **Water Breathing** | 15 | 60 s | 90 seconds of water breathing |
| **Featherfall** | 10 | 30 s | 20 seconds of slow falling |
| **Night Eye** | 10 | 60 s | 3 minutes of night vision |

## Code

| Piece | What it does |
|---|---|
| `api/CantripRules` | Their numbers (tested) |
| `content/cantrip/*Spell` | The seven cantrips (Spell subclasses in the `arcane` school) |
| `content/cantrip/ModCantrips` | The scroll item and its `elementalarcana:cantrip` component, the Mage Light block |
| `content/cantrip/Recalls` | Recall's channel: still, unhurt, then home |
| `core/MagicData` | Learned cantrips (saved and synced); `canCast` counts them |
| `network/ProspectPayload`, `client/ProspectOutlines` | The ores' positions to the caster, drawn through walls |
| `tools/gen_cantrips.py` | The spell icons, the scroll, the Mage Light, loot and trades |

## Testing

- `CantripRulesTest`.
- `tools/server_tests/cantrips.txt`: scrolls in chests' loot (rolled), a Mage Light block placed and
  broken (no drop).
- `tools/autotest/cantrips.txt`: a scroll used and the cantrip learned (Status window shot), then
  each cast: a Mage Light in a dark cave (shot), Prospect underground (shot), Mend on a worn pick
  (durability logged), the three potions' effects logged, Recall to a bed and broken by a step.
