package com.chappadodle.elementalarcana.content.cantrip;

import com.chappadodle.elementalarcana.api.Spell;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

/**
 * Cantrip Scrolls in the world's old places (see the Cantrips spec): when one of the chests its
 * conditions name is filled, now and then ({@code chance}) a scroll of a random cantrip is added.
 * Data: data/elementalarcana/loot_modifiers/cantrip_scrolls_*.json.
 */
public class CantripScrollsModifier extends LootModifier {
    public static final MapCodec<CantripScrollsModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(Codec.floatRange(0f, 1f).fieldOf("chance").forGetter(modifier -> modifier.chance))
            .apply(instance, CantripScrollsModifier::new));

    private final float chance;

    public CantripScrollsModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        List<Spell> cantrips = ModCantrips.cantrips();
        if (!cantrips.isEmpty() && context.getRandom().nextFloat() < chance) {
            loot.add(ModCantrips.scroll(cantrips.get(context.getRandom().nextInt(cantrips.size()))));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
