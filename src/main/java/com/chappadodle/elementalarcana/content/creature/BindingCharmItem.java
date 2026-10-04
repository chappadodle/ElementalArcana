package com.chappadodle.elementalarcana.content.creature;

import com.chappadodle.elementalarcana.api.FamiliarRules;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;

/**
 * A Binding Charm (see the Familiars spec): used on a wild wisp worn down to a quarter of its
 * health, it binds the wisp to the mage as their familiar (Familiars#bind).
 */
public class BindingCharmItem extends Item {
    public BindingCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof WispEntity wisp)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer mage = (ServerPlayer) player;
        String refusal = null;
        if (!MagicAttachments.get(mage).isAwakened()) {
            refusal = "message.elementalarcana.familiar.asleep";
        } else if (wisp.isGuardian()) {
            refusal = "message.elementalarcana.familiar.guardian";
        } else if (!FamiliarRules.canBind(wisp.getHealth(), wisp.getMaxHealth())) {
            refusal = "message.elementalarcana.familiar.too_strong";
        }
        if (refusal != null) {
            mage.displayClientMessage(Component.translatable(refusal), true);
            ((ServerLevel) mage.level()).sendParticles(ParticleTypes.SMOKE, wisp.getX(), wisp.getY(0.5), wisp.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            mage.level().playSound(null, wisp.getX(), wisp.getY(), wisp.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.6f);
            return InteractionResult.CONSUME;
        }
        if (Familiars.bind(mage, wisp) != null) {
            stack.consume(1, mage);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.binding_charm.tip").withStyle(ChatFormatting.GRAY));
    }
}
