package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.client.ScreenEffects;
import com.chappadodle.elementalarcana.client.sound.FireballSounds;
import com.chappadodle.elementalarcana.content.FireBlastOptions;
import com.chappadodle.elementalarcana.content.spell.FireballEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A fireball's explosion, played out over several ticks on this client (the server sends it as one
 * particle, see FireBlastOptions): each tick asks FireballEffects#blast for that moment's
 * particles. Meteor impacts shake the camera and Sunfire flashes the screen for players near
 * enough to feel them.
 */
public class FireBlastEmitter extends NoRenderParticle {
    private final FireBlastOptions blast;
    private final Vec3 toBlast;

    protected FireBlastEmitter(ClientLevel level, double x, double y, double z, FireBlastOptions blast) {
        super(level, x, y, z);
        this.blast = blast;
        this.lifetime = FireballEffects.BLAST_TICKS;
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 toBlast = new Vec3(x, y, z).subtract(eye);
        this.toBlast = toBlast.lengthSqr() > 1.0e-4 ? toBlast.normalize() : new Vec3(0, 0, 1);
        FireballSounds.blast(level, new Vec3(x, y, z), blast.look(), blast.bomblet());
        feel();
    }

    @Override
    public void tick() {
        FireballEffects.blast(level, new Vec3(x, y, z), blast.look(), blast.radius(), blast.bomblet(), age, toBlast);
        if (++age >= lifetime) {
            remove();
        }
    }

    /** The shake and the flash, for the player watching. */
    private void feel() {
        Player self = Minecraft.getInstance().player;
        if (self == null || blast.bomblet()) {
            return;
        }
        Vec3 at = new Vec3(x, y, z);
        double distance = self.getEyePosition().distanceTo(at);
        if (FireballEffects.shakes(blast.look()) && distance < 16) {
            ScreenEffects.shake((float) (1.0 - distance / 16));
        }
        if (FireballEffects.flashes(blast.look()) && distance < 32) {
            // Brightest up close and when looking straight at it; still a little from behind.
            double facing = self.getLookAngle().dot(at.subtract(self.getEyePosition()).normalize());
            float amount = (float) ((1.0 - distance / 32) * (0.35 + 0.65 * Math.max(0, facing)));
            ScreenEffects.flash(0.85f * amount);
        }
    }

    public static class Provider implements ParticleProvider<FireBlastOptions> {
        @Override
        public Particle createParticle(FireBlastOptions blast, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new FireBlastEmitter(level, x, y, z, blast);
        }
    }
}
