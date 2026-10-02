package com.chappadodle.elementalarcana.content.brew;

import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A Mana Draught's effect: at once, 30% of the drinker's mana comes back (60% for a strong one).
 * A splash potion gives less the farther from where it lands. Mana that still sleeps can't take it.
 */
public class ManaRestorationEffect extends InstantenousMobEffect {
    private static final double SHARE_PER_LEVEL = 0.3;

    public ManaRestorationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x6A5BFF);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        restore(entity, amplifier, 1.0);
        return true;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource, LivingEntity entity, int amplifier,
                                        double effectiveness) {
        restore(entity, amplifier, effectiveness);
    }

    private static void restore(LivingEntity entity, int amplifier, double effectiveness) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        if (!data.isAwakened()) {
            return;
        }
        data.setMana(data.mana() + (float) (data.maxMana() * SHARE_PER_LEVEL * (amplifier + 1) * effectiveness));
        MagicAttachments.sync(player);
    }
}
