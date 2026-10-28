package dev.arsmisc;

import dev.arsmisc.mixin.LivingTotemInvoker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import java.util.ArrayList;

/** One scoped execution attempt; never retries after successful death protection. */
@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class AvadaKedavraExecution {
    private record Attempt(LivingEntity target, ArrayList<LivingDeathEvent> deaths,
                           ArrayList<LivingUseTotemEvent> totems) {
        boolean protectedDeath() {
            return deaths.stream().anyMatch(LivingDeathEvent::isCanceled)
                    || totems.stream().anyMatch(event -> !event.isCanceled());
        }
    }
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();

    public static void execute(LivingEntity caster, LivingEntity target, DamageSource source) {
        var previous = CURRENT.get();
        var attempt = new Attempt(target, new ArrayList<>(), new ArrayList<>());
        CURRENT.set(attempt);
        try {
            // The barrier's damage protection can exhaust it before the call returns.
            boolean barrierProtected = target instanceof Player player && FinalBarrier.active(player);
            target.hurt(source, Float.MAX_VALUE);
            if (!target.isAlive() || attempt.protectedDeath() || barrierProtected) return;
            // Damage caps may prevent a lethal hit from reaching vanilla's totem check.
            if (((LivingTotemInvoker) target).arsMisc$tryTotem(source)) return;
            target.setLastHurtByMob(caster);
            if (caster instanceof Player player) target.setLastHurtByPlayer(player);
            float health = target.getHealth();
            target.getCombatTracker().recordDamage(source, health);
            target.setHealth(0);
            target.die(source);
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void death(LivingDeathEvent event) {
        var attempt = CURRENT.get();
        if (attempt != null && attempt.target == event.getEntity()) attempt.deaths.add(event);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void totem(LivingUseTotemEvent event) {
        var attempt = CURRENT.get();
        if (attempt != null && attempt.target == event.getEntity()) attempt.totems.add(event);
    }
    private AvadaKedavraExecution() {}
}
