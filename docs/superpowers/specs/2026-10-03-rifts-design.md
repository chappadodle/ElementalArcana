# Elemental Rifts

## Why

Shrines, towers and sanctums wait to be found; once they're found, the overworld goes quiet. A
fantasy world should come to the player now and then too. Rifts are the Hollow's hunger made
visible: the seals are fading, and now and then the world tears open and pours out Attuned
creatures. Close the tear and it spills what it swallowed. It's a repeatable fight with a reward,
in the same world the player is already in, and it grows harder further from spawn like everything
else.

## When and where

- Only near players whose magic has woken, in the overworld.
- Every 30 seconds each such player rolls for a rift near them: **1.5% at night, 0.4% by day, twice
  that in a thunderstorm**. That's about one night in four, a day now and then. No new rift within
  10 minutes of the last one near the same player, and never two within 96 blocks of each other.
- The rift opens on open ground 24–40 blocks from the player (a few tries at random spots: solid
  ground, not under water or lava, with room for the rift), never inside a structure's protected
  zone (towers, sanctums).
- **Its element** comes from the land, like an Attuned creature's (the biome's lean, so deserts
  give Fire and snowfields Ice; derived elements too, rarely).
- **It's announced**: everyone within 64 blocks hears the tear and reads "The air tears open
  nearby: a rift of Fire!" in the element's colour.
- `/arcana rift [element]` opens one in front of the player, for testing.

## The fight

- A rift stands 3 blocks tall and 2 wide: a jagged tear in the air, its edges crackling in the
  element's colour, the inside a swirl of the element. Nothing can hurt it.
- **Three waves.** Creatures climb out of the tear over a couple of seconds, Attuned to its element,
  as strong as the place makes them (the level zone):
  - Wave 1: three Adepts and a wisp.
  - Wave 2: three Adepts, a Magus and a wisp.
  - Wave 3: three Adepts, a Magus, and the **Rift Warden**: an Archmage (with its boss bar), or a
    Magus if the mage it opened near is below level 15.
  - The creatures are the land's own (zombies, skeletons and spiders; husks in the desert, strays in
    the snow, drowned by water) and are told where the nearest player is.
- The next wave comes when the last one is down. Only kills count: if a creature goes any other
  way (the world turned peaceful, one lost in an unloaded chunk) the rift fades. Its creatures don't
  despawn while it's open.
- A **boss bar** shows the rift for players within 48 blocks: "Fire Rift — wave 2 of 3", filling as
  that wave's creatures fall.
- **Closing it:** when the third wave is down, the rift collapses in on itself with a flash and a
  boom, and throws out its **cache**: Essence of its element (4–8), experience, two to four finds
  (emeralds, gold, amethyst, lapis, bottles o' enchanting, a Mana Draught, now and then a diamond),
  and a fair chance at a Tome of Insight, a Scroll of Unbinding or a Catalyst (the loot table
  `elementalarcana:gameplay/rift_cache`, so it can be tuned; the Essence is added for its element).
  The Warden drops its own loot as any Archmage does.
- **Left alone:** with no player within 48 blocks for a minute, or five minutes after it opened,
  the rift fades away (no cache), pulling what's left of its creatures back in with it.
- An advancement, **Rift Closer**: close a rift.

## Looks and sounds

- The tear: a few layered, jagged additive quads (the element's colour and a white-hot edge),
  slowly turning and flickering, with motes drifting into it; pixel-art texture drawn by
  `tools/gen_textures.py`.
- Sounds stay vanilla: the portal's ambient hum around it, the end portal's opening when it tears
  open, the wither's spawn far off-pitch for the Warden's wave, and the end gateway's blast when it
  closes.

## Code

| Piece | What it does |
|---|---|
| `api/RiftRules` | Chances, timings, waves, the Warden's rank, the cache's size (tested) |
| `content/rift/RiftEntity` | The rift: its element, wave, boss bar and timers (synced, not saved) |
| `content/rift/Rifts` | Rolling and placing rifts, the command, the wave spawns |
| `client/RiftRenderer` | The tear |
| data | The cache's loot table, the advancement, lang |

## Testing

- `RiftRulesTest`: the chances by time and weather, the waves' make-up, the cooldowns.
- A server test (`rifts`): `/arcana rift fire`, the waves killed by command as they come (the
  numbers killed show each wave's size), then the cache's items counted.
- `tools/autotest/rifts.txt`: a rift opening, its waves, the boss bar, the collapse and the cache.
