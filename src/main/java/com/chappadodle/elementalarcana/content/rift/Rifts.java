package com.chappadodle.elementalarcana.content.rift;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.RiftRules;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Where and when Elemental Rifts open (see RiftEntity and the Rifts spec): every half minute each
 * player whose magic has woken, in the overworld, rolls for one (RiftRules: likelier by night and in
 * storms); it opens on open ground 24–40 blocks away, never in a village, tower or sanctum, never
 * near another, and not again near the same player for ten minutes. Its element is the land's.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Rifts {
    /** Structures no rift opens in. */
    public static final TagKey<Structure> NO_RIFTS = TagKey.create(Registries.STRUCTURE, ElementalArcana.id("no_rifts"));
    private static final double ANNOUNCE = 64;
    private static final int TRIES = 12;
    private static final Map<UUID, Long> LAST_RIFT = new HashMap<>();

    private Rifts() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % RiftRules.CHECK_TICKS != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || player.isSpectator() || player.isCreative() || level.getDifficulty() == Difficulty.PEACEFUL
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || !MagicAttachments.get(player).isAwakened()) {
            return;
        }
        long now = level.getGameTime();
        Long last = LAST_RIFT.get(player.getUUID());
        if (last != null && now - last < RiftRules.PLAYER_COOLDOWN_TICKS) {
            return;
        }
        if (player.getRandom().nextDouble() >= RiftRules.chance(!level.isDay(), level.isThundering())) {
            return;
        }
        BlockPos at = findSpot(level, player.position(), player.getRandom());
        if (at != null) {
            open(level, at, Attunement.landElement(level, at, player.getRandom()), RiftRules.wardenRank(MagicAttachments.get(player).level()));
            LAST_RIFT.put(player.getUUID(), now);
        }
    }

    /** Somewhere for a rift 24–40 blocks from {@code around}, or null if a few tries find nowhere. */
    @Nullable
    public static BlockPos findSpot(ServerLevel level, Vec3 around, RandomSource random) {
        for (int i = 0; i < TRIES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = RiftRules.MIN_DISTANCE + random.nextDouble() * (RiftRules.MAX_DISTANCE - RiftRules.MIN_DISTANCE);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (fits(level, pos) && !nearRift(level, pos) && !level.structureManager().getStructureWithPieceAt(pos, NO_RIFTS).isValid()) {
                return pos;
            }
        }
        return null;
    }

    /** Solid, dry ground with three by three by four blocks of open air above it. */
    static boolean fits(ServerLevel level, BlockPos pos) {
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.isFaceSturdy(level, pos.below(), Direction.UP) || !ground.getFluidState().isEmpty()) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 3, 1))) {
            BlockState state = level.getBlockState(p);
            if (!state.getCollisionShape(level, p).isEmpty() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean nearRift(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(RiftEntity.class, new AABB(pos).inflate(RiftRules.SPACING)).isEmpty();
    }

    /** Opens a rift of {@code element} standing at {@code pos}, its Warden of rank {@code warden}, and tells everyone near. */
    @Nullable
    public static RiftEntity open(ServerLevel level, BlockPos pos, Element element, AttunementRank warden) {
        RiftEntity rift = ModContent.RIFT.get().create(level);
        if (rift == null) {
            return null;
        }
        rift.setElement(element);
        rift.setWarden(warden);
        rift.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        level.addFreshEntity(rift);
        Vec3 middle = rift.middle();
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 2.5f, 1.3f);
        Component name = Component.translatable("school.elementalarcana." + element.name().toLowerCase(Locale.ROOT));
        Component message = Component.translatable("message.elementalarcana.rift.opened", name)
                .withStyle(style -> style.withColor(element.color()));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(middle) < ANNOUNCE * ANNOUNCE) {
                player.sendSystemMessage(message);
            }
        }
        return rift;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LAST_RIFT.clear();
    }
}
