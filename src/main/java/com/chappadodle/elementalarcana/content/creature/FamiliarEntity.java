package com.chappadodle.elementalarcana.content.creature;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.FamiliarRules;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.CreatureLevels;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.brew.ModBrews;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A wisp bound to a mage (see the Familiars spec and BindingCharmItem): it follows its owner at the
 * shoulder (teleporting to them when left far behind), fights what they fight with its element's
 * creature spells (CastMobSpellGoal: it's Attuned, at a rank that grows with its owner's level), and
 * rests or follows when told (sneak and right-click it). A Wisp Mote heals it. Never hostile, never
 * despawns; one per mage.
 */
public class FamiliarEntity extends TamableAnimal implements FlyingAnimal, ElementalOrb {
    private final Element element;

    public FamiliarEntity(EntityType<? extends FamiliarEntity> type, Level level, Element element) {
        super(type, level);
        this.element = element;
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    public Element element() {
        return element;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new CastMobSpellGoal(this));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0, FamiliarRules.FOLLOW_START, FamiliarRules.FOLLOW_STOP));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    /** What it may fight: what its owner's magic may hurt, never villagers or golems, never its owner's other pets. */
    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof AbstractVillager || target instanceof IronGolem) {
            return false;
        }
        if (target instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) {
            return false;
        }
        return SpellTargets.canAffect(owner, target);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 40 == 0) {
            growWithOwner();
        }
    }

    /**
     * Grows with its owner: it casts at the rank their magic level gives it (Attuned again when that
     * changes), and its level is theirs (its zone level set so that, with its rank's bonus, it matches).
     */
    void growWithOwner() {
        if (!(getOwner() instanceof Player owner)) {
            return;
        }
        int ownerLevel = MagicAttachments.get(owner).level();
        AttunementRank rank = FamiliarRules.rankFor(ownerLevel);
        CreatureMagic magic = Attunement.get(this);
        float share = getHealth() / getMaxHealth();
        if (magic == null || magic.rank() != rank || magic.element() != element) {
            Attunement.attune(this, element, rank);
        }
        int base = Math.max(1, ownerLevel - rank.bonusLevels());
        if (!hasData(MagicAttachments.CREATURE_LEVEL) || getData(MagicAttachments.CREATURE_LEVEL) != base) {
            CreatureLevels.setBaseLevel(this, base);
        }
        setHealth(getMaxHealth() * share);
    }

    /** It catches up with its owner by teleporting only when it's far behind. */
    @Override
    public boolean shouldTryTeleportToOwner() {
        LivingEntity owner = getOwner();
        return owner != null && distanceToSqr(owner) >= FamiliarRules.TELEPORT * FamiliarRules.TELEPORT;
    }

    /** Sneak and right-click with an empty hand: stay or follow. A Wisp Mote heals it. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (stack.is(ModBrews.WISP_MOTE.get()) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide()) {
                heal(getMaxHealth());
                stack.consume(1, player);
                ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY(0.8), getZ(), 3, 0.2, 0.2, 0.2, 0);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (stack.isEmpty() && player.isShiftKeyDown()) {
            if (!level().isClientSide()) {
                setOrderedToSit(!isOrderedToSit());
                setInSittingPose(isOrderedToSit());
                navigation.stop();
                setTarget(null);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f,
                        isOrderedToSit() ? 0.8f : 1.4f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Override
    public boolean isFlying() {
        return !onGround();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    /** It leaves a Wisp Mote and a little of its element when it dies. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModBrews.WISP_MOTE.get()));
        spawnAtLocation(new ItemStack(ModItems.essence(element)));
    }
}
