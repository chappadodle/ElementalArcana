package com.chappadodle.elementalarcana.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Ley Anchor's name (see the Ley Anchors spec): the name the item was given in an anvil, kept by
 * the stone and given back to the item when it's broken (its loot table copies it), so the ley menu
 * can call the place "Home".
 */
public class LeyAnchorBlockEntity extends BlockEntity implements Nameable {
    @Nullable
    private Component name;

    public LeyAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModWorld.LEY_ANCHOR_ENTITY.get(), pos, state);
    }

    /** Its name as written, or "" if it has none. */
    public String name() {
        return name == null ? "" : name.getString();
    }

    @Override
    public Component getName() {
        return name != null ? name : Component.translatable("block.elementalarcana.ley_anchor");
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return name;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (name != null) {
            tag.putString("CustomName", Component.Serializer.toJson(name, registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        name = tag.contains("CustomName", 8) ? parseCustomNameSafe(tag.getString("CustomName"), registries) : null;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        name = input.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, name);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("CustomName");
    }
}
