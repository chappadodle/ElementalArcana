package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.PyronadoOptions;
import com.chappadodle.elementalarcana.content.spell.Pyronados;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Pyronado's wheels, drawn on this client for as long as they spin (the server sends one particle
 * when they're called, see PyronadoOptions; where they are is Pyronados'). Each tick every wheel's
 * rim and spokes are traced in plain flames that live only a few ticks, so each wheel reads as a
 * spinning ring of fire with a short tail and the three stay apart; a small glow, anchored to the
 * caster, burns at each hub. They fling off a flame or an ember now and then, and lava pops from
 * their hubs.
 */
public class PyronadoEmitter extends NoRenderParticle {
    private static final int RIM_POINTS = 12;
    private static final int RIM_LIFE = 3;
    private static final int SPOKES = 3;
    private static final int SPOKE_LIFE = 2;

    @Nullable
    private final Entity caster;
    private final GlowParticleOptions hub;

    protected PyronadoEmitter(ClientLevel level, double x, double y, double z, PyronadoOptions options) {
        super(level, x, y, z);
        this.caster = level.getEntity(options.caster());
        this.lifetime = options.duration();
        GlowParticleOptions hub = GlowParticleOptions.of(ModContent.FLARE.get(), 0xFFD27A, 0xFF7A28, 0.28f, 3);
        this.hub = caster == null ? hub : hub.anchoredTo(caster);
    }

    @Override
    public void tick() {
        if (caster == null || caster.isRemoved() || !caster.isAlive() || age >= lifetime) {
            remove();
            return;
        }
        // The hub's glow is anchored: its place relative to the caster's position this tick.
        Vec3 base = caster.position();
        setPos(base.x, base.y, base.z);
        double roll = age * Pyronados.ROLL;
        for (int wheel = 0; wheel < Pyronados.WHEELS; wheel++) {
            Vec3 center = base.add(Pyronados.wheelCenter(wheel, age));
            for (int i = 0; i < RIM_POINTS; i++) {
                flame(ParticleTypes.FLAME, base.add(Pyronados.rimPoint(wheel, age, roll + Math.PI * 2 * i / RIM_POINTS, 1.0)), Vec3.ZERO, RIM_LIFE);
            }
            for (int i = 0; i < SPOKES; i++) {
                flame(ParticleTypes.SMALL_FLAME, base.add(Pyronados.rimPoint(wheel, age, roll + Math.PI * 2 * i / SPOKES, 0.5)), Vec3.ZERO, SPOKE_LIFE);
            }
            level.addParticle(hub, center.x, center.y, center.z, 0, 0, 0);
            if (random.nextInt(4) == 0) {
                // A flame flung off the rim, left behind in the air for a moment.
                Vec3 at = base.add(Pyronados.rimPoint(wheel, age, random.nextDouble() * Math.PI * 2, 1.0));
                flame(ParticleTypes.FLAME, at, at.subtract(center).scale(0.06).add(0, 0.02, 0), 8 + random.nextInt(6));
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(ModContent.EMBER.get(), center.x, center.y, center.z,
                        (random.nextDouble() - 0.5) * 0.08, 0.04, (random.nextDouble() - 0.5) * 0.08);
            }
            if ((age + wheel * 3) % 9 == 0) {
                level.addParticle(ParticleTypes.LAVA, center.x, center.y, center.z, 0, 0, 0);
            }
        }
        age++;
    }

    /** A flame that burns out after {@code lifetime} ticks (vanilla's last two seconds and smear the wheels together). */
    private static void flame(ParticleOptions type, Vec3 at, Vec3 velocity, int lifetime) {
        Particle flame = Minecraft.getInstance().particleEngine.createParticle(type, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
        if (flame != null) {
            flame.setLifetime(lifetime);
        }
    }

    public static class Provider implements ParticleProvider<PyronadoOptions> {
        @Override
        public Particle createParticle(PyronadoOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new PyronadoEmitter(level, x, y, z, options);
        }
    }
}
