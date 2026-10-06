# Arcane Flora

## Why

Magic so far lives in places: shrines, towers, crypts, rings. Between them the land is plain
Minecraft. A fantasy world is magic all the way down: the grass by the road, the mushrooms in a
cave. Eight herbs, one for each element, growing where their element is strong, make every biome a
little enchanted. They glow, they give off a little of their element, and an herbalist can brew them
into tonics that bring you closer to that element for a while. That is a reason to wander, gather and
prepare for a fight, the way a witcher does.

## The herbs

| Herb | Element | Where it grows | Looks | Light |
|---|---|---|---|---|
| Emberbloom | Fire | deserts, badlands, savannas; the Nether's wastes, crimson forests and basalt deltas | a flame-shaped red and orange flower with a yellow heart; embers rise from it | 7 |
| Moonlily | Water | on still water in swamps, mangrove swamps and rivers | a blue lily pad; its pale flower opens at night and glows, and closes by day | 8 open |
| Skyplume | Wind | windswept hills, forests and gravelly hills, meadows | tall white plumes on thin stems; wisps of air blow off them | 0 |
| Deepcap | Earth | cave floors all through the Overworld's underground | a cluster of brown caps with glowing amber spots | 6 |
| Frostcap | Ice | snowy biomes | pale blue mushrooms rimed with frost; snowflakes drift from them | 3 |
| Prismleaf | Crystal | stony peaks, stony shores, gravelly hills, and the floors of dripstone caves | a sprig of violet crystal leaves that glint | 5 |
| Stormthistle | Lightning | windswept savannas, savanna plateaus, windswept hills and forests | a thistle whose spines end in yellow sparks; it crackles more in a thunderstorm | 4 |
| Sunpetal | Radiance | meadows, sunflower plains, flower forests, cherry groves | a golden flower that opens by day and closes at night | 6 open |

- Each grows in small patches (three to six), uncommonly, about one patch every eight chunks in its
  biomes (Moonlilies and Sunpetals a little more often, Deepcaps a few to a chunk underground).
- Herbs break at a touch and drop themselves. They can be planted again on their own ground (each
  herb's ground is a block tag, `elementalarcana:herb_soil/<herb>`), and bone meal on one makes a
  second to pick, like a tall flower, so a garden can be grown.
- All but the Moonlily go in a flower pot. They compost like flowers.
- Sunpetals and Moonlilies open and close with the sun as random ticks reach them, so a meadow at
  dusk closes over a minute or two, not all at once.

## Tonics

Brewed in a brewing stand from an Awkward Potion and an herb, like any potion, and made splash or
lingering or tipped onto arrows the same way.

| Herb | Tonic | Effect |
|---|---|---|
| Emberbloom | Tonic of Embers | Fire Kinship |
| Moonlily | Tonic of Tides | Water Kinship |
| Skyplume | Tonic of Gales | Wind Kinship |
| Deepcap | Tonic of Stone | Earth Kinship |
| Frostcap | Tonic of Frost | Ice Kinship |
| Prismleaf | Tonic of Prisms | Crystal Kinship |
| Stormthistle | Tonic of Storms | Lightning Kinship |
| Sunpetal | Tonic of Dawn | Radiance Kinship |

- **Kinship** with an element: your spells of that element are 20% stronger (damage, knockback and
  durations, as spell power is), and that element's magic harms you 20% less. 3:00; redstone makes
  it 8:00; glowstone makes it 35% for 1:30.
- So before facing the Fire Sovereign you might drink a Tonic of Embers to take its fire, or a Tonic
  of Tides to drown it harder. A splash tonic shields your companions too.
- Arcanists buy herbs (six for an emerald, Novice) and sell a tonic (Apprentice).

## Also

- Advancements: "Herbalist" (gather an herb of every element) and "Kinship" (brew a tonic).
- The Arcanist's Journal gets a page on herbs.

## Code

| Piece | What it does |
|---|---|
| `api/Herb` | The eight herbs, their elements and tonic names (tested) |
| `api/KinshipRules` | How much stronger and safer Kinship makes you (tested) |
| `content/flora/ModFlora` | Herbs, pots, items, Kinship effects, tonics, brewing, pots registered |
| `content/flora/HerbBlock` | A herb: its ground, its glow and motes, bone meal |
| `content/flora/SunpetalBlock`, `MoonlilyBlock` | The two that open and close with the sun |
| `content/flora/Kinships` | Kinship's power (on casting) and ward (on harm) |
| `tools/gen_flora.py` | Art, models, loot, tags, worldgen features and biome modifiers |

Worldgen is data: a configured `random_patch` for each herb, placed features (surface, on water,
cave floors scanned for, and the Nether's layers), and NeoForge biome modifiers adding them at
`vegetal_decoration`.

## Testing

- `HerbTest`, `KinshipRulesTest`: every element has one herb; the numbers.
- `tools/server_tests/flora.txt`: every herb's patch placed by command on its own ground (each
  places), every tonic brewed in a brewing stand, and Kinship's ward: two level-1 husks hit for 10
  by earth magic, the kin one loses 8.
- `tools/autotest/flora.txt`: a garden of the eight on their grounds, and the potted seven, by day
  and at night (shots; the Sunpetal shut and the Moonlily open at night, random ticks sped up for a
  moment), and Kinship's power: a Smite on a level-40 husk with and without Radiance Kinship (9.1
  then 10.8: a fifth more on the Smite itself, plus the same small aftermath both times).
- `tools/autotest/flora_wild.txt`: the herbs growing wild, each found in new land near the nearest
  of one of its biomes (AutoTest's new `biome` step, like `goto` for a biome) and shot, and Deepcaps
  found on a cave floor.
