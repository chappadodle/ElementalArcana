# Essence Spell Progression: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:**
- **Infuse:** Elemental Essence fills a same-element spell's mastery bar, by less each level (exponential falloff).
- **Condense:** Essence of any mix of elements becomes bonus skill points, at a rising cost (8, 12, 16…).

**Architecture:**
- **Formulas:** they go in the unit-tested `Progression` class.
- **Saved points:** `MagicData` gains saved, synced `bonusSkillPoints`, which `skillPoints()` adds in.
- **Inventory logic:** a common `EssenceService` maps spells to elements and counts and removes Essence.
- **Network:** a new C2S `EssencePayload` (`INFUSE`, `CONDENSE`) validates and applies on the server.
- **Buttons:** Infuse goes on `SpellDetailScreen` (under the mastery bar) and Condense on `StatusScreen` (under the affinities).

**Tech Stack:** NeoForge 21.1.252 (MC 1.21.1), Java 21, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-28-essence-progression-design.md`

## Global Constraints

- **Infuse:**
  - Fraction per Essence = `0.5 × 0.8^(level − 1)`.
  - Mastery per Essence = `max(1, round(bar × fraction))`.
  - Shift fills the bar using just enough Essence.
  - Only for castable spells below max level whose bar isn't full, with Essence of the spell's element.
- **Condense:**
  - Cost = `8 + 4 × (bonus points already condensed)`, paid in any mix of elements, largest stacks first.
  - The result is +1 `bonusSkillPoints`.
- **The server decides everything**, and the server removes the items.
- **Vanilla sounds.**
- **Git:**
  - Never modify git config.
  - Commit only after the user play-tests and confirms.
  - Commit with the noreply env vars `GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com`, with the `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` trailer.
- `.../` = `src/main/java/com/chappadodle/elementalarcana/`.

---

### Task 1: Formulas (pure, TDD)

**Files:**
- Modify: `.../api/Progression.java`
- Modify: `src/test/java/com/chappadodle/elementalarcana/api/ProgressionTest.java`

**Interfaces:**
- Produces:
  - `Progression.essenceBarFraction(int spellLevel) : float`
  - `Progression.essenceMastery(int spellLevel, int barSize) : int`
  - `Progression.essenceToFill(int spellLevel, int barSize, int currentMastery) : int`
  - `Progression.condenseCost(int alreadyCondensed) : int`

- [ ] **Step 1: Failing tests.** Add these test methods to `ProgressionTest` (inside the class, after `regenGrowsWithMagicLevel`), plus the static imports `essenceBarFraction`, `essenceMastery`, `essenceToFill` and `condenseCost` from `Progression`:

```java
    @Test
    void essenceFillsLessOfTheBarEachLevel() {
        assertEquals(0.5f, essenceBarFraction(1), 1e-6f);
        assertEquals(0.4f, essenceBarFraction(2), 1e-6f);
        assertEquals(0.32f, essenceBarFraction(3), 1e-6f);
        assertEquals(0.2048f, essenceBarFraction(5), 1e-5f);
    }

    @Test
    void essenceMasteryPerEssence() {
        // Bars are 60 x level mastery.
        assertEquals(30, essenceMastery(1, 60));
        assertEquals(58, essenceMastery(3, 180));  // 57.6
        assertEquals(61, essenceMastery(5, 300));  // 61.44
        assertEquals(45, essenceMastery(9, 540));  // 45.3
        assertEquals(1, essenceMastery(9, 1));     // never 0
    }

    @Test
    void essenceNeededToFillTheBar() {
        assertEquals(2, essenceToFill(1, 60, 0));
        assertEquals(1, essenceToFill(1, 60, 30));
        assertEquals(0, essenceToFill(1, 60, 60));
        assertEquals(12, essenceToFill(9, 540, 0));
        int total = 0;
        for (int level = 1; level <= 9; level++) {
            total += essenceToFill(level, 60 * level, 0);
        }
        assertEquals(55, total);
    }

    @Test
    void condensingCostsMoreEachTime() {
        assertEquals(8, condenseCost(0));
        assertEquals(12, condenseCost(1));
        assertEquals(16, condenseCost(2));
        int total = 0;
        for (int i = 0; i < 16; i++) {
            total += condenseCost(i);
        }
        assertEquals(608, total);
    }
