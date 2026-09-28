# Creature Attunement: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hostile mobs occasionally spawn **Attuned** as an Adept, Magus or Archmage of an element. They get bonus health, faint element hints and (for Archmages) a boss bar, and they are affected by the matchup chart and shown by Jade. They cast no spells yet.

**Architecture:**
- **Pure rules:** a pure-Java `AttunementRank` enum and `AttunementRules` (rank roll, element pick) are unit tested with JUnit.
- **Mob data:** a synced, saved data attachment `CreatureMagic { element, rank }` sits on the mob.
- **`Attunement` class:** owns attuning (health and knockback modifiers, persistence), the spawn roll (`FinalizeSpawnEvent`) and the hints. `ArchmageBossBars` shows the bars.
- **Element lookup:** `CreatureElements.elementOf` reads the attachment after the innate tags. The matchup chart and Jade pick up Attuned mobs with no changes to either.
- **Testing:** a reusable `tools/server_console.sh` types commands into a headless dev server, to verify results on a real server.

**Tech Stack:** NeoForge 21.1.252 for Minecraft 1.21.1, ModDevGradle 2.0.147, Java 21, Parchment 2024.11.17, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-28-elements-and-creature-magic-design.md`. This plan is **build step 2 (Attunement)**. Mob spells (step 3) and rewards (step 4) get their own plans.

## Global Constraints

- Mod id `elementalarcana`, package `com.chappadodle.elementalarcana`. Match the surrounding code style (Javadoc on classes and public methods, same comment density).
- Rank table from the spec:

  | Rank | Chance | Magic Level | Health |
  |---|---|---|---|
  | Adept | 5% | 1 | normal |
  | Magus | 1% | 5 | +50% |
  | Archmage | 0.1% | 10 | ×3, boss bar, never despawns, knockback resistant |

- Spawn chance = `base × min(3, 1 + distance from world spawn / 1000)`.
- Only natural, chunk-generation, spawner, structure and patrol spawns roll. The nearest player who has awakened magic, within 128 blocks, sets the Magic Level. With no such player, the mob is normal.
- Element choice: innate creatures always use their innate element. Otherwise the biome's element(s) get weight 4 and the others get weight 1.
- Nothing reveals an element except the optional Jade tooltip. Hints are single faint particles, with no names. The Archmage boss bar is **purple for every element**, so its color doesn't reveal the element.
- Vanilla sounds only.
- **Git:**
  - Never modify git config.
  - Commit only after the user play-tests and says to.
  - Commit with the env vars `GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com`.
  - Pass the message via a heredoc ending in `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Paths:**
  - All paths are relative to the repo root `elemental-arcana/`.
  - `.../` means `src/main/java/com/chappadodle/elementalarcana/`.

## Deviation from the spec (recorded in Task 6)

The spec says `CreatureMagic` is "saved (not client-synced)", and that Jade gets Attuned elements through a Jade server data provider. The mod's attachment system can already sync to everyone who sees an entity (the Frost Shield attachment does this). Syncing `CreatureMagic` lets the existing Jade tooltip work with no changes, and removes a whole component. The spec is updated to match.

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `tools/server_console.sh` | create | Headless dev server driven by a commands file (test harness) |
| `tools/server_tests/*.txt` | create | Command scripts used to verify this step |
| `.../api/AttunementRank.java` | create | Ranks: chance, required Magic Level, health bonus (no MC types) |
| `.../api/AttunementRules.java` | create | Rank roll and element pick (no MC types) |
| `src/test/java/.../api/AttunementRulesTest.java` | create | Unit tests for the rules |
| `.../api/CreatureMagic.java` | create | The attachment record, with its codecs |
| `.../core/MagicAttachments.java` | modify | Register `CREATURE_MAGIC` |
| `.../api/CreatureElements.java` | modify | `innateElementOf`, and attuned elements in `elementOf` |
| `.../content/Attunement.java` | create | Attune/clear, spawn roll, hints, dev spawn |
| `.../content/ArchmageBossBars.java` | create | Archmage boss bars |
| `.../core/ArcanaCommand.java` | modify | `/arcana attune <targets> <element> <rank>` and `/arcana attune <targets> none` |
| `.../network/DevActionPayload.java` | modify | `SPAWN_ATTUNED` action |
| `.../client/DevScreen.java` | modify | The "Creature" row |
| `src/main/resources/data/elementalarcana/tags/entity_type/can_attune.json` | create | Which mobs can be Attuned |
| `src/main/resources/data/elementalarcana/tags/worldgen/biome/attunes/{fire,water,ice,wind}.json` | create | Biome elements |
| `src/main/resources/assets/elementalarcana/lang/en_us.json` | modify | Command, dev, boss bar and rank text |
| `README.md`, the spec | modify | Docs |

---

### Task 1: Server console test harness

Gradle doesn't pass console input through to `runServer`. This script captures the dev server's own `java` command line once (from a normal `runServer` start), then reruns that command from `run/` with a commands file piped into its console.

**Files:**
- Create: `tools/server_console.sh`
- Create: `tools/server_tests/smoke.txt`

- [ ] **Step 1: Write the script**

Create `tools/server_console.sh`:

