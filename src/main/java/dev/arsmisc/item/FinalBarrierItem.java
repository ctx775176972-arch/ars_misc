package dev.arsmisc.item;

import com.hollingsworth.arsnouveau.api.item.ArsNouveauCurio;
import dev.arsmisc.FinalBarrier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import top.theillusivec4.curios.api.SlotContext;
import java.util.List;

public final class FinalBarrierItem extends ArsNouveauCurio {
    public FinalBarrierItem() { super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()); }
    public static boolean enabled(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("final_barrier_enabled");
    }
    public static void setEnabled(ItemStack stack, boolean enabled) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("final_barrier_enabled", enabled));
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return false; }
    @Override public void curioTick(SlotContext context, ItemStack stack) {
        if (!context.cosmetic() && context.entity() instanceof Player player && !player.level().isClientSide) FinalBarrier.tick(player);
    }
    @Override public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        if (!context.entity().level().isClientSide) setEnabled(stack, false);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.ars_misc.final_barrier"));
        lines.add(Component.translatable(enabled(stack) ? "tooltip.ars_misc.final_barrier.enabled" : "tooltip.ars_misc.final_barrier.disabled"));
    }
}
