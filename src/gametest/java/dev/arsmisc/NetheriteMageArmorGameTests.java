package dev.arsmisc;

import net.minecraft.gametest.framework.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class NetheriteMageArmorGameTests {
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void equippedPiecesDiscountNativeSpellQuoteAndActualManaCost(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h);
        var spell=new com.hollingsworth.arsnouveau.api.spell.Spell(
                com.hollingsworth.arsnouveau.common.spell.method.MethodSelf.INSTANCE,EffectCrystalShield.INSTANCE);
        var resolver=new com.hollingsworth.arsnouveau.api.spell.SpellResolver(
                com.hollingsworth.arsnouveau.api.spell.SpellContext.fromEntity(spell,p,ItemStack.EMPTY));
        int baseline=resolver.getResolveCost();
        var set=java.util.List.of(ArsMiscMod.NETHERITE_MAGE_HELMET.get(),ArsMiscMod.NETHERITE_MAGE_CHESTPLATE.get(),
                ArsMiscMod.NETHERITE_MAGE_LEGGINGS.get(),ArsMiscMod.NETHERITE_MAGE_BOOTS.get());
        int count=0;
        for(var item:set) {
            p.setItemSlot(item.getEquipmentSlot(),item.getDefaultInstance());count++;
            h.assertTrue(resolver.getResolveCost()==Math.max(0,baseline-count*10)
                    &&resolver.getExpendedCost()==Math.max(0,baseline-count*10),"Incorrect per-piece fixed mana discount");
        }
        double before=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(p).getCurrentMana();
        int cost=resolver.getExpendedCost();resolver.expendMana();
        h.assertTrue(Math.abs(com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(p).getCurrentMana()-before+cost)<.001,
                "Actual mana payment differs from quoted discount");
        var cheap=new com.hollingsworth.arsnouveau.api.spell.SpellResolver(
                com.hollingsworth.arsnouveau.api.spell.SpellContext.fromEntity(new com.hollingsworth.arsnouveau.api.spell.Spell(
                        com.hollingsworth.arsnouveau.common.spell.method.MethodSelf.INSTANCE,
                        com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm.INSTANCE),p,ItemStack.EMPTY));
        h.assertTrue(cheap.getResolveCost()==0&&cheap.getExpendedCost()==0,"Cheap spells did not clamp at zero");
        for(var item:set)p.setItemSlot(item.getEquipmentSlot(),ItemStack.EMPTY);
        h.assertTrue(resolver.getResolveCost()==baseline,"Unequipped armor retained discount");h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void nativeRepairingThreadMakesTheSetUnbreakable(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var chest = ArsMiscMod.NETHERITE_MAGE_CHESTPLATE.get().getDefaultInstance();
        var holder = com.hollingsworth.arsnouveau.api.util.PerkUtil.getPerkHolder(chest);
        var perks = holder.getSlotsForTier(chest).stream().map(slot -> (com.hollingsworth.arsnouveau.api.perk.IPerk)
                (slot.value() == 3 ? com.hollingsworth.arsnouveau.common.perk.RepairingPerk.INSTANCE
                        : com.hollingsworth.arsnouveau.common.perk.EmptyPerk.INSTANCE)).toList();
        chest.set(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.ARMOR_PERKS.get(), holder.setPerks(perks));
        var hat = ArsMiscMod.NETHERITE_MAGE_HELMET.get().getDefaultInstance();
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, hat);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        player.tick();
        h.assertTrue(com.hollingsworth.arsnouveau.api.util.PerkUtil.countForPerk(
                com.hollingsworth.arsnouveau.common.perk.RepairingPerk.INSTANCE, player) == 3,
                "Native repairing thread was not recognized at level III");
        h.assertTrue(!hat.isDamageableItem() && !chest.isDamageableItem(), "Native repairing III did not protect custom armor");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.tick();
        h.assertTrue(hat.isDamageableItem(), "Removing repairing thread retained unbreakability");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void robeSetUsesNetheriteDefenseManaAndCustomThreadSlots(GameTestHelper h) {
        var custom = new ArmorItem[]{ArsMiscMod.NETHERITE_MAGE_HELMET.get(), ArsMiscMod.NETHERITE_MAGE_CHESTPLATE.get(),
                ArsMiscMod.NETHERITE_MAGE_LEGGINGS.get(), ArsMiscMod.NETHERITE_MAGE_BOOTS.get()};
        int[] defense = {3, 8, 6, 3};
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var attributes = java.util.List.of(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS,
                net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
                com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MAX_MANA,
                com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MANA_REGEN_BONUS,
                com.hollingsworth.arsnouveau.api.perk.PerkAttributes.SPELL_DAMAGE_BONUS);
        double[] baseline = attributes.stream().mapToDouble(player::getAttributeValue).toArray();
        var tags = java.util.List.of(ItemTags.HEAD_ARMOR, ItemTags.CHEST_ARMOR, ItemTags.LEG_ARMOR, ItemTags.FOOT_ARMOR);
        for (int i = 0; i < custom.length; i++) {
            var item = custom[i];
            var stack = item.getDefaultInstance();
            h.assertTrue(item.getDefense() == defense[i] && item.getToughness() == 3
                    && stack.getMaxDamage() == 7500 && item.getEnchantmentValue() == 30
                    && stack.getRarity() == Rarity.UNCOMMON,
                    "Mage armor differs from intended numeric stats: " + item);
            h.assertTrue(stack.has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT)
                    && item.isValidRepairItem(stack, new ItemStack(Items.NETHERITE_INGOT))
                    && !item.isValidRepairItem(stack, new ItemStack(Items.IRON_INGOT)),
                    "Incorrect fire resistance or repair material: " + item);
            h.assertTrue(stack.is(tags.get(i)) && stack.is(ItemTags.ARMOR_ENCHANTABLE)
                    && stack.is(ItemTags.DURABILITY_ENCHANTABLE), "Missing native armor enchantment tags: " + item);
            var holder = com.hollingsworth.arsnouveau.api.util.PerkUtil.getPerkHolder(stack);
            var slots = holder.getSlotsForTier(stack).stream().map(com.hollingsworth.arsnouveau.api.perk.PerkSlot::value).toList();
            h.assertTrue(slots.equals(i == 0 || i == 3 ? java.util.List.of(3, 2, 1) : java.util.List.of(3, 2, 2)),
                    "Incorrect custom thread slots: " + slots);
            player.setItemSlot(item.getEquipmentSlot(), stack);
        }
        player.tick();
        double[] expected = {20, 12, 0.4, 600, 24, 0};
        for (int i = 0; i < attributes.size(); i++) h.assertTrue(
                Math.abs(player.getAttributeValue(attributes.get(i)) - baseline[i] - expected[i]) < 0.00001,
                "Incorrect equipped set attribute: " + attributes.get(i));
        for (var item : custom) player.setItemSlot(item.getEquipmentSlot(), ItemStack.EMPTY);
        player.tick();
        for (int i = 0; i < attributes.size(); i++) h.assertTrue(
                Math.abs(player.getAttributeValue(attributes.get(i)) - baseline[i]) < 0.00001,
                "Unequipping retained an armor attribute");
        h.succeed();
    }
}
