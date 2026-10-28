package dev.arsmisc;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

public final class DecayEvents {
    private DecayEvents() {}
    private static final ThreadLocal<java.util.Set<LivingEntity>> CONVERTING = ThreadLocal.withInitial(() -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    public static void onHeal(LivingHealEvent event) {
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        if (target.level().isClientSide || !target.isAlive() || event.isCanceled()
                || !Float.isFinite(amount) || amount <= 0 || !target.hasEffect(ArsMiscMod.DECAY)) return;
        // Convert the final requested heal, without damage bonuses or critical rolls.
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
