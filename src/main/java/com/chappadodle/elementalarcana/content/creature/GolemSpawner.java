package com.chappadodle.elementalarcana.content.creature;

import com.chappadodle.elementalarcana.core.ArcanaServerConfig;
import com.chappadodle.elementalarcana.api.ConfigRates;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.GolemRules;
import com.chappadodle.elementalarcana.content.Attunement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Where Elemental Golems come from (see the Golems spec): every half minute each player in the
 * overworld rolls for one (GolemRules: likelier at night); it stands up on open ground 24 to 40
 * blocks away, of the land's element family, if no other golem is within 64 blocks.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class GolemSpawner {
    private static final int TRIES = 6;

    private GolemSpawner() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || (player.tickCount + player.getId() * 7) % GolemRules.CHECK_TICKS != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !WispSpawner.canSpawn(level)
                || player.getRandom().nextDouble() >= ConfigRates.scaled(GolemRules.chance(!level.isDay()), ArcanaServerConfig.GOLEMS.get())) {
            return;
        }
        trySpawn(level, player.position(), player.getRandom(), null);
    }

    /** Tries to stand a golem up 24 to 40 blocks from {@code around} ({@code forced}: of that element, for testing). */
    @Nullable
    public static GolemEntity trySpawn(ServerLevel level, Vec3 around, RandomSource random, @Nullable Element forced) {
        for (int i = 0; i < TRIES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = GolemRules.MIN_DISTANCE + random.nextDouble() * (GolemRules.MAX_DISTANCE - GolemRules.MIN_DISTANCE);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!fits(level, pos) || !level.getEntitiesOfClass(GolemEntity.class, new AABB(pos).inflate(GolemRules.SPACING)).isEmpty()) {
                continue;
            }
            Element element = forced != null ? forced : Attunement.landElement(level, pos, random).family();
            GolemEntity golem = ModCreatures.golem(element).spawn(level, pos, MobSpawnType.NATURAL);
            if (golem != null) {
                return golem;
            }
        }
        return null;
    }

    /** Solid, dry ground with room above it for a golem (two wide, three high). */
    private static boolean fits(ServerLevel level, BlockPos pos) {
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.isFaceSturdy(level, pos.below(), Direction.UP) || !ground.getFluidState().isEmpty()) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 2, 1))) {
            BlockState state = level.getBlockState(p);
            if (!state.getCollisionShape(level, p).isEmpty() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
