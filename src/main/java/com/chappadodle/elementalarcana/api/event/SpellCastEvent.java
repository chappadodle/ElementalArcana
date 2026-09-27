package com.chappadodle.elementalarcana.api.event;

import com.chappadodle.elementalarcana.api.Spell;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Fired on {@code NeoForge.EVENT_BUS}, server-side, around every cast. */
public abstract class SpellCastEvent extends Event {
    private final ServerPlayer caster;
    private final Spell spell;

    protected SpellCastEvent(ServerPlayer caster, Spell spell) {
        this.caster = caster;
        this.spell = spell;
    }

    public ServerPlayer caster() {
        return caster;
    }

    public Spell spell() {
        return spell;
    }

    /** Before the spell runs. Cancel to block the cast, or change the mana it will cost. */
    public static class Pre extends SpellCastEvent implements ICancellableEvent {
        private int manaCost;

        public Pre(ServerPlayer caster, Spell spell, int manaCost) {
            super(caster, spell);
            this.manaCost = manaCost;
        }

        public int manaCost() {
            return manaCost;
        }

        public void setManaCost(int manaCost) {
            this.manaCost = Math.max(0, manaCost);
        }
    }

    /** After a successful cast, once mana and cooldown have been applied. */
    public static class Post extends SpellCastEvent {
        public Post(ServerPlayer caster, Spell spell) {
            super(caster, spell);
        }
    }
}
