package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.content.spell.HydroJetSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Riptide (Hydro Jet Lv 4): the next hit a player lands on a marked creature, from anything but
 * the jet itself, bursts the mark for bonus damage and splashes everything close by.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class WaterEvents {
    private static final float RIPTIDE_BONUS = 5f;
    private static final double SPLASH_RADIUS = 2.5;

    private WaterEvents() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level) || !target.hasEffect(ModContent.RIPTIDE)
                || !(event.getSource().getEntity() instanceof Player) || HydroJetSpell.isJetDamage()) {
            return;
        }
        target.removeEffect(ModContent.RIPTIDE);
        event.setAmount(event.getAmount() + RIPTIDE_BONUS);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(SPLASH_RADIUS),
                e -> e != target && !(e instanceof Player) && e.isAlive() && e.distanceTo(target) <= SPLASH_RADIUS)) {
            ElementalReactions.waterHit(nearby, 100);
        }
        for (int i = 0; i < 24; i++) {
            float angle = i * Mth.TWO_PI / 24;
            level.sendParticles(ModContent.HYDRO_DROP.get(), target.getX(), target.getY(0.5), target.getZ(),
                    0, Mth.cos(angle), 0.3, Mth.sin(angle), 0.45);
        }
        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.6), target.getZ(), 30, 0.5, 0.4, 0.5, 0.2);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1f, 1.2f);
    }
}
