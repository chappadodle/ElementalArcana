package com.chappadodle.elementalarcana.content.hollow;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.HollowRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

/**
 * The way to the Hollow and back (see the Hollow spec). A turned Prime Key takes its holder and
 * everyone within 6 blocks to the Ruin. Three seconds after anyone is there, if the Hollow isn't, it
 * rises from the maw. When it falls everyone in the Hollow wins (a title, the epilogue, a Heart of the
 * Prime each), and ten seconds later the Hollow lets them all go home. Whoever falls off the island
 * isn't left to the void: the Hollow spits them back onto it, hard.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class HollowEvents {
    private static final int RISE_DELAY_TICKS = 60;
    private static final double PARTY_RADIUS = 6;
    private static final float SPAT_DAMAGE = 8f;

    private HollowEvents() {
    }

    /** Takes {@code user} and everyone within 6 blocks of them to the Ruin. */
    public static void enter(ServerPlayer user) {
        ServerLevel hollow = user.server.getLevel(ModHollow.THE_HOLLOW);
        if (hollow == null) {
            return;
        }
        HollowArena arena = HollowArena.get(hollow);
        arena.ensureBuilt(hollow);
        long riseAt = hollow.getGameTime() + RISE_DELAY_TICKS;
        if (arena.nextRiseAt() < riseAt) {
            arena.setNextRiseAt(riseAt);
        }
        List<ServerPlayer> party = user.serverLevel().getEntitiesOfClass(ServerPlayer.class, user.getBoundingBox().inflate(PARTY_RADIUS),
                player -> player.isAlive() && !player.isSpectator());
        for (int i = 0; i < party.size(); i++) {
            ServerPlayer player = party.get(i);
            player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(0.5), player.getZ(), 80, 0.4, 0.8, 0.4, 0.2);
            double offset = (i - (party.size() - 1) / 2.0) * 1.5;
            Vec3 at = HollowArena.ARRIVAL.add(offset, 0, 0);
            player.teleportTo(hollow, at.x, at.y, at.z, 180f, 0f);
            title(player, Component.translatable("title.elementalarcana.hollow").withStyle(ChatFormatting.DARK_PURPLE),
                    Component.translatable("title.elementalarcana.hollow.sub"));
            player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.AMBIENT, 0.6f, 0.6f);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ModHollow.THE_HOLLOW) {
            return;
        }
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (player.isAlive() && !player.isSpectator() && HollowArena.fallen(player.position())) {
                spitBack(level, player);
            }
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        HollowArena arena = HollowArena.get(level);
        arena.ensureBuilt(level);
        long now = level.getGameTime();
        if (arena.releaseAt() > 0) {
            if (now >= arena.releaseAt()) {
                arena.setReleaseAt(0);
                List.copyOf(level.players()).forEach(HollowEvents::sendHome);
            }
            return;
        }
        boolean anyone = level.players().stream().anyMatch(player -> player.isAlive() && !player.isSpectator() && HollowArena.near(player.position()));
        if (anyone && now >= arena.nextRiseAt()
                && level.getEntities(ModHollow.HOLLOW.get(), new AABB(HollowArena.CENTER).inflate(96), Entity::isAlive).isEmpty()) {
            rise(level);
        }
    }

    private static void rise(ServerLevel level) {
        HollowEntity hollow = ModHollow.HOLLOW.get().create(level);
        if (hollow == null) {
            return;
        }
        Vec3 at = HollowArena.MAW;
        hollow.moveTo(at.x, at.y, at.z, 0f, 0f);
        EventHooks.finalizeMobSpawn(hollow, level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        level.addFreshEntity(hollow);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 2, at.z, 300, 2, 3, 2, 0.3);
        level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 2, at.z, 150, 2, 2, 2, 0.2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2f, 0.6f);
        Component message = Component.translatable("message.elementalarcana.hollow.rises").withStyle(ChatFormatting.DARK_PURPLE);
        level.players().forEach(player -> player.sendSystemMessage(message));
    }

    /** The Hollow fell: the seals are whole again, and everyone who was there won. */
    public static void bound(ServerLevel level) {
        HollowArena.get(level).setReleaseAt(level.getGameTime() + HollowRules.RELEASE_TICKS);
        for (ServerPlayer player : level.players()) {
            title(player, Component.translatable("title.elementalarcana.hollow.bound").withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.translatable("title.elementalarcana.hollow.bound.sub"));
            player.sendSystemMessage(Component.translatable("message.elementalarcana.hollow.epilogue").withStyle(ChatFormatting.ITALIC, ChatFormatting.LIGHT_PURPLE));
            ItemStack heart = new ItemStack(ModHollow.PRIME_HEART.get());
            if (!player.getInventory().add(heart)) {
                player.drop(heart, false);
            }
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        }
    }

    /** Fallen off the island: back onto it, hurt (the hunger's damage, so it can kill). */
    private static void spitBack(ServerLevel level, ServerPlayer player) {
        Vec3 at = HollowArena.ARRIVAL;
        player.teleportTo(level, at.x, at.y, at.z, 180f, 0f);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.hurt(level.damageSources().source(ModHollow.HUNGER), SPAT_DAMAGE);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 60, 0.4, 0.8, 0.4, 0.2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.2f, 0.8f);
        player.sendSystemMessage(Component.translatable("message.elementalarcana.hollow.spat").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Back to where they would wake after dying: their bed, or the world's spawn. */
    private static void sendHome(ServerPlayer player) {
        DimensionTransition home = player.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING);
        player.changeDimension(home);
        player.sendSystemMessage(Component.translatable("message.elementalarcana.hollow.release").withStyle(ChatFormatting.GRAY));
    }

    private static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }
}
