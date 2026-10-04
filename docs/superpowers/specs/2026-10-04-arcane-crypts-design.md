# Arcane Crypts

## Why

The world's places so far are open-air (shrines, ruins, sanctums) or a single tower. Nothing takes
you underground for a proper delve: rooms after rooms, a door that has to be opened, a floor that
bites, the dead in the walls, and something old at the bottom. Fantasy adventures live on that kind
of dungeon (Skyrim's barrows, Zelda's dungeons). Crypts are where the first mages lie, sealed in
with runes of their element. They make the middle of the game a dungeon crawl: a reason to dig
in, a puzzle any player can solve, a boss, and loot worth the trip.

## Finding one

- Under most overworld land (the ruins' biomes), about one every 600 blocks, never within 400
  blocks of the world's centre. On the surface a **mausoleum** marks it: a small house of dark stone
  under a stepped roof with the crypt's light at its peak, a paved way to its door (cut through any
  slope) between two lantern posts, and inside, a long stair down into the dark.
- A crypt needs fairly level ground: the rooms lie 16 blocks or more under the lowest ground above
  them, and the stair must reach them.
- **Element:** the land's, as for mage towers (Fire, Water, Ice, Wind or Earth), and one crypt in
  six belongs to that element's kin instead (Radiance, Lightning, Crystal; Water's kin is Ice).
  Everything in a crypt is of its element: its runes, its traps, its dead and its Revenant. Its
  trim, lights and candles take the element's colours on the deep dark stone.

## The way down

The rooms are 11 blocks square and 5 high (the burial chamber 23 and 9), on a grid; the way turns
at random. The main way always runs:

**Entry hall** → a **Hall of Tombs** and a **Warded Hall** (in either order), with a **Rune Gate**
after the first of them → a **Rune Gate** → the **Burial Chamber**.

Off the way lie a **library** and a **storeroom** (when there's room for them).

- **Entry hall:** statues of the wardens, urns, soul lanterns.
- **Hall of Tombs:** four upright **coffins** stand in the side walls. Come within 4 blocks of one
  (and not as a spectator) and its lid bursts: one of the dead steps out, the land's own (a zombie
  or skeleton; husks in the desert, strays in the snow), Attuned to the crypt's element (an Adept,
  one in four a Magus). A stone bier with candles, urns.
- **Warded Hall:** **glyphs** cover much of the floor, faint sigils of the element. Step on one and
  it flares for half a second, then bursts: damage (4, plus a quarter of the place's creature
  level) and the element's touch (Fire burns, Water soaks and shoves, Ice freezes, Wind throws you
  up (Airborne), Earth slows and bruises, Crystal cuts, Lightning stuns, Radiance blinds). Then it
  rests for 3 seconds. A safe path always runs between the doors, but nothing marks it. The dead
  of the crypt's element walk over glyphs unharmed, and so do players in creative. Breaking a
  glyph sets it off.
- **Rune Gate:** the way on is closed by a **runic seal**, a glowing wall nothing can break. Three
  **runestones** of the crypt's element stand on pedestals. Light one by casting a spell of its
  element family while looking at it (within 24 blocks), or by using its family's Essence on it
  (the crypt's dead often carry it). Touching an unlit one with anything else tells you what it
  answers to. The **keystone** above the seal shows how many are lit. With all three lit, the
  seal dissolves, and the two coffins flanking it burst open.
- **Library:** shelves, a lectern, candles, and a chest of old books: enchanted books, paper,
  experience, now and then one of two new lore pages.
- **Storeroom:** barrels, urns and a chest: gold, iron, emeralds, the crypt's Essence, a Mana
  Draught now and then.
- **Burial Chamber:** pillars, an aisle lit with the element's light, four sealed coffins in the
  side walls, and on a dais at the far end the Revenant's tomb with the **grave flame** at its
  head and two **reliquary** chests behind it.

**Urns** (decorated pots) hold a little each: break them.

## The Revenant

Come within 10 blocks of the grave flame and the **Revenant** rises from its tomb: the crypt's
first warden, who would not sleep. It's a tall skeleton in robes of its element's colour with
burning eyes, an Attuned Archmage of the crypt's element with its own boss bar ("Revenant of
Fire").

- It keeps its distance and casts its element's spells (and bursts its element up close, like
  every Attuned caster).
