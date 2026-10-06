package com.chappadodle.elementalarcana.content.circle;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.people.ModPeople;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Enclave's heart (see the Circle spec, part 1), under the middle of the Spire's library floor.
 * The first time someone (not a spectator) comes within 40 blocks, the Enclave's people are there
 * (and any leaves that trees beyond the walls spread into the courtyard are cleared away): the
 * Archmagister in the study, four Circle Mages of four elements about the courtyard, and an
 * Arcanist in each cottage. They're part of the place, so peace and the spawning rules don't keep
 * them away. One who falls is replaced, one a day while someone is near: a new Archmagister in the
 * study, or a mage or an Arcanist who walks in at the gate.
 */
public class CircleHeartBlockEntity extends BlockEntity {
    private static final double WAKE_RADIUS = 40;
    // The fallen are replaced only while someone is this near (so the whole Enclave is loaded), and
    // not till the heart has ticked this long since it was loaded (so its people have loaded too).
    private static final double WATCH_RADIUS = 24;
    private static final int SETTLE_TICKS = 200;
    private static final int MAGES = 4;
    private static final int ARCANISTS = 2;
    // Where the mages stand about the Spire (x, z from its middle, on the courtyard floor), more than they need.
    private static final int[][] POSTS = {{4, 9}, {-4, 9}, {9, -3}, {-9, 3}, {6, 6}, {-6, 6}};
    // The cottages' middles (x, z from the Spire's middle).
    private static final int[][] COTTAGES = {{12, 11}, {-12, 11}};

    private boolean awake;
    /** The day (by the world's clock) it last looked for its fallen. */
    private long checkedDay;
    /** How long it has ticked since it was loaded (not saved). */
    private int settled;

    public CircleHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModCircle.CIRCLE_HEART_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CircleHeartBlockEntity heart) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        heart.settled++;
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        if (!heart.awake) {
            if (someoneNear(server, pos, WAKE_RADIUS)) {
                heart.awake = true;
                heart.checkedDay = day(server);
                heart.setChanged();
                heart.clearSpill(server);
                heart.callPeople(server);
            }
            return;
        }
        long day = day(server);
        if (day != heart.checkedDay && heart.settled >= SETTLE_TICKS && someoneNear(server, pos, WATCH_RADIUS)) {
            heart.checkedDay = day;
            heart.setChanged();
            heart.replaceFallen(server);
        }
    }

    private static long day(ServerLevel level) {
        return level.getDayTime() / 24000L;
    }

    private static boolean someoneNear(ServerLevel level, BlockPos pos, double radius) {
        return level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, radius, EntitySelector.NO_SPECTATORS) != null;
    }

    /**
     * Leaves that reached over the walls into the courtyard (a tree in a chunk built after the
     * Enclave's own can spread into it), and their vines: gone before anyone sees them. The garden's
     * hedge is placed (persistent), so it stays.
     */
    private void clearSpill(ServerLevel level) {
        BlockPos floor = floor();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 1 - EnclavePiece.HALF; x < EnclavePiece.HALF; x++) {
            for (int z = 1 - EnclavePiece.HALF; z < EnclavePiece.HALF; z++) {
                for (int y = 0; y <= EnclavePiece.TOP; y++) {
                    pos.set(floor.getX() + x, floor.getY() + y, floor.getZ() + z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.LEAVES) && !state.getOptionalValue(LeavesBlock.PERSISTENT).orElse(true) || state.is(Blocks.VINE)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private void callPeople(ServerLevel level) {
        BlockPos floor = floor();
        summon(level, ModCircle.ARCHMAGISTER.get(), Element.RADIANCE, study(), study());
        List<Element> elements = new ArrayList<>(Arrays.asList(Element.values()));
        int placed = 0;
        for (int[] post : POSTS) {
            if (placed == MAGES) {
                break;
            }
            Element element = elements.remove(level.getRandom().nextInt(elements.size()));
            if (summon(level, ModCircle.CIRCLE_MAGE.get(), element, floor.offset(post[0], 0, post[1]), floor) != null) {
                placed++;
            }
        }
        for (int[] cottage : COTTAGES) {
            arcanist(level, floor.offset(cottage[0], 0, cottage[1]));
        }
    }

    /**
     * One of the fallen replaced, if any: the Archmagister first (in the study), then a mage (of an
     * element the others lack), then an Arcanist (both walking in at the gate).
     */
    private void replaceFallen(ServerLevel level) {
        BlockPos floor = floor();
        AABB enclave = new AABB(floor).inflate(EnclavePiece.HALF + 8, EnclavePiece.TOP, EnclavePiece.HALF + 8);
        // The Enclave's own (not a sigil's mage someone brought along).
        List<CircleMageEntity> circle = level.getEntitiesOfClass(CircleMageEntity.class, enclave,
                mage -> mage.isAlive() && mage.companionId() == null);
        if (circle.stream().noneMatch(CircleMageEntity::isArchmagister)) {
            summon(level, ModCircle.ARCHMAGISTER.get(), Element.RADIANCE, study(), study());
            return;
        }
        // Just inside the gate, on the main path.
        BlockPos gate = floor.south(EnclavePiece.HALF - 2);
        List<CircleMageEntity> mages = circle.stream().filter(mage -> !mage.isArchmagister()).toList();
        List<Element> missing = new ArrayList<>(Arrays.asList(Element.values()));
        mages.forEach(mage -> missing.remove(mage.element()));
        if (mages.size() < MAGES && !missing.isEmpty()) {
            summon(level, ModCircle.CIRCLE_MAGE.get(), missing.get(level.getRandom().nextInt(missing.size())), gate, floor);
            return;
        }
        int arcanists = level.getEntitiesOfClass(Villager.class, enclave,
                villager -> villager.isAlive() && villager.getVillagerData().getProfession() == ModPeople.ARCANIST.get()).size();
        if (arcanists < ARCANISTS) {
            arcanist(level, gate);
        }
    }

    /** Where the courtyard's and the library's floor is (one up from the heart). */
    private BlockPos floor() {
        return worldPosition.above();
    }

    /** Where the Archmagister stands in the study (seven up from the heart, a step north of the middle). */
    private BlockPos study() {
        return floor().above(EnclavePiece.STOREY + 1).north(1);
    }

    private static void arcanist(ServerLevel level, BlockPos at) {
        Villager arcanist = EntityType.VILLAGER.create(level);
        if (arcanist == null) {
            return;
        }
        arcanist.setVillagerData(arcanist.getVillagerData().setProfession(ModPeople.ARCANIST.get()).setLevel(2));
        arcanist.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        EventHooks.finalizeMobSpawn(arcanist, level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        arcanist.setPersistenceRequired();
        level.addFreshEntity(arcanist);
    }

    /** A mage of {@code element} at {@code at}, keeping near {@code home}, or null if there's no room. */
    @Nullable
    private CircleMageEntity summon(ServerLevel level, EntityType<CircleMageEntity> type, Element element, BlockPos at, BlockPos home) {
        if (!level.noCollision(type.getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5))) {
            return null;
        }
        CircleMageEntity mage = type.create(level);
        if (mage == null) {
            return null;
        }
        mage.serve(element, home);
        mage.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        EventHooks.finalizeMobSpawn(mage, level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(mage);
        return mage;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("awake", awake);
        tag.putLong("checked_day", checkedDay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        awake = tag.getBoolean("awake");
        checkedDay = tag.getLong("checked_day");
    }
}
