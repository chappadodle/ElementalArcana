package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.BubblePrisons;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Water, Magic Level 3: traps the creature under your crosshair (up to 20 blocks) in a floating
 * water bubble for 4 seconds. It hangs there helpless and Wet; any hit from a player pops it for
 * bonus damage (see BubblePrisons). No target: the cast fails and costs nothing.
 */
public class BubblePrisonSpell extends Spell {
    /** The bubble's 3D model (drawn by BubbleRenderer). */
    public static final ResourceLocation BUBBLE_MODEL = ElementalArcana.id("spell/bubble");
    private static final double RANGE = 20;
    // A little leeway around creatures, so small ones are easy to target.
    private static final double AIM_LEEWAY = 0.5;

    public BubblePrisonSpell() {
        super(ModSchools.WATER, 30, 240, 3);
    }

    @Override
    public CastResult cast(CastContext context) {
        LivingEntity target = targetUnderCrosshair(context.caster());
        if (target == null) {
            return CastResult.fail(Component.translatable("message.elementalarcana.bubble.no_target"));
        }
        BubblePrisons.trap(target);
        ServerLevel level = context.level();
        Vec3 from = context.eyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        // A trail of drops from the caster's hand to the target.
        for (double d = 1; d < from.distanceTo(to); d += 0.6) {
            Vec3 at = from.add(to.subtract(from).normalize().scale(d));
            level.sendParticles(ModContent.HYDRO_DROP.get(), at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
        return CastResult.SUCCESS;
    }

    /** The nearest creature the caster is looking at, in sight and trappable, or null. */
    @Nullable
    private static LivingEntity targetUnderCrosshair(ServerPlayer caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(RANGE));
        BlockHitResult wall = caster.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : caster.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(AIM_LEEWAY + 1),
                e -> e != caster && !e.isSpectator() && BubblePrisons.canTrap(e))) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(AIM_LEEWAY).clip(eye, end);
            if (hit.isPresent() && hit.get().distanceToSqr(eye) < bestDistance) {
                best = entity;
                bestDistance = hit.get().distanceToSqr(eye);
            }
        }
        return best;
    }
}
