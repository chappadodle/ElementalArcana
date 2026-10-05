# Dawnbreak

## Why

Radiance is the element of light against the dark: Smite's pillars, Sanctuary's circle that sears
the undead. Its signature spell, the last of the derived elements' three (after Stormcall and the
Geode Sentinel), brings the day itself: at night, in a cave, in the Hollow, a mage of Radiance can
make it morning for a little while.

## The spell

- **Dawnbreak** (Radiance; Magic level 15; 70 mana; 35 seconds' cooldown; one level). Learned from
  a new branch of the Radiance tree (two small nodes off its start, then the spell).
- **The sun**: a small sun rises over the place you stand, to six blocks up over a second and a
  half, and shines there for 12 seconds, then sinks and fades. It's a block of a sun, as
  Minecraft's own is square: a glowing core turning slowly in a soft golden shell, rays turning
  round it, motes of light drifting down. With a dynamic lights mod it lights the ground too.
- **Its light**, once a second, on everything within 10 blocks of where it was called:
  - the undead you may hurt catch fire and take 2.5 times your power (Radiance);
  - every other foe takes 1 times your power and glows (seen through walls) while it's lit;
  - you and your allies are cleared of Darkness and Blindness.
- It stays where it rose (you can leave its light). One at a time: calling another puts the first
  out.
- Sounds: a beacon waking as it rises, its hum while it shines, and fading as it sets.

## Code

| Piece | What it does |
|---|---|
| `content/spell/DawnbreakSpell` | The spell |
| `content/spell/Dawnbreaks` | The suns: their light, once a second (server, never saved) |
| `content/DawnbreakOptions`, `client/particle/DawnSunParticle` | The sun drawn on each client from one particle |
| `tools/gen_skill_tree.py`, `tools/gen_textures.py` | The new branch; the icon |

## Testing

- `tools/autotest/dawnbreak.txt`: at midnight, a Radiance mage among two zombies (undead) and a
  spider (not), all of the mage's level: the sun rising and shining (shots); after a few seconds the
  zombies burning and hurt more than the spider, which glows (health and effects logged); the sun
  gone after.
