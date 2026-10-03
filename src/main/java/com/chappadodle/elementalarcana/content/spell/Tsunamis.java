package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.api.TsunamiRules;
import com.chappadodle.elementalarcana.content.TsunamiOptions;
import com.chappadodle.elementalarcana.content.WaterBurstOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Tsunami's waves (see TsunamiSpell and the Tsunami spec): a wall of water rolling from in front of
 * its caster along a path worked out when it rises (TsunamiRules: it climbs steps, pours down drops,
 * ends at walls), half a block a tick. Whatever its caster may hurt that the wave reaches takes
 * Water damage once and is carried along on its face; fires in its way go out; where the path ends
 * it crashes and throws what it carried. The sweeping is worked out here, on the server, never
 * saved; clients are sent one particle when it rises and draw it from the same path
 * (TsunamiOptions, TsunamiParticle).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Tsunamis {
    public static final double HALF_WIDTH = 2.5;
    public static final double HEIGHT = 2.6;
    /** How far in front of its caster the wave rises. */
    private static final double AHEAD = 0.8;
    /** How far a caster's feet may be above the ground for the wave to rise under them. */
    private static final int MAX_STAND = 4;
    /** How far behind its front the wave's face reaches. */
    private static final double DEPTH = 1.5;
    private static final float DAMAGE = 4f;
    private static final int WET_TICKS = 200;
    /** How high above its foot what it carries rides. */
    private static final double RIDE = 1.0;
    private static final double THROW = 0.6;
    private static final double SEEN = 96;

    private record Wave(ServerLevel level, LivingEntity caster, float power, Vec3 origin, Vec3 dir, int[] heights,
                        long startedAt, Set<Integer> swept) {
        Vec3 front(int step) {
            return origin.add(dir.scale(step * TsunamiRules.STEP));
        }
    }

    private static final List<Wave> ACTIVE = new ArrayList<>();

    private Tsunamis() {
    }

    /** Raises a wave in front of {@code caster}, rolling the way they face. False if it has no room to rise. */
    public static boolean start(LivingEntity caster, float power) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 look = caster.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1.0e-4) {
            // Looking straight up or down: the way they're turned.
            dir = Vec3.directionFromRotation(0, caster.getYRot());
        }
        dir = dir.normalize();
        Vec3 origin = caster.position().add(dir.scale(AHEAD));
        Integer foot = footUnder(level, BlockPos.containing(origin));
        if (foot == null) {
            return false;
        }
        int[] heights = TsunamiRules.path((x, y, z) -> holds(level, new BlockPos(x, y, z)), origin.x, foot, origin.z, dir.x, dir.z);
        if (!TsunamiRules.hasRoom(heights)) {
            return false;
        }
        Vec3 start = new Vec3(origin.x, foot, origin.z);
        ACTIVE.add(new Wave(level, caster, power, start, dir, heights, level.getGameTime(), new HashSet<>()));
        TsunamiOptions drawn = TsunamiOptions.of(dir, heights);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(start) < SEEN * SEEN) {
                level.sendParticles(player, drawn, true, start.x, start.y, start.z, 1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, start.x, start.y, start.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.2f, 0.6f);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1f, 0.5f);
        return true;
    }

    /** The foot height for a wave rising at {@code pos}: the first open block above ground, at most a few below. */
    @Nullable
    private static Integer footUnder(ServerLevel level, BlockPos pos) {
        if (holds(level, pos)) {
            return holds(level, pos.above()) ? null : pos.getY() + 1;
        }
        for (int down = 0; down <= MAX_STAND; down++) {
            if (holds(level, pos.below(down + 1))) {
                return pos.getY() - down;
            }
        }
        return null;
    }

    /** What holds the wave up or stops it: anything solid, and water and lava (it rides them). */
    private static boolean holds(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ACTIVE.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Wave> it = ACTIVE.iterator(); it.hasNext(); ) {
            Wave wave = it.next();
            if (wave.level() != level) {
                continue;
            }
            int step = (int) (now - wave.startedAt());
            int last = wave.heights().length - 1;
            if (step >= last) {
                crash(level, wave, last);
                it.remove();
            } else {
                roll(level, wave, step);
            }
        }
    }

    /** One tick of the wave: what's in its face is swept (hurt once) and carried; fires in its way go out. */
    private static void roll(ServerLevel level, Wave wave, int step) {
        Vec3 front = wave.front(step);
        int foot = wave.heights()[step];
        Vec3 dir = wave.dir();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        double reach = HALF_WIDTH + DEPTH + 1;
        AABB box = new AABB(front.x - reach, foot - 1, front.z - reach, front.x + reach, foot + HEIGHT + 0.5, front.z + reach);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, t -> SpellTargets.canAffect(wave.caster(), t))) {
            Vec3 offset = target.position().subtract(front);
            double along = offset.x * dir.x + offset.z * dir.z;
            double across = offset.x * side.x + offset.z * side.z;
            double half = target.getBbWidth() / 2;
            if (along > 0.6 + half || along < -DEPTH - half || Math.abs(across) > HALF_WIDTH + half) {
                continue;
            }
            if (wave.swept().add(target.getId())) {
                float damage = DAMAGE * wave.power() * ElementalReactions.waterHit(target, WET_TICKS);
                SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.WATER, wave.caster(), wave.caster()), damage);
                target.clearFire();
                level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.5), target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            }
            carry(target, dir, foot);
        }
        douse(level, front, side, foot);
        if (step % 5 == 0) {
            level.playSound(null, front.x, foot, front.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.9f,
                    0.6f + level.getRandom().nextFloat() * 0.2f);
        }
    }

    /** Carried on the wave's face: moved along with it and lifted to ride it, as much as knockback resistance lets it. */
    private static void carry(LivingEntity target, Vec3 dir, int foot) {
        double hold = 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (hold <= 0) {
            return;
        }
        Vec3 motion = target.getDeltaMovement();
        double speed = TsunamiRules.STEP * hold;
        double lift = target.getY() < foot + RIDE ? 0.18 * hold : 0.02;
        target.setDeltaMovement(dir.x * speed + motion.x * (1 - hold), Math.max(motion.y, lift), dir.z * speed + motion.z * (1 - hold));
        target.hurtMarked = true;
        target.resetFallDistance();
    }

    /** Puts out fires across the wave's front, up to its height. */
    private static void douse(ServerLevel level, Vec3 front, Vec3 side, int foot) {
        boolean any = false;
        for (int across = -2; across <= 2; across++) {
            Vec3 column = front.add(side.scale(across));
            for (int up = 0; up <= 2; up++) {
                BlockPos pos = BlockPos.containing(column.x, foot + up, column.z);
                if (level.getBlockState(pos).is(BlockTags.FIRE)) {
                    level.removeBlock(pos, false);
                    any = true;
                }
            }
        }
        if (any) {
            level.playSound(null, front.x, foot, front.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1f);
        }
    }

    /** The end of the way: the wave crashes down in a ring of water (with its sound) and throws what it carried. */
    private static void crash(ServerLevel level, Wave wave, int last) {
        Vec3 front = wave.front(last);
        Vec3 at = new Vec3(front.x, wave.heights()[last], front.z);
        WaterBurstOptions ring = new WaterBurstOptions(WaterBurstOptions.WAVE, 3.5f, 14);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) < SEEN * SEEN) {
                level.sendParticles(player, ring, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.5, at.z, 40, 1.5, 0.4, 1.5, 0.3);
        douse(level, front, new Vec3(-wave.dir().z, 0, wave.dir().x), wave.heights()[last]);
        for (int id : wave.swept()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity target && target.isAlive() && target.distanceToSqr(at) < 5 * 5) {
                double hold = 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                if (hold > 0) {
                    target.setDeltaMovement(wave.dir().scale(THROW * hold).add(0, 0.3 * hold, 0));
                    target.hurtMarked = true;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }
}
