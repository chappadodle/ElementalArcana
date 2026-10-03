package com.chappadodle.elementalarcana.content.sanctum;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SovereignRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.BubblePrisons;
import com.chappadodle.elementalarcana.content.FireField;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.creature.WispEntity;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import com.chappadodle.elementalarcana.content.mob.MobCasting;
import com.chappadodle.elementalarcana.content.mob.MobJet;
import com.chappadodle.elementalarcana.content.mob.MobSpell;
import com.chappadodle.elementalarcana.content.mob.MobSpells;
import com.chappadodle.elementalarcana.content.mob.StoneWards;
import com.chappadodle.elementalarcana.content.spell.ChainLightningSpell;
import com.chappadodle.elementalarcana.content.spell.WindBladeSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Sovereigns' own spells (see the sanctums spec), two each: one from the start, one from half
 * health. They come after their element's creature spells in each list, so they are cast first
 * whenever they are ready.
 */
public final class SovereignSpells {
    private static final Map<Element, List<MobSpell>> FIRST_PHASE = new EnumMap<>(Element.class);
    private static final Map<Element, List<MobSpell>> SECOND_PHASE = new EnumMap<>(Element.class);

    static {
        put(Element.FIRE, new RainOfEmbers(), new Inferno());
        put(Element.WATER, new TidalSurge(), new DrowningBubble());
        put(Element.WIND, new GaleVolley(), new Tempest());
        put(Element.EARTH, new Quake(), new Bulwark());
    }

    private SovereignSpells() {
    }

    private static void put(Element element, MobSpell first, MobSpell second) {
        List<MobSpell> calm = new ArrayList<>(MobSpells.of(element));
        calm.add(first);
        List<MobSpell> enraged = new ArrayList<>(calm);
        enraged.add(second);
        FIRST_PHASE.put(element, List.copyOf(calm));
        SECOND_PHASE.put(element, List.copyOf(enraged));
    }

    /** What a Sovereign of {@code element} casts, in its first or second phase. */
    public static List<MobSpell> of(Element element, boolean secondPhase) {
        return (secondPhase ? SECOND_PHASE : FIRST_PHASE).getOrDefault(element, MobSpells.of(element));
    }

    public static boolean isSignature(MobSpell spell) {
        return spell instanceof Signature;
    }

    /** Where the ground is under a hovering caster (water counts). */
    private static Vec3 ground(Mob caster) {
        Vec3 from = caster.position();
        BlockHitResult hit = caster.level().clip(new ClipContext(from, from.add(0, -16, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : from.add(0, -3, 0);
    }

    private abstract static class Signature implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ARCHMAGE;
        }

        static SovereignEntity sovereign(Mob caster) {
            return (SovereignEntity) caster;
        }
    }

    // ---- Vulkhar, Sovereign of Flame ----

    /** Six meteors: one on its target, five around it, each spot marked with fire first. */
    private static final class RainOfEmbers extends Signature {
        @Override
        public int cooldownTicks() {
            return 200;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 28;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            List<Vec3> spots = new ArrayList<>();
            spots.add(target.position());
            for (int i = 0; i < 5; i++) {
                double angle = caster.getRandom().nextDouble() * Math.PI * 2;
                double distance = 2 + caster.getRandom().nextDouble() * 4;
                spots.add(target.position().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance));
            }
            for (int i = 0; i < spots.size(); i++) {
                Vec3 spot = spots.get(i);
                for (int k = 0; k < 12; k++) {
                    double angle = Math.PI * 2 * k / 12;
                    level.sendParticles(ParticleTypes.FLAME, spot.x + Math.cos(angle) * 1.2, spot.y + 0.1, spot.z + Math.sin(angle) * 1.2,
                            1, 0, 0.02, 0, 0.01);
                }
                sovereign(caster).later(6 + i * 4, () -> ModSpells.FIREBALL.get().shootForMob(caster, spot, SovereignRules.POWER, true));
            }
            MobCasting.play(caster, SoundEvents.BLAZE_SHOOT, 2f, 0.6f);
        }
    }

