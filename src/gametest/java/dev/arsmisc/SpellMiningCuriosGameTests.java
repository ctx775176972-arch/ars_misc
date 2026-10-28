package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.common.util.HolderHelper;
import dev.arsmisc.compat.SpellBlockLootOwner;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class SpellMiningCuriosGameTests {
    private static final Map<UUID,List<LootContext>> OBSERVED=new HashMap<>();
    private static boolean registered;

    private static Player equipped(GameTestHelper h) {
        if(!registered) {
            CuriosApi.registerCurio(Items.PRISMARINE_CRYSTALS,new ICurioItem() {
                @Override public int getFortuneLevel(SlotContext slot, LootContext context, ItemStack stack) {
                    if(context!=null && OBSERVED.containsKey(slot.entity().getUUID()))
                        OBSERVED.get(slot.entity().getUUID()).add(context);
                    return 2;
                }
            });
            registered=true;
        }
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        OBSERVED.put(player.getUUID(),new ArrayList<>());
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("ring",0,new ItemStack(Items.PRISMARINE_CRYSTALS));
        return player;
    }

    private static void cast(GameTestHelper h, Player player, BlockPos pos, SpellStats stats) {
        var spell=new Spell(MethodTouch.INSTANCE,EffectBreak.INSTANCE);
        var context=SpellContext.fromEntity(spell,player,ItemStack.EMPTY);
        EffectBreak.INSTANCE.onResolveBlock(new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false),
                h.getLevel(),player,stats,context,new SpellResolver(context));
    }

    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void installedCursedRingContributesItsNativeFortune(GameTestHelper h) {
        var relic=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse("enigmaticlegacyplus:cursed_ring"));
        if(relic==Items.AIR) { h.succeed();return; }
        var player=equipped(h);var pos=h.absolutePos(new BlockPos(2,2,2));
        try {
            var ring=new ItemStack(relic);
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("ring",1,ring);
            h.getLevel().setBlockAndUpdate(pos,Blocks.DIAMOND_ORE.defaultBlockState());
            cast(h,player,pos,new SpellStats.Builder().build());
            var calls=OBSERVED.get(player.getUUID());
            h.assertTrue(calls.size()==1,"Native relic harvest did not query player Curios exactly once");
            var loot=calls.getFirst();
            int nativeBonus=CuriosApi.getCurio(ring).orElseThrow().getFortuneLevel(new SlotContext("ring",player,1,false,true),loot);
            int combined=top.theillusivec4.curios.mixin.CuriosUtilMixinHooks.getFortuneLevel(loot);
            h.assertTrue(nativeBonus>0 && combined==2+nativeBonus,"Installed Cursed Ring did not contribute its native Fortune: "+nativeBonus+", "+combined);
            h.succeed();
        } finally { OBSERVED.remove(player.getUUID()); }
    }

    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void nativeSpellReadsCuriosOnceAndKeepsFakeHarvest(GameTestHelper h) {
        var player=equipped(h);var pos=h.absolutePos(new BlockPos(2,2,2));
        boolean[] fakeHarvest={false};
        Consumer<BlockEvent.BreakEvent> listener=event->{
            if(event.getLevel()==h.getLevel() && event.getPos().equals(pos)) fakeHarvest[0]=event.getPlayer() instanceof FakePlayer;
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("ring",1,ArsMiscMod.FORTUNE_STAGES.get(1).get().getDefaultInstance());
            var context=SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE,EffectBreak.INSTANCE),player,ItemStack.EMPTY);
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
            var stats=new SpellStats.Builder().build(EffectBreak.INSTANCE,hit,h.getLevel(),player,context);
            h.getLevel().setBlockAndUpdate(pos,Blocks.DIAMOND_ORE.defaultBlockState());
            cast(h,player,pos,stats);
            var calls=OBSERVED.get(player.getUUID());
            h.assertTrue(fakeHarvest[0] && h.getLevel().getBlockState(pos).isAir(),"Native fake-player harvest changed or block survived");
            h.assertTrue(calls.size()==1,"Curios bonus missing or queried twice: "+calls.size());
            var loot=calls.getFirst();
            h.assertTrue(loot.getParamOrNull(LootContextParams.THIS_ENTITY)==player,"Loot context did not retain real caster");
            var tool=loot.getParamOrNull(LootContextParams.TOOL);
            h.assertTrue(tool!=null && tool.getEnchantmentLevel(HolderHelper.unwrap(h.getLevel(),Enchantments.FORTUNE))==4,"External bonus changed glyph/tool Fortune IV");
            h.assertTrue(stats.getBuffCount(AugmentFortune.INSTANCE)==4,"External bonus changed spell augments");
            h.succeed();
        } finally { NeoForge.EVENT_BUS.unregister(listener);OBSERVED.remove(player.getUUID()); }
    }

    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void everyAreaBlockUsesCasterAndSilkTouchStillWorks(GameTestHelper h) {
        var player=equipped(h);var pos=h.absolutePos(new BlockPos(16,2,16));
        try {
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
            var stats=new SpellStats.Builder().addAOE(2).addAugment(AugmentAOE.INSTANCE).addAugment(AugmentAOE.INSTANCE).build();
            var positions=SpellUtil.calcAOEBlocks(player,pos,hit,stats);
            for(var block:positions) h.getLevel().setBlockAndUpdate(block,Blocks.DIAMOND_ORE.defaultBlockState());
            cast(h,player,pos,stats);
            var calls=OBSERVED.get(player.getUUID());
            h.assertTrue(positions.size()==9 && calls.size()==positions.size(),"Area harvest missed or duplicated Curios: "+calls.size());
            h.assertTrue(calls.stream().allMatch(c->c.getParamOrNull(LootContextParams.THIS_ENTITY)==player),"Area harvest lost caster");
            calls.clear();
            h.getLevel().setBlockAndUpdate(pos,Blocks.DIAMOND_ORE.defaultBlockState());
            cast(h,player,pos,new SpellStats.Builder().addAugment(AugmentExtract.INSTANCE).build());
            h.assertTrue(calls.isEmpty(),"Silk Touch unexpectedly rolled Fortune");
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(pos).inflate(1),e->e.getItem().is(Items.DIAMOND_ORE)).stream().anyMatch(e->e.getItem().getCount()==1),"Silk Touch did not drop the ore block");
            h.succeed();
        } finally { OBSERVED.remove(player.getUUID()); }
    }

    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void normalMiningAndFailedScopesDoNotLeak(GameTestHelper h) {
        var player=equipped(h);var pos=h.absolutePos(new BlockPos(2,2,2));
        var fake=ANFakePlayer.getPlayer(h.getLevel());var tool=new ItemStack(Items.DIAMOND_PICKAXE);
        try {
            var state=Blocks.DIAMOND_ORE.defaultBlockState();
            net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),pos,null,player,tool);
            h.assertTrue(OBSERVED.get(player.getUUID()).size()==1,"Ordinary player mining lost its native Curios bonus");
            OBSERVED.get(player.getUUID()).clear();
            try { SpellBlockLootOwner.duringHarvest(player,h.getLevel(),pos,()->{throw new IllegalStateException("Expected test failure");}); }
            catch(IllegalStateException expected) {}
            net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),pos,null,fake,tool);
            h.assertTrue(OBSERVED.get(player.getUUID()).isEmpty(),"Failed harvest leaked caster into later fake-player loot");
            SpellBlockLootOwner.duringHarvest(player,h.getLevel(),pos,()-> {
                net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),pos.east(),null,fake,tool);
                return null;
            });
            h.assertTrue(OBSERVED.get(player.getUUID()).isEmpty(),"Unrelated block inherited the caster");
            h.succeed();
        } finally { OBSERVED.remove(player.getUUID()); }
    }
}
