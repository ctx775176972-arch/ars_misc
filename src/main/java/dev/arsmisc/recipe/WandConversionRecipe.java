package dev.arsmisc.recipe;

import com.mojang.serialization.MapCodec;
import dev.arsmisc.registry.ModBookRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** A normal, visible shapeless recipe that carries the input wand's components. */
public final class WandConversionRecipe extends ShapelessRecipe {
    private static final ShapelessRecipe.Serializer VANILLA = new ShapelessRecipe.Serializer();
    public static final MapCodec<WandConversionRecipe> CODEC = VANILLA.codec().xmap(WandConversionRecipe::new, recipe -> recipe.template);
    public static final StreamCodec<RegistryFriendlyByteBuf, WandConversionRecipe> STREAM_CODEC = VANILLA.streamCodec().map(WandConversionRecipe::new, recipe -> recipe.template);
    private final ShapelessRecipe template;
    private WandConversionRecipe(ShapelessRecipe template) {
        // Vanilla's static result does not consult the registry lookup.
        super(template.getGroup(), template.category(), template.getResultItem(null), template.getIngredients());
        this.template = template;
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (input.ingredientCount() != 1) return ItemStack.EMPTY;
        ItemStack result = super.assemble(input, registries);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack source = input.getItem(slot);
            if (source.isEmpty()) continue;
            if (!(source.getItem() instanceof com.hollingsworth.arsnouveau.common.items.Wand)) return ItemStack.EMPTY;
            result.applyComponents(source.getComponentsPatch());
            return result;
        }
        return ItemStack.EMPTY;
    }
    @Override public RecipeSerializer<?> getSerializer() { return ModBookRecipes.WAND_CONVERSION.get(); }
}