- Struck from close by, it **steps through the grave**: it sinks in a swirl of souls and rises 6 to
  9 blocks away (every 5 seconds at most).
- At half health it **raises the dead**: the chamber's four coffins burst open.
- It never leaves the chamber. With no one to fight for 20 seconds it goes back to its tomb and
  heals.
- When it falls the grave flame goes out ("The crypt falls silent"). It leaves its element's
  Essence (3 to 5), and from its loot table bones, gold, and often (35%) a Tome of Insight.

## Loot

| Where | What |
|---|---|
| Urns, barrels | bones, gold nuggets, an emerald, arrows, a little of the crypt's Essence |
| Storeroom | gold and iron, emeralds, the crypt's Essence, a Mana Draught, an apprentice's wand now and then |
| Library | enchanted books, paper, experience bottles, a lore page |
| Reliquary (two chests) | diamonds, gold, the crypt's Essence (4 to 8), a Tome of Insight (40%), its family's Catalyst (30%), a Scroll of Unbinding (10%), an Adept Staff of its element (20%), enchanted books |

## Lore, Journal, advancements

- Two more lore pages, found in crypt libraries: **The Wardens' Rest** (why the first mages were
  laid in sealed crypts) and **Of Revenants** (a warden who would not sleep).
- Journal page 27: Crypts.
- Advancements: **Into the Crypt** (enter a crypt), **Runebreaker** (open a rune gate), **Laid to
  Rest** (defeat a Revenant).

## Code

| Piece | What it does |
|---|---|
| `api/CryptLayout` | Plans the rooms on a 13-block grid: the way, its turns, the chamber, the side rooms, the doors (tested) |
| `api/CryptRules` | Distances, timings, damage, chances (tested) |
| `content/crypt/CryptStructure` | Where a crypt may stand, its element, its depth; makes the pieces |
| `content/crypt/CryptEntrancePiece` | The mausoleum and the stair |
| `content/crypt/CryptRoomPiece` | Each room and the chamber, with its element's palette |
| `content/crypt/RunestoneBlock`, `RuneLockBlock`, `RunicSealBlock`, `Runes` | The gate: lighting runes (casts and Essence), the keystone, opening the seal |
| `content/crypt/GlyphBlock` | The floor traps |
| `content/crypt/CoffinBlock`, `CoffinBlockEntity` | Upright coffins and what's in them |
| `content/crypt/GraveFlameBlock`, `GraveFlameBlockEntity` | The chamber's heart: raises the Revenant, raises the dead, goes out |
| `content/crypt/RevenantEntity` | The boss |
| `client/RevenantModel`, `RevenantRenderer` | Skeleton, robe (tinted), burning eyes |
| `tools/gen_crypts.py` | Textures, block models and the loot tables |

The gate needs no stored links: the keystone sits in the middle of the gate's wall, facing into
the room, so the room (and the runes in it), the seal under it and the coffins beside it all
follow from where it is.

The pieces place blocks in world coordinates with no vanilla orientation and turn their own
blocks: a `StructurePiece` oriented SOUTH keeps positions but mirrors every block state's north and
south, which turned the keystones, coffins, stairs and the grave flame of north- and south-facing
rooms the wrong way.

## Testing

- `CryptLayoutTest`: over thousands of seeds and every heading, rooms never overlap (the chamber
  and the stair included), every room is within reach of the start chunk, the way is connected
  door to door, there are two gates and the last one opens on the chamber, side rooms hang off the
  way past the entry, and doors always come in pairs.
- `CryptRulesTest`: glyph damage, chances.
- `tools/server_tests/crypts.txt`: a crypt located; its loot tables rolled; a Revenant summoned.
- `tools/autotest/crypts.txt`: a crypt found (the mausoleum), then each room visited (a new
  `crypt <room>` step): a coffin's ambush, a glyph's burst, runes lit by a cast and by Essence and
  the seal opening, the Revenant rising, raising the dead and falling, the flame going out.
