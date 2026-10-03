# Skyward Leap and gliding

Date: 2026-10-03
Status: built (milestone 12, step 4 of the approved wind plan: "Skyward Leap + glide: replaces
Updraft"; Wind Blade and wind's framework were steps 1 and 2).

The world is big and the mod sends you far into it (towers, sanctums, the derived elements' peaks).
Wind's answer is to take to the air: leap up, then glide down and away, Genshin-style, without an
elytra. Updraft (a single throw upward) was standing in for this.

## The spell

**Skyward Leap** (Wind, level 5, 30 mana, 8 s). Tap R: a gust throws you straight up, about 9
blocks, and throws every creature you may hurt within 4 blocks away and up (Airborne). Then you're
**Skyward** for 8 seconds (an effect, with its icon): hold jump while you fall to **glide**. You
sail the way you look at about 6 blocks a second, sinking 1.6 a second; let go of jump and you
drop. Your first landing after the leap never hurts. A gliding player leans forward (everyone sees
it), wind streams off their hands, and the air rushes past (the elytra's sound).

It levels to 10 like Wind Blade (a chain behind its node in the tree, mastery from casting and
Infusing), with a choice at 5 and at 10:

| Lv | Name | What changes |
|---|---|---|
| 1 | Skyward Leap | as above |
| 2 | Higher Ground | a higher leap (about 13 blocks), Skyward for 12 seconds, and you sink slower (1.3 a second) |
| 3 | Rising Current | press R again while gliding (Skyward Leap selected) to catch an updraft, about 5 blocks up: once a leap, free, even while the spell recharges |
| 4 | Plunge | sneak while Skyward and in the air to dive straight down; you land in a shockwave that grows with the height you fell (2.5 to 5.5 blocks across, 3 to 10 damage times your power), throwing creatures back |
| 5 | **Skyfall** | the plunge's shockwave is half again as wide and hits half again as hard, and launches what it hits Airborne |
| 5 | **Wind Rider** | the first 6 seconds of Skyward are free flight (jump to rise, sneak to sink, as in creative); then you glide |
| 6 | Lift | the leap's gust reaches 6 blocks and throws creatures higher |
| 7 | Tailwind Glide | you glide faster (about 9 blocks a second), and no landing hurts while you're Skyward |
| 8 | Aerial Barrage | wind spells cost 30% less mana while you're Skyward and off the ground |
| 9 | Swirling Plunge | the plunge's shockwave Swirls the elements of whatever it hits |
| 10 | **Heaven's Descent** | the plunge's shockwave is twice as wide and leaves a whirlwind where you land (Tempest Edge's), pulling creatures in |
| 10 | **Endless Sky** | Skyward lasts until you land (a minute at most), and each Wind Blade you throw while gliding lifts you a little |

Skyward Leap keeps Updraft's id (`updraft`) and its node at the end of Wind's sky arm, so whoever
learned Updraft has it now; its chain of levels follows that node.

## How it's built

- `api/SkywardRules` (unit tested): the numbers by level and branch (leap speed, the gust's reach,
  how long Skyward lasts, glide speed and sink rate) and the plunge's shockwave by the height fallen.
- **The glide is the client's**, because a player's movement is: `client/Gliding`, each tick before
  the local player moves, if they're Skyward, in the air, falling and holding jump, steers their
  motion toward where they look at the glide speed and holds their fall to the sink rate (always
  faster than a sixteenth of a block a tick, so a server never takes it for flying). Sneaking dives
  instead (from level 4). It tells the server when gliding starts and stops (`GlidePayload`), which
  sets a small attachment synced to everyone who can see the player; every client draws a player
  with it leaning forward, with wind streaks off their hands. The local player also hears the rush
  of air.
- **The rest is the server's** (`content/spell/SkywardLeaps`): the leap and its gust; Skyward (a
  mob effect); landing safely (the leap's own fall immunity, or the whole of Skyward from level 7);
  the plunge (a player who sneaks while Skyward and in the air is plunging: where they started is
  kept, and where they land the shockwave goes off); Wind Rider's flight (NeoForge's creative-flight
  attribute for 6 seconds, so a server never kicks them for flying); Aerial Barrage (on
  `SpellCastEvent.Pre`); Endless Sky's lift (when a Wind Blade leaves a gliding caster's hand).
- **Rising Current** needs one thing from the spell API: a spell may say it's usable while it
  recharges (`Spell#usableWhileRecharging`); such a use costs no mana and doesn't restart the
  cooldown. Skyward Leap says so for a Skyward player in the air who hasn't used their updraft.
- Sounds are vanilla: the breeze's jump and a wind charge's burst for the leap, the elytra's rush
  while gliding, the mace's heavy smash for a plunge's landing.
- The icon: a figure on a gust, arms out. Skyward's effect icon: a feather on the wind.

For testing (and operators), `/arcana spell <spell> <level> [branches]` puts any spell at a level
through its tree path for free, taking the named branches at its forks.

## Testing

- Unit: `SkywardRulesTest`: the numbers at each level and branch; the shockwave's reach and damage
  at heights from 0 to 30 (capped), with Skyfall and Heaven's Descent.
- In game (`tools/autotest/skyward_leap.txt`, with a new autotest step, `key`, to hold jump and
  sneak): the leap throws four husks 2 to 4.5 blocks out and 4 up; the player rises about 12
  blocks and glides east at about 7 blocks a second, sinking about 1.3 a second; Rising Current
  lifts them 5 more for no mana; a level-4 plunge from 12 blocks takes 25 to 35 health from each
  husk below (200 each), level 10's Skyfall and Heaven's Descent 50 to 60, with the whirlwind; Wind
  Rider flies (abilities.flying 1b) and lands after 6 seconds (0b); the player never loses health.
