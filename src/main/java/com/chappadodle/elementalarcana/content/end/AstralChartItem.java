package com.chappadodle.elementalarcana.content.end;

import com.chappadodle.elementalarcana.api.FarIslesRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * An Astral Chart (see the Far Isles spec), from an observatory's vault (or now and then a
 * Stargazer): used in the End, a trail of starlight points the way home, toward the End's heart
 * (where the exit portal stands), and it says how far; anywhere else its stars are wrong. It isn't
 * used up.
 */
public class AstralChartItem extends Item {
    public AstralChartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        player.getCooldowns().addCooldown(this, FarIslesRules.CHART_COOLDOWN_TICKS);
        if (level.dimension() != Level.END) {
            player.displayClientMessage(Component.translatable("message.elementalarcana.astral_chart.wrong_sky").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.consume(stack);
        }
        double dx = -player.getX();
        double dz = -player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 48) {
            player.displayClientMessage(Component.translatable("message.elementalarcana.astral_chart.home").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResultHolder.consume(stack);
        }
        // A trail of starlight along the way home, shown to its reader.
        Vec3 way = new Vec3(dx / distance, 0, dz / distance);
        Vec3 from = player.getEyePosition().subtract(0, 0.3, 0);
        for (int i = 2; i <= FarIslesRules.TRAIL_LENGTH; i++) {
            Vec3 at = from.add(way.scale(i));
            server.sendParticles(serverPlayer, ParticleTypes.END_ROD, true, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        player.displayClientMessage(Component.translatable("message.elementalarcana.astral_chart.way", (int) Math.round(distance / 10) * 10,
                Component.translatable("direction.elementalarcana." + FarIslesRules.direction(dx, dz))).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
