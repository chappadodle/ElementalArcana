package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.HydroStreamOptions;
import com.chappadodle.elementalarcana.content.spell.HydroJetSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A Water Adept's Hydro Jet: a one-second stream from the mob at its target, hitting the first
 * creature on the line five times (pushing it back and soaking it); it streams through those the
 * mob's magic spares (SpellTargets.spares: for the Circle's mages, players and one another).
 * Server-side, never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class MobJet {
    private static final List<MobJet> ACTIVE = new ArrayList<>();
    private static final int DURATION_TICKS = 20;
    private static final int HIT_INTERVAL = 4;
    private static final double RANGE = 12;
    private static final float HIT_DAMAGE = 1f;

    private final Mob caster;
    private final LivingEntity target;
    private final float power;
    private int age;

    private MobJet(Mob caster, LivingEntity target, float power) {
        this.caster = caster;
        this.target = target;
        this.power = power;
    }

    public static void start(Mob caster, LivingEntity target, float power) {
        ACTIVE.add(new MobJet(caster, target, power));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ACTIVE.removeIf(jet -> !jet.tick());
    }

    /** One tick of spraying; false when it's over. */
    private boolean tick() {
        if (!caster.isAlive() || age++ >= DURATION_TICKS) {
            return false;
        }
        ServerLevel level = (ServerLevel) caster.level();
        caster.getLookControl().setLookAt(target, 30f, 30f);
        Vec3 origin = caster.getEyePosition().add(0, -0.3, 0);
        Vec3 aim = target.isAlive() ? MobCasting.aimPoint(target) : origin.add(caster.getLookAngle().scale(RANGE));
        Vec3 direction = aim.subtract(origin).normalize();
        Vec3 end = origin.add(direction.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, caster, origin, stop, new AABB(origin, stop).inflate(1),
                entity -> entity instanceof LivingEntity && entity != caster && entity.isAlive() && !SpellTargets.spares(caster, entity), 0.3f);
        if (hit != null) {
            stop = hit.getEntity().getBoundingBox().inflate(0.3).clip(origin, stop).orElse(hit.getLocation());
        }
        Vec3 line = stop.subtract(origin);
        level.sendParticles(new HydroStreamOptions(caster.getId(), HydroJetSpell.LOOK_SPRING, 0f), origin.x, origin.y, origin.z, 0, line.x, line.y, line.z, 1.0);

        if (hit != null && hit.getEntity() instanceof LivingEntity victim && age % HIT_INTERVAL == 0) {
            float damage = HIT_DAMAGE * power * ElementalReactions.waterHit(victim, 100);
            Vec3 motion = victim.getDeltaMovement();
            SpellDamage.hurtMultiHit(victim, SpellDamage.source(level, Element.WATER, caster, caster), damage);
            victim.setDeltaMovement(motion.add(direction.x * 0.12, 0.02, direction.z * 0.12));
            victim.hurtMarked = true;
        }
        return true;
    }
}
