package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Which element a creature has. Innate creatures are listed in the entity-type tags
 * {@code elementalarcana:innate/<element>} and always have that element; Attuned creatures carry
 * theirs in {@link CreatureMagic}. Players never have one here. The game never shows a creature's
 * element; it only changes how spells hit it.
 */
public final class CreatureElements {
    private static final Map<Element, TagKey<EntityType<?>>> INNATE = new EnumMap<>(Element.class);

    static {
        for (Element element : Element.values()) {
            INNATE.put(element, TagKey.create(Registries.ENTITY_TYPE,
                    ElementalArcana.id("innate/" + element.name().toLowerCase(Locale.ROOT))));
        }
    }

    private CreatureElements() {
    }

    /** The entity-type tag of creatures born with {@code element}. */
    public static TagKey<EntityType<?>> innateTag(Element element) {
        return INNATE.get(element);
    }

    /** The creature's element, or null if it has none: innate first, then Attuned. Works on both sides. */
    @Nullable
    public static Element elementOf(LivingEntity entity) {
        if (entity instanceof Player) {
            return null;
        }
        Element innate = innateElementOf(entity);
        if (innate != null) {
            return innate;
        }
        return entity.hasData(MagicAttachments.CREATURE_MAGIC) ? entity.getData(MagicAttachments.CREATURE_MAGIC).element() : null;
    }

    /** The element the creature's type is born with (the innate tags), or null. */
    @Nullable
    public static Element innateElementOf(Entity entity) {
        for (Element element : Element.values()) {
            if (entity.getType().is(INNATE.get(element))) {
                return element;
            }
        }
        return null;
    }
}
