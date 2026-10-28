package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class LootSenseGameTests {
    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void scansLootBlocksAndVehiclesWithoutGeneratingLoot(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h);
        h.setBlock(2,2,3,Blocks.CHEST);h.setBlock(3,2,3,Blocks.BARREL);h.setBlock(4,2,3,Blocks.CHEST);
        var chest=(RandomizableContainerBlockEntity)h.getBlockEntity(new BlockPos(2,2,3));
        var barrel=(RandomizableContainerBlockEntity)h.getBlockEntity(new BlockPos(3,2,3));
        chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON);barrel.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
        var cart=new net.minecraft.world.entity.vehicle.MinecartChest(h.getLevel(),p.getX()+2,p.getY(),p.getZ());
        cart.setLootTable(BuiltInLootTables.ABANDONED_MINESHAFT);h.getLevel().addFreshEntity(cart);
        var ordinaryCart=new net.minecraft.world.entity.vehicle.MinecartChest(h.getLevel(),p.getX()+3,p.getY(),p.getZ());
        h.getLevel().addFreshEntity(ordinaryCart);
        int chunks=h.getLevel().getChunkSource().getLoadedChunksCount();
        var result=LootSenseEffect.scan(p);
        h.assertTrue(result.blocks().contains(chest.getBlockPos()) && result.blocks().contains(barrel.getBlockPos()),"Missing loot blocks");
        h.assertTrue(!result.blocks().contains(h.absolutePos(new BlockPos(4,2,3))),"Player storage was revealed");
        h.assertTrue(result.entities().contains(cart.getId()) && !result.entities().contains(ordinaryCart.getId()),"Wrong vehicle filtering");
        h.assertTrue(chest.getLootTable()!=null && barrel.getLootTable()!=null && cart.getLootTable()!=null,"Scanning generated loot");
        h.assertTrue(chunks==h.getLevel().getChunkSource().getLoadedChunksCount(),"Scanning loaded extra chunks");
        chest.setLootTable(null);cart.setLootTable(null);
        result=LootSenseEffect.scan(p);
        h.assertTrue(!result.blocks().contains(chest.getBlockPos()) && !result.entities().contains(cart.getId()),"Opened containers remain marked");
        h.succeed();
    }

    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void lootSenseDurationRangeAndRecipe(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h);
        var spell=new Spell(MethodSelf.INSTANCE,EffectLootSense.INSTANCE);
        var context=SpellContext.fromEntity(spell,p,ItemStack.EMPTY);
        EffectLootSense.INSTANCE.onResolveEntity(new EntityHitResult(p),h.getLevel(),p,new SpellStats.Builder().build(),context,new SpellResolver(context));
        h.assertTrue(p.getEffect(ArsMiscMod.LOOT_SENSE).getDuration()==1200,"Wrong base duration");
        var extended=new SpellStats.Builder().addAugment(AugmentExtendTime.INSTANCE);
        AugmentExtendTime.INSTANCE.applyModifiers(extended,EffectLootSense.INSTANCE,new EntityHitResult(p),h.getLevel(),p,context);
        EffectLootSense.INSTANCE.onResolveEntity(new EntityHitResult(p),h.getLevel(),p,extended.build(),context,new SpellResolver(context));
        h.assertTrue(p.getEffect(ArsMiscMod.LOOT_SENSE).getDuration()==1500,"Extend Time failed");
        var far=new net.minecraft.world.entity.vehicle.MinecartChest(h.getLevel(),p.getX()+76,p.getY(),p.getZ());
        far.setLootTable(BuiltInLootTables.ABANDONED_MINESHAFT);h.getLevel().addFreshEntity(far);
        h.assertTrue(!LootSenseEffect.scan(p).entities().contains(far.getId()),"Out-of-range vehicle revealed");
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_misc","glyph_loot_sense");
        var recipe=(com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe)
                h.getLevel().getRecipeManager().byKey(id).orElseThrow().value();
        h.assertTrue(recipe.getOutput().is(EffectLootSense.INSTANCE.getGlyph()) && recipe.getInputs().size()==8,"Wrong glyph recipe");
        for(var item:java.util.List.of(net.minecraft.world.item.Items.CHEST,net.minecraft.world.item.Items.TRAPPED_CHEST))
            h.assertTrue(recipe.getInputs().stream().filter(ingredient->ingredient.test(item.getDefaultInstance())).count()==5,
                    "Five chest-tag ingredients missing: "+item);
        h.succeed();
    }
}
