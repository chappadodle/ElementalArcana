package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.SchoolElements;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Elemental Essence for spell progression, shared by the server handler and the screens:
 * which element a spell is, how much Essence a player carries, and what infusing is worth.
 */
public final class EssenceService {

    private EssenceService() {
    }

    /** The element of a spell's school, or null for schools that aren't one of the four elements. */
    @Nullable
    public static Element elementOf(Spell spell) {
        return SchoolElements.of(spell.school());
    }

    public static int count(Player player, Element element) {
        return player.getInventory().countItem(ModItems.essence(element));
    }

    public static int total(Player player) {
        int total = 0;
        for (Element element : Element.values()) {
            total += count(player, element);
        }
        return total;
    }

    /** Takes up to {@code amount} Essence of {@code element}; returns how many were taken. */
    public static int remove(Player player, Element element, int amount) {
        Item item = ModItems.essence(element);
        return player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), amount, player.inventoryMenu.getCraftSlots());
    }

    /** Mastery one Essence adds to {@code spell} right now (0 if it can't take any). */
    public static int infuseAmount(MagicData data, Spell spell) {
        int bar = data.masteryToNextLevel(spell);
        return bar <= 0 ? 0 : Progression.essenceMastery(data.spellLevel(spell), bar);
    }

    /** How many Essence an infusion would use: 1, or just enough to fill the bar with {@code fill}. */
    public static int infuseCount(MagicData data, Spell spell, boolean fill, int available) {
        int bar = data.masteryToNextLevel(spell);
        int needed = Progression.essenceToFill(data.spellLevel(spell), bar, data.progress(spell).mastery());
        if (bar <= 0 || needed <= 0) {
            return 0;
        }
        return Math.min(available, fill ? needed : 1);
    }
}
