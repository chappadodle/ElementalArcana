package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.StormeyeRules;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wind's level-8 spell, Stormeye: a tornado on the ground where the caster looks (up to 20 blocks
 * away) that drags in, lifts and spins whatever they may hurt (see StormeyeEntity). It levels to
 * 10 (StormeyeRules has the numbers):
 * <pre>
 * Lv1 Stormeye     4 s, reach 5              Lv6  Crushing Winds    harder, and flings what it held
 * Lv2 Gale Force   pulls harder, 5 s         Lv7  Eye Pulse         Swirls what it holds every second
 * Lv3 Absorption   takes the element it meets Lv8  Scavenger Wind    7 s, and sends loot to its caster
 * Lv4 Wider Eye    reach 7                   Lv9  Eye of the Storm  the Airborne it holds take double
 * Lv5 Wandering Storm | Twin Storms          Lv10 Great Tempest | Eye of Calm
 * </pre>
 */
public class StormeyeSpell extends Spell {

    public StormeyeSpell() {
        super(ModSchools.WIND, 50, 400, 8);
    }

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(StormeyeRules.WANDERING, StormeyeRules.TWIN);
            case 10 -> List.of(StormeyeRules.GREAT_TEMPEST, StormeyeRules.EYE_OF_CALM);
            default -> List.of();
        };
    }

    @Override
    public CastResult cast(CastContext context) {
        Vec3 at = groundUnderAim(context.level(), context.caster());
        if (at == null) {
            return CastResult.fail(Component.translatable("message.elementalarcana.stormeye.no_ground"));
        }
        int level = context.spellLevel();
        StormeyeEntity.raise(context.level(), context.caster(), at, level,
                level >= 5 ? context.branch(5) : null, level >= 10 ? context.branch(10) : null, context.power());
        return CastResult.SUCCESS;
    }

    /** The ground under where {@code caster} looks, up to 20 blocks away (or as far as they can see), or null. */
    @Nullable
    public static Vec3 groundUnderAim(ServerLevel level, LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end = eye.add(look.scale(StormeyeRules.RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
        Vec3 point = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.3));
        return groundBelow(level, point.add(0, 0.5, 0), 24);
    }

    /** The top of the first ground (or water) at or below {@code from}, at most {@code depth} blocks down, or null. */
    @Nullable
    public static Vec3 groundBelow(ServerLevel level, Vec3 from, int depth) {
        BlockPos start = BlockPos.containing(from);
        for (int down = 0; down <= depth; down++) {
            BlockPos pos = start.below(down);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) {
                return new Vec3(from.x, pos.getY() + 1, from.z);
            }
        }
        return null;
    }
}
