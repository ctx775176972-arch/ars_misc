package dev.arsmisc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class LecternBookGameTests {
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void taggedModBooksCanBeOpenedAndRetrievedWithComponents(GameTestHelper h) {
        var player = ReactiveShieldGameTests.player(h);
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        for (String id : new String[]{"ars_nouveau:novice_spell_book", "ars_nouveau:apprentice_spell_book",
                "ars_nouveau:archmage_spell_book", "ars_nouveau:creative_spell_book",
                "ars_nouveau:caster_tome", "ars_nouveau:worn_notebook",
                "ars_elemental:water_caster_tome", "irons_spellbooks:iron_spell_book"}) {
            var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
            if (item.isEmpty()) continue;
            player.getInventory().clearContent();
            h.getLevel().setBlockAndUpdate(pos, Blocks.LECTERN.defaultBlockState());
            var stack = item.get().getDefaultInstance();
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Lectern preservation test"));
            var original = stack.copy();
            h.assertTrue(stack.is(ItemTags.LECTERN_BOOKS), "Integration book is missing the placement tag: " + id);
            h.assertTrue(LecternBlock.tryPlaceBook(player, h.getLevel(), pos,
                    h.getLevel().getBlockState(pos), stack), "Book placement failed: " + id);
            var lectern = (LecternBlockEntity)h.getLevel().getBlockEntity(pos);
            var menu = (LecternMenu)lectern.createMenu(1, player.getInventory(), player);
            h.assertTrue(lectern.hasBook() && menu.stillValid(player), "Lectern immediately invalidated the book menu: " + id);
            h.assertTrue(menu.clickMenuButton(player, LecternMenu.BUTTON_TAKE_BOOK), "Take Book failed: " + id);
            h.assertTrue(lectern.getBook().isEmpty() && !h.getLevel().getBlockState(pos).getValue(LecternBlock.HAS_BOOK)
                    && !menu.stillValid(player), "Taking the book did not clear and invalidate the lectern: " + id);
            h.assertTrue(player.getInventory().items.stream().filter(s -> ItemStack.isSameItemSameComponents(s, original))
                    .mapToInt(ItemStack::getCount).sum() == 1, "Retrieved book was changed, lost, or duplicated: " + id);
        }
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void vanillaBooksPermissionsDistanceAndInvalidItemsRemainUnchanged(GameTestHelper h) {
        var player = ReactiveShieldGameTests.player(h);
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(pos, Blocks.LECTERN.defaultBlockState());
        var lectern = (LecternBlockEntity)h.getLevel().getBlockEntity(pos);
        for (Item item : new Item[]{Items.WRITABLE_BOOK, Items.WRITTEN_BOOK}) {
            lectern.setBook(new ItemStack(item));
            var menu = (LecternMenu)lectern.createMenu(2, player.getInventory(), player);
            h.assertTrue(lectern.hasBook() && menu.stillValid(player), "Vanilla book menu stopped working");
            double x = player.getX(), y = player.getY(), z = player.getZ();
            player.setPos(pos.getX() + 30, pos.getY(), pos.getZ());
            h.assertTrue(!menu.stillValid(player), "Out-of-range menu remained valid");
            player.setPos(x, y, z);
            player.getAbilities().mayBuild = false;
            h.assertTrue(!menu.clickMenuButton(player, LecternMenu.BUTTON_TAKE_BOOK) && !lectern.getBook().isEmpty(),
                    "Player without build permission could take the book");
            player.getAbilities().mayBuild = true;
        }
        lectern.setBook(new ItemStack(Items.DIAMOND));
        h.assertTrue(!lectern.hasBook(), "An unrelated item became a valid lectern book");
        lectern.setBook(ItemStack.EMPTY);
        h.assertTrue(!lectern.hasBook(), "An empty lectern became valid");
        h.succeed();
    }
}
