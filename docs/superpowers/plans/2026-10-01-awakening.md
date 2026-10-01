# Awakening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Progression step 3 (`docs/superpowers/specs/2026-10-01-awakening-design.md`): your first
element is rolled (brushes with an element, a daily chance, a day-10 ramp, a day-16 guarantee),
extra elements come from crafted Catalysts, and the pick-your-element screen and affinity slots go.

**Architecture:** Every chance and the weighted element pick live in plain-Java `api/AwakeningRules`
(unit tested). `MagicData` keeps the clock (the world day the player was first seen, the last day
rolled) and the Catalyst failure bonus. `content/Awakenings` is the one place that wakes an element
(moment, title, sound, particles, chat line). `content/AwakeningEvents` detects brushes and runs the
daily check; `content/CatalystItem` rolls extra elements.

**Tech Stack:** NeoForge 21.1.252 (Minecraft 1.21.1), Java 21, JUnit 5, Python 3 (item textures).

## Global Constraints

- Brush: survive an elemental hit at 6 health or less; 1/3 chance, certain from day 10; at most one
  brush per player per 5 seconds. Fire/lava/burning, drowning, freezing, falls.
- Daily chance: 3% before day 10; day 10 25%, +15 points a day, so certain from day 15; guaranteed
  by day 16. Days count on the world's day clock from when the player was first seen.
