package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc")
@PrefixGameTestTemplate(false)
public final class LifeSenseGameTests {
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void lifeSenseScribingRecipeUsesRequestedMaterials(GameTestHelper h) {
        var id=net.minecraft.resources.ResourceLocation.parse("ars_misc:glyph_life_sense");
        var holder=h.getLevel().getRecipeManager().byKey(id).orElseThrow();
        h.assertTrue(holder.value() instanceof com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe,"Life Sense is not a scribing recipe");
        var recipe=(com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe)holder.value();
        h.assertTrue(recipe.getOutput().is(EffectLifeSense.INSTANCE.getGlyph()) && recipe.getInputs().size()==8,"Wrong Life Sense output or ingredient count");
        for(var item:java.util.List.of("ars_nouveau:dowsing_rod","ars_nouveau:starbuncle_shards","ars_nouveau:abjuration_essence")) {
            var stack=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(item)).getDefaultInstance();
            h.assertTrue(!stack.isEmpty() && recipe.getInputs().stream().filter(i->i.test(stack)).count()==1,"Wrong ingredient: "+item);
        }
        h.assertTrue(recipe.getInputs().stream().filter(i->i.test(net.minecraft.world.item.Items.SPECTRAL_ARROW.getDefaultInstance())).count()==5,"Life Sense needs five Spectral Arrows");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void glyphRegistrationAndDuration(GameTestHelper helper) {
        var glyph=EffectLifeSense.INSTANCE;
        helper.assertTrue(GlyphRegistry.getSpellPart(glyph.getRegistryName())==glyph,"Glyph missing from Ars registry");
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(glyph.getGlyph()).equals(glyph.getRegistryName()),"Glyph item is not registered");
        helper.assertTrue(glyph.getConfigTier()==SpellTier.TWO,"Glyph is not tier 2");
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,glyph),player,ItemStack.EMPTY);
        glyph.onResolveEntity(new EntityHitResult(player),helper.getLevel(),player,new SpellStats.Builder().build(),context,new SpellResolver(context));
        var effect=player.getEffect(ArsMiscMod.LIFE_SENSE);
        helper.assertTrue(effect!=null && effect.getDuration()==1200,"Default duration is not 60s");
        player.removeEffect(ArsMiscMod.LIFE_SENSE);
        var extended = new SpellStats.Builder().addAugment(AugmentExtendTime.INSTANCE);
        AugmentExtendTime.INSTANCE.applyModifiers(extended,glyph,new EntityHitResult(player),helper.getLevel(),player,context);
        glyph.onResolveEntity(new EntityHitResult(player),helper.getLevel(),player,extended.build(),context,new SpellResolver(context));
        helper.assertTrue(player.getEffect(ArsMiscMod.LIFE_SENSE).getDuration()==1500,"Extend Time did not add 15 seconds");
        player.removeEffect(ArsMiscMod.LIFE_SENSE);
        var shorten=com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown.INSTANCE;
        var shortened=new SpellStats.Builder().addAugment(shorten);
        shorten.applyModifiers(shortened,glyph,new EntityHitResult(player),helper.getLevel(),player,context);
        glyph.onResolveEntity(new EntityHitResult(player),helper.getLevel(),player,shortened.build(),context,new SpellResolver(context));
        helper.assertTrue(player.getEffect(ArsMiscMod.LIFE_SENSE).getDuration()==900,"Duration Down did not remove 15 seconds");
        var descriptions=new java.util.HashMap<com.hollingsworth.arsnouveau.api.spell.AbstractAugment,String>();
        glyph.addAugmentDescriptions(descriptions);
        helper.assertTrue(descriptions.containsKey(AugmentExtendTime.INSTANCE) && descriptions.containsKey(shorten),"Duration descriptions are missing");
        helper.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void rangeAndPrivateVisibility(GameTestHelper helper) {
        var observer=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var target=helper.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        helper.assertTrue(!LifeSenseEffect.shouldHighlight(observer,target),"Unbuffed observer sees outlines");
        observer.addEffect(new net.minecraft.world.effect.MobEffectInstance(ArsMiscMod.LIFE_SENSE,1200));
        target.setPos(observer.getX()+75,observer.getY(),observer.getZ());
        helper.assertTrue(LifeSenseEffect.shouldHighlight(observer,target),"75 block boundary excluded");
        target.setPos(observer.getX()+75.01,observer.getY(),observer.getZ());
        helper.assertTrue(!LifeSenseEffect.shouldHighlight(observer,target),"Entity outside range included");
        target.setPos(observer.getX()+50,observer.getY()+60,observer.getZ());
        helper.assertTrue(!LifeSenseEffect.shouldHighlight(observer,target),"Range is a cube instead of sphere");
        helper.assertTrue(!LifeSenseEffect.shouldHighlight(observer,observer),"Observer highlights itself");
        target.setPos(observer.getX()+2,observer.getY(),observer.getZ());
        helper.assertTrue(!target.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),"Sensing applied global Glowing");
        observer.removeEffect(ArsMiscMod.LIFE_SENSE);
        helper.assertTrue(!LifeSenseEffect.shouldHighlight(observer,target),"Outlines persisted after removal");
        helper.succeed();
    }
}
