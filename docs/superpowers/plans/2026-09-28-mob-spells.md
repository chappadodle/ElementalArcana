# Mob Spells: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Attuned creatures cast spells of their element: an Adept knows 1, a Magus 2 and an Archmage 3. Every cast has a visible, audible half-second wind-up so players can dodge.

**Architecture:**
- **Spells:** a small `MobSpell` interface, with one implementation per spell. They live in one file per element (`FireMobSpells`, `WaterMobSpells`, `IceMobSpells`, `WindMobSpells`) and are listed per element in `MobSpells`.
- **Casting:** a `CastMobSpellGoal`, added to every Attuned mob, picks the best ready spell, winds up for 10 ticks, and casts.
- **Reuse:** the spells reuse the player spells' pieces: the projectiles through `SpellProjectile.shootFrom`, plus `FireField`, `Whirlpool`, the reactions, and the Frost Nova burst.
- **Who gets hurt:** a new `SpellTargets.canAffect(owner, target)` rule decides who an area effect may hurt. A player's magic spares players, and a monster's magic spares monsters. It replaces the hard-coded "never players" checks, so player spells behave exactly as before.

**Tech Stack:** NeoForge 21.1.252 for Minecraft 1.21.1, Java 21, Parchment 2024.11.17, JUnit 5, and `tools/server_console.sh` for scripted server tests.

**Spec:** `docs/superpowers/specs/2026-09-28-elements-and-creature-magic-design.md`, build step 3. Rewards (step 4) get their own plan.

## Global Constraints

- The spell table from the spec:

  | Element | Adept | Magus adds | Archmage adds |
  |---|---|---|---|
  | Fire | Fireball | Burning ground under the target | Meteor |
  | Water | 1 s Hydro Jet burst (pushes, Wet) | Heals nearby monsters | Whirlpool at the target's feet |
  | Ice | Icicle | Frost Nova when the target is close | Frost Shield on itself |
  | Wind | Wind Blade | Gale Dash (dodge away or close in) | Updraft (launches the target: Airborne) |

- **Wind-up:** every cast has a **10-tick wind-up**: the mob stops, faces the target, element particles gather at its hands, and the evoker "prepare" sound plays.
- **Casting rules:**
  - The mob needs a target it can see.
  - At least **40 ticks** must pass between any two casts.
  - Each spell has its own cooldown.
  - The mob uses the highest-rank spell that is ready.
- **Damage:** mob damage is **60%** of the player Lv 1 equivalent (`MobSpells.POWER = 0.6f`).
- **Reactions work on players.** A Freeze on a player lasts **20 ticks**; on creatures it stays 50.
- **Mobs keep their normal attacks.** The goal only takes over movement and looking during a wind-up.
- **Sounds:** vanilla only.
- **No labels:** nothing names an element. Attuned mobs are recognized by their hints and their spells.
- **Git:**
  - Never modify git config.
  - After each element task, the user play-tests. Commit and push only after they confirm.
  - Commit with the env vars `GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com`.
  - Pass the message via a heredoc ending in `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Paths:** relative to the repo root `elemental-arcana/`, with `.../` = `src/main/java/com/chappadodle/elementalarcana/`.

## Deviations from the spec (recorded in the spec in Task 5)

1. **Archmage "Frost Shield" is a mob-only ice ward.** The player shield system is built around `ServerPlayer` throughout: the `ShieldSpell` API, the ice hearts HUD and the shard renderer. Generalizing it is a large refactor of tested code for one mob spell. `IceWards` absorbs 10 damage for 10 s (Frost Shield Lv 2 values), shows orbiting frost particles, and shatters, slowing nearby enemies.
2. **Who area magic hurts is now defined.** Players' area magic spares players (unchanged), and monsters' area magic spares monsters. The spec didn't say; without this rule, a monster's Burning Ground would never burn players.

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `.../api/SpellTargets.java` | create | Who an area effect may hurt |
| `.../api/ElementalReactions.java` | modify | Freeze on players lasts 20 ticks |
| `.../content/spell/FireballSpell.java` | modify | `SpellTargets` in the blast; `shootForMob` (fireball and Meteor) |
| `.../content/FireField.java`, `.../content/Whirlpool.java` | modify | `SpellTargets` (keep the owner entity) |
| `.../content/mob/MobSpell.java` | create | The mob spell interface |
| `.../content/mob/MobSpells.java` | create | Spells per element; `POWER` |
| `.../content/mob/MobCasting.java` | create | Shared helpers: aim, shoot, wind-up visuals, sounds |
| `.../content/mob/CastMobSpellGoal.java` | create | The AI goal: pick, wind up, cast |
| `.../content/Attunement.java` | modify | Give Attuned mobs the goal |
| `.../content/mob/FireMobSpells.java` | create | Fireball, Burning Ground, Meteor |
| `.../content/mob/WaterMobSpells.java`, `.../content/mob/MobJet.java` | create | Jet burst, Heal, Whirlpool |
| `.../client/particle/HydroStreamEmitter.java` | modify | Don't re-aim a mob's stream to your own hand |
| `.../content/spell/FrostNovaSpell.java` | modify | Extract `burst(...)` so mobs can use it |
| `.../content/mob/IceMobSpells.java`, `.../content/mob/IceWards.java` | create | Icicle, Frost Nova, Ice Ward |
| `.../content/mob/WindMobSpells.java` | create | Wind Blade, Gale Dash, Updraft |
| `tools/server_tests/mobspells_{fire,water,ice,wind}.txt` | create | Scripted arena tests |
| `README.md`, the spec | modify | Docs |

### The arena test (used by Tasks 2 to 5)

Each element gets a command file that builds a flat stone arena at world spawn. It places a **villager** (`NoAI`, a valid zombie target) 8 blocks from an Attuned **zombie** whose movement speed is 0: the zombie can't walk, but it still targets, looks and casts. After a few seconds the file prints the villager's health. A drop below `20.0f` (or `No entity was found`, if the villager died) proves the goal found a target, wound up and cast a spell that hit. It runs at night, with mob spawning off, and puts spawning back at the end.

---

### Task 1: Casting framework (no spells yet)

**Files:**
- Create: `.../api/SpellTargets.java`
- Modify: `.../api/ElementalReactions.java` (`freeze`)
- Modify: `.../content/spell/FireballSpell.java` (the `explode` target filter)
- Modify: `.../content/FireField.java`
- Modify: `.../content/Whirlpool.java`
- Create: `.../content/mob/MobSpell.java`
- Create: `.../content/mob/MobSpells.java`
- Create: `.../content/mob/MobCasting.java`
- Create: `.../content/mob/CastMobSpellGoal.java`
- Modify: `.../content/Attunement.java`

**Interfaces:**
- Consumes: `Attunement.get(Entity)`, `CreatureMagic`, `AttunementRank`, `Element` (step 2).
- Produces:
  - `SpellTargets.canAffect(@Nullable Entity owner, LivingEntity target) : boolean`
  - `interface MobSpell { AttunementRank rank(); int cooldownTicks(); boolean canCast(Mob, LivingEntity); void cast(Mob, LivingEntity); }`
  - `MobSpells.POWER : float` (0.6), and `MobSpells.of(Element) : List<MobSpell>` (lowest rank first)
  - `MobCasting.distance(Mob, LivingEntity) : double`
  - `MobCasting.aimPoint(LivingEntity) : Vec3`
  - `MobCasting.castOrigin(Mob, Vec3 aim) : Vec3`
  - `MobCasting.shoot(Mob, S spell, LivingEntity target) : SpellProjectile`, where `S extends Spell & ProjectileSpell`
  - `MobCasting.play(Entity, SoundEvent, float volume, float pitch)`
  - `MobCasting.windup(Mob, Element)`
  - `MobCasting.handsParticle(Element) : ParticleOptions`
  - `CastMobSpellGoal(Mob)`
  - `Attunement.ensureCastGoal(Mob)`

- [ ] **Step 1: `SpellTargets`**

Create `src/main/java/com/chappadodle/elementalarcana/api/SpellTargets.java`:

```java
package com.chappadodle.elementalarcana.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Who a spell's area effects (splash, burning ground, whirlpools, bursts) may hurt, given who cast
 * it. A player's magic spares other players (they can still be hit directly by a projectile), and
 * a monster's magic spares other monsters. The caster is never affected.
 */
