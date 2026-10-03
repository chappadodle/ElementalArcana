package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.ChainLightningRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellHold;
import com.chappadodle.elementalarcana.content.ArcOptions;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.CastingService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Chain Lightning's work (see ChainLightningSpell and the Chain Lightning spec): the chain itself,
 * from creature to creature (one at a time, or forking, or bursting at every strike); Live Wire's
 * charged creatures arcing to each other; Ball Lightning's drifting orbs; and Thunder Lord's held
 * stream of strikes. The bolts are drawn by each client from one particle apiece (ArcOptions).
 * Nothing here is saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ChainLightnings {
    private static final double SEEN = 64;
    /** How high above its target a bolt from the sky starts, and how thick it is. */
    private static final double SKY = 24;
    private static final float SKY_WIDTH = 2.5f;

    /**
     * How a chain behaves: its numbers by level and fork (ChainLightningRules), and how hard it
     * strikes ({@code share} of its full damage). {@link #BASE} is the Lv 1 chain other casters use.
     */
    public record Chain(int jumps, double jumpRange, float falloff, float conducted, int wetJumps, int maxStrikes, int fanOut,
                        int stunTicks, boolean overload, boolean skyBolt, boolean liveWire, float share) {
        public static final Chain BASE = of(1, null);

        public static Chain of(int level, @Nullable String fork) {
            return new Chain(ChainLightningRules.jumps(level, fork), ChainLightningRules.jumpRange(level),
                    ChainLightningRules.falloff(level), ChainLightningRules.conducted(level), ChainLightningRules.wetJumps(level),
                    ChainLightningRules.maxStrikes(level, fork), ChainLightningRules.fanOut(fork), ChainLightningRules.stunTicks(level),
                    ChainLightningRules.OVERLOAD.equals(fork), ChainLightningRules.skyBolt(level), ChainLightningRules.liveWire(level), 1f);
        }

        /** Thunder Lord's pulses: weaker, and no bolt from the sky (the first strike had it). */
        Chain pulse() {
            return new Chain(jumps, jumpRange, falloff, conducted, wetJumps, maxStrikes, fanOut, stunTicks, overload, false, liveWire,
                    ChainLightningRules.LORD_SHARE);
        }

        /** Ball Lightning's chains: short, plain (no fork, burst, bolt from the sky or charge) and weaker. */
        Chain orb() {
            int short_ = ChainLightningRules.BALL_JUMPS;
            return new Chain(short_, jumpRange, falloff, conducted, wetJumps, short_ + 1 + wetJumps, 1,
                    stunTicks, false, false, false, ChainLightningRules.BALL_SHARE);
        }
    }

    /** A strike waiting its turn: what it hits, where the bolt comes from, how far down the chain, and how many jumps are left after it. */
    private record Link(LivingEntity target, Vec3 from, double skip, int jump, int jumpsLeft) {
    }

    /** Live Wire: a charged creature, whose it is, and until when. */
    private record Charge(ServerLevel level, LivingEntity caster, float power, long until) {
    }

    private static final class Orb {
        final ServerLevel level;
        final LivingEntity caster;
        final float power;
        final Chain chain;
        final Predicate<LivingEntity> affects;
        Vec3 position;
        Vec3 velocity;
        int age;

        Orb(ServerLevel level, LivingEntity caster, float power, Chain chain, Predicate<LivingEntity> affects, Vec3 position, Vec3 velocity) {
            this.level = level;
            this.caster = caster;
            this.power = power;
            this.chain = chain;
            this.affects = affects;
            this.position = position;
            this.velocity = velocity;
        }
    }

    private static final Map<Integer, Charge> CHARGED = new HashMap<>();
    private static final List<Orb> ORBS = new ArrayList<>();

    private ChainLightnings() {
    }

    /**
     * The chain: from {@code from} to {@code first} (drawn from {@code skip} blocks along), then on
     * from creature to creature that {@code jumpsTo} allows, never back to one already struck and
     * never to the caster.
     */
    public static void chain(ServerLevel level, LivingEntity caster, Vec3 from, double skip, LivingEntity first, float power, Chain chain,
                             Predicate<LivingEntity> jumpsTo) {
        DamageSource source = SpellDamage.source(level, Element.LIGHTNING, caster, caster);
        float damage = ChainLightningRules.DAMAGE * power * chain.share();
        Set<Integer> struck = new HashSet<>();
        struck.add(caster.getId());
        struck.add(first.getId());
        Deque<Link> waiting = new ArrayDeque<>();
        waiting.add(new Link(first, from, skip, 0, chain.jumps()));
        int strikes = 0;
        while (!waiting.isEmpty()) {
            Link link = waiting.poll();
            LivingEntity target = link.target();
            Vec3 to = target.getBoundingBox().getCenter();
            bolt(level, link.from(), to, link.skip(), 1f);
            boolean wet = ElementalReactions.auraOf(target) == ElementalReactions.Aura.HYDRO;
            float hit = damage * (float) Math.pow(chain.falloff(), link.jump()) * (wet ? chain.conducted() : 1f);
            if (link.jump() == 0 && chain.skyBolt()) {
                hit *= ChainLightningRules.SKY_BONUS;
                skyBolt(level, target);
            }
            SpellDamage.hurtMultiHit(target, source, hit);
            strikes++;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 12, 0.3, 0.4, 0.3, 0.2);
            if (wet) {
                level.sendParticles(ParticleTypes.SPLASH, to.x, to.y, to.z, 10, 0.3, 0.3, 0.3, 0.1);
            }
            play(level, to, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.6f + level.random.nextFloat() * 0.3f);
            if (chain.stunTicks() > 0) {
                // A jolt: slowed to a crawl and too weak to strike back, for a moment.
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, chain.stunTicks(), 4));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, chain.stunTicks(), 1));
            }
            if (chain.overload()) {
                overload(level, caster, target, hit * ChainLightningRules.OVERLOAD_SHARE, source, jumpsTo);
            }
            if (chain.liveWire()) {
                CHARGED.put(target.getId(), new Charge(level, caster, power, level.getGameTime() + ChainLightningRules.LIVE_WIRE_TICKS));
            }
            int left = link.jumpsLeft() + (wet ? chain.wetJumps() : 0);
            if (left <= 0) {
                continue;
            }
            for (LivingEntity next : nearest(level, to, chain.jumpRange(), chain.fanOut(), struck, jumpsTo)) {
                if (strikes + waiting.size() >= chain.maxStrikes()) {
                    break;
                }
                struck.add(next.getId());
                waiting.add(new Link(next, to, 0, link.jump() + 1, left - 1));
            }
        }
    }

    /** The cast's own sound, at the caster: a short roll of thunder. */
    public static void thunder(ServerLevel level, LivingEntity caster) {
        play(level, caster.position(), SoundEvents.TRIDENT_THUNDER.value(), 0.35f, 1.8f);
    }

    /** Up to {@code count} creatures nearest {@code at} within {@code range} that haven't been struck. */
    private static List<LivingEntity> nearest(ServerLevel level, Vec3 at, double range, int count, Set<Integer> struck,
                                              Predicate<LivingEntity> jumpsTo) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(range),
                        e -> e.isAlive() && !e.isSpectator() && !struck.contains(e.getId()) && jumpsTo.test(e)
                                && e.getBoundingBox().getCenter().distanceToSqr(at) <= range * range)
                .stream()
                .sorted(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(at)))
                .limit(count)
                .toList();
    }

    /** Overload: a shock bursts from a struck creature, hitting everything else near it. */
    private static void overload(ServerLevel level, LivingEntity caster, LivingEntity center, float damage, DamageSource source,
                                 Predicate<LivingEntity> affects) {
        Vec3 at = center.getBoundingBox().getCenter();
        double radius = ChainLightningRules.OVERLOAD_RADIUS;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(radius),
                e -> e != center && e != caster && e.isAlive() && affects.test(e))) {
            Vec3 to = other.getBoundingBox().getCenter();
            if (to.distanceTo(at) > radius + other.getBbWidth() / 2) {
                continue;
            }
            bolt(level, at, to, 0, 0.6f);
            SpellDamage.hurtMultiHit(other, source, damage);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 30, 0.8, 0.6, 0.8, 0.35);
        play(level, at, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.7f, 1.4f);
    }

    /** Thunderstruck: a bolt from the sky onto the target, with the trident's thunder. */
    private static void skyBolt(ServerLevel level, LivingEntity target) {
        Vec3 foot = target.position();
        Vec3 sky = foot.add((level.random.nextDouble() - 0.5) * 4, SKY, (level.random.nextDouble() - 0.5) * 4);
        bolt(level, sky, foot, 0, SKY_WIDTH);
        play(level, foot, SoundEvents.TRIDENT_THUNDER.value(), 2f, 1f);
    }

    /** A bolt from {@code a} to {@code b}, drawn by every client near it (none of the first {@code skip} blocks). */
    public static void bolt(ServerLevel level, Vec3 a, Vec3 b, double skip, float width) {
        ArcOptions arc = ArcOptions.between(a, b, skip, width);
        Vec3 middle = a.lerp(b, 0.5);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(middle) < SEEN * SEEN) {
                level.sendParticles(player, arc, true, a.x, a.y, a.z, 1, 0, 0, 0, 0);
            }
        }
    }

    // ---- Ball Lightning ----

    /** Looses an orb from {@code from} drifting along {@code direction}, striking short chains as it goes. */
    public static void loose(ServerLevel level, LivingEntity caster, Vec3 from, Vec3 direction, float power, Chain chain,
                             Predicate<LivingEntity> affects) {
        ORBS.add(new Orb(level, caster, power, chain.orb(), affects, from, direction.normalize().scale(ChainLightningRules.BALL_SPEED)));
        play(level, from, SoundEvents.BEACON_ACTIVATE, 0.6f, 2f);
    }

    private static boolean tickOrb(Orb orb) {
        ServerLevel level = orb.level;
        if (++orb.age > ChainLightningRules.BALL_TICKS) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, orb.position.x, orb.position.y, orb.position.z, 24, 0.3, 0.3, 0.3, 0.3);
            play(level, orb.position, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.9f);
            return false;
        }
        if (orb.velocity.lengthSqr() > 0) {
            Vec3 next = orb.position.add(orb.velocity);
            BlockHitResult wall = level.clip(new ClipContext(orb.position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    CollisionContext.empty()));
            if (wall.getType() != HitResult.Type.MISS) {
                // Stopped by a wall: it hangs there, still striking.
                orb.position = wall.getLocation().subtract(orb.velocity.normalize().scale(0.3));
                orb.velocity = Vec3.ZERO;
            } else {
                orb.position = next;
            }
        }
        Vec3 at = orb.position;
        ParticleOptions glow = GlowParticleOptions.of(ModContent.FLARE.get(), 0xFFF8D0, 0xFFC830, 0.5f, 3);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) < SEEN * SEEN) {
                level.sendParticles(player, glow, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, 0.25, 0.25, 0.25, 0.08);
        if (orb.age % 20 == 1) {
            play(level, at, SoundEvents.BEACON_AMBIENT, 0.8f, 1.9f);
        }
        if (orb.age % ChainLightningRules.BALL_INTERVAL == 0) {
            LivingEntity target = inReach(level, orb);
            if (target != null) {
                chain(level, orb.caster, at, 0, target, orb.power, orb.chain, orb.affects);
            }
        }
        return true;
    }

    /** The nearest creature the orb can see within its reach, or null. */
    @Nullable
    private static LivingEntity inReach(ServerLevel level, Orb orb) {
        Vec3 at = orb.position;
        double reach = ChainLightningRules.BALL_REACH;
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach),
                        e -> e != orb.caster && e.isAlive() && !e.isSpectator() && orb.affects.test(e)
                                && e.getBoundingBox().getCenter().distanceToSqr(at) <= reach * reach)
                .stream()
                .filter(e -> level.clip(new ClipContext(at, e.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS)
                .min(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(at)))
                .orElse(null);
    }

    // ---- Thunder Lord ----

    /**
     * Thunder Lord's hold: while the cast is held, the chain strikes again every half second at
     * whatever is under the crosshair, each pulse paid for in mana; out of mana, it ends.
     */
    public static SpellHold lord(CastContext context, Chain chain, Predicate<LivingEntity> affects) {
        ServerPlayer caster = context.caster();
        int spellLevel = context.spellLevel();
        float power = context.power();
        Chain pulse = chain.pulse();
        return new SpellHold() {
            @Override
            public boolean tick(int heldTicks) {
                if (!caster.isAlive()) {
                    return false;
                }
                if (heldTicks <= 0 || heldTicks % ChainLightningRules.LORD_INTERVAL != 0) {
                    return true;
                }
                LivingEntity target = ChainLightningSpell.aim(caster, spellLevel);
                if (target == null) {
                    // Nothing to strike: no pulse, and nothing paid.
                    return true;
                }
                if (!CastingService.payUpkeep(caster, MagicAttachments.get(caster), ChainLightningRules.LORD_UPKEEP)) {
                    return false;
                }
                chain(caster.serverLevel(), caster, ChainLightningSpell.hand(caster), ChainLightningSpell.FIRST_PERSON_SKIP, target, power,
                        pulse, affects);
                return true;
            }

            @Override
            public void release(int heldTicks) {
            }

            @Override
            public void cancel() {
            }

            @Override
            public int maxHoldTicks() {
                return ChainLightningRules.LORD_TICKS;
            }
        };
    }

    // ---- ticking ----

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!ORBS.isEmpty()) {
            ORBS.removeIf(orb -> orb.level == level && !tickOrb(orb));
        }
        if (!CHARGED.isEmpty() && level.getGameTime() % ChainLightningRules.LIVE_WIRE_INTERVAL == 0) {
            arcWires(level);
        }
    }

    /**
     * Live Wire: every charged creature arcs to the nearest other one of its caster's within reach,
     * sparing those already struck this second while there are others, so the arcs spread out.
     */
    private static void arcWires(ServerLevel level) {
        long now = level.getGameTime();
        List<LivingEntity> charged = new ArrayList<>();
        for (Iterator<Map.Entry<Integer, Charge>> it = CHARGED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Charge> entry = it.next();
            Charge charge = entry.getValue();
            if (charge.level() != level) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (now >= charge.until() || !(entity instanceof LivingEntity living) || !living.isAlive()) {
                it.remove();
            } else {
                charged.add(living);
            }
        }
        double range = ChainLightningRules.LIVE_WIRE_RANGE;
        Set<Integer> zapped = new HashSet<>();
        for (LivingEntity from : charged) {
            Charge charge = CHARGED.get(from.getId());
            Vec3 at = from.getBoundingBox().getCenter();
            charged.stream()
                    .filter(to -> to != from && CHARGED.get(to.getId()).caster() == charge.caster()
                            && to.getBoundingBox().getCenter().distanceToSqr(at) <= range * range)
                    .min(Comparator.<LivingEntity>comparingInt(to -> zapped.contains(to.getId()) ? 1 : 0)
                            .thenComparingDouble(to -> to.getBoundingBox().getCenter().distanceToSqr(at)))
                    .ifPresent(to -> {
                        zapped.add(to.getId());
                        Vec3 end = to.getBoundingBox().getCenter();
                        bolt(level, at, end, 0, 0.5f);
                        SpellDamage.hurtMultiHit(to, SpellDamage.source(level, Element.LIGHTNING, charge.caster(), charge.caster()),
                                ChainLightningRules.LIVE_WIRE_DAMAGE * charge.power());
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 4, 0.2, 0.3, 0.2, 0.1);
                        play(level, end, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.25f, 2f);
                    });
        }
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CHARGED.clear();
        ORBS.clear();
    }
}
