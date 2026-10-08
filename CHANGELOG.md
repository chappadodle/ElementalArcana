# Changelog

## 1.0.0 — the Far Isles — 2026-10-08

The first full release: the story runs from your first shrine to the End's far isles, and its
bosses have been measured and tuned.

### The Far Isles

- **Starfallen Observatories** on the End's outer islands, 1000 blocks and more from the centre:
  a terrace under four pillars, a **Star Lens** at each pillar's foot and an **Astral Orrery** in
  the middle. The orrery's chart is set in the floor, a line of colour toward each lens: turn the
  lenses to match and the orrery wakes, and its vault rises. The Seeker's Compass finds them.
- **Stargazers**, tall robed figures Attuned to Radiance, drift over the outer islands. They leave
  you be unless you strike them or look them in the face. Their **Stardust** makes the
  **Voidwalker's Charm**: carried, a fall into the void carries you back to solid ground, once
  every five minutes.
- The **Astral Chart** (from the vaults) shows the way home from the far isles.
- Caelith's tale has an epilogue, **Beyond the Last Sky**, for those who bound the Hollow (and
  worlds where the tale was already told find it waiting).

### Balance

- Measured with a new gauge (`/arcana gauge start|stop`, for your own play-tests too): the story's
  bosses were out of reach of a player of the place they stand in, with the Archmage's +20 levels
  on top of it. Now they stand 10 levels above their place: a crypt's Revenant or a tower's
  Magister in a level-15 land is level 25 (was 35), the Sovereigns 40 and up (were 50), the Hollow
  60 (was 70) with about 750 health (was 1020).
- Sovereigns' own spells hit a fifth softer. The Magister blinks away at most every 8 seconds when
  hurt (was 4). The Forgewarden's slam, vents and burning are softer.
- The End's central island, where the dragon waits, is level 40 (was 60); the End rises to 65 on
  the outer islands and 85 at most.

### Changes

- A held fireball keeps out of its caster's way in first person: smaller, low in the corners of
  the screen, and Sunfire's sun hangs high ahead. Others see it as before.
- Phoenix has a bird's cry (a parrot's blaze call and the dragon's wingbeat) instead of the
  phantom's.

### Fixes

- A crash at Hydro Jet level 8 and up with bloom on.

## 0.11.0 — the Ember Reaches — 2026-10-07

The Nether gets its own magic.

### Cinder Forges

- **The forges**: the first mages' smithies in the nether wastes, carved out of the netherrack over
  the lava sea: a vaulted hall of blackstone, a ring of lava round an anvil on a dais, the forge's
  tools, storerooms behind iron bars, tunnels out to the caves. The Seeker's Compass finds them in
  the Nether.
- **The Forgewarden**, the forge's keeper, wakes when you come near: a construct of blackstone and
  magma nearly twice a golem's size that slams the floor, hurls magma at those who keep their
  distance, vents fire all round it and turns molten at half its strength. Fire and lava don't touch
  it; water and ice hurt it most.
- **Ember Cores** it leaves make the **Forgefire Charm** (fire and lava harm you a third less, and you
  burn out twice as fast) and burn in a furnace as long as a bucket of lava.

### Tempering

- Every forge's dais holds an **Ember Anvil** (and you can make one with an Ember Core). Use it with a
  wand, a staff or a piece of a mage's robe, an Ember Core and five levels of experience: the piece
  is tempered, each of its stats one higher, up to three tempers.

### Creatures of the Nether

- **Ash Wraiths** drift over the soul sand valleys, casting fire spells; once you burn, their touch
  withers you. Water and ice tear them. Their Soul Ash makes the **Ashen Shroud**: carried, the
  undead don't notice you unless you strike them first.
- **Cinder Hounds** hunt the hot wastes in packs of two to four; their bite burns, and hurting one
  brings the pack. Their fangs make the **Houndstooth Charm**: carried, you run a sixth faster in the
  Nether.

### Caelith's tale

- Two new chapters between the Hollowed and the seals: **The Ones Who Stayed** (find an Enclave of
  the Circle) and **Where Fire Was Worked** (find a Cinder Forge in the Nether). The tale now has 17
  chapters.
- A player's place in the tale is saved by chapter, so new chapters never move anyone; worlds from
  0.9 and 0.10 keep their place.

### Changes

- The Charm Pouch now takes the Hungerward Charm.
- JEI info pages for the Circle's and the Ember Reaches' items.

## 0.10.0 — the Circle — 2026-10-06

### The Circle

