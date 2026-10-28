package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.*;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.items.data.ArmorPerkHolder;
import com.hollingsworth.arsnouveau.common.perk.EmptyPerk;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectDispel;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class CreativeFlightGameTests {
    private static ItemStack threaded(Item item) {
        var stack = item.getDefaultInstance();
        var holder = new ArmorPerkHolder().setTier(2);
        var perks = new java.util.ArrayList<IPerk>();
        boolean installed = false;
        for (var slot : holder.getSlotsForTier(stack)) {
            if (!installed && slot.value() >= 3) { perks.add(CreativeFlightPerk.INSTANCE); installed = true; }
            else perks.add(EmptyPerk.INSTANCE);
        }
        if (!installed) throw new IllegalStateException("No native tier III slot");
        stack.set(DataComponentRegistry.ARMOR_PERKS.get(), holder.setPerks(perks));
        return stack;
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void nativeArmorGrantsFlightAndKeepsOtherFlightSources(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        h.assertTrue(!player.mayFly(), "Survival player already flies");
        player.setItemSlot(EquipmentSlot.CHEST, threaded(ItemsRegistry.ARCANIST_ROBES.get()));
        player.tick();
        h.assertTrue(player.mayFly() && player.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT) == 1,
                "Native armor did not apply creative flight");
        var context = SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE, EffectFlight.INSTANCE), player, ItemStack.EMPTY);
        EffectFlight.INSTANCE.onResolveEntity(new EntityHitResult(player), h.getLevel(), player,
                new SpellStats.Builder().build(), context, new SpellResolver(context));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.tick();
        h.assertTrue(player.mayFly() && player.hasEffect(ModPotions.FLIGHT_EFFECT), "Removing the thread removed spell flight");
        player.removeEffect(ModPotions.FLIGHT_EFFECT);
        h.assertTrue(!player.mayFly(), "Last flight source retained permission");
        player.setItemSlot(EquipmentSlot.CHEST, threaded(ItemsRegistry.ARCANIST_ROBES.get()));
        player.tick();
        EffectFlight.INSTANCE.onResolveEntity(new EntityHitResult(player), h.getLevel(), player,
                new SpellStats.Builder().build(), context, new SpellResolver(context));
        EffectDispel.INSTANCE.onResolveEntity(new EntityHitResult(player), h.getLevel(), player,
                new SpellStats.Builder().build(), context, new SpellResolver(context));
        h.assertTrue(!player.hasEffect(ModPotions.FLIGHT_EFFECT) && player.mayFly(), "Dispel removed equipment flight");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.tick();
        h.assertTrue(!player.mayFly() && !player.getAbilities().mayfly, "Thread left a permanent ability flag");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void threadRequiresTierThreeAndRegistersAsNativePerk(GameTestHelper h) {
        var perk = CreativeFlightPerk.INSTANCE;
        var item = ArsMiscMod.CREATIVE_FLIGHT_THREAD.get();
        h.assertTrue(perk.minimumSlot() == PerkSlot.THREE
                && com.hollingsworth.arsnouveau.api.registry.PerkRegistry.getPerkItemMap().get(perk.getRegistryName()) == item,
                "Flight thread registration or minimum slot is wrong");
        var attributes = net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;
        h.assertTrue(perk.applyAttributeModifiers(attributes, ItemStack.EMPTY, 2,
                net.minecraft.world.entity.EquipmentSlotGroup.CHEST).equals(attributes), "Tier II slot granted flight");
        h.succeed();
    }
}
