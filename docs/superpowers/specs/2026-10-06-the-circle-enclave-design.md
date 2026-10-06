# The Circle, part 1: the Enclave

## Why

The world's mages so far are lone Arcanists in villages, wandering mages on the road, and enemies:
Magisters in their towers, the Hollowed in their camps. There is nowhere mages gather, nowhere a
young mage sees magic practised and is welcomed. The Circle is the mages who keep the old oaths
(the Hollowed's opposite), and its Enclave is their refuge and school. Part 1 is the Enclave and its
people; part 2 brings the Archmagister's commissions and the Circle's marks to spend; part 3, the
duelling ring.

## The Enclave

- **Where**: rare, about one every 1,500 blocks, in plains, sunflower plains, meadows, forests and
  flower forests, kept clear of villages, shrines, mage towers and Hollowed camps (the `spread_away`
  placement).
- **What**: a walled compound 41 blocks square, the land shaped to it.
  - Walls of stone bricks four high with crenels, a squat tower at each corner with a lantern, a
    gate in the south wall under an arch.
  - In the middle the **Spire**: a round tower of white stone (calcite and quartz bricks, gold trim)
    with three floors, a ladder up the north wall through a trapdoor in each floor: the **library**
    (bookshelves, lecterns, a chest), the **study** (the
    Archmagister's desk and chair, shelves, an enchanting table, a brewing stand, the Circle's gold
    banners either side of the ladder), and the **observatory** under a glass dome at the top.
  - An **herb garden**: raised beds growing the eight herbs, each on its own ground.
  - A **duelling ring** (a circle of polished stone with benches round it; its use comes in part 3).
  - Two **cottages**, each with a bed and an Arcane Lectern, where Arcanists live.
  - A well east of the Spire, and practice targets west of it (a line to cast from).
  - Paths and lanterns.
- **Who**:
  - **Circle Mages**, four (each of an element of the eight), in white robes trimmed with their
    element's colour. They wander the Enclave and greet a player (use one: a line of talk, which
    changes as the player's story goes on), and they fight any monster that comes within 24 blocks
    of the Spire, the Hollowed above all: Attuned Magi casting their element's spells. A player's
    magic spares them (a projectile flies on through them), and theirs touches only monsters and
    whatever they're fighting, never players, their pets or one another (a water mage's healing
    tends players and the other mages instead of monsters). A player's aim looks past them, so a
    spell thrown at a monster behind one flies on to it. They never turn on a player (nor a pet),
    even one who strikes them.
  - **The Archmagister**, in the study (and keeping to it): old, in white and gold, an Archmage
    without a boss bar (a friend). Use the Archmagister: the Circle's welcome, by the player's story
    (part 2: commissions).
  - Two **Arcanists**, villagers of the mod's profession, at their lecterns.
  - They stay (the Enclave's people come when someone first comes near, as a tower's do). One who
    falls is replaced, one a day while someone is near: a new Archmagister in the study, or a mage
    (of an element the others lack) or an Arcanist who walks in at the gate.
- Lit throughout, so nothing spawns inside. Leaves that trees outside spread over the walls while
  the land was made are cleared when the Enclave's people come.

## Also

- The Seeker's Compass also seeks enclaves; Wandering Mages sell rumours of one.
- An advancement, **The Circle**: find an Enclave.
- A journal page.

## Code

| Piece | What it does |
|---|---|
| `content/circle/ModCircle` | The structure, its pieces, the mages' entity types, the Spire's heart block |
| `content/circle/EnclaveStructure`, `EnclavePiece` | The Enclave, code-built in world coordinates |
| `content/circle/CircleHeartBlock(Entity)` | A hidden block in the Spire that brings the Enclave's people when someone first comes near |
| `content/circle/CircleMageEntity` | A Circle Mage or the Archmagister: friendly, talks, defends |
| `api/CircleTalk` | What a mage says, by the player's story so far (tested) |
| `api/MageAlly` | Marks the mages' side |
| `api/SpellTargets` | The Circle spared by players' magic, even by a direct hit (`spares`), and players by theirs |
| `client/CircleMageRenderer` | The villager model in white robes |
| `tools/gen_circle.py` | Their skins, the structure's data, tags |

## Testing

- `CircleTalkTest`: lines by progress.
- `tools/server_tests/circle.txt`: an Enclave located (by its id and by the compass's tag); a Circle
  Mage summoned beside a zombie on a floor high up (the zombie falls).
- `tools/autotest/circle.txt`: the nearest Enclave from above (a shot), its people come (logged, a
  shot in the courtyard), a mage greeted twice (two lines logged, a shot), a player's Fireball through
  a mage at a husk (the mage unhurt and the husk hit, logged), the well and the targets (shots),
  the library (a shot), the study with the Archmagister (a shot), greeted (the line logged, a shot),
  a mage killed and replaced a day later (counted before and after).
- `tools/autotest/mp_smoke.txt`: a Circle Mage and the Archmagister drawn on a client joined to a
  dedicated server, and the mage greeted.
