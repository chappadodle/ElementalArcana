package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ShieldSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellShield;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ice's defensive spell, levels 1-10: a shell of ice shards orbits you and soaks up damage,
 * then shatters outward when it breaks.
 *
 * <pre>
 * Lv1 Frost Shield    absorbs 6 for 8s       Lv6  Shared Winter      allies nearby get a half shield
 * Lv2 Thicker Ice     absorbs 10 for 10s     Lv7  Shatterstorm       breaking it deals damage
 * Lv3 Cold Touch      melee attackers chilled Lv8 Permafrost         regrows; fire/freeze immunity
 * Lv4 Ice Parry       raise it just before    Lv9  Frozen Retaliation 3 hits = attacker Frozen
 *                     a hit: perfect block    Lv10 capstone: Absolute Zero | Frozen Sanctuary
 * Lv5 branch:         Mirror Frost | Frost Mantle
 * </pre>
 */
public class FrostShieldSpell extends Spell implements ShieldSpell {
    public static final String MIRROR = "mirror";
    public static final String MANTLE = "mantle";
    public static final String ABSOLUTE_ZERO = "absolute_zero";
    public static final String SANCTUARY = "sanctuary";

    private static final ResourceLocation SHARD_MODEL = ElementalArcana.id("spell/icicle");
    private static final ResourceLocation MANTLE_ARMOR = ElementalArcana.id("frost_mantle_armor");
    private static final ResourceLocation MANTLE_KNOCKBACK = ElementalArcana.id("frost_mantle_knockback");
    private static final int PARRY_WINDOW_TICKS = 6;
    private static final double ALLY_RADIUS = 5.0;
    private static final double DOME_RADIUS = 3.5;
    private static final long SANCTUARY_COOLDOWN = 5 * 60 * 20;
    private static final String TAG_SANCTUARY_READY = "ea_frost_sanctuary_ready";

    public FrostShieldSpell() {
        super(ModSchools.ICE, 25, 400, 3);
    }

