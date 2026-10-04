package com.chappadodle.elementalarcana.api;

import java.util.List;

/**
 * The numbers of the Elemental Drakes (docs/superpowers/specs/2026-10-04-drakes-design.md). Plain
 * Java, unit tested.
 */
public final class DrakeRules {
    /** The drakes' elements, in the order their kinds are registered. */
    public static final List<Element> ELEMENTS = List.of(Element.FIRE, Element.ICE, Element.LIGHTNING, Element.WATER, Element.WIND);
    /** Levels above its place's. */
    public static final int LEVEL_BONUS = 10;
    public static final double MAX_HEALTH = 140;
    public static final double ARMOR = 8;
    public static final float CLAW_DAMAGE = 10f;
    /** A breath hit, every BREATH_HIT_TICKS while it breathes. */
    public static final float BREATH_DAMAGE = 2f;
    public static final int BREATH_HIT_TICKS = 4;
    public static final double BREATH_RANGE = 10;
    public static final double BREATH_HALF_ANGLE = Math.toRadians(30);
    /** It soars this far above the ground round its home, this far out. */
    public static final double SOAR_HEIGHT_MIN = 18;
    public static final double SOAR_HEIGHT_MAX = 28;
    public static final double SOAR_RADIUS = 22;
    /** It sees prey this far off, and never strays farther than LEASH from home. */
    public static final double SIGHT = 40;
    public static final double LEASH = 64;
    public static final int SWOOP_COOLDOWN_TICKS = 120;
    public static final int BREATH_COOLDOWN_TICKS = 180;
    public static final int WINDUP_TICKS = 20;
    public static final int BREATH_TICKS = 40;
    public static final double SWOOP_REACH = 3.0;
    /** Where it hovers to breathe: this far from its prey, this much above. */
    public static final double HOVER_DISTANCE = 10;
    public static final double HOVER_HEIGHT = 4;
    /** With no prey this long it lands to rest, for this long at most. */
    public static final int CALM_TICKS = 300;
    public static final int REST_TICKS = 600;
    /** Someone this close wakes a resting drake. */
    public static final double WAKE_RADIUS = 12;
    /** At or below this share of its health it can't fly. */
    public static final float GROUNDED_SHARE = 0.25f;
    public static final double SOAR_SPEED = 0.45;
    public static final double HUNT_SPEED = 0.6;
    public static final double SWOOP_SPEED = 0.95;
    /** Wild drakes: a check every so often near each player, at this chance, never two within SPACING. */
    public static final int WILD_CHECK_TICKS = 600;
    public static final float WILD_CHANCE = 0.03f;
    public static final double WILD_SPACING = 128;
    /** Wild drakes come only this high up (tide drakes over the sea instead), and this far from the centre. */
    public static final int WILD_MIN_Y = 90;
    public static final int MIN_DISTANCE = 1000;

    private DrakeRules() {
    }

    /**
     * Whether a point {@code (dx, dy, dz)} from the drake's mouth is in its breath: within range and
     * within BREATH_HALF_ANGLE of where it faces ({@code (fx, fy, fz)}, a unit vector).
     */
    public static boolean inBreath(double dx, double dy, double dz, double fx, double fy, double fz) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-6) {
            return true;
        }
        if (length > BREATH_RANGE) {
            return false;
        }
        return (dx * fx + dy * fy + dz * fz) / length >= Math.cos(BREATH_HALF_ANGLE);
    }

    /** Whether a drake this hurt can no longer fly. */
    public static boolean grounded(float health, float maxHealth) {
        return health <= maxHealth * GROUNDED_SHARE;
    }

    /** How high above the ground it soars, from a number 0..1 picked for it. */
    public static double soarHeight(double pick) {
        return SOAR_HEIGHT_MIN + (SOAR_HEIGHT_MAX - SOAR_HEIGHT_MIN) * Math.clamp(pick, 0, 1);
    }

    /** Whether a cooldown ending at {@code readyAt} is over at {@code now}. */
    public static boolean ready(long readyAt, long now) {
        return now >= readyAt;
    }
}