```

- [ ] **Step 2:** Run `./gradlew test --console=plain`. Expected: FAIL, `cannot find symbol` for the new methods.

- [ ] **Step 3: Implement.** Add to `Progression` (constants next to the others, methods after `regenPerSecond`):

```java
    private static final float ESSENCE_FIRST_FILL = 0.5f;
    private static final float ESSENCE_FALLOFF = 0.8f;
    private static final int CONDENSE_BASE_COST = 8;
    private static final int CONDENSE_COST_STEP = 4;
```

```java
    /** Share of a spell's mastery bar one Essence fills: half at Lv 1, then 20% less each level. */
    public static float essenceBarFraction(int spellLevel) {
        return ESSENCE_FIRST_FILL * (float) Math.pow(ESSENCE_FALLOFF, Math.max(1, spellLevel) - 1);
    }

    /** Mastery one Essence gives at {@code spellLevel}, for a bar of {@code barSize}; at least 1. */
    public static int essenceMastery(int spellLevel, int barSize) {
        return Math.max(1, Math.round(barSize * essenceBarFraction(spellLevel)));
    }

    /** How many Essence fill the bar from {@code currentMastery}. */
    public static int essenceToFill(int spellLevel, int barSize, int currentMastery) {
        int missing = barSize - currentMastery;
        if (missing <= 0) {
            return 0;
        }
        int each = essenceMastery(spellLevel, barSize);
        return (missing + each - 1) / each;
    }

    /** Essence needed to condense the next bonus skill point, after {@code alreadyCondensed} of them. */
    public static int condenseCost(int alreadyCondensed) {
        return CONDENSE_BASE_COST + CONDENSE_COST_STEP * Math.max(0, alreadyCondensed);
    }
```

Change the class Javadoc to: `How getting better makes magic faster. Plain Java, no Minecraft types (unit tested). A spell's own level shortens its cooldown; your Magic Level speeds up mana regeneration; Elemental Essence fills mastery (less at higher levels) and condenses into skill points (costing more each time).`

- [ ] **Step 4:** Run `./gradlew test --console=plain`. Expected: `BUILD SUCCESSFUL`, with ProgressionTest at `tests="7"` (40 tests in total).

---

### Task 2: Bonus skill points and the server side

**Files:**
- Modify: `.../core/MagicData.java`
- Create: `.../content/EssenceService.java`
- Create: `.../network/EssencePayload.java`
- Modify: `.../network/ModNetwork.java`
- Modify: `src/main/resources/assets/elementalarcana/lang/en_us.json`

**Interfaces:**
- Consumes: `Progression.*` (Task 1), `ModItems.essence(Element)`, and `Element` (existing).
- Produces:
  - `MagicData.bonusSkillPoints() : int`
  - `MagicData.addBonusSkillPoint()`
  - `EssenceService.elementOf(Spell) : @Nullable Element`
  - `EssenceService.count(Player, Element) : int`
  - `EssenceService.total(Player) : int`
  - `EssenceService.infuseAmount(MagicData, Spell) : int` (mastery per Essence right now)
  - `EssenceService.infuseCount(MagicData, Spell, boolean fill, int available) : int`
  - `EssencePayload.infuse(Spell, boolean fill)`
  - `EssencePayload.condense()`

