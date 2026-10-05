# Wisp Rings

## Why

Wisps are drops of the Prime that never broke clean ("they guard what is theirs"). Something of
the old magic should be gentle and strange rather than a fight: a place you stumble on at night,
that does something to you you didn't ask for, good or bad. Rings where wisps dance, like the
fairy rings of old stories, give night wandering that.

## The rings

- **Where**: rare (about one every 600 blocks), in forests, birch and dark forests, flower forests,
  meadows and plains.
- **By day**: a ring of mushrooms (red and brown) and flowers seven blocks across around a patch of
  short grass, a mossy stone at its middle. Nothing more.
- **By night**: wisp lights circle over it, slowly (seven little lights, the colours of the
  elements), and it chimes softly when you're near.
- **Stepping in at night**: the wisps notice. Once a night for each ring (and each player), they do
  something, a boon or a trick, by chance (two boons in three):
  - **Boons**: *Fey Luck* (Luck II for ten minutes), *Wisp Sight* (Night Vision for five), *Moonlit
    Step* (Speed and Jump Boost for three), *A Gift* (an Essence, a Star Fragment or a golden carrot,
    dropped at your feet), *Full Moon* (your mana filled).
  - **Tricks**: *Turned Around* (spun, and set down 30 to 60 blocks away), *Small Folk* (shrunk to
    half your size for two minutes), *Will-o'-the-Wisp* (three wisps of the ring's mood come out to
    sting).
  - A line says which: "The wisps laugh. You feel lucky." / "The wisps laugh, and the world turns
    around you."
- **Advancement**: "Dancing Lights", step into a wisp ring at night.

## Code

| Piece | What it does |
|---|---|
| `api/WispRingRules` | The boons and tricks and their odds, the ring's shape (tested) |
| `content/world/WispRingStructure`, `WispRingPiece` | The ring (code-built), placed on the surface |
| `content/world/WispRings` | Night: the lights over rings near players, the chime, stepping in |
| `tools/gen_wisp_rings.py` | The structure, its spread and biomes |

## Testing

- `WispRingRulesTest`: night and the night's number, two boons in three, the ring's size.
- `tools/server_tests/wisp_rings.txt`: a ring located, and one placed by command on natural dry
  ground (rings never grow over water, so it's chunk 12,12 in the dev world): its stone and its
  podzol, found with `positioned over`.
- `tools/autotest/wisp_rings.txt`: the nearest ring that grew with the world (one placed by command
  is only blocks, not a ring the game knows), at night: its lights (a shot), stepping in (the boon
  or trick logged, and the advancement), stepping in again the same night (nothing).
