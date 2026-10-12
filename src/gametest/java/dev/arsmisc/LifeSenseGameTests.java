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
        helper.assertTrue(player.getEffect(ArsMiscMod.LIFE_SENSE).getDuration()>1200,"Extend Time did not extend duration");
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
