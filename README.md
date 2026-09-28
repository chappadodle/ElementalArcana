# Elemental Arcana

Innate elemental magic for **NeoForge 1.21.1**, built on a small, expandable framework. There are no wands and no scrolls: magic is a power your character has and grows through use, the way it works in RPGs and isekai.

## Playing

**Awakening.** The first time you join, you choose the element your magic answers to: Fire, Water, Ice or Wind. The choice is permanent. You get another affinity slot at Magic Level 10, 20 and 30.

**Controls** (rebindable under *Elemental Arcana* in Controls):

| Key | |
|---|---|
| **V** (hold) | Spell wheel: point at a spell and release to select it |
| **R** | Cast the selected spell (with any item in hand, or none) |
| **K** | Status window: level, XP, mana stats, affinities and every spell |

**Mana and growth**
- Every point of mana you spend gives Magic XP. Each level adds +10 max mana, faster regen and +2% spell power (max level 30).
- Mana regenerates over time. To meditate, sneak and stand still for 2 seconds; regen is tripled while you do. A full night's sleep refills your mana.
- Emptying your mana gives you *Mana Sickness* for 15s: slower movement, weaker attacks and half regen.
- If you cast without enough mana, the missing amount is paid in health (1 heart per 20 mana). This is called overcasting. It never kills you and always causes Mana Sickness. The HUD mana bar pulses red when your next cast would overcast.

**Spells**

| Element | Lv 1 | Lv 5 |
|---|---|---|
| Fire | Fireball: hold R to grow a fireball, release to throw it; explodes and ignites (sets blocks alight only with `mobGriefing`) | Flame Burst: ring of fire around you |
| Water | Hydro Jet: hold R to spray a pressurized stream that pushes enemies back and soaks them; Tidal Wave: cone that pushes, extinguishes, hurts endermen/blazes | Healing Rain: Regeneration II for you and nearby players |
| Ice | Icicle: hold R to charge an icicle, release to launch it; a full charge freezes | Frost Nova: freezes everything around you |
| Wind | Wind Blade: hold R to charge crescents of wind, release to slash; Gale Dash: launch forward, no fall damage | Updraft: throw yourself and nearby mobs skyward |

**Frost Shield** (Ice, Magic Level 3): tap R to raise orbiting ice shards that absorb damage (shown as ice hearts) and shatter outward when broken.

**Fire** has **Melt**: a fire hit on a frozen or frosted enemy thaws it in a burst of steam for 75% more damage.

**Water** makes enemies **Wet** (so does standing in rain or water). **Vaporize**: water on a burning enemy, or fire on a wet one, deals 50% more damage in a burst of steam. **Freeze**: ice on a wet enemy, or water on a frosted one, freezes it solid.

**Elements matter.** Every spell deals damage of its element, and some creatures are born with an element of their own. Water is strong against Fire, Fire against Ice and Ice against Water (×1.5), and every element resists itself (×0.5). Wind is neutral. The game never tells you a creature's element: watch and listen to how your hits land. (With [Jade](https://modrinth.com/mod/jade) installed, its tooltip shows the element.)

**Wind** has two signature mechanics. **Swirl**: a wind hit on a frozen, frosted, burning or wet enemy spreads that element to every creature within 4 blocks. **Airborne**: enemies launched by wind take 25% more damage until they land.

Fireball, Hydro Jet, Icicle, Frost Shield and Wind Blade level from 1 to 10. Casting a spell fills its mastery bar, and each level-up costs one skill point (you earn one per Magic Level). Each offers permanent path choices at Lv 5 and 10. Open a spell's skill tree from the Status window.

Operators can use `/arcana level set <n>`, `/arcana xp add <n>`, `/arcana affinity add <element>`, `/arcana affinity reset`, `/arcana mana fill|set <n>` and `/arcana cooldowns reset`.

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
