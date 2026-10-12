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
    public static void successfulCriticalGrantsManaRegeneration(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        inventory.setEquippedCurio("belt", 0, ArsMiscMod.CRITICAL_BELT.get().getDefaultInstance());
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(1.0);
        var spell = new Spell(MethodSelf.INSTANCE, EffectHarm.INSTANCE);
        var context = SpellContext.fromEntity(spell, player, ItemStack.EMPTY);
        var resolver = new SpellResolver(context);
        var target = helper.spawnWithNoFreeWill(EntityType.COW, 1, 2, 1);
        boolean damaged = EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                context, resolver, target, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 2.0f);
        helper.assertTrue(damaged, "Test damage failed");
        var effect = player.getEffect(ModPotions.MANA_REGEN_EFFECT);
        helper.assertTrue(effect != null && effect.getAmplifier() == 1 && effect.getDuration() == 200,
                "Successful Sauce critical did not grant Mana Regeneration II for 200 ticks");
        player.removeEffect(ModPotions.MANA_REGEN_EFFECT);
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(0);
        var ordinary = helper.spawnWithNoFreeWill(EntityType.COW, 2, 2, 1);
        EffectHarm.INSTANCE.attemptDamage(helper.getLevel(), player, new SpellStats.Builder().build(),
                context, resolver, ordinary, EffectHarm.INSTANCE.buildDamageSource(helper.getLevel(), player), 2.0f);
        helper.assertTrue(!player.hasEffect(ModPotions.MANA_REGEN_EFFECT), "Noncritical hit granted regeneration");
        helper.succeed();
    }
}
