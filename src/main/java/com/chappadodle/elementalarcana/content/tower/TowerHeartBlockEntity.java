package com.chappadodle.elementalarcana.content.tower;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Runs a mage tower (see the mage towers spec). When someone (not a spectator) first comes within
 * 40 blocks, the tower wakes: two acolytes appear on each of the four floors below the sanctum.
 * When someone reaches the sanctum, the Magister appears, once. When the Magister falls, the heart
 * goes dark ({@link #conquer}). The heart sits two blocks above the sanctum floor, at its middle;
 * the floors are 6 blocks apart.
 */
public class TowerHeartBlockEntity extends BlockEntity {
    private static final double WAKE_RADIUS = 40;
    private static final double SANCTUM_RADIUS = 6;
    private static final int FLOOR_HEIGHT = 6;
    private static final int ACOLYTES_PER_FLOOR = 2;
    // Where acolytes stand, best first; furniture may take some of these spots.
    private static final int[][] POSTS = {{2, 2}, {-2, -2}, {2, -2}, {-2, 2}, {0, 3}, {0, -3}, {3, 0}, {-3, 0}, {1, 1}, {-1, -1}};

    private boolean awake;
    private boolean magisterCalled;
    private boolean conquered;

    public TowerHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModTowers.TOWER_HEART_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TowerHeartBlockEntity heart) {
        if (level.getGameTime() % 20 != 0 || heart.conquered || !(level instanceof ServerLevel server) || !WispSpawner.canSpawn(server)) {
            return;
        }
        Element element = state.getValue(TowerHeartBlock.KIND).element();
        if (!heart.awake && server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, WAKE_RADIUS,
                EntitySelector.NO_SPECTATORS) != null) {
            heart.awake = true;
            heart.setChanged();
            heart.callAcolytes(server, element);
        }
        if (!heart.magisterCalled && !server.getEntitiesOfClass(Player.class,
                new AABB(pos).inflate(SANCTUM_RADIUS, 3, SANCTUM_RADIUS), EntitySelector.NO_SPECTATORS).isEmpty()) {
            heart.magisterCalled = true;
            heart.setChanged();
            heart.callMagister(server, element);
        }
    }

    private void callAcolytes(ServerLevel level, Element element) {
        // The sanctum floor stands at heart - 1; the floors below it, 6 blocks apart.
        for (int floor = 1; floor <= 4; floor++) {
            int y = worldPosition.getY() - 1 - FLOOR_HEIGHT * floor;
            int placed = 0;
            for (int i = 0; i < POSTS.length && placed < ACOLYTES_PER_FLOOR; i++) {
                if (summon(level, ModTowers.ACOLYTE.get(), element, worldPosition.offset(POSTS[i][0], 0, POSTS[i][1]).atY(y)) != null) {
                    placed++;
                }
            }
        }
    }

    private void callMagister(ServerLevel level, Element element) {
        for (int[] spot : new int[][]{{0, 3}, {3, 0}, {0, -3}, {-3, 0}}) {
            if (summon(level, ModTowers.MAGISTER.get(), element, worldPosition.offset(spot[0], -1, spot[1])) != null) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, worldPosition.getX() + 0.5 + spot[0], worldPosition.getY(),
                        worldPosition.getZ() + 0.5 + spot[1], 60, 0.4, 1, 0.4, 0.1);
                level.playSound(null, worldPosition, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5f, 0.7f);
                Component message = Component.translatable("message.elementalarcana.tower.magister").withStyle(ChatFormatting.DARK_PURPLE);
                level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(24)).forEach(player -> player.sendSystemMessage(message));
                return;
            }
        }
    }

    @Nullable
    private TowerMageEntity summon(ServerLevel level, EntityType<TowerMageEntity> type, Element element, BlockPos at) {
        if (!level.noCollision(type.getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5))
                || !level.getBlockState(at.below()).isSolid()) {
            return null;
        }
        TowerMageEntity mage = type.create(level);
        if (mage == null) {
            return null;
        }
        mage.serve(element, worldPosition);
        mage.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        EventHooks.finalizeMobSpawn(mage, level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(mage);
        return mage;
    }

    /** The Magister fell: the tower's power is spent. */
    public void conquer() {
        if (conquered || !(level instanceof ServerLevel server)) {
            return;
        }
        conquered = true;
        setChanged();
        server.setBlockAndUpdate(worldPosition, getBlockState().setValue(TowerHeartBlock.LIT, false));
        server.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5,
                60, 0.5, 0.8, 0.5, 0.15);
        server.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5f, 0.8f);
        Component message = Component.translatable("message.elementalarcana.tower.conquered").withStyle(ChatFormatting.LIGHT_PURPLE);
        server.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(32)).forEach(player -> player.sendSystemMessage(message));
    }

    public boolean isConquered() {
        return conquered;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("awake", awake);
        tag.putBoolean("magister_called", magisterCalled);
        tag.putBoolean("conquered", conquered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        awake = tag.getBoolean("awake");
        magisterCalled = tag.getBoolean("magister_called");
        conquered = tag.getBoolean("conquered");
    }
}
