package com.chappadodle.elementalarcana.content.circle;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CircleTalk;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.MageAlly;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.hollowed.HollowedEntity;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import com.chappadodle.elementalarcana.content.tower.KeepDistanceGoal;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

/**
 * One of the Circle's mages (see the Circle spec, parts 1 and 2), or their Archmagister: friendly,
 * a villager's look in white robes trimmed with its element's colour (the Archmagister's with
 * gold). It keeps near the Enclave's Spire, greets a player who uses it (a line of talk by the
 * player's story, CircleTalk), and fights any monster near the Spire and the Hollowed anywhere
 * near, as an Attuned caster of its element. It never turns on a player (nor their pets), even one
 * who strikes it. A player's magic spares it, and its magic touches only monsters (MageAlly,
 * SpellTargets).
 * <p>
 * The Archmagister also hands out and takes in commissions (Commissions), and, used by someone
 * sneaking, opens the Circle's stores (a vanilla merchant, CircleStores). A mage called by a Sigil
 * of the Circle has no Spire: it follows the one it serves, takes on their foes, and bows out when
 * its time is up (CircleSigilItem). An Enclave's mage (the Archmagister too) offered a Mark of the
 * Circle takes it as a wager and duels for it in the Enclave's ring, once a day (Duels).
 */
public class CircleMageEntity extends PathfinderMob implements MageAlly, Merchant {
    private static final EntityDataAccessor<Byte> ELEMENT = SynchedEntityData.defineId(CircleMageEntity.class, EntityDataSerializers.BYTE);
    private static final String[] NAMES = {"Ilsa", "Corvin", "Maren", "Tobin", "Elowen", "Fenn", "Rhosyn", "Aldric", "Wren", "Sabine",
            "Odo", "Brisa", "Halvard", "Nim", "Teodor", "Liesel"};
    /** How far from the Spire it keeps, and defends. */
    private static final int HOME_RADIUS = 20;
    private static final double DEFEND_RADIUS = 24;
    /** A sigil's mage: how near it keeps to the one it serves, how far from them it fights, and how far behind it's left before it's beside them again. */
    private static final double FOLLOW_DISTANCE = 6;
    private static final double GUARD_RADIUS = 12;
    private static final double LEASH = 24;

    private final boolean archmagister;
    @Nullable
    private BlockPos home;
    private int talks;
    private int nameIndex = -1;
    /** For a sigil's mage: whom it serves, and when it leaves (game time). */
    @Nullable
    private UUID companion;
    private long leaveAt;
    /** The Archmagister's stores: who's trading, what's for sale and the day it was stocked. */
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    private long stockedDay = -1;
    /** The day it last fought a duel (one a day), and the ring of the bout it's promised to (or null). */
    private long lastDuelDay = -1;
    @Nullable
    private BlockPos duelRing;

    public CircleMageEntity(EntityType<? extends CircleMageEntity> type, Level level, boolean archmagister) {
        super(type, level);
        this.archmagister = archmagister;
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes(boolean archmagister) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, archmagister ? 80 : 40)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    public boolean isArchmagister() {
        return archmagister;
    }

    public Element element() {
        return Element.values()[Mth.clamp(entityData.get(ELEMENT), 0, Element.values().length - 1)];
    }

    /** Sets its element and the Spire it keeps near (before it's added to the world). */
    public void serve(Element element, BlockPos home) {
        entityData.set(ELEMENT, (byte) element.ordinal());
        this.home = home.immutable();
        restrictTo(this.home, archmagister ? 4 : HOME_RADIUS);
    }

    /** Makes it a sigil's mage, at {@code player}'s side until game time {@code until}. */
    public void attend(Player player, long until) {
        companion = player.getUUID();
        leaveAt = until;
        home = null;
        clearRestriction();
    }

    /** Whom it serves, if it came at a sigil's call. */
    @Nullable
    public UUID companionId() {
        return companion;
    }

    @Nullable
    private Player companionPlayer() {
        return companion == null ? null : level().getPlayerByUUID(companion);
    }

    /** Where its Enclave's ring is (the middle, where duellists stand), or null if it has no Enclave. */
    @Nullable
    BlockPos ring() {
        if (home == null || companion != null) {
            return null;
        }
        // The Archmagister keeps to the study: seven up from the courtyard's floor, a step north.
        BlockPos floor = archmagister ? home.below(EnclavePiece.STOREY + 1).south() : home;
        return floor.offset(12, 0, -12);
    }

