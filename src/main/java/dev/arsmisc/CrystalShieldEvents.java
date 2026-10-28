package dev.arsmisc;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class CrystalShieldEvents {
    private CrystalShieldEvents() {}

    public static double reduction(LivingEntity target) {
        var effect = target.getEffect(ArsMiscMod.CRYSTAL_SHIELD);
        if (effect == null) return 0;
        double power = target.getPersistentData().getDouble(CrystalShieldEffect.POWER_KEY);
        if (!Double.isFinite(power)) power = 0;
        boolean focused = PaladinFocusEvents.equipped(target);
        return Math.clamp(0.10 + 0.05 * (effect.getAmplifier() + 1) + (focused ? 0.02 : 0.01) * power,
                0, focused ? 0.8 : 0.6);
    }

    public static void refreshMovement(LivingEntity target) {
        var speed = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (!target.hasEffect(ArsMiscMod.CRYSTAL_SHIELD) || PaladinFocusEvents.equipped(target)) {
            speed.removeModifier(CrystalShieldEffect.SLOW_ID);
        } else if (speed.getModifier(CrystalShieldEffect.SLOW_ID) == null) {
            speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    CrystalShieldEffect.SLOW_ID, -0.25,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @SubscribeEvent public static void movement(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide) refreshMovement(living);
    }

    @SubscribeEvent public static void reduceDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getSource().is(DamageTypeTags.BYPASSES_EFFECTS)
                || event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) return;
        event.setAmount((float) (event.getAmount() * (1 - reduction(event.getEntity()))));
    }

}
