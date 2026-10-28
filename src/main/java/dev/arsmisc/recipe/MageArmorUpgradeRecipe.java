package dev.arsmisc.recipe;

import com.hollingsworth.arsnouveau.api.util.PerkUtil;
import com.hollingsworth.arsnouveau.common.crafting.recipes.*;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.arsmisc.registry.ModBookRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;

/** Both armor paths retain center-item data and the result's native fire resistance and thread tier. */
public final class MageArmorUpgradeRecipe extends EnchantingApparatusRecipe {
    public static final MapCodec<MageArmorUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("reagent").forGetter(MageArmorUpgradeRecipe::reagent),
            ItemStack.CODEC.fieldOf("result").forGetter(MageArmorUpgradeRecipe::result),
            Ingredient.CODEC.listOf().fieldOf("pedestalItems").forGetter(MageArmorUpgradeRecipe::pedestalItems),
            Codec.INT.optionalFieldOf("sourceCost",0).forGetter(MageArmorUpgradeRecipe::sourceCost),
            Codec.BOOL.optionalFieldOf("requiresMageTierThree",false).forGetter(recipe -> recipe.requiresMageTierThree)
    ).apply(instance,MageArmorUpgradeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,MageArmorUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,MageArmorUpgradeRecipe::reagent,
            ItemStack.STREAM_CODEC,MageArmorUpgradeRecipe::result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),MageArmorUpgradeRecipe::pedestalItems,
            ByteBufCodecs.INT,MageArmorUpgradeRecipe::sourceCost,
            ByteBufCodecs.BOOL,recipe -> recipe.requiresMageTierThree,MageArmorUpgradeRecipe::new);
    private final boolean requiresMageTierThree;
    public MageArmorUpgradeRecipe(Ingredient reagent,ItemStack result,List<Ingredient> materials,int sourceCost,boolean tierThree) {
        super(tierThree ? tierThreeDisplay(reagent) : reagent,result,materials,sourceCost,true);requiresMageTierThree=tierThree;
    }
    private static Ingredient tierThreeDisplay(Ingredient reagent) {
        // Recipe viewers use these stacks. Keep the component-aware tier check in validCatalyst.
        return Ingredient.of(java.util.Arrays.stream(reagent.getItems()).map(stack -> {
            var display=stack.copy();
            var holder=PerkUtil.getPerkHolder(display);
            if (holder!=null) display.set(DataComponentRegistry.ARMOR_PERKS.get(),holder.setTier(2));
            return display;
        }));
    }
    private boolean validCatalyst(ItemStack stack) {
        if (!reagent().test(stack)) return false;
        var holder=PerkUtil.getPerkHolder(stack);
        return !requiresMageTierThree || holder!=null && holder.getTier()>=2;
    }
    @Override public boolean doesReagentMatch(ApparatusRecipeInput input,Level level,Player player) {
        return validCatalyst(input.catalyst());
    }
    @Override public ItemStack assemble(ApparatusRecipeInput input,HolderLookup.Provider registries) {
        if (!validCatalyst(input.catalyst())) return ItemStack.EMPTY;
        var result=result().copyWithCount(1);
        result.applyComponents(input.catalyst().getComponentsPatch());
        var holder=PerkUtil.getPerkHolder(result);
        if (holder!=null) result.set(DataComponentRegistry.ARMOR_PERKS.get(),holder.setTier(2));
        result.set(DataComponents.FIRE_RESISTANT,Unit.INSTANCE);
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer() { return ModBookRecipes.MAGE_ARMOR_UPGRADE.get(); }
}
