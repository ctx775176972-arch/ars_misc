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

public final class CriticalGlassesItem extends ArsNouveauCurio {
    public CriticalGlassesItem() {
        super(new Properties().stacksTo(1));
    }

    @Override public boolean isFoil(ItemStack stack) { return false; }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return "head".equals(context.identifier());
    }

    @Override public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext context, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        if ("head".equals(context.identifier())) {
            modifiers.put(ModRegistry.SPELL_CRIT, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "critical_glasses_chance"),
                    0.5, AttributeModifier.Operation.ADD_VALUE));
        }
        return modifiers;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.ars_misc.critical_glasses.mana_refund")
                .withStyle(ChatFormatting.BLUE));
    }
}
