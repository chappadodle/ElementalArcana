package com.chappadodle.elementalarcana.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record CastResult(boolean success, @Nullable Component failReason) {
    public static final CastResult SUCCESS = new CastResult(true, null);

    public static CastResult fail(Component reason) {
        return new CastResult(false, reason);
    }
}
