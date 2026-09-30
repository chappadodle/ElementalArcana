package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellHold;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.sound.HydroJetSounds;
import com.chappadodle.elementalarcana.content.HydroStreamOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.WaterBurstOptions;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.content.Whirlpool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Water's basic spell, levels 1-10. Hold the cast key to spray a pressurized stream from your
 * hand at whatever you aim at: rapid small hits that push enemies back and soak them (Wet). The
 * stream stops at the first creature (unless a branch says otherwise) or block.
 *
 * <pre>
 * Lv1 Hydro Jet       2s, 12 blocks           Lv6  Pressure Build  +10% per 0.5s held, max +50%
 * Lv2 Surging Stream  3s, 16 blocks           Lv7  Lifestream      heals players it touches
 * Lv3 Splash          the landing point       Lv8  Recoil          aim down to ride your jet
 *                     splashes 1.5 blocks     Lv9  Drench          Wet lasts 10s and slows
 * Lv4 Riptide         1s soaked = marked;     Lv10 capstone:       Maelstrom | Tsunami Lance
 *                     next hit bursts
 * Lv5 branch:         Tidecutter | Torrent
 * </pre>
 */
public class HydroJetSpell extends Spell implements ProjectileSpell {
    public static final String TIDECUTTER = "tidecutter";
    public static final String TORRENT = "torrent";
    public static final String MAELSTROM = "maelstrom";
    public static final String LANCE = "lance";


    // Looks of the stream (HydroStreamOptions#look): the pressure rises as it levels, and each
    // branch has its own look. Drawn as a 3D water beam by client/visual/WaterBeams
    // (see docs/superpowers/specs/2026-09-30-hydro-jet-vfx-design.md).
    public static final int LOOK_SPRING = 0;
    public static final int LOOK_CURRENT = 1;
    public static final int LOOK_SURGE = 2;
    public static final int LOOK_DELUGE = 3;
    public static final int LOOK_TIDECUTTER = 4;
    public static final int LOOK_TORRENT = 5;
    public static final int LOOK_MAELSTROM = 6;
    public static final int LOOK_LANCE = 7;
    /** Added to the look at Lv 8+: the core shines brighter. */
    public static final int BRIGHT = 8;
    private static final int HIT_INTERVAL = 4;
    private static final float HIT_DAMAGE = 1f;
    private static final int RIPTIDE_HITS = 5;
    private static final int MAELSTROM_MIN_TICKS = 40;
    private static final int LANCE_MIN_TICKS = 10;

    private static final String TAG_LANCE_DAMAGE = "ea_lance_damage";
    private static final String TAG_LEVEL = "ea_jet_level";

    // True while the jet deals its own damage, so that damage doesn't set off a Riptide mark.
    private static boolean dealingJetDamage;

    public HydroJetSpell() {
        super(ModSchools.WATER, 20, 80);
    }

