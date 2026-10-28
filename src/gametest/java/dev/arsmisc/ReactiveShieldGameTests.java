package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.event.ReactiveEvents;
import com.hollingsworth.arsnouveau.common.items.data.ReactiveCasterData;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.*;
import dev.arsmisc.spell.ReactiveShieldCasting;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class ReactiveShieldGameTests {
    static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"shield-test");
        var p = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault()) {
            // The test starts after the vanilla five-tick shield raise delay.
            @Override public boolean isBlocking() { return isUsingItem() && ReactiveShieldCasting.isShield(getUseItem()); }
        };
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        p.setGameMode(GameType.SURVIVAL); p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,2)));
        try {
            var spawnProtection = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnProtection.setAccessible(true); spawnProtection.setInt(p,0);
        } catch (ReflectiveOperationException problem) { throw new IllegalStateException(problem); }
        p.setYRot(0);p.setYHeadRot(0);p.setXRot(0);
        var mana = CapabilityRegistry.getMana(p);mana.setMaxMana(1000);mana.setMana(500);
        return p;
    }
    private static ItemStack reactive(GameTestHelper h, Item item, boolean healing) {
        var stack = item.getDefaultInstance();
        stack.enchant(h.getLevel().holderOrThrow(EnchantmentRegistry.REACTIVE_ENCHANTMENT),4);
        new ReactiveCasterData(0,"",false,"",1).setSpell(new Spell(MethodSelf.INSTANCE,
                healing ? EffectHeal.INSTANCE : EffectRegeneration.INSTANCE)).saveToStack(stack);
        return stack;
    }
    private static net.minecraft.world.entity.animal.Cow attacker(GameTestHelper h) {
        return h.spawnWithNoFreeWill(EntityType.COW,2,2,4);
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void offhandBlockCastsWithNativeManaAndShortCooldown(GameTestHelper h) {
        var p = player(h);var shield = reactive(h,Items.SHIELD,false);var sword = new ItemStack(Items.DIAMOND_SWORD);
        p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.setItemInHand(InteractionHand.OFF_HAND,shield);p.startUsingItem(InteractionHand.OFF_HAND);
        var attacker = attacker(h);p.hurt(p.damageSources().mobAttack(attacker),4);
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION) && p.getHealth()==20,"Full block did not cast");
        double mana = CapabilityRegistry.getMana(p).getCurrentMana();h.assertTrue(mana<500,"Reactive cast was free");
        h.assertTrue(p.getMainHandItem()==sword && p.getUseItem()==shield && !attacker.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Wrong caster, tool or self target");
        p.removeEffect(net.minecraft.world.effect.MobEffects.REGENERATION);p.invulnerableTime=0;
        p.hurt(p.damageSources().mobAttack(attacker),4);
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION) && CapabilityRegistry.getMana(p).getCurrentMana()==mana,"Cooldown bypassed");
        h.runAfterDelay(10,()->{
            p.invulnerableTime=0;p.hurt(p.damageSources().mobAttack(attacker),4);
            h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Cooldown never expired");h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void cancelledOrZeroBlockAndInsufficientManaDoNotCast(GameTestHelper h) {
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,reactive(h,Items.SHIELD,false));p.startUsingItem(InteractionHand.OFF_HAND);
        var attacker=attacker(h);
        boolean[] observed={false};
        java.util.function.Consumer<LivingShieldBlockEvent> cancel=event->{if(event.getEntity()==p){observed[0]=true;event.setCanceled(true);}};
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,cancel);
        try {p.hurt(p.damageSources().mobAttack(attacker),4);} finally {NeoForge.EVENT_BUS.unregister(cancel);}
        h.assertTrue(observed[0],"Test attack did not reach block processing");
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Cancelled block cast");
        p.invulnerableTime=0;
        java.util.function.Consumer<LivingShieldBlockEvent> unblock=event->{if(event.getEntity()==p)event.setBlocked(false);};
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,unblock);
        try {p.hurt(p.damageSources().mobAttack(attacker),4);} finally {NeoForge.EVENT_BUS.unregister(unblock);}
        h.assertTrue(p.getHealth()<20 && !p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Unblocked hit cast");
        p.invulnerableTime=0;
        java.util.function.Consumer<LivingShieldBlockEvent> zero=event->{if(event.getEntity()==p)event.setBlockedDamage(0);};
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,zero);
        try {p.hurt(p.damageSources().mobAttack(attacker),4);} finally {NeoForge.EVENT_BUS.unregister(zero);}
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Zero damage block cast");
        CapabilityRegistry.getMana(p).setMana(0);p.invulnerableTime=0;p.hurt(p.damageSources().mobAttack(attacker),4);
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Shield cast without mana");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void healingRunsAfterPartialBlockDamageOnNativeEnchantersShield(GameTestHelper h) {
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,reactive(h,ItemsRegistry.ENCHANTERS_SHIELD.get(),true));p.startUsingItem(InteractionHand.MAIN_HAND);
        java.util.function.Consumer<LivingShieldBlockEvent> partial=event->{if(event.getEntity()==p)event.setBlockedDamage(2);};
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,partial);
        try {p.hurt(p.damageSources().mobAttack(attacker(h)),6);} finally {NeoForge.EVENT_BUS.unregister(partial);}
        h.assertTrue(p.getHealth()>16,"Healing ran before partial damage or did not cast");
        h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana()<500,"Partial block did not spend mana");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void shieldLeftClickDisabledButBowReactionUnchanged(GameTestHelper h) {
        var p=player(h);var shield=reactive(h,Items.SHIELD,false);p.setItemInHand(InteractionHand.MAIN_HAND,shield);
        ReactiveEvents.castSpell(p,shield);
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Shield retained left click trigger");
        var bow=reactive(h,Items.BOW,false);p.setItemInHand(InteractionHand.MAIN_HAND,bow);ReactiveEvents.castSpell(p,bow);
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Bow reaction changed");h.succeed();
    }
}