public final class SpellTargets {

    private SpellTargets() {
    }

    public static boolean canAffect(@Nullable Entity owner, LivingEntity target) {
        if (target == owner || !target.isAlive()) {
            return false;
        }
        if (owner instanceof Enemy) {
            return !(target instanceof Enemy);
        }
        return !(target instanceof Player);
    }
}
```

- [ ] **Step 2: Use it in the Fireball blast, FireField and Whirlpool**

`.../content/spell/FireballSpell.java`, in `explode`, replace:

```java
                e -> e != owner && e.isAlive() && (e == directHit || !(e instanceof Player)
```

with:

```java
                e -> e != owner && e.isAlive() && (e == directHit || SpellTargets.canAffect(owner, e)
```

and add `import com.chappadodle.elementalarcana.api.SpellTargets;`.

`.../content/FireField.java`:
- change the field `@Nullable private final UUID owner;` to `@Nullable private final Entity owner;`
- change the constructor parameter `@Nullable UUID owner` to `@Nullable Entity owner`
- in `spawn`, change `owner == null ? null : owner.getUUID()` to `owner`
- in `tick`, replace the predicate `e -> !(e instanceof Player) && !e.getUUID().equals(owner) && e.isAlive()` with `e -> SpellTargets.canAffect(owner, e)` (keep the `&& Math.hypot(...) <= radius` part)
- remove the now-unused imports `java.util.UUID` and `net.minecraft.world.entity.player.Player`
- add `import com.chappadodle.elementalarcana.api.SpellTargets;`
- change the class Javadoc "sets creatures that stand in it alight (never players, never its caster)" to "sets creatures that stand in it alight (whoever its caster's magic may hurt, see SpellTargets)"

`.../content/Whirlpool.java`, in `tick`:
- delete the line `UUID ownerId = owner == null ? null : owner.getUUID();`
- replace the predicate `e -> !(e instanceof Player) && !e.getUUID().equals(ownerId) && e.isAlive()` with `e -> SpellTargets.canAffect(owner, e)` (keep the `&& Math.hypot(...) <= RADIUS` part)
- remove the now-unused imports `java.util.UUID` and `net.minecraft.world.entity.player.Player`
- add `import com.chappadodle.elementalarcana.api.SpellTargets;`
- change the class Javadoc "drags creatures (never players, never its caster)" to "drags creatures (whoever its caster's magic may hurt, see SpellTargets)"

- [ ] **Step 3: A Freeze on a player lasts 1 second**

In `.../api/ElementalReactions.java`, add this constant next to `FREEZE_TICKS`:

```java
    private static final int PLAYER_FREEZE_TICKS = 20;
```

In `freeze`, replace:

```java
        target.addEffect(new MobEffectInstance(ModContent.FROZEN, FREEZE_TICKS));
        target.getPersistentData().putLong(TAG_FREEZE_IMMUNE_UNTIL, level.getGameTime() + FREEZE_TICKS + FREEZE_IMMUNITY_TICKS);
```

with:

```java
        // Frozen solid is harsh on a player, so it's short for them.
        int ticks = target instanceof Player ? PLAYER_FREEZE_TICKS : FREEZE_TICKS;
        target.addEffect(new MobEffectInstance(ModContent.FROZEN, ticks));
        target.getPersistentData().putLong(TAG_FREEZE_IMMUNE_UNTIL, level.getGameTime() + ticks + FREEZE_IMMUNITY_TICKS);
```

Change that method's Javadoc "Freeze: the target is frozen solid for 2.5s" to "Freeze: the target is frozen solid for 2.5s (1s for players)".

- [ ] **Step 4: `MobSpell` and `MobSpells`**

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/MobSpell.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * A spell an Attuned creature can cast. Mobs don't use the player spell classes; these reuse their
 * projectiles and effects instead. Cast by CastMobSpellGoal after a short wind-up.
 */
public interface MobSpell {

    /** The lowest rank that knows this spell. */
    AttunementRank rank();

    int cooldownTicks();

    /** Whether casting now makes sense (range, situation). The target is alive and in sight. */
    boolean canCast(Mob caster, LivingEntity target);

    /** Runs when the wind-up ends. */
    void cast(Mob caster, LivingEntity target);
}
```

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/MobSpells.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.Element;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The spells each element's Attuned creatures know, lowest rank first. */
public final class MobSpells {
    /** Mob spell power: 60% of what a player's Lv 1 spell does. */
    public static final float POWER = 0.6f;

    private static final Map<Element, List<MobSpell>> BY_ELEMENT = new EnumMap<>(Element.class);

    static {
        BY_ELEMENT.put(Element.FIRE, List.of());
        BY_ELEMENT.put(Element.WATER, List.of());
        BY_ELEMENT.put(Element.ICE, List.of());
        BY_ELEMENT.put(Element.WIND, List.of());
    }

    private MobSpells() {
    }

    public static List<MobSpell> of(Element element) {
        return BY_ELEMENT.get(element);
    }
}
```

- [ ] **Step 5: `MobCasting` helpers**

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/MobCasting.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Small helpers shared by the mob spells. */
public final class MobCasting {

    private MobCasting() {
    }

    public static double distance(Mob caster, LivingEntity target) {
        return caster.distanceTo(target);
    }

    /** Where to aim at a creature: the middle of its body. */
    public static Vec3 aimPoint(LivingEntity target) {
        return target.getBoundingBox().getCenter();
    }

    /** Where a spell leaves the caster: just in front of its face, toward {@code aim}. */
    public static Vec3 castOrigin(Mob caster, Vec3 aim) {
        Vec3 eye = caster.getEyePosition();
        return eye.add(aim.subtract(eye).normalize().scale(0.6));
    }

    /** Fires a player spell's projectile (its plain Lv 1 form) straight at {@code target}, at mob power. */
    public static <S extends Spell & ProjectileSpell> SpellProjectile shoot(Mob caster, S spell, LivingEntity target) {
        Vec3 aim = aimPoint(target);
        Vec3 from = castOrigin(caster, aim);
        return SpellProjectile.shootFrom(caster, spell, from, aim.subtract(from).normalize().scale(spell.releaseSpeed(1f)), MobSpells.POWER);
    }

    public static void play(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    /** One tick of the wind-up: the element gathers at the caster's hands. */
    public static void windup(Mob caster, Element element) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        float yaw = caster.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 chest = caster.getEyePosition().add(caster.getLookAngle().scale(0.4)).add(0, -0.5, 0);
        for (int side = -1; side <= 1; side += 2) {
            Vec3 hand = chest.add(right.scale(0.35 * side));
            level.sendParticles(handsParticle(element), hand.x, hand.y, hand.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    public static ParticleOptions handsParticle(Element element) {
        return switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ModContent.HYDRO_DROP.get();
            case ICE -> ModContent.FROST_SPARKLE.get();
            case WIND -> ModContent.WIND_STREAK.get();
        };
    }
}
```

- [ ] **Step 6: `CastMobSpellGoal`**

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/CastMobSpellGoal.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.content.Attunement;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An Attuned creature's spellcasting. When it has a target in sight, it picks the highest-rank
 * spell that is ready, stops, faces the target and winds up for half a second (hands glowing
 * with its element, a warning sound), then casts. Between casts it fights normally.
 */
public class CastMobSpellGoal extends Goal {
    private static final int WINDUP_TICKS = 10;
    // At least this long between any two casts.
    private static final int MIN_GAP_TICKS = 40;

    private final Mob mob;
    private final Map<MobSpell, Long> readyAt = new HashMap<>();
    private long nextCastAt;
    @Nullable
    private MobSpell casting;
    @Nullable
    private LivingEntity target;
    private int windup;

    public CastMobSpellGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        CreatureMagic magic = Attunement.get(mob);
        LivingEntity target = mob.getTarget();
        long now = mob.level().getGameTime();
        if (magic == null || target == null || !target.isAlive() || now < nextCastAt || !mob.getSensing().hasLineOfSight(target)) {
            return false;
        }
        casting = pick(magic, target, now);
        this.target = target;
        return casting != null;
    }

    /** The highest-rank spell this creature knows that is off cooldown and makes sense right now. */
    @Nullable
    private MobSpell pick(CreatureMagic magic, LivingEntity target, long now) {
        List<MobSpell> spells = MobSpells.of(magic.element());
        for (int i = spells.size() - 1; i >= 0; i--) {
            MobSpell spell = spells.get(i);
            if (spell.rank().ordinal() <= magic.rank().ordinal() && now >= readyAt.getOrDefault(spell, 0L) && spell.canCast(mob, target)) {
                return spell;
            }
        }
        return null;
    }

    @Override
    public void start() {
        windup = WINDUP_TICKS;
        mob.getNavigation().stop();
        MobCasting.play(mob, SoundEvents.EVOKER_PREPARE_ATTACK, 1f, 1.2f);
    }

    @Override
    public boolean canContinueToUse() {
        return windup > 0 && casting != null && target != null && target.isAlive() && Attunement.get(mob) != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        CreatureMagic magic = Attunement.get(mob);
        if (casting == null || target == null || magic == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30f, 30f);
        MobCasting.windup(mob, magic.element());
        if (--windup == 0) {
            casting.cast(mob, target);
            long now = mob.level().getGameTime();
            readyAt.put(casting, now + casting.cooldownTicks());
            nextCastAt = now + MIN_GAP_TICKS;
        }
    }

    @Override
    public void stop() {
        casting = null;
        target = null;
        windup = 0;
    }
}
```

- [ ] **Step 7: Give Attuned mobs the goal**

In `.../content/Attunement.java`:

1. Add this public method after `clear`:

```java
    /** Gives an Attuned mob its spellcasting goal, once. The goal does nothing if it isn't Attuned. */
    public static void ensureCastGoal(Mob mob) {
        boolean hasGoal = mob.goalSelector.getAvailableGoals().stream().anyMatch(wrapped -> wrapped.getGoal() instanceof CastMobSpellGoal);
        if (!hasGoal) {
            mob.goalSelector.addGoal(1, new CastMobSpellGoal(mob));
        }
    }
```

2. At the end of `attune`, right before `return true;`, add:

```java
        if (!mob.level().isClientSide()) {
            ensureCastGoal(mob);
        }
```

3. Add this event handler after `onFinalizeSpawn`. It covers Attuned mobs loaded from disk; a freshly spawned one gets the goal from `attune`, and this call is harmless for it:

```java
    /** Attuned mobs loaded from disk get their spellcasting goal back. */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && get(mob) != null) {
            ensureCastGoal(mob);
        }
    }
