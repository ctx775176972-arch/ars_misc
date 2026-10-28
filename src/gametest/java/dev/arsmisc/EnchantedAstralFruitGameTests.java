package dev.arsmisc;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class EnchantedAstralFruitGameTests {
    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse("enigmaticlegacyplus:" + path));
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void nativeRewardRequiresFruitCursesEndCityAndUnclaimedPlayer(GameTestHelper h) throws Exception {
        if (item("enchanted_astral_fruit") == Items.AIR) { h.succeed(); return; }
        var type = Class.forName("auviotre.enigmatic.legacy.contents.loot.modifiers.SpecialLootModifier");
        var modifier = type.getConstructor(LootItemCondition[].class).newInstance((Object)new LootItemCondition[0]);
        var apply = type.getDeclaredMethod("doApply", ObjectArrayList.class, LootContext.class);
        apply.setAccessible(true);
        var handler = Class.forName("auviotre.enigmatic.legacy.handlers.EnigmaticHandler");
        var player = ReactiveShieldGameTests.player(h);
        var data = (CompoundTag)handler.getMethod("getPersistedData", Player.class).invoke(null, player);
        data.putBoolean("LootedArchitectEye", true);
        data.putBoolean("LootedIchorBottle", true);
        data.putBoolean("SevenCursesBearing", true);
        var end = h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(end != null, "End dimension did not load");
        var params = new LootParams.Builder(end).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
        var context = new LootContext.Builder(params).create(java.util.Optional.empty());
        context.setQueriedLootTableId(BuiltInLootTables.END_CITY_TREASURE.location());

        var wrongItem = loot(item("void_tome"));
        apply.invoke(modifier, wrongItem, context);
        h.assertTrue(!data.contains("LootedFirstEndCityChest") && has(wrongItem, item("void_tome")),
                "Void tome still triggered the fruit reward");
        data.putBoolean("SevenCursesBearing", false);
        var uncursed = loot(item("astral_fruit"));
        apply.invoke(modifier, uncursed, context);
        h.assertTrue(!has(uncursed, item("enchanted_astral_fruit")), "Uncursed player received the reward");
        data.putBoolean("SevenCursesBearing", true);
        var otherContext = new LootContext.Builder(params).create(java.util.Optional.empty());
        otherContext.setQueriedLootTableId(ResourceLocation.parse("minecraft:chests/simple_dungeon"));
        var otherChest = loot(item("astral_fruit"));
        apply.invoke(modifier, otherChest, otherContext);
        h.assertTrue(!data.contains("LootedFirstEndCityChest"), "Other chest consumed the reward");
        var reward = loot(item("astral_fruit"));
        apply.invoke(modifier, reward, context);
        h.assertTrue(has(reward, item("enchanted_astral_fruit")) && !has(reward, item("astral_fruit"))
                && has(reward, Items.EMERALD) && data.getBoolean("LootedFirstEndCityChest"),
                "First eligible fruit was not upgraded through the native modifier");
        var repeat = loot(item("astral_fruit"));
        apply.invoke(modifier, repeat, context);
        h.assertTrue(has(repeat, item("astral_fruit")) && !has(repeat, item("enchanted_astral_fruit")),
                "Reward repeated after the native claim flag was set");
        h.succeed();
    }

    private static ObjectArrayList<ItemStack> loot(Item item) {
        return new ObjectArrayList<>(java.util.List.of(new ItemStack(Items.DIAMOND),
                new ItemStack(Items.EMERALD), new ItemStack(item)));
    }
    private static boolean has(ObjectArrayList<ItemStack> loot, Item item) {
        return loot.stream().anyMatch(stack -> stack.is(item));
    }
}
