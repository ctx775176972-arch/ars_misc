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

public final class SpellDamageBeltItem extends ArsNouveauCurio {
    public SpellDamageBeltItem() { super(new Properties().stacksTo(1)); }

    @Override public boolean isFoil(ItemStack stack) { return false; }

    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return "belt".equals(context.identifier());
    }

    @Override public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext context, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        if ("belt".equals(context.identifier())) {
            modifiers.put(ModRegistry.SPELL_DAMAGE_MULTIPLIER, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "belt_spell_damage"),
                    0.15, AttributeModifier.Operation.ADD_VALUE));
        }
        return modifiers;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.ars_misc.critical_belt").withStyle(ChatFormatting.GRAY));
    }
}
