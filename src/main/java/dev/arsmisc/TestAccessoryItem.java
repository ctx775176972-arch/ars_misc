package dev.arsmisc;

import com.alexthw.sauce.registry.ModRegistry;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hollingsworth.arsnouveau.api.item.ArsNouveauCurio;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import java.util.List;

public final class TestAccessoryItem extends ArsNouveauCurio {
    private final String slot;
    private final boolean combat;
    private final String tooltip;

    public TestAccessoryItem(String slot, boolean combat, String tooltip) {
        super(new Properties().stacksTo(1));
        this.slot = slot;
        this.combat = combat;
        this.tooltip = tooltip;
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return slot.equals(context.identifier());
    }

    @Override public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext context, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        if (combat && slot.equals(context.identifier())) {
            modifiers.put(ModRegistry.SPELL_CRIT, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "critical_belt_chance"),
                    0.5, AttributeModifier.Operation.ADD_VALUE));
        }
        return modifiers;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.ars_misc." + tooltip)
                .withStyle(ChatFormatting.GRAY));
        if (combat) lines.add(Component.translatable("tooltip.ars_misc.critical_belt.regeneration")
                .withStyle(ChatFormatting.BLUE));
    }
}
