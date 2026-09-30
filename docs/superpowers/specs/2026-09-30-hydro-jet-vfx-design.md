# Hydro Jet visual and sound effects: design

Date: 2026-09-30
Status: direction approved in conversation. It's built in the same four steps as the Fireball,
Icicle and Wind Blade passes, and reuses their systems.

## Direction

**A rush of little water blocks, Minecraft style.** The stream used to be loose droplets filling
the line from the hand to the target each tick. Now it's drawn as a flow of small tumbling cubes
of water rushing from the palm to the impact point.
- Two earlier tries didn't look right. A continuous tube of water, then smooth 3D water balls,
  both looked too realistic next to Minecraft's blocky pixel style.
- Each cube wears the game's own animated water texture (`minecraft:block/water_still`), tinted
  per look the way biomes tint water.
- It's only the look: the stream still hits along its whole line at once, so no upgrade changed.
- The cubes emerge small at the palm, grow, and spread slightly as they go. Each keeps its own
  size, tumble and patch of the texture as it travels, so the flow reads as moving water.
- It keeps the existing hand re-aiming for your own jet, so it never lags the crosshair.
- **Pressure Build (Lv 6+):** the cubes grow bigger and glow brighter from inside the longer you
  spray.
- **Branches:** Tidecutter is tiny, fast, tightly packed cubes; Torrent is big, pale, foamy cubes
  spraying out in a wide cone; Maelstrom's cubes leave the hand straight like the others, then wind into a spiral from about 1.5 blocks out (at full width by 4); spiralling right out of the hand looked bad.

**The pressure rises with the level, and each branch has its own look.**

| Look | When | Style |
|---|---|---|
| Spring | Lv 1–2 (and mobs) | a clear, light-blue stream with little bubbles |
| Current | Lv 3–4 | bluer, with foam starting to show |
| Surge | Lv 6–7 (and Lv 5 with no branch yet) | deep blue with a white foam core, flowing faster |
| Deluge | Lv 8–9 | dark ocean blue with a glowing cyan core (bloom) |
| Tidecutter | Lv 5+ Tidecutter | a razor-thin, almost white high-pressure line with a glint |
| Torrent | Lv 5+ Torrent | a wide, churning white-water fire-hose |
| Maelstrom | Lv 10 Maelstrom | a teal stream twisting in a spiral |
| Tsunami Lance | Lv 10 Tsunami Lance | a deep ocean-blue stream; its spear becomes a new 3D water spear |

**Priority:** the Lv 10 capstone, then the Lv 5 branch, then the tier. Lv 8+ brightens every look.

**Dynamic lights:** water doesn't glow, and the beam isn't an entity (dynamic lights follow entities), so only the Tsunami Lance spear gives a faint light.

## Steps

1. **Looks:** the beam renderer, its textures, the 8 looks and Pressure Build.
2. **Particles:**
   - A swirl at the palm while spraying.
   - Droplets and mist peeling off the beam.
   - Spray under your feet when riding the jet (Recoil, Lv 8).
   - Branch signatures.
3. **Impacts:**
   - A 3D splash crown and a foam ring where the beam lands.
   - Wet marks on the blocks that darken, then dry.
   - Maelstrom: a 3D whirlpool.
   - Tsunami Lance: a 3D water spear and a crashing wave ring where it lands.
4. **Sounds:**
   - A rushing-water loop following the beam (pitched up with pressure), and a splashing loop at
     the impact point.
   - Vanilla water mixes per look.
   - ★ signature sounds for Tsunami Lance and Maelstrom, with the A/B switch.

## Step 1 structure

- **One packet per tick:** the three simple stream particles (normal, thin, wide) become one
  `HYDRO_STREAM` particle with `HydroStreamOptions(caster id, look with brightness, pressure)`. Its
  velocity is still the line from the hand to where the stream lands.
  - Look: the Lv 10 capstone, then the Lv 5 branch, then the tier, plus a `BRIGHT` bit at Lv 8+.
  - Pressure (0–1): the Pressure Build bonus, 0 below Lv 6.
- **`client/visual/WaterBeams`** keeps one stream per caster, refreshed by each packet: its hand
  point, line, look and pressure. A stream that isn't refreshed for 3 ticks is gone.
  - Your own stream is re-aimed from your hand and view every frame, as before.
  - It's drawn after the translucent blocks, as cubes spaced 2.6 half-edges apart and moving at a
    speed set by the look's flow. At most 60 per stream.
    - A cube's size, tumble axis and texture patch come from its number in the stream, so it keeps
      them as it goes. Its drift off the line grows toward the far end (the look's spread);
      Maelstrom's cubes circle the line.
    - Each cube uses the vanilla water sprite from the block atlas: a half-width patch per cube,
      tinted, and drawn with the translucent block sheet (lit by the world, never darker than a
      block light of 10).
  - Looks with a glow (Surge and up, Tidecutter, Maelstrom, Lance; stronger with pressure and
    at Lv 8+) also draw an additive inner cube, and Lv 8+ blooms it.
- **`HydroStreamEmitter`** now just updates the beam and keeps the splash where the stream lands
  (the droplets come back in step 2, sparser, peeling off the beam).

## Step 2 structure (particles)

- **`HydroJetEffects.stream`** is called by `HydroStreamEmitter` each tick of a stream. Your own
  stream is re-aimed from your hand and view, like its cubes. It uses vanilla-style pixel water:
  - **At the palm:** droplets swirling out and a splash (more for Torrent).
  - **Along the stream:** vanilla falling-water drips peeling off, 1 to 4 a tick by look.
  - **Tidecutter:** fine glints and mist along the line.
  - **Torrent:** heavy splashes and spray off the sides.
  - **Maelstrom:** droplets flung off the spiral once it has wound out.
  - **Tsunami Lance:** nautilus swirls, like a conduit's.
  - **Lv 8+:** cyan glints.
  - **Recoil (Lv 8+, aiming down):** droplets and a splash ringing out around your feet.
