# Arcane Infusion

## Why

Magic so far lives in spells: a sword is still just a sword, and Essence is mostly spent on
Catalysts and trades. In a fantasy world, mages bind elements into blades and armour. An altar
where Essence infuses gear gives Essence a lasting use, makes melee and archery elemental (the
element chart and the reactions apply to them as they do to spells), and lets a mage make a
favourite weapon their own.

## The Infusion Altar

- A block: a dark stone pedestal with amethyst at its corners and a gold-rimmed basin, crafted from
  amethyst shards, gold ingots, obsidian, smooth stone and any Essence.
- Use it holding a weapon or armour piece: the item is set on the altar, floating and slowly
  turning over the basin.
- Use it holding Essence: one is poured in (the basin glows in the element's colour, brighter with
  each). Eight of one element infuse the item: a burst of the element, a chime, and the item is
  **Infused** with it. (Essence of another element while the basin holds some is refused.)
- Use it with an empty hand to take the item back (any Essence poured in stays for the next item).
- An infused item can be infused again with another element, replacing the first.
- An infused item shows "Infused: <Element>" in its tooltip, and its name takes the element's
  colour. Held, it sheds a faint trace of its element.

## Infused weapons

Swords, axes, maces and tridents (melee), and bows and crossbows (their arrows). Their hits become
the element's:
- **The chart applies**: ×1.5 on creatures the element is strong against, ×0.5 on those that resist
  it (its own element included); players resist the elements they've awakened, as against spells.
- **The element lands**: each hit inflicts the element (Fire sets alight, Water soaks, Ice frosts
  and slows, Wind gives a gust of knockback, Earth staggers (a moment's slowness), Lightning
  sparks to one more foe within 4 blocks for a third of the hit, Radiance burns the undead half
  again as hard and makes them glow, Crystal leaves a crystal shard as Crystallize does), and the
  **reactions** trigger as they do for spells (Melt, Vaporize, Freeze, Swirl, Crystallize).

## Infused armour

- Each infused piece takes 10% off the harm of its element's family (spells and everyday harm
  alike), up to 40% with four.
- **Four pieces of one element** add its set boon: Fire: burning can't hurt you; Water: you breathe
  under water; Ice: you don't freeze, and slow your attackers; Wind: falls can't hurt you; Earth:
  you can't be knocked back; Lightning: you're quicker (Speed I); Radiance: you heal slowly in
  daylight; Crystal: every 30 seconds, 4 absorption hearts.

## Code

| Piece | What it does |
|---|---|
| `api/InfusionRules` | Essence per infusion, armour resistance per piece, the spark's share, boons (tested) |
| `content/infusion/Infusion` | The `elementalarcana:infusion` component (the element) |
| `content/infusion/InfusionAltarBlock` (+BE) | Setting items, pouring Essence, infusing, taking back |
| `content/infusion/Infusions` | Infused hits (melee and arrows), armour resistance, set boons, tooltips and names |
| `client/InfusionAltarRenderer` | The floating item and the basin's glow |
| `tools/gen_infusion.py` | The altar's textures and model, its recipe |

## Testing

- `InfusionRulesTest`.
- `tools/server_tests/infusion.txt`: an altar placed, a sword set and infused by its block data,
  given infused items listed (their component).
- `tools/autotest/infusion.txt`: a sword set on the altar and infused with eight Fire Essence
  (shots), then a husk hit with it (its fire and health logged against a plain sword's hit), an
  ice-infused sword on a soaked husk (Freeze), and four fire-infused armour pieces standing in fire.
