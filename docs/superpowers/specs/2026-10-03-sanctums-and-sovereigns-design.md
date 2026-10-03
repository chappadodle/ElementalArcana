# Sanctums and the Sovereigns

Date: 2026-10-03
Status: built (milestone 11, first half, of `2026-10-02-fantasy-adventure-roadmap.md`; the Hollow
itself is the second half).

The story says four Sovereigns keep the seals that bind the Hollow, one for each base element, and
that the seals are fading. This milestone puts them in the world: a sanctum for each, a fight worth
travelling for, and what their hearts unlock.

## The four Sovereigns

| Element | Sovereign | Sanctum | Where |
|---|---|---|---|
| Fire | **Vulkhar**, Sovereign of Flame | the Caldera | badlands, deserts, savannas |
| Water | **Thalassa**, Sovereign of Tides | the Tidehall | beaches, swamps, mangroves, shallow coasts |
| Wind | **Caelum**, Sovereign of Storms | the Aerie | mountains and windswept hills |
| Earth | **Orvald**, Sovereign of Stone | the Deepvault | forests, taiga, plains |

## Sanctums

One structure set holds the four sanctums (a candidate every 40 chunks, 20 apart); in each cell
the game tries them in random order until one fits the land, so each element's sanctum turns up
every thousand blocks or so. None stands within 1500 blocks of the world's centre.

A sanctum is an open-air arena 39 blocks across in its element's stone (the mage towers'
palettes), built procedurally (`content/sanctum/SanctumPiece`):

- a round floor with rings and eight spokes of trim, on a foundation down to the ground;
- a wall around it (6 high, 2 for the Aerie, 8 for the Deepvault) with four gates;
- eight pillars topped with light;
- in the middle a stepped dais, four obelisks, and on top **the Seal**; behind it the reward
  chest (`chests/sanctum`).

Each element adds its own ground. The Caldera has four lava pits between the pillars and magma in
its rings. The Tidehall has water pools and channels from the dais to the gates, and is built up
from shallow water where it must be. The Aerie has a low wall, tall slender pillars, and room to be
thrown. The Deepvault has thick walls crowned with amethyst, and moss and geodes on the floor.

## The Seal

`sanctum_seal`: unbreakable, one per sanctum, with a kind (its element) and a state:

- **sealed:** dimly lit. When someone (not a spectator) comes within 10 blocks, the Sovereign
  rises out of it. Everyone within 48 blocks sees its name as a title, and the seal turns
  **awake**.
- **awake:** while the Sovereign lives. A Sovereign leashed to its seal never wanders more than
  16 blocks from it. If no one fights it for 20 seconds, it drifts back and heals to full.
- **restored:** when the Sovereign falls the seal blazes with its element's light ("The seal of
  Flame holds again"). That is permanent: a sanctum is won once.

A Sovereign that vanishes without falling (removed by a command) leaves its seal sealed again after
half a minute. Operators can seal the sanctums near them again with `/arcana sanctum reset` (its
Sovereign, if out, is removed), so a fight can be had again or tested.

## The Sovereign

One entity class (`SovereignEntity`), one type per element (`<element>_sovereign`). It is a
colossal floating mask over a core of its element, about 3 blocks tall. Its hands rise when it
casts, a crown of shards hovers over it, and plates of its element orbit it. It flies (no
gravity), hovers about 8 blocks from its target and 3 above it, and turns its mask to face them.

- **Level:** an Archmage (+20 levels) on top of its zone's level, never less than 30 before the
  bonus, so the weakest Sovereign is level 50. Health 320 before Vitality, armour 10, no
  knockback. Fire's is fire-immune; none takes fall damage or drowns.
- **Boss bar:** "Vulkhar, Sovereign of Flame" in its element's colour, within 48 blocks; it
  darkens the sky like the Wither's.
- **Spells:** every spell its element's Archmages know, cast faster (60% cooldowns, a quicker
  wind-up and gap), plus two of its own (`SovereignSpells`, power 1.0 against the creatures'
  0.6):

| Sovereign | Signature (from the start) | Second (from half health) |
|---|---|---|
| Vulkhar | **Rain of Embers:** six meteors on and around its target | **Inferno:** three rings of burning ground roll out from beneath it |
| Thalassa | **Tidal Surge:** a wave that hurts, soaks and throws back everyone within 12 blocks | **Drowning Bubble:** traps its target in a bubble, then jets it |
| Caelum | **Gale Volley:** five wind blades fanned at its target | **Tempest:** drags everyone within 14 blocks toward it, then Chain Lightning strikes them |
| Orvald | **Quake:** three tremors roll out from beneath it | **Bulwark:** three Earth wisps rise; while any lives it takes 75% less damage |

- **Half health:** it cries out, calls two wisps of its element and its eyes burn brighter (a
  second texture); the second signature opens up.
- **When it falls:** the seal is restored, and it leaves a **Sovereign Heart** of its element, its
  loot (diamonds, its Essence, experience bottles, a Tome of Insight) and Archmage XP.

The rules that are plain numbers (phases, levels, cooldown scaling) are in `api/SovereignRules`,
unit tested.

## Sovereign Heart and archmage gear

The heart (`sovereign_heart`, its element in the `elementalarcana:element` component, tinted like
a Guardian Core) is the key to the archmage tier (level 50):

| Item | Recipe | Gives |
|---|---|---|
| Archmage Staff | a Master Staff, a Sovereign Heart of its element's family, 2 Essence and 2 netherite scrap | +10 Potency, +6 Focus, +15 Affinity |
| Archmage's hood / robe / trousers / boots | the master piece, a Sovereign Heart of any element and 2 netherite scrap | Insight +5 Reservoir +5 / Reservoir +12 Ward +8 / Focus +9 Ward +4 / Vitality +5 Reservoir +5 |

Archmage robes are white and gold with a prismatic trim (armour 3, 6, 8, 3, toughness 2).

## Finding them

- Master Arcanists sell a **Sanctum Map** (24 emeralds and a compass) to the nearest sanctum of an
  element (an explorer map with a red X).
- Two journal pages: Sanctums (where, and the maps) and Sovereigns (the fight, the heart).

## Testing

- Unit: `SovereignRulesTest`.
- Server: `tools/server_tests/sovereigns.txt`: each Sovereign summons at level 50 (437.9 health),
  half health calls two wisps each (8), and their deaths drop 4 Hearts.
- In game (`tools/autotest/sanctums.txt`): each sanctum found with `goto` far out on fresh land
  (around 30000, 30000, where the Sovereigns are level 57 to 60), its seal reset, then the fight:
  the Sovereign rises with its title, casts, enters its second phase (its cry, its wisps, and for
  Orvald the Bulwark's three), falls, restores the seal and leaves its Heart.
