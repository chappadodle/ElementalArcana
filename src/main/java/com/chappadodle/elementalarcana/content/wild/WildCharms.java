package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.WildTrophyRules;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What the Trophies of the Wild do for whoever carries them (see their spec): the Heartwood
 * Talisman's rest in the sun and its refusal of Rooted, the Wraithsilk Veil's flicker away from a
 * hit, and the Salamander Charm's crust over lava and cool feet.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class WildCharms {
    /** When each player was last hurt (game time), for the talisman's rest. */
    private static final Map<Player, Long> LAST_HURT = new WeakHashMap<>();

    private WildCharms() {
    }

    /** Whether {@code player} carries {@code charm} anywhere in their inventory. */
    public static boolean carries(Player player, Item charm) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(charm)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (player.onGround() && !player.isPassenger() && carries(player, ModWild.SALAMANDER_CHARM.get())) {
            coolLava(level, player);
        }
        if (player.tickCount % WildTrophyRules.REST_INTERVAL_TICKS == 0 && player.getHealth() < player.getMaxHealth()
                && carries(player, ModWild.HEARTWOOD_TALISMAN.get())) {
            rest(level, player);
        }
    }

    /** Still lava in a disc under the bearer's feet cools to crust; the crust right under them stays whole. */
    private static void coolLava(ServerLevel level, Player player) {
        BlockPos feet = player.blockPosition();
        int radius = WildTrophyRules.CRUST_RADIUS;
        LavaCrustBlock crustBlock = ModWild.LAVA_CRUST.get();
        BlockState crust = crustBlock.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-radius, -1, -radius), feet.offset(radius, -1, radius))) {
            double dx = pos.getX() + 0.5 - player.getX();
            double dz = pos.getZ() + 0.5 - player.getZ();
            if (!WildTrophyRules.inCrustDisc(dx, dz)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(crustBlock)) {
                if (state.getValue(LavaCrustBlock.AGE) > 0 && Math.abs(dx) < 1.3 && Math.abs(dz) < 1.3) {
                    level.setBlock(pos, state.setValue(LavaCrustBlock.AGE, 0), Block.UPDATE_CLIENTS);
                }
            } else if (state.is(Blocks.LAVA) && state.getFluidState().isSource() && level.getBlockState(pos.above()).isAir()
                    && level.isUnobstructed(crust, pos, CollisionContext.empty())) {
                level.setBlockAndUpdate(pos, crust);
                if (player.getRandom().nextInt(4) == 0) {
                    level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.4f);
                }
            }
        }
    }

    /** By day, under the open sky, on the earth, left alone a while: a little health back. */
    private static void rest(ServerLevel level, Player player) {
        BlockPos feet = player.blockPosition();
        long lastHurt = LAST_HURT.getOrDefault(player, Long.MIN_VALUE / 2);
        if (!WildTrophyRules.canRest(level.isDay(), level.canSeeSky(feet), level.getBlockState(feet.below()).is(BlockTags.DIRT),
                level.getGameTime(), lastHurt)) {
            return;
        }
        player.heal(WildTrophyRules.REST_HEAL);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.4, player.getZ(), 5, 0.35, 0.3, 0.35, 0);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.HOT_FLOOR) && carries(player, ModWild.SALAMANDER_CHARM.get())) {
            event.setCanceled(true);
            return;
        }
        LAST_HURT.put(player, player.level().getGameTime());
        Item veil = ModWild.WRAITHSILK_VEIL.get();
        Entity from = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
        if (from == null || from == player || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || player.getCooldowns().isOnCooldown(veil) || !carries(player, veil)) {
            return;
        }
        if (flicker(player.serverLevel(), player, from.position())) {
            event.setAmount(WildTrophyRules.flickerDamage(event.getAmount()));
            player.getCooldowns().addCooldown(veil, WildTrophyRules.FLICKER_COOLDOWN_TICKS);
        }
    }

    /** The veil's flicker: 4 to 6 blocks away from {@code from}, onto safe ground. Returns false if there was none. */
    private static boolean flicker(ServerLevel level, ServerPlayer player, Vec3 from) {
        RandomSource random = player.getRandom();
        Vec3 away = player.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0e-4) {
            away = Vec3.directionFromRotation(0, player.getYRot()).scale(-1);
        }
        away = away.normalize();
        for (int tries = 0; tries < 16; tries++) {
            Vec3 direction = away.yRot((float) ((random.nextDouble() - 0.5) * Math.PI * 0.9));
            double distance = WildTrophyRules.FLICKER_MIN + random.nextDouble() * (WildTrophyRules.FLICKER_MAX - WildTrophyRules.FLICKER_MIN);
            Vec3 spot = player.position().add(direction.scale(distance));
            BlockPos column = BlockPos.containing(spot);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos feet = column.above(dy);
                Vec3 to = new Vec3(spot.x, feet.getY(), spot.z);
                if (safe(level, player, feet, to)) {
                    Vec3 start = player.position();
                    level.sendParticles(ModContent.FROST_MIST.get(), start.x, start.y + 1, start.z, 10, 0.3, 0.6, 0.3, 0.02);
                    level.playSound(null, start.x, start.y, start.z, SoundEvents.VEX_CHARGE, SoundSource.PLAYERS, 1f, 0.7f);
                    player.teleportTo(to.x, to.y, to.z);
                    player.resetFallDistance();
                    level.sendParticles(ModContent.FROST_MIST.get(), to.x, to.y + 1, to.z, 10, 0.3, 0.6, 0.3, 0.02);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, to.x, to.y + 1, to.z, 12, 0.3, 0.6, 0.3, 0.02);
                    level.playSound(null, to.x, to.y, to.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8f, 1.2f);
                    return true;
                }
            }
        }
        return false;
    }

    /** Sturdy ground, no liquid, and room for the player. */
    private static boolean safe(ServerLevel level, Player player, BlockPos feet, Vec3 to) {
        BlockPos below = feet.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || !level.getFluidState(feet).isEmpty()
                || !level.getFluidState(feet.above()).isEmpty()) {
            return false;
        }
        return level.noCollision(player, player.getBoundingBox().move(to.subtract(player.position())));
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && event.getEffectInstance().is(ModWild.ROOTED)
                && carries(player, ModWild.HEARTWOOD_TALISMAN.get())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
