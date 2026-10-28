package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.event.SpellModifierEvent;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentFortune;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import com.hollingsworth.arsnouveau.api.util.ManaUtil;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class AccessoryEvents {
    private static final String GLASSES_NEXT_REFUND = "ars_misc:glasses_next_mana_refund";
    private AccessoryEvents() {}

    public static boolean isWearing(LivingEntity entity, Item item, String slot) {
        return entity != null && CuriosApi.getCuriosInventory(entity)
                .map(handler -> {
                    var slots = handler.getStacksHandler(slot);
                    if (slots.isEmpty()) return false;
                    var stacks = slots.get().getStacks();
                    for (int index = 0; index < stacks.getSlots(); index++) {
                        if (handler.isSlotActive(slot, index) && stacks.getStackInSlot(index).is(item)) return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public static void modifySpell(SpellModifierEvent event) {
        if (!(event.spellPart instanceof EffectBreak)) return;
        // Stats only: no saved recipe changes or additional mana costs.
        int fortuneTier = highestTier(event.caster, MiningRingItem.Series.FORTUNE);
        if (fortuneTier > 0) {
            var augments = new java.util.ArrayList<>(event.builder.build().getAugments());
            int current = event.builder.build().getBuffCount(AugmentFortune.INSTANCE);
            int desired = Math.min(4, current + fortuneTier * 2);
            augments.removeIf(augment -> augment == AugmentFortune.INSTANCE);
            for (int count = 0; count < desired; count++) augments.add(AugmentFortune.INSTANCE);
            event.builder.setAugments(augments);
        }
        var blastingRing = highestRing(event.caster, MiningRingItem.Series.BLASTING);
        int blastingTier = blastingRing.getItem() instanceof MiningRingItem ring ? ring.tier : 0;
        if (blastingTier > 0) {
            for (int count = 0; count < blastingTier; count++) {
                event.builder.addAugment(AugmentAmplify.INSTANCE);
                AugmentAmplify.INSTANCE.applyModifiers(event.builder, event.spellPart,
                        event.rayTraceResult, event.world, event.caster, event.spellContext);
            }
            if (MiningRingItem.rangeEnabled(blastingRing)) {
                for (int count = 0; count < 2; count++) {
                    event.builder.addAugment(AugmentAOE.INSTANCE);
                    AugmentAOE.INSTANCE.applyModifiers(event.builder, event.spellPart,
                            event.rayTraceResult, event.world, event.caster, event.spellContext);
                }
                if (blastingTier == 2) {
                    event.builder.addAugment(AugmentPierce.INSTANCE).addAugment(AugmentPierce.INSTANCE);
                }
            }
        }
    }

    private static int highestTier(LivingEntity entity, MiningRingItem.Series series) {
        var stack = highestRing(entity, series);
        return stack.getItem() instanceof MiningRingItem ring ? ring.tier : 0;
    }

    private static ItemStack highestRing(LivingEntity entity, MiningRingItem.Series series) {
        if (entity == null) return ItemStack.EMPTY;
        return CuriosApi.getCuriosInventory(entity).map(handler -> {
            var slots = handler.getStacksHandler("ring");
            if (slots.isEmpty()) return ItemStack.EMPTY;
            var stacks = slots.get().getStacks();
            int highest = 0;
            ItemStack selected = ItemStack.EMPTY;
            for (int index = 0; index < stacks.getSlots(); index++) {
                if (handler.isSlotActive("ring", index)
                        && stacks.getStackInSlot(index).getItem() instanceof MiningRingItem ring
                        && ring.series == series && ring.tier > highest) {
                    highest = ring.tier;
                    selected = stacks.getStackInSlot(index);
                }
            }
            return selected;
        }).orElse(ItemStack.EMPTY);
    }
    public static void afterSpellDamage(SpellDamageEvent.Post event) {
        boolean critical = CriticalTracker.consume(event);
        if (critical && event.caster instanceof Player player && !player.level().isClientSide) {
            if (isWearing(player, ArsMiscMod.CRITICAL_GLASSES.get(), "head")) {
                long now = player.level().getGameTime();
                var data = player.getPersistentData();
                if (data.contains(GLASSES_NEXT_REFUND) && now < data.getLong(GLASSES_NEXT_REFUND)) return;
                // Player-wide cooldown prevents multiple targets and equipment swaps from multiplying refunds.
                data.putLong(GLASSES_NEXT_REFUND, now + 20);
                double amount = ManaUtil.getManaRegen(player) * 5;
                var mana = CapabilityRegistry.getMana(player);
                if (mana != null && Double.isFinite(amount) && amount > 0) mana.addMana(amount);
            }
        }
    }
}
