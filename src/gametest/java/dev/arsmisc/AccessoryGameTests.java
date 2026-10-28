package dev.arsmisc;

import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.event.SpellModifierEvent;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentFortune;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.ModPotions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("ars_misc")
@PrefixGameTestTemplate(false)
public final class AccessoryGameTests {
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void damageBeltAndGlassesMultiplyAndUseSeparateSlots(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        // Isolate Sauce's guaranteed critical from Apothic Attributes' independent random critical.
        net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(
                net.minecraft.resources.ResourceLocation.parse("apothic_attributes:crit_chance"))
                .ifPresent(attribute -> player.getAttribute(attribute).setBaseValue(0));
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        var belt = ArsMiscMod.CRITICAL_BELT.get();
        var glasses = ArsMiscMod.CRITICAL_GLASSES.get();
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_misc", "accessory_test");
        var beltSlot = new top.theillusivec4.curios.api.SlotContext("belt", player, 0, false, true);
        var headSlot = new top.theillusivec4.curios.api.SlotContext("head", player, 0, false, true);
        helper.assertTrue(!belt.canEquip(headSlot, belt.getDefaultInstance())
                && !glasses.canEquip(beltSlot, glasses.getDefaultInstance()), "Accessories accepted the wrong slot");
        double before = player.getAttributeValue(ModRegistry.SPELL_DAMAGE_MULTIPLIER);
        belt.getAttributeModifiers(beltSlot, id, belt.getDefaultInstance()).forEach(
                (attribute, modifier) -> player.getAttribute(attribute).addTransientModifier(modifier));
        helper.assertTrue(Math.abs(player.getAttributeValue(ModRegistry.SPELL_DAMAGE_MULTIPLIER) - before - 0.15) < 0.00001,
                "Belt did not add 15% spell damage");
        helper.assertTrue(belt.getAttributeModifiers(headSlot, id, belt.getDefaultInstance()).isEmpty(), "Wrong belt slot granted damage");
        var mana = com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player);
        mana.setMaxMana(1000); mana.setMana(0);
        inventory.setEquippedCurio("belt", 0, belt.getDefaultInstance());
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(1);
        var context = SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE, EffectHarm.INSTANCE), player, ItemStack.EMPTY);
        var resolver = new SpellResolver(context);
        var first = helper.spawnWithNoFreeWill(EntityType.COW, 1, 2, 1);
        float hp = first.getHealth();
        double expected = 2 * (before + 0.15) * (1 + player.getAttributeValue(ModRegistry.SPELL_CRIT_DAMAGE));
        EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(), context,
                resolver, first, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 2);
        helper.assertTrue(Math.abs(hp - first.getHealth() - expected) < 0.001,
                "Damage bonus and critical multiplier did not multiply: actual=" + (hp-first.getHealth()) + " expected=" + expected);
        helper.assertTrue(mana.getCurrentMana() == 0, "Damage belt retained its former mana refund");
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(0);
        glasses.getAttributeModifiers(headSlot, id, glasses.getDefaultInstance()).forEach(
                (attribute, modifier) -> player.getAttribute(attribute).addTransientModifier(modifier));
        helper.assertTrue(Math.abs(player.getAttributeValue(ModRegistry.SPELL_CRIT) - 0.5) < 0.00001, "Glasses did not grant 50% critical chance");
        inventory.setEquippedCurio("head", 0, glasses.getDefaultInstance());
        inventory.setSlotActive("head", 0, false);
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(1);
        var second = helper.spawnWithNoFreeWill(EntityType.COW, 2, 2, 1);
        EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(), context,
                resolver, second, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 2);
        helper.assertTrue(mana.getCurrentMana() == 0, "Inactive glasses refunded mana");
        inventory.setSlotActive("head", 0, true);
        var third = helper.spawnWithNoFreeWill(EntityType.COW, 3, 2, 1);
        EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(), context,
                resolver, third, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 2);
        helper.assertTrue(mana.getCurrentMana() > 0, "Active glasses did not refund mana with the damage belt equipped");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void fortuneIsEquippedOnlyAndBreakOnly(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        var spell = new Spell(MethodSelf.INSTANCE, EffectBreak.INSTANCE);
        var context = SpellContext.fromEntity(spell, player, ItemStack.EMPTY);
        var hit = new BlockHitResult(Vec3.ZERO, Direction.UP, BlockPos.ZERO, false);
        var without = new SpellStats.Builder();
        NeoForge.EVENT_BUS.post(new SpellModifierEvent(player, without, EffectBreak.INSTANCE, hit, helper.getLevel(), context));
        helper.assertTrue(without.build().getBuffCount(AugmentFortune.INSTANCE) == 0, "Unequipped ring added Fortune");
        inventory.setEquippedCurio("ring", 0, ArsMiscMod.FORTUNE_RING.get().getDefaultInstance());
        helper.assertTrue(AccessoryEvents.isWearing(player, ArsMiscMod.FORTUNE_RING.get(), "ring"), "Ring is missing from mock Curios inventory: " + inventory.getCurios());
        var with = new SpellStats.Builder();
        NeoForge.EVENT_BUS.post(new SpellModifierEvent(player, with, EffectBreak.INSTANCE, hit, helper.getLevel(), context));
        helper.assertTrue(with.build().getBuffCount(AugmentFortune.INSTANCE) == 2, "Equipped ring did not add Fortune");
        var harm = new SpellStats.Builder();
        NeoForge.EVENT_BUS.post(new SpellModifierEvent(player, harm, EffectHarm.INSTANCE, hit, helper.getLevel(), context));
        helper.assertTrue(harm.build().getBuffCount(AugmentFortune.INSTANCE) == 0, "Ring changed another glyph");
        helper.assertTrue(spell.unsafeList().size() == 2, "Saved spell was modified");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void successfulCriticalRefundsManaWithOneSecondSharedCooldown(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        inventory.setEquippedCurio("head", 0, ArsMiscMod.CRITICAL_GLASSES.get().getDefaultInstance());
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(1.0);
        var mana = com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player);
        mana.setMaxMana(1000);mana.setMana(0);
        player.getAttribute(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MANA_REGEN_BONUS).setBaseValue(7);
        double refund = com.hollingsworth.arsnouveau.api.util.ManaUtil.getManaRegen(player) * 5;
        var spell = new Spell(MethodSelf.INSTANCE, EffectHarm.INSTANCE);
        var context = SpellContext.fromEntity(spell, player, ItemStack.EMPTY);
        var resolver = new SpellResolver(context);
        var target = helper.spawnWithNoFreeWill(EntityType.COW, 1, 2, 1);
        boolean damaged = EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                context, resolver, target, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 2.0f);
        helper.assertTrue(damaged, "Test damage failed");
        helper.assertTrue(Math.abs(mana.getCurrentMana()-refund)<0.00001 && !player.hasEffect(ModPotions.MANA_REGEN_EFFECT),
                "Successful Sauce critical did not refund five seconds of current regeneration");
        var areaTarget = helper.spawnWithNoFreeWill(EntityType.COW, 2, 2, 2);
        EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                context, resolver, areaTarget, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 1);
        helper.assertTrue(Math.abs(mana.getCurrentMana()-refund)<0.00001,"Another target bypassed the player-wide cooldown");
        inventory.setEquippedCurio("head",0,ItemStack.EMPTY);
        inventory.setEquippedCurio("head",0,ArsMiscMod.CRITICAL_GLASSES.get().getDefaultInstance());
        var swapped = helper.spawnWithNoFreeWill(EntityType.COW, 3, 2, 2);
        EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                context, resolver, swapped, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 1);
        helper.assertTrue(Math.abs(mana.getCurrentMana()-refund)<0.00001,"Re-equipping bypassed cooldown");
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(0);
        var ordinary = helper.spawnWithNoFreeWill(EntityType.COW, 2, 2, 1);
        EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                context, resolver, ordinary, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 2.0f);
        helper.assertTrue(Math.abs(mana.getCurrentMana()-refund)<0.00001, "Noncritical hit refunded mana");
        helper.runAfterDelay(19, () -> {
            player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(1);
            var early = helper.spawnWithNoFreeWill(EntityType.COW, 1, 2, 2);
            EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                    context, resolver, early, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 1);
            helper.assertTrue(Math.abs(mana.getCurrentMana()-refund)<0.00001,"Cooldown expired before 20 ticks");
        });
        helper.runAfterDelay(20, () -> {
            var immune = helper.spawnWithNoFreeWill(EntityType.COW, 1, 2, 3);immune.setInvulnerable(true);
            EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                    context, resolver, immune, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 1);
            helper.assertTrue(Math.abs(mana.getCurrentMana()-refund)<0.00001,"Blocked hit refunded mana");
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModPotions.MANA_REGEN_EFFECT,200,1));
            double enhanced = com.hollingsworth.arsnouveau.api.util.ManaUtil.getManaRegen(player)*5;
            var ready = helper.spawnWithNoFreeWill(EntityType.COW, 2, 2, 3);
            EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                    context, resolver, ready, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 1);
            helper.assertTrue(Math.abs(mana.getCurrentMana()-refund-enhanced)<0.00001,"Cooldown failed at 20 ticks or current regeneration effect was ignored");
        });
        helper.runAfterDelay(40, () -> {
            mana.setMana(999);
            var capped = helper.spawnWithNoFreeWill(EntityType.COW, 3, 2, 3);
            EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                    context, resolver, capped, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 1);
            helper.assertTrue(mana.getCurrentMana()==1000,"Refund exceeded maximum mana");
            helper.succeed();
        });
    }
}
