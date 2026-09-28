package com.chappadodle.elementalarcana.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("3")
                .playToServer(CastSpellPayload.TYPE, CastSpellPayload.STREAM_CODEC, CastSpellPayload::handle)
                .playToServer(SelectSpellPayload.TYPE, SelectSpellPayload.STREAM_CODEC, SelectSpellPayload::handle)
                .playToServer(AwakenPayload.TYPE, AwakenPayload.STREAM_CODEC, AwakenPayload::handle)
                .playToServer(DevActionPayload.TYPE, DevActionPayload.STREAM_CODEC, DevActionPayload::handle)
                .playToServer(SpellProgressPayload.TYPE, SpellProgressPayload.STREAM_CODEC, SpellProgressPayload::handle)
                .playToServer(EssencePayload.TYPE, EssencePayload.STREAM_CODEC, EssencePayload::handle);
    }
}
