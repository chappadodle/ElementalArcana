package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.spell.HydroJetSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * One tick of Hydro Jet's stream: fills the line from the caster's hand to where the stream lands
 * with droplets and splashes the end. The server sends it as one particle whose velocity is that
 * line. For your own jet, the line is re-aimed from your current hand and view so it never lags
 * behind your crosshair.
 */
public class HydroStreamEmitter extends NoRenderParticle {
    private final Vec3 start;
    private final Vec3 line;
    private final float width;

    protected HydroStreamEmitter(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, float width) {
        super(level, x, y, z);
        Vec3 start = new Vec3(x, y, z);
        Vec3 line = new Vec3(xd, yd, zd);
        Player self = Minecraft.getInstance().player;
        if (self != null) {
            Vec3 hand = HydroJetSpell.streamOrigin(self);
            // Only your own jet: it starts at your hand and points where you look (a mob next to
            // you, spraying at you, points the other way).
            if (hand.distanceTo(start) < 1.2 && line.lengthSqr() > 0 && self.getLookAngle().dot(line.normalize()) > 0.8) {
                start = hand;
                line = self.getLookAngle().scale(line.length());
            }
        }
        this.start = start;
        this.line = line;
        this.width = width;
    }

    @Override
    public void tick() {
        double length = line.length();
        if (length < 0.01) {
            remove();
            return;
        }
        Vec3 direction = line.scale(1 / length);
        double step = width < 1 ? 0.22 : 0.35;
        int perStep = width > 1 ? 3 : 1;
        double jitter = 0.07 * width;
        for (double d = 0; d < length; d += step) {
            Vec3 at = start.add(direction.scale(d));
            for (int i = 0; i < perStep; i++) {
                level.addParticle(ModContent.HYDRO_DROP.get(),
                        at.x + (random.nextDouble() - 0.5) * jitter * 2, at.y + (random.nextDouble() - 0.5) * jitter * 2, at.z + (random.nextDouble() - 0.5) * jitter * 2,
                        direction.x * 0.15, direction.y * 0.15, direction.z * 0.15);
            }
        }
        Vec3 end = start.add(line);
        for (int i = 0; i < Math.round(2 * width) + 1; i++) {
            level.addParticle(ParticleTypes.SPLASH, end.x + (random.nextDouble() - 0.5) * 0.4 * width, end.y, end.z + (random.nextDouble() - 0.5) * 0.4 * width, 0, 0.1, 0);
            level.addParticle(ModContent.HYDRO_DROP.get(), end.x, end.y, end.z,
                    (random.nextDouble() - 0.5) * 0.3 - direction.x * 0.1, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.3 - direction.z * 0.1);
        }
        remove();
    }

    public record Provider(float width) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new HydroStreamEmitter(level, x, y, z, xd, yd, zd, width);
        }
    }
}