```

4. Add these imports: `com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal` and `net.neoforged.neoforge.event.entity.EntityJoinLevelEvent`.
5. In the class Javadoc, change "spells come in a later step." to "their spells are cast by CastMobSpellGoal."

- [ ] **Step 8: Build and check nothing regressed**

Run: `./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL` (28 unit tests).

Run: `tools/server_console.sh tools/server_tests/attunement.txt`
Expected: exactly the step 2 output (30.0f, 60.0f, 1b, the ice/archmage attachment, the blaze refusal, cleared, 20.0f). Then check the log:

```bash
grep -cE "Exception|/ERROR\]" build/server-console.log
```

This should print `0`.

---

### Task 2: Fire (Fireball, Burning Ground, Meteor), then play-test and commit

**Files:**
- Modify: `.../content/spell/FireballSpell.java` (`shootForMob`; move `launchInArc` out of `Hold`)
- Create: `.../content/mob/FireMobSpells.java`
- Modify: `.../content/mob/MobSpells.java`
- Create: `tools/server_tests/mobspells_fire.txt`

**Interfaces:**
- Consumes: `MobSpell`, `MobSpells.POWER`, `MobCasting.*` (Task 1); `FireField.spawn(ServerLevel, Vec3, double, int, Entity)` (existing).
- Produces: `FireballSpell#shootForMob(Mob caster, Vec3 target, float power, boolean meteor) : SpellProjectile`, and `FireMobSpells.ALL : List<MobSpell>`.

