package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * A short-lived patch of burning ground: sets creatures that stand in it alight (never players,
 * never its caster) and flickers with flames. It doesn't place real fire blocks. Server-side only.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class FireField {
    private static final List<FireField> ACTIVE = new ArrayList<>();

    private final ServerLevel level;
    private final Vec3 center;
    private final double radius;
    @Nullable
    private final UUID owner;
    private int ticksLeft;

    private FireField(ServerLevel level, Vec3 center, double radius, int ticks, @Nullable UUID owner) {
        this.level = level;
        this.center = center;
        this.radius = radius;
        this.ticksLeft = ticks;
        this.owner = owner;
    }

    public static void spawn(ServerLevel level, Vec3 center, double radius, int ticks, @Nullable Entity owner) {
        ACTIVE.add(new FireField(level, center, radius, ticks, owner == null ? null : owner.getUUID()));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<FireField> it = ACTIVE.iterator(); it.hasNext(); ) {
            FireField field = it.next();
            field.tick();
            if (--field.ticksLeft <= 0) {
                it.remove();
            }
        }
    }

    private void tick() {
        if (ticksLeft % 10 == 0) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 1.5, radius),
                    e -> !(e instanceof Player) && !e.getUUID().equals(owner) && e.isAlive()
                            && Math.hypot(e.getX() - center.x, e.getZ() - center.z) <= radius)) {
                entity.igniteForTicks(60);
            }
        }
        for (int i = 0; i < 3; i++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(level.getRandom().nextDouble()) * radius;
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            level.sendParticles(ParticleTypes.FLAME, x, center.y + 0.1, z, 1, 0, 0.05, 0, 0.01);
            if (i == 0 && ticksLeft % 3 == 0) {
                level.sendParticles(ModContent.EMBER.get(), x, center.y + 0.2, z, 1, 0, 0.05, 0, 0.02);
            }
        }
    }
}
