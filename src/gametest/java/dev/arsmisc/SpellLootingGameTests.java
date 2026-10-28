package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.DamageUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentFortune;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class SpellLootingGameTests {
    private static LootContext kill(GameTestHelper h, Player player, int fortune) {
        return kill(h, player, fortune, false);
    }

    private static LootContext kill(GameTestHelper h, Player player, int fortune, boolean modifiers) {
        var target = h.spawnWithNoFreeWill(EntityType.COW, 2, 2, 2);
        target.setHealth(1);
        var spell = new Spell(MethodTouch.INSTANCE, EffectHarm.INSTANCE);
        var context = SpellContext.fromEntity(spell, player, ItemStack.EMPTY);
        var stats = new SpellStats.Builder();
        for (int i = 0; i < fortune; i++) stats.addAugment(AugmentFortune.INSTANCE);
        var hit = new EntityHitResult(target);
        var resolved = modifiers ? stats.build(EffectHarm.INSTANCE, hit, h.getLevel(), player, context) : stats.build();
        EffectHarm.INSTANCE.onResolveEntity(hit, h.getLevel(), player,
                resolved, context, new SpellResolver(context));
        var source = target.getLastDamageSource();
        h.assertTrue(!target.isAlive() && source instanceof DamageUtil.SpellDamageSource
                && source.getEntity() == player, "Native Harm kill lost the real caster or spell damage source");
        h.assertTrue(((DamageUtil.SpellDamageSource)source).getLuckLevel() == resolved.getBuffCount(AugmentFortune.INSTANCE), "Native Harm lost Fortune augments");
        var params = new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, target)
                .withParameter(LootContextParams.ORIGIN, target.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .create(LootContextParamSets.ENTITY);
        return new LootContext.Builder(params).create(java.util.Optional.empty());
    }

    private static int count(GameTestHelper h, LootContext context) {
        var function = EnchantedCountIncreaseFunction.lootingMultiplier(h.getLevel().registryAccess(), ConstantValue.exactly(1)).build();
        return function.apply(new ItemStack(Items.ROTTEN_FLESH), context).getCount();
    }

    private static ItemStack cursedRing() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse("enigmaticlegacyplus:cursed_ring")).getDefaultInstance();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void removedCharmIsAbsentAndMinerRingDoesNotAddCombatFortune(GameTestHelper h) {
        h.assertTrue(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("ars_misc:lucky_casting_charm")),
                "Removed test charm is still registered");
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        inventory.setEquippedCurio("ring", 0, ArsMiscMod.FORTUNE_STAGES.get(1).get().getDefaultInstance());
        h.assertTrue(count(h, kill(h, player, 0, true)) == 1, "Miner ring granted combat Fortune");
        h.assertTrue(count(h, kill(h, player, 2, true)) == 3, "Removing the test charm changed written spell Fortune");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void nativeHarmFortuneAndCurioLootingCombineInDropCounts(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(count(h, kill(h, player, 0)) == 1, "Unexpected baseline looting");
        h.assertTrue(count(h, kill(h, player, 2)) == 3, "Native Fortune did not increase spell kill drops");
        var ring = cursedRing();
        if (!ring.isEmpty()) {
            var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
            inventory.setEquippedCurio("ring", 0, ring);
            var plain = kill(h, player, 0);
            int bonus = inventory.getLootingLevel(plain);
            h.assertTrue(bonus > 0 && count(h, plain) == 1 + bonus, "Equipped Cursed Ring did not affect Harm kill drops");
            h.assertTrue(count(h, kill(h, player, 2)) == 3 + bonus, "Curio looting did not add to spell Fortune");
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(h.getLevel().holderOrThrow(Enchantments.LOOTING), 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            h.assertTrue(count(h, kill(h, player, 2)) == 4 + bonus,
                    "Weapon and spell looting no longer use their maximum before adding Curios");
        }
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void currentCuriosHookDoesNotIncreaseRareDropCondition(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var condition = new LootItemRandomChanceWithEnchantedBonusCondition(0, LevelBasedValue.perLevel(1),
                h.getLevel().holderOrThrow(Enchantments.LOOTING));
        h.assertTrue(!condition.test(kill(h, player, 0)), "Baseline rare drop condition was not zero");
        var ring = cursedRing();
        if (!ring.isEmpty()) {
            var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
            inventory.setEquippedCurio("ring", 0, ring);
            var loot = kill(h, player, 0);
            h.assertTrue(inventory.getLootingLevel(loot) > 0 && !condition.test(loot),
                    "Curios now also modifies rare-drop conditions; update the compatibility assessment");
        }
        h.assertTrue(condition.test(kill(h, player, 1)), "Native spell Fortune did not affect rare-drop conditions");
        h.succeed();
    }
}
