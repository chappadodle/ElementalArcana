package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Keystones;
import com.chappadodle.elementalarcana.api.Stat;
import com.chappadodle.elementalarcana.api.StatRules;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Set;

/**
 * The stats that live on a player's vanilla attributes: Vitality's max health, and the keystones'
 * max health, speed and knockback (see Keystones).
 */
public final class PlayerStats {
    private static final ResourceLocation VITALITY = ElementalArcana.id("vitality");
    private static final ResourceLocation KEYSTONE_HEALTH = ElementalArcana.id("keystone_health");
    private static final ResourceLocation KEYSTONE_SPEED = ElementalArcana.id("keystone_speed");
    private static final ResourceLocation KEYSTONE_KNOCKBACK = ElementalArcana.id("keystone_knockback");

    private PlayerStats() {
    }

    /** Re-applies Vitality and the keystones (after a stat, level or tree change, a login or a respawn). */
    public static void apply(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        health.removeModifier(VITALITY);
        double bonus = StatRules.healthMultiplier(data.stat(Stat.VITALITY)) - 1;
        if (bonus > 0) {
            health.addTransientModifier(new AttributeModifier(VITALITY, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        Set<String> keystones = data.keystones();
        set(player, Attributes.MAX_HEALTH, KEYSTONE_HEALTH, Keystones.healthBonus(keystones), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(player, Attributes.MOVEMENT_SPEED, KEYSTONE_SPEED, Keystones.speedBonus(keystones), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(player, Attributes.KNOCKBACK_RESISTANCE, KEYSTONE_KNOCKBACK, keystones.contains(Keystones.MOUNTAIN_HEART) ? 1 : 0,
                AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Replaces modifier {@code id} on {@code attribute}; an amount of 0 just removes it. */
    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
