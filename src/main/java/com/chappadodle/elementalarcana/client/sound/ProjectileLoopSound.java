package com.chappadodle.elementalarcana.client.sound;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A looping sound that follows a spell projectile: while it's held (swelling as it grows) or in
 * flight (with a Doppler shift: higher while it closes in on you, lower as it flies away). It
 * stops by itself once the projectile is gone, changes phase (thrown) or changes look (fused).
 */
public class ProjectileLoopSound extends AbstractTickableSoundInstance {
    private static final float DOPPLER = 0.1f;

    private final SpellProjectile projectile;
    private final boolean held;
    private final int look;
    private final float baseVolume;
    private final float basePitch;
    private double lastDistance = -1;
    private float doppler = 1f;

    public ProjectileLoopSound(SpellProjectile projectile, SoundEvent sound, float volume, float pitch) {
        super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.projectile = projectile;
        this.held = projectile.isHeld();
        this.look = projectile.variant();
        this.baseVolume = volume;
        this.basePitch = pitch;
        this.looping = true;
        this.delay = 0;
        follow();
        this.volume = currentVolume();
        this.pitch = pitch;
    }

    /** Whether this loop still belongs to the projectile as it is now. */
    public boolean matches(SpellProjectile projectile) {
        return !isStopped() && this.projectile == projectile && projectile.isHeld() == held && projectile.variant() == look;
    }

    @Override
    public void tick() {
        if (projectile.isRemoved() || !matches(projectile)) {
            stop();
            return;
        }
        follow();
        volume = currentVolume();
        if (!held) {
            // Doppler: how fast it's closing in on the listener, in blocks per tick.
            Vec3 listener = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            double distance = listener.distanceTo(projectile.position());
            if (lastDistance >= 0) {
                float closing = (float) (lastDistance - distance);
                float target = Mth.clamp(1f + closing * DOPPLER, 0.7f, 1.4f);
                doppler += (target - doppler) * 0.5f;
            }
            lastDistance = distance;
        }
        pitch = basePitch * doppler;
    }

    private void follow() {
        x = projectile.getX();
        y = projectile.getY();
        z = projectile.getZ();
    }

    private float currentVolume() {
        return held ? baseVolume * (0.35f + 0.65f * projectile.charge(0f)) : baseVolume;
    }

    /** Stops the loop (it's been replaced). */
    public void end() {
        stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
