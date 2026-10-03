package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Fire's level-3 spell: places an Ember Sprite where the caster looks, up to 20 blocks away (just
 * off the wall or ground it lands on), which spits fire bolts at the nearest hostile creature for 8
 * seconds (see EmberSprite). A new one sends the old one off.
 */
public class EmberSpriteSpell extends Spell {
    private static final double RANGE = 20;

    public EmberSpriteSpell() {
        super(ModSchools.FIRE, 25, 300, 3);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        BlockHitResult hit = context.level().clip(new ClipContext(eye, eye.add(look.scale(RANGE)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, caster));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(RANGE))
                : hit.getLocation().subtract(look.scale(0.8)).add(0, 1.0, 0);
        EmberSprite.place(context.level(), caster, at, context.power());
        return CastResult.SUCCESS;
    }
}
