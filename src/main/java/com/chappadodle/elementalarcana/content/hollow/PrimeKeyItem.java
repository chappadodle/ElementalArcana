package com.chappadodle.elementalarcana.content.hollow;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Prime Key (see the Hollow spec): the four Sovereign Hearts made one. Hold it for two seconds in
 * the Overworld (the key hums and the air tears in front of you) and it is spent: you, and everyone
 * within 6 blocks, are taken to the Hollow.
 */
public class PrimeKeyItem extends Item {
    private static final int TURN_TICKS = 40;

    public PrimeKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.dimension() != Level.OVERWORLD) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.elementalarcana.prime_key.wrong_place").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return TURN_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    /** The air tears in front of its holder, wider as the key turns. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        float turned = 1f - remaining / (float) TURN_TICKS;
        Vec3 at = entity.getEyePosition().add(entity.getLookAngle().scale(1.6));
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 4 + Math.round(turned * 12), 0.2 + turned * 0.6,
                0.3 + turned * 0.8, 0.2 + turned * 0.6, 0.02);
        if (remaining % 10 == 0) {
            server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.6f + turned);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && level.dimension() == Level.OVERWORLD) {
            stack.consume(1, player);
            HollowEvents.enter(player);
        }
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.prime_key.desc").withStyle(ChatFormatting.GRAY));
    }
}
