package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.documentation.ReloadDocumentationEvent;
import com.hollingsworth.arsnouveau.api.documentation.builder.DocEntryBuilder;
import com.hollingsworth.arsnouveau.api.registry.DocumentationRegistry;
import net.minecraft.network.chat.Component;

public final class WatchDocumentation {
    public static void addEntries(ReloadDocumentationEvent.AddEntries event) {
        var watch=ArsMiscMod.ENCHANTERS_POCKET_WATCH.get();
        var builder=new DocEntryBuilder(ArsMiscMod.MOD_ID, DocumentationRegistry.ITEMS, watch)
            .withSortNum(100)
            .withIntroPageNoIncrement(Component.translatable("documentation.ars_misc.enchanters_pocket_watch.summary"),
                Component.translatable("item.ars_misc.enchanters_pocket_watch"),watch.getDefaultInstance())
            .withTextPage(Component.translatable("documentation.ars_misc.enchanters_pocket_watch.operation"))
            .withCraftingPages().addConnectedSearch(watch);
        DocumentationRegistry.registerEntry(DocumentationRegistry.ITEMS,builder.build());
    }
}
