package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.api.Element;

/** What the drake model and renderer read from a drake, wild or tamed (see DrakeEntity, TamedDrakeEntity). */
public interface DrakeLike {
    Element element();

    boolean isFlyingPose();

    boolean isResting();

    boolean isClimbing();

    float breathOpen(float partialTick);

    float bodyPitch(float partialTick);

    float bank(float partialTick);

    /** Whether it wears a saddle. */
    default boolean isSaddled() {
        return false;
    }
}
