package dev.arsmisc;

import com.hollingsworth.arsnouveau.common.crafting.recipes.ApparatusRecipeInput;
import com.hollingsworth.arsnouveau.common.items.data.ArmorPerkHolder;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import dev.arsmisc.recipe.MageArmorUpgradeRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class MageArmorUpgradeGameTests {
    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }
    private static MageArmorUpgradeRecipe recipe(GameTestHelper h,String part,String path) {
        return (MageArmorUpgradeRecipe)h.getLevel().getRecipeManager().byKey(
                ResourceLocation.parse("ars_misc:netherite_mage_"+part+"_from_"+path)).orElseThrow().value();
    }
    private static List<ItemStack> materials(MageArmorUpgradeRecipe recipe) {
        return recipe.pedestalItems().stream().map(ingredient->ingredient.getItems()[0].copy()).toList();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void magePathRequiresTierThreeAndCorrectPartForAllNativeStyles(GameTestHelper h) {
        for(var names:List.of(new String[]{"helmet","hood"},new String[]{"chestplate","robes"},
                new String[]{"leggings","leggings"},new String[]{"boots","boots"})) {
            var recipe=recipe(h,names[0],"mage");var materials=materials(recipe);
            h.assertTrue(java.util.Arrays.stream(recipe.reagent().getItems()).allMatch(stack ->
                    stack.get(DataComponentRegistry.ARMOR_PERKS.get()).getTier()==2),"Recipe viewer center does not display tier III");
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try {
                MageArmorUpgradeRecipe.STREAM_CODEC.encode(buffer,recipe);
                var synced=MageArmorUpgradeRecipe.STREAM_CODEC.decode(buffer);
                h.assertTrue(java.util.Arrays.stream(synced.reagent().getItems()).allMatch(stack ->
                        stack.get(DataComponentRegistry.ARMOR_PERKS.get()).getTier()==2),"Client recipe sync lost tier III display");
            } finally {buffer.release();}
            h.assertTrue(materials.size()==2&&materials.stream().anyMatch(s->s.is(Items.NETHERITE_INGOT))
                    &&materials.stream().anyMatch(s->s.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),"Wrong mage path materials");
            for(var style:List.of("arcanist","sorcerer","battlemage")) {
                var source=item("ars_nouveau:"+style+"_"+names[1]).getDefaultInstance();
                h.assertTrue(!source.isEmpty(),"Missing native armor");
                for(int tier=0;tier<=2;tier++) {
                    source.set(DataComponentRegistry.ARMOR_PERKS.get(),new ArmorPerkHolder().setTier(tier));
                    var input=new ApparatusRecipeInput(source,materials,null);
                    h.assertTrue(recipe.matches(input,h.getLevel(),null)==(tier==2),"Wrong tier gate: "+style+" / "+tier);
                    var result=recipe.assemble(input,h.getLevel().registryAccess());
                    h.assertTrue(tier==2?result.is(item("ars_misc:netherite_mage_"+names[0])):result.isEmpty(),"Assembly bypassed tier gate");
                }
            }
            var wrong=new ApparatusRecipeInput(new ItemStack(Items.IRON_CHESTPLATE),materials,null);
            h.assertTrue(!recipe.matches(wrong,h.getLevel(),null),"Unrelated armor accepted");
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void netheritePathUsesFullMageMaterialsAndKeepsResultDefaults(GameTestHelper h) {
        for(var part:List.of("helmet","chestplate","leggings","boots")) {
            var recipe=recipe(h,part,"netherite");var materials=materials(recipe);
            h.assertTrue(materials.size()==6&&materials.stream().filter(s->s.is(Items.BLAZE_ROD)).count()==2
                    &&materials.stream().filter(s->s.is(Items.ENDER_PEARL)).count()==2
                    &&materials.stream().anyMatch(s->s.is(Items.CHORUS_FRUIT))
                    &&materials.stream().anyMatch(s->s.is(item("ars_nouveau:magebloom_block"))),"Wrong netherite path materials");
            var source=item("minecraft:netherite_"+part).getDefaultInstance();source.setDamageValue(80);
            source.remove(DataComponents.FIRE_RESISTANT);
            var input=new ApparatusRecipeInput(source,materials,null);
            h.assertTrue(recipe.matches(input,h.getLevel(),null),"Valid netherite path rejected");
            h.assertTrue(!recipe.matches(new ApparatusRecipeInput(source,materials.subList(0,5),null),h.getLevel(),null),"Missing pedestal accepted");
            var result=recipe.assemble(input,h.getLevel().registryAccess());
            h.assertTrue(result.is(item("ars_misc:netherite_mage_"+part))&&result.getMaxDamage()==7500&&result.getDamageValue()==80
                    &&result.has(DataComponents.FIRE_RESISTANT)&&result.get(DataComponentRegistry.ARMOR_PERKS.get()).getTier()==2,
                    "Upgrade lost damage, fire resistance or native thread tier");
            h.assertTrue(recipe.sourceCost()==0,"Unexpected Source cost");
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void upgradeRetainsEnchantmentsThreadsAndCustomComponents(GameTestHelper h) {
        var recipe=recipe(h,"chestplate","mage");
        var source=item("ars_nouveau:arcanist_robes").getDefaultInstance();
        var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("progress",42);
        var holder=new ArmorPerkHolder().setTier(2).setColor("green").setPerks(List.of(
                com.hollingsworth.arsnouveau.common.perk.RepairingPerk.INSTANCE,
                com.hollingsworth.arsnouveau.common.perk.EmptyPerk.INSTANCE,
                com.hollingsworth.arsnouveau.common.perk.EmptyPerk.INSTANCE))
                .setTagForPerk(com.hollingsworth.arsnouveau.common.perk.RepairingPerk.INSTANCE,tag);
        source.set(DataComponentRegistry.ARMOR_PERKS.get(),holder);
        source.set(DataComponentRegistry.UNBREAKING.get(),true);
        source.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Adventure Robe"));
        source.set(DataComponents.BASE_COLOR,DyeColor.GREEN);source.setDamageValue(50);
        source.enchant(h.getLevel().holderOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION),4);
        var result=recipe.assemble(new ApparatusRecipeInput(source,materials(recipe),null),h.getLevel().registryAccess());
        h.assertTrue(result.get(DataComponentRegistry.ARMOR_PERKS.get()).equals(holder)
                &&result.has(DataComponentRegistry.UNBREAKING.get())
                &&result.getEnchantments().equals(source.getEnchantments())
                &&result.getHoverName().getString().equals("Adventure Robe")&&result.get(DataComponents.BASE_COLOR)==DyeColor.GREEN
                &&result.getDamageValue()==50,"Equipment data was lost during upgrade");
        h.assertTrue(source.is(item("ars_nouveau:arcanist_robes"))&&source.get(DataComponentRegistry.ARMOR_PERKS.get()).equals(holder),"Input armor was mutated");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void eachPiecePreventsGoldAggroAndDroppedItemsSurviveLava(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(var piece:List.of(ArsMiscMod.NETHERITE_MAGE_HELMET.get(),ArsMiscMod.NETHERITE_MAGE_CHESTPLATE.get(),
                ArsMiscMod.NETHERITE_MAGE_LEGGINGS.get(),ArsMiscMod.NETHERITE_MAGE_BOOTS.get())) {
            player.setItemSlot(piece.getEquipmentSlot(),piece.getDefaultInstance());
            h.assertTrue(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingGold(player),"Piglins do not recognize mage armor");
            player.setItemSlot(piece.getEquipmentSlot(),ItemStack.EMPTY);
            h.assertTrue(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingGold(player),"Piglin neutrality remained after unequip");
            var drop=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),0,0,0,piece.getDefaultInstance());
            h.assertTrue(!drop.hurt(drop.damageSources().lava(),100)&&!drop.isRemoved(),"Lava destroyed fire-resistant armor");
        }
        h.succeed();
    }
}