```bash
#!/usr/bin/env bash
# Runs the dev server headless, types console commands into it, and prints what the server says.
#
#   tools/server_console.sh tools/server_tests/<name>.txt
#
# The commands file has one console command per line (no leading slash). "sleep <seconds>" pauses;
# blank lines and lines starting with # are skipped. The server is stopped at the end.
#
# Gradle doesn't pass console input through to runServer, so the script captures the server's
# java command line once (from a normal runServer start) and reruns it with the input piped in.
# Linux only (it reads /proc).
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMMANDS="$(realpath "$1")"
CMDLINE="$ROOT/build/server-cmdline.bin"
LOG="$ROOT/build/server-console.log"
cd "$ROOT"

./gradlew classes --console=plain -q || exit 1

if [[ ! -s "$CMDLINE" || "$ROOT/build.gradle" -nt "$CMDLINE" ]]; then
  echo "Capturing the dev server's launch command..." >&2
  CAPTURE_LOG="$ROOT/build/server-capture.log"
  (timeout 180 ./gradlew runServer --console=plain > "$CAPTURE_LOG" 2>&1 &)
  timeout 180 bash -c "until grep -qE 'Done \(|Failed to start' '$CAPTURE_LOG'; do sleep 1; done"
  for pid in $(pgrep -x java); do
    if tr '\0' ' ' < "/proc/$pid/cmdline" | grep -q "devlaunch.Main"; then
      cp "/proc/$pid/cmdline" "$CMDLINE"
      kill "$pid"
      while kill -0 "$pid" 2>/dev/null; do sleep 0.5; done
    fi
  done
  [[ -s "$CMDLINE" ]] || { echo "Could not capture the server command (see $CAPTURE_LOG)" >&2; exit 1; }
fi

mapfile -d '' ARGS < "$CMDLINE"
FIFO="$(mktemp -u)"
mkfifo "$FIFO"
(cd "$ROOT/run" && exec timeout 300 "${ARGS[@]}" < "$FIFO" > "$LOG" 2>&1) &
SERVER=$!
exec 3>"$FIFO"
timeout 180 bash -c "until grep -qE 'Done \(|Failed to start' '$LOG'; do sleep 1; done"

while IFS= read -r line || [[ -n "$line" ]]; do
  [[ -z "$line" || "$line" == \#* ]] && continue
  if [[ "$line" == sleep\ * ]]; then
    sleep "${line#sleep }"
  else
    echo "$line" >&3
    sleep 0.5
  fi
done < "$COMMANDS"
echo "stop" >&3
exec 3>&-
wait "$SERVER"
rm -f "$FIFO"

# Everything the server said after starting, minus startup/shutdown noise.
sed -n '/Done (/,$p' "$LOG" \
  | grep -E '\[Server thread/(INFO|WARN|ERROR)\]|Exception' \
  | grep -vE 'Done \(|Stopping|Saving|saved|ThreadedAnvilChunkStorage|Loaded [0-9]+ language|Gametest|PermissionAPI|DualStackUtils' \
  | sed -E 's/^\[[0-9:]+\] \[Server thread\/[A-Z]+\] \[[^]]*\]: //'
```

Then run: `chmod +x tools/server_console.sh`

- [ ] **Step 2: Smoke test it**

Create `tools/server_tests/smoke.txt`:

```
# The harness can type a command and read the answer.
time query daytime
```

Run: `tools/server_console.sh tools/server_tests/smoke.txt`
Expected: one line like `The time is 1234`. Run it a second time: it must skip the capture step (no "Capturing…" message) and print the same kind of line.

---

### Task 2: Ranks and the attunement dice (pure, unit tested)

**Files:**
- Create: `.../api/AttunementRank.java`
- Create: `.../api/AttunementRules.java`
- Test: `src/test/java/com/chappadodle/elementalarcana/api/AttunementRulesTest.java`

**Interfaces:**
- Consumes: `Element` (existing).
- Produces:
  - `enum AttunementRank { ADEPT, MAGUS, ARCHMAGE }`, with `double baseChance()`, `int requiredMagicLevel()` and `double healthBonus()`.
  - `AttunementRules.distanceMultiplier(double) : double`
  - `@Nullable AttunementRules.rollRank(int magicLevel, double distanceFromSpawn, DoubleSupplier random) : AttunementRank`
  - `AttunementRules.pickElement(Set<Element> biomeElements, DoubleSupplier random) : Element`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/chappadodle/elementalarcana/api/AttunementRulesTest.java`:

```java
package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.DoubleSupplier;

