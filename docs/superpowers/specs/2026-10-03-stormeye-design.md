# Stormeye

Date: 2026-10-03
Status: built (milestone 12, step 5 of the approved wind plan: "Stormeye: a tornado entity... with
pull, lift and all tiers").

Wind's last move and its "ultimate": a tornado you set down where you look, that drags in whatever
you may hurt, lifts it and spins it, and takes on the elements it touches.

## The spell

**Stormeye** (Wind, level 8, 50 mana, 20 s). Tap R: a tornado rises on the ground at the point
you look at (up to 20 blocks away, or as far as you can see) and spins for 4 seconds. Whatever you
may hurt within 5 blocks of its eye is dragged toward it, lifted off the ground and carried round
it (Airborne), and takes 1.5 × your power Wind damage every half second. When it ends, what it
held drops out. It's a funnel of wind 7 blocks tall, narrow at the ground and wide at the top,
turning fast, with bits of the ground it stands on (the block's own pieces) spinning up through it,
and it whirls like a breeze.

It levels to 10 like the other leveled spells, with a choice at 5 and at 10:

| Lv | Name | What changes |
|---|---|---|
| 1 | Stormeye | as above |
| 2 | Gale Force | a stronger pull, and 5 seconds |
| 3 | Absorption | the first burning, frozen or frosted, or wet creature it takes gives it that element: it turns orange, icy or deep blue, and everything it holds catches it (set alight, frosted, soaked) |
| 4 | Wider Eye | it reaches 7 blocks |
| 5 | **Wandering Storm** | it drifts toward where you look, as long as it lasts |
| 5 | **Twin Storms** | two smaller tornadoes (4 blocks each) circling each other |
| 6 | Crushing Winds | half again the damage, and when it ends it flings what it held outward, hard |
| 7 | Eye Pulse | every second it pulses: what it holds is Swirled (its element spreads to what's near it) |
| 8 | Scavenger Wind | 7 seconds, and it sends dropped items and experience to you |
| 9 | Eye of the Storm | Airborne creatures it holds take double damage from everything |
| 10 | **Great Tempest** | twice the size and reach, and projectiles that fly into it are thrown back out |
| 10 | **Eye of Calm** | it rises around you and moves with you: projectiles aimed at you are thrown aside, and you and your allies inside it mend (a heart every 2 seconds) |

## How it's built

- `api/StormeyeRules` (unit tested): the numbers by level and branch (reach, pull, lift, damage,
  how long it lasts, Twin Storms' and Great Tempest's sizes).
- `content/spell/StormeyeEntity`: the tornado is an entity (no physics, never saved), so it moves
  smoothly for everyone (Wandering Storm, Eye of Calm, Twin Storms' circling) and its element and
  size are synced with it. Each tick on the server it pulls and lifts what it holds (knockback
  resistance holds against it), hurts it on the half second, and does whatever its level adds.
- `client/StormeyeRenderer`: a funnel drawn like Tempest Edge's (`WindFunnelParticle`), with its
  texture, grown to the tornado's size, swaying and twisting, drawn additively from both sides and
  tinted by its element; the entity itself throws the ground's block pieces and wind curls round
  it on the client.
- Sounds are vanilla: the breeze's whirl, over and over while it spins, and a wind charge's burst
  when it rises and when it ends.
- The tree: a new arm off Wind's start (Wind Affinity, Focus, then the spell), heading west, its
  chain of levels behind it.
- `ElementalReactions.inflict` gives a creature an element without a hit (Absorption uses it, and
  so does Swirl now).
- The icon: a pixel tornado.

## Testing

- Unit: `StormeyeRulesTest`.
- In game (`tools/autotest/stormeye.txt`): a level-1 tornado set among four husks (200 health,
  slowed) drags them from 3 blocks out to within one of its eye and lifts them 3 blocks, for about
  60 damage each over its 4 seconds; at level 3 it takes a burning husk's fire (it turns orange)
  and gives it to the other three; Twin Storms circle each other; at level 10 Great Tempest towers
  14 blocks and Scavenger Wind sends three dropped diamonds to the player.

Testing it turned up an older bug, fixed alongside (`content/DamageSafety`): the mod's damage
bonuses multiplied the /kill command's hit (the largest float) past the largest float, and the
game's armour maths turned the infinity into NaN health: a creature neither alive nor dying,
beyond commands and spells, never removed. The mod's last word on any hit is now finite, and a
creature loaded with NaN health is put down.
