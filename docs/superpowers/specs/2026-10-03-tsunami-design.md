# Tsunami

Date: 2026-10-03
Status: built (milestone 12, the paused Water move of `2026-09-28-elements-and-creature-magic-design.md`:
"Tsunami also removes Tidal Wave").

Tidal Wave was Water's first sketch: a cone of splashes that shoves what's in front of you. Tsunami
is the move it was standing in for: a real wave that rises at your feet and rolls away from you,
sweeping up whatever it meets.

## The spell

**Tsunami** (Water, level 6, 40 mana, 12 s). Tap R: a wall of water rises at your feet and rolls
forward the way you face (level with the ground, whatever your pitch): 16 blocks in 1.6 seconds
(half a block a tick), 5 blocks wide, about 2.5 tall in the middle and lower at its edges, its top
curling forward.

- **It follows the land.** It climbs steps of a block, pours down drops of up to 3, and breaks
  early against anything taller (a wall of 2 or more blocks across its middle). Where it can't go
  even two blocks, it doesn't rise ("No room for a wave to rise").
- **It sweeps.** Whatever you may hurt that the wave reaches takes 4 × your power Water damage, once
  (the element chart applies: Fire creatures take more), and is carried along with it, riding its
  face, until it breaks. Knockback resistance holds against it (a Sovereign barely moves). You and
  those you can't hurt are left standing.
- **It drowns fire.** Burning creatures are put out, and so are fires along its path.
- **It breaks.** Where it ends it crashes down in a ring of water (the Tsunami Lance's crash) and
  throws everything it carried a few blocks on.

Tsunami keeps Tidal Wave's id (`tidal_wave`) and its node next to Water's start, so anyone who
learned Tidal Wave has Tsunami now (castable from level 6).

## How it's built

- `api/TsunamiRules` (unit tested): the wave's path. Given what's solid along the line it rolls
  (a lookup, so the rules don't touch the world), the height it rolls at for each half block of the
  way, and where it breaks: it climbs one block, falls up to three, stops at two-block walls, never
  goes past 16.
- `content/spell/TsunamiSpell` and `Tsunamis` (like Pyronado's wheels, server-side and never saved):
  works out the path at the cast, then each tick moves the front, sweeps and carries what's in it,
  puts out fires, and plays the crash. Sounds are vanilla: a high splash and an emptying bucket
  when it rises, splashes along its way, a deep splash when it breaks.
- `content/TsunamiOptions` + `client/particle/TsunamiParticle`: the server sends the wave as one
  particle (where it starts, the way it goes, its length and the heights along it), and each client
  draws it every frame out of the Hydro Jet's little water cubes (`WaterCubes`): a stack of cubes
  for each of a dozen columns across the front, standing on the ground under that column, deep blue
  below and lighter above, the top cubes leaning forward into the curl, pale foam cubes along the
  crest. It rises over its first few ticks and slumps as it breaks, throwing spray (splashes and
  bubbles) off its crest the whole way.
- The icon: a curling pixel wave.

## Testing

- Unit: `TsunamiRulesTest`: flat ground runs the full 16; a step of one is climbed; a drop of
  three is followed and one of four ends it; a two-block wall ends it; a wall right in front leaves
  no room; a lake is ridden like ground.
- In game (`tools/autotest/tsunami.txt`): three husks in its way (slowed so they can't walk) are
  swept from 5 to 10 blocks out to about 23, up a step, and hurt; a fire in its path goes out;
  shots from the side (rising, rolling, climbing, the crash) and as its caster sees it.
