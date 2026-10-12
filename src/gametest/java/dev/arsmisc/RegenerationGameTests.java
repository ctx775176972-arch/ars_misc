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
            helper.assertTrue(effect!=null && effect.getDuration()==200 && effect.getAmplifier()==0,
                "Glyph did not grant vanilla Regeneration I for 200 ticks");
            helper.assertTrue(target.getHealth()==4,"Glyph healed instantly");
        }
        var extended=new SpellStats.Builder().addAugment(AugmentExtendTime.INSTANCE);
        AugmentExtendTime.INSTANCE.applyModifiers(extended,glyph,new EntityHitResult(player),helper.getLevel(),player,context);
        glyph.onResolveEntity(new EntityHitResult(player),helper.getLevel(),player,extended.build(),context,resolver);
        helper.assertTrue(player.getEffect(MobEffects.REGENERATION).getDuration()==300,"Extend Time is not +5 seconds");
        helper.runAfterDelay(55, () -> {
            helper.assertTrue(normal.getHealth()>4,"Regeneration glyph did not heal over time");
            helper.assertTrue(decayed.getHealth()<4,"Regeneration glyph was not reversed by Decay");
            helper.succeed();
        });
    }
}
