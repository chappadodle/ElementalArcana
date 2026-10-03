# Gale Dash rework

Date: 2026-10-03
Status: built (milestone 12, step 3 of the approved wind plan: "Gale Dash rework: multiple charges
(the framework needs per-spell charges: a small extension to cooldowns), i-frames, all tiers").

Gale Dash was Wind's first sketch of movement: one burst forward. Now it's the movement spell a
wind mage leans on, with several dashes held at once and ten levels.

## The spell

**Gale Dash** (Wind, level 1, 20 mana, 3 s a charge). Tap R: a burst of wind throws you the way you
look (on the ground always with a little lift), shoving away foes within 3 blocks of where you
started; your landing never hurts. A trail of wind follows you.

| Lv | Name | What changes |
|---|---|---|
| 1 | Gale Dash | as above |
| 2 | Second Wind | two dashes held at once, each coming back a cooldown after the last |
| 3 | Air Step | in the air you dash any way you look (even down), and nothing hurts you while you dash (a command's kill still does) |
| 4 | Slipstream Trail | the dash shoves foes it passes aside and quickens the players it passes (Speed) |
| 5 | **Phantom Step** | for a second after a dash you're invisible, and whatever was hunting you loses you and can't find you again until it ends |
| 5 | **Gale Strike** | foes the dash passes through take 4 × your power Wind damage and are thrown Airborne |
| 6 | Tailwind | a burst of speed after each dash, and a quarter off its cooldown |
| 7 | Triple Charge | three dashes held |
| 8 | Featherfall | after a dash in the air you drift down slowly, so you can chain dashes |
| 9 | Swirling Rush | dashing through a foe that carries an element Swirls it |
| 10 | **Blink Storm** | the dash becomes a blink of up to 8 blocks (as far as there's room and sight), with a gust at either end that hurts (3 × power) and throws back foes |
| 10 | **Hurricane Rush** | hold cast to keep rushing the way you look for up to 2 seconds, hitting everything you pass (3 × power, Airborne) |

## Charges

Spells may now hold several uses (`Spell#charges`). There's still one cooldown clock per spell;
with charges it reads how long until every charge is back, each taking a full cooldown, and a
use adds a cooldown to it (`api/SpellCharges`, unit tested). With one charge that's the ordinary
cooldown, so nothing else changes. The HUD shows how many are ready beside the spell's icon, the
icon's shade is the charge coming back, and the seconds show only while none is ready; the spell
wheel greys a spell only when it has none.

## How it's built

- `api/GaleDashRules` (unit tested): the numbers by level and branch.
- `content/spell/GaleDashSpell`: the spell, its levels, charges and Tailwind's cooldown;
  `GaleDashes`: the dash, the blink and the rush (a hold), and while a dash lasts (8 ticks, or the
  rush) its trail, guard, shoves, strikes and Swirls; Phantom Step's hiding (invisibility, targets
  dropped, new targeting refused); Tailwind and Featherfall when it ends.
- Sounds are vanilla: a wind charge's burst and the breeze's slide for the dash, the breeze's
  wind-charge burst for the blink's gusts, the breeze's whirl while rushing, a sweep for Gale
  Strike's hits.
- The tree: Gale Dash's node gets its chain of levels, heading east (Lightning's cluster leaves it
  to the north-east).

## Testing

- Unit: `SpellChargesTest`, `GaleDashRulesTest`.
- In game (`tools/autotest/gale_dash.txt`): at level 2 two dashes go out back to back (8 and 17
  blocks east) and a third is refused; the HUD counts 1, then 0. At level 3 a hit landing mid-dash
  in the air does nothing, nor does the landing. Gale Strike takes 15 to 21 from each of three
  husks it passes; Blink Storm blinks 8.8 blocks and hurts husks at both ends; Hurricane Rush,
  held a second and a half, carries the player 37 blocks through a line of husks, hitting all
  three; Phantom Step leaves the player invisible.
