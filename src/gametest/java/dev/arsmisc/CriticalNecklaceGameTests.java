package dev.arsmisc;

import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.ModPotions;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.*;

@GameTestHolder("ars_misc")
@PrefixGameTestTemplate(false)
public final class CriticalNecklaceGameTests {
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void criticalDamageAndSharedProcs(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow();
        var necklace=ArsMiscMod.CRITICAL_NECKLACE.get();
        var stack=necklace.getDefaultInstance();
        inv.setEquippedCurio("necklace",0,stack);
        inv.setEquippedCurio("belt",0,ArsMiscMod.CRITICAL_BELT.get().getDefaultInstance());
        var slot=new SlotContext("necklace",player,0,false,true);
        double before=player.getAttributeValue(ModRegistry.SPELL_CRIT_DAMAGE);
        var modifiers=necklace.getAttributeModifiers(slot,ResourceLocation.fromNamespaceAndPath("ars_misc","test_necklace"),stack);
        modifiers.forEach((attribute,modifier) -> player.getAttribute(attribute).addTransientModifier(modifier));
        h.assertTrue(Math.abs(player.getAttributeValue(ModRegistry.SPELL_CRIT_DAMAGE)-before-1.0)<0.00001,"Necklace did not add 100 percentage points of crit damage");
        h.assertTrue(necklace.getAttributeModifiers(new SlotContext("ring",player,0,false,true),ResourceLocation.fromNamespaceAndPath("ars_misc","test"),stack).isEmpty(),"Wrong slot provided crit damage");
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(1);
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,EffectHarm.INSTANCE),player,ItemStack.EMPTY);
        var resolver=new SpellResolver(context);
        var target=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        float health=target.getHealth();
        EffectHarm.INSTANCE.attemptDamage(h.getLevel(),player,new SpellStats.Builder().build(),context,resolver,target,
            EffectHarm.INSTANCE.buildDamageSource(h.getLevel(),player),2);
        h.assertTrue(Math.abs((health-target.getHealth())-2*(1+before+1))<0.001,"Sauce critical did not use necklace damage bonus");
        var effect=player.getEffect(ModPotions.SPELL_DAMAGE_EFFECT);
        h.assertTrue(effect!=null && effect.getAmplifier()==1 && effect.getDuration()==200,"Crit did not grant Spell Damage II for 200 ticks");
        h.assertTrue(player.hasEffect(ModPotions.MANA_REGEN_EFFECT),"Belt lost its proc when necklace was worn");
        player.removeEffect(ModPotions.SPELL_DAMAGE_EFFECT);
        player.addEffect(new MobEffectInstance(ModPotions.SPELL_DAMAGE_EFFECT,40,0));
        var second=h.spawnWithNoFreeWill(EntityType.COW,2,2,1);
        EffectHarm.INSTANCE.attemptDamage(h.getLevel(),player,new SpellStats.Builder().build(),context,resolver,second,
            EffectHarm.INSTANCE.buildDamageSource(h.getLevel(),player),1);
        h.assertTrue(player.getEffect(ModPotions.SPELL_DAMAGE_EFFECT).getDuration()==200,"Repeated critical did not refresh duration");
        player.removeEffect(ModPotions.SPELL_DAMAGE_EFFECT);
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(0);
        var ordinary=h.spawnWithNoFreeWill(EntityType.COW,3,2,1);
        EffectHarm.INSTANCE.attemptDamage(h.getLevel(),player,new SpellStats.Builder().build(),context,resolver,ordinary,
            EffectHarm.INSTANCE.buildDamageSource(h.getLevel(),player),1);
        h.assertTrue(!player.hasEffect(ModPotions.SPELL_DAMAGE_EFFECT),"Noncritical hit granted Spell Damage");
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(1);
        var immune=h.spawnWithNoFreeWill(EntityType.COW,4,2,1);
        immune.setInvulnerable(true);
        EffectHarm.INSTANCE.attemptDamage(h.getLevel(),player,new SpellStats.Builder().build(),context,resolver,immune,
            EffectHarm.INSTANCE.buildDamageSource(h.getLevel(),player),1);
        h.assertTrue(!player.hasEffect(ModPotions.SPELL_DAMAGE_EFFECT),"Failed damage granted Spell Damage");
        inv.setEquippedCurio("necklace",0,ItemStack.EMPTY);
        var unadorned=h.spawnWithNoFreeWill(EntityType.COW,2,2,2);
        EffectHarm.INSTANCE.attemptDamage(h.getLevel(),player,new SpellStats.Builder().build(),context,resolver,unadorned,
            EffectHarm.INSTANCE.buildDamageSource(h.getLevel(),player),1);
        h.assertTrue(!player.hasEffect(ModPotions.SPELL_DAMAGE_EFFECT),"Unequipped necklace granted Spell Damage");
        h.succeed();
    }
}
