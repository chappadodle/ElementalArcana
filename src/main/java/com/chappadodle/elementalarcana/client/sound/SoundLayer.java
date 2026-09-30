package com.chappadodle.elementalarcana.client.sound;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** One sound in a spell's mix, at its volume and pitch. */
public record SoundLayer(SoundEvent sound, float volume, float pitch) {

    public static SoundLayer of(SoundEvent sound, float volume, float pitch) {
        return new SoundLayer(sound, volume, pitch);
    }

    /** Plays a mix once at {@code at} on this client, each layer slightly varied in pitch. */
    public static void play(Level level, Vec3 at, List<SoundLayer> layers, float volumeScale, float pitchScale) {
        RandomSource random = level.getRandom();
        for (SoundLayer layer : layers) {
            float pitch = layer.pitch() * pitchScale * (0.95f + random.nextFloat() * 0.1f);
            level.playLocalSound(at.x, at.y, at.z, layer.sound(), SoundSource.PLAYERS, layer.volume() * volumeScale, pitch, false);
        }
    }
}
