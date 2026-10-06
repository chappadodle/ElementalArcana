package com.chappadodle.elementalarcana.content.flora;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.KinshipRules;
import com.chappadodle.elementalarcana.api.SchoolElements;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Kinship with an element, from a tonic (see the Arcane Flora spec): the element's spells stronger
 * when cast (CastingService asks), and its magic softer when it lands.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Kinships {
    private Kinships() {
    }

    /** The multiplier Kinship puts on a spell's power: 1 unless the caster is kin to its element. */
    public static float powerFactor(LivingEntity caster, Spell spell) {
        Element element = SchoolElements.of(spell.school());
        if (element == null) {
            return 1f;
        }
        MobEffectInstance kinship = caster.getEffect(ModFlora.kinship(element));
        return kinship == null ? 1f : KinshipRules.powerFactor(kinship.getAmplifier());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Element element = SpellDamage.elementOf(event.getSource());
        if (element == null || event.getEntity().level().isClientSide()) {
            return;
        }
        MobEffectInstance kinship = event.getEntity().getEffect(ModFlora.kinship(element));
        if (kinship != null) {
            event.setAmount(event.getAmount() * KinshipRules.wardFactor(kinship.getAmplifier()));
        }
    }
}
