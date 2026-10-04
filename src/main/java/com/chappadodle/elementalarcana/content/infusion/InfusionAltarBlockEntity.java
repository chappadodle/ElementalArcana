package com.chappadodle.elementalarcana.content.infusion;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.InfusionRules;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.mob.MobCasting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * An Infusion Altar's contents: the item set on it, and the Essence poured into its basin (its
 * element and how many). Synced to clients, which draw the item and the basin's glow.
 */
public class InfusionAltarBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    @Nullable
    private Element element;
    private int charge;

    public InfusionAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModInfusion.ALTAR_ENTITY.get(), pos, state);
    }

    public ItemStack item() {
        return item;
    }

    /** The element of the Essence in the basin, or null if it's empty. */
    @Nullable
    public Element element() {
        return charge > 0 ? element : null;
    }

    public int charge() {
        return charge;
    }

    /** Sets {@code stack} on the empty altar. */
    public void set(ServerLevel level, ItemStack stack) {
        item = stack;
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1f, 1.2f);
        changed();
    }

    /** Takes the item back (the basin keeps its Essence). */
    public ItemStack take(ServerLevel level) {
        ItemStack out = item;
        item = ItemStack.EMPTY;
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1f, 1f);
        changed();
        return out;
    }

    /** Pours one Essence of {@code essence} in; the eighth infuses the item. Returns false if the basin refuses it. */
    public boolean pour(ServerLevel level, Player player, Element essence) {
        if (item.isEmpty() || !InfusionRules.canPour(element(), charge, essence)) {
            return false;
        }
        element = essence;
        charge++;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.9;
        double z = worldPosition.getZ() + 0.5;
        level.sendParticles(MobCasting.handsParticle(essence), x, y, z, 6 + charge, 0.25, 0.1, 0.25, 0.02);
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6f, 0.8f + charge * 0.08f);
        if (charge >= InfusionRules.ESSENCE_NEEDED) {
            infuse(level, player);
        }
        changed();
        return true;
    }

    private void infuse(ServerLevel level, Player player) {
        Element infused = element;
        Infusions.infuse(item, infused);
        charge = 0;
        element = null;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.15;
        double z = worldPosition.getZ() + 0.5;
        level.sendParticles(MobCasting.handsParticle(infused), x, y, z, 40, 0.3, 0.3, 0.3, 0.12);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 16, 0.2, 0.2, 0.2, 0.08);
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 1f);
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.4f);
        if (player instanceof ServerPlayer server) {
            MagicTriggers.fire(server, "infusion", infused.name().toLowerCase(Locale.ROOT), 1);
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!item.isEmpty()) {
            tag.put("item", item.save(registries));
        }
        // Always written: a client ignores an empty update, so an emptied altar must still say so.
        tag.putInt("charge", element == null ? 0 : charge);
        if (element != null && charge > 0) {
            tag.putString("element", element.name().toLowerCase(Locale.ROOT));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        item = tag.contains("item") ? ItemStack.parse(registries, tag.getCompound("item")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        element = null;
        for (Element candidate : Element.values()) {
            if (candidate.name().equalsIgnoreCase(tag.getString("element"))) {
                element = candidate;
            }
        }
        charge = element == null ? 0 : Math.min(tag.getInt("charge"), InfusionRules.ESSENCE_NEEDED - 1);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