- [ ] **Step 1: Mobs can throw fireballs and Meteors**

In `.../content/spell/FireballSpell.java`:

1. Move `launchInArc` out of the `Hold` class. Cut the whole method, including its Javadoc (`/** Meteor: lob it in a high arc that comes down on the target. */ private static void launchInArc(...) {...}`), and paste it into `FireballSpell` itself, right before the `// ---- the hold ----` comment. It stays `private static`, so `Hold.release` still calls it unchanged.
2. Add this public method right before `// ---- the hold ----` as well:

```java
    /**
     * Mob magic (Attuned creatures): throws a plain Lv 1 fireball at {@code target}, or, with
     * {@code meteor}, lobs a Meteor that comes down on it. {@code power} scales it like a player's
     * spell power.
     */
    public SpellProjectile shootForMob(Mob caster, Vec3 target, float power, boolean meteor) {
        Vec3 eye = caster.getEyePosition();
        Vec3 from = eye.add(target.subtract(eye).normalize().scale(0.6));
        SpellProjectile fireball = SpellProjectile.shootFrom(caster, this, from, target.subtract(from).normalize().scale(releaseSpeed(1f)), power);
        if (meteor) {
            fireball.getPersistentData().putString(TAG_BRANCH_5, METEOR);
            launchInArc(fireball, target);
        }
        return fireball;
    }
```

3. Add `import net.minecraft.world.entity.Mob;`.

- [ ] **Step 2: The Fire spells**

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/FireMobSpells.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.content.FireField;
import com.chappadodle.elementalarcana.content.ModSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/** Fire: Adepts throw fireballs, Magi set the ground under you alight, Archmages call down Meteors. */
public final class FireMobSpells {
    public static final List<MobSpell> ALL = List.of(new Fireball(), new BurningGround(), new Meteor());

    private FireMobSpells() {
    }

    private static final class Fireball implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ADEPT;
        }

        @Override
        public int cooldownTicks() {
            return 60;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 2.5 && distance <= 24;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ModSpells.FIREBALL.get().shootForMob(caster, MobCasting.aimPoint(target), MobSpells.POWER, false);
            MobCasting.play(caster, SoundEvents.BLAZE_SHOOT, 1f, 1.1f);
        }
    }

    /** The ground under the target bursts into flames for 5 seconds. */
    private static final class BurningGround implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }

        @Override
        public int cooldownTicks() {
            return 160;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 3 && distance <= 16;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            FireField.spawn(level, target.position(), 2.5, 100, caster);
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.1, target.getZ(), 30, 1.2, 0.05, 1.2, 0.03);
            MobCasting.play(target, SoundEvents.FIRECHARGE_USE, 1f, 0.8f);
        }
    }

    private static final class Meteor implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ARCHMAGE;
        }

        @Override
        public int cooldownTicks() {
            return 240;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 6 && distance <= 24;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ModSpells.FIREBALL.get().shootForMob(caster, target.position(), MobSpells.POWER, true);
            MobCasting.play(caster, SoundEvents.BLAZE_SHOOT, 1.5f, 0.6f);
        }
    }
}
```

- [ ] **Step 3: Register them**

In `.../content/mob/MobSpells.java`, change `BY_ELEMENT.put(Element.FIRE, List.of());` to `BY_ELEMENT.put(Element.FIRE, FireMobSpells.ALL);`.

- [ ] **Step 4: Arena test**

Create `tools/server_tests/mobspells_fire.txt`:

```
# An Attuned Fire zombie that can't walk casts at a villager 8 blocks away.
time set midnight
gamerule doMobSpawning false
forceload add -16 -16 16 16
fill -12 99 -12 12 99 12 minecraft:stone
fill -12 100 -12 12 106 12 minecraft:air
summon villager 8.5 100 0.5 {NoAI:1b,Tags:["eatest"]}
summon zombie 0.5 100 0.5 {Tags:["eatest","eacaster"],attributes:[{id:"minecraft:generic.movement_speed",base:0.0}]}
arcana attune @e[tag=eacaster] fire adept
sleep 8
data get entity @e[type=villager,tag=eatest,limit=1] Health
kill @e[tag=eatest]
kill @e[type=elementalarcana:spell_projectile]
gamerule doMobSpawning true
forceload remove all
```

- [ ] **Step 5: Build and run it**

Run: `./gradlew build --console=plain`, then `tools/server_console.sh tools/server_tests/mobspells_fire.txt`, then `grep -cE "Exception|/ERROR\]" build/server-console.log`.
Expected:
- `BUILD SUCCESSFUL`.
- The villager's health is below `20.0f` (or `No entity was found`, if two fireballs and the burning killed it).
- `0` errors.

- [ ] **Step 6: Launch the game and hand over the play-test**

Run `./gradlew runClient` in the background, wait for `Sound engine started`, and grep the log for `Exception|ERROR` (ignore the goat-horn warnings). Then give the user this checklist. It uses the dev menu Creature row (Spawn zombie), at night, in survival (mobs don't target creative players), after setting the time with `/time set midnight`:
- **Fire Adept:** after the evoker sound and flames at its hands, it throws fireballs. Step aside during the wind-up to dodge.
- **Fire Magus:** also sets the ground under you on fire. It burns you, but not other zombies standing in it.
- **Fire Archmage:** also lobs Meteors that arc down on you, with a big blast and burning ground.
- **Between casts** it still walks up and hits you normally.

- [ ] **Step 7: Commit and push (only after the user confirms)**

```bash
git add docs src tools
GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com git commit -F - <<'EOF'
Attuned mobs cast spells: casting framework and Fire

