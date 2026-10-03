package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SkywardRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.api.event.SpellCastEvent;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.content.WindVortex;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Skyward Leap on the server (see SkywardLeapSpell and the Skyward Leap spec; client/Gliding
 * steers the glide itself, since a player's movement is their client's). Here: the leap and its
 * gust, and while a player is Skyward (a mob effect): Rising Current's updraft, safe landings, the
 * plunge (a player who sneaks while Skyward and in the air is plunging; where they land the
 * shockwave goes off), Wind Rider's flight (NeoForge's creative-flight attribute, so a server never
 * kicks them for flying), Aerial Barrage's cheaper wind spells, and Endless Sky (Skyward ends when
 * they land; a Wind Blade thrown while gliding lifts them). What each player's leap was is kept
 * while they're Skyward; never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class SkywardLeaps {
    private static final ResourceLocation WIND_RIDER = ElementalArcana.id("wind_rider");
    private static final int WIND_COLOR = 0xCFF5E4;
    private static final int WIND_FADE = 0x7FC8A8;

    /** A Skyward player's leap: its level and branches, the updraft spent or not, a plunge under way, Wind Rider's flight. */
    private static final class Leap {
        final int level;
        final float power;
        @Nullable
        final String branch5;
        @Nullable
        final String branch10;
        boolean updraftUsed;
        boolean leftGround;
        @Nullable
        Double plungeFrom;
        long riderUntil;

        Leap(int level, float power, @Nullable String branch5, @Nullable String branch10) {
            this.level = level;
            this.power = power;
            this.branch5 = branch5;
            this.branch10 = branch10;
        }

        boolean endless() {
            return level >= 10 && SkywardRules.ENDLESS_SKY.equals(branch10);
        }
    }

    private static final Map<UUID, Leap> LEAPS = new HashMap<>();

    private SkywardLeaps() {
    }

    /** The leap: up, Skyward, and a gust that throws whatever the caster may hurt nearby away and up. */
    public static void leap(CastContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        int spellLevel = context.spellLevel();
        Leap leap = new Leap(spellLevel, context.power(), spellLevel >= 5 ? context.branch(5) : null,
                spellLevel >= 10 ? context.branch(10) : null);
        stopRiding(caster);
        LEAPS.put(caster.getUUID(), leap);

        Vec3 motion = caster.getKnownMovement();
        caster.setDeltaMovement(motion.x * 0.5, SkywardRules.leapSpeed(spellLevel), motion.z * 0.5);
        caster.hurtMarked = true;
        caster.resetFallDistance();
        MagicAttachments.get(caster).setFallImmune(true);
        caster.addEffect(new MobEffectInstance(ModContent.SKYWARD, SkywardRules.skywardTicks(spellLevel, leap.branch10), 0, false, false, true));
        if (SkywardRules.WIND_RIDER.equals(leap.branch5)) {
            startRiding(caster, leap);
        }

        double reach = SkywardRules.gustReach(spellLevel);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(reach),
                target -> SpellTargets.canAffect(caster, target) && target.distanceTo(caster) <= reach)) {
            Vec3 away = target.position().subtract(caster.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
            double hold = 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            target.setDeltaMovement(away.scale(0.6 * hold));
            ElementalReactions.launchAirborne(target, SkywardRules.gustLift(spellLevel) * hold);
        }

        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, caster.getX(), caster.getY(), caster.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.1, caster.getZ(), 24, reach * 0.3, 0.05, reach * 0.3, 0.08);
        for (int height = 0; height < 8; height++) {
            level.sendParticles(ModContent.WIND_STREAK.get(), caster.getX(), caster.getY() + height * 0.8, caster.getZ(), 3, 0.35, 0.3, 0.35, 0.05);
        }
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.2f, 0.9f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1f, 0.8f);
    }

    /** Whether {@code player} has an updraft to catch (Rising Current, from level 3): Skyward, in the air, not yet used. */
    public static boolean hasUpdraft(ServerPlayer player) {
        Leap leap = LEAPS.get(player.getUUID());
        return leap != null && leap.level >= 3 && !leap.updraftUsed && player.hasEffect(ModContent.SKYWARD)
                && !player.onGround() && !player.isInWater() && !player.getAbilities().flying;
    }

    /** Rising Current: a gust from below lifts the glider, keeping their way. */
    public static void risingCurrent(ServerPlayer player) {
        Leap leap = LEAPS.get(player.getUUID());
        if (leap == null) {
            return;
        }
        leap.updraftUsed = true;
        leap.plungeFrom = null;
        lift(player, SkywardRules.RISING_CURRENT, true);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY() - 0.5, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1f, 1.2f);
    }

    /** Endless Sky: a Wind Blade thrown while gliding lifts its thrower a little. */
    public static void bladeThrown(ServerPlayer caster) {
        Leap leap = LEAPS.get(caster.getUUID());
        if (leap != null && leap.endless() && gliding(caster)) {
            lift(caster, SkywardRules.BLADE_LIFT, false);
        }
    }

    /** Throws {@code player} up at {@code speed}, keeping the way they were going (as the server last saw it). */
    private static void lift(ServerPlayer player, double speed, boolean replace) {
        Vec3 motion = player.getKnownMovement();
        double up = replace ? speed : Math.max(motion.y, 0) + speed;
        player.setDeltaMovement(motion.x, up, motion.z);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    /** Whether {@code player} is gliding (their client says so; see GlidePayload). */
    public static boolean gliding(ServerPlayer player) {
        return player.getData(MagicAttachments.GLIDING);
    }

    /** The client's word that {@code player} started or stopped gliding: only a Skyward player in the air glides. */
    public static void setGliding(ServerPlayer player, boolean gliding) {
        boolean allowed = gliding && player.hasEffect(ModContent.SKYWARD) && !player.onGround();
        if (gliding(player) != allowed) {
            player.setData(MagicAttachments.GLIDING, allowed);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Leap leap = LEAPS.get(player.getUUID());
        boolean skyward = player.hasEffect(ModContent.SKYWARD);
        if (leap == null || !skyward) {
            if (leap != null) {
                end(player);
            }
            return;
        }
        long now = player.level().getGameTime();
        if (leap.riderUntil > 0 && now >= leap.riderUntil) {
            stopRiding(player);
            leap.riderUntil = 0;
        }
        boolean grounded = player.onGround() || player.isInWater();
        if (gliding(player)) {
            // A glide never ends in a fall: the distance starts again each tick it holds.
            player.resetFallDistance();
            if (grounded) {
                player.setData(MagicAttachments.GLIDING, false);
            }
        }
        if (!grounded) {
            leap.leftGround = true;
            if (leap.level >= 4 && leap.plungeFrom == null && player.isShiftKeyDown() && !player.getAbilities().flying) {
                leap.plungeFrom = player.getY();
                MagicAttachments.get(player).setFallImmune(true);
            }
            return;
        }
        if (leap.plungeFrom != null) {
            double height = leap.plungeFrom - player.getY();
            leap.plungeFrom = null;
            if (height >= SkywardRules.MIN_PLUNGE_HEIGHT && !player.isInWater()) {
                shockwave(player, leap, height);
            }
        }
        if (leap.leftGround && leap.endless()) {
            // Endless Sky lasts until you land.
            player.removeEffect(ModContent.SKYWARD);
            end(player);
        }
    }

    /** The plunge's landing: a shockwave that grows with the height fallen, throwing creatures back. */
    private static void shockwave(ServerPlayer player, Leap leap, double height) {
        ServerLevel level = player.serverLevel();
        double radius = SkywardRules.plungeRadius(height, leap.branch5, leap.branch10);
        float damage = SkywardRules.plungeDamage(height, leap.branch5) * leap.power;
        boolean skyfall = SkywardRules.SKYFALL.equals(leap.branch5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2, radius),
                target -> SpellTargets.canAffect(player, target) && target.distanceTo(player) <= radius + target.getBbWidth() / 2)) {
            double closeness = 1 - 0.5 * Math.min(1, target.distanceTo(player) / radius);
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.WIND, player, player), (float) (damage * closeness));
            Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
            target.knockback(0.8 + 0.4 * closeness, -away.x, -away.z);
            target.hurtMarked = true;
            if (skyfall) {
                ElementalReactions.launchAirborne(target, 0.7);
            }
            if (leap.level >= 9) {
                ElementalReactions.swirl(target, player, leap.power);
            }
        }
        if (SkywardRules.HEAVENS_DESCENT.equals(leap.branch10)) {
            WindVortex.spawn(level, player.position(), 5.0, 0.35, 60, player);
        }
        Vec3 at = player.position();
        level.sendParticles(radius > 6 ? ParticleTypes.GUST_EMITTER_LARGE : ParticleTypes.GUST_EMITTER_SMALL, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(GlowParticleOptions.of(ModContent.SHOCKWAVE.get(), WIND_COLOR, WIND_FADE, (float) radius, 10), at.x, at.y + 0.05, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.1, at.z, (int) (radius * 8), radius * 0.4, 0.05, radius * 0.4, 0.1);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2f, 0.9f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1f, 0.6f);
    }

    /** Wind Rider: free flight for the start of Skyward. */
    private static void startRiding(ServerPlayer player, Leap leap) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null) {
            return;
        }
        if (!flight.hasModifier(WIND_RIDER)) {
            flight.addTransientModifier(new AttributeModifier(WIND_RIDER, 1, AttributeModifier.Operation.ADD_VALUE));
        }
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        leap.riderUntil = player.level().getGameTime() + SkywardRules.WIND_RIDER_TICKS;
    }

    private static void stopRiding(ServerPlayer player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null || !flight.hasModifier(WIND_RIDER)) {
            return;
        }
        flight.removeModifier(WIND_RIDER);
        if (!player.mayFly()) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
        // Whoever stops flying in the air glides (or falls) from there, safely.
        player.resetFallDistance();
        MagicAttachments.get(player).setFallImmune(true);
    }

    private static void end(ServerPlayer player) {
        stopRiding(player);
        LEAPS.remove(player.getUUID());
        if (gliding(player)) {
            player.setData(MagicAttachments.GLIDING, false);
        }
    }

    /** Every landing is safe while Skyward from level 7 (Tailwind Glide). */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasEffect(ModContent.SKYWARD)) {
            Leap leap = LEAPS.get(player.getUUID());
            if (leap != null && SkywardRules.safeLandings(leap.level)) {
                event.setCanceled(true);
            }
        }
    }

    /** Aerial Barrage (level 8): wind spells cost less while Skyward and in the air. */
    @SubscribeEvent
    public static void onCast(SpellCastEvent.Pre event) {
        ServerPlayer caster = event.caster();
        Leap leap = LEAPS.get(caster.getUUID());
        if (leap != null && !caster.onGround() && caster.hasEffect(ModContent.SKYWARD) && event.spell().school() == ModSchools.WIND.get()) {
            event.setManaCost(Math.round(event.manaCost() * SkywardRules.barrageCost(leap.level)));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LEAPS.clear();
    }
}
