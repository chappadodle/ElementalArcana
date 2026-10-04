package com.chappadodle.elementalarcana.content.infusion;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AffinityRules;
import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.InfusionRules;
import com.chappadodle.elementalarcana.api.Keystones;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ArcOptions;
import com.chappadodle.elementalarcana.content.ElementalMatchups;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What infusion does (see the Arcane Infusion spec): an infused weapon's hits (melee, and the
 * arrows and tridents it looses) become its element's, by the chart and with its reactions; infused
 * armour wards off its element family's harm, and four pieces of one element give its set boon.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Infusions {
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private Infusions() {
    }

    /** Infuses {@code stack} with {@code element}: the component, and its name in the element's colour. */
    public static void infuse(ItemStack stack, Element element) {
        stack.set(ModInfusion.INFUSION.get(), element);
        stack.set(DataComponents.ITEM_NAME, stack.getItem().getName(stack).copy().withColor(element.color()));
    }

    /** The infusions of {@code entity}'s four armour pieces (null for a piece that isn't infused). */
    public static List<Element> armourInfusions(LivingEntity entity) {
        List<Element> infusions = new ArrayList<>(ARMOUR.length);
        for (EquipmentSlot slot : ARMOUR) {
            infusions.add(ModInfusion.infusionOf(entity.getItemBySlot(slot)));
        }
        return infusions;
    }

    @Nullable
    private static Element setOf(LivingEntity entity) {
        return InfusionRules.setElement(armourInfusions(entity));
    }

    /** The infusion behind {@code source}: the weapon of a melee hit, or the bow or trident an arrow or trident came from. */
    @Nullable
    private static Element weaponInfusion(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof AbstractArrow arrow) {
            return ModInfusion.infusionOf(arrow.getWeaponItem());
        }
        if (direct instanceof LivingEntity attacker && direct == source.getEntity()
                && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))) {
            return ModInfusion.infusionOf(attacker.getMainHandItem());
        }
        return null;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        // The target's armour first: its ward and set boons.
        if (ward(event, target, source)) {
            return;
        }
        Element element = weaponInfusion(source);
        if (element != null) {
            event.setAmount(infusedHit(level, target, source.getEntity(), element, event.getAmount()));
        }
    }

    /** The target's infused armour against {@code source}. Returns true if it turned the harm away entirely. */
    private static boolean ward(LivingIncomingDamageEvent event, LivingEntity target, DamageSource source) {
        List<Element> infusions = armourInfusions(target);
        if (infusions.stream().allMatch(e -> e == null)) {
            return false;
        }
        Element set = InfusionRules.setElement(infusions);
        if (set == Element.FIRE && (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.HOT_FLOOR))
                || set == Element.ICE && source.is(DamageTypes.FREEZE)
                || set == Element.WIND && source.is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
            if (set == Element.FIRE) {
                target.clearFire();
            }
            return true;
        }
        if (set == Element.ICE && source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                && attacker.level() instanceof ServerLevel level) {
            // The Ice set chills whoever strikes its wearer.
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            level.sendParticles(ParticleTypes.SNOWFLAKE, attacker.getX(), attacker.getY(0.6), attacker.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
        }
        Element harm = SpellDamage.elementOf(source);
        if (harm == null) {
            harm = ElementalMatchups.natureOf(source);
        }
        if (harm != null) {
            event.setAmount(event.getAmount() * InfusionRules.wardFactor(infusions, harm));
        }
        return false;
    }

    /** An infused hit: the chart, the element landing, its reactions. Returns the hit's new strength. */
    private static float infusedHit(ServerLevel level, LivingEntity target, @Nullable Entity attacker, Element element, float amount) {
        float multiplier;
        if (target instanceof Player player) {
            MagicData data = MagicAttachments.get(player);
            multiplier = AffinityRules.damageTaken(data.affinityElements(), element) * Keystones.damageTakenFactor(data.keystones(), element);
        } else {
            multiplier = element.multiplierAgainst(CreatureElements.elementOf(target));
        }
        if (multiplier != 1f) {
            ElementalMatchups.feedback(level, target, element, multiplier > 1f);
        }
        amount *= multiplier;
        switch (element) {
            case FIRE -> {
                amount *= ElementalReactions.fireHit(target);
                target.igniteForSeconds(4);
            }
            case RADIANCE -> {
                amount *= ElementalReactions.fireHit(target);
                if (target.getType().is(EntityTypeTags.UNDEAD)) {
                    amount *= InfusionRules.RADIANCE_UNDEAD;
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100));
                    level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.6), target.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                }
            }
            case WATER -> amount *= ElementalReactions.waterHit(target, 100);
            case ICE -> {
                if (!ElementalReactions.iceHit(target)) {
                    ElementalReactions.inflict(target, ElementalReactions.Aura.CRYO);
                }
            }
            case WIND -> {
                if (!ElementalReactions.swirl(target, attacker, 1f) && attacker != null) {
                    target.knockback(0.6, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                    level.sendParticles(ParticleTypes.GUST, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
                }
            }
            case EARTH -> {
                ElementalReactions.earthHit(target, attacker, 100);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                        target.getX(), target.getY(0.2), target.getZ(), 10, 0.3, 0.1, 0.3, 0.1);
            }
            case CRYSTAL -> {
                ElementalReactions.earthHit(target, attacker, 100);
                if (target.getRandom().nextFloat() < InfusionRules.CRYSTAL_SHATTER_CHANCE) {
                    amount *= InfusionRules.CRYSTAL_SHATTER;
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                            target.getX(), target.getY(0.6), target.getZ(), 16, 0.3, 0.4, 0.3, 0.15);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.3f);
                }
            }
            case LIGHTNING -> spark(level, target, attacker, amount * InfusionRules.SPARK_SHARE);
        }
        return amount;
    }

    /** A lightning-infused hit leaps to the nearest other foe. */
    private static void spark(ServerLevel level, LivingEntity target, @Nullable Entity attacker, float amount) {
        LivingEntity next = null;
        double best = InfusionRules.SPARK_RANGE * InfusionRules.SPARK_RANGE;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(InfusionRules.SPARK_RANGE))) {
            if (nearby == target || nearby == attacker || !nearby.isAlive() || nearby instanceof Player
                    || nearby instanceof OwnableEntity pet && attacker != null && attacker.getUUID().equals(pet.getOwnerUUID())) {
                continue;
            }
            double distance = nearby.distanceToSqr(target);
            if (distance < best) {
                best = distance;
                next = nearby;
            }
        }
        Vec3 from = target.getBoundingBox().getCenter();
        if (next == null) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 8, 0.3, 0.4, 0.3, 0.1);
            return;
        }
        Vec3 to = next.getBoundingBox().getCenter();
        level.sendParticles(ArcOptions.between(from, to, 0, 1f), from.x, from.y, from.z, 1, 0, 0, 0, 0);
        Entity source = attacker != null ? attacker : target;
        SpellDamage.hurtMultiHit(next, SpellDamage.source(level, Element.LIGHTNING, source, source), amount);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.3f, 1.8f);
    }

    /** Set boons that last: breath, speed, the sun's rest, the crystal ward. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        Element set = setOf(player);
        if (set == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        switch (set) {
            case WATER -> player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, true));
            case LIGHTNING -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
            case RADIANCE -> {
                if (player.tickCount % InfusionRules.RADIANCE_HEAL_TICKS == 0 && level.isDay() && level.canSeeSky(player.blockPosition())
                        && player.getHealth() < player.getMaxHealth()) {
                    player.heal(1f);
                    level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 3, 0.3, 0.3, 0.3, 0.01);
                }
            }
            case CRYSTAL -> {
                if (player.tickCount % InfusionRules.CRYSTAL_WARD_TICKS == 0 && player.getAbsorptionAmount() <= 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, InfusionRules.CRYSTAL_WARD_TICKS, 0, true, false, true));
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (setOf(event.getEntity()) == Element.WIND) {
            event.setDamageMultiplier(0f);
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (setOf(event.getEntity()) == Element.EARTH) {
            event.setCanceled(true);
        }
    }

    /** The name of {@code element} (as the skill tree's schools call it). */
    public static Component elementName(Element element) {
        return Component.translatable("school.elementalarcana." + element.name().toLowerCase(java.util.Locale.ROOT));
    }
}
