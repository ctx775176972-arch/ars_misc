package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

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
    public static void equippedSpellHitAppliesDecay(GameTestHelper helper) {
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,EffectHarm.INSTANCE),player,ItemStack.EMPTY);
        var resolver=new SpellResolver(context);
        var unmarked=helper.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        EffectHarm.INSTANCE.attemptDamage(helper.getLevel(),player,new SpellStats.Builder().build(),context,resolver,
            unmarked,EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(),player),1);
        helper.assertTrue(!unmarked.hasEffect(ArsMiscMod.DECAY), "Unequipped hit applied decay");
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("ring",0,ArsMiscMod.DECAY_RING.get().getDefaultInstance());
        var marked=helper.spawnWithNoFreeWill(EntityType.COW,2,2,1);
        EffectHarm.INSTANCE.attemptDamage(helper.getLevel(),player,new SpellStats.Builder().build(),context,resolver,
            marked,EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(),player),1);
        var effect=marked.getEffect(ArsMiscMod.DECAY);
        helper.assertTrue(effect!=null && effect.getDuration()==600,"Equipped spell hit did not apply 30s decay");
        helper.succeed();
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
