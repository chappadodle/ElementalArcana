package com.chappadodle.elementalarcana.content.circle;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CircleTalk;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.MageAlly;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.hollowed.HollowedEntity;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import com.chappadodle.elementalarcana.content.tower.KeepDistanceGoal;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * One of the Circle's mages (see the Circle spec, part 1), or their Archmagister: friendly, a
 * villager's look in white robes trimmed with its element's colour (the Archmagister's with gold).
 * It keeps near the Enclave's Spire, greets a player who uses it (a line of talk by the player's
 * story, CircleTalk), and fights any monster near the Spire and the Hollowed anywhere near, as an
 * Attuned caster of its element. It never turns on a player (nor their pets), even one who strikes
 * it. A player's magic spares it, and its magic touches only monsters (MageAlly, SpellTargets).
 */
public class CircleMageEntity extends PathfinderMob implements MageAlly {
    private static final EntityDataAccessor<Byte> ELEMENT = SynchedEntityData.defineId(CircleMageEntity.class, EntityDataSerializers.BYTE);
    private static final String[] NAMES = {"Ilsa", "Corvin", "Maren", "Tobin", "Elowen", "Fenn", "Rhosyn", "Aldric", "Wren", "Sabine",
            "Odo", "Brisa", "Halvard", "Nim", "Teodor", "Liesel"};
    /** How far from the Spire it keeps, and defends. */
    private static final int HOME_RADIUS = 20;
    private static final double DEFEND_RADIUS = 24;

    private final boolean archmagister;
    @Nullable
    private BlockPos home;
    private int talks;
    private int nameIndex = -1;

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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELEMENT, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CastMobSpellGoal(this));
        goalSelector.addGoal(2, new KeepDistanceGoal(this, 4, 9));
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

    private boolean nearHome(BlockPos pos) {
        return home == null || pos.closerThan(home, DEFEND_RADIUS);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        if (home == null) {
            // From an egg or a command: an element of its own.
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
    }

    /** Who's speaking: "Ilsa, a mage of the Circle (Fire)", or the Archmagister by name and title. */
    public Component speaker() {
        Component school = Component.translatable("school.elementalarcana." + element().name().toLowerCase(Locale.ROOT));
        String name = NAMES[Math.floorMod(nameIndex < 0 ? getId() : nameIndex, NAMES.length)];
        return archmagister ? Component.translatable("entity.elementalarcana.archmagister.named", name)
                : Component.translatable("entity.elementalarcana.circle_mage.named", name, school);
    }

    /** Used: it greets the player, a line by their story so far. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || getTarget() != null) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide()) {
            MagicData data = MagicAttachments.get(player);
            CircleTalk.Stage stage = CircleTalk.stage(data.isAwakened(), data.level());
            String key = archmagister ? "circle.elementalarcana.archmagister." + stage.id()
                    : "circle.elementalarcana.talk." + stage.id() + "." + CircleTalk.line(nameIndex < 0 ? getId() : nameIndex, talks++);
            player.sendSystemMessage(Component.translatable("message.elementalarcana.circle.says", speaker(), Component.translatable(key))
                    .withStyle(archmagister ? ChatFormatting.GOLD : ChatFormatting.WHITE));
            getLookControl().setLookAt(player, 30f, 30f);
            playSound(SoundEvents.VILLAGER_AMBIENT, 1f, archmagister ? 0.8f : 1.1f);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
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
    }
}
