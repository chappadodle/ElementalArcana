package com.chappadodle.elementalarcana.api;

/**
 * A soft glow halo drawn around a spell projectile: light added on top of what's behind it (see
 * SpellProjectileRenderer). {@code color} is 0xRRGGBB, {@code size} is the halo's width relative to
 * the projectile, and {@code intensity} scales its brightness (1 = the color as given).
 */
public record Glow(int color, float size, float intensity) {
}
