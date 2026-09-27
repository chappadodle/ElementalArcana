package com.chappadodle.elementalarcana.api;

/**
 * A cast that stays active while the cast key is held, like charging an icicle before throwing
 * it. A spell starts one from {@link Spell#cast} via {@link CastContext#holdUntilRelease}. Mana
 * is paid when the hold starts; the cooldown starts when it ends. Runs server-side.
 */
public interface SpellHold {

    /** Every tick while held. Return false to end the hold now (it is then released). */
    default boolean tick(int heldTicks) {
        return true;
    }

    /** The cast key was released, or {@link #maxHoldTicks()} ran out. */
    void release(int heldTicks);

    /** The hold broke without a release: the caster died, logged out or changed dimension. */
    void cancel();

    default int maxHoldTicks() {
        return 10 * 20;
    }
}
