package com.chappadodle.elementalarcana.content.circle;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CommissionRules;
import com.chappadodle.elementalarcana.api.DuelRules;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The Circle's duels (see the Circle spec, part 3). A player holds a Mark of the Circle out to one
 * of an Enclave's mages: the wager. The mage goes to the Enclave's ring; once both are in it, a
 * count, and they fight, the mage at the player's level (the Archmagister a little above: a duel
 * tests skill, not the land's strength), fair game for each other's spells (SpellTargets) till one
 * yields (a blow
 * that would leave them below a fifth of their health stops there: no one dies of a duel), the
 * player steps out of the ring, or two minutes pass (a draw). Server-side and never saved: a
 * restart ends every bout.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Duels {
    private enum Phase {
        GATHERING, COUNT, BOUT
    }

    private enum Outcome {
        WON, YIELDED, LEFT_RING, DRAW, NO_SHOW
    }

    /** A bout: the player, the mage, the ring's middle (where they stand), and how far along it is. */
    private static final class Bout {
        final UUID player;
        final CircleMageEntity mage;
        final ServerLevel level;
        final BlockPos ring;
        Phase phase = Phase.GATHERING;
        long phaseStart;
        /** Who yielded to a blow (set as the blow lands; the bout ends on the next tick). */
        @Nullable
        Outcome yielded;

        Bout(UUID player, CircleMageEntity mage, ServerLevel level, BlockPos ring) {
            this.player = player;
            this.mage = mage;
            this.level = level;
            this.ring = ring;
            this.phaseStart = level.getGameTime();
        }
    }

    private static final Map<UUID, Bout> BY_PLAYER = new HashMap<>();

    private Duels() {
    }

    /** Hooks the duels into who spells may touch (ModCircle#register). */
    static void init() {
        SpellTargets.duels(Duels::duelling);
    }

    /** Whether {@code a} and {@code b} are the two of a bout under way (either way round). */
    private static boolean duelling(Entity a, Entity b) {
        if (BY_PLAYER.isEmpty()) {
            return false;
        }
        Bout bout = a instanceof Player ? BY_PLAYER.get(a.getUUID()) : b instanceof Player ? BY_PLAYER.get(b.getUUID()) : null;
        return bout != null && bout.phase == Phase.BOUT && (a == bout.mage || b == bout.mage);
    }

    /** Whether {@code mage} is promised to a bout. */
    static boolean busy(CircleMageEntity mage) {
        for (Bout bout : BY_PLAYER.values()) {
            if (bout.mage == mage) {
                return true;
            }
        }
        return false;
    }

    /** The bout {@code entity} is fighting, as the player or the mage, or null. */
    @Nullable
    private static Bout boutOf(Entity entity) {
        if (entity instanceof Player) {
            return BY_PLAYER.get(entity.getUUID());
        }
        for (Bout bout : BY_PLAYER.values()) {
            if (bout.mage == entity) {
                return bout;
            }
        }
        return null;
    }

    /** The duels {@code player} has won. */
    public static int wins(Player player) {
        return player.getData(ModCircle.DUEL_WINS);
    }

    /**
     * {@code player} holds out {@code wager} (a Mark of the Circle) to {@code mage}: a challenge. The
     * mage takes the mark and goes to the ring, or declines with a word (the mark kept).
     */
    static void challenge(CircleMageEntity mage, ServerPlayer player, ItemStack wager) {
        ServerLevel level = player.serverLevel();
        BlockPos ring = mage.ring();
        String refusal = ring == null ? "no_ring"
                : BY_PLAYER.containsKey(player.getUUID()) ? "player_busy"
                : busy(mage) ? "busy"
                : mage.lastDuelDay() == day(level) ? "tired"
                : mage.isArchmagister() && wins(player) < DuelRules.ARCHMAGISTER_WINS ? "not_yet"
                : null;
        if (refusal != null) {
            mage.say(player, Component.translatable("circle.elementalarcana.duel." + refusal, DuelRules.ARCHMAGISTER_WINS));
            return;
        }
        wager.shrink(1);
        mage.markDuelled(day(level));
        BY_PLAYER.put(player.getUUID(), new Bout(player.getUUID(), mage, level, ring));
        mage.goToRing(ring);
        mage.say(player, Component.translatable(mage.isArchmagister() ? "circle.elementalarcana.duel.accept_archmagister"
                : "circle.elementalarcana.duel.accept"));
        bell(level, ring, 1.2f);
    }

    private static long day(ServerLevel level) {
        return level.getDayTime() / 24000L;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (BY_PLAYER.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (Iterator<Bout> it = BY_PLAYER.values().iterator(); it.hasNext(); ) {
            Bout bout = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(bout.player);
            Outcome outcome = step(bout, player);
            if (outcome != null) {
                it.remove();
                end(bout, player, outcome);
            }
        }
    }

    /** One tick of a bout; how it ended, or null while it goes on. */
    @Nullable
    private static Outcome step(Bout bout, @Nullable ServerPlayer player) {
        CircleMageEntity mage = bout.mage;
        if (!mage.isAlive()) {
            return bout.phase == Phase.BOUT ? Outcome.WON : Outcome.NO_SHOW;
        }
        if (player == null || !player.isAlive() || player.level() != bout.level) {
            return bout.phase == Phase.BOUT ? Outcome.YIELDED : Outcome.NO_SHOW;
        }
        long now = bout.level.getGameTime();
        long ticks = now - bout.phaseStart;
        switch (bout.phase) {
            case GATHERING -> {
                if (!inRing(mage, bout.ring, false) && ticks >= DuelRules.WALK_TICKS) {
                    mage.stepInto(bout.ring);
                }
                if (inRing(player, bout.ring, false) && inRing(mage, bout.ring, false)) {
                    next(bout, Phase.COUNT, now);
                    mage.getNavigation().stop();
                } else if (ticks >= DuelRules.GATHER_TICKS) {
                    return Outcome.NO_SHOW;
                }
            }
            case COUNT -> {
                mage.getLookControl().setLookAt(player, 30f, 30f);
                int count = DuelRules.count(ticks);
                if (ticks % 20 == 0 && count > 0) {
                    title(player, Component.literal(String.valueOf(count)));
                    bout.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(),
                            SoundSource.PLAYERS, 1f, 1f);
                }
                if (count == 0) {
                    if (!inRing(player, bout.ring, true)) {
                        // Stepped out before it began: back to waiting (the clock goes on).
                        bout.phase = Phase.GATHERING;
                        return null;
                    }
                    next(bout, Phase.BOUT, now);
                    title(player, Component.translatable("circle.elementalarcana.duel.begin"));
                    bell(bout.level, bout.ring, 1f);
                    mage.matchLevel(player);
                    mage.fight(player);
                }
            }
            case BOUT -> {
                if (bout.yielded != null) {
                    return bout.yielded;
                }
                if (!inRing(player, bout.ring, true)) {
                    return Outcome.LEFT_RING;
                }
                if (ticks >= DuelRules.BOUT_TICKS) {
                    return Outcome.DRAW;
                }
                mage.fight(player);
            }
        }
        return null;
    }

    private static void next(Bout bout, Phase phase, long now) {
        bout.phase = phase;
        bout.phaseStart = now;
    }

    private static boolean inRing(Entity entity, BlockPos ring, boolean leaving) {
        return Math.abs(entity.getY() - ring.getY()) <= 3
                && DuelRules.inRing(entity.getX() - (ring.getX() + 0.5), entity.getZ() - (ring.getZ() + 0.5), leaving);
    }

    /** The bout over: a bell, a word, the wager and the prize as it fell out, the mage back to its post. */
    private static void end(Bout bout, @Nullable ServerPlayer player, Outcome outcome) {
        CircleMageEntity mage = bout.mage;
        boolean fought = bout.phase == Phase.BOUT;
        mage.endDuel();
        if (fought) {
            bell(bout.level, bout.ring, 0.8f);
        }
        if (player == null) {
            return;
        }
        if (fought) {
            title(player, Component.empty());
        }
        int marks = switch (outcome) {
            case WON -> 1 + DuelRules.prize(mage.isArchmagister());
            case DRAW, NO_SHOW -> 1;
            case YIELDED, LEFT_RING -> 0;
        };
        if (marks > 0) {
            ItemStack paid = new ItemStack(ModCircle.MARK.get(), marks);
            if (!player.getInventory().add(paid)) {
                player.drop(paid, false);
            }
        }
        if (outcome == Outcome.WON) {
            int prize = DuelRules.prize(mage.isArchmagister());
            ExperienceOrb.award(bout.level, player.position(), prize * CommissionRules.XP_PER_MARK);
            player.setData(ModCircle.DUEL_WINS, wins(player) + 1);
            MagicTriggers.fire(player, "duel_won", mage.isArchmagister() ? "archmagister" : null, 1);
        }
        String line = switch (outcome) {
            case WON -> "won";
            case YIELDED -> "yielded";
            case LEFT_RING -> "left_ring";
            case DRAW -> "draw";
            case NO_SHOW -> "no_show";
        };
        mage.say(player, Component.translatable("circle.elementalarcana.duel." + line));
    }

    /** A blow in a bout stops at the yield (no one dies of a duel), and its taker yields. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (BY_PLAYER.isEmpty() || victim.level().isClientSide() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        Bout bout = boutOf(victim);
        if (bout == null || bout.phase != Phase.BOUT || bout.yielded != null) {
            return;
        }
        float health = victim.getHealth();
        float max = victim.getMaxHealth();
        if (DuelRules.yields(health, max, event.getNewDamage())) {
            event.setNewDamage(DuelRules.capped(health, max, event.getNewDamage()));
            bout.yielded = victim instanceof Player ? Outcome.YIELDED : Outcome.WON;
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BY_PLAYER.clear();
    }

    private static void title(ServerPlayer player, Component text) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
        player.connection.send(new ClientboundSetTitleTextPacket(text));
    }

    private static void bell(ServerLevel level, BlockPos ring, float pitch) {
        level.playSound(null, ring.getX() + 0.5, ring.getY() + 1, ring.getZ() + 0.5, SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 1.5f, pitch);
    }
}
