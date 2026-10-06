package com.chappadodle.elementalarcana.content.wonder;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A Bottled Glowmoth (see the Wonders of the Wild spec), of an element (the {@code element}
 * component): set it on a block and it's a Moth Jar; use it in the air and the moth goes free.
 */
public class BottledGlowmothItem extends BlockItem {

    public BottledGlowmothItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack of(Element element) {
        ItemStack stack = new ItemStack(ModWonders.BOTTLED_GLOWMOTH.get());
        stack.set(ModGear.ELEMENT.get(), element);
        return stack;
    }

    public static Element elementOf(ItemStack stack) {
        Element element = stack.get(ModGear.ELEMENT.get());
        return element == null ? Element.RADIANCE : element;
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(MothJarBlock.ELEMENT, elementOf(context.getItemInHand()).ordinal());
    }

    /** Used in the air: the moth goes free, and the bottle is left. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            GlowmothEntity moth = ModWonders.GLOWMOTH.get().create(level);
            if (moth != null) {
                Vec3 at = player.getEyePosition().add(player.getLookAngle());
                moth.setElement(elementOf(stack));
                moth.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
                level.addFreshEntity(moth);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1f, 1.3f);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)),
                level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        Element element = elementOf(stack);
        return Component.translatable("item.elementalarcana.bottled_glowmoth.named",
                Component.translatable("school.elementalarcana." + element.name().toLowerCase(Locale.ROOT))).withColor(element.color());
    }
}
