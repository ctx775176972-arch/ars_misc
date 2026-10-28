package dev.arsmisc.client.jei;

import com.hollingsworth.arsnouveau.common.crafting.recipes.ReactiveEnchantmentRecipe;
import com.hollingsworth.arsnouveau.setup.registry.EnchantmentRegistry;
import com.hollingsworth.arsnouveau.setup.registry.RecipeRegistry;
import dev.arsmisc.ArsMiscMod;
import mezz.jei.api.*;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Optional JEI integration: hide Reactive application displays without changing actual recipes. */
@JeiPlugin
public final class MiscJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "reactive_recipe_hider");
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        var type=RecipeType.createFromVanilla(RecipeRegistry.ENCHANTMENT_TYPE.get());
        var manager=runtime.getRecipeManager();
        var reactive=manager.createRecipeLookup(type).includeHidden().get()
                .filter(holder -> holder.value() instanceof ReactiveEnchantmentRecipe
                        || EnchantmentRegistry.REACTIVE_ENCHANTMENT.equals(holder.value().enchantmentKey()))
                .toList();
        if (!reactive.isEmpty()) manager.hideRecipes(type,reactive);

        // JEI independently generates anvil recipes for every item accepting this enchantment.
        // Each native recipe groups book levels for one enchantment; leave mixed third-party recipes intact.
        var anvil=manager.createRecipeLookup(RecipeTypes.ANVIL).includeHidden().get()
                .filter(recipe -> !recipe.getRightInputs().isEmpty()
                        && recipe.getRightInputs().stream().allMatch(MiscJeiPlugin::hasReactive))
                .toList();
        if (!anvil.isEmpty()) manager.hideRecipes(RecipeTypes.ANVIL,anvil);
    }

    private static boolean hasReactive(ItemStack stack) {
        return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).keySet().stream()
                .anyMatch(holder -> holder.is(EnchantmentRegistry.REACTIVE_ENCHANTMENT))
                || stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet().stream()
                .anyMatch(holder -> holder.is(EnchantmentRegistry.REACTIVE_ENCHANTMENT));
    }
}
