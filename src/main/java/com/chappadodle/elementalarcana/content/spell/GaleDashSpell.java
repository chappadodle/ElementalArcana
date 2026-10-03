package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.GaleDashRules;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Wind's Gale Dash: a burst of wind that throws the caster the way they look, shoving away what's
 * near where they started; their landing is safe. It holds several dashes (charges) and levels to
 * 10 (GaleDashRules has the numbers; GaleDashes does the work):
 * <pre>
 * Lv1 Gale Dash         the dash                    Lv6  Tailwind        speed after, a quarter off the cooldown
 * Lv2 Second Wind       2 charges                   Lv7  Triple Charge   3 charges
 * Lv3 Air Step          any way mid-air, unharmed   Lv8  Featherfall     a slow drift down after
 * Lv4 Slipstream Trail  shoves foes, quickens allies Lv9 Swirling Rush   Swirls what it passes
 * Lv5 Phantom Step | Gale Strike                    Lv10 Blink Storm | Hurricane Rush
 * </pre>
 */
public class GaleDashSpell extends Spell {

    public GaleDashSpell() {
        super(ModSchools.WIND, 20, 60);
    }

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(GaleDashRules.PHANTOM_STEP, GaleDashRules.GALE_STRIKE);
            case 10 -> List.of(GaleDashRules.BLINK_STORM, GaleDashRules.HURRICANE_RUSH);
            default -> List.of();
        };
    }

    @Override
    public int charges(int spellLevel) {
        return GaleDashRules.charges(spellLevel);
    }

    @Override
    public int cooldownTicks(int spellLevel, float cooldownFactor) {
        return Math.round(super.cooldownTicks(spellLevel, cooldownFactor) * GaleDashRules.cooldownFactor(spellLevel));
    }

    @Override
    public CastResult cast(CastContext context) {
        if (context.spellLevel() >= 10 && GaleDashRules.HURRICANE_RUSH.equals(context.branch(10))) {
            context.holdUntilRelease(GaleDashes.rush(context));
            return CastResult.SUCCESS;
        }
        if (context.spellLevel() >= 10 && GaleDashRules.BLINK_STORM.equals(context.branch(10))) {
            return GaleDashes.blink(context) ? CastResult.SUCCESS
                    : CastResult.fail(Component.translatable("message.elementalarcana.gale_dash.no_room"));
        }
        GaleDashes.dash(context);
        return CastResult.SUCCESS;
    }
}
