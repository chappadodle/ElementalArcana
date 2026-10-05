# Charm Pouch

## Why

Five Drakescale Charms, six Trophies of the Wild and nine relics now work from anywhere in the
inventory, and that means they fill it: a mage who wants their wards, their tricks and their relic
at once gives up most of a row. A pouch that holds them, where they still work, gives the slots
back, and makes gathering charms feel like putting together a kit.

## The pouch

- **Charm Pouch**: five leather, a string and a gold ingot (the clasp). One to a stack.
- **Use it** to open it: nine slots (three by three) for charms and relics only (the item tag
  `elementalarcana:charms`: the Drakescale Charms, the Trophies of the Wild, the relics; a datapack
  can add more). Shift-click moves them in and out. A pouch can't go in a pouch, and while it's
  open it can't be moved.
- **Everything inside works** as it would loose in the inventory: the Drakescale wards, the
  Trophies' tricks, the relic you bound last (a pouched Phylactery still saves you, and still
  recharges). Two pouches work as well as one.
- **Like a bundle**: right-click a charm onto the pouch, or the pouch onto a charm, to slip it in.
- **Its tooltip** shows what's inside, a grid of icons (as a bundle's does). A pouch with charms in
  it looks full, with a glint at its neck.
- **Advancement**: "Bag of Tricks", carry a Charm Pouch with three charms or relics in it.

## Code

| Piece | What it does |
|---|---|
| `content/pouch/ModPouch` | The item, its menu type, the charms tag |
| `content/pouch/CharmPouchItem` | Opening it, bundle-style stuffing, the tooltip |
| `content/pouch/CharmPouchMenu` | The three-by-three menu: charms only, the pouch's own slot locked |
| `content/pouch/CharmPouches` | Everything a player carries, loose or pouched (the charm and relic checks use it), and writing a changed charm back into its pouch |
| `client/CharmPouchScreen`, `client/CharmPouchTooltip` | The screen and the tooltip's grid |
| `tools/gen_pouch.py` | The icons, the screen's texture, the recipe, the tag |

## Testing

- `tools/server_tests/charm_pouch.txt`: a pouch with charms in it, dropped and read back (its
  contents kept); the recipe and the tag load with the server.
- `tools/autotest/charm_pouch.txt`: the pouch opened (a shot); three charms and a bound Storm Sigil
  shift-clicked in, a stick refused, the pouch's own slot locked; then, closed: the fire ward (fire
  damage does nothing), the Bog Pearl's swim speed, the Storm Sigil's speed (all logged); the
  tooltip (a shot).
