package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.Perk;
import com.hollingsworth.arsnouveau.api.perk.PerkSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.NeoForgeMod;

public final class CreativeFlightPerk extends Perk {
    public static final CreativeFlightPerk INSTANCE = new CreativeFlightPerk();

    private CreativeFlightPerk() {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "thread_creative_flight"));
    }

    @Override public PerkSlot minimumSlot() { return PerkSlot.THREE; }
    @Override public String getLangName() { return "Flight"; }
    @Override public String getName() { return Component.translatable("item.ars_misc.thread_creative_flight").getString(); }
    @Override public String getLangDescription() {
        return "Grants flight while equipped in a tier III or higher thread slot.";
    }

    @Override public ItemAttributeModifiers applyAttributeModifiers(ItemAttributeModifiers modifiers, ItemStack stack,
            int level, EquipmentSlotGroup slot) {
        if (level < 3) return modifiers;
        // Separate equipment IDs let another installed thread retain flight when one piece is removed.
        var id = getRegistryName().withSuffix("_" + slot.getSerializedName());
        return modifiers.withModifierAdded(NeoForgeMod.CREATIVE_FLIGHT,
                new AttributeModifier(id, 1, AttributeModifier.Operation.ADD_VALUE), slot);
    }
}
