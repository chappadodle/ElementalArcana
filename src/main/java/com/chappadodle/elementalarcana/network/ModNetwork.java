package com.chappadodle.elementalarcana.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("10")
                .playToServer(CastSpellPayload.TYPE, CastSpellPayload.STREAM_CODEC, CastSpellPayload::handle)
                .playToServer(SelectSpellPayload.TYPE, SelectSpellPayload.STREAM_CODEC, SelectSpellPayload::handle)
                .playToServer(DevActionPayload.TYPE, DevActionPayload.STREAM_CODEC, DevActionPayload::handle)
                .playToServer(EssencePayload.TYPE, EssencePayload.STREAM_CODEC, EssencePayload::handle)
                .playToServer(StatPayload.TYPE, StatPayload.STREAM_CODEC, StatPayload::handle)
                .playToServer(TreePayload.TYPE, TreePayload.STREAM_CODEC, TreePayload::handle)
                .playToServer(AuraPayload.TYPE, AuraPayload.STREAM_CODEC, AuraPayload::handle)
                .playToServer(GlidePayload.TYPE, GlidePayload.STREAM_CODEC, GlidePayload::handle)
                .playToServer(RelicJumpPayload.TYPE, RelicJumpPayload.STREAM_CODEC, RelicJumpPayload::handle)
                .playToServer(MentorClaimPayload.TYPE, MentorClaimPayload.STREAM_CODEC, MentorClaimPayload::handle)
                .playToClient(ManaTidePayload.TYPE, ManaTidePayload.STREAM_CODEC, ManaTidePayload::handle)
                .playToClient(SkillTreeSyncPayload.TYPE, SkillTreeSyncPayload.STREAM_CODEC, SkillTreeSyncPayload::handle)
                .playToClient(ProspectPayload.TYPE, ProspectPayload.STREAM_CODEC, ProspectPayload::handle)
                .playToClient(StarStreakPayload.TYPE, StarStreakPayload.STREAM_CODEC, StarStreakPayload::handle)
                .playToClient(StarPillarsPayload.TYPE, StarPillarsPayload.STREAM_CODEC, StarPillarsPayload::handle)
                .playToClient(MentorPayload.TYPE, MentorPayload.STREAM_CODEC, MentorPayload::handle);
    }
}
