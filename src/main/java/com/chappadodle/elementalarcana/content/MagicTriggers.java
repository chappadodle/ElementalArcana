package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.event.SpellCastEvent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * Fires the mod's advancement trigger (MagicTrigger). Casts as they happen; reactions and Attuned
 * kills from where they're rewarded (ReactionRewards, CreatureRewards); and every two seconds what
 * a mage has become (elements held, magic level, keystones, each spell's level), so progress made
 * any way (play, a command, an older world) counts.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class MagicTriggers {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, ElementalArcana.MODID);
    public static final DeferredHolder<CriterionTrigger<?>, MagicTrigger> MAGIC = TRIGGERS.register("magic", MagicTrigger::new);
    private static final int CHECK_TICKS = 40;

    private MagicTriggers() {
    }

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }

    public static void fire(ServerPlayer player, String event, @Nullable String key, int value) {
        MAGIC.get().trigger(player, event, key, value);
    }

    @SubscribeEvent
    public static void onCast(SpellCastEvent.Post event) {
        Spell spell = event.spell();
        fire(event.caster(), "cast", spell.id().toString(), MagicAttachments.get(event.caster()).spellLevel(spell));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % CHECK_TICKS != 0) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        if (!data.isAwakened()) {
            return;
        }
        fire(player, "elements", null, data.affinityElements().size());
        fire(player, "level", null, data.level());
        fire(player, "keystones", null, data.keystones().size());
        for (Spell spell : SpellRegistries.SPELLS) {
            int level = data.spellLevel(spell);
            if (level >= 2) {
                fire(player, "spell_level", spell.id().toString(), level);
            }
        }
    }
}
