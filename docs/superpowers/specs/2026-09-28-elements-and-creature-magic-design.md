# Elements and Creature Magic: design

Date: 2026-09-28
Status: approved in conversation, awaiting spec review

## Goal

Give the four elements (Fire, Water, Ice, Wind) real meaning outside the player's own spells:

1. **Elements and matchups.** Elements relate to each other, creatures can have an element,
   and spell damage is stronger or weaker depending on it.
2. **Creature magic.** Magic is rare and special. Some creatures are born with an element; most
   hostile mobs are normal, but occasionally one spawns **Attuned**: it has an element, a rank, and
   casts spells.

The game never tells the player a creature's element. Players learn it by fighting: hits feel
different when they are strong or weak against the target.

## Part 1: Elements, damage and matchups

### Elements

A new `Element` enum: `FIRE`, `WATER`, `ICE`, `WIND`. Each spell school maps to one element.

**Opposed pairs** ("Fire vs the cold"): Fire is opposed to Water and to Ice. Water + Ice are
compatible, and Wind is compatible with everything. `Element#opposes(Element)` answers this.

What an opposed pair means for a **player's affinities** is deliberately left open (see Open
questions). This design only records which pairs are opposed.

### Elemental damage

Four new damage types, one per element:
`elementalarcana:fire_spell`, `water_spell`, `ice_spell`, `wind_spell`.

- Each is a data file under `data/elementalarcana/damage_type/`, with the same settings and the
  same damage-type tag memberships as vanilla `indirect_magic`, so armor, enchantments and death
  behave exactly as spell damage does today.
- A helper `SpellDamage.source(Element, direct, owner)` builds the damage source.
- **Every existing spell switches to it:** Fireball (explosion), Combustion explosions, Icicle,
  Frost Nova, Frost Shield retaliation, Hydro Jet (stream, splash, Tsunami Lance), Whirlpool, Wind
  Blade, Swirl damage (uses the swirled element), Tidal Wave, Flame Burst, Gale Dash/Updraft where
  they deal damage.
- Vanilla damage (swords, arrows, burning, freezing, drowning) has **no element**.

### Creature elements

A creature's element comes from exactly one place:

1. **Innate:** the entity type is in the tag `elementalarcana:innate/<element>`. It always has
   that element and can never have another.
2. **Attuned:** otherwise, the element stored on the creature when it spawned Attuned (Part 2).
3. Otherwise it has **no element**.

`CreatureElements.elementOf(LivingEntity)` returns the element or null. Players always return null
(player resistances are an open question).

Default innate tags:

| Tag | Entities |
|---|---|
| `innate/fire` | blaze, magma cube, ghast, strider |
| `innate/water` | drowned, guardian, elder guardian, squid, glow squid |
| `innate/ice` | stray, polar bear, snow golem |
| `innate/wind` | breeze, phantom, vex |

### Matchup chart

Applied in one place: a `LivingIncomingDamageEvent` handler reads the damage type's element and
the target's element and multiplies the damage.

| Spell ↓ / Creature → | Fire | Water | Ice | Wind | none |
|---|---|---|---|---|---|
| **Fire** | ×0.5 | ×0.5 | ×1.5 | ×1 | ×1 |
| **Water** | ×1.5 | ×0.5 | ×1 | ×1 | ×1 |
| **Ice** | ×0.5 | ×1.5 | ×0.5 | ×1 | ×1 |
| **Wind** | ×1 | ×1 | ×1 | ×0.5 | ×1 |

Water puts out fire, fire melts ice, ice freezes water, and every element resists itself. Wind has
no strengths or weaknesses; its role is Swirl. Reactions (Melt, Vaporize, Freeze) multiply
on top of the chart.

### Hit feedback instead of labels

When the chart changes a hit, the hit sounds and looks different, at most once every 10 ticks per
target (so Hydro Jet's rapid hits don't spam):

- **Strong (×1.5):** a bright, sharp sound and a burst of crit-like particles tinted in the spell
  element's color.
- **Resisted (×0.5):** a dull, low thud and a grey puff of smoke.

Sounds are vanilla and are chosen during playtesting (for example amethyst chimes for strong hits,
a low shield block for resisted ones). Nothing else ever reveals a creature's element.

## Part 2: Creature magic

### Which creatures can be Attuned

A creature can spawn Attuned only if its entity type is in the tag `elementalarcana:can_attune`.
Everything else (bosses, passive animals, villagers, golems, players, other mods' mobs until
added) is never Attuned.

Default `can_attune`: zombie, husk, zombie villager, drowned, skeleton, stray, bogged, wither
skeleton, spider, cave spider, creeper, witch, pillager, vindicator, piglin, zombified piglin,
slime, enderman, blaze, magma cube, breeze, guardian, phantom.

An Attuned **innate** creature keeps its innate element: a blaze can be a Fire Magus, never a Water
Magus. The elder guardian, the Wither, the Ender Dragon and the Warden are not in `can_attune`.

### Ranks

| Rank | Base chance | Needs a player at | Health | Spells | Extras |
|---|---|---|---|---|---|
| Adept | 5% | Magic Lv 1 (awakened) | normal | 1 | none |
| Magus | 1% | Magic Lv 5 | +50% | 2 | none |
| Archmage | 0.1% | Magic Lv 10 | ×3 | 3 | boss bar, never despawns, knockback resistant |

### Spawning

The roll happens when the creature finishes spawning (`FinalizeSpawnEvent`), only for natural,
chunk-generation, spawner, structure and patrol spawns. Spawn eggs, commands, breeding and
conversions never roll.

1. Find the nearest player within 128 blocks who has awakened magic. If there is none, the
   creature is normal. Magic creatures don't appear in a world where nobody has magic.
