package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SmiteRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.SmiteOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Smite's light (see SmiteSpell and the Smite spec), worked out here on the server and never saved:
 * pillars waiting over their marks, then falling (SmiteRules has the numbers); the consecrated ground
 * they leave; Sunlances following their caster's aim; Wrath of Heaven's rain of small pillars; and
 * Avatars of Light. Each client draws the light from one particle apiece (SmiteOptions).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Smites {
    private static final double SEEN = 96;
    private static final int PILLAR_LIFE = 12;

    /**
     * How a pillar strikes: how hard ({@code share} of its full damage) and wide, and what its level
     * adds. {@link #BASE} is the Lv 1 pillar other casters use.
     */
    public record Judgment(float share, double radius, boolean consecrates, boolean purges, float undead, int fireSeconds,
                           boolean bursts) {
        public static final Judgment BASE = of(1);

        public static Judgment of(int level) {
            return new Judgment(1f, SmiteRules.radius(level), SmiteRules.consecrates(level), SmiteRules.purges(level),
                    SmiteRules.undead(level), SmiteRules.fireSeconds(level), SmiteRules.bursts(level));
        }

        /** Wrath of Heaven's and Avatar of Light's small pillars: narrower and weaker, leaving no holy ground, throwing nothing back. */
        Judgment small() {
            return new Judgment(SmiteRules.SMALL_SHARE, SmiteRules.SMALL_RADIUS, false, purges, undead, fireSeconds, false);
        }

        /** The same pillar, {@code radius} wide (Triple Judgment's are narrower, so the three don't overlap). */
        public Judgment narrowed(double radius) {
            return new Judgment(share, Math.min(this.radius, radius), consecrates, purges, undead, fireSeconds, bursts);
        }
    }

    private static final class Strike {
        final ServerLevel level;
        final Vec3 at;
        final Entity caster;
        final float power;
        final Judgment judgment;
        final Predicate<LivingEntity> affects;
        final long marksAt;
        final long fallsAt;
        @Nullable
        final Consumer<Vec3> onFall;
        boolean marked;

        Strike(ServerLevel level, Vec3 at, Entity caster, float power, Judgment judgment, Predicate<LivingEntity> affects,
               long marksAt, long fallsAt, @Nullable Consumer<Vec3> onFall) {
            this.level = level;
            this.at = at;
            this.caster = caster;
            this.power = power;
            this.judgment = judgment;
            this.affects = affects;
            this.marksAt = marksAt;
            this.fallsAt = fallsAt;
            this.onFall = onFall;
        }
    }

    /** Consecrated ground. */
    private record Ground(ServerLevel level, Vec3 at, double radius, Entity caster, float power, Predicate<LivingEntity> affects,
                          long until) {
    }

    private static final class Lance {
        final ServerLevel level;
        final LivingEntity caster;
        final float power;
        final Judgment judgment;
        final Predicate<LivingEntity> affects;
        Vec3 at;
        int age;

        Lance(ServerLevel level, LivingEntity caster, float power, Judgment judgment, Predicate<LivingEntity> affects, Vec3 at) {
            this.level = level;
            this.caster = caster;
            this.power = power;
            this.judgment = judgment;
            this.affects = affects;
            this.at = at;
        }
    }

    private record Avatar(ServerLevel level, LivingEntity caster, float power, Judgment judgment, Predicate<LivingEntity> affects,
                          long until) {
    }

    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final List<Ground> GROUNDS = new ArrayList<>();
    private static final List<Lance> LANCES = new ArrayList<>();
    private static final Map<UUID, Avatar> AVATARS = new HashMap<>();

    private Smites() {
    }

    /** The Lv 1 Smite, for other casters: a mark at {@code at}, the pillar three quarters of a second later. */
    public static void strike(ServerLevel level, Vec3 at, Entity caster, float power, Predicate<LivingEntity> affects) {
        strike(level, at, caster, power, Judgment.BASE, SmiteRules.delay(1), 0, affects, null);
    }

    /**
     * Marks {@code at} in {@code wait} ticks; {@code delay} ticks after that the pillar falls, and then
     * {@code onFall} (if any) runs with where it fell.
     */
    public static void strike(ServerLevel level, Vec3 at, Entity caster, float power, Judgment judgment, int delay, int wait,
                              Predicate<LivingEntity> affects, @Nullable Consumer<Vec3> onFall) {
        long now = level.getGameTime();
        Strike strike = new Strike(level, at, caster, power, judgment, affects, now + wait, now + wait + delay, onFall);
        STRIKES.add(strike);
        if (wait <= 0) {
            mark(level, strike, now);
        }
    }

    /** Sunlance: from where the pillar fell, its beam follows {@code caster}'s aim for 3 seconds. */
    public static void lance(ServerLevel level, LivingEntity caster, Vec3 at, float power, Judgment judgment, Predicate<LivingEntity> affects) {
        LANCES.add(new Lance(level, caster, power, judgment, affects, at));
    }

    /** Wrath of Heaven: six small pillars rain down within 6 blocks of {@code at} over two seconds. */
    public static void wrath(ServerLevel level, Entity caster, Vec3 at, float power, Judgment judgment, Predicate<LivingEntity> affects) {
        for (int i = 0; i < SmiteRules.WRATH_PILLARS; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = 1.5 + level.random.nextDouble() * (SmiteRules.WRATH_RADIUS - 1.5);
            Vec3 spot = surface(level, at.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance));
            if (spot != null) {
                strike(level, spot, caster, power, judgment.small(), SmiteRules.SMALL_DELAY, SmiteRules.wrathDelay(i) - SmiteRules.SMALL_DELAY,
                        affects, null);
            }
        }
    }

    /** Avatar of Light: {@code caster} shines for 8 seconds, small pillars falling on the foes near them. */
    public static void avatar(ServerLevel level, LivingEntity caster, float power, Judgment judgment, Predicate<LivingEntity> affects) {
        AVATARS.put(caster.getUUID(), new Avatar(level, caster, power, judgment.small(), affects, level.getGameTime() + SmiteRules.AVATAR_TICKS));
        caster.addEffect(new MobEffectInstance(MobEffects.GLOWING, SmiteRules.AVATAR_TICKS, 0, false, false, true));
        play(level, caster.position(), SoundEvents.BEACON_POWER_SELECT, 1f, 1.4f);
    }

    /** The ground at or near {@code point}: a few blocks above or below it, or null. */
    @Nullable
    static Vec3 surface(ServerLevel level, Vec3 point) {
        return StormeyeSpell.groundBelow(level, point.add(0, 3, 0), 8);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (!STRIKES.isEmpty()) {
            List<Strike> falling = new ArrayList<>();
            for (Iterator<Strike> it = STRIKES.iterator(); it.hasNext(); ) {
                Strike strike = it.next();
                if (strike.level != level) {
                    continue;
                }
                if (!strike.marked && now >= strike.marksAt) {
                    mark(level, strike, now);
                }
                if (now >= strike.fallsAt) {
                    it.remove();
                    falling.add(strike);
                }
            }
            // After the loop: what falling sets off (more strikes, a lance) can't touch the list mid-walk.
            falling.forEach(strike -> fall(level, strike));
        }
        if (!GROUNDS.isEmpty() && now % 20 == 0) {
            GROUNDS.removeIf(ground -> ground.level() == level && !sanctify(level, ground, now));
        }
        if (!LANCES.isEmpty()) {
            LANCES.removeIf(lance -> lance.level == level && !sweep(level, lance));
        }
        if (!AVATARS.isEmpty()) {
            List<Avatar> shining = new ArrayList<>(AVATARS.values());
            for (Avatar avatar : shining) {
                if (avatar.level() == level && !shine(level, avatar, now)) {
                    AVATARS.remove(avatar.caster().getUUID());
                }
            }
        }
    }

    /** The mark: a ring of light on the ground, brightening until the pillar falls. */
    private static void mark(ServerLevel level, Strike strike, long now) {
        strike.marked = true;
        send(level, strike.at, new SmiteOptions(SmiteOptions.MARK, (float) strike.judgment.radius(), (int) Math.max(1, strike.fallsAt - now)));
        play(level, strike.at, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.8f);
    }

    private static void fall(ServerLevel level, Strike strike) {
        Vec3 at = strike.at;
        Judgment judgment = strike.judgment;
        send(level, at, new SmiteOptions(SmiteOptions.PILLAR, (float) judgment.radius(), PILLAR_LIFE));
        play(level, at, SoundEvents.TRIDENT_THUNDER.value(), judgment.share() < 1f ? 0.5f : 0.8f, 1.5f);
        play(level, at, SoundEvents.BEACON_DEACTIVATE, 1f, 2f);
        hit(level, strike.caster, strike.power, judgment.share(), judgment.radius(), at, judgment, strike.affects, true);
        if (judgment.consecrates()) {
            GROUNDS.add(new Ground(level, at, judgment.radius(), strike.caster, strike.power, strike.affects,
                    level.getGameTime() + SmiteRules.CONSECRATION_TICKS));
            send(level, at, new SmiteOptions(SmiteOptions.GROUND, (float) judgment.radius(), SmiteRules.CONSECRATION_TICKS));
        }
        if (strike.onFall != null) {
            strike.onFall.accept(at);
        }
    }

    /** What the light does where it strikes, {@code share} as hard as a full pillar, within {@code radius}. */
    private static void hit(ServerLevel level, Entity caster, float power, float share, double radius, Vec3 at, Judgment judgment,
                            Predicate<LivingEntity> affects, boolean landing) {
        DamageSource source = SpellDamage.source(level, Element.RADIANCE, caster, caster);
        AABB area = new AABB(at, at).inflate(radius + 1, 2.5, radius + 1);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != caster && within(e, at, radius) && affects.test(e))) {
            boolean undead = target.getType().is(EntityTypeTags.UNDEAD);
            SpellDamage.hurtMultiHit(target, source, SmiteRules.DAMAGE * power * share * (undead ? judgment.undead() : 1f));
            target.igniteForSeconds(judgment.fireSeconds());
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, SmiteRules.GLOW_TICKS));
            if (judgment.purges()) {
                purge(target, MobEffectCategory.BENEFICIAL);
            }
            if (landing && judgment.bursts()) {
                throwBack(target, at);
            }
        }
        if (judgment.purges()) {
            for (Player player : level.getEntitiesOfClass(Player.class, area, p -> p.isAlive() && within(p, at, radius))) {
                purge(player, MobEffectCategory.HARMFUL);
            }
        }
    }

    private static boolean within(Entity entity, Vec3 at, double radius) {
        double dx = entity.getX() - at.x;
        double dz = entity.getZ() - at.z;
        double reach = radius + entity.getBbWidth() / 2;
        return dx * dx + dz * dz <= reach * reach;
    }

    /** Purge: takes away every effect of {@code category} (a foe's boons, a player's banes). */
    private static void purge(LivingEntity entity, MobEffectCategory category) {
        for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == category) {
                entity.removeEffect(effect.getEffect());
            }
        }
    }

    /** Radiant Burst: thrown back from the pillar's middle (as much as knockback resistance lets it) and blinded. */
    private static void throwBack(LivingEntity target, Vec3 at) {
        Vec3 away = new Vec3(target.getX() - at.x, 0, target.getZ() - at.z);
        away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
        double hold = Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (hold > 0) {
            target.setDeltaMovement(away.scale(SmiteRules.BURST_PUSH * hold).add(0, 0.35 * hold, 0));
            target.hurtMarked = true;
        }
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, SmiteRules.BLIND_TICKS));
    }

    /** One second of consecrated ground: foes set alight, the undead burned, players healed. False once it's spent. */
    private static boolean sanctify(ServerLevel level, Ground ground, long now) {
        if (now >= ground.until()) {
            return false;
        }
        AABB area = new AABB(ground.at(), ground.at()).inflate(ground.radius() + 1, 2, ground.radius() + 1);
        DamageSource source = SpellDamage.source(level, Element.RADIANCE, ground.caster(), ground.caster());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && within(e, ground.at(), ground.radius()))) {
            if (entity instanceof Player player) {
                player.heal(SmiteRules.CONSECRATION_HEAL);
            } else if (entity != ground.caster() && ground.affects().test(entity)) {
                entity.igniteForSeconds(2);
                if (entity.getType().is(EntityTypeTags.UNDEAD)) {
                    SpellDamage.hurtMultiHit(entity, source, SmiteRules.CONSECRATION_UNDEAD_DAMAGE * ground.power());
                }
            }
        }
        return true;
    }

    /** One tick of a Sunlance: it slides toward its caster's aim, burning what it touches now and then. False once it's spent. */
    private static boolean sweep(ServerLevel level, Lance lance) {
        if (++lance.age > SmiteRules.LANCE_TICKS || !lance.caster.isAlive() || lance.caster.level() != level) {
            return false;
        }
        Vec3 aim = StormeyeSpell.groundUnderAim(level, lance.caster);
        if (aim != null) {
            Vec3 toward = new Vec3(aim.x - lance.at.x, 0, aim.z - lance.at.z);
            double distance = toward.length();
            if (distance > 1.0e-3) {
                Vec3 next = lance.at.add(toward.scale(Math.min(SmiteRules.LANCE_SPEED, distance) / distance));
                Vec3 ground = surface(level, next);
                lance.at = ground != null ? ground : next;
            }
        }
        double radius = lance.judgment.radius() * 0.6;
        if (lance.age % 2 == 0) {
            send(level, lance.at, new SmiteOptions(SmiteOptions.BEAM, (float) radius, 3));
        }
        if (lance.age % SmiteRules.LANCE_INTERVAL == 0) {
            hit(level, lance.caster, lance.power, SmiteRules.LANCE_SHARE, radius, lance.at, lance.judgment, lance.affects, false);
            level.sendParticles(ParticleTypes.END_ROD, lance.at.x, lance.at.y + 0.2, lance.at.z, 8, radius / 2, 0.1, radius / 2, 0.08);
        }
        if (lance.age % 20 == 1) {
            play(level, lance.at, SoundEvents.BEACON_AMBIENT, 1f, 1.6f);
        }
        return true;
    }

    /** One tick of an Avatar of Light: a halo now and then, and every second a small pillar on the nearest foe. False once it's spent. */
    private static boolean shine(ServerLevel level, Avatar avatar, long now) {
        LivingEntity caster = avatar.caster();
        if (now > avatar.until() || !caster.isAlive() || caster.level() != level) {
            return false;
        }
        long left = avatar.until() - now;
        if (left % 10 == 0) {
            Vec3 crown = caster.position().add(0, caster.getBbHeight() + 0.4, 0);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8 + now * 0.1;
                level.sendParticles(ParticleTypes.END_ROD, crown.x + Math.cos(angle) * 0.45, crown.y, crown.z + Math.sin(angle) * 0.45, 1, 0, 0, 0, 0);
            }
        }
        if (left % SmiteRules.AVATAR_INTERVAL == 0) {
            LivingEntity foe = nearestFoe(level, caster, avatar.affects());
            if (foe != null) {
                strike(level, foe.position(), caster, avatar.power(), avatar.judgment(), SmiteRules.SMALL_DELAY, 0, avatar.affects(), null);
            }
        }
        return true;
    }

    @Nullable
    private static LivingEntity nearestFoe(ServerLevel level, LivingEntity caster, Predicate<LivingEntity> affects) {
        Vec3 eye = caster.getEyePosition();
        double range = SmiteRules.AVATAR_RANGE;
        return level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(range),
                        e -> e != caster && e.isAlive() && affects.test(e) && e.distanceToSqr(caster) <= range * range)
                .stream()
                .filter(e -> level.clip(new ClipContext(eye, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                        CollisionContext.empty())).getType() == HitResult.Type.MISS)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(caster)))
                .orElse(null);
    }

    /** Avatar of Light: the shining caster takes a fifth less damage. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Avatar avatar = AVATARS.get(event.getEntity().getUUID());
        if (avatar != null && event.getEntity().level().getGameTime() <= avatar.until()) {
            event.setAmount(event.getAmount() * SmiteRules.AVATAR_GUARD);
        }
    }

    private static void send(ServerLevel level, Vec3 at, SmiteOptions options) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) < SEEN * SEEN) {
                level.sendParticles(player, options, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STRIKES.clear();
        GROUNDS.clear();
        LANCES.clear();
        AVATARS.clear();
    }
}
