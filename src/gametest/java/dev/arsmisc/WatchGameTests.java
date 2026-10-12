package dev.arsmisc;

import dev.arsmisc.item.EnchantersPocketWatchItem;
import dev.arsmisc.menu.EnchantersPocketWatchMenu;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectLight;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.SlotContext;

@GameTestHolder("ars_misc")
@PrefixGameTestTemplate(false)
public final class WatchGameTests {
    private static ItemStack watch() {
        var stack=ArsMiscMod.ENCHANTERS_POCKET_WATCH.get().getDefaultInstance();
        new SpellCaster().setSpell(new Spell(MethodSelf.INSTANCE,EffectLifeSense.INSTANCE)).saveToStack(stack);
        return stack;
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void nativeScribingInsertsSelf(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stack=ArsMiscMod.ENCHANTERS_POCKET_WATCH.get().getDefaultInstance();
        var item=ArsMiscMod.ENCHANTERS_POCKET_WATCH.get();
        h.assertTrue(SpellCasterRegistry.hasCaster(stack),"Watch not registered as native caster");
        var book=ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        SpellCasterRegistry.from(book).setSpell(new Spell(EffectLight.INSTANCE)).saveToStack(book);
        player.setItemInHand(InteractionHand.MAIN_HAND,book);
        h.assertTrue(item.onScribe(h.getLevel(),BlockPos.ZERO,player,InteractionHand.MAIN_HAND,stack),"Native scribing failed");
        var spell=item.getSpellCaster(stack).getSpell();
        h.assertTrue(spell.getCastMethod()==MethodSelf.INSTANCE && spell.unsafeList().size()==2,"Scribing failed to prepend Self");
        h.assertTrue(!item.isScribedSpellValid(item.getSpellCaster(stack),player,InteractionHand.MAIN_HAND,stack,
            new Spell(MethodSelf.INSTANCE,EffectLight.INSTANCE)),"Accepted an existing cast form");
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void playerUsesNativeMana(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stack=watch();
        var item=ArsMiscMod.ENCHANTERS_POCKET_WATCH.get();
        var mana=CapabilityRegistry.getMana(player);
        mana.setMaxMana(1000);
        mana.setMana(0);
        player.tickCount=0;
        item.curioTick(new SlotContext("charm",player,0,false,true),stack);
        h.assertTrue(!player.hasEffect(ArsMiscMod.LIFE_SENSE),"Watch cast without player mana");
        // A failed cast still respects the original watch's configured schedule.
        mana.setMana(100);
        item.curioTick(new SlotContext("charm",player,0,false,true),stack);
        h.assertTrue(!player.hasEffect(ArsMiscMod.LIFE_SENSE),"Failed cast was queued immediately");
        var funded=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var fundedMana=CapabilityRegistry.getMana(funded);
        fundedMana.setMaxMana(1000);
        fundedMana.setMana(100);
        double before=fundedMana.getCurrentMana();
        funded.tickCount=0;
        item.curioTick(new SlotContext("charm",funded,0,false,true),watch());
        h.assertTrue(funded.hasEffect(ArsMiscMod.LIFE_SENSE) && CapabilityRegistry.getMana(funded).getCurrentMana()<before,"Funded cast: effect=" + funded.hasEffect(ArsMiscMod.LIFE_SENSE) + ", mana=" + CapabilityRegistry.getMana(funded).getCurrentMana() + ", before=" + before);
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void nonplayerKeepsCasterAndSharedGate(GameTestHelper h) {
        var entity=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        h.assertTrue(CapabilityRegistry.getMana(entity)==null,"Test entity unexpectedly has mana");
        var item=ArsMiscMod.ENCHANTERS_POCKET_WATCH.get();
        var stack=watch();
        entity.tickCount=0;
        var slot=new SlotContext("charm",entity,0,false,true);
        item.curioTick(slot,stack);
        h.assertTrue(entity.hasEffect(ArsMiscMod.LIFE_SENSE),"Nonplayer casting did not affect real wearer");
        entity.removeEffect(ArsMiscMod.LIFE_SENSE);
        item.curioTick(slot,watch());
        h.assertTrue(!entity.hasEffect(ArsMiscMod.LIFE_SENSE),"Second watch bypassed shared 5 second gate");
        var paused=h.spawnWithNoFreeWill(EntityType.COW,2,2,1);
        paused.tickCount=0;
        var disabled=watch();
        EnchantersPocketWatchItem.setEnabled(disabled,false);
        item.curioTick(new SlotContext("charm",paused,0,false,true),disabled);
        h.assertTrue(!paused.hasEffect(ArsMiscMod.LIFE_SENSE),"Disabled watch cast");
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void menuEditsOnlyOpeningStack(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stack=watch();
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var menu=new EnchantersPocketWatchMenu(1,player.getInventory(),InteractionHand.MAIN_HAND);
        h.assertTrue(menu.clickMenuButton(player,105) && EnchantersPocketWatchItem.interval(stack)==5,"Menu did not set interval");
        h.assertTrue(!menu.clickMenuButton(player,104),"Menu accepted out of range interval");
        h.assertTrue(menu.clickMenuButton(player,1) && !EnchantersPocketWatchItem.enabled(stack),"Menu toggle failed");
        player.setItemInHand(InteractionHand.MAIN_HAND,watch());
        h.assertTrue(!menu.stillValid(player) && !menu.clickMenuButton(player,130),"Menu edited after held item changed");
        h.succeed();
    }
}
