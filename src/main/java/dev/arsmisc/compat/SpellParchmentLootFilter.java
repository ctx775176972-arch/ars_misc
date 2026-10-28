package dev.arsmisc.compat;

import dev.arsmisc.MiscConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Filters generated loot after every global modifier, including dynamically injected parchment loot. */
public final class SpellParchmentLootFilter {
    private static final ResourceLocation PARCHMENT = ResourceLocation.parse("ars_additions:imbued_spell_parchment");
    private SpellParchmentLootFilter() {}
    public static void filter(List<ItemStack> loot) {
        if (!MiscConfig.DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT.get()) return;
        BuiltInRegistries.ITEM.getOptional(PARCHMENT).ifPresent(item -> loot.removeIf(stack -> stack.is(item)));
    }
}
