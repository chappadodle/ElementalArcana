package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.DawnbreakOptions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Dawnbreak's suns (see DawnbreakSpell): a small sun rising over the place it was called, shining
 * there for 12 seconds. Once a second, everything within 10 blocks of that place is in its light:
 * the undead its caster may hurt catch fire and take 2.5 times their power, every other foe takes 1
 * times it and glows (shown through walls); the caster and their allies are cleared of Darkness and
 * Blindness. One per caster. Worked out on the server and never saved; each client draws the sun
 * itself from one particle sent when it's called (DawnbreakOptions).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Dawnbreaks {
    public static final int DURATION_TICKS = 240;
    /** How high over the place it was called the sun shines, and how long it takes to rise there. */
    public static final double HEIGHT = 6;
    public static final int RISE_TICKS = 30;
    private static final double RADIUS = 10;
    private static final float UNDEAD_DAMAGE = 2.5f;
    private static final float DAMAGE = 1f;
    private static final double SEEN = 128;

    private record Dawn(ServerLevel level, UUID caster, Vec3 at, float power, long startedAt) {
    }

    private static final List<Dawn> ACTIVE = new ArrayList<>();

    private Dawnbreaks() {
    }

    public static void start(ServerLevel level, LivingEntity caster, float power) {
        ACTIVE.removeIf(dawn -> dawn.caster().equals(caster.getUUID()));
        Vec3 at = caster.position();
        ACTIVE.add(new Dawn(level, caster.getUUID(), at, power, level.getGameTime()));
        DawnbreakOptions drawn = new DawnbreakOptions(DURATION_TICKS);
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(at) < SEEN * SEEN) {
                level.sendParticles(player, drawn, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.5f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.7f);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ACTIVE.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Dawn> it = ACTIVE.iterator(); it.hasNext(); ) {
            Dawn dawn = it.next();
            if (dawn.level() != level) {
                continue;
            }
            Entity entity = level.getEntity(dawn.caster());
            long age = now - dawn.startedAt();
            if (!(entity instanceof LivingEntity caster) || !caster.isAlive() || age >= DURATION_TICKS) {
                it.remove();
                level.playSound(null, dawn.at().x, dawn.at().y + HEIGHT, dawn.at().z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
                continue;
            }
            if (age % 20 == 10) {
                shine(level, caster, dawn);
            }
            if (age % 40 == 20) {
                level.playSound(null, dawn.at().x, dawn.at().y + HEIGHT, dawn.at().z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.9f, 1.6f);
            }
        }
    }

    private static void shine(ServerLevel level, LivingEntity caster, Dawn dawn) {
        Vec3 at = dawn.at();
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(RADIUS, RADIUS, RADIUS),
                other -> other.isAlive() && other.position().distanceTo(at) <= RADIUS)) {
            if (other == caster || !SpellTargets.canAffect(caster, other)) {
                other.removeEffect(MobEffects.DARKNESS);
                other.removeEffect(MobEffects.BLINDNESS);
                continue;
            }
            boolean undead = other.getType().is(EntityTypeTags.UNDEAD);
            if (undead && !other.fireImmune()) {
                other.igniteForSeconds(3);
            }
            SpellDamage.hurtMultiHit(other, SpellDamage.source(level, Element.RADIANCE, caster, caster),
                    (undead ? UNDEAD_DAMAGE : DAMAGE) * dawn.power());
            other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
            level.sendParticles(ParticleTypes.END_ROD, other.getX(), other.getY(1.0), other.getZ(), 3, 0.2, 0.3, 0.2, 0.02);
        }
    }

    /** Whether {@code pos} is in a Dawnbreak's light (on the server; Sunborn counts it as daylight). */
    public static boolean shines(Level level, BlockPos pos) {
        for (Dawn dawn : ACTIVE) {
            if (dawn.level() == level && dawn.at().distanceTo(Vec3.atCenterOf(pos)) <= RADIUS) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }
}
