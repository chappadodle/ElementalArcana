# Relics

## Why

Crypts end in a boss and two reliquary chests, but everything in them can be found elsewhere.
A dungeon needs treasure you can only get there, the kind that changes how you play: Zelda's
items, Diablo's uniques, Terraria's accessories. Relics are the first mages' own trinkets, sealed
in with them: one per element, each with a power of its element, and the Revenant's own
phylactery. They give every crypt a reason to be cleared, and a reason to seek out crypts of other
elements.

## Bearing a relic

- A relic works while it's anywhere in your inventory, but only once you've **bound** it: use it
  (right-click) to bind it to yourself, with a chime and a ring of its element's light. Its name
  glints and its tooltip says it's bound to you.
- **One relic at a time:** the one you bound last is the one that works. Binding another wakes the
  new one and puts the old to sleep (it stays bound, so you can carry both and switch by using the
  other). A relic bound to someone else does nothing for you until you bind it yourself.
- Each relic adds a few stat points (like gear) and has one **power**.

## The relics

| Relic | Found in | Stats | Power |
|---|---|---|---|
| **Ember Heart** | Fire crypts | +3 Potency | Burning foes take 25% more from your spells; you don't burn (fire and burning can't hurt you; lava still does) |
| **Tidecaller's Pearl** | Water crypts | +3 Vitality | Underwater you breathe and see as if under a conduit; your Water spells mend you by a tenth of the damage they deal |
| **Rimeheart Locket** | Ice crypts | +3 Ward | Cold can't touch you (no freezing, Frozen or slowness); your spells hit frozen and frosted foes 20% harder |
| **Feather of the Gale** | Wind crypts | +3 Focus | Jump again in mid-air, once a jump; no fall damage |
| **Stoneheart Idol** | Earth crypts | +3 Vitality | A hard hit (4 or more) hardens your skin: 2 hearts of absorption for 10 seconds, at most every 15 seconds |
| **Prism of the Deep** | Crystal crypts | +3 Insight | Your Earth and Crystal spell hits restore 2 mana (at most twice a second) |
| **Storm Sigil** | Lightning crypts | +3 Focus | You run 15% faster; in a thunderstorm your spells hit 20% harder |
| **Sunstone** | Radiance crypts | +3 Potency | You see in the dark; the undead within 6 blocks smoulder (1 damage a second, set alight) |
| **Revenant's Phylactery** | Revenants | +3 Reservoir | Once every 10 minutes a killing blow leaves you at 1 health instead, in a burst of souls that throws foes back; its tooltip shows when it's ready |

- **Where:** each reliquary chest holds its crypt's relic 30% of the time (so about half of all
  crypts give theirs), and a Revenant leaves its Phylactery one time in four.
- Relics don't stack and can't be crafted. Arcanists don't sell them.
- An advancement, **Relic of the First Mages**: bind a relic.
- Journal page 28: Relics.

## Code

| Piece | What it does |
|---|---|
| `api/RelicRules` | The numbers: bonuses, cooldowns, radii (tested) |
| `content/relic/Relic` | The nine relics: element, stats |
| `content/relic/RelicItem` | Binding by use; tooltip (power, bound, the Phylactery's recharge) |
| `content/relic/RelicBond` | The data component: who bound it, and when (the latest bound one works) |
| `content/relic/Relics` | Which relic a player bears (cached per tick) and every power's event hooks |
| `content/gear/GearStats` | Adds the borne relic's stats |
| `client/RelicJump` | The Feather's mid-air jump (the player's own client moves them), told to the server |
| `network/RelicJumpPayload` | The server checks the jump and shows it to everyone |
| `tools/gen_relics.py` | The relics' icons |

## Testing

- `RelicRulesTest`: the numbers.
- `tools/server_tests/relics.txt`: each relic given, bound (by command), and its stats read back;
  the Phylactery saving a player from `/kill`-level damage once, then not again until it recharges.
- `tools/autotest/relics.txt`: binding (the ring of light, the tooltip), the Feather's double jump
  (heights logged), the Ember Heart in fire, the Sunstone's smouldering undead, the Stoneheart's
  absorption.