- [ ] **Step 1: `MagicData`.** In `.../core/MagicData.java`:
  1. **Field:** add `private int bonusSkillPoints;` after `private long respecReadyAt;`.
  2. **Codec:** add the field `Codec.INT.optionalFieldOf("bonus_skill_points", 0).forGetter(data -> data.bonusSkillPoints)` after the `respec_ready_at` field. Extend the `apply` lambda to `(mana, level, xp, affinities, selected, cooldowns, fallImmune, freeCast, spells, respecReadyAt, bonusSkillPoints) -> new MagicData(mana, level, xp, affinities, selected.orElse(null), cooldowns, fallImmune, freeCast, spells, respecReadyAt, bonusSkillPoints, false)`.
  3. **Stream codec:**
     - In the writer, add `buf.writeVarInt(data.bonusSkillPoints);` after `buf.writeVarLong(data.respecReadyAt);`.
     - In the reader, add `buf.readVarInt(),` after `buf.readVarLong(),` (the respec line), before the final `buf.readBoolean()`.
  4. **Constructors:**
     - Add the constructor parameter `int bonusSkillPoints` after `long respecReadyAt`, and the line `this.bonusSkillPoints = Math.max(0, bonusSkillPoints);`.
     - The no-arg constructor passes `0L, 0, false`.
  5. **`skillPoints()`:** change `return Math.max(0, (level - 1) - spent);` to `return Math.max(0, (level - 1) + bonusSkillPoints - spent);`.
  6. **Methods:** add these after `skillPoints()`:

```java
    /** Skill points condensed from Elemental Essence, on top of those from Magic Level. */
    public int bonusSkillPoints() {
        return bonusSkillPoints;
    }

    public void addBonusSkillPoint() {
        bonusSkillPoints++;
    }
```

- [ ] **Step 2: `EssenceService`.** Create `src/main/java/com/chappadodle/elementalarcana/content/EssenceService.java`:

```java
package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Elemental Essence for spell progression, shared by the server handler and the screens:
 * which element a spell is, how much Essence a player carries, and what infusing is worth.
 */
public final class EssenceService {

    private EssenceService() {
    }

    /** The element of a spell's school, or null for schools that aren't one of the four elements. */
    @Nullable
    public static Element elementOf(Spell spell) {
        try {
            return Element.valueOf(spell.school().id().getPath().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static int count(Player player, Element element) {
        return player.getInventory().countItem(ModItems.essence(element));
    }

    public static int total(Player player) {
        int total = 0;
        for (Element element : Element.values()) {
            total += count(player, element);
        }
        return total;
    }

    /** Takes up to {@code amount} Essence of {@code element}; returns how many were taken. */
    public static int remove(Player player, Element element, int amount) {
        Item item = ModItems.essence(element);
        return player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), amount, player.inventoryMenu.getCraftSlots());
    }

    /** Mastery one Essence adds to {@code spell} right now (0 if it can't take any). */
    public static int infuseAmount(MagicData data, Spell spell) {
        int bar = data.masteryToNextLevel(spell);
        return bar <= 0 ? 0 : Progression.essenceMastery(data.spellLevel(spell), bar);
    }

    /** How many Essence an infusion would use: 1, or just enough to fill the bar with {@code fill}. */
    public static int infuseCount(MagicData data, Spell spell, boolean fill, int available) {
        int bar = data.masteryToNextLevel(spell);
        int needed = Progression.essenceToFill(data.spellLevel(spell), bar, data.progress(spell).mastery());
        if (bar <= 0 || needed <= 0) {
            return 0;
        }
        return Math.min(available, fill ? needed : 1);
    }
}
```

- [ ] **Step 3: `EssencePayload`.** Create `src/main/java/com/chappadodle/elementalarcana/network/EssencePayload.java`:

