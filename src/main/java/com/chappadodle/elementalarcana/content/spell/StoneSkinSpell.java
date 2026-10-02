package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.ShieldSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellShield;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Earth's defensive spell, levels 1-10 (docs/superpowers/specs/2026-10-01-earth-design.md): stones
 * orbit you and soak up damage, then fly outward when it breaks.
 *
 * <pre>
 * Lv1 Stone Skin     absorbs 8 for 10s      Lv6  Shared Stone    allies nearby get a half shield
 * Lv2 Thicker Stone  absorbs 14 for 12s     Lv7  Rockburst       breaking it deals damage
 * Lv3 Thorns         melee attackers hurt   Lv8  Bedrock Skin    regrows; no fall/suffocation damage
 * Lv4 Brace          raise it just before   Lv9  Stonefist       3 hits = attacker rooted
 *                    a hit: perfect block   Lv10 capstone: Fortress | Titan
 * Lv5 branch:        Bastion | Crystal Carapace
 * </pre>
 */
public class StoneSkinSpell extends Spell implements ShieldSpell {
    public static final String BASTION = "bastion";
    public static final String CARAPACE = "carapace";
    public static final String FORTRESS = "fortress";
    public static final String TITAN = "titan";

    private static final ResourceLocation SHARD_MODEL = ElementalArcana.id("spell/stone_shard");
    private static final ResourceLocation BASTION_ARMOR = ElementalArcana.id("stone_bastion_armor");
    private static final ResourceLocation BASTION_KNOCKBACK = ElementalArcana.id("stone_bastion_knockback");
    private static final ResourceLocation TITAN_KNOCKBACK = ElementalArcana.id("stone_titan_knockback");
    private static final ResourceLocation TITAN_DAMAGE = ElementalArcana.id("stone_titan_damage");
    private static final int PARRY_WINDOW_TICKS = 6;
    private static final double ALLY_RADIUS = 5.0;
    private static final double DOME_RADIUS = 3.5;
    private static final int TITAN_TICKS = 300;

    public StoneSkinSpell() {
        super(ModSchools.EARTH, 25, 400);
    }

