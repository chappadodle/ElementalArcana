# Ruins and Lore

## Why

The story of the Prime and the Hollow is told in three Journal pages and then not again until the
Hollow itself. A fantasy world should have its past lying about: broken places where mages once
worked, and pages that tell what happened there. Ruins give the overworld small, frequent things to
find between shrines and towers, and the pages build the story up toward the Sovereigns and the
Hollow.

## The ruins

- Small ruined places of old stone (stone bricks, cracked and mossy, cobblestone, chiseled stone),
  weathered as they're placed: blocks missing, cracked, mossy, with vines and the odd cobweb. Three
  layouts, chosen at random:
  - **Fallen Arch:** two broken pillars, the arch between them half down, rubble all round, and a
    cracked altar with a chest.
  - **Stone Circle:** a ring of broken pillars of uneven height round a sunken floor, a chest at
    the middle under a slab.
  - **Ruined Hall:** the stumps of a small hall's walls, a fallen-in floor, an old bookshelf or two
    and a lectern beside the chest.
- They stand on fairly level land in most overworld biomes (not oceans, rivers or the deep cold),
  about as often as shrines.
- **The chest:** a little of everything (Essence, emeralds, iron and gold, bones, an apprentice's
  wand or robe now and then), and often a **lore page**.

## The lore

Eight pages, written books titled with their subject and signed by old hands, found in ruins (one
at a time, at random). Together they tell the story the Journal only sketches:

1. **The Splitting:** how the first mages broke the Prime into the elements.
2. **Of the Hollow:** what the Hollow is, a hunger, not a creature.
3. **The Four Seals:** the Sovereigns' oath, and why there are four.
4. **A Lament:** a mage whose magic woke and faded as the seals held.
5. **On Wisps:** wisps as droplets of the Prime, drawn to living mana.
6. **The Torn Sky:** rifts in the old records, and what they meant then.
7. **The Golem-Wrights:** who first stood the elements up as golems, and why they still walk.
8. **The Last Entry:** a mage who turned a key and went into the Hollow.

The text lives in the language file (the books use translated text), so it can be translated like
the rest. An advancement, **Lorekeeper**, for finding one.

## Code

| Piece | What it does |
|---|---|
| `content/world/RuinStructure`, `RuinPiece` | Where a ruin may stand, and building one of the three layouts, weathered |
| data | The structure, its set and biome tag, the chest's loot table (with the eight books), the advancement |
| lang | The pages |

## Testing

- `tools/autotest/ruins.txt`: each layout found and viewed (`goto`), its chest's loot listed.
- The server suite (`smoke` loads the world with the new structures).