```java
package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.content.EssenceService;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Client -> server: spend Elemental Essence. INFUSE puts Essence of a spell's element into its
 * mastery bar (one, or just enough to fill it); CONDENSE turns Essence of any elements into a
 * bonus skill point. The server checks every rule and takes the items itself.
 */
public record EssencePayload(Action action, ResourceLocation spell, boolean fill) implements CustomPacketPayload {
    public static final Type<EssencePayload> TYPE = new Type<>(ElementalArcana.id("essence"));
    public static final StreamCodec<ByteBuf, EssencePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> Action.values()[ordinal], Action::ordinal), EssencePayload::action,
            ResourceLocation.STREAM_CODEC, EssencePayload::spell,
            ByteBufCodecs.BOOL, EssencePayload::fill,
            EssencePayload::new);

    public enum Action {
        INFUSE, CONDENSE
    }

    public static EssencePayload infuse(Spell spell, boolean fill) {
        return new EssencePayload(Action.INFUSE, spell.id(), fill);
    }

    public static EssencePayload condense() {
        return new EssencePayload(Action.CONDENSE, ElementalArcana.id("none"), false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EssencePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        switch (payload.action()) {
            case INFUSE -> infuse(player, data, SpellRegistries.SPELLS.get(payload.spell()), payload.fill());
            case CONDENSE -> condense(player, data);
        }
        MagicAttachments.sync(player);
    }

    private static void infuse(ServerPlayer player, MagicData data, Spell spell, boolean fill) {
        if (spell == null || !data.canCast(spell)) {
            return;
        }
        Element element = EssenceService.elementOf(spell);
        if (element == null) {
            return;
        }
        int count = EssenceService.infuseCount(data, spell, fill, EssenceService.count(player, element));
        int each = EssenceService.infuseAmount(data, spell);
        if (count <= 0 || each <= 0) {
            return;
        }
        int taken = EssenceService.remove(player, element, count);
        data.addMastery(spell, taken * each);
        int color = element.color();
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 1.0f);
        player.serverLevel().sendParticles(dust, player.getX(), player.getY(1.0), player.getZ(), 16, 0.4, 0.5, 0.4, 0.05);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
    }

    private static void condense(ServerPlayer player, MagicData data) {
        int cost = Progression.condenseCost(data.bonusSkillPoints());
        if (EssenceService.total(player) < cost) {
            return;
        }
        // Largest stacks first.
        Element[] byCount = Element.values().clone();
        Arrays.sort(byCount, Comparator.comparingInt((Element element) -> EssenceService.count(player, element)).reversed());
        int remaining = cost;
        for (Element element : byCount) {
            remaining -= EssenceService.remove(player, element, remaining);
            if (remaining <= 0) {
                break;
            }
        }
        data.addBonusSkillPoint();
        player.sendSystemMessage(Component.translatable("message.elementalarcana.condensed", cost).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.playNotifySound(ModContent.LEVEL_UP_SOUND.get(), SoundSource.PLAYERS, 0.9f, 1.2f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 20, 0.4, 0.6, 0.4, 0.04);
    }
}
```

- [ ] **Step 4: Register it.** In `.../network/ModNetwork.java`:
  - Change `event.registrar("2")` to `event.registrar("3")`. The protocol changed, so mismatched clients and servers now refuse to connect instead of misbehaving.
  - Add `.playToServer(EssencePayload.TYPE, EssencePayload.STREAM_CODEC, EssencePayload::handle)` after the `SpellProgressPayload` line. Move the `;` accordingly.

- [ ] **Step 5: Lang.** Add to `en_us.json`:

```json
  "message.elementalarcana.condensed": "Condensed %s Essence into a skill point",
  "item.elementalarcana.essence.tooltip": "Infuse it into a spell of its element (spell screen), or condense it into skill points (Status screen)."
```

The second line replaces the existing `item.elementalarcana.essence.tooltip` value; it's the same key.

- [ ] **Step 6: Build and regression check.** Run `./gradlew build --console=plain` (`BUILD SUCCESSFUL`, 40 tests), then `tools/server_console.sh tools/server_tests/attunement.txt` (the step 2 output) and `grep -cE "Exception|/ERROR\]" build/server-console.log` (`0`).

---

### Task 3: The buttons, docs, play-test, commit

**Files:**
- Modify: `.../client/SpellDetailScreen.java`
- Modify: `.../client/StatusScreen.java`
- Modify: `src/main/resources/assets/elementalarcana/lang/en_us.json`
- Modify: `README.md`