import static com.chappadodle.elementalarcana.api.AttunementRank.ADEPT;
import static com.chappadodle.elementalarcana.api.AttunementRank.ARCHMAGE;
import static com.chappadodle.elementalarcana.api.AttunementRank.MAGUS;
import static com.chappadodle.elementalarcana.api.AttunementRules.distanceMultiplier;
import static com.chappadodle.elementalarcana.api.AttunementRules.pickElement;
import static com.chappadodle.elementalarcana.api.AttunementRules.rollRank;
import static com.chappadodle.elementalarcana.api.Element.FIRE;
import static com.chappadodle.elementalarcana.api.Element.ICE;
import static com.chappadodle.elementalarcana.api.Element.WATER;
import static com.chappadodle.elementalarcana.api.Element.WIND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttunementRulesTest {

    private static DoubleSupplier always(double value) {
        return () -> value;
    }

    @Test
    void ranksNeedTheirMagicLevel() {
        // A roll of 0 always hits, so the result is the rarest rank the level allows.
        assertEquals(ADEPT, rollRank(1, 0, always(0)));
        assertEquals(ADEPT, rollRank(4, 0, always(0)));
        assertEquals(MAGUS, rollRank(5, 0, always(0)));
        assertEquals(MAGUS, rollRank(9, 0, always(0)));
        assertEquals(ARCHMAGE, rollRank(10, 0, always(0)));
    }

    @Test
    void baseChancesAtWorldSpawn() {
        assertEquals(ADEPT, rollRank(1, 0, always(0.049)));
        assertNull(rollRank(1, 0, always(0.05)));
        assertEquals(MAGUS, rollRank(5, 0, always(0.0099)));
        assertEquals(ARCHMAGE, rollRank(10, 0, always(0.00099)));
    }

    @Test
    void chancesGrowWithDistanceUpToTriple() {
        assertEquals(1.0, distanceMultiplier(0), 1e-9);
        assertEquals(2.0, distanceMultiplier(1000), 1e-9);
        assertEquals(3.0, distanceMultiplier(2000), 1e-9);
        assertEquals(3.0, distanceMultiplier(50_000), 1e-9);
        assertEquals(ADEPT, rollRank(1, 1000, always(0.099)));
        assertNull(rollRank(1, 1000, always(0.1)));
        assertEquals(ADEPT, rollRank(1, 50_000, always(0.149)));
        assertNull(rollRank(1, 50_000, always(0.151)));
    }

    @Test
    void everyAllowedRankGetsItsOwnRoll() {
        // Archmage and Magus rolls miss, then the Adept roll hits: three rolls in total.
        double[] rolls = {0.5, 0.5, 0.01};
        int[] used = {0};
        assertEquals(ADEPT, rollRank(10, 0, () -> rolls[used[0]++]));
        assertEquals(3, used[0]);
    }

    @Test
    void elementsAreEvenWithoutABiomeElement() {
        assertEquals(FIRE, pickElement(Set.of(), always(0.0)));
        assertEquals(FIRE, pickElement(Set.of(), always(0.24)));
        assertEquals(WATER, pickElement(Set.of(), always(0.26)));
        assertEquals(ICE, pickElement(Set.of(), always(0.51)));
        assertEquals(WIND, pickElement(Set.of(), always(0.99)));
    }

    @Test
    void biomeElementIsFourTimesAsLikely() {
        // With Ice as the biome element the weights are Fire 1, Water 1, Ice 4, Wind 1 (total 7).
        assertEquals(FIRE, pickElement(Set.of(ICE), always(0.5 / 7)));
        assertEquals(WATER, pickElement(Set.of(ICE), always(1.5 / 7)));
        assertEquals(ICE, pickElement(Set.of(ICE), always(2.1 / 7)));
        assertEquals(ICE, pickElement(Set.of(ICE), always(5.9 / 7)));
        assertEquals(WIND, pickElement(Set.of(ICE), always(6.5 / 7)));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --console=plain`
Expected: FAIL at `compileTestJava` with `cannot find symbol` for `AttunementRank` and `AttunementRules`.

- [ ] **Step 3: Write `AttunementRank`**

Create `src/main/java/com/chappadodle/elementalarcana/api/AttunementRank.java`:

```java
package com.chappadodle.elementalarcana.api;

/**
 * How strong an Attuned creature is. Plain data with no Minecraft types: how rare each rank is,
 * the Magic Level a nearby player needs before it can appear, and its bonus max health (a share of
 * the base value: 0.5 = +50%, 2.0 = three times the health).
 */
public enum AttunementRank {
    ADEPT(0.05, 1, 0.0),
    MAGUS(0.01, 5, 0.5),
    ARCHMAGE(0.001, 10, 2.0);

    private final double baseChance;
    private final int requiredMagicLevel;
    private final double healthBonus;

    AttunementRank(double baseChance, int requiredMagicLevel, double healthBonus) {
        this.baseChance = baseChance;
        this.requiredMagicLevel = requiredMagicLevel;
        this.healthBonus = healthBonus;
    }

    /** Chance per eligible spawn, before the distance multiplier. */
    public double baseChance() {
        return baseChance;
    }

    public int requiredMagicLevel() {
        return requiredMagicLevel;
    }

    public double healthBonus() {
        return healthBonus;
    }
}
```

- [ ] **Step 4: Write `AttunementRules`**

Create `src/main/java/com/chappadodle/elementalarcana/api/AttunementRules.java`:

```java
package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.DoubleSupplier;

/**
 * The dice for creature attunement, free of Minecraft types so they can be unit tested. The spawn
 * hook (Attunement) supplies the nearby player's Magic Level, the distance from world spawn, the
 * biome's elements and a random source.
 */
public final class AttunementRules {
    // Chances grow with distance from world spawn: x2 at 1000 blocks, capped at x3.
    private static final double DISTANCE_PER_STEP = 1000;
    private static final double MAX_DISTANCE_MULTIPLIER = 3;
    private static final double BIOME_ELEMENT_WEIGHT = 4;
    private static final double OTHER_ELEMENT_WEIGHT = 1;
    private static final AttunementRank[] RAREST_FIRST = {AttunementRank.ARCHMAGE, AttunementRank.MAGUS, AttunementRank.ADEPT};

    private AttunementRules() {
    }

    public static double distanceMultiplier(double distanceFromSpawn) {
        return Math.min(MAX_DISTANCE_MULTIPLIER, 1 + distanceFromSpawn / DISTANCE_PER_STEP);
    }

    /**
     * Rolls a rank for a newly spawned creature, rarest first. Every rank the player's Magic Level
     * allows gets its own roll. Returns null when the creature spawns normal.
     */
    @Nullable
    public static AttunementRank rollRank(int magicLevel, double distanceFromSpawn, DoubleSupplier random) {
        double multiplier = distanceMultiplier(distanceFromSpawn);
        for (AttunementRank rank : RAREST_FIRST) {
            if (magicLevel >= rank.requiredMagicLevel() && random.getAsDouble() < rank.baseChance() * multiplier) {
                return rank;
            }
        }
        return null;
    }

    /** Picks an element: the biome's elements are 4 times as likely as the others (all even if none). */
    public static Element pickElement(Set<Element> biomeElements, DoubleSupplier random) {
        double total = 0;
        for (Element element : Element.values()) {
            total += weight(element, biomeElements);
        }
        double roll = random.getAsDouble() * total;
        for (Element element : Element.values()) {
            roll -= weight(element, biomeElements);
            if (roll < 0) {
                return element;
            }
        }
        return Element.values()[Element.values().length - 1];
    }

    private static double weight(Element element, Set<Element> biomeElements) {
        return biomeElements.contains(element) ? BIOME_ELEMENT_WEIGHT : OTHER_ELEMENT_WEIGHT;
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew test --console=plain`
Expected: `BUILD SUCCESSFUL`. Check the counts:

```bash
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' build/test-results/test/TEST-*.xml
```

This prints `tests="22" … failures="0"` (ElementTest) and `tests="6" … failures="0"` (AttunementRulesTest).

---

### Task 3: Attunement data, attuning, and `/arcana attune`

**Files:**
- Create: `.../api/CreatureMagic.java`
- Modify: `.../core/MagicAttachments.java`
- Modify: `.../api/CreatureElements.java`
- Create: `.../content/Attunement.java`
- Create: `src/main/resources/data/elementalarcana/tags/entity_type/can_attune.json`
- Modify: `.../core/ArcanaCommand.java`
- Modify: `src/main/resources/assets/elementalarcana/lang/en_us.json`
- Create: `tools/server_tests/attunement.txt`, `tools/server_tests/attunement_persist_1.txt`, `tools/server_tests/attunement_persist_2.txt`

**Interfaces:**
- Consumes: `Element`, `AttunementRank` (Task 2); `tools/server_console.sh` (Task 1).
- Produces:
  - `record CreatureMagic(Element element, AttunementRank rank)`, with `CODEC` and `STREAM_CODEC`
  - `MagicAttachments.CREATURE_MAGIC : Supplier<AttachmentType<CreatureMagic>>`
  - `CreatureElements.innateElementOf(Entity) : @Nullable Element`
  - `Attunement.CAN_ATTUNE : TagKey<EntityType<?>>`
  - `Attunement.get(Entity) : @Nullable CreatureMagic`
  - `Attunement.attune(Mob, Element, AttunementRank) : boolean` (false when an innate creature would get a different element)
  - `Attunement.clear(Mob) : boolean` (false if it wasn't Attuned)

- [ ] **Step 1: `CreatureMagic`**

Create `src/main/java/com/chappadodle/elementalarcana/api/CreatureMagic.java`:

```java
package com.chappadodle.elementalarcana.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

/** An Attuned creature's magic: its element and rank. Saved with the creature and synced to clients. */
public record CreatureMagic(Element element, AttunementRank rank) {
    public static final Codec<CreatureMagic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            enumCodec(Element.class).fieldOf("element").forGetter(CreatureMagic::element),
            enumCodec(AttunementRank.class).fieldOf("rank").forGetter(CreatureMagic::rank)
    ).apply(instance, CreatureMagic::new));

    public static final StreamCodec<ByteBuf, CreatureMagic> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> Element.values()[ordinal], Element::ordinal), CreatureMagic::element,
            ByteBufCodecs.VAR_INT.map(ordinal -> AttunementRank.values()[ordinal], AttunementRank::ordinal), CreatureMagic::rank,
            CreatureMagic::new);

    /** Saves an enum as its lowercase name ("fire", "archmage"). */
    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
            }
        }, value -> value.name().toLowerCase(Locale.ROOT));
    }
}
```

- [ ] **Step 2: Register the attachment**

In `.../core/MagicAttachments.java`, add after the `SHIELD` field:

```java
    // An Attuned creature's element and rank. Only Attuned creatures have it (check hasData; the
    // default below is never read). Saved, and synced to everyone who can see the creature, so the
    // Jade tooltip can show its element.
    public static final Supplier<AttachmentType<CreatureMagic>> CREATURE_MAGIC = ATTACHMENT_TYPES.register("creature_magic",
            () -> AttachmentType.builder(() -> new CreatureMagic(Element.FIRE, AttunementRank.ADEPT))
                    .serialize(CreatureMagic.CODEC)
                    .sync(CreatureMagic.STREAM_CODEC)
                    .build());
