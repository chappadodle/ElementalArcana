package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.spell.FireballSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Combustion (Fireball Lv 7): a creature marked by a Combustion fireball that dies while burning
 * explodes, setting everything around it alight and marking them too, so it can chain.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class FireEvents {
    public static final String TAG_COMBUST_UNTIL = "ea_combust_until";
    private static final double RADIUS = 2.5;
    private static final float DAMAGE = 4f;
    private static final long MARK_TICKS = 100;

    private FireEvents() {
    }

    /** Marks {@code target}: if it dies while burning in the next few seconds, it explodes. */
    public static void markForCombustion(LivingEntity target) {
        target.getPersistentData().putLong(TAG_COMBUST_UNTIL, target.level().getGameTime() + MARK_TICKS);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || dead instanceof Player || !dead.isOnFire()
                || level.getGameTime() > dead.getPersistentData().getLong(TAG_COMBUST_UNTIL)) {
            return;
        }
        dead.getPersistentData().remove(TAG_COMBUST_UNTIL);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, dead.getBoundingBox().inflate(RADIUS),
                e -> e != dead && !(e instanceof Player) && e.isAlive() && e.distanceTo(dead) <= RADIUS)) {
            markForCombustion(nearby);
            nearby.igniteForTicks(100);
            SpellDamage.hurtMultiHit(nearby, SpellDamage.source(level, Element.FIRE, dead, dead), DAMAGE);
        }
        level.sendParticles(new FireBlastOptions(FireballSpell.LOOK_HEAT_2, 2f, false), dead.getX(), dead.getY(0.5), dead.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, dead.getX(), dead.getY(), dead.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.5f);
    }
}