2. Roll from the rarest rank down: Archmage, then Magus, then Adept. A rank can only be rolled if
   that player's Magic Level meets the rank's requirement.
3. Chance for a rank = `base × min(3, 1 + distance from world spawn / 1000)`.
4. Pick the element:
   - Innate creatures always use their innate element.
   - Otherwise it is a weighted random pick. The biome's element has weight 4, and the others
     have weight 1 each. With no biome element, all four have equal weight.

Biome elements come from biome tags, so they are data-driven:

| Tag | Includes (by default) |
|---|---|
| `elementalarcana:attunes/fire` | Nether biomes, deserts, badlands |
| `elementalarcana:attunes/water` | oceans, rivers, swamps |
| `elementalarcana:attunes/ice` | snowy biomes |
| `elementalarcana:attunes/wind` | mountains, peaks, windswept biomes |

### What an Attuned creature is like

- **Stored data:** a saved (not client-synced) data attachment on the mob:
  `CreatureMagic { element, rank }`.
- **Health:** the rank's max-health bonus is applied as an attribute modifier when it spawns.
- **Faint hints:** Adepts and Magi occasionally (about every 2 seconds) give off one particle of
  their element: an ember, a drip, a snowflake or a wisp. There is no name and no label.
- **Archmage:**
  - A boss bar named "<Mob> Archmage" (for example "Zombie Archmage"), shown to players within 32
    blocks, like the Wither's.
  - It is marked persistent, so it never despawns.
  - It gets knockback resistance.
- **Keeps its normal attacks:** an Attuned skeleton still shoots arrows.

### Mob spells

Mobs don't use the player spell classes. A small `MobSpell` interface (element, rank that unlocks
it, cooldown, usable range, `cast(mob, target)`) has one implementation per spell below. They reuse
`SpellProjectile.shootFrom` (which already accepts any entity as owner), `FireField`, `Whirlpool`
and the reaction helpers.

A new AI goal, `CastMobSpellGoal`, is added to Attuned mobs when they join the level:

- **When it casts:** it needs a target in line of sight, within the spell's range, with the spell
  off cooldown. There is a shared minimum gap between any two casts.
- **Wind-up:** every cast has a **10-tick wind-up** before it fires, so players can see it coming
  and dodge. Particles of the element gather at the mob's hands and a sound plays.
- **Which spell:** it uses the highest-rank spell that is ready.

| Element | Adept | Magus adds | Archmage adds |
|---|---|---|---|
| Fire | Fireball | Burning ground under the target | Meteor (arcing, big blast) |
| Water | 1-second Hydro Jet burst (pushes, Wet) | Heals nearby monsters | Whirlpool at the target's feet |
| Ice | Icicle | Frost Nova when the target is close | Frost Shield on itself (absorbs damage) |
| Wind | Wind Blade | Gale Dash (dodges away or closes in) | Updraft (launches the target: Airborne) |

- **Damage:** starting values are about 60% of the equivalent player spell at Lv 1, tuned in
  playtesting.
- **Reactions work on players:** a Water Adept makes you Wet, and then an Ice Adept freezes you.
  A Freeze on a player lasts **20 ticks (1s)**; on creatures it stays 2.5s.

### Rewards

On death, if the killer is a player:

| Creature | Magic XP to the killer | Elemental Essence (its element) |
|---|---|---|
| Plain innate creature | 0 | 5% chance of 1 |
| Adept | 20 | 50% chance of 1 |
| Magus | 60 | 1 to 2 |
| Archmage | 250 | 3 to 5 |

**Elemental Essence** is four new items: Fire, Water, Ice and Wind Essence. Each has a 16×16 icon
and a tooltip. It has no use yet; it is a material reserved for later features (respec, affinity
unlocks, staffs).

## Testing tools

- **Dev menu (F6):** a new "Creature" row. Cycle an element and a rank, then press Spawn to summon
  an Attuned zombie in front of you.
- **Command:** `/arcana attune <targets> <element> <rank>` makes any existing mob Attuned; `none`
  clears it. It is used to test innate mobs and ranks.
- Each build step ends as usual: `./gradlew build`, a clean `runServer` start, a playtest in
  `runClient`, then commit and push after confirmation.

## Build order

Each step is tested and committed on its own.

1. **Elements, damage and matchups.**
   - The `Element` enum, the damage types, and `SpellDamage.source`.
   - Migrating every spell to elemental damage.
   - The innate tags, the matchup handler and the hit feedback.
   - *Test:* Hydro Jet on a blaze is strong (×1.5, bright sound). Fireball on a blaze is resisted
     (×0.5, dull thud). Icicle on a drowned is strong.
2. **Attunement.**
   - The `can_attune` tag, the biome tags, and the `CreatureMagic` attachment.
   - Spawn rolls, health bonuses, faint hints, and the Archmage boss bar.
   - The dev row and the command.
   - No spells yet.
3. **Mob spells**, one element at a time: Fire, Water, Ice, Wind (the wind-up, the goal, and each
   element's three spells).
4. **Rewards:** the Magic XP awards, the Essence items and their drops.

## Open questions (decided later)

- What a player's **clashing affinity** (Fire + Water, Fire + Ice) does: blocked, allowed with a
  cost, or something else.
- Whether a player **resists the elements of their own affinities**. Until then, the chart applies
  only to non-player creatures.
- What **Elemental Essence** is used for.

## Relationship to other work

These spell steps are paused, and resume after this work:
- Water's Bubble Prison and Tsunami (Tsunami also removes Tidal Wave).
- Fire's Ember Sprite and Pyronado.
- Wind's Gale Dash rework, Skyward Leap and Stormeye.

Mob spells are separate from these, so neither blocks the other.