```

and the imports:

```java
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
```

- [ ] **Step 3: Attuned creatures have an element**

In `.../api/CreatureElements.java`, replace the whole `elementOf` method with these two methods:

```java
    /** The creature's element, or null if it has none: innate first, then Attuned. Works on both sides. */
    @Nullable
    public static Element elementOf(LivingEntity entity) {
        if (entity instanceof Player) {
            return null;
        }
        Element innate = innateElementOf(entity);
        if (innate != null) {
            return innate;
        }
        return entity.hasData(MagicAttachments.CREATURE_MAGIC) ? entity.getData(MagicAttachments.CREATURE_MAGIC).element() : null;
    }

    /** The element the creature's type is born with (the innate tags), or null. */
    @Nullable
    public static Element innateElementOf(Entity entity) {
        for (Element element : Element.values()) {
            if (entity.getType().is(INNATE.get(element))) {
                return element;
            }
        }
        return null;
    }
```

Add these imports: `com.chappadodle.elementalarcana.core.MagicAttachments` and `net.minecraft.world.entity.Entity`. In the class Javadoc, replace "Innate creatures are listed in the entity-type tags {@code elementalarcana:innate/<element>} and always have that element." with "Innate creatures are listed in the entity-type tags {@code elementalarcana:innate/<element>} and always have that element; Attuned creatures carry theirs in {@link CreatureMagic}."

- [ ] **Step 4: `Attunement` (attuning and clearing)**

Create `src/main/java/com/chappadodle/elementalarcana/content/Attunement.java`:

```java
package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

/**
 * Creature magic (design spec, Part 2): now and then a hostile mob spawns Attuned, with an element
 * and a rank. This class owns the attunement data and its health and knockback bonuses. The spawn
 * roll and the faint element hints are added below; spells come in a later step.
 */
public final class Attunement {
    /** Entity types that can spawn Attuned. */
    public static final TagKey<EntityType<?>> CAN_ATTUNE = TagKey.create(Registries.ENTITY_TYPE, ElementalArcana.id("can_attune"));
    private static final ResourceLocation HEALTH_BONUS = ElementalArcana.id("attunement_health");
    private static final ResourceLocation ARCHMAGE_KNOCKBACK = ElementalArcana.id("archmage_knockback");
    private static final double ARCHMAGE_KNOCKBACK_RESISTANCE = 0.75;

    private Attunement() {
    }

    /** The creature's attunement, or null if it isn't Attuned. */
    @Nullable
    public static CreatureMagic get(Entity entity) {
        return entity.hasData(MagicAttachments.CREATURE_MAGIC) ? entity.getData(MagicAttachments.CREATURE_MAGIC) : null;
    }

