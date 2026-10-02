package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A shrine's memory: who has found it (they get discovery XP once) and the last day each player
 * used it (once a Minecraft day each). Once a second it gives the players standing within 12 blocks
 * the Place of Power effect (stronger for those whose element it shares).
 */
public class ShrineCoreBlockEntity extends BlockEntity {
    private static final double RADIUS = 12;
    private final Set<UUID> visitors = new HashSet<>();
    private final Map<UUID, Long> lastUsedDay = new HashMap<>();

    public ShrineCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModWorld.SHRINE_CORE_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShrineCoreBlockEntity shrine) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        ShrineKind kind = state.getValue(ShrineCoreBlock.KIND);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(RADIUS),
                p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(pos.getCenter()) <= RADIUS * RADIUS)) {
            boolean kin = MagicAttachments.get(player).holdsFamily(kind.element());
            player.addEffect(new MobEffectInstance(ModContent.PLACE_OF_POWER, 60, kin ? 1 : 0, true, true, true));
        }
    }

    /** Records a first visit; returns whether this was it. */
    public boolean discover(UUID player) {
        boolean first = visitors.add(player);
        if (first) {
            setChanged();
        }
        return first;
    }

    public boolean usedToday(UUID player, long day) {
        return lastUsedDay.getOrDefault(player, -1L) == day;
    }

    public void markUsed(UUID player, long day) {
        lastUsedDay.put(player, day);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag found = new ListTag();
        visitors.forEach(id -> found.add(StringTag.valueOf(id.toString())));
        tag.put("visitors", found);
        CompoundTag used = new CompoundTag();
        lastUsedDay.forEach((id, day) -> used.put(id.toString(), LongTag.valueOf(day)));
        tag.put("last_used_day", used);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        visitors.clear();
        for (Tag entry : tag.getList("visitors", Tag.TAG_STRING)) {
            try {
                visitors.add(UUID.fromString(entry.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // A damaged entry: that player just discovers it again.
            }
        }
        lastUsedDay.clear();
        CompoundTag used = tag.getCompound("last_used_day");
        for (String key : used.getAllKeys()) {
            try {
                lastUsedDay.put(UUID.fromString(key), used.getLong(key));
            } catch (IllegalArgumentException ignored) {
                // A damaged entry: that player can use it again today.
            }
        }
    }
}
