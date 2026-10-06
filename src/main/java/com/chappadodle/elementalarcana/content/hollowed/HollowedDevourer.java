package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.api.HollowedRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A Hollowed Devourer (see the Hollowed spec): a head taller than the rest, it rushes in, and each
 * hit of its claws eats mana and heals it.
 */
public class HollowedDevourer extends HollowedEntity {

    public HollowedDevourer(EntityType<? extends HollowedDevourer> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.SCALE, 1.15);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        registerCommonGoals();
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && level() instanceof ServerLevel) {
            eat(living, HollowedRules.CLAW_MANA, HollowedRules.CLAW_HEAL);
        }
        return hit;
    }

    @Override
    public IllagerArmPose getArmPose() {
        return isAggressive() ? IllagerArmPose.ATTACKING : IllagerArmPose.CROSSED;
    }
}
