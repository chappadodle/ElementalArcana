package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.api.CryptRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.Attunement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * The crypt's dead: the land's own (strays in the snow, husks in the desert, else zombies and
 * skeletons), Attuned to the crypt's element, stepping out of a coffin (see the Arcane Crypts spec).
 */
public final class CryptDead {

    private CryptDead() {
    }

    /** One of the dead steps out in front of the coffin whose lower half is at {@code lower}. */
    @Nullable
    public static Mob raise(ServerLevel level, BlockPos lower, Element element, Direction facing, @Nullable LivingEntity target) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        BlockPos at = lower.relative(facing);
        Mob mob = spawn(level, creatureFor(level, at), at, facing);
        if (mob == null) {
            return null;
        }
        if (!Attunement.attune(mob, element, CryptRules.coffinRank(level.getRandom().nextFloat()))) {
            // Born to another element: a zombie instead.
            mob.discard();
            mob = spawn(level, EntityType.ZOMBIE, at, facing);
            if (mob == null || !Attunement.attune(mob, element, CryptRules.coffinRank(level.getRandom().nextFloat()))) {
                return mob;
            }
        }
        if (target != null && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            mob.setTarget(target);
        }
        return mob;
    }

    @Nullable
    private static Mob spawn(ServerLevel level, EntityType<? extends Mob> type, BlockPos at, Direction facing) {
        Mob mob = type.spawn(level, at, MobSpawnType.STRUCTURE);
        if (mob == null) {
            return null;
        }
        if (mob instanceof Zombie zombie) {
            zombie.setBaby(false);
        }
        mob.setYRot(facing.toYRot());
        mob.setYHeadRot(facing.toYRot());
        mob.yBodyRot = facing.toYRot();
        return mob;
    }

    /** The land's dead: strays in the snow, husks in the desert and badlands, else zombies and skeletons. */
    private static EntityType<? extends Mob> creatureFor(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        boolean first = level.getRandom().nextBoolean();
        if (biome.value().coldEnoughToSnow(pos.atY(level.getSeaLevel()))) {
            return first ? EntityType.STRAY : EntityType.ZOMBIE;
        }
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BADLANDS)) {
            return first ? EntityType.HUSK : EntityType.SKELETON;
        }
        return first ? EntityType.ZOMBIE : EntityType.SKELETON;
    }
}
