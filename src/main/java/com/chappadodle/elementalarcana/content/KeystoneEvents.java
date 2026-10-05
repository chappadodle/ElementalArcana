package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Keystones;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.spell.ChainLightnings;
import com.chappadodle.elementalarcana.content.spell.Dawnbreaks;
import com.chappadodle.elementalarcana.content.spell.PrismWards;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The keystones that live on game events (see Keystones for the rest): Gale Step's soft landings,
 * Refraction's projectiles turned back and its harder melee blows, Conductor's arcs, and Sunborn's
 * mending in bright light (and its light, which CastingService and Conjuring use for spell power).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class KeystoneEvents {
    /** How many projectiles have come at each Refraction holder (every third turns back). */
    private static final Map<Player, Integer> INCOMING = new WeakHashMap<>();
    /** Set while a Conductor arc lands, so an arc doesn't arc again. */
    private static boolean arcing;

    private KeystoneEvents() {
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && MagicAttachments.get(player).hasKeystone(Keystones.GALE_STEP)) {
            event.setDamageMultiplier(0f);
        }
    }

    /** Sunborn's multiplier on a player's spell power where they stand (1 without Sunborn). */
    public static float lightFactor(ServerPlayer player) {
        MagicData data = MagicAttachments.get(player);
        return data.hasKeystone(Keystones.SUNBORN) ? Keystones.lightFactor(data.keystones(), light(player)) : 1f;
    }

    /** The light a player stands in (a Dawnbreak's counts as full daylight). */
    private static int light(Player player) {
        BlockPos pos = player.blockPosition();
        return Dawnbreaks.shines(player.level(), pos) ? 15 : player.level().getMaxLocalRawBrightness(pos);
    }

    /** Refraction: every third projectile that would hit its holder turns back at its shooter. */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide() || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Player player) || projectile.getOwner() == player) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        if (!data.hasKeystone(Keystones.REFRACTION)) {
            return;
        }
        int count = INCOMING.merge(player, 1, Integer::sum);
        if (Keystones.refracts(data.keystones(), count)) {
            event.setCanceled(true);
            PrismWards.reflect(projectile, player);
        }
    }

    /** Refraction's price: melee blows hurt its holder more. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isMelee(event.getSource())) {
            event.setAmount(event.getAmount() * Keystones.meleeTakenFactor(MagicAttachments.get(player).keystones()));
        }
    }

    /** A blow struck by hand: a living attacker hitting directly, not a projectile, a blast or a spell. */
    private static boolean isMelee(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()
                && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION) && SpellDamage.elementOf(source) == null;
    }

    /** Conductor: a spell's hit arcs on to the nearest other foe for a share of its damage. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (arcing || event.getNewDamage() <= 0 || !(source.getEntity() instanceof ServerPlayer player)
                || SpellDamage.elementOf(source) == null || !MagicAttachments.get(player).hasKeystone(Keystones.CONDUCTOR)) {
            return;
        }
        LivingEntity struck = event.getEntity();
        ServerLevel level = player.serverLevel();
        LivingEntity next = level.getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox().inflate(Keystones.CONDUCTOR_REACH),
                        other -> other != struck && other != player && other.isAlive() && other.distanceTo(struck) <= Keystones.CONDUCTOR_REACH
                                && SpellTargets.canAffect(player, other))
                .stream()
                .min(Comparator.comparingDouble(other -> other.distanceToSqr(struck)))
                .orElse(null);
        if (next == null) {
            return;
        }
        arcing = true;
        try {
            SpellDamage.hurtMultiHit(next, SpellDamage.source(level, Element.LIGHTNING, player, player), event.getNewDamage() * Keystones.CONDUCTOR_SHARE);
        } finally {
            arcing = false;
        }
        ChainLightnings.bolt(level, struck.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), 0, 0.6f);
        level.playSound(null, next.getX(), next.getY(), next.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.3f, 2f);
    }

    /** Sunborn: in bright light its holder mends, a little at a time. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % Keystones.SUNBORN_MEND_TICKS == 0
                && player.isAlive() && player.getHealth() < player.getMaxHealth()
                && MagicAttachments.get(player).hasKeystone(Keystones.SUNBORN) && light(player) >= Keystones.SUNBORN_BRIGHT) {
            player.heal(Keystones.SUNBORN_MEND);
            player.serverLevel().sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), 0xFFE9A0, 0xFFB040, 0.3f, 12),
                    player.getX(), player.getY(1.0), player.getZ(), 4, 0.3, 0.4, 0.3, 0.01);
        }
    }
}
