package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.GaleDashRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellHold;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gale Dash on the server (see GaleDashSpell and the Gale Dash spec): the dash, Blink Storm's blink
 * and Hurricane Rush's rush, and while a dash lasts, what it does along its way (its trail; from
 * level 3 no harm comes to the dasher; Slipstream Trail shoves foes aside and quickens allies;
 * Gale Strike and the rush hit what they pass; Swirling Rush Swirls it) and after it (Tailwind,
 * Featherfall). Phantom Step hides the dasher from the creatures hunting them. Never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class GaleDashes {

    /** A dash under way: what the caster's spell was, and who it has already passed through. */
    private static final class Dash {
        final int level;
        final float power;
        @Nullable
        final String branch5;
        final boolean rush;
        final long start;
        long end;
        final Vec3 direction;
        final Set<Integer> struck = new HashSet<>();
        final Set<Integer> swirled = new HashSet<>();

        Dash(CastContext context, Vec3 direction, boolean rush) {
            this.level = context.spellLevel();
            this.power = context.power();
            this.branch5 = level >= 5 ? context.branch(5) : null;
            this.rush = rush;
            this.start = context.level().getGameTime();
            this.end = start + GaleDashRules.DASH_TICKS;
            this.direction = direction;
        }
    }

    private static final Map<UUID, Dash> DASHES = new HashMap<>();
    private static final Map<UUID, Long> UNSEEN_UNTIL = new HashMap<>();

    private GaleDashes() {
    }

    /** The dash: a burst of wind the way the caster looks, shoving away what's near where they started. */
    public static void dash(CastContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        Vec3 look = context.look();
        boolean airStep = GaleDashRules.airStep(context.spellLevel()) && !caster.onGround();
        // On the ground (and before Air Step) never into the ground: always a little lift.
        Vec3 direction = airStep ? look.normalize() : new Vec3(look.x, Math.max(look.y, 0.1), look.z).normalize();
        caster.setDeltaMovement(direction.scale(GaleDashRules.speed(context.power())).add(0, airStep ? 0.1 : 0.35, 0));
        caster.hurtMarked = true;
        caster.resetFallDistance();
        MagicAttachments.get(caster).setFallImmune(true);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(3.0),
                target -> SpellTargets.canAffect(caster, target))) {
            Vec3 away = target.position().subtract(caster.position()).normalize();
            target.knockback(1.0 * context.power(), -away.x, -away.z);
            target.hurtMarked = true;
        }
        begin(context, direction, false);
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, caster.getX(), caster.getY(), caster.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.2, caster.getZ(), 15, 0.4, 0.1, 0.4, 0.05);
        play(level, caster.position(), SoundEvents.WIND_CHARGE_BURST.value(), 1f, 1f);
        play(level, caster.position(), SoundEvents.BREEZE_SLIDE, 1f, 1.3f);
    }

    /**
     * Blink Storm: the dash becomes a blink, as far as there's room and sight up to 8 blocks the way
     * the caster looks, with a gust at either end. False if there's no room to blink.
     */
    public static boolean blink(CastContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        Vec3 look = context.look();
        Vec3 from = caster.position();
        Vec3 eye = caster.getEyePosition();
        Vec3 to = null;
        for (double distance = GaleDashRules.BLINK_RANGE; distance >= 1.5; distance -= 0.5) {
            Vec3 spot = from.add(look.scale(distance));
            if (level.noCollision(caster, caster.getBoundingBox().move(spot.subtract(from)))
                    && level.clip(new ClipContext(eye, spot.add(0, caster.getEyeHeight(), 0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, caster)).getType() == HitResult.Type.MISS) {
                to = spot;
                break;
            }
        }
        if (to == null) {
            return false;
        }
        gust(level, caster, from, context.power());
        caster.teleportTo(to.x, to.y, to.z);
        caster.setDeltaMovement(look.scale(0.3));
        caster.hurtMarked = true;
        caster.resetFallDistance();
        MagicAttachments.get(caster).setFallImmune(true);
        gust(level, caster, to, context.power());
        Vec3 way = to.subtract(from);
        for (double t = 0; t <= 1; t += 0.5 / Math.max(1, way.length())) {
            Vec3 at = from.add(way.scale(t)).add(0, 1, 0);
            level.sendParticles(ModContent.WIND_STREAK.get(), at.x, at.y, at.z, 2, 0.15, 0.3, 0.15, 0.02);
        }
        begin(context, look.normalize(), false);
        return true;
    }

    /** A gust at a blink's end: foes near it are hurt and thrown back. */
    private static void gust(ServerLevel level, ServerPlayer caster, Vec3 at, float power) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(GaleDashRules.BLINK_GUST),
                target -> SpellTargets.canAffect(caster, target) && target.position().distanceTo(at) <= GaleDashRules.BLINK_GUST)) {
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.WIND, caster, caster), GaleDashRules.BLINK_DAMAGE * power);
            Vec3 away = target.position().subtract(at).normalize();
            target.knockback(1.0, -away.x, -away.z);
            target.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        play(level, at, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1f, 1.2f);
    }

    /** Hurricane Rush: hold the cast key to rush the way you look for up to 2 seconds, hitting what you pass. */
    public static SpellHold rush(CastContext context) {
        ServerPlayer caster = context.caster();
        Dash dash = begin(context, context.look().normalize(), true);
        MagicAttachments.get(caster).setFallImmune(true);
        play(context.level(), caster.position(), SoundEvents.WIND_CHARGE_BURST.value(), 1f, 0.8f);
        return new SpellHold() {
            @Override
            public boolean tick(int heldTicks) {
                if (!caster.isAlive() || DASHES.get(caster.getUUID()) != dash) {
                    return false;
                }
                Vec3 look = caster.getLookAngle();
                caster.setDeltaMovement(look.scale(GaleDashRules.RUSH_SPEED).add(0, caster.onGround() ? 0.2 : 0.04, 0));
                caster.hurtMarked = true;
                caster.resetFallDistance();
                dash.end = caster.level().getGameTime() + 2;
                if (heldTicks % 10 == 0) {
                    play((ServerLevel) caster.level(), caster.position(), SoundEvents.BREEZE_WHIRL, 1f, 1.4f);
                }
                return true;
            }

            @Override
            public void release(int heldTicks) {
                dash.end = caster.level().getGameTime();
            }

            @Override
            public void cancel() {
                dash.end = caster.level().getGameTime();
            }

            @Override
            public int maxHoldTicks() {
                return GaleDashRules.RUSH_TICKS;
            }
        };
    }

    private static Dash begin(CastContext context, Vec3 direction, boolean rush) {
        ServerPlayer caster = context.caster();
        Dash dash = new Dash(context, direction, rush);
        DASHES.put(caster.getUUID(), dash);
        if (GaleDashRules.PHANTOM_STEP.equals(dash.branch5)) {
            vanish(context.level(), caster);
        }
        return dash;
    }

    /** Phantom Step: unseen for a second, and whatever was hunting the dasher loses them. */
    private static void vanish(ServerLevel level, ServerPlayer caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, GaleDashRules.PHANTOM_TICKS, 0, false, false, true));
        UNSEEN_UNTIL.put(caster.getUUID(), level.getGameTime() + GaleDashRules.PHANTOM_TICKS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(32), mob -> mob.getTarget() == caster)) {
            mob.setTarget(null);
        }
        level.sendParticles(ParticleTypes.POOF, caster.getX(), caster.getY() + 1, caster.getZ(), 16, 0.3, 0.5, 0.3, 0.03);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Dash dash = DASHES.get(player.getUUID());
        if (dash == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now > dash.end) {
            finish(player, dash);
            DASHES.remove(player.getUUID());
            return;
        }
        // The wind it leaves behind.
        Vec3 back = dash.direction.scale(-0.15);
        level.sendParticles(ModContent.WIND_STREAK.get(), player.getX(), player.getY() + 0.9, player.getZ(), 0, back.x, back.y, back.z, 1);
        level.sendParticles(ModContent.WIND_STREAK.get(), player.getX(), player.getY() + 0.4, player.getZ(), 2, 0.2, 0.3, 0.2, 0.02);
        boolean strikes = dash.rush || GaleDashRules.GALE_STRIKE.equals(dash.branch5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(GaleDashRules.REACH),
                target -> target != player && target.isAlive())) {
            if (!SpellTargets.canAffect(player, target)) {
                // Slipstream Trail: allies it passes are quickened.
                if (GaleDashRules.slipstream(dash.level) && target instanceof Player) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
                }
                continue;
            }
            if (strikes && dash.struck.add(target.getId())) {
                float damage = (dash.rush ? GaleDashRules.RUSH_DAMAGE : GaleDashRules.STRIKE_DAMAGE) * dash.power;
                SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.WIND, player, player), damage);
                ElementalReactions.launchAirborne(target, 0.6 * grip(target));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0, 0, 0, 0);
                play(level, target.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
            } else if (GaleDashRules.slipstream(dash.level)) {
                // Slipstream Trail: shoved aside, off the dasher's line.
                Vec3 off = target.position().subtract(player.position()).multiply(1, 0, 1);
                Vec3 side = new Vec3(-dash.direction.z, 0, dash.direction.x);
                Vec3 shove = side.scale(Math.signum(off.dot(side)) == 0 ? 1 : Math.signum(off.dot(side))).scale(0.6 * grip(target));
                target.setDeltaMovement(target.getDeltaMovement().add(shove.x, 0.15 * grip(target), shove.z));
                target.hurtMarked = true;
            }
            if (GaleDashRules.swirls(dash.level) && ElementalReactions.auraOf(target) != null && dash.swirled.add(target.getId())) {
                ElementalReactions.swirl(target, player, dash.power);
            }
        }
    }

    /** What's left of a dash when it ends: Tailwind's speed, Featherfall's drift. */
    private static void finish(ServerPlayer player, Dash dash) {
        if (GaleDashRules.tailwind(dash.level)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
        }
        if (GaleDashRules.featherfall(dash.level) && !player.onGround()) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0));
        }
    }

    private static double grip(LivingEntity target) {
        return Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Air Step (level 3): no harm comes to a dasher while the dash lasts (a command's kill still does). */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        Dash dash = DASHES.get(player.getUUID());
        if (dash != null && GaleDashRules.airStep(dash.level)
                && player.level().getGameTime() <= Math.max(dash.end, dash.start + GaleDashRules.GUARD_TICKS)) {
            event.setCanceled(true);
        }
    }

    /** Phantom Step: while unseen, nothing new takes the dasher for its target. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) {
            Long until = UNSEEN_UNTIL.get(player.getUUID());
            if (until != null) {
                if (player.level().getGameTime() <= until) {
                    event.setCanceled(true);
                } else {
                    UNSEEN_UNTIL.remove(player.getUUID());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DASHES.remove(event.getEntity().getUUID());
        UNSEEN_UNTIL.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DASHES.clear();
        UNSEEN_UNTIL.clear();
    }
}