    /**
     * Makes {@code mob} Attuned, replacing any earlier attunement, and heals it to its new max
     * health. An innate creature can only be attuned to its own element: returns false otherwise.
     */
    public static boolean attune(Mob mob, Element element, AttunementRank rank) {
        Element innate = CreatureElements.innateElementOf(mob);
        if (innate != null && innate != element) {
            return false;
        }
        mob.setData(MagicAttachments.CREATURE_MAGIC, new CreatureMagic(element, rank));
        setBonus(mob, Attributes.MAX_HEALTH, HEALTH_BONUS, rank.healthBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mob.setHealth(mob.getMaxHealth());
        boolean archmage = rank == AttunementRank.ARCHMAGE;
        setBonus(mob, Attributes.KNOCKBACK_RESISTANCE, ARCHMAGE_KNOCKBACK,
                archmage ? ARCHMAGE_KNOCKBACK_RESISTANCE : 0, AttributeModifier.Operation.ADD_VALUE);
        if (archmage) {
            mob.setPersistenceRequired();
        }
        return true;
    }

    /** Makes {@code mob} normal again. Returns false if it wasn't Attuned. */
    public static boolean clear(Mob mob) {
        if (!mob.hasData(MagicAttachments.CREATURE_MAGIC)) {
            return false;
        }
        mob.removeData(MagicAttachments.CREATURE_MAGIC);
        setBonus(mob, Attributes.MAX_HEALTH, HEALTH_BONUS, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setBonus(mob, Attributes.KNOCKBACK_RESISTANCE, ARCHMAGE_KNOCKBACK, 0, AttributeModifier.Operation.ADD_VALUE);
        mob.setHealth(Math.min(mob.getHealth(), mob.getMaxHealth()));
        return true;
    }

    /** Replaces the modifier {@code id} on {@code attribute}; an amount of 0 just removes it. Saved with the mob. */
    private static void setBonus(Mob mob, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
```

- [ ] **Step 5: The `can_attune` tag**

Create `src/main/resources/data/elementalarcana/tags/entity_type/can_attune.json`:

```json
{
  "values": [
    "minecraft:zombie", "minecraft:husk", "minecraft:zombie_villager", "minecraft:drowned",
    "minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:wither_skeleton",
    "minecraft:spider", "minecraft:cave_spider", "minecraft:creeper", "minecraft:witch",
    "minecraft:pillager", "minecraft:vindicator", "minecraft:piglin", "minecraft:zombified_piglin",
    "minecraft:slime", "minecraft:enderman", "minecraft:blaze", "minecraft:magma_cube",
    "minecraft:breeze", "minecraft:guardian", "minecraft:phantom"
  ]
}
```

- [ ] **Step 6: `/arcana attune`**

In `.../core/ArcanaCommand.java`, replace the last line of the `register` chain:

```java
                        .executes(ctx -> modify(ctx.getSource(), MagicData::clearCooldowns, "commands.elementalarcana.cooldowns_reset")))));
```

with:

```java
                        .executes(ctx -> modify(ctx.getSource(), MagicData::clearCooldowns, "commands.elementalarcana.cooldowns_reset"))))
                .then(Commands.literal("attune").then(attuneTargets())));
