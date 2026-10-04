package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.WildRules;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Where the Creatures of the Wild come from (see their spec): every 20 seconds each player rolls for
 * each of the three, and one that comes up is born 20 to 40 blocks off in its own land, never two
 * of a kind within 48 blocks. Treants stand up among the trees, wraiths drift over the snow at
 * night, and salamanders bask in the badlands and deserts and on the Nether's floor.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class WildSpawner {
    public static final TagKey<Biome> TREANT_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/treant"));
    public static final TagKey<Biome> WRAITH_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/frost_wraith"));
    public static final TagKey<Biome> SALAMANDER_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/salamander"));
    private static final int TRIES = 6;

    /** The three, for the spawner and the test command. */
    public enum Kind {
        TREANT, WRAITH, SALAMANDER
    }

    private WildSpawner() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || (player.tickCount + player.getId() * 11) % WildRules.CHECK_TICKS != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!WispSpawner.canSpawn(level)) {
            return;
        }
        RandomSource random = player.getRandom();
        if (level.dimension() == Level.OVERWORLD) {
            if (random.nextFloat() < WildRules.TREANT_CHANCE) {
                trySpawn(level, Kind.TREANT, player.position(), random, false);
            }
            if (!level.isDay() && random.nextFloat() < WildRules.WRAITH_CHANCE) {
                trySpawn(level, Kind.WRAITH, player.position(), random, false);
            }
        }
        if ((level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER) && random.nextFloat() < WildRules.SALAMANDER_CHANCE) {
            trySpawn(level, Kind.SALAMANDER, player.position(), random, false);
        }
    }

    /**
     * Tries to bring one of {@code kind} in 20 to 40 blocks from {@code around}. {@code forced} (the
     * test command) skips the land and spacing checks. Returns the creature, or null if there was no
     * place for it.
     */
    @Nullable
    public static Mob trySpawn(ServerLevel level, Kind kind, Vec3 around, RandomSource random, boolean forced) {
        for (int i = 0; i < TRIES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = WildRules.MIN_DISTANCE + random.nextDouble() * (WildRules.MAX_DISTANCE - WildRules.MIN_DISTANCE);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos pos = ground(level, x, Mth.floor(around.y), z);
            if (pos == null || !forced && !level.getBiome(pos).is(lands(kind))) {
                continue;
            }
            if (!forced && !level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(WildRules.SPACING),
                    mob -> mob.getType() == type(kind)).isEmpty()) {
                continue;
            }
            if (!fits(level, kind, pos)) {
                continue;
            }
            BlockPos at = kind == Kind.WRAITH ? pos.above(3 + random.nextInt(3)) : pos;
            Mob mob = type(kind).spawn(level, at, forced ? MobSpawnType.COMMAND : MobSpawnType.NATURAL);
            if (mob != null) {
                return mob;
            }
        }
        return null;
    }

    public static EntityType<? extends Mob> type(Kind kind) {
        return switch (kind) {
            case TREANT -> ModWild.TREANT.get();
            case WRAITH -> ModWild.FROST_WRAITH.get();
            case SALAMANDER -> ModWild.SALAMANDER.get();
        };
    }

    private static TagKey<Biome> lands(Kind kind) {
        return switch (kind) {
            case TREANT -> TREANT_LANDS;
            case WRAITH -> WRAITH_LANDS;
            case SALAMANDER -> SALAMANDER_LANDS;
        };
    }

    /**
     * The open ground at (x, z): the top of the land outside the Nether; in the Nether (under its
     * roof) the first floor with room above it, searching down from a little over {@code nearY}.
     */
    @Nullable
    private static BlockPos ground(ServerLevel level, int x, int nearY, int z) {
        if (level.dimensionType().hasCeiling()) {
            for (int y = Math.min(nearY + 16, level.getMaxBuildHeight() - 3); y > Math.max(nearY - 24, level.getMinBuildHeight()); y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                    return pos;
                }
            }
            return null;
        }
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Dry, sturdy ground (a forest floor for a treant) with room for the creature above it. */
    private static boolean fits(ServerLevel level, Kind kind, BlockPos pos) {
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.isFaceSturdy(level, pos.below(), Direction.UP) || !ground.getFluidState().isEmpty()) {
            return false;
        }
        if (kind == Kind.TREANT && !ground.is(BlockTags.DIRT)) {
            return false;
        }
        int radius = kind == Kind.TREANT ? 1 : 0;
        int height = switch (kind) {
            case TREANT -> 3;
            case WRAITH -> 5;
            case SALAMANDER -> 1;
        };
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, 0, -radius), pos.offset(radius, height - 1, radius))) {
            BlockState state = level.getBlockState(p);
            if (!state.getCollisionShape(level, p).isEmpty() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
