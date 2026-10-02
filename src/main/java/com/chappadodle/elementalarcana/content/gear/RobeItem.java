package com.chappadodle.elementalarcana.content.gear;

import com.chappadodle.elementalarcana.api.StatGear;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Map;

/** A piece of mage's robes: light armor that adds to its wearer's stats while worn. */
public class RobeItem extends ArmorItem implements StatGear {
    private final int requiredLevel;
    private final Map<String, Integer> stats;

    public RobeItem(Holder<ArmorMaterial> material, Type type, int requiredLevel, Map<String, Integer> stats, Properties properties) {
        super(material, type, properties);
        this.requiredLevel = requiredLevel;
        this.stats = stats;
    }

    @Override
    public Map<String, Integer> statBonus(ItemStack stack) {
        return stats;
    }

    @Override
    public int requiredLevel() {
        return requiredLevel;
    }

    @Override
    public boolean countsIn(EquipmentSlot slot) {
        return slot == getEquipmentSlot();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GearTooltips.add(this, stack, tooltip);
    }
}