    long lastDuelDay() {
        return lastDuelDay;
    }

    void markDuelled(long day) {
        lastDuelDay = day;
    }

    /** Promised to a bout: off to the ring (the Archmagister steps there at once, by magic). */
    void goToRing(BlockPos ring) {
        duelRing = ring;
        restrictTo(ring, 5);
        if (archmagister) {
            stepInto(ring);
        } else {
            getNavigation().moveTo(ring.getX() + 0.5, ring.getY(), ring.getZ() + 0.5, 1.0);
        }
    }

    /** Into the ring by magic, a few steps north of its middle: a flash where it was, and where it lands. */
    void stepInto(BlockPos ring) {
        flash();
        moveTo(ring.getX() + 0.5, ring.getY(), ring.getZ() - 2.5, getYRot(), getXRot());
        getNavigation().stop();
        flash();
    }

    /** In a bout: {@code player} is its one foe, whatever else comes near. */
    void fight(Player player) {
        targetSelector.disableControlFlag(Goal.Flag.TARGET);
        if (getTarget() != player) {
            setTarget(player);
        }
    }

    /** The bout over: whole again, and back to its post (the Archmagister to the study, by magic). */
    void endDuel() {
        duelRing = null;
        targetSelector.enableControlFlag(Goal.Flag.TARGET);
        setTarget(null);
        setHealth(getMaxHealth());
        clearFire();
        if (home != null) {
            restrictTo(home, archmagister ? 4 : HOME_RADIUS);
            if (archmagister) {
                goHome();
            }
        }
    }

