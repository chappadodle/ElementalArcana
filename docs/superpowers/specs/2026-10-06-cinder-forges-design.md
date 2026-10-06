# The Ember Reaches, part 1: Cinder Forges

## Why

The Nether is where fire was first worked, and the mod has left it nearly bare: herbs, fire wisps,
salamanders, harder creatures. Players go there for blaze rods and netherite and find no magic of
their own. The Ember Reaches give the Nether its own places and foes, starting with the **Cinder
Forges**: the first mages' smithies, built over the lava sea and long since ruined, still kept by
the construct that guarded them. They sit between the mage towers and the Sovereigns' sanctums in
difficulty (the Nether's zone levels run 25 to 50), and they pay in what a smith of magic needs.

## The Cinder Forge

- **Where**: in nether wastes (not the basalt deltas, whose columns and lava pools grow after the
  land's structures are built and would grow into the hall), about one every 900 blocks (Nether
  blocks), kept clear of fortresses and bastions. The forge carves its own cavern out of the
  netherrack, with four tunnels out to the Nether's caves: a hall on a basalt plinth whose floor is
  at y 40, above the lava sea.
- **What** (code-built, like the Enclave):
  - A round hall of blackstone bricks, 25 blocks across, under a vaulted ceiling hung with chains and
    soul lanterns; polished basalt pillars round its rim; gilded blackstone in the floor's pattern.
  - A ring of lava channels round the middle, crossed by four short bridges, and in the middle, on a
    dais, the **Ember Anvil** (part 2 gives it its use; for now it's a fine-looking anvil that does
    what an anvil does).
  - The forge's tools: blast furnaces, smithing tables, cauldrons of lava, grindstones.
  - Two storerooms off the hall behind iron bars, a chest in each (`chests/cinder_forge`: gold,
    iron, a little netherite scrap, Fire Essence, blaze powder, enchanted books, now and then a
    Tome of Insight).
  - A **Forge Heart** under the anvil (hidden, unbreakable), as a tower has its heart.
- **The Forgewarden**: the first time a player (not a spectator) comes within 24 blocks of the
  heart, the forge's keeper stirs ("The forge's fires roar up. Its keeper wakes."): a construct of
  blackstone and magma, an Elemental Golem's body at 1.75 times the size, rising out of the hall's
  floor south of the anvil, with a boss bar of its own (its title, in red).
  - Slow, heavy: it walks at the nearest player and **slams** the floor (a ring of cinders that
    hurts and throws everyone near up), **hurls magma** (the mobs' Fireball, larger) at range,
    and every so often **vents**: fire bursts from its seams in a ring (set alight, knocked back).
  - Below half its health it's **molten**: faster, and it leaves burning ground where it walks.
  - Immune to fire and lava (it wades through lava as a strider does); Water and Ice hurt it the
    more (the element chart makes it so: it's Attuned to Fire, at the Adept rank, so its strength is
    the Nether's zone level, 25 to 50, not a Magus's or an Archmage's on top).
  - It fights the players in its forge and whatever strikes it; like any monster, its magic spares
    other monsters.
  - Falls with its loot: the **Ember Core** (one, and a second half the time on Hard), Fire
    Essence, gold, magma cream, blaze rods, experience. It doesn't come back: a forge is cleared
    once.
- Lit by lava and soul lanterns, so nothing spawns inside but what the forge calls.

## The Ember Core

A slow-pulsing heart of the forge's fire. In part 1:

- It crafts the **Forgefire Charm** (a chain, two gold ingots, the core and blaze powder), a
  carried charm the Charm Pouch takes: fire and lava harm you a third less, and you burn out twice
  as fast.
- It burns in a furnace as long as a bucket of lava.
- Part 2 (the Ember Anvil's tempering) will give spare cores their real use.

## Also

- The Seeker's Compass also seeks cinder forges, in the Nether (no rumours: a map bought in the
  Overworld can't show the Nether).
- Advancements: **Into the Forge** (find a Cinder Forge) and **Quench** (defeat a Forgewarden; a
  challenge).
- A journal page.

## Code

| Piece | What it does |
|---|---|
| `content/forge/ModForge` | The structure, its piece, the Forge Heart, the Forgewarden, the core and what it makes |
| `content/forge/CinderForgeStructure`, `CinderForgePiece` | The forge, code-built in world coordinates over the lava sea |
| `content/forge/ForgeHeartBlock(Entity)` | Wakes the Forgewarden once |
| `content/forge/ForgewardenEntity` | The boss: slam, hurl, vent, molten |
| `client/ForgewardenRenderer` | The golem model at 1.75 times the size, its own blackstone-and-magma skin, its seams glowing (wider when molten) |
| `content/forge/ForgeEvents` | The Forgefire Charm's ward |
| `api/ForgewardenRules` | Its numbers (tested) |
| `tools/gen_forge.py` | Its skin, the items' icons, the structure's data, loot, recipes |

## Testing

- `ForgewardenRulesTest`.
- `tools/server_tests/forges.txt`: a forge located in the Nether (by id and by the compass's
  tag); the Forgewarden summoned on a platform: an iron golem goes for it and it slams the golem
  down (health logged), a husk farther off strikes it and it hurls magma at it (a direct hit,
  health logged), and lava under it doesn't burn it.
- `tools/autotest/forges.txt`: the nearest forge, in the Nether, from inside its hall (a shot);
  the Forgewarden wakes (the message logged) and rises (a shot from the dais); below half its
  health, molten (a shot); killed, its Ember Core (counted).
