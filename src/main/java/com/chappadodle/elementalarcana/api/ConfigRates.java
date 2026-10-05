package com.chappadodle.elementalarcana.api;

/**
 * The server settings' rates (see core/ArcanaServerConfig): each scales one of the world's chances
 * (a rift opening, a star falling, a creature coming), 1 as it's made, 0 never.
 */
public final class ConfigRates {

    private ConfigRates() {
    }

    /** {@code chance} at {@code rate} times its usual: never below 0 nor above 1. */
    public static double scaled(double chance, double rate) {
        return Math.clamp(chance * rate, 0.0, 1.0);
    }
}
