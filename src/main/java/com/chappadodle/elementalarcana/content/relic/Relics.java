package com.chappadodle.elementalarcana.content.relic;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.Relic;
import com.chappadodle.elementalarcana.api.RelicRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The relics' powers (see the Relics spec). A player bears the relic they bound last of those bound
 * to them in their inventory (worked out once a tick); each power hooks the event it needs.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Relics {
    private static final ResourceLocation SIGIL_SPEED = ElementalArcana.id("storm_sigil_speed");
    private static final String IDOL_READY = "elementalarcana_idol_ready";
    private static final String PRISM_READY = "elementalarcana_prism_ready";
    private static final Map<Player, Bearing> BEARINGS = new WeakHashMap<>();

    /** The relic a player bears (and its stack), as of a game tick. */
    private record Bearing(long time, @Nullable Relic relic, ItemStack stack) {
    }

    private Relics() {
    }

    /** The relic {@code player} bears, or null. */
    @Nullable
    public static Relic borne(Player player) {
        return bearing(player).relic();
    }

    public static boolean bears(Player player, Relic relic) {
        return borne(player) == relic;
    }

    /** Forgets what a player bears (a relic was just bound), so the next look counts again. */
    public static void forget(Player player) {
        synchronized (BEARINGS) {
            BEARINGS.remove(player);
        }
    }

    private static Bearing bearing(Player player) {
        long now = player.level().getGameTime();
        synchronized (BEARINGS) {
            Bearing cached = BEARINGS.get(player);
            if (cached != null && cached.time() == now) {
                return cached;
            }
        }
        Inventory inventory = player.getInventory();
        ItemStack best = ItemStack.EMPTY;
        Relic relic = null;
        long latest = Long.MIN_VALUE;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof RelicItem item) {
                RelicBond bond = stack.get(ModRelics.BOND.get());
                if (bond != null && bond.owner().equals(player.getUUID()) && bond.boundAt() > latest) {
                    latest = bond.boundAt();
                    best = stack;
                    relic = item.relic();
                }
            }
        }
        Bearing bearing = new Bearing(now, relic, best);
        synchronized (BEARINGS) {
            BEARINGS.put(player, bearing);
        }
        return bearing;
    }

    /** A borne relic's stat points, for GearStats. */
    public static void addStats(Player player, Map<String, Integer> stats) {
        Relic relic = borne(player);
        if (relic != null) {
            stats.merge(relic.stat().key(), RelicRules.STAT_BONUS, Integer::sum);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Relic relic = borne(player);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean fast = relic == Relic.STORM_SIGIL;
        if (speed != null && fast != speed.hasModifier(SIGIL_SPEED)) {
            if (fast) {
                speed.addTransientModifier(new AttributeModifier(SIGIL_SPEED, RelicRules.SIGIL_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else {
                speed.removeModifier(SIGIL_SPEED);
            }
        }
        if (relic == null) {
            return;
        }
        switch (relic) {
            case EMBER_HEART -> {
                if (player.isOnFire() && !player.isInLava()) {
                    player.clearFire();
                }
            }
            case TIDECALLERS_PEARL -> {
                if (player.isInWater() && player.tickCount % 20 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, false, true));
                }
            }
            case RIMEHEART_LOCKET -> {
                player.setTicksFrozen(0);
                player.removeEffect(ModContent.FROZEN);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
            case SUNSTONE -> {
                if (player.tickCount % 20 == 0) {
                    sunstone(player);
                }
            }
            default -> {
            }
        }
    }

    /** The Sunstone: sight in the dark, and the undead near smoulder. */
    private static void sunstone(ServerPlayer player) {
        MobEffectInstance sight = player.getEffect(MobEffects.NIGHT_VISION);
        if (sight == null || sight.getDuration() < 240) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, true));
        }
        ServerLevel level = player.serverLevel();
        for (LivingEntity undead : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RelicRules.SUNSTONE_RADIUS),
                entity -> entity.getType().is(EntityTypeTags.UNDEAD) && entity.isAlive() && entity != player
                        && !(entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null))) {
            undead.hurt(SpellDamage.source(level, Element.RADIANCE, player, player), RelicRules.SUNSTONE_DAMAGE);
            undead.igniteForTicks(40);
            level.sendParticles(ParticleTypes.END_ROD, undead.getX(), undead.getY(0.6), undead.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (target instanceof Player bearer) {
            Relic relic = borne(bearer);
            boolean spared = switch (relic) {
                case EMBER_HEART -> source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.HOT_FLOOR);
                case RIMEHEART_LOCKET -> source.is(DamageTypes.FREEZE);
                case FEATHER_OF_THE_GALE -> source.is(DamageTypeTags.IS_FALL);
                case null, default -> false;
            };
            if (spared) {
                event.setCanceled(true);
                return;
            }
        }
        if (source.getEntity() instanceof Player caster && SpellDamage.elementOf(source) != null) {
            float bonus = switch (borne(caster)) {
                case EMBER_HEART -> target.isOnFire() ? RelicRules.EMBER_BURNING_BONUS : 1f;
                case RIMEHEART_LOCKET -> ElementalReactions.auraOf(target) == ElementalReactions.Aura.CRYO ? RelicRules.LOCKET_COLD_BONUS : 1f;
                case STORM_SIGIL -> caster.level().isThundering() ? RelicRules.SIGIL_STORM_BONUS : 1f;
                case null, default -> 1f;
            };
            if (bonus != 1f) {
                event.setAmount(event.getAmount() * bonus);
            }
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        float dealt = event.getNewDamage();
        if (dealt <= 0 || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (target instanceof ServerPlayer bearer && target.isAlive() && bears(bearer, Relic.STONEHEART_IDOL) && RelicRules.hardHit(dealt)
                && RelicRules.ready(bearer.getPersistentData().getLong(IDOL_READY), now)) {
            bearer.getPersistentData().putLong(IDOL_READY, now + RelicRules.IDOL_COOLDOWN_TICKS);
            bearer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, RelicRules.IDOL_ABSORPTION_TICKS, 0, false, true, true));
            level.sendParticles(ParticleTypes.CRIT, bearer.getX(), bearer.getY(0.6), bearer.getZ(), 12, 0.4, 0.5, 0.4, 0.05);
            level.playSound(null, bearer.getX(), bearer.getY(), bearer.getZ(), SoundEvents.DEEPSLATE_PLACE, SoundSource.PLAYERS, 1f, 0.6f);
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer caster)) {
            return;
        }
        Element element = SpellDamage.elementOf(event.getSource());
        if (element == null) {
            return;
        }
        Relic relic = borne(caster);
        if (relic == Relic.TIDECALLERS_PEARL && element.family() == Element.WATER) {
            caster.heal(RelicRules.mend(dealt));
        } else if (relic == Relic.PRISM_OF_THE_DEEP && element.family() == Element.EARTH
                && RelicRules.ready(caster.getPersistentData().getLong(PRISM_READY), now)) {
            caster.getPersistentData().putLong(PRISM_READY, now + RelicRules.PRISM_GAP_TICKS);
            MagicData data = MagicAttachments.get(caster);
            data.setMana(Math.min(data.maxMana(), data.mana() + RelicRules.PRISM_MANA));
            MagicAttachments.sync(caster);
        }
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && bears(player, Relic.RIMEHEART_LOCKET)
                && (event.getEffectInstance().is(ModContent.FROZEN) || event.getEffectInstance().is(MobEffects.MOVEMENT_SLOWDOWN))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && bears(player, Relic.FEATHER_OF_THE_GALE)) {
            event.setDamageMultiplier(0f);
        }
    }

    /**
     * The Phylactery: a killing blow (short of the void or /kill) leaves its bearer at 1 health, every
     * 10 minutes. First of all death listeners, so a death it stops counts for nothing else.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        Bearing bearing = bearing(player);
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (bearing.relic() != Relic.REVENANTS_PHYLACTERY || !RelicRules.ready(bearing.stack().getOrDefault(ModRelics.READY_AT.get(), 0L), now)) {
            return;
        }
        event.setCanceled(true);
        bearing.stack().set(ModRelics.READY_AT.get(), now + RelicRules.PHYLACTERY_COOLDOWN_TICKS);
        player.setHealth(1f);
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5),
                entity -> entity != player && entity instanceof Enemy)) {
            Vec3 away = foe.position().subtract(player.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
            foe.setDeltaMovement(foe.getDeltaMovement().add(away.x * 1.2, 0.45, away.z * 1.2));
            foe.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY(0.6), player.getZ(), 60, 0.6, 0.9, 0.6, 0.08);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY(0.3), player.getZ(), 40, 1.2, 0.2, 1.2, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5f, 0.8f);
        player.displayClientMessage(Component.translatable("message.elementalarcana.relic.phylactery").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
}
