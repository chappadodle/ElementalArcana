# Chain Lightning, Lv 1–10

## Why

The derived elements arrived with one level per spell. A Lightning mage gets Chain Lightning and
Thunderclap and then has nothing to grow into, while every base element's first spell levels to 10
with two forks. This gives Lightning's signature spell the same depth, and gives its bolt a proper
look: today it's ~100 server-sent dust particles per strike (one packet each), a dotted line rather
than lightning.

## The ladder

Lv 1 is the spell as it is: the bolt leaps from the caster's hand to the creature under the
crosshair (up to 20 blocks), then on to the nearest creature within 6 blocks of the last that the
caster may hurt, up to 3 jumps, each a fifth weaker. A wet creature conducts it: half again as much
damage, and two more jumps from it (never more than 6 jumps in all). No target: the cast fails and
costs nothing. 28 mana, 3.5 s.

| Lv | Tier | What changes |
|---|---|---|
| 1 | Chain Lightning | As above |
| 2 | Long Arc | It jumps up to 8 blocks, and the first strike reaches 26 |
| 3 | Static | Every strike jolts: the creature is stunned for half a second (slowed to a crawl and weakened, like Thunderclap's stun) |
| 4 | Forked Chain | One more jump (4), and each jump loses only a tenth |
| 5 | Fork | **Storm Fork:** every strike forks to the *two* nearest creatures not yet struck, so the chain spreads like a web (at most 10 strikes in all). **Overload:** the chain stops after 2 jumps, but every strike bursts: a 2.5-block shock around the creature hits everything else the caster may hurt there for half the strike |
| 6 | Conductor | Wet creatures take double and give 3 more jumps (at most 8 jumps in all). Rain already counts as wet, so a storm is a Lightning mage's weather |
| 7 | Thunderstruck | The first strike calls down a bolt from the sky as well: a real (harmless, fireless) lightning flash and thunder on the target, and that strike hits half again as hard |
| 8 | Live Wire | One more jump (5). Struck creatures stay charged for 3 s: every second, each arcs to the nearest other charged one within 4 blocks (1 damage × power to it), sparing those already struck that second while there are others |
| 9 | Supercharge | No falloff: every jump strikes as hard as the first |
| 10 | Capstone | **Ball Lightning:** the cast also looses a crackling orb that drifts the way you look (a third of a block a tick, 4 s, stopped by walls), striking a short, plain chain (2 jumps, 40% damage, no fork or burst) from itself every three quarters of a second at the nearest creature it can see within 6 blocks. With the orb, the cast needs no target. **Thunder Lord:** hold cast and the lightning keeps coming: the chain strikes again every half second while held (up to 3 s), at 60% damage, re-aimed at whatever is under the crosshair; each pulse after the first costs 6 mana, and the hold ends when the mana runs out |

Numbers worked through (power 1, dry): Lv 1: 5, 4, 3.2, 2.6. Lv 4: 5, 4.5, 4.05, 3.6, 3.3.
Lv 9: 5 every strike, up to 6 strikes (more with wet creatures).

Rules that hold at every level:
- The first strike may be aimed at anyone (like a projectile); jumps go only to creatures the caster
  may hurt (`SpellTargets.canAffect`), never back to one already struck, never to the caster.
- Every strike is Lightning damage (`hurtMultiHit`), so the element chart and Potency apply as
  before.
- Attuned Lightning creatures, lightning wisps, Thunderclap and the Wind Sovereign keep casting the
  Lv 1 chain.

## Looks and sounds

- **The bolt** is drawn by each client from one particle per strike (`ArcOptions`: from, to, a seed,
  how much of the start to skip): a jagged, blocky bolt of thin glowing boxes, a white core in a
  yellow glow, with a branch or two splitting off. It re-jags every tick and flickers out over 6
  ticks, like vanilla's lightning bolt but small. Electric sparks burst where it strikes. Thunderclap
  and the creatures' chains get the same bolt.
- **Sounds** stay vanilla: each strike cracks (the lightning impact, high-pitched); the cast rolls a
  short thunder. Thunderstruck brings the real thunder. Live Wire's arcs snap quietly (the
  amethyst's chime pitched up would be too sweet; a quiet high lightning impact instead).
- **Ball Lightning's orb**: a white-yellow glow with sparks crackling around it, humming (the
  beacon's ambient sound pitched up) while it lives.

## Code

| Piece | What it does |
|---|---|
| `api/ChainLightningRules` | The numbers by level and branch (tested) |
| `content/spell/ChainLightnings` | The chain (single, forking, overloading), Live Wire charges, Ball Lightning orbs, the Thunder Lord hold |
| `content/spell/ChainLightningSpell` | Levels, branches and routing; `chain`/`bolt` kept for other casters |
| `content/ArcOptions`, `client/particle/LightningArcParticle` | The client-drawn bolt |
| Tree | A `lightning_chain_lightning` path off Lightning's start, like the base elements' first spells |

## Testing

- `ChainLightningRulesTest`: jumps, reach, falloff, conduction and the branches' numbers by level.
- `tools/autotest/chain_lightning.txt`: rows of slowed husks; Lv 1, Lv 4, Storm Fork, Overload, Lv 7's
  sky bolt, Live Wire, Ball Lightning and Thunder Lord, each with health logged and screenshots.
- The server suite (`derived_elements` and the mob spell tests cast the Lv 1 chain).
