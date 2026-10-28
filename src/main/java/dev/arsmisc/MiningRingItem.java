package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.item.ArsNouveauCurio;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import java.util.List;

public final class MiningRingItem extends ArsNouveauCurio {
    public enum Series { FORTUNE, BLASTING }
    public final Series series;
    public final int tier;
    public MiningRingItem(Series series, int tier) {
        super(new Properties().stacksTo(1));
        this.series = series;
        this.tier = tier;
    }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) { return context.identifier().equals("ring"); }
    public static boolean rangeEnabled(ItemStack stack) {
        var data = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return !data.getBoolean("mining_range_disabled");
    }
    public static void setRangeEnabled(ItemStack stack, boolean enabled) {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                stack, data -> data.putBoolean("mining_range_disabled", !enabled));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if (series == Series.FORTUNE) lines.add(Component.translatable("tooltip.ars_misc.mining_fortune", tier * 2).withStyle(ChatFormatting.GRAY));
        else {
            lines.add(Component.translatable(rangeEnabled(stack) ? "tooltip.ars_misc.mining_blasting_" + tier
                    : "tooltip.ars_misc.mining_blasting_single", tier).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.ars_misc.mining_range." + (rangeEnabled(stack) ? "enabled" : "disabled"))
                    .withStyle(ChatFormatting.BLUE));
        }
    }
}
