package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class RemovedContentGameTests {
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void archivedSummonsAreAbsentFromRuntimeRegistries(GameTestHelper h) {
        for(var name:java.util.List.of("guardian","warrior")) {
            var entity=ResourceLocation.fromNamespaceAndPath("ars_misc","summoned_"+name);
            var glyph=ResourceLocation.fromNamespaceAndPath("ars_misc","glyph_summon_"+name);
            h.assertTrue(!BuiltInRegistries.ENTITY_TYPE.containsKey(entity),"Archived summon entity is still registered: "+entity);
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(glyph) && !GlyphRegistry.getSpellpartMap().containsKey(glyph)
                    && !GlyphRegistry.getGlyphItemMap().containsKey(glyph),"Archived summon glyph is still registered: "+glyph);
        }
        h.assertTrue(GlyphRegistry.getSpellPart(EffectLifeSense.INSTANCE.getRegistryName())==EffectLifeSense.INSTANCE,
                "Removing summons broke another glyph registration");
        var sword=ResourceLocation.parse("ars_misc:advanced_enchanters_sword");
        var scribe=ResourceLocation.parse("ars_misc:reality_marble");
        h.assertTrue(!BuiltInRegistries.ITEM.containsKey(sword) && !BuiltInRegistries.ITEM.containsKey(scribe)
                && !BuiltInRegistries.MENU.containsKey(scribe),"Archived sword or scribe is still registered");
        var nativeSword=com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.ENCHANTERS_SWORD.get().getDefaultInstance();
        h.assertTrue(((com.hollingsworth.arsnouveau.api.item.ICasterTool)nativeSword.getItem()).getSpellCaster(nativeSword)!=null
                && com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.hasCaster(ArsMiscMod.COMBAT_WAND.get().getDefaultInstance()),
                "Removing prototypes broke native sword or Combat Wand casting registration");
        h.succeed();
    }
}
