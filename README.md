# Elemental Arcana

Innate elemental magic for **NeoForge 1.21.1**, built on a small, expandable framework. There are no wands and no scrolls: magic is a power your character has and grows through use, the way it works in RPGs and isekai.

## Playing

**Awakening.** You don't pick your element: your magic chooses you. You start with dormant mana and no spells. It wakes by itself, at a small chance each Minecraft day and surely by day 16 (after day 10 the odds rise sharply), or sooner if you survive a brush with an element at 3 hearts or less: fire, drowning, freezing or a hard fall (about a 1 in 3 chance each time, certain from day 10). A brush leans toward its own element, and Ice is about a hundred times rarer than Water to wake first (it's a relative of Water, reached through Water's part of the skill tree). More elements come from **Catalysts** (Fire, Water, Wind or Earth; 8 of its Essence and a diamond, shapeless): right-click one to try to wake that element. It's used up every try. A second element is very unlikely before level 10 and realistic after; a third, harder one is realistic from level 20, a fourth from level 30 (the odds also rise a little with each level, and each failure helps the next try). There are no element slots; rarity and cost are the limit. Opposed elements (Fire vs Water, Fire vs Ice) can't be combined until level 50. Your own elements hurt you 25% less: their spells, and their everyday damage too (Fire: burning and lava; Ice: freezing; Water: drowning; Wind: falling).

**Controls** (rebindable under *Elemental Arcana* in Controls):

| Key | |
|---|---|
| **V** (hold) | Spell wheel: point at a spell and release to select it |
| **R** | Cast the selected spell (with any item in hand, or none). For Icicle, Fireball and Wind Blade: each press **conjures** one more, up to your spell level's maximum |
| **Left click** (while conjuring) | Launch one conjured projectile |
| **Right click** (while conjuring) | Launch all of them. With nothing conjured, the mouse works as normal |
| **K** | Status window: level, XP, mana stats, affinities and every spell |