Attuned creatures now wind up (half a second, hands glowing, evoker
sound) and cast their element's spells: Fire Adepts throw fireballs,
Magi burn the ground under you, Archmages call down Meteors. Area magic
now follows one rule (SpellTargets): players' magic spares players,
monsters' magic spares monsters. A Freeze on a player lasts 1 second.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git push
```

---

### Task 3: Water (Jet burst, Heal, Whirlpool), then play-test and commit

**Files:**
- Create: `.../content/mob/MobJet.java`
- Create: `.../content/mob/WaterMobSpells.java`
- Modify: `.../content/mob/MobSpells.java`
- Modify: `.../client/particle/HydroStreamEmitter.java`
- Create: `tools/server_tests/mobspells_water.txt`

**Interfaces:**
- Consumes: `MobSpell`, `MobSpells.POWER`, `MobCasting.*` (Task 1); `Whirlpool.spawn(ServerLevel, Vec3, float, int, Entity)`, `ElementalReactions.waterHit(LivingEntity, int)`, `SpellDamage.source(Level, Element, Entity, Entity)`, `ModContent.HYDRO_STREAM` (existing).
- Produces: `MobJet.start(Mob caster, LivingEntity target, float power)`, and `WaterMobSpells.ALL : List<MobSpell>`.

- [ ] **Step 1: The mob's stream**

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/MobJet.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A Water Adept's Hydro Jet: a one-second stream from the mob at its target, hitting the first
 * creature on the line five times (pushing it back and soaking it). Server-side, never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class MobJet {
    private static final List<MobJet> ACTIVE = new ArrayList<>();
    private static final int DURATION_TICKS = 20;
    private static final int HIT_INTERVAL = 4;
    private static final double RANGE = 12;
    private static final float HIT_DAMAGE = 1f;

    private final Mob caster;
    private final LivingEntity target;
    private final float power;
    private int age;

    private MobJet(Mob caster, LivingEntity target, float power) {
        this.caster = caster;
        this.target = target;
        this.power = power;
    }

    public static void start(Mob caster, LivingEntity target, float power) {
        ACTIVE.add(new MobJet(caster, target, power));
        MobCasting.play(caster, SoundEvents.BUCKET_EMPTY, 1f, 1.3f);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ACTIVE.removeIf(jet -> !jet.tick());
    }

    /** One tick of spraying; false when it's over. */
    private boolean tick() {
        if (!caster.isAlive() || age++ >= DURATION_TICKS) {
            return false;
        }
        ServerLevel level = (ServerLevel) caster.level();
        caster.getLookControl().setLookAt(target, 30f, 30f);
        Vec3 origin = caster.getEyePosition().add(0, -0.3, 0);
        Vec3 aim = target.isAlive() ? MobCasting.aimPoint(target) : origin.add(caster.getLookAngle().scale(RANGE));
        Vec3 direction = aim.subtract(origin).normalize();
        Vec3 end = origin.add(direction.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, caster, origin, stop, new AABB(origin, stop).inflate(1),
                entity -> entity instanceof LivingEntity && entity != caster && entity.isAlive(), 0.3f);
        if (hit != null) {
            stop = hit.getEntity().getBoundingBox().inflate(0.3).clip(origin, stop).orElse(hit.getLocation());
        }
        Vec3 line = stop.subtract(origin);
        level.sendParticles(ModContent.HYDRO_STREAM.get(), origin.x, origin.y, origin.z, 0, line.x, line.y, line.z, 1.0);

        if (hit != null && hit.getEntity() instanceof LivingEntity victim && age % HIT_INTERVAL == 0) {
            float damage = HIT_DAMAGE * power * ElementalReactions.waterHit(victim, 100);
            Vec3 motion = victim.getDeltaMovement();
            SpellDamage.hurtMultiHit(victim, SpellDamage.source(level, Element.WATER, caster, caster), damage);
            victim.setDeltaMovement(motion.add(direction.x * 0.12, 0.02, direction.z * 0.12));
            victim.hurtMarked = true;
        }
        if (age % 6 == 0) {
            level.playSound(null, stop.x, stop.y, stop.z, SoundEvents.GENERIC_SPLASH, caster.getSoundSource(), 0.3f, 1.5f);
        }
        return true;
    }
}
```

- [ ] **Step 2: Keep a mob's stream from re-aiming to your hand**

`HydroStreamEmitter` redraws a stream from the local player's current hand whenever the stream starts within 1.2 blocks of it. A zombie right next to you, spraying *at* you, must not trigger that. Only re-aim when the stream also points the way you're looking. In `.../client/particle/HydroStreamEmitter.java`, change:

```java
            if (hand.distanceTo(start) < 1.2) {
```

to:

```java
            // Only your own jet: it starts at your hand and points where you look (a mob next to
            // you, spraying at you, points the other way).
            if (hand.distanceTo(start) < 1.2 && line.lengthSqr() > 0 && self.getLookAngle().dot(line.normalize()) > 0.8) {
```

- [ ] **Step 3: The Water spells**

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/WaterMobSpells.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.content.Whirlpool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.List;

/** Water: Adepts spray a jet, Magi heal the monsters around them, Archmages open a whirlpool under you. */
public final class WaterMobSpells {
    public static final List<MobSpell> ALL = List.of(new JetBurst(), new HealAllies(), new Maelstrom());

    private static final double HEAL_RADIUS = 8;
    private static final float HEAL_AMOUNT = 4f;
    // Heal only when some monster nearby (or the caster) is below this share of its health.
    private static final float HEAL_BELOW = 0.7f;

    private WaterMobSpells() {
    }

    private static List<LivingEntity> alliesAround(Mob caster) {
        return caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(HEAL_RADIUS),
                entity -> entity instanceof Enemy && entity.isAlive() && entity.distanceTo(caster) <= HEAL_RADIUS);
    }

    private static final class JetBurst implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ADEPT;
        }

        @Override
        public int cooldownTicks() {
            return 80;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 10;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            MobJet.start(caster, target, MobSpells.POWER);
        }
    }

    private static final class HealAllies implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }

        @Override
        public int cooldownTicks() {
            return 200;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return alliesAround(caster).stream().anyMatch(ally -> ally.getHealth() < ally.getMaxHealth() * HEAL_BELOW);
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            for (LivingEntity ally : alliesAround(caster)) {
                ally.heal(HEAL_AMOUNT);
                level.sendParticles(ParticleTypes.SPLASH, ally.getX(), ally.getY(0.8), ally.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
                level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(1.0) + 0.3, ally.getZ(), 2, 0.3, 0.1, 0.3, 0);
            }
            MobCasting.play(caster, SoundEvents.GENERIC_SPLASH, 1f, 1.4f);
        }
    }

    private static final class Maelstrom implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ARCHMAGE;
        }

        @Override
        public int cooldownTicks() {
            return 300;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 4 && distance <= 16;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            Whirlpool.spawn((ServerLevel) caster.level(), target.position(), 1.5f * MobSpells.POWER, 80, caster);
        }
    }
}
```

- [ ] **Step 4: Register them**

In `.../content/mob/MobSpells.java`, change `BY_ELEMENT.put(Element.WATER, List.of());` to `BY_ELEMENT.put(Element.WATER, WaterMobSpells.ALL);`.

- [ ] **Step 5: Arena test**

Create `tools/server_tests/mobspells_water.txt`. It is the same as `mobspells_fire.txt`, with these two differences:
- the attune line is `arcana attune @e[tag=eacaster] water adept`
- the first comment reads `# An Attuned Water zombie that can't walk sprays a villager 8 blocks away.`

