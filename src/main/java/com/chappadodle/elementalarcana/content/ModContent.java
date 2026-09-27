package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The shared spell projectile, custom sounds and the Mana Sickness effect. */
public final class ModContent {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ElementalArcana.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ElementalArcana.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SpellProjectile>> SPELL_PROJECTILE =
            ENTITY_TYPES.register("spell_projectile", () -> EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ElementalArcana.MODID + ":spell_projectile"));

    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKEN_SOUND = sound("magic.awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVEL_UP_SOUND = sound("magic.level_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIZZLE_SOUND = sound("spell.fizzle");

    /** Moderate exhaustion from emptying your mana: slower, weaker, and slower mana regen. */
    public static final DeferredHolder<MobEffect, MobEffect> MANA_SICKNESS = EFFECTS.register("mana_sickness",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x7A3FA0) {
            }
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, ElementalArcana.id("mana_sickness_speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, ElementalArcana.id("mana_sickness_damage"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private ModContent() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ElementalArcana.id(name)));
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        SOUNDS.register(modEventBus);
        EFFECTS.register(modEventBus);
    }
}
