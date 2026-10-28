package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc")
@PrefixGameTestTemplate(false)
public final class RegenerationGameTests {
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void regenerationThreeRequiresFocusAndCraftingAllowsOneAmplify(GameTestHelper h) {
        var glyph=EffectRegeneration.INSTANCE;
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,glyph),p,ItemStack.EMPTY);
        var resolver=new SpellResolver(context);
        for(int count=0;count<=3;count++) {
            p.removeEffect(MobEffects.REGENERATION);
            var stats=new SpellStats.Builder();
            for(int i=0;i<count;i++) stats.addAugment(com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify.INSTANCE);
            glyph.onResolveEntity(new EntityHitResult(p),h.getLevel(),p,stats.build(),context,resolver);
            h.assertTrue(p.getEffect(MobEffects.REGENERATION).getAmplifier()==Math.min(count,1)
                    &&p.getEffect(MobEffects.REGENERATION).getDuration()==400,"Wrong regeneration level or duration");
        }
        var parts=new java.util.ArrayList<AbstractSpellPart>(java.util.List.of(MethodSelf.INSTANCE,glyph,
                com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify.INSTANCE));
        var validator=com.hollingsworth.arsnouveau.api.ArsNouveauAPI.getInstance().getSpellCraftingSpellValidator();
        h.assertTrue(validator.validate(parts).isEmpty(),"One Amplify glyph was rejected");
        parts.add(com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify.INSTANCE);
        h.assertTrue(!validator.validate(parts).isEmpty(),"Second crafted Amplify was accepted");
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("an_focus",0,
                ArsMiscMod.PALADIN_FOCUS.get().getDefaultInstance());
        p.removeEffect(MobEffects.REGENERATION);
        var focusedStats=new SpellStats.Builder().addAugment(com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify.INSTANCE)
                .build(glyph,new EntityHitResult(p),h.getLevel(),p,context);
        glyph.onResolveEntity(new EntityHitResult(p),h.getLevel(),p,focusedStats,context,resolver);
        h.assertTrue(p.getEffect(MobEffects.REGENERATION).getAmplifier()==2
                && p.getEffect(MobEffects.REGENERATION).getDuration()==400,"Focus did not grant Regeneration III at unchanged duration");
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("an_focus",0,ItemStack.EMPTY);
        p.removeEffect(MobEffects.REGENERATION);
        glyph.onResolveEntity(new EntityHitResult(p),h.getLevel(),p,focusedStats,context,resolver);
        h.assertTrue(p.getEffect(MobEffects.REGENERATION).getAmplifier()==1,"Focused stats bypassed unequipped level cap");
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void scribingRecipeHasRequestedIngredients(GameTestHelper h) {
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_misc","glyph_regeneration");
        var holder=h.getLevel().getRecipeManager().byKey(id).orElseThrow();
        h.assertTrue(holder.value() instanceof com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe,"Regeneration recipe is not a scribing recipe");
        var recipe=(com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe)holder.value();
        h.assertTrue(recipe.getOutput().is(EffectRegeneration.INSTANCE.getGlyph()) && recipe.getInputs().size()==6,"Wrong recipe output or ingredient count");
        long tears=recipe.getInputs().stream().filter(i->i.test(new ItemStack(net.minecraft.world.item.Items.GHAST_TEAR))).count();
        h.assertTrue(tears==4 && recipe.getInputs().get(0).test(com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.ABJURATION_ESSENCE.get().getDefaultInstance())
            && recipe.getInputs().get(1).test(new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE)),"Wrong scribing ingredients");
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc", timeoutTicks=90)
    public static void glyphHealsAndDecayReverses(GameTestHelper helper) {
        var glyph=EffectRegeneration.INSTANCE;
        helper.assertTrue(GlyphRegistry.getSpellPart(glyph.getRegistryName())==glyph && glyph.getConfigTier()==SpellTier.TWO,
            "Regeneration glyph missing or wrong tier");
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,glyph),player,ItemStack.EMPTY);
        var resolver=new SpellResolver(context);
        var normal=helper.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        var decayed=helper.spawnWithNoFreeWill(EntityType.COW,2,2,1);
        normal.setHealth(4); decayed.setHealth(4);
        decayed.addEffect(new MobEffectInstance(ArsMiscMod.DECAY,600));
        for (var target: java.util.List.of(normal,decayed)) {
            glyph.onResolveEntity(new EntityHitResult(target),helper.getLevel(),player,new SpellStats.Builder().build(),context,resolver);
            var effect=target.getEffect(MobEffects.REGENERATION);
            helper.assertTrue(effect!=null && effect.getDuration()==400 && effect.getAmplifier()==0,
                "Glyph did not grant vanilla Regeneration I for 400 ticks");
            helper.assertTrue(target.getHealth()==4,"Glyph healed instantly");
        }
        var extended=new SpellStats.Builder().addAugment(AugmentExtendTime.INSTANCE);
        AugmentExtendTime.INSTANCE.applyModifiers(extended,glyph,new EntityHitResult(player),helper.getLevel(),player,context);
        glyph.onResolveEntity(new EntityHitResult(player),helper.getLevel(),player,extended.build(),context,resolver);
        helper.assertTrue(player.getEffect(MobEffects.REGENERATION).getDuration()==500,"Extend Time is not +5 seconds");
        helper.runAfterDelay(55, () -> {
            helper.assertTrue(normal.getHealth()>4,"Regeneration glyph did not heal over time");
            helper.assertTrue(decayed.getHealth()<4,"Regeneration glyph was not reversed by Decay");
            helper.succeed();
        });
    }
}
