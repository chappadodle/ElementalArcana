package com.chappadodle.elementalarcana.content.brew;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Brews (docs/superpowers/specs/2026-10-02-brews-design.md): for when the pool runs dry. Brewed in a
 * brewing stand from an Awkward Potion, made splash, lingering or into arrows like any potion.
 * <ul>
 * <li>Mana Draught (amethyst shard): refills 30% of your mana at once (60% strong).</li>
 * <li>Elixir of Clarity (Wisp Mote): mana regenerates twice as fast (three times strong).</li>
 * <li>Elixir of Focus (lapis lazuli): +6 Focus (+10 strong), so shorter cooldowns.</li>
 * <li>Elixir of Warding (prismarine crystals): +6 Ward (+10 strong), so less elemental damage.</li>
 * </ul>
 * Focus and Warding feed the stat system like gear (GearStats); Clarity is read by the mana regen
 * (MagicEvents).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ModBrews {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ElementalArcana.MODID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final int NORMAL = 20 * 180;
    private static final int LONG = 20 * 480;
    private static final int STRONG = 20 * 90;
    /** An elixir's stat bonus, and how much more each level of strength adds. */
    public static final int ELIXIR_STAT = 6;
    public static final int ELIXIR_STAT_PER_LEVEL = 4;

    public static final DeferredHolder<MobEffect, ManaRestorationEffect> MANA_RESTORATION = EFFECTS.register("mana_restoration", ManaRestorationEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> CLARITY = EFFECTS.register("clarity",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x8FE3FF) {
            });
    public static final DeferredHolder<MobEffect, MobEffect> FOCUS = EFFECTS.register("focus",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFD36A) {
            });
    public static final DeferredHolder<MobEffect, MobEffect> WARDING = EFFECTS.register("warding",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x7FB8C9) {
            });

    public static final DeferredHolder<Potion, Potion> MANA_DRAUGHT = potion("mana_draught", "mana_draught", MANA_RESTORATION, 1, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_MANA_DRAUGHT = potion("strong_mana_draught", "mana_draught", MANA_RESTORATION, 1, 1);
    public static final DeferredHolder<Potion, Potion> CLARITY_ELIXIR = potion("clarity", "clarity", CLARITY, NORMAL, 0);
    public static final DeferredHolder<Potion, Potion> LONG_CLARITY_ELIXIR = potion("long_clarity", "clarity", CLARITY, LONG, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_CLARITY_ELIXIR = potion("strong_clarity", "clarity", CLARITY, STRONG, 1);
    public static final DeferredHolder<Potion, Potion> FOCUS_ELIXIR = potion("focus", "focus", FOCUS, NORMAL, 0);
    public static final DeferredHolder<Potion, Potion> LONG_FOCUS_ELIXIR = potion("long_focus", "focus", FOCUS, LONG, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_FOCUS_ELIXIR = potion("strong_focus", "focus", FOCUS, STRONG, 1);
    public static final DeferredHolder<Potion, Potion> WARDING_ELIXIR = potion("warding", "warding", WARDING, NORMAL, 0);
    public static final DeferredHolder<Potion, Potion> LONG_WARDING_ELIXIR = potion("long_warding", "warding", WARDING, LONG, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_WARDING_ELIXIR = potion("strong_warding", "warding", WARDING, STRONG, 1);

    /** A wisp's spark of pure mana (wisps drop it): what an Elixir of Clarity is brewed from. */
    public static final DeferredItem<Item> WISP_MOTE = ITEMS.register("wisp_mote", () -> new Item(new Item.Properties()));

    private ModBrews() {
    }

    /** Every brew, weakest first (for the creative tab). */
    public static List<DeferredHolder<Potion, Potion>> potions() {
        return List.of(MANA_DRAUGHT, STRONG_MANA_DRAUGHT, CLARITY_ELIXIR, LONG_CLARITY_ELIXIR, STRONG_CLARITY_ELIXIR,
                FOCUS_ELIXIR, LONG_FOCUS_ELIXIR, STRONG_FOCUS_ELIXIR, WARDING_ELIXIR, LONG_WARDING_ELIXIR, STRONG_WARDING_ELIXIR);
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        POTIONS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    /** A potion named "elementalarcana.<name>" (strong and long versions share the name, as vanilla's do). */
    private static DeferredHolder<Potion, Potion> potion(String id, String name, DeferredHolder<MobEffect, ? extends MobEffect> effect,
                                                         int duration, int amplifier) {
        return POTIONS.register(id, () -> new Potion(ElementalArcana.MODID + "." + name, new MobEffectInstance(effect, duration, amplifier)));
    }

    @SubscribeEvent
    public static void onBrewingRecipes(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder brewing = event.getBuilder();
        brewing.addMix(Potions.AWKWARD, Items.AMETHYST_SHARD, MANA_DRAUGHT);
        brewing.addMix(MANA_DRAUGHT, Items.GLOWSTONE_DUST, STRONG_MANA_DRAUGHT);
        brewing.addMix(Potions.AWKWARD, WISP_MOTE.get(), CLARITY_ELIXIR);
        brewing.addMix(CLARITY_ELIXIR, Items.REDSTONE, LONG_CLARITY_ELIXIR);
        brewing.addMix(CLARITY_ELIXIR, Items.GLOWSTONE_DUST, STRONG_CLARITY_ELIXIR);
        brewing.addMix(Potions.AWKWARD, Items.LAPIS_LAZULI, FOCUS_ELIXIR);
        brewing.addMix(FOCUS_ELIXIR, Items.REDSTONE, LONG_FOCUS_ELIXIR);
        brewing.addMix(FOCUS_ELIXIR, Items.GLOWSTONE_DUST, STRONG_FOCUS_ELIXIR);
        brewing.addMix(Potions.AWKWARD, Items.PRISMARINE_CRYSTALS, WARDING_ELIXIR);
        brewing.addMix(WARDING_ELIXIR, Items.REDSTONE, LONG_WARDING_ELIXIR);
        brewing.addMix(WARDING_ELIXIR, Items.GLOWSTONE_DUST, STRONG_WARDING_ELIXIR);
    }
}