**Interfaces:**
- Consumes: `EssenceService.*` and `EssencePayload.*` (Task 2), `Progression.condenseCost` and `essenceBarFraction` (Task 1).

- [ ] **Step 1: Infuse button.** In `.../client/SpellDetailScreen.java`:
  1. Add the field `private Button infuseButton;` next to `levelUpButton`.
  2. In `init()`, before `updateButtons();`, add:

```java
        // Under the mastery bar (the header has room between the bar text and the list).
        infuseButton = addRenderableWidget(Button.builder(Component.empty(),
                        button -> PacketDistributor.sendToServer(EssencePayload.infuse(spell, Screen.hasShiftDown())))
                .bounds(left + 50, top + 51, 170, 13).build());
```

  3. At the end of `updateButtons()`, add:

```java
        updateInfuseButton(data);
```

  4. Add this method after `updateButtons()`:

```java
    /** "Infuse 1 Fire Essence (+32%)", or with Shift "Infuse 3 Fire Essence (fill)"; disabled with a reason. */
    private void updateInfuseButton(MagicData data) {
        Element element = EssenceService.elementOf(spell);
        infuseButton.visible = element != null && spell.maxLevel() > 1;
        if (!infuseButton.visible) {
            return;
        }
        Component essenceName = ModItems.essence(element).getDescription();
        int have = EssenceService.count(minecraft.player, element);
        boolean fill = Screen.hasShiftDown();
        int count = EssenceService.infuseCount(data, spell, fill, have);
        int percent = Math.round(Progression.essenceBarFraction(data.spellLevel(spell)) * 100);
        infuseButton.setMessage(fill
                ? Component.translatable("screen.elementalarcana.detail.infuse_fill", Math.max(count, 1), essenceName)
                : Component.translatable("screen.elementalarcana.detail.infuse", essenceName, percent));
        Component reason = null;
        if (!data.canCast(spell)) {
            reason = Component.translatable("screen.elementalarcana.detail.locked");
        } else if (data.masteryToNextLevel(spell) <= 0) {
            reason = Component.translatable("screen.elementalarcana.detail.max_level");
        } else if (data.isMasteryFull(spell)) {
            reason = Component.translatable("screen.elementalarcana.detail.infuse.full");
        } else if (have <= 0) {
            reason = Component.translatable("screen.elementalarcana.detail.infuse.none", essenceName);
        }
        infuseButton.active = reason == null;
        infuseButton.setTooltip(Tooltip.create(reason != null ? reason
                : Component.translatable("screen.elementalarcana.detail.infuse.hint", have, essenceName)));
    }
```

  5. Add these imports:

```java
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.content.EssenceService;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.network.EssencePayload;
```

`Screen` is the superclass, so `Screen.hasShiftDown()` needs no import.

- [ ] **Step 2: Condense button.** In `.../client/StatusScreen.java`:
  1. Add the field `private Button condenseButton;` next to `awakenButton`.
  2. In `init()`, after `updateAwakenButton();`, add:

```java
        // Under the affinities, above the spell list.
        condenseButton = addRenderableWidget(Button.builder(Component.empty(),
                        button -> PacketDistributor.sendToServer(EssencePayload.condense()))
                .bounds(left + 10, top + 101, 190, 14).build());
        updateCondenseButton();
```

  3. Add this method after `updateAwakenButton()`:

```java
    private void updateCondenseButton() {
        MagicData data = MagicAttachments.get(minecraft.player);
        int cost = Progression.condenseCost(data.bonusSkillPoints());
        int have = EssenceService.total(minecraft.player);
        condenseButton.setMessage(Component.translatable("screen.elementalarcana.status.condense", cost));
        condenseButton.active = have >= cost;
        condenseButton.setTooltip(Tooltip.create(Component.translatable("screen.elementalarcana.status.condense.hint",
                have, data.bonusSkillPoints())));
    }
```

  4. Call `updateCondenseButton()` every tick. If the screen already overrides `tick()`, add the call there. Otherwise add:

