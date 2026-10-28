package dev.arsmisc;

import dev.arsmisc.item.EnchantersFieldItem;
import dev.arsmisc.item.EnchantersPocketWatchItem;
import dev.arsmisc.menu.EnchantersFieldMenu;
import dev.arsmisc.network.ToggleCastingPayload;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.setup.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class FieldGameTests {
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void castingDevicesHaveOneDedicatedSlot(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();
        h.assertTrue(inventory.getStacksHandler("casting_device").orElseThrow().getStacks().getSlots()==1,
                "Casting devices must share one dedicated slot");
        var deviceTag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios","casting_device"));
        var charmTag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios","charm"));
        for(var item:java.util.List.of(ArsMiscMod.ENCHANTERS_FIELD.get(),ArsMiscMod.ENCHANTERS_POCKET_WATCH.get(),ArsMiscMod.FINAL_BARRIER.get())) {
            var stack=item.getDefaultInstance();
            h.assertTrue(stack.is(deviceTag) && !stack.is(charmTag),"Casting device still uses the charm slot: "+stack);
        }
        h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void upkeepFollowsRadiusRegardlessOfSpellIntervalAndDiscounts(GameTestHelper h) {
        var item=ArsMiscMod.ENCHANTERS_FIELD.get();
        for(int radius:new int[]{1,15,80,160}) {
            var p=h.makeMockPlayer(GameType.SURVIVAL);var stack=item.getDefaultInstance();
            var mana=CapabilityRegistry.getMana(p);mana.setMaxMana(1000);mana.setMana(500);
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,ArsMiscMod.NETHERITE_MAGE_CHESTPLATE.get().getDefaultInstance());
            EnchantersFieldItem.setRadius(stack,radius);EnchantersFieldItem.setInterval(stack,3600);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,
                    data->data.putLong("field_next",h.getLevel().getGameTime()+72000));
            p.tickCount=0;var slot=new SlotContext("casting_device",p,0,false,true);
            item.curioTick(slot,stack);
            h.assertTrue(mana.getCurrentMana()==500-radius,"Radius upkeep changed with empty spell, interval or discount");
            EnchantersFieldItem.setEnabled(stack,false);item.curioTick(slot,stack);
            EnchantersFieldItem.setEnabled(stack,true);item.curioTick(slot,stack);
            h.assertTrue(mana.getCurrentMana()==500-radius,"Repeated tick or toggle charged the same second twice");
            p.getPersistentData().putLong(EnchantersFieldItem.NEXT_UPKEEP,h.getLevel().getGameTime());
            item.curioTick(slot,stack);
            h.assertTrue(mana.getCurrentMana()==500-radius*2,"Long spell interval suppressed ongoing upkeep");
        }
        h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void upkeepAndNativePerTargetSpellCostsAreBothPaid(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(16,2,16)));
        var first=h.spawnWithNoFreeWill(EntityType.COW,16,2,17);var second=h.spawnWithNoFreeWill(EntityType.COW,17,2,16);
        var item=ArsMiscMod.ENCHANTERS_FIELD.get();var stack=field();
        var mana=CapabilityRegistry.getMana(p);mana.setMaxMana(1000);mana.setMana(500);
        int cost=new dev.arsmisc.spell.FieldSpellResolver(SpellContext.fromEntity(item.getSpellCaster(stack).getSpell(),p,stack)).getExpendedCost();
        int count=item.targets(p,stack).size();h.assertTrue(count==2,"Test requires exactly two eligible targets");
        float a=first.getHealth(),b=second.getHealth();p.tickCount=0;
        item.curioTick(new SlotContext("casting_device",p,0,false,true),stack);
        h.assertTrue(first.getHealth()<a&&second.getHealth()<b,"Field did not cast on both targets");
        h.assertTrue(Math.abs(mana.getCurrentMana()-(500-80-count*cost))<.001,"Upkeep replaced or duplicated per-target spell costs");
        h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void unpaidUpkeepDisablesWithoutCastingAndDoesNotAutoRestart(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(16,2,16)));
        var target=h.spawnWithNoFreeWill(EntityType.COW,16,2,17);float health=target.getHealth();
        var item=ArsMiscMod.ENCHANTERS_FIELD.get();var stack=field();var slot=new SlotContext("casting_device",p,0,false,true);
        var mana=CapabilityRegistry.getMana(p);mana.setMaxMana(1000);mana.setMana(79);p.tickCount=0;
        item.curioTick(slot,stack);
        h.assertTrue(!EnchantersFieldItem.enabled(stack)&&target.getHealth()==health&&mana.getCurrentMana()==79,
                "Unaffordable upkeep cast a spell or partially consumed mana");
        mana.setMana(500);item.curioTick(slot,stack);
        h.assertTrue(!EnchantersFieldItem.enabled(stack)&&mana.getCurrentMana()==500&&target.getHealth()==health,"Mana recovery automatically restarted field");
        EnchantersFieldItem.setEnabled(stack,true);item.curioTick(slot,stack);
        h.assertTrue(EnchantersFieldItem.enabled(stack)&&target.getHealth()<health&&mana.getCurrentMana()<420,"Manual restart did not pay upkeep and cast cost");
        h.succeed();
    }
    private static ItemStack field() {
        var s=ArsMiscMod.ENCHANTERS_FIELD.get().getDefaultInstance();
        new SpellCaster().setSpell(new Spell(EffectHarm.INSTANCE)).saveToStack(s);return s;
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void scribingRemovesFormAndMenuChecksHeldStack(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);var s=field();var item=ArsMiscMod.ENCHANTERS_FIELD.get();
        var book=ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(book).setSpell(new Spell(MethodProjectile.INSTANCE,EffectHarm.INSTANCE)).saveToStack(book);
        p.setItemInHand(InteractionHand.MAIN_HAND,book);
        h.assertTrue(item.onScribe(h.getLevel(),BlockPos.ZERO,p,InteractionHand.MAIN_HAND,s),"Scribing failed");
        var spell=item.getSpellCaster(s).getSpell();h.assertTrue(spell.getCastMethod()==null && spell.unsafeList().size()==1 && spell.get(0)==EffectHarm.INSTANCE,"Initial cast form was not removed");
        p.setItemInHand(InteractionHand.MAIN_HAND,s);var menu=new EnchantersFieldMenu(1,p.getInventory(),InteractionHand.MAIN_HAND);
        h.assertTrue(menu.clickMenuButton(p,101) && menu.clickMenuButton(p,10015),"Menu config failed");
        h.assertTrue(EnchantersFieldItem.interval(s)==1 && EnchantersFieldItem.radiusSetting(s)==15,"Wrong config");
        h.assertTrue(!menu.clickMenuButton(p,10161),"Accepted invalid radius");
        p.setItemInHand(InteractionHand.MAIN_HAND,field());h.assertTrue(!menu.clickMenuButton(p,1),"Menu edited replacement item");h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void noTargetsStillPayUpkeepAndOwnedTargetsAreExcluded(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(16,2,16)));
        var s=field();var item=ArsMiscMod.ENCHANTERS_FIELD.get();var mana=CapabilityRegistry.getMana(p);mana.setMaxMana(1000);mana.setMana(500);
        var wolf=h.spawnWithNoFreeWill(EntityType.WOLF,16,2,17);wolf.setTame(true,true);wolf.setOwnerUUID(p.getUUID());
        h.assertTrue(!EnchantersFieldItem.validTarget(p,wolf) && !EnchantersFieldItem.validTarget(p,p),"Owned target or player accepted");
        // The entity tag allows addon summons without OwnableEntity to be excluded by a datapack.
        p.tickCount=0;double before=mana.getCurrentMana();item.curioTick(new SlotContext("casting_device",p,0,false,true),s);
        h.assertTrue(mana.getCurrentMana()==before-80,"Empty field did not charge exactly its maintenance fee");h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void targetedCastingSpendsManaAndDoesNotRequireSight(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(16,2,16)));
        var target=h.spawnWithNoFreeWill(EntityType.COW,16,2,18);var mana=CapabilityRegistry.getMana(p);mana.setMaxMana(1000);mana.setMana(500);
        h.setBlock(new BlockPos(16,3,17),Blocks.STONE);var item=ArsMiscMod.ENCHANTERS_FIELD.get();var s=field();
        double before=mana.getCurrentMana();float health=target.getHealth();p.tickCount=0;
        item.curioTick(new SlotContext("casting_device",p,0,false,true),s);
        h.assertTrue(target.getHealth()<health && mana.getCurrentMana()<before,"Native targeted cast failed through wall");
        var unfunded=field();mana.setMana(0);health=target.getHealth();item.curioTick(new SlotContext("casting_device",p,0,false,true),unfunded);
        h.assertTrue(target.getHealth()==health,"Cast without mana");
        p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(5);h.assertTrue(EnchantersFieldItem.radius(p,s)==8,"Default radius depended on reach");
        EnchantersFieldItem.setRadius(s,20);h.assertTrue(EnchantersFieldItem.radius(p,s)==2,"Manual radius not applied");h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void nativeFiltersStopRightHandEffectsButSpendMana(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);var target=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        var filter=new AbstractFilter(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_misc","test_reject"),"Reject") {
            @Override public boolean shouldResolveOnBlock(net.minecraft.world.phys.BlockHitResult hit,net.minecraft.world.level.Level level) { return false; }
            @Override public boolean shouldResolveOnEntity(net.minecraft.world.phys.EntityHitResult hit,net.minecraft.world.level.Level level) { return false; }
        };
        var spell=new Spell(filter,EffectHarm.INSTANCE);
        var mana=CapabilityRegistry.getMana(p);mana.setMaxMana(1000);mana.setMana(500);double before=mana.getCurrentMana();float health=target.getHealth();
        var resolver=new dev.arsmisc.spell.FieldSpellResolver(SpellContext.fromEntity(spell,p,ItemStack.EMPTY));resolver.withSilent(true);resolver.castOnTarget(p,target);
        h.assertTrue(target.getHealth()==health && mana.getCurrentMana()<before,"Filter skipped native cast cost or did not stop Harm");h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void extendedRadiusCastsBeyondReachAndCapsAtSixteen(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(16,2,16)));
        p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(3);
        var target=h.spawnWithNoFreeWill(EntityType.COW,28,2,16);
        var outside=h.spawnWithNoFreeWill(EntityType.COW,0,2,0);
        var item=ArsMiscMod.ENCHANTERS_FIELD.get();var s=field();
        h.assertTrue(!item.targets(p,s).contains(target),"Default eight-block radius accepted distant target");
        EnchantersFieldItem.setRadius(s,999);
        h.assertTrue(EnchantersFieldItem.radius(p,s)==16 && item.targets(p,s).contains(target) && !item.targets(p,s).contains(outside),"Maximum radius or reach independence failed");
        var mana=CapabilityRegistry.getMana(p);mana.setMaxMana(1000);mana.setMana(500);
        float health=target.getHealth();double before=mana.getCurrentMana();p.tickCount=0;
        item.curioTick(new SlotContext("casting_device",p,0,false,true),s);
        h.assertTrue(target.getHealth()<health && mana.getCurrentMana()<before && EnchantersFieldItem.enabled(s),"Direct effect beyond interaction range failed");h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void togglesOnlyActiveEquippedItems(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);var s=field();p.setItemInHand(InteractionHand.MAIN_HAND,s);
        h.assertTrue(!ToggleCastingPayload.toggle(p) && EnchantersFieldItem.enabled(s),"Held field toggled");
        var inv=CuriosApi.getCuriosInventory(p).orElseThrow();inv.setEquippedCurio("casting_device",0,s);
        boolean toggled=ToggleCastingPayload.toggle(p);
        h.assertTrue(toggled && !EnchantersFieldItem.enabled(inv.getStacksHandler("casting_device").orElseThrow().getStacks().getStackInSlot(0)),"Field toggle failed: found="+toggled+", stack="+inv.getStacksHandler("casting_device").orElseThrow().getStacks().getStackInSlot(0)+", enabled="+EnchantersFieldItem.enabled(inv.getStacksHandler("casting_device").orElseThrow().getStacks().getStackInSlot(0)));
        h.assertTrue(ToggleCastingPayload.toggle(p) && EnchantersFieldItem.enabled(inv.getStacksHandler("casting_device").orElseThrow().getStacks().getStackInSlot(0)),"Shared key did not re-enable field");
        var watch=ArsMiscMod.ENCHANTERS_POCKET_WATCH.get().getDefaultInstance();inv.setEquippedCurio("casting_device",0,watch);
        h.assertTrue(ToggleCastingPayload.toggle(p) && !EnchantersPocketWatchItem.enabled(inv.getStacksHandler("casting_device").orElseThrow().getStacks().getStackInSlot(0)),"Watch toggle failed");h.succeed();
    }

    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void sharedToggleSynchronizesWatchAndFieldAndIgnoresInactiveSlots(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();
        var handler=inventory.getStacksHandler("casting_device").orElseThrow();
        handler.grow(1);
        var watch=ArsMiscMod.ENCHANTERS_POCKET_WATCH.get().getDefaultInstance();
        var field=ArsMiscMod.ENCHANTERS_FIELD.get().getDefaultInstance();
        EnchantersPocketWatchItem.setEnabled(watch,true);
        EnchantersFieldItem.setEnabled(field,false);
        inventory.setEquippedCurio("casting_device",0,watch);
        inventory.setEquippedCurio("casting_device",1,field);
        h.assertTrue(ToggleCastingPayload.toggle(player),"Shared toggle found no equipped accessories");
        h.assertTrue(!EnchantersPocketWatchItem.enabled(watch) && !EnchantersFieldItem.enabled(field),"Mixed states did not all switch off");
        h.assertTrue(ToggleCastingPayload.toggle(player),"Shared toggle could not turn accessories on");
        h.assertTrue(EnchantersPocketWatchItem.enabled(watch) && EnchantersFieldItem.enabled(field),"Not all accessories switched on");
        inventory.setSlotActive("casting_device",1,false);
        ToggleCastingPayload.toggle(player);
        h.assertTrue(!EnchantersPocketWatchItem.enabled(watch) && EnchantersFieldItem.enabled(field),"Inactive slot was toggled");
        handler.getStacks().setStackInSlot(0,ItemStack.EMPTY);
        h.assertTrue(!ToggleCastingPayload.toggle(player),"Inactive-only equipment was toggled");
        h.succeed();
    }
}
