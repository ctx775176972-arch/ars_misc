package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsmisc.network.ToggleCastingPayload;
import net.minecraft.gametest.framework.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.gametest.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class AvadaKedavraGameTests {
    private static LivingEntity target(GameTestHelper h) {
        var mob=h.spawnWithNoFreeWill(EntityType.HUSK,2,2,4);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); mob.setHealth(100);
        return mob;
    }
    private static void cast(GameTestHelper h, LivingEntity caster, LivingEntity target) {
        var glyph=EffectAvadaKedavra.INSTANCE;
        var context=SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE,glyph),caster,ItemStack.EMPTY);
        glyph.onResolveEntity(new EntityHitResult(target),h.getLevel(),caster,new SpellStats.Builder().build(),context,new SpellResolver(context));
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void dedicatedDamageAndManaCostAreRegistered(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h); var glyph=EffectAvadaKedavra.INSTANCE;
        var source=glyph.buildDamageSource(h.getLevel(),p);
        for (var tag:java.util.List.of(DamageTypeTags.BYPASSES_ARMOR,DamageTypeTags.BYPASSES_ENCHANTMENTS,
                DamageTypeTags.BYPASSES_RESISTANCE,DamageTypeTags.BYPASSES_EFFECTS,DamageTypeTags.BYPASSES_SHIELD,
                DamageTypeTags.BYPASSES_COOLDOWN,DamageTypeTags.BYPASSES_INVULNERABILITY))
            h.assertTrue(source.is(tag),"Missing execution damage tag: "+tag);
        h.assertTrue(glyph.getDefaultManaCost()==10000 && glyph.defaultTier()==SpellTier.THREE
                && glyph.getSchools().contains(SpellSchools.NECROMANCY),"Wrong glyph defaults");
        var spell=new Spell(MethodTouch.INSTANCE,glyph);
        var resolver=new SpellResolver(SpellContext.fromEntity(spell,p,ItemStack.EMPTY));
        h.assertTrue(!resolver.canCast(p),"Insufficient mana was accepted");
        CapabilityRegistry.getMana(p).setMaxMana(30000); CapabilityRegistry.getMana(p).setMana(20000);
        h.assertTrue(resolver.canCast(p),"Sufficient mana was rejected");
        int cost=resolver.getExpendedCost(); resolver.expendMana();
        h.assertTrue(Math.abs(CapabilityRegistry.getMana(p).getCurrentMana()-(20000-cost))<.01,"Native mana was not charged");
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void lethalDamageHasOneDeathAndCorrectCaster(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h); var mob=target(h); var count=new AtomicInteger();
        mob.getAttribute(Attributes.ARMOR).setBaseValue(30);
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,1200,4)); mob.invulnerableTime=20;
        Consumer<LivingDeathEvent> death=e->{if(e.getEntity()==mob) {
            count.incrementAndGet(); h.assertTrue(e.getSource().getEntity()==p,"Missing killer attribution");
        }};
        NeoForge.EVENT_BUS.addListener(death);
        try {cast(h,p,mob);cast(h,p,mob);}
        finally {NeoForge.EVENT_BUS.unregister(death);}
        h.assertTrue(mob.isDeadOrDying()&&count.get()==1,"Target was not killed exactly once"); h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void damageCapFallsBackToCancelableDeath(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h); var mob=target(h); var count=new AtomicInteger();
        Consumer<LivingIncomingDamageEvent> cap=e->{if(e.getEntity()==mob)e.setAmount(1);};
        Consumer<LivingDeathEvent> death=e->{if(e.getEntity()==mob){count.incrementAndGet();e.setCanceled(true);mob.setHealth(7);}};
        NeoForge.EVENT_BUS.addListener(cap); NeoForge.EVENT_BUS.addListener(death);
        try {cast(h,p,mob); h.assertTrue(mob.isAlive()&&mob.getHealth()==7&&count.get()==1,"Fallback did not respect canceled death");}
        finally {NeoForge.EVENT_BUS.unregister(cap);NeoForge.EVENT_BUS.unregister(death);}
        cast(h,p,mob);
        h.assertTrue(mob.isDeadOrDying(),"Execution scope leaked after protection"); h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void cappedTargetDiesWithNormalLootEventAndAttribution(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h); var mob=target(h); var drops=new AtomicInteger();
        Consumer<LivingIncomingDamageEvent> cap=e->{if(e.getEntity()==mob)e.setAmount(1);};
        Consumer<LivingDropsEvent> loot=e->{if(e.getEntity()==mob){
            drops.incrementAndGet(); h.assertTrue(e.getSource().getEntity()==p,"Fallback lost loot attribution");
        }};
        NeoForge.EVENT_BUS.addListener(cap);NeoForge.EVENT_BUS.addListener(loot);
        try {cast(h,p,mob);}
        finally {NeoForge.EVENT_BUS.unregister(cap);NeoForge.EVENT_BUS.unregister(loot);}
        h.assertTrue(mob.isDeadOrDying()&&drops.get()==1,"Fallback did not produce one normal death/drop event");h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void totemsProtectBothLethalDamageAndCappedFallback(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h);
        for(boolean capped:new boolean[]{false,true}) {
            var mob=target(h); mob.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
            Consumer<LivingIncomingDamageEvent> cap=e->{if(capped&&e.getEntity()==mob)e.setAmount(1);};
            NeoForge.EVENT_BUS.addListener(cap);
            try {cast(h,p,mob);}
            finally {NeoForge.EVENT_BUS.unregister(cap);}
            h.assertTrue(mob.isAlive()&&mob.getHealth()>0&&mob.getOffhandItem().isEmpty(),"Totem failed or was followed by execution, capped="+capped);
        }
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void firstDeathRescueIsNotExecutedAgain(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h); var mob=target(h); var count=new AtomicInteger();
        Consumer<LivingDeathEvent> rescue=e->{if(e.getEntity()==mob){count.incrementAndGet();e.setCanceled(true);mob.setHealth(15);}};
        NeoForge.EVENT_BUS.addListener(rescue);
        try {cast(h,p,mob);}
        finally {NeoForge.EVENT_BUS.unregister(rescue);}
        h.assertTrue(mob.isAlive()&&mob.getHealth()==15&&count.get()==1,"Successful death rescue was attacked twice"); h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void barrierExhaustionProtectsOneCompleteCurse(GameTestHelper h) {
        var caster=h.spawnWithNoFreeWill(EntityType.COW,1,2,1); var p=ReactiveShieldGameTests.player(h);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow()
                .setEquippedCurio("casting_device",0,ArsMiscMod.FINAL_BARRIER.get().getDefaultInstance());
        h.assertTrue(ToggleCastingPayload.toggle(p),"Could not enable barrier");
        float health=p.getHealth();cast(h,caster,p);
        h.assertTrue(p.isAlive()&&p.getHealth()==health&&!FinalBarrier.active(p)
                &&CapabilityRegistry.getMana(p).getCurrentMana()==0,"Barrier protection mismatch: health="+p.getHealth()
                +", active="+FinalBarrier.active(p)+", mana="+CapabilityRegistry.getMana(p).getCurrentMana());
        cast(h,caster,p); h.assertTrue(p.isDeadOrDying(),"Exhausted barrier protected a second curse");h.succeed();
    }
}
