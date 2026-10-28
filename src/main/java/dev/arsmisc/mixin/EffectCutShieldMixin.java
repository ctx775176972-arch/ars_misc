package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectCut;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsmisc.spell.AmplifiedCutShield;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EffectCut.class)
public class EffectCutShieldMixin {
    @WrapOperation(method = "onResolveEntity", at = @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/common/spell/effect/EffectCut;attemptDamage(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean amplifiedShieldBreak(EffectCut effect, Level level, LivingEntity caster, SpellStats stats,
            SpellContext context, SpellResolver resolver, Entity target, DamageSource source, float amount,
            Operation<Boolean> original) {
        if (!level.isClientSide && stats.getBuffCount(AugmentAmplify.INSTANCE) > 0 && target instanceof LivingEntity living) {
            return AmplifiedCutShield.duringDamage(living, source,
                    () -> original.call(effect, level, caster, stats, context, resolver, target, source, amount));
        }
        return original.call(effect, level, caster, stats, context, resolver, target, source, amount);
    }
}
