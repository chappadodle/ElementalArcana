package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.content.tower.KeepDistanceGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A Hollowed Acolyte (see the Hollowed spec): keeps 5 to 10 blocks off and throws a Hunger Bolt
 * every three seconds or so.
 */
public class HollowedAcolyte extends HollowedEntity {

    public HollowedAcolyte(EntityType<? extends HollowedAcolyte> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        registerCommonGoals();
        goalSelector.addGoal(1, new HungerBoltGoal(this, 60, 1));
        goalSelector.addGoal(2, new KeepDistanceGoal(this, 5, 10));
    }

    @Override
    public IllagerArmPose getArmPose() {
        return isCasting() ? IllagerArmPose.SPELLCASTING : IllagerArmPose.CROSSED;
    }
}