The full file:

```
# An Attuned Water zombie that can't walk sprays a villager 8 blocks away.
time set midnight
gamerule doMobSpawning false
forceload add -16 -16 16 16
fill -12 99 -12 12 99 12 minecraft:stone
fill -12 100 -12 12 106 12 minecraft:air
summon villager 8.5 100 0.5 {NoAI:1b,Tags:["eatest"]}
summon zombie 0.5 100 0.5 {Tags:["eatest","eacaster"],attributes:[{id:"minecraft:generic.movement_speed",base:0.0}]}
arcana attune @e[tag=eacaster] water adept
sleep 8
data get entity @e[type=villager,tag=eatest,limit=1] Health
kill @e[tag=eatest]
gamerule doMobSpawning true
forceload remove all
```

- [ ] **Step 6: Build and run it**

Run: `./gradlew build --console=plain`, then `tools/server_console.sh tools/server_tests/mobspells_water.txt`, then `grep -cE "Exception|/ERROR\]" build/server-console.log`.
Expected:
- `BUILD SUCCESSFUL`.
- The villager's health is below `20.0f`. Each 1 s burst deals 5 × 0.6 = 3, so one or two bursts leave 17.0f or 14.0f.
- `0` errors.

Also rerun `tools/server_console.sh tools/server_tests/mobspells_fire.txt` (still below 20.0f).

- [ ] **Step 7: Launch the game and hand over the play-test**

Launch as in Task 2 Step 6. Checklist:
- **Water Adept:** a one-second stream that pushes you back and makes you Wet. Your own Hydro Jet still comes from your own hand, even when a zombie is spraying right next to you.
- **Water Magus:** when a zombie nearby is hurt, it heals all monsters around it: splashes and hearts.
- **Water Archmage:** a whirlpool opens under you and drags you around. It doesn't drag other zombies.
- **Combo:** get soaked by a Water zombie, then hit by an Ice zombie (once Ice exists), and you freeze for 1 second.

- [ ] **Step 8: Commit and push (only after the user confirms)**

```bash
git add src tools
GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com git commit -F - <<'EOF'
Attuned mobs cast Water spells

Water Adepts spray a one-second jet that pushes and soaks, Magi heal
the monsters around them, Archmages open a whirlpool under their
target. A mob's stream no longer re-aims to your own hand.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git push
```

---

### Task 4: Ice (Icicle, Frost Nova, Ice Ward), then play-test and commit

**Files:**
- Modify: `.../content/spell/FrostNovaSpell.java` (extract `burst`)
- Create: `.../content/mob/IceWards.java`
- Create: `.../content/mob/IceMobSpells.java`
- Modify: `.../content/mob/MobSpells.java`
- Create: `tools/server_tests/mobspells_ice.txt`

**Interfaces:**
- Consumes: `MobSpell`, `MobSpells.POWER`, `MobCasting.*` (Task 1); `SpellTargets.canAffect` (Task 1); `ModSpells.ICICLE` (existing).
- Produces:
  - `FrostNovaSpell.burst(ServerLevel level, LivingEntity caster, float power, Predicate<LivingEntity> affects)`
  - `IceWards.raise(Mob)`
  - `IceWards.has(LivingEntity) : boolean`
  - `IceMobSpells.ALL : List<MobSpell>`

- [ ] **Step 1: Frost Nova's burst, reusable**

In `.../content/spell/FrostNovaSpell.java`, replace the whole `cast` method with:

```java
    @Override
    public CastResult cast(CastContext context) {
        burst(context.level(), context.caster(), context.power(), target -> true);
        return CastResult.SUCCESS;
    }

    /**
     * The nova itself: damages, slows and frosts everything around {@code caster} that
     * {@code affects} allows (never the caster), freezes nearby water, and plays the burst. Attuned
     * creatures cast it too.
     */
    public static void burst(ServerLevel level, LivingEntity caster, float power, Predicate<LivingEntity> affects) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(RADIUS),
                entity -> entity != caster && entity.isAlive() && entity.distanceTo(caster) <= RADIUS && affects.test(entity))) {
            ElementalReactions.iceHit(target);
            target.hurt(SpellDamage.source(level, Element.ICE, caster, caster), 3f * power);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(100 * power), 2));
            if (target.canFreeze()) {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 120));
            }
        }
        Freezing.freezeWater(level, caster.blockPosition().below(), (int) RADIUS - 1);

        for (int step = 0; step < 48; step++) {
            float angle = step * Mth.TWO_PI / 48;
            double dx = Mth.cos(angle);
            double dz = Mth.sin(angle);
            level.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX() + dx, caster.getY() + 0.4, caster.getZ() + dz,
                    0, dx, 0.02, dz, 0.35);
        }
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, caster.getX(), caster.getY() + 0.5, caster.getZ(), 30, RADIUS * 0.4, 0.3, RADIUS * 0.4, 0.1);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1f, 0.8f);
    }
```

Add `import java.util.function.Predicate;`, and remove the `import net.minecraft.server.level.ServerPlayer;` line if nothing else uses it. The player cast behaves exactly as before: `target -> true` plus the built-in `entity != caster` check.

