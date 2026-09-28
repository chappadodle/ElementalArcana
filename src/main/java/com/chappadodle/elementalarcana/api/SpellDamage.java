package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class SpellDamage {
    private static final Map<Element, ResourceKey<DamageType>> TYPES = new EnumMap<>(Element.class);

    static {
        for (Element element : Element.values()) {
            TYPES.put(element, ResourceKey.create(Registries.DAMAGE_TYPE,
                    ElementalArcana.id(element.name().toLowerCase(Locale.ROOT) + "_spell")));
        }
    }

    private SpellDamage() {
    }

    /** The damage type {@code elementalarcana:<element>_spell}. */
    public static ResourceKey<DamageType> damageType(Element element) {
        return TYPES.get(element);
    }

    /**
     * Spell damage of an element. {@code direct} is what touched the target (a projectile, or the
     * caster for touch/area spells) and {@code owner} is who cast it. Pass at least one entity: death
     * messages name it.
     */
    public static DamageSource source(Level level, Element element, @Nullable Entity direct, @Nullable Entity owner) {
        // DamageSources#source hands its first entity on as the direct one and its second as the causing one.
        return level.damageSources().source(TYPES.get(element), direct, owner);
    }

    /** The element of a spell's damage, or null for any other damage (swords, arrows, burning…). */
    @Nullable
    public static Element elementOf(DamageSource source) {
        for (Element element : Element.values()) {
            if (source.is(TYPES.get(element))) {
                return element;
            }
        }
        return null;
    }

    /**
     * Damages {@code target} even if it was hit a moment ago. After any hit, Minecraft ignores
     * further damage for 10 ticks (unless it's bigger, and then only the difference counts), so
     * several projectiles from one volley would otherwise land as a single hit.
     */
    public static boolean hurtMultiHit(Entity target, DamageSource source, float amount) {
        target.invulnerableTime = 0;
        return target.hurt(source, amount);
    }
}
