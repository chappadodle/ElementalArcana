package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.client.visual.WaterBeams;
import com.chappadodle.elementalarcana.content.HydroStreamOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.spell.HydroJetSpell;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * One tick of a Hydro Jet stream: the server sends it as one particle whose velocity is the line
 * from the caster's hand to where the stream lands. It keeps the caster's water beam up to date
 * (WaterBeams draws it) and splashes where the stream lands.
 */
public class HydroStreamEmitter extends NoRenderParticle {
    private final Vec3 end;
    private final Vec3 direction;
    private final float width;

    protected HydroStreamEmitter(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, HydroStreamOptions stream) {
        super(level, x, y, z);
        Vec3 start = new Vec3(x, y, z);
        Vec3 line = new Vec3(xd, yd, zd);
        WaterBeams.update(level, stream, start, line);
        this.end = start.add(line);
        this.direction = line.lengthSqr() > 1.0e-6 ? line.normalize() : Vec3.ZERO;
        int look = HydroJetSpell.look(stream.look());
        this.width = look == HydroJetSpell.LOOK_TORRENT ? 2.5f : look == HydroJetSpell.LOOK_TIDECUTTER ? 0.35f : 1f;
    }

    @Override
    public void tick() {
        // The splash where it lands.
        for (int i = 0; i < Math.round(2 * width) + 1; i++) {
            level.addParticle(ParticleTypes.SPLASH, end.x + (random.nextDouble() - 0.5) * 0.4 * width, end.y, end.z + (random.nextDouble() - 0.5) * 0.4 * width, 0, 0.1, 0);
            level.addParticle(ModContent.HYDRO_DROP.get(), end.x, end.y, end.z,
                    (random.nextDouble() - 0.5) * 0.3 - direction.x * 0.1, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.3 - direction.z * 0.1);
        }
        remove();
    }

    public static class Provider implements ParticleProvider<HydroStreamOptions> {
        @Override
        public Particle createParticle(HydroStreamOptions stream, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new HydroStreamEmitter(level, x, y, z, xd, yd, zd, stream);
        }
    }
}
