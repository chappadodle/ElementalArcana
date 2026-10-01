package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.core.CastingService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Reactions pay off. A hit that sets off a reaction (Melt, Vaporize, Freeze, Swirl) is raised by
 * the attacking player's Insight (see LevelCombat), and a player who sets one off on a creature
 * absorbs a fifth of its kill XP, at most once per creature every 5 seconds.
 */
public final class ReactionRewards {
    private static final String TAG_REACTED_AT = "ea_reacted_at";
    private static final String TAG_XP_AT = "ea_reaction_xp_at";
    private static final int XP_GAP_TICKS = 100;
    private static final double XP_SHARE = 0.2;

    private ReactionRewards() {
    }

    /**
     * {@code target} just reacted. The XP goes to {@code attacker} if it's a player, or else to the
     * player who hurt the target in the last 5 seconds.
     */
    public static void reacted(LivingEntity target, @Nullable Entity attacker) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        target.getPersistentData().putLong(TAG_REACTED_AT, now);
        ServerPlayer player = null;
        if (attacker instanceof ServerPlayer direct) {
            player = direct;
        } else if (target.getLastHurtByMob() instanceof ServerPlayer recent
                && target.tickCount - target.getLastHurtByMobTimestamp() < XP_GAP_TICKS) {
            player = recent;
        }
        if (player == null || target instanceof Player || now < target.getPersistentData().getLong(TAG_XP_AT)) {
            return;
        }
        target.getPersistentData().putLong(TAG_XP_AT, now + XP_GAP_TICKS);
        int xp = (int) Math.round(CreatureRewards.killXp(player, target) * XP_SHARE);
        if (xp > 0) {
            CastingService.grantXp(player, xp);
        }
    }

    /** Whether {@code target} reacted this tick (so the hit landing now set it off). */
    public static boolean reactedThisTick(LivingEntity target) {
        return target.getPersistentData().getLong(TAG_REACTED_AT) == target.level().getGameTime();
    }
}
