package dev.arsmisc;

import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsmisc.item.FinalBarrierItem;
import dev.arsmisc.network.ToggleCastingPayload;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class FinalBarrierGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var p = ReactiveShieldGameTests.player(h);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); p.setHealth(100);
        p.getAttribute(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MAX_MANA).setBaseValue(10000);
        var mana = CapabilityRegistry.getMana(p); mana.setMaxMana(10000); mana.setMana(5000);
        return p;
    }
    private static ItemStack equip(net.minecraft.world.entity.player.Player player) {
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        inventory.setEquippedCurio("casting_device", 0, ArsMiscMod.FINAL_BARRIER.get().getDefaultInstance());
        return inventory.getStacksHandler("casting_device").orElseThrow().getStacks().getStackInSlot(0);
    }
    private static void enable(GameTestHelper h, net.minecraft.world.entity.player.Player player) {
        equip(player);
        h.assertTrue(ToggleCastingPayload.toggle(player) && FinalBarrier.active(player), "Shared key did not activate equipped barrier");
    }
    private static double mana(net.minecraft.world.entity.player.Player player) { return CapabilityRegistry.getMana(player).getCurrentMana(); }
    private static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.002, message + ": " + actual + " != " + expected);
    }
    private static void hurt(GameTestHelper h, net.minecraft.world.entity.player.Player player, float damage) {
        var attacker = h.spawnWithNoFreeWill(EntityType.HUSK, 2, 2, 4);
        net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(
                net.minecraft.resources.ResourceLocation.parse("apothic_attributes:crit_chance"))
                .ifPresent(attribute -> attacker.getAttribute(attribute).setBaseValue(0));
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(attacker), damage);
        attacker.discard();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void barrierUsesExistingKeyAndHasNoRightClickActivation(GameTestHelper h) {
        var p = player(h); var held = ArsMiscMod.FINAL_BARRIER.get().getDefaultInstance();
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        ArsMiscMod.FINAL_BARRIER.get().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(!FinalBarrierItem.enabled(held) && !ToggleCastingPayload.toggle(p), "Held or right-clicked barrier became active");
        var stack = equip(p);
        h.assertTrue(!FinalBarrierItem.enabled(stack), "New barrier should start disabled");
        h.assertTrue(ToggleCastingPayload.toggle(p) && FinalBarrierItem.enabled(stack), "Shared key could not enable barrier");
        close(h, mana(p), 4990, "Initial upkeep was not charged");
        FinalBarrier.tick(p); close(h, mana(p), 4990, "Same tick charged upkeep twice");
        p.doTick();
        h.assertTrue(FinalBarrier.active(p), "Native equipment ticking disabled the barrier");
        h.assertTrue(ToggleCastingPayload.toggle(p) && !FinalBarrierItem.enabled(stack), "Shared key could not disable barrier");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void goldenHeartsAreConsumedBeforeManaProtection(GameTestHelper h) {
        var p = player(h); enable(h, p);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1));
        double initial = mana(p);
        hurt(h, p, 5);
        close(h, p.getAbsorptionAmount(), 3, "Golden hearts were not consumed first");
        close(h, mana(p), initial, "Damage fully absorbed by golden hearts cost mana");
        hurt(h, p, 5);
        close(h, p.getAbsorptionAmount(), 0, "Partial golden hearts were preserved");
        close(h, p.getHealth(), 100, "Remaining damage reached health");
        close(h, mana(p), initial - 40, "Barrier charged for absorbed damage");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void armorResistanceAndCrystalShieldReduceProtectionCost(GameTestHelper h) {
        var reference = player(h); var p = player(h);
        for (var target : java.util.List.of(reference, p)) {
            target.getAttribute(Attributes.ARMOR).setBaseValue(20);
            target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200));
            target.getPersistentData().putDouble(CrystalShieldEffect.POWER_KEY, 10);
            target.addEffect(new MobEffectInstance(ArsMiscMod.CRYSTAL_SHIELD, 1200, 3));
        }
        hurt(h, reference, 20);
        double reduced = 100 - reference.getHealth();
        h.assertTrue(reduced > 0 && reduced < 20, "Reference reductions did not apply");
        enable(h, p); double initial = mana(p);
        hurt(h, p, 20);
        close(h, p.getHealth(), 100, "Reduced damage reached health");
        close(h, mana(p), initial - reduced * 20, "Protection was priced before other reductions");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void bypassAndExtremeDamageAreProtectedButExhaustionHasNoFreeFollowup(GameTestHelper h) {
        var p = player(h); enable(h, p); double initial = mana(p);
        p.hurt(p.damageSources().genericKill(), 4);
        close(h, p.getHealth(), 100, "Generic kill damage bypassed protection");
        close(h, mana(p), initial - 80, "Generic kill damage had wrong cost");
        CapabilityRegistry.getMana(p).setMana(5);
        p.invulnerableTime = 0;
        p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE);
        close(h, p.getHealth(), 100, "Insufficient mana failed to complete the last protection");
        close(h, mana(p), 0, "Last protection did not drain mana");
        h.assertTrue(!FinalBarrier.active(p) && FinalBarrier.cooldownTicks(p) == 200, "Exhaustion did not disable and cool down barrier");
        p.invulnerableTime = 0;
        p.hurt(p.damageSources().genericKill(), 4);
        close(h, p.getHealth(), 96, "Cooldown granted extra damage protection");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void deathRescueRestoresFullHealthAndCanContinueUntilUnaffordable(GameTestHelper h) {
        var p = player(h); enable(h, p); double initial = mana(p);
        p.setHealth(0); p.die(p.damageSources().genericKill());
        close(h, p.getHealth(), 100, "Death event did not restore full health");
        close(h, mana(p), initial - 1000, "Death rescue did not charge its own fee");
        h.assertTrue(FinalBarrier.active(p) && p.isAlive(), "Paid death rescue disabled the barrier or left player dead");
        CapabilityRegistry.getMana(p).setMana(5);
        p.setHealth(0); p.die(p.damageSources().genericKill());
        close(h, p.getHealth(), 100, "Unaffordable death rescue failed");
        close(h, mana(p), 0, "Unaffordable death rescue did not drain mana");
        h.assertTrue(!FinalBarrier.active(p) && FinalBarrier.cooldownTicks(p) == 200, "Unaffordable rescue did not enter cooldown");
        var event = new LivingDeathEvent(p, p.damageSources().genericKill());
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(!event.isCanceled(), "Cooldown cancelled another death");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void lethalHealthWriteWithoutDeathEventIsCaughtDuringDeathTick(GameTestHelper h) {
        var p = player(h); enable(h, p); double initial = mana(p);
        // Isolate the rescue fee from the native regeneration tick, which depends on world time.
        p.getAttribute(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MANA_REGEN_BONUS)
                .addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        net.minecraft.resources.ResourceLocation.parse("ars_misc:test_no_regen"), -1,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        p.setHealth(0);
        p.doTick();
        close(h, p.getHealth(), 100, "Raw zero-health write reached death ticking without rescue");
        close(h, mana(p), initial - 1000, "Death ticking charged a duplicate or missing rescue fee");
        h.assertTrue(p.isAlive() && p.deathTime == 0, "Death ticking continued after rescue");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void upkeepFailureDoesNotHealAndCooldownSurvivesEquipmentChangesAndRespawn(GameTestHelper h) {
        var p = player(h); enable(h, p);
        p.setHealth(50);
        CapabilityRegistry.getMana(p).setMana(5);
        p.getPersistentData().putLong(FinalBarrier.NEXT_UPKEEP, h.getLevel().getGameTime());
        FinalBarrier.tick(p);
        close(h, p.getHealth(), 50, "Upkeep failure granted a rescue");
        close(h, mana(p), 0, "Upkeep failure did not drain remaining mana");
        CapabilityRegistry.getMana(p).addMana(2000);
        h.assertTrue(mana(p) == 2000 && !FinalBarrier.active(p), "Mana restoration was blocked or auto-enabled protection");
        var replacement = equip(p);
        ToggleCastingPayload.toggle(p);
        h.assertTrue(!FinalBarrierItem.enabled(replacement), "Replacement item bypassed player cooldown");
        var clone = player(h);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone, p, true));
        h.assertTrue(FinalBarrier.cooldownTicks(clone) == FinalBarrier.cooldownTicks(p), "Respawn reset the cooldown");
        p.getPersistentData().putLong(FinalBarrier.COOLDOWN, 0);
        FinalBarrier.tick(p);
        h.assertTrue(!FinalBarrierItem.enabled(replacement), "Cooldown expiry auto-enabled barrier");
        h.assertTrue(ToggleCastingPayload.toggle(p) && FinalBarrier.active(p), "Manual reactivation failed after cooldown");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void cooldownReadyNotifiesOnceAfterRespawnAndActiveReequip(GameTestHelper h) {
        var p=player(h);enable(h,p);
        CapabilityRegistry.getMana(p).setMana(5);
        p.getPersistentData().putLong(FinalBarrier.NEXT_UPKEEP,h.getLevel().getGameTime());
        FinalBarrier.tick(p);
        var clone=player(h);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone,p,true));
        var stack=equip(clone);
        var packets=new java.util.ArrayList<net.minecraft.network.protocol.Packet<?>>();
        clone.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),clone,
                net.minecraft.server.network.CommonListenerCookie.createInitial(clone.getGameProfile(),false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {packets.add(packet);}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {packets.add(packet);}
        };
        FinalBarrier.tick(clone);
        h.assertTrue(packets.isEmpty(),"Ready notification arrived before cooldown expired");
        var inventory=CuriosApi.getCuriosInventory(clone).orElseThrow();
        inventory.setSlotActive("casting_device",0,false);
        clone.getPersistentData().putLong(FinalBarrier.COOLDOWN,h.getLevel().getGameTime()-1);
        FinalBarrier.tick(clone);
        h.assertTrue(packets.isEmpty(),"Inactive barrier sent a ready notification");
        inventory.setSlotActive("casting_device",0,true);
        double mana=mana(clone);
        var context=new top.theillusivec4.curios.api.SlotContext("casting_device",clone,0,false,true);
        for(int i=0;i<3;i++) ArsMiscMod.FINAL_BARRIER.get().curioTick(context,stack);
        long messages=packets.stream().filter(packet->{
            net.minecraft.network.chat.Component text=null;
            if(packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat && chat.overlay()) text=chat.content();
            if(packet instanceof net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket bar) text=bar.text();
            return text!=null && text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                    && contents.getKey().equals("message.ars_misc.final_barrier.ready");
        }).count();
        long sounds=packets.stream().filter(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket).count();
        h.assertTrue(messages==1&&sounds==1,"Expected one ready message and one sound, got "+messages+" / "+sounds);
        h.assertTrue(!FinalBarrierItem.enabled(stack),"Ready notification automatically enabled the barrier");
        close(h,mana(clone),mana,"Ready notification charged upkeep");h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void inactiveSlotsAndUnequippedItemsDoNotProtect(GameTestHelper h) {
        var p = player(h); enable(h, p);
        var inventory = CuriosApi.getCuriosInventory(p).orElseThrow();
        inventory.setSlotActive("casting_device", 0, false);
        double initial = mana(p);
        hurt(h, p, 4);
        close(h, p.getHealth(), 96, "Inactive slot protected player");
        close(h, mana(p), initial, "Inactive slot spent mana");
        var stack = inventory.getStacksHandler("casting_device").orElseThrow().getStacks().getStackInSlot(0);
        ArsMiscMod.FINAL_BARRIER.get().onUnequip(new top.theillusivec4.curios.api.SlotContext("casting_device", p, 0, false, true), ItemStack.EMPTY, stack);
        h.assertTrue(!FinalBarrierItem.enabled(stack), "Unequipped barrier retained enabled state");
        h.succeed();
    }
}
