package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SkillTree;
import com.chappadodle.elementalarcana.api.SkillTrees;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Map;

/** Server -> client: the whole skill tree (paths already expanded), sent on join and after /reload. */
public record SkillTreeSyncPayload(List<SkillTree.Node> nodes, List<List<String>> links) implements CustomPacketPayload {
    public static final Type<SkillTreeSyncPayload> TYPE = new Type<>(ElementalArcana.id("skill_tree"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillTreeSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeCollection(payload.nodes(), SkillTreeSyncPayload::writeNode);
                buf.writeCollection(payload.links(), (out, pair) -> {
                    out.writeUtf(pair.get(0));
                    out.writeUtf(pair.get(1));
                });
            },
            buf -> new SkillTreeSyncPayload(buf.readList(SkillTreeSyncPayload::readNode),
                    buf.readList(in -> List.of(in.readUtf(), in.readUtf()))));

    public static SkillTreeSyncPayload of(SkillTree tree) {
        return new SkillTreeSyncPayload(List.copyOf(tree.nodes()), tree.linkPairs());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillTreeSyncPayload payload, IPayloadContext context) {
        SkillTrees.set(new SkillTree(payload.nodes(), payload.links()));
    }

    private static void writeNode(FriendlyByteBuf buf, SkillTree.Node node) {
        buf.writeUtf(node.id());
        buf.writeEnum(node.type());
        buf.writeVarInt(node.x());
        buf.writeVarInt(node.y());
        buf.writeNullable(node.element(), FriendlyByteBuf::writeUtf);
        buf.writeNullable(node.stat(), FriendlyByteBuf::writeUtf);
        buf.writeVarInt(node.amount());
        buf.writeNullable(node.spell(), FriendlyByteBuf::writeUtf);
        buf.writeVarInt(node.spellLevel());
        buf.writeNullable(node.branch(), FriendlyByteBuf::writeUtf);
        buf.writeNullable(node.requiresStat(), FriendlyByteBuf::writeUtf);
        buf.writeVarInt(node.requiresMin());
        buf.writeMap(node.stats(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
        buf.writeNullable(node.keystone(), FriendlyByteBuf::writeUtf);
    }

    private static SkillTree.Node readNode(FriendlyByteBuf buf) {
        return new SkillTree.Node(buf.readUtf(), buf.readEnum(SkillTree.Type.class), buf.readVarInt(), buf.readVarInt(),
                buf.readNullable(FriendlyByteBuf::readUtf), buf.readNullable(FriendlyByteBuf::readUtf), buf.readVarInt(),
                buf.readNullable(FriendlyByteBuf::readUtf), buf.readVarInt(), buf.readNullable(FriendlyByteBuf::readUtf),
                buf.readNullable(FriendlyByteBuf::readUtf), buf.readVarInt(),
                Map.copyOf(buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt)), buf.readNullable(FriendlyByteBuf::readUtf));
    }
}
