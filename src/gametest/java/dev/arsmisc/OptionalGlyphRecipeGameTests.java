package dev.arsmisc;

import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class OptionalGlyphRecipeGameTests {
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void optionalRecipesAreOffByDefaultButCreativeGlyphsRemainEnabled(GameTestHelper h) {
        h.assertTrue(!MiscConfig.ENABLE_SOUL_HARVEST_RECIPE.get()&&!MiscConfig.ENABLE_AVADA_KEDAVRA_RECIPE.get(),"Optional recipe defaults are enabled");
        for(var name:java.util.List.of("soul_harvest","avada_kedavra")) {
            var id=ResourceLocation.fromNamespaceAndPath("ars_misc","glyph_"+name);
            h.assertTrue(h.getLevel().getRecipeManager().byKey(id).isEmpty(),"Disabled recipe was loaded: "+id);
            var glyph=com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(id);
            h.assertTrue(glyph!=null&&glyph.isEnabled()&&glyph.getGlyph()!=null,"Recipe gating disabled the creative glyph");
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void serializedConditionsRespectIndependentConfigSwitches(GameTestHelper h) {
        var soul=ICondition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(
                "{\"type\":\"ars_misc:glyph_recipe_enabled\",\"glyph\":\"soul_harvest\"}")).getOrThrow();
        var avada=ICondition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(
                "{\"type\":\"ars_misc:glyph_recipe_enabled\",\"glyph\":\"avada_kedavra\"}")).getOrThrow();
        boolean oldSoul=MiscConfig.ENABLE_SOUL_HARVEST_RECIPE.get(),oldAvada=MiscConfig.ENABLE_AVADA_KEDAVRA_RECIPE.get();
        try {
            MiscConfig.ENABLE_SOUL_HARVEST_RECIPE.set(true);MiscConfig.ENABLE_AVADA_KEDAVRA_RECIPE.set(false);
            h.assertTrue(soul.test(ICondition.IContext.EMPTY)&&!avada.test(ICondition.IContext.EMPTY),"Soul recipe toggle affects Avada");
            MiscConfig.ENABLE_SOUL_HARVEST_RECIPE.set(false);MiscConfig.ENABLE_AVADA_KEDAVRA_RECIPE.set(true);
            h.assertTrue(!soul.test(ICondition.IContext.EMPTY)&&avada.test(ICondition.IContext.EMPTY),"Avada recipe toggle affects Soul Harvest");
        } finally {
            MiscConfig.ENABLE_SOUL_HARVEST_RECIPE.set(oldSoul);MiscConfig.ENABLE_AVADA_KEDAVRA_RECIPE.set(oldAvada);
        }
        h.succeed();
    }
}
