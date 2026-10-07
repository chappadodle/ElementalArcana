package com.chappadodle.elementalarcana.content.mentor;

import com.mojang.datafixers.util.Either;
import com.chappadodle.elementalarcana.api.MentorChapters;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Voice in the Stone (docs/superpowers/specs/2026-10-05-voice-in-the-stone-design.md): the
 * Sending Stone Caelith speaks through, and how far each player is in their tale.
 */
public final class ModMentor {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ElementalArcana.MODID);

    /** A player's place in the tale (the chapter they're on), and whether they've been given a stone. */
    public record Tale(int chapter, boolean stoneGiven) {
        public static final Tale START = new Tale(0, false);
        /** A place in the tale, saved by its chapter's id (0.9 and 0.10 saved a number: read through their order). */
        private static final Codec<Integer> PLACE = Codec.either(Codec.INT, Codec.STRING).xmap(
                saved -> saved.map(MentorChapters::placeOfLegacy, MentorChapters::placeOf),
                place -> Either.right(MentorChapters.idOf(place)));
        public static final Codec<Tale> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PLACE.fieldOf("chapter").forGetter(Tale::chapter),
                Codec.BOOL.fieldOf("stone_given").forGetter(Tale::stoneGiven)
        ).apply(instance, Tale::new));
    }

    public static final DeferredItem<SendingStoneItem> SENDING_STONE = ITEMS.register("sending_stone",
            () -> new SendingStoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final Supplier<AttachmentType<Tale>> TALE = ATTACHMENT_TYPES.register("mentor_tale",
            () -> AttachmentType.builder(() -> Tale.START).serialize(Tale.CODEC).copyOnDeath().build());

    private ModMentor() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
