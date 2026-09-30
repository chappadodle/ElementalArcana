package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A swirling whirlpool at a point (Hydro Jet's Maelstrom, a Water Archmage's spell) that drags
 * creatures (whoever its caster's magic may hurt, see SpellTargets) around and into its center,
 * soaking and hurting them. Server-side; players see it as spiral arms of little water cubes
 * flowing into it (WaterBurstParticle).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Whirlpool {
    private static final List<Whirlpool> ACTIVE = new ArrayList<>();
    private static final double RADIUS = 5.0;

    private final ServerLevel level;
    private final Vec3 center;
    private final float damage;
    @Nullable
    private final Entity owner;
    private int ticksLeft;
    private int age;

    private Whirlpool(ServerLevel level, Vec3 center, float damage, int ticks, @Nullable Entity owner) {
        this.level = level;
        this.center = center;
        this.damage = damage;
        this.ticksLeft = ticks;
        this.owner = owner;
    }

    public static void spawn(ServerLevel level, Vec3 center, float damage, int ticks, @Nullable Entity owner) {
        ACTIVE.add(new Whirlpool(level, center, damage, ticks, owner));
        WaterBurstOptions burst = new WaterBurstOptions(WaterBurstOptions.WHIRLPOOL, (float) RADIUS, ticks);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) < 48 * 48) {
                level.sendParticles(player, burst, true, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.2, center.z, 40, 1.5, 0.2, 1.5, 0.2);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<Whirlpool> it = ACTIVE.iterator(); it.hasNext(); ) {
            Whirlpool pool = it.next();
            pool.tick();
            if (--pool.ticksLeft <= 0) {
                pool.collapse();
                it.remove();
            }
        }
    }

    private void tick() {
        age++;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(RADIUS, 2.5, RADIUS),
                e -> SpellTargets.canAffect(owner, e)
                        && Math.hypot(e.getX() - center.x, e.getZ() - center.z) <= RADIUS)) {
            Vec3 toward = new Vec3(center.x - entity.getX(), 0, center.z - entity.getZ());
            double distance = toward.length();
            if (distance > 0.3) {
                // Inward and around: the spiral of a drain.
                Vec3 in = toward.scale(1 / distance);
                Vec3 around = new Vec3(-in.z, 0, in.x);
                Vec3 pull = in.scale(0.12).add(around.scale(0.18));
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5, 1, 0.5).add(pull));
                entity.hurtMarked = true;
            }
            if (age % 10 == 0) {
                ElementalReactions.waterHit(entity, 100);
                Entity source = owner != null ? owner : entity;
                SpellDamage.hurtMultiHit(entity, SpellDamage.source(level, Element.WATER, source, source), damage);
            }
        }
        if (age % 3 == 0) {
            level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.1, center.z, 6, RADIUS * 0.4, 0.05, RADIUS * 0.4, 0.1);
            level.sendParticles(ParticleTypes.BUBBLE_POP, center.x, center.y + 0.2, center.z, 2, 0.6, 0.1, 0.6, 0.02);
        }
    }

    private void collapse() {
        level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.3, center.z, 60, 1.2, 0.3, 1.2, 0.3);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1f, 0.8f);
    }
}
