package com.chappadodle.elementalarcana.content.cantrip;

import com.chappadodle.elementalarcana.api.CantripRules;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.network.ProspectPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Prospect (a cantrip): for eight seconds its caster sees the ores within 12 blocks through the
 * stone, each outlined in its colour (the client draws them: ProspectOutlines).
 */
public class ProspectSpell extends Spell {
    public ProspectSpell() {
        super(ModSchools.ARCANE, 20, 600);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer player = context.caster();
        ServerLevel level = context.level();
        BlockPos centre = player.blockPosition();
        int radius = CantripRules.PROSPECT_RADIUS;
        List<BlockPos> found = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Tags.Blocks.ORES)) {
                found.add(pos.immutable());
                colours.add(colourOf(state));
                if (found.size() >= CantripRules.PROSPECT_MAX_ORES) {
                    break;
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new ProspectPayload(found, colours, CantripRules.PROSPECT_TICKS));
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY(1.0), player.getZ(), 40, 1.2, 0.8, 1.2, 0.8);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.7f);
        player.displayClientMessage(Component.translatable("message.elementalarcana.cantrip.prospect", found.size()), true);
        return CastResult.SUCCESS;
    }

    /** An ore's outline colour: what it yields. */
    static int colourOf(BlockState state) {
        if (state.is(Tags.Blocks.ORES_DIAMOND)) {
            return 0x5EF0E6;
        }
        if (state.is(Tags.Blocks.ORES_EMERALD)) {
            return 0x30E060;
        }
        if (state.is(Tags.Blocks.ORES_GOLD)) {
            return 0xFCEE4B;
        }
        if (state.is(Tags.Blocks.ORES_IRON)) {
            return 0xE0B898;
        }
        if (state.is(Tags.Blocks.ORES_COPPER)) {
            return 0xE8823C;
        }
        if (state.is(Tags.Blocks.ORES_REDSTONE)) {
            return 0xFF3030;
        }
        if (state.is(Tags.Blocks.ORES_LAPIS)) {
            return 0x3A68FF;
        }
        if (state.is(Tags.Blocks.ORES_COAL)) {
            return 0x9A9A9A;
        }
        if (state.is(Tags.Blocks.ORES_NETHERITE_SCRAP)) {
            return 0xA0705A;
        }
        if (state.is(Tags.Blocks.ORES_QUARTZ)) {
            return 0xF4EEE2;
        }
        return 0xD8C8FF;
    }
}
