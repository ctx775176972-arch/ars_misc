package dev.arsmisc.recipe;

import com.hollingsworth.arsnouveau.common.crafting.recipes.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.arsmisc.item.NetheriteEnchantersSwordItem;
import dev.arsmisc.registry.ModBookRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.util.Unit;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.*;
import java.util.List;

/** Retain center-item data while replacing the old weapon's built-in attributes. */
public final class SwordUpgradeRecipe extends EnchantingApparatusRecipe {
    public static final MapCodec<SwordUpgradeRecipe> CODEC=RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("reagent").forGetter(SwordUpgradeRecipe::reagent),
            ItemStack.CODEC.fieldOf("result").forGetter(SwordUpgradeRecipe::result),
            Ingredient.CODEC.listOf().fieldOf("pedestalItems").forGetter(SwordUpgradeRecipe::pedestalItems),
            Codec.INT.optionalFieldOf("sourceCost",0).forGetter(SwordUpgradeRecipe::sourceCost)
    ).apply(instance,SwordUpgradeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,SwordUpgradeRecipe> STREAM_CODEC=StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,SwordUpgradeRecipe::reagent,ItemStack.STREAM_CODEC,SwordUpgradeRecipe::result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),SwordUpgradeRecipe::pedestalItems,
            ByteBufCodecs.INT,SwordUpgradeRecipe::sourceCost,SwordUpgradeRecipe::new);
    public SwordUpgradeRecipe(Ingredient reagent,ItemStack result,List<Ingredient> materials,int sourceCost) {
        super(reagent,result,materials,sourceCost,true);
    }
    @Override public ItemStack assemble(ApparatusRecipeInput input,HolderLookup.Provider registries) {
        if (!reagent().test(input.catalyst())) return ItemStack.EMPTY;
        var result=result().copyWithCount(1);
        result.applyComponents(input.catalyst().getComponentsPatch());
        var builder=ItemAttributeModifiers.builder();
        NetheriteEnchantersSwordItem.attributes().modifiers().forEach(entry -> builder.add(entry.attribute(),entry.modifier(),entry.slot()));
        input.catalyst().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).modifiers().forEach(entry -> {
            var id=entry.modifier().id();
            if (!id.equals(Item.BASE_ATTACK_DAMAGE_ID) && !id.equals(Item.BASE_ATTACK_SPEED_ID)
                    && !id.toString().equals("ars_nouveau:sword_spell_bonus")
                    && !id.equals(NetheriteEnchantersSwordItem.SPELL_BONUS))
                builder.add(entry.attribute(),entry.modifier(),entry.slot());
        });
        result.set(DataComponents.ATTRIBUTE_MODIFIERS,builder.build());
        result.set(DataComponents.FIRE_RESISTANT,Unit.INSTANCE);
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer() { return ModBookRecipes.SWORD_UPGRADE.get(); }
}
