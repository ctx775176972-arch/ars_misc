package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc")
@PrefixGameTestTemplate(false)
public final class DecayGameTests {
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void healingReversesOnlyWhileMarked(GameTestHelper helper) {
        var target = helper.spawnWithNoFreeWill(EntityType.COW, 1, 2, 1);
        target.setHealth(8);
        target.heal(1);
        helper.assertTrue(target.getHealth()==9, "Unmarked healing changed");
        target.addEffect(new MobEffectInstance(ArsMiscMod.DECAY, 600));
        target.heal(3);
        helper.assertTrue(target.getHealth()==6, "Healing did not become equal damage");
        target.setInvulnerable(true);
        target.heal(3);
        helper.assertTrue(target.getHealth()==6, "Blocked conversion restored healing");
        target.setHealth(8);
        helper.assertTrue(target.getHealth()==8, "Script health setting was intercepted");
        target.removeEffect(ArsMiscMod.DECAY);
        target.heal(1);
        helper.assertTrue(target.getHealth()==9, "Healing remained reversed after effect removal");
        helper.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void retiredDecayRingIsNotRegistered(GameTestHelper helper) {
        helper.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
                net.minecraft.resources.ResourceLocation.parse("ars_misc:decay_ring")),
                "Retired decay ring is still registered");
        helper.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void decayGlyphRequiresWitherAndKeepsEqualHealingConversion(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var target=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);target.setHealth(8);
        var spell=new Spell(MethodSelf.INSTANCE,EffectDecay.INSTANCE);var context=SpellContext.fromEntity(spell,player,ItemStack.EMPTY);
        var resolver=new SpellResolver(context);var stats=new SpellStats.Builder().build();
        EffectDecay.INSTANCE.onResolveEntity(new net.minecraft.world.phys.EntityHitResult(target),h.getLevel(),player,stats,context,resolver);
        h.assertTrue(!target.hasEffect(ArsMiscMod.DECAY),"Glyph marked a target without Wither");
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,600,0));
        EffectDecay.INSTANCE.onResolveEntity(new net.minecraft.world.phys.EntityHitResult(target),h.getLevel(),player,stats,context,resolver);
        var decay=target.getEffect(ArsMiscMod.DECAY);
        h.assertTrue(decay!=null&&decay.getDuration()==600,"Glyph did not apply 30 seconds of decay");
        target.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);target.heal(3);
        h.assertTrue(target.getHealth()==5,"Existing decay lost equal healing conversion after Wither was removed");
        h.assertTrue(EffectDecay.INSTANCE.getDefaultManaCost()==100&&EffectDecay.INSTANCE.defaultTier()==SpellTier.THREE,"Wrong glyph cost/tier");
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc", timeoutTicks=80)
    public static void regenerationAndExpiration(GameTestHelper helper) {
        var regen=helper.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        regen.setHealth(4);
        regen.addEffect(new MobEffectInstance(ArsMiscMod.DECAY,600));
        regen.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,50));
        var expired=helper.spawnWithNoFreeWill(EntityType.COW,2,2,1);
        expired.setHealth(4);
        expired.addEffect(new MobEffectInstance(ArsMiscMod.DECAY,2));
        helper.runAfterDelay(55, () -> {
            helper.assertTrue(regen.getHealth()<4,"Vanilla regeneration did not become damage");
            helper.assertTrue(!expired.hasEffect(ArsMiscMod.DECAY),"Decay did not expire");
            expired.heal(2);
            helper.assertTrue(expired.getHealth()==6,"Expired decay still reversed healing");
            helper.succeed();
        });
    }}
