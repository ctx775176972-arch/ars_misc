package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.documentation.ReloadDocumentationEvent;
import com.hollingsworth.arsnouveau.api.documentation.builder.DocEntryBuilder;
import com.hollingsworth.arsnouveau.api.registry.DocumentationRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import com.hollingsworth.arsnouveau.api.documentation.DocCategory;

public final class WatchDocumentation {
    public static void addEntries(ReloadDocumentationEvent.AddEntries event) {
        add(DocumentationRegistry.SPELL_CASTING, ArsMiscMod.ENCHANTERS_POCKET_WATCH.get(), "enchanters_pocket_watch", true);
        add(DocumentationRegistry.SPELL_CASTING, ArsMiscMod.ENCHANTERS_FIELD.get(), "enchanters_field", true);
        add(DocumentationRegistry.SPELL_CASTING, ArsMiscMod.FINAL_BARRIER.get(), "final_barrier", false);
        add(DocumentationRegistry.SPELL_CASTING, ArsMiscMod.COMBAT_WAND.get(), "combat_wand", false);
        add(DocumentationRegistry.SPELL_CASTING, ArsMiscMod.NETHERITE_ENCHANTERS_SWORD.get(), "netherite_enchanters_sword", false);
        for (var ring : ArsMiscMod.FORTUNE_STAGES) {
            add(DocumentationRegistry.ITEMS, ring.get(), ring.getId().getPath(), false);
        }
        for (var ring : ArsMiscMod.BLASTING_STAGES) {
            add(DocumentationRegistry.ITEMS, ring.get(), ring.getId().getPath(), false);
        }
        add(DocumentationRegistry.ITEMS, ArsMiscMod.CRITICAL_BELT.get(), "critical_belt", false);
        add(DocumentationRegistry.ITEMS, ArsMiscMod.CRITICAL_GLASSES.get(), "critical_glasses", false);
        add(DocumentationRegistry.ITEMS, ArsMiscMod.CRITICAL_NECKLACE.get(), "critical_necklace", false);
        add(DocumentationRegistry.ITEMS, ArsMiscMod.PALADIN_FOCUS.get(), "paladin_focus", false);
    }

    private static void add(DocCategory category, Item item, String id, boolean operation) {
        var builder = new DocEntryBuilder(ArsMiscMod.MOD_ID, category, item)
                .withSortNum(100)
                .withIntroPageNoIncrement(Component.translatable("documentation.ars_misc." + id + ".summary"),
                        item.getDescription(), item.getDefaultInstance());
        if (operation) builder.withTextPage(Component.translatable("documentation.ars_misc." + id + ".operation"));
        var details = switch (id) {
            case "paladin_focus" -> java.util.List.of("protection", "dispel");
            case "final_barrier" -> java.util.List.of("defense", "cooldown");
            case "netherite_enchanters_sword" -> java.util.List.of("damage", "upgrades");
            default -> java.util.List.<String>of();
        };
        for (var page : details)
            builder.withTextPage(Component.translatable("documentation.ars_misc." + id + "." + page));
        builder.withCraftingPages().addConnectedSearch(item);
        DocumentationRegistry.registerEntry(category, builder.build());
    }
}
