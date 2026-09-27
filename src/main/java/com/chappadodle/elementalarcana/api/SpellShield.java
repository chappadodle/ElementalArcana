package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A magic shield on a player: how much damage it can still absorb, when it runs out, and which
 * {@link ShieldSpell} raised it (with that caster's spell level and branches, so allies shielded
 * by someone else behave like the caster's shield). Synced to everyone who can see the player,
 * so its visuals show for others too. Not saved: a shield doesn't survive logging out.
 */
public final class SpellShield {
    public static final StreamCodec<RegistryFriendlyByteBuf, SpellShield> STREAM_CODEC = StreamCodec.of(
            (buf, shield) -> {
                buf.writeNullable(shield.spell, FriendlyByteBuf::writeResourceLocation);
                buf.writeFloat(shield.amount);
                buf.writeFloat(shield.max);
                buf.writeVarLong(shield.raisedAt);
                buf.writeVarLong(shield.endsAt);
                buf.writeVarInt(shield.level);
                buf.writeMap(shield.branches, (out, level) -> out.writeVarInt(level), (out, branch) -> out.writeUtf(branch));
            },
            buf -> {
                SpellShield shield = new SpellShield();
                shield.spell = buf.readNullable(FriendlyByteBuf::readResourceLocation);
                shield.amount = buf.readFloat();
                shield.max = buf.readFloat();
                shield.raisedAt = buf.readVarLong();
                shield.endsAt = buf.readVarLong();
                shield.level = buf.readVarInt();
                shield.branches.putAll(buf.readMap(in -> in.readVarInt(), in -> in.readUtf()));
                return shield;
            });

    @Nullable
    private ResourceLocation spell;
    private float amount;
    private float max;
    private long raisedAt;
    private long endsAt;
    private int level;
    private final Map<Integer, String> branches = new HashMap<>();
    // Server-only scratch space for shield spells, e.g. counting hits per attacker.
    private final Map<Integer, Integer> hitsByAttacker = new HashMap<>();

    public static SpellShield of(Player player) {
        return player.getData(MagicAttachments.SHIELD);
    }

    /**
     * Raises (or replaces) {@code player}'s shield. The previous shield, if any, ends first
     * without breaking.
     */
    public static void raise(ServerPlayer player, Spell spell, float amount, int durationTicks, int spellLevel, Map<Integer, String> branches) {
        SpellShield shield = of(player);
        if (shield.isActive()) {
            end(player, false);
        }
        long now = player.level().getGameTime();
        shield.spell = spell.id();
        shield.amount = amount;
        shield.max = amount;
        shield.raisedAt = now;
        shield.endsAt = now + durationTicks;
        shield.level = spellLevel;
        shield.branches.clear();
        shield.branches.putAll(branches);
        shield.hitsByAttacker.clear();
        sync(player);
        ShieldSpell shieldSpell = shield.shieldSpell();
        if (shieldSpell != null) {
            shieldSpell.onShieldRaised(player, shield);
        }
    }

    /** Ends {@code player}'s shield; {@code broken} = destroyed by damage rather than expiring or being replaced. */
    public static void end(ServerPlayer player, boolean broken) {
        SpellShield shield = of(player);
        if (!shield.isActive()) {
            return;
        }
        ShieldSpell shieldSpell = shield.shieldSpell();
        SpellShield ended = shield.copy();
        shield.spell = null;
        shield.amount = 0;
        sync(player);
        if (shieldSpell != null) {
            shieldSpell.onShieldEnd(player, ended, broken);
        }
    }

    public static void sync(ServerPlayer player) {
        player.syncData(MagicAttachments.SHIELD);
    }

    public SpellShield copy() {
        SpellShield copy = new SpellShield();
        copy.spell = spell;
        copy.amount = amount;
        copy.max = max;
        copy.raisedAt = raisedAt;
        copy.endsAt = endsAt;
        copy.level = level;
        copy.branches.putAll(branches);
        return copy;
    }

    public boolean isActive() {
        return spell != null && amount > 0;
    }

    @Nullable
    public ShieldSpell shieldSpell() {
        return spell != null && SpellRegistries.SPELLS.get(spell) instanceof ShieldSpell shieldSpell ? shieldSpell : null;
    }

    public float amount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = Mth.clamp(amount, 0, max);
    }

    public float max() {
        return max;
    }

    /** 0..1 of the shield's strength left. */
    public float fraction() {
        return max <= 0 ? 0 : amount / max;
    }

    public long raisedAt() {
        return raisedAt;
    }

    public long endsAt() {
        return endsAt;
    }

    /** The shielding caster's level in the spell that raised it. */
    public int level() {
        return level;
    }

    public boolean hasBranch(int level, String branch) {
        return branch.equals(branches.get(level));
    }

    public Map<Integer, Integer> hitsByAttacker() {
        return hitsByAttacker;
    }
}
