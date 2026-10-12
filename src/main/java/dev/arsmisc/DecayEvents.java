package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

public final class DecayEvents {
    private DecayEvents() {}
    private static final ThreadLocal<java.util.Set<LivingEntity>> CONVERTING = ThreadLocal.withInitial(() -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    public static void afterSpellDamage(SpellDamageEvent.Post event) {
        var hitTarget = event.target instanceof net.neoforged.neoforge.entity.PartEntity<?> part ? part.getParent() : event.target;
        if (!event.caster.level().isClientSide && event.caster instanceof Player player
                && hitTarget instanceof LivingEntity target && target != player
                && target.isAlive() && !player.isAlliedTo(target)
                && AccessoryEvents.isWearing(player, ArsMiscMod.DECAY_RING.get(), "ring")) {
            target.addEffect(new MobEffectInstance(ArsMiscMod.DECAY, 600, 0));
        }
    }

    public static void onHeal(LivingHealEvent event) {
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        if (target.level().isClientSide || !target.isAlive() || event.isCanceled()
                || !Float.isFinite(amount) || amount <= 0 || !target.hasEffect(ArsMiscMod.DECAY)) return;
        // Prototype: convert the final requested heal, without damage bonuses or critical rolls.
        // Failed hurt() is not used to infer boss phases or to restore the cancelled healing.
        event.setCanceled(true);
        var converting = CONVERTING.get();
        if (!converting.add(target)) return;
        try {
            target.hurt(target.damageSources().magic(), amount);
        } finally {
            converting.remove(target);
            if (converting.isEmpty()) CONVERTING.remove();
        }
    }
}
