package com.chappadodle.elementalarcana.content.cantrip;

import com.chappadodle.elementalarcana.api.CantripRules;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Mage Light (a cantrip): sets a small floating orb of light where you look, up to 24 blocks off
 * (in the open air at that distance if you look at nothing). It stays until it's punched out.
 */
public class MageLightSpell extends Spell {
    public MageLightSpell() {
        super(ModSchools.ARCANE, 4, 10);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer player = context.caster();
        ServerLevel level = context.level();
        Vec3 eye = context.eyePosition();
        Vec3 end = eye.add(context.look().scale(CantripRules.LIGHT_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockPos pos = hit.getType() == HitResult.Type.MISS ? BlockPos.containing(end) : hit.getBlockPos().relative(hit.getDirection());
        BlockState there = level.getBlockState(pos);
        if (!there.canBeReplaced() || !level.getFluidState(pos).isEmpty() || !level.mayInteract(player, pos) || !player.mayBuild()
                || !level.isInWorldBounds(pos)) {
            return CastResult.fail(Component.translatable("message.elementalarcana.cantrip.no_room"));
        }
        level.setBlockAndUpdate(pos, ModCantrips.MAGE_LIGHT.get().defaultBlockState());
        Vec3 centre = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y, centre.z, 8, 0.15, 0.15, 0.15, 0.03);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
        return CastResult.SUCCESS;
    }
}