- [ ] **Step 2: The Ice Ward**

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/IceWards.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An Ice Archmage's ward: the mob's version of Frost Shield (the player shield system only works
 * on players). It absorbs 10 damage for 10 seconds, shown as frost circling the mob, and
 * shatters when broken, slowing the enemies around it. Server-side, never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class IceWards {
    private static final float STRENGTH = 10f;
    private static final int DURATION_TICKS = 200;
    private static final double SHATTER_RADIUS = 3;
    private static final Map<UUID, Ward> WARDS = new HashMap<>();

    private static final class Ward {
        private float amount;
        private final long endsAt;

        private Ward(float amount, long endsAt) {
            this.amount = amount;
            this.endsAt = endsAt;
        }
    }

    private IceWards() {
    }

    public static boolean has(LivingEntity entity) {
        return WARDS.containsKey(entity.getUUID());
    }

    public static void raise(Mob mob) {
        WARDS.put(mob.getUUID(), new Ward(STRENGTH, mob.level().getGameTime() + DURATION_TICKS));
        ServerLevel level = (ServerLevel) mob.level();
        level.sendParticles(ModContent.FROST_SPARKLE.get(), mob.getX(), mob.getY(0.5), mob.getZ(), 20, 0.5, 0.6, 0.5, 0.02);
        MobCasting.play(mob, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.8f);
        MobCasting.play(mob, SoundEvents.GLASS_PLACE, 1f, 1.2f);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity mob = event.getEntity();
        Ward ward = WARDS.get(mob.getUUID());
        if (ward == null || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        float absorbed = Math.min(ward.amount, event.getAmount());
        ward.amount -= absorbed;
        event.setAmount(event.getAmount() - absorbed);
        level.sendParticles(ModContent.ICE_SHARD.get(), mob.getX(), mob.getY(0.5), mob.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
        MobCasting.play(mob, SoundEvents.GLASS_HIT, 1f, 1.2f);
        if (ward.amount <= 0) {
            WARDS.remove(mob.getUUID());
            shatter(level, mob);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Ward ward = WARDS.get(mob.getUUID());
        if (ward == null) {
            return;
        }
        if (level.getGameTime() >= ward.endsAt) {
            WARDS.remove(mob.getUUID());
            level.sendParticles(ModContent.FROST_MIST.get(), mob.getX(), mob.getY(0.5), mob.getZ(), 6, 0.4, 0.4, 0.4, 0.01);
            return;
        }
        if (mob.tickCount % 2 == 0) {
            // Three glints circling the mob at chest height.
            double radius = mob.getBbWidth() * 0.5 + 0.5;
            for (int i = 0; i < 3; i++) {
                float angle = mob.tickCount * 0.2f + i * Mth.TWO_PI / 3;
                level.sendParticles(ModContent.FROST_SPARKLE.get(), mob.getX() + Mth.cos(angle) * radius, mob.getY(0.6),
                        mob.getZ() + Mth.sin(angle) * radius, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Broken: the ward bursts into shards and chills the enemies around it. */
    private static void shatter(ServerLevel level, LivingEntity mob) {
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(SHATTER_RADIUS),
                entity -> SpellTargets.canAffect(mob, entity) && entity.distanceTo(mob) <= SHATTER_RADIUS)) {
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        level.sendParticles(ModContent.ICE_SHARD.get(), mob.getX(), mob.getY(0.5), mob.getZ(), 30, 0.2, 0.3, 0.2, 0.3);
        MobCasting.play(mob, SoundEvents.GLASS_BREAK, 1f, 0.8f);
    }

    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        WARDS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WARDS.clear();
    }
}
```

- [ ] **Step 3: The Ice spells**

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/IceMobSpells.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.spell.FrostNovaSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/** Ice: Adepts throw icicles, Magi burst a Frost Nova when you're close, Archmages ward themselves in ice. */
public final class IceMobSpells {
    public static final List<MobSpell> ALL = List.of(new Icicle(), new Nova(), new Ward());

    private IceMobSpells() {
    }

    private static final class Icicle implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ADEPT;
        }

        @Override
        public int cooldownTicks() {
            return 60;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 2.5 && distance <= 24;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            MobCasting.shoot(caster, ModSpells.ICICLE.get(), target);
            MobCasting.play(caster, SoundEvents.SNOW_GOLEM_SHOOT, 1f, 0.8f);
        }
    }

    private static final class Nova implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }

        @Override
        public int cooldownTicks() {
            return 120;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 4;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            FrostNovaSpell.burst((ServerLevel) caster.level(), caster, MobSpells.POWER, entity -> SpellTargets.canAffect(caster, entity));
        }
    }

    private static final class Ward implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ARCHMAGE;
        }

        @Override
        public int cooldownTicks() {
            return 400;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return !IceWards.has(caster) && MobCasting.distance(caster, target) <= 16;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            IceWards.raise(caster);
        }
    }
}
```

- [ ] **Step 4: Register them**

In `.../content/mob/MobSpells.java`, change `BY_ELEMENT.put(Element.ICE, List.of());` to `BY_ELEMENT.put(Element.ICE, IceMobSpells.ALL);`.

- [ ] **Step 5: Arena test**

Create `tools/server_tests/mobspells_ice.txt`:

```
# An Attuned Ice zombie that can't walk throws icicles at a villager 8 blocks away.
time set midnight
gamerule doMobSpawning false
forceload add -16 -16 16 16
fill -12 99 -12 12 99 12 minecraft:stone
fill -12 100 -12 12 106 12 minecraft:air
summon villager 8.5 100 0.5 {NoAI:1b,Tags:["eatest"]}
summon zombie 0.5 100 0.5 {Tags:["eatest","eacaster"],attributes:[{id:"minecraft:generic.movement_speed",base:0.0}]}
arcana attune @e[tag=eacaster] ice adept
sleep 8
data get entity @e[type=villager,tag=eatest,limit=1] Health
kill @e[tag=eatest]
kill @e[type=elementalarcana:spell_projectile]
gamerule doMobSpawning true
forceload remove all
```

- [ ] **Step 6: Build and run it**

Run: `./gradlew build --console=plain`, then `tools/server_console.sh tools/server_tests/mobspells_ice.txt`, then `grep -cE "Exception|/ERROR\]" build/server-console.log`.
Expected:
- `BUILD SUCCESSFUL`.
- The villager's health is below `20.0f` (each icicle deals 8 × 0.6 = 4.8).
- `0` errors.

- [ ] **Step 7: Launch the game and hand over the play-test**

Launch as in Task 2 Step 6. Checklist:
- **Ice Adept:** throws icicles. A hit frosts you over and slows you.
- **Ice Magus:** when you get within 4 blocks, it bursts a Frost Nova: you're hurt and slowed, but zombies next to it aren't.
- **Ice Archmage:** wraps itself in circling frost (the ice ward). Your first ~10 damage goes into the ward, which then shatters and slows you.
- **Player cast:** your own Frost Nova still works as before.
- **Freeze combo:** a Water zombie soaks you, then an Ice zombie freezes you, for only 1 second.

- [ ] **Step 8: Commit and push (only after the user confirms)**

```bash
git add src tools
GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com git commit -F - <<'EOF'
Attuned mobs cast Ice spells

Ice Adepts throw icicles, Magi burst a Frost Nova when you're close,
Archmages ward themselves in ice that absorbs damage and shatters.
Frost Nova's burst is now shared by the player spell and the mob one.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git push
```

---

### Task 5: Wind (Wind Blade, Gale Dash, Updraft), docs, play-test, commit

**Files:**
- Create: `.../content/mob/WindMobSpells.java`
- Modify: `.../content/mob/MobSpells.java`
- Create: `tools/server_tests/mobspells_wind.txt`
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-09-28-elements-and-creature-magic-design.md`

**Interfaces:**
- Consumes: `MobSpell`, `MobSpells.POWER`, `MobCasting.*` (Task 1); `ElementalReactions.launchAirborne(LivingEntity, double)`, `SpellDamage.source(...)`, `ModSpells.WIND_BLADE` (existing).
- Produces: `WindMobSpells.ALL : List<MobSpell>`.

- [ ] **Step 1: The Wind spells**

Create `src/main/java/com/chappadodle/elementalarcana/content/mob/WindMobSpells.java`:

```java
package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ModSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Wind: Adepts slash wind blades, Magi dash away or in, Archmages throw you into the air. */
public final class WindMobSpells {
    public static final List<MobSpell> ALL = List.of(new Blade(), new GaleDash(), new Updraft());

    private WindMobSpells() {
    }

    private static final class Blade implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ADEPT;
        }

        @Override
        public int cooldownTicks() {
            return 60;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 2.5 && distance <= 24;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            MobCasting.shoot(caster, ModSpells.WIND_BLADE.get(), target);
            MobCasting.play(caster, SoundEvents.BREEZE_SHOOT, 1f, 1.1f);
        }
    }

    /** Too close: leap back out of reach. Too far: rush in. */
    private static final class GaleDash implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }

        @Override
        public int cooldownTicks() {
            return 100;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return caster.onGround() && (distance < 3 || distance > 8 && distance < 16);
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            Vec3 toward = target.position().subtract(caster.position()).multiply(1, 0, 1).normalize();
            Vec3 direction = MobCasting.distance(caster, target) < 3 ? toward.scale(-1) : toward;
            caster.setDeltaMovement(direction.scale(1.2).add(0, 0.35, 0));
            caster.hurtMarked = true;
            caster.resetFallDistance();
            ServerLevel level = (ServerLevel) caster.level();
            level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, caster.getX(), caster.getY(), caster.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.2, caster.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
            MobCasting.play(caster, SoundEvents.WIND_CHARGE_BURST.value(), 1f, 1f);
        }
    }

    private static final class Updraft implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ARCHMAGE;
        }

        @Override
        public int cooldownTicks() {
            return 200;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 12 && target.onGround();
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.WIND, caster, caster), 2f * MobSpells.POWER);
            ElementalReactions.launchAirborne(target, 1.0);
            level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, target.getX(), target.getY(), target.getZ(), 1, 0, 0, 0, 0);
            for (int height = 0; height < 5; height++) {
                level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + height * 0.7, target.getZ(), 5, 0.3, 0.2, 0.3, 0.02);
            }
            MobCasting.play(target, SoundEvents.WIND_CHARGE_BURST.value(), 1f, 0.7f);
        }
    }
}
```

- [ ] **Step 2: Register them**

In `.../content/mob/MobSpells.java`, change `BY_ELEMENT.put(Element.WIND, List.of());` to `BY_ELEMENT.put(Element.WIND, WindMobSpells.ALL);`.

- [ ] **Step 3: Arena test**

Create `tools/server_tests/mobspells_wind.txt`:

```
# An Attuned Wind zombie that can't walk slashes wind blades at a villager 8 blocks away.
time set midnight
gamerule doMobSpawning false
forceload add -16 -16 16 16
fill -12 99 -12 12 99 12 minecraft:stone
fill -12 100 -12 12 106 12 minecraft:air
summon villager 8.5 100 0.5 {NoAI:1b,Tags:["eatest"]}
summon zombie 0.5 100 0.5 {Tags:["eatest","eacaster"],attributes:[{id:"minecraft:generic.movement_speed",base:0.0}]}
arcana attune @e[tag=eacaster] wind adept
sleep 8
data get entity @e[type=villager,tag=eatest,limit=1] Health
kill @e[tag=eatest]
kill @e[type=elementalarcana:spell_projectile]
gamerule doMobSpawning true
forceload remove all
```

- [ ] **Step 4: README**

In `README.md`, replace the last two sentences of the **Creature magic** paragraph ("Attuned creatures are hit by the element chart like any elemental creature. Watch for a faint hint of their element.") with:

```markdown
Attuned creatures are hit by the element chart like any elemental creature, and they cast their element's spells: Adepts know one, Magi two, Archmages three (Fire: fireballs, burning ground, Meteors; Water: a jet, healing their allies, a whirlpool; Ice: icicles, Frost Nova, an ice ward; Wind: wind blades, Gale Dash, Updraft). Every cast is announced by half a second of glowing hands and a warning sound: move! Watch for a faint hint of their element.
```

- [ ] **Step 5: Record the deviations in the spec**

In `docs/superpowers/specs/2026-09-28-elements-and-creature-magic-design.md`, add at the end of the `### Mob spells` section (right before `### Rewards`):

