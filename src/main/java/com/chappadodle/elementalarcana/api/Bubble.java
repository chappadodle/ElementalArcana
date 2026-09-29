package com.chappadodle.elementalarcana.api;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * A creature trapped in a Bubble Prison: where the bubble is anchored (the creature's feet when it
 * was trapped), when it was trapped and when it pops by itself (game ticks). Synced, never saved.
 */
public record Bubble(Vec3 anchor, long startTick, long endsAt) {
    // It rises this far over the first RISE_TICKS, then hangs there.
    public static final double RISE = 1.2;
    public static final int RISE_TICKS = 8;

    public static final StreamCodec<RegistryFriendlyByteBuf, Bubble> STREAM_CODEC = StreamCodec.of(
            (buf, bubble) -> {
                buf.writeDouble(bubble.anchor.x);
                buf.writeDouble(bubble.anchor.y);
                buf.writeDouble(bubble.anchor.z);
                buf.writeVarLong(bubble.startTick);
                buf.writeVarLong(bubble.endsAt);
            },
            buf -> new Bubble(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readVarLong(), buf.readVarLong()));

    /** Where the trapped creature's feet should be at {@code gameTime}: rising, then hanging. */
    public Vec3 holdPoint(long gameTime) {
        double risen = Math.min(1.0, Math.max(0, gameTime - startTick) / (double) RISE_TICKS);
        return anchor.add(0, RISE * risen, 0);
    }
}
