package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentFortune;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class TieredMiningGameTests {
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void miningRecipesLoadAndCostNoSource(GameTestHelper helper) {
        String[] ids = {"fortune_ring", "fortune_ring_2", "blasting_miner_ring_1", "blasting_miner_ring_2"};
        String[] reagents = {"ars_nouveau:ring_of_potential", "ars_misc:fortune_ring", "ars_nouveau:ring_of_potential", "ars_misc:blasting_miner_ring_1"};
        String[][] materials = {
            {"minecraft:diamond_pickaxe", "minecraft:rabbit_foot", "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:emerald", "minecraft:emerald"},
            {"minecraft:netherite_pickaxe", "minecraft:rabbit_foot", "minecraft:rabbit_foot", "minecraft:gold_block", "minecraft:gold_block", "minecraft:gold_block", "minecraft:emerald_block", "minecraft:emerald_block"},
            {"minecraft:diamond_pickaxe", "minecraft:tnt", "minecraft:tnt", "minecraft:tnt", "minecraft:tnt", "minecraft:tnt", "ars_nouveau:source_gem", "ars_nouveau:source_gem"},
            {"minecraft:netherite_pickaxe", "minecraft:end_crystal", "minecraft:end_crystal", "minecraft:end_crystal", "minecraft:end_crystal", "minecraft:end_crystal", "ars_nouveau:source_gem_block", "ars_nouveau:source_gem_block"}
        };
        for (int i=0;i<ids.length;i++) {
            var holder=helper.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_misc",ids[i])).orElseThrow();
            helper.assertTrue(holder.value() instanceof com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe,"Wrong apparatus recipe type");
            var recipe=(com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe)holder.value();
            helper.assertTrue(recipe.sourceCost()==0 && !recipe.consumesSource(),"Recipe consumes source");
            helper.assertTrue(recipe.pedestalItems().size()==8,"Recipe must use eight pedestals");
            var reagent=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(reagents[i])).getDefaultInstance();
            helper.assertTrue(recipe.reagent().test(reagent),"Wrong central reagent");
            var stacks=java.util.Arrays.stream(materials[i]).map(id->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)).getDefaultInstance()).toList();
            helper.assertTrue(com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe.doItemsMatch(stacks,recipe.pedestalItems()),"Wrong material quantities");
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(recipe.result().getItem()).getPath().equals(ids[i]),"Wrong output");
        }
        helper.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void fortuneStagesCapAndHighest(GameTestHelper helper) {
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();
        var spell=new Spell(MethodTouch.INSTANCE,EffectBreak.INSTANCE);
        var context=SpellContext.fromEntity(spell,player,ItemStack.EMPTY);
        for(int tier=1;tier<=2;tier++) {
            inventory.setEquippedCurio("ring",0,ArsMiscMod.FORTUNE_STAGES.get(tier-1).get().getDefaultInstance());
            inventory.setEquippedCurio("ring",1,ArsMiscMod.FORTUNE_RING.get().getDefaultInstance());
            var stats=new SpellStats.Builder().build(EffectBreak.INSTANCE,null,helper.getLevel(),player,context);
            helper.assertTrue(stats.getBuffCount(AugmentFortune.INSTANCE)==tier*2,"Stages stacked or wrong tier");
            var capped=new SpellStats.Builder().addAugment(AugmentFortune.INSTANCE).addAugment(AugmentFortune.INSTANCE)
                .addAugment(AugmentFortune.INSTANCE).addAugment(AugmentFortune.INSTANCE)
                .build(EffectBreak.INSTANCE,null,helper.getLevel(),player,context);
            helper.assertTrue(capped.getBuffCount(AugmentFortune.INSTANCE)==4,"Fortune exceeded IV");
        }
        helper.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void blastingStagesMineObsidian(GameTestHelper helper) {
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();
        var spell=new Spell(MethodTouch.INSTANCE,EffectBreak.INSTANCE);
        var context=SpellContext.fromEntity(spell,player,ItemStack.EMPTY);
        var origin=helper.absolutePos(new BlockPos(2,2,2));
        var hit=new BlockHitResult(Vec3.atCenterOf(origin),Direction.UP,origin,false);
        for(int tier=1;tier<=2;tier++) {
            inventory.setEquippedCurio("ring",0,ArsMiscMod.BLASTING_STAGES.get(tier-1).get().getDefaultInstance());
            inventory.setEquippedCurio("ring",1,ArsMiscMod.BLASTING_STAGES.get(0).get().getDefaultInstance());
            var stats=new SpellStats.Builder().build(EffectBreak.INSTANCE,hit,helper.getLevel(),player,context);
            helper.assertTrue(stats.getAmpMultiplier()==tier,"Amplify stacked between tiers");
            var positions=SpellUtil.calcAOEBlocks(player,origin,hit,stats);
            helper.assertTrue(positions.size()==(tier==1?9:27),"Wrong mining size");
            for(var pos:positions) helper.getLevel().setBlockAndUpdate(pos,Blocks.OBSIDIAN.defaultBlockState());
            EffectBreak.INSTANCE.onResolveBlock(hit,helper.getLevel(),player,stats,context,new SpellResolver(context));
            for(var pos:positions) helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(),"Cannot mine obsidian: tier "+tier);
        }
        var harm=new SpellStats.Builder().build(EffectHarm.INSTANCE,hit,helper.getLevel(),player,context);
        helper.assertTrue(harm.getAmpMultiplier()==0 && harm.getAoeMultiplier()==0,"Mining rings modified Harm");
        helper.assertTrue(spell.unsafeList().size()==2,"Saved spell changed");
        helper.succeed();
    }
}
