package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Arcanist's Journal's pages, shown in the game's own book screen: the guide pages
 * {@code journal.elementalarcana.page.<n>} (as many as the language file has), a chapter for each
 * element the reader holds, and the lore pages {@code journal.elementalarcana.lore.<n>}.
 */
public final class JournalBook {

    private JournalBook() {
    }

    public static void open(Player player) {
        List<Component> pages = new ArrayList<>();
        addNumbered(pages, "journal.elementalarcana.page.");
        MagicData data = MagicAttachments.get(player);
        for (Element element : Element.values()) {
            if (data.affinityElements().contains(element)) {
                pages.add(Component.translatable("journal.elementalarcana.element." + element.name().toLowerCase(Locale.ROOT)));
            }
        }
        addNumbered(pages, "journal.elementalarcana.lore.");
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
    }

    /** Adds {@code prefix + 1}, {@code prefix + 2}... for as long as the language file has them. */
    private static void addNumbered(List<Component> pages, String prefix) {
        Language language = Language.getInstance();
        for (int i = 1; language.has(prefix + i); i++) {
            pages.add(Component.translatable(prefix + i));
        }
    }
}
