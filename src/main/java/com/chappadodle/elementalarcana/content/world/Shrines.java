package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.api.AwakeningRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.content.Awakenings;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.CastingService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;


/**
 * Using a Shrine Core (see the shrines spec). The first time each player finds a shrine, they get
 * discovery XP. Then, once a Minecraft day per shrine:
 * <ul>
 * <li>a player whose magic still sleeps communes with it: a one in two chance that their magic wakes,
 * leaning to the shrine's element (Ice shrines give Water now and then, as kin);</li>
 * <li>an awakened player is blessed: their mana is refilled and for 10 minutes the shrine's element
 * family gets +5 Affinity and +2 Potency (see GearStats).</li>
 * </ul>
 */
public final class Shrines {
    private static final double COMMUNE_CHANCE = 0.5;
    private static final int BLESSING_TICKS = 20 * 60 * 10;
    // Discovery is worth about three kills of the player's own level.
    private static final int DISCOVERY_KILLS = 3;

    private Shrines() {
    }

    public static void use(ServerPlayer player, ShrineCoreBlockEntity shrine, ShrineKind kind) {
        ServerLevel level = player.serverLevel();
        MagicData data = MagicAttachments.get(player);
        Component name = Component.translatable("school.elementalarcana." + kind.getSerializedName()).withColor(kind.element().color());
        if (shrine.discover(player.getUUID())) {
            int xp = Progression.creatureXp(data.level()) * DISCOVERY_KILLS;
            player.sendSystemMessage(Component.translatable("message.elementalarcana.shrine.discovered", name, xp).withStyle(ChatFormatting.LIGHT_PURPLE));
            CastingService.grantXp(player, xp);
        }
        long day = level.getDayTime() / AwakeningRules.DAY_TICKS;
        if (shrine.usedToday(player.getUUID(), day)) {
            player.displayClientMessage(Component.translatable("message.elementalarcana.shrine.spent").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        shrine.markUsed(player.getUUID(), day);
        double x = shrine.getBlockPos().getX() + 0.5;
        double y = shrine.getBlockPos().getY() + 0.8;
        double z = shrine.getBlockPos().getZ() + 0.5;
        if (!data.isAwakened()) {
            if (player.getRandom().nextDouble() < COMMUNE_CHANCE) {
                Element element = AwakeningRules.pick(AwakeningRules.weights(kind.element()), player.getRandom().nextDouble());
                if (element != null) {
                    Awakenings.wake(player, element, Component.translatable("message.elementalarcana.awakening.shrine", name));
                    return;
                }
            }
            player.sendSystemMessage(Component.translatable("message.elementalarcana.shrine.stirs", name).withStyle(ChatFormatting.GRAY));
            level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 30, 0.6, 0.6, 0.6, 0.6);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 0.7f);
            return;
        }
        data.fillMana();
        MagicAttachments.sync(player);
        player.addEffect(new MobEffectInstance(ModContent.blessing(kind.element().family()), BLESSING_TICKS));
        player.sendSystemMessage(Component.translatable("message.elementalarcana.shrine.blessed", name).withStyle(ChatFormatting.LIGHT_PURPLE));
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 24, 0.4, 0.6, 0.4, 0.06);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
        level.playSound(null, x, y, z, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 1.3f);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.0f);
    }
}
