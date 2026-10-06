package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.api.HollowedRules;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Runs a Hollowed camp from its obelisk (see the Hollowed spec). The first time someone (not a
 * spectator) comes within 32 blocks, the camp's people are there: the Herald by the obelisk, two
 * Acolytes and two Devourers about it, and they stay. While the obelisk stands, every two seconds
 * the Hollowed within 16 blocks regenerate and resist. The obelisk stands two blocks over the camp's
 * floor, on its pillar.
 */
public class HungerObeliskBlockEntity extends BlockEntity {
    private static final double WAKE_RADIUS = 32;
    private static final int BUFF_TICKS = 60;
    // Where the camp's people stand about the pillar (x, z), the Herald first; a tent or fire may take some.
    private static final int[][] HERALD_POSTS = {{0, 3}, {3, 0}, {-3, 0}, {0, -3}};
    private static final int[][] POSTS = {{3, 3}, {-3, 3}, {3, -2}, {-3, -2}, {2, 5}, {-2, 5}, {5, 1}, {-5, 1}};

    private boolean awake;

    public HungerObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(ModHollowed.OBELISK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HungerObeliskBlockEntity obelisk) {
        if (level.getGameTime() % 40 != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        if (!obelisk.awake && WispSpawner.canSpawn(server) && server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                WAKE_RADIUS, EntitySelector.NO_SPECTATORS) != null) {
            obelisk.awake = true;
            obelisk.setChanged();
            obelisk.callCamp(server);
        }
        for (HollowedEntity hollowed : server.getEntitiesOfClass(HollowedEntity.class, new AABB(pos).inflate(HollowedRules.OBELISK_RADIUS))) {
            hollowed.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFF_TICKS, 0, true, true));
            hollowed.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_TICKS, 0, true, true));
        }
    }

    /** The camp's people, about the pillar on the camp's floor. */
    private void callCamp(ServerLevel level) {
        BlockPos floor = worldPosition.below(2);
        for (int[] post : HERALD_POSTS) {
            HollowHerald herald = HollowedCamps.summon(level, ModHollowed.HERALD.get(), floor.offset(post[0], 0, post[1]), MobSpawnType.STRUCTURE);
            if (herald != null) {
                herald.setObelisk(worldPosition);
                herald.setPersistenceRequired();
                break;
            }
        }
        int acolytes = 0;
        int devourers = 0;
        for (int[] post : POSTS) {
            if (acolytes == 2 && devourers == 2) {
                break;
            }
            EntityType<? extends HollowedEntity> type = acolytes <= devourers ? ModHollowed.ACOLYTE.get() : ModHollowed.DEVOURER.get();
            HollowedEntity hollowed = HollowedCamps.summon(level, type, floor.offset(post[0], 0, post[1]), MobSpawnType.STRUCTURE);
            if (hollowed != null) {
                hollowed.setPersistenceRequired();
                if (type == ModHollowed.ACOLYTE.get()) {
                    acolytes++;
                } else {
                    devourers++;
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("awake", awake);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        awake = tag.getBoolean("awake");
    }
}
