package dev.arsmisc.spell;

import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.registry.ModBookComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class SpellbookBlessing {
    public static boolean blessed(ItemStack stack) {
        return stack.getItem() instanceof SpellBook && Boolean.TRUE.equals(stack.get(ModBookComponents.BLESSING.get()));
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity entity && !entity.level().isClientSide()) refresh(entity);
    }
    public static void refresh(LivingEntity entity) {
        boolean equipped = CuriosApi.getCuriosInventory(entity).map(inventory -> {
            for (var entry : inventory.getCurios().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int index = 0; index < stacks.getSlots(); index++) {
                    if (inventory.isSlotActive(entry.getKey(), index) && blessed(stacks.getStackInSlot(index))) return true;
                }
            }
            return false;
        }).orElse(false);
        update(entity, PerkAttributes.MAX_MANA, "blessing_max_mana", 600, equipped);
        update(entity, PerkAttributes.MANA_REGEN_BONUS, "blessing_mana_regen", 50, equipped);
        update(entity, PerkAttributes.SPELL_DAMAGE_BONUS, "blessing_spell_damage", 8, equipped);
    }
    private static void update(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, String name, double amount, boolean enabled) {
        var instance = entity.getAttribute(attribute);
        if (instance == null) return;
        var id = ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, name);
        var existing = instance.getModifier(id);
        if (!enabled) { if (existing != null) instance.removeModifier(id); return; }
        if (existing != null && existing.amount() == amount) return;
        if (existing != null) instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (!blessed(event.getItemStack())) return;
        event.getToolTip().add(Component.translatable("tooltip.ars_misc.spellbook_blessing").withStyle(net.minecraft.ChatFormatting.GOLD));
        event.getToolTip().add(Component.translatable("tooltip.ars_misc.spellbook_blessing_bonus", 600, 50, 8).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
