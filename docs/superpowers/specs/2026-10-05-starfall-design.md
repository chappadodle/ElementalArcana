# Starfall

## Why

The world's events so far come to you (rifts, mana tides) or wait in place (structures). A fantasy
night sky should now and then send you running: a star falls, you see where, and whoever gets
there first finds what it brought. Fallen stars belong to Radiance, the rarest element, and give
the night a reason to go out into it.

## The event

- Some nights (one in three, rolled at dusk for each awakened player in the Overworld), a star
  falls 120 to 260 blocks away, at a random moment of the night.
- Everyone within 400 blocks sees it: a bright streak across the sky down to the impact, and a
  boom when it lands. Awakened players nearby get a line: "A star falls to the east!" (its
  direction from them).
- **The crater**: a scorched bowl a few blocks across (blackstone, magma, basalt), and at its
  heart a **Fallen Star** block that glows (light 15) and hums. A **pillar of light** stands over
  it until dawn, seen from far off (like a beacon's, in pale gold).
- Two or three **Radiance wisps** gather round the star (Attuned, of course).
- Mining the Fallen Star (any pickaxe) gives 2 to 4 **Star Fragments** and some Radiance Essence;
  the pillar goes out. At dawn an unmined star cools into **Starstone** (one fragment).

## Star Fragments

- **Starlit Lantern** (4 fragments, an iron ingot, a glass pane): a light (15); no hostile
  monster can spawn within 24 blocks of it. (A mage's way to keep a home safe.)
- **Wishing Star** (2 fragments, a Radiance Essence): used, it fills your mana and health and
  ends your cooldowns, once.
- A trade good: Arcanists buy fragments for emeralds.

## Code

| Piece | What it does |
|---|---|
| `api/StarfallRules` | Chances, distances, times (tested) |
| `content/star/Starfalls` | The nightly roll, the fall, the crater, the wisps; the pillars' list synced to clients |
| `content/star/FallenStarBlock`, `StarstoneBlock` | The star and what it cools into |
| `content/star/StarlitLanternBlock` | The light that keeps monsters from spawning |
| `content/star/WishingStarItem` | The wish |
| `network/StarfallPayload` | Falls and pillars to clients |
| `client/StarfallClient` | The streak across the sky and the pillar of light |
| `tools/gen_star.py` | Textures, models, loot, recipes |

## Testing

- `StarfallRulesTest`.
- `tools/server_tests/starfall.txt`: a star made to fall by command near a point (the crater, the
  star block, the wisps), mined (its drops), a lantern stopping a spawn.
- `tools/autotest/starfall.txt`: a star falling at night (shots of the streak, the impact, the
  pillar from afar), the crater up close, mining the star.
