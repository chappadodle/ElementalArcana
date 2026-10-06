# The Hollowed

## Why

The story's enemy, the Hollow, shows itself only at the very end. Between awakening and the Prime
Key the world should feel it pulling: people who heard its hunger when the seals faded and answered
it, gathering in camps where the seals are thin and coming for mages at night. They fight like
nothing else in the mod: they don't make mana, they eat yours, so a mage who spends freely starves.
Their camps are held by an obelisk that has to be broken. And what they drop helps against the
Hollow itself.

## The Hollowed

Mages and others the Hollow has hollowed out: black and violet robes, ash-grey skin veined with
violet, eyes like violet coals. They are illager-shaped and stand together; they hunt players and
fight whoever hurts them (iron golems hate them, as they hate every monster). Their attacks are
**hunger** (a new damage type, `elementalarcana:hollowed`, which armour doesn't stop), and what
they eat heals them. Zone levels apply to them as to every creature.

| | Health | | What it does |
|---|---|---|---|
| **Hollowed Acolyte** | 24, armour 2 | keeps 5 to 10 blocks off | throws a **Hunger Bolt** every 3 seconds: a slow violet orb, 4 hunger harm; a player it hits loses 15 mana, and the Acolyte heals 2 |
| **Hollowed Devourer** | 40, armour 4, a head taller | rushes in | claws for 5; each hit eats 10 mana and heals it 3 |
| **Hollow Herald** | 130, armour 6, half again as tall, a purple bar | leads a camp | throws three bolts in a fan; every 10 seconds **the Pull** (everyone within 10 blocks dragged in, then a burst: 5 hunger harm, thrown back); its **hunger** eats 2 mana a second from players within 12 blocks; blinks away when hurt; at half health calls two Acolytes |

When a player has no mana left to eat, the Hollowed bite deeper: their mana-eating hits do 2 more
harm.

## Camps

- **Where**: plains, forests, taiga, savannas, meadows, snowy plains; about one every 600 blocks,
  never within four chunks of a village or a shrine (a new placement type, `spread_away`, which
  keeps a structure clear of several structure sets; vanilla's exclusion zone takes only one).
- **What**: a trampled clearing 21 blocks across, its blighted floor of soul soil, gravel and packed mud (the land shaped to it as it generates), three tents of
  black wool on spruce poles, soul campfires ringed in blackstone, and at its middle the **Hunger
  Obelisk**: a pillar of polished blackstone and crying obsidian with the obelisk on top, glowing
  violet. The biggest tent holds a chest (Hollow Shards, a letter, gold, ender pearls, Essences).
- **Who**: a Herald, two Acolytes and two Devourers, there from the start and staying.
- **The Hunger Obelisk**: while it stands, the Hollowed within 16 blocks regenerate and resist
  (Regeneration I and Resistance I). It is as hard as obsidian (a diamond pickaxe). Broken, it
  shatters: the Hollowed within 16 blocks are weakened and slowed for 30 seconds, and two Hollow
  Shards drop.

## Patrols

At dusk, for each awakened player of level 15 or more out in the Overworld, a chance (8% at 15,
0.3% more a level, at most 20%; the server setting `hollowedPatrols` scales it) that a band comes
for them from 24 to 40 blocks off: an Acolyte and a Devourer, another Acolyte from level 30 and
another Devourer from level 45. The player hears it coming ("Something hungry is coming for your
magic."). They are ordinary monsters: they despawn if left far behind.

## Hollow Shards and the Hungerward

- **Hollow Shard**: a sliver of the Hollow's dark. Acolytes drop one a third of the time, Devourers
  half the time (Looting adds), Heralds three to five; camp chests have more.
- **Hungerward Charm** (four shards round an amethyst shard, gold ingots in the corners): carried, or in
  a Charm Pouch, it halves the mana the Hollowed and the Hollow itself eat from you, and takes a
  quarter off their hunger's harm. So fighting the cult readies you for the end.

## Story

- Caelith gets a chapter after the Magister's: "The Hollowed" (task: defeat one). It explains them
  and points at their camps.
- Three letters, found in camp chests and on Heralds: a Herald's orders, an Acolyte's diary, and a
  note on why the camps stand where the seals are thin.
- A journal page.
- Advancements: The Hollowed (defeat one), Shattered Hunger (break a Hunger Obelisk), Silence the
  Herald (defeat a Herald, a challenge).

## Code

| Piece | What it does |
|---|---|
| `api/HollowedRules` | What they eat, the Hungerward's halving, patrol odds and size (tested) |
| `content/hollowed/ModHollowed` | Entities, items, the obelisk, camp structure and piece, damage type key |
| `content/hollowed/HollowedEntity` | What all three share: eating mana, standing together, sounds |
| `content/hollowed/HollowedAcolyte`, `HollowedDevourer`, `HollowHerald` | The three |
| `content/hollowed/HungerBolt` | The orb (a thrown item, drawn by the vanilla renderer) |
| `content/hollowed/HungerObeliskBlock(Entity)` | The obelisk's ward over its camp, and its shattering |
| `content/hollowed/HollowedCampStructure`, `HollowedCampPiece` | The camp, code-built; the land is shaped to it (terrain adaptation `beard_thin`) |
| `content/world/SpreadAwayPlacement` | Random spread kept clear of other structure sets |
| `content/hollowed/HollowedPatrols` | Dusk patrols (`/arcana hollowed patrol` calls one) |
| `client/HollowedRenderer` | The illager model in their robes |
| `tools/gen_hollowed.py` | Art, models, loot, recipe, letters, worldgen data, tags |

## Testing

- `HollowedRulesTest`: eating with and without a Hungerward, patrol odds and sizes.
- `tools/server_tests/hollowed.txt`: the nearest natural camp (its obelisk, crying obsidian, tent and
  chest where they belong); the three summoned there, kept strong by the obelisk, weakened once it's
  broken (their effects logged); the obelisk set back so the next run finds it; hunger's harm.
- `tools/autotest/hollowed.txt`: the nearest camp, asleep and then awake (shots); an Acolyte eating
  mana (the pool logged every second: 15 a bolt; with a Hungerward, 7.5; `/arcana mana get`); a
  Herald's Pull (shots, the mage's place logged); a patrol called by command.
