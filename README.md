# Elemental Arcana

Innate elemental magic for **NeoForge 1.21.1**, built on a small, expandable framework. There are no wands and no scrolls: magic is a power your character has and grows through use, the way it works in RPGs and isekai.

## Playing

**Awakening.** The first time you join, you choose the element your magic answers to: Fire, Water, Ice or Wind. The choice is permanent. You get another affinity slot at Magic Level 10, 20 and 30. Opposed elements (Fire vs Water, Fire vs Ice) can't be combined until Magic Level 30; from then on they fit any free slot. Your own elements hurt you 25% less: their spells, and their everyday damage too (Fire: burning and lava; Ice: freezing; Water: drowning; Wind: falling).

**Controls** (rebindable under *Elemental Arcana* in Controls):

| Key | |
|---|---|
| **V** (hold) | Spell wheel: point at a spell and release to select it |
| **R** | Cast the selected spell (with any item in hand, or none). For Icicle: each press **conjures** one more icicle, up to your spell level's maximum |
| **Left click** (while conjuring) | Launch one conjured projectile |
| **Right click** (while conjuring) | Launch all of them. With nothing conjured, the mouse works as normal |
| **K** | Status window: level, XP, mana stats, affinities and every spell |

**Mana and growth**
- Every point of mana you spend gives Magic XP. Each level adds +10 max mana, +0.25 mana/s regen (2.5/s at level 1) and +2% spell power (max level 30).
- Mana regenerates over time. To meditate, sneak and stand still for 2 seconds; regen is tripled while you do. A full night's sleep refills your mana.
- Emptying your mana gives you *Mana Sickness* for 15s: slower movement, weaker attacks and half regen.
- If you cast without enough mana, the missing amount is paid in health (1 heart per 20 mana). This is called overcasting. It never kills you and always causes Mana Sickness. The HUD mana bar pulses red when your next cast would overcast.

**Spells**

| Element | Lv 1 | Lv 5 |
|---|---|---|
| Fire | Fireball: hold R to grow a fireball, release to throw it; explodes and ignites (sets blocks alight only with `mobGriefing`) | Flame Burst: ring of fire around you |
| Water | Hydro Jet: hold R to spray a pressurized stream that pushes enemies back and soaks them; Tidal Wave: cone that pushes, extinguishes, hurts endermen/blazes | Healing Rain: Regeneration II for you and nearby players |
| Ice | Icicle: press R to conjure icicles one at a time (they grow sharper while held, 1 mana/s each), click to launch one or all; a fully grown icicle freezes | Frost Nova: freezes everything around you |
| Wind | Wind Blade: hold R to charge crescents of wind, release to slash; Gale Dash: launch forward, no fall damage | Updraft: throw yourself and nearby mobs skyward |

**Conjuring** (Icicle; Fireball and Wind Blade coming): press R to conjure one projectile at a time, up to your spell level's maximum. Each costs mana when conjured (the first full price, each extra less), grows while you hold it, and costs 1 mana per second to keep; if you can't pay, everything you hold launches itself. Left click launches one, right click launches all. The cooldown starts when the last one leaves. With a full, fully grown set, hold R for a second to fuse it (Glacial Lance).

**Bubble Prison** (Water, Magic Level 3): tap R to trap the creature (or player) under your crosshair in a floating water bubble for 4 seconds. It's helpless and Wet, which sets up Freeze and Vaporize combos, and any hit pops it for +4 damage. Bosses can't be trapped.

**Frost Shield** (Ice, Magic Level 3): tap R to raise orbiting ice shards that absorb damage (shown as ice hearts) and shatter outward when broken.

**Fire** has **Melt**: a fire hit on a frozen or frosted enemy thaws it in a burst of steam for 75% more damage.

**Water** makes enemies **Wet** (so does standing in rain or water). **Vaporize**: water on a burning enemy, or fire on a wet one, deals 50% more damage in a burst of steam. **Freeze**: ice on a wet enemy, or water on a frosted one, freezes it solid.

**Elements matter.** Every spell deals damage of its element, and some creatures are born with an element of their own. Water is strong against Fire, Fire against Ice and Ice against Water (×1.5), and every element resists itself (×0.5). Wind is neutral. The game never tells you a creature's element: watch and listen to how your hits land. (With [Jade](https://modrinth.com/mod/jade) installed, its tooltip shows the element.)

