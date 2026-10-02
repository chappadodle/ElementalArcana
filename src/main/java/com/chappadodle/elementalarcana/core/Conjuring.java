package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.ConjureSpell;
import com.chappadodle.elementalarcana.api.Keystones;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.api.event.SpellCastEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Conjuring (see ConjureSpell): each press of the cast key conjures one more projectile, up to the
 * spell level's maximum; they hover and grow, costing 1 mana per second each; "launch one" throws
 * the oldest, "launch all" throws the rest. When upkeep can't be paid, everything launches itself.
 * Holding the key for a second with a full, grown set fuses it (capstones). The cooldown starts
 * when the last projectile leaves. Each projectile keeps its place in the full formation for as
 * long as it's held, so nothing shifts when you add or throw one. Server-side, one set per player.
 */
public final class Conjuring {
    private static final int UPKEEP_INTERVAL_TICKS = 20;
    private static final int UPKEEP_PER_PROJECTILE = 1;
    private static final int FUSE_HOLD_TICKS = 20;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        private final Spell spell;
        private final int seed;
        private final List<SpellProjectile> held = new ArrayList<>();
        private final Set<Integer> grown = new HashSet<>();
        private long nextUpkeep;
        private boolean keyHeld;
        private long fuseStart = -1;
        private boolean fused;

        private Session(Spell spell, int seed) {
            this.spell = spell;
            this.seed = seed;
        }

