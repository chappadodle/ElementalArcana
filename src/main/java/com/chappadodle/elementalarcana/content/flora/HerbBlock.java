package com.chappadodle.elementalarcana.content.flora;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Herb;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A herb of the elements (see the Arcane Flora spec): it grows on its own ground (the block tag
 * {@code elementalarcana:herb_soil/<herb>}), glows as its herb does, gives off a little of its
 * element now and then, and bone meal on it makes a second to pick, like a tall flower.
 */
public class HerbBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<HerbBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            herbCodec(), propertiesCodec()).apply(instance, HerbBlock::new));
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    private final Herb herb;
    private final TagKey<Block> soil;

    public HerbBlock(Herb herb, Properties properties) {
        super(properties);
        this.herb = herb;
        this.soil = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ElementalArcana.MODID, "herb_soil/" + herb.id()));
    }

    /** The "herb" field of a herb block's codec. */
    protected static <B extends HerbBlock> RecordCodecBuilder<B, Herb> herbCodec() {
        return Codec.STRING.xmap(Herb::byId, Herb::id).fieldOf("herb").forGetter(HerbBlock::herb);
    }

    public Herb herb() {
        return herb;
    }

    @Override
    protected MapCodec<? extends HerbBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(soil);
    }

    /** Its outline before the random nudge flowers get. */
    protected VoxelShape shape() {
        return SHAPE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return shape().move(offset.x, offset.y, offset.z);
    }

    /** Whether the flower is open: always, for all but the Sunpetal and the Moonlily. */
    protected static boolean open(BlockState state) {
        return !state.hasProperty(BlockStateProperties.OPEN) || state.getValue(BlockStateProperties.OPEN);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        popResource(level, pos, new ItemStack(this));
    }

    /** A little of the herb's element now and then: embers, snowflakes, sparks, motes of light. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!open(state)) {
            return;
        }
        Vec3 offset = state.getOffset(level, pos);
        double x = pos.getX() + 0.5 + offset.x + (random.nextDouble() - 0.5) * 0.4;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5 + offset.z + (random.nextDouble() - 0.5) * 0.4;
        switch (herb) {
            case EMBERBLOOM -> {
                if (random.nextInt(4) == 0) {
                    level.addParticle(glow(0xFFC040, 0xFF4010, 0.06f, 22), x, y + 0.2, z, 0, 0.025, 0);
                }
            }
            case MOONLILY -> {
                if (random.nextInt(5) == 0) {
                    level.addParticle(glow(0xD8F0FF, 0x3F9CFF, 0.06f, 34), x, pos.getY() + 0.15, z, 0, 0.015, 0);
                }
            }
            case SKYPLUME -> {
                if (random.nextInt(9) == 0) {
                    level.addParticle(glow(0xF4FFFA, 0x8FE3C0, 0.05f, 30), x, y + 0.35, z, 0.035, 0.004, 0.012);
                }
            }
            case DEEPCAP -> {
                if (random.nextInt(7) == 0) {
                    level.addParticle(glow(0xFFC468, 0xB5895A, 0.045f, 34), x, y - 0.1, z, 0, 0.01, 0);
                }
            }
            case FROSTCAP -> {
                if (random.nextInt(9) == 0) {
                    level.addParticle(ParticleTypes.SNOWFLAKE, x, y + 0.3, z, 0, -0.01, 0);
                }
            }
            case PRISMLEAF -> {
                if (random.nextInt(7) == 0) {
                    level.addParticle(glow(0xF4E4FF, 0xD08CFF, 0.05f, 12), x, y + (random.nextDouble() - 0.5) * 0.4, z, 0, 0, 0);
                }
            }
            case STORMTHISTLE -> {
                if (random.nextInt(level.isThundering() ? 2 : 12) == 0) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y + 0.3, z, (random.nextDouble() - 0.5) * 0.2, 0.06,
                            (random.nextDouble() - 0.5) * 0.2);
                }
            }
            case SUNPETAL -> {
                if (random.nextInt(9) == 0) {
                    level.addParticle(glow(0xFFF8D8, 0xFFE070, 0.05f, 18), x, y + 0.2, z, 0, 0.012, 0);
                }
            }
        }
    }

    private static GlowParticleOptions glow(int color, int fade, float size, int lifetime) {
        return GlowParticleOptions.of(ModContent.FLARE.get(), color, fade, size, lifetime);
    }
}
