package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc")
@PrefixGameTestTemplate(false)
public final class AdvancedSwordGameTests {
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void attributesAndNativeTouchScribing(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stack=ArsMiscMod.ADVANCED_ENCHANTERS_SWORD.get().getDefaultInstance();
        h.assertTrue(SpellCasterRegistry.hasCaster(stack),"Sword is not a registered caster");
        stack.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier) -> player.getAttribute(attribute).addTransientModifier(modifier));
        h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE)-8)<0.001,"Attack damage is not 8");
        h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED)-1.3)<0.001,"Attack speed is not 1.3");
        h.assertTrue(player.getAttributeValue(PerkAttributes.SPELL_DAMAGE_BONUS)==9,"Spell bonus is not exactly 9");
        h.assertTrue(player.getAttributeValue(Attributes.ARMOR)==4,"Armor bonus is not 4");
        h.assertTrue(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)==5,"Block range is not 5");
        h.assertTrue(player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)==5,"Entity range is not 5");
        int[] offhand={0};
        stack.forEachModifier(EquipmentSlot.OFFHAND,(attribute,modifier)->offhand[0]++);
        h.assertTrue(offhand[0]==0,"Sword bonuses apply in offhand");
        var book=ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        SpellCasterRegistry.from(book).setSpell(new Spell(com.hollingsworth.arsnouveau.common.spell.effect.EffectLight.INSTANCE)).saveToStack(book);
        player.setItemInHand(InteractionHand.MAIN_HAND,book);
        h.assertTrue(ArsMiscMod.ADVANCED_ENCHANTERS_SWORD.get().onScribe(h.getLevel(),BlockPos.ZERO,player,InteractionHand.MAIN_HAND,stack),"Native sword scribing failed");
        var recipe=SpellCasterRegistry.from(stack).getSpell().unsafeList();
        h.assertTrue(recipe.get(0)==MethodTouch.INSTANCE && recipe.size()==2 && recipe.get(1)==com.hollingsworth.arsnouveau.common.spell.effect.EffectLight.INSTANCE,"Native sword Touch prefix or written effect missing");
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var mana=CapabilityRegistry.getMana(player);
        mana.setMaxMana(1000); mana.setMana(1000);
        var target=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        ArsMiscMod.ADVANCED_ENCHANTERS_SWORD.get().hurtEnemy(stack,target,player);
        h.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"Sword hit did not cast its stored Touch spell");
        h.assertTrue(CapabilityRegistry.getMana(player).getCurrentMana()<1000,"Sword casting did not spend mana");
        h.succeed();
    }
}
