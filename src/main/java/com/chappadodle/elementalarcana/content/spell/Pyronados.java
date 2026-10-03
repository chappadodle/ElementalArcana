package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.PyronadoOptions;
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
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pyronado's wheels (see PyronadoSpell): three wheels of flame 3 blocks out, a third of a turn apart,
 * orbiting their caster for 8 seconds, each spinning in its own upright ring. Anything the caster
 * may hurt that a wheel sweeps through takes 3 damage times their power, burns for 3 seconds and is
 * thrown back (at most twice a second). The hits are worked out here, on the server, never saved;
 * clients are sent one particle when the wheels are called and draw them from the same geometry
 * (PyronadoOptions, PyronadoEmitter).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Pyronados {
    public static final int WHEELS = 3;
    /** How fast each wheel spins on itself, in radians a tick. */
    public static final double ROLL = 0.5;
    private static final double ORBIT = 3.0;
    /** How fast the wheels orbit, in radians a tick (a turn in about two seconds). */
    private static final double SPIN = 0.16;
    private static final double HEIGHT = 1.0;
    private static final double WHEEL = 0.8;
    private static final double REACH = 1.3;
    private static final int DURATION_TICKS = 160;
    /** How far away players are sent the wheels to draw. */
    private static final double SEEN = 64;
    private static final float DAMAGE = 3f;
    private static final int HIT_GAP_TICKS = 10;

    private record Wheels(ServerLevel level, UUID caster, float power, long startedAt, long endsAt, Map<Integer, Long> lastHit) {
    }

    private static final List<Wheels> ACTIVE = new ArrayList<>();

    private Pyronados() {
    }

    public static void start(LivingEntity caster, float power) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        ACTIVE.removeIf(wheels -> wheels.caster().equals(caster.getUUID()));
        long now = level.getGameTime();
        ACTIVE.add(new Wheels(level, caster.getUUID(), power, now, now + DURATION_TICKS, new HashMap<>()));
        PyronadoOptions drawn = new PyronadoOptions(caster.getId(), DURATION_TICKS);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(caster) < SEEN * SEEN) {
                level.sendParticles(player, drawn, true, caster.getX(), caster.getY(), caster.getZ(), 1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.6f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 0.8f);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ACTIVE.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Wheels> it = ACTIVE.iterator(); it.hasNext(); ) {
            Wheels wheels = it.next();
            if (wheels.level() != level) {
                continue;
            }
            Entity entity = level.getEntity(wheels.caster());
            if (!(entity instanceof LivingEntity caster) || !caster.isAlive() || now >= wheels.endsAt()) {
                it.remove();
                if (entity != null) {
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
                }
                continue;
            }
            spin(level, caster, wheels, now);
        }
    }

    /** Where wheel {@code wheel}'s hub is, from its caster's feet, {@code ticks} after the wheels were called. */
    public static Vec3 wheelCenter(int wheel, double ticks) {
        double angle = orbitAngle(wheel, ticks);
        return new Vec3(Math.cos(angle) * ORBIT, HEIGHT, Math.sin(angle) * ORBIT);
    }

    /**
     * A point of wheel {@code wheel}, from its caster's feet: {@code turn} radians round from the top
     * and {@code out} of the way from the hub to the rim (1 is on the rim). Each wheel stands upright
     * along its path, so it rolls the way it goes.
     */
    public static Vec3 rimPoint(int wheel, double ticks, double turn, double out) {
        double angle = orbitAngle(wheel, ticks);
        Vec3 along = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
        double radius = WHEEL * out;
        return wheelCenter(wheel, ticks).add(along.scale(Math.sin(turn) * radius)).add(0, Math.cos(turn) * radius, 0);
    }

    private static double orbitAngle(int wheel, double ticks) {
        return ticks * SPIN + Math.PI * 2 * wheel / WHEELS;
    }

    private static void spin(ServerLevel level, LivingEntity caster, Wheels wheels, long now) {
        for (int k = 0; k < WHEELS; k++) {
            sweep(level, caster, wheels, caster.position().add(wheelCenter(k, now - wheels.startedAt())), now);
        }
        if (now % 20 == 0) {
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
    }

    private static void sweep(ServerLevel level, LivingEntity caster, Wheels wheels, Vec3 center, long now) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(REACH),
                target -> target != caster && target.isAlive() && SpellTargets.canAffect(caster, target)
                        && target.getBoundingBox().getCenter().distanceTo(center) <= REACH + target.getBbWidth() / 2)) {
            Long last = wheels.lastHit().get(target.getId());
            if (last != null && now - last < HIT_GAP_TICKS) {
                continue;
            }
            wheels.lastHit().put(target.getId(), now);
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.FIRE, caster, caster), DAMAGE * wheels.power());
            target.igniteForSeconds(3);
            Vec3 away = target.position().subtract(caster.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(away.scale(0.7)).add(0, 0.3, 0));
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY(0.5), target.getZ(), 3, 0.2, 0.2, 0.2, 0);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }
}
