package com.chappadodle.elementalarcana.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The advancement trigger for what a mage does (see MagicTriggers, which fires it): an
 * {@code event} ("elements", "level", "cast", "spell_level", "keystones", "reaction",
 * "defeated"), with an optional {@code key} it must match (a spell's id, an Attuned rank) and an
 * optional {@code min} its number must reach (how many elements, which level).
 * <pre>{"trigger": "elementalarcana:magic", "conditions": {"event": "level", "min": 25}}</pre>
 */
public class MagicTrigger extends SimpleCriterionTrigger<MagicTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String event, @Nullable String key, int value) {
        trigger(player, instance -> instance.matches(event, key, value));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String event, Optional<String> key,
                                  Optional<Integer> min) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("event").forGetter(TriggerInstance::event),
                Codec.STRING.optionalFieldOf("key").forGetter(TriggerInstance::key),
                Codec.INT.optionalFieldOf("min").forGetter(TriggerInstance::min)
        ).apply(instance, TriggerInstance::new));

        boolean matches(String event, @Nullable String key, int value) {
            return this.event.equals(event) && this.key.map(wanted -> wanted.equals(key)).orElse(true)
                    && this.min.map(least -> value >= least).orElse(true);
        }
    }
}
