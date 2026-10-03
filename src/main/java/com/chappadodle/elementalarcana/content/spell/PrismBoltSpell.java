package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.Glow;
import com.chappadodle.elementalarcana.api.PrismBoltRules;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellHold;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.core.CastingService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Crystal's first spell: a fast bolt of violet crystal. Whatever it strikes, it bursts into shards
 * that fly on, fanned out (off a wall they fly back out from it), each hitting for a little under
 * half as much. Like Earth, a crystal hit Crystallizes anything burning, wet or frozen. It levels
 * to 10 (PrismBoltRules has the numbers; CrystalSpire is Lv 10's spire):
 * <pre>
 * Lv1 Prism Bolt       the bolt and 3 shards         Lv6  Crystal Shell   a heart of absorption per hit
 * Lv2 Keen Edge        a fifth harder                Lv7  Twin Prisms     two bolts
 * Lv3 Refraction       5 shards                      Lv8  Resonance       shards hit its bolt's mark half again as hard
 * Lv4 Piercing Light   bursts through one creature   Lv9  Brilliance      shards split once more
 * Lv5 Prismatic Lance | Geode Burst                  Lv10 Crystal Spire | Prism Barrage
 * </pre>
 * A bolt carries its level and forks in its data (shards copy them); a bolt without them, a crystal
 * wisp's or an Attuned creature's, is a Lv 1 bolt.
 */
public class PrismBoltSpell extends Spell implements ProjectileSpell {
    /** A shard of a burst bolt (SpellProjectile#variant). */
    public static final int SHARD = 1;
    private static final String TAG_LEVEL = "elementalarcana_prism_level";
    private static final String TAG_FORK = "elementalarcana_prism_fork";
    private static final String TAG_CAPSTONE = "elementalarcana_prism_capstone";
    private static final String TAG_CAST = "elementalarcana_prism_cast";
    /** What share of its full damage a bolt and its shards deal (a Prism Barrage's later bolts: half). */
    private static final String TAG_SHARE = "elementalarcana_prism_share";
    private static final String TAG_SPLIT = "elementalarcana_prism_split";
    private static final String TAG_PLANTED = "elementalarcana_prism_planted";
    /** On a creature a bolt struck (Resonance): which cast, and until when. */
    private static final String TAG_MARK_CAST = "elementalarcana_prism_mark";
    private static final String TAG_MARK_UNTIL = "elementalarcana_prism_mark_until";
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(0.82f, 0.55f, 1f), 0.9f);

    public PrismBoltSpell() {
        super(ModSchools.CRYSTAL, 22, 40);
    }

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(PrismBoltRules.LANCE, PrismBoltRules.GEODE);
            case 10 -> List.of(PrismBoltRules.SPIRE, PrismBoltRules.BARRAGE);
            default -> List.of();
        };
    }

    @Override
    public CastResult cast(CastContext context) {
        int level = context.spellLevel();
        String fork = level >= 5 ? context.branch(5) : null;
        String capstone = level >= 10 ? context.branch(10) : null;
        int cast = context.caster().getRandom().nextInt();
        fire(context, level, fork, capstone, cast, PrismBoltRules.bolts(level), 1f);
        if (PrismBoltRules.BARRAGE.equals(capstone)) {
            context.holdUntilRelease(barrage(context, level, fork, capstone));
        }
        return CastResult.SUCCESS;
    }

    /** Throws {@code bolts} bolts (two side by side, for Twin Prisms), each carrying its level, forks and damage share. */
    private void fire(CastContext context, int level, @Nullable String fork, @Nullable String capstone, int cast, int bolts, float share) {
        ServerPlayer caster = context.caster();
        float yaw = caster.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        for (int i = 0; i < bolts; i++) {
            SpellProjectile bolt = SpellProjectile.launch(context, this);
            if (bolts > 1) {
                bolt.setPos(bolt.position().add(right.scale(i == 0 ? -PrismBoltRules.TWIN_OFFSET : PrismBoltRules.TWIN_OFFSET)));
            }
            bolt.setDeltaMovement(bolt.getDeltaMovement().normalize().scale(PrismBoltRules.speed(fork)));
            bolt.setPierce(PrismBoltRules.pierce(level, fork));
            CompoundTag tag = bolt.getPersistentData();
            tag.putInt(TAG_LEVEL, level);
            tag.putString(TAG_FORK, fork == null ? "" : fork);
            tag.putString(TAG_CAPSTONE, capstone == null ? "" : capstone);
            tag.putInt(TAG_CAST, cast);
            tag.putFloat(TAG_SHARE, share);
        }
        Vec3 eye = caster.getEyePosition();
        context.level().playSound(null, eye.x, eye.y, eye.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
    }

    /** Prism Barrage: while held, another bolt every few ticks (one, at half strength), each paid for in mana; out of mana, it ends. */
    private SpellHold barrage(CastContext context, int level, @Nullable String fork, @Nullable String capstone) {
        ServerPlayer caster = context.caster();
        return new SpellHold() {
            @Override
            public boolean tick(int heldTicks) {
                if (!caster.isAlive()) {
                    return false;
                }
                if (heldTicks <= 0 || heldTicks % PrismBoltRules.BARRAGE_INTERVAL != 0) {
                    return true;
                }
                if (!CastingService.payUpkeep(caster, MagicAttachments.get(caster), PrismBoltRules.BARRAGE_UPKEEP)) {
                    return false;
                }
                fire(context, level, fork, capstone, caster.getRandom().nextInt(), 1, PrismBoltRules.BARRAGE_SHARE);
                return true;
            }

            @Override
            public void release(int heldTicks) {
            }

            @Override
            public void cancel() {
            }

            @Override
            public int maxHoldTicks() {
                return PrismBoltRules.BARRAGE_TICKS;
            }
        };
    }

    @Override
    public ParticleOptions trailParticle() {
        return TRAIL;
    }

    @Override
    public float projectileSpeed() {
        return (float) PrismBoltRules.SPEED;
    }

    @Override
    public int lifetimeTicks() {
        return 40;
    }

    @Override
    public Glow glow(int variant) {
        return new Glow(0xD08CFF, variant == SHARD ? 0.45f : 0.9f, 0.7f);
    }

    @Override
    public int luminance(SpellProjectile projectile) {
        return projectile.variant() == SHARD ? 4 : 9;
    }

    @Override
    public void onHitEntity(SpellProjectile projectile, EntityHitResult hit) {
        if (!(projectile.level() instanceof ServerLevel level) || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        Entity owner = projectile.getOwner();
        CompoundTag tag = projectile.getPersistentData();
        int spellLevel = levelOf(projectile);
        String fork = text(tag, TAG_FORK);
        boolean shard = projectile.variant() == SHARD;
        Vec3 at = projectile.impactPoint(hit);
        float damage = (shard ? PrismBoltRules.SHARD_DAMAGE * PrismBoltRules.shardFactor(fork) : PrismBoltRules.DAMAGE)
                * PrismBoltRules.damageFactor(spellLevel) * share(tag) * projectile.power();
        if (shard) {
            if (tag.getBoolean(TAG_SPLIT)) {
                damage *= PrismBoltRules.SPLIT_FACTOR;
            }
            if (PrismBoltRules.resonance(spellLevel) && resonates(target, tag, level)) {
                damage *= PrismBoltRules.RESONANCE;
                level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 4, 0.15, 0.15, 0.15, 0.08);
            }
        }
        SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.CRYSTAL, projectile, owner), damage);
        if (!shard) {
            ElementalReactions.earthHit(target, owner, 200);
            mark(target, tag, level);
            if (PrismBoltRules.shell(spellLevel) && owner instanceof LivingEntity caster) {
                harden(level, caster);
            }
            plant(level, projectile, target.position());
            if (PrismBoltRules.burstsOnCreatures(fork)) {
                burst(level, projectile, at, projectile.getDeltaMovement(), target);
            }
        } else if (PrismBoltRules.splits(spellLevel) && !tag.getBoolean(TAG_SPLIT)) {
            split(level, projectile, at, target);
        }
        shatter(level, at, shard);
    }

    @Override
    public void onHitBlock(SpellProjectile projectile, BlockHitResult hit) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        boolean shard = projectile.variant() == SHARD;
        if (!shard) {
            // Off a wall the shards fly back out: the bolt's direction, mirrored in the face it struck.
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 direction = projectile.getDeltaMovement();
            Vec3 reflected = direction.subtract(normal.scale(2 * direction.dot(normal)));
            Vec3 at = hit.getLocation().add(normal.scale(0.2));
            plant(level, projectile, hit.getDirection() == Direction.UP ? hit.getLocation() : at.subtract(0, 0.5, 0));
            burst(level, projectile, at, reflected, null);
        }
        shatter(level, hit.getLocation(), shard);
    }

    /**
     * The burst: shards from {@code at}, fanned around {@code direction} (or all the way round, for a
     * Geode Burst), sparing what the bolt just struck.
     */
    private void burst(ServerLevel level, SpellProjectile bolt, Vec3 at, Vec3 direction, @Nullable Entity spare) {
        Entity owner = bolt.getOwner();
        if (owner == null || direction.lengthSqr() < 1.0e-6) {
            return;
        }
        int spellLevel = levelOf(bolt);
        String fork = text(bolt.getPersistentData(), TAG_FORK);
        int count = PrismBoltRules.shards(spellLevel, fork);
        float spread = (float) Math.toRadians(PrismBoltRules.spreadDegrees(spellLevel, fork));
        Vec3 forward = direction.normalize();
        if (PrismBoltRules.GEODE.equals(fork)) {
            // All the way round, flat, from a little above the impact so none go straight into the ground.
            Vec3 flat = new Vec3(forward.x, 0, forward.z);
            forward = flat.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : flat.normalize();
            at = at.add(0, 0.3, 0);
        }
        for (int i = 0; i < count; i++) {
            float angle = PrismBoltRules.GEODE.equals(fork) ? spread * i : spread * (i - (count - 1) / 2f);
            Vec3 velocity = forward.yRot(angle).add(0, PrismBoltRules.GEODE.equals(fork) ? 0.05 : 0, 0).normalize().scale(1.3);
            shard(owner, bolt, at, velocity, spare, false, 0.6f);
        }
    }

    /** Brilliance: a shard that strikes splits into two small ones flying on, sparing what it struck. */
    private void split(ServerLevel level, SpellProjectile shard, Vec3 at, Entity spare) {
        Entity owner = shard.getOwner();
        Vec3 direction = shard.getDeltaMovement();
        if (owner == null || direction.lengthSqr() < 1.0e-6) {
            return;
        }
        float spread = (float) Math.toRadians(PrismBoltRules.SPLIT_SPREAD_DEGREES);
        for (int side = -1; side <= 1; side += 2) {
            shard(owner, shard, at, direction.normalize().yRot(spread * side).scale(1.2), spare, true, 0.4f);
        }
    }

    /** One shard, carrying its parent's level, forks and cast. */
    private void shard(Entity owner, SpellProjectile parent, Vec3 at, Vec3 velocity, @Nullable Entity spare, boolean split, float scale) {
        SpellProjectile shard = SpellProjectile.shootFrom(owner, this, at, velocity, parent.power());
        shard.setVariant(SHARD);
        shard.setVisualScale(scale);
        if (spare != null) {
            shard.ignoreEntity(spare);
        }
        CompoundTag from = parent.getPersistentData();
        CompoundTag tag = shard.getPersistentData();
        tag.putInt(TAG_LEVEL, levelOf(parent));
        tag.putString(TAG_FORK, from.getString(TAG_FORK));
        tag.putInt(TAG_CAST, from.getInt(TAG_CAST));
        tag.putFloat(TAG_SHARE, share(from));
        tag.putBoolean(TAG_SPLIT, split);
    }

    /** Crystal Shell: a heart of absorption hardens on the caster, up to four, for 15 seconds. */
    private static void harden(ServerLevel level, LivingEntity caster) {
        float shell = PrismBoltRules.shellAfterHit(caster.getAbsorptionAmount());
        // Absorption II holds up to four hearts; set the shell after it (it would fill them all at once).
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, PrismBoltRules.SHELL_TICKS, 1, false, false, true));
        caster.setAbsorptionAmount(shell);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.3f);
        // A few small glints (crumbs of amethyst here would fill the caster's own view).
        level.sendParticles(ParticleTypes.END_ROD, caster.getX(), caster.getY(0.4), caster.getZ(), 3, 0.35, 0.3, 0.35, 0.02);
    }

    /** Resonance: marks a creature the bolt struck, for its shards. */
    private static void mark(LivingEntity target, CompoundTag bolt, ServerLevel level) {
        target.getPersistentData().putInt(TAG_MARK_CAST, bolt.getInt(TAG_CAST));
        target.getPersistentData().putLong(TAG_MARK_UNTIL, level.getGameTime() + PrismBoltRules.RESONANCE_TICKS);
    }

    /** Whether a shard's target was struck by its own bolt a moment ago. */
    private static boolean resonates(LivingEntity target, CompoundTag shard, ServerLevel level) {
        CompoundTag data = target.getPersistentData();
        return data.contains(TAG_MARK_CAST) && data.getInt(TAG_MARK_CAST) == shard.getInt(TAG_CAST)
                && level.getGameTime() <= data.getLong(TAG_MARK_UNTIL);
    }

    /** Crystal Spire: where a bolt first strikes, its spire grows (one per cast). */
    private static void plant(ServerLevel level, SpellProjectile bolt, Vec3 at) {
        CompoundTag tag = bolt.getPersistentData();
        if (!PrismBoltRules.SPIRE.equals(text(tag, TAG_CAPSTONE)) || tag.getBoolean(TAG_PLANTED)
                || !(bolt.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        tag.putBoolean(TAG_PLANTED, true);
        CrystalSpire.grow(level, owner, at, bolt.power(), tag.getInt(TAG_LEVEL), text(tag, TAG_FORK), tag.getInt(TAG_CAST));
    }

    /**
     * A shard thrown by a Crystal Spire at {@code target}, as one of {@code owner}'s with the spire's
     * level and fork (it never plants a spire of its own).
     */
    void throwFromSpire(Entity owner, Vec3 from, LivingEntity target, float power, int spellLevel, @Nullable String fork, int cast) {
        Vec3 velocity = target.getBoundingBox().getCenter().subtract(from).normalize().scale(PrismBoltRules.SPIRE_SHARD_SPEED);
        SpellProjectile shard = SpellProjectile.shootFrom(owner, this, from, velocity, power);
        shard.setVariant(SHARD);
        shard.setVisualScale(0.6f);
        CompoundTag tag = shard.getPersistentData();
        tag.putInt(TAG_LEVEL, spellLevel);
        tag.putString(TAG_FORK, fork == null ? "" : fork);
        tag.putInt(TAG_CAST, cast);
    }

    /** The damage share a bolt or shard carries (1 when it carries none). */
    private static float share(CompoundTag tag) {
        return tag.contains(TAG_SHARE) ? tag.getFloat(TAG_SHARE) : 1f;
    }

    private static int levelOf(SpellProjectile projectile) {
        return Math.max(1, projectile.getPersistentData().getInt(TAG_LEVEL));
    }

    @Nullable
    private static String text(CompoundTag tag, String key) {
        String value = tag.getString(key);
        return value.isEmpty() ? null : value;
    }

    private static void shatter(ServerLevel level, Vec3 at, boolean shard) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                at.x, at.y, at.z, shard ? 6 : 16, 0.15, 0.15, 0.15, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, shard ? 1 : 5, 0.1, 0.1, 0.1, 0.06);
        level.playSound(null, at.x, at.y, at.z, shard ? SoundEvents.AMETHYST_BLOCK_HIT : SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.PLAYERS, shard ? 0.5f : 1f, shard ? 1.6f : 1.1f);
    }
}
