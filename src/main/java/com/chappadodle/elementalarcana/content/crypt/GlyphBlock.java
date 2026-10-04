package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.CryptRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.CreatureLevels;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.mob.MobCasting;
import com.chappadodle.elementalarcana.content.world.ShrineKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/**
 * A crypt's glyph (see the Arcane Crypts spec): a faint sigil of the crypt's element on the floor.
 * Something steps on it and it flares for half a second, then bursts (damage and the element's
 * touch to everything on and around it), then rests for 3 seconds. The crypt's own dead (Attuned to
 * its element's family, or born to it) and players in creative or spectating pass safely. Breaking
 * one sets it off.
 */
public class GlyphBlock extends Block {
    public static final MapCodec<GlyphBlock> CODEC = simpleCodec(GlyphBlock::new);
    public static final EnumProperty<ShrineKind> ELEMENT = EnumProperty.create("element", ShrineKind.class);
    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public enum Stage implements StringRepresentable {
        ARMED, FLARING, RESTING;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public GlyphBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ELEMENT, ShrineKind.FIRE).setValue(STAGE, Stage.ARMED));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ELEMENT, STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel server && state.getValue(STAGE) == Stage.ARMED && entity instanceof LivingEntity living
                && entity.getY() < pos.getY() + 0.25 && triggers(living, element(state))) {
            flare(server, pos, state);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && state.getValue(STAGE) != Stage.RESTING) {
            burst(server, pos, element(state));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        switch (state.getValue(STAGE)) {
            case FLARING -> {
                burst(level, pos, element(state));
                level.setBlock(pos, state.setValue(STAGE, Stage.RESTING), Block.UPDATE_ALL);
                level.scheduleTick(pos, this, CryptRules.GLYPH_REST_TICKS);
            }
            case RESTING -> level.setBlock(pos, state.setValue(STAGE, Stage.ARMED), Block.UPDATE_ALL);
            default -> {
            }
        }
    }

    public static Element element(BlockState state) {
        return state.getValue(ELEMENT).element();
    }

    /** Whether a creature sets the glyph off: anyone but the crypt's own and players in creative or spectating. */
    public static boolean triggers(LivingEntity entity, Element element) {
        if (!entity.isAlive() || entity.isSpectator() || entity instanceof Player player && player.isCreative()) {
            return false;
        }
        CreatureMagic magic = Attunement.get(entity);
        if (magic != null && magic.element().family() == element.family()) {
            return false;
        }
        Element innate = CreatureElements.innateElementOf(entity);
        return innate == null || innate.family() != element.family();
    }

    private void flare(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(STAGE, Stage.FLARING), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, CryptRules.GLYPH_WARNING_TICKS);
        level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.8f, 0.6f);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.6f, 1.8f);
        level.sendParticles(MobCasting.handsParticle(element(state)), pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5,
                12, 0.35, 0.05, 0.35, 0.02);
    }

    /** The burst: damage and the element's touch to everything that would set it off, on it and around it. */
    public static void burst(ServerLevel level, BlockPos pos, Element element) {
        Vec3 center = Vec3.atBottomCenterOf(pos);
        float damage = CryptRules.glyphDamage(CreatureLevels.zoneLevelAt(level, pos));
        AABB area = new AABB(center, center).inflate(CryptRules.GLYPH_RADIUS, 0, CryptRules.GLYPH_RADIUS).expandTowards(0, 2.2, 0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, target -> triggers(target, element))) {
            touch(level, target, element, damage, center);
        }
        effects(level, center, element);
    }

    private static void touch(ServerLevel level, LivingEntity target, Element element, float damage, Vec3 center) {
        Vec3 away = target.position().subtract(center).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0e-4 ? Vec3.ZERO : away.normalize();
        switch (element) {
            case FIRE, RADIANCE -> damage *= ElementalReactions.fireHit(target);
            case WATER -> damage *= ElementalReactions.waterHit(target, 120);
            case ICE -> ElementalReactions.iceHit(target);
            case CRYSTAL -> damage *= 1.25f;
            default -> {
            }
        }
        SpellDamage.hurtMultiHit(target, SpellDamage.source(level, element, null, null), damage);
        switch (element) {
            case FIRE -> target.igniteForTicks(80);
            case WATER -> {
                target.setDeltaMovement(target.getDeltaMovement().add(away.x * 0.7, 0.45, away.z * 0.7));
                target.hurtMarked = true;
            }
            case ICE -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                if (target.canFreeze()) {
                    target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 60));
                }
            }
            case WIND -> ElementalReactions.launchAirborne(target, 1.0);
            case EARTH -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                ElementalReactions.earthHit(target, null, 200);
            }
            case CRYSTAL -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            case LIGHTNING -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            }
            case RADIANCE -> {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
                target.igniteForTicks(40);
            }
        }
    }

    private static void effects(ServerLevel level, Vec3 at, Element element) {
        double x = at.x;
        double y = at.y + 0.2;
        double z = at.z;
        level.sendParticles(MobCasting.handsParticle(element), x, y + 0.4, z, 30, 0.5, 0.4, 0.5, 0.08);
        switch (element) {
            case FIRE -> level.sendParticles(ParticleTypes.FLAME, x, y, z, 40, 0.5, 0.6, 0.5, 0.06);
            case WATER -> level.sendParticles(ParticleTypes.SPLASH, x, y + 0.3, z, 60, 0.5, 0.4, 0.5, 0.3);
            case ICE -> level.sendParticles(ModContent.ICE_SHARD.get(), x, y + 0.3, z, 24, 0.4, 0.4, 0.4, 0.15);
            case WIND -> level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, x, y + 0.3, z, 1, 0, 0, 0, 0);
            case EARTH -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLED_DEEPSLATE.defaultBlockState()),
                    x, y + 0.3, z, 40, 0.5, 0.3, 0.5, 0.15);
            case CRYSTAL -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                    x, y + 0.3, z, 40, 0.5, 0.4, 0.5, 0.15);
            case LIGHTNING -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.6, z, 50, 0.5, 0.8, 0.5, 0.4);
            case RADIANCE -> {
                level.sendParticles(ParticleTypes.FLASH, x, y + 0.6, z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.END_ROD, x, y + 0.3, z, 24, 0.3, 0.8, 0.3, 0.05);
            }
        }
        level.playSound(null, x, y, z, sound(element), SoundSource.BLOCKS, 1.2f, 0.9f);
    }

    private static SoundEvent sound(Element element) {
        return switch (element) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case WATER -> SoundEvents.GENERIC_SPLASH;
            case ICE -> SoundEvents.GLASS_BREAK;
            case WIND -> SoundEvents.WIND_CHARGE_BURST.value();
            case EARTH -> SoundEvents.DEEPSLATE_BREAK;
            case CRYSTAL -> SoundEvents.AMETHYST_CLUSTER_BREAK;
            case LIGHTNING -> SoundEvents.TRIDENT_THUNDER.value();
            case RADIANCE -> SoundEvents.BEACON_DEACTIVATE;
        };
    }

    /** An armed glyph glimmers now and then; a flaring one blazes. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Stage stage = state.getValue(STAGE);
        if (stage == Stage.FLARING) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(MobCasting.handsParticle(element(state)), pos.getX() + random.nextDouble(), pos.getY() + 0.1,
                        pos.getZ() + random.nextDouble(), 0, 0.05, 0);
            }
        } else if (stage == Stage.ARMED && random.nextInt(12) == 0) {
            level.addParticle(MobCasting.handsParticle(element(state)), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.08,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.01, 0);
        }
    }
}
