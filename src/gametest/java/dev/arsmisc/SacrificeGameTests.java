package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectDispel;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc")
@PrefixGameTestTemplate(false)
public final class SacrificeGameTests {
    private static SpellStats stats(int count) {
        var builder = new SpellStats.Builder();
        for (int i=0; i<count; i++) builder.addAugment(AugmentAmplify.INSTANCE);
        return builder.build();
    }
    private static void cast(GameTestHelper h, AbstractEffect effect, LivingEntity caster, LivingEntity target, SpellStats stats) {
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,effect),caster,ItemStack.EMPTY);
        effect.onResolveEntity(new EntityHitResult(target),h.getLevel(),caster,stats,context,new SpellResolver(context));
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void shieldLevelsRefreshAndDispel(GameTestHelper h) {
        var caster=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        caster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        var glyph=EffectSacrificialShield.INSTANCE;
        h.assertTrue(glyph.getCastingCost()==100 && glyph.getAugmentLimit(AugmentAmplify.INSTANCE.getRegistryName())==4,
            "Shield mana cost or Amplify limit incorrect");
        for (int i=0; i<=5; i++) {
            caster.setHealth(100);
            caster.setAbsorptionAmount(1);
            int level=2+2*Math.min(i,4);
            cast(h,glyph,caster,caster,stats(i));
            var effect=caster.getEffect(MobEffects.ABSORPTION);
            h.assertTrue(effect!=null && effect.getAmplifier()==level-1 && effect.getDuration()==1200,"Shield level/duration incorrect");
            h.assertTrue(caster.getHealth()==100-level,"Real health cost incorrect");
            h.assertTrue(caster.getAbsorptionAmount()==level*4,"Absorption was not refilled or exceeded cap");
        }
        caster.setHealth(100);
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,600,4));
        cast(h,glyph,caster,caster,stats(0));
        h.assertTrue(caster.getHealth()==98 && caster.getAbsorptionAmount()==8,"Resistance or existing absorption paid the cost");
        caster.setAbsorptionAmount(2);
        cast(h,glyph,caster,caster,stats(0));
        h.assertTrue(caster.getHealth()==96 && caster.getAbsorptionAmount()==8,"Recasting did not pay/refill");
        cast(h,EffectDispel.INSTANCE,caster,caster,stats(0));
        h.assertTrue(!caster.hasEffect(MobEffects.ABSORPTION) && caster.getAbsorptionAmount()==0,"Dispel left gold hearts");
        h.assertTrue(caster.getHealth()==96,"Dispel refunded life");
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void sacrificeCanKillAndPaysCaster(GameTestHelper h) {
        var caster=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        var target=h.spawnWithNoFreeWill(EntityType.COW,2,2,1);
        caster.setHealth(8); target.setHealth(8);
        cast(h,EffectSacrificialShield.INSTANCE,caster,target,stats(0));
        h.assertTrue(caster.getHealth()==6 && target.getHealth()==8 && target.getAbsorptionAmount()==8,"Ally shielding paid wrong entity");
        caster.setHealth(2);
        caster.setInvulnerable(true);
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,600,9));
        cast(h,EffectSacrificialShield.INSTANCE,caster,caster,stats(0));
        h.assertTrue(caster.getHealth()==0 && caster.isDeadOrDying(),"Insufficient health was not lethal");
        h.runAfterDelay(2, () -> {
            h.assertTrue(caster.deathTime>0 || caster.isRemoved(),"Death flow was not started");
            h.succeed();
        });
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void regenerationLevelsAndShieldExtension(GameTestHelper h) {
        var target=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        h.assertTrue(EffectRegeneration.INSTANCE.getAugmentLimit(AugmentAmplify.INSTANCE.getRegistryName())==4,"Regen cap incorrect");
        for (int i=0; i<=5; i++) {
            target.removeEffect(MobEffects.REGENERATION);
            cast(h,EffectRegeneration.INSTANCE,target,target,stats(i));
            var effect=target.getEffect(MobEffects.REGENERATION);
            h.assertTrue(effect.getAmplifier()==Math.min(i,4) && effect.getDuration()==200,"Regen level/cap incorrect");
        }
        var glyph=EffectSacrificialShield.INSTANCE;
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,glyph),target,ItemStack.EMPTY);
        var extended=new SpellStats.Builder().addAugment(AugmentExtendTime.INSTANCE);
        AugmentExtendTime.INSTANCE.applyModifiers(extended,glyph,new EntityHitResult(target),h.getLevel(),target,context);
        target.setHealth(10);
        cast(h,glyph,target,target,extended.build());
        h.assertTrue(target.getEffect(MobEffects.ABSORPTION).getDuration()==1500 && target.getHealth()==8,"Extension changed health cost or wrong duration");
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void lethalCostUsesVanillaTotems(GameTestHelper h) {
        for (var hand: net.minecraft.world.InteractionHand.values()) {
            var caster=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);
            caster.setHealth(2);
            caster.setItemInHand(hand,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
            cast(h,EffectSacrificialShield.INSTANCE,caster,caster,stats(0));
            h.assertTrue(caster.isAlive() && caster.getHealth()==1,"Totem did not save lethal sacrifice");
            h.assertTrue(caster.getItemInHand(hand).isEmpty(),"Totem was not consumed");
            h.assertTrue(caster.hasEffect(MobEffects.REGENERATION) && caster.hasEffect(MobEffects.FIRE_RESISTANCE),"Vanilla totem effects missing");
            h.assertTrue(caster.getAbsorptionAmount()==8 && caster.getEffect(MobEffects.ABSORPTION).getDuration()==1200,
                "Saved cast did not grant sacrificial shield");
        }
        h.succeed();
    }
}
