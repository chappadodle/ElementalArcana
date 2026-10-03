# Bubble Prison visual and sound effects: design

Date: 2026-09-30
Status: direction approved in conversation. Minecraft style, like the Hydro Jet pass, whose water
cubes it reuses. Bubble Prison has one level and no branches, so this pass has two steps.

## Direction

**A voxel bubble.** The bubble becomes a hollow sphere built of small see-through water cubes (the
vanilla water texture, bright water blue), sized to the trapped creature:
- It turns slowly, wobbles (squashing and stretching out of step on each axis) and shimmers.
- A few cubes near the top catch a white shine, like a highlight on a bubble.
- Tiny pale cubes float up inside it.

**Its moments:**
- **Cast:** a quick line of little water cubes zips from the caster's hand to the target,
  replacing the droplet trail.
- **Forming:** the bubble's cubes rush in from all around and snap into the sphere in about a
  third of a second.
- **Popping:** when it ends, whether popped or run out, its cubes burst outward as tumbling water
  cubes that splash when they land, and it leaves a wet mark on the ground below.

## Steps

1. **The bubble and its moments** (looks, cast, forming, popping).
2. **Sounds** (built 2026-10-03 with vanilla sounds only, as the user prefers simple vanilla
   sounds to synthesized ones):
   - A bubbly "bloop" as it forms (a bubble column's inside and a bottle filling), and a gurgling
     loop while it holds (a bubble column's ambience, played by each client and following the
     bubble until it pops, `BubbleRenderer`).
   - A pop with a splash when it bursts: a full splash when a hit bursts it, a light one when it
     runs out (`BubblePrisons#release`).
   - A watery whoosh on the cast (a swim splash and a high riptide, `BubbleCastEmitter`).

## Step 1 structure

- **`BubbleRenderer`** draws the voxel bubble in the trapped creature's render pass.
  - The shell's cubes are the points of a 10×10×10 grid lying within half a cell of a sphere of
    radius 5. That's worked out once and scaled to the creature: the bubble is 1.3 times the
    creature's larger side.
  - Each cube uses `WaterCubes` with the translucent block sheet.
  - Forming: for the first 6 ticks of the bubble (from `Bubble#startTick`), the cubes come in from
    3.5 times the radius, growing as they arrive.
  - Shine: cubes on the upper front of the sphere are drawn near-white.
  - Rising cubes: 5 small pale cubes drift up through the inside and wrap round.
- **Popping:** `BubbleRenderer` remembers each bubble it draws. Every client tick, a bubble whose
  creature is no longer trapped (or is gone) bursts: about 40 `WaterCubeParticle`s flying outward
  from its shell, splashes, and a `WET` mark on the ground below. The old block model and its
  texture are removed.
- **Cast:** `BubblePrisonSpell` sends one `BUBBLE_CAST` particle, whose velocity is the line from
  the caster's eyes to the target. `BubbleCastEmitter` sends 8 little water cubes zipping along it,
  with no gravity, gone by the end of it.