**Creature magic.** Once someone has awakened magic, hostile mobs nearby occasionally spawn **Attuned** to an element: an **Adept** (5%), a **Magus** (1%, needs Magic Level 5, +50% health) or, very rarely, an **Archmage** (0.1%, needs Magic Level 10, three times the health, a boss bar). They are more common far from world spawn (up to three times as likely), and their element leans toward the biome's. Attuned creatures are hit by the element chart like any elemental creature, and they cast their element's spells: Adepts know one, Magi two, Archmages three (Fire: fireballs, burning ground, Meteors; Water: a jet, healing their allies, a whirlpool; Ice: icicles, Frost Nova, an ice ward; Wind: wind blades, Gale Dash, Updraft). Every cast is announced by half a second of glowing hands and a warning sound: move! Getting close doesn't stop them: up close they cast point-blank, or burst their element in your face to shove you back. Defeating one grants Magic XP (Adept 20, Magus 60, Archmage 250) and may drop **Elemental Essence** of its element (Adept: half the time; Magus: 1-2; Archmage: 3-5). Creatures born with an element, like blazes, drop it now and then too. Essence feeds your spells (see below). Watch for a faint hint of their element.

**Wind** has two signature mechanics. **Swirl**: a wind hit on a frozen, frosted, burning or wet enemy spreads that element to every creature within 4 blocks. **Airborne**: enemies launched by wind take 25% more damage until they land.

Fireball, Hydro Jet, Icicle, Frost Shield and Wind Blade level from 1 to 10. Each level also shortens the spell's cooldown (Lv 10: under half of Lv 1), and so does every Magic Level (1% each). Casting a spell fills its mastery bar, and each level-up costs one skill point (you earn one per Magic Level). Each offers permanent path choices at Lv 5 and 10. Open a spell's skill tree from the Status window. Elemental Essence speeds this up: **Infuse** it into a spell of its element from the spell's screen (one Essence fills half of a Lv 1 bar, 20% less each level after), or **Condense** it into bonus skill points from the Status screen (8 Essence for the first, 4 more for each after).

Operators can use `/arcana level set <n>`, `/arcana xp add <n>`, `/arcana affinity add <element>`, `/arcana affinity reset`, `/arcana mana fill|set <n>`, `/arcana attune <targets> <element> <rank>|none` and `/arcana cooldowns reset`.

## Adding a spell

The built-in spells are registered this same way, so anything they do, yours can do too.

1. **Write it.** Extend `Spell`. If it fires a projectile, also implement `ProjectileSpell`; it will reuse the shared `SpellProjectile` entity, so you need no new entity or renderer.

   ```java
   public class LightningSpell extends Spell {
       public LightningSpell() {
           // school, mana cost, cooldown in ticks, required magic level
           super(MySchools.STORM, 30, 60, 8);
       }

       @Override
       public CastResult cast(CastContext context) {
           // context.caster(), context.level(), context.look(), context.power() ...
           return CastResult.SUCCESS; // or CastResult.fail(Component) to cancel at no cost
       }
   }
   ```

2. **Register it:**

   ```java
   DeferredRegister<Spell> SPELLS = DeferredRegister.create(SpellRegistries.SPELL_KEY, "yourmod");
   SPELLS.register("lightning", LightningSpell::new);
   ```

3. **Add assets:** lang keys `spell.yourmod.lightning` and `spell.yourmod.lightning.desc`, plus a 16×16 icon at `assets/yourmod/textures/spell/lightning.png`.

That's all it takes. Players who have awakened the spell's element and reached its level will see it in the wheel, Status window and HUD, with mana, cooldowns, XP and syncing handled for them.

**New element:** register a `SpellSchool` (color + cast sound) on `SpellRegistries.SCHOOL_KEY` and add the lang keys `school.yourmod.<name>` and `school.yourmod.<name>.desc`. It shows up on the Awakening screen automatically.

**Shields:** implement `ShieldSpell` and raise a `SpellShield` in `cast`. Absorbing damage, ice hearts, orbiting shards and syncing are handled for you, and the hooks cover parries, reflections, breaking and death-saves.

**Multi-hit spells:** deal damage with `SpellDamage.hurtMultiHit(...)`. Minecraft otherwise ignores hits that land within half a second of each other, so a volley would only count once.

**Hooks:** `SpellCastEvent.Pre` (cancel a cast or change its mana cost) and `SpellCastEvent.Post`, both on `NeoForge.EVENT_BUS`.

## Assets

Textures and sounds are generated by scripts in `tools/`, so they can be tweaked and rebuilt:

```
python3 tools/gen_textures.py   # needs Pillow
bash tools/gen_sounds.sh        # needs sox
python3 tools/gen_spell_sounds.py  # needs numpy, scipy and sox
```

See `CREDITS.md`.