        private ConjureSpell conjurer() {
            return (ConjureSpell) spell;
        }
    }

    private Conjuring() {
    }

    public static boolean isConjuring(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** The cast key went down with a conjurable spell selected (casting checks already passed). */
    static void press(ServerPlayer player) {
        MagicData data = MagicAttachments.get(player);
        Spell spell = data.selectedSpell();
        if (!(spell instanceof ConjureSpell conjurer)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session != null && session.spell != spell) {
            launchAll(player);
            session = null;
        }
        if (session == null) {
            session = new Session(spell, player.getRandom().nextInt());
        }
        session.keyHeld = true;
        int level = data.spellLevel(spell);
        if (session.fused || session.held.size() >= conjurer.maxConjured(level)) {
            // A full set: holding the key now may fuse it (see tick).
            return;
        }

        SpellCastEvent.Pre pre = NeoForge.EVENT_BUS.post(new SpellCastEvent.Pre(player, spell, conjurer.conjureCost(level, session.held.size())));
        if (pre.isCanceled() || !CastingService.canAfford(player, data, pre.manaCost())) {
            return;
        }
        CastContext context = new CastContext(player, player.serverLevel(), InteractionHand.MAIN_HAND, data.spellPower(spell),
                level, data.branches(spell));
        boolean first = session.held.isEmpty();
        SpellProjectile projectile = conjurer.conjure(context, session.seed);
        projectile.setFormation(freeSlot(session), conjurer.maxConjured(level));
        session.held.add(projectile);
        SESSIONS.put(player.getUUID(), session);
        if (first) {
            session.nextUpkeep = player.level().getGameTime() + UPKEEP_INTERVAL_TICKS;
        }
        CastingService.pay(player, data, spell, pre.manaCost());
        CastingService.castFeedback(player, spell);
        data.setConjured(session.held.size());
        MagicAttachments.sync(player);
        NeoForge.EVENT_BUS.post(new SpellCastEvent.Post(player, spell));
    }

    /** The cast key came back up: stop any fusing in progress. */
    static void releaseKey(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null) {
            session.keyHeld = false;
            session.fuseStart = -1;
        }
    }

    /** Throws the oldest held projectile at the crosshair. */
    public static void launchOne(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.held.isEmpty()) {
            return;
        }
        SpellProjectile first = session.held.remove(0);
        session.conjurer().launch(player, List.of(first), SpellProjectile.crosshairTarget(player));
        if (session.held.isEmpty()) {
            end(player, session);
        } else {
            updateCount(player, session);
        }
    }

    /** Throws everything held at the crosshair. */
    public static void launchAll(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        List<SpellProjectile> all = List.copyOf(session.held);
        session.held.clear();
        if (!all.isEmpty()) {
            session.conjurer().launch(player, all, SpellProjectile.crosshairTarget(player));
        }
        end(player, session);
    }

    /** Switching spells throws whatever is held. */
    public static void onSpellSwitched(ServerPlayer player) {
        launchAll(player);
    }

    /** Death, logout or a dimension change: the held projectiles fizzle, and the cooldown still starts. */
    static void cancel(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        for (SpellProjectile projectile : session.held) {
            if (projectile.isAlive()) {
                session.conjurer().fizzle(projectile);
            }
        }
        session.held.clear();
        end(player, session);
    }

    /** Every server tick for a conjuring player: growth chimes, upkeep, fusion. */
    static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (session.held.removeIf(projectile -> !projectile.isAlive())) {
            if (session.held.isEmpty()) {
                end(player, session);
                return;
            }
            updateCount(player, session);
        }
        MagicData data = MagicAttachments.get(player);
        long now = player.level().getGameTime();

        for (SpellProjectile projectile : session.held) {
            if (projectile.charge(0f) >= 1f && session.grown.add(projectile.getId())) {
                session.conjurer().onFullyGrown(projectile);
            }
        }

        if (now >= session.nextUpkeep) {
            session.nextUpkeep = now + UPKEEP_INTERVAL_TICKS;
            if (!CastingService.isFree(player, data)) {
                int upkeep = UPKEEP_PER_PROJECTILE * session.held.size();
                boolean blood = data.hasKeystone(Keystones.BLOOD_MAGIC);
                if (blood ? player.getHealth() - CastingService.bloodPrice(upkeep) < 1f : data.mana() < upkeep) {
                    // Can't hold them any longer: they fly.
                    launchAll(player);
                    return;
                }
                if (blood) {
                    CastingService.bleed(player, CastingService.bloodPrice(upkeep));
                } else {
                    data.setMana(data.mana() - upkeep);
                }
                data.interruptMeditation();
                MagicAttachments.sync(player);
            }
        }

        int level = data.spellLevel(session.spell);
        boolean fullSet = session.held.size() >= session.conjurer().maxConjured(level);
        boolean allGrown = session.held.stream().allMatch(projectile -> projectile.charge(0f) >= 1f);
        if (session.keyHeld && !session.fused && fullSet && allGrown && session.held.size() > 1
                && session.conjurer().canFuse(level, data.branches(session.spell))) {
            if (session.fuseStart < 0) {
                session.fuseStart = now;
            } else if (now - session.fuseStart >= FUSE_HOLD_TICKS) {
                SpellProjectile fused = session.conjurer().fuse(player, List.copyOf(session.held));
                session.held.clear();
                session.held.add(fused);
                session.fused = true;
                updateCount(player, session);
            }
        } else {
            session.fuseStart = -1;
        }
    }

    /** The first place in the full formation that no held projectile is using. */
    private static int freeSlot(Session session) {
        int slot = 0;
        while (true) {
            int candidate = slot;
            if (session.held.stream().noneMatch(projectile -> projectile.formationSlot() == candidate)) {
                return slot;
            }
            slot++;
        }
    }

    private static void updateCount(ServerPlayer player, Session session) {
        MagicData data = MagicAttachments.get(player);
        data.setConjured(session.held.size());
        MagicAttachments.sync(player);
    }

    /** Nothing left in hand: the cooldown starts. */
    private static void end(ServerPlayer player, Session session) {
        SESSIONS.remove(player.getUUID());
        MagicData data = MagicAttachments.get(player);
        if (!CastingService.isFree(player, data)) {
            Spell spell = session.spell;
            data.startCooldown(spell.id(), player.level().getGameTime(), spell.cooldownTicks(data.spellLevel(spell), data.cooldownFactor()));
        }
        data.setConjured(0);
        MagicAttachments.sync(player);
    }
}
