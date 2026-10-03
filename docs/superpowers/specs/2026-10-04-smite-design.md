# Smite, Lv 1–10

## Why

The last of the derived elements' first spells to stay at one level. Radiance is Fire's pure light:
it falls from the sky, judges the undead, and shelters its own. This gives Smite the same ladder as
Chain Lightning and Prism Bolt, and a proper look: today its pillar is ~100 server-sent particles a
strike (a packet each), and its mark a dozen more every other tick.

## The ladder

Lv 1 is the spell as it is: the ground where the caster looks (up to 24 blocks, or the feet of a
creature they look at) is marked with a ring of rising light; three quarters of a second later a
pillar of light falls on it: 7 damage within 2.5 blocks, half again as much for the undead, 3 seconds
alight and 5 glowing. The delay is the counterplay. 30 mana, 4 s.

| Lv | Tier | What changes |
|---|---|---|
| 1 | Smite | As above |
| 2 | Wide Judgment | The pillar strikes within 3.5 blocks |
| 3 | Swift Verdict | It falls after half a second |
| 4 | Consecration | Where it fell, the ground stays holy for 4 s: foes in it are set alight, the undead take 1 × power a second, and players in it (the caster too) are healed 1 a second |
| 5 | Fork | **Sunlance:** the pillar doesn't fade: for 3 s it follows the caster's aim across the ground (3 blocks a second), burning what it touches every half second for 40%. **Triple Judgment:** three narrower pillars (2 blocks) fall along the caster's line of sight, 4 blocks apart so they don't overlap, a quarter second after one another, the middle one on the mark |
| 6 | Purge | Foes the light strikes lose their good effects; players in it lose their bad ones |
| 7 | Holy Fire | The undead take double, and the burning lasts 6 s |
| 8 | Halo | Every cast gives the caster Regeneration I for 4 s |
| 9 | Radiant Burst | Where the pillar lands, foes are thrown back from its middle and blinded for 2 s |
| 10 | Capstone | **Wrath of Heaven:** six more, smaller pillars rain down within 6 blocks of the mark over 2 s (1.5 blocks wide, 60%). **Avatar of Light:** for 8 s the caster shines: every second a small pillar falls on the nearest foe within 12 blocks (60%), and they take a fifth less damage |

Attuned Radiance creatures and radiance wisps keep casting the Lv 1 spell.

## Looks and sounds

Drawn by each client from one particle (`SmiteOptions`: what it is, how wide, how long):
- **The mark:** a ring of golden light on the ground with short shafts of light rising from it,
  brightening until the pillar falls.
- **The pillar:** a column of white-gold light, a bright core in a wide soft glow, that drops out
  of the sky in two ticks, flashes where it lands and narrows away over half a second. Blocky, like
  a beacon's beam.
- **Sunlance's beam** is the pillar kept lit, sliding across the ground.
- Sounds stay vanilla: the beacon's power-up as it's marked, the trident's thunder and the beacon's
  power-down as it falls, the amethyst's chime for a Halo.

## Code

| Piece | What it does |
|---|---|
| `api/SmiteRules` | The numbers by level and fork (tested) |
| `content/spell/Smites` | Strikes, consecrated ground, Sunlances, Wrath's rain, Avatars (server, never saved) |
| `content/spell/SmiteSpell` | Levels, forks and routing |
| `content/SmiteOptions`, `client/particle/SmiteParticle` | The mark, the pillar and the beam |
| Tree | A `radiance_smite` path off Radiance's start |

## Testing

- `SmiteRulesTest`: the numbers by level and fork.
- `tools/autotest/smite.txt`: husks and zombies (the undead) in rows and blocks; Lv 1, Wide
  Judgment, Consecration (a player standing in it healed), both forks, Radiant Burst, both
  capstones; health logged, shots of the mark, the pillar and the beam.
- The server suite (`derived_elements` and the mob tests cast the Lv 1 spell).
