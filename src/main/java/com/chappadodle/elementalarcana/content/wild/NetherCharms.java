package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.NetherCreatureRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The Creatures of the Nether's trophies (see their spec), carried or in the Charm Pouch: the Ashen
 * Shroud, which the undead look past (unless its bearer struck them first; bosses always see them),
 * and the Houndstooth Charm, a sixth faster in the Nether.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class NetherCharms {
    private static final ResourceLocation HOUNDSTOOTH = ElementalArcana.id("houndstooth_charm");

    private NetherCharms() {
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        LivingEntity mob = event.getEntity();
        if (!mob.getType().is(EntityTypeTags.UNDEAD) || mob.getType().is(Tags.EntityTypes.BOSSES) || mob.getLastHurtByMob() == player) {
            return;
        }
        if (WildCharms.carries(player, ModWild.ASHEN_SHROUD.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 10 != 0) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean wanted = player.level().dimension() == Level.NETHER && WildCharms.carries(player, ModWild.HOUNDSTOOTH_CHARM.get());
        if (wanted && !speed.hasModifier(HOUNDSTOOTH)) {
            speed.addTransientModifier(new AttributeModifier(HOUNDSTOOTH, NetherCreatureRules.HOUNDSTOOTH_SPEED,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!wanted && speed.hasModifier(HOUNDSTOOTH)) {
            speed.removeModifier(HOUNDSTOOTH);
        }
    }
}
