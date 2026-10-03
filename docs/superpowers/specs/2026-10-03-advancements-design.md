# The advancement tab

Date: 2026-10-03
Status: built (after milestone 12 of `2026-10-02-fantasy-adventure-roadmap.md`: the roadmap's
places and bosses were all there, but nothing in the game pointed a player down the path).

An "Elemental Arcana" tab in the advancements screen (amethyst background) maps the adventure:
from waking your magic to binding the Hollow, with the side roads along the way. Each step says
what to do next, so a new player always has a goal in front of them.

## The tab

`Elemental Arcana` (the Journal; granted at once, so the tab is there from the first day) →
**Awakening** (goal) → from there:

- **First Spark** (cast a spell) → Elemental Chemistry (a reaction), Crossroads (a spell at
  level 5) → **Capstone** (level 10), Take to the Sky (Skyward).
- Two Natures (a second element) → *Elemental Sage* (four at once).
- Apprentice (magic level 10) → Adept (25) → **Master** (40) → *Archmage* (60).
- Rule Breaker (a keystone).
- Places of Power (a shrine) → Blessed (a shrine's blessing).
- Wisp Hunter (a wisp); Attuned Foe (an Attuned creature) → *Archmage Slayer*.
- The Arcanist (buy from one) → Liquid Mana (brew a Mana Draught).
- The Tower Wakes (enter a mage tower) → **Magister's Fall** → Staff of a Master, The Sanctums
  (find one) → **Sovereign's Fall** → Staff of an Archmage, *Four Hearts* → **The Prime Key** →
  **Into the Hollow** → *Bound Again*.

(**Goals** in bold, *challenges* in italics; 31 in all.)

## How it's built

- `tools/gen_advancements.py` writes them all (`data/elementalarcana/advancement/arcana/`), the
  tags they use (structures: `shrines`, `sanctums`; items: `arcanist_goods`, everything an
  Arcanist sells that only an Arcanist sells, so buying one from a villager means an Arcanist) and
  their titles and descriptions in the lang file.
- Most use vanilla triggers: places (`location` with a structure), kills, items in the
  inventory, effects, brewing, trading, changing dimension.
- The rest use one trigger of the mod's own, `elementalarcana:magic` (`content/MagicTrigger`): an
  event, an optional key it must match and an optional least number. `MagicTriggers` fires it:
  `cast` on every cast; `reaction` where reactions are rewarded; `defeated` (key: the rank) where
  Attuned kills are; and every two seconds what a mage has become (`elements` held, `level`,
  `keystones`, each spell's `spell_level`), so progress made any way counts, old worlds included.

## Testing

`tools/autotest/advancements.txt` revokes the tab and earns ten of them one by one: awakening, a
cast, magic level 25, a spell at level 5, Skyward, a second element, a wisp killed (also an Attuned
foe), a Swirl from a level-9 Gale Dash through a burning husk, and standing in a shrine; each is
checked with an advancements selector (all passed). The autotest can open the tab
(`screen advancements`). The server loads all 31 without errors.
