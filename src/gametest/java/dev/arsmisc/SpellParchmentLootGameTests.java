package dev.arsmisc;

import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class SpellParchmentLootGameTests {
    private static java.util.List<ItemStack> generate(GameTestHelper h, Item... items) {
        var table = LootTable.lootTable();
        for (var item : items) table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(item)));
        var params = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        return table.build().getRandomItems(params);
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void ordinaryLootAndNativeCasterTomesAreUnchanged(GameTestHelper h) {
        boolean previous = MiscConfig.DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT.get();
        try {
            MiscConfig.DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT.set(true);
            var loot = generate(h, Items.DIAMOND, ItemsRegistry.CASTER_TOME.get(), ItemsRegistry.SOURCE_GEM.get());
            h.assertTrue(loot.size() == 3 && loot.stream().anyMatch(stack -> stack.is(ItemsRegistry.CASTER_TOME.get())),
                    "Parchment filter removed unrelated loot or native caster tomes");
            h.succeed();
        } finally { MiscConfig.DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT.set(previous); }
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void installedParchmentIsFilteredOnlyWhenEnabled(GameTestHelper h) {
        var parchment = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("ars_additions:imbued_spell_parchment"));
        if (parchment.isEmpty()) { h.succeed(); return; }
        boolean previous = MiscConfig.DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT.get();
        try {
            var existing = parchment.get().getDefaultInstance();
            MiscConfig.DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT.set(false);
            var allowed = generate(h, parchment.get(), Items.DIAMOND);
            h.assertTrue(allowed.size() == 2 && allowed.stream().anyMatch(stack -> stack.is(parchment.get())),
                    "Disabled filter still removed parchment loot");
            MiscConfig.DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT.set(true);
            var filtered = generate(h, parchment.get(), Items.DIAMOND);
            h.assertTrue(filtered.size() == 1 && filtered.getFirst().is(Items.DIAMOND),
                    "Enabled filter did not remove exactly the parchment loot");
            h.assertTrue(!existing.isEmpty() && existing.is(parchment.get()), "Filtering changed an existing item");
            h.assertTrue(com.hollingsworth.arsnouveau.api.loot.DungeonLootTables.BASIC_LOOT.stream()
                    .anyMatch(supplier -> supplier.get().is(parchment.get())), "Ars Additions parchment loot injection did not load");
            var recipe = h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("ars_additions:imbue_spell_scroll/imbue_scroll"));
            h.assertTrue(recipe.isPresent(), "Parchment crafting recipe was removed");
            h.succeed();
        } finally { MiscConfig.DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT.set(previous); }
    }
}
