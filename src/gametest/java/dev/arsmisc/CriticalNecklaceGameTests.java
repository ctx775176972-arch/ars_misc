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
        // This test measures Sauce's critical multiplier, not Apothic Attributes' separate random critical.
        net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse("apothic_attributes:crit_chance"))
                .ifPresent(attribute -> player.getAttribute(attribute).setBaseValue(0));
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow();
        var necklace=ArsMiscMod.CRITICAL_NECKLACE.get();
        var stack=necklace.getDefaultInstance();
        inv.setEquippedCurio("necklace",0,stack);
        inv.setEquippedCurio("head",0,ArsMiscMod.CRITICAL_GLASSES.get().getDefaultInstance());
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
        var mana=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player);
        mana.setMaxMana(1000);mana.setMana(0);
        EffectHarm.INSTANCE.attemptDamage(h.getLevel(),player,new SpellStats.Builder().build(),context,resolver,target,
            EffectHarm.INSTANCE.buildDamageSource(h.getLevel(),player),2);
        h.assertTrue(Math.abs((health-target.getHealth())-2*(1+before+1))<0.001,
                "Sauce critical did not use necklace damage bonus: actual=" + (health-target.getHealth()) + " expected=" + 2*(1+before+1));
        h.assertTrue(!player.hasEffect(ModPotions.SPELL_DAMAGE_EFFECT),"Necklace retained the retired critical spell-damage proc");
        h.assertTrue(mana.getCurrentMana()>0&&!player.hasEffect(ModPotions.MANA_REGEN_EFFECT),"Glasses did not refund mana alongside necklace or retained the old effect");
        player.removeEffect(ModPotions.SPELL_DAMAGE_EFFECT);
        player.addEffect(new MobEffectInstance(ModPotions.SPELL_DAMAGE_EFFECT,40,0));
        var second=h.spawnWithNoFreeWill(EntityType.COW,2,2,1);
        EffectHarm.INSTANCE.attemptDamage(h.getLevel(),player,new SpellStats.Builder().build(),context,resolver,second,
            EffectHarm.INSTANCE.buildDamageSource(h.getLevel(),player),1);
        h.assertTrue(player.getEffect(ModPotions.SPELL_DAMAGE_EFFECT).getDuration()==40
                && player.getEffect(ModPotions.SPELL_DAMAGE_EFFECT).getAmplifier()==0,"Necklace altered an independently granted spell-damage effect");
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

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void manaPowerRoundsDownRefreshesAndClearsOnRemoval(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var mana=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player);
        var attribute=player.getAttribute(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.SPELL_DAMAGE_BONUS);
        double baseline=attribute.getValue();
        var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();
        mana.setMaxMana(300);
        NecklaceManaPower.refresh(player);
        h.assertTrue(attribute.getValue()==baseline,"Unequipped necklace granted spell power");
        inventory.setEquippedCurio("necklace",0,ArsMiscMod.CRITICAL_NECKLACE.get().getDefaultInstance());
        for(int maximum:new int[]{0,149,150,299,300,449,450,1500,299}) {
            mana.setMaxMana(maximum);
            NecklaceManaPower.refresh(player);
            NecklaceManaPower.refresh(player);
            h.assertTrue(attribute.getValue()==baseline+maximum/150,"Incorrect or accumulating bonus at maximum mana " + maximum);
        }
        mana.setMana(0);
        NecklaceManaPower.refresh(player);
        h.assertTrue(attribute.getValue()==baseline+1,"Current mana changed a maximum-mana bonus");
        var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        NecklaceManaPower.tooltip(new net.neoforged.neoforge.event.entity.player.ItemTooltipEvent(
                ArsMiscMod.CRITICAL_NECKLACE.get().getDefaultInstance(),player,lines,net.minecraft.world.item.TooltipFlag.NORMAL,
                net.minecraft.world.item.Item.TooltipContext.of(h.getLevel())));
        h.assertTrue(lines.stream().anyMatch(line->line.getString().contains("299")&&line.getString().contains("1")),"Dynamic tooltip did not show the current mana and integer bonus");
        inventory.setSlotActive("necklace",0,false);
        NecklaceManaPower.refresh(player);
        h.assertTrue(attribute.getValue()==baseline,"Inactive necklace slot retained spell power");
        inventory.setSlotActive("necklace",0,true);
        NecklaceManaPower.refresh(player);
        h.assertTrue(attribute.getValue()==baseline+1,"Reactivated necklace did not restore spell power");
        inventory.setEquippedCurio("necklace",0,ItemStack.EMPTY);
        NecklaceManaPower.refresh(player);
        h.assertTrue(attribute.getValue()==baseline,"Removed necklace retained spell power");
        h.succeed();
    }
}
