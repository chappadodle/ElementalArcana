package com.chappadodle.elementalarcana.api;

/**
 * The numbers of the Trophies of the Wild (docs/superpowers/specs/2026-10-04-wild-trophies-design.md):
 * the Heartwood Talisman's rest, the Wraithsilk Veil's flicker and the Salamander Charm's crust.
 * Plain Java, unit tested.
 */
public final class WildTrophyRules {
    // The Heartwood Talisman: 1 health every 3 seconds, once nothing has hurt you for 5.
    public static final int REST_INTERVAL_TICKS = 60;
    public static final int REST_CALM_TICKS = 100;
    public static final float REST_HEAL = 1f;

    // The Wraithsilk Veil: the hit at half strength, a flicker 4 to 6 blocks off, then 12 seconds' rest.
    public static final float FLICKER_DAMAGE = 0.5f;
    public static final double FLICKER_MIN = 4;
    public static final double FLICKER_MAX = 6;
    public static final int FLICKER_COOLDOWN_TICKS = 240;

    // The Salamander Charm: lava within 2 blocks cools; the crust first ages after 3 to 6 seconds,
    // then every 1 to 2, and melts at its fourth stage.
    public static final int CRUST_RADIUS = 2;
    public static final int CRUST_FIRST_MIN_TICKS = 60;
    public static final int CRUST_FIRST_MAX_TICKS = 120;
    public static final int CRUST_AGE_MIN_TICKS = 20;
    public static final int CRUST_AGE_MAX_TICKS = 40;
    public static final int CRUST_MAX_AGE = 3;

    // Trophies II (docs/superpowers/specs/2026-10-05-wild-trophies-2-design.md): the plume's leap and
    // its ward on short falls, the prism's climb, the pearl's swimming.
    public static final double LEAP_BOOST = 0.55;
    public static final double LEAP_LIFT = 0.12;
    public static final float LEAP_FALL_WARD = 8f;
    public static final double CLIMB_SPEED = 0.2;
    public static final double SWIM_BOOST = 0.5;

    private WildTrophyRules() {
    }

    /** Whether the plume keeps a fall of {@code distance} blocks from hurting. */
    public static boolean fallWarded(float distance) {
        return distance <= LEAP_FALL_WARD;
    }

    /** Whether the talisman's bearer rests: by day, under the open sky, on the earth, and left alone a while. */
    public static boolean canRest(boolean day, boolean openSky, boolean onEarth, long now, long lastHurt) {
        return day && openSky && onEarth && now - lastHurt >= REST_CALM_TICKS;
    }

    /** How hard a hit lands on the veil's bearer when it flickers them away. */
    public static float flickerDamage(float amount) {
        return amount * FLICKER_DAMAGE;
    }

    /** Whether a block {@code dx}, {@code dz} from the bearer's feet (block centre to feet) is in the crust's disc. */
    public static boolean inCrustDisc(double dx, double dz) {
        return dx * dx + dz * dz < CRUST_RADIUS * CRUST_RADIUS;
    }

    /** The crust's next stage, or -1 when it melts back to lava. */
    public static int nextCrustAge(int age) {
        return age < CRUST_MAX_AGE ? age + 1 : -1;
    }
}
