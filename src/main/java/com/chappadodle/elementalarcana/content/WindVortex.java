package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * A short-lived whirlwind at a point: pulls nearby creatures (never players) toward its center
 * and draws itself as a rising spiral of wind. Server-side only; nothing is saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class WindVortex {
    private static final List<WindVortex> ACTIVE = new ArrayList<>();

    private final ServerLevel level;
    private final Vec3 center;
    private final double radius;
    private final double pull;
    @Nullable
    private final UUID owner;
    private int ticksLeft;
    private int age;

    private WindVortex(ServerLevel level, Vec3 center, double radius, double pull, int ticks, @Nullable UUID owner) {
        this.level = level;
        this.center = center;
        this.radius = radius;
        this.pull = pull;
        this.ticksLeft = ticks;
        this.owner = owner;
    }

    /** Starts a whirlwind at {@code center} pulling with {@code pull} blocks/tick toward the middle. */
    public static void spawn(ServerLevel level, Vec3 center, double radius, double pull, int ticks, @Nullable Entity owner) {
        ACTIVE.add(new WindVortex(level, center, radius, pull, ticks, owner == null ? null : owner.getUUID()));
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 1f, 1.2f);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<WindVortex> it = ACTIVE.iterator(); it.hasNext(); ) {
            WindVortex vortex = it.next();
            vortex.tick();
            if (--vortex.ticksLeft <= 0) {
                it.remove();
            }
        }
    }

    private void tick() {
        age++;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> !(e instanceof Player) && !e.getUUID().equals(owner) && e.isAlive() && e.position().distanceTo(center) <= radius)) {
            Vec3 toward = center.subtract(entity.position());
            Vec3 horizontal = new Vec3(toward.x, 0, toward.z);
            if (horizontal.lengthSqr() > 0.04) {
                Vec3 step = horizontal.normalize().scale(pull);
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.6, 1, 0.6).add(step.x, 0, step.z));
                entity.hurtMarked = true;
            }
        }
        for (int i = 0; i < 3; i++) {
            float angle = age * 0.6f + i * Mth.TWO_PI / 3;
            double height = (age % 20) / 20.0 * 1.8;
            double ring = radius * 0.35 * (0.6 + height / 1.8);
            level.sendParticles(ModContent.WIND_STREAK.get(), center.x + Mth.cos(angle) * ring, center.y + height,
                    center.z + Mth.sin(angle) * ring, 0, -Mth.sin(angle), 0.3, Mth.cos(angle), 0.25);
        }
    }
}
