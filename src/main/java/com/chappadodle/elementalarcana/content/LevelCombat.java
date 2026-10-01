package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.Stat;
import com.chappadodle.elementalarcana.api.StatRules;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Everyone plays by the same rules. Damage between two creatures (players included) is scaled by
 * their level gap, x1.045 per level either way (Progression#damageLevelFactor). Elemental damage
 * is softened by the target's Ward and, coming from a creature, raised by its Potency (a player's
 * Potency is already in their spell power). A hit that set off a reaction is raised by the
 * attacking player's Insight (see ReactionRewards).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class LevelCombat {

    private LevelCombat() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        float amount = event.getAmount();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living && living != target ? living : null;
        if (attacker != null) {
            amount *= Progression.damageLevelFactor(CreatureLevels.levelOf(attacker), CreatureLevels.levelOf(target));
        }
        if (SpellDamage.elementOf(source) != null) {
            int ward = target instanceof Player player ? MagicAttachments.get(player).stat(Stat.WARD) : CreatureLevels.creaturePoints(target);
            amount *= StatRules.wardFactor(ward);
            if (attacker != null && !(attacker instanceof Player)) {
                amount *= (float) StatRules.effect(CreatureLevels.creaturePoints(attacker), Stat.POTENCY.exponent());
            }
        }
        if (attacker instanceof Player player && ReactionRewards.reactedThisTick(target)) {
            amount *= StatRules.insightFactor(MagicAttachments.get(player).stat(Stat.INSIGHT));
        }
        event.setAmount(amount);
    }
}
