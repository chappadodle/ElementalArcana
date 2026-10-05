package com.chappadodle.elementalarcana.content.seeker;

import com.chappadodle.elementalarcana.api.SeekerRules;
import com.chappadodle.elementalarcana.api.StarfallRules;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Seeker's Compass (see its spec). Sneak and use it to choose what it seeks; use it to seek
 * (the nearest of that kind, searched for on the server as an explorer map's is); it points there
 * until you arrive, then settles.
 */
public class SeekersCompassItem extends Item {

    public SeekersCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        String kind = ModSeeker.seeking(stack);
        if (player.isShiftKeyDown()) {
            kind = SeekerRules.next(kind);
            stack.set(ModSeeker.SEEKING.get(), kind);
            stack.remove(ModSeeker.SOUGHT.get());
            serverPlayer.displayClientMessage(Component.translatable("message.elementalarcana.seeker.seeking", ModSeeker.kindName(kind))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.3f);
            return InteractionResultHolder.success(stack);
        }
        player.getCooldowns().addCooldown(this, SeekerRules.COOLDOWN_TICKS);
        if (server.dimension() != Level.OVERWORLD) {
            stack.remove(ModSeeker.SOUGHT.get());
            serverPlayer.displayClientMessage(Component.translatable("message.elementalarcana.seeker.elsewhere").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.success(stack);
        }
        BlockPos start = server.findNearestMapStructure(ModSeeker.structures(kind), player.blockPosition(), SeekerRules.SEARCH_RINGS, false);
        if (start == null) {
            stack.remove(ModSeeker.SOUGHT.get());
            serverPlayer.displayClientMessage(Component.translatable("message.elementalarcana.seeker.none", ModSeeker.oneName(kind))
                    .withStyle(ChatFormatting.GRAY), true);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.6f);
            return InteractionResultHolder.success(stack);
        }
        // A structure is found by the chunk it starts in: point at that chunk's middle.
        BlockPos target = new BlockPos(start.getX() + 8, player.getBlockY(), start.getZ() + 8);
        stack.set(ModSeeker.SOUGHT.get(), GlobalPos.of(server.dimension(), target));
        double dx = target.getX() + 0.5 - player.getX();
        double dz = target.getZ() + 0.5 - player.getZ();
        serverPlayer.displayClientMessage(Component.translatable("message.elementalarcana.seeker.found",
                Component.translatable("direction.elementalarcana." + StarfallRules.direction(dx, dz)), ModSeeker.oneName(kind),
                SeekerRules.roughDistance(dx, dz)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1f, 1f);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        return InteractionResultHolder.success(stack);
    }

    /** Arriving: once a second, a compass pointing somewhere close by settles. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0 || !(entity instanceof ServerPlayer player)) {
            return;
        }
        GlobalPos sought = stack.get(ModSeeker.SOUGHT.get());
        if (sought == null || sought.dimension() != server.dimension()
                || !SeekerRules.arrived(sought.pos().getX() + 0.5 - player.getX(), sought.pos().getZ() + 0.5 - player.getZ())) {
            return;
        }
        String kind = ModSeeker.seeking(stack);
        stack.remove(ModSeeker.SOUGHT.get());
        player.displayClientMessage(Component.translatable("message.elementalarcana.seeker.arrived", ModSeeker.oneName(kind))
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1f, 1.4f);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8f, 1.6f);
        MagicTriggers.fire(player, "seek_found", kind, 1);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModSeeker.SOUGHT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.seekers_compass.seeking", ModSeeker.kindName(ModSeeker.seeking(stack)))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.elementalarcana.seekers_compass.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
