package com.chappadodle.elementalarcana.content.star;

import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import com.chappadodle.elementalarcana.core.ArcanaServerConfig;
import com.chappadodle.elementalarcana.api.ConfigRates;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.StarfallRules;
import com.chappadodle.elementalarcana.content.creature.ModCreatures;
import com.chappadodle.elementalarcana.content.creature.WispEntity;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.network.StarPillarsPayload;
import com.chappadodle.elementalarcana.network.StarStreakPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Starfall (see its spec): at dusk each awakened player in the Overworld may draw a falling star for
 * the night; when it comes, a streak crosses the sky for everyone near, and where it lands it
 * scorches the ground, leaves a Fallen Star under a pillar of light, and draws Radiance wisps. At
 * dawn unmined stars cool into Starstone. Also the Starlit Lanterns' list, and their ward.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Starfalls {
    private record Pending(UUID player, long at) {
    }

    private record Fall(BlockPos impact, long landsAt) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    /** The Overworld's day time when it last ticked (-1: not yet), so dusk is rolled for once as it passes, not every tick time stands still on it. */
    private static long lastDayTime = -1;
    private static final List<Fall> FALLING = new ArrayList<>();
    private static final Set<BlockPos> PILLARS = new LinkedHashSet<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> LANTERNS = new HashMap<>();

    private Starfalls() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        long now = level.getGameTime();
        long dayTime = level.getDayTime();
        long before = lastDayTime < 0 ? dayTime : lastDayTime;
        lastDayTime = dayTime;
        RandomSource random = level.getRandom();
        if (before != dayTime && StarfallRules.crosses(before, dayTime, StarfallRules.DUSK)) {
            for (ServerPlayer player : level.players()) {
                if (MagicAttachments.get(player).isAwakened() && random.nextFloat() < ConfigRates.scaled(StarfallRules.NIGHT_CHANCE, ArcanaServerConfig.STARFALL.get())) {
                    PENDING.add(new Pending(player.getUUID(), now + StarfallRules.fallTime(random.nextDouble()) - StarfallRules.DUSK));
                }
            }
        }
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (now >= pending.at()) {
                it.remove();
                if (level.getPlayerByUUID(pending.player()) instanceof ServerPlayer player && player.level() == level) {
                    fallNear(level, player.position(), random);
                }
            }
        }
        for (Iterator<Fall> it = FALLING.iterator(); it.hasNext(); ) {
            Fall fall = it.next();
            if (now >= fall.landsAt()) {
                it.remove();
                land(level, fall.impact());
            }
        }
        if (StarfallRules.crosses(dayTime - 1, dayTime, StarfallRules.DAWN) && !PILLARS.isEmpty()) {
            coolAll(level);
        }
    }

    /** A star falls somewhere 100 to 180 blocks from {@code around}, in loaded dry land. Returns whether one fell. */
    public static boolean fallNear(ServerLevel level, Vec3 around, RandomSource random) {
        for (int tries = 0; tries < 10; tries++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = StarfallRules.MIN_DISTANCE + random.nextDouble() * (StarfallRules.MAX_DISTANCE - StarfallRules.MIN_DISTANCE);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos impact = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (level.getFluidState(impact.below()).isEmpty() && level.getBlockState(impact.below()).isSolid()) {
                fallAt(level, impact, random);
                return true;
            }
        }
        return false;
    }

    /** A star falls on {@code impact}: the streak now, the landing in three seconds. */
    public static void fallAt(ServerLevel level, BlockPos impact, RandomSource random) {
        Vec3 end = Vec3.atBottomCenterOf(impact);
        double side = random.nextDouble() * Math.PI * 2;
        Vec3 start = end.add(Math.cos(side) * StarfallRules.FALL_SIDEWAYS, StarfallRules.FALL_HEIGHT, Math.sin(side) * StarfallRules.FALL_SIDEWAYS);
        StarStreakPayload streak = new StarStreakPayload(new Vector3f((float) start.x, (float) start.y, (float) start.z),
                new Vector3f((float) end.x, (float) end.y, (float) end.z), StarfallRules.FALL_TICKS);
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(end) <= StarfallRules.SEEN_WITHIN * StarfallRules.SEEN_WITHIN) {
                PacketDistributor.sendToPlayer(player, streak);
                if (MagicAttachments.get(player).isAwakened()) {
                    String direction = StarfallRules.direction(end.x - player.getX(), end.z - player.getZ());
                    player.sendSystemMessage(Component.translatable("message.elementalarcana.starfall",
                            Component.translatable("direction.elementalarcana." + direction)).withStyle(style -> style.withColor(0xFFE9A8)));
                }
            }
        }
        FALLING.add(new Fall(impact.immutable(), level.getGameTime() + StarfallRules.FALL_TICKS));
    }

    /** It lands: the ground scorched, the star set down, a boom, wisps, and the pillar of light. */
    private static void land(ServerLevel level, BlockPos impact) {
        scorch(level, impact);
        BlockPos at = impact;
        while (!level.getBlockState(at).canBeReplaced() && at.getY() < impact.getY() + 3) {
            at = at.above();
        }
        level.setBlockAndUpdate(at, ModStars.FALLEN_STAR.get().defaultBlockState());
        Vec3 centre = Vec3.atCenterOf(at);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLASH, centre.x, centre.y + 1, centre.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y + 0.5, centre.z, 60, 1.2, 0.8, 1.2, 0.25);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y + 0.5, centre.z, 30, 1.5, 0.5, 1.5, 0.05);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 4f, 0.6f);
        for (ServerPlayer player : level.players()) {
            double distance = player.position().distanceTo(centre);
            if (distance > 48 && distance <= StarfallRules.SEEN_WITHIN) {
                // Far off it's a distant rumble.
                player.playNotifySound(SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 0.6f, 0.5f);
            }
        }
        RandomSource random = level.getRandom();
        int wisps = StarfallRules.WISPS_MIN + random.nextInt(StarfallRules.WISPS_MAX - StarfallRules.WISPS_MIN + 1);
        for (int i = 0; i < wisps; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            BlockPos spot = BlockPos.containing(centre.x + Math.cos(angle) * 3, centre.y + 2, centre.z + Math.sin(angle) * 3);
            WispEntity wisp = ModCreatures.wisp(Element.RADIANCE).spawn(level, spot, MobSpawnType.EVENT);
            if (wisp != null) {
                wisp.guard(at);
            }
        }
        PILLARS.add(at.immutable());
        syncPillars(level);
    }

    /** The ground round the impact blackened: only natural ground and plants, never anything built. */
    private static void scorch(ServerLevel level, BlockPos impact) {
        RandomSource random = level.getRandom();
        int radius = StarfallRules.SCORCH_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(impact.offset(-radius, -2, -radius), impact.offset(radius, 1, radius))) {
            double distance = Math.sqrt(pos.distSqr(new BlockPos(impact.getX(), pos.getY(), impact.getZ())));
            if (distance > radius + 0.5 - random.nextDouble()) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (pos.getY() >= impact.getY() && (state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.FLOWERS)
                    || state.is(Blocks.SNOW))) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else if (pos.getY() < impact.getY() && natural(state) && level.getBlockState(pos.above()).canBeReplaced()) {
                BlockState scorched = distance < 1.2 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : random.nextInt(3) == 0 ? Blocks.BASALT.defaultBlockState()
                        : distance < radius - 0.5 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                level.setBlockAndUpdate(pos, scorched);
            }
        }
    }

    private static boolean natural(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SNOW_BLOCK) || state.is(BlockTags.TERRACOTTA);
    }

    /** A star mined (or gone): its pillar goes out. */
    public static void extinguish(ServerLevel level, BlockPos pos) {
        if (PILLARS.remove(pos)) {
            syncPillars(level);
        }
    }

    /** Dawn: the stars no one reached cool into Starstone. */
    private static void coolAll(ServerLevel level) {
        for (BlockPos pos : new ArrayList<>(PILLARS)) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(ModStars.FALLEN_STAR.get())) {
                level.setBlockAndUpdate(pos, ModStars.STARSTONE.get().defaultBlockState());
            }
        }
        PILLARS.clear();
        syncPillars(level);
    }

    private static void syncPillars(ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, new StarPillarsPayload(List.copyOf(PILLARS)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sendPillarsTo(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendPillarsTo(event.getEntity());
    }

    private static void sendPillarsTo(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer server) {
            PacketDistributor.sendToPlayer(server, new StarPillarsPayload(
                    server.level().dimension() == Level.OVERWORLD ? List.copyOf(PILLARS) : List.of()));
        }
    }

    // ---- Starlit Lanterns ----

    public static void addLantern(ServerLevel level, BlockPos pos) {
        LANTERNS.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
    }

    public static void removeLantern(ServerLevel level, BlockPos pos) {
        Set<BlockPos> lanterns = LANTERNS.get(level.dimension());
        if (lanterns != null) {
            lanterns.remove(pos);
        }
    }

    /** No hostile monster spawns on its own within 24 blocks of a Starlit Lantern. */
    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof Enemy) || event.getSpawnType() != MobSpawnType.NATURAL
                && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION && event.getSpawnType() != MobSpawnType.PATROL) {
            return;
        }
        Set<BlockPos> lanterns = LANTERNS.get(event.getLevel().getLevel().dimension());
        if (lanterns == null || lanterns.isEmpty()) {
            return;
        }
        for (BlockPos lantern : lanterns) {
            if (StarfallRules.lanternWards(lantern.distToCenterSqr(event.getX(), event.getY(), event.getZ()))) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
        lastDayTime = -1;
    }
}
