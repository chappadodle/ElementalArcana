package com.chappadodle.elementalarcana.content.cantrip;

import com.chappadodle.elementalarcana.api.CantripRules;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Mend (a cantrip): the worn item in your hand (your main hand first, else the other) gets back a
 * fifth of its durability, at least 25. Nothing worn in your hands: the cast is refunded.
 */
public class MendSpell extends Spell {
    public MendSpell() {
        super(ModSchools.ARCANE, 25, 60);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer player = context.caster();
        ItemStack stack = worn(player.getItemInHand(InteractionHand.MAIN_HAND)) ? player.getItemInHand(InteractionHand.MAIN_HAND)
                : player.getItemInHand(InteractionHand.OFF_HAND);
        if (!worn(stack)) {
            return CastResult.fail(Component.translatable("message.elementalarcana.cantrip.mend_nothing"));
        }
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - CantripRules.mendAmount(stack.getMaxDamage())));
        context.level().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY(1.0), player.getZ(), 30, 0.4, 0.4, 0.4, 0.5);
        context.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(0.7), player.getZ(), 6, 0.3, 0.2, 0.3, 0);
        context.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.35f, 1.6f);
        context.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.1f);
        return CastResult.SUCCESS;
    }

    private static boolean worn(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged();
    }
}