- Element pick: base element weight 100, derived element (Ice) weight 1, within the family the
  brush leans to (freezing and drowning: Water's family); the background roll picks across families
  equally, then within the family 100:1.
- Catalyst chance: base 25% / 6.25% / 1.5625% for the 2nd / 3rd / 4th family; before the milestone
  (level 10 / 20 / 30) it's 4% of base; from it, base × min(3, 1 + 0.04 × (level − milestone));
  plus 10% of base per earlier failure (per number of families held); never above certain.
- Catalyst: Fire, Water, Wind (not Ice); shapeless, **8** Essence of its element + 1 diamond (fills
  the 3×3 grid; a recipe can't hold 16 of one ingredient); used up on every try.
- Opposed families locked until level 50 (AffinityRules); a refused Catalyst is not used up.
- No affinity slots. Never modify git config; no commits until the user play-tests.

---

## File map

| File | Responsibility |
|---|---|
| `api/AwakeningRules.java` (new) | Chances, day count, weighted pick |
| `api/Element.java` (modify) | `derived()` |
| `core/MagicData.java` (modify) | Clock and failure state; slots removed; `awaken` without slots |
| `content/Awakenings.java` (new) | The awakening moment; Catalyst use |
| `content/AwakeningEvents.java` (new) | Brushes and the daily check |
| `content/CatalystItem.java`, `content/ModItems.java` (modify) | Catalysts |
| `core/ArcanaCommand.java` (modify) | `/arcana awaken ...` |
| `client/StatusScreen.java`, `client/ArcanaClient.java` (modify) | No slots, no screen |
| `client/AwakeningScreen.java`, `network/AwakenPayload.java` (delete) | Replaced |
| `tools/gen_textures.py`, item models, recipes, lang, README | Assets and text |

---

### Task 1: Rules (pure Java)

**Interfaces (produces):**
```java
public final class AwakeningRules {
    public static final int DAY_TICKS = 24000;
    public static final int RAMP_DAY = 10;
    public static final int GUARANTEE_DAY = 16;
    public static final double DERIVED_WEIGHT = 0.01;           // relative to a base element
    public static int day(long dayTime, long startDayTime);     // max(0, (dayTime - start) / DAY_TICKS)
    public static double dailyChance(int day);
    public static double brushChance(int day);
    public static Map<Element, Double> weights(Element family); // base 100, derived 1, for one family
    public static Map<Element, Double> anyWeights();            // every family equally (family weight 100), then 100:1 inside
    public static @Nullable Element pick(Map<Element, Double> weights, double roll);
    public static int milestoneLevel(int familiesHeld);         // 10 * familiesHeld
    public static double catalystBase(int familiesHeld);        // 0.25 / 4^(held - 1)
    public static double catalystChance(int familiesHeld, int level, int failures);
}
```
`Element.derived()` is `this == ICE`. Tests: `AwakeningRulesTest` covers `day` (never negative),
`dailyChance` (day 0 = 0.03, 9 = 0.03, 10 = 0.25, 12 = 0.55, 15 = 1, 16 = 1), `brushChance`
(1/3 before day 10, 1 from it), `weights(WATER)` (Water 100, Ice 1; pick at 0.5 → Water, at 0.999 →
Ice), `anyWeights` (the per-family total is equal: Fire 100, Wind 100, Water 99.01 + Ice 0.99), the
pick of a null/empty map, `catalystChance` (held 1 at level 5 = 0.01; at 10 = 0.25; at 20 = 0.25 ×
1.4 = 0.35; at 60 = 0.25 × 3 = 0.75; held 2 at level 19 = 0.0625 × 0.04; with 3 failures at level
10 = 0.25 + 0.075; capped at 1) and `milestoneLevel`/`catalystBase`.

### Task 2: Player state and the moment

- `MagicData`: new saved + synced `awakeningStart` (long, -1 = not started), `lastRolledDay`
  (int, -1), `catalystFailures` (`List<Integer>`, index = families held − 1). Accessors:
  `awakeningStart()`, `startAwakeningClock(long dayTime)` (only if unset), `setAwakeningStart(long)`,
  `lastRolledDay()`, `setLastRolledDay(int)`, `failures(int held)`, `addFailure(int held)`,
  `resetFailures(int held)`, `familiesHeld()` (distinct `family()` of `affinityElements()`),
  `holdsFamily(Element)`.
- Remove `AFFINITY_SLOT_LEVELS`, `affinitySlots`, `hasFreeAffinitySlot`; `awaken(school)` keeps the
  already-held and opposed checks only.
- `Awakenings.wake(ServerPlayer, Element, Component cause)`: awaken the element's school, sync,
  title/subtitle, awakening sound, totem particles, a chat line `cause`, starter spell selected (as
  `MagicData.awaken` already does); `Awakenings.force(...)` skips the opposed check for commands.
- Delete `AwakenPayload`, `AwakeningScreen`; remove the slot message in `CastingService.onLevelUp`.

### Task 3: First awakening events

`AwakeningEvents` (`@EventBusSubscriber`):
- `PlayerTickEvent.Post`, once a second (`tickCount % 20 == 0`), unawakened survival players: start
  the clock at `level.getDayTime()` if unset; on a new day number roll `dailyChance(day)` and
  `Awakenings.wake(player, pick(anyWeights()), "awakening.timer")`; first sight only records the day.
- `LivingDamageEvent.Post` on a `ServerPlayer`: unawakened, alive, `getHealth() <= 6`, damage type in
  IS_FIRE → Fire family, IS_DROWNING and IS_FREEZING → Water family, IS_FALL → Wind family, 5 s
  throttle (a `Map<UUID, Long>` of the game time after which another brush counts); roll
  `brushChance(day)`; pick from `weights(family)`; cause lang `awakening.brush.<type>`.

### Task 4: Catalysts

- `CatalystItem(Element family)` registered as `<element>_catalyst` for Fire, Water, Wind (foil
  rarity RARE, name in the element's colour, tooltip). `use`: on the server, `Awakenings.useCatalyst`:
  refused (message, not consumed) if not awakened, if the family is already held, or if
  `data.opposedBy(school) != null`; else consume one (not in creative) and roll
  `catalystChance(familiesHeld, level, failures(held))`: success → `wake` + `resetFailures`; failure →
  `addFailure`, particles of the element's colour, message `awakening.catalyst.fail`.
- Recipes `data/elementalarcana/recipe/<element>_catalyst.json`: `minecraft:crafting_shapeless`, 8×
  the element's Essence + `minecraft:diamond`.
- Creative tab: Ingredients. Item models + `gen_textures.py` catalyst textures (a small glowing
  vial in the element's colours). Lang.

### Task 5: Commands, Status, docs

- `/arcana awaken <element>` forces, `/arcana awaken day <n>` sets the clock so today is day n,
  `/arcana awaken clock` prints day, daily/brush odds and each Catalyst's odds.
- `StatusScreen`: no Awaken button or slot labels: badges for held elements, and a dormant-magic
  note when none. `ArcanaClient`: K opens Status; no join offer.
- README, lang, roadmap memory; full build, all server tests, hand off for play-testing.
