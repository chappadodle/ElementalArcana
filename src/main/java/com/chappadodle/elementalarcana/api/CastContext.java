package com.chappadodle.elementalarcana.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Everything a spell needs to know about one cast. {@code power} grows with the caster's magic
 * level; {@code spellLevel} and the chosen branches come from the caster's progress in this spell.
 */
public final class CastContext {
    private final ServerPlayer caster;
    private final ServerLevel level;
    private final InteractionHand hand;
    private final float power;
    private final int spellLevel;
    private final Map<Integer, String> branches;
    @Nullable
    private SpellHold hold;

    public CastContext(ServerPlayer caster, ServerLevel level, InteractionHand hand, float power, int spellLevel, Map<Integer, String> branches) {
        this.caster = caster;
        this.level = level;
        this.hand = hand;
        this.power = power;
        this.spellLevel = spellLevel;
        this.branches = Map.copyOf(branches);
    }

    public ServerPlayer caster() {
        return caster;
    }

    public ServerLevel level() {
        return level;
    }

    public InteractionHand hand() {
        return hand;
    }

    public float power() {
        return power;
    }

    /** The caster's level in this spell (1 when the spell doesn't level). */
    public int spellLevel() {
        return spellLevel;
    }

    /** The branch the caster chose at {@code level}, or null if none was chosen. */
    @Nullable
    public String branch(int level) {
        return branches.get(level);
    }

    public boolean hasBranch(int level, String branch) {
        return branch.equals(branches.get(level));
    }

    public Vec3 eyePosition() {
        return caster.getEyePosition();
    }

    public Vec3 look() {
        return caster.getLookAngle();
    }

    /** Keep this cast going while the cast key is held; see {@link SpellHold}. */
    public void holdUntilRelease(SpellHold hold) {
        this.hold = hold;
    }

    @Nullable
    public SpellHold hold() {
        return hold;
    }
}
