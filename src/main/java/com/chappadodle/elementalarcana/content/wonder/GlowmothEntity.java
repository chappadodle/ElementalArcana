package com.chappadodle.elementalarcana.content.wonder;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.WonderRules;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A glowmoth (see the Wonders of the Wild spec): a moth whose wings glow in its element's colour,
 * out only at night. It flutters in loose loops near where it came, a few blocks over the ground,
 * and is drawn to light: each time it picks somewhere to flutter, it looks at a few spots near it
 * and makes for the brightest, so it ends up circling a lantern, a torch or a glowing herb. A
 * glass bottle used on it bottles it. As dawn comes, they fade away one by one.
 */
public class GlowmothEntity extends AmbientCreature {
    private static final EntityDataAccessor<Byte> ELEMENT = SynchedEntityData.defineId(GlowmothEntity.class, EntityDataSerializers.BYTE);

    @Nullable
    private BlockPos target;
    @Nullable
    private BlockPos home;

    public GlowmothEntity(EntityType<? extends GlowmothEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELEMENT, (byte) Element.RADIANCE.ordinal());
    }

    public Element element() {
        return Element.values()[Mth.clamp(entityData.get(ELEMENT), 0, Element.values().length - 1)];
    }

    public void setElement(Element element) {
        entityData.set(ELEMENT, (byte) element.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (random.nextInt(5) == 0) {
                level().addParticle(GlowParticleOptions.of(ModContent.FLARE.get(), element().color(), 0xFFFFFF, 0.05f, 16),
                        getX(), getY() + 0.1, getZ(), 0, -0.01, 0);
            }
        } else if (!WonderRules.mothTime(level().getDayTime()) && random.nextInt(80) == 0 && level() instanceof ServerLevel server) {
            // Dawn: it fades.
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.1, getZ(), 4, 0.1, 0.1, 0.1, 0.02);
            discard();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home == null) {
            home = blockPosition();
        }
        if (target != null && !level().isEmptyBlock(target)) {
            target = null;
        }
        if (target == null || random.nextInt(25) == 0 || target.closerToCenterThan(position(), 1.2)) {
            target = pickTarget();
        }
        // Flutter toward it, as a bat does, but gently.
        Vec3 to = Vec3.atCenterOf(target).subtract(position());
        Vec3 motion = getDeltaMovement();
        Vec3 next = motion.add((Math.signum(to.x) * 0.25 - motion.x) * 0.1, (Math.signum(to.y) * 0.3 - motion.y) * 0.1,
                (Math.signum(to.z) * 0.25 - motion.z) * 0.1);
        setDeltaMovement(next);
        float yaw = (float) (Mth.atan2(next.z, next.x) * Mth.RAD_TO_DEG) - 90f;
        zza = 0.5f;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
    }

    /** Where to flutter next: the brightest of a few spots near it, if any is brighter (moth to flame); else near home, low. */
    private BlockPos pickTarget() {
        BlockPos here = blockPosition();
        BlockPos brightest = null;
        int light = level().getBrightness(LightLayer.BLOCK, here);
        for (int i = 0; i < 6; i++) {
            BlockPos at = here.offset(random.nextInt(2 * WonderRules.LIGHT_REACH + 1) - WonderRules.LIGHT_REACH, random.nextInt(5) - 2,
                    random.nextInt(2 * WonderRules.LIGHT_REACH + 1) - WonderRules.LIGHT_REACH);
            int there = level().isEmptyBlock(at) ? level().getBrightness(LightLayer.BLOCK, at) : -1;
            if (there > light) {
                brightest = at;
                light = there;
            }
        }
        if (brightest != null) {
            return brightest;
        }
        BlockPos center = home != null && home.closerThan(here, 12) ? home : here;
        int x = center.getX() + random.nextInt(7) - 3;
        int z = center.getZ() + random.nextInt(7) - 3;
        int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new BlockPos(x, ground + 1 + random.nextInt(3), z);
    }

    /** A glass bottle used on it bottles it. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.GLASS_BOTTLE)) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, BottledGlowmothItem.of(element())));
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.NEUTRAL, 1f, 1.3f);
            if (player instanceof ServerPlayer serverPlayer) {
                MagicTriggers.fire(serverPlayer, "glowmoth_bottled", element().name().toLowerCase(Locale.ROOT), 1);
            }
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    protected float getSoundVolume() {
        return 0.2f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("element", entityData.get(ELEMENT));
        if (home != null) {
            tag.put("home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ELEMENT, tag.getByte("element"));
        home = NbtUtils.readBlockPos(tag, "home").orElse(null);
    }
}
