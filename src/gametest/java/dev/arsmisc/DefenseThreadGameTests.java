package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.IPerk;
import com.hollingsworth.arsnouveau.common.items.data.ArmorPerkHolder;
import com.hollingsworth.arsnouveau.common.perk.EmptyPerk;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class DefenseThreadGameTests {
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void percentageScalesEquipmentAndFlatArmorWithoutMultiplyingOtherPercentBonuses(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.HEAD,armor(ArsMiscMod.NETHERITE_MAGE_HELMET.get(),null,0));
        player.setItemSlot(EquipmentSlot.CHEST,armor(ArsMiscMod.NETHERITE_MAGE_CHESTPLATE.get(),DefensePerk.STURDY,3));
        player.setItemSlot(EquipmentSlot.LEGS,armor(ArsMiscMod.NETHERITE_MAGE_LEGGINGS.get(),null,0));
        player.setItemSlot(EquipmentSlot.FEET,armor(ArsMiscMod.NETHERITE_MAGE_BOOTS.get(),null,0));
        player.tick();DefensePerk.refresh(player);
        var attribute=player.getAttribute(Attributes.ARMOR);
        h.assertTrue(Math.abs(attribute.getValue()-32)<.00001,"Tier III did not turn 20 equipment armor into 32");
        var flat=net.minecraft.resources.ResourceLocation.parse("ars_misc:test_flat_armor");
        var percent=net.minecraft.resources.ResourceLocation.parse("ars_misc:test_percent_armor");
        attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(flat,4,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        h.assertTrue(Math.abs(attribute.getValue()-38.4)<.00001,"Thread did not scale external flat armor");
        attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(percent,.1,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        for(int i=0;i<5;i++)DefensePerk.refresh(player);
        h.assertTrue(Math.abs(attribute.getValue()-40.8)<.00001,"Percent bonuses multiplied each other or repeated refresh accumulated");
        attribute.removeModifier(flat);attribute.removeModifier(percent);
        player.setItemSlot(EquipmentSlot.CHEST,armor(ArsMiscMod.NETHERITE_MAGE_CHESTPLATE.get(),null,0));
        player.tick();DefensePerk.refresh(player);
        h.assertTrue(Math.abs(attribute.getValue()-20)<.00001,"Removing percentage thread did not restore equipment armor");h.succeed();
    }
    private static ItemStack armor(Item item, DefensePerk perk, int level) {
        var stack=item.getDefaultInstance();
        var holder=new ArmorPerkHolder().setTier(2);
        var perks=new java.util.ArrayList<IPerk>();
        boolean installed=false;
        for(var slot:holder.getSlotsForTier(stack)) {
            if(!installed && perk!=null && slot.value()==level) { perks.add(perk); installed=true; }
            else perks.add(EmptyPerk.INSTANCE);
        }
        if(perk!=null && !installed) throw new IllegalStateException("Missing requested thread slot");
        stack.set(DataComponentRegistry.ARMOR_PERKS.get(),holder.setPerks(perks));
        return stack;
    }

    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void defenseThreadsScaleWithNativeSlotLevel(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.CHEST,armor(ItemsRegistry.ARCANIST_ROBES.get(),null,0));
        player.tick();DefensePerk.refresh(player);
        double baseArmor=player.getAttributeValue(Attributes.ARMOR);
        double baseToughness=player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        for(int level=1;level<=3;level++) {
            player.setItemSlot(EquipmentSlot.CHEST,armor(ItemsRegistry.ARCANIST_ROBES.get(),DefensePerk.STURDY,level));
            player.tick();DefensePerk.refresh(player);
            h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR)-baseArmor*(1+level*.2))<.00001,"Wrong armor at level "+level);
            player.setItemSlot(EquipmentSlot.CHEST,armor(ItemsRegistry.ARCANIST_ROBES.get(),DefensePerk.RESILIENT,level));
            player.tick();DefensePerk.refresh(player);
            h.assertTrue(player.getAttributeValue(Attributes.ARMOR)==baseArmor
                    && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==baseToughness+level*4,"Wrong toughness or retained armor at level "+level);
        }
        player.setItemSlot(EquipmentSlot.CHEST,armor(ItemsRegistry.ARCANIST_ROBES.get(),null,0));
        player.tick();DefensePerk.refresh(player);
        h.assertTrue(player.getAttributeValue(Attributes.ARMOR)==baseArmor
                && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==baseToughness,"Removed thread retained defense");
        var registry=com.hollingsworth.arsnouveau.api.registry.PerkRegistry.getPerkItemMap();
        h.assertTrue(registry.get(DefensePerk.STURDY.getRegistryName())==ArsMiscMod.STURDY_THREAD.get()
                && registry.get(DefensePerk.RESILIENT.getRegistryName())==ArsMiscMod.RESILIENT_THREAD.get(),"Threads lack native registration");
        h.assertTrue(!registry.containsKey(net.minecraft.resources.ResourceLocation.parse("ars_misc:thread_withering")),"Withering thread is still registered");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void duplicateThreadsUseHighestLevelAndRestoreLowerLevel(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var robes=ItemsRegistry.ARCANIST_ROBES.get();
        var hood=ItemsRegistry.ARCANIST_HOOD.get();
        player.setItemSlot(EquipmentSlot.CHEST,armor(robes,null,0));
        player.setItemSlot(EquipmentSlot.HEAD,armor(hood,null,0));
        player.tick();DefensePerk.refresh(player);
        double baseArmor=player.getAttributeValue(Attributes.ARMOR);
        player.setItemSlot(EquipmentSlot.CHEST,armor(robes,DefensePerk.STURDY,3));
        player.setItemSlot(EquipmentSlot.HEAD,armor(hood,DefensePerk.STURDY,3));
        player.tick();DefensePerk.refresh(player);DefensePerk.refresh(player);
        h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR)-baseArmor*1.6)<.00001,"Duplicate threads stacked or refresh accumulated armor");
        player.setItemSlot(EquipmentSlot.HEAD,armor(hood,DefensePerk.STURDY,1));
        player.tick();DefensePerk.refresh(player);
        h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR)-baseArmor*1.6)<.00001,"A lower-level thread replaced the highest one");
        player.setItemSlot(EquipmentSlot.CHEST,armor(robes,DefensePerk.RESILIENT,3));
        player.tick();DefensePerk.refresh(player);
        h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR)-baseArmor*1.2)<.00001
                && player.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(DefensePerk.RESILIENT.getRegistryName()).amount()==12,"Removing highest did not restore lower armor or types did not coexist");
        player.setItemSlot(EquipmentSlot.HEAD,armor(hood,null,0));
        player.setItemSlot(EquipmentSlot.CHEST,armor(robes,null,0));
        player.tick();DefensePerk.refresh(player);
        h.assertTrue(player.getAttributeValue(Attributes.ARMOR)==baseArmor
                && player.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(DefensePerk.RESILIENT.getRegistryName())==null,"Unequipping left defense modifiers");
        h.succeed();
    }
}
