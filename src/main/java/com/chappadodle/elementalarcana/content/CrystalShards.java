package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellShield;
import com.chappadodle.elementalarcana.content.spell.StoneSkinSpell;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Crystallize shards (see ElementalReactions#earthHit): a shard sits where a creature's burning,
 * wetness or frost hardened, tinted that aura's colour, and gives the first player to walk into it
 * absorption hearts (3 hearts for 10 seconds). With Stone Skin's Crystal Carapace it also tops up
 * the shield. Tracked on the server with no entity, drawn with particles, never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class CrystalShards {
    private static final double PICKUP_RADIUS = 1.2;
    private static final float ABSORPTION = 6f;
    private static final int ABSORPTION_TICKS = 200;
    private static final float CARAPACE_SHIELD = 4f;

    private record Shard(ServerLevel level, Vec3 at, int color, long endsAt) {
    }

    private static final List<Shard> SHARDS = new ArrayList<>();

    private CrystalShards() {
    }

    /** A shard at {@code at} that lasts {@code ticks}. */
    public static void spawn(ServerLevel level, Vec3 at, int color, int ticks) {
        SHARDS.add(new Shard(level, at, color, level.getGameTime() + ticks));
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1f, 1.2f);
        dust(level, at, color, 14, 0.3);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || SHARDS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Shard> it = SHARDS.iterator(); it.hasNext(); ) {
            Shard shard = it.next();
            if (shard.level() != level) {
                continue;
            }
            if (now >= shard.endsAt()) {
                it.remove();
                continue;
            }
            if (now % 4 == 0) {
                dust(level, shard.at().add(0, 0.5 + Math.sin(now * 0.15) * 0.1, 0), shard.color(), 2, 0.12);
                level.sendParticles(ParticleTypes.END_ROD, shard.at().x, shard.at().y + 0.5, shard.at().z, 1, 0.05, 0.1, 0.05, 0);
            }
            ServerPlayer taker = level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(shard.at(), shard.at()).inflate(PICKUP_RADIUS),
                    player -> player.isAlive() && !player.isSpectator() && player.position().distanceTo(shard.at()) <= PICKUP_RADIUS + 0.5)
                    .stream().findFirst().orElse(null);
            if (taker != null) {
                it.remove();
                pickUp(taker, shard);
            }
        }
    }

    private static void pickUp(ServerPlayer player, Shard shard) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_TICKS, 0, false, false, true));
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), ABSORPTION));
        SpellShield shield = SpellShield.of(player);
        if (shield.isActive() && shield.hasBranch(5, StoneSkinSpell.CARAPACE)) {
            shield.setAmount(shield.amount() + CARAPACE_SHIELD);
            SpellShield.sync(player);
        }
        dust(shard.level(), player.position().add(0, 1.0, 0), shard.color(), 16, 0.4);
        shard.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
    }

    private static void dust(ServerLevel level, Vec3 at, int color, int count, double spread) {
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 1.2f);
        level.sendParticles(dust, at.x, at.y + 0.2, at.z, count, spread, spread, spread, 0.02);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SHARDS.clear();
    }
}
