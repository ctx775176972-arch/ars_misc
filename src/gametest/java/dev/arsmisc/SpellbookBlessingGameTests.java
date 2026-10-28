package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.crafting.recipes.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.*;
import dev.arsmisc.recipe.SpellbookBlessingRecipe;
import dev.arsmisc.registry.ModBookComponents;
import dev.arsmisc.spell.SpellbookBlessing;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class SpellbookBlessingGameTests {
    private static SpellbookBlessingRecipe recipe(GameTestHelper h) {
        return (SpellbookBlessingRecipe) h.getLevel().getRecipeManager()
                .byKey(net.minecraft.resources.ResourceLocation.parse("ars_misc:spellbook_blessing")).orElseThrow().value();
    }
    private static ItemStack book() {
        var stack = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        stack.set(ModBookComponents.BLESSING.get(), true);
        return stack;
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void apparatusPreservesAnyBookAndRejectsRepeatedBlessing(GameTestHelper h) {
        var recipe = recipe(h);
        var materials = java.util.List.of(new ItemStack(ItemsRegistry.WILDEN_TRIBUTE.get()), new ItemStack(Items.BEACON),
                new ItemStack(Items.DRAGON_HEAD), new ItemStack(Items.ECHO_SHARD), new ItemStack(Items.NETHERITE_BLOCK),
                new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.HEAVY_CORE), new ItemStack(Items.CONDUIT));
        for (var item : java.util.List.of(ItemsRegistry.NOVICE_SPELLBOOK.get(), ItemsRegistry.APPRENTICE_SPELLBOOK.get(), ItemsRegistry.ARCHMAGE_SPELLBOOK.get())) {
            var original = item.getDefaultInstance();
            original.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Adventure"));
            SpellCasterRegistry.from(original).setSpell(new Spell(MethodSelf.INSTANCE, EffectRegeneration.INSTANCE)).saveToStack(original);
            var input = new ApparatusRecipeInput(original, materials, null);
            h.assertTrue(recipe.matches(input, h.getLevel(), null), "Actual apparatus materials did not match");
            var result = recipe.assemble(input, h.getLevel().registryAccess());
            h.assertTrue(result.is(item) && SpellbookBlessing.blessed(result) && !SpellbookBlessing.blessed(original), "Blessing changed tier or mutated input");
            h.assertTrue(result.getHoverName().getString().equals("Adventure") && SpellCasterRegistry.from(result).getSpell().size() == 2, "Blessing lost book data");
            h.assertTrue(!recipe.matches(new ApparatusRecipeInput(result, materials, null), h.getLevel(), null), "Repeated blessing accepted");
        }
        h.assertTrue(!recipe.matches(new ApparatusRecipeInput(new ItemStack(Items.BOOK), materials, null), h.getLevel(), null), "Ordinary book accepted");
        h.assertTrue(!recipe.matches(new ApparatusRecipeInput(ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance(), materials.subList(0,7), null), h.getLevel(), null), "Missing material accepted");
        h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void multipleBooksProvideOneBonusAndLastRemovalClearsIt(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        double mana = player.getAttributeValue(PerkAttributes.MAX_MANA), regen = player.getAttributeValue(PerkAttributes.MANA_REGEN_BONUS), damage = player.getAttributeValue(PerkAttributes.SPELL_DAMAGE_BONUS);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, book());
        SpellbookBlessing.refresh(player);
        h.assertTrue(player.getAttributeValue(PerkAttributes.MAX_MANA) == mana, "Held book granted equipped bonus");
        inventory.setEquippedCurio("spellbook",0,book()); inventory.setEquippedCurio("ring",0,book());
        for (int i=0;i<5;i++) SpellbookBlessing.refresh(player);
        h.assertTrue(player.getAttributeValue(PerkAttributes.MAX_MANA) == mana+600 && player.getAttributeValue(PerkAttributes.MANA_REGEN_BONUS) == regen+50 && player.getAttributeValue(PerkAttributes.SPELL_DAMAGE_BONUS) == damage+8, "Multiple books stacked or bonus missing");
        inventory.setEquippedCurio("spellbook",0,ItemStack.EMPTY); SpellbookBlessing.refresh(player);
        h.assertTrue(player.getAttributeValue(PerkAttributes.MAX_MANA) == mana+600, "Removing one book removed remaining blessing");
        inventory.loadInventory(inventory.saveInventory(false)); SpellbookBlessing.refresh(player);
        h.assertTrue(player.getAttributeValue(PerkAttributes.MAX_MANA) == mana+600, "Reload changed bonus");
        inventory.setSlotActive("ring",0,false); SpellbookBlessing.refresh(player);
        h.assertTrue(player.getAttributeValue(PerkAttributes.MAX_MANA) == mana, "Inactive slot granted bonus");
        inventory.setSlotActive("ring",0,true); SpellbookBlessing.refresh(player);
        inventory.setEquippedCurio("ring",0,ItemStack.EMPTY); SpellbookBlessing.refresh(player);
        h.assertTrue(player.getAttributeValue(PerkAttributes.MAX_MANA) == mana && player.getAttributeValue(PerkAttributes.MANA_REGEN_BONUS) == regen && player.getAttributeValue(PerkAttributes.SPELL_DAMAGE_BONUS) == damage, "Last removal retained bonuses");
        h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void originalUpgradeRecipesPreserveBlessingAndSpells(GameTestHelper h) {
        var book = book();
        SpellCasterRegistry.from(book).setSpell(new Spell(MethodSelf.INSTANCE, EffectRegeneration.INSTANCE),1).setCurrentSlot(1).saveToStack(book);
        for (String id : new String[]{"ars_nouveau:apprentice_book_upgrade", "ars_nouveau:archmage_book_upgrade"}) {
            var upgrade = (BookUpgradeRecipe) h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(id)).orElseThrow().value();
            book = upgrade.assemble(CraftingInput.of(1,1,java.util.List.of(book)), h.getLevel().registryAccess());
            h.assertTrue(SpellbookBlessing.blessed(book) && SpellCasterRegistry.from(book).getCurrentSlot()==1 && SpellCasterRegistry.from(book).getSpell().size()==2, "Upgrade lost blessing or spell");
        }
        h.assertTrue(book.is(ItemsRegistry.ARCHMAGE_SPELLBOOK.get()), "Wrong upgraded item"); h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void oldManaEnchantmentsNoLongerGrantCurioBonus(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL); var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        var holder = h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(EnchantmentRegistry.MANA_BOOST_ENCHANTMENT);
        var recipe = new EnchantmentRecipe(java.util.List.of(), EnchantmentRegistry.MANA_BOOST_ENCHANTMENT,1,2000);
        h.assertTrue(!recipe.doesReagentMatch(new ApparatusRecipeInput(book,java.util.List.of(),null), h.getLevel(),null), "Old apparatus enchantment compatibility remained");
        book.enchant(holder,3); CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("spellbook",0,book);
        double before = player.getAttributeValue(PerkAttributes.MAX_MANA);
        new top.theillusivec4.curios.common.event.CuriosEventHandler().tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
        SpellbookBlessing.refresh(player);
        h.assertTrue(player.getAttributeValue(PerkAttributes.MAX_MANA) == before, "Old enchantment still granted curio mana"); h.succeed();
    }
}
