package dev.arsmisc.registry;

import com.mojang.serialization.MapCodec;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.recipe.SpellbookBlessingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ModBookRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ArsMiscMod.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpellbookBlessingRecipe>> BLESSING = SERIALIZERS.register("spellbook_blessing", () -> new RecipeSerializer<>() {
        @Override public MapCodec<SpellbookBlessingRecipe> codec() { return SpellbookBlessingRecipe.CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SpellbookBlessingRecipe> streamCodec() { return SpellbookBlessingRecipe.STREAM_CODEC; }
    });
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<dev.arsmisc.recipe.MageArmorUpgradeRecipe>> MAGE_ARMOR_UPGRADE = SERIALIZERS.register("mage_armor_upgrade", () -> new RecipeSerializer<>() {
        @Override public MapCodec<dev.arsmisc.recipe.MageArmorUpgradeRecipe> codec() { return dev.arsmisc.recipe.MageArmorUpgradeRecipe.CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, dev.arsmisc.recipe.MageArmorUpgradeRecipe> streamCodec() { return dev.arsmisc.recipe.MageArmorUpgradeRecipe.STREAM_CODEC; }
    });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<dev.arsmisc.recipe.WandConversionRecipe>> WAND_CONVERSION = SERIALIZERS.register("wand_conversion", () -> new RecipeSerializer<>() {
        @Override public MapCodec<dev.arsmisc.recipe.WandConversionRecipe> codec() { return dev.arsmisc.recipe.WandConversionRecipe.CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, dev.arsmisc.recipe.WandConversionRecipe> streamCodec() { return dev.arsmisc.recipe.WandConversionRecipe.STREAM_CODEC; }
    });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<dev.arsmisc.recipe.SwordUpgradeRecipe>> SWORD_UPGRADE = SERIALIZERS.register("sword_upgrade", () -> new RecipeSerializer<>() {
        @Override public MapCodec<dev.arsmisc.recipe.SwordUpgradeRecipe> codec() { return dev.arsmisc.recipe.SwordUpgradeRecipe.CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, dev.arsmisc.recipe.SwordUpgradeRecipe> streamCodec() { return dev.arsmisc.recipe.SwordUpgradeRecipe.STREAM_CODEC; }
    });
}
