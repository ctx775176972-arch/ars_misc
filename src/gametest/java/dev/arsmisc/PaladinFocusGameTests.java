package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class PaladinFocusGameTests {
    private static void equip(Player p, boolean equipped) {
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("an_focus", 0,
                equipped ? ArsMiscMod.PALADIN_FOCUS.get().getDefaultInstance() : ItemStack.EMPTY);
    }
    private static SpellStats stats(Player p, AbstractSpellPart effect, boolean explicitAmplify) {
        var spell = explicitAmplify ? new Spell(MethodSelf.INSTANCE, effect, AugmentAmplify.INSTANCE)
                : new Spell(MethodSelf.INSTANCE, effect);
        var context = SpellContext.fromEntity(spell, p, ItemStack.EMPTY);
        var builder = new SpellStats.Builder();
        if (explicitAmplify) builder.addAugment(AugmentAmplify.INSTANCE);
        return builder.build(effect, new EntityHitResult(p), p.level(), p, context);
    }
    private static void close(GameTestHelper h, double a, double b, String message) {
        h.assertTrue(Math.abs(a-b)<0.0001, message+": "+a+" != "+b);
    }

    private static void dispel(Player caster, net.minecraft.world.entity.LivingEntity target) {
        var spell=new Spell(MethodSelf.INSTANCE,com.hollingsworth.arsnouveau.common.spell.effect.EffectDispel.INSTANCE);
        var context=SpellContext.fromEntity(spell,caster,ItemStack.EMPTY);
        com.hollingsworth.arsnouveau.common.spell.effect.EffectDispel.INSTANCE.onResolveEntity(
                new EntityHitResult(target),caster.level(),caster,new SpellStats.Builder().build(),context,new SpellResolver(context));
    }

    private static void mixedEffects(net.minecraft.world.entity.LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION,600));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,600));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING,600));
    }

    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void focusDispelSelectsSelfPetTeamAndOtherTargets(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h);equip(p,true);
        mixedEffects(p);dispel(p,p);
        h.assertTrue(p.hasEffect(MobEffects.REGENERATION) && p.hasEffect(MobEffects.WEAKNESS)
                && p.hasEffect(MobEffects.GLOWING),"Focus dispel changed self effects");
        var pet=h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.WOLF,2,2,2);
        pet.setTame(true,false);pet.setOwnerUUID(p.getUUID());
        mixedEffects(pet);dispel(p,pet);
        h.assertTrue(pet.hasEffect(MobEffects.REGENERATION) && pet.hasEffect(MobEffects.WEAKNESS),"Focus dispel changed owned pet effects");
        var ally=h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW,3,2,2);
        var board=h.getLevel().getScoreboard();
        var team=board.addPlayerTeam("paladin_"+p.getId());
        board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(ally.getScoreboardName(),team);
        mixedEffects(ally);dispel(p,ally);
        h.assertTrue(ally.hasEffect(MobEffects.REGENERATION) && ally.hasEffect(MobEffects.WEAKNESS),"Focus dispel changed teammate effects");
        var other=h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW,4,2,2);
        mixedEffects(other);dispel(p,other);
        h.assertTrue(!other.hasEffect(MobEffects.REGENERATION) && other.hasEffect(MobEffects.WEAKNESS)
                && other.hasEffect(MobEffects.GLOWING),"Other-target dispel removed debuffs or neutral effects");
        board.removePlayerTeam(team);h.succeed();
    }

    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void focusDispelPreservesNativeCuresAndUnequippedBehavior(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h);equip(p,true);
        var other=h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW,2,2,2);
        var uncurable=new MobEffectInstance(MobEffects.REGENERATION,600);
        uncurable.getCures().clear();other.addEffect(uncurable);
        other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,600));
        dispel(p,other);
        h.assertTrue(other.hasEffect(MobEffects.REGENERATION) && other.hasEffect(MobEffects.WEAKNESS),"Focus bypassed native cure protection");
        equip(p,false);mixedEffects(p);dispel(p,p);
        h.assertTrue(!p.hasEffect(MobEffects.REGENERATION) && !p.hasEffect(MobEffects.WEAKNESS)
                && !p.hasEffect(MobEffects.GLOWING),"Unequipped native dispel changed");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void freeAmplifyRespectsCapAndDoesNotRewriteRecipe(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h);
        close(h, stats(p, EffectRegeneration.INSTANCE, false).getBuffCount(AugmentAmplify.INSTANCE), 0, "Unequipped bonus");
        equip(p,true);
        close(h, stats(p, EffectRegeneration.INSTANCE, false).getBuffCount(AugmentAmplify.INSTANCE), 1, "Missing free Amplify");
        close(h, stats(p, EffectRegeneration.INSTANCE, true).getBuffCount(AugmentAmplify.INSTANCE), 2, "Free Amplify did not reach the new regeneration cap");
        var spell=new Spell(MethodSelf.INSTANCE, EffectRegeneration.INSTANCE);
        var resolver=new SpellResolver(SpellContext.fromEntity(spell,p,ItemStack.EMPTY));
        int expected=spell.getCost()-PaladinFocusEvents.discount(spell);
        close(h,resolver.getResolveCost(),expected,"Quoted cost");
        close(h,resolver.getExpendedCost(),expected,"Spent cost");
        h.assertTrue(spell.unsafeList().size()==2,"Free augment modified recipe");
        equip(p,false);
        close(h,resolver.getResolveCost(),spell.getCost(),"Discount survived unequip");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void healingAndRegenerationGrantVanillaAbsorption(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h); equip(p,true);
        for (var effect: java.util.List.of(EffectHeal.INSTANCE,EffectRegeneration.INSTANCE)) {
            p.removeEffect(MobEffects.ABSORPTION);
            new SpellResolver(SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,effect),p,ItemStack.EMPTY)).onResolveEffect(h.getLevel(),new EntityHitResult(p));
            var gold=p.getEffect(MobEffects.ABSORPTION);
            h.assertTrue(gold!=null && gold.getDuration()==200 && gold.getAmplifier()==0,"Missing 10-second Absorption I");
            close(h,p.getAbsorptionAmount(),4,"Wrong golden hearts");
        }
        h.assertTrue(p.getEffect(MobEffects.REGENERATION).getAmplifier()==1,"Free Amplify did not affect regeneration");
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,1200,3));
        new SpellResolver(SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,EffectHeal.INSTANCE),p,ItemStack.EMPTY)).onResolveEffect(h.getLevel(),new EntityHitResult(p));
        h.assertTrue(p.getEffect(MobEffects.ABSORPTION).getAmplifier()==3,"Healing downgraded strong absorption");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void crystalScalingCapAndMovementFollowCurrentRecipientEquipment(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h);
        double speed=p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        p.addEffect(new MobEffectInstance(ArsMiscMod.CRYSTAL_SHIELD,600,3));
        p.getPersistentData().putDouble(CrystalShieldEffect.POWER_KEY,6);
        close(h,CrystalShieldEvents.reduction(p),.36,"Normal scaling");
        equip(p,true); CrystalShieldEvents.refreshMovement(p);
        close(h,CrystalShieldEvents.reduction(p),.42,"Focus scaling");
        close(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),speed,"Focus retained slow");
        p.getPersistentData().putDouble(CrystalShieldEffect.POWER_KEY,100);
        close(h,CrystalShieldEvents.reduction(p),.8,"Focus cap");
        equip(p,false); CrystalShieldEvents.refreshMovement(p);
        close(h,CrystalShieldEvents.reduction(p),.6,"Normal cap");
        close(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),speed*.75,"Unequip did not restore slow");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void actualAbsorptionDamageAtCrystalCapCleansesOneEffectButManualLossDoesNot(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h); equip(p,true);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,1200,1));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,1200));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,1200));
        p.setAbsorptionAmount(7);
        p.addEffect(new MobEffectInstance(ArsMiscMod.CRYSTAL_SHIELD,600,3));
        p.getPersistentData().putDouble(CrystalShieldEffect.POWER_KEY,100);
        close(h,CrystalShieldEvents.reduction(p),.8,"Focused crystal shield did not cap at 80 percent");
        h.assertTrue(p.hasEffect(MobEffects.WEAKNESS)&&p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Manual gold loss cleansed effects");
        var attacker=h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK,2,2,2);
        net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(
                net.minecraft.resources.ResourceLocation.parse("apothic_attributes:crit_chance"))
                .ifPresent(attribute -> attacker.getAttribute(attribute).setBaseValue(0));
        p.hurt(p.damageSources().mobAttack(attacker),10);
        close(h,p.getAbsorptionAmount(),5,"Capped crystal shield prevented absorption consumption");
        close(h,p.getHealth(),p.getMaxHealth(),"Absorbed damage reached real health");
        int harmful=(p.hasEffect(MobEffects.WEAKNESS)?1:0)+(p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)?1:0);
        h.assertTrue(harmful==1,"Damage must remove exactly one harmful effect");
        h.succeed();
    }
}
