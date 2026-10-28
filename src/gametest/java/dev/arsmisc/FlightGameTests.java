package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.ModPotions;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class FlightGameTests {
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void flightSupportsNativeDurationAugments(GameTestHelper h) {
        var glyph=EffectFlight.INSTANCE;
        var extend=com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime.INSTANCE;
        var shorten=com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown.INSTANCE;
        h.assertTrue(glyph.getCompatibleAugments().contains(extend) && glyph.getCompatibleAugments().contains(shorten),"Duration augments are not supported");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,glyph),player,ItemStack.EMPTY);
        var hit=new EntityHitResult(player);
        var resolver=new SpellResolver(context);
        var longer=new SpellStats.Builder().addAugment(extend);
        extend.applyModifiers(longer,glyph,hit,h.getLevel(),player,context);
        glyph.onResolveEntity(hit,h.getLevel(),player,longer.build(),context,resolver);
        h.assertTrue(player.getEffect(ModPotions.FLIGHT_EFFECT).getDuration()==800,"Extend Time did not add 10 seconds");
        player.removeEffect(ModPotions.FLIGHT_EFFECT);
        var shorter=new SpellStats.Builder().addAugment(shorten);
        shorten.applyModifiers(shorter,glyph,hit,h.getLevel(),player,context);
        glyph.onResolveEntity(hit,h.getLevel(),player,shorter.build(),context,resolver);
        h.assertTrue(player.getEffect(ModPotions.FLIGHT_EFFECT).getDuration()==400,"Duration Down did not shorten Flight");
        player.removeEffect(ModPotions.FLIGHT_EFFECT);
        for(int count=0;count<3;count++) {
            shorter.addAugment(shorten);
            shorten.applyModifiers(shorter,glyph,hit,h.getLevel(),player,context);
        }
        glyph.onResolveEntity(hit,h.getLevel(),player,shorter.build(),context,resolver);
        h.assertTrue(player.getEffect(ModPotions.FLIGHT_EFFECT).getDuration()==1,"Flight duration was not clamped above zero");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void flightRecipesUseRequestedMaterials(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        var flight=manager.byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_misc","glyph_flight")).orElseThrow().value();
        h.assertTrue(flight instanceof com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe,"Flight is not a scribing recipe");
        var glyph=(com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe)flight;
        var ingredients=java.util.List.of(net.minecraft.world.item.Items.DRAGON_HEAD,
                net.minecraft.world.item.Items.PHANTOM_MEMBRANE,net.minecraft.world.item.Items.FERMENTED_SPIDER_EYE,
                net.minecraft.world.item.Items.POPPED_CHORUS_FRUIT);
        h.assertTrue(glyph.getOutput().is(EffectFlight.INSTANCE.getGlyph()) && glyph.getInputs().size()==8,"Wrong flight output or ingredient count");
        for(var item:ingredients) h.assertTrue(glyph.getInputs().stream().filter(i->i.test(item.getDefaultInstance())).count()==1,"Wrong flight ingredient: "+item);
        h.assertTrue(glyph.getInputs().stream().filter(i->i.test(com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.AIR_ESSENCE.get().getDefaultInstance())).count()==4,"Flight needs four Air Essences");
        h.assertTrue(EffectFlight.INSTANCE.getDefaultManaCost()==200 && EffectFlight.INSTANCE.getCastingCost()==200,"Flight cost is not 200");
        var thread=manager.byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_misc","thread_creative_flight")).orElseThrow().value();
        h.assertTrue(thread instanceof com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe,"Flight thread is not an apparatus recipe");
        var apparatus=(com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe)thread;
        var glide=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("ars_nouveau:thread_gliding")).getDefaultInstance();
        h.assertTrue(apparatus.reagent().test(glide) && apparatus.result().is(ArsMiscMod.CREATIVE_FLIGHT_THREAD.get()),"Wrong flight thread reagent or output");
        h.assertTrue(!apparatus.reagent().test(com.hollingsworth.arsnouveau.common.spell.effect.EffectGlide.INSTANCE.getGlyph().getDefaultInstance()),"Flight thread still accepts a Glide glyph");
        h.assertTrue(apparatus.pedestalItems().size()==3 && apparatus.sourceCost()==0,"Wrong pedestal count or Source cost");
        var stacks=ingredients.subList(1,4).stream().map(item->item.getDefaultInstance()).toList();
        h.assertTrue(com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe.doItemsMatch(stacks,apparatus.pedestalItems()),"Wrong flight thread materials");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void nativeFlightGrantsAndRemovesFlyingAttribute(GameTestHelper h) {
        var glyph=EffectFlight.INSTANCE;
        h.assertTrue(GlyphRegistry.getSpellPart(glyph.getRegistryName())==glyph && glyph.getConfigTier()==SpellTier.THREE,"Flight glyph missing or wrong tier");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,glyph),player,ItemStack.EMPTY);
        var resolver=new SpellResolver(context);
        double before=player.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT);
        glyph.onResolveEntity(new EntityHitResult(player),h.getLevel(),player,new SpellStats.Builder().build(),context,resolver);
        var effect=player.getEffect(ModPotions.FLIGHT_EFFECT);
        h.assertTrue(effect!=null && effect.getDuration()==600 && effect.getAmplifier()==0,"Native Flight was not applied for 600 ticks");
        h.assertTrue(player.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT)>before,"Native flight attribute not granted");
        effect.tick(player,()->{});
        glyph.onResolveEntity(new EntityHitResult(player),h.getLevel(),player,new SpellStats.Builder().build(),context,resolver);
        h.assertTrue(player.getEffect(ModPotions.FLIGHT_EFFECT).getDuration()==600,"Recasting did not refresh Flight");
        player.removeEffect(ModPotions.FLIGHT_EFFECT);
        h.assertTrue(player.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT)==before,"Removing Flight left its attribute active");
        h.succeed();
    }
}
