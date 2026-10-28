package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsmisc.AccessoryEvents;
import dev.arsmisc.ArsMiscMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IDamageEffect.class)
public interface SpellDamageBeltMixin {
    @WrapOperation(method = "attemptDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean arsMisc$clearHurtCooldown(Entity target, DamageSource source, float amount,
            Operation<Boolean> original, Level level, LivingEntity caster, SpellStats stats,
            SpellContext context, SpellResolver resolver, Entity spellTarget, DamageSource spellSource,
            float baseDamage) {
        // This runs after damage events and their cancellation checks, immediately before the real hit.
        if (!level.isClientSide && target instanceof LivingEntity living
                && AccessoryEvents.isWearing(caster, ArsMiscMod.CRITICAL_BELT.get(), "belt")) {
            living.invulnerableTime = 0;
        }
        return original.call(target, source, amount);
    }
}
