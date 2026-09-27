package com.chappadodle.elementalarcana.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * Everything a spell needs to know about one cast. {@code power} is 1.0 today; it is the
 * hook for future spell levels, school-boosting gear, or addon modifiers.
 */
public record CastContext(ServerPlayer caster, ServerLevel level, InteractionHand hand, float power) {

    public Vec3 eyePosition() {
        return caster.getEyePosition();
    }

    public Vec3 look() {
        return caster.getLookAngle();
    }
}
