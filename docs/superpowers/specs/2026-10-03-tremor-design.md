# Tremor: a shockwave that runs

## Why

Tremor was instant: all ten blocks of the shockwave were hit on the tick it was cast, with a spray
of vanilla block crumbs along the line. It didn't read as a wave, there was nothing to dodge, and
an Attuned Earth Magus's Tremor landed before anyone could react to it. This pass makes it travel
and gives it the look and sound Earth has been missing, in the Minecraft style of the Fireball,
Icicle and Tsunami passes.

## The shockwave

- The caster stamps the ground and the front runs from under their feet the way they face, **a
  block a tick for 10 blocks** (half a second).
- It follows the ground (`api/GroundPath`, shared with Tsunami): it climbs a step of one block,
  follows a drop of up to two, and ends against anything taller or deeper. The path is worked out
  once, when it starts (`api/TremorRules`).
- Whatever the caster may hurt (`SpellTargets.canAffect`, as for Tsunami) that stands where the
  front passes is hit **once**, the moment the front reaches it: 3 × power Earth damage,
  Crystallize as before, thrown up (by 0.6, less knockback resistance) and Slowness III for 3 s.
  "Where the front passes": within 0.6 blocks of it along its way and 1.6 across, plus half the
  creature's width, with its feet no more than a block above the ground there. **A creature in
  mid-jump escapes it**, and anything off its line is missed. Attuned Earth Magi's Tremors run
  the same way, so they can be dodged too.
- No ground under the caster (more than two blocks up, or flying): it fails with "No ground under
  you for a Tremor to run along" and costs nothing. A wall right ahead fails the same way.

## Looks and sounds

Drawn on each client from one particle sent when it starts, with the path (`TremorOptions`,
`TremorEmitter`); the hits are the server's (`Tremors`).

- **The stamp:** a ring of dust pillars of the ground block around the caster's feet, and the
  mace's heavy ground smash.
- **The front**, each block: chunks of whatever ground it crosses (3D cubes of the block's own
  texture, like the Fireball's debris) thrown up and forward, and a few puffs of the mace's dust
  pillars, held to a block or so high. Every other block, the ground block's own break sound.
- **Rock spikes:** every other block, a spike of rock heaves up out of the ground on alternating
  sides, leaning the way the wave runs (and a little outward), taller the further it gets
  (0.65 to 1.4 blocks). A spike is a stack of three stone cubes in the Boulder's rock texture,
  each smaller than the one below and turned a little: blocky, like Minecraft's own pointed
  dripstone. It shoots up in 3 ticks, stands about a second, then sinks back into the ground,
  shedding a few chips of stone. Each spike cracks like basalt breaking.
- **The end:** where the path ends (after 10 blocks, or against a wall), three tall spikes
  (1.4 to 1.8 blocks) burst up together in a fan, with more debris, the mace's ground smash and
  deepslate breaking.

## Code

| Piece | What it does |
|---|---|
| `api/GroundPath` | A straight path along the ground: climbs, drops, ends at walls (Tsunami's, made general) |
| `api/TremorRules` | Tremor's path and who the front catches (tested) |
| `content/spell/Tremors` | The running shockwaves: hits a block a tick, never saved |
| `content/spell/TremorSpell` | The spell; fails without ground |
| `content/TremorOptions` | The one particle a shockwave is sent as, with its path |
| `client/particle/TremorEmitter` | The stamp, the front, the spikes and the sounds |
| `client/particle/StoneSpikeParticle` | A spike of stacked stone cubes |
| `client/particle/BlockMesh` | A textured, block-shaded cube (shared with `DebrisParticle`) |

## Testing

- `TremorRulesTest`: the path climbs a step, follows a drop of two, ends at a wall of two or a
  drop of three; the front catches what stands where it passes, not what's off its line or in
  mid-jump. `TsunamiRulesTest` still passes on the shared path.
- `tools/autotest/tremor.txt`: a Tremor up a step past three husks, with a fourth off to the side,
  watched from the side; the three are hit and thrown, the fourth keeps its health. Then one seen
  as its caster sees it.
- The server suite (`mobspells_earth` casts an Attuned Magus's Tremor).