    /** The Archmagister, back to the study by magic. */
    private void goHome() {
        if (home != null) {
            flash();
            moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, getYRot(), getXRot());
            getNavigation().stop();
            flash();
        }
    }

    private void flash() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 16, 0.3, 0.6, 0.3, 0.05);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.NEUTRAL, 0.8f, 1.2f);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELEMENT, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new StandForTradeGoal());
        goalSelector.addGoal(1, new CastMobSpellGoal(this));
        goalSelector.addGoal(2, new FollowCompanionGoal());
        goalSelector.addGoal(2, new DuelFootworkGoal());
        goalSelector.addGoal(3, new KeepDistanceGoal(this, 4, 9));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.45));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        // It never takes a player (or their pet, or another of the mages' side) as its foe, even one who strikes it.
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class, MageAlly.class) {
            @Override
            public boolean canUse() {
                return !(mob.getLastHurtByMob() instanceof OwnableEntity pet && pet.getOwnerUUID() != null) && super.canUse();
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, HollowedEntity.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                target -> target instanceof Enemy && !(target instanceof MageAlly) && nearHome(target.blockPosition())));
    }

    /** Whether {@code pos} is near what it guards: the Spire, or (a sigil's mage) the one it serves. */
    private boolean nearHome(BlockPos pos) {
        if (companion != null) {
            Player player = companionPlayer();
            return player != null && pos.closerThan(player.blockPosition(), GUARD_RADIUS);
        }
        return home == null || pos.closerThan(home, DEFEND_RADIUS);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        if (home == null) {
            // From an egg, a command or a sigil: an element of its own.
            entityData.set(ELEMENT, (byte) getRandom().nextInt(Element.values().length));
        }
        nameIndex = getRandom().nextInt(NAMES.length);
        Attunement.attune(this, element(), archmagister ? AttunementRank.ARCHMAGE : AttunementRank.MAGUS);
        setPersistenceRequired();
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // Brought without a spawn's setting up (a /summon with data skips it): Attuned now, so it can cast.
        if (Attunement.get(this) == null) {
            Attunement.attune(this, element(), archmagister ? AttunementRank.ARCHMAGE : AttunementRank.MAGUS);
        }
        if (companion != null) {
            serveCompanion();
        }
        // A new day, new stock (and new rumours), once no one's trading.
        if (offers != null && tradingPlayer == null && level().getDayTime() / 24000L != stockedDay) {
            offers = null;
        }
        // The Archmagister, away from the study (a bout's end missed, say) and in no bout: back there.
        if (archmagister && home != null && tickCount % 100 == 0 && distanceToSqr(home.getCenter()) > 64 && !Duels.busy(this)) {
            goHome();
        }
    }

    /** A sigil's mage: gone when its time's up or the one it serves is; beside them if left behind; their foes its foes. */
    private void serveCompanion() {
        Player player = companionPlayer();
        if (player == null || !player.isAlive() || player.isSpectator() || level().getGameTime() >= leaveAt) {
            bowOut();
            return;
        }
        if (distanceToSqr(player) > LEASH * LEASH) {
            comeBeside(player);
        }
        if (getTarget() == null) {
            LivingEntity attacker = player.getLastHurtByMob();
            LivingEntity struck = player.getLastHurtMob();
            if (attacker != null && attacker.isAlive() && !SpellTargets.spares(this, attacker)) {
                setTarget(attacker);
            } else if (struck instanceof Enemy && struck.isAlive() && player.tickCount - player.getLastHurtMobTimestamp() < 100) {
                setTarget(struck);
            }
        }
    }

    /** A free spot two or three blocks from {@code player}, stepped to at once (as a wolf comes to its owner). */
    private void comeBeside(Player player) {
        BlockPos at = player.blockPosition();
        for (int i = 0; i < 12; i++) {
            int dx = getRandom().nextIntBetweenInclusive(-3, 3);
            int dz = getRandom().nextIntBetweenInclusive(-3, 3);
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            BlockPos spot = at.offset(dx, 0, dz);
            if (level().getBlockState(spot.below()).isFaceSturdy(level(), spot.below(), Direction.UP)
                    && level().noCollision(this, getBoundingBox().move(spot.getX() + 0.5 - getX(), spot.getY() - getY(), spot.getZ() + 0.5 - getZ()))) {
                moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, getYRot(), getXRot());
                getNavigation().stop();
                return;
            }
        }
    }

    /** A sigil's mage, its service done: a flash of light, and it's gone. */
    public void bowOut() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 24, 0.3, 0.6, 0.3, 0.06);
            server.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.8f, 1.4f);
        }
        discard();
    }

    /** Who's speaking: "Ilsa, a mage of the Circle (Fire)", or the Archmagister by name and title. */
    public Component speaker() {
        Component school = Component.translatable("school.elementalarcana." + element().name().toLowerCase(Locale.ROOT));
        String name = NAMES[Math.floorMod(nameIndex < 0 ? getId() : nameIndex, NAMES.length)];
        return archmagister ? Component.translatable("entity.elementalarcana.archmagister.named", name)
                : Component.translatable("entity.elementalarcana.circle_mage.named", name, school);
    }

    /** Says {@code words} to {@code player}, in chat: "Ilsa, a mage of the Circle (Fire): ...". */
    public void say(Player player, Component words) {
        player.sendSystemMessage(Component.translatable("message.elementalarcana.circle.says", speaker(), words)
                .withStyle(archmagister ? ChatFormatting.GOLD : ChatFormatting.WHITE));
    }

    /**
     * Used: it greets the player, a line by their story so far. The Archmagister takes a finished
     * commission instead, and after greeting gives one (or a word on the one carried); used by
     * someone sneaking, the Archmagister opens the Circle's stores. Held out a Mark of the Circle,
     * an Enclave's mage takes it as a duel's wager (Duels).
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || getTarget() != null || tradingPlayer != null) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            getLookControl().setLookAt(player, 30f, 30f);
            ItemStack held = player.getItemInHand(hand);
            if (held.is(ModCircle.MARK.get()) && companion == null && !player.isSecondaryUseActive()) {
                Duels.challenge(this, serverPlayer, held);
            } else if (archmagister && player.isSecondaryUseActive()) {
                setTradingPlayer(player);
                openTradingScreen(player, Component.translatable("merchant.elementalarcana.circle_stores"), 0);
            } else if (!(archmagister && Commissions.handIn(this, serverPlayer))) {
                MagicData data = MagicAttachments.get(player);
                CircleTalk.Stage stage = CircleTalk.stage(data.isAwakened(), data.level());
                String key = archmagister ? "circle.elementalarcana.archmagister." + stage.id()
                        : "circle.elementalarcana.talk." + stage.id() + "." + CircleTalk.line(nameIndex < 0 ? getId() : nameIndex, talks++);
                say(player, Component.translatable(key));
                playSound(SoundEvents.VILLAGER_AMBIENT, 1f, archmagister ? 0.8f : 1.1f);
                if (archmagister) {
                    Commissions.offerOrRemind(this, serverPlayer, stage);
                }
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    // ---- the Circle's stores (the Archmagister's) ----

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            offers = level().isClientSide() || !archmagister ? new MerchantOffers() : CircleStores.today(this);
            stockedDay = level().getDayTime() / 24000L;
        }
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        ambientSoundTime = -getAmbientSoundInterval();
        playSound(getNotifyTradeSound(), 1f, 0.8f);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!level().isClientSide() && ambientSoundTime > -getAmbientSoundInterval() + 20) {
            ambientSoundTime = -getAmbientSoundInterval();
            playSound(stack.isEmpty() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_YES, 1f, 0.8f);
        }
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // The stores close with it.
        setTradingPlayer(null);
    }

    /** Stands still facing whoever's trading with it, while the stores are open. */
    private final class StandForTradeGoal extends Goal {
        StandForTradeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return tradingPlayer != null && tradingPlayer.isAlive() && tradingPlayer.containerMenu instanceof MerchantMenu
                    && distanceToSqr(tradingPlayer) < 64;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (tradingPlayer != null) {
                getLookControl().setLookAt(tradingPlayer, 30f, 30f);
            }
        }
    }

    /**
     * In a bout: footwork that keeps to the ring (a casting mage's usual footwork circles its foe
     * wherever that leads), to a spot a few steps from the ring's middle on the far side from the
     * foe, give or take, now and then.
     */
    private final class DuelFootworkGoal extends Goal {
        private int repath;

        DuelFootworkGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return duelRing != null && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity foe = getTarget();
            if (foe == null || duelRing == null) {
                return;
            }
            getLookControl().setLookAt(foe, 30f, 30f);
            if (--repath > 0) {
                return;
            }
            repath = 15 + getRandom().nextInt(15);
            Vec3 middle = Vec3.atBottomCenterOf(duelRing);
            Vec3 away = position().subtract(foe.position());
            double angle = Math.atan2(away.z, away.x) + (getRandom().nextDouble() - 0.5) * 1.6;
            double reach = 2.5 + getRandom().nextDouble() * 2;
            getNavigation().moveTo(middle.x + Math.cos(angle) * reach, middle.y, middle.z + Math.sin(angle) * reach, 0.9);
        }
    }

    /** A sigil's mage keeping near the one it serves (and leaving a fight that has drawn it too far). */
    private final class FollowCompanionGoal extends Goal {
        private int repath;

        FollowCompanionGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player player = companionPlayer();
            if (player == null) {
                return false;
            }
            double distance = distanceToSqr(player);
            return distance > FOLLOW_DISTANCE * FOLLOW_DISTANCE && (getTarget() == null || distance > GUARD_RADIUS * GUARD_RADIUS);
        }

        @Override
        public boolean canContinueToUse() {
            Player player = companionPlayer();
            return player != null && !getNavigation().isDone() && distanceToSqr(player) > (FOLLOW_DISTANCE - 3) * (FOLLOW_DISTANCE - 3);
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            Player player = companionPlayer();
            if (player == null) {
                return;
            }
            getLookControl().setLookAt(player, 10f, getMaxHeadXRot());
            if (--repath <= 0) {
                repath = 10;
                getNavigation().moveTo(player, 1.0);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("element", entityData.get(ELEMENT));
        if (home != null) {
            tag.put("home", NbtUtils.writeBlockPos(home));
        }
        tag.putInt("talks", talks);
        tag.putInt("name", nameIndex);
        if (companion != null) {
            tag.putUUID("companion", companion);
            tag.putLong("leave_at", leaveAt);
        }
        tag.putLong("last_duel_day", lastDuelDay);
        if (offers != null && !offers.isEmpty() && !level().isClientSide()) {
            tag.put("offers", MerchantOffers.CODEC.encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), offers).getOrThrow());
            tag.putLong("stocked_day", stockedDay);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ELEMENT, tag.getByte("element"));
        home = NbtUtils.readBlockPos(tag, "home").orElse(null);
        if (home != null) {
            restrictTo(home, archmagister ? 4 : HOME_RADIUS);
        }
        talks = tag.getInt("talks");
        nameIndex = tag.contains("name") ? tag.getInt("name") : -1;
        companion = tag.hasUUID("companion") ? tag.getUUID("companion") : null;
        leaveAt = tag.getLong("leave_at");
        lastDuelDay = tag.contains("last_duel_day") ? tag.getLong("last_duel_day") : -1;
        if (tag.contains("offers")) {
            MerchantOffers.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get("offers"))
                    .result().ifPresent(saved -> offers = saved);
            stockedDay = tag.getLong("stocked_day");
        }
    }
}
