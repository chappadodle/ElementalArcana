package com.chappadodle.elementalarcana.content.creature;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Binding wisps (see the Familiars spec): a worn-down wild wisp becomes its binder's familiar, of
 * the same element, where it was; a mage's earlier familiar, if any, fades away. The mage keeps the
 * familiar's id in their own data, so it's found again after a restart. And a mage's spells count as
 * their attacks, so their pets join in.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Familiars {
    private static final String TAG_FAMILIAR = ElementalArcana.MODID + "_familiar";

    private Familiars() {
    }

    /** Binds {@code wisp} to {@code mage}: the new familiar, or null if it couldn't be made. */
    @Nullable
    public static FamiliarEntity bind(ServerPlayer mage, WispEntity wisp) {
        ServerLevel level = (ServerLevel) wisp.level();
        FamiliarEntity familiar = ModCreatures.familiar(wisp.element()).create(level);
        if (familiar == null) {
            return null;
        }
        release(mage);
        Vec3 at = wisp.position();
        familiar.moveTo(at.x, at.y, at.z, wisp.getYRot(), 0);
        familiar.tame(mage);
        familiar.setPersistenceRequired();
        familiar.growWithOwner();
        familiar.setCustomName(Component.translatable("entity.elementalarcana.familiar_of",
                Component.translatable("school.elementalarcana." + wisp.element().name().toLowerCase(Locale.ROOT)), mage.getName()));
        wisp.discard();
        level.addFreshEntity(familiar);
        mage.getPersistentData().putUUID(TAG_FAMILIAR, familiar.getUUID());
        int color = wisp.element().color();
        level.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), color, 0xFFFFFF, 0.6f, 14), at.x, at.y + 0.3, at.z,
                16, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.2f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS, 1f, 1f);
        mage.displayClientMessage(Component.translatable("message.elementalarcana.familiar.bound", familiar.getName()), true);
        MagicTriggers.fire(mage, "familiar", wisp.element().name().toLowerCase(Locale.ROOT), 1);
        return familiar;
    }

    /**
     * Whatever a mage's magic hurts counts as their attack, as a sword's or an arrow's does: their
     * pets (a familiar, a wolf) take it as the foe to fight.
     */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != event.getEntity() && !attacker.level().isClientSide()) {
            attacker.setLastHurtMob(event.getEntity());
        }
    }

    /** Lets {@code mage}'s familiar go, if they have one: it fades away. */
    public static void release(ServerPlayer mage) {
        if (!mage.getPersistentData().hasUUID(TAG_FAMILIAR)) {
            return;
        }
        UUID id = mage.getPersistentData().getUUID(TAG_FAMILIAR);
        mage.getPersistentData().remove(TAG_FAMILIAR);
        for (ServerLevel level : mage.server.getAllLevels()) {
            Entity old = level.getEntity(id);
            if (old instanceof FamiliarEntity familiar) {
                level.sendParticles(ParticleTypes.POOF, familiar.getX(), familiar.getY(0.5), familiar.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
                familiar.discard();
                return;
            }
        }
    }
}