```

Add these methods to the class, before `modify`:

```java
    /** /arcana attune <targets> <element> <rank>, or /arcana attune <targets> none. Mobs only. */
    private static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> attuneTargets() {
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targets = Commands.argument("targets", EntityArgument.entities());
        targets.then(Commands.literal("none")
                .executes(ctx -> clearAttunement(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"))));
        for (Element element : Element.values()) {
            LiteralArgumentBuilder<CommandSourceStack> elementNode = Commands.literal(element.name().toLowerCase(Locale.ROOT));
            for (AttunementRank rank : AttunementRank.values()) {
                elementNode.then(Commands.literal(rank.name().toLowerCase(Locale.ROOT))
                        .executes(ctx -> attune(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"), element, rank)));
            }
            targets.then(elementNode);
        }
        return targets;
    }

    private static int attune(CommandSourceStack source, Collection<? extends Entity> targets, Element element, AttunementRank rank) {
        int attuned = 0;
        int refused = 0;
        for (Entity entity : targets) {
            if (entity instanceof Mob mob) {
                if (Attunement.attune(mob, element, rank)) {
                    attuned++;
                } else {
                    refused++;
                }
            }
        }
        int done = attuned;
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.attuned", done), true);
        if (refused > 0) {
            source.sendFailure(Component.translatable("commands.elementalarcana.attune_innate", refused));
        }
        return done;
    }

    private static int clearAttunement(CommandSourceStack source, Collection<? extends Entity> targets) {
        int cleared = 0;
        for (Entity entity : targets) {
            if (entity instanceof Mob mob && Attunement.clear(mob)) {
                cleared++;
            }
        }
        int done = cleared;
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.attune_cleared", done), true);
        return done;
    }
```

Add these imports:

```java
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.Attunement;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import java.util.Collection;
import java.util.Locale;
```

Update the class Javadoc to read `/** /arcana: testing and admin shortcuts. Op level 2. */`, since `attune` acts on the targets and not on the command's player.

- [ ] **Step 7: Lang**

Add to `src/main/resources/assets/elementalarcana/lang/en_us.json`:

```json
  "commands.elementalarcana.attuned": "Attuned %s creature(s)",
  "commands.elementalarcana.attune_cleared": "Cleared the attunement of %s creature(s)",
  "commands.elementalarcana.attune_innate": "%s creature(s) skipped: innate creatures keep their own element",
  "rank.elementalarcana.adept": "Adept",
  "rank.elementalarcana.magus": "Magus",
  "rank.elementalarcana.archmage": "Archmage"
```

- [ ] **Step 8: Build**

Run: `./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`, with 28 unit tests passing.

- [ ] **Step 9: Verify on the server**

Create `tools/server_tests/attunement.txt`:

```
# Attuning, health bonuses, innate refusal and clearing (zombie base health 20).
time set midnight
forceload add 0 0
summon zombie 0.5 100 0.5 {NoAI:1b,Tags:["eatest"]}
summon blaze 3.5 100 0.5 {NoAI:1b,Tags:["eatest"]}
arcana attune @e[type=zombie,tag=eatest] fire magus
data get entity @e[type=zombie,tag=eatest,limit=1] Health
arcana attune @e[type=zombie,tag=eatest] ice archmage
data get entity @e[type=zombie,tag=eatest,limit=1] Health
data get entity @e[type=zombie,tag=eatest,limit=1] PersistenceRequired
data get entity @e[type=zombie,tag=eatest,limit=1] "neoforge:attachments"
arcana attune @e[type=blaze,tag=eatest] water adept
arcana attune @e[type=blaze,tag=eatest] fire adept
arcana attune @e[type=zombie,tag=eatest] none
data get entity @e[type=zombie,tag=eatest,limit=1] Health
kill @e[tag=eatest]
forceload remove 0 0
```

Run: `tools/server_console.sh tools/server_tests/attunement.txt`
Expected, in order:
- `Attuned 1 creature(s)`, then `Zombie has the following entity data: 30.0f` (Magus +50%)
- `Attuned 1 creature(s)`, then `…: 60.0f` (Archmage ×3), then `…: 1b` (persistent)
- the attachments: `{"elementalarcana:creature_magic": {element: "ice", rank: "archmage"}}`
- for the blaze with water: `Attuned 0 creature(s)`, then `1 creature(s) skipped: innate creatures keep their own element`
- for the blaze with fire: `Attuned 1 creature(s)`
- `Cleared the attunement of 1 creature(s)`, then `…: 20.0f`

- [ ] **Step 10: Verify it survives a save and restart**

Create `tools/server_tests/attunement_persist_1.txt`:

```
time set midnight
forceload add 0 0
summon zombie 0.5 100 0.5 {NoAI:1b,Tags:["eapersist"]}
arcana attune @e[tag=eapersist] wind archmage
save-all flush
```

Create `tools/server_tests/attunement_persist_2.txt`:

```
sleep 3
data get entity @e[tag=eapersist,limit=1] Health
data get entity @e[tag=eapersist,limit=1] "neoforge:attachments"
kill @e[tag=eapersist]
forceload remove 0 0
```

Run `tools/server_console.sh tools/server_tests/attunement_persist_1.txt`, then `tools/server_console.sh tools/server_tests/attunement_persist_2.txt`.
Expected from the second run:
- `Zombie has the following entity data: 60.0f`
- `{"elementalarcana:creature_magic": {element: "wind", rank: "archmage"}}`

---

### Task 4: Mobs spawn Attuned

**Files:**
- Modify: `.../content/Attunement.java`
- Create: `src/main/resources/data/elementalarcana/tags/worldgen/biome/attunes/fire.json`, `water.json`, `ice.json`, `wind.json`

**Interfaces:**
- Consumes: `AttunementRules.rollRank` and `pickElement` (Task 2); `Attunement.attune` and `CAN_ATTUNE` (Task 3); `CreatureElements.innateElementOf` (Task 3); `MagicAttachments.get(Player)`, `MagicData.isAwakened()` and `level()` (existing).

- [ ] **Step 1: The biome tags**

`src/main/resources/data/elementalarcana/tags/worldgen/biome/attunes/fire.json`:
```json
{ "values": ["#c:is_nether", "#c:is_desert", "#c:is_badlands"] }
```
`attunes/water.json`:
```json
{ "values": ["#c:is_ocean", "#c:is_river", "#c:is_swamp"] }
```
`attunes/ice.json`:
```json
{ "values": ["#c:is_snowy", "#c:is_icy"] }
```
`attunes/wind.json`:
```json
{ "values": ["#c:is_mountain", "#c:is_windswept"] }
```

A biome can match more than one of these (for example snowy peaks); then each of its elements gets weight 4.

- [ ] **Step 2: The spawn roll**

In `.../content/Attunement.java`, add these fields below `ARCHMAGE_KNOCKBACK_RESISTANCE`:

```java
    private static final Map<Element, TagKey<Biome>> BIOME_TAGS = new EnumMap<>(Element.class);
    // Only these spawns roll: never spawn eggs, commands, breeding or conversions.
    private static final Set<MobSpawnType> ROLLED_SPAWNS = EnumSet.of(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION,
            MobSpawnType.SPAWNER, MobSpawnType.STRUCTURE, MobSpawnType.PATROL);
    private static final double PLAYER_SEARCH_RADIUS = 128;

    static {
        for (Element element : Element.values()) {
            BIOME_TAGS.put(element, TagKey.create(Registries.BIOME,
                    ElementalArcana.id("attunes/" + element.name().toLowerCase(Locale.ROOT))));
        }
    }
```

Add these methods after `clear`:

```java
    /** Rolls whether a freshly spawned creature is Attuned (see AttunementRules for the odds). */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!ROLLED_SPAWNS.contains(event.getSpawnType()) || !mob.getType().is(CAN_ATTUNE)) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        Player player = level.getNearestPlayer(event.getX(), event.getY(), event.getZ(), PLAYER_SEARCH_RADIUS,
                entity -> entity instanceof Player nearby && MagicAttachments.get(nearby).isAwakened());
        if (player == null) {
            // Nobody nearby has magic: creatures stay normal.
            return;
        }
        BlockPos worldSpawn = level.getSharedSpawnPos();
        double distance = Math.hypot(event.getX() - worldSpawn.getX(), event.getZ() - worldSpawn.getZ());
        AttunementRank rank = AttunementRules.rollRank(MagicAttachments.get(player).level(), distance, mob.getRandom()::nextDouble);
        if (rank == null) {
            return;
        }
        Element innate = CreatureElements.innateElementOf(mob);
        Element element = innate != null ? innate : AttunementRules.pickElement(
                biomeElements(event.getLevel(), BlockPos.containing(event.getX(), event.getY(), event.getZ())), mob.getRandom()::nextDouble);
        attune(mob, element, rank);
    }

    /** The elements of the biome at {@code pos} (the attunes/<element> biome tags). */
    private static Set<Element> biomeElements(LevelReader level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        Set<Element> elements = EnumSet.noneOf(Element.class);
        BIOME_TAGS.forEach((element, tag) -> {
            if (biome.is(tag)) {
                elements.add(element);
            }
        });
        return elements;
    }
```

Add these imports:

```java
import com.chappadodle.elementalarcana.api.AttunementRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
```

Annotate the class so NeoForge registers its event handlers. Put `@EventBusSubscriber(modid = ElementalArcana.MODID)` on the line above `public final class Attunement {` (its import is in the list above). It can't go on earlier: NeoForge refuses a subscriber class that has no `@SubscribeEvent` methods, and this is the first one.

In the class Javadoc, replace "The spawn roll and the faint element hints are added below; spells come in a later step." with "It also rolls attunement when creatures spawn, and plays the faint element hints; spells come in a later step."

- [ ] **Step 3: Build, and check the server still starts**

Run: `./gradlew build --console=plain`, then `tools/server_console.sh tools/server_tests/smoke.txt`
Expected:
- `BUILD SUCCESSFUL`.
- The smoke test prints `The time is …`, with no `ERROR` or `Exception` lines. A bad biome tag file would show up here as a tag loading error.

The roll itself needs a real player, so it is checked in the Task 6 play-test with a zombie spawner.

---

### Task 5: Faint hints and Archmage boss bars

**Files:**
- Modify: `.../content/Attunement.java`
- Create: `.../content/ArchmageBossBars.java`
- Modify: `src/main/resources/assets/elementalarcana/lang/en_us.json`

**Interfaces:**
- Consumes: `Attunement.get(Entity)` (Task 3); `ModContent.EMBER` and `ModContent.WIND_STREAK` (existing).

- [ ] **Step 1: Hints**

In `.../content/Attunement.java`, add this field:

```java
    private static final int HINT_INTERVAL_TICKS = 40;
```

and these methods after `biomeElements`:

```java
    /** Adepts and Magi now and then give off one faint particle of their element (no label, ever). */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || (entity.tickCount + entity.getId()) % HINT_INTERVAL_TICKS != 0) {
            return;
        }
        CreatureMagic magic = get(entity);
        if (magic == null || magic.rank() == AttunementRank.ARCHMAGE) {
            return;
        }
        level.sendParticles(hintParticle(magic.element()), entity.getRandomX(0.5), entity.getRandomY(), entity.getRandomZ(0.5),
                1, 0, 0, 0, 0.01);
    }

    private static ParticleOptions hintParticle(Element element) {
        return switch (element) {
            case FIRE -> ModContent.EMBER.get();
            case WATER -> ParticleTypes.DRIPPING_WATER;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case WIND -> ModContent.WIND_STREAK.get();
        };
    }
```

Add these imports:

```java
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
```

- [ ] **Step 2: Boss bars**

Create `src/main/java/com/chappadodle/elementalarcana/content/ArchmageBossBars.java`:

```java
package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An Archmage announces itself with a boss bar ("Zombie Archmage") for players within 32 blocks,
 * like the Wither's. The bar is purple for every element, so it doesn't give the element away.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ArchmageBossBars {
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static final double VIEW_DISTANCE = 32;
    private static final int UPDATE_INTERVAL_TICKS = 5;

    private ArchmageBossBars() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().tickCount % UPDATE_INTERVAL_TICKS != 0 || !(event.getEntity() instanceof LivingEntity mob)
                || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        CreatureMagic magic = Attunement.get(mob);
        if (magic == null || magic.rank() != AttunementRank.ARCHMAGE) {
            return;
        }
        ServerBossEvent bar = BARS.computeIfAbsent(mob.getUUID(), id -> new ServerBossEvent(
                Component.translatable("bossbar.elementalarcana.archmage", mob.getType().getDescription()),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS));
        bar.setProgress(mob.getHealth() / mob.getMaxHealth());
        for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
            if (player.level() != level || player.distanceTo(mob) > VIEW_DISTANCE) {
                bar.removePlayer(player);
            }
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceTo(mob) <= VIEW_DISTANCE) {
                bar.addPlayer(player);
            }
        }
    }

    /** Death, despawn or chunk unload: take the bar down. */
    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        ServerBossEvent bar = BARS.remove(event.getEntity().getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BARS.clear();
    }
}
```

- [ ] **Step 3: Lang**

Add to `en_us.json`:

```json
  "bossbar.elementalarcana.archmage": "%s Archmage"
