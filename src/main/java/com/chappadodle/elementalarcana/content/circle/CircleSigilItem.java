package com.chappadodle.elementalarcana.content.circle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;

/**
 * The Sigil of the Circle (see the Circle spec, part 2), bought for marks: used, it calls a mage of
 * the Circle, of any element, to fight at the user's side for two minutes (CircleMageEntity's
 * companion ways). Only one answers a player at a time: a second sigil sends the first home.
 */
public class CircleSigilItem extends Item {
    /** How long the mage stays, in ticks. */
    public static final int STAY_TICKS = 2400;

    public CircleSigilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            CircleMageEntity mage = ModCircle.CIRCLE_MAGE.get().create(server);
            if (mage == null) {
                return InteractionResultHolder.fail(stack);
            }
            for (CircleMageEntity other : server.getEntities(ModCircle.CIRCLE_MAGE.get(), other -> player.getUUID().equals(other.companionId()))) {
                other.bowOut();
            }
            Vec3 at = beside(player);
            mage.moveTo(at.x, at.y, at.z, player.getYRot(), 0f);
            EventHooks.finalizeMobSpawn(mage, server, server.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
            mage.attend(player, server.getGameTime() + STAY_TICKS);
            server.addFreshEntity(mage);
            server.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 30, 0.3, 0.6, 0.3, 0.08);
            server.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
            stack.consume(1, player);
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** A step to the player's right (or where they stand, if that's blocked). */
    private static Vec3 beside(Player player) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 spot = player.position().add(right.scale(1.5));
        BlockPos pos = BlockPos.containing(spot);
        Level level = player.level();
        boolean free = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        return free ? spot : player.position();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.sigil_of_the_circle.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
