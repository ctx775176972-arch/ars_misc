package dev.arsmisc.spell;

import dev.arsmisc.ArsMiscMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import java.util.function.BooleanSupplier;

/** Only committed shield blocks of the current amplified Cut damage can disable a shield. */
@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class AmplifiedCutShield {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private static final String COOLDOWN = "ars_misc:cut_shield_disabled_until";
    private static final class Frame {
        final LivingEntity target;
        final DamageSource source;
        boolean blocked;
        Frame(LivingEntity target, DamageSource source) { this.target = target; this.source = source; }
    }

    public static boolean duringDamage(LivingEntity target, DamageSource source, BooleanSupplier damage) {
        var previous = CURRENT.get();
        var frame = new Frame(target, source);
        CURRENT.set(frame);
        try {
            boolean result = damage.getAsBoolean();
            if (frame.blocked && target.isAlive() && ReactiveShieldCasting.isShield(target.getUseItem())) {
                if (target instanceof Player player) player.disableShield();
                else {
                    target.getPersistentData().putLong(COOLDOWN, target.level().getGameTime() + 100);
                    target.stopUsingItem();
                    target.level().broadcastEntityEvent(target, (byte)30);
                }
            }
            return result;
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    public static void recordBlock(LivingEntity target, DamageSource source, float amount) {
        var frame = CURRENT.get();
        if (frame != null && frame.target == target && frame.source == source && amount > 0) frame.blocked = true;
    }

    @SubscribeEvent public static void startUsing(LivingEntityUseItemEvent.Start event) {
        var target = event.getEntity();
        if (!target.level().isClientSide && !(target instanceof Player)
                && ReactiveShieldCasting.isShield(event.getItem())
                && target.getPersistentData().getLong(COOLDOWN) > target.level().getGameTime()) event.setCanceled(true);
    }
}
