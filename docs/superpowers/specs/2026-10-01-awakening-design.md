# Awakening (progression step 3): design

Date: 2026-10-01
Status: approved in conversation. It's step 3 of
`docs/superpowers/specs/2026-10-01-progression-system-design.md` ("Awakening"), and its numbers are
drafts to tune.

## Decisions

- **No affinity slots.** Rarity and cost are the only limit on how many elements you hold. The odds
  depend on your level and how many elements you already hold: very rare at first, much easier for a
  second element after level 10, hard for a third (level 20) and harder for a fourth (level 30).
- **Catalysts are crafted from Essence.**
- **The pick-your-element screen is gone.** Your first element is rolled for you.

## Your first element

Until it wakes you have dormant mana and no spells. The Status window just says so.

**What wakes it:**
- **A brush with an element.** You survive an elemental hit that leaves you at 3 hearts (6 health) or
  less:
  - fire, lava or burning (Fire)
  - drowning (Water)
  - freezing (the Water family)
  - a fall (Wind)

  Each brush has about a 1 in 3 chance of waking your magic.
- **A slow background chance:** once per Minecraft day, about 3%.
- **After day 10 the odds rise sharply.** The daily chance becomes about 25% and grows 15 points each
  further day, brushes become 3 times as likely (capped at certain), and it is **guaranteed by day 16**.
  That's about 5 hours of play at the very most.
- **Day count:** days since the player first joined the world, counted on the world's day clock, so
  sleeping through a night counts and joining an old world doesn't wake you at once. Going back in
  time (`/time set`) never lowers it.

**Which element you get:**
- A brush leans toward its element's family: Fire, Water, Wind, and Water's family for freezing.
- Within a family, a derived element (Ice) is **1/100 as likely** as the base one. So a freezing
  brush almost always gives Water, and Ice about once in a hundred wakings.
- The background roll picks any element, with an equal chance for each base element, and the same
  1/100 rule for derived ones.
- Elements with no content yet (Earth, until step 4) are left out of every roll.

**The moment:** a title, the awakening sound, particles in the element's color, a chat line saying
what woke it ("The flames did not burn you: your magic woke as Fire", or "Your magic woke on its
own"), and its starter spell selected.

**Existing worlds:** anyone already awakened keeps their elements. Anyone who isn't starts their day
count the first time they're seen after the update.

## Extra elements: Catalysts

- A **Catalyst** for each element family that isn't derived: Fire, Water and Wind (Earth in step 4).
  There is none for Ice, because Ice is reached through Water's tree.
- **Crafted** shapeless from **8 of that element's Essence and 1 diamond** (a recipe can't hold more
  than the 9 slots of the grid). The Water Catalyst takes Water Essence.
- **Used** by right-clicking while holding it, with no screen. It's **used up every time**, success or
  failure.

**The odds** per Catalyst, by how many element families you already hold:

| You hold | Base chance | Before the level milestone | Milestone |
|---|---|---|---|
| 1 (reaching for a 2nd) | 25% | 4% of base (about 1%) | level 10 |
| 2 (reaching for a 3rd) | 6% | 4% of base (about 0.25%) | level 20 |
| 3 (reaching for a 4th) | 1.5% | 4% of base (about 0.06%) | level 30 |

- Before its milestone the chance is 4% of the base. From the milestone it's the full base chance,
  rising 4% of the base for each level after it, up to 3 times the base.
- **Each failure** adds a bonus of 10% of the base chance to the next try, until a Catalyst works
  (kept per number of families held).
- A Catalyst of a family you already hold is refused and not used up. Ice and Water are one family.
- **Opposites:** Fire and Water families stay opposed until **level 50**. Using a Catalyst of an
  opposed family before that is refused, and it isn't used up.
- **Success:** the same moment as a first awakening, with a line saying what woke. **Failure:** a
  crumbling puff of the element's particles and a line saying it didn't take.

## What goes away

The Awakening screen and its offer when you join, the Awaken button in Status and the slot labels,
`MagicData.AFFINITY_SLOT_LEVELS` and the slot logic, and `AwakenPayload`.

## Code

| Unit | Job |
|---|---|
| `api/AwakeningRules` (pure Java, unit tested) | Chances (first awakening by day and cause, Catalyst by families held, level and failures), the weighted element pick, the day count |
| `api/Element` | `derived()` (Ice) and the family's base element; whether an element has content |
| `core/MagicData` | Awakening state: when the clock started (world day), the failure bonus per number of families held; saved and synced; `awaken` without slots |
| `content/AwakeningEvents` | Brush detection (a hit the player survives at 6 health or less, by damage type), the daily check (including the day-10 ramp and the day-16 guarantee), and the moment |
| `content/Catalysts` and `content/CatalystItem` | The items, their use and the recipes (data) |
| `core/ArcanaCommand` | `/arcana awaken <element>`, `/arcana awaken day <n>`, `/arcana awaken clock` |
| client | Status shows the awakening state and the odds hint; the Awakening screen is removed |

## Testing

- Unit tests for the odds and the weighted pick (including the 1/100 rule and the day-16
  guarantee).
- The `/arcana awaken` commands for hands-on testing: forcing an awakening, setting the day count,
  and reading the current odds.
- A play-test of a brush (survive a fall at low health), the day ramp, and a Catalyst.
