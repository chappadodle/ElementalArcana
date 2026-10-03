package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.TremorOptions;
import com.chappadodle.elementalarcana.content.spell.ImpactSurfaces;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tremor's shockwave, drawn on this client as its front runs (the server sends one particle when it
 * starts, with its path, see TremorOptions; the hits are Tremors'). The caster stamps the ground,
 * and the front tears it up a block a tick: chunks of whatever it crosses thrown into the air, dust
 * shooting up like a mace's smash, and every other block a spike of rock heaved up, leaning the way
 * it runs, on alternating sides. Where it ends, three tall spikes burst up together. Each spike sinks
 * back in a second or so.
 */
public class TremorEmitter extends NoRenderParticle {
    private static final int SPIKE_LIFE = 24;

    private final Vec3 dir;
    private final Vec3 side;
    private final int[] heights;
    private final TextureAtlasSprite rock;

    protected TremorEmitter(ClientLevel level, double x, double y, double z, TremorOptions options) {
        super(level, x, y, z);
        this.dir = new Vec3(options.dx(), 0, options.dz());
        this.side = new Vec3(-options.dz(), 0, options.dx());
        List<Integer> rise = options.rise();
        int foot = (int) Math.round(y);
        this.heights = rise.stream().mapToInt(r -> foot + r).toArray();
        this.lifetime = heights.length;
        this.rock = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ElementalArcana.id("block/boulder"));
    }

    @Override
    public void tick() {
        if (age == 0) {
            stamp();
        }
        int step = age + 1;
        if (step >= heights.length) {
            remove();
            return;
        }
        Vec3 front = new Vec3(x, heights[step], z).add(dir.scale(step));
        boolean last = step == heights.length - 1;
        BlockPos groundPos = BlockPos.containing(front.x, heights[step] - 1, front.z);
        BlockState ground = level.getBlockState(groundPos);
        if (ground.getRenderShape() != RenderShape.INVISIBLE) {
            tearUp(front, ground, groundPos, last ? 6 : 3);
            if (step % 2 == 0) {
                sound(front, ground.getSoundType().getBreakSound(), 0.7f, 0.55f + random.nextFloat() * 0.2f);
            }
        }
        if (last) {
            for (int i = -1; i <= 1; i++) {
                spike(front.add(dir.scale(0.3 * (1 - Math.abs(i)))).add(side.scale(0.55 * i)), 1.4f + random.nextFloat() * 0.4f, i * 25f);
            }
            sound(front, SoundEvents.MACE_SMASH_GROUND, 1f, 0.75f);
            sound(front, SoundEvents.DEEPSLATE_BREAK, 1f, 0.6f);
        } else if (step % 2 == 1) {
            double across = ((step / 2) % 2 == 0 ? 1 : -1) * (0.45 + 0.3 * random.nextDouble());
            spike(front.add(side.scale(across)), 0.6f + 0.05f * step + random.nextFloat() * 0.3f, (float) Math.signum(across) * 12f);
            sound(front, SoundEvents.BASALT_BREAK, 0.8f, 0.7f + random.nextFloat() * 0.15f);
        }
        age++;
    }

    /** The caster stamping the ground: a ring of dust around their feet and a heavy thud. */
    private void stamp() {
        BlockPos groundPos = BlockPos.containing(x, heights[0] - 1, z);
        BlockState ground = level.getBlockState(groundPos);
        if (ground.getRenderShape() != RenderShape.INVISIBLE) {
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI * 2 * i / 10;
                dust(ground, new Vec3(x + Math.cos(angle) * 0.9, heights[0], z + Math.sin(angle) * 0.9),
                        new Vec3(Math.cos(angle) * 0.08, 0.15 + random.nextDouble() * 0.1, Math.sin(angle) * 0.08));
            }
        }
        sound(new Vec3(x, heights[0], z), SoundEvents.MACE_SMASH_GROUND_HEAVY, 1f, 0.85f);
    }

    /** The ground torn up where the front is: chunks of it flung up and forward, and a few pillars of dust. */
    private void tearUp(Vec3 front, BlockState ground, BlockPos groundPos, int chunks) {
        for (int i = 0; i < chunks; i++) {
            double across = (random.nextDouble() - 0.5) * 2 * 0.9;
            Vec3 at = front.add(side.scale(across)).add(dir.scale((random.nextDouble() - 0.5) * 0.8)).add(0, 0.05, 0);
            Vec3 velocity = dir.scale(0.04 + 0.06 * random.nextDouble()).add(side.scale(across * 0.05))
                    .add(0, 0.28 + 0.16 * random.nextDouble(), 0);
            Minecraft.getInstance().particleEngine.add(new DebrisParticle(level, at, velocity, ground, groundPos, 0.1f + random.nextFloat() * 0.12f));
        }
        for (int i = 0; i < 5; i++) {
            dust(ground, front.add(side.scale((random.nextDouble() - 0.5) * 2.0)),
                    new Vec3(random.nextGaussian() * 0.03, 0.15 + random.nextDouble() * 0.15, random.nextGaussian() * 0.03));
        }
    }

    /** A puff of dust from {@code ground}, like a mace's smash, but rising only a block or so (vanilla's fly far higher). */
    private static void dust(BlockState ground, Vec3 at, Vec3 velocity) {
        Particle dust = Minecraft.getInstance().particleEngine.createParticle(new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground),
                at.x, at.y, at.z, 0, 0, 0);
        if (dust != null) {
            dust.setParticleSpeed(velocity.x, velocity.y, velocity.z);
        }
    }

    /** A spike of rock heaved up at {@code at} (if there's ground there), leaning forward and {@code outward} degrees to its side. */
    private void spike(Vec3 at, float height, float outward) {
        BlockHitResult ground = ImpactSurfaces.groundBelow(level, at.add(0, 0.9, 0));
        if (ground == null) {
            return;
        }
        double angle = Math.toRadians(outward);
        Vec3 toward = dir.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
        float tilt = 14f + random.nextFloat() * 10f;
        Minecraft.getInstance().particleEngine.add(new StoneSpikeParticle(level, ground.getLocation(), rock, height,
                StoneSpikeParticle.leaning(toward.x, toward.z, tilt), SPIKE_LIFE + random.nextInt(8)));
    }

    private void sound(Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    public static class Provider implements ParticleProvider<TremorOptions> {
        @Override
        public Particle createParticle(TremorOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new TremorEmitter(level, x, y, z, options);
        }
    }
}
