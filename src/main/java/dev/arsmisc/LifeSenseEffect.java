package dev.arsmisc;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class LifeSenseEffect extends MobEffect {
    public static final double RANGE = 75;
    public LifeSenseEffect() { super(MobEffectCategory.BENEFICIAL, 0x64c9ba); }
    public static boolean shouldHighlight(LivingEntity observer, Entity entity) {
        if (observer == null || entity == observer || observer.level() != entity.level()
                || !observer.hasEffect(ArsMiscMod.LIFE_SENSE)) return false;
        Entity subject = entity instanceof net.neoforged.neoforge.entity.PartEntity<?> part ? part.getParent() : entity;
        return subject != observer && subject instanceof LivingEntity living && living.isAlive()
                && !living.isSpectator() && observer.distanceToSqr(subject) <= RANGE * RANGE;
    }
}