```

- [ ] **Step 4: Build and smoke test**

Run: `./gradlew build --console=plain`, then `tools/server_console.sh tools/server_tests/attunement.txt`
Expected: `BUILD SUCCESSFUL`, and the same output as Task 3 Step 9. The Archmage zombie now runs the boss bar code every 5 ticks without errors, and no `Exception` lines appear.

---

### Task 6: Dev menu row, docs, full verification, play-test, commit

**Files:**
- Modify: `.../content/Attunement.java` (dev spawn helper)
- Modify: `.../network/DevActionPayload.java`
- Modify: `.../client/DevScreen.java`
- Modify: `src/main/resources/assets/elementalarcana/lang/en_us.json`
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-09-28-elements-and-creature-magic-design.md`

**Interfaces:**
- Consumes: `Attunement.attune` (Task 3).
- Produces:
  - `Attunement.spawnForTesting(ServerPlayer, Element, AttunementRank) : void`
  - `DevActionPayload.Action.SPAWN_ATTUNED`
  - `DevActionPayload.spawnAttuned(Element, AttunementRank) : DevActionPayload`

- [ ] **Step 1: Dev spawn helper**

In `.../content/Attunement.java`, add after `clear`:

```java
    /** Dev menu: summons a zombie 3 blocks in front of the player and attunes it. */
    public static void spawnForTesting(ServerPlayer player, Element element, AttunementRank rank) {
        Vec3 ahead = player.position().add(Vec3.directionFromRotation(0, player.getYRot()).scale(3));
        Zombie zombie = EntityType.ZOMBIE.spawn(player.serverLevel(), BlockPos.containing(ahead), MobSpawnType.COMMAND);
        if (zombie != null) {
            attune(zombie, element, rank);
        }
    }
```

Add these imports: `net.minecraft.server.level.ServerPlayer`, `net.minecraft.world.entity.monster.Zombie` and `net.minecraft.world.phys.Vec3`.

- [ ] **Step 2: The dev action**

In `.../network/DevActionPayload.java`:
- Add `SPAWN_ATTUNED` at the end of the `Action` enum (after `CLEAR_BRANCHES`).
- Add this factory after `forSpell`:

```java
    /** Spawn a zombie Attuned to {@code element} at {@code rank}. */
    public static DevActionPayload spawnAttuned(Element element, AttunementRank rank) {
        return new DevActionPayload(Action.SPAWN_ATTUNED, rank.ordinal(), element.name());
    }
```

- Add this case to the `switch` in `handle`, after the `ADD_SPELL_LEVELS, FILL_MASTERY, CLEAR_BRANCHES` case:

```java
            case SPAWN_ATTUNED -> {
                Element element = parseElement(payload.target());
                int rank = payload.value();
                if (element != null && rank >= 0 && rank < AttunementRank.values().length) {
                    Attunement.spawnForTesting(player, element, AttunementRank.values()[rank]);
                }
            }
```

- Add this helper at the end of the class:

```java
    @Nullable
    private static Element parseElement(String name) {
        try {
            return Element.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
```

- Add these imports:

```java
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.Attunement;
import org.jetbrains.annotations.Nullable;
```

- [ ] **Step 3: The "Creature" row**

In `.../client/DevScreen.java`:

1. Change `private static final int ROWS = 6;` to `private static final int ROWS = 7;`.
2. Add these fields after `freeCastButton`:

```java
    private Element devElement = Element.FIRE;
    private AttunementRank devRank = AttunementRank.ADEPT;
    private Button elementButton;
    private Button rankButton;
```

3. In `init()`, right before `updateLabels();`, add:

