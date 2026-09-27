package com.chappadodle.elementalarcana.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Everything a spell needs to know about one cast. {@code power} grows with the caster's magic
 * level and is the hook for future gear or addon modifiers.
 */
public final class CastContext {
    private final ServerPlayer caster;
    private final ServerLevel level;
    private final InteractionHand hand;
    private final float power;
    @Nullable
    private SpellHold hold;

    public CastContext(ServerPlayer caster, ServerLevel level, InteractionHand hand, float power) {
        this.caster = caster;
        this.level = level;
        this.hand = hand;
        this.power = power;
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
