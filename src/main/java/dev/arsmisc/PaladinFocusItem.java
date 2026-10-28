package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.item.ArsNouveauCurio;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.SlotContext;
import java.util.List;

public final class PaladinFocusItem extends ArsNouveauCurio {
    public PaladinFocusItem() { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) { return context.identifier().equals("an_focus"); }
    @Override public void curioTick(SlotContext context, ItemStack stack) {
        if (!context.cosmetic() && !context.entity().level().isClientSide) CrystalShieldEvents.refreshMovement(context.entity());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.ars_misc.paladin_focus.summary"));
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            lines.add(Component.translatable("tooltip.ars_misc.paladin_focus.amplify"));
            lines.add(Component.translatable("tooltip.ars_misc.paladin_focus.cleanse"));
            lines.add(Component.translatable("tooltip.ars_misc.paladin_focus.dispel"));
        } else {
            lines.add(Component.translatable("tooltip.ars_misc.paladin_focus.shift")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
