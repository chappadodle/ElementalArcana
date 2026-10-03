# Mana sense and mana weather

Date: 2026-10-03
Status: built (milestone 12, first step, of `2026-10-02-fantasy-adventure-roadmap.md`; it builds
ideas 2 and 3 of `2026-10-01-progression-system-design.md` and a first taste of idea 4).

Magic so far lives in the player. These make it something felt in the world: you sense the creatures
around you, and the air itself is richer or poorer in mana.

## Mana sense

**Reading auras.** Looking at a creature within 24 blocks shows a short line under the crosshair,
as much as your Insight lets you read:

| Insight | You read |
|---|---|
| under 5 | only how dangerous it feels: a mark in the danger colour |
| 5 to 14 | its level ("Lv 34") |
| 15 and up | its level and its element ("Lv 34 · Fire", in the element's colour) |

The danger colour compares its level with yours: green at 5 or more below, gold within 4, red at 5
to 9 above, violet at 10 or more above. (Jade, if installed, still shows everything.)

**Pressure.** A creature 15 or more levels above you, within 12 blocks, weighs on you: the
**Pressure** effect, stronger the wider the gap (one rank per 10 levels past 15, three at most).
Each rank costs 12% of your spell power and 20% of your mana regeneration. Ward pushes back: every
4 points of it count as a level less of gap.

**Hiding your aura.** A key (H) hides your aura, or shows it again. Hidden, creatures notice you at
half the distance (as if you were sneaking twice over) and your mana regenerates at half speed. The
mana bar says so.

## Mana weather

Mana regeneration is multiplied by the mana in the air where you stand, worked out once a second
(`api/ManaWeatherRules`, unit tested):

- **Rich lands:** the high-level zones are rich: +1% per zone level above 10, at most +40%.
- **Dead zones:** the deep dark drinks mana (×0.25); so does the Hollow. Biome tag
  `elementalarcana:dead_zones`.
- **Comfort:** the lands of an element you hold quicken you (×1.25, the `attunes/<element>` biome
  tags); the lands of an element opposed to yours (fire against water and ice) slow you (×0.8).
- **The tide:** ×1.5 while a mana tide runs.

When the air isn't ordinary the mana bar shows the multiplier (×1.4 in rich land, ×0.3 in the dark).

## Mana tides

Every 4 to 8 days (the first after 3 to 6), at nightfall, a **mana tide** rises and runs until dawn:
"A mana tide rises. The air hums with power." While it runs:

- mana regenerates half again as fast everywhere (see above);
- a sleeping player's daily chance to awaken doubles;
- creatures are twice as likely to spawn Attuned, and wisps come twice as often;
- creatures that spawn are 5 levels stronger;
- the sky's fog turns violet, and motes of mana drift up around players.

The tide is kept with the world (`ManaTides`, saved data), and clients are told when it turns.
Operators can force it: `/arcana tide start|stop`.

## Testing

- Unit: `ManaWeatherRulesTest` (rich, dead, comfort and the tide combined; pressure ranks and Ward).
- Server: `tools/server_tests/mana_weather.txt`: the tide starts and stops by command, and its
  messages go out.
- In game (`tools/autotest/mana_sense.txt`): the readout under the crosshair at three Insight
  levels, Pressure from an Archmage, the hidden aura on the mana bar, a tide's violet sky.
