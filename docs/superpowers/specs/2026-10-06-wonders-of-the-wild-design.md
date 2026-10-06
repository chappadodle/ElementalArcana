# Wonders of the Wild

## Why

The herbs made the ground magical; the air should be too. Small and great creatures that fight no
one and are simply there, beautiful: glowmoths drifting round the herbs at night, skyrays gliding
over the sea. They make a night walk or a voyage a pleasure, they light a home, and the skyrays
give gliders a way across the ocean.

## Glowmoths

- Moths a third of a block long whose wings glow in an element's colour (the eight, as the herbs).
- **When and where**: at night only, round herbs and flowers in the Overworld: every five seconds,
  near a player who has fewer than six about, two or three come to a herb or flower within 16
  blocks (a herb's own element; any for a flower). At dawn they fade away one by one in a few
  sparks.
- **How they fly**: in loose fluttering loops a few blocks over the ground near where they came,
  and drawn to light: each time one picks where to flutter next it tries a few spots within 4
  blocks and makes for the brightest, so near a lantern, torch or glowing herb it ends up circling
  it (moth to flame).
- **Catching one**: use an empty glass bottle on it: a **Bottled Glowmoth** of its element. Placed,
  it's a **Moth Jar**, a glass jar with the moth glowing inside (light 12): a lantern that never
  goes out, in the element's colour. Broken, the jar gives the bottle back. Use the bottle in the
  air to let the moth go.
- Harmless; one hit puts one out (no drops).

## Skyrays

- Great gentle rays, five blocks from wingtip to wingtip, deep blue above with bands of spots that
  glow at night, a long tail behind.
- **Where**: high over the ocean (from 100 to 150 up), in slow wide circles round a point that
  drifts off across the sea every few minutes, by day and night; up to three in sight of a player
  out at sea, coming and going as they travel.
- **Riding the wake**: someone gliding (with elytra, or on Skyward Leap's glide) within 8 blocks of a
  skyray is borne up on its wake, a gentle steady lift, so a glider can follow the skyrays across
  the sea. A shimmer of wind trails them.
- Hard to reach, 30 health; hurt, they climb away. They drop nothing: they're wonders, not game.

## Also

- Advancements: "Glowing Company" (bottle a glowmoth), "On the Skyray's Wake" (be lifted by one).
- Server settings: `glowmoths` and `skyrays` scale how often they come (0: never).
- A journal page.

## Code

| Piece | What it does |
|---|---|
| `api/WonderRules` | When moths come and how many, the wake's reach and lift (tested) |
| `content/wonder/GlowmothEntity` | A moth: fluttering, light-seeking, fading at dawn, bottled |
| `content/wonder/SkyrayEntity` | A ray: circling an anchor high up, climbing away when hurt |
| `content/wonder/Wonders` | Bringing moths at night and skyrays over the sea (`/arcana wonders moths` / `skyray` call them); the wake's advancement |
| `content/wonder/MothJarBlock`, `BottledGlowmothItem` | The jar, the bottle |
| `client/GlowmothModel/Renderer`, `SkyrayModel/Renderer` | Their models, their glow |
| `client/SkyrayWake` | The wake's lift, for elytra and the mod's glide alike (client side: a player's movement is their own client's) |
| `tools/gen_wonders.py` | Their art, the jar's and bottle's models, the blockstate, their (empty) loot |

## Testing

- `WonderRulesTest`: the moths' numbers and night, the wake's reach.
- `tools/server_tests/wonders.txt`: both summoned and killed; a jar of each element set; the
  settings listed.
- `tools/autotest/wonders.txt`: night in a meadow of herbs (moths come; a shot), a moth bottled and
  set as a jar (a shot), skyrays over the sea (a shot), a glider lifted by one (height logged).
