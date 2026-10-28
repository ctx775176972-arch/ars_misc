package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.arsmisc.compat.SpellBlockLootOwner;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.function.Consumer;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class SpellHarvestEffectsGameTests {
    private static void cast(GameTestHelper h, Player player, BlockPos pos) {
        var context = SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE, EffectBreak.INSTANCE), player, ItemStack.EMPTY);
        EffectBreak.INSTANCE.onResolveBlock(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false),
                h.getLevel(), player, new SpellStats.Builder().build(), context, new SpellResolver(context));
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void harvestQueriesReadCasterEffectsAndCleanUpAfterCancellation(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 2));
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        FakePlayer[] observed = {null};
        int[] calls = {0};
        Consumer<BlockEvent.BreakEvent> listener = event -> {
            if (event.getLevel() != h.getLevel() || !event.getPos().equals(pos)) return;
            // Ars checks permissions with the real caster before entering its fake-player harvest.
            if (!(event.getPlayer() instanceof FakePlayer)) return;
            calls[0]++;
            var fake = (FakePlayer) event.getPlayer(); observed[0] = fake;
            h.assertTrue(fake.hasEffect(MobEffects.LUCK) && fake.getEffect(MobEffects.LUCK).getAmplifier() == 2,
                    "Harvest did not inherit caster effect queries");
            h.assertTrue(fake.getActiveEffects().stream().anyMatch(e -> e.is(MobEffects.LUCK))
                    && fake.getActiveEffectsMap().containsKey(MobEffects.LUCK), "Effect collection queries lost caster buffs");
            fake.getEffect(MobEffects.LUCK).update(new MobEffectInstance(MobEffects.LUCK, 2000, 4));
            fake.getActiveEffectsMap().clear();
            h.assertTrue(player.getEffect(MobEffects.LUCK).getAmplifier() == 2, "Query snapshots mutated caster effects");
            event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            h.getLevel().setBlockAndUpdate(pos, Blocks.OAK_LOG.defaultBlockState());
            cast(h, player, pos);
            h.assertTrue(calls[0] == 1 && h.getLevel().getBlockState(pos).is(Blocks.OAK_LOG), "Cancelled harvest was not respected");
            h.assertTrue(!observed[0].hasEffect(MobEffects.LUCK), "Cancelled harvest leaked buff onto cached fake player");
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void nestedAndFailedHarvestsRestoreEffectOwner(GameTestHelper h) {
        var first = h.makeMockPlayer(GameType.SURVIVAL);
        var second = h.makeMockPlayer(GameType.SURVIVAL);
        first.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 2));
        second.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1));
        var fake = ANFakePlayer.getPlayer(h.getLevel());
        var other = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "unrelated-harvest"));
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        SpellBlockLootOwner.duringHarvest(first, h.getLevel(), pos, () -> {
            SpellBlockLootOwner.bindBreaker(fake);
            h.assertTrue(fake.hasEffect(MobEffects.LUCK) && !other.hasEffect(MobEffects.LUCK), "Effect inheritance escaped its bound breaker");
            try {
                SpellBlockLootOwner.duringHarvest(second, h.getLevel(), pos.east(), () -> {
                    SpellBlockLootOwner.bindBreaker(fake);
                    h.assertTrue(fake.hasEffect(MobEffects.REGENERATION) && !fake.hasEffect(MobEffects.LUCK), "Nested caster inherited previous buffs");
                    throw new IllegalStateException("Expected test failure");
                });
            } catch (IllegalStateException expected) {}
            h.assertTrue(fake.hasEffect(MobEffects.LUCK) && !fake.hasEffect(MobEffects.REGENERATION), "Failed nested harvest did not restore outer caster");
            return null;
        });
        h.assertTrue(!fake.hasEffect(MobEffects.LUCK) && !fake.hasEffect(MobEffects.REGENERATION), "Finished harvest retained caster effects");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void installedOccultismThirdEyeChangesNativeLogHarvest(GameTestHelper h) {
        if (!net.neoforged.fml.ModList.get().isLoaded("occultism")) { h.succeed(); return; }
        var effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("occultism:third_eye"));
        var block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse("occultism:otherworld_log_natural"));
        h.assertTrue(effect.isPresent() && block.isPresent(), "Installed Occultism test resources were not found");
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var bounds = new AABB(pos).inflate(2);
        h.getLevel().setBlockAndUpdate(pos, block.get().defaultBlockState());
        cast(h, player, pos);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
                .anyMatch(e -> e.getItem().is(net.minecraft.world.item.Items.OAK_LOG)), "Unbuffed harvest should retain covered oak drops");
        h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).forEach(ItemEntity::discard);
        player.addEffect(new MobEffectInstance(effect.get(), 1200));
        h.getLevel().setBlockAndUpdate(pos, block.get().defaultBlockState());
        cast(h, player, pos);
        var expected = BuiltInRegistries.ITEM.get(ResourceLocation.parse("occultism:otherworld_log"));
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
                .anyMatch(e -> e.getItem().is(expected)), "Third Eye spell harvest did not produce native Otherworld log drops");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void scopedCuriosQueriesRestoreAfterNestedAndFailedHarvests(GameTestHelper h) {
        var first = h.makeMockPlayer(GameType.SURVIVAL);
        var second = h.makeMockPlayer(GameType.SURVIVAL);
        var capability = top.theillusivec4.curios.api.CuriosCapability.INVENTORY;
        var firstInventory = first.getCapability(capability);
        var secondInventory = second.getCapability(capability);
        h.assertTrue(firstInventory != null && secondInventory != null && firstInventory != secondInventory, "Missing test Curios inventories");
        var fake = ANFakePlayer.getPlayer(h.getLevel());
        var originalWearer = fake.getCapability(capability).getWearer();
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        SpellBlockLootOwner.duringHarvest(first, h.getLevel(), pos, () -> {
            SpellBlockLootOwner.bindBreaker(fake);
            h.assertTrue(fake.getCapability(capability).getWearer() == first
                    && fake.getCapability(capability, null).getWearer() == first
                    && top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(fake).orElseThrow().getWearer() == first,
                    "Direct or API Curios query did not resolve the caster");
            try {
                SpellBlockLootOwner.duringHarvest(second, h.getLevel(), pos.east(), () -> {
                    SpellBlockLootOwner.bindBreaker(fake);
                    h.assertTrue(fake.getCapability(capability).getWearer() == second, "Nested harvest retained previous caster's Curios");
                    throw new IllegalStateException("Expected test failure");
                });
            } catch (IllegalStateException expected) {}
            h.assertTrue(fake.getCapability(capability).getWearer() == first, "Failed nested harvest did not restore outer Curios");
            return null;
        });
        h.assertTrue(fake.getCapability(capability).getWearer() == originalWearer, "Finished harvest retained caster's Curios");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void installedOccultismCurioStaffWorksWithoutThirdEyeAndStopsWhenUnequipped(GameTestHelper h) {
        if (!net.neoforged.fml.ModList.get().isLoaded("occultism")) { h.succeed(); return; }
        var staff = BuiltInRegistries.ITEM.get(ResourceLocation.parse("occultism:true_sight_staff"));
        h.assertTrue(staff != net.minecraft.world.item.Items.AIR, "Installed True Sight Staff was not found");
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("occultism:otherworld_log_natural"));
        var expected = BuiltInRegistries.ITEM.get(ResourceLocation.parse("occultism:otherworld_log"));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
        var slot = curios.getCurios().entrySet().stream().filter(e -> e.getValue().getStacks().getSlots() > 0)
                .map(java.util.Map.Entry::getKey).findFirst().orElseThrow();
        curios.setEquippedCurio(slot, 0, new ItemStack(staff));
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var bounds = new AABB(pos).inflate(2);
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        cast(h, player, pos);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
                .anyMatch(e -> e.getItem().is(expected)), "Curio staff did not reveal Otherworld log without Third Eye");
        h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).forEach(ItemEntity::discard);
        curios.setEquippedCurio(slot, 0, ItemStack.EMPTY);
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        cast(h, player, pos);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
                .anyMatch(e -> e.getItem().is(net.minecraft.world.item.Items.OAK_LOG))
                && h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream().noneMatch(e -> e.getItem().is(expected)),
                "Unequipped staff still revealed Otherworld drops");
        h.succeed();
    }
}
