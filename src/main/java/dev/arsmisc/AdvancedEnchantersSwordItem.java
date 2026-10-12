package dev.arsmisc;

import com.hollingsworth.arsnouveau.common.items.EnchantersSword;
import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public final class AdvancedEnchantersSwordItem extends EnchantersSword {
    public AdvancedEnchantersSwordItem() { super(Tiers.DIAMOND, 4, -2.7f); }
    @Override public com.hollingsworth.arsnouveau.api.spell.AbstractCaster<?> getSpellCaster(net.minecraft.world.item.ItemStack stack) {
        return stack.getOrDefault(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER.get(),
            new com.hollingsworth.arsnouveau.api.spell.SpellCaster());
    }
    public static void modifyAttributes(ItemAttributeModifierEvent event) {
        if (!(event.getItemStack().getItem() instanceof AdvancedEnchantersSwordItem)) return;
        // Replace the inherited sword bonus rather than adding it on top of the requested +9.
        event.replaceModifier(PerkAttributes.SPELL_DAMAGE_BONUS, new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath("ars_nouveau","sword_spell_bonus"),9,
            AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
        event.addModifier(Attributes.ARMOR, modifier("sword_armor",4),EquipmentSlotGroup.MAINHAND);
        event.addModifier(Attributes.BLOCK_INTERACTION_RANGE, modifier("sword_block_range",0.5),EquipmentSlotGroup.MAINHAND);
        event.addModifier(Attributes.ENTITY_INTERACTION_RANGE, modifier("sword_entity_range",2),EquipmentSlotGroup.MAINHAND);
    }
    private static AttributeModifier modifier(String name,double value) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID,name),value,AttributeModifier.Operation.ADD_VALUE);
    }
}