    // ---- levels ----

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(BASTION, CARAPACE);
            case 10 -> List.of(FORTRESS, TITAN);
            default -> List.of();
        };
    }

    private static float strength(int level) {
        return level >= 2 ? 14f : 8f;
    }

    private static int duration(int level) {
        return level >= 2 ? 240 : 200;
    }

    // ---- casting ----

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        int level = context.spellLevel();
        boolean titan = context.hasBranch(10, TITAN);
        float strength = strength(level) * context.power() * (context.hasBranch(5, BASTION) ? 0.6f : 1f);
        SpellShield.raise(caster, this, strength, titan ? TITAN_TICKS : duration(level), level, branchesOf(context));

        if (level >= 6) {
            // Shared Stone: nearby players get half a shield (with this caster's level and paths).
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
        if (shield.hasBranch(5, BASTION)) {
            addModifier(player.getAttribute(Attributes.ARMOR), BASTION_ARMOR, 6.0, AttributeModifier.Operation.ADD_VALUE);
            addModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), BASTION_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE);
        }
        if (shield.hasBranch(10, TITAN)) {
            // Titan: +4 hearts, +50% melee damage and no knockback while the shield lasts.
            addModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), TITAN_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE);
            addModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), TITAN_DAMAGE, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, TITAN_TICKS, 1, false, false, true));
            player.heal(8f);
        }
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 24; i++) {
            float angle = i * Mth.TWO_PI / 24;
            level.sendParticles(rubble(), player.getX() + Mth.cos(angle) * 1.1, player.getY(0.4),
                    player.getZ() + Mth.sin(angle) * 1.1, 2, 0.1, 0.3, 0.1, 0.02);
        }
        playAt(player, SoundEvents.STONE_PLACE, 1.2f, 0.7f);
        playAt(player, SoundEvents.GRAVEL_PLACE, 1f, 0.8f);
    }

    private static void addModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (attribute != null && !attribute.hasModifier(id)) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }

    @Override
    public void onShieldEnd(ServerPlayer player, SpellShield shield, boolean broken) {
        removeModifier(player.getAttribute(Attributes.ARMOR), BASTION_ARMOR);
        removeModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), BASTION_KNOCKBACK);
        removeModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), TITAN_KNOCKBACK);
        removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), TITAN_DAMAGE);
        if (shield.hasBranch(10, TITAN)) {
            player.removeEffect(MobEffects.HEALTH_BOOST);
        }
        ServerLevel level = player.serverLevel();
        if (!broken) {
            level.sendParticles(rubble(), player.getX(), player.getY(0.5), player.getZ(), 14, 0.6, 0.5, 0.6, 0.03);
            playAt(player, SoundEvents.GRAVEL_BREAK, 0.8f, 0.9f);
            return;
        }
        // Burst: the fragments shove everything close back; from Lv7 (Rockburst) they also cut.
        double radius = shield.level() >= 7 ? 4.0 : 3.0;
        float damage = shield.level() >= 7 ? 5f : 0f;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && !(e instanceof Player) && e.isAlive() && e.distanceTo(player) <= radius)) {
            Vec3 away = nearby.position().subtract(player.position()).normalize();
            nearby.knockback(1.0, -away.x, -away.z);
            nearby.hurtMarked = true;
            if (damage > 0) {
                SpellDamage.hurtMultiHit(nearby, SpellDamage.source(player.level(), Element.EARTH, player, player), damage);
            }
        }
        level.sendParticles(rubble(), player.getX(), player.getY(0.6), player.getZ(), damage > 0 ? 50 : 30, 0.4, 0.4, 0.4, 0.25);
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY(0.4), player.getZ(), 6, 0.6, 0.3, 0.6, 0.02);
        playAt(player, SoundEvents.STONE_BREAK, 1.4f, 0.7f);
    }

    // ---- hits ----

    @Override
    public boolean blocksHit(ServerPlayer player, SpellShield shield, DamageSource source, float amount) {
        long now = player.level().getGameTime();
        if (shield.level() >= 4 && now - shield.raisedAt() <= PARRY_WINDOW_TICKS) {
            brace(player, source, amount);
            return true;
        }
        // Bedrock Skin: no fall or suffocation damage while it's up.
        return shield.level() >= 8 && (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL));
    }

    /** Brace: no damage and no knockback; the attacker takes the hit instead and is thrown back. */
    private static void brace(ServerPlayer player, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
            Vec3 away = attacker.position().subtract(player.position()).normalize();
            attacker.knockback(1.2, -away.x, -away.z);
            attacker.hurtMarked = true;
            SpellDamage.hurtMultiHit(attacker, SpellDamage.source(player.level(), Element.EARTH, player, player), amount);
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(rubble(), player.getX(), player.getY(0.6), player.getZ(), 26, 0.5, 0.5, 0.5, 0.2);
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY(0.6), player.getZ(), 1, 0, 0, 0, 0);
        playAt(player, SoundEvents.ANVIL_LAND, 1.0f, 1.2f);
        player.displayClientMessage(Component.translatable("message.elementalarcana.stone_skin.brace").withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public void onShieldHit(ServerPlayer player, SpellShield shield, DamageSource source, float absorbed) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(rubble(), player.getX(), player.getY(0.6), player.getZ(), 8, 0.5, 0.4, 0.5, 0.08);
        playAt(player, SoundEvents.STONE_HIT, 1f, 0.8f);

        // Only melee attackers (the attacker is also what directly hit you) get hurt back or counted.
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker || attacker == player) {
            return;
        }
        if (shield.level() >= 3) {
            // Thorns.
            SpellDamage.hurtMultiHit(attacker, SpellDamage.source(player.level(), Element.EARTH, player, player), 2f + absorbed * 0.3f);
            if (shield.hasBranch(5, CARAPACE)) {
                // Crystal Carapace: thorns Crystallize whatever burns, soaks or freezes the attacker.
                ElementalReactions.earthHit(attacker, player, 200);
            }
        }
        if (shield.level() >= 9) {
            // Stonefist: the third hit from the same attacker roots it for 2 seconds.
            int hits = shield.hitsByAttacker().merge(attacker.getId(), 1, Integer::sum);
            if (hits >= 3) {
                shield.hitsByAttacker().remove(attacker.getId());
                BoulderSpell.stun(attacker, 40);
                level.sendParticles(rubble(), attacker.getX(), attacker.getY(0.2), attacker.getZ(), 20, 0.4, 0.2, 0.4, 0.1);
                playAt(attacker, SoundEvents.STONE_BREAK, 1f, 0.6f);
            }
        }
    }

    // ---- while up ----

    @Override
    public void onShieldTick(ServerPlayer player, SpellShield shield) {
        if (shield.level() >= 8 && shield.amount() < shield.max()) {
            // Bedrock Skin: the stone slowly regrows.
            shield.setAmount(shield.amount() + 0.5f / 20f);
            if (player.tickCount % 20 == 0) {
                SpellShield.sync(player);
            }
        }
        if (shield.hasBranch(10, FORTRESS)) {
            fortress(player);
        }
    }

    /** Fortress: a dome no projectile passes and no mob can push into. */
    private void fortress(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(DOME_RADIUS), e -> e.distanceToSqr(center) <= DOME_RADIUS * DOME_RADIUS)) {
            if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() != player) {
                level.sendParticles(rubble(), projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
                playAt(projectile, SoundEvents.STONE_HIT, 0.8f, 0.9f);
                projectile.discard();
            } else if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                Vec3 away = living.position().subtract(center).normalize();
                living.setDeltaMovement(away.x * 0.5, 0.15, away.z * 0.5);
                living.hurtMarked = true;
            }
        }
        if (player.tickCount % 4 == 0) {
            for (int i = 0; i < 14; i++) {
                double theta = player.getRandom().nextDouble() * Mth.TWO_PI;
                double y = player.getRandom().nextDouble();
                double ring = Math.sqrt(1 - y * y);
                level.sendParticles(rubble(), center.x + Math.cos(theta) * ring * DOME_RADIUS,
                        center.y + y * DOME_RADIUS, center.z + Math.sin(theta) * ring * DOME_RADIUS, 1, 0, 0, 0, 0);
            }
        }
    }

    // ---- visuals ----

    @Override
    public ResourceLocation shardModel() {
        return SHARD_MODEL;
    }

    private static ParticleOptions rubble() {
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
    }

    /** A few crumbs drifting around you; also what you see of your own shield in first person. */
    @Override
    public void shieldParticles(Player player, SpellShield shield) {
        if (player.tickCount % 4 != 0) {
            return;
        }
        float angle = player.tickCount * 0.2f;
        double radius = 0.9;
        player.level().addParticle(rubble(),
                player.getX() + Mth.cos(angle) * radius, player.getY(0.4) + player.getRandom().nextDouble() * 0.6,
                player.getZ() + Mth.sin(angle) * radius, 0, 0.01, 0);
    }

    private static void playAt(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
