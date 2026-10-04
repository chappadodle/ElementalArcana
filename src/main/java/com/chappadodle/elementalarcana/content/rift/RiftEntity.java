package com.chappadodle.elementalarcana.content.rift;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.RiftRules;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import com.chappadodle.elementalarcana.content.people.Bounties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * An Elemental Rift (see Rifts and the Rifts spec): a tear in the air that opens, sends out three
 * waves of creatures Attuned to its element, and, once the last is down, collapses and throws out
 * its cache. Only kills count: left alone, or if its creatures go some other way (the world turned
 * peaceful, a creature lost to an unloaded chunk), it fades, pulling what's left of them back in.
 * Its element and stage are synced (client/RiftRenderer draws it); it's never saved.
 */
public class RiftEntity extends Entity {
    public static final int OPENING = 0;
    public static final int OPEN = 1;
    public static final int CLOSING = 2;
    public static final int FADING = 3;
    public static final String CREATURE_TAG = "elementalarcana_rift";
    private static final ResourceKey<LootTable> CACHE = ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("gameplay/rift_cache"));
    private static final EntityDataAccessor<Integer> ELEMENT = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    /** How high its middle is above its foot. */
    public static final double MIDDLE = 2.2;

    private int stageTicks;
    private AttunementRank warden = AttunementRank.ARCHMAGE;
    private int wave;
    private int waveSize;
    private long nextSpawnAt;
    private long lastPresence;
    /** What's still to climb out this wave. */
    private final Deque<Spawn> queue = new ArrayDeque<>();
    private final Set<Mob> members = new HashSet<>();
    @Nullable
    private ServerBossEvent bar;

    /** One of a wave's creatures: Attuned with this rank, or a wisp (no rank). */
    private record Spawn(@Nullable AttunementRank rank) {
    }

    public RiftEntity(EntityType<? extends RiftEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ELEMENT, Element.FIRE.ordinal());
        builder.define(STAGE, OPENING);
    }

    public Element element() {
        return Element.values()[entityData.get(ELEMENT)];
    }

    void setElement(Element element) {
        entityData.set(ELEMENT, element.ordinal());
    }

    void setWarden(AttunementRank warden) {
        this.warden = warden;
    }

    public int stage() {
        return entityData.get(STAGE);
    }

    private void setStage(int stage) {
        entityData.set(STAGE, stage);
        stageTicks = 0;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STAGE.equals(key)) {
            stageTicks = 0;
        }
    }

    /** How far open the tear is this frame, 0 to 1: opening, open, then closing or fading. */
    public float openness(float partialTicks) {
        float time = stageTicks + partialTicks;
        return switch (stage()) {
            case OPENING -> {
                float t = Math.min(1f, time / RiftRules.OPENING_TICKS);
                yield 1f - (1f - t) * (1f - t);
            }
            case OPEN -> 1f;
            default -> Math.max(0f, 1f - time / RiftRules.CLOSING_TICKS);
        };
    }

    public int stageTicks() {
        return stageTicks;
    }

    public Vec3 middle() {
        return position().add(0, MIDDLE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        stageTicks++;
        if (level().isClientSide()) {
            motes();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        long now = level.getGameTime();
        if (lastPresence == 0) {
            lastPresence = now;
        }
        if (level.getNearestPlayer(getX(), getY(), getZ(), RiftRules.PRESENCE, player -> !player.isSpectator()) != null) {
            lastPresence = now;
        }
        if (tickCount % 10 == 0) {
            updateBar(level);
        }
        if (tickCount % 40 == 0) {
            level.playSound(null, getX(), getY() + MIDDLE, getZ(), SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, 0.7f, 0.6f);
        }
        switch (stage()) {
            case OPENING -> {
                if (stageTicks >= RiftRules.OPENING_TICKS) {
                    setStage(OPEN);
                    nextWave(level, now);
                }
            }
            case OPEN -> {
                if (now - lastPresence > RiftRules.ABANDON_TICKS || tickCount > RiftRules.MAX_OPEN_TICKS
                        || level.getDifficulty() == Difficulty.PEACEFUL || members.stream().anyMatch(RiftEntity::lostOtherwise)) {
                    fade(level);
                    return;
                }
                if (!queue.isEmpty() && now >= nextSpawnAt) {
                    climbOut(level, queue.poll().rank());
                    nextSpawnAt = now + RiftRules.SPAWN_GAP_TICKS;
                }
                members.removeIf(Entity::isRemoved);
                if (queue.isEmpty() && members.isEmpty()) {
                    if (wave >= RiftRules.WAVES) {
                        collapse(level);
                    } else {
                        nextWave(level, now);
                    }
                }
            }
            case CLOSING -> {
                if (stageTicks >= RiftRules.CLOSING_TICKS) {
                    spill(level);
                    discard();
                }
            }
            default -> {
                if (stageTicks >= RiftRules.CLOSING_TICKS) {
                    discard();
                }
            }
        }
    }

    private void nextWave(ServerLevel level, long now) {
        wave++;
        queue.clear();
        RiftRules.wave(wave, warden).forEach(rank -> queue.add(new Spawn(rank)));
        for (int i = 0; i < RiftRules.wisps(wave); i++) {
            queue.add(new Spawn(null));
        }
        waveSize = queue.size();
        nextSpawnAt = now;
        Vec3 at = middle();
        if (wave == RiftRules.WAVES) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 1.6f);
        } else {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 0.8f, 1.4f);
        }
    }

    /** A creature climbs out of the tear: the land's own, Attuned to the rift's element, after the nearest player. */
    private void climbOut(ServerLevel level, @Nullable AttunementRank rank) {
        Vec3 at = position().add((random.nextDouble() - 0.5) * 0.8, 0, (random.nextDouble() - 0.5) * 0.8);
        BlockPos pos = BlockPos.containing(at);
        Mob mob;
        if (rank == null) {
            mob = WispSpawner.spawnAt(level, element(), pos.above(), MobSpawnType.EVENT);
        } else {
            mob = creatureFor(level, pos).spawn(level, pos, MobSpawnType.EVENT);
            if (mob != null && !Attunement.attune(mob, element(), rank)) {
                // Born with another element: a zombie instead.
                mob.discard();
                mob = EntityType.ZOMBIE.spawn(level, pos, MobSpawnType.EVENT);
                if (mob != null) {
                    Attunement.attune(mob, element(), rank);
                }
            }
        }
        if (mob == null) {
            return;
        }
        mob.addTag(CREATURE_TAG);
        // It stays for the fight (and goes back into the rift if the rift fades, see fade).
        mob.setPersistenceRequired();
        members.add(mob);
        Player target = level.getNearestPlayer(getX(), getY(), getZ(), RiftRules.PRESENCE,
                entity -> entity instanceof Player player && !player.isSpectator() && !player.isCreative());
        if (target != null) {
            mob.setTarget(target);
            Vec3 toward = target.position().subtract(at);
            Vec3 flat = new Vec3(toward.x, 0, toward.z);
            if (flat.lengthSqr() > 1.0e-4) {
                mob.setDeltaMovement(flat.normalize().scale(0.3).add(0, 0.3, 0));
            }
        }
        int color = element().color();
        level.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), color, color, 0.6f, 8), at.x, at.y + 1, at.z, 6, 0.3, 0.6, 0.3, 0.02);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 20, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 0.7f);
    }

    /** The land's creatures: strays in the snow, husks in the desert, else zombies, skeletons and spiders. */
    private EntityType<? extends Mob> creatureFor(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        int roll = random.nextInt(5);
        if (biome.value().coldEnoughToSnow(pos)) {
            return roll < 3 ? EntityType.STRAY : EntityType.ZOMBIE;
        }
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BADLANDS)) {
            return roll < 3 ? EntityType.HUSK : EntityType.SKELETON;
        }
        return roll < 2 ? EntityType.ZOMBIE : roll < 4 ? EntityType.SKELETON : EntityType.SPIDER;
    }

    /** Whether a creature of the rift is gone some way other than being killed. */
    private static boolean lostOtherwise(Mob mob) {
        return mob.isRemoved() && mob.getRemovalReason() != RemovalReason.KILLED;
    }

    /** Left alone, or cheated of its kills: the tear fades, pulling what's left of its creatures back in. */
    private void fade(ServerLevel level) {
        setStage(FADING);
        for (Mob mob : members) {
            if (!mob.isRemoved()) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY(0.5), mob.getZ(), 16, 0.3, 0.5, 0.3, 0.05);
                mob.discard();
            }
        }
        members.clear();
        queue.clear();
        Vec3 at = middle();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1f, 1.4f);
    }

    /** The last wave is down: the tear collapses (its cache spills when it's shut). */
    private void collapse(ServerLevel level) {
        setStage(CLOSING);
        Vec3 at = middle();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5f, 0.6f);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < RiftRules.PRESENCE * RiftRules.PRESENCE) {
                MagicTriggers.fire(player, "rift_closed", element().name().toLowerCase(Locale.ROOT), 1);
                Bounties.riftClosed(player);
            }
        }
    }

    /** Shut: a flash and a boom, and the cache thrown out. */
    private void spill(ServerLevel level) {
        Vec3 at = middle();
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), element().color(), 0xFFFFFF, 0.8f, 12), at.x, at.y, at.z,
                24, 0.6, 1.0, 0.6, 0.15);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.END_GATEWAY_SPAWN, SoundSource.HOSTILE, 1.5f, 1.2f);
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(ModItems.essence(element()), RiftRules.essence(random::nextDouble)));
        LootTable table = level.getServer().reloadableRegistries().getLootTable(CACHE);
        table.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, at).create(LootContextParamSets.CHEST),
                loot::add);
        for (ItemStack stack : loot) {
            ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
            item.setDeltaMovement((random.nextDouble() - 0.5) * 0.35, 0.25 + random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.35);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        ExperienceOrb.award(level, at, 50);
    }

    // ---- the boss bar ----

    private void updateBar(ServerLevel level) {
        if (bar == null) {
            bar = new ServerBossEvent(Component.empty(), barColor(element()), BossEvent.BossBarOverlay.PROGRESS);
        }
        Component name = Component.translatable("school.elementalarcana." + element().name().toLowerCase(Locale.ROOT));
        if (wave == 0) {
            bar.setName(Component.translatable("bossbar.elementalarcana.rift_opening", name));
            bar.setProgress(1f);
        } else {
            bar.setName(Component.translatable("bossbar.elementalarcana.rift", name, wave, RiftRules.WAVES));
            bar.setProgress(waveSize == 0 ? 0f : (queue.size() + members.size()) / (float) waveSize);
        }
        for (ServerPlayer player : level.players()) {
            boolean near = !isRemoved() && stage() != FADING && player.distanceToSqr(this) < RiftRules.PRESENCE * RiftRules.PRESENCE;
            if (near) {
                bar.addPlayer(player);
            } else {
                bar.removePlayer(player);
            }
        }
    }

    private static BossEvent.BossBarColor barColor(Element element) {
        return switch (element) {
            case FIRE -> BossEvent.BossBarColor.RED;
            case WATER -> BossEvent.BossBarColor.BLUE;
            case WIND -> BossEvent.BossBarColor.GREEN;
            case EARTH, LIGHTNING -> BossEvent.BossBarColor.YELLOW;
            case CRYSTAL -> BossEvent.BossBarColor.PURPLE;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    // ---- the client's motes ----

    /** Motes of the element drifting into the tear (rushing in as it closes). */
    private void motes() {
        Vec3 middle = middle();
        boolean closing = stage() == CLOSING;
        int count = closing ? 4 : 1;
        int color = element().color();
        for (int i = 0; i < count; i++) {
            Vec3 offset = new Vec3(random.nextGaussian(), random.nextGaussian() * 1.2, random.nextGaussian()).normalize()
                    .scale(1.6 + random.nextDouble() * 1.2);
            Vec3 from = middle.add(offset);
            Vec3 velocity = offset.scale(closing ? -0.12 : -0.05);
            level().addParticle(GlowParticleOptions.of(ModContent.SPARK.get(), color, 0xFFFFFF, 0.1f, 18), from.x, from.y, from.z,
                    velocity.x, velocity.y, velocity.z);
        }
    }

    // ---- never hurt, never saved ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
