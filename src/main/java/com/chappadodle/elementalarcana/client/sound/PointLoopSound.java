package com.chappadodle.elementalarcana.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A looping sound that follows a point that isn't an entity: a Hydro Jet stream's hand or landing
 * point, a whirlpool. It asks {@code position} every tick where to be, and stops once that's null
 * (the stream stopped, the whirlpool ended). {@code pitch} can change as it plays (Pressure Build).
 */
public class PointLoopSound extends AbstractTickableSoundInstance {
    private final Supplier<@Nullable Vec3> position;
    private final Supplier<Float> pitchScale;
    private final float basePitch;

    public PointLoopSound(SoundEvent sound, float volume, float pitch, Supplier<@Nullable Vec3> position, Supplier<Float> pitchScale) {
        super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.position = position;
        this.pitchScale = pitchScale;
        this.basePitch = pitch;
        this.looping = true;
        this.delay = 0;
        this.volume = volume;
        this.pitch = pitch;
        follow(position.get());
    }

    @Override
    public void tick() {
        Vec3 at = position.get();
        if (at == null) {
            stop();
            return;
        }
        follow(at);
        pitch = basePitch * pitchScale.get();
    }

    private void follow(@Nullable Vec3 at) {
        if (at != null) {
            x = at.x;
            y = at.y;
            z = at.z;
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
