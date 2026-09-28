# Elements, Elemental Damage and Matchups: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every spell deals damage of its element. Innate creatures (blaze, drowned, stray, breeze…) take ×1.5 or ×0.5 from the matchup chart, and those hits sound and look different, without the game ever naming a creature's element.

**Architecture:**
- **Element:** a pure-Java `Element` enum holds the opposed pairs and the chart, and is unit tested with JUnit.
- **Damage types:** four data-driven damage types (`elementalarcana:<element>_spell`) replace `indirect_magic` in every spell, created through `SpellDamage.source(...)`.
- **Matchup handler:** one `LivingIncomingDamageEvent` handler reads the damage's element and the target's element (from `innate/<element>` entity-type tags), scales the damage and plays the hit feedback.

**Tech Stack:** NeoForge 21.1.252 for Minecraft 1.21.1, ModDevGradle 2.0.147, Java 21, Parchment 2024.11.17, JUnit 5 (new, unit tests only).

**Spec:** `docs/superpowers/specs/2026-09-28-elements-and-creature-magic-design.md`. This plan covers **build step 1 (Part 1)** only. Attunement, mob spells and rewards get their own plans.

## Global Constraints

- Mod id `elementalarcana`, package `com.chappadodle.elementalarcana`, MIT license.
- Match the surrounding code style: Javadoc on public classes and methods, same comment density, `SpellDamage.hurtMultiHit` for all spell damage.
- Vanilla sounds only (the user prefers simple vanilla sounds).
- Nothing in the UI, HUD or tooltips may reveal a creature's element.
- Player targets are never affected by the chart; `CreatureElements.elementOf(player)` is always null.
- **Git:**
  - Never modify git config.
  - Commit only after the user has play-tested and said to commit.
  - Commit with the env vars `GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com`.
  - Pass the message via a heredoc ending in `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
  - Never commit `build/` or `run/`.
- All paths below are relative to the repo root `elemental-arcana/`.

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `build.gradle` | modify | JUnit 5 for `src/test` |
| `src/main/java/.../api/Element.java` | create | The 4 elements, opposed pairs, matchup chart (no Minecraft types) |
| `src/test/java/.../api/ElementTest.java` | create | Unit tests for `Element` |
| `src/main/java/.../api/SpellDamage.java` | modify | `source(level, element, direct, owner)`, `elementOf(DamageSource)` |
| `src/main/resources/data/elementalarcana/damage_type/{fire,water,ice,wind}_spell.json` | create | The 4 damage types |
| `src/main/resources/data/minecraft/tags/damage_type/{bypasses_armor,bypasses_wolf_armor,panic_causes,witch_resistant_to}.json` | create | Same vanilla tag memberships as `indirect_magic` |
| `src/main/resources/data/neoforge/tags/damage_type/is_magic.json` | create | Same NeoForge tag membership as `indirect_magic` |
| `src/main/resources/assets/elementalarcana/lang/en_us.json` | modify | Death messages |
| every spell / effect that deals damage (Task 3 list) | modify | Use elemental damage |
| `src/main/java/.../api/CreatureElements.java` | create | A creature's element (innate tags) |
| `src/main/resources/data/elementalarcana/tags/entity_type/innate/{fire,water,ice,wind}.json` | create | Innate creatures |
| `src/main/java/.../content/ElementalMatchups.java` | create | Applies the chart and the hit feedback |
| `README.md` | modify | Player-facing explanation |

`...` = `com/chappadodle/elementalarcana`.

---

### Task 1: JUnit and the `Element` enum

**Files:**
- Modify: `build.gradle` (the `dependencies { }` block, and a new block after it)
- Create: `src/main/java/com/chappadodle/elementalarcana/api/Element.java`
- Test: `src/test/java/com/chappadodle/elementalarcana/api/ElementTest.java`

**Interfaces:**
- Produces: `enum Element { FIRE, WATER, ICE, WIND }` with `int color()`, `boolean opposes(Element other)`, `float multiplierAgainst(@Nullable Element target)`.

- [ ] **Step 1: Add JUnit 5 to the build**

In `build.gradle`, add these two lines inside the existing `dependencies { ... }` block (after the commented examples):

```groovy
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

And add this block right after the `dependencies { ... }` block:

```groovy
tasks.named('test', Test).configure {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Write the failing test**

Create `src/test/java/com/chappadodle/elementalarcana/api/ElementTest.java`:

```java
package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementTest {

    @Test
    void fireOpposesTheCold() {
        assertTrue(Element.FIRE.opposes(Element.WATER));
        assertTrue(Element.FIRE.opposes(Element.ICE));
        assertTrue(Element.WATER.opposes(Element.FIRE));
        assertTrue(Element.ICE.opposes(Element.FIRE));
    }

    @Test
    void everythingElseIsCompatible() {
        assertFalse(Element.WATER.opposes(Element.ICE));
        assertFalse(Element.ICE.opposes(Element.WATER));
        for (Element element : Element.values()) {
            assertFalse(Element.WIND.opposes(element));
            assertFalse(element.opposes(Element.WIND));
            assertFalse(element.opposes(element));
        }
    }

    // The full chart from the spec: spell element, creature element (NONE = no element), multiplier.
    @ParameterizedTest
    @CsvSource({
            "FIRE, FIRE, 0.5", "FIRE, WATER, 0.5", "FIRE, ICE, 1.5", "FIRE, WIND, 1.0", "FIRE, NONE, 1.0",
            "WATER, FIRE, 1.5", "WATER, WATER, 0.5", "WATER, ICE, 1.0", "WATER, WIND, 1.0", "WATER, NONE, 1.0",
            "ICE, FIRE, 0.5", "ICE, WATER, 1.5", "ICE, ICE, 0.5", "ICE, WIND, 1.0", "ICE, NONE, 1.0",
            "WIND, FIRE, 1.0", "WIND, WATER, 1.0", "WIND, ICE, 1.0", "WIND, WIND, 0.5", "WIND, NONE, 1.0",
    })
    void matchupChart(Element spell, String creature, float expected) {
        Element target = creature.equals("NONE") ? null : Element.valueOf(creature);
        assertEquals(expected, spell.multiplierAgainst(target), 1e-6f);
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew test --console=plain`
Expected: FAIL at `compileTestJava` with `cannot find symbol ... class Element`.

- [ ] **Step 4: Write the implementation**

Create `src/main/java/com/chappadodle/elementalarcana/api/Element.java`:

```java
package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

/**
 * The four elements. Plain data with no Minecraft types: which elements oppose each other, and how
 * hard a spell of one element hits a creature of another (see ElementalMatchups).
 */
public enum Element {
    FIRE(0xFF7A1F),
    WATER(0x3F9CFF),
    ICE(0x9EE6FF),
    WIND(0x8FE3C0);

    private static final float STRONG = 1.5f;
    private static final float RESISTED = 0.5f;

    private final int color;

    Element(int color) {
        this.color = color;
    }

    /** The element's signature color, as 0xRRGGBB. */
    public int color() {
        return color;
    }

    /** Fire is opposed to Water and to Ice ("Fire vs the cold"); every other pair is compatible. */
    public boolean opposes(Element other) {
        return this == FIRE && (other == WATER || other == ICE)
                || other == FIRE && (this == WATER || this == ICE);
    }

    /**
     * Damage multiplier for a spell of this element hitting a creature of {@code target}'s element
     * (null = a creature with no element). Water beats fire, fire beats ice, ice beats water, and
     * every element resists itself.
     */
    public float multiplierAgainst(@Nullable Element target) {
        if (target == null) {
            return 1f;
        }
        if (target == this) {
            return RESISTED;
        }
        return switch (this) {
            case FIRE -> target == ICE ? STRONG : target == WATER ? RESISTED : 1f;
            case WATER -> target == FIRE ? STRONG : 1f;
            case ICE -> target == WATER ? STRONG : target == FIRE ? RESISTED : 1f;
            case WIND -> 1f;
        };
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew test --console=plain`
Expected: `BUILD SUCCESSFUL`. Confirm the 22 test cases ran: `ls build/test-results/test/` contains `TEST-com.chappadodle.elementalarcana.api.ElementTest.xml`, and `grep -o 'tests="[0-9]*"' build/test-results/test/TEST-*ElementTest.xml` prints `tests="22"`.

---

### Task 2: Elemental damage types

**Files:**
- Create: `src/main/resources/data/elementalarcana/damage_type/fire_spell.json`, `water_spell.json`, `ice_spell.json`, `wind_spell.json`
- Create: `src/main/resources/data/minecraft/tags/damage_type/bypasses_armor.json`, `bypasses_wolf_armor.json`, `panic_causes.json`, `witch_resistant_to.json`
- Create: `src/main/resources/data/neoforge/tags/damage_type/is_magic.json`
- Modify: `src/main/java/com/chappadodle/elementalarcana/api/SpellDamage.java`
- Modify: `src/main/resources/assets/elementalarcana/lang/en_us.json`

**Interfaces:**
- Consumes: `Element` (Task 1).
- Produces:
  - `SpellDamage.damageType(Element) : ResourceKey<DamageType>`
  - `SpellDamage.source(Level level, Element element, @Nullable Entity direct, @Nullable Entity owner) : DamageSource`
  - `@Nullable SpellDamage.elementOf(DamageSource) : Element`

- [ ] **Step 1: Write the four damage type files**

`src/main/resources/data/elementalarcana/damage_type/fire_spell.json`:

```json
{
  "exhaustion": 0.0,
  "message_id": "elementalarcana.fire_spell",
  "scaling": "when_caused_by_living_non_player"
}
```

Create `water_spell.json`, `ice_spell.json` and `wind_spell.json` the same way. Each file is identical to the one above, except that `message_id` is `elementalarcana.water_spell`, `elementalarcana.ice_spell` or `elementalarcana.wind_spell`.

- [ ] **Step 2: Give them `indirect_magic`'s tag memberships**

Vanilla `indirect_magic` is in `minecraft:bypasses_armor`, `bypasses_wolf_armor`, `panic_causes` and `witch_resistant_to`, and NeoForge puts it in `neoforge:is_magic`. Create each of these five files with this content:

```json
{
  "replace": false,
  "values": [
    "elementalarcana:fire_spell",
    "elementalarcana:water_spell",
    "elementalarcana:ice_spell",
    "elementalarcana:wind_spell"
  ]
}
```

The five paths:
- `src/main/resources/data/minecraft/tags/damage_type/bypasses_armor.json`
- `src/main/resources/data/minecraft/tags/damage_type/bypasses_wolf_armor.json`
- `src/main/resources/data/minecraft/tags/damage_type/panic_causes.json`
- `src/main/resources/data/minecraft/tags/damage_type/witch_resistant_to.json`
- `src/main/resources/data/neoforge/tags/damage_type/is_magic.json`

Do **not** add them to `minecraft:is_fire`. That tag makes fire-immune mobs immune to the damage, and fire spells should only be *resisted* by fire creatures (×0.5).

- [ ] **Step 3: Add `source` and `elementOf` to `SpellDamage`**

Replace the whole of `src/main/java/com/chappadodle/elementalarcana/api/SpellDamage.java` with:

```java
package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class SpellDamage {
    private static final Map<Element, ResourceKey<DamageType>> TYPES = new EnumMap<>(Element.class);

    static {
        for (Element element : Element.values()) {
            TYPES.put(element, ResourceKey.create(Registries.DAMAGE_TYPE,
                    ElementalArcana.id(element.name().toLowerCase(Locale.ROOT) + "_spell")));
        }
    }

    private SpellDamage() {
    }

    /** The damage type {@code elementalarcana:<element>_spell}. */
    public static ResourceKey<DamageType> damageType(Element element) {
        return TYPES.get(element);
    }

    /**
     * Spell damage of an element. {@code direct} is what touched the target (a projectile, or the
     * caster for touch/area spells) and {@code owner} is who cast it. Pass at least one entity: death
     * messages name it.
     */
    public static DamageSource source(Level level, Element element, @Nullable Entity direct, @Nullable Entity owner) {
        // DamageSources#source hands its first entity on as the direct one and its second as the causing one.
        return level.damageSources().source(TYPES.get(element), direct, owner);
    }

    /** The element of a spell's damage, or null for any other damage (swords, arrows, burning…). */
    @Nullable
    public static Element elementOf(DamageSource source) {
        for (Element element : Element.values()) {
            if (source.is(TYPES.get(element))) {
                return element;
            }
        }
        return null;
    }

    /**
     * Damages {@code target} even if it was hit a moment ago. After any hit, Minecraft ignores
     * further damage for 10 ticks (unless it's bigger, and then only the difference counts), so
     * several projectiles from one volley would otherwise land as a single hit.
     */
    public static boolean hurtMultiHit(Entity target, DamageSource source, float amount) {
        target.invulnerableTime = 0;
        return target.hurt(source, amount);
    }
}
```

- [ ] **Step 4: Add the death messages**

In `src/main/resources/assets/elementalarcana/lang/en_us.json`, add these keys. Keep the file valid JSON: add them after the last existing entry, with a comma after it.

```json
  "death.attack.elementalarcana.fire_spell": "%1$s was burned to ash by %2$s",
  "death.attack.elementalarcana.fire_spell.player": "%1$s was burned to ash while fighting %2$s",
  "death.attack.elementalarcana.fire_spell.item": "%1$s was burned to ash by %2$s using %3$s",
  "death.attack.elementalarcana.water_spell": "%1$s was swept away by %2$s",
  "death.attack.elementalarcana.water_spell.player": "%1$s was swept away while fighting %2$s",
  "death.attack.elementalarcana.water_spell.item": "%1$s was swept away by %2$s using %3$s",
  "death.attack.elementalarcana.ice_spell": "%1$s was frozen solid by %2$s",
  "death.attack.elementalarcana.ice_spell.player": "%1$s was frozen solid while fighting %2$s",
  "death.attack.elementalarcana.ice_spell.item": "%1$s was frozen solid by %2$s using %3$s",
  "death.attack.elementalarcana.wind_spell": "%1$s was cut down by %2$s's winds",
  "death.attack.elementalarcana.wind_spell.player": "%1$s was cut down by the wind while fighting %2$s",
  "death.attack.elementalarcana.wind_spell.item": "%1$s was cut down by %2$s's winds using %3$s"
```

- [ ] **Step 5: Verify it builds and the damage types load**

Run: `./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`. Unit tests still pass.

Run the dedicated server check (it exits by itself after 150s at most):

```bash
L=build/server-check.log; (timeout 150 ./gradlew runServer --console=plain > $L 2>&1 &); timeout 150 bash -c "until grep -qE 'Done \(|Exception' $L; do sleep 2; done"; grep -E "Done \(|Exception|ERROR|damage_type" $L | head; pkill -f "runServer|net.neoforged.*server"; true
```

Expected: a `Done (…)! For help` line, and no `ERROR` line mentioning `damage_type` or `elementalarcana`. A malformed damage type file makes the server fail to load datapacks here.

---

### Task 3: Every spell deals elemental damage

**Files (all under `src/main/java/com/chappadodle/elementalarcana/`):**
- Modify: `api/ElementalReactions.java` (the `swirl` method, the 3 `hurtMultiHit` calls)
- Modify: `content/FireEvents.java` (`onDeath`)
- Modify: `content/Whirlpool.java` (`tick`)
- Modify: `content/spell/FireballSpell.java` (`explode`)
- Modify: `content/spell/FlameBurstSpell.java` (`cast`)
- Modify: `content/spell/IcicleSpell.java` (`onHitEntity`, `shatterburst`)
- Modify: `content/spell/FrostNovaSpell.java` (`cast`)
- Modify: `content/spell/FrostShieldSpell.java` (the shatter damage, around line 170)
- Modify: `content/spell/HydroJetSpell.java` (`hitCreature`, `splash`, `onHitEntity`)
- Modify: `content/spell/TidalWaveSpell.java` (`cast`)
- Modify: `content/spell/WindBladeSpell.java` (the blade hit, around line 268)

**Interfaces:**
- Consumes: `SpellDamage.source(Level, Element, Entity, Entity)` (Task 2), `Element` (Task 1).

Each change below replaces only the damage-source argument; amounts and everything else stay the same. Add `import com.chappadodle.elementalarcana.api.Element;` to each file outside the `api` package. Also add `import com.chappadodle.elementalarcana.api.SpellDamage;` wherever it isn't imported yet.

- [ ] **Step 1: Fire**

`content/spell/FireballSpell.java`, in `explode`:
```java
            SpellDamage.hurtMultiHit(target, fireball.damageSources().indirectMagic(fireball, owner), hit);
```
becomes
```java
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.FIRE, fireball, owner), hit);
```

`content/spell/FlameBurstSpell.java`, in `cast`:
```java
            target.hurt(level.damageSources().indirectMagic(caster, caster), 5f * context.power());
```
becomes
```java
            target.hurt(SpellDamage.source(level, Element.FIRE, caster, caster), 5f * context.power());
```

`content/FireEvents.java`, in `onDeath` (Combustion explosions). The dead creature is the source, so the death message names it:
```java
            SpellDamage.hurtMultiHit(nearby, level.damageSources().onFire(), DAMAGE);
```
becomes
```java
            SpellDamage.hurtMultiHit(nearby, SpellDamage.source(level, Element.FIRE, dead, dead), DAMAGE);
```

- [ ] **Step 2: Water**

`content/spell/HydroJetSpell.java`. In `hitCreature` **and** in `splash`, the two calls that read:
```java
caster.damageSources().indirectMagic(caster, caster)
```
become:
```java
SpellDamage.source(level, Element.WATER, caster, caster)
```
In `onHitEntity` (the Tsunami Lance):
```java
        SpellDamage.hurtMultiHit(target, lance.damageSources().indirectMagic(lance, owner), damage);
```
becomes
```java
        SpellDamage.hurtMultiHit(target, SpellDamage.source(lance.level(), Element.WATER, lance, owner), damage);
```

`content/Whirlpool.java`, in `tick`:
```java
                SpellDamage.hurtMultiHit(entity, owner == null ? level.damageSources().magic()
                        : level.damageSources().indirectMagic(owner, owner), damage);
```
becomes
```java
                Entity source = owner != null ? owner : entity;
                SpellDamage.hurtMultiHit(entity, SpellDamage.source(level, Element.WATER, source, source), damage);
```

`content/spell/TidalWaveSpell.java`, in `cast`:
```java
                target.hurt(level.damageSources().drown(), 5f * context.power());
```
becomes
```java
                target.hurt(SpellDamage.source(level, Element.WATER, caster, caster), 5f * context.power());
```

- [ ] **Step 3: Ice**

`content/spell/IcicleSpell.java`. In `onHitEntity`:
```java
        SpellDamage.hurtMultiHit(target, icicle.damageSources().indirectMagic(icicle, icicle.getOwner()), damage(icicle, tag));
```
becomes
```java
        SpellDamage.hurtMultiHit(target, SpellDamage.source(icicle.level(), Element.ICE, icicle, icicle.getOwner()), damage(icicle, tag));
```
and in `shatterburst`:
```java
            SpellDamage.hurtMultiHit(nearby, icicle.damageSources().indirectMagic(icicle, icicle.getOwner()), damage);
```
becomes
```java
            SpellDamage.hurtMultiHit(nearby, SpellDamage.source(level, Element.ICE, icicle, icicle.getOwner()), damage);
```

`content/spell/FrostNovaSpell.java`, in `cast`:
```java
            target.hurt(level.damageSources().indirectMagic(caster, caster), 3f * context.power());
```
becomes
```java
            target.hurt(SpellDamage.source(level, Element.ICE, caster, caster), 3f * context.power());
```

`content/spell/FrostShieldSpell.java`, the shatter damage:
```java
                SpellDamage.hurtMultiHit(nearby, player.damageSources().indirectMagic(player, player), damage);
```
becomes
```java
                SpellDamage.hurtMultiHit(nearby, SpellDamage.source(player.level(), Element.ICE, player, player), damage);
```

- [ ] **Step 4: Wind, and Swirl**

`content/spell/WindBladeSpell.java`, the blade hit:
```java
        SpellDamage.hurtMultiHit(target, blade.damageSources().indirectMagic(blade, owner), damage(blade, tag, target));
```
becomes
```java
        SpellDamage.hurtMultiHit(target, SpellDamage.source(blade.level(), Element.WIND, blade, owner), damage(blade, tag, target));
```

`api/ElementalReactions.java`, in `swirl`. Swirl damage has the swirled element. Add this line right before the `for (LivingEntity nearby : ...)` loop:
```java
        Entity source = attacker != null ? attacker : target;
```
Then, inside the `switch (aura)`, replace the three damage sources:
- `level.damageSources().freeze()` → `SpellDamage.source(level, Element.ICE, source, source)`
- `level.damageSources().inFire()` → `SpellDamage.source(level, Element.FIRE, source, source)`
- `level.damageSources().magic()` → `SpellDamage.source(level, Element.WATER, source, source)`

- [ ] **Step 5: Verify nothing still uses the old sources**

Run:
```bash
grep -rn "indirectMagic\|damageSources()\.\(magic\|freeze\|inFire\|onFire\|drown\)" src/main/java
```
Expected: no output.

Run: `./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`.

---

### Task 4: Innate creatures, the matchup chart and hit feedback

**Files:**
- Create: `src/main/resources/data/elementalarcana/tags/entity_type/innate/fire.json`, `water.json`, `ice.json`, `wind.json`
- Create: `src/main/java/com/chappadodle/elementalarcana/api/CreatureElements.java`
- Create: `src/main/java/com/chappadodle/elementalarcana/content/ElementalMatchups.java`
- Modify: `src/main/java/com/chappadodle/elementalarcana/content/spell/HydroJetSpell.java` (`hitCreature`, the water-sensitivity line)

**Interfaces:**
- Consumes: `Element` (Task 1); `SpellDamage.elementOf(DamageSource)` (Task 2).
- Produces:
  - `CreatureElements.innateTag(Element) : TagKey<EntityType<?>>`
  - `@Nullable CreatureElements.elementOf(LivingEntity) : Element`

  Build step 2 (attunement) will extend `elementOf` to also read attuned creatures.

- [ ] **Step 1: The innate tags**

`src/main/resources/data/elementalarcana/tags/entity_type/innate/fire.json`:
```json
{
  "values": ["minecraft:blaze", "minecraft:magma_cube", "minecraft:ghast", "minecraft:strider"]
}
```
`innate/water.json`:
```json
{
  "values": ["minecraft:drowned", "minecraft:guardian", "minecraft:elder_guardian", "minecraft:squid", "minecraft:glow_squid"]
}
```
`innate/ice.json`:
```json
{
  "values": ["minecraft:stray", "minecraft:polar_bear", "minecraft:snow_golem"]
}
```
`innate/wind.json`:
```json
{
  "values": ["minecraft:breeze", "minecraft:phantom", "minecraft:vex"]
}
```

- [ ] **Step 2: `CreatureElements`**

Create `src/main/java/com/chappadodle/elementalarcana/api/CreatureElements.java`:

```java
package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Which element a creature has. Innate creatures are listed in the entity-type tags
 * {@code elementalarcana:innate/<element>} and always have that element. Players never have one
 * here. The game never shows a creature's element; it only changes how spells hit it.
 */
public final class CreatureElements {
    private static final Map<Element, TagKey<EntityType<?>>> INNATE = new EnumMap<>(Element.class);

    static {
        for (Element element : Element.values()) {
            INNATE.put(element, TagKey.create(Registries.ENTITY_TYPE,
                    ElementalArcana.id("innate/" + element.name().toLowerCase(Locale.ROOT))));
        }
    }

    private CreatureElements() {
    }

    /** The entity-type tag of creatures born with {@code element}. */
    public static TagKey<EntityType<?>> innateTag(Element element) {
        return INNATE.get(element);
    }

    /** The creature's element, or null if it has none. */
    @Nullable
    public static Element elementOf(LivingEntity entity) {
        if (entity instanceof Player) {
            return null;
        }
        for (Element element : Element.values()) {
            if (entity.getType().is(INNATE.get(element))) {
                return element;
            }
        }
        return null;
    }
}
```

- [ ] **Step 3: `ElementalMatchups`**

Create `src/main/java/com/chappadodle/elementalarcana/content/ElementalMatchups.java`:

```java
package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellDamage;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

/**
 * The matchup chart (see Element#multiplierAgainst): spell damage is scaled by the target
 * creature's element. Strong and resisted hits sound and look different, which is the only way a
 * player learns a creature's element.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ElementalMatchups {
    private static final String TAG_FEEDBACK_UNTIL = "ea_matchup_fx_until";
    // Rapid hits (Hydro Jet) show the feedback at most this often per target.
    private static final int FEEDBACK_GAP_TICKS = 10;

    private ElementalMatchups() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Element spell = SpellDamage.elementOf(event.getSource());
        if (spell == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        float multiplier = spell.multiplierAgainst(CreatureElements.elementOf(target));
        if (multiplier == 1f) {
            return;
        }
        event.setAmount(event.getAmount() * multiplier);
        if (target.level() instanceof ServerLevel level) {
            feedback(level, target, spell, multiplier > 1f);
        }
    }

    private static void feedback(ServerLevel level, LivingEntity target, Element spell, boolean strong) {
        CompoundTag data = target.getPersistentData();
        long now = level.getGameTime();
        if (now < data.getLong(TAG_FEEDBACK_UNTIL)) {
            return;
        }
        data.putLong(TAG_FEEDBACK_UNTIL, now + FEEDBACK_GAP_TICKS);
        double x = target.getX();
        double y = target.getY(0.6);
        double z = target.getZ();
        if (strong) {
            // A bright crack and a burst in the spell's color.
            int color = spell.color();
            DustParticleOptions dust = new DustParticleOptions(
                    new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 1.2f);
            level.sendParticles(dust, x, y, z, 14, 0.35, 0.4, 0.35, 0.1);
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 10, 0.3, 0.4, 0.3, 0.3);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.5f);
        } else {
            // A dull thud and a grey puff.
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 8, 0.25, 0.3, 0.25, 0.02);
            level.playSound(null, x, y, z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.6f);
        }
    }
}
```

- [ ] **Step 4: Stop Hydro Jet double-counting blazes**

Hydro Jet doubles damage against water-sensitive mobs. A blaze is also innate Fire, so the chart would make it ×3. Keep the doubling only for creatures without an element (endermen).

In `content/spell/HydroJetSpell.java`, `hitCreature`:
```java
            if (target.isSensitiveToWater()) {
                damage *= 2f;
            }
```
becomes
```java
            // Water-sensitive creatures without an element (endermen) take double. Elemental ones
            // (blazes) are handled by the matchup chart instead.
            if (target.isSensitiveToWater() && CreatureElements.elementOf(target) == null) {
                damage *= 2f;
            }
```
and add `import com.chappadodle.elementalarcana.api.CreatureElements;`.

- [ ] **Step 5: Verify the build**

Run: `./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL` (unit tests included).

---

### Task 5: README, full verification, play-test, commit

**Files:**
- Modify: `README.md` (the mechanics paragraphs under **Spells**)

- [ ] **Step 1: Explain it to players (without naming creature elements)**

In `README.md`, right after the paragraph that starts with `**Water** makes enemies **Wet**`, add:

```markdown
**Elements matter.** Every spell deals damage of its element, and some creatures are born with an element of their own. Water is strong against Fire, Fire against Ice and Ice against Water (×1.5), and every element resists itself (×0.5). Wind is neutral. The game never tells you a creature's element: watch and listen to how your hits land.
```

- [ ] **Step 2: Build, test, and check the server**

Run: `./gradlew build --console=plain`, then run the dedicated server check command from Task 2 Step 5.
Expected: `BUILD SUCCESSFUL`, and `Done (…)` with no `ERROR`/`Exception` lines from `elementalarcana`.

- [ ] **Step 3: Launch the client for the user**

Run `./gradlew runClient --console=plain` in the background, writing to a log. Wait for `Sound engine started`, then grep the log for `Exception|ERROR` (ignore the vanilla goat-horn "Missing sound" warnings).

- [ ] **Step 4: Play-test checklist (hand this to the user)**

In a creative world with cheats, on any difficulty except Peaceful (Peaceful removes the blaze):

```
/summon blaze ~ ~ ~4 {NoAI:1b}
/damage @e[type=blaze,limit=1,sort=nearest] 4 elementalarcana:water_spell
/data get entity @e[type=blaze,limit=1,sort=nearest] Health
```
Expected: `14.0f` (20 − 4×1.5), with a bright crack and a colored burst.

```
/damage @e[type=blaze,limit=1,sort=nearest] 4 elementalarcana:fire_spell
/data get entity @e[type=blaze,limit=1,sort=nearest] Health
```
Expected: `12.0f` (−2, resisted), with a dull thud and a smoke puff.

```
/damage @e[type=blaze,limit=1,sort=nearest] 4 elementalarcana:wind_spell
/data get entity @e[type=blaze,limit=1,sort=nearest] Health
```
Expected: `8.0f` (−4, neutral), with no feedback.

Then with real spells:
- Hydro Jet on a blaze: rapid hits, and the crack plays at most twice a second.
- Icicle on a drowned: strong.
- Fireball on a stray: strong.
- Fireball on a magma cube: resisted.
- Wind Blade on a breeze: resisted.
- Any spell on a zombie, or on another player: no feedback, normal damage.
- Kill a zombie with each element and read the death messages.

- [ ] **Step 5: Commit and push, only after the user confirms it works**

```bash
git add build.gradle README.md src docs/superpowers/plans/2026-09-28-elements-and-matchups.md
GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com git commit -F - <<'EOF'
Add elemental damage types and the element matchup chart

Every spell now deals fire/water/ice/wind spell damage. Innate creatures
(blaze, drowned, stray, breeze...) take x1.5 or x0.5 from the chart,
with strong/resisted hit feedback instead of any label. Adds the Element
enum with JUnit tests.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git push
```