```java
        // Spawn an Attuned zombie: pick the element and rank, then spawn.
        row(6);
        elementButton = cycleButton(56, () -> devElement = Element.values()[(devElement.ordinal() + 1) % Element.values().length]);
        rankButton = cycleButton(64, () -> devRank = AttunementRank.values()[(devRank.ordinal() + 1) % AttunementRank.values().length]);
        addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.dev.spawn_zombie"),
                        b -> PacketDistributor.sendToServer(DevActionPayload.spawnAttuned(devElement, devRank)))
                .bounds(cursorX, cursorY, 86, BUTTON_HEIGHT).build());
```

4. Add this method after `addButton`:

```java
    private Button cycleButton(int width, Runnable onPress) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), b -> {
            onPress.run();
            updateLabels();
        }).bounds(cursorX, cursorY, width, BUTTON_HEIGHT).build());
        cursorX += width + 3;
        return button;
    }
```

5. At the end of `updateLabels()`, add:

```java
        elementButton.setMessage(Component.translatable("school.elementalarcana." + devElement.name().toLowerCase(Locale.ROOT))
                .withColor(devElement.color()));
        rankButton.setMessage(Component.translatable("rank.elementalarcana." + devRank.name().toLowerCase(Locale.ROOT)));
```

6. In `render`, move the selected-spell line below the new row. Change `left + 10, rowY(5) + ROW_HEIGHT, 0xFF8A8A9A, false);` to `left + 10, rowY(ROWS - 1) + ROW_HEIGHT, 0xFF8A8A9A, false);`.
7. In `render`, change the labels array to `String[] labels = {"level", "xp", "mana", "elements", "other", "spell", "creature"};`.
8. Add these imports: `com.chappadodle.elementalarcana.api.AttunementRank`, `com.chappadodle.elementalarcana.api.Element` and `java.util.Locale`.

- [ ] **Step 4: Lang**

Add to `en_us.json`:

```json
  "screen.elementalarcana.dev.row.creature": "Creature",
  "screen.elementalarcana.dev.spawn_zombie": "Spawn zombie"
```

- [ ] **Step 5: README**

In `README.md`, right after the paragraph starting with `**Elements matter.**`, add:

```markdown
**Creature magic.** Once someone has awakened magic, hostile mobs nearby occasionally spawn **Attuned** to an element: an **Adept** (5%), a **Magus** (1%, needs Magic Level 5, +50% health) or, very rarely, an **Archmage** (0.1%, needs Magic Level 10, three times the health, a boss bar). They are more common far from world spawn (up to three times as likely), and their element leans toward the biome's. Attuned creatures are hit by the element chart like any elemental creature. Watch for a faint hint of their element.
```

In the operator commands sentence (`Operators can use …`), add `` `/arcana attune <targets> <element> <rank>|none` `` to the list, before the final item.

- [ ] **Step 6: Update the spec**

In `docs/superpowers/specs/2026-09-28-elements-and-creature-magic-design.md`, replace the bullet that starts with `- **Stored data:** a saved (not client-synced) data attachment` (all of its lines) with:

```markdown
- **Stored data:** a saved data attachment on the mob, `CreatureMagic { element, rank }`, synced to
  everyone who can see the mob (the same mechanism as the Frost Shield). Synced, `CreatureElements`
  knows Attuned elements on both sides, so the Jade tooltip needs no server data provider.
  (Changed during planning of step 2.)
```

In the **Archmage** bullet list, change `- A boss bar named "<Mob> Archmage" (for example "Zombie Archmage"), shown to players within 32 blocks, like the Wither's.` to `- A purple boss bar (the same color for every element) named "<Mob> Archmage" (for example "Zombie Archmage"), shown to players within 32 blocks, like the Wither's.`

- [ ] **Step 7: Full verification**

Run each of these:
- `./gradlew build --console=plain` → `BUILD SUCCESSFUL`, 28 tests.
- `tools/server_console.sh tools/server_tests/attunement.txt` → the Task 3 Step 9 output.
- `tools/server_console.sh tools/server_tests/attunement_persist_1.txt`, then `tools/server_console.sh tools/server_tests/attunement_persist_2.txt` → the Task 3 Step 10 output.
- `./gradlew runClient` in the background, logging to a file. Wait for `Sound engine started`, then grep the log for `Exception|ERROR` (ignore the vanilla goat-horn "Missing sound" warnings).

- [ ] **Step 8: Play-test checklist (hand this to the user)**

At night in a creative world, with Magic Level 10 (dev menu **Lv 10**) and an awakened element:
1. **Dev menu → Creature row.** Pick Fire + Adept, then **Spawn zombie**. Jade shows `Element: Fire`, and roughly every 2 seconds the zombie gives off a single ember.
2. **Magus:** Jade shows 30 health.
3. **Archmage:** Jade shows 60 health, and a purple **"Zombie Archmage"** boss bar appears. It drains as you hit the zombie. It disappears when you walk more than 32 blocks away, and when the zombie dies.
4. **Matchups on Attuned zombies:** Fireball on an Ice zombie is strong (bright crack); Fireball on a Fire zombie is resisted (dull thud).
5. **Natural spawns:** put a zombie spawner nearby with `/setblock ~3 ~ ~ minecraft:spawner{SpawnData:{entity:{id:"minecraft:zombie"}}}` and look at the zombies with Jade. Roughly 1 in 16 should be Attuned, and in a snowy biome most of those should be Ice. Then **Lv 1** in the dev menu: only Adepts appear from then on.
6. **No magic, no Attuned:** with **Reset** elements (no affinity), newly spawned zombies are never Attuned.
7. `/arcana attune @e[type=blaze,distance=..10] water adept` refuses: "innate creatures keep their own element".

- [ ] **Step 9: Commit and push, only after the user confirms it works**

```bash
git add README.md docs src tools
GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com git commit -F - <<'EOF'
Add creature attunement: Adept, Magus and Archmage mobs

Hostile mobs near players with magic now and then spawn Attuned to an
element (biome-weighted; innate mobs keep theirs), with health bonuses,
faint element hints and an Archmage boss bar. Attuned elements are
synced, so the matchup chart and Jade pick them up. Adds /arcana attune,
a dev-menu Creature row, and tools/server_console.sh for scripted
server tests.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git push
```
