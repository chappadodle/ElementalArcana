package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Stat;
import com.chappadodle.elementalarcana.api.StatRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The stats that live on a player's vanilla attributes: Vitality's max health. */
public final class PlayerStats {
    private static final ResourceLocation VITALITY = ElementalArcana.id("vitality");

    private PlayerStats() {
    }

    /** Re-applies Vitality (after a stat or level change, a login or a respawn). */
    public static void apply(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        int vitality = MagicAttachments.get(player).stats().get(Stat.VITALITY);
        health.removeModifier(VITALITY);
        double bonus = StatRules.healthMultiplier(vitality) - 1;
        if (bonus > 0) {
            health.addTransientModifier(new AttributeModifier(VITALITY, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
