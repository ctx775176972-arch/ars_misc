package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class NecklaceManaPower {
    private static final ResourceLocation BONUS = ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "necklace_mana_power");
    private NecklaceManaPower() {}

    public static int maxMana(LivingEntity entity) {
        var mana = CapabilityRegistry.getMana(entity);
        return mana == null ? 0 : Math.max(0, mana.getMaxMana());
    }

    public static int power(LivingEntity entity) { return maxMana(entity) / 150; }

    public static void refresh(LivingEntity entity) {
        var attribute = entity.getAttribute(PerkAttributes.SPELL_DAMAGE_BONUS);
        if (attribute == null) return;
        int amount = AccessoryEvents.isWearing(entity, ArsMiscMod.CRITICAL_NECKLACE.get(), "necklace") ? power(entity) : 0;
        var existing = attribute.getModifier(BONUS);
        if (existing != null && existing.amount() == amount) return;
        if (existing != null) attribute.removeModifier(BONUS);
        if (amount > 0) attribute.addTransientModifier(new AttributeModifier(BONUS, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity entity && !entity.level().isClientSide) refresh(entity);
    }

    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ArsMiscMod.CRITICAL_NECKLACE.get()) || event.getEntity() == null) return;
        var player = event.getEntity();
        event.getToolTip().add(Component.translatable("tooltip.ars_misc.critical_necklace.current_power", maxMana(player), power(player))
                .withStyle(ChatFormatting.BLUE));
    }
}