    // ---- levels ----

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(TIDECUTTER, TORRENT);
            case 10 -> List.of(MAELSTROM, LANCE);
            default -> List.of();
        };
    }

    private static int sprayTicks(int level) {
        return level >= 2 ? 60 : 40;
    }

    private static double range(int level) {
        return level >= 2 ? 16 : 12;
    }

    private static int wetTicks(int level) {
        return level >= 9 ? 200 : 100;
    }

    /** Whether the damage being dealt right now is the jet's own (see WaterEvents). */
    public static boolean isJetDamage() {
        return dealingJetDamage;
    }

    /** Where the stream leaves the caster: just in front of their right hand. Used on both sides. */
    /** The look without the Lv 8+ brightness. */
    public static int look(int variant) {
        return variant & (BRIGHT - 1);
    }

    /** A caster's stream: its capstone, then its Lv 5 branch, then how hard it sprays; bright at Lv 8+. */
    private static int lookFor(int level, @Nullable String branch5, @Nullable String branch10) {
        int look;
        if (MAELSTROM.equals(branch10)) {
            look = LOOK_MAELSTROM;
        } else if (LANCE.equals(branch10)) {
            look = LOOK_LANCE;
        } else if (TIDECUTTER.equals(branch5)) {
            look = LOOK_TIDECUTTER;
        } else if (TORRENT.equals(branch5)) {
            look = LOOK_TORRENT;
        } else {
            look = level >= 8 ? LOOK_DELUGE : level >= 5 ? LOOK_SURGE : level >= 3 ? LOOK_CURRENT : LOOK_SPRING;
        }
        return look | (level >= 8 ? BRIGHT : 0);
    }

    public static Vec3 streamOrigin(Player player) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        return player.getEyePosition().add(player.getLookAngle().scale(0.5)).add(right.scale(0.3)).add(0, -0.2, 0);
    }

    // ---- casting ----

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        context.holdUntilRelease(new Jet(context));
        return CastResult.SUCCESS;
    }

    private static void playAt(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** A creature the stream passes through, and where. */
    private record LineHit(LivingEntity entity, Vec3 at) {
    }

    private final class Jet implements SpellHold {
        private final ServerPlayer caster;
        private final ServerLevel level;
        private final float power;
        private final int spellLevel;
        private final boolean tidecutter;
        private final boolean torrent;
        @Nullable
        private final String capstone;
        private final int look;
        // Riptide: per creature, {consecutive hits, tick of the last hit}.
        private final Map<Integer, int[]> soaking = new HashMap<>();
        private Vec3 lastImpact;

        Jet(CastContext context) {
            this.caster = context.caster();
            this.level = context.level();
            this.power = context.power();
            this.spellLevel = context.spellLevel();
            this.tidecutter = context.hasBranch(5, TIDECUTTER);
            this.torrent = context.hasBranch(5, TORRENT);
            this.capstone = context.branch(10);
            this.look = lookFor(spellLevel, context.branch(5), context.branch(10));
            this.lastImpact = caster.position();
        }

        @Override
        public int maxHoldTicks() {
            return sprayTicks(spellLevel);
        }

        @Override
        public boolean tick(int heldTicks) {
            Vec3 look = caster.getLookAngle();
            Vec3 origin = streamOrigin(caster);
            Vec3 end = origin.add(look.scale(range(spellLevel)));
            BlockHitResult blockHit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
            Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

            double width = torrent ? 1.2 : tidecutter ? 0.1 : 0.3;
            List<LineHit> onLine = creaturesOnLine(origin, stop, width);
            boolean stoppedByCreature = false;
            if (!tidecutter && !torrent && !onLine.isEmpty()) {
                // A plain stream stops at the first creature it meets.
                onLine = onLine.subList(0, 1);
                stop = onLine.get(0).at();
                stoppedByCreature = true;
            }
            lastImpact = stop;

            // Pressure Build shows too: the beam thickens and brightens as it builds.
            float pressure = spellLevel >= 6 ? Math.min(1f, heldTicks / 50f) : 0f;
            HydroStreamOptions stream = new HydroStreamOptions(caster.getId(), this.look, pressure);
            Vec3 line = stop.subtract(origin);
            level.sendParticles(stream, origin.x, origin.y, origin.z, 0, line.x, line.y, line.z, 1.0);

            if (heldTicks % HIT_INTERVAL == 0) {
                List<LivingEntity> hit = new ArrayList<>();
                for (LineHit lineHit : onLine) {
                    hitCreature(lineHit.entity(), heldTicks, look);
                    hit.add(lineHit.entity());
                }
                if (spellLevel >= 3) {
                    splash(stop, hit);
                }
                if (!stoppedByCreature && blockHit.getType() == HitResult.Type.BLOCK) {
                    douse(blockHit);
                }
            }
            if (spellLevel >= 8) {
                recoil(look);
            }
            return true;
        }

        private List<LineHit> creaturesOnLine(Vec3 from, Vec3 to, double width) {
            List<LineHit> hits = new ArrayList<>();
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(width + 0.5),
                    e -> e != caster && e.isAlive() && !e.isSpectator())) {
                Optional<Vec3> at = entity.getBoundingBox().inflate(width).clip(from, to);
                at.ifPresent(point -> hits.add(new LineHit(entity, point)));
            }
            hits.sort(Comparator.comparingDouble(hit -> hit.at().distanceToSqr(from)));
            return hits;
        }

        private void hitCreature(LivingEntity target, int heldTicks, Vec3 look) {
            if (target instanceof Player player && spellLevel >= 7) {
                // Lifestream: allies are healed and cooled off instead of hurt.
                player.heal(1f);
                player.clearFire();
                level.sendParticles(heldTicks % 12 == 0 ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER,
                        player.getX(), player.getY(1.0) + 0.3, player.getZ(), 1, 0.3, 0.1, 0.3, 0);
                return;
            }
            float damage = HIT_DAMAGE * power;
            if (spellLevel >= 6) {
                // Pressure Build: +10% for every half second of spraying, up to +50%.
                damage *= 1f + Math.min(0.5f, 0.1f * (heldTicks / 10));
            }
            if (tidecutter) {
                damage *= 1.6f;
            }
            // Water-sensitive creatures without an element (endermen) take double. Elemental ones
            // (blazes) are handled by the matchup chart instead.
            if (target.isSensitiveToWater() && CreatureElements.elementOf(target) == null) {
                damage *= 2f;
            }
            damage *= ElementalReactions.waterHit(target, wetTicks(spellLevel));

            // The stream decides the knockback, not the damage: Tidecutter slices without pushing.
            Vec3 motion = target.getDeltaMovement();
            dealingJetDamage = true;
            try {
                SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.WATER, caster, caster), damage);
            } finally {
                dealingJetDamage = false;
            }
            if (!tidecutter) {
                double push = torrent ? 0.35 : 0.12;
                motion = motion.add(look.x * push, torrent ? 0.08 : 0.02, look.z * push);
            }
            target.setDeltaMovement(motion);
            target.hurtMarked = true;

            if (spellLevel >= 9) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
            if (spellLevel >= 4) {
                markRiptide(target, heldTicks);
            }
        }

        /** Riptide: a creature soaked for a full second straight gets marked. */
        private void markRiptide(LivingEntity target, int heldTicks) {
            int[] soak = soaking.computeIfAbsent(target.getId(), id -> new int[] {0, -100});
            soak[0] = heldTicks - soak[1] <= HIT_INTERVAL + 2 ? soak[0] + 1 : 1;
            soak[1] = heldTicks;
            if (soak[0] >= RIPTIDE_HITS && target.isAlive() && !target.hasEffect(ModContent.RIPTIDE)) {
                soak[0] = 0;
                target.addEffect(new MobEffectInstance(ModContent.RIPTIDE, 200));
                playAt(level, target.position(), SoundEvents.TRIDENT_RETURN, 1f, 1.3f);
            }
        }

        /** Splash (Lv 3): where the stream lands, the spray soaks and hits everything close by. */
        private void splash(Vec3 at, List<LivingEntity> alreadyHit) {
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.5),
                    e -> e != caster && !(e instanceof Player) && e.isAlive() && !alreadyHit.contains(e)
                            && e.getBoundingBox().getCenter().distanceTo(at) <= 1.5 + e.getBbWidth() / 2)) {
                float damage = HIT_DAMAGE * power * 0.5f * ElementalReactions.waterHit(nearby, wetTicks(spellLevel));
                dealingJetDamage = true;
                try {
                    SpellDamage.hurtMultiHit(nearby, SpellDamage.source(level, Element.WATER, caster, caster), damage);
                } finally {
                    dealingJetDamage = false;
                }
            }
            level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 12, 0.6, 0.2, 0.6, 0.15);
        }

        /** The stream puts out fires where it lands; Torrent also turns lava into stone. */
        private void douse(BlockHitResult hit) {
            boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            boolean hissed = false;
            BlockPos center = hit.getBlockPos();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                if (level.getBlockState(pos).is(BlockTags.FIRE)) {
                    level.removeBlock(pos, false);
                    hissed = true;
                } else if (torrent && griefing) {
                    FluidState fluid = level.getFluidState(pos);
                    if (fluid.is(FluidTags.LAVA)) {
                        level.setBlockAndUpdate(pos, fluid.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.02);
                        hissed = true;
                    }
                }
            }
            if (hissed) {
                playAt(level, hit.getLocation(), SoundEvents.LAVA_EXTINGUISH, 0.7f, 1.2f);
            }
        }

        /** Recoil (Lv 8): aim down and the jet lifts you; otherwise you drift down gently. */
        private void recoil(Vec3 look) {
            caster.fallDistance = 0;
            if (look.y < -0.6) {
                caster.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 3, 1, false, false, false));
            } else if (!caster.onGround()) {
                caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 3, 0, false, false, false));
            }
        }

        @Override
        public void release(int heldTicks) {
            if (spellLevel >= 8 && !caster.onGround()) {
                // Let go mid-air: float down instead of dropping.
                caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
            }
            if (MAELSTROM.equals(capstone) && heldTicks >= MAELSTROM_MIN_TICKS) {
                Whirlpool.spawn(level, groundBelow(lastImpact), HIT_DAMAGE * 1.5f * power, 80, caster);
            } else if (LANCE.equals(capstone) && heldTicks >= LANCE_MIN_TICKS) {
                fireLance(heldTicks);
            }
        }

        /** Tsunami Lance: the stream compresses into a spear that pierces everything; bigger the longer you sprayed. */
        private void fireLance(int heldTicks) {
            float fill = Math.min(1f, heldTicks / (float) sprayTicks(spellLevel));
            SpellProjectile lance = SpellProjectile.shootFrom(caster, HydroJetSpell.this, streamOrigin(caster), caster.getLookAngle().scale(2.4), power);
            lance.setPierce(64);
            lance.setVisualScale(1f + fill);
            CompoundTag tag = lance.getPersistentData();
            tag.putFloat(TAG_LANCE_DAMAGE, (4f + 10f * fill) * power);
            tag.putInt(TAG_LEVEL, spellLevel);
        }

        /** The floor under a point (up to 4 blocks down), so a whirlpool sits on the ground. */
        private Vec3 groundBelow(Vec3 at) {
            BlockHitResult floor = level.clip(new ClipContext(at.add(0, 0.5, 0), at.subtract(0, 4, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
            return floor.getType() == HitResult.Type.MISS ? at : floor.getLocation();
        }

        @Override
        public void cancel() {
        }
    }

    // ---- Tsunami Lance (the projectile) ----

    @Override
    public ParticleOptions trailParticle() {
        return ModContent.HYDRO_DROP.get();
    }

    /** With a dynamic lights mod: the Tsunami Lance spear shines faintly (the stream is no entity, so it can't). */
    @Override
    public int luminance(SpellProjectile lance) {
        return 5;
    }

    @Override
    public int lifetimeTicks() {
        return 30;
    }

    @Override
    public void flightParticles(SpellProjectile lance) {
        Vec3 back = lance.getDeltaMovement().scale(-0.08);
        for (int i = 0; i < 4; i++) {
            lance.spawnParticleAround(ModContent.HYDRO_DROP.get(), 0.2 * lance.visualScale(), back);
        }
        lance.spawnParticleAround(ParticleTypes.SPLASH, 0.3, Vec3.ZERO);
        HydroJetSounds.lanceFlight(lance);
    }

    @Override
    public void onHitEntity(SpellProjectile lance, EntityHitResult hit) {
        if (!(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        CompoundTag tag = lance.getPersistentData();
        float damage = tag.getFloat(TAG_LANCE_DAMAGE) * ElementalReactions.waterHit(target, wetTicks(tag.getInt(TAG_LEVEL)));
        Entity owner = lance.getOwner();
        SpellDamage.hurtMultiHit(target, SpellDamage.source(lance.level(), Element.WATER, lance, owner), damage);
        Vec3 push = lance.getDeltaMovement().normalize().scale(0.6);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.2, push.z));
        target.hurtMarked = true;
        ServerLevel level = (ServerLevel) lance.level();
        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.5), target.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
        playAt(level, target.position(), SoundEvents.GENERIC_SPLASH, 0.8f, 1.2f);
    }

    @Override
    public void onHitBlock(SpellProjectile lance, BlockHitResult hit) {
        ServerLevel level = (ServerLevel) lance.level();
        Vec3 at = hit.getLocation();
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 30, 0.8, 0.3, 0.8, 0.3);
        // A wave ring of water crashing outward (WaterBurstParticle).
        WaterBurstOptions wave = new WaterBurstOptions(WaterBurstOptions.WAVE, 3.5f, 14);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) < 48 * 48) {
                level.sendParticles(player, wave, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
    }
}