    // ---- levels ----

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(MIRROR, MANTLE);
            case 10 -> List.of(ABSOLUTE_ZERO, SANCTUARY);
            default -> List.of();
        };
    }

    private static float strength(int level) {
        return level >= 2 ? 10f : 6f;
    }

    private static int duration(int level) {
        return level >= 2 ? 200 : 160;
    }

    // ---- casting ----

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        int level = context.spellLevel();
        float strength = strength(level) * context.power() * (context.hasBranch(5, MANTLE) ? 0.6f : 1f);
        SpellShield.raise(caster, this, strength, duration(level), level, branchesOf(context));

        if (level >= 6) {
            // Shared Winter: nearby players get half a shield (with this caster's level and paths).
            for (Player ally : context.level().getEntitiesOfClass(Player.class, caster.getBoundingBox().inflate(ALLY_RADIUS),
                    p -> p != caster && p.isAlive() && p.distanceTo(caster) <= ALLY_RADIUS)) {
                if (ally instanceof ServerPlayer serverAlly && SpellShield.of(serverAlly).amount() < strength * 0.5f) {
                    SpellShield.raise(serverAlly, this, strength * 0.5f, duration(level), level, branchesOf(context));
                }
            }
        }
        return CastResult.SUCCESS;
    }

    private static Map<Integer, String> branchesOf(CastContext context) {
        Map<Integer, String> branches = new HashMap<>();
        for (int level : new int[]{5, 10}) {
            String branch = context.branch(level);
            if (branch != null) {
                branches.put(level, branch);
            }
        }
        return branches;
    }

    // ---- shield lifecycle ----

    @Override
    public void onShieldRaised(ServerPlayer player, SpellShield shield) {
        if (shield.hasBranch(5, MANTLE)) {
            addModifier(player.getAttribute(Attributes.ARMOR), MANTLE_ARMOR, 6.0);
            addModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), MANTLE_KNOCKBACK, 1.0);
        }
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 24; i++) {
            float angle = i * Mth.TWO_PI / 24;
            level.sendParticles(ModContent.FROST_SPARKLE.get(), player.getX() + Mth.cos(angle) * 1.1, player.getY(0.5),
                    player.getZ() + Mth.sin(angle) * 1.1, 1, 0, 0.3, 0, 0.02);
        }
        level.sendParticles(ModContent.FROST_MIST.get(), player.getX(), player.getY(0.3), player.getZ(), 6, 0.5, 0.2, 0.5, 0.01);
        playAt(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.4f);
        playAt(player, SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 1.1f);
    }

    private static void addModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute != null && !attribute.hasModifier(id)) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void onShieldEnd(ServerPlayer player, SpellShield shield, boolean broken) {
        removeModifier(player.getAttribute(Attributes.ARMOR), MANTLE_ARMOR);
        removeModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), MANTLE_KNOCKBACK);
        ServerLevel level = player.serverLevel();
        if (!broken) {
            // It just melts away.
            level.sendParticles(ModContent.FROST_SPARKLE.get(), player.getX(), player.getY(0.5), player.getZ(), 12, 0.6, 0.5, 0.6, 0.02);
            playAt(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
            return;
        }
        // Shatter: slows everything close; from Lv7 (Shatterstorm) the shards also cut.
        double radius = shield.level() >= 7 ? 4.0 : 3.0;
        float damage = shield.level() >= 7 ? 5f : 0f;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && !(e instanceof Player) && e.isAlive() && e.distanceTo(player) <= radius)) {
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            if (damage > 0) {
                SpellDamage.hurtMultiHit(nearby, SpellDamage.source(player.level(), Element.ICE, player, player), damage);
            }
        }
        level.sendParticles(ModContent.ICE_SHARD.get(), player.getX(), player.getY(0.6), player.getZ(), damage > 0 ? 40 : 24, 0.4, 0.4, 0.4, 0.35);
        level.sendParticles(ModContent.FROST_MIST.get(), player.getX(), player.getY(0.4), player.getZ(), 8, 0.8, 0.3, 0.8, 0.02);
        level.sendParticles(ModContent.FROST_SPARKLE.get(), player.getX(), player.getY(0.6), player.getZ(), 20, 0.6, 0.5, 0.6, 0.2);
        playAt(player, ModContent.ICICLE_IMPACT.get(), 1.3f, 0.9f);
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }

    // ---- hits ----

    @Override
    public boolean blocksHit(ServerPlayer player, SpellShield shield, DamageSource source, float amount) {
        long now = player.level().getGameTime();
        if (shield.level() >= 4 && now - shield.raisedAt() <= PARRY_WINDOW_TICKS) {
            perfectBlock(player, source);
            return true;
        }
        // Permafrost: while shielded, fire and cold can't touch you.
        return shield.level() >= 8 && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FREEZING));
    }

    /** Ice Parry: no damage, and the attacker is thrown back and frozen for a moment. */
    private static void perfectBlock(ServerPlayer player, DamageSource source) {
        if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
            Vec3 away = attacker.position().subtract(player.position()).normalize();
            attacker.knockback(1.2, -away.x, -away.z);
            attacker.hurtMarked = true;
            attacker.addEffect(new MobEffectInstance(ModContent.FROZEN, 20));
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ModContent.FROST_SPARKLE.get(), player.getX(), player.getY(0.6), player.getZ(), 30, 0.5, 0.5, 0.5, 0.25);
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY(0.6), player.getZ(), 1, 0, 0, 0, 0);
        playAt(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 2.0f);
        playAt(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.8f);
        player.displayClientMessage(Component.translatable("message.elementalarcana.frost_shield.parry").withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    public void onShieldHit(ServerPlayer player, SpellShield shield, DamageSource source, float absorbed) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(ModContent.FROST_SPARKLE.get(), player.getX(), player.getY(0.6), player.getZ(), 8, 0.5, 0.4, 0.5, 0.1);
        playAt(player, SoundEvents.GLASS_HIT, 1f, 1.4f);

        // Only melee attackers (the attacker is also what directly hit you) get frostbitten or counted.
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker || attacker == player) {
            return;
        }
        if (shield.level() >= 3) {
            // Cold Touch.
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            if (attacker.canFreeze()) {
                attacker.setTicksFrozen(Math.max(attacker.getTicksFrozen(), attacker.getTicksRequiredToFreeze() + 40));
            }
        }
        if (shield.level() >= 9) {
            // Frozen Retaliation: the third hit from the same attacker freezes it solid.
            int hits = shield.hitsByAttacker().merge(attacker.getId(), 1, Integer::sum);
            if (hits >= 3) {
                shield.hitsByAttacker().remove(attacker.getId());
                attacker.addEffect(new MobEffectInstance(ModContent.FROZEN, 40));
                level.sendParticles(ModContent.ICE_SHARD.get(), attacker.getX(), attacker.getY(0.5), attacker.getZ(), 16, 0.3, 0.4, 0.3, 0.08);
                playAt(attacker, SoundEvents.GLASS_PLACE, 1f, 0.6f);
            }
        }
    }

    /** Mirror Frost: projectiles bounce off, flying back at whoever fired them. */
    @Override
    public boolean onProjectile(ServerPlayer player, SpellShield shield, Projectile projectile) {
        if (!shield.hasBranch(5, MIRROR) || projectile.getOwner() == player) {
            return false;
        }
        reflect(player, projectile);
        return true;
    }

    private static void reflect(ServerPlayer player, Projectile projectile) {
        Entity shooter = projectile.getOwner();
        double speed = Math.max(0.8, projectile.getDeltaMovement().length());
        Vec3 direction = shooter != null
                ? shooter.getBoundingBox().getCenter().subtract(projectile.position()).normalize()
                : projectile.getDeltaMovement().scale(-1).normalize();
        projectile.setDeltaMovement(direction.scale(speed));
        projectile.setOwner(player);
        projectile.hasImpulse = true;
        player.serverLevel().sendParticles(ModContent.FROST_SPARKLE.get(), projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.1, 0.1, 0.1, 0.1);
        playAt(projectile, SoundEvents.AMETHYST_BLOCK_HIT, 1.2f, 1.5f);
    }

    /** Frost Mantle: your own melee hits chill what they strike. */
    @Override
    public void onOwnerAttack(ServerPlayer player, SpellShield shield, LivingEntity target) {
        if (shield.hasBranch(5, MANTLE)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            if (target.canFreeze()) {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 20));
            }
            player.serverLevel().sendParticles(ModContent.FROST_SPARKLE.get(), target.getX(), target.getY(0.6), target.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
        }
    }

    // ---- while up ----

    @Override
    public void onShieldTick(ServerPlayer player, SpellShield shield) {
        if (shield.level() >= 8) {
            // Permafrost: the ice slowly regrows, and keeps you from burning or freezing.
            if (shield.amount() < shield.max()) {
                shield.setAmount(shield.amount() + 0.5f / 20f);
                if (player.tickCount % 20 == 0) {
                    SpellShield.sync(player);
                }
            }
            player.clearFire();
            player.setTicksFrozen(0);
        }
        if (shield.hasBranch(10, ABSOLUTE_ZERO)) {
            absoluteZero(player);
        }
    }

    /** Absolute Zero: a dome no projectile passes and no mob can push into. */
    private void absoluteZero(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(DOME_RADIUS), e -> e.distanceToSqr(center) <= DOME_RADIUS * DOME_RADIUS)) {
            if (entity instanceof Projectile projectile && projectile.getOwner() != player) {
                level.sendParticles(ModContent.FROST_SPARKLE.get(), projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
                playAt(projectile, SoundEvents.AMETHYST_BLOCK_HIT, 0.8f, 1.6f);
                projectile.discard();
            } else if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                Vec3 away = living.position().subtract(center).normalize();
                living.setDeltaMovement(away.x * 0.5, 0.15, away.z * 0.5);
                living.hurtMarked = true;
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
        }
        if (player.tickCount % 4 == 0) {
            // Draw the dome as scattered glints on a hemisphere.
            for (int i = 0; i < 14; i++) {
                double theta = player.getRandom().nextDouble() * Mth.TWO_PI;
                double y = player.getRandom().nextDouble();
                double ring = Math.sqrt(1 - y * y);
                level.sendParticles(ModContent.FROST_SPARKLE.get(), center.x + Math.cos(theta) * ring * DOME_RADIUS,
                        center.y + y * DOME_RADIUS, center.z + Math.sin(theta) * ring * DOME_RADIUS, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Frozen Sanctuary: a killing blow instead encases you in ice while you heal. Once every 5 minutes. */
    @Override
    public boolean preventDeath(ServerPlayer player, SpellShield shield) {
        long now = player.level().getGameTime();
        if (!shield.hasBranch(10, SANCTUARY) || now < player.getPersistentData().getLong(TAG_SANCTUARY_READY)) {
            return false;
        }
        player.getPersistentData().putLong(TAG_SANCTUARY_READY, now + SANCTUARY_COOLDOWN);
        player.setHealth(1f);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 2));
        player.addEffect(new MobEffectInstance(ModContent.FROZEN, 60));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ModContent.ICE_SHARD.get(), player.getX(), player.getY(0.5), player.getZ(), 40, 0.4, 0.6, 0.4, 0.15);
        level.sendParticles(ModContent.FROST_MIST.get(), player.getX(), player.getY(0.5), player.getZ(), 12, 0.5, 0.6, 0.5, 0.02);
        playAt(player, SoundEvents.GLASS_PLACE, 1.5f, 0.5f);
        playAt(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.7f);
        player.displayClientMessage(Component.translatable("message.elementalarcana.frost_shield.sanctuary").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    // ---- visuals ----

    @Override
    public ResourceLocation shardModel() {
        return SHARD_MODEL;
    }

    /** A few glints drifting around you; also what you see of your own shield in first person. */
    @Override
    public void shieldParticles(Player player, SpellShield shield) {
        if (player.tickCount % 3 != 0) {
            return;
        }
        float angle = player.tickCount * 0.25f;
        double radius = 0.9;
        player.level().addParticle(ModContent.FROST_SPARKLE.get(),
                player.getX() + Mth.cos(angle) * radius, player.getY(0.4) + player.getRandom().nextDouble() * 0.6,
                player.getZ() + Mth.sin(angle) * radius, 0, 0.01, 0);
    }

    private static void playAt(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
