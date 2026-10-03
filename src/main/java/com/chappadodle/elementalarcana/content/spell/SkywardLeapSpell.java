package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.SkywardRules;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Wind's level-5 spell, Skyward Leap (registered as updraft, the throw upward it grew out of, so
 * those who learned that have this now): a gust throws the caster up, and whatever they may hurt
 * nearby away and up (Airborne); then they're Skyward, and may glide while it lasts (see
 * SkywardLeaps; client/Gliding steers the glide). It levels to 10 (SkywardRules has the numbers):
 * <pre>
 * Lv1 Skyward Leap    the leap and the glide          Lv6  Lift              the gust reaches 6, throws higher
 * Lv2 Higher Ground   higher, 12 s, a slower sink     Lv7  Tailwind Glide    a faster glide, every landing safe
 * Lv3 Rising Current  R while gliding: an updraft     Lv8  Aerial Barrage    wind spells 30% cheaper in the air
 * Lv4 Plunge          sneak to dive, a shockwave      Lv9  Swirling Plunge   the shockwave Swirls
 * Lv5 Skyfall | Wind Rider                            Lv10 Heaven's Descent | Endless Sky
 * </pre>
 */
public class SkywardLeapSpell extends Spell {

    public SkywardLeapSpell() {
        super(ModSchools.WIND, 30, 160, 5);
    }

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(SkywardRules.SKYFALL, SkywardRules.WIND_RIDER);
            case 10 -> List.of(SkywardRules.HEAVENS_DESCENT, SkywardRules.ENDLESS_SKY);
            default -> List.of();
        };
    }

    /** Rising Current: while gliding, pressing cast catches an updraft instead (free, once a leap). */
    @Override
    public boolean hasFreeUse(ServerPlayer player) {
        return SkywardLeaps.hasUpdraft(player);
    }

    @Override
    public CastResult cast(CastContext context) {
        if (SkywardLeaps.hasUpdraft(context.caster())) {
            SkywardLeaps.risingCurrent(context.caster());
        } else {
            SkywardLeaps.leap(context);
        }
        return CastResult.SUCCESS;
    }
}