**Level and stats**
- Everyone has a level, up to 100: you, and every creature in the world. Each level needs 9% more XP than the last.
- XP is mana you absorb from what you kill: more from higher-level creatures, Attuned ones and big ones, less from weaker ones, and nothing from creatures 10 or more levels below you. Setting off a reaction (Melt, Vaporize, Freeze, Swirl) on a creature gives a fifth of its kill XP, once every 5 seconds. Casting itself gives no XP.
- The level gap matters in every fight: each level of difference makes the stronger side hit 4.5% harder and take 4.5% less, both ways (a creature 20 levels above you hits about 2.4 times as hard and takes 2.4 times less).
- Each level gives a **stat point**. Spend them in the **Stats** window (the Status window's top-left button):

  | Stat | Does |
  |---|---|
  | Reservoir | Max mana and regen (both also grow a little with your level) |
  | Potency | Power of every spell |
  | Affinity (one per element family; Ice belongs to Water's) | Power of that family's spells |
  | Focus | Shorter cooldowns |
  | Ward | Less elemental damage taken |
  | Vitality | More max health |
  | Insight | Harder reaction hits, more Essence |

  Points are worth the most up to 20 in a stat, less up to 40 and less again after that.
- Mana regenerates over time. To meditate, sneak and stand still for 2 seconds; regen is tripled while you do. A full night's sleep refills your mana.
- Emptying your mana gives you *Mana Sickness* for 15s: slower movement, weaker attacks and half regen.
- If you cast without enough mana, the missing amount is paid in health (1 heart per 20 mana). This is called overcasting. It never kills you and always causes Mana Sickness. The HUD mana bar pulses red when your next cast would overcast.

**Spells**

| Element | Lv 1 | Lv 5 |
|---|---|---|
| Fire | Fireball: press R to conjure fireballs that grow in your palm, click to throw one or all; explodes and ignites (sets blocks alight only with `mobGriefing`) | Flame Burst: ring of fire around you |
| Water | Hydro Jet: hold R to spray a pressurized stream that pushes enemies back and soaks them; Tidal Wave: cone that pushes, extinguishes, hurts endermen/blazes | Healing Rain: Regeneration II for you and nearby players |
| Ice | Icicle: press R to conjure icicles one at a time (they grow sharper while held, 1 mana/s each), click to launch one or all; a fully grown icicle freezes | Frost Nova: freezes everything around you |
| Earth | Boulder: press R to conjure a heavy boulder, click to throw it; it arcs, crushes and shoves far, and bounces from Lv 2; Stone Skin: orbiting stones absorb damage, then burst outward (a shield spell like Frost Shield); Tremor: a shockwave along the ground that throws enemies up and slows them | |
| Wind | Wind Blade: press R to conjure crescents of wind, click to slash with one or all; Gale Dash: launch forward, no fall damage | Updraft: throw yourself and nearby mobs skyward |

**Conjuring** (Icicle, Fireball, Wind Blade): press R to conjure one projectile at a time, up to your spell level's maximum. Each costs mana when conjured (the first full price, each extra less), grows while you hold it, and costs 1 mana per second to keep; if you can't pay, everything you hold launches itself. Left click launches one, right click launches all. The cooldown starts when the last one leaves. With a full, fully grown set, hold R for a second to fuse it (Glacial Lance, Sunfire, Storm Scythe).

**Bubble Prison** (Water, level 3): tap R to trap the creature (or player) under your crosshair in a floating water bubble for 4 seconds. It's helpless and Wet, which sets up Freeze and Vaporize combos, and any hit pops it for +4 damage. Bosses can't be trapped.

**Frost Shield** (Ice, level 3): tap R to raise orbiting ice shards that absorb damage (shown as ice hearts) and shatter outward when broken.

**Fire** has **Melt**: a fire hit on a frozen or frosted enemy thaws it in a burst of steam for 75% more damage.

**Water** makes enemies **Wet** (so does standing in rain or water). **Vaporize**: water on a burning enemy, or fire on a wet one, deals 50% more damage in a burst of steam. **Freeze**: ice on a wet enemy, or water on a frosted one, freezes it solid.

**Elements matter.** Every spell deals damage of its element, and some creatures are born with an element of their own. Water is strong against Fire, Fire against Ice and Ice against Water (×1.5); Earth is strong against Wind and Water is strong against Earth (×1.5); and every element resists itself (×0.5). Everything else is neutral. The game never tells you a creature's element: watch and listen to how your hits land. (With [Jade](https://modrinth.com/mod/jade) installed, its tooltip shows the element.)

**Creature magic.** Once someone has awakened magic, hostile mobs nearby occasionally spawn **Attuned** to an element: an **Adept** (5%), a **Magus** (1%, 8 levels above the creatures around it) or, very rarely, an **Archmage** (0.1%, 20 levels above them, with a boss bar). Every creature's level comes from where it is: 1 to 5 near world spawn and higher further away (up to +30), more in deep caves, structures and the deep dark; the Nether is level 25 to 50 and the End 60 and up. Creatures put their stat points into Vitality, Ward and Potency. They are more common far from world spawn (up to three times as likely), and their element leans toward the biome's. Attuned creatures are hit by the element chart like any elemental creature, and they cast their element's spells: Adepts know one, Magi two, Archmages three (Fire: fireballs, burning ground, Meteors; Water: a jet, healing their allies, a whirlpool; Ice: icicles, Frost Nova, an ice ward; Wind: wind blades, Gale Dash, Updraft; Earth: boulders, a Tremor, a stone ward). Every cast is announced by half a second of glowing hands and a warning sound: move! Getting close doesn't stop them: up close they cast point-blank, or burst their element in your face to shove you back. Defeating one gives far more XP than an ordinary creature (Adept ×3, Magus ×5, Archmage ×12) and may drop **Elemental Essence** of its element (Adept: half the time; Magus: 1-2; Archmage: 3-5). Creatures born with an element, like blazes, drop it now and then too. Essence feeds your spells (see below). Watch for a faint hint of their element.

**Earth** is the heavy, sturdy element. Its signature is **Crystallize**: an Earth hit on a burning, wet or frozen creature puts that out and leaves a shard where it stood; walk into the shard for 3 hearts of absorption for 10 seconds (a creature leaves one shard every 5 seconds). Boulder grows into Landslide or Bedrock at Lv 5 and Mountainfall or Avalanche at Lv 10; Stone Skin into Bastion or Crystal Carapace and Fortress or Titan. Earth's brush (to wake it first) is surviving a falling block, stalagmite, anvil or suffocation at 3 hearts or less. Innate Earth creatures: iron golems, silverfish and armadillos.

**Wind** has two signature mechanics. **Swirl**: a wind hit on a frozen, frosted, burning or wet enemy spreads that element to every creature within 4 blocks. **Airborne**: enemies launched by wind take 25% more damage until they land.

**The skill tree.** Each level also gives a **tree point** for the skill tree (the Status window's **Tree** button, or click a spell there). It's one big map: a ring of small stat nodes in the middle and a region for each element around it (Wind north, Fire east, Water south, Earth west; Ice is a cluster on Water's outer edge, behind Water Affinity 10). Your element's start node is free and gives you its first spell (Fireball, Hydro Jet, Icicle, Wind Blade or Boulder). From there you take nodes next to ones you hold: small nodes add +2 to a stat, spell nodes unlock the other spells, and each leveled spell (Fireball, Hydro Jet, Icicle, Frost Shield, Wind Blade) has a chain of nodes behind it, its levels 2 to 10, splitting into a choice of paths at Lv 5 and 10 (taking one locks the other). A spell's next level also needs its mastery bar full: casting fills it, and so does **Infusing** Essence of its element (from the tree's bottom panel). Each level also shortens the spell's cooldown (Lv 10: under half of Lv 1), and so does Focus. Regions of elements you don't have are sealed, but related elements (Water and Ice) reach into each other's. Right-click a node you hold to give it back, or right-click a stat in the Stats window, for 1 Essence (plus 1 every 20 levels), as long as the rest of your tree stays connected. **Condense** Essence into bonus tree points from the Status screen (8 Essence for the first, 4 more for each after).

Operators can use `/arcana level set <n>`, `/arcana xp add <n>`, `/arcana affinity add <element>`, `/arcana affinity reset`, `/arcana awaken <element>`, `/arcana awaken day <n>`, `/arcana awaken clock`, `/arcana mana fill|set <n>`, `/arcana attune <targets> <element> <rank>|none`, `/arcana cooldowns reset`, `/arcana stats reset`, `/arcana stats spend <count> <stat>` (e.g. `potency` or `affinity/fire`) and `/arcana creaturelevel <targets> [set <n>]` and `/arcana tree info|take <node>|refund <node>|reset`.

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

**New element:** register a `SpellSchool` (color + cast sound) on `SpellRegistries.SCHOOL_KEY` and add the lang keys `school.yourmod.<name>` and `school.yourmod.<name>.desc`. It joins the awakening roll once it has a region in the skill tree (the roll currently covers the built-in elements).

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