```markdown
Decided while planning step 3:
- **Who area magic hurts** (`SpellTargets`): a player's area effects spare other players (as
  before), a monster's spare other monsters. Direct projectile hits can still hit anything.
- **The Archmage's Frost Shield is an ice ward** (`IceWards`): the player shield system only
  works on players, so mobs get their own version: it absorbs 10 damage for 10 s, is drawn with
  frost particles instead of the 3D shards, and shatters (slowing nearby enemies) when broken.
```

- [ ] **Step 6: Full verification**

Run:
- `./gradlew build --console=plain` → `BUILD SUCCESSFUL`, 28 tests.
- `tools/server_console.sh` on each of `mobspells_fire.txt`, `mobspells_water.txt`, `mobspells_ice.txt` and `mobspells_wind.txt` → every villager's health is below `20.0f` (or it died).
- `tools/server_console.sh tools/server_tests/attunement.txt` → the step 2 output.
- After each run, `grep -cE "Exception|/ERROR\]" build/server-console.log` → `0`.

Launch the game as in Task 2 Step 6.

- [ ] **Step 7: Play-test checklist (hand to the user)**

- **Wind Adept:** slashes wind blades. A wind hit on you while you're burning or wet doesn't Swirl onto other players.
- **Wind Magus:** leaps back when you get close, and rushes in when you back off.
- **Wind Archmage:** throws you into the air. You're Airborne, so the zombies' hits and spells do +25% until you land.
- **Mixed fight:** a Fire Archmage, a Water Magus and an Ice Adept together. Casts are staggered (never two at once from one mob), every cast has its wind-up, and they never hurt each other with area spells.

- [ ] **Step 8: Commit and push (only after the user confirms)**

```bash
git add README.md docs src tools
GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com git commit -F - <<'EOF'
Attuned mobs cast Wind spells

Wind Adepts slash wind blades, Magi Gale Dash away or in, Archmages
launch their target with an Updraft. Documents mob spellcasting in the
README and records the SpellTargets and ice ward decisions in the spec.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git push
```