- **Enclaves**: rare walled refuges of the mages who keep the old oaths, in plains, meadows and
  forests: a white Spire (a library, the Archmagister's study, an observatory under a glass dome),
  an herb garden of all eight herbs, a duelling ring, two Arcanists' cottages, a well and practice
  targets. The Seeker's Compass finds them, and Wandering Mages sell rumours of them.
- **Circle Mages**: four mages of four elements keep each Enclave. They greet you with a word that
  follows your story and fight any monster that comes near, the Hollowed above all. Your spells fly
  through them and theirs through you; a water mage even mends your wounds mid-fight. One who falls
  is replaced the next day.
- **Commissions**: the Archmagister has work for any mage whose magic is awake: a deed against the
  Hollow's servants or the world's great foes, harder as your story goes on, paid in **Marks of the
  Circle**.
- **The Circle's stores** (sneak and use the Archmagister): Tomes of Insight, Scrolls of Unbinding,
  the Hungerward Charm, brews, Wisp Motes, rumours of crypts, sanctums and sky isles, and the
  **Sigil of the Circle**, which calls a Circle Mage to fight at your side for two minutes.
- **Duels**: hold out a Mark to a Circle Mage and duel for it in the Enclave's ring, a fair fight
  no one dies of (whoever is brought below a fifth of their health yields), fought at your own
  level. Win three, and the Archmagister will face you.

### Changes

- A held spell now aims past anyone your magic spares (a pet, a Circle Mage) at the creature behind
  them.
- The mobs' water jets and healing take sides: monsters heal monsters, and the Circle's water mages
  heal players and one another.
- An Archmage on your side shows no boss bar.

## 0.9.0 (first beta) — 2026-10-06

Elemental Arcana's first public build: innate elemental magic that turns Minecraft into a fantasy
adventure, for NeoForge 1.21.1.

### Magic

- **Eight elements.** Fire, Water, Wind and Earth, and the kin elements they give rise to: Ice,
  Crystal, Lightning and Radiance. An element chart decides what hurts what, and reactions (Melt,
  Vaporize, Freeze, Swirl, Crystallize) reward mixing them.
- **Awakening.** Magic sleeps in everyone until it wakes: a brush with an element, a Catalyst, or
  the day it stirs on its own.
- **Spells that grow.** Every spell levels from 1 to 10, forks into one of two paths at level 5 and
  ends in a capstone; most are held to charge and conjure. Among them: Fireball, Ember Sprite,
  Pyronado, Icicle, Frost Shield, Hydro Jet, Bubble Prison, Tsunami, Wind Blade, Gale Dash,
  Skyward Leap (with gliding), Stormeye, Boulder, Stone Skin, Tremor, Prism Bolt, Prism Ward,
  Geode Sentinel, Chain Lightning, Thunderclap, Stormcall, Smite, Sanctuary and Dawnbreak.
- **Cantrips**, everyday magic of the Arcane school, learned from scrolls.
- **Progression**: a magic level with stats (Potency, Affinity, Focus, Ward, Reservoir, Vitality,
  Insight), a skill tree with notables and rule-breaking keystones, and mana that reads the world:
  mana weather, mana tides, places of power and dead zones.

### Gear and craft

- Elemental foci (wands and staves) and mage's robes in four tiers, the last two made from what
  Magisters and Sovereigns guard.
- Brews: Mana Draughts and the Elixirs of Clarity, Focus and Warding.
- Arcane Infusion: bind an element into weapons and armour at an altar.
- Relics of the first mages; Trophies of the Wild (charms that teach a creature's trick); the Charm
  Pouch that carries nine of them; the Seeker's Compass, which finds the world's places.
- **Arcane Flora**: eight herbs, one for each element, growing where their element is strong.
  They brew into tonics of Kinship, which make that element's spells stronger and its magic softer
  on you.

### The world

- **Places**: elemental shrines (with ley lines between them, and Ley Anchors to bring them home),
  mage towers and their Magisters, the four Sovereigns' sanctums, arcane crypts with rune gates and
  Revenants, weathered ruins with pages of the old story, drake nests, sky isles, wisp rings, and
  the camps of the Hollowed.
- **Creatures**: Attuned mobs that cast spells, wisps (and wisp familiars), elemental golems,
  drakes of five elements (hatch one, raise it and ride it), the Creatures of the Wild (treant,
  frost wraith, salamander, gale harpy, crystal crawler, bog lurker), the mana-eating Hollowed, and
  the gentle wonders: glowmoths and skyrays.
- **Events**: elemental rifts, falling stars, wandering mages with rumours to sell, Hollowed patrols
  at dusk.
- **People**: the Arcanist, a village mage who trades in magic and posts bounties.

### Story

- Caelith, the voice in the Sending Stone, leads you through the whole mod, from your first shrine
  to the Hollow: the hunger the Sovereigns bound, which you must bind again with the Prime Key.
- The Arcanist's Journal explains everything as you go; an advancement tab marks the way.

### Everything else

- Server settings to tune or turn off every event and creature that comes on its own.
- Optional support for JEI (recipes and info pages), Jade, Veil bloom and dynamic lights.
- Tested in single player and on a dedicated server.