```java
    @Override
    public void tick() {
        updateCondenseButton();
    }
```

  5. Add these imports as needed: `com.chappadodle.elementalarcana.api.Progression`, `com.chappadodle.elementalarcana.content.EssenceService`, `com.chappadodle.elementalarcana.network.EssencePayload`, `net.minecraft.client.gui.components.Tooltip`, and `net.neoforged.neoforge.network.PacketDistributor`.

- [ ] **Step 3: Lang.** Add to `en_us.json`:

```json
  "screen.elementalarcana.detail.infuse": "Infuse 1 %s (+%s%%)",
  "screen.elementalarcana.detail.infuse_fill": "Infuse %s %s (fill)",
  "screen.elementalarcana.detail.infuse.full": "The mastery bar is already full: level the spell up first.",
  "screen.elementalarcana.detail.infuse.none": "You have no %s.",
  "screen.elementalarcana.detail.infuse.hint": "You have %s %s. Each fills less of the bar the higher the spell's level. Hold Shift to fill the bar.",
  "screen.elementalarcana.status.condense": "Condense %s Essence → +1 skill point",
  "screen.elementalarcana.status.condense.hint": "You carry %s Essence (any element counts). Bonus points so far: %s. Each costs 4 more than the last."
```

- [ ] **Step 4: README.** In the paragraph that starts "Fireball, Hydro Jet, Icicle, Frost Shield and Wind Blade level from 1 to 10.", add at the end:

```markdown
Elemental Essence speeds this up: **Infuse** it into a spell of its element from the spell's screen (one Essence fills half of a Lv 1 bar, 20% less each level after), or **Condense** it into bonus skill points from the Status screen (8 Essence for the first, 4 more for each after).
```

In the **Creature magic** paragraph, change "Essence has no use yet: hold on to it." to "Essence feeds your spells (see below)."

- [ ] **Step 5: Verify.** Run `./gradlew build --console=plain` (`BUILD SUCCESSFUL`, 40 tests), then `tools/server_console.sh tools/server_tests/essence_items.txt` (`Test passed, count: 4`), then `grep -cE "Exception|/ERROR\]" build/server-console.log` (`0`). Launch `runClient` and check its log for `Exception|ERROR` (ignore the goat-horn warnings).

- [ ] **Step 6: Play-test checklist (for the user).** Get test Essence with `/give @s elementalarcana:fire_essence 64` (and the same for water, ice and wind).
  1. Open Fireball's screen (Status → Fireball) at Lv 1 with an empty bar. The button reads **"Infuse 1 Fire Essence (+50%)"**. Two clicks fill the bar and use 2 Essence.
  2. Level it up, then hold **Shift**: the button reads **"Infuse 3 Fire Essence (fill)"** at Lv 2. One click fills the bar.
  3. The button is disabled with a reason when the bar is full, when you have no Fire Essence, and on a spell of an element you haven't awakened.
  4. Status screen: **"Condense 8 Essence → +1 skill point"**. Clicking it takes 8 Essence (from your biggest stack) and adds 1 skill point. The next condense costs 12.
  5. Die and respawn: the bonus skill points are still there.

- [ ] **Step 7: Commit and push (after the user confirms)**

```bash
git add README.md docs src
GIT_AUTHOR_NAME=chappadodle GIT_AUTHOR_EMAIL=86350776+chappadodle@users.noreply.github.com GIT_COMMITTER_NAME=chappadodle GIT_COMMITTER_EMAIL=86350776+chappadodle@users.noreply.github.com git commit -F - <<'EOF'
Elemental Essence feeds spell progression

Infuse Essence into a spell of its element to fill its mastery bar
(half a bar at Lv 1, 20% less each level), or condense Essence of any
element into bonus skill points (8, then 4 more each time). Buttons on
the spell and Status screens; the server validates and takes the items.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git push
```
