package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.content.Attunement;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An Attuned creature's spellcasting. When it has a target in sight, it picks the highest-rank
 * spell that is ready, stops, faces the target and winds up for half a second (hands glowing
 * with its element, a warning sound), then casts. Between casts it fights normally. Bosses (the
 * Sovereigns) override the spell list, the wind-up, the gap and the cooldowns.
 */
public class CastMobSpellGoal extends Goal {
    private static final int WINDUP_TICKS = 10;
    // At least this long between any two casts.
    private static final int MIN_GAP_TICKS = 40;

    protected final Mob mob;
    private final Map<MobSpell, Long> readyAt = new HashMap<>();
    private long nextCastAt;
    @Nullable
    private MobSpell casting;
    @Nullable
    private LivingEntity target;
    private int windup;

    public CastMobSpellGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        CreatureMagic magic = Attunement.get(mob);
        LivingEntity target = mob.getTarget();
        long now = mob.level().getGameTime();
        if (magic == null || target == null || !target.isAlive() || now < nextCastAt || !mob.getSensing().hasLineOfSight(target)) {
            return false;
        }
        casting = pick(magic, target, now);
        this.target = target;
        return casting != null;
    }

    /** The spells this creature may cast, lowest rank first (the last ready one that fits wins). */
    protected List<MobSpell> spells(CreatureMagic magic) {
        return MobSpells.of(magic.element());
    }

    protected int windupTicks() {
        return WINDUP_TICKS;
    }

    protected int minGapTicks() {
        return MIN_GAP_TICKS;
    }

    protected int cooldownOf(MobSpell spell) {
        return spell.cooldownTicks();
    }

    /** The highest-rank spell this creature knows that is off cooldown and makes sense right now. */
    @Nullable
    private MobSpell pick(CreatureMagic magic, LivingEntity target, long now) {
        List<MobSpell> spells = spells(magic);
        for (int i = spells.size() - 1; i >= 0; i--) {
            MobSpell spell = spells.get(i);
            if (spell.rank().ordinal() <= magic.rank().ordinal() && now >= readyAt.getOrDefault(spell, 0L) && spell.canCast(mob, target)) {
                return spell;
            }
        }
        return null;
    }

    @Override
    public void start() {
        windup = windupTicks();
        mob.getNavigation().stop();
        MobCasting.play(mob, SoundEvents.EVOKER_PREPARE_ATTACK, 1f, 1.2f);
    }

    @Override
    public boolean canContinueToUse() {
        return windup > 0 && casting != null && target != null && target.isAlive() && Attunement.get(mob) != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        CreatureMagic magic = Attunement.get(mob);
        if (casting == null || target == null || magic == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30f, 30f);
        MobCasting.windup(mob, magic.element());
        if (--windup == 0) {
            casting.cast(mob, target);
            long now = mob.level().getGameTime();
            readyAt.put(casting, now + cooldownOf(casting));
            nextCastAt = now + minGapTicks();
        }
    }

    @Override
    public void stop() {
        casting = null;
        target = null;
        windup = 0;
    }
}
