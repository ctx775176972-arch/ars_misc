package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.event.*;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class PaladinFocusEvents {
    public static boolean equipped(LivingEntity entity) {
        return AccessoryEvents.isWearing(entity, ArsMiscMod.PALADIN_FOCUS.get(), "an_focus");
    }

    /** Ownership also covers native Ars summons through OwnableEntity. */
    public static boolean friendly(net.minecraft.world.entity.Entity caster, net.minecraft.world.entity.Entity target) {
        if (caster == target || caster.isAlliedTo(target) || target.isAlliedTo(caster)) return true;
        var casterOwner = principal(caster);
        var targetOwner = principal(target);
        return principalId(casterOwner).equals(principalId(targetOwner))
                || casterOwner.isAlliedTo(targetOwner) || targetOwner.isAlliedTo(casterOwner);
    }

    private static java.util.UUID principalId(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof net.minecraft.world.entity.OwnableEntity owned && owned.getOwnerUUID() != null)
            return owned.getOwnerUUID();
        return entity.getUUID();
    }

    private static net.minecraft.world.entity.Entity principal(net.minecraft.world.entity.Entity entity) {
        var seen = new java.util.HashSet<java.util.UUID>();
        while (entity instanceof net.minecraft.world.entity.OwnableEntity owned && seen.add(entity.getUUID())) {
            var owner = owned.getOwner();
            if (owner == null) break;
            entity = owner;
        }
        return entity;
    }

    public static java.util.Collection<MobEffectInstance> dispellableEffects(
            LivingEntity caster, LivingEntity target, java.util.Collection<MobEffectInstance> effects) {
        if (caster == null || !equipped(caster)) return effects;
        if (friendly(caster, target)) return java.util.List.of();
        return effects.stream().filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL).toList();
    }

    @SubscribeEvent public static void amplify(SpellModifierEvent event) {
        if (!equipped(event.caster) || !SpellSchools.ABJURATION.isPartOfSchool(event.spellPart)
                || !event.spellPart.compatibleAugments.contains(AugmentAmplify.INSTANCE)) return;
        int limit = event.spellPart.getAugmentLimit(AugmentAmplify.INSTANCE.getRegistryName());
        // Regeneration's second Amplify is exclusive to the focus, not spell crafting.
        if (event.spellPart == EffectRegeneration.INSTANCE) limit = Math.min(2, limit + 1);
        if (event.builder.build().getBuffCount(AugmentAmplify.INSTANCE) >= limit) return;
        event.builder.addAugment(AugmentAmplify.INSTANCE);
        AugmentAmplify.INSTANCE.applyModifiers(event.builder, event.spellPart, event.rayTraceResult,
                event.world, event.caster, event.spellContext);
    }

    /** Discounts only Abjuration parts and their following augments, using native per-part augment prices. */
    public static int discount(Spell spell) {
        double eligible = 0;
        AbstractSpellPart parent = null;
        for (var part : spell.recipe()) {
            if (part instanceof AbstractAugment augment) {
                if (parent != null && SpellSchools.ABJURATION.isPartOfSchool(parent))
                    eligible += Math.max(0, augment.getCostForPart(parent));
            } else {
                parent = part;
                if (part != null && SpellSchools.ABJURATION.isPartOfSchool(part)) eligible += Math.max(0, part.getCastingCost());
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(eligible * 0.25));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void cost(SpellCostCalcEvent event) {
        if (event.context != null && equipped(event.context.getUnwrappedCaster()))
            event.currentCost = Math.max(0, event.currentCost - discount(event.context.getSpell()));
    }

    @SubscribeEvent public static void healingAbsorption(EffectResolveEvent.Post event) {
        if (event.world.isClientSide || !equipped(event.shooter)
                || !(event.resolveEffect instanceof EffectHeal || event.resolveEffect == EffectRegeneration.INSTANCE)
                || !(event.rayTraceResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)
                || !target.isAlive()) return;
        // Vanilla merging preserves stronger Absorption and handles hidden lower-level effects.
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0), event.shooter);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cleanse(LivingDamageEvent.Post event) {
        var target = event.getEntity();
        if (target.level().isClientSide || !equipped(target)
                || event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        for (var instance : java.util.List.copyOf(target.getActiveEffects())) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && target.removeEffect(instance.getEffect())) break;
        }
    }
    private PaladinFocusEvents() {}
}