    /** Three rings of burning ground roll out from beneath it, 3, 6 and 9 blocks out. */
    private static final class Inferno extends Signature {
        @Override
        public int cooldownTicks() {
            return 260;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 18;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 center = ground(caster);
            for (int ring = 1; ring <= 3; ring++) {
                double radius = ring * 3;
                int points = ring * 6;
                sovereign(caster).later((ring - 1) * 8, () -> {
                    for (int k = 0; k < points; k++) {
                        double angle = Math.PI * 2 * k / points;
                        Vec3 at = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                        FireField.spawn(level, at, 1.6, 80, caster);
                        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 6, 0.5, 0.1, 0.5, 0.05);
                    }
                    level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRECHARGE_USE, caster.getSoundSource(), 1.5f, 0.7f);
                });
            }
        }
    }

    // ---- Thalassa, Sovereign of Tides ----

    /** A wave out of it: it hurts, soaks and throws back everyone within 12 blocks. */
    private static final class TidalSurge extends Signature {
        private static final double RADIUS = 12;

        @Override
        public int cooldownTicks() {
            return 160;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= RADIUS;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 center = ground(caster);
            for (LivingEntity foe : sovereign(caster).foesWithin(RADIUS)) {
                SpellDamage.hurtMultiHit(foe, SpellDamage.source(level, Element.WATER, caster, caster), 4f * SovereignRules.POWER);
                Vec3 away = foe.position().subtract(center).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
                foe.setDeltaMovement(foe.getDeltaMovement().add(away.scale(1.6)).add(0, 0.45, 0));
                foe.hurtMarked = true;
                foe.addEffect(new MobEffectInstance(ModContent.WET, 200));
            }
            for (int r = 2; r <= RADIUS; r += 2) {
                int points = r * 4;
                for (int k = 0; k < points; k++) {
                    double angle = Math.PI * 2 * k / points;
                    level.sendParticles(ParticleTypes.SPLASH, center.x + Math.cos(angle) * r, center.y + 0.3, center.z + Math.sin(angle) * r,
                            2, 0.2, 0.2, 0.2, 0.1);
                }
            }
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, center.x, center.y, center.z, 40, 2, 0.5, 2, 0.3);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_SPLASH, caster.getSoundSource(), 2f, 0.6f);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, caster.getSoundSource(), 2f, 0.8f);
        }
    }

    /** Its target is trapped in a bubble, then a jet hits it (and pops it). */
    private static final class DrowningBubble extends Signature {
        @Override
        public int cooldownTicks() {
            return 300;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 20 && BubblePrisons.canTrap(target) && !BubblePrisons.isTrapped(target);
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            BubblePrisons.trap(target);
            sovereign(caster).later(15, () -> {
                if (target.isAlive()) {
                    MobJet.start(caster, target, SovereignRules.POWER);
                }
            });
        }
    }

    // ---- Caelum, Sovereign of Storms ----

    /** Five wind blades, fanned 12 degrees apart, at its target. */
    private static final class GaleVolley extends Signature {
        @Override
        public int cooldownTicks() {
            return 120;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 24;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            WindBladeSpell blade = ModSpells.WIND_BLADE.get();
            Vec3 aim = MobCasting.aimPoint(target);
            Vec3 from = MobCasting.castOrigin(caster, aim);
            Vec3 direction = aim.subtract(from).normalize();
            for (int i = -2; i <= 2; i++) {
                Vec3 fanned = direction.yRot((float) Math.toRadians(12 * i));
                SpellProjectile.shootFrom(caster, blade, from, fanned.scale(blade.releaseSpeed(1f)), SovereignRules.POWER);
            }
            MobCasting.play(caster, SoundEvents.BREEZE_SHOOT, 2f, 0.8f);
        }
    }

    /** It drags everyone within 14 blocks toward it; a moment later lightning strikes each of them. */
    private static final class Tempest extends Signature {
        private static final double RADIUS = 14;

        @Override
        public int cooldownTicks() {
            return 280;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= RADIUS;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            List<LivingEntity> caught = sovereign(caster).foesWithin(RADIUS);
            for (LivingEntity foe : caught) {
                Vec3 toward = caster.position().subtract(foe.position()).multiply(1, 0, 1);
                toward = toward.lengthSqr() < 1.0e-4 ? Vec3.ZERO : toward.normalize();
                foe.setDeltaMovement(foe.getDeltaMovement().add(toward.scale(0.9)).add(0, 0.3, 0));
                foe.hurtMarked = true;
                level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, foe.getX(), foe.getY(), foe.getZ(), 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, caster.getX(), caster.getY(0.5), caster.getZ(), 1, 0, 0, 0, 0);
            MobCasting.play(caster, SoundEvents.WIND_CHARGE_BURST.value(), 2f, 0.6f);
            sovereign(caster).later(14, () -> {
                for (LivingEntity foe : caught) {
                    if (!foe.isAlive()) {
                        continue;
                    }
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt != null) {
                        bolt.setVisualOnly(true);
                        bolt.moveTo(foe.getX(), foe.getY(), foe.getZ());
                        level.addFreshEntity(bolt);
                    }
                    ChainLightningSpell.chain(level, caster, caster.getBoundingBox().getCenter(), 0, foe, SovereignRules.POWER,
                            other -> SpellTargets.canAffect(caster, other));
                }
            });
        }
    }

    // ---- Orvald, Sovereign of Stone ----

    /** Three tremors roll out along the ground from beneath it, a block a tick, 12 blocks out. */
    private static final class Quake extends Signature {
        private static final int RADIUS = 12;

        @Override
        public int cooldownTicks() {
            return 200;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 14;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            Vec3 center = ground(caster);
            for (int wave = 0; wave < 3; wave++) {
                Set<Integer> struck = new HashSet<>();
                for (int r = 1; r <= RADIUS; r++) {
                    int radius = r;
                    sovereign(caster).later(wave * 15 + r, () -> ring(level, caster, center, radius, struck));
                }
            }
        }

        private static void ring(ServerLevel level, Mob caster, Vec3 center, int radius, Set<Integer> struck) {
            int points = Math.max(8, radius * 6);
            for (int k = 0; k < points; k++) {
                double angle = Math.PI * 2 * k / points;
                Vec3 at = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                BlockState underfoot = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
                if (!underfoot.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, underfoot), at.x, at.y + 0.1, at.z, 3, 0.2, 0.1, 0.2, 0.15);
                }
            }
            for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius + 1, 2, radius + 1),
                    e -> e.isAlive() && e != caster && !struck.contains(e.getId()) && Math.abs(e.getY() - center.y) < 2
                            && Math.abs(Math.hypot(e.getX() - center.x, e.getZ() - center.z) - radius) < 0.9 && SpellTargets.canAffect(caster, e))) {
                struck.add(foe.getId());
                SpellDamage.hurtMultiHit(foe, SpellDamage.source(level, Element.EARTH, caster, caster), 3f * SovereignRules.POWER);
                ElementalReactions.earthHit(foe, caster, 200);
                foe.setDeltaMovement(foe.getDeltaMovement().add(0, 0.6, 0));
                foe.hurtMarked = true;
                foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            if (radius == 1) {
                level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), caster.getSoundSource(), 1f, 0.6f);
                level.playSound(null, center.x, center.y, center.z, SoundEvents.GRAVEL_BREAK, caster.getSoundSource(), 2f, 0.5f);
            }
        }
    }

    /** Three Earth wisps rise to guard it: while any lives, it takes a quarter of the damage. */
    private static final class Bulwark extends Signature {
        @Override
        public int cooldownTicks() {
            return 600;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return !sovereign(caster).hasBulwark();
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            for (int i = 0; i < 3; i++) {
                double angle = Math.PI * 2 * i / 3;
                BlockPos at = BlockPos.containing(caster.getX() + Math.cos(angle) * 3, caster.getY(), caster.getZ() + Math.sin(angle) * 3);
                WispEntity wisp = WispSpawner.spawnAt(level, Element.EARTH, at, MobSpawnType.MOB_SUMMONED);
                if (wisp != null) {
                    wisp.setTarget(target);
                    sovereign(caster).guardBy(wisp);
                }
            }
            StoneWards.raise(caster);
            MobCasting.play(caster, SoundEvents.BASALT_PLACE, 2f, 0.6f);
        }
    }
}
