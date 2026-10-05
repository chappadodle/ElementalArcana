package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.WispRingOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.util.Mth;

/**
 * A Wisp Ring's dancing lights (the server sends one of these over the ring every couple of seconds
 * while someone is near at night, see WispRingOptions): seven little lights, the elements' colours,
 * circling over the ring two blocks and a half out, each bobbing on its own beat, as small glows that
 * live a few ticks each so the circle trails a little. The circle turns at the same pace for every
 * light sent, so one hands over to the next without a jump.
 */
public class WispRingEmitter extends NoRenderParticle {
    private static final Element[] LIGHTS = {Element.FIRE, Element.WATER, Element.WIND, Element.EARTH, Element.ICE, Element.CRYSTAL,
            Element.RADIANCE};
    private static final double RADIUS = 2.5;

    protected WispRingEmitter(ClientLevel level, double x, double y, double z, WispRingOptions options) {
        super(level, x, y, z);
        this.lifetime = options.ticks();
    }

    @Override
    public void tick() {
        if (age++ >= lifetime) {
            remove();
            return;
        }
        // The circle's turn follows the world's clock, so a fresh emitter takes up where the last left off.
        double time = level.getGameTime();
        for (int i = 0; i < LIGHTS.length; i++) {
            double angle = time * 0.03 + Mth.TWO_PI * i / LIGHTS.length;
            double bob = Math.sin(time * 0.11 + i * 1.7) * 0.35;
            int color = LIGHTS[i].color();
            level.addParticle(GlowParticleOptions.of(ModContent.FLARE.get(), color, 0xFFFFFF, 0.22f, 6),
                    x + Math.cos(angle) * RADIUS, y + bob, z + Math.sin(angle) * RADIUS, 0, 0, 0);
        }
    }

    public static class Provider implements ParticleProvider<WispRingOptions> {
        @Override
        public Particle createParticle(WispRingOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new WispRingEmitter(level, x, y, z, options);
        }
    }
}
