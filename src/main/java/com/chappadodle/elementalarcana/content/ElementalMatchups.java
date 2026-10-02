package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AffinityRules;
import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.Keystones;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * The matchup chart (see Element#multiplierAgainst): spell damage is scaled by the target
 * creature's element, and players resist the elements they've awakened, their spells and their
 * everyday damage alike (see AffinityRules). Strong and resisted spell hits sound and look
 * different, which is how a player learns a creature's element.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ElementalMatchups {
    private static final String TAG_FEEDBACK_UNTIL = "ea_matchup_fx_until";
    // Rapid hits (Hydro Jet) show the feedback at most this often per target.
    private static final int FEEDBACK_GAP_TICKS = 10;

    private ElementalMatchups() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Element spell = SpellDamage.elementOf(event.getSource());
        if (spell == null) {
            // A player's affinities (and keystones) also change their elements' everyday damage. No
            // feedback: it would play on every burning tick.
            Element nature = natureOf(event.getSource());
            if (nature != null && target instanceof Player player) {
                MagicData data = MagicAttachments.get(player);
                event.setAmount(event.getAmount() * AffinityRules.damageTaken(data.affinityElements(), nature)
                        * Keystones.damageTakenFactor(data.keystones(), nature));
            }
            return;
        }
        // A caster with Winter's Grasp hits the frozen and frosted harder.
        if (event.getSource().getEntity() instanceof Player caster) {
            boolean chilled = ElementalReactions.auraOf(target) == ElementalReactions.Aura.CRYO;
            event.setAmount(event.getAmount() * Keystones.damageDealtFactor(MagicAttachments.get(caster).keystones(), chilled));
        }
        // Creatures use the chart; players resist the elements they've awakened (and take keystones' extra).
        float multiplier;
        if (target instanceof Player player) {
            MagicData data = MagicAttachments.get(player);
            multiplier = AffinityRules.damageTaken(data.affinityElements(), spell) * Keystones.damageTakenFactor(data.keystones(), spell);
        } else {
            multiplier = spell.multiplierAgainst(CreatureElements.elementOf(target));
        }
        if (multiplier == 1f) {
            return;
        }
        event.setAmount(event.getAmount() * multiplier);
        if (target.level() instanceof ServerLevel level) {
            feedback(level, target, spell, multiplier > 1f);
        }
    }

    /**
     * The element of a vanilla damage source: a lightning strike, burning and lava, freezing,
     * drowning, falling, and falling blocks and suffocation (earth).
     */
    @Nullable
    private static Element natureOf(DamageSource source) {
        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            return Element.LIGHTNING;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return Element.FIRE;
        }
        if (source.is(DamageTypeTags.IS_FREEZING)) {
            return Element.ICE;
        }
        if (source.is(DamageTypeTags.IS_DROWNING)) {
            return Element.WATER;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return Element.WIND;
        }
        if (source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FALLING_STALACTITE)
                || source.is(DamageTypes.STALAGMITE) || source.is(DamageTypes.IN_WALL)) {
            return Element.EARTH;
        }
        return null;
    }

    private static void feedback(ServerLevel level, LivingEntity target, Element spell, boolean strong) {
        CompoundTag data = target.getPersistentData();
        long now = level.getGameTime();
        if (now < data.getLong(TAG_FEEDBACK_UNTIL)) {
            return;
        }
        data.putLong(TAG_FEEDBACK_UNTIL, now + FEEDBACK_GAP_TICKS);
        double x = target.getX();
        double y = target.getY(0.6);
        double z = target.getZ();
        if (strong) {
            // A bright crack and a burst in the spell's color.
            int color = spell.color();
            DustParticleOptions dust = new DustParticleOptions(
                    new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 1.2f);
            level.sendParticles(dust, x, y, z, 14, 0.35, 0.4, 0.35, 0.1);
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 10, 0.3, 0.4, 0.3, 0.3);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.5f);
        } else {
            // A dull thud and a grey puff.
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 8, 0.25, 0.3, 0.25, 0.02);
            level.playSound(null, x, y, z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.6f);
        }
    }
}
