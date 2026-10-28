package dev.arsmisc.recipe;

import com.hollingsworth.arsnouveau.common.crafting.recipes.*;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.arsmisc.registry.ModBookComponents;
import dev.arsmisc.registry.ModBookRecipes;
import dev.arsmisc.spell.SpellbookBlessing;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class SpellbookBlessingRecipe extends EnchantingApparatusRecipe {
    public static final MapCodec<SpellbookBlessingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.listOf().fieldOf("pedestalItems").forGetter(SpellbookBlessingRecipe::pedestalItems),
            Codec.INT.optionalFieldOf("sourceCost", 0).forGetter(SpellbookBlessingRecipe::sourceCost)
    ).apply(instance, SpellbookBlessingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpellbookBlessingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), SpellbookBlessingRecipe::pedestalItems,
            ByteBufCodecs.INT, SpellbookBlessingRecipe::sourceCost, SpellbookBlessingRecipe::new);
    public SpellbookBlessingRecipe(java.util.List<Ingredient> materials, int sourceCost) {
        super(Ingredient.of(BuiltInRegistries.ITEM.stream().filter(item -> item instanceof SpellBook).toArray(Item[]::new)),
                displayResult(), materials, sourceCost, true);
    }
    private static ItemStack displayResult() {
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        book.set(ModBookComponents.BLESSING.get(), true);
        return book;
    }
    @Override public boolean doesReagentMatch(ApparatusRecipeInput input, Level level, Player player) {
        return input.catalyst().getItem() instanceof SpellBook && !SpellbookBlessing.blessed(input.catalyst());
    }
    @Override public ItemStack assemble(ApparatusRecipeInput input, HolderLookup.Provider registries) {
        if (!(input.catalyst().getItem() instanceof SpellBook) || SpellbookBlessing.blessed(input.catalyst())) return ItemStack.EMPTY;
        var result = input.catalyst().copyWithCount(1);
        result.set(ModBookComponents.BLESSING.get(), true);
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer() { return ModBookRecipes.BLESSING.get(); }
}
